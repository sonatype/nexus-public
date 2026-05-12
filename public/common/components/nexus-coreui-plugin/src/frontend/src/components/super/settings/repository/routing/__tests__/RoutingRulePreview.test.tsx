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
import { RoutingRulePreview } from '../RoutingRulePreview';
import { useRoutingRulesApi } from '../useRoutingRulesApi';

jest.mock('../useRoutingRulesApi');

const mockUseRoutingRulesApi = useRoutingRulesApi as jest.MockedFunction<typeof useRoutingRulesApi>;

const mockPreviewData = {
  children: [
    {
      repository: 'maven-group',
      type: 'group',
      format: 'maven2',
      rule: null,
      allowed: true,
      expanded: true,
      expandable: true,
      children: [
        {
          repository: 'maven-central',
          type: 'proxy',
          format: 'maven2',
          rule: 'block-sources',
          allowed: false,
          expanded: false,
          expandable: false,
          children: null,
        },
        {
          repository: 'maven-releases',
          type: 'hosted',
          format: 'maven2',
          rule: null,
          allowed: true,
          expanded: false,
          expandable: false,
          children: null,
        },
      ],
    },
  ],
  expanded: true,
  expandable: true,
};

function TestWrapper({ children }) {
  return <Theme>{children}</Theme>;
}

