import { defineConfig } from 'vitepress'
import { withMermaid } from 'vitepress-plugin-mermaid'

const isExternal = process.argv.includes('--external')

export default withMermaid(
  defineConfig({
    title: 'Native Mobile Learning Plan',
    description: 'Tài liệu hướng dẫn và lộ trình học lập trình Native Mobile (Kotlin + Swift) kèm tài liệu tra cứu chính thức Android & iOS',
    lang: 'vi',
    lastUpdated: true,
    cleanUrls: true,
    vite: {
      server: {
        host: isExternal ? '0.0.0.0' : 'localhost',
        port: 5173
      },
      optimizeDeps: {
        include: [
          'fastdom',
          'mermaid',
          'dayjs',
          'debug'
        ]
      }
    },
  head: [
    ['link', { rel: 'icon', href: '/logo.svg' }],
    ['meta', { name: 'theme-color', content: '#6366f1' }]
  ],
  themeConfig: {
    siteTitle: 'Native Mobile Plan',
    nav: [
      { text: 'Trang chủ', link: '/', activeMatch: '^/$' },
      { text: 'Tổng quan', link: '/guide/overview', activeMatch: '^/guide/' },
      { text: 'Kiến thức (Modules)', link: '/modules/ui', activeMatch: '^/modules/' },
      { text: 'Lộ trình tuần', link: '/schedule/', activeMatch: '^/schedule/(?!exercises)' },
      { text: 'Bài tập', link: '/schedule/exercises', activeMatch: '^/schedule/exercises' },
      { text: 'Tra cứu (Refs)', link: '/references/comparison', activeMatch: '^/references/' }
    ],
    sidebar: {
      '/guide/': [
        {
          text: 'Hướng dẫn chung',
          items: [
            { text: 'Mục tiêu & Phạm vi', link: '/guide/overview' },
            { text: 'Kiến trúc Native Core', link: '/guide/architecture' }
          ]
        }
      ],
      '/modules/': [
        {
          text: 'Part I — Knowledge Modules',
          items: [
            { text: '1. Native UI & Async Update', link: '/modules/ui' },
            { text: '2. Permission Manager', link: '/modules/permission' },
            { text: '3. Bluetooth BLE Manager', link: '/modules/bluetooth' },
            { text: '4. Camera & Media Manager', link: '/modules/media' },
            { text: '5. HTTP API Client', link: '/modules/api' },
            { text: '6. Background Device Monitor', link: '/modules/background' }
          ]
        }
      ],
      '/schedule/': [
        {
          text: 'Part II — Lộ trình thực hành',
          items: [
            { text: 'Tổng quan lộ trình & Ưu tiên', link: '/schedule/' },
            { text: 'Tuần 1: Fundamentals & UI', link: '/schedule/week-1' },
            { text: 'Tuần 2: Native APIs & BLE Flow', link: '/schedule/week-2' },
            { text: 'Tuần 3: NativeCore & Background', link: '/schedule/week-3' },
            { text: 'Tuần 4: Testing & Refactoring', link: '/schedule/week-4' },
            { text: '📝 Tổng hợp 17 Bài tập thực hành', link: '/schedule/exercises' }
          ]
        }
      ],
      '/references/': [
        {
          text: 'Trung tâm tra cứu (References)',
          items: [
            { text: 'Bảng đối chiếu Android vs iOS', link: '/references/comparison' },
            { text: 'Android Reference Docs', link: '/references/android-ref' },
            { text: 'iOS Reference Docs', link: '/references/ios-ref' }
          ]
        }
      ]
    },
    search: {
      provider: 'local',
      options: {
        translations: {
          button: {
            buttonText: 'Tìm kiếm',
            buttonAriaLabel: 'Tìm kiếm tài liệu'
          },
          modal: {
            noResultsText: 'Không tìm thấy kết quả cho',
            resetButtonTitle: 'Xoá tìm kiếm',
            footer: {
              selectText: 'chọn',
              navigateText: 'di chuyển',
              closeText: 'đóng'
            }
          }
        }
      }
    },
    docFooter: {
      prev: 'Trang trước',
      next: 'Trang tiếp theo'
    },
    outline: {
      label: 'Mục lục trang',
      level: [2, 3]
    },
    footer: {
      message: 'Native Mobile Development Learning Plan (Kotlin + Swift) — Team CarMDConnect',
      copyright: 'Bản quyền tài liệu nội bộ © 2026'
    }
  }
})
)
