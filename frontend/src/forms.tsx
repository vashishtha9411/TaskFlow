import { useMutation } from '@tanstack/react-query';
import { useForm, type FieldValues, type Path, type UseFormSetError } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Link, Navigate } from 'react-router-dom';
import { CheckCircle2, Layers3, Plus, Trash2 } from 'lucide-react';
import { cloneElement, useId, useState, type ReactElement } from 'react';
import { z } from 'zod';
import { api, errorDetails, labels, priorities, saveSession, statuses, type Project, type Session, type Task, type Assignee } from './api';
import { Brand, Modal, Problem } from './components';

function applyFieldErrors<T extends FieldValues>(error: unknown, setError: UseFormSetError<T>, fields: readonly Path<T>[]) {
  for (const issue of errorDetails(error)?.validationErrors ?? []) {
    const field = fields.find(name => name === issue.field);
    if (field) setError(field, { message: issue.message });
  }
}

function Field({ label, error, children }: {
  label: string;
  error?: string;
  children: ReactElement<{ id?: string; 'aria-invalid'?: boolean; 'aria-describedby'?: string }>;
}) {
  const id = useId();
  const errorId = id + '-error';
  return <div className="field">
    <label htmlFor={id}>{label}</label>
    {cloneElement(children, { id, 'aria-invalid': !!error, 'aria-describedby': error ? errorId : undefined })}
    {error && <small id={errorId} role="alert">{error}</small>}
  </div>;
}

const authSchema = z.object({
  name: z.string().trim().min(2, 'Enter at least 2 characters').max(100).optional(),
  email: z.string().trim().email('Enter a valid email').max(150),
  password: z.string().min(6, 'Enter at least 6 characters')
    .refine(value => new TextEncoder().encode(value).length <= 72, 'Password must fit in 72 UTF-8 bytes'),
});

export function AuthForm({ mode, session }: { mode: 'login' | 'register'; session: Session | null }) {
  const registering = mode === 'register';
  const { register, handleSubmit, setError, formState: { errors } } = useForm<z.infer<typeof authSchema>>({
    resolver: zodResolver(authSchema),
  });
  const submit = useMutation({
    mutationFn: (input: z.infer<typeof authSchema>) => api.authenticate(mode, input),
    onSuccess: saveSession,
    onError: error => applyFieldErrors(error, setError, ['name', 'email', 'password']),
  });
  if (session) return <Navigate to="/projects" replace />;
  return <main className="auth-shell">
    <section className="auth-panel">
      <Brand />
      <div className="auth-form">
        <h1>{registering ? 'Create your account' : 'Welcome back'}</h1>
        <p>{registering ? 'Set up your TaskFlow workspace.' : 'Sign in to manage your projects and tasks.'}</p>
        <form onSubmit={handleSubmit(data => submit.mutate(data))} noValidate>
          {registering && <Field label="Full name" error={errors.name?.message}>
            <input autoComplete="name" placeholder="Alex Morgan" {...register('name')} />
          </Field>}
          <Field label="Email address" error={errors.email?.message}>
            <input type="email" autoComplete="email" placeholder="you@example.com" {...register('email')} />
          </Field>
          <Field label="Password" error={errors.password?.message}>
            <input type="password" autoComplete={registering ? 'new-password' : 'current-password'}
              placeholder={registering ? 'At least 6 characters' : 'Enter your password'} {...register('password')} />
          </Field>
          {submit.isError && <Problem error={submit.error} />}
          <button className="button primary" disabled={submit.isPending}>{submit.isPending ? 'Please wait…' : registering ? 'Create account' : 'Sign in'}</button>
        </form>
        <p className="auth-switch">{registering ? 'Already have an account?' : 'New to TaskFlow?'}{' '}
          <Link to={registering ? '/login' : '/register'}>{registering ? 'Sign in' : 'Create an account'}</Link>
        </p>
      </div>
    </section>
    <aside className="auth-art">
      <div className="auth-art-content"><Layers3 size={38} /><h2>Keep projects moving.</h2>
        <div className="art-board" aria-hidden="true">
          {['To Do', 'In Progress', 'Done'].map((label, index) => <div key={label} className="art-column">
            <span>{label}</span>
            <div className="art-task">{index === 2 && <CheckCircle2 size={16} />}<i /><i /></div>
            {index !== 1 && <div className="art-task"><i /><i /></div>}
          </div>)}
        </div>
      </div>
    </aside>
  </main>;
}

