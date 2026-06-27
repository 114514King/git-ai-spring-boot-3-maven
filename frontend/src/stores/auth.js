import { defineStore } from 'pinia';
import http from '../api/http';

const TOKEN_KEY = 'accessToken';
const EXPIRES_AT_KEY = 'tokenExpiresAt';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    accessToken: localStorage.getItem(TOKEN_KEY) || '',
    expiresAt: localStorage.getItem(EXPIRES_AT_KEY) || '',
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.accessToken),
  },
  actions: {
    async login(payload) {
      const response = await http.post('/auth/login', payload);
      const data = response.data.data;

      this.accessToken = data.accessToken;
      this.expiresAt = data.expiresAt;

      localStorage.setItem(TOKEN_KEY, data.accessToken);
      localStorage.setItem(EXPIRES_AT_KEY, data.expiresAt);

      return data;
    },
    async register(payload) {
      const response = await http.post('/auth/register', payload);
      return response.data.data;
    },
    logout() {
      this.accessToken = '';
      this.expiresAt = '';

      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(EXPIRES_AT_KEY);
    },
  },
});
