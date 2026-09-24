<template>
  <div class="simulator-wrapper">
    <div class="simulator-phone">
      <!-- Phone Top Notch / Speaker -->
      <div class="phone-speaker"></div>
      
      <!-- App Header -->
      <div class="phone-header">
        <div class="status-bar">
          <span class="time">09:41</span>
          <div class="status-icons">
            <span :class="['icon-net', { active: wifiOn }]">📶</span>
            <span class="icon-bat">🔋 {{ batteryLevel }}%</span>
          </div>
        </div>
        <div class="app-title-bar">
          <span class="app-brand">📱 Device Monitor Demo</span>
          <span class="platform-indicator">{{ currentPlatform }}</span>
        </div>
      </div>

      <!-- App Tabs -->
      <div class="phone-tabs">
        <button 
          :class="['tab-btn', { active: activeTab === 'ble' }]"
          @click="activeTab = 'ble'"
        >
          📡 Bluetooth
        </button>
        <button 
          :class="['tab-btn', { active: activeTab === 'bg' }]"
          @click="activeTab = 'bg'"
        >
          🔋 Monitor
        </button>
        <button 
          :class="['tab-btn', { active: activeTab === 'api' }]"
          @click="activeTab = 'api'"
        >
          🌐 HTTP API
        </button>
      </div>

      <!-- Tab 1: Bluetooth BLE Simulator -->
      <div v-if="activeTab === 'ble'" class="tab-content">
        <div class="ble-controls">
          <button 
            v-if="!isScanning && bleState === 'idle'" 
            @click="startScan" 
            class="sim-btn btn-primary"
          >
            🔍 Bắt đầu quét BLE
          </button>
          <button 
            v-else-if="isScanning" 
            @click="stopScan" 
            class="sim-btn btn-warning"
          >
            ⏹️ Dừng quét (Scanning...)
          </button>
          <button 
            v-if="bleState === 'connected'" 
            @click="disconnectBle" 
            class="sim-btn btn-danger"
          >
            🔌 Ngắt kết nối
          </button>
          <button 
            v-if="bleState === 'connected'" 
            @click="simulateDrop" 
            class="sim-btn btn-outline"
            title="Thử nghiệm ngắt nguồn thiết bị đột ngột"
          >
            ⚡ Test rớt kết nối
          </button>
        </div>

        <!-- Connection Status Pill -->
        <div class="status-pill" :class="'status-' + bleState">
          Trạng thái: <strong>{{ statusText }}</strong>
        </div>

        <!-- Discovered Devices List -->
        <div v-if="bleState === 'idle' || isScanning" class="section-box">
          <div class="box-title">Thiết bị tìm thấy ({{ discoveredDevices.length }})</div>
          <div v-if="discoveredDevices.length === 0" class="empty-hint">
            {{ isScanning ? 'Đang dò sóng thiết bị BLE...' : 'Nhấn "Bắt đầu quét BLE" để tìm thiết bị' }}
          </div>
          <div v-for="dev in discoveredDevices" :key="dev.id" class="device-row">
            <div class="dev-info">
              <span class="dev-name">{{ dev.name }}</span>
              <span class="dev-rssi">{{ dev.rssi }} dBm</span>
            </div>
            <button @click="connectDevice(dev)" class="btn-connect">Kết nối</button>
          </div>
        </div>

        <!-- Connected GATT Services & Live Data -->
        <div v-if="bleState === 'connected'" class="section-box connected-view">
          <div class="box-title">Đã kết nối: {{ connectedDevice?.name }}</div>
          
          <div class="gatt-actions">
            <button 
              @click="toggleNotify" 
              class="sim-btn" 
              :class="isNotifying ? 'btn-danger' : 'btn-success'"
            >
              {{ isNotifying ? '⏸️ Dừng Notify' : '▶️ Subscribe Notify' }}
            </button>
            <button @click="readCharacteristic" class="sim-btn btn-secondary">
              📖 Đọc Characteristic
            </button>
          </div>

          <!-- Live Stream Box -->
          <div class="live-stream-box">
            <div class="stream-header">Live Notification Stream (0x2A37):</div>
            <div class="stream-log" ref="streamLogRef">
              <div v-for="(log, idx) in streamLogs" :key="idx" class="log-entry">
                <span class="log-time">{{ log.time }}</span>
                <span class="log-hex">{{ log.hex }}</span>
                <span class="log-val">➔ {{ log.val }}</span>
              </div>
              <div v-if="streamLogs.length === 0" class="empty-hint">
                Chưa có dữ liệu. Bấm "Subscribe Notify" để nhận stream.
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Tab 2: Background Monitor & JSONL -->
      <div v-if="activeTab === 'bg'" class="tab-content">
        <div class="monitor-controls">
          <button 
            @click="toggleMonitor" 
            class="sim-btn"
            :class="isMonitoring ? 'btn-danger' : 'btn-primary'"
          >
            {{ isMonitoring ? '⏹️ Dừng giám sát' : '▶️ Bật Background Monitor (3s/lần)' }}
          </button>
          <button @click="clearJsonlLogs" class="sim-btn btn-outline">
            🗑️ Xóa file log
          </button>
        </div>

        <!-- Simulated Controls -->
        <div class="sensor-sliders">
          <div class="slider-group">
            <label>Phần trăm Pin: <strong>{{ batteryLevel }}%</strong></label>
            <input type="range" min="1" max="100" v-model.number="batteryLevel" />
          </div>
          <div class="toggle-group">
            <label>Wi-Fi State:</label>
            <button @click="wifiOn = !wifiOn" :class="['toggle-pill', { on: wifiOn }]">
              {{ wifiOn ? 'Đã kết nối' : 'Đã ngắt Wi-Fi' }}
            </button>
          </div>
        </div>

        <!-- JSONL File Viewer -->
        <div class="section-box jsonl-view">
          <div class="box-title">Nội dung file: <code>monitor_logs.jsonl</code> ({{ jsonlLogs.length }} dòng)</div>
          <div class="jsonl-code-area">
            <div v-for="(line, idx) in jsonlLogs" :key="idx" class="jsonl-line">
              {{ line }}
            </div>
            <div v-if="jsonlLogs.length === 0" class="empty-hint">
              Chưa có bản ghi nào. Bật monitor để tự động ghi log!
            </div>
          </div>
        </div>
      </div>

      <!-- Tab 3: HTTP API Tester -->
      <div v-if="activeTab === 'api'" class="tab-content">
        <div class="api-form">
          <div class="input-label">Endpoint URL:</div>
          <input type="text" class="sim-input" v-model="apiUrl" />
          <div class="api-actions">
            <button @click="sendApiGet" class="sim-btn btn-primary" :disabled="apiLoading">
              {{ apiLoading ? 'Đang gửi...' : 'Gửi GET' }}
            </button>
            <button @click="sendApiPost" class="sim-btn btn-secondary" :disabled="apiLoading">
              Gửi POST JSON
            </button>
          </div>
        </div>

        <div v-if="apiResponse" class="section-box api-result">
          <div class="box-title">
            Status: <span :class="apiResponse.status === 200 ? 'status-ok' : 'status-err'">{{ apiResponse.status }} OK</span>
            <span class="api-time">{{ apiResponse.timeMs }}ms</span>
          </div>
          <pre class="json-code">{{ apiResponse.body }}</pre>
        </div>
      </div>

      <!-- Phone Bottom Bar -->
      <div class="phone-home-indicator"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onUnmounted } from 'vue'

