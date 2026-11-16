import { createApp, h } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './utils/uni-shim'

const app = createApp(App)
const pinia = createPinia()

// 移动端组件兼容映射
app.component('rich-text', {
  props: ['nodes'],
  render() {
    return h('span', {
      class: 'u-rich-text',
      innerHTML: this.nodes || ''
    })
  }
})

app.component('picker', {
  props: ['range', 'value'],
  emits: ['change'],
  render() {
    const options = (this.range || []).map((item, idx) =>
      h('option', { value: idx }, item)
    )
    const select = h(
      'select',
      {
        style: {
          position: 'absolute',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          width: '100%',
          height: '100%',
          opacity: 0,
          zIndex: 10,
          cursor: 'pointer'
        },
        value: this.value || 0,
        onChange: (e) => {
          this.$emit('change', { detail: { value: Number(e.target.selectedIndex) } })
        }
      },
      options
    )
    return h(
      'div',
      { style: { position: 'relative', width: '100%', display: 'block' } },
      [this.$slots.default ? this.$slots.default() : null, select]
    )
  }
})

app.use(pinia)
app.use(router)

app.mount('#app')
