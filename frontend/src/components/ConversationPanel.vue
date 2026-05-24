<template>
    <div class="conversation-panel">
      <div class="messages">
        <div v-for="(msg, idx) in messages" :key="idx" :class="['message', msg.role]">
          <div class="role-label">{{ msg.role === 'user' ? '你' : 'AI' }}</div>
  
          <!-- 加载动画：独立于内容区域，只要 loading 为 true 就显示 -->
          <div v-if="msg.loading" class="loading">
            <span class="spinner"></span> AI 正在分析白板内容...
          </div>
  
          <!-- 正常内容：非加载状态时显示 -->
          <div v-else class="content">
            <template v-if="msg.role === 'assistant'">
              <AICard v-if="msg.type" :content="msg.content" :type="msg.type" />
              <div v-else v-html="renderMarkdown(msg.content)"></div>
            </template>
            <template v-else>
              {{ msg.content }}
            </template>
          </div>
        </div>
      </div>
      <div class="input-area">
        <input v-model="inputText" @keyup.enter="send" placeholder="输入追问..." />
        <button @click="send">发送</button>
      </div>
    </div>
  </template>
  
  <script setup>
  import { ref } from 'vue'
  import { marked } from 'marked'
  import AICard from './AICard.vue'
  
  const props = defineProps({
    messages: { type: Array, default: () => [] }
  })
  const emit = defineEmits(['send'])
  const inputText = ref('')
  
  function renderMarkdown(text) {
    if (!text) return ''
    return marked.parse(text)
  }
  
  function send() {
    if (inputText.value.trim()) {
      emit('send', inputText.value)
      inputText.value = ''
    }
  }
  </script>
  
  <style scoped>
  .conversation-panel {
    display: flex; flex-direction: column; height: 100%;
    border-left: 2px solid #ccc; padding: 10px; background: #f9f9f9;
  }
  .messages { flex: 1; overflow-y: auto; margin-bottom: 10px; }
  .message { margin-bottom: 12px; padding: 8px; border-radius: 8px; }
  .message.user { background: #e3f2fd; }
  .message.assistant { background: #fff; border: 1px solid #eee; }
  .role-label { font-weight: bold; font-size: 12px; margin-bottom: 4px; }
  .content { margin-top: 4px; }
  
  .loading {
    color: #555; font-style: italic; display: flex; align-items: center; gap: 6px;
  }
  .spinner {
    display: inline-block; width: 14px; height: 14px;
    border: 2px solid #ccc; border-top-color: #2196f3;
    border-radius: 50%; animation: spin 0.6s linear infinite;
  }
  @keyframes spin { to { transform: rotate(360deg); } }
  
  .input-area { display: flex; gap: 5px; }
  .input-area input { flex: 1; padding: 8px; border: 1px solid #ccc; border-radius: 4px; }
  .input-area button { padding: 8px 16px; }
  </style>