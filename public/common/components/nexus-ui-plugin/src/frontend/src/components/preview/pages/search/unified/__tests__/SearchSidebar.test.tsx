/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions
 * Sonatype and Sonatype Nexus are trademarks of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation.
 * M2Eclipse is a trademark of the Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';
import { SearchSidebar } from '../SearchSidebar';
import * as searchFilters from '../searchFilters';

function renderWithTheme(ui) {
  return render(<Theme>{ui}</Theme>);
}

describe('SearchSidebar', () => {
  const defaultProps = {
    selectedFormat: '' as const,
    onFormatChange: jest.fn(),
    filters: {},
    onFilterChange: jest.fn(),
    onReset: jest.fn(),
    repositories: ['repo1', 'repo2', 'maven-central'],
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('reset button', () => {
    it('renders Reset filters button', () => {
      renderWithTheme(<SearchSidebar {...defaultProps} />);
      expect(screen.getByText('Reset filters')).toBeInTheDocument();
    });

    it('calls onReset and onFormatChange when Reset filters is clicked', async () => {
      const onReset = jest.fn();
      const onFormatChange = jest.fn();
      renderWithTheme(
        <SearchSidebar
          {...defaultProps}
          selectedFormat="maven"
          onFormatChange={onFormatChange}
          onReset={onReset}
        />,
      );
      await userEvent.click(screen.getByText('Reset filters'));
      expect(onFormatChange).toHaveBeenCalledWith('');
      expect(onReset).toHaveBeenCalled();
    });
  });

  describe('format section', () => {
    it('renders Format section title', () => {
      renderWithTheme(<SearchSidebar {...defaultProps} />);
      expect(screen.getByText('Format')).toBeInTheDocument();
    });

    it('renders format dropdown with All formats default', () => {
      renderWithTheme(<SearchSidebar {...defaultProps} />);
      const trigger = screen.getByTestId('format-dropdown-trigger');
      expect(trigger).toHaveTextContent('All formats');
      expect(trigger).toHaveAttribute('aria-label', 'Format: All formats');
    });

    it('shows selected format in dropdown trigger', () => {
      renderWithTheme(
        <SearchSidebar {...defaultProps} selectedFormat="maven" />,
      );
      const trigger = screen.getByTestId('format-dropdown-trigger');
      expect(trigger).toHaveTextContent('Maven');
    });

    it('shows format-specific filters when format is selected', () => {
      renderWithTheme(
        <SearchSidebar
          {...defaultProps}
          selectedFormat="maven"
          filters={{}}
        />,
      );
      expect(screen.getByText('Filters')).toBeInTheDocument();
      expect(screen.getByLabelText(/Group ID/i)).toBeInTheDocument();
    });
  });

  describe('repository section', () => {
    it('renders Repository section title', () => {
      renderWithTheme(<SearchSidebar {...defaultProps} />);
      expect(screen.getByText('Repository')).toBeInTheDocument();
    });

    it('renders repository dropdown with All repositories default', () => {
      renderWithTheme(<SearchSidebar {...defaultProps} />);
      const trigger = screen.getByTestId('repository-dropdown-trigger');
      expect(trigger).toHaveTextContent('All repositories');
      expect(trigger).toHaveAttribute(
        'aria-label',
        'Repository: All repositories',
      );
    });

    it('shows selected repository in trigger when filter is set', () => {
      renderWithTheme(
        <SearchSidebar
          {...defaultProps}
          filters={{ repository: 'maven-central' }}
        />,
      );
      const trigger = screen.getByTestId('repository-dropdown-trigger');
      expect(trigger).toHaveTextContent('maven-central');
    });
  });

  describe('focus preservation', () => {
    it('is wrapped with React.memo to prevent re-renders when parent updates with identical props', () => {
      const REACT_MEMO_TYPE = Symbol.for('react.memo');
      expect((SearchSidebar as unknown as { $$typeof: symbol }).$$typeof).toBe(REACT_MEMO_TYPE);
    });

    it('restores focus to the active filter input after disabled transitions false→true→false', () => {
      const { rerender } = renderWithTheme(
        <SearchSidebar
          {...defaultProps}
          selectedFormat="maven"
          filters={{}}
          disabled={false}
        />,
      );

      const input = screen.getByLabelText(/Artifact ID/i) as HTMLInputElement;
      input.focus();
      expect(document.activeElement).toBe(input);

      // Simulate search starting (disabled=true loses focus on the input)
      rerender(
        <Theme>
          <SearchSidebar
            {...defaultProps}
            selectedFormat="maven"
            filters={{}}
            disabled={true}
          />
        </Theme>,
      );

      // Simulate search completing (disabled back to false)
      rerender(
        <Theme>
          <SearchSidebar
            {...defaultProps}
            selectedFormat="maven"
            filters={{}}
            disabled={false}
          />
        </Theme>,
      );

      // Focus must be restored to the input that was active before the search
      expect(document.activeElement).toBe(input);
    });

    it('restores focus even when browser fires blur on the input as it becomes disabled', () => {
      const { rerender } = renderWithTheme(
        <SearchSidebar
          {...defaultProps}
          selectedFormat="maven"
          filters={{}}
          disabled={false}
        />,
      );

      const input = screen.getByLabelText(/Artifact ID/i) as HTMLInputElement;
      input.focus();
      expect(document.activeElement).toBe(input);

      // Browser fires blur on the input when disabled=true is applied
      rerender(
        <Theme>
          <SearchSidebar
            {...defaultProps}
            selectedFormat="maven"
            filters={{}}
            disabled={true}
          />
        </Theme>,
      );
      // Explicitly fire blur as a real browser would when the input becomes disabled
      fireEvent.blur(input);

      // Re-enable — focus should still be restored despite the blur event
      rerender(
        <Theme>
          <SearchSidebar
            {...defaultProps}
            selectedFormat="maven"
            filters={{}}
            disabled={false}
          />
        </Theme>,
      );

      expect(document.activeElement).toBe(input);
    });

    it('preserves local input value while search is in-flight (disabled=true)', () => {
      const onFilterChange = jest.fn();
      const { rerender } = renderWithTheme(
        <SearchSidebar
          {...defaultProps}
          selectedFormat="maven"
          filters={{}}
          onFilterChange={onFilterChange}
          disabled={false}
        />,
      );

      const input = screen.getByLabelText(/Artifact ID/i) as HTMLInputElement;
      fireEvent.change(input, { target: { value: 'commons-lang' } });
      expect(input.value).toBe('commons-lang');

      // Parent triggers disabled=true (search in-flight) but does NOT update filters prop
      rerender(
        <Theme>
          <SearchSidebar
            {...defaultProps}
            selectedFormat="maven"
            filters={{}}
            onFilterChange={onFilterChange}
            disabled={true}
          />
        </Theme>,
      );

      // Value must be preserved from local state, not reset to '' by the empty filters prop
      expect(input.value).toBe('commons-lang');
    });
  });

  describe('external filter changes (browser back/forward)', () => {
    it('updates the input when the filters prop transitions to a new value', () => {
      const { rerender } = renderWithTheme(
        <SearchSidebar
          {...defaultProps}
          selectedFormat="maven"
          filters={{ artifactId: 'guava' }}
        />,
      );

      const input = screen.getByLabelText(/Artifact ID/i) as HTMLInputElement;
      expect(input.value).toBe('guava');

      // User types a new value (local override diverges from prop)
      fireEvent.change(input, { target: { value: 'spring-core' } });
      expect(input.value).toBe('spring-core');
      fireEvent.blur(input);

      // Browser Back: parent rehydrates the machine, filters prop transitions
      // to a different value than what the user typed.
      rerender(
        <Theme>
          <SearchSidebar
            {...defaultProps}
            selectedFormat="maven"
            filters={{ artifactId: 'commons-lang3' }}
          />
        </Theme>,
      );

      // Input must reflect the restored prop value, not the stale typed value.
      expect(input.value).toBe('commons-lang3');
    });

    it('clears the input when an external change removes the filter', () => {
      const { rerender } = renderWithTheme(
        <SearchSidebar
          {...defaultProps}
          selectedFormat="maven"
          filters={{ artifactId: 'commons-lang3' }}
        />,
      );

      const input = screen.getByLabelText(/Artifact ID/i) as HTMLInputElement;
      fireEvent.change(input, { target: { value: 'spring-core' } });
      fireEvent.blur(input);

      // Back to a state with no artifactId filter
      rerender(
        <Theme>
          <SearchSidebar {...defaultProps} selectedFormat="maven" filters={{}} />
        </Theme>,
      );

      expect(input.value).toBe('');
    });

    it('does not disrupt the focused input during an external change', () => {
      const { rerender } = renderWithTheme(
        <SearchSidebar
          {...defaultProps}
          selectedFormat="maven"
          filters={{ artifactId: 'commons-lang3' }}
        />,
      );

      const input = screen.getByLabelText(/Artifact ID/i) as HTMLInputElement;
      input.focus();
      fireEvent.change(input, { target: { value: 'in-progress' } });
      expect(document.activeElement).toBe(input);

      // A filters prop change arrives while the user is actively editing.
      rerender(
        <Theme>
          <SearchSidebar
            {...defaultProps}
            selectedFormat="maven"
            filters={{ artifactId: 'something-else' }}
          />
        </Theme>,
      );

      // The focused input keeps the user's in-progress text.
      expect(input.value).toBe('in-progress');
    });
  });

  describe('nameOrVersion filter exclusion', () => {
    it('never renders a nameOrVersion input in the sidebar even if format filters include it', () => {
      // Temporarily inject a nameOrVersion filter into maven's format-specific filters
      // to simulate a future accidental addition
      jest.spyOn(searchFilters, 'getFiltersForFormat').mockReturnValue([
        {
          id: 'nameOrVersion',
          label: 'Name or Version',
          type: 'text' as const,
          apiParam: 'name',
          placeholder: 'Filter by component name or version',
        },
        {
          id: 'groupId',
          label: 'Group ID',
          type: 'text' as const,
          apiParam: 'maven.groupId',
          placeholder: 'e.g., org.apache.commons',
        },
      ]);

      renderWithTheme(
        <SearchSidebar
          {...defaultProps}
          selectedFormat="maven"
          filters={{}}
        />,
      );

      // nameOrVersion must never appear in the sidebar — it lives in SearchResults header
      expect(
        screen.queryByPlaceholderText('Filter by component name or version'),
      ).not.toBeInTheDocument();
      // groupId is a legitimate sidebar filter and must still appear
      expect(screen.getByLabelText(/Group ID/i)).toBeInTheDocument();

      jest.restoreAllMocks();
    });
  });

  describe('sort', () => {
    it('renders no sorting control — the results-header dropdown is the only one', () => {
      renderWithTheme(<SearchSidebar {...defaultProps} />);
      expect(screen.queryByText('Sort')).not.toBeInTheDocument();
      expect(screen.queryByRole('radiogroup')).not.toBeInTheDocument();
      expect(screen.queryAllByRole('radio')).toHaveLength(0);
    });
  });

  describe('disabled state', () => {
    it('disables Reset filters button when disabled', () => {
      renderWithTheme(<SearchSidebar {...defaultProps} disabled={true} />);
      expect(screen.getByText('Reset filters').closest('button')).toBeDisabled();
    });

    it('disables format dropdown when disabled', () => {
      renderWithTheme(<SearchSidebar {...defaultProps} disabled={true} />);
      expect(screen.getByTestId('format-dropdown-trigger')).toBeDisabled();
    });

    it('disables repository dropdown when disabled', () => {
      renderWithTheme(<SearchSidebar {...defaultProps} disabled={true} />);
      expect(screen.getByTestId('repository-dropdown-trigger')).toBeDisabled();
    });
  });
});
