<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { useAppStore } from '@/stores/index'

const { theme } = storeToRefs(useAppStore())
</script>

<template>
  <div
    class="cyber-bg fixed inset-0 z-0 overflow-hidden pointer-events-none transition-colors duration-500"
    :class="theme === 'dark' ? 'bg-slate-950' : 'bg-gradient-to-b from-slate-50 to-blue-50'"
  >
    <!-- Grid Plane (dark only) -->
    <div v-if="theme === 'dark'" class="grid-plane"></div>
    
    <!-- Horizon Glow -->
    <div :class="theme === 'dark' ? 'horizon-glow' : 'horizon-glow-light'"></div>
    
    <!-- Optional: Vignette -->
    <div :class="theme === 'dark' ? 'vignette' : 'vignette-light'"></div>
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

/* Light theme: remove scanlines */
:global(.light-theme) .cyber-bg::after {
  display: none;
}

/* Light Theme Horizon */
.horizon-glow-light {
  position: absolute;
  top: -10%;
  left: 0;
  width: 100%;
  height: 50%;
  background: radial-gradient(ellipse at 50% 100%, rgba(139, 92, 246, 0.08) 0%, transparent 70%);
  filter: blur(60px);
  opacity: 0.6;
}

.vignette-light {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at center, transparent 0%, rgba(248, 250, 252, 0.4) 100%);
  pointer-events: none;
}
</style>
