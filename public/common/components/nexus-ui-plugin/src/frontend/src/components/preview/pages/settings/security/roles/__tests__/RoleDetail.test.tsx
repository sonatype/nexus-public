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
import { render, screen, waitFor } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import { RoleDetail } from '../RoleDetail';
import { useRolesApi } from '../useRolesApi';
import { Role, NEXUS_SOURCE } from '../types';

// Mock useRolesApi
jest.mock('../useRolesApi');

// Mock RoleForm
jest.mock('../RoleForm', () => ({
  RoleForm: ({ role }: any) => (
    <div data-testid="role-form">
      Editing: {role?.name}
    </div>
  ),
}));

const mockUseRolesApi = useRolesApi as jest.MockedFunction<typeof useRolesApi>;

const mockRole: Role = {
  id: 'test-role',
  version: '1',
  source: 'Default',
  name: 'Test Role',
  description: 'Test Description',
  readOnly: false,
  privileges: ['nx-search-read', 'nx-repository-view-*'],
  roles: ['nx-anonymous'],
};

const mockReadOnlyRole: Role = {
  ...mockRole,
  readOnly: true,
};

const renderWithTheme = (component: React.ReactNode) => {
  return render(<Theme>{component}</Theme>);
};

describe('RoleDetail', () => {
  const mockOnSave = jest.fn();
  const mockOnCancel = jest.fn();
  const mockOnDelete = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockUseRolesApi.mockReturnValue({
      loading: false,
      error: null,
      setError: jest.fn(),
      fetchRoles: jest.fn().mockResolvedValue([]),
      fetchRoleReferences: jest.fn().mockResolvedValue([]),
      fetchRoleSources: jest.fn().mockResolvedValue([]),
      fetchRolesFromSource: jest.fn().mockResolvedValue([]),
      fetchPrivilegeReferences: jest.fn().mockResolvedValue([]),
      findRole: jest.fn().mockResolvedValue(null),
      createRole: jest.fn(),
      updateRole: jest.fn(),
      deleteRole: jest.fn(),
    });
  });

  describe('Loading State', () => {
    it('should show loading indicator when loading', () => {
      renderWithTheme(
        <RoleDetail
          role={null}
          loading={true}
          canEdit={true}
          canDelete={true}
          onSave={mockOnSave}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
        />
      );

      expect(screen.getByText('Loading role details...')).toBeInTheDocument();
    });
  });

  describe('Not Found State', () => {
    it('should show not found message when role is null', () => {
      renderWithTheme(
        <RoleDetail
          role={null}
          loading={false}
          canEdit={true}
          canDelete={true}
          onSave={mockOnSave}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
        />
      );

      expect(screen.getByText('Role not found')).toBeInTheDocument();
    });
  });

  describe('Read-Only View', () => {
    it('should show read-only view when canEdit is false', () => {
      renderWithTheme(
        <RoleDetail
          role={mockRole}
          loading={false}
          canEdit={false}
          canDelete={false}
          onSave={mockOnSave}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
        />
      );

      expect(screen.getByText('Role Information')).toBeInTheDocument();
      expect(screen.getByText('test-role')).toBeInTheDocument();
      expect(screen.getByText('Test Role')).toBeInTheDocument();
      expect(screen.getByText('Test Description')).toBeInTheDocument();
    });

    it('should show read-only view for read-only roles', () => {
      renderWithTheme(
        <RoleDetail
          role={mockReadOnlyRole}
          loading={false}
          canEdit={true}
          canDelete={true}
          onSave={mockOnSave}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
        />
      );

      expect(screen.getByText('Role Information')).toBeInTheDocument();
      expect(screen.getByText(/read-only and cannot be modified/i)).toBeInTheDocument();
    });

    it('should display privileges list in read-only view', () => {
      renderWithTheme(
        <RoleDetail
          role={mockRole}
          loading={false}
          canEdit={false}
          canDelete={false}
          onSave={mockOnSave}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
        />
      );

      expect(screen.getByText('Privileges')).toBeInTheDocument();
      expect(screen.getByText('nx-search-read')).toBeInTheDocument();
      expect(screen.getByText('nx-repository-view-*')).toBeInTheDocument();
    });

    it('should display contained roles list in read-only view', () => {
      renderWithTheme(
        <RoleDetail
          role={mockRole}
          loading={false}
          canEdit={false}
          canDelete={false}
          onSave={mockOnSave}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
        />
      );

      expect(screen.getByText('Contained Roles')).toBeInTheDocument();
      expect(screen.getByText('nx-anonymous')).toBeInTheDocument();
    });

    it('should show empty message when no privileges', () => {
      const roleWithNoPrivileges = { ...mockRole, privileges: [] };
      
      renderWithTheme(
        <RoleDetail
          role={roleWithNoPrivileges}
          loading={false}
          canEdit={false}
          canDelete={false}
          onSave={mockOnSave}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
        />
      );

      expect(screen.getByText('No privileges assigned')).toBeInTheDocument();
    });

    it('should show empty message when no contained roles', () => {
      const roleWithNoRoles = { ...mockRole, roles: [] };
      
      renderWithTheme(
        <RoleDetail
          role={roleWithNoRoles}
          loading={false}
          canEdit={false}
          canDelete={false}
          onSave={mockOnSave}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
        />
      );

      expect(screen.getByText('No contained roles')).toBeInTheDocument();
    });

    it('should render read-only view without edit capabilities', () => {
      renderWithTheme(
        <RoleDetail
          role={mockRole}
          loading={false}
          canEdit={false}
          canDelete={false}
          onSave={mockOnSave}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
        />
      );

      expect(screen.getByText('Role Information')).toBeInTheDocument();
    });
  });

  describe('Edit View', () => {
    it('should show RoleForm when canEdit is true and role is not read-only', async () => {
      renderWithTheme(
        <RoleDetail
          role={mockRole}
          loading={false}
          canEdit={true}
          canDelete={true}
          onSave={mockOnSave}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
        />
      );

      await waitFor(() => {
        expect(screen.getByTestId('role-form')).toBeInTheDocument();
        expect(screen.getByText('Editing: Test Role')).toBeInTheDocument();
      });
    });
  });

  describe('Error Handling', () => {
    it('should display API error when present', async () => {
      mockUseRolesApi.mockReturnValue({
        ...mockUseRolesApi(),
        error: 'API Error',
      });

      renderWithTheme(
        <RoleDetail
          role={mockRole}
          loading={false}
          canEdit={true}
          canDelete={true}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
        />
      );

      expect(await screen.findByText(/API Error/i)).toBeInTheDocument();
    });
  });
});


