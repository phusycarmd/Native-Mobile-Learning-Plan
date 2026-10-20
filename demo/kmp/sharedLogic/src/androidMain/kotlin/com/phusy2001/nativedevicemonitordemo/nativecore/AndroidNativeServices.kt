package com.phusy2001.nativedevicemonitordemo.nativecore

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import com.phusy2001.nativedevicemonitordemo.nativecore.background.BackgroundMonitor
import com.phusy2001.nativedevicemonitordemo.nativecore.bluetooth.BleDevice
import com.phusy2001.nativedevicemonitordemo.nativecore.bluetooth.BleService
import com.phusy2001.nativedevicemonitordemo.nativecore.bluetooth.BluetoothManager
import com.phusy2001.nativedevicemonitordemo.nativecore.bluetooth.ConnectionState
import com.phusy2001.nativedevicemonitordemo.nativecore.device.BatteryInfo
import com.phusy2001.nativedevicemonitordemo.nativecore.device.BatteryProvider
import com.phusy2001.nativedevicemonitordemo.nativecore.device.BluetoothInfo
import com.phusy2001.nativedevicemonitordemo.nativecore.device.BluetoothProvider
import com.phusy2001.nativedevicemonitordemo.nativecore.device.DeviceSnapshot
import com.phusy2001.nativedevicemonitordemo.nativecore.device.NetworkInfo
import com.phusy2001.nativedevicemonitordemo.nativecore.device.NetworkProvider
import com.phusy2001.nativedevicemonitordemo.nativecore.logging.FileLogger
import com.phusy2001.nativedevicemonitordemo.nativecore.network.ApiClient
import com.phusy2001.nativedevicemonitordemo.nativecore.network.ApiResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

// ================= BATTERY =================
class AndroidBatteryProvider(private val context: Context) : BatteryProvider {
    override fun getBatteryLevel(): Int {
        val status = getBatteryIntent()
        val level = status?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = status?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) {
            (level * 100 / scale.toFloat()).toInt().coerceIn(0, 100)
        } else {
            100
        }
    }

    override fun isCharging(): Boolean {
        val status = getBatteryIntent()
        val statusVal = status?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return statusVal == BatteryManager.BATTERY_STATUS_CHARGING ||
                statusVal == BatteryManager.BATTERY_STATUS_FULL
    }

    override fun getBatteryInfo(): BatteryInfo {
        return BatteryInfo(level = getBatteryLevel(), isCharging = isCharging())
    }

    private fun getBatteryIntent(): Intent? {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        return context.registerReceiver(null, filter)
    }
}

