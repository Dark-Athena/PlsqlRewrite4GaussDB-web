<template>
  <div>页面不存在，正在跳转到首页...</div>
</template>
<script setup>
import { onMounted } from 'vue';
import { useRouter } from 'vue-router';
const router = useRouter();
onMounted(() => {
  setTimeout(() => {
    router.replace('/');
  }, 1500);
});
</script> 