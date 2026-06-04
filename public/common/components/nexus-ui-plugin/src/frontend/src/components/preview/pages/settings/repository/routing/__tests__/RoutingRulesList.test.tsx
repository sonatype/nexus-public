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
import { RoutingRulesList } from '../RoutingRulesList';
import { useRoutingRulesApi } from '../useRoutingRulesApi';

jest.mock('../useRoutingRulesApi');

const mockUseRoutingRulesApi = useRoutingRulesApi as jest.MockedFunction<typeof useRoutingRulesApi>;

const mockRules = [
  {
    id: '1',
    name: 'block-sources',
    description: 'Block source artifacts',
    mode: 'BLOCK' as const,
    matchers: ['.*-sources\\.jar'],
    assignedRepositoryCount: 2,
    assignedRepositoryNames: ['maven-central', 'maven-snapshots'],
  },
  {
    id: '2',
    name: 'allow-releases',
    description: 'Allow release artifacts only',
    mode: 'ALLOW' as const,
    matchers: ['.*-SNAPSHOT.*'],
    assignedRepositoryCount: 0,
    assignedRepositoryNames: [],
  },
];

function TestWrapper({ children }) {
  return <Theme>{children}</Theme>;
}

describe('RoutingRulesList', () => {
  const mockOnSelect = jest.fn();
  const mockOnCreate = jest.fn();
  const mockOnPreview = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockUseRoutingRulesApi.mockReturnValue({
      loading: false,
      error: null,
      setError: jest.fn(),
      fetchRoutingRules: jest.fn().mockResolvedValue(mockRules),
      fetchRoutingRule: jest.fn(),
      createRoutingRule: jest.fn(),
      updateRoutingRule: jest.fn(),
      deleteRoutingRule: jest.fn(),
      testRoutingRule: jest.fn(),
      fetchRoutingRulesPreview: jest.fn(),
    });
  });

  it('should render rules in a table', async () => {
    render(
      <RoutingRulesList onSelect={mockOnSelect} onCreate={mockOnCreate} onPreview={mockOnPreview} />,
      { wrapper: TestWrapper }
    );

    await waitFor(() => {
      expect(screen.getByText('block-sources')).toBeInTheDocument();
      expect(screen.getByText('allow-releases')).toBeInTheDocument();
    });
  });

  it('should display mode with icon', async () => {
    render(
      <RoutingRulesList onSelect={mockOnSelect} onCreate={mockOnCreate} onPreview={mockOnPreview} />,
      { wrapper: TestWrapper }
    );

    await waitFor(() => {
      expect(screen.getByText('Block')).toBeInTheDocument();
      expect(screen.getByText('Allow')).toBeInTheDocument();
    });
  });

  it('should display repository count', async () => {
    render(
      <RoutingRulesList onSelect={mockOnSelect} onCreate={mockOnCreate} onPreview={mockOnPreview} />,
      { wrapper: TestWrapper }
    );

    await waitFor(() => {
      expect(screen.getByText('2')).toBeInTheDocument();
      expect(screen.getByText('0')).toBeInTheDocument();
    });
  });

  it('should call onSelect when row is clicked', async () => {
    render(
      <RoutingRulesList onSelect={mockOnSelect} onCreate={mockOnCreate} onPreview={mockOnPreview} />,
      { wrapper: TestWrapper }
    );

    await waitFor(() => {
      expect(screen.getByText('block-sources')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('block-sources'));

    expect(mockOnSelect).toHaveBeenCalledWith('block-sources');
  });

  it('should filter rules by search text', async () => {
    render(
      <RoutingRulesList onSelect={mockOnSelect} onCreate={mockOnCreate} onPreview={mockOnPreview} />,
      { wrapper: TestWrapper }
    );

    await waitFor(() => {
      expect(screen.getByText('block-sources')).toBeInTheDocument();
      expect(screen.getByText('allow-releases')).toBeInTheDocument();
    });

    const searchInput = screen.getByPlaceholderText('Filter by name or description');
    fireEvent.change(searchInput, { target: { value: 'block' } });

    expect(screen.getByText('block-sources')).toBeInTheDocument();
    expect(screen.queryByText('allow-releases')).not.toBeInTheDocument();
  });

  it('should filter by description', async () => {
    render(
      <RoutingRulesList onSelect={mockOnSelect} onCreate={mockOnCreate} onPreview={mockOnPreview} />,
      { wrapper: TestWrapper }
    );

    await waitFor(() => {
      expect(screen.getByText('block-sources')).toBeInTheDocument();
    });

    const searchInput = screen.getByPlaceholderText('Filter by name or description');
    fireEvent.change(searchInput, { target: { value: 'release' } });

    expect(screen.queryByText('block-sources')).not.toBeInTheDocument();
    expect(screen.getByText('allow-releases')).toBeInTheDocument();
  });

  it('should sort by name when header is clicked', async () => {
    render(
      <RoutingRulesList onSelect={mockOnSelect} onCreate={mockOnCreate} onPreview={mockOnPreview} />,
      { wrapper: TestWrapper }
    );

    await waitFor(() => {
      expect(screen.getByText('block-sources')).toBeInTheDocument();
    });

    // Click name header to toggle sort
    const nameHeader = screen.getByText('Name');
    fireEvent.click(nameHeader);
    fireEvent.click(nameHeader); // Click again to reverse

    // Both rules should still be visible
    expect(screen.getByText('block-sources')).toBeInTheDocument();
    expect(screen.getByText('allow-releases')).toBeInTheDocument();
  });

  it('should show loading state using shared LoadingState component', () => {
    mockUseRoutingRulesApi.mockReturnValue({
      ...mockUseRoutingRulesApi(),
      fetchRoutingRules: jest.fn().mockImplementation(() => new Promise(() => {})),
    });

    render(
      <RoutingRulesList onSelect={mockOnSelect} onCreate={mockOnCreate} onPreview={mockOnPreview} />,
      { wrapper: TestWrapper }
    );

    // Verify shared LoadingState component is used via data-testid
    expect(screen.getByTestId('loading-state')).toBeInTheDocument();
    expect(screen.getByText('Loading routing rules...')).toBeInTheDocument();
  });

  it('should show error state using shared ErrorState component', async () => {
    const setErrorMock = jest.fn();
    mockUseRoutingRulesApi.mockReturnValue({
      ...mockUseRoutingRulesApi(),
      error: 'Failed to load',
      setError: setErrorMock,
      fetchRoutingRules: jest.fn().mockRejectedValue(new Error('Failed to load')),
    });

    render(
      <RoutingRulesList onSelect={mockOnSelect} onCreate={mockOnCreate} onPreview={mockOnPreview} />,
      { wrapper: TestWrapper }
    );

    await waitFor(() => {
      // Verify shared ErrorState component is used via data-testid
      expect(screen.getByTestId('error-state')).toBeInTheDocument();
      expect(screen.getByText('Failed to load')).toBeInTheDocument();
    });
  });

  it('should show empty state using shared EmptyState component when no rules', async () => {
    mockUseRoutingRulesApi.mockReturnValue({
      ...mockUseRoutingRulesApi(),
      fetchRoutingRules: jest.fn().mockResolvedValue([]),
    });

    render(
      <RoutingRulesList onSelect={mockOnSelect} onCreate={mockOnCreate} onPreview={mockOnPreview} />,
      { wrapper: TestWrapper }
    );

    await waitFor(() => {
      // Verify shared EmptyState component is used via data-testid
      expect(screen.getByTestId('empty-state')).toBeInTheDocument();
      expect(screen.getByText('No Routing Rules')).toBeInTheDocument();
      // Should have create action
      expect(screen.getByRole('button', { name: /create routing rule/i })).toBeInTheDocument();
    });
  });

  it('should call onCreate when empty state create button is clicked', async () => {
    mockUseRoutingRulesApi.mockReturnValue({
      ...mockUseRoutingRulesApi(),
      fetchRoutingRules: jest.fn().mockResolvedValue([]),
    });

    render(
      <RoutingRulesList onSelect={mockOnSelect} onCreate={mockOnCreate} onPreview={mockOnPreview} />,
      { wrapper: TestWrapper }
    );

    await waitFor(() => {
      expect(screen.getByTestId('empty-state')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /create routing rule/i }));
    expect(mockOnCreate).toHaveBeenCalled();
  });

  it('should show empty filter state without create action', async () => {
    render(
      <RoutingRulesList onSelect={mockOnSelect} onCreate={mockOnCreate} onPreview={mockOnPreview} />,
      { wrapper: TestWrapper }
    );

    await waitFor(() => {
      expect(screen.getByText('block-sources')).toBeInTheDocument();
    });

    const searchInput = screen.getByPlaceholderText('Filter by name or description');
    fireEvent.change(searchInput, { target: { value: 'nonexistent' } });

    // Verify shared EmptyState component is used via data-testid
    expect(screen.getByTestId('empty-state')).toBeInTheDocument();
    expect(screen.getByText('No matching rules')).toBeInTheDocument();
    // Should NOT have create action when filtering
    expect(screen.queryByRole('button', { name: /create routing rule/i })).not.toBeInTheDocument();
  });

  it('should display help section using shared HelpSection component', async () => {
    render(
      <RoutingRulesList onSelect={mockOnSelect} onCreate={mockOnCreate} onPreview={mockOnPreview} />,
      { wrapper: TestWrapper }
    );

    await waitFor(() => {
      // Verify shared HelpSection component is used via data-testid
      expect(screen.getByTestId('help-section')).toBeInTheDocument();
      expect(screen.getByText('What are Routing Rules?')).toBeInTheDocument();
      // Should have documentation link
      expect(screen.getByRole('link', { name: /view documentation/i })).toBeInTheDocument();
    });
  });

  it('should have retry functionality in error state', async () => {
    const mockFetchRoutingRules = jest.fn()
      .mockRejectedValueOnce(new Error('Failed to load'))
      .mockResolvedValueOnce(mockRules);
    
    const setErrorMock = jest.fn();
    mockUseRoutingRulesApi.mockReturnValue({
      ...mockUseRoutingRulesApi(),
      error: 'Failed to load',
      setError: setErrorMock,
      fetchRoutingRules: mockFetchRoutingRules,
    });

    render(
      <RoutingRulesList onSelect={mockOnSelect} onCreate={mockOnCreate} onPreview={mockOnPreview} />,
      { wrapper: TestWrapper }
    );

    await waitFor(() => {
      expect(screen.getByTestId('error-state')).toBeInTheDocument();
    });

    // ErrorState should have a retry button
    const retryButton = screen.getByRole('button', { name: /try again/i });
    expect(retryButton).toBeInTheDocument();
    
    fireEvent.click(retryButton);
    
    // fetchRoutingRules should be called again
    expect(mockFetchRoutingRules).toHaveBeenCalledTimes(2);
  });
});
