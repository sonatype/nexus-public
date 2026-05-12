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
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';

import { ContentSelectorPreview } from '../ContentSelectorPreview';
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

describe('ContentSelectorPreview', () => {
  const mockFetchRepositories = jest.fn();
  const mockPreviewContentSelector = jest.fn();

  const mockRepositories = [
    { id: '*', name: 'All Repositories' },
    { id: 'maven-central', name: 'Maven Central' },
    { id: 'npm-proxy', name: 'npm Proxy' },
  ];

  beforeEach(() => {
    jest.clearAllMocks();
    mockFetchRepositories.mockResolvedValue(mockRepositories);
    mockPreviewContentSelector.mockResolvedValue([
      '/org/example/artifact-1.0.jar',
      '/org/example/artifact-2.0.jar',
    ]);

    mockedUseContentSelectorsApi.mockReturnValue({
      fetchRepositories: mockFetchRepositories,
      previewContentSelector: mockPreviewContentSelector,
      loading: false,
      error: null,
      setError: jest.fn(),
      fetchContentSelectors: jest.fn().mockResolvedValue([]),
      fetchContentSelector: jest.fn().mockResolvedValue(null),
      createContentSelector: jest.fn().mockResolvedValue({}),
      updateContentSelector: jest.fn().mockResolvedValue({}),
      deleteContentSelector: jest.fn().mockResolvedValue({}),
      fetchPrivilegesForSelector: jest.fn().mockResolvedValue([]),
    });
  });

  describe('initial render', () => {
    it('renders description text', () => {
      render(
        <ContentSelectorPreview type="csel" expression='format == "maven2"' />,
        { wrapper: TestWrapper }
      );

      expect(
        screen.getByText(/Select a repository to evaluate the content selector/)
      ).toBeInTheDocument();
    });

    it('renders repository select dropdown', async () => {
      render(
        <ContentSelectorPreview type="csel" expression='format == "maven2"' />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(screen.getByText('Preview Repository')).toBeInTheDocument();
      });
    });

    it('loads repositories on mount', async () => {
      render(
        <ContentSelectorPreview type="csel" expression='format == "maven2"' />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(mockFetchRepositories).toHaveBeenCalled();
      });
    });

    it('renders preview button', async () => {
      render(
        <ContentSelectorPreview type="csel" expression='format == "maven2"' />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Preview/i })).toBeInTheDocument();
      });
    });

    it('renders results table', () => {
      render(
        <ContentSelectorPreview type="csel" expression='format == "maven2"' />,
        { wrapper: TestWrapper }
      );

      expect(screen.getByText('Name')).toBeInTheDocument();
    });
  });

  describe('repository loading', () => {
    it('shows "All Repositories" selected by default', async () => {
      render(
        <ContentSelectorPreview type="csel" expression='format == "maven2"' />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(mockFetchRepositories).toHaveBeenCalled();
      });
    });

    it('displays error when repository fetch fails', async () => {
      mockFetchRepositories.mockRejectedValue(new Error('Failed to load repositories'));

      render(
        <ContentSelectorPreview type="csel" expression='format == "maven2"' />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(screen.getByText('Failed to load repositories')).toBeInTheDocument();
      });
    });
  });

  describe('preview functionality', () => {
    it('calls previewContentSelector when Preview button is clicked', async () => {
      

      render(
        <ContentSelectorPreview type="csel" expression='format == "maven2"' />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(mockFetchRepositories).toHaveBeenCalled();
      });

      const previewButton = screen.getByRole('button', { name: /Preview/i });
      await userEvent.click(previewButton);

      await waitFor(() => {
        expect(mockPreviewContentSelector).toHaveBeenCalledWith(
          '*',
          'csel',
          'format == "maven2"'
        );
      });
    });

    it('displays preview results', async () => {
      

      render(
        <ContentSelectorPreview type="csel" expression='format == "maven2"' />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(mockFetchRepositories).toHaveBeenCalled();
      });

      const previewButton = screen.getByRole('button', { name: /Preview/i });
      await userEvent.click(previewButton);

      await waitFor(() => {
        expect(screen.getByText('/org/example/artifact-1.0.jar')).toBeInTheDocument();
        expect(screen.getByText('/org/example/artifact-2.0.jar')).toBeInTheDocument();
      });
    });

    it('shows loading state while preview is in progress', async () => {
      

      // Make the preview take some time
      mockPreviewContentSelector.mockImplementation(
        () =>
          new Promise((resolve) => {
            setTimeout(() => resolve(['/test.jar']), 100);
          })
      );

      render(
        <ContentSelectorPreview type="csel" expression='format == "maven2"' />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(mockFetchRepositories).toHaveBeenCalled();
      });

      const previewButton = screen.getByRole('button', { name: /Preview/i });
      await userEvent.click(previewButton);

      expect(screen.getByText('Loading...')).toBeInTheDocument();
    });

    it('displays error when preview fails', async () => {
      
      mockPreviewContentSelector.mockRejectedValue(new Error('Preview failed'));

      render(
        <ContentSelectorPreview type="csel" expression='format == "maven2"' />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(mockFetchRepositories).toHaveBeenCalled();
      });

      const previewButton = screen.getByRole('button', { name: /Preview/i });
      await userEvent.click(previewButton);

      await waitFor(() => {
        expect(screen.getByText('Preview failed')).toBeInTheDocument();
      });
    });

    it('disables preview button when expression is empty', async () => {
      render(<ContentSelectorPreview type="csel" expression="" />, {
        wrapper: TestWrapper,
      });

      await waitFor(() => {
        expect(mockFetchRepositories).toHaveBeenCalled();
      });

      const previewButton = screen.getByRole('button', { name: /Preview/i });
      expect(previewButton).toBeDisabled();
    });
  });

  describe('filter functionality', () => {
    it('renders filter input', () => {
      render(
        <ContentSelectorPreview type="csel" expression='format == "maven2"' />,
        { wrapper: TestWrapper }
      );

      expect(screen.getByPlaceholderText('Filter results...')).toBeInTheDocument();
    });

    it('filters results based on input', async () => {
      

      render(
        <ContentSelectorPreview type="csel" expression='format == "maven2"' />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(mockFetchRepositories).toHaveBeenCalled();
      });

      // Click preview to load results
      const previewButton = screen.getByRole('button', { name: /Preview/i });
      await userEvent.click(previewButton);

      await waitFor(() => {
        expect(screen.getByText('/org/example/artifact-1.0.jar')).toBeInTheDocument();
      });

      // Filter results
      const filterInput = screen.getByPlaceholderText('Filter results...');
      await userEvent.type(filterInput, '1.0');

      await waitFor(() => {
        expect(screen.getByText('/org/example/artifact-1.0.jar')).toBeInTheDocument();
        expect(screen.queryByText('/org/example/artifact-2.0.jar')).not.toBeInTheDocument();
      });
    });
  });

  describe('empty states', () => {
    it('shows empty message when no results match', async () => {
      
      mockPreviewContentSelector.mockResolvedValue([]);

      render(
        <ContentSelectorPreview type="csel" expression='format == "maven2"' />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(mockFetchRepositories).toHaveBeenCalled();
      });

      const previewButton = screen.getByRole('button', { name: /Preview/i });
      await userEvent.click(previewButton);

      await waitFor(() => {
        expect(
          screen.getByText('No content in repositories matched the expression')
        ).toBeInTheDocument();
      });
    });

    it('shows filter no-match message when filter excludes all results', async () => {
      

      render(
        <ContentSelectorPreview type="csel" expression='format == "maven2"' />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(mockFetchRepositories).toHaveBeenCalled();
      });

      // Click preview to load results
      const previewButton = screen.getByRole('button', { name: /Preview/i });
      await userEvent.click(previewButton);

      await waitFor(() => {
        expect(screen.getByText('/org/example/artifact-1.0.jar')).toBeInTheDocument();
      });

      // Filter with non-matching text
      const filterInput = screen.getByPlaceholderText('Filter results...');
      await userEvent.type(filterInput, 'nonexistent');

      await waitFor(() => {
        expect(screen.getByText('No results match your filter')).toBeInTheDocument();
      });
    });
  });
});