const activeTab = ref<'ble' | 'bg' | 'api'>('ble')
const currentPlatform = ref('Native Core')

// BLE State
const bleState = ref<'idle' | 'scanning' | 'connected' | 'error'>('idle')
const isScanning = ref(false)
const isNotifying = ref(false)
let scanTimer: any = null
let notifyTimer: any = null

interface Device {
  id: string
  name: string
  rssi: number
}

const discoveredDevices = ref<Device[]>([])
const connectedDevice = ref<Device | null>(null)

interface LogEntry {
  time: string
  hex: string
  val: string
}
const streamLogs = ref<LogEntry[]>([])

const statusText = computed(() => {
  if (isScanning.value) return 'Đang quét sóng LE...'
  if (bleState.value === 'connected') return `Đã kết nối với ${connectedDevice.value?.name}`
  if (bleState.value === 'error') return 'Mất kết nối đột ngột (Dropped)'
  return 'Chưa kết nối (Idle)'
})

function startScan() {
  discoveredDevices.value = []
  isScanning.value = true
  bleState.value = 'scanning'
  
  setTimeout(() => {
    discoveredDevices.value.push({ id: '1', name: 'ESP32_TempSensor', rssi: -64 })
  }, 400)
  setTimeout(() => {
    discoveredDevices.value.push({ id: '2', name: 'OBD2_AutoScanner_BT', rssi: -78 })
  }, 900)
  setTimeout(() => {
    discoveredDevices.value.push({ id: '3', name: 'Polar_H10_HeartRate', rssi: -89 })
  }, 1400)
}

