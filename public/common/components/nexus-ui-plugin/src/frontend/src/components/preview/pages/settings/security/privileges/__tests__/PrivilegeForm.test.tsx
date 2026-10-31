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
import { PrivilegeForm } from '../PrivilegeForm';
import { usePrivilegesApi } from '../usePrivilegesApi';
import { usePrivilegeForm } from '../usePrivilegeForm';
import { PRIVILEGE_TYPES, } from '../types';

// Mock dependencies
jest.mock('../usePrivilegesApi');
jest.mock('../usePrivilegeForm');

const mockUsePrivilegesApi = usePrivilegesApi;
const mockUsePrivilegeForm = usePrivilegeForm;

const mockPrivilegeTypes = [
  { id: 'application', name: 'Application', formFields: null },
  { id: 'wildcard', name: 'Wildcard', formFields: null },
  { id: 'repository-view', name: 'Repository View', formFields: null },
  { id: 'repository-admin', name: 'Repository Admin', formFields: null },
  { id: 'repository-content-selector', name: 'Repository Content Selector', formFields: null },
  { id: 'script', name: 'Script', formFields: null },
];

const renderWithTheme = (component) => {
  return render(<Theme>{component}</Theme>);
};

// Mock child components
jest.mock('../SelectionInsights', () => ({
  SelectionInsights: () => null,
}));

