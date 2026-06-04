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
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import { RoleForm } from '../RoleForm';
import { useRolesApi } from '../useRolesApi';
import { useRolesForm } from '../useRolesForm';
import { Role, NEXUS_SOURCE } from '../types';

// Mock hooks
jest.mock('../useRolesApi');
jest.mock('../useRolesForm');

const mockUseRolesForm = useRolesForm as jest.MockedFunction<typeof useRolesForm>;

function createMockForm(data: any = {}, validationErrors: Record<string, string> = {}) {
  return {
    field: jest.fn((name: string) => {
      const keys = name.split('.');
      let value = data;
      for (const key of keys) {
        value = value?.[key];
      }
      return { name, value: value != null ? String(value) : '', onChange: jest.fn(), onBlur: jest.fn(), error: undefined };
    }),
    data,
    isPristine: true,
    isSaving: false,
    isLoading: false,
    isDeleting: false,
    saveError: null,
    validationErrors,
    state: {
      matches: jest.fn(() => false),
      context: { data, privileges: mockPrivilegeRefs, roles: mockRoleRefs, allSources: [] },
    },
    send: jest.fn(),
    submit: jest.fn(),
    checkbox: jest.fn(() => ({ name: '', checked: false, onChange: jest.fn() })),
    select: jest.fn(() => ({ name: '', value: '', onChange: jest.fn() })),
  } as any;
}

const mockUseRolesApi = useRolesApi as jest.MockedFunction<typeof useRolesApi>;

const mockPrivilegeRefs = [
  { id: 'nx-all', name: 'nx-all' },
  { id: 'nx-search-read', name: 'nx-search-read' },
  { id: 'nx-repository-view-*', name: 'nx-repository-view-*' },
];

const mockRoleRefs = [
  { id: 'nx-admin', name: 'nx-admin' },
  { id: 'nx-anonymous', name: 'nx-anonymous' },
];

const mockRole: Role = {
  id: 'test-role',
  version: '1',
  source: NEXUS_SOURCE,
  name: 'Test Role',
  description: 'Test Description',
  readOnly: false,
  privileges: ['nx-search-read'],
  roles: ['nx-anonymous'],
};

const renderWithTheme = (component: React.ReactNode) => {
  return render(<Theme>{component}</Theme>);
};

