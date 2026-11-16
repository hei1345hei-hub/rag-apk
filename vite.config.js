import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

/**
 * rpx 单位转换插件
 * 普通 Vite 构建不会处理 uni-app 的 rpx，浏览器会把这些声明当非法值丢弃，
 * 导致整页排版失效。这里按 750 设计稿、375px 基准换算为 px（1rpx = 0.5px），
 * 不使用 calc() 乘除，兼容老版本 Android WebView。
 */
const rpxToPx = {
  postcssPlugin: 'rag-rpx-to-px',
  Declaration(decl) {
    if (decl.value && decl.value.includes('rpx')) {
      decl.value = decl.value.replace(/(-?\d*\.?\d+)rpx/g, (_, n) => `${+(parseFloat(n) / 2).toFixed(3)}px`)
    }
  },
  AtRule(atRule) {
    if (atRule.params && atRule.params.includes('rpx')) {
      atRule.params = atRule.params.replace(/(-?\d*\.?\d+)rpx/g, (_, n) => `${+(parseFloat(n) / 2).toFixed(3)}px`)
    }
  }
}

/**
 * uni-app 事件语法兼容插件
 * 项目用纯 Vue3 + Vite 构建，没有 uni-app 运行时：
 * @tap 会被编译成名为 tap 的原生事件，浏览器/WebView 中永远不会触发，
 * 导致所有按钮、卡片点击失效。这里在编译前统一替换为 @click。
 */
function tapToClick() {
  return {
    name: 'rag-tap-to-click',
    enforce: 'pre',
    transform(code, id) {
      if (!id.split('?')[0].endsWith('.vue')) return null
      if (!code.includes('@tap')) return null
      return { code: code.replace(/@tap/g, '@click'), map: null }
    }
  }
}

/**
 * 单文件打包插件
 * 把 JS / CSS 全部内联进 index.html，避免 Android WebView 通过
 * shouldInterceptRequest 加载子资源时出现 ES Module / CORS 失败，
 * 最终产物只有一个 HTML，页面零外部请求。
 */
function inlineSingleFile() {
  return {
    name: 'rag-inline-single-file',
    enforce: 'post',
    apply: 'build',
    generateBundle(_, bundle) {
      const htmlKey = Object.keys(bundle).find((key) => key.endsWith('.html'))
      if (!htmlKey) return
      const htmlAsset = bundle[htmlKey]

      let jsCode = ''
      let cssCode = ''
      for (const key of Object.keys(bundle)) {
        const item = bundle[key]
        if (item.type === 'chunk' && item.isEntry) {
          jsCode += item.code
          delete bundle[key]
        } else if (item.type === 'asset' && key.endsWith('.css')) {
          cssCode += String(item.source)
          delete bundle[key]
        }
      }

      let source = String(htmlAsset.source)
      source = source
        .replace(/<script[^>]*src="[^"]*"[^>]*><\/script>/g, '')
        .replace(/<link[^>]*rel="stylesheet"[^>]*>/g, '')

      if (cssCode) {
        source = source.replace('</head>', `<style>${cssCode}</style>\n</head>`)
      }
      if (jsCode) {
        source = source.replace(
          '</body>',
          `<script>${jsCode.replace(/<\/script/gi, '<\\/script')}</script>\n</body>`
        )
      }
      htmlAsset.source = source
    }
  }
}

export default defineConfig({
  plugins: [
    vue({
      template: {
        compilerOptions: {
          isCustomElement: (tag) => ['view', 'text', 'scroll-view'].includes(tag)
        }
      }
    }),
    tapToClick(),
    inlineSingleFile()
  ],
  css: {
    postcss: {
      plugins: [rpxToPx]
    }
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './'),
      '@dcloudio/uni-app': path.resolve(__dirname, './utils/uni-shim.js')
    }
  },
  base: './',
  build: {
    outDir: 'dist',
    emptyOutDir: true,
    target: 'es2015',
    minify: 'esbuild',
    // 图片等静态资源全部内联为 base64，保证单文件产物没有任何外部引用
    assetsInlineLimit: 500000,
    cssCodeSplit: false,
    modulePreload: false,
    rollupOptions: {
      output: {
        format: 'iife',
        name: 'RagInterviewApp',
        inlineDynamicImports: true
      }
    }
  }
})
