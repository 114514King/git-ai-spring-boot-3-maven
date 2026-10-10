import { defineStore } from 'pinia';
import http from '../api/http';

const TOKEN_KEY = 'accessToken';
const EXPIRES_AT_KEY = 'tokenExpiresAt';
const ROLES_KEY = 'authRoles';

function savedRoles() {
  try {
    const roles = JSON.parse(localStorage.getItem(ROLES_KEY) || '[]');
    return Array.isArray(roles) ? roles.filter((role) => ['STUDENT', 'HR', 'ADMIN'].includes(role)) : [];
  } catch {
    return [];
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    accessToken: localStorage.getItem(TOKEN_KEY) || '',
    expiresAt: localStorage.getItem(EXPIRES_AT_KEY) || '',
    roles: savedRoles(),
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.accessToken) && state.roles.length > 0
      && Date.parse(state.expiresAt) > Date.now(),
    dashboardPath: (state) => state.roles.includes('ADMIN') ? '/admin' : state.roles.includes('HR') ? '/hr' : '/app',
  },
  actions: {
    async login(payload) {
      const response = await http.post('/auth/login', payload);
      const data = response.data.data;

      this.accessToken = data.accessToken;
      this.expiresAt = data.expiresAt;
      this.roles = data.roles;

      localStorage.setItem(TOKEN_KEY, data.accessToken);
      localStorage.setItem(EXPIRES_AT_KEY, data.expiresAt);
      localStorage.setItem(ROLES_KEY, JSON.stringify(data.roles));

      return data;
    },
    async register(payload) {
      const response = await http.post('/auth/register', payload);
      return response.data.data;
    },
    logout() {
      this.accessToken = '';
      this.expiresAt = '';
      this.roles = [];

      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(EXPIRES_AT_KEY);
      localStorage.removeItem(ROLES_KEY);
    },
  },
});
