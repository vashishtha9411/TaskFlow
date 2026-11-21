import { useEffect, useState } from 'react';
import { keepPreviousData, QueryClient, QueryClientProvider, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, NavLink, Navigate, Outlet, Route, Routes, useNavigate, useOutletContext, useParams } from 'react-router-dom';
import { AlertCircle, ArrowDownUp, CheckCircle2, Circle, Clock3, Folder, FolderKanban, LayoutGrid, ListTodo, LogOut, Plus, SlidersHorizontal } from 'lucide-react';
import { api, labels, priorities, readSession, saveSession, statuses, type Filters, type Project, type Session, type Status, type Task } from './api';
import { Brand, initials, Loading, Problem, TaskCard } from './components';
import { AuthForm, ProjectForm, TaskForm } from './forms';

const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });

export function App() {
  const [session, setSession] = useState(readSession);
  useEffect(() => {
    const sync = () => {
      const next = readSession();
      queryClient.clear();
      setSession(next);
    };
    window.addEventListener('taskflow-session', sync);
    return () => window.removeEventListener('taskflow-session', sync);
  }, []);
  return <QueryClientProvider client={queryClient}>
    <Routes>
      <Route path="/login" element={<AuthForm key="login" mode="login" session={session} />} />
      <Route path="/register" element={<AuthForm key="register" mode="register" session={session} />} />
      <Route element={session ? <Workspace session={session} /> : <Navigate to="/login" replace />}>
        <Route path="/projects" element={<Projects />} />
        <Route path="/projects/:id" element={<ProjectRoute />} />
      </Route>
      <Route path="*" element={<Navigate to="/projects" replace />} />
    </Routes>
  </QueryClientProvider>;
}

const useProjects = () => useQuery({ queryKey: ['projects'], queryFn: api.projects });
function Workspace({ session }: { session: Session }) {
  const projects = useProjects();
  const me = useQuery({ queryKey: ['session'], queryFn: api.me });
  const [create, setCreate] = useState(false);
  const cache = useQueryClient();
  const navigate = useNavigate();
  function created(project: Project) {
    setCreate(false);
    void cache.invalidateQueries({ queryKey: ['projects'] });
    navigate('/projects/' + project.id);
  }
  return <div className="workspace">
    <aside className="sidebar">
      <Brand />
      <nav aria-label="Workspace">
        <span className="nav-heading">Workspace</span>
        <NavLink end to="/projects"><LayoutGrid size={17} />All projects</NavLink>
        <div className="nav-heading project-nav">Projects
          <button className="icon-button" aria-label="New project" onClick={() => setCreate(true)}><Plus size={17} /></button>
        </div>
        {projects.data?.map(project => <NavLink key={project.id} to={'/projects/' + project.id}>
          <Folder size={16} /><span>{project.name}</span>
        </NavLink>)}
      </nav>
      <div className="account"><span className="avatar">{initials(session.user.name)}</span>
        <div><strong>{session.user.name}</strong><span>{session.user.email}</span></div>
        <button className="icon-button" aria-label="Sign out" onClick={() => saveSession(null)}><LogOut size={17} /></button>
      </div>
    </aside>
    <div className="workspace-body">
      <header className="topbar"><Link to="/projects"><LayoutGrid size={16} />Workspace</Link>
        <span className="topbar-user"><span className="avatar">{initials(session.user.name)}</span>{session.user.name}</span>
      </header>
      <main className="main-content">
        {me.isError && <Problem error={me.error} retry={() => void me.refetch()} />}
        <Outlet context={() => setCreate(true)} />
      </main>
    </div>
    {create && <ProjectForm onClose={() => setCreate(false)} onCreated={created} />}
  </div>;
}


