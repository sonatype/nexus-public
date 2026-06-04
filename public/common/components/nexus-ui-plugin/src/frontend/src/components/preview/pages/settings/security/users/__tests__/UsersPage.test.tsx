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
import { ToastProvider } from '../../../../../shared/Toast';

// Mock the API hook
jest.mock('../useUsersApi');

const mockedUseUsersApi = useUsersApiModule.useUsersApi as jest.MockedFunction<typeof useUsersApiModule.useUsersApi>;

// Mock child components
jest.mock('../UsersList', () => ({
  UsersList: function MockUsersList(props: {
    onSelect: (userId: string, source: string) => void;
    onCreate: () => void;
  }) {
    const { onSelect, onCreate } = props;
    return (
      <div data-testid="users-list">
        <button onClick={() => onSelect('testuser', 'default')}>Select User</button>
        <button onClick={onCreate}>Create User Button</button>
      </div>
    );
  },
}));

jest.mock('../UserDetail', () => ({
  UserDetail: function MockUserDetail({ user, onSave, onDelete, onCancel }: any) {
    return (
      <div data-testid="user-detail">
        <span>Editing: {user?.userId}</span>
        <button onClick={() => onSave({ userId: 'testuser', firstName: 'Test', lastName: 'User', emailAddress: 'test@test.com', status: true, roles: ['nx-admin'] })}>Save</button>
        <button onClick={onDelete}>Delete</button>
        <button onClick={onCancel}>Close</button>
      </div>
    );
  },
}));

jest.mock('../UserForm', () => ({
  UserForm: function MockUserForm({ isCreate, onSave, onCancel, onDelete, onValidationChange }: any) {

    return (
      <div data-testid="user-form">
        <span>{isCreate ? 'Create User Form' : 'Edit User Form'}</span>
        <button onClick={() => onSave({ userId: 'newuser', firstName: 'New', lastName: 'User', emailAddress: 'new@test.com', password: 'password', passwordConfirm: 'password', status: true, roles: ['nx-admin'] })}>Save</button>
        <button onClick={onCancel}>Close</button>
        {onDelete && <button onClick={onDelete}>Delete</button>}
      </div>
    );
  },
}));

// Mock ExtJS
jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtJS: {
    checkPermission: jest.fn().mockReturnValue(true),
    state: jest.fn().mockReturnValue({
      getValue: jest.fn().mockImplementation((key) => {
        if (key === 'user') return { id: 'admin' };
        if (key === 'anonymousUsername') return 'anonymous';
        return undefined;
      }),
      getUser: jest.fn().mockReturnValue({ id: 'admin' }),
    }),
    useStatus: jest.fn().mockReturnValue({ edition: 'PRO' }),
    useUser: jest.fn().mockReturnValue({ authenticated: true }),
    isProEdition: jest.fn().mockReturnValue(true),
  },
}));

import { ExtJS } from '@sonatype/nexus-ui-plugin';

// Wrapper component for Radix Theme and Toast context
function TestWrapper({ children }: { children: React.ReactNode }) {
  return (
    <Theme>
      <ToastProvider>{children}</ToastProvider>
    </Theme>
  );
}

