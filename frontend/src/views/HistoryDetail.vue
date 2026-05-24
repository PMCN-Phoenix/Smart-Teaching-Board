<template>
  <div class="history-container">
    <h1>课堂历史记录</h1>
    
    <div class="records">
      <h2>分析记录</h2>
      <div v-for="record in records" :key="record.id" class="record-card">
        <AICard :content="record.aiResult" :type="record.whiteboardType" />
        <small>{{ new Date(record.createdAt).toLocaleString() }}</small>
      </div>
      <p v-if="records.length === 0">暂无分析记录</p>
    </div>

    <div class="messages">
      <h2>对话记录</h2>
      <div v-for="msg in messages" :key="msg.id" :class="['msg', msg.role]">
        <strong>{{ msg.role === 'user' ? '你' : 'AI' }}：</strong>
        <span v-if="msg.role === 'user'">{{ msg.content }}</span>
        <AICard v-else :content="msg.content" type="general" />
      </div>
      <p v-if="messages.length === 0">暂无对话</p>
    </div>

    <!-- 新增：板书回放区域 -->
    <div v-if="strokesJson" class="replay-section">
      <h2>板书回放</h2>
      <StrokeReplay :strokes-json="strokesJson" />
    </div>

    <router-link to="/dashboard">返回课堂列表</router-link>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '../services/api'
import AICard from '../components/AICard.vue'
import StrokeReplay from '../components/StrokeReplay.vue'

const route = useRoute()
const classroomId = route.params.classroomId
const records = ref([])
const messages = ref([])
const strokesJson = ref('')  // 笔迹 JSON 字符串

onMounted(async () => {
  try {
    // 同时请求分析记录、对话记录、笔迹数据
    const [recRes, msgRes, strokesRes] = await Promise.all([
      api.get(`/api/classrooms/${classroomId}/records`),
      api.get(`/api/classrooms/${classroomId}/messages`),
      api.get(`/api/classrooms/${classroomId}/strokes`)
    ])
    if (recRes.data.code === 200) records.value = recRes.data.data
    if (msgRes.data.code === 200) messages.value = msgRes.data.data
    if (strokesRes.data.code === 200) {
      strokesJson.value = strokesRes.data.data || ''
    }
  } catch (e) {
    console.error('加载历史数据失败', e)
  }
})
</script>

<style scoped>
.history-container { padding: 20px; }
.record-card { margin-bottom: 20px; padding: 10px; border: 1px solid #eee; border-radius: 8px; }
.msg { margin-bottom: 10px; }
.replay-section { margin-top: 30px; }
</style>