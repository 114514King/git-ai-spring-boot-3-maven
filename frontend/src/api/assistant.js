import http from './http';

export async function sendAssistantMessage(role, payload) {
  const response = await http.post(`/${role}/assistant/chat`, payload, { timeout: 90000 });
  return response.data.data;
}
