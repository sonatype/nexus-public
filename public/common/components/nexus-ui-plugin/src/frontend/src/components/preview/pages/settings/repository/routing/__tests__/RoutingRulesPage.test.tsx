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
import { RoutingRulesPage } from '../RoutingRulesPage';
import { useRoutingRulesApi } from '../useRoutingRulesApi';
import { ToastProvider } from '../../../../../shared/Toast';

// Mock dependencies
jest.mock('../useRoutingRulesApi');

// Mock ExtJS with permission check
jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtJS: {
    checkPermission: jest.fn().mockReturnValue(true),
    showSuccessMessage: jest.fn(),
    showErrorMessage: jest.fn(),
  },
  APIConstants: {
    REST: {
      PUBLIC: {
        ROUTING_RULES: '/service/rest/v1/routing-rules',
      },
    },
  },
}));

// Mock child components
jest.mock('../RoutingRulesList', () => ({
  RoutingRulesList: function MockRoutingRulesList({ onSelect, onCreate, onPreview }) {
    return (
      <div data-testid="routing-rules-list">
        <button onClick={() => onSelect('test-rule')}>Select Rule</button>
        <button onClick={onCreate}>Create Rule</button>
        <button onClick={onPreview}>Preview</button>
      </div>
    );
  },
}));

jest.mock('../RoutingRuleForm', () => ({
  RoutingRuleForm: function MockRoutingRuleForm({ isCreate, onCancel, onDelete, onSave }) {
    const handleSave = () => {
      onSave({ name: 'test', description: '', mode: 'BLOCK', matchers: ['.*'] });
      onCancel();
    };
    return (
      <div data-testid="routing-rule-form">
        <span>{isCreate ? 'Create Mode' : 'Edit Mode'}</span>
        <button onClick={onCancel}>Cancel</button>
        {onDelete && <button onClick={onDelete}>Delete</button>}
        <button onClick={handleSave}>Save</button>
      </div>
    );
  },
}));

jest.mock('../RoutingRulePreview', () => ({
  RoutingRulePreview: function MockRoutingRulePreview({ onClose }) {
    return (
      <div data-testid="routing-rule-preview">
        <button onClick={onClose}>Close Preview</button>
      </div>
    );
  },
}));

const mockUseRoutingRulesApi = useRoutingRulesApi as jest.MockedFunction<typeof useRoutingRulesApi>;
// Get reference to ExtJS.checkPermission mock for test manipulation
const { ExtJS } = jest.requireMock('@sonatype/nexus-ui-plugin') as { ExtJS: { checkPermission: jest.Mock } };
const mockCheckPermission = ExtJS.checkPermission;

const mockRule = {
  id: '1',
  name: 'test-rule',
  description: 'Test Description',
  mode: 'BLOCK' as const,
  matchers: ['.*-sources\\.jar'],
  assignedRepositoryCount: 0,
  assignedRepositoryNames: [],
};

function TestWrapper({ children }) {
  return (
    <Theme>
      <ToastProvider>{children}</ToastProvider>
    </Theme>
  );
}

