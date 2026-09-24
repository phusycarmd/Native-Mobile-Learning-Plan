<template>
  <div class="ionic-mapping-container">
    <!-- Header Controls: Search & View Toggle -->
    <div class="mapping-toolbar">
      <div class="search-wrapper">
        <span class="search-icon">🔍</span>
        <input
          v-model="searchQuery"
          type="text"
          placeholder="Tìm linh kiện (vd: button, modal, input, list, refresher...)"
          class="search-input"
        />
        <button v-if="searchQuery" @click="searchQuery = ''" class="clear-btn" title="Xóa tìm kiếm">✕</button>
      </div>

      <div class="view-toggle">
        <button
          :class="['toggle-btn', { active: viewMode === 'cards' }]"
          @click="viewMode = 'cards'"
          title="Xem dạng thẻ chi tiết"
        >
          🗂️ Dạng Thẻ
        </button>
        <button
          :class="['toggle-btn', { active: viewMode === 'table' }]"
          @click="viewMode = 'table'"
          title="Xem dạng bảng tra cứu"
        >
          📊 Dạng Bảng
        </button>
      </div>
    </div>

    <!-- Category Filter Tabs -->
    <div class="category-tabs">
      <button
        v-for="cat in categories"
        :key="cat.id"
        :class="['cat-tab', { active: selectedCategory === cat.id }]"
        @click="selectedCategory = cat.id"
      >
        <span class="cat-icon">{{ cat.icon }}</span>
        <span class="cat-label">{{ cat.name }}</span>
        <span class="cat-count">{{ getCount(cat.id) }}</span>
      </button>
    </div>

    <!-- Empty State -->
    <div v-if="filteredList.length === 0" class="empty-state">
      <p>Không tìm thấy linh kiện nào khớp với "<strong>{{ searchQuery }}</strong>"</p>
      <button @click="searchQuery = ''; selectedCategory = 'all'" class="reset-btn">Xem tất cả linh kiện</button>
    </div>

    <!-- VIEW MODE: CARDS (Default - Extremely mobile-friendly & readable) -->
    <div v-else-if="viewMode === 'cards'" class="cards-grid">
      <div v-for="(item, idx) in filteredList" :key="idx" class="mapping-card">
        <div class="card-header">
          <div class="ionic-tag">
            <span class="platform-icon">🌐</span>
            <code>{{ item.ionic }}</code>
          </div>
          <span class="category-pill">{{ getCategoryName(item.cat) }}</span>
        </div>

        <div class="card-body">
          <div class="platform-col android-col">
            <div class="platform-label">
              <span class="badge-android">Android Compose</span>
            </div>
            <div class="code-box">
              <code>{{ item.android }}</code>
            </div>
          </div>

          <div class="platform-col ios-col">
            <div class="platform-label">
              <span class="badge-ios">iOS SwiftUI</span>
            </div>
            <div class="code-box">
              <code>{{ item.ios }}</code>
            </div>
          </div>
        </div>

        <div v-if="item.note" class="card-footer">
          <span class="note-icon">💡</span>
          <span class="note-text">{{ item.note }}</span>
        </div>
      </div>
    </div>

    <!-- VIEW MODE: TABLE (Clean, Responsive & No Column Crushing) -->
    <div v-else class="table-wrapper">
      <table class="mapping-table">
        <thead>
          <tr>
            <th style="width: 26%;">Ionic / Capacitor (Web)</th>
            <th style="width: 37%;">Android (Jetpack Compose)</th>
            <th style="width: 37%;">iOS (SwiftUI)</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(item, idx) in filteredList" :key="idx">
            <td class="td-ionic">
              <div class="ionic-cell">
                <code>{{ item.ionic }}</code>
                <span class="sub-cat">{{ getCategoryName(item.cat) }}</span>
              </div>
            </td>
            <td class="td-android">
              <code>{{ item.android }}</code>
              <div v-if="item.note" class="td-note">
                <span class="note-bullet">💡</span> {{ item.note }}
              </div>
            </td>
            <td class="td-ios">
              <code>{{ item.ios }}</code>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="mapping-footer-info">
      Đang hiển thị <strong>{{ filteredList.length }}</strong> / {{ rawData.length }} linh kiện được quy đổi.
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

