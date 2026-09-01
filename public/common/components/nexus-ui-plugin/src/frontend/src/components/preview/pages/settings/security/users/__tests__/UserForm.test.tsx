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

import { UserForm } from '../UserForm';
import type { UseUsersFormResult } from '../useUsersForm';
import { User, UserFormData } from '../types';

jest.mock('../useUserTreePreview', () => ({
  useUserTreePreview: () => ({
    tree: [],
    loading: false,
    error: null,
    toggleExpand: jest.fn(),
    expandAll: jest.fn(),
    collapseAll: jest.fn(),
    setSearchTerm: jest.fn(),
  }),
}));

jest.mock('../../roles/RoleExplorerTree', () => ({
  RoleExplorerTree: () => <div data-testid="role-explorer-tree" />,
}));

function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

const baseFormData: UserFormData = {
  userId: 'jsmith',
  firstName: 'John',
  lastName: 'Smith',
  emailAddress: 'jsmith@example.com',
  status: true,
  roles: ['nx-admin'],
  source: 'default',
};

const baseUser: User = {
  userId: 'jsmith',
  realm: 'default',
  source: 'default',
  firstName: 'John',
  lastName: 'Smith',
  emailAddress: 'jsmith@example.com',
  status: 'active',
  roles: ['nx-admin'],
};

function makeMockForm(formData: UserFormData) {
  const field = jest.fn((name: string) => ({
    name,
    value: (formData as any)[name] ?? '',
    onChange: jest.fn(),
    onBlur: jest.fn(),
  }));
  const checkbox = jest.fn((name: string) => ({
    name,
    checked: !!(formData as any)[name],
    onChange: jest.fn(),
  }));
  return {
    data: formData,
    field,
    checkbox,
    submit: jest.fn(),
    send: jest.fn(),
    reset: jest.fn(),
    state: { context: {}, matches: jest.fn(() => false) },
    isPristine: true,
    isSaving: false,
    isLoading: false,
    isDeleting: false,
    saveError: null,
    validationErrors: {},
    touched: {},
    hasValidationErrors: false,
  };
}

