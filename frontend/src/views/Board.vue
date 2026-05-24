<template>
  <div class="board-container">
    <div class="canvas-area">
      <video ref="videoRef" autoplay playsinline style="transform: scaleX(-1); width: 100%;"></video>
      <canvas ref="canvasRef" style="position: absolute; top: 0; left: 0; width: 100%; z-index: 10;"></canvas>
    </div>
    <div class="controls">
      <button @click="clearCanvas">清屏</button>
      <input type="color" v-model="penColor" @change="setColor(penColor)" />
      <label>笔宽：<input type="range" min="1" max="10" v-model.number="penWidth" @change="setLineWidth(penWidth)" /></label>
      <select v-model="whiteboardType">
        <option value="general">通用</option>
        <option value="math">数学</option>
        <option value="code">代码</option>
        <option value="physics">物理</option>
      </select>
      <button @click="sendSelection" :disabled="!selection || sending">
        {{ sending ? '分析中...' : '圈选发送 AI' }}
      </button>
      <span>手势状态：{{ isPinching ? '书写中' : '未捏合' }}</span>
      <span v-if="selection">已框选区域</span>
    </div>
    <div class="workspace">
      <div class="canvas-wrapper">
        <!-- Canvas 区域在上面 -->
      </div>
      <ConversationPanel :messages="messages" @send="handleFollowUp" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { useHandGesture } from '../composables/useHandGesture'
import { api } from '../services/api'
import ConversationPanel from '../components/ConversationPanel.vue'

const route = useRoute()
const classroomId = route.params.classroomId

const videoRef = ref(null)
const canvasRef = ref(null)
const penColor = ref('#000000')
const penWidth = ref(4)
const whiteboardType = ref('general')
const sending = ref(false)
const messages = ref([])
// 鼠标绘制状态
let isMouseDrawing = false
let lastMouseX = 0
let lastMouseY = 0

const { isPinching, init, stop, clearCanvas, setColor, setLineWidth } = useHandGesture(videoRef, canvasRef, { alpha: 0.3 })

// 框选相关
let selection = ref(null)
let isSelecting = false
let selectStart = { x: 0, y: 0 }

function onMouseDown(e) {
  const rect = canvasRef.value.getBoundingClientRect()
  const scaleX = canvasRef.value.width / rect.width
  const scaleY = canvasRef.value.height / rect.height
  const x = (e.clientX - rect.left) * scaleX
  const y = (e.clientY - rect.top) * scaleY

  if (e.shiftKey) {
    // Shift 按下 → 框选模式
    isSelecting = true
    selectStart = { x, y }
    selection.value = null
    return
  }

  // 未按 Shift → 鼠标书写模式
  isMouseDrawing = true
  lastMouseX = x
  lastMouseY = y
}

function onMouseMove(e) {
  const rect = canvasRef.value.getBoundingClientRect()
  const scaleX = canvasRef.value.width / rect.width
  const scaleY = canvasRef.value.height / rect.height
  const x = (e.clientX - rect.left) * scaleX
  const y = (e.clientY - rect.top) * scaleY

  if (isSelecting) {
    // 框选模式
    selection.value = {
      x: Math.min(selectStart.x, x),
      y: Math.min(selectStart.y, y),
      w: Math.abs(x - selectStart.x),
      h: Math.abs(y - selectStart.y)
    }
    return
  }

  if (isMouseDrawing) {
    // 鼠标书写模式
    const ctx = canvasRef.value.getContext('2d')
    ctx.beginPath()
    ctx.strokeStyle = penColor.value
    ctx.lineWidth = penWidth.value
    ctx.lineCap = 'round'
    ctx.moveTo(lastMouseX, lastMouseY)
    ctx.lineTo(x, y)
    ctx.stroke()
    lastMouseX = x
    lastMouseY = y
  }
}

function onMouseUp() {
  if (isSelecting) {
    isSelecting = false
  }
  if (isMouseDrawing) {
    isMouseDrawing = false
  }
}

// 绑定框选事件
onMounted(() => {
  init()
  const canvas = canvasRef.value
  canvas.addEventListener('mousedown', onMouseDown)
  canvas.addEventListener('mousemove', onMouseMove)
  canvas.addEventListener('mouseup', onMouseUp)
})

onUnmounted(() => {
  stop()
  const canvas = canvasRef.value
  if (canvas) {
    canvas.removeEventListener('mousedown', onMouseDown)
    canvas.removeEventListener('mousemove', onMouseMove)
    canvas.removeEventListener('mouseup', onMouseUp)
  }
})

// 发送圈选区域给 AI
async function sendSelection() {
  if (!selection.value || selection.value.w < 10 || selection.value.h < 10) {
    alert('请先按住 Shift 键并拖动鼠标框选一个区域')
    return
  }
  const sel = selection.value
  const cropCanvas = document.createElement('canvas')
  cropCanvas.width = sel.w
  cropCanvas.height = sel.h
  const cropCtx = cropCanvas.getContext('2d')
  cropCtx.drawImage(canvasRef.value, sel.x, sel.y, sel.w, sel.h, 0, 0, sel.w, sel.h)
  const base64 = cropCanvas.toDataURL('image/png').replace('data:image/png;base64,', '')

  sending.value = true
  messages.value.push({ role: 'user', content: '[发送了白板截图]' })
  messages.value.push({ role: 'assistant', content: '', loading: true })

  try {
    const res = await api.post(`/api/classrooms/${classroomId}/analyze`, {
      image_base64: base64,
      whiteboard_type: whiteboardType.value,
      context: ''
    })
    messages.value = messages.value.filter(m => !m.loading)
    if (res.data.code === 200 && res.data.data.success) {
      messages.value.push({
        role: 'assistant',
        success: true,
        content: res.data.data.content,
        type: whiteboardType.value,
        costTime: res.data.data.costTime || 0,
        modelUsed: res.data.data.modelUsed || '未知模型'
      })
    } else {
      messages.value.push({
        role: 'assistant',
        success: false,
        errorMsg: res.data.data?.errorMsg || 'AI 分析失败',
        content: '',
        type: whiteboardType.value
      })
    }
  } catch (err) {
    messages.value = messages.value.filter(m => !m.loading)
    messages.value.push({
      role: 'assistant',
      success: false,
      errorMsg: '网络错误，请重试',
      content: ''
    })
  } finally {
    sending.value = false
    selection.value = null
  }
}

// 追问
async function handleFollowUp(text) {
  messages.value.push({ role: 'user', content: text })
  messages.value.push({ role: 'assistant', content: '', loading: true })

  try {
    const res = await api.post(`/api/classrooms/${classroomId}/conversation`, { message: text })
    messages.value = messages.value.filter(m => !m.loading)
    if (res.data.code === 200) {
      messages.value.push({
        role: 'assistant',
        success: true,
        content: res.data.data.content,
        type: 'general',
        costTime: 0,
        modelUsed: ''
      })
    } else {
      messages.value.push({
        role: 'assistant',
        success: false,
        errorMsg: 'AI 对话失败',
        content: ''
      })
    }
  } catch (err) {
    messages.value = messages.value.filter(m => !m.loading)
    messages.value.push({
      role: 'assistant',
      success: false,
      errorMsg: '网络错误，请重试',
      content: ''
    })
  }
}
</script>

<style scoped>
.board-container { display: flex; flex-direction: column; height: 100vh; }
.workspace { display: flex; flex: 1; overflow: hidden; }
.canvas-wrapper { flex: 1; position: relative; }
.canvas-area { position: relative; flex-shrink: 0; }
.controls { padding: 10px; display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
</style>