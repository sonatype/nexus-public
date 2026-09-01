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

import { renderHook } from '@testing-library/react';
import { useUsersForm, UseUsersFormOptions } from '../useUsersForm';
import { User, UserFormData } from '../types';

const mockToastSuccess = jest.fn();
const mockToastError = jest.fn();
jest.mock('../../../../../shared', () => ({
  useToast: () => ({
    success: mockToastSuccess,
    error: mockToastError,
  }),
}));

jest.mock('../usersFormMachine', () => ({
  createUsersFormMachine: jest.fn(() => ({ id: 'test-users-form-machine' })),
}));

jest.mock('../useUsersApi', () => ({
  useUsersApi: () => ({
    createUser: jest.fn(),
    updateUser: jest.fn(),
    patchUserStatus: jest.fn(),
    changePassword: jest.fn(),
    resetUserToken: jest.fn(),
    deleteUser: jest.fn(),
    fetchUser: jest.fn(),
    fetchUsers: jest.fn(),
    error: null,
    setError: jest.fn(),
    loading: false,
  }),
}));

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtJS: {
    isProEdition: jest.fn(() => false),
    checkPermission: jest.fn(() => true),
    state: jest.fn(() => ({ getValue: jest.fn(() => []) })),
  },
}));

const mockUseForm = jest.fn();
jest.mock('../../../../../../../interface/form', () => ({
  useForm: (...args: unknown[]) => mockUseForm(...args),
  createFormMachine: jest.fn(),
}));

const mockFormReturn = {
  data: {},
  isPristine: true,
  isSaving: false,
  isLoading: false,
  isDeleting: false,
  saveError: null,
  validationErrors: {},
  field: jest.fn(() => ({ name: '', value: '', onChange: jest.fn(), onBlur: jest.fn() })),
  state: { matches: jest.fn(() => false), context: { user: null } },
  send: jest.fn(),
  submit: jest.fn(),
  reset: jest.fn(),
};

function getSaveService(): (ctx: { data: UserFormData; user: User | null }) => Promise<void> {
  const options = mockUseForm.mock.calls.at(-1)?.[1];
  return options.services.save;
}

function getOnCancelAction(): () => void {
  const options = mockUseForm.mock.calls.at(-1)?.[1];
  return options.actions.onCancel;
}

const baseFormData: UserFormData = {
  userId: 'jsmith',
  firstName: 'John',
  lastName: 'Smith',
  emailAddress: 'jsmith@example.com',
  status: true,
  roles: ['nx-anonymous'],
};

