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

import { UserDetail } from '../UserDetail';
import { User } from '../types';
import * as useUsersApiModule from '../useUsersApi';
import { ToastProvider } from '../../../../../shared/Toast';

// Mock the API hook
jest.mock('../useUsersApi', () => ({
  useUsersApi: jest.fn(),
}));

const mockedUseUsersApi = useUsersApiModule.useUsersApi as jest.MockedFunction<typeof useUsersApiModule.useUsersApi>;

// Mock ExtJS
jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtJS: {
    checkPermission: jest.fn().mockReturnValue(true),
    isProEdition: jest.fn().mockReturnValue(true),
    state: jest.fn().mockReturnValue({
      getValue: jest.fn((key: string) => {
        if (key === 'capabilityActiveTypes') {
          return ['usertoken'];
        }
        return 'anonymous';
      }),
      getUser: jest.fn().mockReturnValue({ id: 'currentuser' }),
    }),
  },
  APIConstants: {
    EXT: {
      USER: {
        ACTION: 'coreui_User',
        METHODS: {
          READ: 'read',
          CREATE: 'create',
          UPDATE: 'update',
          REMOVE: 'remove',
          CHANGE_PASSWORD: 'changePassword',
          READ_SOURCES: 'readSources',
          READ_ROLES: 'readRoles',
        },
      },
    },
    REST: {
      PUBLIC: {
        USERS: '/service/rest/v1/security/users',
      },
    },
  },
}));

// Mock child components
jest.mock('../UserForm', () => ({
  UserForm: function MockUserForm({ user, onSave, onCancel, onDelete }: any) {
    return (
      <div data-testid="user-form">
        <span>Editing: {user?.userId}</span>
        <button onClick={() => onSave({ userId: user?.userId, firstName: 'Updated', lastName: 'User', emailAddress: 'updated@test.com', status: true, roles: ['nx-admin'] })}>Save Form</button>
        <button onClick={onCancel}>Cancel Form</button>
        {onDelete && <button onClick={onDelete}>Delete Form</button>}
      </div>
    );
  },
}));

// Wrapper component for Radix Theme and Toast context
function TestWrapper({ children }: { children: React.ReactNode }) {
  return (
    <Theme>
      <ToastProvider>{children}</ToastProvider>
    </Theme>
  );
}

const mockUser: User = {
  userId: 'testuser',
  firstName: 'Test',
  lastName: 'User',
  emailAddress: 'test@example.com',
  source: 'default',
  realm: 'default',
  status: 'active',
  roles: ['nx-admin'],
};

const mockExternalUser: User = {
  userId: 'ldapuser',
  firstName: 'LDAP',
  lastName: 'User',
  emailAddress: 'ldap@example.com',
  source: 'LDAP',
  realm: 'LDAP',
  status: 'active',
  roles: ['nx-anonymous'],
  externalRoles: ['LDAP-Admin', 'LDAP-Users'],
};

