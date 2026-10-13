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
import { Theme } from '@radix-ui/themes';
import { RoleDetail } from '../RoleDetail';
import { useRolesApi } from '../useRolesApi';
import { useRoleEffectivePrivileges } from '../useRoleEffectivePrivileges';
import { useAssignedUsers } from '../useAssignedUsers';
import { Role } from '../types';

// Mock useRolesApi and the RoleDetail edit-surface hooks so the editable render
// doesn't attempt real API calls. Each hook is stubbed to a synchronous shape.
jest.mock('../useRolesApi');
jest.mock('../useRoleReferenceData', () => ({
  useRoleReferenceData: jest.fn(() => ({
    allPrivileges: [],
    allRoles: [],
    loading: false,
    error: null,
  })),
}));
jest.mock('../useRoleEffectivePrivileges', () => ({
  useRoleEffectivePrivileges: jest.fn(() => ({
    privileges: [],
    roleMap: new Map(),
    loading: false,
    error: null,
  })),
}));
jest.mock('../useAssignedUsers', () => ({
  useAssignedUsers: jest.fn(() => ({ users: [], loading: false, error: null })),
}));
jest.mock('../useRoleTree', () => ({
  useCombinedRoleTree: jest.fn(() => ({
    tree: [],
    loading: false,
    error: null,
    toggleExpand: jest.fn(),
    expandAll: jest.fn(),
    collapseAll: jest.fn(),
  })),
}));

const mockUseRolesApi = useRolesApi as jest.MockedFunction<typeof useRolesApi>;

const READ_ONLY_BANNER_TEXT = /read-only and cannot be modified/i;

const mockRole: Role = {
  id: 'test-role',
  version: '1',
  source: 'Default',
  name: 'Test Role',
  description: 'Test Description',
  readOnly: false,
  privileges: ['nx-search-read', 'nx-repository-view-*'],
  roles: ['nx-anonymous'],
};

const mockReadOnlyRole: Role = {
  ...mockRole,
  readOnly: true,
};

const mockExternalEditableRole: Role = {
  ...mockRole,
  source: 'LDAP',
  readOnly: false,
};

const renderWithTheme = (component: React.ReactNode) => {
  return render(<Theme>{component}</Theme>);
};

