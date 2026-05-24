<template>
    <div class="stats-grid">
      <div class="stat-card">
        <span class="stat-icon">📐</span>
        <div class="stat-info">
          <span class="stat-label">数学公式</span>
          <strong class="stat-value">{{ stats.math || 0 }}</strong>
        </div>
      </div>
      <div class="stat-card">
        <span class="stat-icon">💻</span>
        <div class="stat-info">
          <span class="stat-label">代码片段</span>
          <strong class="stat-value">{{ stats.code || 0 }}</strong>
        </div>
      </div>
      <div class="stat-card">
        <span class="stat-icon">⚛️</span>
        <div class="stat-info">
          <span class="stat-label">物理情景</span>
          <strong class="stat-value">{{ stats.physics || 0 }}</strong>
        </div>
      </div>
      <div class="stat-card">
        <span class="stat-icon">📊</span>
        <div class="stat-info">
          <span class="stat-label">总分析次数</span>
          <strong class="stat-value">{{ stats.total || 0 }}</strong>
        </div>
      </div>
    </div>
  </template>
  
  <script setup>
  defineProps({
    stats: {
      type: Object,
      default: () => ({ math: 0, code: 0, physics: 0, total: 0 })
    }
  })
  </script>
  
  <style scoped>
  .stats-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
    gap: 16px;
    margin-bottom: 24px;
  }
  .stat-card {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 16px;
    background: #fff;
    border-radius: 12px;
    box-shadow: 0 2px 6px rgba(0,0,0,0.04);
    border: 1px solid #e8e8e8;
  }
  .stat-icon {
    font-size: 28px;
  }
  .stat-info {
    display: flex;
    flex-direction: column;
  }
  .stat-label {
    font-size: 13px;
    color: #666;
  }
  .stat-value {
    font-size: 20px;
    font-weight: 700;
    color: #333;
  }
  </style>