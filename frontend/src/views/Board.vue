<template>
  <div class="board-container">
    <div class="canvas-area" :style="{ background: videoVisible ? '#000' : '#fff' }">
      <video
        ref="videoRef"
        autoplay
        playsinline
        style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; object-fit: cover; transform: scaleX(-1); z-index: 1;"
        :style="{ opacity: videoVisible ? 1 : 0 }"
      ></video>
      <canvas
        ref="canvasRef"
        style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; z-index: 10; background: transparent;"
      ></canvas>

      <div
        v-if="selection && selectionVisible"
        class="selection-box"
        :style="selectionBoxStyle"
      ></div>
    </div>

    <div class="controls">
      <button @click="handleClear">清屏</button>
      <input type="color" v-model="penColor" @change="setColor(penColor)" />
      <label>笔宽：<input type="range" min="1" max="10" v-model.number="penWidth" @change="setLineWidth(penWidth)" /></label>

      <button :class="{ active: currentBrush === 'pen' }" @click="currentBrush = 'pen'">🖊 钢笔</button>
      <button :class="{ active: currentBrush === 'highlighter' }" @click="currentBrush = 'highlighter'">🖍 荧光笔</button>

      <!-- 摄像头切换按钮 -->
      <button @click="videoVisible = !videoVisible">
        {{ videoVisible ? '📷 隐藏摄像头' : '📷 显示摄像头' }}
      </button>

      <select v-model="whiteboardType">
        <option value="general">通用</option>
        <option value="math">数学</option>
        <option value="code">代码</option>
        <option value="physics">物理</option>
      </select>
      <button @click="sendSelection" :disabled="!selection || sending">
        {{ sending ? '分析中...' : '圈选发送 AI' }}
      </button>
      <button @click="endClassroom" :disabled="sending" style="margin-left: auto; background: #ff5252; color: white; border: none;">
        结束课堂
      </button>
      <span>手势状态：{{ isPinching ? '书写中' : '未捏合' }}</span>
      <span v-if="selection">已框选区域</span>
    </div>
    <div class="workspace">
      <div class="canvas-wrapper"></div>
      <ConversationPanel :messages="messages" @send="handleFollowUp" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useHandGesture } from '../composables/useHandGesture'
import { api } from '../services/api'
import ConversationPanel from '../components/ConversationPanel.vue'
import { compressBase64Image } from '../utils/imageCompress'

const route = useRoute()
const router = useRouter()
const classroomId = route.params.classroomId

const videoRef = ref(null)
const canvasRef = ref(null)
const penColor = ref('#000000')
const penWidth = ref(4)
const whiteboardType = ref('general')
const sending = ref(false)
const messages = ref([])
const videoVisible = ref(false)   // 摄像头默认关闭

const currentBrush = ref('pen')
const brushStyles = computed(() => ({
  pen: { lineWidth: penWidth.value, globalAlpha: 1 },
  highlighter: { lineWidth: penWidth.value * 5, globalAlpha: 1 }
}))

let isMouseDrawing = false
let lastMouseX = 0
let lastMouseY = 0
let currentMouseStroke = null

const {
  isPinching,
  init,
  stop,
  clearCanvas,
  setColor,
  setLineWidth,
  strokes,
  clearStrokes
} = useHandGesture(videoRef, canvasRef, { alpha: 0.3 })

let selection = ref(null)
let selectionVisible = ref(false)
let isSelecting = false
let selectStart = { x: 0, y: 0 }

const selectionBoxStyle = computed(() => {
  if (!selection.value) return {}
  const rect = canvasRef.value?.getBoundingClientRect()
  if (!rect) return {}
  const scaleX = rect.width / (canvasRef.value.width || 1)
  const scaleY = rect.height / (canvasRef.value.height || 1)
  return {
    left: selection.value.x * scaleX + 'px',
    top: selection.value.y * scaleY + 'px',
    width: selection.value.w * scaleX + 'px',
    height: selection.value.h * scaleY + 'px'
  }
})

function hexToRgba(hex, alpha) {
  const r = parseInt(hex.slice(1,3), 16)
  const g = parseInt(hex.slice(3,5), 16)
  const b = parseInt(hex.slice(5,7), 16)
  return `rgba(${r},${g},${b},${alpha})`
}

function onMouseDown(e) {
  const rect = canvasRef.value.getBoundingClientRect()
  const scaleX = canvasRef.value.width / rect.width
  const scaleY = canvasRef.value.height / rect.height
  const x = (e.clientX - rect.left) * scaleX
  const y = (e.clientY - rect.top) * scaleY

  if (e.shiftKey) {
    isSelecting = true
    selectStart = { x, y }
    selection.value = null
    selectionVisible.value = true
    return
  }

  isMouseDrawing = true
  lastMouseX = x
  lastMouseY = y

  currentMouseStroke = { points: [{ x, y, time: Date.now() }] }
  strokes.value.push(currentMouseStroke)
}

