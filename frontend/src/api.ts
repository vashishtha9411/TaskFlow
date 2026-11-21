import axios from 'axios';

export const statuses = ['TODO', 'IN_PROGRESS', 'DONE'] as const;
export const priorities = ['LOW', 'MEDIUM', 'HIGH'] as const;
export type Status = typeof statuses[number];
export type Priority = typeof priorities[number];
export const labels: Record<Status, string> = { TODO: 'To Do', IN_PROGRESS: 'In Progress', DONE: 'Done' };
export interface Assignee { id: number; name: string }
export interface User extends Assignee { email: string }
export interface Session { token: string; user: User }
export interface Project { id: number; name: string; description: string | null; taskCount: number }
export interface Task {
  id: number; title: string; description: string | null; status: Status; priority: Priority;
  dueDate: string; assignee: Assignee | null; overdue: boolean;
}
export interface Summary { totalTasks: number; todoCount: number; inProgressCount: number; doneCount: number; overdueCount: number }
export interface Page<T> { content: T[]; totalPages: number; totalElements: number }
export interface TaskInput {
  title: string; description: string; status: Status; priority: Priority; dueDate: string; assigneeId: number | null;
}
export interface Filters { status: string; priority: string; assigneeId: string; sortBy: string; direction: string; page: number }

const client = axios.create({ baseURL: '/api', timeout: 15000 });
const key = 'taskflow.session';
export function readSession(): Session | null {
  try {
    const value = JSON.parse(sessionStorage.getItem(key) ?? 'null') as Session | null;
    return value?.token && value.user ? value : null;
  } catch { return null; }
}
export function saveSession(session: Session | null) {
  if (session) sessionStorage.setItem(key, JSON.stringify(session));
  else sessionStorage.removeItem(key);
  window.dispatchEvent(new Event('taskflow-session'));
}
client.interceptors.request.use(config => {
  const { token } = readSession() ?? {};
  if (token) config.headers.Authorization = 'Bearer ' + token;
  return config;
});
client.interceptors.response.use(response => response, error => {
  if (axios.isAxiosError(error) && error.response?.status === 401 &&
      error.config?.headers.Authorization === 'Bearer ' + readSession()?.token &&
      !['/auth/login', '/auth/register'].includes(error.config?.url ?? '')) {
    saveSession(null);
  }
  return Promise.reject(error);
});

export function errorDetails(error: unknown) {
  if (axios.isAxiosError<{ message?: string; validationErrors?: {field: string; message: string}[] }>(error)) {
    return error.response?.data;
  }
}

export function errorMessage(error: unknown) {
  return errorDetails(error)?.message ?? 'TaskFlow could not complete the request. Please try again.';
}

export const api = {
  authenticate: (mode: 'login' | 'register', input: { name?: string; email: string; password: string }) =>
    client.post<Session>('/auth/' + mode, input).then(r => r.data),
  me: () => client.get<User>('/auth/me').then(r => r.data),
  projects: () => client.get<Project[]>('/projects').then(r => r.data),
  project: (id: number) => client.get<Project>('/projects/' + id).then(r => r.data),
  createProject: (input: { name: string; description: string }) => client.post<Project>('/projects', input).then(r => r.data),
  users: () => client.get<Assignee[]>('/users').then(r => r.data),
  summary: (id: number) => client.get<Summary>('/projects/' + id + '/summary').then(r => r.data),
  tasks: (id: number, filters: Filters, signal: AbortSignal) =>
    client.get<Page<Task>>('/projects/' + id + '/tasks', { params: { ...filters, size: 15 }, signal }).then(r => r.data),
  createTask: (id: number, { status: _status, ...input }: TaskInput) =>
    client.post<Task>('/projects/' + id + '/tasks', input).then(r => r.data),
  updateTask: (id: number, input: TaskInput) => client.put<Task>('/tasks/' + id, input).then(r => r.data),
  moveTask: (id: number, status: Status) => client.patch<Task>('/tasks/' + id + '/status', { status }).then(r => r.data),
  deleteTask: (id: number) => client.delete('/tasks/' + id),
};
