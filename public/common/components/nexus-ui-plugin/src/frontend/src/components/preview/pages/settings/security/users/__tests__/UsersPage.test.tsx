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

// Mock child components. `lastPropsFor` captures the most recent props each
// child rendered with, so tests can assert on the prop shape UsersPage plumbs
// down (NEXUS-54437: onDelete/canDelete must no longer reach UsersList or
// UserDetail, and must reach UserForm only when the delete permission is on).
const lastPropsFor: Record<string, Record<string, any>> = {
  UsersList: {},
  UserDetail: {},
  UserForm: {},
};

jest.mock('../UsersList', () => ({
  UsersList: function MockUsersList(props: any) {
    lastPropsFor.UsersList = props;
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
  UserDetail: function MockUserDetail(props: any) {
    lastPropsFor.UserDetail = props;
    const { user, onSave, onCancel } = props;
    return (
      <div data-testid="user-detail">
        <span>Editing: {user?.userId}</span>
        <button onClick={() => onSave({ userId: 'testuser', firstName: 'Test', lastName: 'User', emailAddress: 'test@test.com', status: true, roles: ['nx-admin'] })}>Save</button>
        <button onClick={onCancel}>Close</button>
      </div>
    );
  },
}));

jest.mock('../UserForm', () => ({
  UserForm: function MockUserForm(props: any) {
    lastPropsFor.UserForm = props;
    return <div data-testid="user-form-inner" />;
  },
}));

jest.mock('../EditUserView', () => ({
  EditUserView: function MockEditUserView(props: any) {
    lastPropsFor.UserForm = props;
    const {
      isCreate,
      onSuccess,
      onCancel,
      onDeleteRequest,
      canDelete,
      protectionReason,
      isDeleting,
    } = props;
    return (
      <div data-testid="user-form">
        <span>{isCreate ? 'Create User Form' : 'Edit User Form'}</span>
        <button
          onClick={() =>
            onSuccess({
              userId: 'newuser',
              firstName: 'New',
              lastName: 'User',
              emailAddress: 'new@test.com',
              password: 'password',
              passwordConfirm: 'password',
              status: true,
              roles: ['nx-admin'],
            })
          }
        >
          Save
        </button>
        <button onClick={onCancel}>Cancel</button>
        {canDelete && !isCreate && (
          <button
            data-testid="page-delete-user"
            aria-label={
              protectionReason
                ? `Delete User (${protectionReason})`
                : 'Delete User'
            }
            title={protectionReason ?? undefined}
            disabled={protectionReason !== null || isDeleting}
            onClick={onDeleteRequest}
          >
            Delete User
          </button>
        )}
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

    it('opens DeleteConfirmationModal when the page-level Delete User button is clicked', async () => {
      render(<UsersPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(mockFetchUser).toHaveBeenCalledWith('testuser', 'default');
      });

      triggerHashChange('#preview/admin/security/users/testuser/default');

      const pageDelete = await screen.findByTestId('page-delete-user');
      fireEvent.click(pageDelete);

      await waitFor(() => {
        expect(screen.getByText(/delete user\?/i)).toBeInTheDocument();
      });
    });

    it('requires typing "Delete" to confirm deletion', async () => {
      render(<UsersPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(mockFetchUser).toHaveBeenCalledWith('testuser', 'default');
      });

      triggerHashChange('#preview/admin/security/users/testuser/default');

      const pageDelete = await screen.findByTestId('page-delete-user');
      fireEvent.click(pageDelete);

      const confirmInput = await screen.findByRole('textbox');
      const confirmDeleteButton = screen.getByRole('button', { name: /^delete$/i });

      expect(confirmDeleteButton).toBeDisabled();

      fireEvent.change(confirmInput, { target: { value: 'wronguser' } });
      expect(confirmDeleteButton).toBeDisabled();

      // Acknowledgement is the literal "Delete" (case-insensitive) — NEXUS-53356.
      fireEvent.change(confirmInput, { target: { value: 'Delete' } });
      expect(confirmDeleteButton).not.toBeDisabled();
    });

    it('deletes user after typing user ID and confirming', async () => {
      render(<UsersPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(mockFetchUser).toHaveBeenCalledWith('testuser', 'default');
      });

      triggerHashChange('#preview/admin/security/users/testuser/default');

      const pageDelete = await screen.findByTestId('page-delete-user');
      fireEvent.click(pageDelete);

      const confirmInput = await screen.findByRole('textbox');
      // Acknowledgement is the literal "Delete" (case-insensitive) — NEXUS-53356.
      fireEvent.change(confirmInput, { target: { value: 'Delete' } });

      const confirmDeleteButton = screen.getByRole('button', { name: /^delete$/i });
      await act(async () => {
        fireEvent.click(confirmDeleteButton);
      });

      await waitFor(() => {
        expect(mockDeleteUser).toHaveBeenCalledWith('testuser');
      });
    });

    it('cancels deletion when Cancel clicked', async () => {
      render(<UsersPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(mockFetchUser).toHaveBeenCalledWith('testuser', 'default');
      });

      triggerHashChange('#preview/admin/security/users/testuser/default');

      const pageDelete = await screen.findByTestId('page-delete-user');
      fireEvent.click(pageDelete);

      await waitFor(() => {
        expect(screen.getByText(/delete user\?/i)).toBeInTheDocument();
      });

      const cancelButtons = screen.getAllByRole('button', { name: /cancel/i });
      // The modal's cancel button is typically the last one rendered.
      const modalCancelButton = cancelButtons[cancelButtons.length - 1];
      fireEvent.click(modalCancelButton);

      await waitFor(() => {
        expect(screen.queryByText(/delete user\?/i)).not.toBeInTheDocument();
      });
      expect(mockDeleteUser).not.toHaveBeenCalled();
    });

    it('never passes onDelete or canDelete to UsersList (NEXUS-54437)', () => {
      window.location.hash = '';
      render(<UsersPage />, { wrapper: TestWrapper });

      expect(screen.getByTestId('users-list')).toBeInTheDocument();
      expect('onDelete' in lastPropsFor.UsersList).toBe(false);
      expect('canDelete' in lastPropsFor.UsersList).toBe(false);
    });

    it('renders the page-level Delete User button on the detail view when delete permission is granted', async () => {
      window.location.hash = '#preview/admin/security/users/testuser/default';
      render(<UsersPage />, { wrapper: TestWrapper });

      triggerHashChange('#preview/admin/security/users/testuser/default');

      const btn = await screen.findByTestId('page-delete-user');
      expect(btn).toBeInTheDocument();
      expect(btn).toBeEnabled();
      expect(btn).toHaveAttribute('aria-label', 'Delete User');
      expect(btn).not.toHaveAttribute('title');
    });

    it('does not render the page-level Delete User button when the delete permission is denied', async () => {
      // UsersPage imports ExtJS from the interface/ExtJS module (not the top-level
      // package), so overrides go through NX.Permissions.check per setup.js.
      (global as any).NX.Permissions.check.mockImplementation(
        (p: string) => p !== 'nexus:users:delete'
      );

      window.location.hash = '#preview/admin/security/users/testuser/default';
      render(<UsersPage />, { wrapper: TestWrapper });

      triggerHashChange('#preview/admin/security/users/testuser/default');

      await waitFor(() => {
        expect(screen.getByTestId('user-form')).toBeInTheDocument();
      });

      expect(screen.queryByTestId('page-delete-user')).not.toBeInTheDocument();
    });

    it('does not render the page-level Delete User button in create mode', async () => {
      window.location.hash = '#preview/admin/security/users/create';
      render(<UsersPage />, { wrapper: TestWrapper });

      triggerHashChange('#preview/admin/security/users/create');

      await waitFor(() => {
        expect(screen.getByTestId('user-form')).toBeInTheDocument();
      });

      expect(screen.queryByTestId('page-delete-user')).not.toBeInTheDocument();
    });

    it('renders the page-level Delete User button disabled with the self tooltip when editing the current user', async () => {
      // Default mock: fetchUser returns user id "testuser" and current user id "admin".
      // Switch current user to "testuser" so self-precedence fires.
      (global as any).NX.State.getUser.mockReturnValue({ id: 'testuser' });

      window.location.hash = '#preview/admin/security/users/testuser/default';
      render(<UsersPage />, { wrapper: TestWrapper });

      triggerHashChange('#preview/admin/security/users/testuser/default');

      const btn = await screen.findByTestId('page-delete-user');
      expect(btn).toBeDisabled();
      expect(btn).toHaveAttribute('title', 'You cannot delete your own account.');
      expect(btn).toHaveAttribute(
        'aria-label',
        'Delete User (You cannot delete your own account.)'
      );
    });

    it('renders the page-level Delete User button enabled when editing "admin" from a different account (admin is not specially protected)', async () => {
      mockFetchUser.mockResolvedValue({
        userId: 'admin',
        firstName: 'Admin',
        lastName: 'User',
        emailAddress: 'admin@test.com',
        status: 'active',
        roles: ['nx-admin'],
        source: 'default',
        realm: 'default',
      });
      (global as any).NX.State.getUser.mockReturnValue({ id: 'somebody-else' });

      window.location.hash = '#preview/admin/security/users/admin/default';
      render(<UsersPage />, { wrapper: TestWrapper });

      triggerHashChange('#preview/admin/security/users/admin/default');

      const btn = await screen.findByTestId('page-delete-user');
      expect(btn).toBeEnabled();
      expect(btn).not.toHaveAttribute('title');
    });

    it('renders the page-level Delete User button disabled with the anonymous tooltip when editing the anonymous user', async () => {
      mockFetchUser.mockResolvedValue({
        userId: 'anonymous',
        firstName: 'Anonymous',
        lastName: 'User',
        emailAddress: '',
        status: 'active',
        roles: ['nx-anonymous'],
        source: 'default',
        realm: 'default',
      });
      (global as any).NX.State.getUser.mockReturnValue({ id: 'somebody-else' });
      (global as any).NX.State.getValue.mockImplementation((k: string) =>
        k === 'anonymousUsername' ? 'anonymous' : undefined
      );

      window.location.hash = '#preview/admin/security/users/anonymous/default';
      render(<UsersPage />, { wrapper: TestWrapper });

      triggerHashChange('#preview/admin/security/users/anonymous/default');

      const btn = await screen.findByTestId('page-delete-user');
      expect(btn).toBeDisabled();
      expect(btn).toHaveAttribute(
        'title',
        'The anonymous user is a system account and cannot be deleted.'
      );
    });

    it('renders the page-level Delete User button disabled with the external tooltip when editing an LDAP user', async () => {
      mockFetchUser.mockResolvedValue({
        userId: 'ldapuser',
        firstName: 'LDAP',
        lastName: 'User',
        emailAddress: 'ldap@test.com',
        status: 'active',
        roles: [],
        source: 'LDAP',
        realm: 'LDAP',
      });
      (global as any).NX.State.getUser.mockReturnValue({ id: 'somebody-else' });

      window.location.hash = '#preview/admin/security/users/ldapuser/LDAP';
      render(<UsersPage />, { wrapper: TestWrapper });

      triggerHashChange('#preview/admin/security/users/ldapuser/LDAP');

      const btn = await screen.findByTestId('page-delete-user');
      expect(btn).toBeDisabled();
      expect(btn).toHaveAttribute(
        'title',
        'External users cannot be deleted from Nexus.'
      );
    });

    it('disables the page-level Delete User button while a delete is in flight', async () => {
      let resolveDelete: (value: unknown) => void = () => {};
      mockDeleteUser.mockImplementationOnce(
        () => new Promise((resolve) => { resolveDelete = resolve; })
      );

      render(<UsersPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(mockFetchUser).toHaveBeenCalledWith('testuser', 'default');
      });

      triggerHashChange('#preview/admin/security/users/testuser/default');

      const pageDelete = await screen.findByTestId('page-delete-user');
      expect(pageDelete).toBeEnabled();
      fireEvent.click(pageDelete);

      const confirmInput = await screen.findByRole('textbox');
      fireEvent.change(confirmInput, { target: { value: 'Delete' } });
      const confirmDeleteButton = screen.getByRole('button', { name: /^delete$/i });

      await act(async () => {
        fireEvent.click(confirmDeleteButton);
      });

      // Delete promise is still pending — page-level button must be disabled.
      expect(screen.getByTestId('page-delete-user')).toBeDisabled();

      await act(async () => {
        resolveDelete({});
      });
    });

    it('routes to the read-only UserDetail when canUpdate is false, without wiring onDelete or canDelete (NEXUS-54437)', async () => {
      // canUpdate=false forces the !canUpdate branch that renders <UserDetail>.
      (global as any).NX.Permissions.check.mockImplementation(
        (p: string) => p !== 'nexus:users:update'
      );

      window.location.hash = '#preview/admin/security/users/testuser/default';
      render(<UsersPage />, { wrapper: TestWrapper });

      triggerHashChange('#preview/admin/security/users/testuser/default');

      await waitFor(() => {
        expect(screen.getByTestId('user-detail')).toBeInTheDocument();
      });

      expect('onDelete' in lastPropsFor.UserDetail).toBe(false);
      expect('canDelete' in lastPropsFor.UserDetail).toBe(false);
    });

  });

  describe('NEXUS-54435: permission-branched row click routing', () => {
    const USER_ID = 'testuser';
    const REALM = 'default';
    const BASE = '#preview/admin/security/users';
    const EDIT_URL = `${BASE}/${USER_ID}/${REALM}`;
    const PROFILE_URL = `${EDIT_URL}/profile`;

    beforeEach(() => {
      window.location.hash = '';
    });

    it('routes the row click to the Edit URL when canUpdate is true', async () => {
      // Default NX.Permissions.check returns true, so canUpdate = true.
      render(<UsersPage />, { wrapper: TestWrapper });

      fireEvent.click(screen.getByText('Select User'));

      await waitFor(() => {
        expect(window.location.hash).toBe(EDIT_URL);
      });
    });

    it('routes the row click to the /profile URL when canUpdate is false', async () => {
      (global as any).NX.Permissions.check.mockImplementation(
        (p: string) => p !== 'nexus:users:update'
      );

      render(<UsersPage />, { wrapper: TestWrapper });

      fireEvent.click(screen.getByText('Select User'));

      await waitFor(() => {
        expect(window.location.hash).toBe(PROFILE_URL);
      });
    });

    it('passes a getRowAriaLabel that reads "Edit {fullName}" when canUpdate is true', () => {
      render(<UsersPage />, { wrapper: TestWrapper });

      const getRowAriaLabel = lastPropsFor.UsersList.getRowAriaLabel;
      expect(typeof getRowAriaLabel).toBe('function');
      expect(
        getRowAriaLabel({ userId: USER_ID, firstName: 'Test', lastName: 'User', source: REALM })
      ).toBe('Edit Test User');
    });

    it('passes a getRowAriaLabel that reads "View {fullName}" when canUpdate is false', () => {
      (global as any).NX.Permissions.check.mockImplementation(
        (p: string) => p !== 'nexus:users:update'
      );

      render(<UsersPage />, { wrapper: TestWrapper });

      const getRowAriaLabel = lastPropsFor.UsersList.getRowAriaLabel;
      expect(typeof getRowAriaLabel).toBe('function');
      expect(
        getRowAriaLabel({ userId: USER_ID, firstName: 'Test', lastName: 'User', source: REALM })
      ).toBe('View Test User');
    });

    it('falls back to userId in getRowAriaLabel when the user has no first or last name', () => {
      render(<UsersPage />, { wrapper: TestWrapper });

      const getRowAriaLabel = lastPropsFor.UsersList.getRowAriaLabel;
      expect(
        getRowAriaLabel({ userId: 'anonymous', firstName: '', lastName: '', source: REALM })
      ).toBe('Edit anonymous');
    });

    it('never passes onEdit or canEdit to UsersList', () => {
      render(<UsersPage />, { wrapper: TestWrapper });

      expect('onEdit' in lastPropsFor.UsersList).toBe(false);
      expect('canEdit' in lastPropsFor.UsersList).toBe(false);
    });
  });

  describe('deletion error passthrough', () => {
    beforeEach(() => {
      window.location.hash = '#preview/admin/security/users/testuser/default';
    });

    it('surfaces the deletion error through setError', async () => {
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
      // Acknowledgement is the literal "Delete" (case-insensitive) — NEXUS-53356.
      fireEvent.change(confirmInput, { target: { value: 'Delete' } });
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
