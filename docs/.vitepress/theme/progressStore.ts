import { reactive, watch } from 'vue'

const STORAGE_KEY = 'native_mobile_learning_plan_progress'

interface ProgressState {
  tasks: Record<string, boolean>
}

// Load initial state from localStorage safely (SSR-friendly)
function loadState(): Record<string, boolean> {
  if (typeof window === 'undefined') return {}
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : {}
  } catch (e) {
    console.error('Failed to load progress from localStorage', e)
    return {}
  }
}

export const progressStore = reactive<ProgressState>({
  tasks: loadState()
})

// Save state to localStorage
export function toggleTask(id: string) {
  progressStore.tasks[id] = !progressStore.tasks[id]
  if (typeof window !== 'undefined') {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(progressStore.tasks))
    } catch (e) {
      console.error('Failed to save progress to localStorage', e)
    }
  }
}

export function isTaskCompleted(id: string): boolean {
  return !!progressStore.tasks[id]
}

export function resetCategory(prefix: string) {
  if (typeof window === 'undefined') return
  Object.keys(progressStore.tasks).forEach(key => {
    if (key.startsWith(prefix) || prefix === 'all') {
      delete progressStore.tasks[key]
    }
  })
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(progressStore.tasks))
  } catch (e) {}
}
