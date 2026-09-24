<template>
  <div class="progress-tracker-card">
    <div class="tracker-header">
      <div class="tracker-title-group">
        <span class="tracker-icon">📊</span>
        <h4 class="tracker-title">{{ title || 'Tiến độ học tập' }}</h4>
      </div>
      <div class="tracker-stats">
        <span class="count-badge">{{ completedCount }} / {{ totalCount }} hoàn thành</span>
        <span class="pct-badge">{{ percentage }}%</span>
      </div>
    </div>

    <!-- Progress Bar -->
    <div class="progress-bar-container">
      <div 
        class="progress-bar-fill" 
        :style="{ width: percentage + '%' }"
        :class="{ 'is-complete': percentage === 100 }"
      ></div>
    </div>

    <!-- Celebration message -->
    <div v-if="percentage === 100 && totalCount > 0" class="completion-banner">
      🎉 Tuyệt vời! Bạn đã hoàn thành toàn bộ mục tiêu trong phần này!
    </div>

    <!-- Reset action -->
    <div class="tracker-footer">
      <span class="tracker-note">Đánh dấu vào từng mục để lưu tiến trình tự động vào trình duyệt.</span>
      <button v-if="completedCount > 0" @click="handleReset" class="reset-btn" title="Đặt lại tiến trình">
        Đặt lại
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { progressStore, resetCategory } from '../progressStore'

const props = defineProps<{
  prefix: string // e.g. "w1", "w2", "p0", or comma-separated ids
  title?: string
  total?: number
  ids?: string[] // optional explicit list of task IDs
}>()

const taskIds = computed(() => {
  if (props.ids && props.ids.length > 0) {
    return props.ids
  }
  // Find all tasks matching prefix in store or calculate from total
  return []
})

const completedCount = computed(() => {
  if (props.ids && props.ids.length > 0) {
    return props.ids.filter(id => !!progressStore.tasks[id]).length
  }
  // Otherwise count all keys in progressStore that start with prefix
  return Object.keys(progressStore.tasks).filter(key => key.startsWith(props.prefix) && progressStore.tasks[key]).length
})

const totalCount = computed(() => {
  if (props.total && props.total > 0) return props.total
  if (props.ids && props.ids.length > 0) return props.ids.length
  return Math.max(completedCount.value, 1)
})

const percentage = computed(() => {
  if (totalCount.value === 0) return 0
  return Math.min(100, Math.round((completedCount.value / totalCount.value) * 100))
})

function handleReset() {
  if (confirm('Bạn có chắc muốn đặt lại các mục đã hoàn thành trong phần này?')) {
    if (props.ids && props.ids.length > 0) {
      props.ids.forEach(id => {
        delete progressStore.tasks[id]
      })
      if (typeof window !== 'undefined') {
        localStorage.setItem('native_mobile_learning_plan_progress', JSON.stringify(progressStore.tasks))
      }
    } else {
      resetCategory(props.prefix)
    }
  }
}
</script>

<style scoped>
.progress-tracker-card {
  background: var(--vp-c-bg-soft);
  border: 1px solid var(--vp-c-border);
  border-radius: 12px;
  padding: 16px 20px;
  margin: 20px 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.tracker-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 8px;
}

.tracker-title-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tracker-icon {
  font-size: 1.25rem;
}

.tracker-title {
  margin: 0 !important;
  font-size: 1rem !important;
  font-weight: 700;
  color: var(--vp-c-text-1);
}

.tracker-stats {
  display: flex;
  align-items: center;
  gap: 8px;
}

.count-badge {
  font-size: 0.82rem;
  color: var(--vp-c-text-2);
  background: var(--vp-c-bg-alt);
  padding: 3px 8px;
  border-radius: 6px;
  border: 1px solid var(--vp-c-border);
}

.pct-badge {
  font-size: 0.88rem;
  font-weight: 800;
  color: var(--vp-c-brand-1);
}

.progress-bar-container {
  width: 100%;
  height: 10px;
  background: var(--vp-c-bg-alt);
  border-radius: 9999px;
  overflow: hidden;
  margin-bottom: 10px;
  border: 1px solid var(--vp-c-border);
}

.progress-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #6366f1, #10b981);
  border-radius: 9999px;
  transition: width 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}

.progress-bar-fill.is-complete {
  background: #10b981;
}

.completion-banner {
  background: rgba(16, 185, 129, 0.12);
  color: #059669;
  border: 1px solid rgba(16, 185, 129, 0.3);
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 0.85rem;
  font-weight: 600;
  margin-bottom: 10px;
  animation: fadeIn 0.3s ease;
}

.dark .completion-banner {
  color: #34d399;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-4px); }
  to { opacity: 1; transform: translateY(0); }
}

.tracker-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.78rem;
  color: var(--vp-c-text-3);
  margin-top: 4px;
}

.reset-btn {
  background: none;
  border: none;
  color: var(--vp-c-text-3);
  text-decoration: underline;
  cursor: pointer;
  padding: 0;
  font-size: 0.78rem;
  transition: color 0.2s;
}

.reset-btn:hover {
  color: #ef4444;
}
</style>
