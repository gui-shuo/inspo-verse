<script setup lang="ts">
import { ref } from 'vue'
import { Activity, AlertTriangle } from 'lucide-vue-next'

const logs = ref([
  { id: 1, user: 'User_A', model: 'GPT-4', prompt: '写一个 Python 爬虫...', status: 'success', tokens: 150 },
  { id: 2, user: 'User_B', model: 'Claude 3', prompt: '生成涩图...', status: 'blocked', tokens: 0 },
  { id: 3, user: 'User_C', model: 'GPT-3.5', prompt: '翻译这段话...', status: 'success', tokens: 45 },
])
</script>

<template>
  <div class="space-y-6 animate__animated animate__fadeIn">
    <h2 class="text-2xl font-bold text-white">AI 服务监控</h2>

    <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
      <div class="bg-slate-800 p-6 rounded-xl border border-white/5">
        <div class="text-gray-400 text-sm mb-1">今日 Token 消耗</div>
        <div class="text-3xl font-bold text-neon-blue">1,245,090</div>
      </div>
      <div class="bg-slate-800 p-6 rounded-xl border border-white/5">
        <div class="text-gray-400 text-sm mb-1">请求成功率</div>
        <div class="text-3xl font-bold text-green-400">99.8%</div>
      </div>
      <div class="bg-slate-800 p-6 rounded-xl border border-white/5">
        <div class="text-gray-400 text-sm mb-1">拦截违规请求</div>
        <div class="text-3xl font-bold text-red-400">12</div>
      </div>
    </div>

    <div class="bg-slate-800 rounded-xl border border-white/5 overflow-hidden">
      <table class="w-full text-left text-sm">
        <thead class="bg-slate-900/50 border-b border-white/5 text-gray-400">
          <tr>
            <th class="p-4">用户</th>
            <th class="p-4">模型</th>
            <th class="p-4">Prompt 摘要</th>
            <th class="p-4">状态</th>
            <th class="p-4 text-right">Tokens</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-white/5">
          <tr v-for="log in logs" :key="log.id" class="hover:bg-white/5">
            <td class="p-4 text-white">{{ log.user }}</td>
            <td class="p-4 text-gray-400">{{ log.model }}</td>
            <td class="p-4 text-gray-300 truncate max-w-[200px]">{{ log.prompt }}</td>
            <td class="p-4">
              <span v-if="log.status === 'success'" class="text-green-400 flex items-center gap-1"><Activity class="w-3 h-3" /> 成功</span>
              <span v-else class="text-red-400 flex items-center gap-1"><AlertTriangle class="w-3 h-3" /> 拦截</span>
            </td>
            <td class="p-4 text-right font-mono text-neon-blue">{{ log.tokens }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
