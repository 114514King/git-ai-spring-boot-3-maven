<script setup>
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Message, Phone, User } from '@element-plus/icons-vue';
import { extractApiError } from '../api/http';
import { useAuthStore } from '../stores/auth';

const router = useRouter();
const authStore = useAuthStore();
const formRef = ref();
const loading = ref(false);

const form = reactive({
  username: '',
  password: '',
  email: '',
  phone: '',
  realName: '',
  role: 'STUDENT',
});

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度为 3 到 50 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 72, message: '密码长度为 8 到 72 个字符', trigger: 'blur' },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效邮箱', trigger: 'blur' },
  ],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
};

async function submit() {
  await formRef.value.validate();
  loading.value = true;

  try {
    await authStore.register(form);
    ElMessage.success('注册成功，请登录');
    await router.push('/login');
  } catch (error) {
    ElMessage.error(extractApiError(error));
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <main class="auth-layout">
    <section class="auth-hero" aria-labelledby="register-title">
      <p class="eyebrow">账号注册</p>
      <h1 id="register-title">选择学生或 HR 身份加入平台</h1>
      <p>注册范围仅开放学生和 HR，管理员账号不支持前端自助注册。</p>
    </section>

    <section class="auth-panel" aria-label="注册表单">
      <div class="panel-heading">
        <h2>注册</h2>
        <RouterLink to="/login">已有账号</RouterLink>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
        <el-form-item label="用户名" prop="username">
          <el-input v-model.trim="form.username" :prefix-icon="User" autocomplete="username" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" autocomplete="new-password" show-password />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input v-model.trim="form.email" :prefix-icon="Message" autocomplete="email" />
        </el-form-item>

        <div class="form-grid">
          <el-form-item label="手机号" prop="phone">
            <el-input v-model.trim="form.phone" :prefix-icon="Phone" autocomplete="tel" />
          </el-form-item>

          <el-form-item label="真实姓名" prop="realName">
            <el-input v-model.trim="form.realName" :prefix-icon="User" autocomplete="name" />
          </el-form-item>
        </div>

        <el-form-item label="注册身份" prop="role">
          <el-radio-group v-model="form.role">
            <el-radio-button label="STUDENT">学生</el-radio-button>
            <el-radio-button label="HR">HR</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-button class="submit-button" type="primary" native-type="submit" :loading="loading">
          注册
        </el-button>
      </el-form>
    </section>
  </main>
</template>
