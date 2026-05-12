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
import { PrivilegeDetail } from '../PrivilegeDetail';
import { usePrivilegesApi } from '../usePrivilegesApi';
import { ExtJS } from '@sonatype/nexus-ui-plugin';
import { PRIVILEGE_TYPES } from '../types';

// Mock dependencies
jest.mock('../usePrivilegesApi');
jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtJS: {
    checkPermission: jest.fn(),
  },
}));

// Mock PrivilegeForm
jest.mock('../PrivilegeForm', () => ({
  PrivilegeForm: ({ onCancel, onSave, onDelete }: any) => (
    <div data-testid="privilege-form">
      <button onClick={onCancel}>Cancel Edit</button>
      <button onClick={() => onSave({ name: 'updated', description: 'Updated', type: 'wildcard', properties: {} })}>
        Save Changes
      </button>
      {onDelete && <button onClick={onDelete}>Delete</button>}
    </div>
  ),
}));

const mockUsePrivilegesApi = usePrivilegesApi as any;
const mockCheckPermission = ExtJS.checkPermission as any;

const mockPrivilege = {
  id: 'test-priv',
  version: '1',
  name: 'Test Privilege',
  description: 'Test Description',
  type: PRIVILEGE_TYPES.WILDCARD,
  readOnly: false,
  properties: { pattern: 'test:*' },
  permission: 'test:*',
};

const mockReadOnlyPrivilege = {
  ...mockPrivilege,
  id: 'nx-all',
  name: 'nx-all',
  readOnly: true,
};

const renderWithTheme = (component: React.ReactNode) => {
  return render(<Theme>{component}</Theme>);
};

