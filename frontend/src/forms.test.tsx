import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { api } from './api';
import { TaskForm } from './forms';

HTMLDialogElement.prototype.showModal = function () { this.open = true; };
HTMLDialogElement.prototype.close = function () { this.open = false; };

afterEach(() => jest.restoreAllMocks());

test('labels task form fields and submits the selected priority and assignee', async () => {
  const user = userEvent.setup();
  const save = jest.spyOn(api, 'createTask').mockImplementation(async (_projectId, input) => ({
    ...input, id: 6, assignee: { id: 2, name: 'Alex Morgan' }, overdue: false,
  }));
  const onSaved = jest.fn();
  render(<QueryClientProvider client={new QueryClient()}>
    <TaskForm projectId={4} users={[{ id: 2, name: 'Alex Morgan' }]} onClose={jest.fn()} onSaved={onSaved} />
  </QueryClientProvider>);

  await user.type(screen.getByLabelText('Task title'), 'Review release notes');
  await user.selectOptions(screen.getByRole('combobox', { name: 'Priority' }), 'HIGH');
  await user.selectOptions(screen.getByRole('combobox', { name: 'Assignee' }), '2');
  await user.click(screen.getByRole('button', { name: 'Create task' }));

  await waitFor(() => expect(onSaved).toHaveBeenCalledTimes(1));
  expect(save).toHaveBeenCalledWith(4, expect.objectContaining({
    title: 'Review release notes', priority: 'HIGH', assigneeId: 2,
  }));
});
