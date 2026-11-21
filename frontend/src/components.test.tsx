import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Problem, TaskCard } from './components';
import type { Task } from './api';

const task: Task = {
  id: 12, title: 'Review roadmap', description: 'Share with the team', status: 'TODO',
  priority: 'HIGH', dueDate: '2024-03-15', assignee: { id: 2, name: 'Alex Morgan' }, overdue: true,
};
test('shows an overdue task and requests changes through its controls', async () => {
  const user = userEvent.setup();
  const edit = jest.fn();
  const move = jest.fn();
  render(<TaskCard task={task} onEdit={edit} onMove={move} />);
  expect(screen.getByText(/overdue/i)).toBeVisible();
  expect(screen.getByLabelText('Alex Morgan')).toHaveTextContent('AM');
  await user.click(screen.getByRole('button', { name: task.title }));
  await user.selectOptions(screen.getByRole('combobox', { name: 'Status for Review roadmap' }), 'DONE');
  expect(edit).toHaveBeenCalledTimes(1);
  expect(move).toHaveBeenCalledWith('DONE');
});
test('announces request failures and supports retry', async () => {
  const retry = jest.fn();
  render(<Problem error={new Error('offline')} retry={retry} />);
  expect(screen.getByRole('alert')).toHaveTextContent('Please try again');
  await userEvent.click(screen.getByRole('button', { name: 'Retry' }));
  expect(retry).toHaveBeenCalledTimes(1);
});
