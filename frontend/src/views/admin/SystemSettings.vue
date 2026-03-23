<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Save, RefreshCw } from 'lucide-vue-next'
import { useToastStore } from '@/stores/toast'
import { getAllConfigs, batchUpdateConfigs } from '@/api/admin'

const toast = useToastStore()
const loading = ref(false)
const saving = ref(false)

const configs = ref<Record<string, string>>({})

const loadConfigs = async () => {
  loading.value = true
  try {
    const list = await getAllConfigs()
    const map: Record<string, string> = {}
    for (const item of list) {
      map[item.configKey] = item.configValue
    }
    configs.value = map
  } catch { toast.error('加载配置失败') }
  finally { loading.value = false }
}

const saveSettings = async () => {
  saving.value = true
  try {
    await batchUpdateConfigs(configs.value)
    toast.success('设置已保存')
  } catch { toast.error('保存失败') }
  finally { saving.value = false }
}

onMounted(loadConfigs)
</script>

<template>
  <div class="space-y-6 animate__animated animate__fadeIn">
    <div class="flex justify-between items-center">
      <h2 class="text-2xl font-bold text-white">系统设置</h2>
      <button @click="loadConfigs" class="text-gray-400 hover:text-white p-2 rounded hover:bg-white/10" title="刷新">
        <RefreshCw class="w-5 h-5" :class="loading ? 'animate-spin' : ''" />
      </button>
    </div>

    <div v-if="loading" class="flex items-center justify-center h-32">
      <div class="w-8 h-8 border-4 border-neon-blue border-t-transparent rounded-full animate-spin"></div>
    </div>

    <template v-else>
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-8">
        <!-- 基础设置 -->
        <div class="bg-slate-800 p-6 rounded-xl border border-white/5 space-y-6">
          <h3 class="font-bold text-white border-b border-white/5 pb-4">基础设置</h3>
          
          <div class="space-y-2">
            <label class="text-sm text-gray-400">站点名称</label>
            <input v-model="configs.site_name" type="text" class="w-full bg-slate-900 border border-white/10 rounded-lg px-4 py-2 text-white focus:border-neon-blue focus:outline-none">
          </div>

          <div class="space-y-2">
            <label class="text-sm text-gray-400">默认头像URL</label>
            <input v-model="configs.default_avatar_url" type="text" class="w-full bg-slate-900 border border-white/10 rounded-lg px-4 py-2 text-white focus:border-neon-blue focus:outline-none">
          </div>

          <div class="flex items-center justify-between">
            <div>
              <div class="text-white font-medium">维护模式</div>
              <div class="text-xs text-gray-500">开启后前台将无法访问</div>
            </div>
            <button 
              @click="configs.maintenance_mode = configs.maintenance_mode === 'true' ? 'false' : 'true'"
              class="w-12 h-6 rounded-full transition-colors relative"
              :class="configs.maintenance_mode === 'true' ? 'bg-neon-blue' : 'bg-slate-700'"
            >
              <div class="absolute top-1 left-1 w-4 h-4 bg-white rounded-full transition-transform" :class="configs.maintenance_mode === 'true' ? 'translate-x-6' : ''"></div>
            </button>
          </div>

          <div class="flex items-center justify-between">
            <div>
              <div class="text-white font-medium">开放注册</div>
              <div class="text-xs text-gray-500">关闭后新用户将无法注册</div>
            </div>
            <button 
              @click="configs.enable_registration = configs.enable_registration === 'true' ? 'false' : 'true'"
              class="w-12 h-6 rounded-full transition-colors relative"
              :class="configs.enable_registration === 'true' ? 'bg-neon-blue' : 'bg-slate-700'"
            >
              <div class="absolute top-1 left-1 w-4 h-4 bg-white rounded-full transition-transform" :class="configs.enable_registration === 'true' ? 'translate-x-6' : ''"></div>
            </button>
          </div>

          <div class="space-y-2">
            <label class="text-sm text-gray-400">每日AI最大Token数</label>
            <input v-model="configs.max_ai_tokens_per_day" type="number" class="w-full bg-slate-900 border border-white/10 rounded-lg px-4 py-2 text-white focus:border-neon-blue focus:outline-none">
          </div>
        </div>

        <!-- 公告 & 联系方式 -->
        <div class="space-y-8">
          <div class="bg-slate-800 p-6 rounded-xl border border-white/5 space-y-6">
            <h3 class="font-bold text-white border-b border-white/5 pb-4">发布公告</h3>
            <div class="space-y-2">
              <label class="text-sm text-gray-400">全站公告内容</label>
              <textarea v-model="configs.announcement" rows="3" class="w-full bg-slate-900 border border-white/10 rounded-lg px-4 py-2 text-white focus:border-neon-blue focus:outline-none resize-none"></textarea>
            </div>
          </div>

          <div class="bg-slate-800 p-6 rounded-xl border border-white/5 space-y-6">
            <h3 class="font-bold text-white border-b border-white/5 pb-4">联系方式</h3>
            <div class="space-y-2">
              <label class="text-sm text-gray-400">客服邮箱</label>
              <input v-model="configs.contact_email" type="email" class="w-full bg-slate-900 border border-white/10 rounded-lg px-4 py-2 text-white focus:border-neon-blue focus:outline-none">
            </div>
            <div class="space-y-2">
              <label class="text-sm text-gray-400">客服QQ</label>
              <input v-model="configs.contact_qq" type="text" class="w-full bg-slate-900 border border-white/10 rounded-lg px-4 py-2 text-white focus:border-neon-blue focus:outline-none">
            </div>
            <div class="space-y-2">
              <label class="text-sm text-gray-400">Discord 链接</label>
              <input v-model="configs.contact_discord" type="text" class="w-full bg-slate-900 border border-white/10 rounded-lg px-4 py-2 text-white focus:border-neon-blue focus:outline-none">
            </div>
          </div>
        </div>
      </div>

      <!-- 长文本内容 -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-8">
        <div class="bg-slate-800 p-6 rounded-xl border border-white/5 space-y-4">
          <h3 class="font-bold text-white border-b border-white/5 pb-4">关于我们（支持HTML）</h3>
          <textarea v-model="configs.about_content" rows="8" class="w-full bg-slate-900 border border-white/10 rounded-lg px-4 py-2 text-white focus:border-neon-blue focus:outline-none resize-y font-mono text-xs"></textarea>
        </div>
        <div class="bg-slate-800 p-6 rounded-xl border border-white/5 space-y-4">
          <h3 class="font-bold text-white border-b border-white/5 pb-4">社区规范（支持HTML）</h3>
          <textarea v-model="configs.rules_content" rows="8" class="w-full bg-slate-900 border border-white/10 rounded-lg px-4 py-2 text-white focus:border-neon-blue focus:outline-none resize-y font-mono text-xs"></textarea>
        </div>
      </div>

      <!-- Save Button -->
      <div class="flex justify-end">
        <button @click="saveSettings" :disabled="saving" class="bg-neon-blue text-slate-900 font-bold px-8 py-3 rounded-lg hover:shadow-lg transition-all flex items-center gap-2 disabled:opacity-50">
          <Save class="w-4 h-4" /> {{ saving ? '保存中...' : '保存所有设置' }}
        </button>
      </div>
    </template>
  </div>
</template>
