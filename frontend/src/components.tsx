import { useEffect, useRef, type ReactNode } from 'react';
import { CalendarDays, CircleAlert, Flag, Layers3, X } from 'lucide-react';
import { errorMessage, labels, statuses, type Status, type Task } from './api';

export const initials = (name: string) => name.trim().split(/\s+/).slice(0, 2).map(part => part[0]).join('').toUpperCase();
export const dateLabel = (date: string) => new Intl.DateTimeFormat(undefined, { month: 'short', day: 'numeric' })
  .format(new Date(date + 'T12:00:00'));
export function Brand() {
  return <div className="brand"><span className="brand-icon"><Layers3 size={20} /></span>TaskFlow</div>;
}
export function Problem({ error, retry }: { error: unknown; retry?: () => void }) {
  return <div className="problem" role="alert"><CircleAlert size={17} /><span>{errorMessage(error)}</span>
    {retry && <button type="button" onClick={retry} className="text-button">Retry</button>}</div>;
}
export function Loading({ label }: { label: string }) {
  return <div className="loading" role="status"><span className="spinner" />{label}</div>;
}
export function Modal({ title, children, onClose, busy }: {
  title: string; children: ReactNode; onClose: () => void; busy?: boolean
}) {
  const ref = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    const dialog = ref.current!;
    dialog.showModal();
    return () => dialog.close();
  }, []);
  return <dialog ref={ref} aria-labelledby="dialog-title" className="modal" onCancel={event => {
    event.preventDefault();
    if (!busy) onClose();
  }}>
    <div className="modal-heading"><h2 id="dialog-title">{title}</h2>
      <button aria-label="Close dialog" type="button" className="icon-button" disabled={busy} onClick={onClose}><X size={20} /></button>
    </div>
    {children}
  </dialog>;
}
export function TaskCard({ task, onEdit, onMove, busy }: {
  task: Task; onEdit: () => void; onMove: (status: Status) => void; busy?: boolean
}) {
  return <article className="task-card">
    <div className="task-meta"><span className={'priority ' + task.priority.toLowerCase()}>
      <Flag size={12} />{task.priority.toLowerCase()}</span><span className="task-id">TF-{task.id}</span></div>
    <button type="button" className="task-title" onClick={onEdit}>{task.title}</button>
    {task.description && <p>{task.description}</p>}
    <div className="task-foot">
      <span className={task.overdue ? 'due overdue' : 'due'}><CalendarDays size={14} />
        {task.overdue ? 'Overdue · ' : ''}{dateLabel(task.dueDate)}</span>
      <span className={task.assignee ? 'avatar' : 'avatar unassigned'} title={task.assignee?.name ?? 'Unassigned'}
        aria-label={task.assignee?.name ?? 'Unassigned'}>{task.assignee ? initials(task.assignee.name) : '–'}</span>
    </div>
    <label className="move-label">Status
      <select aria-label={'Status for ' + task.title} value={task.status} disabled={busy}
        onChange={event => onMove(event.target.value as Status)}>
        {statuses.map(status => <option key={status} value={status}>{labels[status]}</option>)}
      </select>
    </label>
  </article>;
}
