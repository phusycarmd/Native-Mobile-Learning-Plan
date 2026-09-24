<template>
  <div class="global-progress-widget">
    <div class="widget-header">
      <div class="header-left">
        <div class="badge-level">{{ levelTitle }}</div>
        <h3 class="widget-title">Tổng tiến độ toàn khóa học</h3>
      </div>
      <div class="header-right">
        <span class="stat-number">{{ totalCompleted }} / {{ totalCount }}</span>
        <span class="stat-pct">{{ overallPercentage }}%</span>
      </div>
    </div>

    <!-- Overall Progress Bar -->
    <div class="big-progress-bar">
      <div 
        class="bar-fill" 
        :style="{ width: overallPercentage + '%' }"
        :class="{ 'is-golden': overallPercentage === 100 }"
      ></div>
    </div>

    <!-- Level description -->
    <p class="level-desc">{{ levelDescription }}</p>

    <!-- Week Breakdown Grid -->
    <div class="weeks-grid">
      <div 
        v-for="w in weeks" 
        :key="w.name" 
        class="week-card"
        @click="goTo(w.link)"
      >
        <div class="week-card-top">
          <span class="week-name">{{ w.name }}</span>
          <span class="week-count">{{ w.completed }} / {{ w.total }}</span>
        </div>
        <div class="mini-bar">
          <div 
            class="mini-bar-fill" 
            :style="{ width: w.pct + '%' }"
            :class="{ 'complete': w.pct === 100 }"
          ></div>
        </div>
        <div class="week-card-bottom">
          <span class="week-topic">{{ w.topic }}</span>
          <span class="week-arrow">➔</span>
        </div>
      </div>
    </div>

    <!-- Footer with export/import / reset buttons -->
    <div class="widget-actions">
      <button @click="exportProgress" class="action-btn export-btn" title="Xuất dữ liệu tiến độ">
        📥 Xuất tiến độ (JSON)
      </button>
      <button @click="importProgress" class="action-btn import-btn" title="Nhập dữ liệu tiến độ">
        📤 Nhập tiến độ
      </button>
      <button v-if="totalCompleted > 0" @click="resetAll" class="action-btn reset-btn">
        🔄 Đặt lại toàn bộ
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { progressStore } from '../progressStore'

const week1Ids = [
  'w1-m1', 'w1-m2', 'w1-m3', 'w1-m4', 'w1-m5',
  'w1-d1', 'w1-d2', 'w1-d3', 'w1-d4', 'w1-d5'
]

const week2Ids = [
  'w2-m1', 'w2-m2', 'w2-m3', 'w2-m4', 'w2-m5', 'w2-m6',
  'w2-d1', 'w2-d2', 'w2-d3', 'w2-d4', 'w2-d5'
]

const week3Ids = [
  'w3-m1', 'w3-m2', 'w3-m3', 'w3-m4', 'w3-m5', 'w3-m6',
  'w3-d1', 'w3-d2', 'w3-d3', 'w3-d4', 'w3-d5'
]

const week4Ids = [
  'w4-m1', 'w4-m2', 'w4-m3',
  'w4-e2e-1', 'w4-e2e-2', 'w4-e2e-3', 'w4-e2e-4', 'w4-e2e-5',
  'w4-e2e-6', 'w4-e2e-7', 'w4-e2e-8', 'w4-e2e-9', 'w4-e2e-10',
  'w4-e2e-11', 'w4-e2e-12', 'w4-e2e-13', 'w4-e2e-14', 'w4-e2e-15',
  'w4-e2e-16', 'w4-e2e-17',
  'w4-rf-1', 'w4-rf-2', 'w4-rf-3', 'w4-rf-4',
  'w4-rf-5', 'w4-rf-6', 'w4-rf-7', 'w4-rf-8'
]

const allCurriculumIds = [...week1Ids, ...week2Ids, ...week3Ids, ...week4Ids]

function countCompleted(ids: string[]) {
  return ids.filter(id => !!progressStore.tasks[id]).length
}

const w1Completed = computed(() => countCompleted(week1Ids))
const w2Completed = computed(() => countCompleted(week2Ids))
const w3Completed = computed(() => countCompleted(week3Ids))
const w4Completed = computed(() => countCompleted(week4Ids))

const totalCompleted = computed(() => countCompleted(allCurriculumIds))
const totalCount = computed(() => allCurriculumIds.length)

const overallPercentage = computed(() => {
  if (totalCount.value === 0) return 0
  return Math.min(100, Math.round((totalCompleted.value / totalCount.value) * 100))
})

const weeks = computed(() => [
  {
    name: 'Tuần 1',
    topic: 'Fundamentals & UI',
    completed: w1Completed.value,
    total: week1Ids.length,
    pct: Math.round((w1Completed.value / week1Ids.length) * 100),
    link: '/schedule/week-1'
  },
  {
    name: 'Tuần 2',
    topic: 'Native APIs & BLE',
    completed: w2Completed.value,
    total: week2Ids.length,
    pct: Math.round((w2Completed.value / week2Ids.length) * 100),
    link: '/schedule/week-2'
  },
  {
    name: 'Tuần 3',
    topic: 'NativeCore & Background',
    completed: w3Completed.value,
    total: week3Ids.length,
    pct: Math.round((w3Completed.value / week3Ids.length) * 100),
    link: '/schedule/week-3'
  },
  {
    name: 'Tuần 4',
    topic: 'E2E Testing & Refactor',
    completed: w4Completed.value,
    total: week4Ids.length,
    pct: Math.round((w4Completed.value / week4Ids.length) * 100),
    link: '/schedule/week-4'
  }
])

