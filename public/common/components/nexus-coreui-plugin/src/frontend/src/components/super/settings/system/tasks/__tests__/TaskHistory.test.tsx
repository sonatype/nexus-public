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
import { render, screen } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import { TaskHistory } from '../TaskHistory';

const renderWithTheme = (component: React.ReactElement) => {
  return render(
    <Theme>
      {component}
    </Theme>
  );
};

describe('TaskHistory', () => {
  const baseTask = {
    id: 'task-1',
    enabled: true,
    name: 'Cleanup Task',
    typeId: 'repository.cleanup',
    typeName: 'Cleanup repositories',
    status: 'WAITING' as const,
    statusDescription: 'Waiting',
    nextRun: new Date('2026-01-22T10:00:00Z'),
    lastRun: new Date('2026-01-21T10:00:00Z'),
    lastRunResult: 'OK [1m30s]',
    runnable: true,
    stoppable: false,
    alertEmail: 'admin@example.com',
    notificationCondition: 'FAILURE' as const,
    properties: { repositoryName: 'maven-central' },
    schedule: 'manual' as const,
    startDate: new Date('2026-01-21T10:00:00Z'),
    recurringDays: [],
    cronExpression: '',
    timeZoneOffset: '+00:00',
  };

  it('renders last run information and result badge', () => {
    renderWithTheme(<TaskHistory task={baseTask} />);

    expect(screen.getByText('Execution History')).toBeInTheDocument();
    expect(screen.getByText('Started')).toBeInTheDocument();
    expect(screen.getByText('Result')).toBeInTheDocument();
    expect(screen.getByText('OK [1m30s]')).toBeInTheDocument();
  });

  it('shows empty state when task has never run', () => {
    renderWithTheme(<TaskHistory task={{ ...baseTask, lastRun: null, lastRunResult: null }} />);

    expect(screen.getByText(/has not been executed yet/i)).toBeInTheDocument();
  });

  it('renders next scheduled run when present', () => {
    renderWithTheme(<TaskHistory task={baseTask} />);

    expect(screen.getByText('Next Scheduled Run')).toBeInTheDocument();
  });

  it('renders schedule description for manual tasks', () => {
    renderWithTheme(<TaskHistory task={baseTask} />);

    expect(screen.getByText('Manual (run on demand)')).toBeInTheDocument();
  });

  it('renders cron schedule for advanced tasks', () => {
    renderWithTheme(
      <TaskHistory
        task={{
          ...baseTask,
          schedule: 'advanced',
          cronExpression: '0 0 * * *',
        }}
      />
    );

    expect(screen.getByText('Cron: 0 0 * * *')).toBeInTheDocument();
  });

  it('shows success icon for OK results', () => {
    const { container } = renderWithTheme(<TaskHistory task={baseTask} />);

    expect(container.querySelector('.task-history__icon--success')).toBeInTheDocument();
  });

  it('shows error icon for failed results', () => {
    const { container } = renderWithTheme(
      <TaskHistory task={{ ...baseTask, lastRunResult: 'Error: failed' }} />
    );

    expect(container.querySelector('.task-history__icon--error')).toBeInTheDocument();
  });

  it('shows canceled icon for canceled results', () => {
    const { container } = renderWithTheme(
      <TaskHistory task={{ ...baseTask, lastRunResult: 'Canceled by user' }} />
    );

    expect(container.querySelector('.task-history__icon--canceled')).toBeInTheDocument();
  });
});


