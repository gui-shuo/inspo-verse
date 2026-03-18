<script setup lang="ts">
defineProps<{
  text: string
  as?: string
}>()
</script>

<template>
  <component :is="as || 'div'" class="glitch-wrapper relative inline-block font-bold text-white">
    <span class="glitch" :data-text="text">{{ text }}</span>
  </component>
</template>

<style scoped>
.glitch {
  position: relative;
  display: inline-block;
}

.glitch::before,
.glitch::after {
  content: attr(data-text);
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: #0f172a; /* Match bg color */
}

.glitch::before {
  left: 2px;
  text-shadow: -1px 0 #00f3ff;
  clip: rect(24px, 550px, 90px, 0);
  animation: glitch-anim-2 3s infinite linear alternate-reverse;
}

.glitch::after {
  left: -2px;
  text-shadow: -1px 0 #bc13fe;
  clip: rect(85px, 550px, 140px, 0);
  animation: glitch-anim 2.5s infinite linear alternate-reverse;
}

@keyframes glitch-anim {
  0% { clip: rect(10px, 9999px, 31px, 0); }
  20% { clip: rect(74px, 9999px, 86px, 0); }
  40% { clip: rect(32px, 9999px, 26px, 0); }
  60% { clip: rect(55px, 9999px, 15px, 0); }
  80% { clip: rect(98px, 9999px, 73px, 0); }
  100% { clip: rect(81px, 9999px, 2px, 0); }
}

@keyframes glitch-anim-2 {
  0% { clip: rect(65px, 9999px, 100px, 0); }
  20% { clip: rect(28px, 9999px, 6px, 0); }
  40% { clip: rect(90px, 9999px, 19px, 0); }
  60% { clip: rect(15px, 9999px, 63px, 0); }
  80% { clip: rect(4px, 9999px, 91px, 0); }
  100% { clip: rect(50px, 9999px, 34px, 0); }
}
</style>
