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
import { render, screen, waitFor, within, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';

import { DataStorePage } from '../DataStorePage';
import * as useDataStoreApiModule from '../useDataStoreApi';
import { DataStoreConfig } from '../types';
import { ExtJS } from '@sonatype/nexus-ui-plugin';
import { ToastProvider } from '../../../../../shared/Toast';

// Mock ExtJS
jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtJS: {
    checkPermission: jest.fn(() => true),
  },
}));

// Mock the API hook
jest.mock('../useDataStoreApi', () => ({
  useDataStoreApi: jest.fn(),
}));

const mockConfig: DataStoreConfig = {
  name: 'nexus',
  source: 'local',
  type: 'jdbc',
  jdbcUrl: 'jdbc:postgresql://localhost:5432/nexus',
  username: 'nexus_user',
  schema: 'nexus',
  maximumConnectionPool: 100,
  advanced: 'socketTimeout=30000;connectTimeout=5000',
};

const defaultApiHook = {
  loading: false,
  error: null,
  setError: jest.fn(),
  fetchConfig: jest.fn().mockResolvedValue(mockConfig),
  updateConfig: jest.fn().mockResolvedValue(mockConfig),
};

function renderComponent() {
  return render(
    <Theme>
      <ToastProvider>
        <DataStorePage />
      </ToastProvider>
    </Theme>
  );
}

const getUser = () => {
  return typeof (userEvent as any).setup === 'function' ? (userEvent as any).setup() : userEvent;
};

const mockCheckPermission = ExtJS.checkPermission as jest.Mock;

