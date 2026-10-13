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
import { RoleForm } from '../RoleForm';
import { useRolesApi } from '../useRolesApi';
import { useRolesForm } from '../useRolesForm';
import { Role, NEXUS_SOURCE, isExternalRole } from '../types';

jest.mock('../useRolesApi');
jest.mock('../useRolesForm');

const mockUseRolesForm = useRolesForm as jest.MockedFunction<typeof useRolesForm>;
const mockUseRolesApi = useRolesApi as jest.MockedFunction<typeof useRolesApi>;

const mockPrivilegeRefs = [
  { id: 'nx-all', name: 'nx-all', description: 'All privileges' },
  { id: 'nx-search-read', name: 'nx-search-read', description: 'Read search' },
  { id: 'nx-repository-view-*', name: 'nx-repository-view-*', description: 'Read repos' },
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

function createMockForm(
  data: any = {},
  validationErrors: Record<string, string> = {},
  isPristine = true,
) {
  return {
    field: jest.fn((name: string) => {
      const keys = name.split('.');
      let value: any = data;
      for (const key of keys) {
        value = value?.[key];
      }
      return {
        name,
        value: value != null ? String(value) : '',
        onChange: jest.fn(),
        onBlur: jest.fn(),
        error: undefined,
      };
    }),
    data,
    isPristine,
    isSaving: false,
    isLoading: false,
    isDeleting: false,
    saveError: null,
    validationErrors,
    state: {
      matches: jest.fn(() => false),
      context: {
        data,
        allPrivileges: mockPrivilegeRefs,
        allRoles: mockRoleRefs,
        allSources: [],
      },
    },
    send: jest.fn(),
    submit: jest.fn(),
    checkbox: jest.fn(() => ({ name: '', checked: false, onChange: jest.fn() })),
    select: jest.fn(() => ({ name: '', value: '', onChange: jest.fn() })),
  } as any;
}

function mockFormFor(role: Role | null, extraFormData: any = {}, isPristine = true) {
  const external = role ? isExternalRole(role.source || NEXUS_SOURCE) : false;
  const formData = role
    ? {
        id: role.id,
        name: role.name,
        description: role.description || '',
        privileges: role.privileges || [],
        roles: role.roles || [],
        source: role.source || NEXUS_SOURCE,
        roleType: external ? 'external' : 'nexus',
        externalSource: external ? role.source : undefined,
        ...extraFormData,
      }
    : {
        id: '',
        name: '',
        description: '',
        privileges: [],
        roles: [],
        source: NEXUS_SOURCE,
        roleType: 'nexus',
        ...extraFormData,
      };
  mockUseRolesForm.mockReturnValue({
    form: createMockForm(formData, {}, isPristine),
    role,
    isCreate: !role,
  } as any);
}

function renderWithTheme(component: React.ReactNode) {
  return render(<Theme>{component}</Theme>);
}

describe('RoleForm', () => {
  const mockOnCancel = jest.fn();
  const mockOnComplete = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockFormFor(null);
    mockUseRolesApi.mockReturnValue({
      loading: false,
      error: null,
      setError: jest.fn(),
      fetchRoles: jest.fn().mockResolvedValue([]),
      fetchRoleReferences: jest.fn().mockResolvedValue(mockRoleRefs),
      fetchRoleSources: jest.fn().mockResolvedValue([]),
      fetchRolesFromSource: jest.fn().mockResolvedValue([]),
      searchRoles: jest.fn().mockResolvedValue([]),
      fetchPrivilegeReferences: jest.fn().mockResolvedValue(mockPrivilegeRefs),
      findRole: jest.fn().mockResolvedValue(null),
      createRole: jest.fn(),
      updateRole: jest.fn(),
      deleteRole: jest.fn(),
    } as any);
  });

  describe('flat form layout', () => {
    it('renders every section inline in Create mode without any wizard step gating', () => {
      renderWithTheme(<RoleForm isCreate onCancel={mockOnCancel} onComplete={mockOnComplete} />);

      expect(screen.getByText('Role Type')).toBeInTheDocument();
      expect(screen.getByText('Role Setup')).toBeInTheDocument();
      expect(screen.getByText('Privileges')).toBeInTheDocument();
      expect(screen.getByText('Contained Roles')).toBeInTheDocument();
    });

    it('omits the Role Type section in Edit mode (type is immutable)', () => {
      mockFormFor(mockRole);
      renderWithTheme(
        <RoleForm isCreate={false} role={mockRole} onCancel={mockOnCancel} onComplete={mockOnComplete} />,
      );

      expect(screen.queryByText('Role Type')).not.toBeInTheDocument();
      expect(screen.getByText('Role Setup')).toBeInTheDocument();
    });

    it('renders the External Source section in Create when roleType=external', () => {
      mockFormFor(null, { roleType: 'external', externalSource: 'LDAP' });
      renderWithTheme(<RoleForm isCreate onCancel={mockOnCancel} onComplete={mockOnComplete} />);

      expect(screen.getByRole('heading', { name: 'External Source' })).toBeInTheDocument();
    });

    it('hides the External Source section in Create when roleType=nexus', () => {
      mockFormFor(null, { roleType: 'nexus' });
      renderWithTheme(<RoleForm isCreate onCancel={mockOnCancel} onComplete={mockOnComplete} />);

      expect(screen.queryByRole('heading', { name: 'External Source' })).not.toBeInTheDocument();
    });

    it('renders the External Source section in Edit when the role is external', () => {
      const externalRole = { ...mockRole, source: 'LDAP' };
      mockFormFor(externalRole);
      renderWithTheme(
        <RoleForm isCreate={false} role={externalRole} onCancel={mockOnCancel} onComplete={mockOnComplete} />,
      );

      expect(screen.getByRole('heading', { name: 'External Source' })).toBeInTheDocument();
    });
  });

  describe('privilege + role selection summary', () => {
    it('shows a zero-count selection summary in Create', () => {
      renderWithTheme(<RoleForm isCreate onCancel={mockOnCancel} onComplete={mockOnComplete} />);

      expect(screen.getByTestId('role-form-privileges-count')).toHaveTextContent('0 privileges selected');
      expect(screen.getByTestId('role-form-roles-count')).toHaveTextContent('0 roles selected');
    });

    it('reflects the current form-data counts', () => {
      mockFormFor(null, { privileges: ['a', 'b', 'c'], roles: ['x'] });
      renderWithTheme(<RoleForm isCreate onCancel={mockOnCancel} onComplete={mockOnComplete} />);

      expect(screen.getByTestId('role-form-privileges-count')).toHaveTextContent('3 privileges selected');
      expect(screen.getByTestId('role-form-roles-count')).toHaveTextContent('1 role selected');
    });

    it('opens the privileges picker when Edit Privileges is clicked', () => {
      renderWithTheme(<RoleForm isCreate onCancel={mockOnCancel} onComplete={mockOnComplete} />);

      expect(screen.queryByTestId('privileges-picker')).not.toBeInTheDocument();
      fireEvent.click(screen.getByTestId('role-form-open-privileges'));
      expect(screen.getByTestId('privileges-picker')).toBeInTheDocument();
    });

    it('opens the contained-roles picker when Edit Contained Roles is clicked', () => {
      renderWithTheme(<RoleForm isCreate onCancel={mockOnCancel} onComplete={mockOnComplete} />);

      expect(screen.queryByTestId('contained-roles-picker')).not.toBeInTheDocument();
      fireEvent.click(screen.getByTestId('role-form-open-contained-roles'));
      expect(screen.getByTestId('contained-roles-picker')).toBeInTheDocument();
    });
  });

  describe('save gating', () => {
    it('disables Save when neither privileges nor contained roles are selected', () => {
      renderWithTheme(<RoleForm isCreate onCancel={mockOnCancel} onComplete={mockOnComplete} />);

      const submit = screen.getByTestId('form-submit');
      expect(submit).toBeDisabled();
      expect(screen.getByTestId('privilege-warning')).toBeInTheDocument();
    });

    it('enables Save when at least one privilege is selected', () => {
      mockFormFor(null, { privileges: ['nx-search-read'] }, /* isPristine */ false);
      renderWithTheme(<RoleForm isCreate onCancel={mockOnCancel} onComplete={mockOnComplete} />);

      expect(screen.getByTestId('form-submit')).not.toBeDisabled();
      expect(screen.queryByTestId('privilege-warning')).not.toBeInTheDocument();
    });

    it('enables Save when at least one contained role is selected', () => {
      mockFormFor(null, { roles: ['nx-anonymous'] }, /* isPristine */ false);
      renderWithTheme(<RoleForm isCreate onCancel={mockOnCancel} onComplete={mockOnComplete} />);

      expect(screen.getByTestId('form-submit')).not.toBeDisabled();
    });
  });

  describe('edit mode locking', () => {
    it('disables the ID field in Edit mode', () => {
      mockFormFor(mockRole);
      renderWithTheme(
        <RoleForm isCreate={false} role={mockRole} onCancel={mockOnCancel} onComplete={mockOnComplete} />,
      );

      const idInput = screen.getByLabelText(/^ID/) as HTMLInputElement;
      expect(idInput).toBeDisabled();
    });

    it('disables every input when the role is backend-flagged readOnly', () => {
      const readOnlyRole = { ...mockRole, readOnly: true };
      mockFormFor(readOnlyRole);
      renderWithTheme(
        <RoleForm isCreate={false} role={readOnlyRole} onCancel={mockOnCancel} onComplete={mockOnComplete} />,
      );

      expect(screen.getByLabelText(/^Name/)).toBeDisabled();
      expect(screen.getByLabelText(/^Description/)).toBeDisabled();
      // Picker triggers are also disabled
      expect(screen.getByTestId('role-form-open-privileges')).toBeDisabled();
      expect(screen.getByTestId('role-form-open-contained-roles')).toBeDisabled();
    });
  });

  describe('cancel', () => {
    it('fires onCancel when the Cancel button is clicked', () => {
      renderWithTheme(<RoleForm isCreate onCancel={mockOnCancel} onComplete={mockOnComplete} />);

      const cancelButtons = screen.getAllByRole('button', { name: /^Cancel$/ });
      // First "Cancel" is the form's; picker cancels only mount when open.
      fireEvent.click(cancelButtons[0]);
      expect(mockOnCancel).toHaveBeenCalled();
    });
  });
});
