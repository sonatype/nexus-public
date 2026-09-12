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
import '@testing-library/jest-dom';

import { EditUserView } from '../EditUserView';
import * as useUsersFormModule from '../useUsersForm';
import type { UseUsersFormResult } from '../useUsersForm';
import { User } from '../types';

jest.mock('../useUsersForm');
jest.mock('../UserForm', () => ({
  UserForm: () => <div data-testid="user-form-inner" />,
}));

const mockedUseUsersForm = useUsersFormModule.useUsersForm as jest.MockedFunction<
  typeof useUsersFormModule.useUsersForm
>;

function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

const localUser: User = {
  userId: 'jsmith',
  realm: 'default',
  source: 'default',
  firstName: 'John',
  lastName: 'Smith',
  emailAddress: 'js@example.com',
  status: 'active',
  roles: ['nx-admin'],
};

function makeVm(overrides: Partial<UseUsersFormResult> = {}): UseUsersFormResult {
  return {
    form: { isPristine: true } as any,
    user: overrides.user ?? localUser,
    isCreate: overrides.isCreate ?? false,
    currentUser: overrides.currentUser ?? localUser,
    formData: overrides.formData ?? {
      userId: 'jsmith',
      firstName: 'John',
      lastName: 'Smith',
      emailAddress: 'js@example.com',
      status: true,
      roles: ['nx-admin'],
      source: 'default',
    },
    allRoles: [],
    sources: [],
    externalRoles: [],
    isExternal: false,
    isDirty: false,
    rolesDirty: false,
    isPro: true,
    isUserTokenCapabilityActive: true,
    canResetUserToken: true,
    showsUserTokenReset: true,
    isLoading: false,
    isSaving: false,
    saveError: undefined,
    validationErrors: {},
    showPasswordChange: false,
    resetTokenDialogOpen: false,
    isResettingToken: false,
    isTogglingStatus: false,
    submit: jest.fn(),
    cancel: jest.fn(),
    setRoles: jest.fn(),
    showPasswordChangeSection: jest.fn(),
    hidePasswordChangeSection: jest.fn(),
    resetPasswordFields: jest.fn(),
    openResetTokenDialog: jest.fn(),
    closeResetTokenDialog: jest.fn(),
    confirmResetToken: jest.fn(),
    toggleStatus: jest.fn(),
    ...overrides,
  };
}

const defaultProps = {
  isCreate: false,
  userId: 'jsmith',
  userSource: 'default',
  user: localUser,
  canDelete: true,
  protectionReason: null,
  onDeleteRequest: jest.fn(),
  onSuccess: jest.fn(),
  onCancel: jest.fn(),
};

