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
import '@testing-library/jest-dom';

import { UserForm } from '../UserForm';
import { User } from '../types';
import * as useUsersApiModule from '../useUsersApi';
import * as useUsersFormModule from '../useUsersForm';

// Mock the API hook and form hook
jest.mock('../useUsersApi');
jest.mock('../useUsersForm');
jest.mock('../../roles/useRoleTree', () => ({
  useRoleTree: jest.fn(() => ({
    tree: [],
    loading: false,
    toggleExpand: jest.fn(),
    expandAll: jest.fn(),
    collapseAll: jest.fn(),
  })),
  useCombinedRoleTree: jest.fn(() => ({
    tree: [],
    loading: false,
    toggleExpand: jest.fn(),
    expandAll: jest.fn(),
    collapseAll: jest.fn(),
  })),
}));
jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtJS: {
    isProEdition: jest.fn().mockReturnValue(true),
    checkPermission: jest.fn().mockReturnValue(true),
    state: jest.fn().mockReturnValue({
      getValue: jest.fn().mockImplementation((key) => {
        if (key === 'anonymousUsername') return 'anonymous';
        return null;
      }),
      getUser: jest.fn().mockReturnValue({ id: 'admin' }),
    }),
  },
}));

import { ExtJS } from '@sonatype/nexus-ui-plugin';

const mockedUseUsersApi = useUsersApiModule.useUsersApi as jest.MockedFunction<typeof useUsersApiModule.useUsersApi>;
const mockedUseUsersForm = useUsersFormModule.useUsersForm as jest.MockedFunction<typeof useUsersFormModule.useUsersForm>;

/**
 * Creates a mock form object that mimics the useForm return shape.
 * The field() and checkbox() helpers return props that map to rendered inputs.
 */
function createMockForm(data: Record<string, any>, context: Record<string, any> = {}) {
  return {
    field: jest.fn((name: string) => ({
      name,
      value: data[name] != null ? String(data[name]) : '',
      error: undefined,
      onChange: jest.fn(),
      onBlur: jest.fn(),
    })),
    checkbox: jest.fn((name: string) => ({
      name,
      checked: Boolean(data[name]),
      error: undefined,
      onChange: jest.fn(),
    })),
    submit: jest.fn(),
    reset: jest.fn(),
    isPristine: true,
    isSaving: false,
    isLoading: false,
    hasLoadError: false,
    hasValidationErrors: false,
    data,
    touched: {} as Record<string, boolean>,
    validationErrors: {} as Record<string, string>,
    saveError: null as string | null,
    loadError: null as string | null,
    state: { context: { allRoles: [], userSources: [], user: null, ...context }, matches: jest.fn(() => false) },
    send: jest.fn(),
  };
}

// Wrapper component for Radix Theme
function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

const mockRoles = [
  { id: 'nx-admin', name: 'Admin' },
  { id: 'nx-anonymous', name: 'Anonymous' },
];

describe('UserForm', () => {
  const mockOnSave = jest.fn();
  const mockOnCancel = jest.fn();
  const mockOnDelete = jest.fn();
  const mockFetchRoles = jest.fn();
  const mockFetchSources = jest.fn();

  const mockUserSources = [
    { id: 'LDAP', name: 'LDAP Server' },
    { id: 'SAML', name: 'SAML Provider' },
  ];

  beforeEach(() => {
    jest.clearAllMocks();
    mockFetchRoles.mockResolvedValue(mockRoles);
    mockFetchSources.mockResolvedValue(mockUserSources);
    mockedUseUsersApi.mockReturnValue({
      loading: false,
      error: null,
      setError: jest.fn(),
      fetchUser: jest.fn(),
      fetchUsers: jest.fn(),
      fetchSources: mockFetchSources,
      fetchRoles: mockFetchRoles,
      createUser: jest.fn(),
      updateUser: jest.fn(),
      deleteUser: jest.fn(),
      changePassword: jest.fn(),
      resetUserToken: jest.fn(),
    });
    // Smart mock for useUsersForm - returns create or edit mode based on args
    mockedUseUsersForm.mockImplementation(({ user, userId } = {} as any) => {
      const isCreate = !userId && !user;
      const contextData = { allRoles: mockRoles, userSources: [{ id: 'default', name: 'Local' }, ...mockUserSources] };
      if (isCreate) {
        return {
          form: createMockForm(
            { userId: '', firstName: '', lastName: '', emailAddress: '', password: '', passwordConfirm: '', status: true, roles: [] as string[], source: 'default' },
            { ...contextData, user: null }
          ) as any,
          user: null,
          isCreate: true,
        };
      }
      // Edit mode - use the provided user data
      const u = user;
      return {
        form: createMockForm(
          {
            userId: u?.userId || '',
            firstName: u?.firstName || '',
            lastName: u?.lastName || '',
            emailAddress: u?.emailAddress || u?.email || '',
            password: '',
            passwordConfirm: '',
            status: u?.status === 'active',
            roles: u?.roles || [],
            source: u?.source || 'default',
          },
          { ...contextData, user: u }
        ) as any,
        user: u,
        isCreate: false,
      };
    });
  });

  it('renders Step 1 (Setup) content by default', async () => {
    render(
      <UserForm isCreate={true} onSave={mockOnSave} onCancel={mockOnCancel} wizardStep={0} hideActions={true} />,
      { wrapper: TestWrapper }
    );
    
    await waitFor(() => {
      expect(screen.getByText('User Setup')).toBeInTheDocument();
    });
    
    expect(screen.getByTestId('input-userId')).toBeInTheDocument();
    expect(screen.getByTestId('input-firstName')).toBeInTheDocument();
  });

  it('renders Step 2 (Roles) content when wizardStep is 1', async () => {
    render(
      <UserForm isCreate={true} onSave={mockOnSave} onCancel={mockOnCancel} wizardStep={1} hideActions={true} />,
      { wrapper: TestWrapper }
    );
    
    await waitFor(() => {
      expect(screen.getByText('Roles')).toBeInTheDocument();
    });
    
    expect(screen.getByText('Available')).toBeInTheDocument();
    expect(screen.getByText('Granted')).toBeInTheDocument();
    expect(screen.getByText('Role Inspector')).toBeInTheDocument();
    expect(screen.getByText(/Select roles from Available or Granted to see combined permissions/)).toBeInTheDocument();
  });

  it('shows password change button in edit mode', async () => {
    const user: User = {
      userId: 'testuser',
      realm: 'default',
      firstName: 'Test',
      lastName: 'User',
      emailAddress: 'test@example.com',
      source: 'default',
      status: 'active',
      roles: ['nx-admin'],
    };

    render(
      <UserForm isCreate={false} user={user} onSave={mockOnSave} onCancel={mockOnCancel} wizardStep={0} hideActions={true} />,
      { wrapper: TestWrapper }
    );
    
    await waitFor(() => {
      expect(screen.getByTestId('change-password-btn')).toBeInTheDocument();
    });
  });

  it('shows reset token button in edit mode for non-external users', async () => {
    const user: User = {
      userId: 'testuser',
      realm: 'default',
      firstName: 'Test',
      lastName: 'User',
      emailAddress: 'test@example.com',
      source: 'default',
      status: 'active',
      roles: ['nx-admin'],
    };

    render(
      <UserForm isCreate={false} user={user} onSave={mockOnSave} onCancel={mockOnCancel} wizardStep={0} hideActions={true} />,
      { wrapper: TestWrapper }
    );
    
    await waitFor(() => {
      expect(screen.getByTestId('reset-token-btn')).toBeInTheDocument();
    });
  });
});