describe('PrivilegeDetail', () => {
  const defaultProps = {
    privilege: mockPrivilege,
    onCancel: jest.fn(),
    onDelete: jest.fn(),
    onSave: jest.fn(),
    loading: false,
    canEdit: true,
    canDelete: true,
  };

  beforeEach(() => {
    jest.clearAllMocks();
    mockCheckPermission.mockReturnValue(true);
    mockUsePrivilegesApi.mockReturnValue({
      loading: false,
      error: null,
      setError: jest.fn(),
      fetchPrivileges: jest.fn().mockResolvedValue({ data: [], total: 0 }),
      fetchPrivilegeReferences: jest.fn().mockResolvedValue([]),
      fetchPrivilegeTypes: jest.fn().mockResolvedValue([]),
      findPrivilege: jest.fn(),
      createPrivilege: jest.fn(),
      updatePrivilege: jest.fn(),
      deletePrivilege: jest.fn(),
    });
  });

  it('should render privilege details in read-only mode', () => {
    // Read-only mode shows privilege details (canEdit: false)
    renderWithTheme(<PrivilegeDetail {...defaultProps} canEdit={false} />);
    
    expect(screen.getByText('Test Privilege')).toBeInTheDocument();
    expect(screen.getByText('Test Description')).toBeInTheDocument();
  });

  it('should display permission pattern in read-only mode', () => {
    renderWithTheme(<PrivilegeDetail {...defaultProps} canEdit={false} />);
    
    // Permission pattern is displayed (may appear in both permission and properties fields)
    const permissionElements = screen.getAllByText('test:*');
    expect(permissionElements.length).toBeGreaterThanOrEqual(1);
  });

  it('should show Back button and call onCancel when clicked', () => {
    // Read-only mode shows Back button
    renderWithTheme(<PrivilegeDetail {...defaultProps} canEdit={false} />);
    
    const backButton = screen.getByRole('button', { name: /back/i });
    expect(backButton).toBeInTheDocument();
    
    fireEvent.click(backButton);
    
    expect(defaultProps.onCancel).toHaveBeenCalled();
  });

  it('should render edit form when canEdit is true and privilege is editable', () => {
    renderWithTheme(<PrivilegeDetail {...defaultProps} />);
    
    // With canEdit: true and non-read-only privilege, the edit form is shown
    expect(screen.getByTestId('privilege-form')).toBeInTheDocument();
  });

  it('should render read-only view when privilege is read-only', () => {
    renderWithTheme(<PrivilegeDetail {...defaultProps} privilege={mockReadOnlyPrivilege} />);
    
    // Read-only privileges show the detail view instead of form
    expect(screen.queryByTestId('privilege-form')).not.toBeInTheDocument();
    expect(screen.getByText('nx-all')).toBeInTheDocument();
  });

  it('should render read-only view when user lacks edit permission', () => {
    renderWithTheme(<PrivilegeDetail {...defaultProps} canEdit={false} />);
    
    // Without edit permission, show read-only view
    expect(screen.queryByTestId('privilege-form')).not.toBeInTheDocument();
    expect(screen.getByText('Test Privilege')).toBeInTheDocument();
  });

  it('should show Delete button in edit form when canDelete is true', () => {
    renderWithTheme(<PrivilegeDetail {...defaultProps} />);
    
    expect(screen.getByRole('button', { name: /delete/i })).toBeInTheDocument();
  });

  it('should hide Delete button when privilege is read-only', () => {
    renderWithTheme(<PrivilegeDetail {...defaultProps} privilege={mockReadOnlyPrivilege} />);
    
    // Read-only view doesn't show form or delete button
    expect(screen.queryByRole('button', { name: /delete/i })).not.toBeInTheDocument();
  });

  it('should call onCancel when Cancel Edit is clicked', () => {
    renderWithTheme(<PrivilegeDetail {...defaultProps} />);
    
    expect(screen.getByTestId('privilege-form')).toBeInTheDocument();
    
    fireEvent.click(screen.getByText('Cancel Edit'));
    
    expect(defaultProps.onCancel).toHaveBeenCalled();
  });

  it('should call onSave when saving in edit mode', () => {
    renderWithTheme(<PrivilegeDetail {...defaultProps} />);
    
    fireEvent.click(screen.getByText('Save Changes'));
    
    expect(defaultProps.onSave).toHaveBeenCalled();
  });

  it('should show Delete button in edit mode when canDelete is true', () => {
    renderWithTheme(<PrivilegeDetail {...defaultProps} />);
    
    expect(screen.getByRole('button', { name: /delete/i })).toBeInTheDocument();
  });

  it('should call onDelete when Delete button is clicked', () => {
    renderWithTheme(<PrivilegeDetail {...defaultProps} />);
    
    fireEvent.click(screen.getByRole('button', { name: /delete/i }));
    
    expect(defaultProps.onDelete).toHaveBeenCalled();
  });

  it('should display read-only message for read-only privileges', () => {
    renderWithTheme(<PrivilegeDetail {...defaultProps} privilege={mockReadOnlyPrivilege} />);
    
    expect(screen.getByText(/read-only and cannot be modified/i)).toBeInTheDocument();
  });

  it('should display type-specific properties in read-only view', () => {
    const repoViewPrivilege = {
      id: 'repo-view-priv',
      version: '1',
      name: 'Repo View Privilege',
      description: 'Repository view privilege',
      type: PRIVILEGE_TYPES.REPOSITORY_VIEW,
      readOnly: false,
      properties: {
        format: 'maven2',
        repository: '*',
        actions: 'read',
      },
      permission: 'nexus:repository-view:maven2:*:read',
    };
    
    // View in read-only mode to see the properties section
    renderWithTheme(<PrivilegeDetail {...defaultProps} privilege={repoViewPrivilege} canEdit={false} />);
    
    expect(screen.getByText('maven2')).toBeInTheDocument();
  });

  it('should show loading indicator when loading prop is true', () => {
    renderWithTheme(<PrivilegeDetail {...defaultProps} loading={true} />);
    
    // When loading, the component shows a loading message instead of the form
    expect(screen.getByText(/loading privilege details/i)).toBeInTheDocument();
  });
});
