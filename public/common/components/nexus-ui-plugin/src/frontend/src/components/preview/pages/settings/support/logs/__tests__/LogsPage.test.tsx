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
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';

import { LogsPage } from '../LogsPage';

const ROUTE_LIST = 'preview.admin.support.logs.list';
const ROUTE_DETAIL = 'preview.admin.support.logs.detail';

// --- Router mocks -----------------------------------------------------------

const mockStateServiceGo = jest.fn();

jest.mock('@uirouter/react', () => ({
  ...jest.requireActual('@uirouter/react'),
  useCurrentStateAndParams: jest.fn(),
  useRouter: jest.fn(() => ({stateService: {go: mockStateServiceGo}})),
}));

const { useCurrentStateAndParams } = jest.requireMock('@uirouter/react');

function setRouteParams(params: Record<string, unknown> = {}) {
  useCurrentStateAndParams.mockReturnValue({params, state: {name: 'test'}});
}

// --- ExtJS mock -------------------------------------------------------------

jest.mock('../../../../../../../interface/ExtJS', () => ({
  ExtJS: {
    state: jest.fn().mockReturnValue({
      getValue: jest.fn().mockReturnValue(false), // not clustered
    }),
    checkPermission: jest.fn().mockReturnValue(true), // has permission
  },
}));

// --- Child component mocks --------------------------------------------------

jest.mock('../LogsList', () => ({
  LogsList: function MockLogsList({onSelect}: {onSelect: (filename: string) => void}) {
    return (
      <div data-testid="logs-list">
        <button onClick={() => onSelect('nexus.log')}>Select nexus.log</button>
        <button onClick={() => onSelect('audit.log')}>Select audit.log</button>
        <button onClick={() => onSelect('tasks/component.normalize.version-20260605161637239.log')}>
          Select task log
        </button>
        <button onClick={() => onSelect('my log file.log')}>Select log with spaces</button>
      </div>
    );
  },
}));

jest.mock('../LogViewer', () => ({
  LogViewer: function MockLogViewer({filename, onBack}: {filename: string; onBack?: () => void}) {
    return (
      <div data-testid="log-viewer" data-filename={filename}>
        <span>Viewing: {filename}</span>
        {onBack && <button onClick={onBack}>Back</button>}
      </div>
    );
  },
}));

// ---------------------------------------------------------------------------

function TestWrapper({children}: {children: React.ReactNode}) {
  return <Theme>{children}</Theme>;
}

