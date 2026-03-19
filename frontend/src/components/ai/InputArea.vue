<script setup lang="ts">
import { ref } from 'vue'
import { useChatStore } from '@/stores/chat'
import { Send, Paperclip, Mic, MicOff, Square, Sparkles, Code, CheckCircle2, Loader2 } from 'lucide-vue-next'

const chatStore = useChatStore()
const inputContent = ref('')
const fileInputRef = ref<HTMLInputElement | null>(null)
const isRecording = ref(false)
const recordingTime = ref(0)
let mediaRecorder: MediaRecorder | null = null
let audioChunks: Blob[] = []
let recordingTimer: ReturnType<typeof setInterval> | null = null

const models = [
  { id: 'creative', name: '创意助手', icon: Sparkles },
  { id: 'coding', name: '代码大师', icon: Code },
  { id: 'precise', name: '严谨模式', icon: CheckCircle2 },
]

const handleSend = () => {
  if (!inputContent.value.trim() || chatStore.isGenerating) return

  chatStore.sendMessage(inputContent.value)
  inputContent.value = ''
}

const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

// ── 文件上传 ────────────────────────────────────────────────────
const triggerFileUpload = () => {
  fileInputRef.value?.click()
}

const handleFileSelected = async (e: Event) => {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  // Validate file type
  const supportedExts = ['.txt', '.pdf', '.docx', '.md', '.csv', '.json', '.py', '.java', '.js', '.ts', '.html', '.css', '.xml', '.yml', '.yaml']
  const ext = '.' + file.name.split('.').pop()?.toLowerCase()
  if (!supportedExts.includes(ext)) {
    alert(`不支持的文件格式: ${ext}\n\n支持: ${supportedExts.join(', ')}`)
    target.value = ''
    return
  }

  // Validate file size (10MB)
  if (file.size > 10 * 1024 * 1024) {
    alert('文件大小不能超过 10MB')
    target.value = ''
    return
  }

  await chatStore.uploadAndSendFile(file)
  target.value = ''
}

// ── 语音录制 ────────────────────────────────────────────────────
const toggleRecording = async () => {
  if (isRecording.value) {
    stopRecording()
  } else {
    await startRecording()
  }
}

const startRecording = async () => {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    mediaRecorder = new MediaRecorder(stream, {
      mimeType: MediaRecorder.isTypeSupported('audio/webm;codecs=opus')
        ? 'audio/webm;codecs=opus'
        : 'audio/webm',
    })
    audioChunks = []

    mediaRecorder.ondataavailable = (e) => {
      if (e.data.size > 0) {
        audioChunks.push(e.data)
      }
    }

    mediaRecorder.onstop = async () => {
      // Stop all tracks
      stream.getTracks().forEach(t => t.stop())

      if (audioChunks.length === 0) return

      const audioBlob = new Blob(audioChunks, { type: 'audio/webm' })

      try {
        const text = await chatStore.transcribeVoice(audioBlob)
        if (text) {
          inputContent.value = text
        }
      } catch {
        alert('语音识别失败，请检查网络或重试')
      }
    }

    mediaRecorder.start(250) // Collect data every 250ms
    isRecording.value = true
    recordingTime.value = 0

    recordingTimer = setInterval(() => {
      recordingTime.value++
    }, 1000)
  } catch (err: any) {
    if (err.name === 'NotAllowedError') {
      alert('请允许浏览器访问麦克风权限')
    } else {
      alert('无法启动录音: ' + err.message)
    }
  }
}

const stopRecording = () => {
  if (mediaRecorder && mediaRecorder.state !== 'inactive') {
    mediaRecorder.stop()
  }
  isRecording.value = false
  if (recordingTimer) {
    clearInterval(recordingTimer)
    recordingTimer = null
  }
}

const formatRecordingTime = (seconds: number) => {
  const m = Math.floor(seconds / 60).toString().padStart(2, '0')
  const s = (seconds % 60).toString().padStart(2, '0')
  return `${m}:${s}`
}

// ── 停止AI生成 ──────────────────────────────────────────────────
const handleStopGenerating = () => {
  chatStore.stopGenerating()
}

