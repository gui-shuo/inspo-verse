<script setup lang="ts">
// Footer 组件，将在 App.vue 中异步加载
</script>

<template>
  <footer class="bg-slate-900 border-t border-white/10 pt-16 pb-8 mt-20">
    <div class="container mx-auto px-4">
      <div class="grid grid-cols-1 md:grid-cols-4 gap-12 mb-12">
        <div class="space-y-4">
          <h3 class="text-xl font-bold text-white">Inspo-Verse</h3>
          <p class="text-gray-400 text-sm leading-relaxed">
            连接每一个创意灵魂的泛娱乐社区。<br>在这里，灵感永不枯竭。
          </p>
        </div>
        
        <div>
          <h4 class="text-white font-bold mb-6">探索</h4>
          <ul class="space-y-3 text-sm text-gray-400">
            <li><RouterLink to="/games" class="hover:text-neon-blue transition-colors">热门游戏</RouterLink></li>
            <li><RouterLink to="/anime" class="hover:text-neon-blue transition-colors">动漫番剧</RouterLink></li>
            <li><RouterLink to="/workshop" class="hover:text-neon-blue transition-colors">创意工坊</RouterLink></li>
          </ul>
        </div>

        <div>
          <h4 class="text-white font-bold mb-6">社区</h4>
          <ul class="space-y-3 text-sm text-gray-400">
            <li><RouterLink to="/about" class="hover:text-neon-blue transition-colors">关于我们</RouterLink></li>
            <li><RouterLink to="/rules" class="hover:text-neon-blue transition-colors">社区规范</RouterLink></li>
            <li><RouterLink to="/contact" class="hover:text-neon-blue transition-colors">联系客服</RouterLink></li>
          </ul>
        </div>

        <div>
          <h4 class="text-white font-bold mb-6">订阅动态</h4>
          <div class="flex gap-2">
            <input type="email" placeholder="输入您的邮箱" class="bg-slate-800 border border-slate-700 rounded-lg px-4 py-2 text-sm w-full focus:outline-none focus:border-neon-blue transition-colors">
            <button class="p-2 bg-neon-blue rounded-lg text-slate-900 hover:bg-neon-blue/80 transition-colors">
              Go
            </button>
          </div>
        </div>
      </div>
      
      <div class="border-t border-white/5 pt-8 text-center text-gray-500 text-sm">
        &copy; 2025 Inspo-Verse. All rights reserved. Designed for Vue 3 Course.
      </div>
    </div>
  </footer>
</template>
