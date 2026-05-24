const config = {
    apiBaseUrl: import.meta.env.VITE_API_BASE_URL || '',   // 空字符串表示使用 Vite 代理
    aiTimeout: 30000   // 30秒超时
  }
  
  export default config