// ================= NETWORK =================
class AndroidNetworkProvider(private val context: Context) : NetworkProvider {
    private val cm: ConnectivityManager? by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    }

    override fun isConnected(): Boolean {
        val caps = getCapabilities() ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun isWifiConnected(): Boolean {
        val caps = getCapabilities() ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    override fun isCellularAvailable(): Boolean {
        val caps = getCapabilities() ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    override fun getNetworkInfo(): NetworkInfo {
        return NetworkInfo(
            isConnected = isConnected(),
            isWifiConnected = isWifiConnected(),
            isCellularAvailable = isCellularAvailable()
        )
    }

    private fun getCapabilities(): NetworkCapabilities? {
        val active = cm?.activeNetwork ?: return null
        return cm?.getNetworkCapabilities(active)
    }
}

// ================= BLUETOOTH PROVIDER =================
class AndroidBluetoothProvider(
    private val context: Context,
    private val bluetoothManager: BluetoothManager? = null
) : BluetoothProvider {
    private val adapter: BluetoothAdapter? by lazy {
        val sysManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
        sysManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
    }

    override fun isBluetoothEnabled(): Boolean {
        return bluetoothManager?.isEnabled() ?: (adapter?.isEnabled == true)
    }

    override fun isBluetoothConnected(): Boolean {
        return bluetoothManager?.getConnectionState() == ConnectionState.CONNECTED
    }

    override fun getBluetoothInfo(): BluetoothInfo {
        return BluetoothInfo(
            isEnabled = isBluetoothEnabled(),
            isConnected = isBluetoothConnected()
        )
    }
}

// ================= FILE LOGGER =================
class AndroidFileLogger(
    private val context: Context,
    private val fileName: String = "monitor_logs.jsonl"
) : FileLogger {
    private val lock = Any()
    private val logFile: File
        get() = File(context.filesDir, fileName)

    override fun append(snapshot: DeviceSnapshot) {
        appendRaw(snapshot.toJson())
    }

    override fun appendRaw(line: String) {
        synchronized(lock) {
            val file = logFile
            if (!file.exists()) {
                file.parentFile?.mkdirs()
                file.createNewFile()
            }
            file.appendText("$line\n", Charsets.UTF_8)
        }
    }

    override fun readAll(): List<DeviceSnapshot> {
        val lines = readRawLines()
        val result = mutableListOf<DeviceSnapshot>()
        for (line in lines) {
            val snapshot = DeviceSnapshot.fromJson(line)
            if (snapshot != null) {
                result.add(snapshot)
            }
        }
        return result
    }

    override fun readRawLines(): List<String> {
        synchronized(lock) {
            val file = logFile
            if (!file.exists()) return emptyList()
            return file.readLines(Charsets.UTF_8).filter { it.isNotBlank() }
        }
    }

    override fun clear() {
        synchronized(lock) {
            val file = logFile
            if (file.exists()) {
                file.writeText("", Charsets.UTF_8)
            }
        }
    }

    override fun getFileSizeBytes(): Long {
        synchronized(lock) {
            val file = logFile
            return if (file.exists()) file.length() else 0L
        }
    }

    override fun getLogCount(): Int {
        return readRawLines().size
    }
}

// ================= API CLIENT =================
class AndroidApiClient(
    private val connectTimeoutMs: Int = 10_000,
    private val readTimeoutMs: Int = 10_000
) : ApiClient {

    override suspend fun get(url: String, headers: Map<String, String>?): ApiResponse =
        withContext(Dispatchers.IO) {
            executeRequest(url, "GET", null, headers)
        }

    override suspend fun post(url: String, body: String, headers: Map<String, String>?): ApiResponse =
        withContext(Dispatchers.IO) {
            executeRequest(url, "POST", body, headers)
        }

    private fun executeRequest(
        urlString: String,
        method: String,
        body: String?,
        headers: Map<String, String>?
    ): ApiResponse {
        val start = System.currentTimeMillis()
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = method
            conn.connectTimeout = connectTimeoutMs
            conn.readTimeout = readTimeoutMs
            conn.instanceFollowRedirects = true

            headers?.forEach { (k, v) -> conn.setRequestProperty(k, v) }

            if (body != null) {
                conn.doOutput = true
                if (headers == null || !headers.keys.any { it.equals("Content-Type", ignoreCase = true) }) {
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(body)
                    writer.flush()
                }
            }

            val statusCode = conn.responseCode
            val isSuccess = statusCode in 200..299
            val stream = if (isSuccess) conn.inputStream else conn.errorStream
            val responseText = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }

            val duration = System.currentTimeMillis() - start
            ApiResponse(
                statusCode = statusCode,
                body = responseText,
                isSuccessful = isSuccess,
                durationMs = duration
            )
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - start
            val isTimeout = e is java.net.SocketTimeoutException
            ApiResponse(
                statusCode = 0,
                body = null,
                isSuccessful = false,
                errorMessage = e.message ?: "Network error",
                durationMs = duration,
                isTimeout = isTimeout
            )
        } finally {
            conn?.disconnect()
        }
    }
}

// ================= BLUETOOTH MANAGER =================
@SuppressLint("MissingPermission")
class AndroidBluetoothManager(private val context: Context) : BluetoothManager {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val sysManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
    private val adapter: BluetoothAdapter? = sysManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()

    private var connectionState: ConnectionState = ConnectionState.DISCONNECTED
    private var stateChangeCallback: ((ConnectionState) -> Unit)? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var scanCallback: ScanCallback? = null
    private val notificationCallbacks = mutableMapOf<String, (ByteArray) -> Unit>()

    override fun isAvailable(): Boolean = adapter != null

    override fun isEnabled(): Boolean = adapter?.isEnabled == true

    override fun startScan(onDeviceFound: (BleDevice) -> Unit, onError: (String) -> Unit) {
        val scanner = adapter?.bluetoothLeScanner
        if (scanner == null || !isEnabled()) {
            onError("Bluetooth chưa được bật hoặc không khả dụng.")
            return
        }

        stopScan()

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result?.let {
                    val device = it.device
                    val name = device.name ?: it.scanRecord?.deviceName ?: "Unknown device"
                    val item = BleDevice(
                        id = device.address,
                        name = name,
                        rssi = it.rssi,
                        isConnectable = it.isConnectable
                    )
                    mainHandler.post { onDeviceFound(item) }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                mainHandler.post { onError("Scan thất bại với mã lỗi: $errorCode") }
            }
        }

