<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { useAppStore } from '@/stores/index'

const { theme } = storeToRefs(useAppStore())
</script>

<template>
  <div
    class="cyber-bg fixed inset-0 z-0 overflow-hidden pointer-events-none"
    :class="theme === 'dark' ? 'bg-slate-950' : 'aurora-bg'"
  >
    <!-- ===== Dark Theme: Cyberpunk Grid ===== -->
    <template v-if="theme === 'dark'">
      <div class="grid-plane"></div>
      <div class="horizon-glow"></div>
      <div class="vignette"></div>
    </template>

    <!-- ===== Light Theme: Aurora Borealis ===== -->
    <template v-else>
      <!-- Deep space base -->
      <div class="aurora-base"></div>
      
      <!-- Animated aurora blobs -->
      <div class="aurora-blob aurora-blob-1"></div>
      <div class="aurora-blob aurora-blob-2"></div>
      <div class="aurora-blob aurora-blob-3"></div>
      <div class="aurora-blob aurora-blob-4"></div>
      
      <!-- Aurora shimmer grid -->
      <div class="aurora-grid"></div>
      
      <!-- Starfield -->
      <div class="aurora-stars"></div>
      
      <!-- Soft vignette -->
      <div class="aurora-vignette"></div>
    </template>
  </div>
</template>

<style scoped>
.cyber-bg {
  perspective: 1000px;
}

/* ===== DARK THEME: Cyberpunk Grid ===== */
.grid-plane {
  position: absolute;
  width: 200%;
  height: 200%;
  left: -50%;
  top: -25%;
  background-image: 
    linear-gradient(rgba(0, 255, 157, 0.15) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 255, 157, 0.15) 1px, transparent 1px);
  background-size: 80px 80px;
  transform: rotateX(75deg);
  animation: moveGrid 5s linear infinite;
  mask-image: linear-gradient(to bottom, transparent 0%, black 20%, black 80%, transparent 100%);
}

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

/* Scanline (dark only) */
.bg-slate-950::after {
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

/* ===== LIGHT THEME: Aurora Borealis ===== */
.aurora-bg {
  background: linear-gradient(160deg, #080b1a 0%, #0d1030 30%, #0f0c25 60%, #0a0818 100%);
}

.aurora-base {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse at 20% 0%, rgba(168, 85, 247, 0.12) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 0%, rgba(96, 165, 250, 0.08) 0%, transparent 50%),
    radial-gradient(ellipse at 50% 100%, rgba(244, 114, 182, 0.06) 0%, transparent 40%);
}

/* Floating aurora color blobs */
.aurora-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.5;
  mix-blend-mode: screen;
}

.aurora-blob-1 {
  width: 600px;
  height: 400px;
  top: -10%;
  left: 10%;
  background: radial-gradient(circle, rgba(168, 85, 247, 0.35) 0%, transparent 70%);
  animation: auroraFloat1 12s ease-in-out infinite;
}

.aurora-blob-2 {
  width: 500px;
  height: 350px;
  top: -5%;
  right: 15%;
  background: radial-gradient(circle, rgba(96, 165, 250, 0.3) 0%, transparent 70%);
  animation: auroraFloat2 15s ease-in-out infinite;
}

.aurora-blob-3 {
  width: 450px;
  height: 300px;
  top: 5%;
  left: 35%;
  background: radial-gradient(circle, rgba(244, 114, 182, 0.25) 0%, transparent 70%);
  animation: auroraFloat3 18s ease-in-out infinite;
}

.aurora-blob-4 {
  width: 400px;
  height: 280px;
  bottom: 30%;
  right: 5%;
  background: radial-gradient(circle, rgba(45, 212, 191, 0.18) 0%, transparent 70%);
  animation: auroraFloat4 20s ease-in-out infinite;
}

