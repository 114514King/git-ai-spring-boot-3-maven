<script setup>
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Message, Phone, User } from '@element-plus/icons-vue';
import { extractApiError } from '../api/http';
import { useAuthStore } from '../stores/auth';
import AuthIntro from '../components/AuthIntro.vue';

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
  if (!await formRef.value.validate().catch(() => false)) return;
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
    <AuthIntro register />

    <section class="auth-panel" aria-label="注册表单">
      <div class="panel-heading">
        <div><h2>创建你的账号</h2><p class="auth-panel-note">学生与 HR 均可加入，带 * 为必填项。</p></div>
        <RouterLink class="auth-switch" to="/login">已有账号</RouterLink>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
        <el-form-item label="注册身份" prop="role">
          <el-radio-group v-model="form.role" class="role-choice" aria-label="注册身份">
            <el-radio-button value="STUDENT">学生 · 求职</el-radio-button>
            <el-radio-button value="HR">HR · 招聘</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="用户名" prop="username">
          <el-input v-model.trim="form.username" :prefix-icon="User" autocomplete="username" placeholder="3 到 50 个字符" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" autocomplete="new-password" show-password placeholder="8 到 72 个字符" />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input v-model.trim="form.email" :prefix-icon="Message" autocomplete="email" />
        </el-form-item>

        <div class="form-grid">
          <el-form-item label="手机号" prop="phone">
            <el-input v-model.trim="form.phone" :prefix-icon="Phone" autocomplete="tel" placeholder="选填" />
          </el-form-item>

          <el-form-item label="真实姓名" prop="realName">
            <el-input v-model.trim="form.realName" :prefix-icon="User" autocomplete="name" placeholder="选填" />
          </el-form-item>
        </div>


        <el-button class="submit-button" type="primary" native-type="submit" :loading="loading" :disabled="loading">
          注册
        </el-button>
      </el-form>
    </section>
  </main>
</template>
