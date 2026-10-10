<script setup>
import { ArrowLeft } from '@element-plus/icons-vue';
import ExternalJobsPanel from '../components/ExternalJobsPanel.vue';
</script>

<template>
  <main class="student-shell">
    <header class="student-header">
      <div>
        <h1>外部岗位</h1>
        <p>无需登录即可浏览公开的远程招聘机会；申请需前往原站完成。</p>
      </div>
      <RouterLink v-slot="{ navigate, href }" to="/login" custom>
        <el-button tag="a" :href="href" :icon="ArrowLeft" @click="navigate">返回登录</el-button>
      </RouterLink>
    </header>
    <ExternalJobsPanel />
  </main>
</template>