// ── 切换模型 ────────────────────────────────────────────────────
const handleModelSwitch = (modelId: string) => {
  chatStore.switchModel(modelId)
}
</script>

<template>
  <div class="space-y-3">
    <!-- Model Selector -->
    <div class="flex gap-2 justify-center md:justify-start">
      <button
        v-for="model in models"
        :key="model.id"
        @click="handleModelSwitch(model.id)"
        class="flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-medium border transition-all"
        :class="chatStore.currentModel === model.id
          ? 'bg-neon-blue/20 text-neon-blue border-neon-blue/50'
          : 'bg-slate-800 text-gray-400 border-white/5 hover:bg-slate-700'"
      >
        <component :is="model.icon" class="w-3 h-3" />
        {{ model.name }}
      </button>
    </div>

    <!-- Input Box -->
    <div class="relative bg-slate-800 border border-white/10 rounded-xl shadow-lg transition-all focus-within:border-neon-blue/50 focus-within:ring-1 focus-within:ring-neon-blue/20">
      <!-- Recording indicator -->
      <div v-if="isRecording" class="flex items-center gap-2 px-4 py-2 text-red-400 text-sm border-b border-white/5">
        <span class="w-2 h-2 bg-red-500 rounded-full animate-pulse"></span>
        录音中 {{ formatRecordingTime(recordingTime) }}
        <button @click="stopRecording" class="ml-auto text-xs text-gray-400 hover:text-white">点击停止</button>
      </div>

      <textarea
        v-model="inputContent"
        @keydown="handleKeydown"
        placeholder="输入您的问题，探索无限灵感... (Enter 发送)"
        class="w-full bg-transparent text-white px-4 py-3 min-h-[60px] max-h-[200px] resize-none focus:outline-none scrollbar-hide text-sm md:text-base"
        rows="2"
        :disabled="isRecording"
      ></textarea>

      <div class="flex justify-between items-center px-2 pb-2">
        <!-- Tools -->
        <div class="flex gap-1">
          <button
            @click="triggerFileUpload"
            class="p-2 text-gray-400 hover:text-white hover:bg-white/10 rounded-lg transition-colors"
            :class="{ 'opacity-50 cursor-not-allowed': chatStore.loading }"
            :disabled="chatStore.loading"
            title="上传文件 (TXT/PDF/DOCX/代码文件)"
          >
            <Loader2 v-if="chatStore.loading" class="w-5 h-5 animate-spin" />
            <Paperclip v-else class="w-5 h-5" />
          </button>
          <input
            ref="fileInputRef"
            type="file"
            class="hidden"
            accept=".txt,.pdf,.docx,.md,.csv,.json,.py,.java,.js,.ts,.html,.css,.xml,.yml,.yaml"
            @change="handleFileSelected"
          />
          <button
            @click="toggleRecording"
            class="p-2 rounded-lg transition-colors"
            :class="isRecording
              ? 'text-red-400 bg-red-500/20 hover:bg-red-500/30'
              : 'text-gray-400 hover:text-white hover:bg-white/10'"
            title="语音输入"
          >
            <MicOff v-if="isRecording" class="w-5 h-5" />
            <Mic v-else class="w-5 h-5" />
          </button>
        </div>

        <!-- Send / Stop Button -->
        <button
          v-if="chatStore.isGenerating"
          @click="handleStopGenerating"
          class="p-2 bg-red-500 text-white rounded-lg hover:bg-red-600 transition-all transform active:scale-95"
          title="停止生成"
        >
          <Square class="w-5 h-5" />
        </button>
        <button
          v-else
          @click="handleSend"
          :disabled="!inputContent.trim() || chatStore.isGenerating"
          class="p-2 bg-neon-blue text-slate-900 rounded-lg disabled:opacity-50 disabled:cursor-not-allowed hover:bg-neon-blue/90 transition-all transform active:scale-95"
        >
          <Send class="w-5 h-5" />
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.scrollbar-hide::-webkit-scrollbar {
    display: none;
}
</style>
