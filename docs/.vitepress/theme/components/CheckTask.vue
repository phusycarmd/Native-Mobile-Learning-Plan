<template>
  <div 
    class="check-task-item" 
    :class="{ 'is-completed': completed }"
    @click="toggle"
    tabindex="0"
    role="checkbox"
    :aria-checked="completed"
    @keydown.space.prevent="toggle"
    @keydown.enter.prevent="toggle"
  >
    <div class="checkbox-box">
      <svg 
        v-if="completed" 
        class="check-icon" 
        viewBox="0 0 24 24" 
        fill="none" 
        stroke="currentColor" 
        stroke-width="3" 
        stroke-linecap="round" 
        stroke-linejoin="round"
      >
        <polyline points="20 6 9 17 4 12"></polyline>
      </svg>
    </div>
    <div class="task-content">
      <slot></slot>
    </div>
    <div v-if="tag" class="task-tag" :class="'tag-' + tag.toLowerCase()">
      {{ tag }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { progressStore, toggleTask } from '../progressStore'

const props = defineProps<{
  id: string
  tag?: string
}>()

const completed = computed(() => !!progressStore.tasks[props.id])

function toggle() {
  toggleTask(props.id)
}
</script>

<style scoped>
.check-task-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 10px 14px;
  margin: 8px 0;
  border-radius: 8px;
  background: var(--vp-c-bg-soft);
  border: 1px solid var(--vp-c-border);
  cursor: pointer;
  user-select: none;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.check-task-item:hover {
  border-color: var(--vp-c-brand-1);
  background: var(--vp-c-bg-alt);
  transform: translateX(2px);
}

.checkbox-box {
  width: 20px;
  height: 20px;
  min-width: 20px;
  margin-top: 2px;
  border-radius: 5px;
  border: 2px solid var(--vp-c-text-3);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--vp-c-bg);
  transition: all 0.2s ease;
}

.check-task-item:hover .checkbox-box {
  border-color: var(--vp-c-brand-1);
}

.check-task-item.is-completed .checkbox-box {
  background: #10b981;
  border-color: #10b981;
}

.check-icon {
  width: 13px;
  height: 13px;
  color: #ffffff;
  animation: checkPop 0.25s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

@keyframes checkPop {
  0% { transform: scale(0); opacity: 0; }
  100% { transform: scale(1); opacity: 1; }
}

.task-content {
  flex: 1;
  font-size: 0.95rem;
  line-height: 1.5;
  color: var(--vp-c-text-1);
  transition: color 0.2s;
}

.check-task-item.is-completed .task-content {
  color: var(--vp-c-text-3);
  text-decoration: line-through;
}

.task-tag {
  font-size: 0.72rem;
  font-weight: 700;
  padding: 2px 7px;
  border-radius: 4px;
  align-self: center;
  text-transform: uppercase;
}

.tag-p0 {
  background: rgba(239, 68, 68, 0.15);
  color: #ef4444;
}

.tag-p1 {
  background: rgba(245, 158, 11, 0.15);
  color: #f59e0b;
}

.tag-p2 {
  background: rgba(100, 116, 139, 0.15);
  color: #94a3b8;
}

.tag-android {
  background: rgba(61, 220, 132, 0.15);
  color: #10b981;
}

.tag-ios {
  background: rgba(0, 113, 227, 0.15);
  color: #3b82f6;
}
</style>
