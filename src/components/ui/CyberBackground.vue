<script setup lang="ts">
// 纯 CSS 实现的赛博朋克网格背景 + 漂浮粒子
</script>

<template>
  <div class="cyber-bg fixed inset-0 z-0 overflow-hidden pointer-events-none bg-slate-950">
    <!-- Grid Plane -->
    <div class="grid-plane"></div>
    
    <!-- Horizon Glow -->
    <div class="horizon-glow"></div>
    
    <!-- Optional: Vignette -->
    <div class="vignette"></div>
  </div>
</template>

<style scoped>
.cyber-bg {
  perspective: 1000px;
}

/* 3D Moving Grid */
.grid-plane {
  position: absolute;
  width: 200%;
  height: 200%;
  left: -50%;
  top: -25%; /* Adjust to lower the horizon */
  background-image: 
    linear-gradient(rgba(0, 255, 157, 0.15) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 255, 157, 0.15) 1px, transparent 1px);
  background-size: 80px 80px;
  transform: rotateX(75deg);
  animation: moveGrid 5s linear infinite;
  mask-image: linear-gradient(to bottom, transparent 0%, black 20%, black 80%, transparent 100%);
}

/* Horizon Light */
.horizon-glow {
  position: absolute;
  top: -10%;
  left: 0;
  width: 100%;
  height: 50%;
  background: radial-gradient(ellipse at 50% 100%, rgba(139, 92, 246, 0.4) 0%, transparent 70%);
  filter: blur(60px);
  opacity: 0.8;
}

.vignette {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at center, transparent 0%, rgba(2, 6, 23, 0.8) 100%);
  pointer-events: none;
}

@keyframes moveGrid {
  0% { background-position: 0 0; }
  100% { background-position: 0 80px; }
}

/* Scanline Effect */
.cyber-bg::after {
  content: '';
  position: absolute;
  inset: 0;
  background: repeating-linear-gradient(
    0deg,
    transparent,
    transparent 3px,
    rgba(0, 0, 0, 0.2) 4px
  );
  pointer-events: none;
  z-index: 10;
  opacity: 0.5;
}
</style>