function makeVm(overrides: Partial<UseUsersFormResult> = {}): UseUsersFormResult {
  const formData = overrides.formData ?? baseFormData;
  const form = overrides.form ?? (makeMockForm(formData) as any);
  return {
    form,
    user: overrides.user ?? baseUser,
    isCreate: false,
    currentUser: overrides.currentUser ?? baseUser,
    formData,
    allRoles: [
      { id: 'nx-admin', name: 'Admin' },
      { id: 'nx-anonymous', name: 'Anonymous' },
    ],
    sources: [{ id: 'default', name: 'Local' }],
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

describe('UserForm', () => {
  it('renders a loading placeholder while the ViewModel is loading', () => {
    const vm = makeVm({ isLoading: true });
    render(<UserForm vm={vm} />, { wrapper: TestWrapper });
    expect(screen.getByRole('status')).toBeInTheDocument();
    expect(screen.getByText(/loading form/i)).toBeInTheDocument();
  });

  it('renders the Details, Roles, and Privileges sections in edit mode', () => {
    render(<UserForm vm={makeVm()} />, { wrapper: TestWrapper });
    expect(screen.getByText('Details')).toBeInTheDocument();
    expect(screen.getByText('Roles')).toBeInTheDocument();
    expect(screen.getByText(/Privileges/i)).toBeInTheDocument();
  });

  it('shows the externally-managed indicator when isExternal is true', () => {
    const externalUser: User = {
      ...baseUser,
      source: 'LDAP',
      realm: 'LDAP',
    };
    const vm = makeVm({
      isExternal: true,
      currentUser: externalUser,
      user: externalUser,
      externalRoles: ['ldap-role'],
    });
    render(<UserForm vm={vm} />, { wrapper: TestWrapper });
    expect(screen.getByTestId('user-form-external-indicator')).toBeInTheDocument();
  });

  it('does not show the externally-managed indicator for local users', () => {
    render(<UserForm vm={makeVm()} />, { wrapper: TestWrapper });
    expect(screen.queryByTestId('user-form-external-indicator')).not.toBeInTheDocument();
  });

  it('renders the Externally Assigned Roles list when external users have external roles', () => {
    const externalUser: User = {
      ...baseUser,
      source: 'LDAP',
      realm: 'LDAP',
    };
    const vm = makeVm({
      isExternal: true,
      currentUser: externalUser,
      user: externalUser,
      externalRoles: ['ldap-admins'],
      allRoles: [{ id: 'ldap-admins', name: 'LDAP Admins' }],
    });
    render(<UserForm vm={vm} />, { wrapper: TestWrapper });
    expect(screen.getByText('Externally Assigned Roles')).toBeInTheDocument();
    const chip = screen.getByTestId('external-role-ldap-admins');
    expect(chip).toBeInTheDocument();
    expect(chip).toHaveTextContent('LDAP Admins');
  });

  it('renders a Change Password button in edit mode for local users when the section is closed', () => {
    render(<UserForm vm={makeVm()} />, { wrapper: TestWrapper });
    expect(screen.getByTestId('change-password-btn')).toBeInTheDocument();
  });

  it('shows password inputs when showPasswordChange is true and cancel triggers resetPasswordFields', () => {
    const resetPasswordFields = jest.fn();
    const vm = makeVm({ showPasswordChange: true, resetPasswordFields });
    render(<UserForm vm={vm} />, { wrapper: TestWrapper });
    fireEvent.click(screen.getByRole('button', { name: /^cancel$/i }));
    expect(resetPasswordFields).toHaveBeenCalled();
  });

  it('invokes showPasswordChangeSection when the Change Password button is clicked', () => {
    const showPasswordChangeSection = jest.fn();
    const vm = makeVm({ showPasswordChangeSection });
    render(<UserForm vm={vm} />, { wrapper: TestWrapper });
    fireEvent.click(screen.getByTestId('change-password-btn'));
    expect(showPasswordChangeSection).toHaveBeenCalled();
  });

  it('shows the unsaved-changes hint on the tree preview when rolesDirty is true', () => {
    const vm = makeVm({ rolesDirty: true });
    render(<UserForm vm={vm} />, { wrapper: TestWrapper });
    expect(screen.getByTestId('tree-preview-unsaved')).toBeInTheDocument();
  });

  it('hides the unsaved-changes hint on the tree preview when rolesDirty is false', () => {
    render(<UserForm vm={makeVm({ rolesDirty: false })} />, { wrapper: TestWrapper });
    expect(screen.queryByTestId('tree-preview-unsaved')).not.toBeInTheDocument();
  });

  it('shows a placeholder in the tree preview when no roles are selected', () => {
    const emptyRolesFormData: UserFormData = { ...baseFormData, roles: [] };
    const vm = makeVm({ formData: emptyRolesFormData });
    render(<UserForm vm={vm} />, { wrapper: TestWrapper });
    expect(screen.getByText(/grant at least one role/i)).toBeInTheDocument();
  });

  it('renders password confirm and password fields on create mode', () => {
    const createFormData: UserFormData = {
      userId: '',
      firstName: '',
      lastName: '',
      emailAddress: '',
      status: true,
      roles: [],
      source: 'default',
    };
    const vm = makeVm({
      isCreate: true,
      currentUser: null,
      user: null,
      formData: createFormData,
    });
    render(<UserForm vm={vm} />, { wrapper: TestWrapper });
    expect(vm.form.field).toHaveBeenCalledWith('password');
    expect(vm.form.field).toHaveBeenCalledWith('passwordConfirm');
  });

  it('does not render password fields for external users on create', () => {
    const externalUser: User = {
      ...baseUser,
      userId: '',
      source: 'LDAP',
      realm: 'LDAP',
    };
    const createFormData: UserFormData = {
      userId: '',
      firstName: '',
      lastName: '',
      emailAddress: '',
      status: true,
      roles: [],
      source: 'LDAP',
    };
    const vm = makeVm({
      isCreate: true,
      isExternal: true,
      currentUser: null,
      user: null,
      formData: createFormData,
    });
    render(<UserForm vm={vm} />, { wrapper: TestWrapper });
    expect(vm.form.field).not.toHaveBeenCalledWith('password');
    expect(vm.form.field).not.toHaveBeenCalledWith('passwordConfirm');
  });

  it('surfaces validation errors on the roles field when validationErrors.roles is present', () => {
    const form = makeMockForm(baseFormData);
    (form as any).validationErrors = { roles: 'At least one role must be assigned' };
    const vm = makeVm({
      form: form as any,
      validationErrors: { roles: 'At least one role must be assigned' },
    });
    render(<UserForm vm={vm} />, { wrapper: TestWrapper });
    expect(screen.getByText('At least one role must be assigned')).toBeInTheDocument();
  });

});
