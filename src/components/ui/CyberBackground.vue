<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'

const canvasRef = ref<HTMLCanvasElement | null>(null)
let ctx: CanvasRenderingContext2D | null = null
let animationFrameId: number
let particles: Particle[] = []
let mouse = { x: 0, y: 0 }

class Particle {
  x: number
  y: number
  size: number
  speedX: number
  speedY: number
  color: string

  constructor(w: number, h: number) {
    this.x = Math.random() * w
    this.y = Math.random() * h
    this.size = Math.random() * 2 + 0.5
    this.speedX = Math.random() * 1 - 0.5
    this.speedY = Math.random() * 1 - 0.5
    // 随机选择青色或紫色
    this.color = Math.random() > 0.5 ? 'rgba(0, 243, 255, 0.5)' : 'rgba(188, 19, 254, 0.5)'
  }

  update(w: number, h: number) {
    this.x += this.speedX
    this.y += this.speedY

    if (this.x > w) this.x = 0
    else if (this.x < 0) this.x = w
    if (this.y > h) this.y = 0
    else if (this.y < 0) this.y = h
  }

  draw() {
    if (!ctx) return
    ctx.fillStyle = this.color
    ctx.beginPath()
    ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2)
    ctx.fill()
  }
}

const init = () => {
  if (!canvasRef.value) return
  ctx = canvasRef.value.getContext('2d')
  if (!ctx) return

  canvasRef.value.width = window.innerWidth
  canvasRef.value.height = window.innerHeight

  particles = []
  const particleCount = Math.min(100, (window.innerWidth * window.innerHeight) / 9000)
  for (let i = 0; i < particleCount; i++) {
    particles.push(new Particle(canvasRef.value.width, canvasRef.value.height))
  }
}

const animate = () => {
  if (!canvasRef.value || !ctx) return
  ctx.clearRect(0, 0, canvasRef.value.width, canvasRef.value.height)
  
  // Update and draw particles
  for (let i = 0; i < particles.length; i++) {
    particles[i].update(canvasRef.value.width, canvasRef.value.height)
    particles[i].draw()

    // Draw connections
    for (let j = i; j < particles.length; j++) {
      const dx = particles[i].x - particles[j].x
      const dy = particles[i].y - particles[j].y
      const distance = Math.sqrt(dx * dx + dy * dy)

      if (distance < 150) {
        ctx.beginPath()
        ctx.strokeStyle = `rgba(100, 116, 139, ${0.1 - distance/1500})`
        ctx.lineWidth = 1
        ctx.moveTo(particles[i].x, particles[i].y)
        ctx.lineTo(particles[j].x, particles[j].y)
        ctx.stroke()
      }
    }
  }

  animationFrameId = requestAnimationFrame(animate)
}

const handleResize = () => {
  init()
}

onMounted(() => {
  init()
  animate()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  cancelAnimationFrame(animationFrameId)
})
</script>

<template>
  <canvas 
    ref="canvasRef" 
    class="fixed top-0 left-0 w-full h-full pointer-events-none z-0 opacity-60"
  ></canvas>
</template>
