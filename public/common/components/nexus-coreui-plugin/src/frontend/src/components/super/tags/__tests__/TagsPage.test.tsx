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

import { TagsPage } from '../TagsPage';
import * as tagsApi from '../tags.api';
import { mockTags, generateManyTags } from './mockData';

// Mock the API
jest.mock('../tags.api');
const mockedFetchTags = tagsApi.fetchTags as jest.MockedFunction<typeof tagsApi.fetchTags>;

// Mock the router
const mockRouterGo = jest.fn();
jest.mock('@uirouter/react', () => ({
  useCurrentStateAndParams: () => ({ params: {} }),
  useRouter: () => ({
    stateService: {
      go: mockRouterGo,
    },
  }),
}));

// Wrapper component for Radix Theme
const wrapper = ({ children }: { children: React.ReactNode }) => (
  <Theme>{children}</Theme>
);

describe('TagsPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockedFetchTags.mockResolvedValue(mockTags);
  });

  it('renders the page title and description', async () => {
    render(<TagsPage />, { wrapper });

    expect(screen.getByText('Tags')).toBeInTheDocument();
    // Description mentions CI builds and workflows
    expect(screen.getByText(/Organize and track components with custom tags/)).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.queryByTestId('tags-list-loading')).not.toBeInTheDocument();
    });
  });

  it('renders the help card', async () => {
    render(<TagsPage />, { wrapper });

    await waitFor(() => {
      expect(screen.queryByTestId('tags-list-loading')).not.toBeInTheDocument();
    });

    expect(screen.getByText('About Tags')).toBeInTheDocument();
  });

  it('shows loading state initially', () => {
    render(<TagsPage />, { wrapper });

    expect(screen.getByTestId('tags-list-loading')).toBeInTheDocument();
  });

  it('shows tags after loading', async () => {
    render(<TagsPage />, { wrapper });

    await waitFor(() => {
      expect(screen.queryByTestId('tags-list-loading')).not.toBeInTheDocument();
    });

    expect(screen.getByText('release-1.0')).toBeInTheDocument();
    expect(screen.getByText('staging')).toBeInTheDocument();
  });

  it('filters tags when typing in filter input', async () => {
    render(<TagsPage />, { wrapper });

    await waitFor(() => {
      expect(screen.queryByTestId('tags-list-loading')).not.toBeInTheDocument();
    });

    // Type in filter
    const filterInput = screen.getByTestId('tags-filter');
    fireEvent.change(filterInput, { target: { value: 'release' } });

    // Only matching tags should be visible
    expect(screen.getByText('release-1.0')).toBeInTheDocument();
    expect(screen.queryByText('staging')).not.toBeInTheDocument();
  });

  it('clears filter when clicking clear button', async () => {
    render(<TagsPage />, { wrapper });

    await waitFor(() => {
      expect(screen.queryByTestId('tags-list-loading')).not.toBeInTheDocument();
    });

    // Type in filter
    const filterInput = screen.getByTestId('tags-filter');
    fireEvent.change(filterInput, { target: { value: 'release' } });

    expect(screen.queryByText('staging')).not.toBeInTheDocument();

    // Click clear button
    const clearButton = screen.getByLabelText('Clear filter');
    fireEvent.click(clearButton);

    // All tags should be visible again
    expect(screen.getByText('staging')).toBeInTheDocument();
    expect(screen.getByText('release-1.0')).toBeInTheDocument();
  });

  it('navigates to tag detail when clicking a row', async () => {
    render(<TagsPage />, { wrapper });

    await waitFor(() => {
      expect(screen.queryByTestId('tags-list-loading')).not.toBeInTheDocument();
    });

    // Click a tag row
    fireEvent.click(screen.getByTestId('tag-row-release-1.0'));

    expect(mockRouterGo).toHaveBeenCalledWith('preview.browse.tags.detail', {
      tagName: 'release-1.0',
    });
  });

  it('sorts tags when clicking column header', async () => {
    render(<TagsPage />, { wrapper });

    await waitFor(() => {
      expect(screen.queryByTestId('tags-list-loading')).not.toBeInTheDocument();
    });

    // Get the rows - initially sorted by name ascending
    const rows = screen.getAllByRole('button', { name: /View tag/ });
    expect(rows[0]).toHaveAttribute('aria-label', 'View tag alpha-test');

    // Click Tag Name header to toggle to descending
    fireEvent.click(screen.getByText('Tag Name'));

    // Wait for re-render
    await waitFor(() => {
      const updatedRows = screen.getAllByRole('button', { name: /View tag/ });
      expect(updatedRows[0]).toHaveAttribute('aria-label', 'View tag staging');
    });
  });

  it('shows pagination when many tags', async () => {
    const manyTags = generateManyTags(50);
    mockedFetchTags.mockResolvedValue(manyTags);

    render(<TagsPage />, { wrapper });

    await waitFor(() => {
      expect(screen.queryByTestId('tags-list-loading')).not.toBeInTheDocument();
    });

    // Pagination should be visible
    expect(screen.getByTestId('pagination')).toBeInTheDocument();
  });

  it('does not show pagination for few tags', async () => {
    render(<TagsPage />, { wrapper });

    await waitFor(() => {
      expect(screen.queryByTestId('tags-list-loading')).not.toBeInTheDocument();
    });

    // Only 4 mock tags, should not show pagination (assuming page size > 4)
    expect(screen.queryByTestId('pagination')).not.toBeInTheDocument();
  });

  it('shows error state and allows retry', async () => {
    mockedFetchTags.mockRejectedValueOnce(new Error('Network error'));

    render(<TagsPage />, { wrapper });

    await waitFor(() => {
      expect(screen.getByTestId('tags-list-error')).toBeInTheDocument();
    });

    expect(screen.getByText('Network error')).toBeInTheDocument();

    // Setup success for retry
    mockedFetchTags.mockResolvedValueOnce(mockTags);

    // Click retry
    fireEvent.click(screen.getByText('Retry'));

    await waitFor(() => {
      expect(screen.queryByTestId('tags-list-error')).not.toBeInTheDocument();
    });

    expect(screen.getByText('release-1.0')).toBeInTheDocument();
  });

  it('has proper accessibility structure', async () => {
    render(<TagsPage />, { wrapper });

    await waitFor(() => {
      expect(screen.queryByTestId('tags-list-loading')).not.toBeInTheDocument();
    });

    expect(screen.getByTestId('tags-page')).toBeInTheDocument();
    expect(screen.getByTestId('tags-filter')).toBeInTheDocument();
    expect(screen.getByTestId('tags-list')).toBeInTheDocument();
  });

  it('renders info callout on page load', async () => {
    render(<TagsPage />, { wrapper });

    await waitFor(() => {
      expect(screen.queryByTestId('tags-list-loading')).not.toBeInTheDocument();
    });

    expect(screen.getByTestId('tags-info-callout')).toBeInTheDocument();
  });

  it('renders API notice callout on page load', async () => {
    render(<TagsPage />, { wrapper });

    await waitFor(() => {
      expect(screen.queryByTestId('tags-list-loading')).not.toBeInTheDocument();
    });

    expect(screen.getByTestId('tags-api-notice')).toBeInTheDocument();
  });

  it('info callout contains a "Learn more" link', async () => {
    render(<TagsPage />, { wrapper });

    await waitFor(() => {
      expect(screen.queryByTestId('tags-list-loading')).not.toBeInTheDocument();
    });

    const callout = screen.getByTestId('tags-info-callout');
    const link = callout.querySelector('a');
    expect(link).toBeInTheDocument();
    expect(link).toHaveTextContent('Learn more');
    expect(link).toHaveAttribute('href', 'https://help.sonatype.com/en/tagging.html');
    expect(link).toHaveAttribute('target', '_blank');
  });
});

