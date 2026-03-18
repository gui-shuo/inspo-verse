<script setup lang="ts">
import { ref } from 'vue'
import { Save, Bell } from 'lucide-vue-next'
import { useToastStore } from '@/stores/toast'

const toast = useToastStore()
const siteName = ref('Inspo-Verse')
const maintenanceMode = ref(false)
const announcement = ref('')

const saveSettings = () => {
  toast.success('系统设置已保存')
}

const sendAnnouncement = () => {
  if (!announcement.value) return
  toast.info(`全站广播: ${announcement.value}`)
  announcement.value = ''
}
</script>

<template>
  <div class="space-y-6 animate__animated animate__fadeIn">
    <h2 class="text-2xl font-bold text-white">系统设置</h2>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-8">
      <!-- General -->
      <div class="bg-slate-800 p-6 rounded-xl border border-white/5 space-y-6">
        <h3 class="font-bold text-white border-b border-white/5 pb-4">基础设置</h3>
        
        <div class="space-y-2">
          <label class="text-sm text-gray-400">站点名称</label>
          <input v-model="siteName" type="text" class="w-full bg-slate-900 border border-white/10 rounded-lg px-4 py-2 text-white focus:border-neon-blue focus:outline-none">
        </div>

        <div class="flex items-center justify-between">
          <div>
            <div class="text-white font-medium">维护模式</div>
            <div class="text-xs text-gray-500">开启后前台将无法访问</div>
          </div>
          <button 
            @click="maintenanceMode = !maintenanceMode"
            class="w-12 h-6 rounded-full transition-colors relative"
            :class="maintenanceMode ? 'bg-neon-blue' : 'bg-slate-700'"
          >
            <div class="absolute top-1 left-1 w-4 h-4 bg-white rounded-full transition-transform" :class="maintenanceMode ? 'translate-x-6' : ''"></div>
          </button>
        </div>

        <button @click="saveSettings" class="w-full bg-neon-blue text-slate-900 font-bold py-2 rounded-lg hover:shadow-lg transition-all flex items-center justify-center gap-2">
          <Save class="w-4 h-4" /> 保存设置
        </button>
      </div>

      <!-- Broadcast -->
      <div class="bg-slate-800 p-6 rounded-xl border border-white/5 space-y-6">
        <h3 class="font-bold text-white border-b border-white/5 pb-4">发布公告</h3>
        
        <div class="space-y-2">
          <label class="text-sm text-gray-400">公告内容</label>
          <textarea v-model="announcement" rows="4" class="w-full bg-slate-900 border border-white/10 rounded-lg px-4 py-2 text-white focus:border-neon-blue focus:outline-none resize-none"></textarea>
        </div>

        <button @click="sendAnnouncement" class="w-full border border-neon-purple text-neon-purple font-bold py-2 rounded-lg hover:bg-neon-purple/10 transition-all flex items-center justify-center gap-2">
          <Bell class="w-4 h-4" /> 发送全站通知
        </button>
      </div>
    </div>
  </div>
</template>