describe('EditUserView', () => {
  beforeEach(() => {
    mockedUseUsersForm.mockReset();
  });

  it('renders the edit-mode toolbar (status, reset token, delete) when not in create mode', () => {
    mockedUseUsersForm.mockReturnValue(makeVm());
    render(<EditUserView {...defaultProps} />, { wrapper: TestWrapper });
    expect(screen.getByTestId('edit-user-toolbar')).toBeInTheDocument();
    expect(screen.getByTestId('user-status-toggle')).toBeInTheDocument();
    expect(screen.getByTestId('reset-user-token-button')).toBeInTheDocument();
    expect(screen.getByTestId('page-delete-user')).toBeInTheDocument();
  });

  it('renders no toolbar in create mode', () => {
    mockedUseUsersForm.mockReturnValue(
      makeVm({ isCreate: true, currentUser: null, user: null }),
    );
    render(
      <EditUserView {...defaultProps} isCreate={true} user={null} />,
      { wrapper: TestWrapper },
    );
    expect(screen.queryByTestId('edit-user-toolbar')).not.toBeInTheDocument();
    expect(screen.queryByTestId('page-delete-user')).not.toBeInTheDocument();
  });

  it('renders the current status label (Active) when formData.status is true', () => {
    mockedUseUsersForm.mockReturnValue(
      makeVm({
        formData: {
          userId: 'jsmith',
          firstName: 'John',
          lastName: 'Smith',
          emailAddress: 'js@example.com',
          status: true,
          roles: ['nx-admin'],
          source: 'default',
        },
      }),
    );
    render(<EditUserView {...defaultProps} />, { wrapper: TestWrapper });
    expect(screen.getByTestId('user-status-label')).toHaveTextContent('Active');
  });

  it('renders the current status label (Inactive) when formData.status is false', () => {
    mockedUseUsersForm.mockReturnValue(
      makeVm({
        formData: {
          userId: 'jsmith',
          firstName: 'John',
          lastName: 'Smith',
          emailAddress: 'js@example.com',
          status: false,
          roles: ['nx-admin'],
          source: 'default',
        },
      }),
    );
    render(<EditUserView {...defaultProps} />, { wrapper: TestWrapper });
    expect(screen.getByTestId('user-status-label')).toHaveTextContent('Inactive');
  });

  it('calls vm.toggleStatus with the new value when the status switch is clicked', () => {
    const toggleStatus = jest.fn();
    mockedUseUsersForm.mockReturnValue(
      makeVm({
        formData: {
          userId: 'jsmith',
          firstName: 'John',
          lastName: 'Smith',
          emailAddress: 'js@example.com',
          status: true,
          roles: ['nx-admin'],
          source: 'default',
        },
        toggleStatus,
      }),
    );
    render(<EditUserView {...defaultProps} />, { wrapper: TestWrapper });
    fireEvent.click(screen.getByTestId('user-status-toggle'));
    expect(toggleStatus).toHaveBeenCalled();
  });

  it('disables the status toggle for external users', () => {
    const externalUser: User = { ...localUser, source: 'LDAP', realm: 'LDAP' };
    mockedUseUsersForm.mockReturnValue(
      makeVm({
        isExternal: true,
        currentUser: externalUser,
        user: externalUser,
      }),
    );
    render(
      <EditUserView {...defaultProps} user={externalUser} userSource="LDAP" />,
      { wrapper: TestWrapper },
    );
    expect(screen.getByTestId('user-status-toggle')).toBeDisabled();
  });

  it('disables the status switch while a form save is in flight', () => {
    mockedUseUsersForm.mockReturnValue(makeVm({ isSaving: true }));
    render(<EditUserView {...defaultProps} />, { wrapper: TestWrapper });
    expect(screen.getByTestId('user-status-toggle')).toBeDisabled();
  });

  it('disables the status switch while a status toggle is in flight', () => {
    mockedUseUsersForm.mockReturnValue(makeVm({ isTogglingStatus: true }));
    render(<EditUserView {...defaultProps} />, { wrapper: TestWrapper });
    expect(screen.getByTestId('user-status-toggle')).toBeDisabled();
  });

  it('keeps the status switch enabled when the form has unsaved changes', () => {
    mockedUseUsersForm.mockReturnValue(
      makeVm({ form: { isPristine: false } as any }),
    );
    render(<EditUserView {...defaultProps} />, { wrapper: TestWrapper });
    expect(screen.getByTestId('user-status-toggle')).not.toBeDisabled();
  });

  it('hides the reset-token button when showsUserTokenReset is false', () => {
    mockedUseUsersForm.mockReturnValue(makeVm({ showsUserTokenReset: false }));
    render(<EditUserView {...defaultProps} />, { wrapper: TestWrapper });
    expect(screen.queryByTestId('reset-user-token-button')).not.toBeInTheDocument();
  });

  it('opens the reset-token dialog when the reset-token button is clicked', () => {
    const openResetTokenDialog = jest.fn();
    mockedUseUsersForm.mockReturnValue(makeVm({ openResetTokenDialog }));
    render(<EditUserView {...defaultProps} />, { wrapper: TestWrapper });
    fireEvent.click(screen.getByTestId('reset-user-token-button'));
    expect(openResetTokenDialog).toHaveBeenCalled();
  });

  it('surfaces the reset-token confirm dialog when resetTokenDialogOpen is true', () => {
    mockedUseUsersForm.mockReturnValue(makeVm({ resetTokenDialogOpen: true }));
    render(<EditUserView {...defaultProps} />, { wrapper: TestWrapper });
    expect(screen.getByTestId('reset-user-token-dialog-confirm')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /reset user token/i })).toBeInTheDocument();
  });

  it('disables the Delete button when the user is protected', () => {
    mockedUseUsersForm.mockReturnValue(makeVm());
    render(
      <EditUserView
        {...defaultProps}
        protectionReason="The anonymous user is a system account and cannot be deleted."
      />,
      { wrapper: TestWrapper },
    );
    const btn = screen.getByTestId('page-delete-user');
    expect(btn).toBeDisabled();
    expect(btn).toHaveAttribute(
      'title',
      'The anonymous user is a system account and cannot be deleted.',
    );
  });

  it('disables the Delete button while a delete is in flight', () => {
    mockedUseUsersForm.mockReturnValue(makeVm());
    render(<EditUserView {...defaultProps} isDeleting={true} />, {
      wrapper: TestWrapper,
    });
    expect(screen.getByTestId('page-delete-user')).toBeDisabled();
  });

  it('does not render Delete when canDelete is false', () => {
    mockedUseUsersForm.mockReturnValue(makeVm());
    render(<EditUserView {...defaultProps} canDelete={false} />, {
      wrapper: TestWrapper,
    });
    expect(screen.queryByTestId('page-delete-user')).not.toBeInTheDocument();
  });

  it('calls onDeleteRequest when the Delete button is clicked', () => {
    const onDeleteRequest = jest.fn();
    mockedUseUsersForm.mockReturnValue(makeVm());
    render(
      <EditUserView {...defaultProps} onDeleteRequest={onDeleteRequest} />,
      { wrapper: TestWrapper },
    );
    fireEvent.click(screen.getByTestId('page-delete-user'));
    expect(onDeleteRequest).toHaveBeenCalled();
  });

  it('renders the Delete button with type="button" so clicking it never submits the form', () => {
    mockedUseUsersForm.mockReturnValue(makeVm());
    render(<EditUserView {...defaultProps} />, { wrapper: TestWrapper });
    expect(screen.getByTestId('page-delete-user')).toHaveAttribute('type', 'button');
  });

  it('renders the UserForm child inside the view', () => {
    mockedUseUsersForm.mockReturnValue(makeVm());
    render(<EditUserView {...defaultProps} />, { wrapper: TestWrapper });
    expect(screen.getByTestId('user-form-inner')).toBeInTheDocument();
  });

  it('passes an onSuccess bridging callback into the ViewModel that triggers the parent onSuccess', async () => {
    const parentOnSuccess = jest.fn();
    mockedUseUsersForm.mockImplementation((options) => {
      const vm = makeVm();
      // Verify the options passed through
      expect(options.onCancel).toBeDefined();
      expect(options.onSave).toBeDefined();
      expect(options.onStatusChanged).toBeUndefined();
      return vm;
    });
    render(
      <EditUserView {...defaultProps} onSuccess={parentOnSuccess} />,
      { wrapper: TestWrapper },
    );
    const optionsPassed = mockedUseUsersForm.mock.calls[0][0];
    await optionsPassed.onSave?.({
      userId: 'jsmith',
      firstName: 'John',
      lastName: 'Smith',
      emailAddress: 'js@example.com',
      status: true,
      roles: ['nx-admin'],
      source: 'default',
    });
    expect(parentOnSuccess).toHaveBeenCalled();
  });

  it('does not wire onStatusChanged so a status toggle keeps the local SYNC_FIELD state and never triggers a parent refresh remount', () => {
    const parentOnSuccess = jest.fn();
    mockedUseUsersForm.mockReturnValue(makeVm());
    render(
      <EditUserView {...defaultProps} onSuccess={parentOnSuccess} />,
      { wrapper: TestWrapper },
    );
    const options = mockedUseUsersForm.mock.calls[0][0];
    expect(options.onStatusChanged).toBeUndefined();
    expect(parentOnSuccess).not.toHaveBeenCalled();
  });

  it('shows the Save submit label on the wrapping SettingsForm in edit mode', () => {
    mockedUseUsersForm.mockReturnValue(makeVm());
    render(<EditUserView {...defaultProps} />, { wrapper: TestWrapper });
    expect(screen.getByRole('button', { name: /^save$/i })).toBeInTheDocument();
  });

  it('shows the Create submit label on the wrapping SettingsForm in create mode', () => {
    mockedUseUsersForm.mockReturnValue(
      makeVm({ isCreate: true, currentUser: null, user: null }),
    );
    render(
      <EditUserView {...defaultProps} isCreate={true} user={null} />,
      { wrapper: TestWrapper },
    );
    expect(screen.getByRole('button', { name: /^create$/i })).toBeInTheDocument();
  });

  it('resets the machine before navigating on discard-confirm to prevent a duplicate unsaved-changes alert', () => {
    const formReset = jest.fn();
    const parentOnCancel = jest.fn();
    mockedUseUsersForm.mockReturnValue(
      makeVm({ form: { reset: formReset, isPristine: false } as any }),
    );
    render(
      <EditUserView {...defaultProps} onCancel={parentOnCancel} />,
      { wrapper: TestWrapper },
    );
    // SettingsForm's cancelLabel defaults to "Discard"; clicking it while dirty
    // opens the "Unsaved Changes" dialog. The confirm-discard action is labelled
    // "Leave".
    fireEvent.click(screen.getByRole('button', { name: /^discard$/i }));
    fireEvent.click(screen.getByRole('button', { name: /^leave$/i }));
    expect(formReset).toHaveBeenCalled();
    expect(parentOnCancel).toHaveBeenCalled();
  });
});
