@file:OptIn(
    kotlinx.cinterop.ExperimentalForeignApi::class,
    kotlinx.cinterop.BetaInteropApi::class
)

package com.phusy2001.nativedevicemonitordemo.nativecore

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
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.*
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceBatteryState
import platform.posix.memcpy
import kotlin.coroutines.resume

// ================= BATTERY =================
class IosBatteryProvider : BatteryProvider {
    init {
        UIDevice.currentDevice.batteryMonitoringEnabled = true
    }

    override fun getBatteryLevel(): Int {
        val level = UIDevice.currentDevice.batteryLevel
        return if (level < 0f) 100 else (level * 100).toInt().coerceIn(0, 100)
    }

    override fun isCharging(): Boolean {
        val state = UIDevice.currentDevice.batteryState
        return state == UIDeviceBatteryState.UIDeviceBatteryStateCharging ||
                state == UIDeviceBatteryState.UIDeviceBatteryStateFull
    }

    override fun getBatteryInfo(): BatteryInfo {
        return BatteryInfo(level = getBatteryLevel(), isCharging = isCharging())
    }
}

// ================= NETWORK =================
class IosNetworkProvider : NetworkProvider {
    // Trên iOS simulator / device mặc định có internet
    override fun isConnected(): Boolean = true
    override fun isWifiConnected(): Boolean = true
    override fun isCellularAvailable(): Boolean = false

    override fun getNetworkInfo(): NetworkInfo {
        return NetworkInfo(
            isConnected = isConnected(),
            isWifiConnected = isWifiConnected(),
            isCellularAvailable = isCellularAvailable()
        )
    }
}

// ================= BLUETOOTH PROVIDER =================
class IosBluetoothProvider(private val bluetoothManager: BluetoothManager? = null) : BluetoothProvider {
    override fun isBluetoothEnabled(): Boolean = bluetoothManager?.isEnabled() ?: true
    override fun isBluetoothConnected(): Boolean =
        bluetoothManager?.getConnectionState() == ConnectionState.CONNECTED

    override fun getBluetoothInfo(): BluetoothInfo {
        return BluetoothInfo(
            isEnabled = isBluetoothEnabled(),
            isConnected = isBluetoothConnected()
        )
    }
}

// ================= FILE LOGGER =================
class IosFileLogger(private val fileName: String = "monitor_logs.jsonl") : FileLogger {
    private val filePath: String
        get() {
            val fileManager = NSFileManager.defaultManager
            val urls = fileManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask)
            val docDir = (urls.firstOrNull() as? NSURL)?.path ?: ""
            return "$docDir/$fileName"
        }

    override fun append(snapshot: DeviceSnapshot) {
        appendRaw(snapshot.toJson())
    }

    override fun appendRaw(line: String) {
        val path = filePath
        val fileManager = NSFileManager.defaultManager
        val existing = if (fileManager.fileExistsAtPath(path)) {
            NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null) ?: ""
        } else {
            ""
        }
        val updated = if (existing.isEmpty()) "$line\n" else "$existing$line\n"
        val nsStr = NSString.create(string = updated)
        nsStr.writeToFile(path, true, NSUTF8StringEncoding, null)
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
        val path = filePath
        val fileManager = NSFileManager.defaultManager
        if (!fileManager.fileExistsAtPath(path)) return emptyList()
        val content = NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null) ?: return emptyList()
        return content.split("\n").filter { it.isNotBlank() }
    }

    override fun clear() {
        val path = filePath
        val emptyStr = NSString.create(string = "")
        emptyStr.writeToFile(path, true, NSUTF8StringEncoding, null)
    }

    override fun getFileSizeBytes(): Long {
        val path = filePath
        val fileManager = NSFileManager.defaultManager
        val attrs = fileManager.attributesOfItemAtPath(path, null) ?: return 0L
        return (attrs["NSFileSize"] as? Long) ?: 0L
    }

    override fun getLogCount(): Int = readRawLines().size
}

