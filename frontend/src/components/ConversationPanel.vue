<template>
    <div class="conversation-panel">
      <div class="messages">
        <div v-for="(msg, idx) in messages" :key="idx" :class="['message', msg.role]">
          <div class="role-label">{{ msg.role === 'user' ? '你' : 'AI' }}</div>
  
          <!-- 骨架屏：只要 loading 为 true 就显示 -->
          <div v-if="msg.loading" class="skeleton-card">
            <div class="skeleton-header"></div>
            <div class="skeleton-line"></div>
            <div class="skeleton-line short"></div>
            <div class="skeleton-line"></div>
          </div>
  
          <!-- 正常内容：非加载状态时显示 -->
          <div v-else class="content">
            <template v-if="msg.role === 'assistant'">
              <AICard
                v-if="msg.type"
                :content="msg.content || ''"
                :type="msg.type"
                :success="msg.success !== false"
                :error-msg="msg.errorMsg || ''"
                :cost-time="msg.costTime || 0"
                :model-used="msg.modelUsed || ''"
              />
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
  
  /* 骨架屏样式 */
  .skeleton-card {
    padding: 16px;
    background: #fff;
    border-radius: 12px;
    margin: 10px 0;
  }
  .skeleton-header {
    height: 20px;
    width: 40%;
    background: linear-gradient(90deg, #e0e0e0 25%, #f0f0f0 50%, #e0e0e0 75%);
    background-size: 200% 100%;
    animation: shimmer 1.5s infinite;
    border-radius: 10px;
    margin-bottom: 12px;
  }
  .skeleton-line {
    height: 12px;
    background: linear-gradient(90deg, #e0e0e0 25%, #f0f0f0 50%, #e0e0e0 75%);
    background-size: 200% 100%;
    animation: shimmer 1.5s infinite;
    border-radius: 6px;
    margin-bottom: 8px;
  }
  .skeleton-line.short {
    width: 70%;
  }
  @keyframes shimmer {
    0% { background-position: 200% 0; }
    100% { background-position: -200% 0; }
  }
  
  .input-area { display: flex; gap: 5px; }
  .input-area input { flex: 1; padding: 8px; border: 1px solid #ccc; border-radius: 4px; }
  .input-area button { padding: 8px 16px; }
  </style>