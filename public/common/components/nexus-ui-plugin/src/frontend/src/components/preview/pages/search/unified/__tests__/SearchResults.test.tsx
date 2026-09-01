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
import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';
import { SearchResults } from '../SearchResults';

// Helper to wrap components with Radix Theme
function renderWithTheme(ui) {
  return render(<Theme>{ui}</Theme>);
}

const mockResults = [
  {
    id: '1',
    name: 'lodash',
    format: 'npm',
    repository: 'npm-proxy',
    group: undefined,
    version: '4.17.21',
    lastUpdated: '2024-01-15T10:00:00Z',
  },
  {
    id: '2',
    name: 'spring-core',
    format: 'maven2',
    repository: 'maven-central',
    group: 'org.springframework',
    version: '6.1.0',
    lastUpdated: '2024-01-10T08:00:00Z',
  },
];

describe('SearchResults', () => {
  const defaultProps = {
    results: mockResults,
    loading: false,
    totalCount: 2,
    hasMore: false,
    onLoadMore: jest.fn(),
    onSelect: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('loading state', () => {
    it('renders loading state when loading with no results', () => {
      renderWithTheme(<SearchResults {...defaultProps} results={[]} loading={true} />);
      expect(screen.getAllByText('Components').length).toBeGreaterThan(0);
      expect(screen.queryByText(/lodash|spring-core/)).not.toBeInTheDocument();
    });

    it('shows loading indicator when loading more results', () => {
      renderWithTheme(<SearchResults {...defaultProps} loading={true} hasMore={true} />);
      expect(screen.getByText(/loading more components/i)).toBeInTheDocument();
    });
  });

  describe('empty state', () => {
    it('renders empty state when no results', () => {
      renderWithTheme(<SearchResults {...defaultProps} results={[]} totalCount={0} />);
      expect(screen.getByText(/no components found/i)).toBeInTheDocument();
    });
  });

  describe('error state', () => {
    it('renders error state with message', () => {
      renderWithTheme(
        <SearchResults {...defaultProps} results={[]} error="Search failed" />
      );
      expect(screen.getByText('Search failed')).toBeInTheDocument();
    });

    it('renders retry button when onRetry is provided', async () => {
      const onRetry = jest.fn();
      renderWithTheme(
        <SearchResults
          {...defaultProps}
          results={[]}
          error="Search failed"
          onRetry={onRetry}
        />
      );
      const retryButton = screen.getByRole('button', { name: /retry/i });
      await userEvent.click(retryButton);
      expect(onRetry).toHaveBeenCalled();
    });
  });

  describe('header', () => {
    it('renders "Components" heading', () => {
      renderWithTheme(<SearchResults {...defaultProps} />);
      expect(screen.getAllByRole('heading', { name: 'Components' }).length).toBeGreaterThan(0);
    });

    it('renders total count', () => {
      renderWithTheme(<SearchResults {...defaultProps} />);
      const countElements = screen.getAllByText('2');
      expect(countElements.length).toBeGreaterThan(0);
    });

    it('renders total count with + when hasMore is true', () => {
      renderWithTheme(
        <SearchResults {...defaultProps} hasMore={true} totalCount={50} />
      );
      expect(screen.getAllByText('50+').length).toBeGreaterThan(0);
    });

    it('renders filter input', () => {
      renderWithTheme(<SearchResults {...defaultProps} />);
      expect(screen.getAllByPlaceholderText(/filter by component name/i).length).toBeGreaterThan(0);
    });
  });

  describe('results list', () => {
    it('renders result cards for each result', () => {
      renderWithTheme(<SearchResults {...defaultProps} />);
      expect(screen.getByText(/lodash/)).toBeInTheDocument();
      expect(screen.getByText(/spring-core/)).toBeInTheDocument();
    });

    it('calls onSelect when result card is clicked', async () => {
      const onSelect = jest.fn();
      renderWithTheme(<SearchResults {...defaultProps} onSelect={onSelect} />);
      await userEvent.click(screen.getByRole('button', { name: /view details for lodash/i }));
      expect(onSelect).toHaveBeenCalledWith(mockResults[0]);
    });

    it('displays format badges', () => {
      renderWithTheme(<SearchResults {...defaultProps} />);
      expect(screen.getByText('npm')).toBeInTheDocument();
      expect(screen.getByText('maven2')).toBeInTheDocument();
    });

    it('displays version', () => {
      renderWithTheme(<SearchResults {...defaultProps} />);
      expect(screen.getByText(/4\.17\.21/)).toBeInTheDocument();
      expect(screen.getByText(/6\.1\.0/)).toBeInTheDocument();
    });

    it('displays repository names', () => {
      renderWithTheme(<SearchResults {...defaultProps} />);
      expect(screen.getByText(/npm-proxy/)).toBeInTheDocument();
      expect(screen.getByText(/maven-central/)).toBeInTheDocument();
    });

    it('displays group/namespace when available', () => {
      renderWithTheme(<SearchResults {...defaultProps} />);
      expect(screen.getByText('org.springframework')).toBeInTheDocument();
    });
  });

  describe('pagination', () => {
    it('renders pagination controls', () => {
      renderWithTheme(<SearchResults {...defaultProps} />);
      expect(screen.getAllByRole('button', { name: /first page/i }).length).toBeGreaterThan(0);
      expect(screen.getAllByRole('button', { name: /previous page/i }).length).toBeGreaterThan(0);
      expect(screen.getAllByRole('button', { name: /next page/i }).length).toBeGreaterThan(0);
      expect(screen.getAllByRole('button', { name: /last page/i }).length).toBeGreaterThan(0);
    });

    it('enables Next page button when hasMore is true', () => {
      renderWithTheme(<SearchResults {...defaultProps} hasMore={true} />);
      const nextButton = screen.getByRole('button', { name: /load more|next page/i });
      expect(nextButton).not.toBeDisabled();
    });

    it('disables Next page button when hasMore is false', () => {
      renderWithTheme(<SearchResults {...defaultProps} hasMore={false} />);
      const nextButton = screen.getByRole('button', { name: /next page/i });
      expect(nextButton).toBeDisabled();
    });

    it('calls onLoadMore when Next page button is clicked', async () => {
      const onLoadMore = jest.fn();
      renderWithTheme(
        <SearchResults {...defaultProps} hasMore={true} onLoadMore={onLoadMore} />
      );
      // When on last page with hasMore, Next button has aria-label "Load more"
      const loadMoreButton = screen.getByRole('button', { name: /load more|next page/i });
      await userEvent.click(loadMoreButton);
      expect(onLoadMore).toHaveBeenCalled();
    });

    it('renders page info', () => {
      renderWithTheme(<SearchResults {...defaultProps} totalCount={50} />);
      // TablePagination shows "Showing X of Y" - with 2 results, 20 per page: "Showing 2 of 2"
      expect(screen.getByText(/Showing/)).toBeInTheDocument();
      expect(screen.getByText(/of 2/)).toBeInTheDocument();
    });
  });

  describe('sort dropdown', () => {
    // jsdom lacks the pointer-capture / scrollIntoView APIs Radix Select touches
    // when opening.
    beforeAll(() => {
      window.HTMLElement.prototype.hasPointerCapture = jest.fn(() => false);
      window.HTMLElement.prototype.releasePointerCapture = jest.fn();
      window.HTMLElement.prototype.scrollIntoView = jest.fn();
    });

    /**
     * The header is rendered three times (mobile / tablet / desktop) because
     * visibility is CSS-driven, so every query below takes the first match.
     */
    function sortTrigger() {
      return screen.getAllByRole('combobox', { name: /^Sort:/ })[0];
    }

    /**
     * Radix Select mounts its content asynchronously (popper positioning, item
     * registration), so the open must be awaited or those updates land outside
     * act() and React warns.
     */
    async function openSortDropdown() {
      await userEvent.click(sortTrigger());
      await screen.findByRole('listbox');
    }

    it('renders the sort dropdown labelled with the active option', () => {
      renderWithTheme(
        <SearchResults {...defaultProps} sortField="lastUpdated" sortDirection="desc" />,
      );
      // The accessible name must contain the visible label text (WCAG 2.5.3),
      // which reads "sort: Last updated — Newest first".
      expect(sortTrigger()).toHaveAccessibleName('Sort: Last updated — Newest first');
    });

    // The closed trigger has no group heading to supply the field name and no
    // open list to show which direction is checked, so its text must name both
    // — the direction arrow beside it is decorative and cannot carry that.
    it.each([
      ['lastUpdated', 'desc', 'Last updated — Newest first'],
      ['lastUpdated', 'asc', 'Last updated — Oldest first'],
      ['name', 'asc', 'Name — A-Z'],
      ['name', 'desc', 'Name — Z-A'],
      ['repository', 'asc', 'Repository — A-Z'],
      ['repository', 'desc', 'Repository — Z-A'],
    ] as const)(
      'names field and direction in the closed trigger for %s %s',
      (sortField, sortDirection, label) => {
        renderWithTheme(
          <SearchResults {...defaultProps} sortField={sortField} sortDirection={sortDirection} />,
        );

        // No listbox is mounted, so this is genuinely the closed state.
        expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
        expect(sortTrigger()).toHaveTextContent(label);
        expect(sortTrigger()).toHaveAccessibleName(`Sort: ${label}`);
      },
    );

    it('defaults to last updated, newest first, when no sort props are supplied', () => {
      renderWithTheme(<SearchResults {...defaultProps} />);
      expect(sortTrigger()).toHaveTextContent('Last updated — Newest first');
    });

    it('offers a field-grouped option for every sortable field and direction', async () => {
      renderWithTheme(<SearchResults {...defaultProps} />);
      await openSortDropdown();

      // Group headings name the field; each option names its direction.
      for (const heading of ['Last updated', 'Name', 'Repository']) {
        expect(screen.getByText(heading)).toBeInTheDocument();
      }
      for (const direction of ['Newest first', 'Oldest first', 'A-Z', 'Z-A']) {
        expect(screen.getAllByRole('option', { name: direction }).length).toBeGreaterThan(0);
      }
      // 3 fields x 2 directions.
      expect(screen.getAllByRole('option')).toHaveLength(6);
    });

    it.each([
      ['Name', 'A-Z', 'name', 'asc'],
      ['Name', 'Z-A', 'name', 'desc'],
      ['Last updated', 'Newest first', 'lastUpdated', 'desc'],
      ['Last updated', 'Oldest first', 'lastUpdated', 'asc'],
    ])(
      'reports %s / %s as onSortChange(%s, %s)',
      async (_field, directionLabel, expectedField, expectedDirection) => {
        const onSortChange = jest.fn();
        // Start from a sort that none of the cases under test already equals, so
        // every selection is a genuine change.
        renderWithTheme(
          <SearchResults
            {...defaultProps}
            sortField="repository"
            sortDirection="asc"
            onSortChange={onSortChange}
          />,
        );
        await openSortDropdown();
        await userEvent.click(screen.getAllByRole('option', { name: directionLabel })[0]);

        expect(onSortChange).toHaveBeenCalledWith(expectedField, expectedDirection);
      },
    );

    it('is keyboard operable: the trigger is focusable and opens on Enter', async () => {
      renderWithTheme(
        <SearchResults {...defaultProps} sortField="lastUpdated" sortDirection="desc" />,
      );

      const trigger = sortTrigger();
      trigger.focus();
      expect(trigger).toHaveFocus();

      // Radix Select opens from the keyboard; no pointer involved.
      fireEvent.keyDown(trigger, { key: 'Enter' });
      expect(await screen.findByRole('option', { name: 'Oldest first' })).toBeInTheDocument();
    });

    it('selects an option from the keyboard', async () => {
      const onSortChange = jest.fn();
      renderWithTheme(
        <SearchResults
          {...defaultProps}
          sortField="lastUpdated"
          sortDirection="desc"
          onSortChange={onSortChange}
        />,
      );

      const trigger = sortTrigger();
      trigger.focus();
      fireEvent.keyDown(trigger, { key: 'Enter' });

      const option = await screen.findByRole('option', { name: 'Oldest first' });
      fireEvent.keyDown(option, { key: 'Enter' });

      expect(onSortChange).toHaveBeenCalledWith('lastUpdated', 'asc');
    });

    it('conveys direction with an icon alongside the text, not by colour', () => {
      const { container } = renderWithTheme(
        <SearchResults {...defaultProps} sortField="name" sortDirection="asc" />,
      );
      // lucide-react renders an <svg class="lucide lucide-arrow-up ...">
      expect(container.querySelector('svg.lucide-arrow-up')).toBeInTheDocument();
      expect(container.querySelector('svg.lucide-arrow-down')).not.toBeInTheDocument();
      // The label states the direction in words as well.
      expect(sortTrigger()).toHaveTextContent('Name — A-Z');
    });

    it('renders the dropdown even when there are no results to sort', () => {
      renderWithTheme(<SearchResults {...defaultProps} results={[]} totalCount={0} />);
      expect(screen.getByText('No components found')).toBeInTheDocument();
      expect(sortTrigger()).toBeInTheDocument();
    });
  });

  describe('filter input', () => {
    it('renders with current nameFilter value', () => {
      renderWithTheme(<SearchResults {...defaultProps} nameFilter="lodash" />);
      const filterInput = screen.getAllByPlaceholderText(/filter by component name/i)[0];
      expect(filterInput).toHaveValue('lodash');
    });

    it('calls onNameFilterChange after debounce when typing', async () => {
      jest.useFakeTimers();
      const onNameFilterChange = jest.fn();
      renderWithTheme(
        <SearchResults {...defaultProps} onNameFilterChange={onNameFilterChange} />
      );
      const filterInput = screen.getAllByPlaceholderText(/filter by component name/i)[0];
      
      // Simulate typing - each character triggers handleFilterChange
      fireEvent.change(filterInput, { target: { value: 'test' } });
      
      // Should not be called immediately (debounced)
      expect(onNameFilterChange).not.toHaveBeenCalled();
      
      // Fast-forward past debounce timeout (500ms)
      jest.advanceTimersByTime(500);
      
      expect(onNameFilterChange).toHaveBeenCalledWith('test');
      jest.useRealTimers();
    });

    it('triggers filter immediately on Enter key with initial value', () => {
      const onNameFilterChange = jest.fn();
      // Set initial nameFilter prop so localFilterValue is already set
      renderWithTheme(
        <SearchResults
          {...defaultProps}
          nameFilter="lodash"
          onNameFilterChange={onNameFilterChange}
        />
      );
      const filterInput = screen.getAllByPlaceholderText(/filter by component name/i)[0];

      // Press Enter - should trigger callback with current filter value
      fireEvent.keyDown(filterInput, { key: 'Enter' });

      // Should be called immediately
      expect(onNameFilterChange).toHaveBeenCalledWith('lodash');
    });

    it('renders the name filter input even when a specific format is selected (AT-016)', () => {
      // A deep link can carry both a format and a query; gating the input on
      // format used to leave that query with nowhere to show.
      renderWithTheme(
        <SearchResults {...defaultProps} nameFilter="commons" onNameFilterChange={jest.fn()} />,
      );
      const inputs = screen.getAllByPlaceholderText(/filter by component name or version/i);
      expect(inputs.length).toBeGreaterThan(0);
      expect(inputs[0]).toHaveValue('commons');
    });

    it('renders the name filter input with no format selected (AT-016)', () => {
      renderWithTheme(
        <SearchResults {...defaultProps} nameFilter="" onNameFilterChange={jest.fn()} />,
      );
      expect(
        screen.getAllByPlaceholderText(/filter by component name or version/i).length,
      ).toBeGreaterThan(0);
    });

    it('cancels a pending debounce on unmount', () => {
      // Clicking a result card mid-type unmounts this component with the 500ms
      // timer still pending; letting it fire pushes a filter change into the
      // parent's search machine after the user has navigated away.
      jest.useFakeTimers();
      try {
        const onNameFilterChange = jest.fn();
        const { unmount } = renderWithTheme(
          <SearchResults {...defaultProps} onNameFilterChange={onNameFilterChange} />,
        );
        const filterInput = screen.getAllByPlaceholderText(/filter by component name/i)[0];

        fireEvent.change(filterInput, { target: { value: 'test' } });
        unmount();
        jest.advanceTimersByTime(500);

        expect(onNameFilterChange).not.toHaveBeenCalled();
      } finally {
        jest.useRealTimers();
      }
    });
  });
});
