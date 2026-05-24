<template>
    <div class="ai-card" :class="type">
      <!-- 失败状态 -->
      <div v-if="!success" class="error-card">
        <div class="error-icon">😥</div>
        <h3>识别不太顺利</h3>
        <p>{{ errorMsg || '未知错误' }}</p>
        <div class="hint">您可以尝试：重新框选更清晰的区域、调整光照或换个角度。</div>
      </div>
  
      <!-- 成功状态 -->
      <div v-else>
        <div class="card-header">
          <span class="badge">{{ typeLabel }}</span>
          <button v-if="hasCode" class="copy-btn" @click="copyCode">复制代码</button>
        </div>
        <div class="card-body" ref="bodyRef" v-html="renderedContent"></div>
        <div class="card-footer">
          <span>⏱ 耗时 {{ costTime }} 秒</span>
          <span>🤖 模型：{{ modelUsed }}</span>
        </div>
      </div>
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
    // 新增字段
    success: { type: Boolean, default: true },
    errorMsg: { type: String, default: '' },
    costTime: { type: Number, default: 0 },
    modelUsed: { type: String, default: '' },
    // 原有字段
    content: { type: String, default: '' },
    type: { type: String, default: 'general' }
  })
  
  const bodyRef = ref(null)
  
  const typeLabel = computed(() => ({
    math: '📐 数学', physics: '⚛️ 物理', code: '💻 代码', general: '📋 通用'
  }[props.type] || '📋 通用'))
  
  const hasCode = computed(() => props.type === 'code' || /```/.test(props.content))
  
  const renderedContent = computed(() => {
    let html = marked.parse(props.content || '')
    if (props.type === 'math' || props.type === 'physics') {
      html = html.replace(/\$\$([^$]+)\$\$/g, (_, formula) => {
        try { return katex.renderToString(formula, { displayMode: true }) } catch { return _ }
      }).replace(/\$([^$]+)\$/g, (_, formula) => {
        try { return katex.renderToString(formula, { displayMode: false }) } catch { return _ }
      })
    }
    return html
  })
  
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
  
  /* 失败卡片样式 */
  .error-card { padding: 20px; text-align: center; color: #b91c1c; }
  .error-icon { font-size: 40px; }
  .hint { font-size: 13px; color: #888; margin-top: 10px; }
  
  /* 底部元信息样式 */
  .card-footer {
    display: flex;
    justify-content: space-between;
    padding: 8px 16px;
    margin-top: 10px;
    background: #fafafa;
    border-radius: 0 0 8px 8px;
    font-size: 12px;
    color: #666;
  }
  </style>