function stopScan() {
  isScanning.value = false
  if (bleState.value === 'scanning') bleState.value = 'idle'
}

function connectDevice(dev: Device) {
  stopScan()
  connectedDevice.value = dev
  bleState.value = 'connected'
  streamLogs.value = []
}

function disconnectBle() {
  stopNotify()
  bleState.value = 'idle'
  connectedDevice.value = null
}

function simulateDrop() {
  stopNotify()
  bleState.value = 'error'
  connectedDevice.value = null
}

function toggleNotify() {
  if (isNotifying.value) {
    stopNotify()
  } else {
    isNotifying.value = true
    notifyTimer = setInterval(() => {
      const temp = (27 + Math.random() * 3).toFixed(1)
      const hex1 = '0x41'
      const hex2 = '0x02'
      const hex3 = '0x' + Math.floor(Math.random() * 255).toString(16).toUpperCase().padStart(2, '0')
      streamLogs.value.unshift({
        time: new Date().toLocaleTimeString(),
        hex: `${hex1} ${hex2} ${hex3}`,
        val: `Nhiệt độ: ${temp}°C`
      })
      if (streamLogs.value.length > 20) streamLogs.value.pop()
    }, 1200)
  }
}

function stopNotify() {
  isNotifying.value = false
  if (notifyTimer) {
    clearInterval(notifyTimer)
    notifyTimer = null
  }
}

function readCharacteristic() {
  alert(`Đọc Characteristic 0x2A19 (Battery Level): 94% [Raw byte: 0x5E]`)
}

// Background Monitor State
const batteryLevel = ref(85)
const wifiOn = ref(true)
const isMonitoring = ref(false)
const jsonlLogs = ref<string[]>([])
let monitorTimer: any = null

function toggleMonitor() {
  if (isMonitoring.value) {
    isMonitoring.value = false
    if (monitorTimer) clearInterval(monitorTimer)
  } else {
    isMonitoring.value = true
    addMonitorTick()
    monitorTimer = setInterval(addMonitorTick, 3000)
  }
}

function addMonitorTick() {
  const line = JSON.stringify({
    timestamp: new Date().toISOString(),
    battery: batteryLevel.value,
    wifiConnected: wifiOn.value,
    cellularAvailable: true,
    bluetoothEnabled: true,
    bluetoothConnected: bleState.value === 'connected'
  })
  jsonlLogs.value.unshift(line)
  if (jsonlLogs.value.length > 15) jsonlLogs.value.pop()
}