function Projects() {
  const projects = useProjects();
  const create = useOutletContext<() => void>();
  return <>
    <div className="page-heading"><div><h1>Projects</h1><p>Manage your projects and track work across your workspace.</p></div>
      <button className="button primary" onClick={create}><Plus size={17} />New project</button></div>
    {projects.isPending && <Loading label="Loading projects…" />}
    {projects.isError && <Problem error={projects.error} retry={() => void projects.refetch()} />}
    {projects.data?.length === 0 && <div className="empty"><FolderKanban size={27} /><h2>No projects yet</h2>
      <p>Create a project to start organizing tasks.</p><button className="button" onClick={create}>Create a project</button></div>}
    <div className="project-grid">{projects.data?.map(project => <Link key={project.id} className="project-card" to={'/projects/' + project.id}>
      <span className="project-icon"><FolderKanban size={21} /></span><h2>{project.name}</h2>
      <p>{project.description}</p><span className="project-count"><ListTodo size={15} />{project.taskCount} {project.taskCount === 1 ? 'task' : 'tasks'}</span>
    </Link>)}</div>
  </>;
}

function ProjectRoute() {
  const id = Number(useParams().id);
  return Number.isSafeInteger(id) && id > 0 ? <Board key={id} id={id} /> : <Navigate to="/projects" replace />;
}