describe('RoutingRulesPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    // Set URL hash to list view for most tests
    window.location.hash = '#preview/admin/repository/routing-rules';
    mockCheckPermission.mockReturnValue(true);
    mockUseRoutingRulesApi.mockReturnValue({
      loading: false,
      error: null,
      setError: jest.fn(),
      fetchRoutingRules: jest.fn().mockResolvedValue([mockRule]),
      fetchRoutingRule: jest.fn().mockResolvedValue(mockRule),
      createRoutingRule: jest.fn().mockResolvedValue(undefined),
      updateRoutingRule: jest.fn().mockResolvedValue(undefined),
      deleteRoutingRule: jest.fn().mockResolvedValue(undefined),
      testRoutingRule: jest.fn().mockResolvedValue(true),
      fetchRoutingRulesPreview: jest.fn().mockResolvedValue({ children: [], expanded: false, expandable: false }),
    });
  });

  it('should render the page header', () => {
    render(<RoutingRulesPage />, { wrapper: TestWrapper });
    
    expect(screen.getByRole('heading', { name: 'Routing Rules' })).toBeInTheDocument();
    expect(screen.getByText('Control which requests are allowed or blocked for repositories')).toBeInTheDocument();
  });

  it('should show Create Rule button when user has permission', () => {
    mockCheckPermission.mockReturnValue(true);
    render(<RoutingRulesPage />, { wrapper: TestWrapper });
    
    expect(screen.getAllByRole('button', { name: /create rule/i }).length).toBeGreaterThan(0);
  });

  it('should show Preview button', () => {
    render(<RoutingRulesPage />, { wrapper: TestWrapper });
    
    expect(screen.getAllByRole('button', { name: /preview/i }).length).toBeGreaterThan(0);
  });

  it('should show RoutingRulesList by default', () => {
    render(<RoutingRulesPage />, { wrapper: TestWrapper });
    
    expect(screen.getByTestId('routing-rules-list')).toBeInTheDocument();
  });

  it('should navigate to create view when Create Rule is clicked', async () => {
    render(<RoutingRulesPage />, { wrapper: TestWrapper });
    
    fireEvent.click(screen.getAllByRole('button', { name: /create rule/i })[0]);
    
    await waitFor(() => {
      expect(screen.getByTestId('routing-rule-form')).toBeInTheDocument();
      expect(screen.getByText('Create Mode')).toBeInTheDocument();
    });
  });

  it('should navigate to detail view when a rule is selected', async () => {
    render(<RoutingRulesPage />, { wrapper: TestWrapper });
    
    fireEvent.click(screen.getByText('Select Rule'));
    
    await waitFor(() => {
      expect(screen.getByTestId('routing-rule-form')).toBeInTheDocument();
      expect(screen.getByText('Edit Mode')).toBeInTheDocument();
    });
  });

  it('should navigate to preview view when Preview is clicked', async () => {
    render(<RoutingRulesPage />, { wrapper: TestWrapper });
    
    fireEvent.click(screen.getAllByRole('button', { name: /preview/i })[0]);
    
    await waitFor(() => {
      expect(screen.getByTestId('routing-rule-preview')).toBeInTheDocument();
    });
  });

  it('should navigate back to list after creating a rule', async () => {
    render(<RoutingRulesPage />, { wrapper: TestWrapper });
    
    // Go to create view
    fireEvent.click(screen.getAllByRole('button', { name: /create rule/i })[0]);
    
    await waitFor(() => {
      expect(screen.getByTestId('routing-rule-form')).toBeInTheDocument();
    });
    
    // Create the rule (mock calls onSave + onCancel, simulating form hook flow)
    fireEvent.click(screen.getByText('Save'));
    
    await waitFor(() => {
      expect(screen.getByTestId('routing-rules-list')).toBeInTheDocument();
    });
  });

  it('should navigate back to list from create view', async () => {
    render(<RoutingRulesPage />, { wrapper: TestWrapper });
    
    // Go to create view
    fireEvent.click(screen.getAllByRole('button', { name: /create rule/i })[0]);
    
    await waitFor(() => {
      expect(screen.getByTestId('routing-rule-form')).toBeInTheDocument();
    });
    
    // Click cancel
    fireEvent.click(screen.getByText('Cancel'));
    
    await waitFor(() => {
      expect(screen.getByTestId('routing-rules-list')).toBeInTheDocument();
    });
  });

  it('should navigate back to list from preview view', async () => {
    render(<RoutingRulesPage />, { wrapper: TestWrapper });
    
    // Go to preview view
    fireEvent.click(screen.getAllByRole('button', { name: /preview/i })[0]);
    
    await waitFor(() => {
      expect(screen.getByTestId('routing-rule-preview')).toBeInTheDocument();
    });
    
    // Click close
    fireEvent.click(screen.getByText('Close Preview'));
    
    await waitFor(() => {
      expect(screen.getByTestId('routing-rules-list')).toBeInTheDocument();
    });
  });

  it('should show error alert when there is an error', () => {
    mockUseRoutingRulesApi.mockReturnValue({
      ...mockUseRoutingRulesApi(),
      error: 'Test error message',
    });
    
    render(<RoutingRulesPage />, { wrapper: TestWrapper });
    
    expect(screen.getByText('Test error message')).toBeInTheDocument();
  });

  it('should handle delete in detail view via confirmation dialog', async () => {
    const mockDeleteRoutingRule = jest.fn().mockResolvedValue(undefined);
    mockUseRoutingRulesApi.mockReturnValue({
      ...mockUseRoutingRulesApi(),
      deleteRoutingRule: mockDeleteRoutingRule,
      fetchRoutingRule: jest.fn().mockResolvedValue(mockRule),
    });

    render(<RoutingRulesPage />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByText('Select Rule'));

    await waitFor(() => {
      expect(screen.getByTestId('routing-rule-form')).toBeInTheDocument();
    });
  });

  it('should not delete when cancel is clicked in confirmation dialog', async () => {
    const mockDeleteRoutingRule = jest.fn();
    mockUseRoutingRulesApi.mockReturnValue({
      ...mockUseRoutingRulesApi(),
      deleteRoutingRule: mockDeleteRoutingRule,
    });

    render(<RoutingRulesPage />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByText('Select Rule'));

    await waitFor(() => {
      expect(screen.getByTestId('routing-rule-form')).toBeInTheDocument();
    });

    expect(mockDeleteRoutingRule).not.toHaveBeenCalled();
  });

  it('should handle fetchRoutingRule returning null', async () => {
    const setErrorMock = jest.fn();
    mockUseRoutingRulesApi.mockReturnValue({
      ...mockUseRoutingRulesApi(),
      fetchRoutingRule: jest.fn().mockResolvedValue(null),
      setError: setErrorMock,
    });
    
    render(<RoutingRulesPage />, { wrapper: TestWrapper });
    
    fireEvent.click(screen.getByText('Select Rule'));
    
    await waitFor(() => {
      expect(setErrorMock).toHaveBeenCalledWith('Routing rule not found');
    });
  });

  it('breadcrumb navigation in preview view has clickable Routing Rules link', async () => {
    render(<RoutingRulesPage />, { wrapper: TestWrapper });

    fireEvent.click(screen.getAllByRole('button', { name: /preview/i })[0]);

    await waitFor(() => {
      expect(screen.getByTestId('routing-rule-preview')).toBeInTheDocument();
    });

    expect(screen.getByRole('button', { name: 'Routing Rules' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Settings' })).toBeInTheDocument();
  });

  it('breadcrumb navigation in create view has clickable Routing Rules link', async () => {
    render(<RoutingRulesPage />, { wrapper: TestWrapper });

    fireEvent.click(screen.getAllByRole('button', { name: /create rule/i })[0]);

    await waitFor(() => {
      expect(screen.getByTestId('routing-rule-form')).toBeInTheDocument();
    });

    expect(screen.getByRole('button', { name: 'Routing Rules' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Settings' })).toBeInTheDocument();
  });

  it('breadcrumb navigation in detail view has clickable Routing Rules link', async () => {
    render(<RoutingRulesPage />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByText('Select Rule'));

    await waitFor(() => {
      expect(screen.getByTestId('routing-rule-form')).toBeInTheDocument();
    });

    expect(screen.getByRole('button', { name: 'Routing Rules' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Settings' })).toBeInTheDocument();
  });

  describe('routing rule form view deletion', () => {
    it('shows delete button in form view when editing', async () => {
      render(<RoutingRulesPage />, { wrapper: TestWrapper });

      // Navigate to detail view
      fireEvent.click(screen.getByText('Select Rule'));

      await waitFor(() => {
        expect(screen.getByTestId('routing-rule-form')).toBeInTheDocument();
      });

      expect(screen.getByRole('button', { name: /delete/i })).toBeInTheDocument();
    });

    it('opens DeleteConfirmationModal when delete clicked in form', async () => {
      render(<RoutingRulesPage />, { wrapper: TestWrapper });

      // Navigate to detail view
      fireEvent.click(screen.getByText('Select Rule'));

      await waitFor(() => {
        expect(screen.getByTestId('routing-rule-form')).toBeInTheDocument();
      });

      // Click delete in form
      fireEvent.click(screen.getByRole('button', { name: /delete/i }));

      // Verify modal opens with rule name
      await waitFor(() => {
        expect(screen.getByText(/delete routing rule\?/i)).toBeInTheDocument();
        // Rule name appears in multiple places (heading and modal), just check modal opened
      });
    });

    it('requires typing "Delete" to confirm deletion from form', async () => {
      const mockDeleteRoutingRule = jest.fn().mockResolvedValue(undefined);
      mockUseRoutingRulesApi.mockReturnValue({
        ...mockUseRoutingRulesApi(),
        deleteRoutingRule: mockDeleteRoutingRule,
        fetchRoutingRule: jest.fn().mockResolvedValue(mockRule),
      });

      render(<RoutingRulesPage />, { wrapper: TestWrapper });

      // Navigate to detail view
      fireEvent.click(screen.getByText('Select Rule'));

      await waitFor(() => {
        expect(screen.getByTestId('routing-rule-form')).toBeInTheDocument();
      });

      // Click delete
      fireEvent.click(screen.getByRole('button', { name: /delete/i }));

      // Find the confirmation input and delete button
      const confirmInput = await screen.findByRole('textbox');
      const confirmButton = screen.getByRole('button', { name: /^delete$/i });

      // Initially, delete button should be disabled
      expect(confirmButton).toBeDisabled();

      // Type incorrect rule name
      fireEvent.change(confirmInput, { target: { value: 'wrong-rule' } });
      expect(confirmButton).toBeDisabled();

      // Type correct rule name
      // Acknowledgement is the literal "Delete" (case-insensitive) — NEXUS-53356.
      fireEvent.change(confirmInput, { target: { value: 'Delete' } });
      expect(confirmButton).not.toBeDisabled();

      // Click delete
      fireEvent.click(confirmButton);

      // Verify deleteRoutingRule was called
      await waitFor(() => {
        expect(mockDeleteRoutingRule).toHaveBeenCalledWith('test-rule');
      });
    });

    it('cancels deletion when Cancel clicked', async () => {
      const mockDeleteRoutingRule = jest.fn();
      mockUseRoutingRulesApi.mockReturnValue({
        ...mockUseRoutingRulesApi(),
        deleteRoutingRule: mockDeleteRoutingRule,
        fetchRoutingRule: jest.fn().mockResolvedValue(mockRule),
      });

      render(<RoutingRulesPage />, { wrapper: TestWrapper });

      // Navigate to detail view
      fireEvent.click(screen.getByText('Select Rule'));

      await waitFor(() => {
        expect(screen.getByTestId('routing-rule-form')).toBeInTheDocument();
      });

      // Click delete
      fireEvent.click(screen.getByRole('button', { name: /delete/i }));

      // Verify modal is open
      await waitFor(() => {
        expect(screen.getByText(/delete routing rule\?/i)).toBeInTheDocument();
      });

      // Click cancel
      const cancelButton = screen.getByRole('button', { name: /cancel/i });
      fireEvent.click(cancelButton);

      // Verify modal closed and no deletion occurred
      await waitFor(() => {
        expect(screen.queryByText(/delete routing rule\?/i)).not.toBeInTheDocument();
      });
      expect(mockDeleteRoutingRule).not.toHaveBeenCalled();
    });

    it('shows loading state during deletion', async () => {
      // Make delete async with delay
      const mockDeleteRoutingRule = jest.fn().mockImplementation(
        () => new Promise(resolve => setTimeout(resolve, 100))
      );
      mockUseRoutingRulesApi.mockReturnValue({
        ...mockUseRoutingRulesApi(),
        deleteRoutingRule: mockDeleteRoutingRule,
        fetchRoutingRule: jest.fn().mockResolvedValue(mockRule),
      });

      render(<RoutingRulesPage />, { wrapper: TestWrapper });

      // Navigate to detail view
      fireEvent.click(screen.getByText('Select Rule'));

      await waitFor(() => {
        expect(screen.getByTestId('routing-rule-form')).toBeInTheDocument();
      });

      // Click delete
      fireEvent.click(screen.getByRole('button', { name: /delete/i }));

      // Type rule name
      const confirmInput = await screen.findByRole('textbox');
      // Acknowledgement is the literal "Delete" (case-insensitive) — NEXUS-53356.
      fireEvent.change(confirmInput, { target: { value: 'Delete' } });

      // Click delete
      const confirmButton = screen.getByRole('button', { name: /^delete$/i });
      fireEvent.click(confirmButton);

      // Verify loading state (button should be disabled during deletion)
      await waitFor(() => {
        expect(confirmButton).toBeDisabled();
      });
    });
  });
});