        try {
            scanner.startScan(scanCallback)
        } catch (e: Exception) {
            onError("Không thể quét BLE: ${e.message}")
        }
    }

    override fun stopScan() {
        scanCallback?.let {
            try {
                adapter?.bluetoothLeScanner?.stopScan(it)
            } catch (_: Exception) {}
            scanCallback = null
        }
    }

    override fun connect(deviceAddress: String, onStateChange: (ConnectionState) -> Unit) {
        stateChangeCallback = onStateChange
        updateState(ConnectionState.CONNECTING)

        val device = try {
            adapter?.getRemoteDevice(deviceAddress)
        } catch (e: Exception) {
            updateState(ConnectionState.DISCONNECTED)
            return
        }

        if (device == null) {
            updateState(ConnectionState.DISCONNECTED)
            return
        }

        bluetoothGatt = device.connectGatt(context, false, gattCallback, android.bluetooth.BluetoothDevice.TRANSPORT_LE)
    }

    override fun disconnect() {
        updateState(ConnectionState.DISCONNECTING)
        bluetoothGatt?.disconnect()
    }

    override fun discoverServices(onSuccess: (List<BleService>) -> Unit, onError: (String) -> Unit) {
        val gatt = bluetoothGatt
        if (gatt == null || connectionState != ConnectionState.CONNECTED) {
            onError("Chưa kết nối tới thiết bị.")
            return
        }
        discoverSuccess = onSuccess
        discoverError = onError
        gatt.discoverServices()
    }

    private var discoverSuccess: ((List<BleService>) -> Unit)? = null
    private var discoverError: ((String) -> Unit)? = null

    override fun read(characteristicUuid: String, onResult: (ByteArray?, String?) -> Unit) {
        val char = findCharacteristic(characteristicUuid)
        if (char == null) {
            onResult(null, "Không tìm thấy Characteristic.")
            return
        }
        readCallbacks[characteristicUuid.lowercase()] = onResult
        bluetoothGatt?.readCharacteristic(char)
    }

    private val readCallbacks = mutableMapOf<String, (ByteArray?, String?) -> Unit>()

    override fun write(characteristicUuid: String, data: ByteArray, onResult: (String?) -> Unit) {
        val char = findCharacteristic(characteristicUuid)
        if (char == null) {
            onResult("Không tìm thấy Characteristic.")
            return
        }
        writeCallbacks[characteristicUuid.lowercase()] = onResult
        char.value = data
        val success = bluetoothGatt?.writeCharacteristic(char) ?: false
        if (!success) {
            writeCallbacks.remove(characteristicUuid.lowercase())
            onResult("Gửi lệnh ghi thất bại.")
        }
    }

    private val writeCallbacks = mutableMapOf<String, (String?) -> Unit>()

    override fun subscribe(characteristicUuid: String, onNotification: (ByteArray) -> Unit) {
        val char = findCharacteristic(characteristicUuid) ?: return
        val gatt = bluetoothGatt ?: return
        notificationCallbacks[characteristicUuid.lowercase()] = onNotification

        gatt.setCharacteristicNotification(char, true)
        val cccd = char.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
        if (cccd != null) {
            cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(cccd)
        }
    }

    override fun unsubscribe(characteristicUuid: String) {
        notificationCallbacks.remove(characteristicUuid.lowercase())
        val char = findCharacteristic(characteristicUuid) ?: return
        bluetoothGatt?.setCharacteristicNotification(char, false)
    }

    override fun getConnectionState(): ConnectionState = connectionState

    private fun updateState(newState: ConnectionState) {
        connectionState = newState
        mainHandler.post { stateChangeCallback?.invoke(newState) }
    }

    private fun findCharacteristic(uuidStr: String): BluetoothGattCharacteristic? {
        val gatt = bluetoothGatt ?: return null
        val target = uuidStr.lowercase()
        for (service in gatt.services) {
            for (char in service.characteristics) {
                if (char.uuid.toString().lowercase() == target) return char
            }
        }
        return null
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                updateState(ConnectionState.CONNECTED)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                updateState(ConnectionState.DISCONNECTED)
                bluetoothGatt?.close()
                bluetoothGatt = null
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && gatt != null) {
                val list = gatt.services.map { service ->
                    val chars = service.characteristics.map { it.uuid.toString() }
                    val readable = service.characteristics
                        .filter { (it.properties and BluetoothGattCharacteristic.PROPERTY_READ) != 0 }
                        .map { it.uuid.toString() }
                    val notifiable = service.characteristics
                        .filter { (it.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0 }
                        .map { it.uuid.toString() }
                    BleService(service.uuid.toString(), chars, readable, notifiable)
                }
                mainHandler.post { discoverSuccess?.invoke(list) }
            } else {
                mainHandler.post { discoverError?.invoke("Khám phá dịch vụ thất bại (status=$status).") }
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            val uuid = characteristic?.uuid?.toString()?.lowercase() ?: return
            val callback = readCallbacks.remove(uuid) ?: return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val data = characteristic.value
                mainHandler.post { callback(data, null) }
            } else {
                mainHandler.post { callback(null, "Lỗi đọc characteristic (status=$status).") }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            val uuid = characteristic?.uuid?.toString()?.lowercase() ?: return
            val callback = writeCallbacks.remove(uuid) ?: return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mainHandler.post { callback(null) }
            } else {
                mainHandler.post { callback("Ghi characteristic thất bại: $status") }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            val uuid = characteristic?.uuid?.toString()?.lowercase() ?: return
            val data = characteristic.value ?: return
            val callback = notificationCallbacks[uuid] ?: return
            mainHandler.post { callback(data) }
        }
    }
}