describe('DataStorePage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (useDataStoreApiModule.useDataStoreApi as jest.Mock).mockReturnValue(defaultApiHook);
    mockCheckPermission.mockReturnValue(true);
  });

  describe('Loading and Error States', () => {
    it('renders loading state initially', () => {
      (useDataStoreApiModule.useDataStoreApi as jest.Mock).mockReturnValue({
        ...defaultApiHook,
        fetchConfig: jest.fn(() => new Promise(() => {})), // Never resolves
      });

      renderComponent();

      expect(screen.getByText('Loading configuration...')).toBeInTheDocument();
    });

    it('displays error message when fetch fails', async () => {
      const mockSetError = jest.fn();
      (useDataStoreApiModule.useDataStoreApi as jest.Mock).mockReturnValue({
        ...defaultApiHook,
        error: 'Failed to load configuration',
        setError: mockSetError,
        fetchConfig: jest.fn().mockRejectedValue(new Error('Network error')),
      });

      renderComponent();

      await waitFor(() => {
        expect(screen.getByText('Failed to load configuration')).toBeInTheDocument();
      });
    });
  });

  describe('Page Header', () => {
    it('renders page header with correct title', async () => {
      renderComponent();

      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /data store/i })).toBeInTheDocument();
      });
    });

    it('displays page description', async () => {
      renderComponent();

      await waitFor(() => {
        expect(screen.getByText(/configure the connection used for the database/i)).toBeInTheDocument();
      });
    });
  });

  describe('Connection Overview Section', () => {
    it('displays connection information as read-only', async () => {
      renderComponent();

      await waitFor(() => {
        expect(screen.getByText('Connection Overview')).toBeInTheDocument();
      });

      expect(screen.getByText('Database Type')).toBeInTheDocument();
      expect(screen.getByText('PostgreSQL')).toBeInTheDocument();
      expect(screen.getByText('JDBC URL')).toBeInTheDocument();
      expect(screen.getByText(mockConfig.jdbcUrl)).toBeInTheDocument();
      expect(screen.getByText('Username')).toBeInTheDocument();
      expect(screen.getByText(mockConfig.username)).toBeInTheDocument();
      expect(screen.getByText('Schema')).toBeInTheDocument();
      expect(screen.getByText(mockConfig.schema)).toBeInTheDocument();
    });

    it('shows "Not configured" for missing connection values', async () => {
      const emptyConfig = { ...mockConfig, username: '', schema: '' };
      (useDataStoreApiModule.useDataStoreApi as jest.Mock).mockReturnValue({
        ...defaultApiHook,
        fetchConfig: jest.fn().mockResolvedValue(emptyConfig),
      });

      renderComponent();

      await waitFor(() => {
        expect(screen.getAllByText('Not configured').length).toBeGreaterThan(0);
      });
    });

    it('detects different database types from JDBC URL', async () => {
      const h2Config = { ...mockConfig, jdbcUrl: 'jdbc:h2:file:./nexus' };
      (useDataStoreApiModule.useDataStoreApi as jest.Mock).mockReturnValue({
        ...defaultApiHook,
        fetchConfig: jest.fn().mockResolvedValue(h2Config),
      });

      renderComponent();

      await waitFor(() => {
        expect(screen.getByText('H2')).toBeInTheDocument();
      });
    });
  });

  describe('Connection Pool Settings Section', () => {
    it('displays editable connection pool settings', async () => {
      renderComponent();

      await waitFor(() => {
        expect(screen.getByText('Connection Pool Settings')).toBeInTheDocument();
      });

      const poolInput = screen.getByLabelText(/maximum connection pool size/i);
      expect(poolInput).toHaveValue(100);
    });

    it('validates maximum pool size is within range', async () => {
      const user = getUser();
      renderComponent();

      await waitFor(() => {
        expect(screen.getByLabelText(/maximum connection pool size/i)).toBeInTheDocument();
      });

      const poolInput = screen.getByLabelText(/maximum connection pool size/i);
      await user.clear(poolInput);
      await user.type(poolInput, '5000');

      await waitFor(() => {
        expect(screen.getByText(/must be at most 3000/i)).toBeInTheDocument();
      });
    });

    it('validates minimum pool size', async () => {
      const user = getUser();
      renderComponent();

      await waitFor(() => {
        expect(screen.getByLabelText(/maximum connection pool size/i)).toBeInTheDocument();
      });

      const poolInput = screen.getByLabelText(/maximum connection pool size/i);
      await user.clear(poolInput);
      await user.type(poolInput, '0');

      await waitFor(() => {
        expect(screen.getByText(/must be at least 1/i)).toBeInTheDocument();
      });
    });
  });

  describe('Advanced JDBC Parameters Section', () => {
    it('displays advanced JDBC parameters section', async () => {
      renderComponent();

      await waitFor(() => {
        expect(screen.getByText('Advanced JDBC Parameters')).toBeInTheDocument();
      });
    });

    it('parses and displays existing parameters from advanced string', async () => {
      renderComponent();

      await waitFor(() => {
        expect(screen.getByDisplayValue('socketTimeout')).toBeInTheDocument();
      });

      expect(screen.getByDisplayValue('30000')).toBeInTheDocument();
      expect(screen.getByDisplayValue('connectTimeout')).toBeInTheDocument();
      expect(screen.getByDisplayValue('5000')).toBeInTheDocument();
    });

    it('shows add parameter button', async () => {
      renderComponent();

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /add parameter/i })).toBeInTheDocument();
      });
    });

    it('adds new empty parameter row when Add Parameter is clicked', async () => {
      const user = getUser();
      renderComponent();

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /add parameter/i })).toBeInTheDocument();
      });

      const initialInputs = screen.getAllByPlaceholderText(/type or select parameter/i);
      expect(initialInputs).toHaveLength(2); // socketTimeout and connectTimeout

      await user.click(screen.getByRole('button', { name: /add parameter/i }));

      const updatedInputs = screen.getAllByPlaceholderText(/type or select parameter/i);
      expect(updatedInputs).toHaveLength(3);
    });

    it('removes parameter row when remove button is clicked', async () => {
      const user = getUser();
      renderComponent();

      await waitFor(() => {
        expect(screen.getByDisplayValue('socketTimeout')).toBeInTheDocument();
      });

      const removeButtons = screen.getAllByRole('button', { name: /remove parameter/i });
      expect(removeButtons.length).toBeGreaterThan(0);

      await user.click(removeButtons[0]);

      await waitFor(() => {
        expect(screen.queryByDisplayValue('socketTimeout')).not.toBeInTheDocument();
      });
    });

    it('shows warning for unknown parameters (does not block save)', async () => {
      const configWithUnknown = { ...mockConfig, advanced: 'unknownParam=value' };
      (useDataStoreApiModule.useDataStoreApi as jest.Mock).mockReturnValue({
        ...defaultApiHook,
        fetchConfig: jest.fn().mockResolvedValue(configWithUnknown),
      });

      renderComponent();

      await waitFor(() => {
        expect(screen.getByDisplayValue('unknownParam')).toBeInTheDocument();
      });

      const warnings = screen.getAllByText(/unknown parameter.*verify this is correct/i);
      expect(warnings.length).toBeGreaterThan(0);
    });

    it('shows description for known parameters', async () => {
      renderComponent();

      await waitFor(() => {
        // Description format: "Time to wait for socket read operations (ms). Default: 0"
        expect(screen.getByText(/time to wait for socket read/i)).toBeInTheDocument();
      });
    });
  });

  describe('Effective Configuration Preview Section', () => {
    it('displays effective configuration preview when parameters exist', async () => {
      renderComponent();

      await waitFor(() => {
        expect(screen.getByText('Effective JDBC Configuration')).toBeInTheDocument();
      });
    });

    it('shows custom badge for custom parameters', async () => {
      renderComponent();

      await waitFor(() => {
        const customBadges = screen.getAllByText('Custom');
        expect(customBadges.length).toBeGreaterThan(0);
      });
    });
  });

  describe('Save and Discard Actions', () => {
    it('enables save button when form is dirty', async () => {
      const user = getUser();
      renderComponent();

      await waitFor(() => {
        expect(screen.getByLabelText(/maximum connection pool size/i)).toBeInTheDocument();
      });

      const saveButton = screen.getByRole('button', { name: /save/i });
      expect(saveButton).toBeDisabled();

      const poolInput = screen.getByLabelText(/maximum connection pool size/i);
      await user.clear(poolInput);
      await user.type(poolInput, '200');

      expect(saveButton).toBeEnabled();
    });

    it('calls updateConfig when save is clicked', async () => {
      const user = getUser();
      const mockUpdateConfig = jest.fn().mockResolvedValue({ ...mockConfig, maximumConnectionPool: 200 });
      (useDataStoreApiModule.useDataStoreApi as jest.Mock).mockReturnValue({
        ...defaultApiHook,
        updateConfig: mockUpdateConfig,
      });

      renderComponent();

      await waitFor(() => {
        expect(screen.getByLabelText(/maximum connection pool size/i)).toBeInTheDocument();
      });

      const poolInput = screen.getByLabelText(/maximum connection pool size/i);
      await user.clear(poolInput);
      await user.type(poolInput, '200');

      const saveButton = screen.getByRole('button', { name: /save/i });
      await act(async () => {
        await user.click(saveButton);
      });

      await waitFor(() => {
        // Send complete config object with updated fields
        expect(mockUpdateConfig).toHaveBeenCalledWith(expect.objectContaining({
          maximumConnectionPool: 200,
          advanced: 'socketTimeout=30000;connectTimeout=5000',
        }));
      });
    });

    it('shows success message after saving', async () => {
      const user = getUser();
      const mockUpdateConfig = jest.fn().mockResolvedValue({ ...mockConfig, maximumConnectionPool: 200 });
      (useDataStoreApiModule.useDataStoreApi as jest.Mock).mockReturnValue({
        ...defaultApiHook,
        updateConfig: mockUpdateConfig,
      });

      renderComponent();

      await waitFor(() => {
        expect(screen.getByLabelText(/maximum connection pool size/i)).toBeInTheDocument();
      });

      const poolInput = screen.getByLabelText(/maximum connection pool size/i);
      await user.clear(poolInput);
      await user.type(poolInput, '200');

      const saveButton = screen.getByRole('button', { name: /save/i });
      await act(async () => {
        await user.click(saveButton);
      });

      await waitFor(() => {
        expect(screen.getByText(/saved successfully/i)).toBeInTheDocument();
      });
    });

    it('discards changes when discard button is clicked', async () => {
      const user = getUser();
      renderComponent();

      await waitFor(() => {
        expect(screen.getByLabelText(/maximum connection pool size/i)).toBeInTheDocument();
      });

      const poolInput = screen.getByLabelText(/maximum connection pool size/i);
      await user.clear(poolInput);
      await user.type(poolInput, '200');

      expect(poolInput).toHaveValue(200);

      const discardButton = screen.getByRole('button', { name: /discard/i });
      await user.click(discardButton);

      // SettingsForm has confirmDiscard=true by default, so click "Leave" in confirmation dialog
      const leaveButton = await screen.findByRole('button', { name: /leave/i });
      await user.click(leaveButton);

      expect(poolInput).toHaveValue(100);
    });

    it('disables save when there are validation errors', async () => {
      const user = getUser();
      renderComponent();

      await waitFor(() => {
        expect(screen.getByLabelText(/maximum connection pool size/i)).toBeInTheDocument();
      });

      // Make form dirty with invalid value
      const poolInput = screen.getByLabelText(/maximum connection pool size/i);
      await user.clear(poolInput);
      await user.type(poolInput, '5000'); // Over max

      const saveButton = screen.getByRole('button', { name: /save/i });
      expect(saveButton).toBeDisabled();
    });

    it('hides save/discard buttons when user lacks permission', async () => {
      mockCheckPermission.mockReturnValue(false);

      const { container } = renderComponent();

      await waitFor(() => {
        expect(screen.getByText('Connection Pool Settings')).toBeInTheDocument();
      });

      expect(container.querySelector('.datastore-page__actions')).toBeNull();
    });
  });

  describe('Reset Parameters', () => {
    it('shows reset button when custom parameters exist', async () => {
      renderComponent();

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /reset to defaults/i })).toBeInTheDocument();
      });
    });

    it('shows confirmation when reset is clicked', async () => {
      const user = getUser();
      renderComponent();

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /reset to defaults/i })).toBeInTheDocument();
      });

      await user.click(screen.getByRole('button', { name: /reset to defaults/i }));

      await waitFor(() => {
        expect(screen.getByText(/reset all advanced jdbc parameters/i)).toBeInTheDocument();
      });
    });
  });

  describe('Input States', () => {
    it('disables inputs when loading', async () => {
      (useDataStoreApiModule.useDataStoreApi as jest.Mock).mockReturnValue({
        ...defaultApiHook,
        loading: true,
      });

      renderComponent();

      await waitFor(() => {
        expect(screen.getByLabelText(/maximum connection pool size/i)).toBeInTheDocument();
      });

      const poolInput = screen.getByLabelText(/maximum connection pool size/i);
      expect(poolInput).toBeDisabled();
    });
  });
});
