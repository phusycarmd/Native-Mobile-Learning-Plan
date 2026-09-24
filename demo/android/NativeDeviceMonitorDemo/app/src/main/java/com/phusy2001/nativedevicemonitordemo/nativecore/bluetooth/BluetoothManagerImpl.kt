package com.phusy2001.nativedevicemonitordemo.nativecore.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.UUID

class BluetoothManagerImpl(
    private val context: Context
) : BluetoothManager {

    companion object {
        private const val TAG = "BluetoothManager"
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        // Nếu OS không gọi lại onConnectionStateChange sau disconnect() (vd: đang CONNECTING),
        // tự đóng GATT sau khoảng thời gian này để tránh rò rỉ kết nối.
        private const val DISCONNECT_TIMEOUT_MS = 2_000L

        // Thao tác GATT không nhận được callback sau khoảng này thì bỏ qua để hàng đợi không bị kẹt
        private const val GATT_OPERATION_TIMEOUT_MS = 5_000L
    }

    // Android chỉ xử lý 1 thao tác GATT (read, ghi descriptor...) tại một thời điểm:
    // gửi lệnh mới khi lệnh trước chưa có callback sẽ bị từ chối. Vì vậy mọi thao tác được xếp hàng.
    private sealed class GattOperation {
        abstract val characteristic: BluetoothGattCharacteristic

        class Read(
            override val characteristic: BluetoothGattCharacteristic,
            val onResult: (ByteArray?, String?) -> Unit
        ) : GattOperation()

        class Write(
            override val characteristic: BluetoothGattCharacteristic,
            val data: ByteArray,
            val writeType: Int,
            val onResult: (String?) -> Unit
        ) : GattOperation()

        class WriteCccd(
            override val characteristic: BluetoothGattCharacteristic,
            val descriptor: BluetoothGattDescriptor,
            val value: ByteArray
        ) : GattOperation()
    }

    private val androidBluetoothManager: android.bluetooth.BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager

    private val adapter: BluetoothAdapter?
        get() = androidBluetoothManager?.adapter

    // Mọi trạng thái bên dưới chỉ được đọc/ghi trên main thread:
    // các callback GATT (chạy trên binder thread) đều được post về mainHandler trước khi xử lý.
    private var activeScanCallback: ScanCallback? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var connectionState: ConnectionState = ConnectionState.DISCONNECTED
    private val mainHandler = Handler(Looper.getMainLooper())

    private var onStateChangeCallback: ((ConnectionState) -> Unit)? = null
    private val notificationCallbacks = mutableMapOf<UUID, (ByteArray) -> Unit>()
    private val gattQueue = ArrayDeque<GattOperation>()
    private var currentOperation: GattOperation? = null
    private var onServicesDiscoveredSuccess: ((List<BleService>) -> Unit)? = null
    private var onServicesDiscoveredError: ((String) -> Unit)? = null

    private val disconnectTimeout = Runnable {
        if (bluetoothGatt != null) {
            Log.w(TAG, "Không nhận được callback ngắt kết nối, tự đóng GATT.")
            closeGatt()
            updateState(ConnectionState.DISCONNECTED)
        }
    }

    private val operationTimeout = Runnable {
        Log.w(TAG, "Thao tác GATT hết thời gian chờ, chuyển sang thao tác kế tiếp.")
        completeCurrentOperation(null, "Hết thời gian chờ phản hồi từ thiết bị.")
    }

    override fun isAvailable(): Boolean {
        return adapter != null
    }

    override fun isEnabled(): Boolean {
        return adapter?.isEnabled == true
    }

    @SuppressLint("MissingPermission")
    override fun startScan(onDeviceFound: (BleDevice) -> Unit, onError: (String) -> Unit) {
        val bluetoothAdapter = adapter
        if (bluetoothAdapter == null) {
            onError("Thiết bị không hỗ trợ Bluetooth.")
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            onError("Bluetooth đang tắt. Vui lòng bật Bluetooth để quét.")
            return
        }

        val scanner = bluetoothAdapter.bluetoothLeScanner
        if (scanner == null) {
            onError("Không thể khởi tạo Bluetooth LE Scanner.")
            return
        }

        // Dừng quét trước đó nếu còn đang chạy
        stopScan()

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result ?: return
                val device = result.device
                val name = device.name ?: result.scanRecord?.deviceName
                val address = device.address
                val rssi = result.rssi

                mainHandler.post {
                    onDeviceFound(
                        BleDevice(
                            name = name,
                            address = address,
                            rssi = rssi,
                            isConnectable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                result.isConnectable
                            } else {
                                true
                            }
                        )
                    )
                }
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                results?.forEach { onScanResult(0, it) }
            }

            override fun onScanFailed(errorCode: Int) {
                mainHandler.post {
                    onError("Quét Bluetooth thất bại (mã lỗi: $errorCode).")
                }
            }
        }

        activeScanCallback = callback
        try {
            scanner.startScan(callback)
        } catch (e: Exception) {
            activeScanCallback = null
            onError("Lỗi khi bắt đầu quét: ${e.localizedMessage}")
        }
    }

    @SuppressLint("MissingPermission")
    override fun stopScan() {
        val scanner = adapter?.bluetoothLeScanner
        activeScanCallback?.let { callback ->
            try {
                scanner?.stopScan(callback)
            } catch (_: Exception) {
            }
            activeScanCallback = null
        }
    }

    @SuppressLint("MissingPermission")
    override fun connect(deviceAddress: String, onStateChange: (ConnectionState) -> Unit) {
        val bluetoothAdapter = adapter ?: return
        val device = try {
            bluetoothAdapter.getRemoteDevice(deviceAddress)
        } catch (_: Exception) {
            onStateChange(ConnectionState.DISCONNECTED)
            return
        }

        // Đóng ngay kết nối cũ (nếu có) mà không báo về callback cũ
        closeGatt()
        onStateChangeCallback = onStateChange
        updateState(ConnectionState.CONNECTING)

        val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                mainHandler.post { handleConnectionStateChange(gatt, status, newState) }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS && gatt != null) {
                    val serviceList = gatt.services.map { service ->
                        BleService(
                            uuid = service.uuid.toString(),
                            characteristics = service.characteristics.map { it.uuid.toString() },
                            readableCharacteristics = service.characteristics
                                .filter { it.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0 }
                                .map { it.uuid.toString() },
                            notifiableCharacteristics = service.characteristics
                                .filter { it.supportsNotifyOrIndicate() }
                                .map { it.uuid.toString() }
                        )
                    }
                    mainHandler.post {
                        onServicesDiscoveredSuccess?.invoke(serviceList)
                    }
                } else {
                    mainHandler.post {
                        onServicesDiscoveredError?.invoke("Khám phá services thất bại với status: $status")
                    }
                }
            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray,
                status: Int
            ) {
                val error = if (status == BluetoothGatt.GATT_SUCCESS) null else "Lỗi đọc dữ liệu status: $status"
                mainHandler.post { completeCurrentOperation(value, error) }
            }

            @Suppress("DEPRECATION")
            @Deprecated("Deprecated for SDK 33+")
            override fun onCharacteristicRead(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
                status: Int
            ) {
                val value = characteristic?.value?.copyOf()
                val error = if (status == BluetoothGatt.GATT_SUCCESS) null else "Lỗi đọc dữ liệu status: $status"
                mainHandler.post { completeCurrentOperation(value, error) }
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
                status: Int
            ) {
                val error = if (status == BluetoothGatt.GATT_SUCCESS) null else "Lỗi ghi dữ liệu status: $status"
                mainHandler.post { completeCurrentOperation(null, error) }
            }

            override fun onDescriptorWrite(
                gatt: BluetoothGatt?,
                descriptor: BluetoothGattDescriptor?,
                status: Int
            ) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "Ghi CCCD thành công cho characteristic ${descriptor?.characteristic?.uuid}")
                }
                val error = if (status == BluetoothGatt.GATT_SUCCESS) null else "status $status"
                mainHandler.post { completeCurrentOperation(null, error) }
            }

            // SDK 33+: value được truyền trực tiếp, an toàn khi nhiều notification tới liên tục
            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray
            ) {
                dispatchNotification(characteristic.uuid, value)
            }

            // SDK < 33: characteristic.value có thể bị ghi đè bởi notification kế tiếp,
            // nên copy ngay trên binder thread trước khi post về main thread.
            @Suppress("DEPRECATION")
            @Deprecated("Deprecated for SDK 33+")
            override fun onCharacteristicChanged(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?
            ) {
                characteristic ?: return
                val value = characteristic.value?.copyOf() ?: return
                dispatchNotification(characteristic.uuid, value)
            }
        }

        bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        if (bluetoothGatt == null) {
            updateState(ConnectionState.DISCONNECTED)
        }
    }

    @SuppressLint("MissingPermission")
    override fun disconnect() {
        val gatt = bluetoothGatt
        if (gatt == null) {
            connectionState = ConnectionState.DISCONNECTED
            return
        }
        if (connectionState == ConnectionState.DISCONNECTING) return

        updateState(ConnectionState.DISCONNECTING)
        // Ngừng đẩy dữ liệu lên UI ngay khi người dùng bấm ngắt kết nối
        notificationCallbacks.clear()

        try {
            // Chỉ gọi disconnect(); close() sẽ được gọi khi OS báo STATE_DISCONNECTED.
            // Gọi close() ngay lập tức sẽ khiến onConnectionStateChange không bao giờ được gọi.
            gatt.disconnect()
            mainHandler.postDelayed(disconnectTimeout, DISCONNECT_TIMEOUT_MS)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi ngắt kết nối: ${e.localizedMessage}")
            closeGatt()
            updateState(ConnectionState.DISCONNECTED)
        }
    }

    @SuppressLint("MissingPermission")
    override fun discoverServices(
        onSuccess: (List<BleService>) -> Unit,
        onError: (String) -> Unit
    ) {
        val gatt = bluetoothGatt
        if (gatt == null || connectionState != ConnectionState.CONNECTED) {
            onError("Chưa kết nối tới thiết bị.")
            return
        }
        onServicesDiscoveredSuccess = onSuccess
        onServicesDiscoveredError = onError
        gatt.discoverServices()
    }

    @SuppressLint("MissingPermission")
    override fun read(characteristicUuid: String, onResult: (ByteArray?, String?) -> Unit) {
        val gatt = bluetoothGatt
        if (gatt == null || connectionState != ConnectionState.CONNECTED) {
            onResult(null, "Chưa kết nối tới thiết bị.")
            return
        }

        val targetCharacteristic = findCharacteristic(characteristicUuid)
        if (targetCharacteristic == null) {
            onResult(null, "Không tìm thấy Characteristic với UUID: $characteristicUuid")
            return
        }

        if (targetCharacteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ == 0) {
            onResult(null, "Characteristic $characteristicUuid không hỗ trợ Read.")
            return
        }

        enqueue(GattOperation.Read(targetCharacteristic, onResult))
    }

    override fun write(characteristicUuid: String, data: ByteArray, onResult: (String?) -> Unit) {
        if (bluetoothGatt == null || connectionState != ConnectionState.CONNECTED) {
            onResult("Chưa kết nối tới thiết bị.")
            return
        }

        val targetCharacteristic = findCharacteristic(characteristicUuid)
        if (targetCharacteristic == null) {
            onResult("Không tìm thấy Characteristic với UUID: $characteristicUuid")
            return
        }

        // Ưu tiên Write (có phản hồi từ thiết bị), chỉ dùng Write Without Response khi characteristic không hỗ trợ
        val properties = targetCharacteristic.properties
        val writeType = when {
            properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0 ->
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT

            properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0 ->
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE

            else -> {
                onResult("Characteristic $characteristicUuid không hỗ trợ Write.")
                return
            }
        }

        enqueue(GattOperation.Write(targetCharacteristic, data, writeType, onResult))
    }

    @SuppressLint("MissingPermission")
    override fun subscribe(characteristicUuid: String, onNotification: (ByteArray) -> Unit) {
        val gatt = bluetoothGatt
        if (gatt == null || connectionState != ConnectionState.CONNECTED) {
            Log.w(TAG, "subscribe: chưa kết nối tới thiết bị.")
            return
        }

        val targetCharacteristic = findCharacteristic(characteristicUuid)
        if (targetCharacteristic == null) {
            Log.w(TAG, "subscribe: không tìm thấy characteristic $characteristicUuid (đã discoverServices chưa?)")
            return
        }

        // Notify: thiết bị gửi không cần ACK. Indicate: thiết bị chờ ACK từ phía điện thoại.
        val cccdValue = when {
            targetCharacteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0 ->
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE

            targetCharacteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0 ->
                BluetoothGattDescriptor.ENABLE_INDICATION_VALUE

            else -> {
                Log.w(TAG, "subscribe: characteristic $characteristicUuid không hỗ trợ Notify/Indicate.")
                return
            }
        }

        // Bước 1: bật notification ở phía Android (local)
        if (!gatt.setCharacteristicNotification(targetCharacteristic, true)) {
            Log.e(TAG, "subscribe: setCharacteristicNotification thất bại.")
            return
        }
        notificationCallbacks[targetCharacteristic.uuid] = onNotification

        // Bước 2: ghi CCCD (0x2902) để yêu cầu thiết bị ngoại vi bắt đầu gửi dữ liệu
        val descriptor = targetCharacteristic.getDescriptor(CCCD_UUID)
        if (descriptor == null) {
            Log.w(TAG, "subscribe: characteristic $characteristicUuid không có CCCD descriptor.")
            return
        }
        enqueue(GattOperation.WriteCccd(targetCharacteristic, descriptor, cccdValue))
    }

    @SuppressLint("MissingPermission")
    override fun unsubscribe(characteristicUuid: String) {
        val targetCharacteristic = findCharacteristic(characteristicUuid) ?: return
        // Gỡ callback trước để không còn dữ liệu nào đẩy lên UI nữa
        notificationCallbacks.remove(targetCharacteristic.uuid)

        val gatt = bluetoothGatt ?: return
        if (connectionState != ConnectionState.CONNECTED) return

        gatt.setCharacteristicNotification(targetCharacteristic, false)
        targetCharacteristic.getDescriptor(CCCD_UUID)?.let { descriptor ->
            enqueue(
                GattOperation.WriteCccd(
                    targetCharacteristic,
                    descriptor,
                    BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                )
            )
        }
    }

    override fun getConnectionState(): ConnectionState {
        return connectionState
    }

    // Chạy trên main thread
    private fun handleConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        // Callback trễ từ một GATT cũ (đã bị thay bởi kết nối mới) -> chỉ cần đóng nó lại
        if (gatt != bluetoothGatt) {
            gatt.close()
            return
        }

        when (newState) {
            BluetoothProfile.STATE_CONNECTED -> {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "Đã kết nối tới ${gatt.device.address}")
                    updateState(ConnectionState.CONNECTED)
                } else {
                    Log.e(TAG, "Kết nối thất bại: ${describeGattStatus(status)}")
                    closeGatt()
                    updateState(ConnectionState.DISCONNECTED)
                }
            }

            BluetoothProfile.STATE_DISCONNECTED -> {
                val userInitiated = connectionState == ConnectionState.DISCONNECTING
                if (userInitiated) {
                    Log.d(TAG, "Đã ngắt kết nối theo yêu cầu người dùng.")
                } else {
                    // Mất kết nối đột ngột: thiết bị tắt nguồn, ra khỏi vùng phủ sóng, Bluetooth bị tắt...
                    Log.w(TAG, "Mất kết nối đột ngột: ${describeGattStatus(status)}")
                }
                closeGatt()
                updateState(ConnectionState.DISCONNECTED)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun closeGatt() {
        mainHandler.removeCallbacks(disconnectTimeout)
        try {
            bluetoothGatt?.close()
        } catch (_: Exception) {
        }
        bluetoothGatt = null
        notificationCallbacks.clear()

        // Huỷ hàng đợi GATT và báo lỗi cho các lệnh read đang chờ để UI không bị treo
        mainHandler.removeCallbacks(operationTimeout)
        val pendingOperations = listOfNotNull(currentOperation) + gattQueue
        currentOperation = null
        gattQueue.clear()
        pendingOperations.forEach {
            when (it) {
                is GattOperation.Read -> it.onResult(null, "Mất kết nối tới thiết bị.")
                is GattOperation.Write -> it.onResult("Mất kết nối tới thiết bị.")
                is GattOperation.WriteCccd -> Unit
            }
        }
        onServicesDiscoveredSuccess = null
        onServicesDiscoveredError = null
    }

    private fun updateState(newState: ConnectionState) {
        connectionState = newState
        onStateChangeCallback?.invoke(newState)
    }

    private fun enqueue(operation: GattOperation) {
        gattQueue.addLast(operation)
        processNextOperation()
    }

    @SuppressLint("MissingPermission")
    private fun processNextOperation() {
        if (currentOperation != null) return
        val gatt = bluetoothGatt ?: return
        val operation = gattQueue.removeFirstOrNull() ?: return

        currentOperation = operation
        val started = when (operation) {
            is GattOperation.Read -> gatt.readCharacteristic(operation.characteristic)
            is GattOperation.Write -> writeCharacteristicCompat(gatt, operation)
            is GattOperation.WriteCccd -> writeDescriptorCompat(gatt, operation.descriptor, operation.value)
        }
        if (started) {
            mainHandler.postDelayed(operationTimeout, GATT_OPERATION_TIMEOUT_MS)
        } else {
            completeCurrentOperation(null, "Không gửi được lệnh GATT tới thiết bị.")
        }
    }

    // Chạy trên main thread: kết thúc thao tác hiện tại và chạy thao tác kế tiếp trong hàng đợi
    private fun completeCurrentOperation(value: ByteArray?, error: String?) {
        mainHandler.removeCallbacks(operationTimeout)
        val operation = currentOperation ?: return
        currentOperation = null

        when (operation) {
            is GattOperation.Read -> operation.onResult(if (error == null) value else null, error)
            is GattOperation.Write -> operation.onResult(error)
            is GattOperation.WriteCccd -> if (error != null) {
                Log.e(TAG, "Ghi CCCD thất bại cho characteristic ${operation.characteristic.uuid} ($error)")
            }
        }
        processNextOperation()
    }

    private fun dispatchNotification(uuid: UUID, value: ByteArray) {
        // Tra callback trên main thread để notification tới sau khi unsubscribe/disconnect bị bỏ qua
        mainHandler.post { notificationCallbacks[uuid]?.invoke(value) }
    }

    @SuppressLint("MissingPermission")
    private fun writeDescriptorCompat(
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor,
        value: ByteArray
    ): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeDescriptor(descriptor, value) == BluetoothStatusCodes.SUCCESS
        } else {
            @Suppress("DEPRECATION")
            descriptor.value = value
            @Suppress("DEPRECATION")
            gatt.writeDescriptor(descriptor)
        }
    }

    @SuppressLint("MissingPermission")
    private fun writeCharacteristicCompat(gatt: BluetoothGatt, operation: GattOperation.Write): Boolean {
        val characteristic = operation.characteristic
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(characteristic, operation.data, operation.writeType) ==
                    BluetoothStatusCodes.SUCCESS
        } else {
            characteristic.writeType = operation.writeType
            @Suppress("DEPRECATION")
            characteristic.value = operation.data
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(characteristic)
        }
    }

    private fun BluetoothGattCharacteristic.supportsNotifyOrIndicate(): Boolean {
        val mask = BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_INDICATE
        return properties and mask != 0
    }

    private fun describeGattStatus(status: Int): String = when (status) {
        BluetoothGatt.GATT_SUCCESS -> "thành công (0)"
        8 -> "hết thời gian chờ - thiết bị tắt nguồn hoặc ngoài vùng phủ sóng (8)"
        19 -> "thiết bị ngoại vi chủ động ngắt kết nối (19)"
        22 -> "điện thoại chủ động ngắt kết nối (22)"
        133 -> "lỗi GATT chung (133)"
        else -> "status $status"
    }

    private fun findCharacteristic(uuidStr: String): BluetoothGattCharacteristic? {
        val gatt = bluetoothGatt ?: return null
        val targetUuid = try {
            UUID.fromString(uuidStr)
        } catch (_: Exception) {
            return null
        }

        for (service in gatt.services) {
            for (characteristic in service.characteristics) {
                if (characteristic.uuid == targetUuid) {
                    return characteristic
                }
            }
        }
        return null
    }
}
