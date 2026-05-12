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
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import '@testing-library/jest-dom';

import { InRepositorySearch } from '../InRepositorySearch';

const mockSearchInRepository = jest.fn();

// Mock the browse API
jest.mock('../browse.api', () => ({
  searchInRepository: (...args) => mockSearchInRepository(...args),
}));

// Helper to wrap components with Radix Theme
function renderWithTheme(ui) {
  return render(<Theme>{ui}</Theme>);
}

describe('InRepositorySearch', () => {
  const mockOnSelectResult = jest.fn();
  const repositoryName = 'maven-central';

  const mockSearchResults = [
    {
      id: '1',
      repository: 'maven-central',
      format: 'maven2',
      group: 'org.apache.commons',
      name: 'commons-lang3',
      version: '3.12.0',
    },
    {
      id: '2',
      repository: 'maven-central',
      format: 'maven2',
      group: 'org.apache.commons',
      name: 'commons-io',
      version: '2.11.0',
    },
  ];

  beforeEach(() => {
    jest.clearAllMocks();
    jest.useFakeTimers();
    mockSearchInRepository.mockResolvedValue(mockSearchResults);
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  describe('rendering', () => {
    it('renders search input with placeholder', () => {
      renderWithTheme(
        <InRepositorySearch
          repositoryName={repositoryName}
          onSelectResult={mockOnSelectResult}
        />
      );

      expect(screen.getByPlaceholderText(`Search in ${repositoryName}...`)).toBeInTheDocument();
    });

    it('renders with correct aria attributes', () => {
      renderWithTheme(
        <InRepositorySearch
          repositoryName={repositoryName}
          onSelectResult={mockOnSelectResult}
        />
      );

      const input = screen.getByRole('combobox');
      expect(input).toHaveAttribute('aria-label', `Search in ${repositoryName}`);
      expect(input).toHaveAttribute('aria-autocomplete', 'list');
      expect(input).toHaveAttribute('aria-expanded', 'false');
    });
  });

  describe('search behavior', () => {
    it('does not search when query is less than 2 characters', async () => {
      renderWithTheme(
        <InRepositorySearch
          repositoryName={repositoryName}
          onSelectResult={mockOnSelectResult}
        />
      );

      const input = screen.getByRole('combobox');
      fireEvent.change(input, { target: { value: 'a' } });

      await act(async () => {
        jest.advanceTimersByTime(400);
      });

      expect(mockSearchInRepository).not.toHaveBeenCalled();
    });

    it('debounces search by 300ms', async () => {
      renderWithTheme(
        <InRepositorySearch
          repositoryName={repositoryName}
          onSelectResult={mockOnSelectResult}
        />
      );

      const input = screen.getByRole('combobox');
      fireEvent.change(input, { target: { value: 'commons' } });

      // Before debounce
      expect(mockSearchInRepository).not.toHaveBeenCalled();

      // After debounce (300ms)
      await act(async () => {
        jest.advanceTimersByTime(300);
      });

      expect(mockSearchInRepository).toHaveBeenCalledWith(repositoryName, 'commons', 20);
    });

    it('shows search results in dropdown', async () => {
      renderWithTheme(
        <InRepositorySearch
          repositoryName={repositoryName}
          onSelectResult={mockOnSelectResult}
        />
      );

      const input = screen.getByRole('combobox');
      fireEvent.change(input, { target: { value: 'commons' } });

      await act(async () => {
        jest.advanceTimersByTime(300);
      });

      await waitFor(() => {
        expect(screen.getByText('org.apache.commons:commons-lang3')).toBeInTheDocument();
        expect(screen.getByText('org.apache.commons:commons-io')).toBeInTheDocument();
      });
    });

    it('shows "No results" message when search returns empty', async () => {
      mockSearchInRepository.mockResolvedValue([]);

      renderWithTheme(
        <InRepositorySearch
          repositoryName={repositoryName}
          onSelectResult={mockOnSelectResult}
        />
      );

      const input = screen.getByRole('combobox');
      fireEvent.change(input, { target: { value: 'nonexistent' } });

      await act(async () => {
        jest.advanceTimersByTime(300);
      });

      await waitFor(() => {
        expect(screen.getByText(/No results found for "nonexistent"/)).toBeInTheDocument();
      });
    });

    it('shows error message when search fails', async () => {
      mockSearchInRepository.mockRejectedValue(new Error('Network error'));

      renderWithTheme(
        <InRepositorySearch
          repositoryName={repositoryName}
          onSelectResult={mockOnSelectResult}
        />
      );

      const input = screen.getByRole('combobox');
      fireEvent.change(input, { target: { value: 'commons' } });

      await act(async () => {
        jest.advanceTimersByTime(300);
      });

      await waitFor(() => {
        expect(screen.getByText('Search failed. Please try again.')).toBeInTheDocument();
      });
    });
  });

  describe('result selection', () => {
    it('calls onSelectResult when clicking a result', async () => {
      renderWithTheme(
        <InRepositorySearch
          repositoryName={repositoryName}
          onSelectResult={mockOnSelectResult}
        />
      );

      const input = screen.getByRole('combobox');
      fireEvent.change(input, { target: { value: 'commons' } });

      await act(async () => {
        jest.advanceTimersByTime(300);
      });

      await waitFor(() => {
        expect(screen.getByText('org.apache.commons:commons-lang3')).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText('org.apache.commons:commons-lang3'));

      expect(mockOnSelectResult).toHaveBeenCalledWith('org/apache/commons/commons-lang3/3.12.0', expect.anything());
    });

    it('clears search and closes dropdown after selection', async () => {
      renderWithTheme(
        <InRepositorySearch
          repositoryName={repositoryName}
          onSelectResult={mockOnSelectResult}
        />
      );

      const input = screen.getByRole('combobox');
      fireEvent.change(input, { target: { value: 'commons' } });

      await act(async () => {
        jest.advanceTimersByTime(300);
      });

      await waitFor(() => {
        expect(screen.getByText('org.apache.commons:commons-lang3')).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText('org.apache.commons:commons-lang3'));

      expect(input.value).toBe('');
      expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
    });
  });

  describe('keyboard navigation', () => {
    it('navigates results with arrow keys', async () => {
      renderWithTheme(
        <InRepositorySearch
          repositoryName={repositoryName}
          onSelectResult={mockOnSelectResult}
        />
      );

      const input = screen.getByRole('combobox');
      fireEvent.change(input, { target: { value: 'commons' } });

      await act(async () => {
        jest.advanceTimersByTime(300);
      });

      await waitFor(() => {
        expect(screen.getByText('org.apache.commons:commons-lang3')).toBeInTheDocument();
      });

      // Press ArrowDown to select first result
      fireEvent.keyDown(input, { key: 'ArrowDown' });

      const firstResult = screen.getByText('org.apache.commons:commons-lang3').closest('[role="option"]');
      expect(firstResult).toHaveClass('in-repo-search__result-item--selected');

      // Press ArrowDown again to select second result
      fireEvent.keyDown(input, { key: 'ArrowDown' });

      const secondResult = screen.getByText('org.apache.commons:commons-io').closest('[role="option"]');
      expect(secondResult).toHaveClass('in-repo-search__result-item--selected');
    });

    it('selects result with Enter key', async () => {
      renderWithTheme(
        <InRepositorySearch
          repositoryName={repositoryName}
          onSelectResult={mockOnSelectResult}
        />
      );

      const input = screen.getByRole('combobox');
      fireEvent.change(input, { target: { value: 'commons' } });

      await act(async () => {
        jest.advanceTimersByTime(300);
      });

      await waitFor(() => {
        expect(screen.getByText('org.apache.commons:commons-lang3')).toBeInTheDocument();
      });

      fireEvent.keyDown(input, { key: 'ArrowDown' });
      fireEvent.keyDown(input, { key: 'Enter' });

      expect(mockOnSelectResult).toHaveBeenCalledWith('org/apache/commons/commons-lang3/3.12.0', expect.anything());
    });

    it('closes dropdown with Escape key', async () => {
      renderWithTheme(
        <InRepositorySearch
          repositoryName={repositoryName}
          onSelectResult={mockOnSelectResult}
        />
      );

      const input = screen.getByRole('combobox');
      fireEvent.change(input, { target: { value: 'commons' } });

      await act(async () => {
        jest.advanceTimersByTime(300);
      });

      await waitFor(() => {
        expect(screen.getByText('org.apache.commons:commons-lang3')).toBeInTheDocument();
      });

      fireEvent.keyDown(input, { key: 'Escape' });

      expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
    });
  });

  describe('clear functionality', () => {
    it('shows clear button when there is input', async () => {
      renderWithTheme(
        <InRepositorySearch
          repositoryName={repositoryName}
          onSelectResult={mockOnSelectResult}
        />
      );

      const input = screen.getByRole('combobox');
      fireEvent.change(input, { target: { value: 'commons' } });

      await act(async () => {
        jest.advanceTimersByTime(300);
      });

      expect(screen.getByLabelText('Clear search')).toBeInTheDocument();
    });

    it('clears input and results when clicking clear button', async () => {
      renderWithTheme(
        <InRepositorySearch
          repositoryName={repositoryName}
          onSelectResult={mockOnSelectResult}
        />
      );

      const input = screen.getByRole('combobox');
      fireEvent.change(input, { target: { value: 'commons' } });

      await act(async () => {
        jest.advanceTimersByTime(300);
      });

      await waitFor(() => {
        expect(screen.getByText('org.apache.commons:commons-lang3')).toBeInTheDocument();
      });

      fireEvent.click(screen.getByLabelText('Clear search'));

      expect(input.value).toBe('');
      expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
    });
  });
});