describe('RoleDetail', () => {
  const mockOnCancel = jest.fn();
  const mockOnDelete = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockUseRolesApi.mockReturnValue({
      loading: false,
      error: null,
      setError: jest.fn(),
      fetchRoles: jest.fn().mockResolvedValue([]),
      fetchRoleReferences: jest.fn().mockResolvedValue([]),
      fetchRoleSources: jest.fn().mockResolvedValue([]),
      fetchRolesFromSource: jest.fn().mockResolvedValue([]),
      fetchPrivilegeReferences: jest.fn().mockResolvedValue([]),
      findRole: jest.fn().mockResolvedValue(null),
      createRole: jest.fn(),
      updateRole: jest.fn(),
      deleteRole: jest.fn(),
    });
  });

  describe('Loading State', () => {
    it('should show loading indicator when loading', () => {
      renderWithTheme(
        <RoleDetail
          role={null}
          loading={true}
          canEdit={true}
          canDelete={true}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
        />
      );

      expect(screen.getByText('Loading role details...')).toBeInTheDocument();
    });
  });

  describe('Not Found State', () => {
    it('should show not found message when role is null', () => {
      renderWithTheme(
        <RoleDetail
          role={null}
          loading={false}
          canEdit={true}
          canDelete={true}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
        />
      );

      expect(screen.getByText('Role not found')).toBeInTheDocument();
    });
  });

  describe('Read-Only View', () => {
    it('should render read-only metadata when canEdit is false', () => {
      renderWithTheme(
        <RoleDetail
          role={mockRole}
          loading={false}
          canEdit={false}
          canDelete={false}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
        />
      );

      expect(screen.getByText('Role Information')).toBeInTheDocument();
      expect(screen.getByText('test-role')).toBeInTheDocument();
      expect(screen.getByText('Test Role')).toBeInTheDocument();
      expect(screen.getByText('Test Description')).toBeInTheDocument();
      expect(screen.queryByTestId('role-form')).not.toBeInTheDocument();
    });

    // NEXUS-54589: the "This role is read-only" banner is a signal that the *backend* has
    // flagged the role as system-protected (readOnly=true), not that the current user
    // lacks the edit permission. When canEdit=false + readOnly=false, the user simply
    // can't edit — no banner.
    it('should show the read-only banner only when the backend flags the role readOnly=true', () => {
      renderWithTheme(
        <RoleDetail
          role={mockReadOnlyRole}
          loading={false}
          canEdit={false}
          canDelete={false}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
        />
      );

      expect(screen.getByText(READ_ONLY_BANNER_TEXT)).toBeInTheDocument();
    });

    it('should NOT show the read-only banner when the user lacks edit permission but the role is not backend-readOnly', () => {
      renderWithTheme(
        <RoleDetail
          role={mockRole}
          loading={false}
          canEdit={false}
          canDelete={false}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
        />
      );

      expect(screen.queryByText(READ_ONLY_BANNER_TEXT)).not.toBeInTheDocument();
    });

    it('should render the error prop in the read-only branch when provided', () => {
      const FETCH_ERROR_MESSAGE = 'Failed to load role from server';
      renderWithTheme(
        <RoleDetail
          role={mockRole}
          loading={false}
          canEdit={false}
          canDelete={false}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
          error={FETCH_ERROR_MESSAGE}
        />
      );

      expect(screen.getByText(FETCH_ERROR_MESSAGE)).toBeInTheDocument();
    });

    it('should display privileges list in read-only view', () => {
      renderWithTheme(
        <RoleDetail
          role={mockRole}
          loading={false}
          canEdit={false}
          canDelete={false}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
        />
      );

      expect(screen.getByText('Privileges')).toBeInTheDocument();
      expect(screen.getByText('nx-search-read')).toBeInTheDocument();
      expect(screen.getByText('nx-repository-view-*')).toBeInTheDocument();
    });

    it('should display contained roles list in read-only view', () => {
      renderWithTheme(
        <RoleDetail
          role={mockRole}
          loading={false}
          canEdit={false}
          canDelete={false}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
        />
      );

      expect(screen.getByText('Contained Roles')).toBeInTheDocument();
      expect(screen.getByText('nx-anonymous')).toBeInTheDocument();
    });

    it('should show empty message when no privileges', () => {
      const roleWithNoPrivileges = { ...mockRole, privileges: [] };

      renderWithTheme(
        <RoleDetail
          role={roleWithNoPrivileges}
          loading={false}
          canEdit={false}
          canDelete={false}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
        />
      );

      expect(screen.getByText('No privileges assigned')).toBeInTheDocument();
    });

    it('should show empty message when no contained roles', () => {
      const roleWithNoRoles = { ...mockRole, roles: [] };

      renderWithTheme(
        <RoleDetail
          role={roleWithNoRoles}
          loading={false}
          canEdit={false}
          canDelete={false}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
        />
      );

      expect(screen.getByText('No contained roles')).toBeInTheDocument();
    });

    it('renders Effective Permissions, Security Tree, and Assigned Users sections in read-only view', () => {
      renderWithTheme(
        <RoleDetail
          role={mockRole}
          loading={false}
          canEdit={false}
          canDelete={false}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
        />
      );

      expect(screen.getByText('Effective Permissions')).toBeInTheDocument();
      expect(screen.getByText('Security Tree')).toBeInTheDocument();
      expect(screen.getByText('Assigned Users')).toBeInTheDocument();
    });

    it('renders the Delete Role button on the read-only branch when canDelete is true', () => {
      renderWithTheme(
        <RoleDetail
          role={mockRole}
          loading={false}
          canEdit={false}
          canDelete={true}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
        />
      );

      expect(screen.getByTestId('role-detail-delete')).toBeInTheDocument();
    });

    it('invokes onDelete when the read-only branch Delete Role button is clicked', () => {
      renderWithTheme(
        <RoleDetail
          role={mockRole}
          loading={false}
          canEdit={false}
          canDelete={true}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
        />
      );

      fireEvent.click(screen.getByTestId('role-detail-delete'));
      expect(mockOnDelete).toHaveBeenCalledTimes(1);
    });

    it('hides the Delete Role button on the read-only branch when canDelete is false', () => {
      renderWithTheme(
        <RoleDetail
          role={mockRole}
          loading={false}
          canEdit={false}
          canDelete={false}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
        />
      );

      expect(screen.queryByTestId('role-detail-delete')).not.toBeInTheDocument();
    });

    it('hides the Delete Role button on the read-only branch when canDelete is true but onDelete is undefined', () => {
      renderWithTheme(
        <RoleDetail
          role={mockRole}
          loading={false}
          canEdit={false}
          canDelete={true}
          onDelete={undefined as unknown as () => void}
          onCancel={mockOnCancel}
        />
      );

      expect(screen.queryByTestId('role-detail-delete')).not.toBeInTheDocument();
    });

    it('renders the Delete Role button on the read-only branch for an external-source role when canDelete is true', () => {
      renderWithTheme(
        <RoleDetail
          role={mockExternalEditableRole}
          loading={false}
          canEdit={false}
          canDelete={true}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
        />
      );

      expect(screen.getByTestId('role-detail-delete')).toBeInTheDocument();
    });

    it('hides the Delete Role button on the read-only branch when the role is backend-readOnly, even if canDelete is true', () => {
      renderWithTheme(
        <RoleDetail
          role={mockReadOnlyRole}
          loading={false}
          canEdit={false}
          canDelete={true}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
        />
      );

      expect(screen.queryByTestId('role-detail-delete')).not.toBeInTheDocument();
    });
  });

  describe('Edit View (inline-edit)', () => {
    it('renders inline-editable Name and Description', () => {
      renderWithTheme(
        <RoleDetail
          role={mockRole}
          loading={false}
          canEdit={true}
          canDelete={true}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
        />,
      );

      const nameInput = screen.getByLabelText(/^Name/) as HTMLInputElement;
      const descInput = screen.getByLabelText(/^Description/) as HTMLTextAreaElement;
      expect(nameInput).not.toBeDisabled();
      expect(descInput).not.toBeDisabled();
      expect(nameInput.value).toBe(mockRole.name);
      expect(descInput.value).toBe(mockRole.description);
    });

    it('renders ID as read-only in edit mode; Source lives in the page header', () => {
      renderWithTheme(
        <RoleDetail
          role={mockRole}
          loading={false}
          canEdit={true}
          canDelete={true}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
        />,
      );

      // ID is displayed via MetadataGrid, not as an editable input.
      expect(screen.queryByLabelText(/^ID/)).not.toBeInTheDocument();
      expect(screen.getByText('test-role')).toBeInTheDocument();
      // Source is intentionally NOT duplicated inside the form; it is shown in
      // the page header's description slot (RolesPage.tsx).
      expect(screen.queryByLabelText(/^Source/)).not.toBeInTheDocument();
    });

    it('renders Save and Cancel disabled when the pending state matches committed', () => {
      renderWithTheme(
        <RoleDetail
          role={mockRole}
          loading={false}
          canEdit={true}
          canDelete={true}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
        />,
      );

      expect(screen.getByTestId('form-submit')).toBeDisabled();
      expect(screen.getByTestId('form-cancel')).toBeDisabled();
    });

    it('shows the Delete Role button when canDelete and role is not backend-readOnly', () => {
      renderWithTheme(
        <RoleDetail
          role={mockRole}
          loading={false}
          canEdit={true}
          canDelete={true}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
        />,
      );

      expect(screen.getByTestId('role-detail-delete')).toBeInTheDocument();
    });

    it('hides the Delete Role button when canDelete is false', () => {
      renderWithTheme(
        <RoleDetail
          role={mockRole}
          loading={false}
          canEdit={true}
          canDelete={false}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
        />,
      );

      expect(screen.queryByTestId('role-detail-delete')).not.toBeInTheDocument();
    });

    it('surfaces the error prop in an alert', () => {
      const EDIT_MODE_ERROR = 'validation failed';
      renderWithTheme(
        <RoleDetail
          role={mockRole}
          loading={false}
          canEdit={true}
          canDelete={false}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
          error={EDIT_MODE_ERROR}
        />,
      );

      expect(screen.getByText(EDIT_MODE_ERROR)).toBeInTheDocument();
    });

    it('renders the editable surface for an external role when canEdit is true and readOnly is false', () => {
      renderWithTheme(
        <RoleDetail
          role={mockExternalEditableRole}
          loading={false}
          canEdit={true}
          canDelete={true}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
        />,
      );

      // The picker triggers exist -> we're on the editable render.
      expect(screen.getByTestId('role-detail-open-privileges')).toBeInTheDocument();
      expect(screen.getByTestId('role-detail-open-contained-roles')).toBeInTheDocument();
      // No read-only banner leaks through.
      expect(screen.queryByText(READ_ONLY_BANNER_TEXT)).not.toBeInTheDocument();
    });

    it('renders the Delete Role button on the editable branch for an external-source role when canDelete is true', () => {
      renderWithTheme(
        <RoleDetail
          role={mockExternalEditableRole}
          loading={false}
          canEdit={true}
          canDelete={true}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
        />,
      );

      expect(screen.getByTestId('role-detail-delete')).toBeInTheDocument();
    });

    it('renders Effective Permissions, Security Tree, and Assigned Users sections', () => {
      renderWithTheme(
        <RoleDetail
          role={mockRole}
          loading={false}
          canEdit={true}
          canDelete={true}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
        />,
      );

      expect(screen.getByText('Effective Permissions')).toBeInTheDocument();
      expect(screen.getByText('Security Tree')).toBeInTheDocument();
      expect(screen.getByText('Assigned Users')).toBeInTheDocument();
    });
  });

  describe('Effective Permissions section', () => {
    const mockEffective = useRoleEffectivePrivileges as jest.MockedFunction<typeof useRoleEffectivePrivileges>;

    afterEach(() => {
      mockEffective.mockImplementation((): any => ({
        privileges: [],
        roleMap: new Map(),
        loading: false,
        error: null,
      }));
    });

    it('shows a loading indicator while effective privileges are computing', () => {
      mockEffective.mockReturnValueOnce({
        privileges: [],
        roleMap: new Map(),
        loading: true,
        error: null,
      } as any);
      renderWithTheme(
        <RoleDetail
          role={mockRole}
          loading={false}
          canEdit={true}
          canDelete={true}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
        />,
      );
      expect(screen.getByText('Computing effective permissions...')).toBeInTheDocument();
    });

    it('surfaces an error when effective-privileges fetch fails', () => {
      mockEffective.mockReturnValueOnce({
        privileges: [],
        roleMap: new Map(),
        loading: false,
        error: 'privs down',
      } as any);
      renderWithTheme(
        <RoleDetail
          role={mockRole}
          loading={false}
          canEdit={true}
          canDelete={true}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
        />,
      );
      expect(screen.getByText('privs down')).toBeInTheDocument();
    });

    it('renders each effective privilege with its granted-by attribution', () => {
      mockEffective.mockReturnValueOnce({
        privileges: [
          {
            id: 'p1',
            name: 'p1',
            description: 'first',
            grantedByRoleIds: ['test-role'],
          } as any,
        ],
        roleMap: new Map([
          ['test-role', { id: 'test-role', name: 'Test Role' } as any],
        ]),
        loading: false,
        error: null,
      } as any);
      renderWithTheme(
        <RoleDetail
          role={mockRole}
          loading={false}
          canEdit={true}
          canDelete={true}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
        />,
      );
      expect(screen.getByTestId('role-detail-effective-privileges')).toBeInTheDocument();
      expect(screen.getByText('1 effective privilege')).toBeInTheDocument();
      expect(screen.getByText(/Granted by: Test Role/)).toBeInTheDocument();
    });
  });

  describe('Assigned Users section', () => {
    const mockAssigned = useAssignedUsers as jest.MockedFunction<typeof useAssignedUsers>;

    afterEach(() => {
      mockAssigned.mockImplementation((): any => ({
        users: [],
        loading: false,
        error: null,
      }));
    });

    it('shows a loading indicator while users are loading', () => {
      mockAssigned.mockReturnValueOnce({ users: [], loading: true, error: null } as any);
      renderWithTheme(
        <RoleDetail
          role={mockRole}
          loading={false}
          canEdit={false}
          canDelete={false}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
        />,
      );
      expect(screen.getByText('Loading assigned users...')).toBeInTheDocument();
    });

    it('surfaces an error when the users fetch fails', () => {
      mockAssigned.mockReturnValueOnce({
        users: [],
        loading: false,
        error: 'users down',
      } as any);
      renderWithTheme(
        <RoleDetail
          role={mockRole}
          loading={false}
          canEdit={false}
          canDelete={false}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
        />,
      );
      expect(screen.getByText('users down')).toBeInTheDocument();
    });

    it('renders the assigned users list with names and source labels', () => {
      mockAssigned.mockReturnValueOnce({
        users: [
          {
            userId: 'alice',
            firstName: 'Alice',
            lastName: 'A',
            source: 'default',
            realm: 'default',
            emailAddress: 'a@x',
            status: 'active',
            roles: ['test-role'],
          } as any,
        ],
        loading: false,
        error: null,
      } as any);
      renderWithTheme(
        <RoleDetail
          role={mockRole}
          loading={false}
          canEdit={false}
          canDelete={false}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
        />,
      );
      expect(screen.getByTestId('role-detail-assigned-users')).toBeInTheDocument();
      expect(screen.getByText('1 user assigned')).toBeInTheDocument();
      expect(screen.getByText('alice')).toBeInTheDocument();
      expect(screen.getByText(/Alice A/)).toBeInTheDocument();
    });
  });
});