const projectSchema = z.object({
  name: z.string().trim().min(1, 'Enter a project name').max(100),
  description: z.string().trim().max(500),
});
export function ProjectForm({ onClose, onCreated }: { onClose: () => void; onCreated: (project: Project) => void }) {
  const { register, handleSubmit, setError, formState: { errors } } = useForm<z.infer<typeof projectSchema>>({
    resolver: zodResolver(projectSchema),
  });
  const submit = useMutation({
    mutationFn: api.createProject, onSuccess: onCreated,
    onError: error => applyFieldErrors(error, setError, ['name', 'description']),
  });
  return <Modal title="New project" onClose={onClose} busy={submit.isPending}>
    <form onSubmit={handleSubmit(data => submit.mutate(data))}>
      <Field label="Project name" error={errors.name?.message}><input placeholder="Product roadmap" {...register('name')} /></Field>
      <Field label="Description" error={errors.description?.message}><textarea rows={3} {...register('description')} /></Field>
      {submit.isError && <Problem error={submit.error} />}
      <div className="form-actions"><button type="button" className="button" onClick={onClose} disabled={submit.isPending}>Cancel</button>
        <button className="button primary" disabled={submit.isPending}><Plus size={16} />{submit.isPending ? 'Creating…' : 'Create project'}</button></div>
    </form>
  </Modal>;
}

const taskSchema = z.object({
  title: z.string().trim().min(1, 'Enter a task title').max(120),
  description: z.string().trim().max(2000),
  status: z.enum(statuses), priority: z.enum(priorities),
  dueDate: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, 'Choose a due date'),
  assigneeId: z.string(),
});
function today() {
  const date = new Date();
  return [date.getFullYear(), date.getMonth() + 1, date.getDate()].map(value => String(value).padStart(2, '0')).join('-');
}
export function TaskForm({ projectId, task, users, onClose, onSaved }: {
  projectId: number; task?: Task; users: Assignee[]; onClose: () => void; onSaved: () => void
}) {
  const [confirmDelete, setConfirmDelete] = useState(false);
  const { register, handleSubmit, setError, formState: { errors } } = useForm<z.infer<typeof taskSchema>>({
    resolver: zodResolver(taskSchema.refine(data => task || data.dueDate >= today(), {
      path: ['dueDate'], message: 'Choose today or a future date',
    })),
    defaultValues: {
      title: task?.title ?? '', description: task?.description ?? '', status: task?.status ?? 'TODO',
      priority: task?.priority ?? 'MEDIUM', dueDate: task?.dueDate ?? today(),
      assigneeId: task?.assignee ? String(task.assignee.id) : '',
    },
  });
  const save = useMutation({
    mutationFn: (data: z.infer<typeof taskSchema>) => {
      const input = { ...data, assigneeId: data.assigneeId ? Number(data.assigneeId) : null };
      return task ? api.updateTask(task.id, input) : api.createTask(projectId, input);
    },
    onSuccess: onSaved,
    onError: error => applyFieldErrors(error, setError, ['title', 'description', 'status', 'priority', 'dueDate', 'assigneeId']),
  });
  const remove = useMutation({ mutationFn: api.deleteTask, onSuccess: onSaved });
  const busy = save.isPending || remove.isPending;
  return <Modal title={task ? 'Edit task' : 'New task'} onClose={onClose} busy={busy}>
    <form onSubmit={handleSubmit(data => save.mutate(data))}>
      <Field label="Task title" error={errors.title?.message}><input {...register('title')} /></Field>
      <Field label="Description" error={errors.description?.message}><textarea rows={3} {...register('description')} /></Field>
      <div className="form-grid">
        <Field label="Priority"><select {...register('priority')}>{priorities.map(priority =>
          <option key={priority} value={priority}>{priority.toLowerCase().replace(/^./, c => c.toUpperCase())}</option>)}</select></Field>
        <Field label="Due date" error={errors.dueDate?.message}><input type="date" {...register('dueDate')} /></Field>
        <Field label="Assignee" error={errors.assigneeId?.message}><select {...register('assigneeId')}>
          <option value="">Unassigned</option>{users.map(user => <option key={user.id} value={user.id}>{user.name}</option>)}
        </select></Field>
        {task && <Field label="Status"><select {...register('status')}>{statuses.map(status =>
          <option key={status} value={status}>{labels[status]}</option>)}</select></Field>}
      </div>
      {save.isError && <Problem error={save.error} />}
      {remove.isError && <Problem error={remove.error} />}
      {confirmDelete && task ? <div className="delete-confirm">
        <p>Delete this task permanently?</p>
        <button type="button" className="button" disabled={busy} onClick={() => setConfirmDelete(false)}>Keep task</button>
        <button type="button" className="button danger" disabled={busy} onClick={() => remove.mutate(task.id)}>
          {remove.isPending ? 'Deleting…' : 'Delete task'}</button>
      </div> : <div className="form-actions">
        {task && <button type="button" className="button delete-button" disabled={busy} onClick={() => setConfirmDelete(true)}><Trash2 size={15} />Delete</button>}
        <button type="button" className="button" disabled={busy} onClick={onClose}>Cancel</button>
        <button className="button primary" disabled={busy}>{save.isPending ? 'Saving…' : task ? 'Save changes' : 'Create task'}</button>
      </div>}
    </form>
  </Modal>;
}
