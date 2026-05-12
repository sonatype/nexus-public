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

import { CleanupPoliciesList } from '../CleanupPoliciesList';
import * as useCleanupPoliciesApiModule from '../useCleanupPoliciesApi';

// Mock the API hook
jest.mock('../useCleanupPoliciesApi');

const mockedUseCleanupPoliciesApi = useCleanupPoliciesApiModule.useCleanupPoliciesApi as jest.MockedFunction<
  typeof useCleanupPoliciesApiModule.useCleanupPoliciesApi
>;

// Wrapper component for Radix Theme
function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

const mockPolicies = [
  {
    name: 'maven-cleanup',
    format: 'maven2',
    notes: 'Cleanup old Maven artifacts',
    criteriaLastBlobUpdated: 30,
    criteriaLastDownloaded: null,
    criteriaReleaseType: null,
    criteriaAssetRegex: null,
    retain: null,
    sortBy: null,
    inUseCount: 2,
  },
  {
    name: 'npm-cleanup',
    format: 'npm',
    notes: 'Remove unused npm packages',
    criteriaLastBlobUpdated: null,
    criteriaLastDownloaded: 60,
    criteriaReleaseType: null,
    criteriaAssetRegex: null,
    retain: null,
    sortBy: null,
    inUseCount: 1,
  },
  {
    name: 'docker-cleanup',
    format: 'docker',
    notes: '',
    criteriaLastBlobUpdated: 90,
    criteriaLastDownloaded: null,
    criteriaReleaseType: null,
    criteriaAssetRegex: null,
    retain: null,
    sortBy: null,
    inUseCount: 0,
  },
];

