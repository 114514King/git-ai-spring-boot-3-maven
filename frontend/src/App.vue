<script setup>
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import PlatformBrand from './components/PlatformBrand.vue';

const route = useRoute();
const context = computed(() => ({ '/app': '学生工作台', '/hr': 'HR 工作台', '/admin': '管理员看板', '/external-jobs': '外部岗位' })[route.path] || '求职 · 招聘');
</script>

<template>
  <PlatformBrand :context="context" />
  <RouterView />
</template>
