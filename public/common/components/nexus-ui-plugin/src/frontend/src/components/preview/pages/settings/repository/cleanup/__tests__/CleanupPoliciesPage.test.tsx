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

import { CleanupPoliciesPage } from '../CleanupPoliciesPage';
import { useCleanupPoliciesApi } from '../useCleanupPoliciesApi';
import { ToastProvider } from '../../../../../shared/Toast';

// Mock the API hook
jest.mock('../useCleanupPoliciesApi');

const mockedUseCleanupPoliciesApi = useCleanupPoliciesApi as jest.MockedFunction<typeof useCleanupPoliciesApi>;

// Mock child components
jest.mock('../CleanupPoliciesList', () => ({
  CleanupPoliciesList: function MockCleanupPoliciesList({
    onSelect,
    onCreate,
  }: {
    onSelect: (name: string) => void;
    onCreate: () => void;
  }) {
    return (
      <div data-testid="cleanup-policies-list">
        <button onClick={() => onSelect('test-policy')}>Select Policy</button>
        <button onClick={onCreate}>Create Policy</button>
      </div>
    );
  },
}));

jest.mock('../CleanupPolicyForm', () => ({
  CleanupPolicyForm: function MockCleanupPolicyForm({
    policy,
    isCreate,
    onSave,
    onDelete,
    onCancel,
  }: any) {
    const handleSave = () => {
      onSave({
        name: 'test-policy',
        format: 'maven2',
        notes: 'Test notes',
        criteriaLastBlobUpdated: 30,
        criteriaLastDownloaded: null,
        criteriaReleaseType: null,
        criteriaAssetRegex: null,
        retain: null,
        sortBy: null,
      });
      onCancel();
    };
    return (
      <div data-testid="cleanup-policy-form">
        <span>{isCreate ? 'Create Policy' : `Edit ${policy?.name || 'Loading...'}`}</span>
        <button onClick={handleSave}>Save</button>
        {onDelete && <button onClick={onDelete}>Delete</button>}
        <button onClick={onCancel}>Cancel</button>
      </div>
    );
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

describe('CleanupPoliciesPage', () => {
  const mockFetchCleanupPolicy = jest.fn();
  const mockFetchFormatCriteria = jest.fn();
  const mockCreateCleanupPolicy = jest.fn();
  const mockUpdateCleanupPolicy = jest.fn();
  const mockDeleteCleanupPolicy = jest.fn();
  const mockSetError = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    // Set URL hash to list view for most tests
    window.location.hash = '#preview/admin/repository/cleanup-policies';
    
    mockFetchFormatCriteria.mockResolvedValue([
      { id: 'maven2', name: 'Maven2', availableCriteria: ['lastBlobUpdated', 'lastDownloaded', 'regex'] },
      { id: 'npm', name: 'npm', availableCriteria: ['lastBlobUpdated', 'lastDownloaded'] },
    ]);
    mockFetchCleanupPolicy.mockResolvedValue({
      name: 'test-policy',
      format: 'maven2',
      notes: 'Test cleanup policy',
      criteriaLastBlobUpdated: 30,
      criteriaLastDownloaded: null,
      criteriaReleaseType: null,
      criteriaAssetRegex: null,
      retain: null,
      sortBy: null,
      inUseCount: 0,
    });
    mockCreateCleanupPolicy.mockResolvedValue({});
    mockUpdateCleanupPolicy.mockResolvedValue({});
    mockDeleteCleanupPolicy.mockResolvedValue({});

    mockedUseCleanupPoliciesApi.mockReturnValue({
      loading: false,
      error: null,
      setError: mockSetError,
      fetchCleanupPolicies: jest.fn().mockResolvedValue([]),
      fetchCleanupPolicy: mockFetchCleanupPolicy,
      fetchFormatCriteria: mockFetchFormatCriteria,
      fetchRepositories: jest.fn().mockResolvedValue([]),
      createCleanupPolicy: mockCreateCleanupPolicy,
      updateCleanupPolicy: mockUpdateCleanupPolicy,
      deleteCleanupPolicy: mockDeleteCleanupPolicy,
      previewCleanupPolicy: jest.fn().mockResolvedValue({ components: [], total: 0 }),
      getDryRunCsvUrl: jest.fn().mockReturnValue(''),
      isPreviewEnabled: jest.fn().mockReturnValue(false),
      isRetainEnabled: jest.fn().mockReturnValue(false),
    });
  });

  it('renders the cleanup policies list by default', async () => {
    render(<CleanupPoliciesPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('cleanup-policies-list')).toBeInTheDocument();
    });
    expect(screen.getByRole('heading', { name: 'Cleanup Policies' })).toBeInTheDocument();
    expect(screen.getByText('Manage component removal configuration')).toBeInTheDocument();
  });

  it('shows create policy form when Create Cleanup Policy button is clicked', async () => {
    render(<CleanupPoliciesPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('cleanup-policies-list')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('Create Policy'));

    await waitFor(() => {
      expect(screen.getByTestId('cleanup-policy-form')).toBeInTheDocument();
      expect(screen.getByText('Create Policy')).toBeInTheDocument();
    });
  });

  it('navigates to policy detail when a policy is selected', async () => {
    render(<CleanupPoliciesPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('cleanup-policies-list')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('Select Policy'));

    await waitFor(() => {
      expect(screen.getByTestId('cleanup-policy-form')).toBeInTheDocument();
    });
  });

  it('returns to list view when cancel is clicked in create mode', async () => {
    render(<CleanupPoliciesPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('cleanup-policies-list')).toBeInTheDocument();
    });

    // Go to create mode
    fireEvent.click(screen.getByText('Create Policy'));

    await waitFor(() => {
      expect(screen.getByTestId('cleanup-policy-form')).toBeInTheDocument();
    });

    // Click cancel
    fireEvent.click(screen.getByText('Cancel'));

    await waitFor(() => {
      expect(screen.getByTestId('cleanup-policies-list')).toBeInTheDocument();
    });
  });

  it('displays page header with icon and description', async () => {
    render(<CleanupPoliciesPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Cleanup Policies' })).toBeInTheDocument();
    });
    expect(screen.getByText('Manage component removal configuration')).toBeInTheDocument();
  });

  it('handles error state', async () => {
    mockedUseCleanupPoliciesApi.mockReturnValue({
      loading: false,
      error: 'Failed to load cleanup policies',
      setError: mockSetError,
      fetchCleanupPolicies: jest.fn().mockResolvedValue([]),
      fetchCleanupPolicy: mockFetchCleanupPolicy,
      fetchFormatCriteria: mockFetchFormatCriteria,
      fetchRepositories: jest.fn().mockResolvedValue([]),
      createCleanupPolicy: mockCreateCleanupPolicy,
      updateCleanupPolicy: mockUpdateCleanupPolicy,
      deleteCleanupPolicy: mockDeleteCleanupPolicy,
      previewCleanupPolicy: jest.fn().mockResolvedValue({ components: [], total: 0 }),
      getDryRunCsvUrl: jest.fn().mockReturnValue(''),
      isPreviewEnabled: jest.fn().mockReturnValue(false),
      isRetainEnabled: jest.fn().mockReturnValue(false),
    });

    render(<CleanupPoliciesPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Failed to load cleanup policies')).toBeInTheDocument();
    });
  });

  it('creates a new cleanup policy successfully', async () => {
    render(<CleanupPoliciesPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('cleanup-policies-list')).toBeInTheDocument();
    });

    // Go to create mode
    fireEvent.click(screen.getByText('Create Policy'));

    await waitFor(() => {
      expect(screen.getByTestId('cleanup-policy-form')).toBeInTheDocument();
    });

    // Save - mock form calls onSave then onCancel (form's useCleanupPolicyForm does API call, then invokes onSave + onCancel)
    fireEvent.click(screen.getByText('Save'));

    await waitFor(() => {
      expect(screen.getByTestId('cleanup-policies-list')).toBeInTheDocument();
    });
  });
});


