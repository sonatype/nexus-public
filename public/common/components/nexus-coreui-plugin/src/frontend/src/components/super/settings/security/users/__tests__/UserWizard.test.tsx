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
import '@testing-library/jest-dom';

import { UsersPage } from '../UsersPage';
import * as useUsersApiModule from '../useUsersApi';
import * as useUsersFormModule from '../useUsersForm';
import { ToastProvider } from '@/components/shared/Toast';

// Mock the API hook
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

// Wrapper component for Radix Theme and Toast context
function TestWrapper({ children }: { children: React.ReactNode }) {
  return (
    <Theme>
      <ToastProvider>{children}</ToastProvider>
    </Theme>
  );
}

const mockRoles = [
  { id: 'nx-admin', name: 'Admin' },
  { id: 'nx-anonymous', name: 'Anonymous' },
];

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
    state: { context: { allRoles: mockRoles, userSources: [{ id: 'default', name: 'Local' }], user: null, ...context }, matches: jest.fn(() => false) },
    send: jest.fn(),
  };
}

describe('UsersPage Wizard Flow', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    window.location.hash = '';
    
    mockedUseUsersApi.mockReturnValue({
      loading: false,
      error: null,
      setError: jest.fn(),
      fetchUser: jest.fn().mockResolvedValue({
        userId: 'testuser',
        firstName: 'Test',
        lastName: 'User',
        emailAddress: 'test@test.com',
        status: 'active',
        roles: ['nx-admin'],
        source: 'default',
        realm: 'default',
      }),
      fetchUsers: jest.fn().mockResolvedValue([]),
      fetchSources: jest.fn().mockResolvedValue([]),
      fetchRoles: jest.fn().mockResolvedValue(mockRoles),
      createUser: jest.fn().mockResolvedValue({}),
      updateUser: jest.fn().mockResolvedValue({}),
      deleteUser: jest.fn().mockResolvedValue({}),
      changePassword: jest.fn().mockResolvedValue({}),
      resetUserToken: jest.fn().mockResolvedValue({}),
    });

    (ExtJS.isProEdition as jest.Mock).mockReturnValue(true);
    (ExtJS.checkPermission as jest.Mock).mockReturnValue(true);
    (ExtJS.state as jest.Mock).mockReturnValue({
      getValue: jest.fn().mockImplementation((key) => {
        if (key === 'anonymousUsername') return 'anonymous';
        return null;
      }),
      getUser: jest.fn().mockReturnValue({ id: 'admin' }),
    });

    mockedUseUsersForm.mockImplementation(() => ({
      form: createMockForm({ userId: '', firstName: '', lastName: '', emailAddress: '', password: '', passwordConfirm: '', status: true, roles: [], source: 'default' }) as any,
      user: null,
      isCreate: true,
    }));
  });

  const triggerHashChange = (hash: string) => {
    act(() => {
      window.location.hash = hash;
      window.dispatchEvent(new HashChangeEvent('hashchange'));
    });
  };

  it('renders Step 1 (Setup) when Create User is clicked', async () => {
    render(<UsersPage />, { wrapper: TestWrapper });
    
    fireEvent.click(screen.getByRole('button', { name: /Create Local User/i }));
    triggerHashChange('#preview/admin/security/users/create');
    
    await waitFor(() => {
      expect(screen.getByText('Step 1: Setup user details')).toBeInTheDocument();
      expect(screen.getByLabelText(/ID/i)).toBeInTheDocument();
    });
  });

  it('navigates to Step 2 (Roles) when Next is clicked and Step 1 is valid', async () => {
    // Mock Step 1 as valid (including passwords for create mode)
    mockedUseUsersForm.mockImplementation(() => ({
      form: {
        ...createMockForm({ 
          userId: 'newuser', 
          firstName: 'New', 
          lastName: 'User', 
          emailAddress: 'new@test.com', 
          password: 'password123',
          passwordConfirm: 'password123',
          roles: [], 
          source: 'default' 
        }),
        isLoading: false,
        isSaving: false,
      } as any,
      user: null,
      isCreate: true,
    }));

    render(<UsersPage />, { wrapper: TestWrapper });
    
    fireEvent.click(screen.getByRole('button', { name: /Create Local User/i }));
    triggerHashChange('#preview/admin/security/users/create');
    
    // In tests, we might need to wait for the validation effect to run
    // Let's manually trigger it by ensuring the mock calls onValidationChange if possible,
    // but here we just need to wait for the next render cycle.
    
    await waitFor(() => {
      const nextButton = screen.getByRole('button', { name: /next: roles/i });
      expect(nextButton).not.toBeDisabled();
    }, { timeout: 3000 });

    fireEvent.click(screen.getByRole('button', { name: /next: roles/i }));

    await waitFor(() => {
      expect(screen.getByText('Step 2: Assign roles')).toBeInTheDocument();
      expect(screen.getByText('Available')).toBeInTheDocument();
      expect(screen.getByText('Granted')).toBeInTheDocument();
    });
  });

  it('disables Next button if Step 1 is invalid', async () => {
    // Mock Step 1 as invalid (missing ID)
    mockedUseUsersForm.mockImplementation(() => ({
      form: createMockForm({ userId: '', firstName: '', lastName: '', emailAddress: '', roles: [], source: 'default' }) as any,
      user: null,
      isCreate: true,
    }));

    render(<UsersPage />, { wrapper: TestWrapper });
    
    fireEvent.click(screen.getByRole('button', { name: /Create Local User/i }));
    triggerHashChange('#preview/admin/security/users/create');
    
    await waitFor(() => {
      const nextButton = screen.getByRole('button', { name: /next: roles/i });
      expect(nextButton).toBeDisabled();
    });
  });

  it('allows free navigation in edit mode', async () => {
    const user = {
      userId: 'testuser',
      firstName: 'Test',
      lastName: 'User',
      emailAddress: 'test@test.com',
      status: 'active' as const,
      roles: ['nx-admin'],
      source: 'default',
      realm: 'default',
    };

    mockedUseUsersApi.mockReturnValue({
      ...mockedUseUsersApi(),
      fetchUser: jest.fn().mockResolvedValue(user),
    });

    mockedUseUsersForm.mockImplementation(() => ({
      form: createMockForm({ ...user, status: true }) as any,
      user: user as any,
      isCreate: false,
    }));

    // Set URL to detail view
    triggerHashChange('#preview/admin/security/users/testuser/default');
    
    render(<UsersPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Step 1: Edit user details')).toBeInTheDocument();
    });

    // In edit mode, Next should be enabled if pre-populated data is valid
    const nextButton = screen.getByRole('button', { name: /next: roles/i });
    expect(nextButton).not.toBeDisabled();

    fireEvent.click(nextButton);

    await waitFor(() => {
      expect(screen.getByText('Step 2: Manage roles')).toBeInTheDocument();
    });

    // Should be able to go back
    const backButton = screen.getByTestId('wizard-back');
    fireEvent.click(backButton);

    await waitFor(() => {
      expect(screen.getByText('Step 1: Edit user details')).toBeInTheDocument();
    });
  });

  it('hides delete button for "admin" user', async () => {
    const adminUser = {
      userId: 'admin',
      firstName: 'Administrator',
      lastName: '',
      emailAddress: 'admin@example.com',
      status: 'active' as const,
      roles: ['nx-admin'],
      source: 'default',
      realm: 'default',
    };

    mockedUseUsersApi.mockReturnValue({
      ...mockedUseUsersApi(),
      fetchUser: jest.fn().mockResolvedValue(adminUser),
    });

    mockedUseUsersForm.mockImplementation(() => ({
      form: createMockForm({ ...adminUser, status: true }) as any,
      user: adminUser as any,
      isCreate: false,
    }));

    triggerHashChange('#preview/admin/security/users/admin/default');
    
    render(<UsersPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText(/Edit Administrator/i)).toBeInTheDocument();
    });

    // Delete button should NOT be in the document for admin user
    expect(screen.queryByRole('button', { name: /delete user/i })).not.toBeInTheDocument();
  });
});