function clearJsonlLogs() {
  jsonlLogs.value = []
}

// API Tester State
const apiUrl = ref('https://jsonplaceholder.typicode.com/posts/1')
const apiLoading = ref(false)
const apiResponse = ref<{ status: number; timeMs: number; body: string } | null>(null)

async function sendApiGet() {
  apiLoading.value = true
  const start = performance.now()
  try {
    const res = await fetch(apiUrl.value)
    const json = await res.json()
    const elapsed = Math.round(performance.now() - start)
    apiResponse.value = {
      status: res.status,
      timeMs: elapsed,
      body: JSON.stringify(json, null, 2)
    }
  } catch (err: any) {
    apiResponse.value = {
      status: 500,
      timeMs: 0,
      body: JSON.stringify({ error: err.message }, null, 2)
    }
  } finally {
    apiLoading.value = false
  }
}

function sendApiPost() {
  apiLoading.value = true
  setTimeout(() => {
    apiResponse.value = {
      status: 201,
      timeMs: 165,
      body: JSON.stringify({
        id: 101,
        title: "Test Native Device Post",
        body: "Sample payload from NativeCore ApiClient",
        userId: 1
      }, null, 2)
    }
    apiLoading.value = false
  }, 350)
}

onUnmounted(() => {
  stopNotify()
  if (monitorTimer) clearInterval(monitorTimer)
})
</script>

<style scoped>
.simulator-wrapper {
  display: flex;
  justify-content: center;
  margin: 30px 0;
}

.simulator-phone {
  width: 100%;
  max-width: 420px;
  background: #09090b;
  color: #f4f4f5;
  border-radius: 36px;
  border: 10px solid #27272a;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.35);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  user-select: none;
}

.phone-speaker {
  width: 70px;
  height: 5px;
  background: #3f3f46;
  border-radius: 9999px;
  margin: 10px auto 4px auto;
}

.phone-header {
  padding: 4px 16px 8px 16px;
}

.status-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.75rem;
  font-weight: 700;
  color: #a1a1aa;
}

.status-icons {
  display: flex;
  gap: 8px;
  font-size: 0.72rem;
}

.app-title-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 6px;
}

.app-brand {
  font-size: 0.95rem;
  font-weight: 800;
  color: #ffffff;
}

.platform-indicator {
  font-size: 0.68rem;
  background: #27272a;
  color: #a1a1aa;
  padding: 2px 6px;
  border-radius: 4px;
}

.phone-tabs {
  display: flex;
  background: #18181b;
  border-bottom: 1px solid #27272a;
}

.tab-btn {
  flex: 1;
  background: none;
  border: none;
  color: #71717a;
  padding: 10px 4px;
  font-size: 0.78rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  border-bottom: 2px solid transparent;
}

.tab-btn.active {
  color: #6366f1;
  border-bottom-color: #6366f1;
  background: rgba(99, 102, 241, 0.08);
}

.tab-content {
  padding: 14px;
  min-height: 380px;
  max-height: 480px;
  overflow-y: auto;
}

.sim-btn {
  padding: 8px 12px;
  font-size: 0.8rem;
  font-weight: 700;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  transition: all 0.15s;
}

.sim-btn:hover {
  filter: brightness(1.15);
}

