import http from './http';

function unwrap(response) {
  return response.data.data;
}

export async function listHrJobs() {
  const response = await http.get('/hr/jobs');
  return unwrap(response);
}

export async function createHrJob(payload) {
  const response = await http.post('/hr/jobs', payload);
  return unwrap(response);
}

export async function updateHrJob(id, payload) {
  const response = await http.put(`/hr/jobs/${id}`, payload);
  return unwrap(response);
}

export async function updateHrJobStatus(id, status) {
  const response = await http.patch(`/hr/jobs/${id}/status`, { status });
  return unwrap(response);
}

export async function analyzeHrJobJd(id) {
  const response = await http.post(`/hr/jobs/${id}/jd-analysis`);
  return unwrap(response);
}

export async function listHrApplications(params) {
  const response = await http.get('/hr/applications', { params });
  return unwrap(response);
}

export async function listHrCandidateRecommendations(params) {
  const response = await http.get('/hr/applications/recommendations', { params });
  return unwrap(response);
}

export async function updateHrApplicationStatus(id, status) {
  const response = await http.patch(`/hr/applications/${id}/status`, { status });
  return unwrap(response);
}

export async function listHrMatches(params) {
  const response = await http.get('/hr/matches', { params });
  return unwrap(response);
}

export async function createHrMatch(payload) {
  const response = await http.post('/hr/matches', payload);
  return unwrap(response);
}
