import { createRouter, createWebHistory } from 'vue-router';
import LoginView from '../views/LoginView.vue';
import RegisterView from '../views/RegisterView.vue';
import ExternalJobsView from '../views/ExternalJobsView.vue';
import HrDashboardView from '../views/HrDashboardView.vue';
import StudentDashboardView from '../views/StudentDashboardView.vue';
import AdminDashboardView from '../views/AdminDashboardView.vue';
import { useAuthStore } from '../stores/auth';

const routes = [
  {
    path: '/',
    redirect: '/login',
  },
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: { guestOnly: true },
  },
  {
    path: '/register',
    name: 'register',
    component: RegisterView,
    meta: { guestOnly: true },
  },
  {
    path: '/external-jobs',
    name: 'external-jobs',
    component: ExternalJobsView,
  },
  {
    path: '/app',
    name: 'student-dashboard',
    component: StudentDashboardView,
    meta: { requiresAuth: true, roles: ['STUDENT'] },
  },
  {
    path: '/hr',
    name: 'hr-dashboard',
    component: HrDashboardView,
    meta: { requiresAuth: true, roles: ['HR'] },
  },
  {
    path: '/admin',
    name: 'admin-dashboard',
    component: AdminDashboardView,
    meta: { requiresAuth: true, roles: ['ADMIN'] },
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to) => {
  const authStore = useAuthStore();

  const expiry = Date.parse(authStore.expiresAt);
  if (authStore.accessToken && (!Number.isFinite(expiry) || expiry <= Date.now() || !authStore.roles.length)) {
    authStore.logout();
  }

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } };
  }

  if (to.meta.guestOnly && authStore.isAuthenticated) {
    return authStore.dashboardPath;
  }

  if (to.meta.roles && !to.meta.roles.some((role) => authStore.roles.includes(role))) {
    return authStore.dashboardPath;
  }

  return true;
});

export default router;
