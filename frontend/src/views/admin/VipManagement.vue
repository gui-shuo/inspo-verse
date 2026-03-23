<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Crown, Edit3, Save, X, ChevronLeft, ChevronRight } from 'lucide-vue-next'
import { useToastStore } from '@/stores/toast'
import { getVipMembers, getVipPlans, updateVipPlan } from '@/api/admin'

const toast = useToastStore()

const members = ref<any[]>([])
const plans = ref<any[]>([])
const loadingMembers = ref(false)
const loadingPlans = ref(false)
const currentPage = ref(1)
const pageSize = ref(15)
const total = ref(0)

const editingPlanId = ref<number | null>(null)
const editForm = ref<Record<string, any>>({})

const loadMembers = async () => {
  loadingMembers.value = true
  try {
    const res = await getVipMembers({ page: currentPage.value, size: pageSize.value })
    members.value = res.records || []
    total.value = res.total || 0
  } catch { toast.error('加载会员列表失败') }
  finally { loadingMembers.value = false }
}

const loadPlans = async () => {
  loadingPlans.value = true
  try { plans.value = await getVipPlans() }
  catch { toast.error('加载VIP方案失败') }
  finally { loadingPlans.value = false }
}

const totalPages = () => Math.max(1, Math.ceil(total.value / pageSize.value))
const prevPage = () => { if (currentPage.value > 1) { currentPage.value--; loadMembers() } }
const nextPage = () => { if (currentPage.value < totalPages()) { currentPage.value++; loadMembers() } }

const startEdit = (plan: any) => {
  editingPlanId.value = plan.id
  editForm.value = { name: plan.name, price: plan.price, durationDays: plan.durationDays, description: plan.description }
}

const cancelEdit = () => { editingPlanId.value = null; editForm.value = {} }

const savePlan = async (planId: number) => {
  try {
    await updateVipPlan(planId, editForm.value)
    toast.success('方案已更新')
    editingPlanId.value = null
    loadPlans()
  } catch { toast.error('更新失败') }
}

onMounted(() => { loadMembers(); loadPlans() })
</script>

<template>
  <div class="space-y-8 animate__animated animate__fadeIn">
    <h2 class="text-2xl font-bold text-white">VIP 管理</h2>

    <!-- VIP Plans -->
    <div>
      <h3 class="text-lg font-bold text-white mb-4 flex items-center gap-2"><Crown class="w-5 h-5 text-yellow-400" /> VIP 方案</h3>
      <div v-if="loadingPlans" class="flex items-center justify-center h-20">
        <div class="w-6 h-6 border-4 border-neon-blue border-t-transparent rounded-full animate-spin"></div>
      </div>
      <div v-else class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div v-for="plan in plans" :key="plan.id" class="bg-slate-800 rounded-xl border border-white/5 p-6">
          <template v-if="editingPlanId === plan.id">
            <div class="space-y-3">
              <input v-model="editForm.name" class="w-full bg-slate-900 border border-white/10 rounded px-3 py-1.5 text-white text-sm focus:border-neon-blue focus:outline-none" placeholder="方案名称" />
              <input v-model.number="editForm.price" type="number" step="0.01" class="w-full bg-slate-900 border border-white/10 rounded px-3 py-1.5 text-white text-sm focus:border-neon-blue focus:outline-none" placeholder="价格" />
              <input v-model.number="editForm.durationDays" type="number" class="w-full bg-slate-900 border border-white/10 rounded px-3 py-1.5 text-white text-sm focus:border-neon-blue focus:outline-none" placeholder="天数" />
              <textarea v-model="editForm.description" rows="2" class="w-full bg-slate-900 border border-white/10 rounded px-3 py-1.5 text-white text-sm focus:border-neon-blue focus:outline-none resize-none" placeholder="描述"></textarea>
              <div class="flex gap-2">
                <button @click="savePlan(plan.id)" class="flex-1 bg-neon-blue text-slate-900 font-bold py-1.5 rounded text-sm flex items-center justify-center gap-1"><Save class="w-3 h-3" /> 保存</button>
                <button @click="cancelEdit" class="flex-1 border border-slate-600 text-gray-300 py-1.5 rounded text-sm flex items-center justify-center gap-1"><X class="w-3 h-3" /> 取消</button>
              </div>
            </div>
          </template>
          <template v-else>
            <div class="flex justify-between items-start mb-3">
              <h4 class="font-bold text-white">{{ plan.name }}</h4>
              <button @click="startEdit(plan)" class="p-1 hover:bg-white/10 rounded text-gray-400 hover:text-neon-blue"><Edit3 class="w-4 h-4" /></button>
            </div>
            <div class="text-3xl font-bold text-neon-blue mb-1">¥{{ plan.price }}</div>
            <div class="text-sm text-gray-400 mb-2">{{ plan.durationDays }} 天</div>
            <p class="text-xs text-gray-500">{{ plan.description || '暂无描述' }}</p>
          </template>
        </div>
      </div>
    </div>

    <!-- VIP Members List -->
    <div>
      <h3 class="text-lg font-bold text-white mb-4">活跃会员列表</h3>
      <div v-if="loadingMembers" class="flex items-center justify-center h-32">
        <div class="w-8 h-8 border-4 border-neon-blue border-t-transparent rounded-full animate-spin"></div>
      </div>
      <div v-else class="bg-slate-800 rounded-xl border border-white/5 overflow-hidden">
        <div class="overflow-x-auto">
          <table class="w-full text-left text-sm">
            <thead class="bg-slate-900/50 border-b border-white/5 text-gray-400">
              <tr>
                <th class="p-4">用户</th>
                <th class="p-4">方案</th>
                <th class="p-4">开始时间</th>
                <th class="p-4">到期时间</th>
                <th class="p-4">状态</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-white/5">
              <tr v-for="m in members" :key="m.id" class="hover:bg-white/5">
                <td class="p-4 text-white">{{ m.username || m.userId }}</td>
                <td class="p-4 text-neon-purple font-medium">{{ m.planName || '-' }}</td>
                <td class="p-4 text-gray-400">{{ m.startDate?.substring(0, 10) }}</td>
                <td class="p-4 text-gray-400">{{ m.endDate?.substring(0, 10) }}</td>
                <td class="p-4">
                  <span v-if="m.status === 'active'" class="text-green-400">有效</span>
                  <span v-else class="text-red-400">已过期</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-if="members.length === 0" class="p-8 text-center text-gray-500">暂无VIP会员</div>

        <div v-if="total > pageSize" class="flex items-center justify-between p-4 border-t border-white/5">
          <span class="text-sm text-gray-400">共 {{ total }} 条</span>
          <div class="flex items-center gap-2">
            <button @click="prevPage" :disabled="currentPage <= 1" class="p-1.5 rounded hover:bg-white/10 text-gray-400 disabled:opacity-30"><ChevronLeft class="w-4 h-4" /></button>
            <span class="text-sm text-gray-300">{{ currentPage }} / {{ totalPages() }}</span>
            <button @click="nextPage" :disabled="currentPage >= totalPages()" class="p-1.5 rounded hover:bg-white/10 text-gray-400 disabled:opacity-30"><ChevronRight class="w-4 h-4" /></button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