describe('UsersPage', () => {
  const mockFetchUser = jest.fn();
  const mockCreateUser = jest.fn();
  const mockUpdateUser = jest.fn();
  const mockDeleteUser = jest.fn();
  const mockSetError = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    window.location.hash = '';
    mockedUseUsersApi.mockReturnValue({
      loading: false,
      error: null,
      setError: mockSetError,
      fetchUser: mockFetchUser.mockResolvedValue({
        userId: 'testuser',
        firstName: 'Test',
        lastName: 'User',
        emailAddress: 'test@test.com',
        status: 'active',
        roles: ['nx-admin'],
        source: 'default',
        realm: 'default',
      }),
      createUser: mockCreateUser.mockResolvedValue({}),
      updateUser: mockUpdateUser.mockResolvedValue({}),
      deleteUser: mockDeleteUser.mockResolvedValue({}),
      fetchUsers: jest.fn().mockResolvedValue([]),
      fetchSources: jest.fn().mockResolvedValue([]),
      fetchRoles: jest.fn().mockResolvedValue([]),
      changePassword: jest.fn().mockResolvedValue({}),
      resetUserToken: jest.fn().mockResolvedValue({}),
    });
  });

  const triggerHashChange = (hash: string) => {
    act(() => {
      window.location.hash = hash;
      window.dispatchEvent(new HashChangeEvent('hashchange'));
    });
  };

  it('renders the users list by default', () => {
    render(<UsersPage />, { wrapper: TestWrapper });
    
    expect(screen.getByTestId('users-list')).toBeInTheDocument();
    expect(screen.getAllByText('Users').length).toBeGreaterThanOrEqual(1);
  });

  it('shows create user form when Create Local User button is clicked', async () => {
    render(<UsersPage />, { wrapper: TestWrapper });
    
    // Find the button in the header actions
    const createButton = screen.getByRole('button', { name: /Create Local User/i });
    fireEvent.click(createButton);
    
    // In tests, we need to manually trigger hashchange because JSDOM doesn't do it automatically on hash assignment
    triggerHashChange('#preview/admin/security/users/create');
    
    await waitFor(() => {
      expect(screen.getByTestId('user-form')).toBeInTheDocument();
      expect(screen.getByText('Create User Form')).toBeInTheDocument();
    });
  });

  it('navigates to user detail when a user is selected', async () => {
    render(<UsersPage />, { wrapper: TestWrapper });
    
    fireEvent.click(screen.getByText('Select User'));
    
    triggerHashChange('#preview/admin/security/users/testuser/default');
    
    await waitFor(() => {
      // With the wizard, detail view now shows the wizard form (UserForm)
      expect(screen.getByTestId('user-form')).toBeInTheDocument();
      expect(screen.getByText('Edit User Form')).toBeInTheDocument();
    });
  });

  it('returns to list view when close is clicked in create mode', async () => {
    render(<UsersPage />, { wrapper: TestWrapper });
    
    fireEvent.click(screen.getByRole('button', { name: /Create Local User/i }));
    triggerHashChange('#preview/admin/security/users/create');
    
    await waitFor(() => {
      expect(screen.getByTestId('user-form')).toBeInTheDocument();
    });
    
    // Click Cancel in the wizard (rendered by WizardForm)
    const cancelButton = screen.getByRole('button', { name: /cancel/i });
    fireEvent.click(cancelButton);
    triggerHashChange('#preview/admin/security/users');
    
    await waitFor(() => {
      expect(screen.getByTestId('users-list')).toBeInTheDocument();
    });
  });

  it('displays page header with icon and description', () => {
    render(<UsersPage />, { wrapper: TestWrapper });

    expect(screen.getAllByText('Users').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('Manage users and their role assignments')).toBeInTheDocument();
  });

  describe('user deletion from detail view', () => {
    beforeEach(() => {
      window.location.hash = '#preview/admin/security/users/testuser/default';
    });

    it('opens DeleteConfirmationModal with user ID', async () => {
      render(<UsersPage />, { wrapper: TestWrapper });

      // Wait for user to load
      await waitFor(() => {
        expect(mockFetchUser).toHaveBeenCalledWith('testuser', 'default');
      });

      // Trigger hashchange to load detail view
      triggerHashChange('#preview/admin/security/users/testuser/default');

      await waitFor(() => {
        expect(screen.getByTestId('user-form')).toBeInTheDocument();
      });

      // Click delete button
      const deleteButton = screen.getByRole('button', { name: /delete/i });
      fireEvent.click(deleteButton);

      // Verify modal opens with user ID
      await waitFor(() => {
        expect(screen.getByText(/delete user\?/i)).toBeInTheDocument();
        // User ID appears in multiple places (heading and modal), just check modal opened
      });
    });

    it('requires typing exact user ID to confirm deletion', async () => {
      render(<UsersPage />, { wrapper: TestWrapper });

      // Wait for user to load
      await waitFor(() => {
        expect(mockFetchUser).toHaveBeenCalledWith('testuser', 'default');
      });

      triggerHashChange('#preview/admin/security/users/testuser/default');

      await waitFor(() => {
        expect(screen.getByTestId('user-form')).toBeInTheDocument();
      });

      // Open delete modal
      const deleteButton = screen.getByRole('button', { name: /delete/i });
      fireEvent.click(deleteButton);

      // Find the confirmation input and delete button in modal
      const confirmInput = await screen.findByRole('textbox');
      const confirmDeleteButton = screen.getByRole('button', { name: /^delete$/i });

      // Initially, delete button should be disabled
      expect(confirmDeleteButton).toBeDisabled();

      // Type incorrect user ID
      fireEvent.change(confirmInput, { target: { value: 'wronguser' } });
      expect(confirmDeleteButton).toBeDisabled();

      // Type correct user ID
      fireEvent.change(confirmInput, { target: { value: 'testuser' } });
      expect(confirmDeleteButton).not.toBeDisabled();
    });

    it('deletes user after typing user ID and confirming', async () => {
      render(<UsersPage />, { wrapper: TestWrapper });

      // Wait for user to load
      await waitFor(() => {
        expect(mockFetchUser).toHaveBeenCalledWith('testuser', 'default');
      });

      triggerHashChange('#preview/admin/security/users/testuser/default');

      await waitFor(() => {
        expect(screen.getByTestId('user-form')).toBeInTheDocument();
      });

      // Open delete modal
      const deleteButton = screen.getByRole('button', { name: /delete/i });
      fireEvent.click(deleteButton);

      // Type user ID
      const confirmInput = await screen.findByRole('textbox');
      fireEvent.change(confirmInput, { target: { value: 'testuser' } });

      // Click delete
      const confirmDeleteButton = screen.getByRole('button', { name: /^delete$/i });
      await act(async () => {
        fireEvent.click(confirmDeleteButton);
      });

      // Verify deleteUser was called
      await waitFor(() => {
        expect(mockDeleteUser).toHaveBeenCalledWith('testuser');
      });
    });

    it('cancels deletion when Cancel clicked', async () => {
      render(<UsersPage />, { wrapper: TestWrapper });

      // Wait for user to load
      await waitFor(() => {
        expect(mockFetchUser).toHaveBeenCalledWith('testuser', 'default');
      });

      triggerHashChange('#preview/admin/security/users/testuser/default');

      await waitFor(() => {
        expect(screen.getByTestId('user-form')).toBeInTheDocument();
      });

      // Open delete modal
      const deleteButton = screen.getByRole('button', { name: /delete/i });
      fireEvent.click(deleteButton);

      // Verify modal is open
      await waitFor(() => {
        expect(screen.getByText(/delete user\?/i)).toBeInTheDocument();
      });

      // Find all cancel buttons and select the one that's part of the AlertDialog (modal)
      const cancelButtons = screen.getAllByRole('button', { name: /cancel/i });
      // The modal's cancel button is typically the last one rendered
      const modalCancelButton = cancelButtons[cancelButtons.length - 1];
      fireEvent.click(modalCancelButton);

      // Verify modal closed and no deletion occurred
      await waitFor(() => {
        expect(screen.queryByText(/delete user\?/i)).not.toBeInTheDocument();
      });
      expect(mockDeleteUser).not.toHaveBeenCalled();
    });

    it('handles deletion error gracefully', async () => {
      // Mock deletion to fail
      const errorMessage = 'Cannot delete user';
      mockDeleteUser.mockRejectedValueOnce(new Error(errorMessage));

      render(<UsersPage />, { wrapper: TestWrapper });

      // Wait for user to load
      await waitFor(() => {
        expect(mockFetchUser).toHaveBeenCalledWith('testuser', 'default');
      });

      triggerHashChange('#preview/admin/security/users/testuser/default');

      await waitFor(() => {
        expect(screen.getByTestId('user-form')).toBeInTheDocument();
      });

      // Open delete modal
      const deleteButton = screen.getByRole('button', { name: /delete/i });
      fireEvent.click(deleteButton);

      // Type user ID and confirm
      const confirmInput = await screen.findByRole('textbox');
      fireEvent.change(confirmInput, { target: { value: 'testuser' } });
      const confirmDeleteButton = screen.getByRole('button', { name: /^delete$/i });

      await act(async () => {
        fireEvent.click(confirmDeleteButton);
      });

      // Verify error was set
      await waitFor(() => {
        expect(mockSetError).toHaveBeenCalledWith(errorMessage);
      });
    });
  });
});