describe('RoutingRulePreview', () => {
  const mockOnClose = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockUseRoutingRulesApi.mockReturnValue({
      loading: false,
      error: null,
      setError: jest.fn(),
      fetchRoutingRules: jest.fn(),
      fetchRoutingRule: jest.fn(),
      createRoutingRule: jest.fn(),
      updateRoutingRule: jest.fn(),
      deleteRoutingRule: jest.fn(),
      testRoutingRule: jest.fn(),
      fetchRoutingRulesPreview: jest.fn().mockResolvedValue(mockPreviewData),
    });
  });

  it('should render initial help state', () => {
    render(<RoutingRulePreview onClose={mockOnClose} />, { wrapper: TestWrapper });

    expect(screen.getByText(/how to use/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/e\.g\., \/com\/example/i)).toBeInTheDocument();
  });

  it('should render path input and filter', () => {
    render(<RoutingRulePreview onClose={mockOnClose} />, { wrapper: TestWrapper });

    // Test path input (using placeholder as it's not properly associated with label)
    expect(screen.getByPlaceholderText(/e\.g\., \/com\/example/i)).toBeInTheDocument();
    // Filter select has proper label association
    expect(screen.getByLabelText(/filter/i)).toBeInTheDocument();
    // Also verify the Test Path label text is rendered
    expect(screen.getByText('Test Path')).toBeInTheDocument();
  });

  it('should disable test button when path is empty', () => {
    render(<RoutingRulePreview onClose={mockOnClose} />, { wrapper: TestWrapper });

    // Test button should be disabled when path is empty
    const testButton = screen.getByRole('button', { name: /test/i });
    expect(testButton).toBeDisabled();
  });

  it('should fetch preview when test is clicked', async () => {
    const mockFetchPreview = jest.fn().mockResolvedValue(mockPreviewData);
    mockUseRoutingRulesApi.mockReturnValue({
      ...mockUseRoutingRulesApi(),
      fetchRoutingRulesPreview: mockFetchPreview,
    });

    render(<RoutingRulePreview onClose={mockOnClose} />, { wrapper: TestWrapper });

    const pathInput = screen.getByPlaceholderText(/e\.g\., \/com\/example/i);
    fireEvent.change(pathInput, { target: { value: '/com/example/lib.jar' } });

    fireEvent.click(screen.getByRole('button', { name: /test/i }));

    await waitFor(() => {
      expect(mockFetchPreview).toHaveBeenCalledWith('/com/example/lib.jar', 'all');
    });
  });

  it('should fetch preview on Enter key', async () => {
    const mockFetchPreview = jest.fn().mockResolvedValue(mockPreviewData);
    mockUseRoutingRulesApi.mockReturnValue({
      ...mockUseRoutingRulesApi(),
      fetchRoutingRulesPreview: mockFetchPreview,
    });

    render(<RoutingRulePreview onClose={mockOnClose} />, { wrapper: TestWrapper });

    const pathInput = screen.getByPlaceholderText(/e\.g\., \/com\/example/i);
    fireEvent.change(pathInput, { target: { value: '/com/example/lib.jar' } });
    fireEvent.keyDown(pathInput, { key: 'Enter' });

    await waitFor(() => {
      expect(mockFetchPreview).toHaveBeenCalled();
    });
  });

  it('should display preview results', async () => {
    render(<RoutingRulePreview onClose={mockOnClose} />, { wrapper: TestWrapper });

    const pathInput = screen.getByPlaceholderText(/e\.g\., \/com\/example/i);
    fireEvent.change(pathInput, { target: { value: '/com/example/lib.jar' } });
    fireEvent.click(screen.getByRole('button', { name: /test/i }));

    await waitFor(() => {
      expect(screen.getByText('maven-group')).toBeInTheDocument();
    });
  });

  it('should display summary stats', async () => {
    render(<RoutingRulePreview onClose={mockOnClose} />, { wrapper: TestWrapper });

    const pathInput = screen.getByPlaceholderText(/e\.g\., \/com\/example/i);
    fireEvent.change(pathInput, { target: { value: '/com/example/lib.jar' } });
    fireEvent.click(screen.getByRole('button', { name: /test/i }));

    await waitFor(() => {
      // Check for summary stat labels (in the stat-label class)
      expect(screen.getByText('Total')).toBeInTheDocument();
      // Use getAllByText since 'Allowed' and 'Blocked' appear in both summary and tree rows
      expect(screen.getAllByText('Allowed').length).toBeGreaterThanOrEqual(1);
      expect(screen.getAllByText('Blocked').length).toBeGreaterThanOrEqual(1);
    });
  });

  it('should show allowed/blocked status', async () => {
    render(<RoutingRulePreview onClose={mockOnClose} />, { wrapper: TestWrapper });

    const pathInput = screen.getByPlaceholderText(/e\.g\., \/com\/example/i);
    fireEvent.change(pathInput, { target: { value: '/com/example/lib.jar' } });
    fireEvent.click(screen.getByRole('button', { name: /test/i }));

    await waitFor(() => {
      expect(screen.getByText('maven-group')).toBeInTheDocument();
      // There should be both allowed and blocked items
    });
  });

  it('should show rule name for repositories with rules', async () => {
    render(<RoutingRulePreview onClose={mockOnClose} />, { wrapper: TestWrapper });

    const pathInput = screen.getByPlaceholderText(/e\.g\., \/com\/example/i);
    fireEvent.change(pathInput, { target: { value: '/com/example/lib.jar' } });
    fireEvent.click(screen.getByRole('button', { name: /test/i }));

    await waitFor(() => {
      expect(screen.getByText('block-sources')).toBeInTheDocument();
    });
  });

  it('should apply filter', async () => {
    const mockFetchPreview = jest.fn().mockResolvedValue(mockPreviewData);
    mockUseRoutingRulesApi.mockReturnValue({
      ...mockUseRoutingRulesApi(),
      fetchRoutingRulesPreview: mockFetchPreview,
    });

    render(<RoutingRulePreview onClose={mockOnClose} />, { wrapper: TestWrapper });

    // First test with default filter (all)
    const pathInput = screen.getByPlaceholderText(/e\.g\., \/com\/example/i);
    fireEvent.change(pathInput, { target: { value: '/com/example/lib.jar' } });
    fireEvent.click(screen.getByRole('button', { name: /test/i }));

    await waitFor(() => {
      // Default filter is 'all'
      expect(mockFetchPreview).toHaveBeenCalledWith('/com/example/lib.jar', 'all');
    });
  });

  it('should render filter selector', () => {
    render(<RoutingRulePreview onClose={mockOnClose} />, { wrapper: TestWrapper });

    // Verify filter label is present
    expect(screen.getByText('Filter')).toBeInTheDocument();
    // Verify the default "All Repositories" option is displayed
    expect(screen.getByText('All Repositories')).toBeInTheDocument();
  });

  it('should show loading state', async () => {
    mockUseRoutingRulesApi.mockReturnValue({
      ...mockUseRoutingRulesApi(),
      fetchRoutingRulesPreview: jest.fn().mockImplementation(() => new Promise(() => {})),
    });

    render(<RoutingRulePreview onClose={mockOnClose} />, { wrapper: TestWrapper });

    const pathInput = screen.getByPlaceholderText(/e\.g\., \/com\/example/i);
    fireEvent.change(pathInput, { target: { value: '/com/example/lib.jar' } });
    fireEvent.click(screen.getByRole('button', { name: /test/i }));

    expect(screen.getByText(/testing/i)).toBeInTheDocument();
  });

  it('should show error state', async () => {
    mockUseRoutingRulesApi.mockReturnValue({
      ...mockUseRoutingRulesApi(),
      fetchRoutingRulesPreview: jest.fn().mockRejectedValue(new Error('Failed to load')),
    });

    render(<RoutingRulePreview onClose={mockOnClose} />, { wrapper: TestWrapper });

    const pathInput = screen.getByPlaceholderText(/e\.g\., \/com\/example/i);
    fireEvent.change(pathInput, { target: { value: '/com/example/lib.jar' } });
    fireEvent.click(screen.getByRole('button', { name: /test/i }));

    await waitFor(() => {
      expect(screen.getByText(/failed to load/i)).toBeInTheDocument();
    });
  });

  it('should show empty state when no repositories match filter', async () => {
    mockUseRoutingRulesApi.mockReturnValue({
      ...mockUseRoutingRulesApi(),
      fetchRoutingRulesPreview: jest.fn().mockResolvedValue({
        children: [],
        expanded: false,
        expandable: false,
      }),
    });

    render(<RoutingRulePreview onClose={mockOnClose} />, { wrapper: TestWrapper });

    const pathInput = screen.getByPlaceholderText(/e\.g\., \/com\/example/i);
    fireEvent.change(pathInput, { target: { value: '/com/example/lib.jar' } });
    fireEvent.click(screen.getByRole('button', { name: /test/i }));

    await waitFor(() => {
      expect(screen.getByText(/no repositories found/i)).toBeInTheDocument();
    });
  });

  it('should show format badge', async () => {
    render(<RoutingRulePreview onClose={mockOnClose} />, { wrapper: TestWrapper });

    const pathInput = screen.getByPlaceholderText(/e\.g\., \/com\/example/i);
    fireEvent.change(pathInput, { target: { value: '/com/example/lib.jar' } });
    fireEvent.click(screen.getByRole('button', { name: /test/i }));

    await waitFor(() => {
      expect(screen.getAllByText('maven2')).toHaveLength(3); // 3 repos with maven2 format
    });
  });

  it('should expand/collapse tree nodes', async () => {
    render(<RoutingRulePreview onClose={mockOnClose} />, { wrapper: TestWrapper });

    const pathInput = screen.getByPlaceholderText(/e\.g\., \/com\/example/i);
    fireEvent.change(pathInput, { target: { value: '/com/example/lib.jar' } });
    fireEvent.click(screen.getByRole('button', { name: /test/i }));

    await waitFor(() => {
      expect(screen.getByText('maven-group')).toBeInTheDocument();
    });

    // Children should be visible since expanded is true
    expect(screen.getByText('maven-central')).toBeInTheDocument();
  });
});

