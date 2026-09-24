<template>
  <Teleport to="body">
    <!-- Lightbox Zoom Modal -->
    <div 
      v-if="isOpen" 
      class="diagram-modal-overlay" 
      @click.self="closeModal"
    >
      <div class="diagram-modal-content">
        <!-- Floating Control Toolbar -->
        <div class="modal-top-bar">
          <div class="modal-title">
            <span class="modal-badge">📊 DIAGRAM VIEWER</span>
            <span class="title-text">Xem chi tiết & Phóng to sơ đồ</span>
          </div>

          <div class="modal-toolbar-actions">
            <button class="tool-btn" @click="zoomOut" title="Thu nhỏ (-25%)">
              <span class="icon">➖</span>
              <span class="btn-text">Thu nhỏ</span>
            </button>
            <span class="zoom-indicator">{{ Math.round(scale * 100) }}%</span>
            <button class="tool-btn" @click="zoomIn" title="Phóng to (+25%)">
              <span class="icon">➕</span>
              <span class="btn-text">Phóng to</span>
            </button>
            <button class="tool-btn" @click="resetZoom" title="Đặt lại kích thước chuẩn">
              <span class="icon">🔄</span>
              <span class="btn-text">Reset</span>
            </button>
            <button class="tool-btn btn-close" @click="closeModal" title="Đóng cửa sổ (hoặc bấm ESC)">
              <span class="icon">✕</span>
              <span class="btn-text">Đóng (ESC)</span>
            </button>
          </div>
        </div>

        <!-- Canvas Viewport -->
        <div 
          class="modal-canvas" 
          @mousedown="startDrag"
          @mousemove="onDrag"
          @mouseup="endDrag"
          @mouseleave="endDrag"
          @wheel.prevent="onWheel"
        >
          <div 
            class="svg-scaler"
            :style="{
              transform: `translate(${translateX}px, ${translateY}px) scale(${scale})`,
              cursor: isDragging ? 'grabbing' : 'grab'
            }"
            v-html="activeSvgContent"
          ></div>
        </div>

        <!-- Footer Guide -->
        <div class="modal-footer-tip">
          <span>💡 <strong>Hướng dẫn:</strong> Cuộn bánh xe chuột để <strong>Phóng to / Thu nhỏ</strong> | Nhấn giữ chuột trái và kéo để <strong>Di chuyển sơ đồ</strong> | Bấm <strong>ESC</strong> để đóng</span>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'

const isOpen = ref(false)
const activeSvgContent = ref('')
const scale = ref(1.0)
const translateX = ref(0)
const translateY = ref(0)
const isDragging = ref(false)
const dragStart = { x: 0, y: 0 }

const zoomIn = () => {
  scale.value = Math.min(Number((scale.value + 0.25).toFixed(2)), 4.0)
}

const zoomOut = () => {
  scale.value = Math.max(Number((scale.value - 0.25).toFixed(2)), 0.4)
}

const resetZoom = () => {
  scale.value = 1.0
  translateX.value = 0
  translateY.value = 0
}

const closeModal = () => {
  isOpen.value = false
  resetZoom()
}

const onWheel = (e: WheelEvent) => {
  if (e.deltaY < 0) {
    zoomIn()
  } else {
    zoomOut()
  }
}

const startDrag = (e: MouseEvent) => {
  isDragging.value = true
  dragStart.x = e.clientX - translateX.value
  dragStart.y = e.clientY - translateY.value
}

const onDrag = (e: MouseEvent) => {
  if (!isDragging.value) return
  translateX.value = e.clientX - dragStart.x
  translateY.value = e.clientY - dragStart.y
}

const endDrag = () => {
  isDragging.value = false
}

const handleKeyDown = (e: KeyboardEvent) => {
  if (e.key === 'Escape' && isOpen.value) {
    closeModal()
  }
}

const openWithSvg = (svgEl: SVGElement) => {
  const clone = svgEl.cloneNode(true) as SVGElement
  clone.removeAttribute('width')
  clone.removeAttribute('max-width')
  clone.style.width = 'auto'
  clone.style.maxWidth = 'none'
  clone.style.height = 'auto'
  activeSvgContent.value = clone.outerHTML
  isOpen.value = true
  resetZoom()
}