describe('useUsersForm', () => {
  let createUser: jest.Mock;
  let updateUser: jest.Mock;
  let changePassword: jest.Mock;
  let onSave: jest.Mock;
  let onCancel: jest.Mock;

  beforeEach(() => {
    jest.clearAllMocks();
    mockUseForm.mockReturnValue(mockFormReturn);
    createUser = jest.fn().mockResolvedValue(undefined);
    updateUser = jest.fn().mockResolvedValue(undefined);
    changePassword = jest.fn().mockResolvedValue(undefined);
    onSave = jest.fn().mockResolvedValue(undefined);
    onCancel = jest.fn();
  });

  function renderUsersForm(overrides: Partial<UseUsersFormOptions> = {}) {
    return renderHook(() =>
      useUsersForm({
        onCancel,
        createUser,
        updateUser,
        changePassword,
        onSave,
        ...overrides,
      })
    );
  }

  it('renders without throwing and returns the expected shape', () => {
    const { result } = renderUsersForm();

    expect(result.current.form).toBe(mockFormReturn);
    expect(result.current.isCreate).toBe(true);
    expect(result.current.user).toBeNull();
  });

  describe('isCreate', () => {
    it('is true when neither userId nor user is provided', () => {
      const { result } = renderUsersForm();
      expect(result.current.isCreate).toBe(true);
    });

    it('is false when userId is provided', () => {
      const { result } = renderUsersForm({ userId: 'jsmith' });
      expect(result.current.isCreate).toBe(false);
    });

    it('is false when a preloaded user is provided', () => {
      const user: User = {
        userId: 'jsmith', realm: 'default', source: 'default', firstName: 'John',
        lastName: 'Smith', emailAddress: 'jsmith@example.com', status: 'active', roles: [],
      };
      const { result } = renderUsersForm({ user });
      expect(result.current.isCreate).toBe(false);
    });
  });

  it('returns the user loaded onto the form machine context', () => {
    const loadedUser: User = {
      userId: 'jsmith', realm: 'default', source: 'default', firstName: 'John',
      lastName: 'Smith', emailAddress: 'jsmith@example.com', status: 'active', roles: [],
    };
    mockUseForm.mockReturnValue({
      ...mockFormReturn,
      state: { ...mockFormReturn.state, context: { user: loadedUser } },
    });

    const { result } = renderUsersForm({ userId: 'jsmith' });

    expect(result.current.user).toBe(loadedUser);
  });

  describe('save service', () => {
    it('creates a new user, shows a success toast, calls onSave, then onCancel', async () => {
      renderUsersForm();
      const save = getSaveService();

      await save({ data: baseFormData, user: null });

      expect(createUser).toHaveBeenCalledWith(baseFormData);
      expect(updateUser).not.toHaveBeenCalled();
      expect(mockToastSuccess).toHaveBeenCalledWith('User "jsmith" created successfully');
      expect(onSave).toHaveBeenCalledWith(baseFormData);
      expect(onCancel).toHaveBeenCalled();
    });

    it('updates using the preloaded user prop, and changes password only when one was entered', async () => {
      const preloadedUser: User = {
        userId: 'jsmith', realm: 'LDAP', source: 'LDAP', firstName: 'John',
        lastName: 'Smith', emailAddress: 'jsmith@example.com', status: 'active', roles: [],
      };
      renderUsersForm({ userId: 'jsmith', userSource: 'LDAP', user: preloadedUser });
      const save = getSaveService();

      await save({ data: baseFormData, user: null });

      expect(updateUser).toHaveBeenCalledWith('jsmith', baseFormData, 'LDAP');
      expect(changePassword).not.toHaveBeenCalled();
      expect(mockToastSuccess).toHaveBeenCalledWith('User "jsmith" updated successfully');

      changePassword.mockClear();
      await save({ data: { ...baseFormData, password: 'newpass123' }, user: null });

      expect(changePassword).toHaveBeenCalledWith('jsmith', 'newpass123');
    });

    it('falls back to ctx.data.userId when no preloaded user or ctx.user is available', async () => {
      renderUsersForm({ userId: 'jsmith', userSource: 'default' });
      const save = getSaveService();

      await save({ data: { ...baseFormData, source: 'default' }, user: null });

      expect(updateUser).toHaveBeenCalledWith('jsmith', { ...baseFormData, source: 'default' }, 'default');
    });

    it('throws when no user can be identified for an update', async () => {
      renderUsersForm({ userId: 'jsmith' });
      const save = getSaveService();

      await expect(save({ data: { ...baseFormData, userId: '' }, user: null })).rejects.toThrow(
        'User data not loaded. Please go back and try again.'
      );
      expect(updateUser).not.toHaveBeenCalled();
      expect(mockToastError).toHaveBeenCalledWith('User data not loaded. Please go back and try again.');
    });

    it('shows an error toast and rethrows when the API call fails', async () => {
      createUser.mockRejectedValue(new Error('User already exists'));
      renderUsersForm();
      const save = getSaveService();

      await expect(save({ data: baseFormData, user: null })).rejects.toThrow('User already exists');

      expect(mockToastError).toHaveBeenCalledWith('User already exists');
      expect(onCancel).not.toHaveBeenCalled();
    });
  });

  it('passes the onCancel prop through as the onCancel action', () => {
    renderUsersForm();
    const cancelAction = getOnCancelAction();

    expect(cancelAction).toBe(onCancel);
  });

  describe('computed properties', () => {
    const localUser: User = {
      userId: 'jsmith', realm: 'default', source: 'default', firstName: 'John',
      lastName: 'Smith', emailAddress: 'js@example.com', status: 'active', roles: ['nx-admin'],
    };
    const ldapUser: User = {
      ...localUser, source: 'LDAP', realm: 'LDAP',
    };

    it('reports isExternal false for local users and true for external users', () => {
      mockUseForm.mockReturnValue({
        ...mockFormReturn,
        data: { ...baseFormData, source: 'default' },
        state: { ...mockFormReturn.state, context: { user: localUser } },
      });
      expect(renderUsersForm({ user: localUser }).result.current.isExternal).toBe(false);

      mockUseForm.mockReturnValue({
        ...mockFormReturn,
        data: { ...baseFormData, source: 'LDAP' },
        state: { ...mockFormReturn.state, context: { user: ldapUser } },
      });
      expect(renderUsersForm({ user: ldapUser }).result.current.isExternal).toBe(true);
    });

    it('reports isDirty as the negation of form.isPristine', () => {
      mockUseForm.mockReturnValue({ ...mockFormReturn, isPristine: false });
      expect(renderUsersForm().result.current.isDirty).toBe(true);
      mockUseForm.mockReturnValue({ ...mockFormReturn, isPristine: true });
      expect(renderUsersForm().result.current.isDirty).toBe(false);
    });

    it('reports rolesDirty true when pending roles differ from the machine snapshot', () => {
      mockUseForm.mockReturnValue({
        ...mockFormReturn,
        data: { ...baseFormData, roles: ['nx-admin', 'nx-anonymous'] },
        state: {
          ...mockFormReturn.state,
          context: { user: localUser, initialRoles: ['nx-admin'] },
        },
      });
      expect(renderUsersForm({ user: localUser }).result.current.rolesDirty).toBe(true);
    });

    it('reports rolesDirty false when pending roles match the machine snapshot regardless of order', () => {
      mockUseForm.mockReturnValue({
        ...mockFormReturn,
        data: { ...baseFormData, roles: ['b', 'a'] },
        state: {
          ...mockFormReturn.state,
          context: { user: localUser, initialRoles: ['a', 'b'] },
        },
      });
      expect(renderUsersForm({ user: localUser }).result.current.rolesDirty).toBe(false);
    });

    it('falls back to currentUser.roles when the machine snapshot is absent', () => {
      mockUseForm.mockReturnValue({
        ...mockFormReturn,
        data: { ...baseFormData, roles: ['nx-admin'] },
        state: { ...mockFormReturn.state, context: { user: localUser } },
      });
      expect(renderUsersForm({ user: localUser }).result.current.rolesDirty).toBe(false);
    });

    it('computes showsUserTokenReset only for edit + local + Pro + capability active + permission', () => {
      mockUseForm.mockReturnValue({
        ...mockFormReturn,
        data: { ...baseFormData, source: 'default' },
        state: { ...mockFormReturn.state, context: { user: localUser } },
      });
      expect(renderUsersForm({ user: localUser }).result.current.showsUserTokenReset).toBe(false);
    });
  });

  describe('commands', () => {
    const localUser: User = {
      userId: 'jsmith', realm: 'default', source: 'default', firstName: 'John',
      lastName: 'Smith', emailAddress: 'js@example.com', status: 'active', roles: ['nx-admin'],
    };

    it('setRoles dispatches an UPDATE event with the new role IDs', () => {
      const send = jest.fn();
      mockUseForm.mockReturnValue({ ...mockFormReturn, send });
      const { result } = renderUsersForm();
      result.current.setRoles(['a', 'b']);
      expect(send).toHaveBeenCalledWith({ type: 'UPDATE', name: 'roles', value: ['a', 'b'] });
    });

    it('submit proxies to form.submit', () => {
      const submit = jest.fn();
      mockUseForm.mockReturnValue({ ...mockFormReturn, submit });
      const { result } = renderUsersForm();
      result.current.submit();
      expect(submit).toHaveBeenCalled();
    });

    it('cancel invokes the parent onCancel callback', () => {
      const { result } = renderUsersForm();
      result.current.cancel();
      expect(onCancel).toHaveBeenCalled();
    });

    it('showPasswordChangeSection sets showPasswordChange to true', () => {
      const { result, rerender } = renderUsersForm();
      expect(result.current.showPasswordChange).toBe(false);
      result.current.showPasswordChangeSection();
      rerender();
      expect(result.current.showPasswordChange).toBe(true);
    });

    it('hidePasswordChangeSection sets showPasswordChange to false', () => {
      const { result, rerender } = renderUsersForm();
      result.current.showPasswordChangeSection();
      rerender();
      result.current.hidePasswordChangeSection();
      rerender();
      expect(result.current.showPasswordChange).toBe(false);
    });

    it('resetPasswordFields clears both password fields and hides the section', () => {
      const send = jest.fn();
      mockUseForm.mockReturnValue({ ...mockFormReturn, send });
      const { result, rerender } = renderUsersForm();
      result.current.showPasswordChangeSection();
      rerender();
      result.current.resetPasswordFields();
      rerender();
      expect(send).toHaveBeenCalledWith({ type: 'UPDATE', name: 'password', value: '' });
      expect(send).toHaveBeenCalledWith({ type: 'UPDATE', name: 'passwordConfirm', value: '' });
      expect(result.current.showPasswordChange).toBe(false);
    });

    it('openResetTokenDialog and closeResetTokenDialog toggle the dialog state', () => {
      const { result, rerender } = renderUsersForm();
      expect(result.current.resetTokenDialogOpen).toBe(false);
      result.current.openResetTokenDialog();
      rerender();
      expect(result.current.resetTokenDialogOpen).toBe(true);
      result.current.closeResetTokenDialog();
      rerender();
      expect(result.current.resetTokenDialogOpen).toBe(false);
    });

    it('confirmResetToken no-ops when there is no current user', async () => {
      const resetUserToken = jest.fn();
      const { result } = renderUsersForm({ resetUserToken });
      await result.current.confirmResetToken();
      expect(resetUserToken).not.toHaveBeenCalled();
    });

    it('confirmResetToken calls the API and surfaces a success toast', async () => {
      mockUseForm.mockReturnValue({
        ...mockFormReturn,
        state: { ...mockFormReturn.state, context: { user: localUser } },
      });
      const resetUserToken = jest.fn().mockResolvedValue(undefined);
      const { result } = renderUsersForm({ user: localUser, resetUserToken });
      await result.current.confirmResetToken();
      expect(resetUserToken).toHaveBeenCalledWith('jsmith', 'default');
      expect(mockToastSuccess).toHaveBeenCalledWith(
        expect.stringContaining('User token has been reset'),
      );
    });

    it('confirmResetToken surfaces an error toast when the API call fails', async () => {
      mockUseForm.mockReturnValue({
        ...mockFormReturn,
        state: { ...mockFormReturn.state, context: { user: localUser } },
      });
      const resetUserToken = jest.fn().mockRejectedValue(new Error('boom'));
      const { result } = renderUsersForm({ user: localUser, resetUserToken });
      await result.current.confirmResetToken();
      expect(resetUserToken).toHaveBeenCalledWith('jsmith', 'default');
      expect(mockToastError).toHaveBeenCalledWith('boom');
      expect(mockToastSuccess).not.toHaveBeenCalled();
    });

    it('toggleStatus persists via patchUserStatus, SYNC_FIELDs status, toasts, and notifies onStatusChanged', async () => {
      const send = jest.fn();
      mockUseForm.mockReturnValue({
        ...mockFormReturn,
        send,
        state: { ...mockFormReturn.state, context: { user: localUser } },
      });
      const patchUserStatus = jest.fn().mockResolvedValue(localUser);
      const onStatusChanged = jest.fn();
      const { result } = renderUsersForm({
        user: localUser,
        patchUserStatus,
        onStatusChanged,
      });
      await result.current.toggleStatus(false);
      expect(patchUserStatus).toHaveBeenCalledWith(localUser, false);
      expect(send).toHaveBeenCalledWith({ type: 'SYNC_FIELD', name: 'status', value: false });
      expect(mockToastSuccess).toHaveBeenCalledWith('User "jsmith" deactivated');
      expect(onStatusChanged).toHaveBeenCalledWith(false);
    });

    it('toggleStatus toasts and shows the activation message for active=true', async () => {
      mockUseForm.mockReturnValue({
        ...mockFormReturn,
        state: { ...mockFormReturn.state, context: { user: localUser } },
      });
      const patchUserStatus = jest.fn().mockResolvedValue(localUser);
      const { result } = renderUsersForm({ user: localUser, patchUserStatus });
      await result.current.toggleStatus(true);
      expect(mockToastSuccess).toHaveBeenCalledWith('User "jsmith" activated');
    });

    it('toggleStatus toasts an error and does not notify onStatusChanged on failure', async () => {
      mockUseForm.mockReturnValue({
        ...mockFormReturn,
        state: { ...mockFormReturn.state, context: { user: localUser } },
      });
      const patchUserStatus = jest
        .fn()
        .mockRejectedValue(new Error('Status update conflict'));
      const onStatusChanged = jest.fn();
      const { result } = renderUsersForm({
        user: localUser,
        patchUserStatus,
        onStatusChanged,
      });
      await result.current.toggleStatus(false);
      expect(mockToastError).toHaveBeenCalledWith('Status update conflict');
      expect(onStatusChanged).not.toHaveBeenCalled();
    });

    it('toggleStatus no-ops when there is no current user, does not toast, and does not notify onStatusChanged (mid-load race guard)', async () => {
      const patchUserStatus = jest.fn();
      const onStatusChanged = jest.fn();
      const { result } = renderUsersForm({ patchUserStatus, onStatusChanged });
      await result.current.toggleStatus(true);
      expect(patchUserStatus).not.toHaveBeenCalled();
      expect(onStatusChanged).not.toHaveBeenCalled();
      expect(mockToastSuccess).not.toHaveBeenCalled();
      expect(mockToastError).not.toHaveBeenCalled();
    });

    it('toggleStatus early-returns when a prior toggle is still in flight (concurrent-click guard)', async () => {
      mockUseForm.mockReturnValue({
        ...mockFormReturn,
        state: { ...mockFormReturn.state, context: { user: localUser } },
      });
      let resolveFirst!: () => void;
      const patchUserStatus = jest
        .fn()
        .mockImplementationOnce(
          () => new Promise<User>((resolve) => {
            resolveFirst = () => resolve(localUser);
          }),
        )
        .mockResolvedValueOnce(localUser);
      const { result } = renderUsersForm({ user: localUser, patchUserStatus });

      const first = result.current.toggleStatus(false);
      await result.current.toggleStatus(true);
      expect(patchUserStatus).toHaveBeenCalledTimes(1);
      resolveFirst();
      await first;
    });
  });
});
