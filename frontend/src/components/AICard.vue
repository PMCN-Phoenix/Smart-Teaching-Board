<template>
    <div class="ai-card" :class="type">
      <div class="card-header">
        <span class="badge">{{ typeLabel }}</span>
        <button v-if="hasCode" class="copy-btn" @click="copyCode">复制代码</button>
      </div>
      <div class="card-body" ref="bodyRef" v-html="renderedContent"></div>
    </div>
  </template>
  
  <script setup>
  import { computed, ref, onMounted, nextTick } from 'vue'
  import { marked } from 'marked'
  import hljs from 'highlight.js'
  import katex from 'katex'
  import 'highlight.js/styles/github.css'
  import 'katex/dist/katex.min.css'
  
  const props = defineProps({
    content: String,
    type: { type: String, default: 'general' }
  })
  
  const bodyRef = ref(null)
  
  const typeLabel = computed(() => ({
    math: '📐 数学', physics: '⚛️ 物理', code: '💻 代码', general: '📋 通用'
  }[props.type] || '📋 通用'))
  
  const hasCode = computed(() => props.type === 'code' || /```/.test(props.content))
  
  // 渲染 Markdown，并对数学/物理类型进行 KaTeX 公式处理
  const renderedContent = computed(() => {
    let html = marked.parse(props.content)
    if (props.type === 'math' || props.type === 'physics') {
      html = html.replace(/\$\$([^$]+)\$\$/g, (_, formula) => {
        try { return katex.renderToString(formula, { displayMode: true }) } catch { return _ }
      }).replace(/\$([^$]+)\$/g, (_, formula) => {
        try { return katex.renderToString(formula, { displayMode: false }) } catch { return _ }
      })
    }
    return html
  })
  
  // 高亮代码块
  onMounted(async () => {
    await nextTick()
    if (bodyRef.value) {
      bodyRef.value.querySelectorAll('pre code').forEach(block => hljs.highlightElement(block))
    }
  })
  
  function copyCode() {
    const code = props.content.match(/```(?:\w+\n)?([\s\S]*?)```/)?.[1] || ''
    navigator.clipboard.writeText(code)
    alert('代码已复制')
  }
  </script>
  
  <style scoped>
  .ai-card {
    border-radius: 8px; padding: 12px; margin-bottom: 10px;
    background: #fff; border: 1px solid #e8e8e8;
  }
  .ai-card.math, .ai-card.physics { border-left: 4px solid #4caf50; }
  .ai-card.code { border-left: 4px solid #2196f3; }
  .card-header { display: flex; justify-content: space-between; margin-bottom: 8px; }
  .badge { font-size: 12px; color: #666; }
  .copy-btn { font-size: 12px; cursor: pointer; }
  </style>