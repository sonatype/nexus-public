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

// Mock ExtJS
jest.mock('../../../../../../../interface/ExtJS', () => ({
  ExtJS: {
    state: jest.fn().mockReturnValue({
      getValue: jest.fn().mockReturnValue(false), // Not clustered by default
    }),
    checkPermission: jest.fn().mockReturnValue(true), // Has permission by default
  },
}));

// Mock child components
jest.mock('../LogsList', () => ({
  LogsList: function MockLogsList({ onSelect }: { onSelect: (filename: string) => void }) {
    return (
      <div data-testid="logs-list">
        <button onClick={() => onSelect('nexus.log')}>Select nexus.log</button>
        <button onClick={() => onSelect('request.log')}>Select request.log</button>
      </div>
    );
  },
}));

jest.mock('../LogViewer', () => ({
  LogViewer: function MockLogViewer({ filename, onBack }: { filename: string; onBack?: () => void }) {
    return (
      <div data-testid="log-viewer">
        <span>Viewing: {decodeURIComponent(filename)}</span>
        {onBack && <button onClick={onBack}>Back</button>}
      </div>
    );
  },
}));

// Wrapper component for Radix Theme
function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

describe('LogsPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders the logs list by default', () => {
    render(<LogsPage />, { wrapper: TestWrapper });

    expect(screen.getByTestId('logs-list')).toBeInTheDocument();
    expect(screen.getByText('Logs')).toBeInTheDocument();
    expect(screen.getByText('View the current log contents')).toBeInTheDocument();
  });

  it('displays page header with icon and description', () => {
    render(<LogsPage />, { wrapper: TestWrapper });

    expect(screen.getByText('Logs')).toBeInTheDocument();
    expect(screen.getByText('View the current log contents')).toBeInTheDocument();
  });

  it('navigates to log viewer when a log is selected', async () => {
    render(<LogsPage />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByText('Select nexus.log'));

    await waitFor(() => {
      expect(screen.getByTestId('log-viewer')).toBeInTheDocument();
      expect(screen.getByText('Viewing: nexus.log')).toBeInTheDocument();
    });
  });

  it('shows different header in viewer mode', async () => {
    render(<LogsPage />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByText('Select nexus.log'));

    await waitFor(() => {
      expect(screen.getByText('Log Viewer')).toBeInTheDocument();
      expect(screen.getByText('nexus.log')).toBeInTheDocument();
    });
  });

  it('returns to list view when back button is clicked', async () => {
    render(<LogsPage />, { wrapper: TestWrapper });

    // Navigate to viewer
    fireEvent.click(screen.getByText('Select nexus.log'));

    await waitFor(() => {
      expect(screen.getByTestId('log-viewer')).toBeInTheDocument();
    });

    // Click back
    fireEvent.click(screen.getByRole('button', { name: '' })); // Back button

    await waitFor(() => {
      expect(screen.getByTestId('logs-list')).toBeInTheDocument();
    });
  });

  it('can navigate to different log files', async () => {
    render(<LogsPage />, { wrapper: TestWrapper });

    // Select first log
    fireEvent.click(screen.getByText('Select nexus.log'));
    await waitFor(() => {
      expect(screen.getByText('Viewing: nexus.log')).toBeInTheDocument();
    });

    // Go back
    fireEvent.click(screen.getByRole('button', { name: '' }));
    await waitFor(() => {
      expect(screen.getByTestId('logs-list')).toBeInTheDocument();
    });

    // Select second log
    fireEvent.click(screen.getByText('Select request.log'));
    await waitFor(() => {
      expect(screen.getByText('Viewing: request.log')).toBeInTheDocument();
    });
  });

  it('has test id on main container', () => {
    render(<LogsPage />, { wrapper: TestWrapper });

    expect(screen.getByTestId('logs-page')).toBeInTheDocument();
  });

  describe('clustered mode', () => {
    beforeEach(() => {
      // Mock clustered mode enabled
      const mockExtJS = jest.requireMock('../../../../../../../interface/ExtJS').ExtJS;
      mockExtJS.state.mockReturnValue({
        getValue: jest.fn().mockReturnValue(true),
      });
      mockExtJS.checkPermission.mockReturnValue(true);
    });

    afterEach(() => {
      // Reset to not clustered
      const mockExtJS = jest.requireMock('../../../../../../../interface/ExtJS').ExtJS;
      mockExtJS.state.mockReturnValue({
        getValue: jest.fn().mockReturnValue(false),
      });
    });

    it('shows warning message when in clustered mode', () => {
      render(<LogsPage />, { wrapper: TestWrapper });

      expect(screen.getByText(/Logs are not available via the UI in a clustered environment/i)).toBeInTheDocument();
      expect(screen.getByText(/Please access log files directly from the file system on each node/i)).toBeInTheDocument();
    });

    it('does not show logs list in clustered mode', () => {
      render(<LogsPage />, { wrapper: TestWrapper });

      expect(screen.queryByTestId('logs-list')).not.toBeInTheDocument();
    });

    it('still shows page header in clustered mode', () => {
      render(<LogsPage />, { wrapper: TestWrapper });

      expect(screen.getByText('Logs')).toBeInTheDocument();
      expect(screen.getByText('View the current log contents')).toBeInTheDocument();
    });
  });

  describe('permission denied', () => {
    beforeEach(() => {
      // Mock no read permission
      const mockExtJS = jest.requireMock('../../../../../../../interface/ExtJS').ExtJS;
      mockExtJS.state.mockReturnValue({
        getValue: jest.fn().mockReturnValue(false),
      });
      mockExtJS.checkPermission.mockReturnValue(false);
    });

    afterEach(() => {
      // Reset to having permission
      const mockExtJS = jest.requireMock('../../../../../../../interface/ExtJS').ExtJS;
      mockExtJS.checkPermission.mockReturnValue(true);
    });

    it('shows permission denied message when user lacks nexus:logging:read permission', () => {
      render(<LogsPage />, { wrapper: TestWrapper });

      expect(screen.getByText(/You do not have permission to view logs/i)).toBeInTheDocument();
    });

    it('does not show logs list when user lacks permission', () => {
      render(<LogsPage />, { wrapper: TestWrapper });

      expect(screen.queryByTestId('logs-list')).not.toBeInTheDocument();
    });

    it('still shows page header when user lacks permission', () => {
      render(<LogsPage />, { wrapper: TestWrapper });

      expect(screen.getByText('Logs')).toBeInTheDocument();
      expect(screen.getByText('View the current log contents')).toBeInTheDocument();
    });
  });
});