describe('LogsPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    setRouteParams(); // default: no filename → list view
  });

  // ── List view (no filename in route) ──────────────────────────────────────

  it('renders the logs list when no filename is in the route', () => {
    render(<LogsPage />, {wrapper: TestWrapper});

    expect(screen.getByTestId('logs-list')).toBeInTheDocument();
    expect(screen.getByRole('heading', {name: 'Logs'})).toBeInTheDocument();
    expect(screen.getByText('View the current log contents')).toBeInTheDocument();
  });

  it('has test id on main container', () => {
    render(<LogsPage />, {wrapper: TestWrapper});

    expect(screen.getByTestId('logs-page')).toBeInTheDocument();
  });

  // ── Detail view (filename in route) ──────────────────────────────────────

  it('shows log viewer when filename is present in route params', () => {
    setRouteParams({filename: 'nexus.log'});
    render(<LogsPage />, {wrapper: TestWrapper});

    expect(screen.getByTestId('log-viewer')).toBeInTheDocument();
    expect(screen.getByTestId('log-viewer')).toHaveAttribute('data-filename', 'nexus.log');
    expect(screen.getByText('Viewing: nexus.log')).toBeInTheDocument();
    expect(screen.queryByTestId('logs-list')).not.toBeInTheDocument();
  });

  it('shows log viewer for audit.log from route params', () => {
    setRouteParams({filename: 'audit.log'});
    render(<LogsPage />, {wrapper: TestWrapper});

    expect(screen.getByTestId('log-viewer')).toHaveAttribute('data-filename', 'audit.log');
  });

  it('shows log viewer for task log path preserving directory separator', () => {
    const taskLog = 'tasks/component.normalize.version-20260605161637239.log';
    setRouteParams({filename: taskLog});
    render(<LogsPage />, {wrapper: TestWrapper});

    expect(screen.getByTestId('log-viewer')).toHaveAttribute('data-filename', taskLog);
    expect(screen.getByText(`Viewing: ${taskLog}`)).toBeInTheDocument();
  });

  it('shows log viewer for filenames with spaces', () => {
    setRouteParams({filename: 'my log file.log'});
    render(<LogsPage />, {wrapper: TestWrapper});

    expect(screen.getByTestId('log-viewer')).toHaveAttribute('data-filename', 'my log file.log');
  });

  it('shows Log Viewer heading in detail view', () => {
    setRouteParams({filename: 'nexus.log'});
    render(<LogsPage />, {wrapper: TestWrapper});

    expect(screen.getByRole('heading', {name: 'Log Viewer'})).toBeInTheDocument();
  });

  it('does not show help section in detail view', () => {
    setRouteParams({filename: 'nexus.log'});
    render(<LogsPage />, {wrapper: TestWrapper});

    expect(screen.queryByText('About Logs')).not.toBeInTheDocument();
  });

  // ── Navigation (route-based) ──────────────────────────────────────────────

  it('navigates to detail route with filename when a log is selected', async () => {
    render(<LogsPage />, {wrapper: TestWrapper});

    fireEvent.click(screen.getByText('Select nexus.log'));

    await waitFor(() => {
      expect(mockStateServiceGo).toHaveBeenCalledWith(ROUTE_DETAIL, {filename: 'nexus.log'});
    });
  });

  it('navigates to detail route with full task log path without encoding slashes', async () => {
    const taskLog = 'tasks/component.normalize.version-20260605161637239.log';
    render(<LogsPage />, {wrapper: TestWrapper});

    fireEvent.click(screen.getByText('Select task log'));

    await waitFor(() => {
      expect(mockStateServiceGo).toHaveBeenCalledWith(ROUTE_DETAIL, {filename: taskLog});
    });
  });

  it('navigates to list route when back is clicked in detail view', async () => {
    setRouteParams({filename: 'nexus.log'});
    render(<LogsPage />, {wrapper: TestWrapper});

    fireEvent.click(screen.getByRole('button', {name: 'Logs'}));

    await waitFor(() => {
      expect(mockStateServiceGo).toHaveBeenCalledWith(ROUTE_LIST);
    });
  });

  it('navigates to list route when LogViewer back button is clicked', async () => {
    setRouteParams({filename: 'nexus.log'});
    render(<LogsPage />, {wrapper: TestWrapper});

    fireEvent.click(screen.getByRole('button', {name: 'Back'}));

    await waitFor(() => {
      expect(mockStateServiceGo).toHaveBeenCalledWith(ROUTE_LIST);
    });
  });

  // ── Refresh / route reload ─────────────────────────────────────────────────

  it('shows same log viewer after route reload (simulated remount with same params)', () => {
    setRouteParams({filename: 'nexus.log'});
    const {unmount} = render(<LogsPage />, {wrapper: TestWrapper});
    expect(screen.getByTestId('log-viewer')).toHaveAttribute('data-filename', 'nexus.log');

    // Simulate route reload: unmount and remount with same params
    unmount();
    render(<LogsPage />, {wrapper: TestWrapper});

    expect(screen.getByTestId('log-viewer')).toHaveAttribute('data-filename', 'nexus.log');
    expect(screen.queryByTestId('logs-list')).not.toBeInTheDocument();
  });

  it('shows same task log viewer after route reload', () => {
    const taskLog = 'tasks/component.normalize.version-20260605161637239.log';
    setRouteParams({filename: taskLog});
    const {unmount} = render(<LogsPage />, {wrapper: TestWrapper});
    expect(screen.getByTestId('log-viewer')).toHaveAttribute('data-filename', taskLog);

    unmount();
    render(<LogsPage />, {wrapper: TestWrapper});

    expect(screen.getByTestId('log-viewer')).toHaveAttribute('data-filename', taskLog);
  });

  // ── Breadcrumb navigation ─────────────────────────────────────────────────

  describe('Breadcrumb navigation — list view', () => {
    it('renders Settings and Logs breadcrumbs', () => {
      render(<LogsPage />, {wrapper: TestWrapper});

      expect(screen.getByRole('button', {name: 'Settings'})).toBeInTheDocument();
    });

    it('renders Logs as current page in breadcrumbs', () => {
      const {container} = render(<LogsPage />, {wrapper: TestWrapper});

      const current = container.querySelector('[aria-current="page"]');
      expect(current).toBeInTheDocument();
      expect(current?.textContent).toBe('Logs');
    });

    it('navigates to Settings when Settings breadcrumb is clicked', () => {
      render(<LogsPage />, {wrapper: TestWrapper});

      const originalHash = window.location.hash;
      try {
        screen.getByRole('button', {name: 'Settings'}).click();
        expect(window.location.hash).toBe('#preview/admin/settings');
      } finally {
        window.location.hash = originalHash;
      }
    });
  });

  describe('Breadcrumb navigation — detail view', () => {
    it('renders filename as current breadcrumb in detail view', () => {
      setRouteParams({filename: 'nexus.log'});
      const {container} = render(<LogsPage />, {wrapper: TestWrapper});

      const current = container.querySelector('[aria-current="page"]');
      expect(current?.textContent).toBe('nexus.log');
    });

    it('renders Logs as clickable breadcrumb in detail view', () => {
      setRouteParams({filename: 'nexus.log'});
      render(<LogsPage />, {wrapper: TestWrapper});

      expect(screen.getByRole('button', {name: 'Logs'})).toBeInTheDocument();
    });

    it('navigates to list route when Logs breadcrumb is clicked', async () => {
      setRouteParams({filename: 'nexus.log'});
      render(<LogsPage />, {wrapper: TestWrapper});

      fireEvent.click(screen.getByRole('button', {name: 'Logs'}));

      await waitFor(() => {
        expect(mockStateServiceGo).toHaveBeenCalledWith(ROUTE_LIST);
      });
    });
  });

  // ── Clustered mode ────────────────────────────────────────────────────────

  describe('clustered mode', () => {
    beforeEach(() => {
      const mockExtJS = jest.requireMock('../../../../../../../interface/ExtJS').ExtJS;
      mockExtJS.state.mockReturnValue({getValue: jest.fn().mockReturnValue(true)});
      mockExtJS.checkPermission.mockReturnValue(true);
    });

    it('shows warning message in clustered mode', () => {
      render(<LogsPage />, {wrapper: TestWrapper});

      expect(screen.getByText(/Logs are not available via the UI in a clustered environment/i)).toBeInTheDocument();
    });

    it('does not show logs list in clustered mode', () => {
      render(<LogsPage />, {wrapper: TestWrapper});

      expect(screen.queryByTestId('logs-list')).not.toBeInTheDocument();
    });

    it('still shows page header in clustered mode', () => {
      render(<LogsPage />, {wrapper: TestWrapper});

      expect(screen.getByRole('heading', {name: 'Logs'})).toBeInTheDocument();
    });
  });

  // ── Permission denied ─────────────────────────────────────────────────────

  describe('permission denied', () => {
    beforeEach(() => {
      const mockExtJS = jest.requireMock('../../../../../../../interface/ExtJS').ExtJS;
      mockExtJS.state.mockReturnValue({getValue: jest.fn().mockReturnValue(false)});
      mockExtJS.checkPermission.mockReturnValue(false);
    });

    it('shows permission denied message', () => {
      render(<LogsPage />, {wrapper: TestWrapper});

      expect(screen.getByText(/You do not have permission to view logs/i)).toBeInTheDocument();
    });

    it('does not show logs list when permission is denied', () => {
      render(<LogsPage />, {wrapper: TestWrapper});

      expect(screen.queryByTestId('logs-list')).not.toBeInTheDocument();
    });
  });
});