// ================= BACKGROUND MONITOR =================
class AndroidBackgroundMonitor(
    private val batteryProvider: BatteryProvider,
    private val networkProvider: NetworkProvider,
    private val bluetoothProvider: BluetoothProvider,
    private val fileLogger: FileLogger
) : BackgroundMonitor {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var monitorJob: Job? = null

    private val _isMonitoring = MutableStateFlow(false)
    override val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    private val _latestSnapshot = MutableStateFlow<DeviceSnapshot?>(null)
    override val latestSnapshot: StateFlow<DeviceSnapshot?> = _latestSnapshot.asStateFlow()

    private val _logCount = MutableStateFlow(fileLogger.getLogCount())
    override val logCount: StateFlow<Int> = _logCount.asStateFlow()

    override fun startMonitoring(intervalSeconds: Long) {
        if (_isMonitoring.value) return
        _isMonitoring.value = true

        monitorJob = scope.launch {
            while (isActive) {
                val snapshot = captureSnapshot()
                fileLogger.append(snapshot)
                _latestSnapshot.value = snapshot
                _logCount.value = fileLogger.getLogCount()
                delay(intervalSeconds * 1000L)
            }
        }
    }

    override fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
        _isMonitoring.value = false
    }

    override fun getLatestSnapshot(): DeviceSnapshot? = _latestSnapshot.value

    override fun readLogEntries(): List<DeviceSnapshot> = fileLogger.readAll()

    override fun triggerManualSnapshot(): DeviceSnapshot {
        val snapshot = captureSnapshot()
        fileLogger.append(snapshot)
        _latestSnapshot.value = snapshot
        _logCount.value = fileLogger.getLogCount()
        return snapshot
    }

    override fun clearLogs(): Boolean {
        fileLogger.clear()
        _logCount.value = 0
        return true
    }

    private fun captureSnapshot(): DeviceSnapshot {
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val timestamp = isoFormat.format(Date())
        val battery = batteryProvider.getBatteryLevel()
        val wifi = networkProvider.isWifiConnected()
        val cellular = networkProvider.isCellularAvailable()
        val bleEnabled = bluetoothProvider.isBluetoothEnabled()
        val bleConnected = bluetoothProvider.isBluetoothConnected()

        return DeviceSnapshot(
            timestamp = timestamp,
            battery = battery,
            wifiConnected = wifi,
            cellularAvailable = cellular,
            bluetoothEnabled = bleEnabled,
            bluetoothConnected = bleConnected
        )
    }
}

// ================= NATIVE CORE CONTAINER =================
class AndroidNativeCore(context: Context) : NativeCore {
    override val batteryProvider: BatteryProvider = AndroidBatteryProvider(context)
    override val networkProvider: NetworkProvider = AndroidNetworkProvider(context)
    override val bluetoothManager: BluetoothManager = AndroidBluetoothManager(context)
    override val bluetoothProvider: BluetoothProvider = AndroidBluetoothProvider(context, bluetoothManager)
    override val fileLogger: FileLogger = AndroidFileLogger(context)
    override val apiClient: ApiClient = AndroidApiClient()
    override val backgroundMonitor: BackgroundMonitor = AndroidBackgroundMonitor(
        batteryProvider,
        networkProvider,
        bluetoothProvider,
        fileLogger
    )
}
