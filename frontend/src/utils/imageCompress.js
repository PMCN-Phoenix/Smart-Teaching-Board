/**
 * 压缩 Base64 图片
 * @param {string} base64 - 原始 Base64（不含 data:image 前缀）
 * @param {number} maxWidth - 最大宽度（px），默认 800
 * @param {number} quality - JPEG 质量（0-1），默认 0.7
 * @returns {Promise<string>} 压缩后的 Base64
 */
export function compressBase64Image(base64, maxWidth = 800, quality = 0.7) {
    return new Promise((resolve, reject) => {
      const img = new Image()
      img.src = 'data:image/png;base64,' + base64
      img.onload = () => {
        const canvas = document.createElement('canvas')
        let { width, height } = img
        if (width > maxWidth) {
          height = Math.round(height * maxWidth / width)
          width = maxWidth
        }
        canvas.width = width
        canvas.height = height
        const ctx = canvas.getContext('2d')
        ctx.drawImage(img, 0, 0, width, height)
        const compressed = canvas.toDataURL('image/jpeg', quality).replace('data:image/jpeg;base64,', '')
        resolve(compressed)
      }
      img.onerror = reject
    })
  }