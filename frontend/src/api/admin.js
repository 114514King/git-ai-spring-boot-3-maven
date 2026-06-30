import http from './http';

export async function getAdminDashboard() {
  const response = await http.get('/admin/dashboard');
  return response.data.data;
}
