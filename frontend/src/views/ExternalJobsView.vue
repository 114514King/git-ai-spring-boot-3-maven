<script setup>
import { ArrowLeft } from '@element-plus/icons-vue';
import ExternalJobsPanel from '../components/ExternalJobsPanel.vue';
</script>

<template>
  <main class="student-shell">
    <header class="student-header">
      <div>
        <h1>外部岗位</h1>
        <p>发现公开的远程招聘机会，查看原站职位详情。</p>
      </div>
      <RouterLink class="external-back-link" to="/login"><el-icon><ArrowLeft /></el-icon>返回登录</RouterLink>
    </header>
    <ExternalJobsPanel />
  </main>
</template>

<style scoped>
.external-back-link { display: inline-flex; flex-shrink: 0; align-items: center; gap: 8px; font-size: 14px; }
</style>
