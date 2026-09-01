/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import { Theme } from '@radix-ui/themes';

import { RecoveryModeTasksTable } from '../RecoveryModeTasksTable';
import { ReconcileTask } from '../types';

// Mock UI-Router so we can assert navigation via stateService.go(...).
// Prefixed with `mock` so jest allows referencing it inside the factory.
const mockGo = jest.fn();
jest.mock('@uirouter/react', () => ({
  useRouter: () => ({ stateService: { go: mockGo } }),
}));

const tasks: ReconcileTask[] = [
  {
    id: '1',
    name: 'Repair - Data Repair Plan',
    type: 'blobstore.planReconciliation',
    currentState: 'RUNNING',
    lastRun: '2026-06-15T10:00:00.000Z',
    lastRunResult: null,
  },
  {
    id: '2',
    name: 'Repair - Execute Data Repair Plan',
    type: 'blobstore.executeReconciliationPlan',
    currentState: 'WAITING',
    lastRun: null,
    lastRunResult: 'OK',
  },
];

const renderInTheme = (ui: React.ReactElement) => render(<Theme>{ui}</Theme>);

beforeEach(() => {
  mockGo.mockClear();
});

describe('RecoveryModeTasksTable', () => {
  it('renders all rows with mapped type labels and placeholders', () => {
    renderInTheme(<RecoveryModeTasksTable tasks={tasks} />);
    expect(screen.getByText('Repair - Data Repair Plan')).toBeInTheDocument();
    // Raw type id is mapped to a friendly label
    expect(screen.getByText('PlanReconciliationTask')).toBeInTheDocument();
    expect(screen.getByText('ExecuteReconcilePlanTask')).toBeInTheDocument();
    // status badges
    expect(screen.getByText('RUNNING')).toBeInTheDocument();
    expect(screen.getByText('WAITING')).toBeInTheDocument();
    // last result placeholder for null
    expect(screen.getByText('OK')).toBeInTheDocument();
  });

  it('renders headers and an empty state when there are no tasks', () => {
    renderInTheme(<RecoveryModeTasksTable tasks={[]} />);
    expect(screen.getByText('Name')).toBeInTheDocument();
    expect(screen.getByText('Last result')).toBeInTheDocument();
    expect(screen.queryAllByTestId('recovery-task-row')).toHaveLength(0);
    // Empty state placeholder is shown (distinct copy from the search-no-results state)
    expect(screen.getByTestId('empty-state')).toBeInTheDocument();
    expect(screen.getByText('No data repair tasks')).toBeInTheDocument();
    expect(
      screen.getByText(/Data repair tasks appear here when recovery mode creates reconciliation work/)
    ).toBeInTheDocument();
  });

  it('filters rows by search query', () => {
    renderInTheme(<RecoveryModeTasksTable tasks={tasks} />);
    fireEvent.change(screen.getByTestId('recovery-tasks-search'), { target: { value: 'Execute' } });
    expect(screen.queryByText('Repair - Data Repair Plan')).not.toBeInTheDocument();
    expect(screen.getByText('Repair - Execute Data Repair Plan')).toBeInTheDocument();
  });

  it('shows the No tasks found state for a non-matching search', () => {
    renderInTheme(<RecoveryModeTasksTable tasks={tasks} />);
    fireEvent.change(screen.getByTestId('recovery-tasks-search'), { target: { value: 'zzzznomatch' } });
    expect(screen.getByTestId('recovery-tasks-no-results')).toBeInTheDocument();
    expect(screen.getByText('No tasks found')).toBeInTheDocument();
    expect(screen.getByText(/No tasks match "zzzznomatch"/)).toBeInTheDocument();
  });

  it('Clear Search resets the query and restores rows', () => {
    renderInTheme(<RecoveryModeTasksTable tasks={tasks} />);
    fireEvent.change(screen.getByTestId('recovery-tasks-search'), { target: { value: 'zzzznomatch' } });
    fireEvent.click(screen.getByRole('button', { name: /Clear Search/i }));
    expect(screen.getByText('Repair - Data Repair Plan')).toBeInTheDocument();
    expect(screen.queryByTestId('recovery-tasks-no-results')).not.toBeInTheDocument();
  });

  it('navigates to the task detail page when a row is clicked', () => {
    renderInTheme(<RecoveryModeTasksTable tasks={tasks} />);
    fireEvent.click(screen.getByText('Repair - Data Repair Plan'));
    expect(mockGo).toHaveBeenCalledWith('preview.admin.system.tasks.detail', { taskId: '1' });
  });

  it('navigates on Enter keypress for accessibility', () => {
    renderInTheme(<RecoveryModeTasksTable tasks={tasks} />);
    const row = screen.getByRole('button', { name: /Open task Repair - Execute Data Repair Plan/i });
    fireEvent.keyDown(row, { key: 'Enter' });
    expect(mockGo).toHaveBeenCalledWith('preview.admin.system.tasks.detail', { taskId: '2' });
  });

  const rowNames = () =>
    screen.getAllByTestId('recovery-task-row').map((r) => r.querySelector('td')?.textContent);

  it('sorts by Name ascending by default', () => {
    renderInTheme(<RecoveryModeTasksTable tasks={tasks} />);
    expect(rowNames()).toEqual([
      'Repair - Data Repair Plan',
      'Repair - Execute Data Repair Plan',
    ]);
  });

  it('toggles to descending when the active header is clicked', () => {
    renderInTheme(<RecoveryModeTasksTable tasks={tasks} />);
    // "Name" header is the active sort; clicking toggles asc -> desc
    fireEvent.click(screen.getByText('Name'));
    expect(rowNames()).toEqual([
      'Repair - Execute Data Repair Plan',
      'Repair - Data Repair Plan',
    ]);
  });

  it('sorts by another column when its header is clicked', () => {
    renderInTheme(<RecoveryModeTasksTable tasks={tasks} />);
    // Status: RUNNING vs WAITING -> ascending puts RUNNING first
    fireEvent.click(screen.getByText('Status'));
    expect(rowNames()[0]).toBe('Repair - Data Repair Plan'); // RUNNING < WAITING
  });

  it('sorts by Type, Last run, and Last result headers', () => {
    renderInTheme(<RecoveryModeTasksTable tasks={tasks} />);
    // Type: ExecuteReconcilePlanTask < PlanReconciliationTask
    fireEvent.click(screen.getByText('Type'));
    expect(rowNames()[0]).toBe('Repair - Execute Data Repair Plan');
    // Last run: null date maps to -Infinity, so ascending puts task 2 first.
    fireEvent.click(screen.getByText('Last run'));
    expect(rowNames()[0]).toBe('Repair - Execute Data Repair Plan');
    // Last result: '' (task 1, null) < 'ok' (task 2) -> task 1 first ascending
    fireEvent.click(screen.getByText('Last result'));
    expect(rowNames()[0]).toBe('Repair - Data Repair Plan');
  });

  it('renders placeholders for missing last run / last result', () => {
    const partial: ReconcileTask[] = [
      { id: '9', name: 'Partial', type: 'blobstore.planReconciliation', currentState: null, lastRun: null, lastRunResult: null },
    ];
    renderInTheme(<RecoveryModeTasksTable tasks={partial} />);
    const cells = screen.getByTestId('recovery-task-row').querySelectorAll('td');
    // status, last run, last result all show the em-dash placeholder
    expect(cells[2].textContent).toBe('—');
    expect(cells[3].textContent).toBe('—');
    expect(cells[4].textContent).toBe('—');
  });

  it('renders a placeholder for an invalid last run date', () => {
    const bad: ReconcileTask[] = [
      { id: '10', name: 'Bad date', type: 'custom.type', currentState: 'OK', lastRun: 'not-a-date', lastRunResult: 'OK' },
    ];
    renderInTheme(<RecoveryModeTasksTable tasks={bad} />);
    const cells = screen.getByTestId('recovery-task-row').querySelectorAll('td');
    expect(cells[3].textContent).toBe('—'); // invalid date -> placeholder
    // Unknown type id falls through to the raw type label
    expect(cells[1].textContent).toBe('custom.type');
  });

  it('maps each task state to the correct status-badge color', () => {
    // [state, expected Radix accent color] — mirrors statusColor() in the component.
    const cases: Array<[string, string]> = [
      ['RUNNING', 'blue'],
      ['RUNNING: 45%', 'blue'],
      ['OK', 'green'],
      ['COMPLETED', 'green'],
      ['FAILED', 'red'],
      ['ERROR', 'red'],
      ['WAITING', 'amber'],
      ['BLOCKED', 'amber'],
      ['SOMETHING_ELSE', 'gray'],
    ];
    const states: ReconcileTask[] = cases.map(([state], i) => ({
      id: String(i),
      name: `task-${i}`,
      type: 't',
      currentState: state,
    }));
    renderInTheme(<RecoveryModeTasksTable tasks={states} />);

    cases.forEach(([state, color]) => {
      const badge = screen.getByText(state);
      expect(badge).toHaveAttribute('data-accent-color', color);
    });
  });
});
