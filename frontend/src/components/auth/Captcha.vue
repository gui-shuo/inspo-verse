<script setup lang="ts">
import { ref, onMounted, defineExpose } from 'vue'

const props = defineProps<{
  width?: number
  height?: number
}>()

const emit = defineEmits<{
  (e: 'update:code', code: string): void
}>()

const canvasRef = ref<HTMLCanvasElement | null>(null)
const width = props.width || 120
const height = props.height || 40

const generateCode = () => {
  const chars = 'ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789'
  let code = ''
  const ctx = canvasRef.value?.getContext('2d')
  if (!ctx) return

  ctx.fillStyle = '#1e293b' // slate-800
  ctx.fillRect(0, 0, width, height)

  // Generate 4 chars with better spacing
  const charWidth = width / 4
  for (let i = 0; i < 4; i++) {
    const char = chars[Math.floor(Math.random() * chars.length)]
    code += char
    ctx.font = 'bold 22px Arial'
    ctx.fillStyle = randomColor(150, 255)
    ctx.save()
    ctx.translate(charWidth * i + charWidth / 2, height / 2 + 5)
    ctx.rotate((Math.random() - 0.5) * 0.4)
    ctx.fillText(char, -8, 0)
    ctx.restore()
  }

  // Add noise lines
  for (let i = 0; i < 5; i++) {
    ctx.strokeStyle = randomColor(80, 150)
    ctx.lineWidth = 1
    ctx.beginPath()
    ctx.moveTo(Math.random() * width, Math.random() * height)
    ctx.lineTo(Math.random() * width, Math.random() * height)
    ctx.stroke()
  }

  // Add noise dots
  for (let i = 0; i < 30; i++) {
    ctx.fillStyle = randomColor(100, 200)
    ctx.beginPath()
    ctx.arc(Math.random() * width, Math.random() * height, 1, 0, 2 * Math.PI)
    ctx.fill()
  }

  emit('update:code', code)
}

const randomColor = (min: number, max: number) => {
  const r = Math.floor(Math.random() * (max - min) + min)
  const g = Math.floor(Math.random() * (max - min) + min)
  const b = Math.floor(Math.random() * (max - min) + min)
  return `rgb(${r},${g},${b})`
}

onMounted(() => {
  generateCode()
})

defineExpose({ refresh: generateCode })
</script>

<template>
  <div class="cursor-pointer flex-shrink-0" @click="generateCode" title="点击刷新验证码">
    <canvas ref="canvasRef" :width="width" :height="height" class="rounded border border-slate-600 hover:border-neon-blue transition-colors"></canvas>
  </div>
</template>
