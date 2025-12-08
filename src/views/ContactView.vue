<script setup lang="ts">
import { ref } from 'vue'
import GlitchText from '@/components/ui/GlitchText.vue'
import { Send, Mail, MapPin, Phone } from 'lucide-vue-next'
import { useToastStore } from '@/stores/toast'

const toast = useToastStore()
const form = ref({ name: '', email: '', message: '' })
const isSending = ref(false)

const handleSubmit = async () => {
  isSending.value = true
  await new Promise(r => setTimeout(r, 1500))
  isSending.value = false
  toast.success('消息已发送！我们会尽快通过全息频道回复您。')
  form.value = { name: '', email: '', message: '' }
}
</script>

<template>
  <div class="container mx-auto px-4 py-20 min-h-screen flex items-center relative">
    <!-- Background Decor -->
    <div class="absolute top-20 left-0 w-[500px] h-[500px] opacity-20 pointer-events-none">
       <img src="https://api.iconify.design/game-icons:world.svg?color=%2300f3ff" class="w-full h-full animate-spin-slow" />
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-20 w-full relative z-10">
      <!-- Info -->
      <div class="space-y-12 backdrop-blur-sm bg-slate-900/30 p-8 rounded-3xl border border-white/5">
        <div>
          <h1 class="text-5xl font-black mb-6"><GlitchText text="CONTACT" /></h1>
          <p class="text-gray-400 text-lg">
            遇到系统故障？或者只是想找人聊聊<span class="text-neon-blue">仿生电子羊</span>？<br>
            我们的全天候客服团队随时待命。
          </p>
        </div>

        <div class="space-y-6">
          <div class="flex items-center gap-4 p-4 rounded-xl bg-slate-800/50 border border-white/5">
            <div class="w-12 h-12 rounded-full bg-neon-blue/10 flex items-center justify-center text-neon-blue">
              <Mail class="w-6 h-6" />
            </div>
            <div>
              <p class="text-sm text-gray-500">Email Support</p>
              <p class="text-white font-mono">support@inspo.verse</p>
            </div>
          </div>
          
          <div class="flex items-center gap-4 p-4 rounded-xl bg-slate-800/50 border border-white/5">
            <div class="w-12 h-12 rounded-full bg-neon-purple/10 flex items-center justify-center text-neon-purple">
              <Phone class="w-6 h-6" />
            </div>
            <div>
              <p class="text-sm text-gray-500">Emergency Line</p>
              <p class="text-white font-mono">+86 400-CYBER-2077</p>
            </div>
          </div>

          <div class="flex items-center gap-4 p-4 rounded-xl bg-slate-800/50 border border-white/5">
            <div class="w-12 h-12 rounded-full bg-neon-green/10 flex items-center justify-center text-neon-green">
              <MapPin class="w-6 h-6" />
            </div>
            <div>
              <p class="text-sm text-gray-500">HQ Location</p>
              <p class="text-white font-mono">Night City, District 1</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Form -->
      <div class="bg-slate-800 p-8 rounded-3xl border border-white/10 shadow-2xl relative overflow-hidden">
        <div class="absolute top-0 right-0 w-64 h-64 bg-neon-blue/20 blur-[100px] rounded-full -translate-y-1/2 translate-x-1/2"></div>
        
        <h2 class="text-2xl font-bold text-white mb-8 relative z-10">发送全息讯息</h2>
        
        <form @submit.prevent="handleSubmit" class="space-y-6 relative z-10">
          <div class="space-y-2">
            <label class="text-sm text-gray-400">您的称呼</label>
            <input v-model="form.name" required type="text" class="w-full bg-slate-900 border border-slate-700 rounded-lg px-4 py-3 text-white focus:border-neon-blue focus:outline-none" />
          </div>
          
          <div class="space-y-2">
            <label class="text-sm text-gray-400">通讯地址 (Email)</label>
            <input v-model="form.email" required type="email" class="w-full bg-slate-900 border border-slate-700 rounded-lg px-4 py-3 text-white focus:border-neon-blue focus:outline-none" />
          </div>
          
          <div class="space-y-2">
            <label class="text-sm text-gray-400">讯息内容</label>
            <textarea v-model="form.message" required rows="4" class="w-full bg-slate-900 border border-slate-700 rounded-lg px-4 py-3 text-white focus:border-neon-blue focus:outline-none resize-none"></textarea>
          </div>
          
          <button 
            type="submit" 
            :disabled="isSending"
            class="w-full bg-gradient-to-r from-neon-blue to-neon-purple text-white font-bold py-4 rounded-xl hover:shadow-lg transition-all flex items-center justify-center gap-2"
          >
            <span v-if="isSending" class="animate-spin w-5 h-5 border-2 border-white border-t-transparent rounded-full"></span>
            <span v-else>发送信号 <Send class="w-4 h-4 inline ml-1" /></span>
          </button>
        </form>
      </div>
    </div>
  </div>
</template>