describe('PrivilegeForm', () => {
  const defaultProps = {
    isCreate: true,  // Default to create mode for most tests
    onCancel: jest.fn(),
    onSave: jest.fn(),
    loading: false,
  };

  beforeEach(() => {
    jest.clearAllMocks();
    const mockFetchPrivilegeTypes = jest.fn().mockResolvedValue(mockPrivilegeTypes);
    mockUsePrivilegesApi.mockReturnValue({
      loading: false,
      error: null,
      setError: jest.fn(),
      fetchPrivileges: jest.fn().mockResolvedValue({ data: [], total: 0 }),
      fetchPrivilegeReferences: jest.fn().mockResolvedValue([]),
      fetchPrivilegeTypes: mockFetchPrivilegeTypes,
      findPrivilege: jest.fn(),
      createPrivilege: jest.fn(),
      updatePrivilege: jest.fn(),
      deletePrivilege: jest.fn(),
    });

    // Mock usePrivilegeForm hook - make it respond to the privilege prop
    mockUsePrivilegeForm.mockImplementation(({ privilege, typeId }) => {
      const formData = privilege ? {
        name: privilege.name,
        description: privilege.description,
        type: privilege.type,
        properties: privilege.properties,
      } : {
        name: '',
        description: '',
        type: typeId || PRIVILEGE_TYPES.APPLICATION,
        properties: {},
      };

      return {
        form: {
          field: jest.fn((name) => {
            const keys = name.split('.');
            let value = formData;
            for (const key of keys) {
              value = value?.[key];
            }
            return {
              name,
              value: value ?? '',
              onChange: jest.fn(),
              onBlur: jest.fn(),
              error: undefined,
            };
          }),
          checkboxGroup: jest.fn((name) => {
            const keys = name.split('.');
            let value = formData;
            for (const key of keys) {
              value = value?.[key];
            }
            return {
              name,
              value: value ?? '',
              onChange: jest.fn(),
              error: undefined,
            };
          }),
          send: jest.fn(),
          state: {
            matches: jest.fn((state) => state === 'idle'),
            context: {
              data: formData,
              privilegeTypes: mockPrivilegeTypes,
              repositories: [],
              formats: [],
              contentSelectors: [],
              scripts: [],
            },
          },
          isPristine: true,
        },
        privilege: privilege || null,
        isCreate: !privilege,
      };
    });
  });

  it('should render only setup fields when wizardStep is 1', async () => {
    renderWithTheme(<PrivilegeForm {...defaultProps} hideActions={true} wizardStep={1} />);
    
    await waitFor(() => {
      expect(screen.getByLabelText(/name/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/description/i)).toBeInTheDocument();
      // Use a more specific matcher for the Configuration section title
      // to avoid matching "configuration" in help text
      const configHeading = screen.queryByRole('heading', { name: /configuration/i });
      expect(configHeading).not.toBeInTheDocument();
    });
  });

  it('should render only configuration fields when wizardStep is 2', async () => {
    renderWithTheme(<PrivilegeForm {...defaultProps} hideActions={true} wizardStep={2} typeId="wildcard" />);

    await waitFor(() => {
      // Setup section should NOT be visible
      expect(screen.queryByLabelText(/name/i)).not.toBeInTheDocument();
      // Configuration section should be visible
      expect(screen.getByText(/configuration/i)).toBeInTheDocument();
      // Wildcard type has "Privilege String" field, not "Pattern"
      expect(screen.getByLabelText(/privilege string/i)).toBeInTheDocument();
    });
  });

  it('should render Scope, Content, Actions sections for repository-content-selector (nexus-internal-i4g5)', async () => {
    renderWithTheme(
      <PrivilegeForm {...defaultProps} hideActions={true} wizardStep={2} typeId={PRIVILEGE_TYPES.REPOSITORY_CONTENT_SELECTOR} />
    );

    await waitFor(() => {
      // Content Selector has three sections: Content, Repository, Actions
      expect(screen.getByRole('heading', { name: /content/i })).toBeInTheDocument();
      expect(screen.getByRole('heading', { name: /repository/i })).toBeInTheDocument();
      expect(screen.getByRole('heading', { name: /actions/i })).toBeInTheDocument();
      // Content Selector does not have format field - only contentSelector and repository
      expect(screen.getByTestId('combobox-properties.contentSelector')).toBeInTheDocument();
      expect(screen.getByTestId('combobox-properties.repository')).toBeInTheDocument();
    });
  });

  it('should call onValidationChange when name is updated', async () => {
    const onValidationChange = jest.fn();
    renderWithTheme(<PrivilegeForm {...defaultProps} wizardStep={1} onValidationChange={onValidationChange} />);
    
    await waitFor(() => {
      expect(onValidationChange).toHaveBeenCalled();
    });
  });

  it('should call onCancel when cancel button is clicked', async () => {
    renderWithTheme(<PrivilegeForm {...defaultProps} />);
    
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /discard/i })).toBeInTheDocument();
    });
    
    fireEvent.click(screen.getByRole('button', { name: /discard/i }));
    
    expect(defaultProps.onCancel).toHaveBeenCalled();
  });

  it('should disable form when loading', async () => {
    renderWithTheme(<PrivilegeForm {...defaultProps} loading={true} />);
    
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /create/i })).toBeDisabled();
    });
  });

  it('should populate form when editing existing privilege', async () => {
    const existingPrivilege = {
      id: 'existing-priv',
      version: '1',
      name: 'Existing Privilege',
      description: 'Existing Description',
      type: PRIVILEGE_TYPES.WILDCARD,
      readOnly: false,
      properties: { pattern: 'existing:*' },
      permission: 'existing:*',
    };
    
    renderWithTheme(<PrivilegeForm {...defaultProps} privilege={existingPrivilege} />);
    
    await waitFor(() => {
      expect(screen.getByDisplayValue('Existing Privilege')).toBeInTheDocument();
      expect(screen.getByDisplayValue('Existing Description')).toBeInTheDocument();
    });
  });

  it('should make name field read-only when editing', async () => {
    const existingPrivilege = {
      id: 'existing-priv',
      version: '1',
      name: 'Existing Privilege',
      description: 'Existing Description',
      type: PRIVILEGE_TYPES.WILDCARD,
      readOnly: false,
      properties: { pattern: 'existing:*' },
      permission: 'existing:*',
    };
    
    // Pass isCreate: false for edit mode - name field should be disabled
    renderWithTheme(<PrivilegeForm {...defaultProps} isCreate={false} privilege={existingPrivilege} />);
    
    await waitFor(() => {
      const nameInput = screen.getByDisplayValue('Existing Privilege');
      // In edit mode (isCreate: false), name field is disabled
      expect(nameInput).toBeDisabled();
    });
  });
});