/* Aurora grid — subtle perspective grid with violet tint */
.aurora-grid {
  position: absolute;
  width: 200%;
  height: 200%;
  left: -50%;
  top: -25%;
  background-image:
    linear-gradient(rgba(168, 85, 247, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(168, 85, 247, 0.06) 1px, transparent 1px);
  background-size: 80px 80px;
  transform: rotateX(75deg);
  animation: moveGrid 8s linear infinite;
  mask-image: linear-gradient(to bottom, transparent 0%, black 30%, black 70%, transparent 100%);
}

/* Stars */
.aurora-stars {
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(1px 1px at 10% 20%, rgba(255,255,255,0.5) 50%, transparent 50%),
    radial-gradient(1px 1px at 30% 60%, rgba(255,255,255,0.4) 50%, transparent 50%),
    radial-gradient(1px 1px at 50% 15%, rgba(255,255,255,0.6) 50%, transparent 50%),
    radial-gradient(1px 1px at 70% 45%, rgba(255,255,255,0.3) 50%, transparent 50%),
    radial-gradient(1px 1px at 85% 70%, rgba(255,255,255,0.5) 50%, transparent 50%),
    radial-gradient(1.5px 1.5px at 15% 80%, rgba(168, 85, 247, 0.6) 50%, transparent 50%),
    radial-gradient(1.5px 1.5px at 45% 35%, rgba(96, 165, 250, 0.5) 50%, transparent 50%),
    radial-gradient(1.5px 1.5px at 65% 85%, rgba(244, 114, 182, 0.5) 50%, transparent 50%),
    radial-gradient(1px 1px at 20% 50%, rgba(255,255,255,0.3) 50%, transparent 50%),
    radial-gradient(1px 1px at 90% 25%, rgba(255,255,255,0.4) 50%, transparent 50%),
    radial-gradient(1px 1px at 5% 95%, rgba(255,255,255,0.3) 50%, transparent 50%),
    radial-gradient(1px 1px at 75% 10%, rgba(255,255,255,0.5) 50%, transparent 50%),
    radial-gradient(1.5px 1.5px at 55% 65%, rgba(45, 212, 191, 0.4) 50%, transparent 50%),
    radial-gradient(1px 1px at 40% 90%, rgba(255,255,255,0.4) 50%, transparent 50%);
  animation: twinkle 4s ease-in-out infinite alternate;
}

.aurora-vignette {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at center, transparent 0%, rgba(8, 11, 26, 0.7) 100%);
  pointer-events: none;
}

/* ===== Aurora Animations ===== */
@keyframes auroraFloat1 {
  0%, 100% { transform: translate(0, 0) scale(1); opacity: 0.5; }
  25% { transform: translate(30px, 15px) scale(1.05); opacity: 0.6; }
  50% { transform: translate(-20px, 25px) scale(1.1); opacity: 0.45; }
  75% { transform: translate(15px, -10px) scale(0.95); opacity: 0.55; }
}

@keyframes auroraFloat2 {
  0%, 100% { transform: translate(0, 0) scale(1); opacity: 0.4; }
  33% { transform: translate(-25px, 20px) scale(1.08); opacity: 0.55; }
  66% { transform: translate(20px, -15px) scale(0.92); opacity: 0.35; }
}

@keyframes auroraFloat3 {
  0%, 100% { transform: translate(0, 0) scale(1); opacity: 0.35; }
  30% { transform: translate(25px, -20px) scale(1.12); opacity: 0.5; }
  60% { transform: translate(-30px, 10px) scale(0.9); opacity: 0.3; }
}

@keyframes auroraFloat4 {
  0%, 100% { transform: translate(0, 0) scale(1); opacity: 0.25; }
  40% { transform: translate(-20px, -25px) scale(1.15); opacity: 0.4; }
  70% { transform: translate(15px, 20px) scale(0.85); opacity: 0.2; }
}

@keyframes twinkle {
  0% { opacity: 0.6; }
  100% { opacity: 1; }
}
</style>
