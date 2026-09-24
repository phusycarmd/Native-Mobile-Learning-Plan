import DefaultTheme from 'vitepress/theme'
import { h } from 'vue'
import CheckTask from './components/CheckTask.vue'
import ProgressTracker from './components/ProgressTracker.vue'
import GlobalProgress from './components/GlobalProgress.vue'
import DeviceSimulator from './components/DeviceSimulator.vue'
import DiagramZoomModal from './components/DiagramZoomModal.vue'
import IonicMappingTable from './components/IonicMappingTable.vue'
import './custom.css'

export default {
  extends: DefaultTheme,
  Layout() {
    return h(DefaultTheme.Layout, null, {
      'layout-bottom': () => h(DiagramZoomModal)
    })
  },
  enhanceApp({ app }) {
    app.component('CheckTask', CheckTask)
    app.component('ProgressTracker', ProgressTracker)
    app.component('GlobalProgress', GlobalProgress)
    app.component('DeviceSimulator', DeviceSimulator)
    app.component('DiagramZoomModal', DiagramZoomModal)
    app.component('IonicMappingTable', IonicMappingTable)
  }
}
