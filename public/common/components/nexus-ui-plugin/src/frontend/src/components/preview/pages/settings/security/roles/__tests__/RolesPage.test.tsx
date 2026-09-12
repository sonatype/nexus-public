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
import { RolesPage } from '../RolesPage';
import { useRolesApi } from '../useRolesApi';
import { ExtJS } from '../../../../../../../interface/ExtJS';
import { ToastProvider } from '../../../../../shared/Toast';

// Mock dependencies
jest.mock('../useRolesApi');
jest.mock('../../../../../../../interface/ExtJS', () => ({
  ExtJS: {
    checkPermission: jest.fn(),
    // RolesPage reads permissions through the provider-independent ExtJS.usePermission
    // (NEXUS-54212); delegate to the getter so tests keep driving behavior via checkPermission.
    usePermission: jest.fn((getValue: () => boolean) => getValue()),
    useUser: jest.fn(() => ({ id: 'admin' })),
  },
}));

// Mock child components
jest.mock('../RolesList', () => ({
  RolesList: ({ onSelect, onDelete, canUpdate, canDelete }: any) => (
    <div
      data-testid="roles-list"
      data-can-update={String(!!canUpdate)}
      data-can-delete={String(!!canDelete)}
      data-has-delete-handler={String(!!onDelete)}
    >
      <button onClick={() => onSelect('test-role')}>Select Role</button>
    </div>
  ),
}));

jest.mock('../RoleForm', () => {
  const React = jest.requireActual('react');
  return {
    RoleForm: ({ onCancel, onComplete, onValidationChange, wizardStep }: any) => {
      // Step 3 is the final "Contained Roles" step - disabled when no privileges selected
      React.useEffect(() => {
        if (wizardStep === 3) {
          onValidationChange?.(false); // No privileges selected - Create Role disabled (P0 fix)
        } else {
          onValidationChange?.(true);
        }
      }, [onValidationChange, wizardStep]);

      return (
        <div data-testid="role-form">
          <div data-testid={`wizard-step-${wizardStep}`}>Step {wizardStep}</div>
          <button onClick={onCancel}>Cancel</button>
          <button onClick={() => onComplete?.()}>
            Complete
          </button>
        </div>
      );
    },
  };
});

jest.mock('../RoleProfilePage', () => {
  const _React = jest.requireActual('react');
  return {
    RoleProfilePage: () => <div data-testid="role-profile-page">Role Profile Page</div>,
  };
});

const mockUseRolesApi = useRolesApi as jest.MockedFunction<typeof useRolesApi>;
const mockCheckPermission = ExtJS.checkPermission as jest.Mock;

const mockRole = {
  id: 'test-role',
  version: '1',
  source: 'Default',
  name: 'Test Role',
  description: 'Test Description',
  readOnly: false,
  privileges: [],
  roles: [],
};

const renderWithTheme = (component: React.ReactNode) => {
  return render(
    <Theme>
      <ToastProvider>{component}</ToastProvider>
    </Theme>
  );
};

