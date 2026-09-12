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

import { ContentSelectorsList } from '../ContentSelectorsList';
import * as useContentSelectorsApiModule from '../useContentSelectorsApi';

// Mock the API hook
jest.mock('../useContentSelectorsApi');

const mockedUseContentSelectorsApi = useContentSelectorsApiModule.useContentSelectorsApi as jest.MockedFunction<
  typeof useContentSelectorsApiModule.useContentSelectorsApi
>;

// Wrapper component for Radix Theme
function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

const mockSelectors = [
  {
    name: 'maven-selector',
    type: 'csel',
    description: 'Select all Maven content',
    expression: 'format == "maven2"',
  },
  {
    name: 'npm-selector',
    type: 'csel',
    description: 'Select all npm packages',
    expression: 'format == "npm"',
  },
  {
    name: 'raw-selector',
    type: 'csel',
    description: '',
    expression: 'format == "raw"',
  },
];

describe('ContentSelectorsList', () => {
  const mockOnSelect = jest.fn();
  const mockOnCreate = jest.fn();
  const mockFetchContentSelectors = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockFetchContentSelectors.mockResolvedValue(mockSelectors);

    mockedUseContentSelectorsApi.mockReturnValue({
      loading: false,
      error: null,
      setError: jest.fn(),
      fetchContentSelectors: mockFetchContentSelectors,
      fetchContentSelector: jest.fn().mockResolvedValue(null),
      fetchRepositories: jest.fn().mockResolvedValue([]),
      createContentSelector: jest.fn().mockResolvedValue({}),
      updateContentSelector: jest.fn().mockResolvedValue({}),
      deleteContentSelector: jest.fn().mockResolvedValue({}),
      previewContentSelector: jest.fn().mockResolvedValue([]),
      fetchPrivilegesForSelector: jest.fn().mockResolvedValue([]),
    });
  });

  it('renders content selectors list', async () => {
    render(<ContentSelectorsList onSelect={mockOnSelect} onCreate={mockOnCreate} />, {
      wrapper: TestWrapper,
    });

    await waitFor(() => {
      expect(screen.getByText('maven-selector')).toBeInTheDocument();
    });
    expect(screen.getByText('npm-selector')).toBeInTheDocument();
    expect(screen.getByText('raw-selector')).toBeInTheDocument();
  });

  it('displays selector types', async () => {
    render(<ContentSelectorsList onSelect={mockOnSelect} onCreate={mockOnCreate} />, {
      wrapper: TestWrapper,
    });

    await waitFor(() => {
      expect(screen.getAllByText('CSEL')).toHaveLength(3);
    });
  });

  it('displays selector descriptions', async () => {
    render(<ContentSelectorsList onSelect={mockOnSelect} onCreate={mockOnCreate} />, {
      wrapper: TestWrapper,
    });

    await waitFor(() => {
      expect(screen.getByText('Select all Maven content')).toBeInTheDocument();
    });
    expect(screen.getByText('Select all npm packages')).toBeInTheDocument();
  });

  it('displays selector expressions', async () => {
    render(<ContentSelectorsList onSelect={mockOnSelect} onCreate={mockOnCreate} />, {
      wrapper: TestWrapper,
    });

    await waitFor(() => {
      expect(screen.getByText('maven-selector')).toBeInTheDocument();
    });
    expect(screen.getByText('format == "maven2"')).toBeInTheDocument();
    expect(screen.getByText('format == "npm"')).toBeInTheDocument();
    expect(screen.getByText('format == "raw"')).toBeInTheDocument();
  });

  it('filters selectors by name', async () => {
    render(<ContentSelectorsList onSelect={mockOnSelect} onCreate={mockOnCreate} />, {
      wrapper: TestWrapper,
    });

    await waitFor(() => {
      expect(screen.getByText('maven-selector')).toBeInTheDocument();
    });

    const filterInput = screen.getByPlaceholderText('Filter by name or description...');
    fireEvent.change(filterInput, { target: { value: 'maven' } });

    expect(screen.getByText('maven-selector')).toBeInTheDocument();
    expect(screen.queryByText('npm-selector')).not.toBeInTheDocument();
    expect(screen.queryByText('raw-selector')).not.toBeInTheDocument();
  });

  it('filters selectors by description', async () => {
    render(<ContentSelectorsList onSelect={mockOnSelect} onCreate={mockOnCreate} />, {
      wrapper: TestWrapper,
    });

    await waitFor(() => {
      expect(screen.getByText('maven-selector')).toBeInTheDocument();
    });

    const filterInput = screen.getByPlaceholderText('Filter by name or description...');
    fireEvent.change(filterInput, { target: { value: 'npm packages' } });

    expect(screen.queryByText('maven-selector')).not.toBeInTheDocument();
    expect(screen.getByText('npm-selector')).toBeInTheDocument();
    expect(screen.queryByText('raw-selector')).not.toBeInTheDocument();
  });

  it('filters selectors by expression', async () => {
    render(<ContentSelectorsList onSelect={mockOnSelect} onCreate={mockOnCreate} />, {
      wrapper: TestWrapper,
    });

    await waitFor(() => {
      expect(screen.getByText('maven-selector')).toBeInTheDocument();
    });

    const filterInput = screen.getByPlaceholderText('Filter by name or description...');
    fireEvent.change(filterInput, { target: { value: 'maven2' } });

    expect(screen.getByText('maven-selector')).toBeInTheDocument();
    expect(screen.queryByText('npm-selector')).not.toBeInTheDocument();
    expect(screen.queryByText('raw-selector')).not.toBeInTheDocument();
  });

  it('calls onSelect when a selector row is clicked', async () => {
    render(<ContentSelectorsList onSelect={mockOnSelect} onCreate={mockOnCreate} />, {
      wrapper: TestWrapper,
    });

    await waitFor(() => {
      expect(screen.getByText('maven-selector')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('maven-selector'));

    expect(mockOnSelect).toHaveBeenCalledWith('maven-selector');
  });

  it('sorts by name when name header is clicked', async () => {
    render(<ContentSelectorsList onSelect={mockOnSelect} onCreate={mockOnCreate} />, {
      wrapper: TestWrapper,
    });

    await waitFor(() => {
      expect(screen.getByText('maven-selector')).toBeInTheDocument();
    });

    // Click name header to toggle sort
    fireEvent.click(screen.getByText('Name'));

    // The table should still render all rows after sort
    await waitFor(() => {
      expect(screen.getByText('maven-selector')).toBeInTheDocument();
      expect(screen.getByText('npm-selector')).toBeInTheDocument();
      expect(screen.getByText('raw-selector')).toBeInTheDocument();
    });
  });

  it('shows EmptyState when no selectors exist', async () => {
    mockFetchContentSelectors.mockResolvedValue([]);

    render(<ContentSelectorsList onSelect={mockOnSelect} onCreate={mockOnCreate} />, {
      wrapper: TestWrapper,
    });

    await waitFor(() => {
      expect(screen.getByTestId('empty-state')).toBeInTheDocument();
    });
    expect(screen.getByText('No Content Selectors')).toBeInTheDocument();
  });

  // NEXUS-54212: the empty-state Create button must respect nexus:selectors:create.
  it('shows empty-state Create button when canCreate is true', async () => {
    mockFetchContentSelectors.mockResolvedValue([]);

    render(<ContentSelectorsList onSelect={mockOnSelect} onCreate={mockOnCreate} canCreate={true} />, {
      wrapper: TestWrapper,
    });

    await waitFor(() => {
      expect(screen.getByTestId('empty-state')).toBeInTheDocument();
    });
    expect(screen.getByRole('button', { name: /Create Selector/i })).toBeInTheDocument();
  });

  it('hides empty-state Create button when canCreate is false', async () => {
    mockFetchContentSelectors.mockResolvedValue([]);

    render(<ContentSelectorsList onSelect={mockOnSelect} onCreate={mockOnCreate} canCreate={false} />, {
      wrapper: TestWrapper,
    });

    await waitFor(() => {
      expect(screen.getByTestId('empty-state')).toBeInTheDocument();
    });
    expect(screen.queryByRole('button', { name: /Create Selector/i })).not.toBeInTheDocument();
  });

  it('shows EmptyState when filter has no matches', async () => {
    render(<ContentSelectorsList onSelect={mockOnSelect} onCreate={mockOnCreate} />, {
      wrapper: TestWrapper,
    });

    await waitFor(() => {
      expect(screen.getByText('maven-selector')).toBeInTheDocument();
    });

    const filterInput = screen.getByPlaceholderText('Filter by name or description...');
    fireEvent.change(filterInput, { target: { value: 'nonexistent' } });

    await waitFor(() => {
      expect(screen.getByTestId('empty-state')).toBeInTheDocument();
    });
    expect(screen.getByText('No Matching Content Selectors')).toBeInTheDocument();
  });

  it('displays loading state with LoadingState component', () => {
    mockFetchContentSelectors.mockImplementation(() => new Promise(() => {})); // Never resolves

    render(<ContentSelectorsList onSelect={mockOnSelect} onCreate={mockOnCreate} />, {
      wrapper: TestWrapper,
    });

    expect(screen.getByTestId('loading-state')).toBeInTheDocument();
    expect(screen.getByText('Loading content selectors...')).toBeInTheDocument();
  });

  it('displays error state with ErrorState component', async () => {
    mockFetchContentSelectors.mockRejectedValue(new Error('Failed to load'));

    render(<ContentSelectorsList onSelect={mockOnSelect} onCreate={mockOnCreate} />, {
      wrapper: TestWrapper,
    });

    await waitFor(() => {
      expect(screen.getByTestId('error-state')).toBeInTheDocument();
    });
    expect(screen.getByText('Failed to Load Content Selectors')).toBeInTheDocument();
    expect(screen.getByText('Failed to load')).toBeInTheDocument();
  });

  it('displays help section using HelpSection component', async () => {
    render(<ContentSelectorsList onSelect={mockOnSelect} onCreate={mockOnCreate} />, {
      wrapper: TestWrapper,
    });

    await waitFor(() => {
      expect(screen.getByTestId('help-section')).toBeInTheDocument();
    });
    expect(screen.getByText('What is a content selector?')).toBeInTheDocument();
    expect(screen.getByText(/Content selectors provide a means/)).toBeInTheDocument();
  });

  it('renders EntityTable correctly', async () => {
    render(<ContentSelectorsList onSelect={mockOnSelect} onCreate={mockOnCreate} />, {
      wrapper: TestWrapper,
    });

    await waitFor(() => {
      expect(screen.getByText('maven-selector')).toBeInTheDocument();
    });

    // Table should be present with column headers
    expect(screen.getByRole('table')).toBeInTheDocument();
    expect(screen.getByText('Name')).toBeInTheDocument();
    expect(screen.getByText('Type')).toBeInTheDocument();
    expect(screen.getByText('Description')).toBeInTheDocument();
    expect(screen.getByText('Expression')).toBeInTheDocument();
  });

  it('shows retry button on error', async () => {
    mockFetchContentSelectors.mockRejectedValue(new Error('Network error'));

    render(<ContentSelectorsList onSelect={mockOnSelect} onCreate={mockOnCreate} />, {
      wrapper: TestWrapper,
    });

    await waitFor(() => {
      expect(screen.getByTestId('error-state')).toBeInTheDocument();
    });

    // ErrorState includes a retry button
    expect(screen.getByText('Try again')).toBeInTheDocument();
  });

  it('rows have selector-row-{name} testId for E2E targeting', async () => {
    render(<ContentSelectorsList onSelect={mockOnSelect} onCreate={mockOnCreate} />, {
      wrapper: TestWrapper,
    });

    await waitFor(() => {
      expect(screen.getByTestId('selector-row-maven-selector')).toBeInTheDocument();
    });
    expect(screen.getByTestId('selector-row-npm-selector')).toBeInTheDocument();
    expect(screen.getByTestId('selector-row-raw-selector')).toBeInTheDocument();
  });
});