interface MappingItem {
  ionic: string
  android: string
  ios: string
  note: string
  cat: 'scaffold' | 'inputs' | 'lists' | 'dialogs' | 'gestures'
}

const searchQuery = ref('')
const selectedCategory = ref('all')
const viewMode = ref<'cards' | 'table'>('cards')

const categories = [
  { id: 'all', name: 'Tất cả', icon: '✨' },
  { id: 'scaffold', name: 'Bố cục & Navigation', icon: '📱' },
  { id: 'inputs', name: 'Form & Nhập liệu', icon: '✍️' },
  { id: 'lists', name: 'Danh sách & Dữ liệu', icon: '📋' },
  { id: 'dialogs', name: 'Hộp thoại & Popups', icon: '💬' },
  { id: 'gestures', name: 'Cử chỉ & UX', icon: '👆' }
]

const rawData: MappingItem[] = [
  // Nhóm 1: Bố cục & Navigation
  {
    cat: 'scaffold',
    ionic: '<ion-app>\n  ...\n</ion-app>',
    android: 'Khối root @Composable trong setContent { }',
    ios: 'WindowGroup trong App @main',
    note: 'Căn nguyên (Root) của toàn bộ ứng dụng'
  },
  {
    cat: 'scaffold',
    ionic: '<ion-page>\n  ...\n</ion-page>',
    android: '@Composable Screen()',
    ios: 'View struct',
    note: 'Một màn hình độc lập'
  },
  {
    cat: 'scaffold',
    ionic: '<ion-header>\n  <ion-toolbar>...</ion-toolbar>\n</ion-header>',
    android: 'TopAppBar() / CenterAlignedTopAppBar()',
    ios: '.navigationTitle() & .toolbar { }',
    note: 'Native tự động co giãn theo Safe Area, tai thỏ và Dynamic Island mà không cần CSS env(safe-area-inset-top)'
  },
  {
    cat: 'scaffold',
    ionic: '<ion-content>\n  ...\n</ion-content>',
    android: 'Scaffold { innerPadding -> Box(Modifier.padding(innerPadding)) }',
    ios: 'ScrollView { ... } hoặc Container',
    note: 'Ionic dùng DOM scroll trong WebView; Native cuộn trực tiếp trên GPU compositor với tần số 120Hz mượt mà'
  },
  {
    cat: 'scaffold',
    ionic: '<ion-footer>\n  <ion-toolbar>...</ion-toolbar>\n</ion-footer>',
    android: 'BottomAppBar() hoặc NavigationBar()',
    ios: '.toolbar { ToolbarItemGroup(placement: .bottomBar) }',
    note: 'Thanh công cụ neo cố định ở đáy màn hình'
  },
  {
    cat: 'scaffold',
    ionic: '<ion-tabs>\n  <ion-tab-bar>...</ion-tab-bar>\n</ion-tabs>',
    android: 'NavigationBar { NavigationBarItem(...) }',
    ios: 'TabView { ... }.tabItem { ... }',
    note: 'Thanh chuyển tab điều hướng chính dưới đáy màn hình'
  },
  {
    cat: 'scaffold',
    ionic: '<ion-menu>\n  ...\n</ion-menu>',
    android: 'ModalNavigationDrawer { ModalDrawerSheet { ... } }',
    ios: 'NavigationSplitView / Custom Drawer View',
    note: 'Menu ngăn kéo trượt từ cạnh màn hình sang'
  },

  // Nhóm 2: Form & Nhập liệu
  {
    cat: 'inputs',
    ionic: '<ion-input>...</ion-input>',
    android: 'OutlinedTextField() / TextField()',
    ios: 'TextField() / SecureField()',
    note: 'Native quản lý bàn phím ảo (IME) chuẩn xác, tự động đẩy view, không bao giờ bị lỗi che khuất (keyboard overlap)'
  },
  {
    cat: 'inputs',
    ionic: '<ion-textarea>...</ion-textarea>',
    android: 'OutlinedTextField(minLines = 3)',
    ios: 'TextEditor()',
    note: 'Khung nhập văn bản nhiều dòng'
  },
  {
    cat: 'inputs',
    ionic: '<ion-button>...</ion-button>',
    android: 'Button(), OutlinedButton(), TextButton()',
    ios: 'Button().buttonStyle(.borderedProminent / .borderless)',
    note: 'Phản hồi xúc giác (Haptic Feedback) và hiệu ứng chuyển động chạm (Ripple / Press state) tức thời (< 16ms)'
  },
  {
    cat: 'inputs',
    ionic: '<ion-toggle>...</ion-toggle>',
    android: 'Switch(checked, onCheckedChange)',
    ios: 'Toggle(isOn: $state)',
    note: 'Công tắc gạt Bật/Tắt'
  },
  {
    cat: 'inputs',
    ionic: '<ion-checkbox>...</ion-checkbox>',
    android: 'Checkbox(checked, onCheckedChange)',
    ios: 'Toggle(isOn: $state).toggleStyle(.checkbox)',
    note: 'Hộp chọn đánh dấu'
  },
  {
    cat: 'inputs',
    ionic: '<ion-radio-group>\n  <ion-radio>...</ion-radio>\n</ion-radio-group>',
    android: 'RadioButton(selected, onClick)',
    ios: 'Picker("...", selection: $sel) { }.pickerStyle(.radioGroup)',
    note: 'Chọn 1 trong nhiều phương án'
  },
  {
    cat: 'inputs',
    ionic: '<ion-range>...</ion-range>',
    android: 'Slider(value, onValueChange)',
    ios: 'Slider(value: $state, in: 0...100)',
    note: 'Thanh trượt điều chỉnh giá trị'
  },
  {
    cat: 'inputs',
    ionic: '<ion-select>\n  <ion-select-option>...</ion-select-option>\n</ion-select>',
    android: 'ExposedDropdownMenuBox { ... }',
    ios: 'Picker("...", selection: $sel) { }.pickerStyle(.menu)',
    note: 'Hộp chọn thả xuống (Dropdown)'
  },
  {
    cat: 'inputs',
    ionic: '<ion-searchbar>...</ion-searchbar>',
    android: 'SearchBar { ... } (Material 3)',
    ios: '.searchable(text: $query)',
    note: 'Thanh tìm kiếm chuẩn tích hợp hiệu ứng lọc'
  },
  {
    cat: 'inputs',
    ionic: '<ion-datetime>...</ion-datetime>',
    android: 'DatePicker() / TimePicker() (Material 3)',
    ios: 'DatePicker(selection: $date)',
    note: 'Bộ chọn ngày giờ native hệ thống'
  },

  // Nhóm 3: Danh sách & Dữ liệu
  {
    cat: 'lists',
    ionic: '<ion-text>...</ion-text>\n<ion-label>...</ion-label>',
    android: 'Text()',
    ios: 'Text()',
    note: 'Render trực tiếp bằng Typography Engine hệ thống, không tốn chi phí layout reflow của WebKit'
  },
  {
    cat: 'lists',
    ionic: '<ion-list>\n  <ion-item>...</ion-item>\n</ion-list>',
    android: 'LazyColumn { items(...) }',
    ios: 'List { ForEach(...) }',
    note: 'Tái sử dụng ô nhớ bộ nhớ đệm (View recycling); cuộn hàng vạn phần tử mà không gây tràn RAM'
  },
  {
    cat: 'lists',
    ionic: '<ion-grid>\n  <ion-row>\n    <ion-col>...</ion-col>\n  </ion-row>\n</ion-grid>',
    android: 'LazyVerticalGrid(GridCells.Fixed(2)) / Row / Column',
    ios: 'LazyVGrid(columns: [GridItem(), ...])',
    note: 'Bố cục dạng lưới đa cột hiệu năng cao'
  },
  {
    cat: 'lists',
    ionic: '<ion-card>\n  <ion-card-content>...</ion-card-content>\n</ion-card>',
    android: 'ElevatedCard { ... } / Card { ... }',
    ios: 'VStack { }.background(RoundedRectangle).shadow()',
    note: 'Vẽ bóng đổ (Elevation shadow) chuẩn xác bằng bộ xử lý đồ họa phần cứng GPU'
  },
  {
    cat: 'lists',
    ionic: '<ion-avatar>\n  <img />\n</ion-avatar>',
    android: 'AsyncImage(modifier = Modifier.clip(CircleShape))',
    ios: 'Image().clipShape(Circle())',
    note: 'Ảnh đại diện bo tròn'
  },
  {
    cat: 'lists',
    ionic: '<ion-badge>...</ion-badge>',
    android: 'Badge { Text("3") }',
    ios: '.badge(3)',
    note: 'Huy hiệu số đếm thông báo'
  },
  {
    cat: 'lists',
    ionic: '<ion-chip>...</ion-chip>',
    android: 'AssistChip(), FilterChip(), InputChip()',
    ios: 'Label().padding(...).background(Capsule())',
    note: 'Thẻ tag nhãn nhỏ gọn'
  },
  {
    cat: 'lists',
    ionic: '<ion-img>...</ion-img>',
    android: 'AsyncImage(model = ...) (Coil) / Image()',
    ios: 'AsyncImage(url: ...) / Image()',
    note: 'Nạp ảnh trực tiếp vào bộ nhớ đồ họa bitmap, không qua HTML canvas hay thẻ img trình duyệt'
  },
  {
    cat: 'lists',
    ionic: '<ion-skeleton-text>...</ion-skeleton-text>',
    android: 'Modifier custom shimmer / Placeholder',
    ios: '.redacted(reason: .placeholder)',
    note: 'Hiệu ứng khung xương tải dữ liệu (Skeleton loader)'
  },

  // Nhóm 4: Hộp thoại & Popups
  {
    cat: 'dialogs',
    ionic: '<ion-spinner>...</ion-spinner>',
    android: 'CircularProgressIndicator()',
    ios: 'ProgressView()',
    note: 'Render vòng quay mượt mà ở tần số 120Hz, không bị giật lag khi có tác vụ ngầm'
  },
  {
    cat: 'dialogs',
    ionic: '<ion-alert>...</ion-alert>\nhoặc AlertController',
    android: 'AlertDialog(onDismissRequest, confirmButton)',
    ios: '.alert("Tiêu đề", isPresented: $show) { }',
    note: 'Hộp thoại cảnh báo popup modal'
  },
  {
    cat: 'dialogs',
    ionic: '<ion-action-sheet>...</ion-action-sheet>',
    android: 'ModalBottomSheet { ... }',
    ios: '.confirmationDialog("Tùy chọn", isPresented: $show) { }',
    note: 'Bảng tùy chọn trượt mượt mà từ đáy màn hình'
  },
  {
    cat: 'dialogs',
    ionic: '<ion-modal>\n  ...\n</ion-modal>',
    android: 'Dialog(onDismissRequest) / ModalBottomSheet',
    ios: '.sheet(isPresented: $show) { DetailView() }',
    note: 'Màn hình modal phủ lên trên có cử chỉ vuốt đóng tự nhiên'
  },
  {
    cat: 'dialogs',
    ionic: '<ion-toast>...</ion-toast>\nhoặc ToastController',
    android: 'SnackbarHostState.showSnackbar("...")',
    ios: 'Custom Banner Overlay / Banner popup',
    note: 'Thông báo nhanh ở đáy màn hình tự biến mất'
  },
  {
    cat: 'dialogs',
    ionic: '<ion-loading>...</ion-loading>\nhoặc LoadingController',
    android: 'Dialog chứa CircularProgressIndicator()',
    ios: '.overlay { ProgressView() }',
    note: 'Màn chắn chờ tác vụ xử lý'
  },
  {
    cat: 'dialogs',
    ionic: '<ion-popover>\n  ...\n</ion-popover>',
    android: 'DropdownMenu() / Popup()',
    ios: '.popover(isPresented: $show) { }',
    note: 'Hộp thoại nhỏ định vị chính xác theo tọa độ nút bấm'
  },

  // Nhóm 5: Cử chỉ & UX
  {
    cat: 'gestures',
    ionic: '<ion-refresher>\n  <ion-refresher-content />\n</ion-refresher>',
    android: 'PullToRefreshBox(isRefreshing, onRefresh)',
    ios: '.refreshable { await refreshData() }',
    note: 'Cử chỉ kéo xuống để nạp lại dữ liệu tự nhiên của hệ điều hành'
  },
  {
    cat: 'gestures',
    ionic: '<ion-infinite-scroll>\n  ...\n</ion-infinite-scroll>',
    android: 'Bắt sự kiện phần tử cuối của LazyListState',
    ios: 'Bắt sự kiện .onAppear của phần tử cuối danh sách',
    note: 'Tải thêm dữ liệu khi cuộn gần đến đáy'
  },
  {
    cat: 'gestures',
    ionic: '<ion-fab>\n  <ion-fab-button>...</ion-fab-button>\n</ion-fab>',
    android: 'FloatingActionButton(onClick = { }) { }',
    ios: '.overlay(alignment: .bottomTrailing) { Button... }',
    note: 'Nút tròn nổi thao tác nhanh ở góc màn hình'
  },
  {
    cat: 'gestures',
    ionic: '<ion-segment>\n  <ion-segment-button>...</ion-segment-button>\n</ion-segment>',
    android: 'TabRow { Tab(...) } / SegmentedButton',
    ios: 'Picker("...", selection: $sel) { }.pickerStyle(.segmented)',
    note: 'Thanh phân đoạn chọn nhóm nội dung'
  },
  {
    cat: 'gestures',
    ionic: '<ion-accordion-group>\n  <ion-accordion>...</ion-accordion>\n</ion-accordion-group>',
    android: 'Column toggle visibility bằng animation',
    ios: 'DisclosureGroup("Tiêu đề", isExpanded: $open) { }',
    note: 'Khối nội dung đóng/mở dạng accordion có hiệu ứng trượt'
  }
]