describe('RoleForm', () => {
  const mockOnCancel = jest.fn();
  const mockOnValidationChange = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockUseRolesForm.mockImplementation(({ role }: any) => {
      const formData = role ? {
        id: role.id, name: role.name, description: role.description || '',
        privileges: role.privileges || [], roles: role.roles || [], source: role.source || NEXUS_SOURCE,
      } : { id: '', name: '', description: '', privileges: [], roles: [], source: NEXUS_SOURCE };
      return { form: createMockForm(formData), role: role || null, isCreate: !role } as any;
    });
    mockUseRolesApi.mockReturnValue({
      loading: false,
      error: null,
      setError: jest.fn(),
      fetchRoles: jest.fn().mockResolvedValue([]),
      fetchRoleReferences: jest.fn().mockResolvedValue(mockRoleRefs),
      fetchRoleSources: jest.fn().mockResolvedValue([]),
      fetchRolesFromSource: jest.fn().mockResolvedValue([]),
      fetchPrivilegeReferences: jest.fn().mockResolvedValue(mockPrivilegeRefs),
      findRole: jest.fn().mockResolvedValue(null),
      createRole: jest.fn(),
      updateRole: jest.fn(),
      deleteRole: jest.fn(),
    });
  });

  describe('Wizard Steps', () => {
    it('should render Step 0: Role Type', async () => {
      renderWithTheme(
        <RoleForm isCreate={true} onCancel={mockOnCancel} wizardStep={0} />
      );

      await waitFor(() => {
        expect(screen.getByText('Role Type')).toBeInTheDocument();
        expect(screen.queryByText('Role Setup')).not.toBeInTheDocument();
      });
    });

    it('should render Step 1: Setup', async () => {
      renderWithTheme(
        <RoleForm isCreate={true} onCancel={mockOnCancel} wizardStep={1} />
      );

      await waitFor(() => {
        expect(screen.getByText('Role Setup')).toBeInTheDocument();
        expect(screen.getByLabelText(/ID/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/Name/i)).toBeInTheDocument();
        expect(screen.queryByText('Privileges')).not.toBeInTheDocument();
      });
    });

    it('should render Step 2: Privileges', async () => {
      renderWithTheme(
        <RoleForm isCreate={true} onCancel={mockOnCancel} wizardStep={2} />
      );

      await waitFor(() => {
        expect(screen.getByText('Privileges')).toBeInTheDocument();
        expect(screen.getByTestId('combobox-privileges')).toBeInTheDocument();
        expect(screen.queryByText('Role Setup')).not.toBeInTheDocument();
      });
    });

    it('should render Step 3: Contained Roles', async () => {
      renderWithTheme(
        <RoleForm isCreate={true} onCancel={mockOnCancel} wizardStep={3} />
      );

      await waitFor(() => {
        expect(screen.getByText('Contained Roles')).toBeInTheDocument();
        expect(screen.queryByText('Privileges')).not.toBeInTheDocument();
      });
    });
  });

  describe('Validation', () => {
    it('should call onValidationChange with false if Step 1 is invalid', async () => {
      const mockForm = createMockForm({ id: '', name: '' });
      mockUseRolesForm.mockReturnValue({ form: mockForm, role: null, isCreate: true } as any);

      renderWithTheme(
        <RoleForm isCreate={true} onCancel={mockOnCancel} wizardStep={1} onValidationChange={mockOnValidationChange} />
      );

      await waitFor(() => {
        expect(mockOnValidationChange).toHaveBeenCalledWith(false);
      });
    });

    it('should call onValidationChange with true if Step 1 is valid', async () => {
      const mockForm = createMockForm({ id: 'test', name: 'Test Role' });
      mockUseRolesForm.mockReturnValue({ form: mockForm, role: null, isCreate: true } as any);

      renderWithTheme(
        <RoleForm isCreate={true} onCancel={mockOnCancel} wizardStep={1} onValidationChange={mockOnValidationChange} />
      );

      await waitFor(() => {
        expect(mockOnValidationChange).toHaveBeenCalledWith(true);
      });
    });

    it('should show warning when no privileges or roles are selected on step 3 after attempt', async () => {
      const mockForm = createMockForm({
        id: 'test', name: 'Test Role', description: '',
        privileges: [], roles: [], source: NEXUS_SOURCE,
      });
      mockForm.isPristine = false;
      mockUseRolesForm.mockReturnValue({ form: mockForm, role: null, isCreate: true } as any);

      // Use onSubmitRef to simulate WizardForm calling submit (button is disabled)
      const submitRef = React.createRef<(() => void) | null>();
      renderWithTheme(
        <RoleForm
          isCreate={true}
          onCancel={mockOnCancel}
          wizardStep={3}
          onSubmitRef={submitRef}
          hideActions
        />
      );

      // Wait for submitRef to be set, then simulate WizardForm calling it
      await waitFor(() => {
        expect(submitRef.current).toBeDefined();
      });
      act(() => {
        submitRef.current!();
      });

      await waitFor(() => {
        expect(screen.getByTestId('privilege-warning')).toBeInTheDocument();
        expect(screen.getByText(/Select at least one privilege or contained role/i)).toBeInTheDocument();
      });
    });

    it('should validate role ID format', async () => {
      const mockForm = createMockForm({ id: 'invalid role!', name: 'Test', description: '', privileges: [], roles: [], source: NEXUS_SOURCE });
      mockForm.validationErrors = { id: 'Role ID can only contain letters, numbers, underscores, and hyphens' };
      mockForm.isPristine = false;
      mockUseRolesForm.mockReturnValue({ form: mockForm, role: null, isCreate: true } as any);

      renderWithTheme(
        <RoleForm isCreate={true} onCancel={mockOnCancel} wizardStep={1} />
      );

      expect(mockForm.field).toHaveBeenCalledWith('id');
    });

    it('should require at least one privilege or role on step 3', async () => {
      const mockForm = createMockForm({ id: 'test-role', name: 'Test Role', description: '', privileges: [], roles: [], source: NEXUS_SOURCE });
      mockForm.validationErrors = { privileges: 'At least one privilege or contained role must be assigned' };
      mockForm.isPristine = false;
      mockUseRolesForm.mockReturnValue({ form: mockForm, role: null, isCreate: true } as any);

      // Render on step 3 with no privileges - button should be disabled
      renderWithTheme(
        <RoleForm isCreate={true} onCancel={mockOnCancel} wizardStep={3} />
      );

      // Verify the Create button is disabled when no privileges selected (P0 requirement)
      const createBtn = screen.getByRole('button', { name: /create/i });
      expect(createBtn).toBeDisabled();
    });

    it('should call onCancel when Cancel is clicked', async () => {
      renderWithTheme(
        <RoleForm isCreate={true} onCancel={mockOnCancel} wizardStep={1} />
      );

      await waitFor(() => {
        expect(screen.getByText('Cancel')).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText('Cancel'));
      expect(mockOnCancel).toHaveBeenCalled();
    });
  });

  describe('Edit Mode', () => {
    it('should disable ID field in edit mode (Step 1)', async () => {
      renderWithTheme(
        <RoleForm
          role={mockRole}
          isCreate={false}
          onCancel={mockOnCancel}
          wizardStep={1}
        />
      );

      await waitFor(() => {
        const idInput = screen.getByDisplayValue('test-role');
        expect(idInput).toBeDisabled();
      });
    });
  });

  describe('Create with no privilege - P0 validation', () => {
    it('shows validation error when Save clicked with no privileges on step 3', async () => {
      const formData = { id: 'test-role', name: 'Test Role', privileges: [], roles: [], source: NEXUS_SOURCE };
      const validationErrors = { privileges: 'Select at least one privilege or contained role' };
      const mockForm = createMockForm(formData, validationErrors);
      mockUseRolesForm.mockReturnValue({
        form: mockForm,
        role: null,
        isCreate: true,
      } as any);

      const submitRef = React.createRef<(() => void) | null>();
      renderWithTheme(
        <RoleForm
          isCreate={true}
          onCancel={mockOnCancel}
          wizardStep={3}
          onSubmitRef={submitRef}
          hideActions
        />
      );

      expect(screen.queryByTestId('privilege-warning')).not.toBeInTheDocument();

      // Wait for useEffect to set submitRef, then simulate parent (WizardForm) calling it
      await waitFor(() => {
        expect(submitRef.current).toBeDefined();
      });
      act(() => {
        submitRef.current!();
      });

      await waitFor(() => {
        expect(screen.getByTestId('privilege-warning')).toBeInTheDocument();
        expect(screen.getByText('Select at least one privilege or contained role')).toBeInTheDocument();
      });
      // P0: Submit is guarded when no privileges - does not call form.submit() to avoid hang
      expect(mockForm.submit).not.toHaveBeenCalled();
    });

    it('does not show privilege warning when privileges are selected', async () => {
      const formData = {
        id: 'test-role',
        name: 'Test Role',
        privileges: ['nx-search-read'],
        roles: [],
        source: NEXUS_SOURCE,
      };
      const mockForm = createMockForm(formData, {});
      mockUseRolesForm.mockReturnValue({
        form: mockForm,
        role: null,
        isCreate: true,
      } as any);

      renderWithTheme(
        <RoleForm isCreate={true} onCancel={mockOnCancel} wizardStep={3} hideActions />
      );

      expect(screen.queryByTestId('privilege-warning')).not.toBeInTheDocument();
      expect(screen.queryByText(/select at least one privilege/i)).not.toBeInTheDocument();
    });
  });
});
