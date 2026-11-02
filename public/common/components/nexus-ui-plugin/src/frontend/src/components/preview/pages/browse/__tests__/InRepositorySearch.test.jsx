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

    it('issues a single search for a burst of keystrokes, using the final query', async () => {
      renderWithTheme(
        <InRepositorySearch
          repositoryName={repositoryName}
          onSelectResult={mockOnSelectResult}
        />
      );

      const input = screen.getByRole('combobox');
      for (const value of ['co', 'com', 'comm', 'commons']) {
        fireEvent.change(input, { target: { value } });
        await act(async () => {
          jest.advanceTimersByTime(100);
        });
      }

      expect(mockSearchInRepository).not.toHaveBeenCalled();

      await act(async () => {
        jest.advanceTimersByTime(300);
      });

      expect(mockSearchInRepository).toHaveBeenCalledTimes(1);
      expect(mockSearchInRepository).toHaveBeenCalledWith(repositoryName, 'commons', 20);
    });

    it('closes the dropdown immediately when the query is deleted below the minimum length', async () => {
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

      // No timer advance: shortening the query must close the dropdown at once rather than
      // leaving stale results visible for another debounce interval.
      fireEvent.change(input, { target: { value: 'c' } });

      expect(screen.queryByText('org.apache.commons:commons-lang3')).not.toBeInTheDocument();
      expect(input).toHaveAttribute('aria-expanded', 'false');
    });

    it('searches again when the same query is re-entered after dropping below the minimum length', async () => {
      renderWithTheme(
        <InRepositorySearch
          repositoryName={repositoryName}
          onSelectResult={mockOnSelectResult}
        />
      );

      const input = screen.getByRole('combobox');
      fireEvent.change(input, { target: { value: 'co' } });
      await act(async () => {
        jest.advanceTimersByTime(300);
      });
      await waitFor(() => {
        expect(screen.getByText('org.apache.commons:commons-lang3')).toBeInTheDocument();
      });

      // Delete below the minimum and retype the same query inside a single debounce window, so
      // the query alone never leaves 'co'.
      fireEvent.change(input, { target: { value: 'c' } });
      expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
      await act(async () => {
        jest.advanceTimersByTime(50);
      });
      expect(mockSearchInRepository).toHaveBeenCalledTimes(1);

      fireEvent.change(input, { target: { value: 'co' } });
      await act(async () => {
        jest.advanceTimersByTime(300);
      });

      // Returning to a valid query has to show that query's results again. Staying empty because
      // the debounced value happens to equal the previously published one leaves the user with a
      // valid query in the field and nothing to select.
      expect(mockSearchInRepository).toHaveBeenCalledTimes(2);
      expect(mockSearchInRepository).toHaveBeenLastCalledWith(repositoryName, 'co', 20);
      await waitFor(() => {
        expect(screen.getByText('org.apache.commons:commons-lang3')).toBeInTheDocument();
      });
      expect(input).toHaveAttribute('aria-expanded', 'true');
    });

    it('searches again when the same query is retyped right after clearing', async () => {
      renderWithTheme(
        <InRepositorySearch
          repositoryName={repositoryName}
          onSelectResult={mockOnSelectResult}
        />
      );

      const input = screen.getByRole('combobox');
      fireEvent.change(input, { target: { value: 'co' } });
      await act(async () => {
        jest.advanceTimersByTime(300);
      });
      await waitFor(() => {
        expect(screen.getByText('org.apache.commons:commons-lang3')).toBeInTheDocument();
      });

      fireEvent.click(screen.getByLabelText('Clear search'));

      // Clearing closes at once, without waiting out the debounce interval.
      expect(input).toHaveValue('');
      expect(screen.queryByRole('listbox')).not.toBeInTheDocument();

      // Retyping the same query inside the interval, so the query alone is unchanged.
      await act(async () => {
        jest.advanceTimersByTime(50);
      });
      expect(mockSearchInRepository).toHaveBeenCalledTimes(1);
      fireEvent.change(input, { target: { value: 'co' } });
      await act(async () => {
        jest.advanceTimersByTime(300);
      });

      expect(mockSearchInRepository).toHaveBeenCalledTimes(2);
      expect(mockSearchInRepository).toHaveBeenLastCalledWith(repositoryName, 'co', 20);
      await waitFor(() => {
        expect(screen.getByText('org.apache.commons:commons-lang3')).toBeInTheDocument();
      });
      expect(input).toHaveAttribute('aria-expanded', 'true');
    });

    it('searches again when the same query is retyped right after picking a result', async () => {
      renderWithTheme(
        <InRepositorySearch
          repositoryName={repositoryName}
          onSelectResult={mockOnSelectResult}
        />
      );

      const input = screen.getByRole('combobox');
      fireEvent.change(input, { target: { value: 'co' } });
      await act(async () => {
        jest.advanceTimersByTime(300);
      });
      await waitFor(() => {
        expect(screen.getByText('org.apache.commons:commons-lang3')).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText('org.apache.commons:commons-lang3'));

      // Picking a result empties the field and closes the dropdown at once.
      expect(input).toHaveValue('');
      expect(screen.queryByRole('listbox')).not.toBeInTheDocument();

      await act(async () => {
        jest.advanceTimersByTime(50);
      });
      expect(mockSearchInRepository).toHaveBeenCalledTimes(1);
      fireEvent.change(input, { target: { value: 'co' } });
      await act(async () => {
        jest.advanceTimersByTime(300);
      });

      expect(mockSearchInRepository).toHaveBeenCalledTimes(2);
      expect(mockSearchInRepository).toHaveBeenLastCalledWith(repositoryName, 'co', 20);
      await waitFor(() => {
        expect(screen.getByText('org.apache.commons:commons-lang3')).toBeInTheDocument();
      });
      expect(input).toHaveAttribute('aria-expanded', 'true');
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

  describe('concurrent requests', () => {
    function deferred() {
      let resolve;
      let reject;
      const promise = new Promise((res, rej) => {
        resolve = res;
        reject = rej;
      });
      return { promise, resolve, reject };
    }

    const mavenResults = [
      {
        id: 'm1',
        repository: 'maven-central',
        format: 'maven2',
        group: 'org.apache.commons',
        name: 'commons-lang3',
        version: '3.12.0',
      },
    ];

    const npmResults = [
      {
        id: 'n1',
        repository: 'npm-proxy',
        format: 'npm',
        group: 'types',
        name: 'lodash',
        version: '4.17.21',
      },
    ];

    function renderWithRepository(initialRepository) {
      const view = renderWithTheme(
        <InRepositorySearch
          repositoryName={initialRepository}
          onSelectResult={mockOnSelectResult}
        />
      );

      return {
        ...view,
        switchRepository: (nextRepository) =>
          view.rerender(
            <Theme>
              <InRepositorySearch
                repositoryName={nextRepository}
                onSelectResult={mockOnSelectResult}
              />
            </Theme>
          ),
      };
    }

    // Type a query, let it settle, then switch repositories while the first request
    // is still in flight. Leaves both requests pending.
    async function switchWhileInFlight(slow, fast) {
      mockSearchInRepository.mockImplementation((repo) =>
        repo === 'maven-central' ? slow.promise : fast.promise
      );

      const { switchRepository } = renderWithRepository('maven-central');

      fireEvent.change(screen.getByRole('combobox'), { target: { value: 'commons' } });
      await act(async () => {
        jest.advanceTimersByTime(300);
      });
      expect(mockSearchInRepository).toHaveBeenCalledWith('maven-central', 'commons', 20);

      switchRepository('npm-proxy');
      await act(async () => {});
      expect(mockSearchInRepository).toHaveBeenCalledWith('npm-proxy', 'commons', 20);
    }

    it('ignores an out-of-order response from the previously selected repository', async () => {
      const slowMaven = deferred();
      const fastNpm = deferred();
      await switchWhileInFlight(slowMaven, fastNpm);

      // The newly selected repository answers first.
      await act(async () => {
        fastNpm.resolve(npmResults);
      });
      expect(screen.getByText('types:lodash')).toBeInTheDocument();

      // The abandoned request answers late and must not overwrite current results.
      await act(async () => {
        slowMaven.resolve(mavenResults);
      });

      expect(screen.getByText('types:lodash')).toBeInTheDocument();
      expect(screen.queryByText('org.apache.commons:commons-lang3')).not.toBeInTheDocument();
    });

    it('ignores a late failure from the previously selected repository', async () => {
      const slowMaven = deferred();
      const fastNpm = deferred();
      await switchWhileInFlight(slowMaven, fastNpm);

      await act(async () => {
        fastNpm.resolve(npmResults);
      });
      expect(screen.getByText('types:lodash')).toBeInTheDocument();

      await act(async () => {
        slowMaven.reject(new Error('Network error'));
      });

      expect(screen.queryByText('Search failed. Please try again.')).not.toBeInTheDocument();
      expect(screen.getByText('types:lodash')).toBeInTheDocument();
    });

    it('does not clear the loading state when an abandoned request settles', async () => {
      const slowMaven = deferred();
      const slowNpm = deferred();
      await switchWhileInFlight(slowMaven, slowNpm);

      // npm-proxy is still in flight, so the spinner replaces the clear button.
      expect(screen.queryByLabelText('Clear search')).not.toBeInTheDocument();

      await act(async () => {
        slowMaven.resolve(mavenResults);
      });

      // The abandoned request settling must not signal that npm-proxy finished.
      expect(screen.queryByLabelText('Clear search')).not.toBeInTheDocument();
    });

    it('ignores an obsolete response after the same query is re-entered', async () => {
      const firstRequest = deferred();
      const secondRequest = deferred();
      let calls = 0;
      mockSearchInRepository.mockImplementation(() =>
        ++calls === 1 ? firstRequest.promise : secondRequest.promise
      );

      renderWithRepository('maven-central');

      const input = screen.getByRole('combobox');
      fireEvent.change(input, { target: { value: 'commons' } });
      await act(async () => {
        jest.advanceTimersByTime(300);
      });
      expect(mockSearchInRepository).toHaveBeenCalledTimes(1);

      // Discard the results and return to the same query while the first request is still running,
      // so re-entry has to issue a second request for a query already in flight.
      fireEvent.change(input, { target: { value: 'c' } });
      await act(async () => {
        jest.advanceTimersByTime(50);
      });
      fireEvent.change(input, { target: { value: 'commons' } });
      await act(async () => {
        jest.advanceTimersByTime(300);
      });
      expect(mockSearchInRepository).toHaveBeenCalledTimes(2);

      // The re-entered request answers first.
      await act(async () => {
        secondRequest.resolve(npmResults);
      });
      expect(screen.getByText('types:lodash')).toBeInTheDocument();

      // The superseded request answers late and must not overwrite the current results.
      await act(async () => {
        firstRequest.resolve(mavenResults);
      });
      expect(screen.getByText('types:lodash')).toBeInTheDocument();
      expect(screen.queryByText('org.apache.commons:commons-lang3')).not.toBeInTheDocument();
    });

    it('still searches the newly selected repository and shows its results', async () => {
      mockSearchInRepository.mockImplementation((repo) =>
        Promise.resolve(repo === 'maven-central' ? mavenResults : npmResults)
      );

      const { switchRepository } = renderWithRepository('maven-central');

      fireEvent.change(screen.getByRole('combobox'), { target: { value: 'commons' } });
      await act(async () => {
        jest.advanceTimersByTime(300);
      });
      expect(screen.getByText('org.apache.commons:commons-lang3')).toBeInTheDocument();

      switchRepository('npm-proxy');
      await act(async () => {});

      expect(mockSearchInRepository).toHaveBeenCalledWith('npm-proxy', 'commons', 20);
      await waitFor(() => {
        expect(screen.getByText('types:lodash')).toBeInTheDocument();
      });
      expect(screen.queryByText('org.apache.commons:commons-lang3')).not.toBeInTheDocument();
    });

    it('ignores a response that lands after the query dropped below the minimum length', async () => {
      const request = deferred();
      mockSearchInRepository.mockImplementation(() => request.promise);

      renderWithRepository('maven-central');

      const input = screen.getByRole('combobox');
      fireEvent.change(input, { target: { value: 'commons' } });
      await act(async () => {
        jest.advanceTimersByTime(300);
      });
      expect(mockSearchInRepository).toHaveBeenCalledTimes(1);

      // Delete back below the minimum length while the request is still running.
      fireEvent.change(input, { target: { value: 'c' } });
      await act(async () => {});
      expect(input).toHaveAttribute('aria-expanded', 'false');

      // The abandoned request answers late; it must not reopen the dropdown over a query too
      // short to have asked for it.
      await act(async () => {
        request.resolve(mavenResults);
      });

      expect(input).toHaveAttribute('aria-expanded', 'false');
      expect(screen.queryByText('org.apache.commons:commons-lang3')).not.toBeInTheDocument();
    });

    it('ignores a response that lands after the query was cleared', async () => {
      const request = deferred();
      mockSearchInRepository.mockImplementation(() => request.promise);

      renderWithRepository('maven-central');

      const input = screen.getByRole('combobox');
      fireEvent.change(input, { target: { value: 'commons' } });
      await act(async () => {
        jest.advanceTimersByTime(300);
      });

      fireEvent.change(input, { target: { value: '' } });
      await act(async () => {});

      await act(async () => {
        request.resolve(mavenResults);
      });

      expect(input).toHaveAttribute('aria-expanded', 'false');
      expect(screen.queryByText('org.apache.commons:commons-lang3')).not.toBeInTheDocument();
    });

    it('clears the loading state when the query drops below the minimum length', async () => {
      const request = deferred();
      mockSearchInRepository.mockImplementation(() => request.promise);

      renderWithRepository('maven-central');

      const input = screen.getByRole('combobox');
      fireEvent.change(input, { target: { value: 'commons' } });
      await act(async () => {
        jest.advanceTimersByTime(300);
      });
      // The spinner replaces the clear button while the request is in flight.
      expect(screen.queryByLabelText('Clear search')).not.toBeInTheDocument();

      // Collapsing abandons the request, so its `finally` is guarded out and the collapse itself
      // has to stop the spinner.
      fireEvent.change(input, { target: { value: 'c' } });
      await act(async () => {});

      expect(screen.getByLabelText('Clear search')).toBeInTheDocument();
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