// ================= API CLIENT =================
class IosApiClient(
    private val timeoutIntervalSec: Double = 10.0
) : ApiClient {

    private val session: NSURLSession by lazy {
        val config = NSURLSessionConfiguration.defaultSessionConfiguration
        config.timeoutIntervalForRequest = timeoutIntervalSec
        config.timeoutIntervalForResource = timeoutIntervalSec
        NSURLSession.sessionWithConfiguration(config)
    }

    override suspend fun get(url: String, headers: Map<String, String>?): ApiResponse =
        executeRequest(url, "GET", null, headers)

    override suspend fun post(url: String, body: String, headers: Map<String, String>?): ApiResponse =
        executeRequest(url, "POST", body, headers)

    @OptIn(ExperimentalForeignApi::class)
    private suspend fun executeRequest(
        urlString: String,
        method: String,
        body: String?,
        headers: Map<String, String>?
    ): ApiResponse = suspendCancellableCoroutine { continuation ->
        val nsUrl = NSURL.URLWithString(urlString)
        if (nsUrl == null) {
            continuation.resume(
                ApiResponse(
                    statusCode = 0,
                    body = null,
                    isSuccessful = false,
                    errorMessage = "URL không hợp lệ: $urlString"
                )
            )
            return@suspendCancellableCoroutine
        }

        val request = NSMutableURLRequest.requestWithURL(nsUrl) as NSMutableURLRequest
        request.setHTTPMethod(method)

        headers?.forEach { (k, v) ->
            request.setValue(v, forHTTPHeaderField = k)
        }

        if (body != null) {
            val nsBody = NSString.create(string = body)
            val data = nsBody.dataUsingEncoding(NSUTF8StringEncoding)
            request.setHTTPBody(data)
            if (headers == null || !headers.keys.any { it.equals("Content-Type", ignoreCase = true) }) {
                request.setValue("application/json; charset=utf-8", forHTTPHeaderField = "Content-Type")
            }
        }

        val mark = kotlin.time.TimeSource.Monotonic.markNow()

        val task = session.dataTaskWithRequest(request) { data, response, error ->
            val durationMs = mark.elapsedNow().inWholeMilliseconds

            if (error != null) {
                val isTimeout = error.code.toLong() == -1001L // NSURLErrorTimedOut
                continuation.resume(
                    ApiResponse(
                        statusCode = 0,
                        body = null,
                        isSuccessful = false,
                        errorMessage = error.localizedDescription,
                        durationMs = durationMs,
                        isTimeout = isTimeout
                    )
                )
                return@dataTaskWithRequest
            }

            val httpResponse = response as? NSHTTPURLResponse
            val statusCode = httpResponse?.statusCode?.toInt() ?: 0
            val isSuccess = statusCode in 200..299

            val responseString = data?.let {
                NSString.create(data = it, encoding = NSUTF8StringEncoding)?.toString()
            }

            continuation.resume(
                ApiResponse(
                    statusCode = statusCode,
                    body = responseString,
                    isSuccessful = isSuccess,
                    durationMs = durationMs
                )
            )
        }

        task.resume()
    }
}

// ================= BLUETOOTH MANAGER (Mock / Wrapper for iOS) =================
class IosBluetoothManager : BluetoothManager {
    private var connectionState: ConnectionState = ConnectionState.DISCONNECTED
    private var stateChangeCallback: ((ConnectionState) -> Unit)? = null
    private val notificationCallbacks = mutableMapOf<String, (ByteArray) -> Unit>()

    override fun isAvailable(): Boolean = true
    override fun isEnabled(): Boolean = true

    override fun startScan(onDeviceFound: (BleDevice) -> Unit, onError: (String) -> Unit) {
        // Sample mock device for testing iOS BLE flow if hardware CentralManager is unavailable in Simulator
        val mockDevice = BleDevice(
            id = "00:11:22:33:44:55",
            name = "CarMD BT Tool",
            rssi = -65,
            isConnectable = true
        )
        onDeviceFound(mockDevice)
    }

    override fun stopScan() {}

    override fun connect(deviceAddress: String, onStateChange: (ConnectionState) -> Unit) {
        stateChangeCallback = onStateChange
        connectionState = ConnectionState.CONNECTED
        onStateChange(ConnectionState.CONNECTED)
    }

    override fun disconnect() {
        connectionState = ConnectionState.DISCONNECTED
        stateChangeCallback?.invoke(ConnectionState.DISCONNECTED)
    }

    override fun discoverServices(onSuccess: (List<BleService>) -> Unit, onError: (String) -> Unit) {
        val sampleServices = listOf(
            BleService(
                uuid = "0000fff0-0000-1000-8000-00805f9b34fb",
                characteristics = listOf(
                    "0000fff1-0000-1000-8000-00805f9b34fb",
                    "0000fff2-0000-1000-8000-00805f9b34fb"
                ),
                readableCharacteristics = listOf("0000fff1-0000-1000-8000-00805f9b34fb"),
                notifiableCharacteristics = listOf("0000fff1-0000-1000-8000-00805f9b34fb")
            )
        )
        onSuccess(sampleServices)
    }

    override fun read(characteristicUuid: String, onResult: (ByteArray?, String?) -> Unit) {
        onResult("Sample BLE Value".encodeToByteArray(), null)
    }

    override fun write(characteristicUuid: String, data: ByteArray, onResult: (String?) -> Unit) {
        onResult(null)
    }

    override fun subscribe(characteristicUuid: String, onNotification: (ByteArray) -> Unit) {
        notificationCallbacks[characteristicUuid.lowercase()] = onNotification
    }

    override fun unsubscribe(characteristicUuid: String) {
        notificationCallbacks.remove(characteristicUuid.lowercase())
    }

    override fun getConnectionState(): ConnectionState = connectionState
}

// ================= BACKGROUND MONITOR =================
class IosBackgroundMonitor(
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
        val formatter = NSISO8601DateFormatter()
        val timestamp = formatter.stringFromDate(NSDate())
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

// ================= NATIVE CORE CONTAINER FOR IOS =================
class IosNativeCore : NativeCore {
    override val batteryProvider: BatteryProvider = IosBatteryProvider()
    override val networkProvider: NetworkProvider = IosNetworkProvider()
    override val bluetoothManager: BluetoothManager = IosBluetoothManager()
    override val bluetoothProvider: BluetoothProvider = IosBluetoothProvider(bluetoothManager)
    override val fileLogger: FileLogger = IosFileLogger()
    override val apiClient: ApiClient = IosApiClient()
    override val backgroundMonitor: BackgroundMonitor = IosBackgroundMonitor(
        batteryProvider,
        networkProvider,
        bluetoothProvider,
        fileLogger
    )

    companion object {
        val shared: IosNativeCore by lazy { IosNativeCore() }
    }
}