describe('UserDetail', () => {
  const mockOnSave = jest.fn();
  const mockOnDelete = jest.fn();
  const mockOnCancel = jest.fn();
  const mockChangePassword = jest.fn();
  const mockResetUserToken = jest.fn();
  const mockSetError = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockChangePassword.mockResolvedValue({});
    mockResetUserToken.mockResolvedValue({});
    mockedUseUsersApi.mockReturnValue({
      loading: false,
      error: null,
      setError: mockSetError,
      fetchUser: jest.fn(),
      fetchUsers: jest.fn(),
      fetchSources: jest.fn(),
      fetchRoles: jest.fn().mockResolvedValue([]),
      createUser: jest.fn(),
      updateUser: jest.fn(),
      deleteUser: jest.fn(),
      changePassword: mockChangePassword,
      resetUserToken: mockResetUserToken,
    });
  });

  it('shows loading state when user is null and loading is true', () => {
    render(
      <UserDetail
        user={null}
        loading={true}
        canEdit={true}
        canDelete={true}
        onSave={mockOnSave}
        onDelete={mockOnDelete}
        onCancel={mockOnCancel}
      />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText(/Loading user details/i)).toBeInTheDocument();
  });

  it('shows not found state when user is null and not loading', () => {
    render(
      <UserDetail
        user={null}
        loading={false}
        canEdit={true}
        canDelete={true}
        onSave={mockOnSave}
        onDelete={mockOnDelete}
        onCancel={mockOnCancel}
      />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText(/User not found/i)).toBeInTheDocument();
  });

  it('renders read-only view when canEdit is false', async () => {
    render(
      <UserDetail
        user={mockUser}
        loading={false}
        canEdit={false}
        canDelete={false}
        onSave={mockOnSave}
        onDelete={mockOnDelete}
        onCancel={mockOnCancel}
      />,
      { wrapper: TestWrapper }
    );

    await waitFor(() => {
      expect(screen.getByText('User Information')).toBeInTheDocument();
    });

    expect(screen.getByText('testuser')).toBeInTheDocument();
    expect(screen.getByText('Test')).toBeInTheDocument();
    expect(screen.getByText('User')).toBeInTheDocument();
    expect(screen.getByText('test@example.com')).toBeInTheDocument();
    expect(screen.getByText('Back to List')).toBeInTheDocument();
  });

  it('shows assigned roles in read-only view', async () => {
    render(
      <UserDetail
        user={mockUser}
        loading={false}
        canEdit={false}
        canDelete={false}
        onSave={mockOnSave}
        onDelete={mockOnDelete}
        onCancel={mockOnCancel}
      />,
      { wrapper: TestWrapper }
    );

    await waitFor(() => {
      expect(screen.getByText('Assigned Roles')).toBeInTheDocument();
    });

    expect(screen.getByText('nx-admin')).toBeInTheDocument();
  });

  it('renders edit form when canEdit is true', async () => {
    render(
      <UserDetail
        user={mockUser}
        loading={false}
        canEdit={true}
        canDelete={true}
        onSave={mockOnSave}
        onDelete={mockOnDelete}
        onCancel={mockOnCancel}
      />,
      { wrapper: TestWrapper }
    );

    await waitFor(() => {
      expect(screen.getByTestId('user-form')).toBeInTheDocument();
    });

    expect(screen.getByText('Editing: testuser')).toBeInTheDocument();
  });

  it('shows change password section for local users with admin permission', async () => {
    render(
      <UserDetail
        user={mockUser}
        loading={false}
        canEdit={true}
        canDelete={true}
        onSave={mockOnSave}
        onDelete={mockOnDelete}
        onCancel={mockOnCancel}
      />,
      { wrapper: TestWrapper }
    );

    await waitFor(() => {
      expect(screen.getByText('Account Actions')).toBeInTheDocument();
    });

    expect(screen.getByRole('button', { name: /Change Password/i })).toBeInTheDocument();
  });

  it('shows user token reset section in Pro edition', async () => {
    render(
      <UserDetail
        user={mockUser}
        loading={false}
        canEdit={true}
        canDelete={true}
        onSave={mockOnSave}
        onDelete={mockOnDelete}
        onCancel={mockOnCancel}
      />,
      { wrapper: TestWrapper }
    );

    await waitFor(() => {
      expect(screen.getByText('Account Actions')).toBeInTheDocument();
    });

    expect(screen.getByText('Reset Token')).toBeInTheDocument();
  });

  it('handles change password flow', async () => {
    render(
      <UserDetail
        user={mockUser}
        loading={false}
        canEdit={true}
        canDelete={true}
        onSave={mockOnSave}
        onDelete={mockOnDelete}
        onCancel={mockOnCancel}
      />,
      { wrapper: TestWrapper }
    );

    // Click to show password form
    fireEvent.click(screen.getByRole('button', { name: /Change Password/i }));

    const newPasswordInput = await screen.findByLabelText(/^New Password/i);
    const confirmPasswordInput = screen.getByLabelText(/^Confirm New Password/i);

    fireEvent.change(newPasswordInput, { target: { value: 'newpassword123' } });
    fireEvent.change(confirmPasswordInput, { target: { value: 'newpassword123' } });

    // Submit the password change
    fireEvent.click(screen.getByRole('button', { name: 'Change Password' }));

    await waitFor(() => {
      expect(mockChangePassword).toHaveBeenCalledWith('testuser', 'newpassword123');
    });
  });

  it('shows password mismatch error', async () => {
    render(
      <UserDetail
        user={mockUser}
        loading={false}
        canEdit={true}
        canDelete={true}
        onSave={mockOnSave}
        onDelete={mockOnDelete}
        onCancel={mockOnCancel}
      />,
      { wrapper: TestWrapper }
    );

    // Click to show password form
    fireEvent.click(screen.getByRole('button', { name: /Change Password/i }));

    const newPasswordInput = await screen.findByLabelText(/^New Password/i);
    const confirmPasswordInput = screen.getByLabelText(/^Confirm New Password/i);

    fireEvent.change(newPasswordInput, { target: { value: 'password1' } });
    fireEvent.change(confirmPasswordInput, { target: { value: 'password2' } });

    // Submit
    fireEvent.click(screen.getByRole('button', { name: 'Change Password' }));

    await waitFor(() => {
      expect(screen.getByText(/Passwords do not match/i)).toBeInTheDocument();
    });
  });

  it('calls onCancel when back button is clicked in read-only mode', async () => {
    render(
      <UserDetail
        user={mockUser}
        loading={false}
        canEdit={false}
        canDelete={false}
        onSave={mockOnSave}
        onDelete={mockOnDelete}
        onCancel={mockOnCancel}
      />,
      { wrapper: TestWrapper }
    );

    await waitFor(() => {
      expect(screen.getByText('Back to List')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('Back to List'));

    expect(mockOnCancel).toHaveBeenCalled();
  });

  it('displays user status', async () => {
    render(
      <UserDetail
        user={mockUser}
        loading={false}
        canEdit={false}
        canDelete={false}
        onSave={mockOnSave}
        onDelete={mockOnDelete}
        onCancel={mockOnCancel}
      />,
      { wrapper: TestWrapper }
    );

    await waitFor(() => {
      expect(screen.getByText('active')).toBeInTheDocument();
    });
  });

  it('shows Local for local user source', async () => {
    render(
      <UserDetail
        user={mockUser}
        loading={false}
        canEdit={false}
        canDelete={false}
        onSave={mockOnSave}
        onDelete={mockOnDelete}
        onCancel={mockOnCancel}
      />,
      { wrapper: TestWrapper }
    );

    await waitFor(() => {
      expect(screen.getByText('Local')).toBeInTheDocument();
    });
  });

  it('handles API errors gracefully', async () => {
    mockedUseUsersApi.mockReturnValue({
      loading: false,
      error: 'Something went wrong',
      setError: mockSetError,
      fetchUser: jest.fn(),
      fetchUsers: jest.fn(),
      fetchSources: jest.fn(),
      fetchRoles: jest.fn().mockResolvedValue([]),
      createUser: jest.fn(),
      updateUser: jest.fn(),
      deleteUser: jest.fn(),
      changePassword: mockChangePassword,
      resetUserToken: mockResetUserToken,
    });

    render(
      <UserDetail
        user={mockUser}
        loading={false}
        canEdit={true}
        canDelete={true}
        onSave={mockOnSave}
        onDelete={mockOnDelete}
        onCancel={mockOnCancel}
      />,
      { wrapper: TestWrapper }
    );

    await waitFor(() => {
      expect(screen.getByText('Something went wrong')).toBeInTheDocument();
    });
  });

  it('shows no roles message when user has no roles assigned', async () => {
    const userWithNoRoles = { ...mockUser, roles: [] };
    
    render(
      <UserDetail
        user={userWithNoRoles}
        loading={false}
        canEdit={false}
        canDelete={false}
        onSave={mockOnSave}
        onDelete={mockOnDelete}
        onCancel={mockOnCancel}
      />,
      { wrapper: TestWrapper }
    );

    await waitFor(() => {
      expect(screen.getByText('No roles assigned')).toBeInTheDocument();
    });
  });

  it('can cancel password change form', async () => {
    render(
      <UserDetail
        user={mockUser}
        loading={false}
        canEdit={true}
        canDelete={true}
        onSave={mockOnSave}
        onDelete={mockOnDelete}
        onCancel={mockOnCancel}
      />,
      { wrapper: TestWrapper }
    );

    // Click to show password form
    fireEvent.click(screen.getByRole('button', { name: /Change Password/i }));

    await screen.findByLabelText(/^New Password/i);

    // Click cancel
    fireEvent.click(screen.getByText('Cancel'));

    await waitFor(() => {
      // Password form should be hidden and button visible again
      expect(screen.getByRole('button', { name: /Change Password/i })).toBeInTheDocument();
    });
  });

  it('shows confirmation dialog when reset token is clicked', async () => {
    render(
      <UserDetail
        user={mockUser}
        loading={false}
        canEdit={true}
        canDelete={true}
        onSave={mockOnSave}
        onDelete={mockOnDelete}
        onCancel={mockOnCancel}
      />,
      { wrapper: TestWrapper }
    );

    // Click reset token button
    fireEvent.click(screen.getByText('Reset Token'));

    // Verify confirmation dialog appears
    await waitFor(() => {
      expect(screen.getByRole('alertdialog')).toBeInTheDocument();
      expect(screen.getByText(/Are you sure you want to reset the token/i)).toBeInTheDocument();
    });
  });

  it('calls resetUserToken when confirmation dialog is confirmed', async () => {
    render(
      <UserDetail
        user={mockUser}
        loading={false}
        canEdit={true}
        canDelete={true}
        onSave={mockOnSave}
        onDelete={mockOnDelete}
        onCancel={mockOnCancel}
      />,
      { wrapper: TestWrapper }
    );

    // Click reset token button
    fireEvent.click(screen.getByText('Reset Token'));

    // Wait for dialog and click confirm
    await waitFor(() => {
      expect(screen.getByRole('alertdialog')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: 'Reset' }));

    await waitFor(() => {
      expect(mockResetUserToken).toHaveBeenCalledWith('testuser', 'default');
    });
  });

  it('closes confirmation dialog when cancel is clicked', async () => {
    render(
      <UserDetail
        user={mockUser}
        loading={false}
        canEdit={true}
        canDelete={true}
        onSave={mockOnSave}
        onDelete={mockOnDelete}
        onCancel={mockOnCancel}
      />,
      { wrapper: TestWrapper }
    );

    // Click reset token button
    fireEvent.click(screen.getByText('Reset Token'));

    // Wait for dialog
    await waitFor(() => {
      expect(screen.getByRole('alertdialog')).toBeInTheDocument();
    });

    // Click cancel in the dialog
    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));

    // Dialog should close
    await waitFor(() => {
      expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
    });

    // resetUserToken should not have been called
    expect(mockResetUserToken).not.toHaveBeenCalled();
  });

  it('hides reset token button when usertoken capability is not active', async () => {
    // Override the mock to return no usertoken capability
    const { ExtJS } = jest.requireMock('@sonatype/nexus-ui-plugin');
    ExtJS.state.mockReturnValue({
      getValue: jest.fn((key: string) => {
        if (key === 'capabilityActiveTypes') {
          return []; // No usertoken capability
        }
        return 'anonymous';
      }),
      getUser: jest.fn().mockReturnValue({ id: 'currentuser' }),
    });

    render(
      <UserDetail
        user={mockUser}
        loading={false}
        canEdit={true}
        canDelete={true}
        onSave={mockOnSave}
        onDelete={mockOnDelete}
        onCancel={mockOnCancel}
      />,
      { wrapper: TestWrapper }
    );

    await waitFor(() => {
      expect(screen.getByTestId('user-form')).toBeInTheDocument();
    });

    // Reset Token button should NOT be visible
    expect(screen.queryByText('Reset Token')).not.toBeInTheDocument();
  });

  it('hides reset token button when user lacks usertoken-user:delete permission', async () => {
    // Override the mock to return false for usertoken permission
    const { ExtJS } = jest.requireMock('@sonatype/nexus-ui-plugin');
    ExtJS.checkPermission.mockImplementation((permission: string) => {
      if (permission === 'nexus:usertoken-user:delete') {
        return false;
      }
      return true;
    });

    render(
      <UserDetail
        user={mockUser}
        loading={false}
        canEdit={true}
        canDelete={true}
        onSave={mockOnSave}
        onDelete={mockOnDelete}
        onCancel={mockOnCancel}
      />,
      { wrapper: TestWrapper }
    );

    await waitFor(() => {
      expect(screen.getByTestId('user-form')).toBeInTheDocument();
    });

    // Reset Token button should NOT be visible
    expect(screen.queryByText('Reset Token')).not.toBeInTheDocument();
  });
});