function onMouseMove(e) {
  const rect = canvasRef.value.getBoundingClientRect()
  const scaleX = canvasRef.value.width / rect.width
  const scaleY = canvasRef.value.height / rect.height
  const x = (e.clientX - rect.left) * scaleX
  const y = (e.clientY - rect.top) * scaleY

  if (isSelecting) {
    selection.value = {
      x: Math.min(selectStart.x, x),
      y: Math.min(selectStart.y, y),
      w: Math.abs(x - selectStart.x),
      h: Math.abs(y - selectStart.y)
    }
    return
  }

  if (isMouseDrawing) {
    const ctx = canvasRef.value.getContext('2d')
    const style = brushStyles.value[currentBrush.value]
    ctx.beginPath()
    ctx.lineWidth = style.lineWidth

    if (currentBrush.value === 'highlighter') {
      ctx.strokeStyle = hexToRgba(penColor.value, 0.3)
      ctx.globalAlpha = 1
      ctx.lineCap = 'butt'
    } else {
      ctx.strokeStyle = penColor.value
      ctx.globalAlpha = 1
      ctx.lineCap = 'round'
    }
    ctx.moveTo(lastMouseX, lastMouseY)
    ctx.lineTo(x, y)
    ctx.stroke()
    lastMouseX = x
    lastMouseY = y

    if (currentMouseStroke) {
      currentMouseStroke.points.push({ x, y, time: Date.now() })
    }
  }
}

function onMouseUp() {
  if (isSelecting) {
    isSelecting = false
    selectionVisible.value = false
  }
  if (isMouseDrawing) {
    isMouseDrawing = false
    currentMouseStroke = null
  }
}

function handleClear() {
  clearCanvas()
  const canvas = canvasRef.value
  if (canvas) {
    const ctx = canvas.getContext('2d')
    ctx.fillStyle = '#fff'
    ctx.fillRect(0, 0, canvas.width, canvas.height)
  }
  currentMouseStroke = null
  isMouseDrawing = false
}

onMounted(() => {
  init()
  const canvas = canvasRef.value
  canvas.addEventListener('mousedown', onMouseDown)
  canvas.addEventListener('mousemove', onMouseMove)
  canvas.addEventListener('mouseup', onMouseUp)

  // 初始白色背景（因为 useHandGesture 未填充）
  const ctx = canvas.getContext('2d')
  ctx.fillStyle = '#fff'
  ctx.fillRect(0, 0, canvas.width, canvas.height)
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

  let base64 = cropCanvas.toDataURL('image/png').replace('data:image/png;base64,', '')

  try {
    base64 = await compressBase64Image(base64, 800, 0.7)
  } catch (e) {
    console.warn('图片压缩失败，将使用原图', e)
  }

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
      let displayContent = res.data.data.content

      try {
        const parsed = JSON.parse(displayContent)
        if (parsed.recognized_text) {
          let formatted = `**识别结果**：${parsed.recognized_text}`
          if (parsed.confidence != null) {
            formatted += `\n\n**置信度**：${Math.round(parsed.confidence * 100)}%`
          }
          if (parsed.structured_content?.body) {
            formatted += `\n\n**内容**：${parsed.structured_content.body}`
          }
          if (parsed.ambiguous_spots?.length > 0) {
            formatted += `\n\n**模糊点**：${parsed.ambiguous_spots.join(', ')}`
          }
          displayContent = formatted
        }
      } catch (e) {}

      messages.value.push({
        role: 'assistant',
        success: true,
        content: displayContent,
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

async function endClassroom() {
  if (strokes.value.length === 0) {
    alert('没有书写记录，无法保存')
    return
  }
  try {
    await api.post(`/api/classrooms/${classroomId}/strokes`, {
      strokes: strokes.value
    })
    alert('课堂已结束，笔迹已保存')
    router.push('/dashboard')
  } catch (err) {
    alert('保存笔迹失败：' + (err.response?.data?.message || err.message))
  }
}
</script>

<style scoped>
.board-container { display: flex; flex-direction: column; height: 100vh; }
.workspace { display: flex; flex: 1; overflow: hidden; }
.canvas-wrapper { flex: 1; position: relative; }
.canvas-area {
  position: relative;
  flex-shrink: 0;
  height: 60vh;
}
.controls { padding: 10px; display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }

.controls button.active {
  background: #2196f3;
  color: white;
  border-color: #2196f3;
  transform: scale(1.05);
  transition: all 0.2s;
}
.controls button {
  padding: 4px 12px;
  border: 1px solid #ccc;
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
  transition: all 0.2s;
}

.selection-box {
  position: absolute;
  border: 2px dashed #3b82f6;
  animation: breathe 1.5s ease-in-out infinite;
  pointer-events: none;
  z-index: 20;
}
@keyframes breathe {
  0%, 100% {
    border-color: #3b82f6;
    box-shadow: 0 0 4px rgba(59,130,246,0.3);
  }
  50% {
    border-color: #93c5fd;
    box-shadow: 0 0 12px rgba(59,130,246,0.6);
  }
}
</style>