function getCategoryName(catId: string): string {
  const found = categories.find(c => c.id === catId)
  return found ? found.name : catId
}

function getCount(catId: string): number {
  if (catId === 'all') return rawData.length
  return rawData.filter(item => item.cat === catId).length
}

const filteredList = computed(() => {
  let list = rawData

  if (selectedCategory.value !== 'all') {
    list = list.filter(item => item.cat === selectedCategory.value)
  }

  if (searchQuery.value.trim()) {
    const q = searchQuery.value.toLowerCase().trim()
    list = list.filter(item =>
      item.ionic.toLowerCase().includes(q) ||
      item.android.toLowerCase().includes(q) ||
      item.ios.toLowerCase().includes(q) ||
      item.note.toLowerCase().includes(q)
    )
  }

  return list
})
</script>

<style scoped>
.ionic-mapping-container {
  margin: 1.5rem 0;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

/* Toolbar */
.mapping-toolbar {
  display: flex;
  gap: 0.75rem;
  align-items: center;
  flex-wrap: wrap;
}

.search-wrapper {
  position: relative;
  flex: 1;
  min-width: 260px;
}

.search-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 0.9rem;
  opacity: 0.7;
}

.search-input {
  width: 100%;
  padding: 8px 32px 8px 36px;
  border-radius: 8px;
  border: 1px solid var(--vp-c-border);
  background: var(--vp-c-bg-soft);
  color: var(--vp-c-text-1);
  font-size: 0.9rem;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.search-input:focus {
  outline: none;
  border-color: var(--vp-c-brand-1);
  box-shadow: 0 0 0 3px var(--vp-c-brand-soft);
}

.clear-btn {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  color: var(--vp-c-text-3);
  cursor: pointer;
  padding: 2px 6px;
  font-size: 0.85rem;
}

.view-toggle {
  display: flex;
  background: var(--vp-c-bg-soft);
  border: 1px solid var(--vp-c-border);
  border-radius: 8px;
  padding: 2px;
}

.toggle-btn {
  padding: 6px 12px;
  font-size: 0.82rem;
  font-weight: 600;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: var(--vp-c-text-2);
  cursor: pointer;
  transition: all 0.2s;
}

.toggle-btn.active {
  background: var(--vp-c-bg);
  color: var(--vp-c-brand-1);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

/* Category Tabs */
.category-tabs {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.cat-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 9999px;
  border: 1px solid var(--vp-c-border);
  background: var(--vp-c-bg-soft);
  color: var(--vp-c-text-2);
  font-size: 0.82rem;
  cursor: pointer;
  transition: all 0.2s;
}

.cat-tab:hover {
  border-color: var(--vp-c-brand-1);
  color: var(--vp-c-text-1);
}

.cat-tab.active {
  background: var(--vp-c-brand-1);
  color: #ffffff;
  border-color: var(--vp-c-brand-1);
}

.cat-count {
  font-size: 0.72rem;
  padding: 1px 6px;
  border-radius: 9999px;
  background: rgba(0, 0, 0, 0.08);
  font-weight: 600;
}

.cat-tab.active .cat-count {
  background: rgba(255, 255, 255, 0.25);
  color: #ffffff;
}

/* Cards Grid */
.cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 1rem;
}

.mapping-card {
  background: var(--vp-c-bg-soft);
  border: 1px solid var(--vp-c-border);
  border-radius: 12px;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  transition: transform 0.15s, border-color 0.2s, box-shadow 0.2s;
}

.mapping-card:hover {
  border-color: var(--vp-c-brand-1);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 0.5rem;
  border-bottom: 1px dashed var(--vp-c-border);
}

.ionic-tag {
  display: flex;
  align-items: center;
  gap: 6px;
}

.ionic-tag code {
  font-weight: 700;
  font-size: 0.88rem;
  color: #3880ff;
  background: rgba(56, 128, 255, 0.1);
  padding: 4px 8px;
  border-radius: 6px;
  white-space: pre-wrap;
  display: inline-block;
  line-height: 1.35;
  font-family: var(--vp-font-family-mono);
}

.category-pill {
  font-size: 0.72rem;
  color: var(--vp-c-text-3);
  background: var(--vp-c-bg-mute);
  padding: 2px 8px;
  border-radius: 6px;
}

.card-body {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.platform-col {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.platform-label {
  display: flex;
  align-items: center;
}

.code-box {
  background: var(--vp-c-bg);
  border: 1px solid var(--vp-c-border);
  border-radius: 6px;
  padding: 6px 10px;
  font-size: 0.85rem;
  line-height: 1.4;
  overflow-x: auto;
}

.code-box code {
  white-space: pre-wrap;
  word-break: break-word;
  color: var(--vp-c-text-1);
}

.card-footer {
  margin-top: auto;
  padding-top: 0.5rem;
  border-top: 1px solid var(--vp-c-border);
  display: flex;
  gap: 6px;
  font-size: 0.8rem;
  color: var(--vp-c-text-2);
  line-height: 1.4;
}

.note-icon {
  flex-shrink: 0;
}

/* Table View */
.table-wrapper {
  overflow-x: auto;
  border: 1px solid var(--vp-c-border);
  border-radius: 10px;
  background: var(--vp-c-bg);
}

.mapping-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.88rem;
  margin: 0 !important;
}

.mapping-table th {
  background: var(--vp-c-bg-soft);
  color: var(--vp-c-text-1);
  font-weight: 700;
  padding: 10px 14px;
  border-bottom: 1px solid var(--vp-c-border);
  text-align: left;
}

.mapping-table td {
  padding: 10px 14px;
  border-bottom: 1px solid var(--vp-c-border);
  vertical-align: top;
}

.mapping-table tr:last-child td {
  border-bottom: none;
}

.mapping-table tr:hover {
  background: var(--vp-c-bg-soft);
}

.td-ionic code {
  font-weight: 700;
  color: #3880ff;
  background: rgba(56, 128, 255, 0.1);
  padding: 4px 8px;
  border-radius: 6px;
  white-space: pre-wrap;
  display: inline-block;
  line-height: 1.35;
  font-family: var(--vp-font-family-mono);
}

.ionic-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.sub-cat {
  font-size: 0.72rem;
  color: var(--vp-c-text-3);
}

.td-android code {
  color: #10b981;
  font-size: 0.85rem;
}

.td-ios code {
  color: #3b82f6;
  font-size: 0.85rem;
}

.td-note {
  margin-top: 6px;
  font-size: 0.78rem;
  color: var(--vp-c-text-2);
  line-height: 1.35;
}

.note-bullet {
  opacity: 0.8;
}

/* Empty State */
.empty-state {
  text-align: center;
  padding: 3rem 1rem;
  background: var(--vp-c-bg-soft);
  border-radius: 12px;
  border: 1px dashed var(--vp-c-border);
}

.reset-btn {
  margin-top: 0.75rem;
  padding: 6px 14px;
  border-radius: 6px;
  background: var(--vp-c-brand-1);
  color: #ffffff;
  border: none;
  cursor: pointer;
  font-size: 0.85rem;
  font-weight: 600;
}

.mapping-footer-info {
  font-size: 0.82rem;
  color: var(--vp-c-text-3);
  text-align: right;
}
</style>