const columns = [
  { status: 'TODO', count: 'todoCount', icon: Circle },
  { status: 'IN_PROGRESS', count: 'inProgressCount', icon: Clock3 },
  { status: 'DONE', count: 'doneCount', icon: CheckCircle2 },
] as const;
const initialFilters: Filters = { status: '', priority: '', assigneeId: '', sortBy: 'dueDate', direction: 'asc', page: 0 };
function Board({ id }: { id: number }) {
  const [filters, setFilters] = useState(initialFilters);
  const [editing, setEditing] = useState<Task | 'new' | null>(null);
  const cache = useQueryClient();
  const project = useQuery({ queryKey: ['project', id], queryFn: () => api.project(id) });
  const ready = project.isSuccess;
  const summary = useQuery({ queryKey: ['summary', id], queryFn: () => api.summary(id), enabled: ready });
  const users = useQuery({ queryKey: ['users'], queryFn: api.users, enabled: ready });
  const tasks = useQuery({
    queryKey: ['tasks', id, filters], queryFn: ({ signal }) => api.tasks(id, filters, signal),
    enabled: ready, placeholderData: keepPreviousData,
  });
  const refresh = () => Promise.all([
    cache.invalidateQueries({ queryKey: ['tasks', id] }),
    cache.invalidateQueries({ queryKey: ['summary', id] }),
    cache.invalidateQueries({ queryKey: ['projects'] }),
  ]);
  const move = useMutation({
    mutationFn: ({ task, status }: { task: Task; status: Status }) => api.moveTask(task.id, status),
    onSuccess: refresh,
  });
  useEffect(() => {
    if (tasks.data && !tasks.isPlaceholderData && filters.page >= tasks.data.totalPages && filters.page > 0) {
      const page = Math.max(0, tasks.data.totalPages - 1);
      setFilters(current => ({ ...current, page }));
    }
  }, [tasks.data, tasks.isPlaceholderData, filters.page]);
  function filter(key: keyof Omit<Filters, 'page'>, value: string) {
    setFilters(current => ({ ...current, [key]: value, page: 0 }));
  }
  if (project.isPending) return <Loading label="Loading project…" />;
  if (project.isError) return <Problem error={project.error} retry={() => void project.refetch()} />;
  const stats = [
    { label: 'Total tasks', value: summary.data?.totalTasks, icon: ListTodo, color: 'purple' },
    { label: 'In progress', value: summary.data?.inProgressCount, icon: Clock3, color: 'blue' },
    { label: 'Completed', value: summary.data?.doneCount, icon: CheckCircle2, color: 'green' },
    { label: 'Overdue', value: summary.data?.overdueCount, icon: AlertCircle, color: 'red' },
  ];
  return <>
    <div className="page-heading"><div><div className="breadcrumb"><Link to="/projects">Projects</Link><span>/</span>Project board</div>
      <h1>{project.data.name}</h1>{project.data.description && <p>{project.data.description}</p>}</div>
      <button className="button primary" disabled={!users.isSuccess} onClick={() => setEditing('new')}><Plus size={17} />New task</button></div>
    {summary.isPending && <Loading label="Loading task summary…" />}
    {summary.isError && <Problem error={summary.error} retry={() => void summary.refetch()} />}
    {summary.data && <section className="stats" aria-label="Project summary">{stats.map(({ label, value, icon: Icon, color }) =>
      <div className="stat" key={label}><span className={'stat-icon ' + color}><Icon size={18} /></span>
        <div><span>{label}</span><strong>{value}</strong></div></div>
    )}</section>}
    <div className="board-bar"><h2><FolderKanban size={19} />Task board</h2>
      <div className="filters"><SlidersHorizontal size={16} aria-hidden="true" />
        <select aria-label="Filter by status" value={filters.status} onChange={event => filter('status', event.target.value)}>
          <option value="">All statuses</option>{statuses.map(status => <option key={status} value={status}>{labels[status]}</option>)}
        </select>
        <select aria-label="Filter by priority" value={filters.priority} onChange={event => filter('priority', event.target.value)}>
          <option value="">All priorities</option>{priorities.map(priority => <option key={priority} value={priority}>{priority[0] + priority.slice(1).toLowerCase()}</option>)}
        </select>
        <select aria-label="Filter by assignee" disabled={!users.isSuccess} value={filters.assigneeId} onChange={event => filter('assigneeId', event.target.value)}>
          <option value="">All assignees</option><option value="0">Unassigned</option>
          {users.data?.map(user => <option key={user.id} value={user.id}>{user.name}</option>)}
        </select>
        <select aria-label="Sort tasks" value={filters.sortBy + ':' + filters.direction} onChange={event => {
          const [sortBy, direction] = event.target.value.split(':');
          setFilters(current => ({ ...current, sortBy, direction, page: 0 }));
        }}>
          <option value="dueDate:asc">Due date (earliest)</option><option value="dueDate:desc">Due date (latest)</option>
          <option value="priority:desc">Priority (highest)</option><option value="priority:asc">Priority (lowest)</option>
        </select><ArrowDownUp size={15} aria-hidden="true" />
      </div>
    </div>
    {users.isPending && <Loading label="Loading assignees…" />}
    {users.isError && <Problem error={users.error} retry={() => void users.refetch()} />}
    {tasks.isFetching && <Loading label="Loading tasks…" />}
    {tasks.isError && <Problem error={tasks.error} retry={() => void tasks.refetch()} />}
    {move.isError && <Problem error={move.error} retry={() => move.variables && move.mutate(move.variables)} />}
    {tasks.data && <>
      <div className="board" aria-busy={tasks.isFetching}>
        {columns.map(({ status, count, icon: Icon }) => {
          const items = tasks.data.content.filter(task => task.status === status);
          return <section key={status} className={'column ' + status.toLowerCase()} aria-label={labels[status]}>
            <div className="column-heading"><Icon size={17} /><h3>{labels[status]}</h3><span>{summary.data?.[count] ?? items.length}</span></div>
            {items.length === 0 ? <p className="column-empty">No tasks on this page</p> : items.map(task => <TaskCard key={task.id}
              task={task} busy={move.isPending || tasks.isPlaceholderData}
              onEdit={() => users.isSuccess && setEditing(task)} onMove={status => move.mutate({ task, status })} />)}
          </section>;
        })}
      </div>
      <div className="pagination"><span>{tasks.data.totalElements} matching tasks</span>
        <button className="button" disabled={filters.page === 0 || tasks.isFetching} onClick={() => setFilters(current => ({ ...current, page: current.page - 1 }))}>Previous</button>
        <span>Page {filters.page + 1} of {Math.max(1, tasks.data.totalPages)}</span>
        <button className="button" disabled={filters.page + 1 >= tasks.data.totalPages || tasks.isFetching} onClick={() => setFilters(current => ({ ...current, page: current.page + 1 }))}>Next</button>
      </div>
    </>}
    {editing && users.data && <TaskForm projectId={id} task={editing === 'new' ? undefined : editing} users={users.data}
      onClose={() => setEditing(null)} onSaved={() => { setEditing(null); void refresh(); }} />}
  </>;
}
