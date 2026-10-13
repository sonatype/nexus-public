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
import {
  getCurrentHash,
  installHashStub,
  setInitialHash,
  uninstallHashStub,
} from './testUtils/hashStub';

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

// Mock child components. NEXUS-54589: RolesList now takes only (onSelect, onCreate); the
// actions column and its per-row permission props are gone. Row click passes just the role id.
jest.mock('../RolesList', () => ({
  RolesList: ({ onSelect }: any) => (
    <div data-testid="roles-list">
      <button onClick={() => onSelect('test-role')}>Select Role</button>
    </div>
  ),
}));

jest.mock('../RoleForm', () => ({
  RoleForm: ({ onCancel, onComplete, isCreate }: any) => (
    <div data-testid="role-form" data-is-create={String(!!isCreate)}>
      <button onClick={onCancel}>Cancel</button>
      <button onClick={() => onComplete?.()}>Complete</button>
    </div>
  ),
}));

const mockRoleDetailTestState: { autoInvokeOnDelete: boolean } = { autoInvokeOnDelete: false };

jest.mock('../RoleDetail', () => {
  // jest.mock() hoists above imports; require react here or ReactMod is undefined.
  const ReactMod = require('react');
  return {
    RoleDetail: ({ role, canEdit, canDelete, onDelete }: any) => {
      ReactMod.useEffect(() => {
        if (mockRoleDetailTestState.autoInvokeOnDelete) {
          onDelete?.();
        }
      }, [onDelete]);
      return ReactMod.createElement(
        'div',
        {
          'data-testid': 'role-detail',
          'data-can-edit': String(!!canEdit),
          'data-can-delete': String(!!canDelete),
        },
        `Role Detail${role ? `: ${role.name}` : ''}`,
      );
    },
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
  beforeAll(() => {
    installHashStub({ dispatchOnAssign: true });
  });

  afterAll(() => {
    uninstallHashStub();
  });

  beforeEach(() => {
    jest.clearAllMocks();
    setInitialHash('');
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

  it('should navigate to the create route when Create Role is clicked', async () => {
    renderWithTheme(<RolesPage />);

    fireEvent.click(screen.getByRole('button', { name: 'Create Role' }));

    expect(getCurrentHash()).toBe('#preview/admin/security/roles/create');
  });

  it('should render flat create form when hash is create', async () => {
    setInitialHash('#preview/admin/security/roles/create');
    renderWithTheme(<RolesPage />);

    await waitFor(() => {
      expect(screen.getByTestId('role-form')).toBeInTheDocument();
    });
    expect(screen.getByTestId('role-form')).toHaveAttribute('data-is-create', 'true');
    expect(screen.getByText('Create Role')).toBeInTheDocument();
  });

  // NEXUS-54589 AC1: single row-click destination — no /profile suffix, no second mode arg.
  it('should navigate to the role detail URL when a role is selected', async () => {
    renderWithTheme(<RolesPage />);

    fireEvent.click(screen.getByText('Select Role'));

    expect(getCurrentHash()).toBe('#preview/admin/security/roles/test-role');
  });

  // NEXUS-54589: any legacy /profile suffix in a bookmarked URL still resolves to the unified
  // detail route (parseRoute treats it as the role id segment's tail). Guard so a rename of
  // parseRoute doesn't quietly break bookmarks.
  it('should treat a legacy /profile suffix as the unified detail route', async () => {
    setInitialHash('#preview/admin/security/roles/test-role/profile');
    renderWithTheme(<RolesPage />);

    await waitFor(() => {
      expect(screen.getByTestId('role-detail')).toBeInTheDocument();
    });
    expect(screen.queryByTestId('role-profile-page')).not.toBeInTheDocument();
  });

  it('should render editable RoleDetail when hash has role ID and user has update permission', async () => {
    setInitialHash('#preview/admin/security/roles/test-role');
    renderWithTheme(<RolesPage />);

    await waitFor(() => {
      expect(screen.getByTestId('role-detail')).toBeInTheDocument();
    });
    expect(screen.getByTestId('role-detail')).toHaveAttribute('data-can-edit', 'true');
    expect(screen.getByText(/Edit Test Role/)).toBeInTheDocument();
  });

  // NEXUS-54589 AC2: user with view-only permission on an editable role sees the read-only
  // RoleDetail branch (canEdit=false; RoleDetail internally routes to the read-only render).
  it('should render RoleDetail with canEdit=false when the user lacks the update permission', async () => {
    mockCheckPermission.mockImplementation((perm: string) => perm === 'nexus:roles:read');
    setInitialHash('#preview/admin/security/roles/test-role');
    renderWithTheme(<RolesPage />);

    await waitFor(() => {
      expect(screen.getByTestId('role-detail')).toBeInTheDocument();
    });
    expect(screen.getByTestId('role-detail')).toHaveAttribute('data-can-edit', 'false');
    // Header shows the plain role name, not the "Edit {name}" wording.
    expect(screen.queryByText(/Edit Test Role/)).not.toBeInTheDocument();
  });

  // NEXUS-54589 AC2: system-protected roles (nx-admin, nx-anonymous) come back with
  // readOnly=true; render with canEdit=false regardless of the user's update permission.
  it('should render RoleDetail with canEdit=false when the role is backend-flagged readOnly=true', async () => {
    const readOnlyRole = { ...mockRole, readOnly: true };
    mockUseRolesApi.mockReturnValue({
      loading: false,
      error: null,
      setError: jest.fn(),
      fetchRoles: jest.fn().mockResolvedValue([readOnlyRole]),
      fetchRoleReferences: jest.fn().mockResolvedValue([]),
      fetchRoleSources: jest.fn().mockResolvedValue([]),
      fetchRolesFromSource: jest.fn().mockResolvedValue([]),
      fetchPrivilegeReferences: jest.fn().mockResolvedValue([]),
      findRole: jest.fn().mockResolvedValue(readOnlyRole),
      createRole: jest.fn(),
      updateRole: jest.fn(),
      deleteRole: jest.fn(),
    });
    setInitialHash('#preview/admin/security/roles/test-role');
    renderWithTheme(<RolesPage />);

    await waitFor(() => {
      expect(screen.getByTestId('role-detail')).toBeInTheDocument();
    });
    expect(screen.getByTestId('role-detail')).toHaveAttribute('data-can-edit', 'false');
  });

  // NEXUS-54589 AC0: external-source roles (LDAP/SAML/Crowd/OAuth2) that the backend
  // does NOT flag readOnly must render editable, not the read-only view.
  // Reproduces the pre-fix bug where isReadOnlyRole locked every external role.
  it('should render RoleDetail with canEdit=true for an external role when readOnly is false', async () => {
    const externalRole = { ...mockRole, source: 'LDAP', readOnly: false };
    mockUseRolesApi.mockReturnValue({
      loading: false,
      error: null,
      setError: jest.fn(),
      fetchRoles: jest.fn().mockResolvedValue([externalRole]),
      fetchRoleReferences: jest.fn().mockResolvedValue([]),
      fetchRoleSources: jest.fn().mockResolvedValue([]),
      fetchRolesFromSource: jest.fn().mockResolvedValue([]),
      fetchPrivilegeReferences: jest.fn().mockResolvedValue([]),
      findRole: jest.fn().mockResolvedValue(externalRole),
      createRole: jest.fn(),
      updateRole: jest.fn(),
      deleteRole: jest.fn(),
    });
    setInitialHash('#preview/admin/security/roles/test-role');
    renderWithTheme(<RolesPage />);

    await waitFor(() => {
      expect(screen.getByTestId('role-detail')).toBeInTheDocument();
    });
    expect(screen.getByTestId('role-detail')).toHaveAttribute('data-can-edit', 'true');
    expect(screen.getByText(/Edit Test Role/)).toBeInTheDocument();
  });

  it('should show error alert when there is an error', () => {
    mockUseRolesApi.mockReturnValue({
      ...mockUseRolesApi(),
      error: 'Test error message',
    });

    renderWithTheme(<RolesPage />);

    expect(screen.getByText('Test error message')).toBeInTheDocument();
  });

  describe('Delete confirmation modal', () => {
    const CONFIRMATION_WORD = 'Delete';
    const CONFIRMATION_PLACEHOLDER = new RegExp(`type "${CONFIRMATION_WORD}" to confirm`, 'i');

    beforeEach(() => {
      mockRoleDetailTestState.autoInvokeOnDelete = true;
    });

    afterEach(() => {
      mockRoleDetailTestState.autoInvokeOnDelete = false;
    });

    it('does not render the modal until handleDelete opens the dialog', async () => {
      mockRoleDetailTestState.autoInvokeOnDelete = false;
      setInitialHash('#preview/admin/security/roles/test-role');
      renderWithTheme(<RolesPage />);

      await waitFor(() => {
        expect(screen.getByTestId('role-detail')).toBeInTheDocument();
      });
      expect(screen.queryByPlaceholderText(CONFIRMATION_PLACEHOLDER)).not.toBeInTheDocument();
    });

    it('renders the role name inside the modal description once opened', async () => {
      setInitialHash('#preview/admin/security/roles/test-role');
      renderWithTheme(<RolesPage />);

      const input = await screen.findByPlaceholderText(CONFIRMATION_PLACEHOLDER);
      expect(input).toBeInTheDocument();
      expect(screen.getByText(/All data associated with/)).toBeInTheDocument();
      expect(screen.getAllByText(/Test Role/).length).toBeGreaterThanOrEqual(2);
    });

    it('keeps the confirm button disabled until the user types the confirmation word', async () => {
      setInitialHash('#preview/admin/security/roles/test-role');
      renderWithTheme(<RolesPage />);

      const input = await screen.findByPlaceholderText(CONFIRMATION_PLACEHOLDER);
      const confirmButton = screen.getByRole('button', { name: /^delete$/i });
      expect(confirmButton).toBeDisabled();

      fireEvent.change(input, { target: { value: 'delete role' } });
      expect(confirmButton).toBeDisabled();

      fireEvent.change(input, { target: { value: CONFIRMATION_WORD.toLowerCase() } });
      expect(confirmButton).not.toBeDisabled();
    });

    it('calls deleteRole and navigates back to the list on confirmation', async () => {
      const mockDeleteRole = jest.fn().mockResolvedValue(undefined);
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
        createRole: jest.fn(),
        updateRole: jest.fn(),
        deleteRole: mockDeleteRole,
      });
      setInitialHash('#preview/admin/security/roles/test-role');
      renderWithTheme(<RolesPage />);

      const input = await screen.findByPlaceholderText(CONFIRMATION_PLACEHOLDER);
      fireEvent.change(input, { target: { value: CONFIRMATION_WORD } });
      fireEvent.click(screen.getByRole('button', { name: /^delete$/i }));

      await waitFor(() => {
        expect(mockDeleteRole).toHaveBeenCalledWith('test-role');
      });
      await waitFor(() => {
        expect(getCurrentHash()).toBe('#preview/admin/security/roles');
      });
    });

    it('does not render the modal when the user lacks delete permission', async () => {
      mockCheckPermission.mockImplementation((perm: string) => perm !== 'nexus:roles:delete');
      setInitialHash('#preview/admin/security/roles/test-role');
      renderWithTheme(<RolesPage />);

      await waitFor(() => {
        expect(screen.getByTestId('role-detail')).toBeInTheDocument();
      });
      expect(screen.queryByPlaceholderText(CONFIRMATION_PLACEHOLDER)).not.toBeInTheDocument();
    });

    it('does not render the modal for a backend-readOnly role even when the user has delete permission', async () => {
      const readOnlyRole = { ...mockRole, readOnly: true };
      mockUseRolesApi.mockReturnValue({
        loading: false,
        error: null,
        setError: jest.fn(),
        fetchRoles: jest.fn().mockResolvedValue([readOnlyRole]),
        fetchRoleReferences: jest.fn().mockResolvedValue([]),
        fetchRoleSources: jest.fn().mockResolvedValue([]),
        fetchRolesFromSource: jest.fn().mockResolvedValue([]),
        fetchPrivilegeReferences: jest.fn().mockResolvedValue([]),
        findRole: jest.fn().mockResolvedValue(readOnlyRole),
        createRole: jest.fn(),
        updateRole: jest.fn(),
        deleteRole: jest.fn(),
      });
      setInitialHash('#preview/admin/security/roles/test-role');
      renderWithTheme(<RolesPage />);

      await waitFor(() => {
        expect(screen.getByTestId('role-detail')).toBeInTheDocument();
      });
      expect(screen.queryByPlaceholderText(CONFIRMATION_PLACEHOLDER)).not.toBeInTheDocument();
    });
  });
});