// Attach natural click-to-zoom to all mermaid diagrams
const setupDiagramInteractions = () => {
  if (typeof document === 'undefined') return

  const mermaidContainers = document.querySelectorAll('.mermaid')
  mermaidContainers.forEach((container) => {
    const el = container as HTMLElement
    if (el.dataset.zoomReady === 'true') return
    el.dataset.zoomReady = 'true'

    el.style.cursor = 'zoom-in'
    el.title = 'Nhấp chuột vào sơ đồ để phóng to (Zoom in)'

    el.addEventListener('click', (e) => {
      const svg = el.querySelector('svg')
      if (svg) {
        openWithSvg(svg as unknown as SVGElement)
      }
    })
  })
}

onMounted(() => {
  window.addEventListener('keydown', handleKeyDown)

  setupDiagramInteractions()

  const observer = new MutationObserver(() => {
    nextTick(() => {
      setupDiagramInteractions()
    })
  })

  observer.observe(document.body, {
    childList: true,
    subtree: true
  })

  onUnmounted(() => {
    window.removeEventListener('keydown', handleKeyDown)
    observer.disconnect()
  })
})
</script>

<style>
/* Natural and elegant styling for Mermaid diagrams */
.mermaid {
  background: #0f0f12 !important;
  border: 1px solid #27272a !important;
  border-radius: 12px !important;
  padding: 1.25rem !important;
  margin: 1.5rem 0 !important;
  overflow-x: auto !important;
  cursor: zoom-in !important;
  transition: border-color 0.2s ease, box-shadow 0.2s ease !important;
}

.mermaid:hover {
  border-color: #6366f1 !important;
  box-shadow: 0 4px 20px rgba(99, 102, 241, 0.12) !important;
}
</style>

<style scoped>
.diagram-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(4, 4, 7, 0.92);
  backdrop-filter: blur(12px);
  z-index: 999999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.5rem;
  box-sizing: border-box;
}

.diagram-modal-content {
  width: 96vw;
  height: 92vh;
  background: #141417;
  border: 1px solid #27272a;
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 25px 70px -15px rgba(0, 0, 0, 0.9);
  overflow: hidden;
}

.modal-top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background: #18181b;
  border-bottom: 1px solid #27272a;
  flex-wrap: wrap;
  gap: 10px;
}

.modal-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.modal-badge {
  background: rgba(99, 102, 241, 0.2);
  color: #a5b4fc;
  border: 1px solid rgba(99, 102, 241, 0.4);
  font-size: 0.68rem;
  font-weight: 700;
  padding: 2px 7px;
  border-radius: 4px;
  letter-spacing: 0.05em;
  font-family: var(--vp-font-family-mono);
}

.title-text {
  font-size: 0.95rem;
  font-weight: 700;
  color: #ffffff;
}

.modal-toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tool-btn {
  background: #27272a;
  color: #e4e4e7;
  border: 1px solid #3f3f46;
  padding: 7px 12px;
  border-radius: 8px;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  transition: all 0.2s ease;
}

.tool-btn:hover {
  background: #3f3f46;
  color: #ffffff;
}

.btn-close {
  background: #dc2626 !important;
  border-color: #ef4444 !important;
  color: #ffffff !important;
  margin-left: 6px;
}

.btn-close:hover {
  background: #b91c1c !important;
}

.zoom-indicator {
  font-family: var(--vp-font-family-mono);
  font-size: 0.86rem;
  font-weight: 700;
  color: #60a5fa;
  background: #09090b;
  border: 1px solid #27272a;
  padding: 5px 12px;
  border-radius: 6px;
  min-width: 60px;
  text-align: center;
}

.modal-canvas {
  flex: 1;
  overflow: hidden;
  position: relative;
  background: radial-gradient(#27272a 1.2px, transparent 1.2px);
  background-size: 26px 26px;
  background-color: #09090b;
  display: flex;
  align-items: center;
  justify-content: center;
  user-select: none;
}

.svg-scaler {
  transition: transform 0.04s ease-out;
  transform-origin: center center;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

.svg-scaler :deep(svg) {
  max-width: none !important;
  max-height: none !important;
  width: auto !important;
  height: auto !important;
  display: block;
}

.modal-footer-tip {
  padding: 8px 16px;
  background: #18181b;
  border-top: 1px solid #27272a;
  font-size: 0.78rem;
  color: #a1a1aa;
  text-align: center;
}
</style>
