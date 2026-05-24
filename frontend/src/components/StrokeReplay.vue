<template>
    <div class="replay-container">
      <div class="replay-controls">
        <button @click="startReplay" :disabled="playing">▶ 播放</button>
        <button @click="stopReplay" :disabled="!playing">⏹ 停止</button>
        <span v-if="playing">回放中...</span>
      </div>
      <canvas ref="replayCanvas" :width="canvasWidth" :height="canvasHeight" class="replay-canvas"></canvas>
    </div>
  </template>
  
  <script setup>
  import { ref, onMounted, watch } from 'vue'
  
  const props = defineProps({
    strokesJson: { type: String, default: '[]' }
  })
  
  const replayCanvas = ref(null)
  const playing = ref(false)
  const canvasWidth = ref(800)
  const canvasHeight = ref(600)
  
  let ctx = null
  let animationId = null
  let startTime = 0
  let strokes = []
  
  function parseStrokes() {
    try {
      return JSON.parse(props.strokesJson || '[]')
    } catch {
      return []
    }
  }
  
  function getTotalDuration() {
    if (strokes.length === 0) return 0
    const firstTime = strokes[0]?.points[0]?.time || 0
    const lastTime = strokes[strokes.length - 1]?.points?.slice(-1)[0]?.time || firstTime
    return lastTime - firstTime
  }
  
  function drawStroke(stroke, elapsed) {
    if (!stroke.points || stroke.points.length < 2) return
    ctx.beginPath()
    ctx.strokeStyle = '#000'
    ctx.lineWidth = 4
    ctx.lineCap = 'round'
    let lastPoint = stroke.points[0]
    for (let i = 1; i < stroke.points.length; i++) {
      const pt = stroke.points[i]
      if (pt.time - startTime > elapsed) break
      ctx.moveTo(lastPoint.x, lastPoint.y)
      ctx.lineTo(pt.x, pt.y)
      lastPoint = pt
    }
    ctx.stroke()
  }
  
  function animate() {
    const elapsed = Date.now() - startTime
    ctx.clearRect(0, 0, canvasWidth.value, canvasHeight.value)
    strokes.forEach(s => drawStroke(s, elapsed))
    const total = getTotalDuration()
    if (elapsed < total) {
      animationId = requestAnimationFrame(animate)
    } else {
      playing.value = false
    }
  }
  
  function startReplay() {
    strokes = parseStrokes()
    if (strokes.length === 0) return
    const canvas = replayCanvas.value
    if (!canvas) return
    ctx = canvas.getContext('2d')
    // 若存储了画布尺寸，可在此读取；否则使用默认
    startTime = strokes[0]?.points[0]?.time || Date.now()
    playing.value = true
    animate()
  }
  
  function stopReplay() {
    if (animationId) cancelAnimationFrame(animationId)
    playing.value = false
  }
  
  onMounted(() => {
    // 可从 strokes 中提取画布宽高（如果保存了的话），暂用默认值
    const data = parseStrokes()
    if (data.length > 0 && data[0].canvasWidth) {
      canvasWidth.value = data[0].canvasWidth
      canvasHeight.value = data[0].canvasHeight
    }
  })
  </script>
  
  <style scoped>
  .replay-container { margin-top: 20px; }
  .replay-controls { margin-bottom: 10px; }
  .replay-controls button { margin-right: 8px; }
  .replay-canvas { border: 1px solid #ccc; background: #fff; }
  </style>