describe('RolesPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    window.location.hash = '';
    mockCheckPermission.mockReturnValue(true);
    mockUseRolesApi.mockReturnValue({
      loading: false,
      error: null,
      setError: jest.fn(),
      fetchRoles: jest.fn().mockResolvedValue([mockRole]),
      fetchRoleReferences: jest.fn().mockResolvedValue([]),
      fetchRoleSources: jest.fn().mockResolvedValue([]),
      fetchRolesFromSource: jest.fn().mockResolvedValue([]),
      fetchPrivilegeReferences: jest.fn().mockResolvedValue([]),
      findRole: jest.fn().mockResolvedValue(mockRole),
      createRole: jest.fn().mockResolvedValue(mockRole),
      updateRole: jest.fn().mockResolvedValue(mockRole),
      deleteRole: jest.fn().mockResolvedValue(undefined),
    });
  });

  it('should render the page header', () => {
    renderWithTheme(<RolesPage />);
    
    expect(screen.getAllByText('Roles').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('Manage roles and their privilege assignments')).toBeInTheDocument();
  });

  it('should show Create Role button when user has permission', () => {
    mockCheckPermission.mockReturnValue(true);
    renderWithTheme(<RolesPage />);
    
    expect(screen.getByRole('button', { name: 'Create Role' })).toBeInTheDocument();
  });

  it('Create Role button has blue accent color', () => {
    mockCheckPermission.mockReturnValue(true);
    renderWithTheme(<RolesPage />);
    const btn = screen.getByRole('button', { name: 'Create Role' });
    // Radix Button with color="blue" highContrast sets data-accent-color="blue"
    expect(btn).toHaveAttribute('data-accent-color', 'blue');
  });

  it('should disable Create Role button when user lacks permission', () => {
    mockCheckPermission.mockImplementation((perm: string) => perm !== 'nexus:roles:create');
    (global as any).NX.Permissions.check.mockImplementation((perm: string) => perm !== 'nexus:roles:create');
    renderWithTheme(<RolesPage />);

    // Large create button is shown but disabled (NEXUS-54212), not hidden.
    expect(screen.getByRole('button', { name: 'Create Role' })).toBeDisabled();
  });

  it('should show RolesList by default', () => {
    renderWithTheme(<RolesPage />);

    expect(screen.getByTestId('roles-list')).toBeInTheDocument();
  });

  it('passes update/delete permissions to RolesList when the user has them', () => {
    mockCheckPermission.mockReturnValue(true);
    renderWithTheme(<RolesPage />);

    const list = screen.getByTestId('roles-list');
    expect(list).toHaveAttribute('data-can-update', 'true');
    expect(list).toHaveAttribute('data-can-delete', 'true');
    expect(list).toHaveAttribute('data-has-delete-handler', 'true');
  });

  it('gates update/delete on RolesList when the user lacks those permissions (NEXUS-54212)', () => {
    // Read-only user: only nexus:roles:read granted.
    mockCheckPermission.mockImplementation((perm: string) => perm === 'nexus:roles:read');
    renderWithTheme(<RolesPage />);

    const list = screen.getByTestId('roles-list');
    expect(list).toHaveAttribute('data-can-update', 'false');
    expect(list).toHaveAttribute('data-can-delete', 'false');
    // onDelete is only wired when the user can delete, so no delete handler reaches the list.
    expect(list).toHaveAttribute('data-has-delete-handler', 'false');
  });

  it('should navigate to wizard when Create Role is clicked', async () => {
    renderWithTheme(<RolesPage />);
    
    fireEvent.click(screen.getByRole('button', { name: 'Create Role' }));
    
    expect(window.location.hash).toBe('#preview/admin/security/roles/create');
  });

  it('should render wizard when hash is create', async () => {
    window.location.hash = '#preview/admin/security/roles/create';
    renderWithTheme(<RolesPage />);

    expect(screen.getByTestId('wizard-form')).toBeInTheDocument();
    expect(screen.getByTestId('role-form')).toBeInTheDocument();
    expect(screen.getByText('Step 1: Type')).toBeInTheDocument();
  });

  it('should navigate through wizard steps', async () => {
    window.location.hash = '#preview/admin/security/roles/create';
    renderWithTheme(<RolesPage />);

    // Step 0: Role Type - wait for mock RoleForm to render
    await waitFor(() => {
      expect(screen.getByTestId('wizard-step-0')).toBeInTheDocument();
    });

    // Click Next via submit button
    fireEvent.submit(screen.getByTestId('wizard-form'));

    // Step 1: Setup
    await waitFor(() => {
      expect(screen.getByTestId('wizard-step-1')).toBeInTheDocument();
    });

    // Click Next via submit button
    fireEvent.submit(screen.getByTestId('wizard-form'));

    // Step 2: Privileges
    await waitFor(() => {
      expect(screen.getByTestId('wizard-step-2')).toBeInTheDocument();
    });

    // Click Next via submit button
    fireEvent.submit(screen.getByTestId('wizard-form'));

    // Step 3: Contained Roles (final step)
    await waitFor(() => {
      expect(screen.getByTestId('wizard-step-3')).toBeInTheDocument();
    });
  });

  it('P0: should disable Create Role button on final step when no privileges selected', async () => {
    window.location.hash = '#preview/admin/security/roles/create';
    renderWithTheme(<RolesPage />);

    // Wait for initial step to render
    await waitFor(() => {
      expect(screen.getByTestId('wizard-step-0')).toBeInTheDocument();
    });

    // Navigate through all steps to reach final step
    fireEvent.submit(screen.getByTestId('wizard-form')); // Go to step 1
    await waitFor(() => {
      expect(screen.getByTestId('wizard-step-1')).toBeInTheDocument();
    });

    fireEvent.submit(screen.getByTestId('wizard-form')); // Go to step 2
    await waitFor(() => {
      expect(screen.getByTestId('wizard-step-2')).toBeInTheDocument();
    });

    fireEvent.submit(screen.getByTestId('wizard-form')); // Go to step 3 (final)
    await waitFor(() => {
      expect(screen.getByTestId('wizard-step-3')).toBeInTheDocument();
    });

    // Button should be disabled because no privileges selected (isRoleFormValid = false)
    await waitFor(() => {
      const createButton = screen.getByTestId('form-submit');
      expect(createButton).toBeDisabled();
    });
  });

  it('should navigate to role profile when a role is selected', async () => {
    renderWithTheme(<RolesPage />);
    
    fireEvent.click(screen.getByText('Select Role'));
    
    expect(window.location.hash).toBe('#preview/admin/security/roles/test-role/profile');
  });

  it('should render role profile when hash has profile suffix', async () => {
    window.location.hash = '#preview/admin/security/roles/test-role/profile';
    renderWithTheme(<RolesPage />);
    
    await waitFor(() => {
      expect(screen.getByTestId('role-profile-page')).toBeInTheDocument();
    });
  });

  it('should render edit wizard when hash has role ID', async () => {
    window.location.hash = '#preview/admin/security/roles/test-role';
    renderWithTheme(<RolesPage />);

    await waitFor(() => {
      expect(screen.getByTestId('wizard-form')).toBeInTheDocument();
    });
    expect(screen.getByText(/Edit Test Role/)).toBeInTheDocument();
  });

  // NEXUS-54267 AC (c): edit wizard skips the immutable Type step and starts on Setup.
  it('edit wizard skips step 0 (Type) and starts on Setup', async () => {
    window.location.hash = '#preview/admin/security/roles/test-role';
    renderWithTheme(<RolesPage />);

    // Mock RoleForm renders `wizard-step-<N>` from the logical step it receives.
    // In edit mode the boundary shifts by +1, so internal step 0 → logical step 1 (Setup).
    await waitFor(() => {
      expect(screen.getByTestId('wizard-step-1')).toBeInTheDocument();
    });
    expect(screen.queryByTestId('wizard-step-0')).not.toBeInTheDocument();
    // Description reflects the 3-step edit flow, not the 4-step create flow.
    expect(screen.getByText(/Step 1: Setup/)).toBeInTheDocument();
  });

  it('should show error alert when there is an error', () => {
    mockUseRolesApi.mockReturnValue({
      ...mockUseRolesApi(),
      error: 'Test error message',
    });
    
    renderWithTheme(<RolesPage />);
    
    expect(screen.getByText('Test error message')).toBeInTheDocument();
  });
});
