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

import { InviteUserForm } from '../InviteUserForm';
import * as useUsersApiModule from '../useUsersApi';

jest.mock('../useUsersApi');

const mockedUseUsersApi = useUsersApiModule.useUsersApi as jest.MockedFunction<typeof useUsersApiModule.useUsersApi>;

function createMockApi(overrides: Partial<ReturnType<typeof useUsersApiModule.useUsersApi>> = {}) {
  return {
    loading: false,
    error: null,
    setError: jest.fn(),
    fetchSources: jest.fn(),
    fetchUsers: jest.fn(),
    fetchUser: jest.fn(),
    fetchRoles: jest.fn(),
    createUser: jest.fn(),
    updateUser: jest.fn(),
    deleteUser: jest.fn(),
    changePassword: jest.fn(),
    resetUserToken: jest.fn(),
    inviteUser: jest.fn().mockResolvedValue(undefined),
    ...overrides,
  };
}

function renderForm(props: Partial<React.ComponentProps<typeof InviteUserForm>> = {}) {
  const defaultProps = {
    onSuccess: jest.fn(),
    onCancel: jest.fn(),
  };
  return render(
    <Theme>
      <InviteUserForm {...defaultProps} {...props} />
    </Theme>
  );
}

describe('InviteUserForm', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockedUseUsersApi.mockReturnValue(createMockApi());
  });

  it('renders First Name, Last Name and Email fields', () => {
    renderForm();
    expect(screen.getByLabelText(/first name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/last name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
  });

  it('renders Invite User submit button', () => {
    renderForm();
    expect(screen.getByRole('button', { name: /invite user/i })).toBeInTheDocument();
  });

  it('calls onCancel when cancel button is clicked', () => {
    const onCancel = jest.fn();
    renderForm({ onCancel });
    fireEvent.click(screen.getByTestId('form-cancel'));
    expect(onCancel).toHaveBeenCalled();
  });

  it('shows validation errors when submitting empty form', async () => {
    renderForm();
    fireEvent.click(screen.getByRole('button', { name: /invite user/i }));
    await waitFor(() => {
      expect(screen.getByText(/first name is required/i)).toBeInTheDocument();
      expect(screen.getByText(/last name is required/i)).toBeInTheDocument();
      expect(screen.getByText(/email is required/i)).toBeInTheDocument();
    });
  });

  it('shows email validation error for invalid email', async () => {
    renderForm();
    fireEvent.change(screen.getByLabelText(/first name/i), { target: { value: 'John' } });
    fireEvent.change(screen.getByLabelText(/last name/i), { target: { value: 'Smith' } });
    fireEvent.change(screen.getByLabelText(/email/i), { target: { value: 'not-an-email' } });
    fireEvent.click(screen.getByRole('button', { name: /invite user/i }));
    await waitFor(() => {
      expect(screen.getByText(/enter a valid email address/i)).toBeInTheDocument();
    });
  });

  it('calls inviteUser with trimmed payload and invokes onSuccess on success', async () => {
    const mockInviteUser = jest.fn().mockResolvedValue(undefined);
    const onSuccess = jest.fn();
    mockedUseUsersApi.mockReturnValue(createMockApi({ inviteUser: mockInviteUser }));

    renderForm({ onSuccess });

    fireEvent.change(screen.getByLabelText(/first name/i), { target: { value: '  John  ' } });
    fireEvent.change(screen.getByLabelText(/last name/i), { target: { value: '  Smith  ' } });
    fireEvent.change(screen.getByLabelText(/email/i), { target: { value: '  jsmith@example.com  ' } });
    fireEvent.click(screen.getByRole('button', { name: /invite user/i }));

    await waitFor(() => {
      expect(mockInviteUser).toHaveBeenCalledWith({
        firstName: 'John',
        lastName: 'Smith',
        email: 'jsmith@example.com',
      });
      expect(onSuccess).toHaveBeenCalledWith('jsmith@example.com');
    });
  });

  it('does not call onSuccess when inviteUser throws', async () => {
    const mockInviteUser = jest.fn().mockRejectedValue(new Error('User already exists'));
    const onSuccess = jest.fn();
    mockedUseUsersApi.mockReturnValue(createMockApi({ inviteUser: mockInviteUser, error: 'User already exists' }));

    renderForm({ onSuccess });

    fireEvent.change(screen.getByLabelText(/first name/i), { target: { value: 'John' } });
    fireEvent.change(screen.getByLabelText(/last name/i), { target: { value: 'Smith' } });
    fireEvent.change(screen.getByLabelText(/email/i), { target: { value: 'jsmith@example.com' } });
    fireEvent.click(screen.getByRole('button', { name: /invite user/i }));

    await waitFor(() => {
      expect(mockInviteUser).toHaveBeenCalled();
      expect(onSuccess).not.toHaveBeenCalled();
    });
  });

  it('displays external error prop', () => {
    renderForm({ error: 'Server error occurred' });
    expect(screen.getByText(/server error occurred/i)).toBeInTheDocument();
  });

  it('calls onCancel directly when form is empty (no discard dialog)', () => {
    const onCancel = jest.fn();
    renderForm({ onCancel });
    fireEvent.click(screen.getByTestId('form-cancel'));
    expect(onCancel).toHaveBeenCalled();
    expect(screen.queryByText(/unsaved changes/i)).not.toBeInTheDocument();
  });

  it('shows discard dialog when canceling with filled fields', async () => {
    const onCancel = jest.fn();
    renderForm({ onCancel });
    fireEvent.change(screen.getByLabelText(/first name/i), { target: { value: 'John' } });
    fireEvent.click(screen.getByTestId('form-cancel'));
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /leave/i })).toBeInTheDocument();
    });
    expect(onCancel).not.toHaveBeenCalled();
  });

  it('navigates away after confirming discard', async () => {
    const onCancel = jest.fn();
    renderForm({ onCancel });
    fireEvent.change(screen.getByLabelText(/email/i), { target: { value: 'test@x.com' } });
    fireEvent.click(screen.getByTestId('form-cancel'));
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /leave/i })).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole('button', { name: /leave/i }));
    await waitFor(() => {
      expect(onCancel).toHaveBeenCalled();
    });
  });
});