.btn-primary { background: #6366f1; color: white; }
.btn-success { background: #10b981; color: white; }
.btn-warning { background: #f59e0b; color: #18181b; }
.btn-danger { background: #ef4444; color: white; }
.btn-secondary { background: #3f3f46; color: white; }
.btn-outline { background: transparent; border: 1px solid #52525b; color: #d4d4d8; }

.ble-controls, .gatt-actions, .monitor-controls {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.status-pill {
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 0.78rem;
  margin-bottom: 12px;
  background: #18181b;
  border: 1px solid #27272a;
}

.status-connected { border-color: #10b981; color: #10b981; background: rgba(16, 185, 129, 0.1); }
.status-scanning { border-color: #f59e0b; color: #f59e0b; background: rgba(245, 158, 11, 0.1); }
.status-error { border-color: #ef4444; color: #ef4444; background: rgba(239, 68, 68, 0.1); }

.section-box {
  background: #18181b;
  border: 1px solid #27272a;
  border-radius: 10px;
  padding: 10px;
  margin-bottom: 12px;
}

.box-title {
  font-size: 0.82rem;
  font-weight: 700;
  color: #a1a1aa;
  margin-bottom: 8px;
  display: flex;
  justify-content: space-between;
}

.device-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #27272a;
}

.device-row:last-child { border-bottom: none; }

.dev-info {
  display: flex;
  flex-direction: column;
}

.dev-name {
  font-size: 0.85rem;
  font-weight: 700;
  color: #f4f4f5;
}

.dev-rssi {
  font-size: 0.72rem;
  color: #10b981;
}

.btn-connect {
  background: #6366f1;
  color: white;
  border: none;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 700;
  cursor: pointer;
}

.live-stream-box {
  background: #09090b;
  border-radius: 8px;
  padding: 8px;
  border: 1px solid #27272a;
  margin-top: 10px;
}

.stream-header {
  font-size: 0.75rem;
  font-weight: 700;
  color: #6366f1;
  margin-bottom: 6px;
}

.stream-log {
  max-height: 120px;
  overflow-y: auto;
  font-family: monospace;
  font-size: 0.72rem;
}

.log-entry {
  display: flex;
  gap: 8px;
  padding: 2px 0;
  border-bottom: 1px solid #18181b;
}

.log-time { color: #71717a; }
.log-hex { color: #f59e0b; }
.log-val { color: #10b981; font-weight: 700; }

.sensor-sliders {
  background: #18181b;
  border: 1px solid #27272a;
  border-radius: 10px;
  padding: 10px;
  margin-bottom: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.slider-group label, .toggle-group label {
  font-size: 0.78rem;
  color: #a1a1aa;
  display: block;
  margin-bottom: 4px;
}

.slider-group input {
  width: 100%;
}

.toggle-pill {
  padding: 4px 10px;
  border-radius: 6px;
  border: 1px solid #3f3f46;
  background: #27272a;
  color: #71717a;
  font-size: 0.75rem;
  cursor: pointer;
}

.toggle-pill.on {
  background: rgba(16, 185, 129, 0.15);
  border-color: #10b981;
  color: #10b981;
}

.jsonl-code-area {
  max-height: 160px;
  overflow-y: auto;
  font-family: monospace;
  font-size: 0.7rem;
  background: #09090b;
  border-radius: 6px;
  padding: 8px;
  border: 1px solid #27272a;
}

.jsonl-line {
  word-break: break-all;
  padding: 3px 0;
  border-bottom: 1px solid #18181b;
  color: #38bdf8;
}

.api-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.input-label {
  font-size: 0.78rem;
  color: #a1a1aa;
}

.sim-input {
  background: #18181b;
  border: 1px solid #27272a;
  color: white;
  padding: 6px 10px;
  border-radius: 6px;
  font-size: 0.78rem;
}

.api-actions {
  display: flex;
  gap: 8px;
}

.status-ok { color: #10b981; font-weight: 700; }
.status-err { color: #ef4444; font-weight: 700; }
.api-time { color: #71717a; font-size: 0.72rem; }

.json-code {
  font-family: monospace;
  font-size: 0.72rem;
  color: #e4e4e7;
  max-height: 150px;
  overflow-y: auto;
  margin: 0;
}

.empty-hint {
  font-size: 0.75rem;
  color: #71717a;
  padding: 10px 0;
  text-align: center;
}

.phone-home-indicator {
  width: 120px;
  height: 4px;
  background: #52525b;
  border-radius: 9999px;
  margin: 10px auto;
}
</style>
