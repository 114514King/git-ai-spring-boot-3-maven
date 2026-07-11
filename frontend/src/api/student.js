import http from './http';

function unwrap(response) {
  return response.data.data;
}

export async function listJobs(params) {
  const response = await http.get('/jobs', { params });
  return unwrap(response);
}

export async function getJob(id) {
  const response = await http.get(`/jobs/${id}`);
  return unwrap(response);
}

export async function listResumes() {
  const response = await http.get('/student/resumes');
  return unwrap(response);
}

export async function createResume(payload) {
  const response = await http.post('/student/resumes', payload);
  return unwrap(response);
}

export async function updateResume(id, payload) {
  const response = await http.put(`/student/resumes/${id}`, payload);
  return unwrap(response);
}

export async function updateResumeStatus(id, status) {
  const response = await http.patch(`/student/resumes/${id}/status`, { status });
  return unwrap(response);
}

export async function optimizeResume(id, payload) {
  const response = await http.post(`/student/resumes/${id}/optimization`, payload);
  return unwrap(response);
}

export async function listApplications() {
  const response = await http.get('/student/applications');
  return unwrap(response);
}

export async function submitApplication(payload) {
  const response = await http.post('/student/applications', payload);
  return unwrap(response);
}

export async function withdrawApplication(id) {
  const response = await http.patch(`/student/applications/${id}/withdraw`);
  return unwrap(response);
}

export async function generateApplicationActionPlan(id) {
  const response = await http.post(`/student/applications/${id}/action-plan`);
  return unwrap(response);
}

export async function listMatches(params) {
  const response = await http.get('/student/matches', { params });
  return unwrap(response);
}

export async function createMatch(payload) {
  const response = await http.post('/student/matches', payload);
  return unwrap(response);
}