const levelTitle = computed(() => {
  const p = overallPercentage.value
  if (p === 100) return '🏆 BẬC THẦY NATIVE'
  if (p >= 70) return '🚀 KỸ SƯ NATIVE CORE'
  if (p >= 40) return '📡 CHUYÊN GIA PHẦN CỨNG & BLE'
  if (p >= 15) return '⚡ NỀN TẢNG VỮNG VÀNG'
  return '🌱 NGƯỜI MỚI NHẬP MÔN'
})

const levelDescription = computed(() => {
  const p = overallPercentage.value
  if (p === 100) return 'Xuất sắc! Bạn đã hoàn tất toàn bộ 100% mục tiêu, bài tập và checklist của cả 4 tuần học!'
  if (p >= 70) return 'Bạn đã xây dựng kiến trúc NativeCore hoàn chỉnh và sẵn sàng kiểm thử toàn diện End-to-End.'
  if (p >= 40) return 'Bạn đã làm chủ Bluetooth BLE, tương tác Camera và gọi REST API bất đồng bộ.'
  if (p >= 15) return 'Bạn đã nắm vững cú pháp Kotlin, Swift và dựng thành công vỏ ứng dụng Native UI.'
  return 'Hãy bắt đầu với các bài tập của Tuần 1 để tích lũy tiến trình học tập!'
})

function goTo(link: string) {
  if (typeof window !== 'undefined') {
    window.location.href = link
  }
}

function exportProgress() {
  const data = JSON.stringify(progressStore.tasks, null, 2)
  const blob = new Blob([data], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `native_mobile_learning_progress_${new Date().toISOString().slice(0, 10)}.json`
  a.click()
  URL.revokeObjectURL(url)
}

function importProgress() {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'application/json'
  input.onchange = e => {
    const file = (e.target as HTMLInputElement).files?.[0]
    if (!file) return
    const reader = new FileReader()
    reader.onload = ev => {
      try {
        const parsed = JSON.parse(ev.target?.result as string)
        if (typeof parsed === 'object') {
          Object.assign(progressStore.tasks, parsed)
          localStorage.setItem('native_mobile_learning_plan_progress', JSON.stringify(progressStore.tasks))
          alert('Đã nhập dữ liệu tiến độ thành công!')
        }
      } catch (err) {
        alert('File không hợp lệ!')
      }
    }
    reader.readAsText(file)
  }
  input.click()
}

function resetAll() {
  if (confirm('Bạn có chắc muốn đặt lại toàn bộ tiến độ của tất cả các tuần?')) {
    Object.keys(progressStore.tasks).forEach(k => delete progressStore.tasks[k])
    localStorage.removeItem('native_mobile_learning_plan_progress')
  }
}
</script>

<style scoped>
.global-progress-widget {
  background: var(--vp-c-bg-soft);
  border: 1px solid var(--vp-c-border);
  border-radius: 16px;
  padding: 24px;
  margin: 28px 0;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}

.widget-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 12px;
}

.badge-level {
  display: inline-block;
  font-size: 0.72rem;
  font-weight: 800;
  letter-spacing: 0.06em;
  padding: 2px 10px;
  border-radius: 9999px;
  background: rgba(99, 102, 241, 0.15);
  color: var(--vp-c-brand-1);
  margin-bottom: 6px;
}

.widget-title {
  margin: 0 !important;
  font-size: 1.25rem !important;
  font-weight: 800;
  color: var(--vp-c-text-1);
}

.header-right {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.stat-number {
  font-size: 0.9rem;
  color: var(--vp-c-text-2);
}

.stat-pct {
  font-size: 1.8rem;
  font-weight: 900;
  color: var(--vp-c-brand-1);
  line-height: 1;
}

.big-progress-bar {
  width: 100%;
  height: 14px;
  background: var(--vp-c-bg-alt);
  border-radius: 9999px;
  overflow: hidden;
  border: 1px solid var(--vp-c-border);
  margin-bottom: 10px;
}

.bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #6366f1, #a855f7, #10b981);
  border-radius: 9999px;
  transition: width 0.4s ease;
}

.bar-fill.is-golden {
  background: linear-gradient(90deg, #f59e0b, #10b981);
}

.level-desc {
  margin: 0 0 20px 0 !important;
  font-size: 0.88rem;
  color: var(--vp-c-text-2);
  line-height: 1.5;
}

.weeks-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.week-card {
  background: var(--vp-c-bg);
  border: 1px solid var(--vp-c-border);
  border-radius: 10px;
  padding: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.week-card:hover {
  border-color: var(--vp-c-brand-1);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.week-card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.week-name {
  font-weight: 700;
  font-size: 0.85rem;
  color: var(--vp-c-text-1);
}

.week-count {
  font-size: 0.75rem;
  color: var(--vp-c-text-3);
}

.mini-bar {
  width: 100%;
  height: 6px;
  background: var(--vp-c-bg-alt);
  border-radius: 9999px;
  overflow: hidden;
  margin-bottom: 8px;
}

.mini-bar-fill {
  height: 100%;
  background: var(--vp-c-brand-1);
  border-radius: 9999px;
  transition: width 0.3s ease;
}

.mini-bar-fill.complete {
  background: #10b981;
}

.week-card-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.75rem;
  color: var(--vp-c-text-2);
}

.week-arrow {
  color: var(--vp-c-brand-1);
  font-size: 0.75rem;
}

.widget-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  padding-top: 12px;
  border-top: 1px solid var(--vp-c-border);
}

.action-btn {
  background: var(--vp-c-bg-alt);
  border: 1px solid var(--vp-c-border);
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--vp-c-text-2);
  cursor: pointer;
  transition: all 0.2s;
}

.action-btn:hover {
  border-color: var(--vp-c-brand-1);
  color: var(--vp-c-brand-1);
}

.reset-btn:hover {
  border-color: #ef4444;
  color: #ef4444;
}
</style>
