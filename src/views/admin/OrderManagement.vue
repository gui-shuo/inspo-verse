<script setup lang="ts">
import { ref } from 'vue'
import { ShoppingBag, CheckCircle, XCircle } from 'lucide-vue-next'

const orders = ref([
  { id: 'ORD-001', user: 'User_A', item: '黄金会员年卡', amount: 299, status: 'completed', date: '2024-01-01' },
  { id: 'ORD-002', user: 'User_B', item: 'AI 点数包', amount: 49, status: 'pending', date: '2024-01-02' },
])
</script>

<template>
  <div class="space-y-6 animate__animated animate__fadeIn">
    <h2 class="text-2xl font-bold text-white">订单管理</h2>
    
    <div class="bg-slate-800 rounded-xl border border-white/5 overflow-hidden">
      <table class="w-full text-left text-sm">
        <thead class="bg-slate-900/50 border-b border-white/5 text-gray-400">
          <tr>
            <th class="p-4">订单号</th>
            <th class="p-4">用户</th>
            <th class="p-4">商品</th>
            <th class="p-4">金额</th>
            <th class="p-4">状态</th>
            <th class="p-4 text-right">日期</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-white/5">
          <tr v-for="order in orders" :key="order.id" class="hover:bg-white/5">
            <td class="p-4 font-mono text-gray-400">{{ order.id }}</td>
            <td class="p-4 text-white">{{ order.user }}</td>
            <td class="p-4 text-gray-300">{{ order.item }}</td>
            <td class="p-4 font-bold text-neon-blue">¥ {{ order.amount }}</td>
            <td class="p-4">
              <span v-if="order.status === 'completed'" class="text-green-400 flex items-center gap-1"><CheckCircle class="w-3 h-3" /> 已完成</span>
              <span v-else class="text-yellow-400 flex items-center gap-1">处理中</span>
            </td>
            <td class="p-4 text-right text-gray-500">{{ order.date }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
