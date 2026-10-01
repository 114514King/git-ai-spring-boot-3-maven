<script setup>
import { reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Lock, User } from '@element-plus/icons-vue';
import { extractApiError } from '../api/http';
import { useAuthStore } from '../stores/auth';
import AuthIntro from '../components/AuthIntro.vue';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const formRef = ref();
const loading = ref(false);

const form = reactive({
  account: '',
  password: '',
});

const rules = {
  account: [{ required: true, message: '请输入用户名或邮箱', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
};

async function submit() {
  await formRef.value.validate();
  loading.value = true;

  try {
    await authStore.login(form);
    ElMessage.success('登录成功');
    await router.push(route.query.redirect || '/app');
  } catch (error) {
    ElMessage.error(extractApiError(error));
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <main class="auth-layout">
    <AuthIntro />

    <section class="auth-panel" aria-label="登录表单">
      <div class="panel-heading">
        <div><h2>欢迎回来</h2><p class="auth-panel-note">登录，继续你的下一步。</p></div>
        <RouterLink to="/register">创建账号</RouterLink>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
        <el-form-item label="用户名或邮箱" prop="account">
          <el-input v-model.trim="form.account" :prefix-icon="User" autocomplete="username" placeholder="请输入用户名或邮箱" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            :prefix-icon="Lock"
            type="password"
            autocomplete="current-password"
            show-password
            placeholder="请输入密码"
          />
        </el-form-item>

        <el-button class="submit-button" type="primary" native-type="submit" :loading="loading">
          登录工作台
        </el-button>
      </el-form>
    </section>
  </main>
</template>
