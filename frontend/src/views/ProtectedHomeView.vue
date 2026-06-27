<script setup>
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { SwitchButton } from '@element-plus/icons-vue';
import { useAuthStore } from '../stores/auth';

const router = useRouter();
const authStore = useAuthStore();

const expiresAtText = computed(() => {
  if (!authStore.expiresAt) {
    return '未记录';
  }

  return new Date(authStore.expiresAt).toLocaleString();
});

async function logout() {
  authStore.logout();
  ElMessage.success('已退出登录');
  await router.push('/login');
}
</script>

<template>
  <main class="protected-layout">
    <section class="protected-panel" aria-labelledby="protected-title">
      <p class="eyebrow">受保护路由</p>
      <h1 id="protected-title">登录态已生效</h1>
      <p>
        这里仅用于验证 Day 12 的路由守卫和令牌保存。学生端、HR 端和管理员业务页面将在后续任务中实现。
      </p>

      <dl class="token-meta">
        <div>
          <dt>令牌状态</dt>
          <dd>已保存</dd>
        </div>
        <div>
          <dt>过期时间</dt>
          <dd>{{ expiresAtText }}</dd>
        </div>
      </dl>

      <el-button type="primary" :icon="SwitchButton" @click="logout">退出登录</el-button>
    </section>
  </main>
</template>
