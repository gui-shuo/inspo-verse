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

  ctx.fillStyle = '#f3f4f6' // bg-gray-100
  ctx.fillRect(0, 0, width, height)

  // Generate 4 chars
  for (let i = 0; i < 4; i++) {
    const char = chars[Math.floor(Math.random() * chars.length)]
    code += char
    ctx.font = 'bold 24px Arial'
    ctx.fillStyle = randomColor(50, 150)
    ctx.save()
    ctx.translate(30 * i + 15, 20)
    ctx.rotate((Math.random() - 0.5) * 0.5)
    ctx.fillText(char, -10, 8)
    ctx.restore()
  }

  // Add noise lines
  for (let i = 0; i < 5; i++) {
    ctx.strokeStyle = randomColor(100, 200)
    ctx.beginPath()
    ctx.moveTo(Math.random() * width, Math.random() * height)
    ctx.lineTo(Math.random() * width, Math.random() * height)
    ctx.stroke()
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
  <div class="cursor-pointer" @click="generateCode" title="点击刷新验证码">
    <canvas ref="canvasRef" :width="width" :height="height" class="rounded border border-gray-600"></canvas>
  </div>
</template>
