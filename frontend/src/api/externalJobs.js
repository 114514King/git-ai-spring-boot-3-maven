import axios from 'axios';

// Public feed requests stay separate from the client's JWT interceptor.
const publicHttp = axios.create({
  baseURL: '/api',
  timeout: 30000,
});

export async function listExternalJobs(params, signal) {
  const response = await publicHttp.get('/external-jobs', { params, signal });
  const feed = response.data?.data;

  if (!feed || !Array.isArray(feed.jobs)) {
    throw new Error('暂时无法读取外部岗位，请稍后重试。');
  }

  return feed;
}
