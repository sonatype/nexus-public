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
});