describe('CleanupPoliciesList', () => {
  const mockOnSelect = jest.fn();
  const mockOnCreate = jest.fn();
  const mockFetchCleanupPolicies = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockFetchCleanupPolicies.mockResolvedValue(mockPolicies);

    mockedUseCleanupPoliciesApi.mockReturnValue({
      loading: false,
      error: null,
      setError: jest.fn(),
      fetchCleanupPolicies: mockFetchCleanupPolicies,
      fetchCleanupPolicy: jest.fn().mockResolvedValue(null),
      fetchFormatCriteria: jest.fn().mockResolvedValue([]),
      fetchRepositories: jest.fn().mockResolvedValue([]),
      createCleanupPolicy: jest.fn().mockResolvedValue({}),
      updateCleanupPolicy: jest.fn().mockResolvedValue({}),
      deleteCleanupPolicy: jest.fn().mockResolvedValue({}),
      previewCleanupPolicy: jest.fn().mockResolvedValue({ components: [], total: 0 }),
      getDryRunCsvUrl: jest.fn().mockReturnValue(''),
      isPreviewEnabled: jest.fn().mockReturnValue(false),
      isRetainEnabled: jest.fn().mockReturnValue(false),
    });
  });

  it('renders cleanup policies list', async () => {
    render(<CleanupPoliciesList onSelect={mockOnSelect} onCreate={mockOnCreate} />, {
      wrapper: TestWrapper,
    });

    await waitFor(() => {
      expect(screen.getByText('maven-cleanup')).toBeInTheDocument();
    });
    expect(screen.getByText('npm-cleanup')).toBeInTheDocument();
    expect(screen.getByText('docker-cleanup')).toBeInTheDocument();
  });

  it('displays policy formats', async () => {
    render(<CleanupPoliciesList onSelect={mockOnSelect} onCreate={mockOnCreate} />, {
      wrapper: TestWrapper,
    });

    await waitFor(() => {
      expect(screen.getByText('maven2')).toBeInTheDocument();
    });
    expect(screen.getByText('npm')).toBeInTheDocument();
    expect(screen.getByText('docker')).toBeInTheDocument();
  });

  it('displays policy notes', async () => {
    render(<CleanupPoliciesList onSelect={mockOnSelect} onCreate={mockOnCreate} />, {
      wrapper: TestWrapper,
    });

    await waitFor(() => {
      expect(screen.getByText('Cleanup old Maven artifacts')).toBeInTheDocument();
    });
    expect(screen.getByText('Remove unused npm packages')).toBeInTheDocument();
  });

  it('filters policies by name', async () => {
    render(<CleanupPoliciesList onSelect={mockOnSelect} onCreate={mockOnCreate} />, {
      wrapper: TestWrapper,
    });

    await waitFor(() => {
      expect(screen.getByText('maven-cleanup')).toBeInTheDocument();
    });

    const filterInput = screen.getByPlaceholderText('Filter by name, format, or description...');
    fireEvent.change(filterInput, { target: { value: 'maven' } });

    expect(screen.getByText('maven-cleanup')).toBeInTheDocument();
    expect(screen.queryByText('npm-cleanup')).not.toBeInTheDocument();
    expect(screen.queryByText('docker-cleanup')).not.toBeInTheDocument();
  });

  it('filters policies by format', async () => {
    render(<CleanupPoliciesList onSelect={mockOnSelect} onCreate={mockOnCreate} />, {
      wrapper: TestWrapper,
    });

    await waitFor(() => {
      expect(screen.getByText('maven-cleanup')).toBeInTheDocument();
    });

    const filterInput = screen.getByPlaceholderText('Filter by name, format, or description...');
    fireEvent.change(filterInput, { target: { value: 'npm' } });

    expect(screen.queryByText('maven-cleanup')).not.toBeInTheDocument();
    expect(screen.getByText('npm-cleanup')).toBeInTheDocument();
    expect(screen.queryByText('docker-cleanup')).not.toBeInTheDocument();
  });

  it('filters policies by notes', async () => {
    render(<CleanupPoliciesList onSelect={mockOnSelect} onCreate={mockOnCreate} />, {
      wrapper: TestWrapper,
    });

    await waitFor(() => {
      expect(screen.getByText('maven-cleanup')).toBeInTheDocument();
    });

    const filterInput = screen.getByPlaceholderText('Filter by name, format, or description...');
    fireEvent.change(filterInput, { target: { value: 'unused' } });

    expect(screen.queryByText('maven-cleanup')).not.toBeInTheDocument();
    expect(screen.getByText('npm-cleanup')).toBeInTheDocument();
    expect(screen.queryByText('docker-cleanup')).not.toBeInTheDocument();
  });

  it('calls onSelect when a policy row is clicked', async () => {
    render(<CleanupPoliciesList onSelect={mockOnSelect} onCreate={mockOnCreate} />, {
      wrapper: TestWrapper,
    });

    await waitFor(() => {
      expect(screen.getByText('maven-cleanup')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('maven-cleanup'));

    expect(mockOnSelect).toHaveBeenCalledWith('maven-cleanup');
  });

  it('sorts by name when name header is clicked', async () => {
    render(<CleanupPoliciesList onSelect={mockOnSelect} onCreate={mockOnCreate} />, {
      wrapper: TestWrapper,
    });

    await waitFor(() => {
      expect(screen.getByText('maven-cleanup')).toBeInTheDocument();
    });

    // Click name header to toggle sort
    fireEvent.click(screen.getByText('Name'));

    // Verify all policies are still visible after sort
    await waitFor(() => {
      expect(screen.getByText('docker-cleanup')).toBeInTheDocument();
      expect(screen.getByText('maven-cleanup')).toBeInTheDocument();
      expect(screen.getByText('npm-cleanup')).toBeInTheDocument();
    });
  });

  it('shows empty message when no policies exist', async () => {
    mockFetchCleanupPolicies.mockResolvedValue([]);

    render(<CleanupPoliciesList onSelect={mockOnSelect} onCreate={mockOnCreate} />, {
      wrapper: TestWrapper,
    });

    await waitFor(() => {
      // EmptyState component shows this title
      expect(screen.getByText('No Cleanup Policies')).toBeInTheDocument();
    });
  });

  it('shows empty message when filter has no matches', async () => {
    render(<CleanupPoliciesList onSelect={mockOnSelect} onCreate={mockOnCreate} />, {
      wrapper: TestWrapper,
    });

    await waitFor(() => {
      expect(screen.getByText('maven-cleanup')).toBeInTheDocument();
    });

    const filterInput = screen.getByPlaceholderText('Filter by name, format, or description...');
    fireEvent.change(filterInput, { target: { value: 'nonexistent' } });

    // EmptyState shows "No Matching Policies" for filtered empty
    expect(screen.getByText('No Matching Policies')).toBeInTheDocument();
  });

  it('displays loading state', () => {
    mockFetchCleanupPolicies.mockImplementation(() => new Promise(() => {})); // Never resolves

    render(<CleanupPoliciesList onSelect={mockOnSelect} onCreate={mockOnCreate} />, {
      wrapper: TestWrapper,
    });

    expect(screen.getByText('Loading cleanup policies...')).toBeInTheDocument();
  });

  it('displays error state', async () => {
    mockFetchCleanupPolicies.mockRejectedValue(new Error('Failed to load'));

    render(<CleanupPoliciesList onSelect={mockOnSelect} onCreate={mockOnCreate} />, {
      wrapper: TestWrapper,
    });

    await waitFor(() => {
      expect(screen.getByText('Failed to load')).toBeInTheDocument();
    });
  });

  it('displays help section', async () => {
    render(<CleanupPoliciesList onSelect={mockOnSelect} onCreate={mockOnCreate} />, {
      wrapper: TestWrapper,
    });

    await waitFor(() => {
      expect(screen.getByText('What is a cleanup policy?')).toBeInTheDocument();
    });
    expect(screen.getByText(/Cleanup policies can be used to remove content/)).toBeInTheDocument();
  });

  it('rows have policy-row-{name} testId for E2E targeting', async () => {
    render(<CleanupPoliciesList onSelect={mockOnSelect} onCreate={mockOnCreate} />, {
      wrapper: TestWrapper,
    });

    await waitFor(() => {
      expect(screen.getByTestId('policy-row-maven-cleanup')).toBeInTheDocument();
    });
    expect(screen.getByTestId('policy-row-npm-cleanup')).toBeInTheDocument();
  });
});

