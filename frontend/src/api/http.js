import axios from 'axios';

const http = axios.create({
  baseURL: '/api',
  timeout: 10000,
});

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

export function extractApiError(error) {
  return error.response?.data?.message || error.message || '请求失败';
}

export default http;
