import { ref, onMounted, onUnmounted } from 'vue'

export function useHandGesture(videoRef, canvasRef, options = {}) {
  const isPinching = ref(false)
  const lastPoint = ref(null)
  const color = ref('#000000')
  const lineWidth = ref(4)

  // 笔迹记录数组
  const strokes = ref([])
  let currentStroke = null

  let hands = null
  let camera = null
  let animationFrame = null

  // 指数平滑系数（0~1，越小越平滑）
  const alpha = options.alpha || 0.3
  let smoothedX = 0
  let smoothedY = 0
  let prevX = 0
  let prevY = 0
  let hasPrev = false
  let trackingLostFrames = 0          // 连续丢失追踪的帧数
  const MAX_LOST_FRAMES = 5           // 最多容忍 5 帧无手，之后才真正重置

  const init = async () => {
    // 等待 Vue DOM 更新
    await new Promise(resolve => setTimeout(resolve, 100))

    // 防御性检查
    if (!videoRef.value) {
        console.error('videoRef 未挂载，请检查模板中 <video ref="video"> 是否存在')
        return
    }
    if (!canvasRef.value) {
        console.error('canvasRef 未挂载，请检查模板中 <canvas ref="canvas"> 是否存在')
        return
    }
    // 动态加载本地 hands.js
    await new Promise((resolve, reject) => {
      const script = document.createElement('script')
      script.src = '/models/hands.js'
      script.onload = resolve
      script.onerror = reject
      document.head.appendChild(script)
    })

    const { Hands } = window
    hands = new Hands({
        locateFile: (file) => {
          // 开发阶段使用 CDN，确保网络畅通
          const cdnPath = `https://cdn.jsdelivr.net/npm/@mediapipe/hands@0.4.1675469240/${file}`
          return cdnPath
        }
      })

    hands.setOptions({
      maxNumHands: 1,
      modelComplexity: 0,           // 降低复杂度提高帧率
      minDetectionConfidence: 0.7,
      minTrackingConfidence: 0.5
    })

    hands.onResults(onResults)

    // 启动摄像头
    const stream = await navigator.mediaDevices.getUserMedia({ video: true })
    videoRef.value.srcObject = stream
    await videoRef.value.play()

    // 初始化画布
    const canvas = canvasRef.value
    canvas.width = videoRef.value.videoWidth || 640
    canvas.height = videoRef.value.videoHeight || 480
    const ctx = canvas.getContext('2d')
    ctx.fillStyle = 'white'
    ctx.fillRect(0, 0, canvas.width, canvas.height)

    // 帧循环
    const sendFrame = async () => {
        if (videoRef.value && videoRef.value.readyState >= 2) {
            await hands.send({ image: videoRef.value })
        }
        animationFrame = requestAnimationFrame(sendFrame)
    }
    sendFrame()
  }

  const onResults = (results) => {
    const canvas = canvasRef.value
    const ctx = canvas.getContext('2d')

    // 处理手部追踪丢失
    if (!results.multiHandLandmarks || results.multiHandLandmarks.length === 0) {
      trackingLostFrames++
      if (trackingLostFrames < MAX_LOST_FRAMES) return
      // 超过容忍帧数，真正重置
      isPinching.value = false
      hasPrev = false
      prevX = 0
      prevY = 0
      currentStroke = null   // 结束当前笔画
      trackingLostFrames = 0
      return
    }
    trackingLostFrames = 0

    // 提取关键点
    const landmarks = results.multiHandLandmarks[0]
    const indexTip = landmarks[8]
    const thumbTip = landmarks[4]

    const ix = (1 - indexTip.x) * canvas.width   // 镜像 x
    const iy = indexTip.y * canvas.height
    const tx = (1 - thumbTip.x) * canvas.width
    const ty = thumbTip.y * canvas.height
    const distance = Math.hypot(ix - tx, iy - ty)

    // 动态基础阈值
    const indexBase = landmarks[5]
    const pinkyBase = landmarks[17]
    const handSize = Math.hypot(
      (indexBase.x - pinkyBase.x) * canvas.width,
      (indexBase.y - pinkyBase.y) * canvas.height
    )
    const baseThreshold = Math.max(15, handSize * 0.25)

    // 滞后阈值
    const upperThreshold = baseThreshold * 1.6
    const lowerThreshold = baseThreshold

    const isCurrentlyPinching = isPinching.value
    const shouldPinch = isCurrentlyPinching
      ? distance < upperThreshold
      : distance < lowerThreshold

    if (shouldPinch) {
      // 捏合状态
      const rawX = (ix + tx) / 2
      const rawY = (iy + ty) / 2

      if (!hasPrev) {
        // 新笔画第一帧：初始化平滑器，创建笔画对象
        smoothedX = rawX
        smoothedY = rawY
        prevX = smoothedX
        prevY = smoothedY
        hasPrev = true
        isPinching.value = true

        // 创建新笔画，记录起点（带时间戳）
        currentStroke = {
          points: [{ x: smoothedX, y: smoothedY, time: Date.now() }]
        }
        strokes.value.push(currentStroke)
        return
      }

      // 连续书写：平滑并绘制
      smoothedX = alpha * rawX + (1 - alpha) * smoothedX
      smoothedY = alpha * rawY + (1 - alpha) * smoothedY

      if (prevX !== 0 && prevY !== 0) {
        ctx.beginPath()
        ctx.strokeStyle = color.value
        ctx.lineWidth = lineWidth.value
        ctx.lineCap = 'round'
        ctx.moveTo(prevX, prevY)
        ctx.lineTo(smoothedX, smoothedY)
        ctx.stroke()
      }

      prevX = smoothedX
      prevY = smoothedY
      isPinching.value = true

      // 记录当前点
      if (currentStroke) {
        currentStroke.points.push({ x: smoothedX, y: smoothedY, time: Date.now() })
      }
    } else {
      // 未捏合
      isPinching.value = false
      hasPrev = false
      prevX = 0
      prevY = 0
      currentStroke = null   // 结束当前笔画
    }
  }

  const stop = () => {
    if (animationFrame) cancelAnimationFrame(animationFrame)
  }

  const clearCanvas = () => {
    const canvas = canvasRef.value
    if (!canvas) return
    const ctx = canvas.getContext('2d')
    ctx.clearRect(0, 0, canvas.width, canvas.height)
    ctx.fillStyle = 'white'
    ctx.fillRect(0, 0, canvas.width, canvas.height)
  }

  // 清空笔迹记录（用于清屏按钮）
  const clearStrokes = () => {
    strokes.value = []
    currentStroke = null
  }

  const setColor = (c) => { color.value = c }
  const setLineWidth = (w) => { lineWidth.value = w }

  return {
    isPinching,
    init,
    stop,
    clearCanvas,
    setColor,
    setLineWidth,
    color,
    lineWidth,
    strokes,
    clearStrokes
  }
}