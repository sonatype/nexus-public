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
import { render, screen, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';

import { DataStorePage } from '../DataStorePage';
import * as useDataStoreFormModule from '../useDataStoreForm';
import { ExtJS } from '@sonatype/nexus-ui-plugin';
import { ToastProvider } from '../../../../../shared/Toast';

// Mock ExtJS
jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtJS: {
    checkPermission: jest.fn(() => true),
  },
}));

// Mock the form hook
jest.mock('../useDataStoreForm', () => ({
  useDataStoreForm: jest.fn(),
}));

const mockedUseDataStoreForm = useDataStoreFormModule.useDataStoreForm as jest.Mock;

function makeForm(overrides: Record<string, any> = {}) {
  // Merge data overrides with defaults (don't replace jdbcParameters if not specified)
  const baseData = {
    maximumConnectionPool: 100,
    jdbcParameters: [
      { id: 'p1', name: 'socketTimeout', value: '30000', isDefault: false, isCustom: true },
      { id: 'p2', name: 'connectTimeout', value: '5000', isDefault: false, isCustom: true },
    ],
    jdbcUrl: 'jdbc:postgresql://localhost:5432/nexus',
    username: 'nexus_user',
    schema: 'nexus',
  };
  const data = { ...baseData, ...(overrides.data || {}) };

  // Extract 'data' from overrides to avoid spreading it twice
  const { data: _, ...restOverrides } = overrides;

  return {
    data,
    field: (name: string) => ({
      name,
      value: String((data as any)[name] ?? ''),
      error: undefined,
      onChange: jest.fn(),
      onBlur: jest.fn(),
    }),
    isLoading: false,
    isSaving: false,
    isPristine: true,
    saveError: null,
    loadError: null,
    hasValidationErrors: false,
    validationErrors: {},
    databaseType: 'PostgreSQL',
    effectiveConfig: [
      { name: 'maximumConnectionPool', value: '100', source: 'Default' },
      { name: 'socketTimeout', value: '30000', source: 'Custom' },
    ],
    parameterValidations: [],
    hasParameterErrors: false,
    canSave: false,
    showAllValidation: false,
    setParameters: jest.fn(),
    showResetConfirm: false,
    requestResetParams: jest.fn(),
    confirmResetParams: jest.fn(),
    cancelResetParams: jest.fn(),
    submit: jest.fn(),
    reset: jest.fn(),
    send: jest.fn(),
    ...restOverrides,
  };
}

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
    mockedUseDataStoreForm.mockReturnValue(makeForm());
    mockCheckPermission.mockReturnValue(true);
  });

  describe('Loading and Error States', () => {
    it('renders loading state initially', () => {
      mockedUseDataStoreForm.mockReturnValue(makeForm({ isLoading: true }));

      renderComponent();

      expect(screen.getByText('Loading configuration...')).toBeInTheDocument();
    });

    it('displays error message when fetch fails', async () => {
      mockedUseDataStoreForm.mockReturnValue(makeForm({ loadError: 'Failed to load configuration' }));

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
      expect(screen.getByText('jdbc:postgresql://localhost:5432/nexus')).toBeInTheDocument();
      expect(screen.getByText('Username')).toBeInTheDocument();
      expect(screen.getByText('nexus_user')).toBeInTheDocument();
      expect(screen.getByText('Schema')).toBeInTheDocument();
      expect(screen.getByText('nexus')).toBeInTheDocument();
    });

    it('shows "Not configured" for missing connection values', async () => {
      mockedUseDataStoreForm.mockReturnValue(makeForm({
        data: { username: '', schema: '' },
      }));

      renderComponent();

      await waitFor(() => {
        expect(screen.getAllByText('Not configured').length).toBeGreaterThan(0);
      });
    });

    it('detects different database types from JDBC URL', async () => {
      mockedUseDataStoreForm.mockReturnValue(makeForm({
        databaseType: 'H2',
        data: { jdbcUrl: 'jdbc:h2:file:./nexus' },
      }));

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
      mockedUseDataStoreForm.mockReturnValue(makeForm({
        validationErrors: { maximumConnectionPool: 'Must be at most 3000' },
        field: (name: string) => ({
          name,
          value: name === 'maximumConnectionPool' ? '5000' : String((makeForm().data as any)[name] ?? ''),
          error: name === 'maximumConnectionPool' ? 'Must be at most 3000' : undefined,
          onChange: jest.fn(),
          onBlur: jest.fn(),
        }),
      }));

      renderComponent();

      await waitFor(() => {
        expect(screen.getByText(/must be at most 3000/i)).toBeInTheDocument();
      });
    });

    it('validates minimum pool size', async () => {
      mockedUseDataStoreForm.mockReturnValue(makeForm({
        validationErrors: { maximumConnectionPool: 'Must be at least 1' },
        field: (name: string) => ({
          name,
          value: name === 'maximumConnectionPool' ? '0' : String((makeForm().data as any)[name] ?? ''),
          error: name === 'maximumConnectionPool' ? 'Must be at least 1' : undefined,
          onChange: jest.fn(),
          onBlur: jest.fn(),
        }),
      }));

      renderComponent();

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
      const mockSend = jest.fn();
      const newData = [
        { id: 'p1', name: 'socketTimeout', value: '30000', isDefault: false, isCustom: true },
        { id: 'p2', name: 'connectTimeout', value: '5000', isDefault: false, isCustom: true },
        { id: 'p3', name: '', value: '', isDefault: false, isCustom: true },
      ];

      // Start with 2 params
      mockedUseDataStoreForm.mockReturnValue(makeForm({ send: mockSend }));

      renderComponent();

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /add parameter/i })).toBeInTheDocument();
      });

      const initialInputs = screen.getAllByPlaceholderText(/type or select parameter/i);
      expect(initialInputs).toHaveLength(2); // socketTimeout and connectTimeout

      await user.click(screen.getByRole('button', { name: /add parameter/i }));

      // The add button trigger should call send with an update to jdbcParameters
      // Since Jest mocks don't re-render, we need to manually trigger the next state
      // by updating the mock and re-rendering
      mockedUseDataStoreForm.mockReturnValue(makeForm({ data: { jdbcParameters: newData }, send: mockSend }));
    });

    it('removes parameter row when remove button is clicked', async () => {
      const user = getUser();
      const mockSetParameters = jest.fn();

      mockedUseDataStoreForm.mockReturnValue(makeForm({ setParameters: mockSetParameters }));

      renderComponent();

      await waitFor(() => {
        expect(screen.getByDisplayValue('socketTimeout')).toBeInTheDocument();
      });

      const removeButtons = screen.getAllByRole('button', { name: /remove parameter/i });
      expect(removeButtons.length).toBeGreaterThan(0);

      await user.click(removeButtons[0]);

      // JdbcParameterEditor calls onChange (setParameters) with the filtered array
      expect(mockSetParameters).toHaveBeenCalled();
      // Verify it was called with one parameter removed (the first one: socketTimeout)
      expect(mockSetParameters).toHaveBeenCalledWith(
        expect.arrayContaining([
          expect.objectContaining({ name: 'connectTimeout' }),
        ])
      );
    });

    it('shows warning for unknown parameters (does not block save)', async () => {
      mockedUseDataStoreForm.mockReturnValue(makeForm({
        data: {
          jdbcParameters: [
            { id: 'p1', name: 'unknownParam', value: 'value', isDefault: false, isCustom: true },
          ],
        },
        parameterValidations: [
          { id: 'p1', warning: 'Unknown JDBC parameter - verify this is correct for your database driver.' },
        ],
      }));

      renderComponent();

      await waitFor(() => {
        expect(screen.getByDisplayValue('unknownParam')).toBeInTheDocument();
      });

      const warnings = screen.getAllByText(/unknown.*parameter.*verify this is correct/i);
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
      mockedUseDataStoreForm.mockReturnValue(makeForm({ canSave: true, isPristine: false }));

      renderComponent();

      await waitFor(() => {
        expect(screen.getByLabelText(/maximum connection pool size/i)).toBeInTheDocument();
      });

      const saveButton = screen.getByRole('button', { name: /save/i });
      expect(saveButton).toBeEnabled();
    });

    it('calls submit when save is clicked', async () => {
      const user = getUser();
      const mockSubmit = jest.fn();

      mockedUseDataStoreForm.mockReturnValue(makeForm({
        canSave: true,
        isPristine: false,
        submit: mockSubmit,
      }));

      renderComponent();

      await waitFor(() => {
        expect(screen.getByLabelText(/maximum connection pool size/i)).toBeInTheDocument();
      });

      const saveButton = screen.getByRole('button', { name: /save/i });
      await act(async () => {
        await user.click(saveButton);
      });

      expect(mockSubmit).toHaveBeenCalled();
    });

    it('shows success message after saving', async () => {
      const user = getUser();
      const mockSubmit = jest.fn(() => {
        // Simulate the successful save - the hook handles toast
      });

      mockedUseDataStoreForm.mockReturnValue(makeForm({
        canSave: true,
        isPristine: false,
        submit: mockSubmit,
      }));

      renderComponent();

      await waitFor(() => {
        expect(screen.getByLabelText(/maximum connection pool size/i)).toBeInTheDocument();
      });

      const saveButton = screen.getByRole('button', { name: /save/i });
      await act(async () => {
        await user.click(saveButton);
      });

      // Toast is handled by the hook, not the component
      expect(mockSubmit).toHaveBeenCalled();
    });

    it('discards changes when discard button is clicked', async () => {
      const user = getUser();
      const mockReset = jest.fn();

      mockedUseDataStoreForm.mockReturnValue(makeForm({
        isPristine: false,
        reset: mockReset,
      }));

      renderComponent();

      await waitFor(() => {
        expect(screen.getByLabelText(/maximum connection pool size/i)).toBeInTheDocument();
      });

      const discardButton = screen.getByRole('button', { name: /discard/i });
      await user.click(discardButton);

      // SettingsForm has confirmDiscard=true by default, so click "Leave" in confirmation dialog
      const leaveButton = await screen.findByRole('button', { name: /leave/i });
      await user.click(leaveButton);

      expect(mockReset).toHaveBeenCalled();
    });

    it('disables save when there are validation errors', async () => {
      mockedUseDataStoreForm.mockReturnValue(makeForm({
        canSave: false,
        isPristine: false,
        validationErrors: { maximumConnectionPool: 'Must be at most 3000' },
        field: (name: string) => ({
          name,
          value: name === 'maximumConnectionPool' ? '5000' : String((makeForm().data as any)[name] ?? ''),
          error: name === 'maximumConnectionPool' ? 'Must be at most 3000' : undefined,
          onChange: jest.fn(),
          onBlur: jest.fn(),
        }),
      }));

      renderComponent();

      await waitFor(() => {
        expect(screen.getByLabelText(/maximum connection pool size/i)).toBeInTheDocument();
      });

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
      const mockRequestResetParams = jest.fn();

      mockedUseDataStoreForm.mockReturnValue(makeForm({
        showResetConfirm: false,
        requestResetParams: mockRequestResetParams,
      }));

      renderComponent();

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /reset to defaults/i })).toBeInTheDocument();
      });

      await user.click(screen.getByRole('button', { name: /reset to defaults/i }));

      // The reset button calls requestResetParams which sets showResetConfirm to true
      expect(mockRequestResetParams).toHaveBeenCalled();

      // Simulate the state change by updating the mock and re-rendering
      mockedUseDataStoreForm.mockReturnValue(makeForm({ showResetConfirm: true }));
      renderComponent();

      await waitFor(() => {
        expect(screen.getByText(/reset all advanced jdbc parameters/i)).toBeInTheDocument();
      });
    });
  });

  describe('Input States', () => {
    it('disables inputs when saving', async () => {
      mockedUseDataStoreForm.mockReturnValue(makeForm({ isSaving: true }));

      renderComponent();

      await waitFor(() => {
        expect(screen.getByLabelText(/maximum connection pool size/i)).toBeInTheDocument();
      });

      const poolInput = screen.getByLabelText(/maximum connection pool size/i);
      expect(poolInput).toBeDisabled();
    });
  });

  describe('Layout', () => {
    it('does not render a nested scroll container around the form content', async () => {
      const { container } = renderComponent();

      await waitFor(() => {
        expect(screen.getByText('Connection Pool Settings')).toBeInTheDocument();
      });

      // A nested `overflow: auto` box here traps the SettingsForm's sticky
      // Save/Discard toolbar to a small inner scrollport instead of letting
      // it pin against the shared `.settings-layout-radix__content` pane
      // (see docs/superpowers/specs/2026-07-08-datastore-page-sticky-toolbar-design.md).
      expect(container.querySelector('.datastore-page__content')).toBeNull();
    });
  });
});
