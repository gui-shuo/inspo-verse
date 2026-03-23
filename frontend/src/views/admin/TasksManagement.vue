<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Save, X, Edit3 } from 'lucide-vue-next'
import { useToastStore } from '@/stores/toast'
import { getDailyTasks, updateDailyTask } from '@/api/admin'

const toast = useToastStore()
const tasks = ref<any[]>([])
const loading = ref(false)
const editingId = ref<number | null>(null)
const editForm = ref<Record<string, any>>({})

const loadTasks = async () => {
  loading.value = true
  try { tasks.value = await getDailyTasks() }
  catch { toast.error('加载任务失败') }
  finally { loading.value = false }
}

const startEdit = (task: any) => {
  editingId.value = task.id
  editForm.value = {
    taskName: task.taskName,
    description: task.description,
    rewardPoints: task.rewardPoints,
    rewardExp: task.rewardExp,
    dailyLimit: task.dailyLimit,
    status: task.status
  }
}

const cancelEdit = () => { editingId.value = null }

const saveTask = async (taskId: number) => {
  try {
    await updateDailyTask(taskId, editForm.value)
    toast.success('任务已更新')
    editingId.value = null
    loadTasks()
  } catch { toast.error('更新失败') }
}

onMounted(loadTasks)
</script>

<template>
  <div class="space-y-6 animate__animated animate__fadeIn">
    <h2 class="text-2xl font-bold text-white">每日任务管理</h2>

    <div v-if="loading" class="flex items-center justify-center h-32">
      <div class="w-8 h-8 border-4 border-neon-blue border-t-transparent rounded-full animate-spin"></div>
    </div>

    <div v-else class="bg-slate-800 rounded-xl border border-white/5 overflow-hidden">
      <div class="overflow-x-auto">
        <table class="w-full text-left text-sm">
          <thead class="bg-slate-900/50 border-b border-white/5 text-gray-400">
            <tr>
              <th class="p-4">任务名称</th>
              <th class="p-4">描述</th>
              <th class="p-4">积分奖励</th>
              <th class="p-4">经验奖励</th>
              <th class="p-4">每日上限</th>
              <th class="p-4">状态</th>
              <th class="p-4 text-right">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-white/5">
            <tr v-for="task in tasks" :key="task.id" class="hover:bg-white/5 transition-colors">
              <template v-if="editingId === task.id">
                <td class="p-4"><input v-model="editForm.taskName" class="w-full bg-slate-900 border border-white/10 rounded px-2 py-1 text-white text-sm focus:border-neon-blue focus:outline-none" /></td>
                <td class="p-4"><input v-model="editForm.description" class="w-full bg-slate-900 border border-white/10 rounded px-2 py-1 text-white text-sm focus:border-neon-blue focus:outline-none" /></td>
                <td class="p-4"><input v-model.number="editForm.rewardPoints" type="number" class="w-20 bg-slate-900 border border-white/10 rounded px-2 py-1 text-white text-sm focus:border-neon-blue focus:outline-none" /></td>
                <td class="p-4"><input v-model.number="editForm.rewardExp" type="number" class="w-20 bg-slate-900 border border-white/10 rounded px-2 py-1 text-white text-sm focus:border-neon-blue focus:outline-none" /></td>
                <td class="p-4"><input v-model.number="editForm.dailyLimit" type="number" class="w-20 bg-slate-900 border border-white/10 rounded px-2 py-1 text-white text-sm focus:border-neon-blue focus:outline-none" /></td>
                <td class="p-4">
                  <select v-model.number="editForm.status" class="bg-slate-900 border border-white/10 rounded px-2 py-1 text-white text-sm focus:border-neon-blue focus:outline-none">
                    <option :value="1">启用</option>
                    <option :value="0">禁用</option>
                  </select>
                </td>
                <td class="p-4 text-right space-x-1">
                  <button @click="saveTask(task.id)" class="p-2 hover:bg-neon-blue/20 rounded text-neon-blue"><Save class="w-4 h-4" /></button>
                  <button @click="cancelEdit" class="p-2 hover:bg-white/10 rounded text-gray-400"><X class="w-4 h-4" /></button>
                </td>
              </template>
              <template v-else>
                <td class="p-4 font-medium text-white">{{ task.taskName }}</td>
                <td class="p-4 text-gray-400 max-w-[200px] truncate">{{ task.description || '-' }}</td>
                <td class="p-4 text-neon-blue font-mono">{{ task.rewardPoints }}</td>
                <td class="p-4 text-neon-purple font-mono">{{ task.rewardExp }}</td>
                <td class="p-4 text-gray-300">{{ task.dailyLimit }}</td>
                <td class="p-4">
                  <span v-if="task.status === 1" class="text-green-400">启用</span>
                  <span v-else class="text-red-400">禁用</span>
                </td>
                <td class="p-4 text-right">
                  <button @click="startEdit(task)" class="p-2 hover:bg-white/10 rounded text-gray-400 hover:text-neon-blue"><Edit3 class="w-4 h-4" /></button>
                </td>
              </template>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-if="tasks.length === 0" class="p-8 text-center text-gray-500">暂无任务配置</div>
    </div>
  </div>
</template>
