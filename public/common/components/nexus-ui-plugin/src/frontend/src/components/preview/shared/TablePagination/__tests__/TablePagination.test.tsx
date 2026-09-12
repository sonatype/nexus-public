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
import { render, screen, within, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';
import { computeMatches, getPageTokens, TablePagination } from '../TablePagination';

function renderWithTheme(ui: React.ReactElement) {
  return render(<Theme>{ui}</Theme>);
}

describe('getPageTokens', () => {
  describe('small totals (no ellipsis, up to threshold)', () => {
    it('returns [1] when totalPages=1', () => {
      expect(getPageTokens(1, 1)).toEqual([1]);
    });

    it('returns 1..5 when totalPages=5, regardless of currentPage', () => {
      expect(getPageTokens(1, 5)).toEqual([1, 2, 3, 4, 5]);
      expect(getPageTokens(3, 5)).toEqual([1, 2, 3, 4, 5]);
      expect(getPageTokens(5, 5)).toEqual([1, 2, 3, 4, 5]);
    });

    it('returns 1..10 when totalPages=10, regardless of currentPage', () => {
      const all = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
      expect(getPageTokens(1, 10)).toEqual(all);
      expect(getPageTokens(5, 10)).toEqual(all);
      expect(getPageTokens(10, 10)).toEqual(all);
    });
  });

  describe('boundary transitions (totalPages=11, smallest windowed case)', () => {
    it('shifts the window right so ~10 tiles are visible near the start', () => {
      expect(getPageTokens(1, 11)).toEqual([1, 2, 3, 4, 5, 6, 7, 8, 'ellipsis-right', 11]);
      expect(getPageTokens(4, 11)).toEqual([1, 2, 3, 4, 5, 6, 7, 8, 'ellipsis-right', 11]);
    });

    it('fills in single-page gaps in the middle instead of eliding them', () => {
      // At c=6, the window is [3..9]; the gap between 1 and 3 is just page 2,
      // and between 9 and 11 is just page 10 — so both single-hidden pages
      // are shown inline and no ellipsis appears.
      expect(getPageTokens(6, 11)).toEqual([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11]);
    });

    it('shifts the window left so ~10 tiles are visible near the end', () => {
      expect(getPageTokens(8, 11)).toEqual([1, 'ellipsis-left', 4, 5, 6, 7, 8, 9, 10, 11]);
      expect(getPageTokens(11, 11)).toEqual([1, 'ellipsis-left', 4, 5, 6, 7, 8, 9, 10, 11]);
    });
  });

  describe('large totals with 4-digit page numbers (totalPages=2133)', () => {
    // With totalPages >= 1000 the sibling count drops from 3 to 2 (5-page window)
    // so the wider 4-digit tiles still fit on a single line.

    it('near start (currentPage=1..4) — window pinned to [2..6], right ellipsis', () => {
      const expected = [1, 2, 3, 4, 5, 6, 'ellipsis-right', 2133];
      expect(getPageTokens(1, 2133)).toEqual(expected);
      expect(getPageTokens(2, 2133)).toEqual(expected);
      expect(getPageTokens(3, 2133)).toEqual(expected);
      expect(getPageTokens(4, 2133)).toEqual(expected);
    });

    it('in the middle (currentPage=42) — both ellipses, 5-page window around current', () => {
      expect(getPageTokens(42, 2133)).toEqual([
        1,
        'ellipsis-left',
        40,
        41,
        42,
        43,
        44,
        'ellipsis-right',
        2133,
      ]);
    });

    it('near end — window pinned to [2128..2132], left ellipsis', () => {
      const expected = [1, 'ellipsis-left', 2128, 2129, 2130, 2131, 2132, 2133];
      expect(getPageTokens(2131, 2133)).toEqual(expected);
      expect(getPageTokens(2132, 2133)).toEqual(expected);
      expect(getPageTokens(2133, 2133)).toEqual(expected);
    });

    it('never exceeds 9 tokens in the 4-digit windowed branch', () => {
      for (const c of [1, 2, 3, 42, 1000, 2131, 2132, 2133]) {
        expect(getPageTokens(c, 2133).length).toBeLessThanOrEqual(9);
      }
    });

    it('produces at least 8 tokens at typical (mid-range) positions', () => {
      for (const c of [42, 100, 500, 1000, 1500, 2000]) {
        expect(getPageTokens(c, 2133).length).toBeGreaterThanOrEqual(8);
      }
    });
  });
});

describe('TablePagination component — windowed inline buttons', () => {
  const baseProps = {
    currentPage: 1,
    totalPages: 5,
    itemsPerPage: 20,
    totalItems: 100,
    onPageChange: jest.fn(),
  };

  it('renders every page as an inline button when totalPages is small', () => {
    renderWithTheme(<TablePagination {...baseProps} totalPages={5} totalItems={100} />);
    const nav = screen.getByRole('navigation', { name: /pagination/i });
    for (const label of ['1', '2', '3', '4', '5']) {
      expect(within(nav).getByRole('button', { name: label })).toBeInTheDocument();
    }
  });

  it('does not render an ellipsis when totalPages <= 10', () => {
    renderWithTheme(<TablePagination {...baseProps} totalPages={5} totalItems={100} />);
    expect(screen.queryByText('…')).not.toBeInTheDocument();
  });

  it('still renders every page at totalPages=10 (upper end of "show all" threshold)', () => {
    renderWithTheme(<TablePagination {...baseProps} totalPages={10} totalItems={200} />);
    const nav = screen.getByRole('navigation', { name: /pagination/i });
    for (const label of ['1', '2', '3', '4', '5', '6', '7', '8', '9', '10']) {
      expect(within(nav).getByRole('button', { name: label })).toBeInTheDocument();
    }
    expect(within(nav).queryByText('…')).not.toBeInTheDocument();
  });

  it('collapses to windowed rendering at totalPages=11 (just above threshold)', () => {
    // At c=8 the window shifts left so the tile count stays near 10.
    // Result: [1, …, 4, 5, 6, 7, 8, 9, 10, 11]
    renderWithTheme(
      <TablePagination {...baseProps} currentPage={8} totalPages={11} totalItems={220} />
    );
    const nav = screen.getByRole('navigation', { name: /pagination/i });
    for (const label of ['1', '4', '5', '6', '7', '8', '9', '10', '11']) {
      expect(within(nav).getByRole('button', { name: label })).toBeInTheDocument();
    }
    for (const hiddenLabel of ['2', '3']) {
      expect(within(nav).queryByRole('button', { name: hiddenLabel })).not.toBeInTheDocument();
    }
    expect(within(nav).getAllByText('…')).toHaveLength(1);
  });

  it('renders only windowed buttons at very large totalPages', () => {
    renderWithTheme(
      <TablePagination
        {...baseProps}
        currentPage={42}
        totalPages={2133}
        itemsPerPage={250}
        totalItems={533250}
      />
    );
    const nav = screen.getByRole('navigation', { name: /pagination/i });
    for (const label of ['1', '40', '41', '42', '43', '44', '2133']) {
      expect(within(nav).getByRole('button', { name: label })).toBeInTheDocument();
    }
    for (const hiddenLabel of ['2', '39', '45', '100', '1000', '2000', '2132']) {
      expect(within(nav).queryByRole('button', { name: hiddenLabel })).not.toBeInTheDocument();
    }
  });

  it('renders two ellipsis markers when currentPage is in the middle of a large range', () => {
    renderWithTheme(
      <TablePagination
        {...baseProps}
        currentPage={42}
        totalPages={2133}
        itemsPerPage={250}
        totalItems={533250}
      />
    );
    const nav = screen.getByRole('navigation', { name: /pagination/i });
    const ellipses = within(nav).getAllByText('…');
    expect(ellipses).toHaveLength(2);
    for (const el of ellipses) {
      expect(el).toHaveAttribute('aria-hidden', 'true');
    }
  });

  it('collapses the left ellipsis when currentPage is near the start', () => {
    renderWithTheme(
      <TablePagination {...baseProps} currentPage={1} totalPages={2133} totalItems={533250} />
    );
    const nav = screen.getByRole('navigation', { name: /pagination/i });
    expect(within(nav).getAllByText('…')).toHaveLength(1);
    // 4-digit case: window pinned to [2..6]. Page 6 visible, page 7 hidden.
    expect(within(nav).getByRole('button', { name: '6' })).toBeInTheDocument();
    expect(within(nav).queryByRole('button', { name: '7' })).not.toBeInTheDocument();
  });

  it('collapses the right ellipsis when currentPage is near the end', () => {
    renderWithTheme(
      <TablePagination
        {...baseProps}
        currentPage={2133}
        totalPages={2133}
        totalItems={533250}
      />
    );
    const nav = screen.getByRole('navigation', { name: /pagination/i });
    expect(within(nav).getAllByText('…')).toHaveLength(1);
    // 4-digit case: window pinned to [2128..2132]. Page 2128 visible, page 2127 hidden.
    expect(within(nav).getByRole('button', { name: '2128' })).toBeInTheDocument();
    expect(within(nav).queryByRole('button', { name: '2127' })).not.toBeInTheDocument();
  });
});

describe('TablePagination — preserved behaviors', () => {
  const baseProps = {
    currentPage: 1,
    totalPages: 10,
    itemsPerPage: 20,
    totalItems: 200,
    onPageChange: jest.fn(),
  };

  let scrollToSpy: jest.SpyInstance;
  beforeEach(() => {
    scrollToSpy = jest.spyOn(window, 'scrollTo').mockImplementation(() => {});
  });
  afterEach(() => {
    scrollToSpy.mockRestore();
  });

  it('returns null when totalItems is 0', () => {
    renderWithTheme(<TablePagination {...baseProps} totalItems={0} totalPages={0} />);
    expect(screen.queryByRole('navigation', { name: /pagination/i })).not.toBeInTheDocument();
    expect(screen.queryByText(/^of\s/)).not.toBeInTheDocument();
  });

  it('disables First and Previous on the first page', () => {
    renderWithTheme(<TablePagination {...baseProps} currentPage={1} />);
    expect(screen.getByRole('button', { name: /first page/i })).toBeDisabled();
    expect(screen.getByRole('button', { name: /previous page/i })).toBeDisabled();
  });

  it('disables Last on the last page', () => {
    renderWithTheme(<TablePagination {...baseProps} currentPage={10} />);
    expect(screen.getByRole('button', { name: /last page/i })).toBeDisabled();
  });

  it('disables Next on the last page when hasMore is not set', () => {
    renderWithTheme(<TablePagination {...baseProps} currentPage={10} />);
    expect(screen.getByRole('button', { name: /next page/i })).toBeDisabled();
  });

  it('shows "Load more" on the last page when hasMore is true', () => {
    renderWithTheme(
      <TablePagination
        {...baseProps}
        currentPage={10}
        hasMore
        onLoadMore={jest.fn()}
      />
    );
    expect(screen.getByRole('button', { name: /load more/i })).toBeInTheDocument();
  });

  it('calls onLoadMore when Next is clicked on the last page with hasMore', () => {
    const onLoadMore = jest.fn();
    renderWithTheme(
      <TablePagination
        {...baseProps}
        currentPage={10}
        hasMore
        onLoadMore={onLoadMore}
      />
    );
    fireEvent.click(screen.getByRole('button', { name: /load more/i }));
    expect(onLoadMore).toHaveBeenCalledTimes(1);
  });

  it('advances the current page when Next is clicked mid-range', () => {
    const onPageChange = jest.fn();
    renderWithTheme(
      <TablePagination {...baseProps} currentPage={5} onPageChange={onPageChange} />
    );
    fireEvent.click(screen.getByRole('button', { name: /next page/i }));
    expect(onPageChange).toHaveBeenCalledWith(6);
  });

  it('jumps to page 1 when First is clicked', () => {
    const onPageChange = jest.fn();
    renderWithTheme(
      <TablePagination {...baseProps} currentPage={5} onPageChange={onPageChange} />
    );
    fireEvent.click(screen.getByRole('button', { name: /first page/i }));
    expect(onPageChange).toHaveBeenCalledWith(1);
  });

  it('jumps to totalPages when Last is clicked', () => {
    const onPageChange = jest.fn();
    renderWithTheme(
      <TablePagination {...baseProps} currentPage={5} onPageChange={onPageChange} />
    );
    fireEvent.click(screen.getByRole('button', { name: /last page/i }));
    expect(onPageChange).toHaveBeenCalledWith(10);
  });

  it('marks the current page button with aria-current="page" and leaves others unmarked', () => {
    renderWithTheme(
      <TablePagination {...baseProps} currentPage={3} totalPages={5} totalItems={100} />
    );
    expect(screen.getByRole('button', { name: '3' })).toHaveAttribute('aria-current', 'page');
    for (const label of ['1', '2', '4', '5']) {
      expect(screen.getByRole('button', { name: label })).not.toHaveAttribute('aria-current');
    }
  });

  it('exposes the Pagination navigation with a page-X-of-Y aria-label', () => {
    renderWithTheme(<TablePagination {...baseProps} currentPage={3} totalPages={10} />);
    expect(
      screen.getByRole('navigation', { name: /pagination, page 3 of 10/i })
    ).toBeInTheDocument();
  });

  it('formats totalItems with locale-aware thousands separators', () => {
    renderWithTheme(
      <TablePagination
        {...baseProps}
        totalItems={533250}
        totalPages={2133}
        itemsPerPage={250}
        currentPage={1}
      />
    );
    expect(screen.getByText(/of 533,250/)).toBeInTheDocument();
  });

  it('appends the totalItemsSuffix (e.g. "+") to the total when provided', () => {
    renderWithTheme(
      <TablePagination
        {...baseProps}
        totalItems={100}
        totalPages={5}
        totalItemsSuffix="+"
      />
    );
    expect(screen.getByText(/of 100\+/)).toBeInTheDocument();
  });
});

describe('computeMatches', () => {
  it('returns a window of ±50 pages around currentPage when the query is empty', () => {
    const matches = computeMatches('', 500, 2133);
    expect(matches.length).toBe(101);
    expect(matches[0]).toEqual({ id: 450, displayName: '450' });
    expect(matches[50]).toEqual({ id: 500, displayName: '500' });
    expect(matches[100]).toEqual({ id: 550, displayName: '550' });
  });

  it('stays bounded regardless of how large totalPages is', () => {
    expect(computeMatches('', 15000, 30000).length).toBe(101);
    expect(computeMatches('', 15000, 3000000).length).toBe(101);
  });

  it('clamps the empty-query window to page 1 near the start', () => {
    const matches = computeMatches('', 5, 2133);
    expect(matches[0]).toEqual({ id: 1, displayName: '1' });
    expect(matches[matches.length - 1]).toEqual({ id: 55, displayName: '55' });
  });

  it('clamps the empty-query window to totalPages near the end', () => {
    const matches = computeMatches('', 2130, 2133);
    expect(matches[0]).toEqual({ id: 2080, displayName: '2080' });
    expect(matches[matches.length - 1]).toEqual({ id: 2133, displayName: '2133' });
  });

  it('returns every page for a small totalPages when the query is empty', () => {
    expect(computeMatches('', 4, 8).map((m) => m.id)).toEqual([1, 2, 3, 4, 5, 6, 7, 8]);
  });

  it('returns pages whose string representation starts with the query, in ascending order', () => {
    expect(computeMatches('15', 42, 2133).map((m) => m.id)).toEqual([
      15, 150, 151, 152, 153, 154, 155, 156, 157, 158, 159, 1500, 1501, 1502, 1503, 1504, 1505,
      1506, 1507, 1508,
    ]);
  });

  it('returns the exact page plus one decade at the top when the query equals currentPage', () => {
    expect(computeMatches('42', 42, 2133).map((m) => m.id)).toEqual([
      42, 420, 421, 422, 423, 424, 425, 426, 427, 428, 429,
    ]);
  });

  it('returns an empty array for a query that no page starts with', () => {
    expect(computeMatches('9999', 42, 2133)).toEqual([]);
    expect(computeMatches('0', 42, 2133)).toEqual([]);
  });

  it('caps the number of matches at 20 even for very common prefixes', () => {
    const matches = computeMatches('1', 42, 100000);
    expect(matches.length).toBeLessThanOrEqual(20);
    for (let i = 1; i < matches.length; i++) {
      expect(matches[i].id).toBeGreaterThan(matches[i - 1].id);
    }
  });
});

describe('TablePagination — combobox jump-to control', () => {
  const baseProps = {
    currentPage: 42,
    totalPages: 2133,
    itemsPerPage: 250,
    totalItems: 533250,
    onPageChange: jest.fn(),
  };

  let scrollToSpy: jest.SpyInstance;
  let originalScrollIntoView: typeof Element.prototype.scrollIntoView;
  beforeEach(() => {
    scrollToSpy = jest.spyOn(window, 'scrollTo').mockImplementation(() => {});
    // JSDOM does not implement Element.scrollIntoView; NxCombobox calls it during arrow-nav.
    originalScrollIntoView = Element.prototype.scrollIntoView;
    Element.prototype.scrollIntoView = jest.fn();
  });
  afterEach(() => {
    scrollToSpy.mockRestore();
    Element.prototype.scrollIntoView = originalScrollIntoView;
  });

  it('renders a combobox labeled "Jump to page" whose initial value is currentPage', () => {
    renderWithTheme(<TablePagination {...baseProps} />);
    const combobox = screen.getByRole('combobox', { name: /jump to page/i }) as HTMLInputElement;
    expect(combobox).toBeInTheDocument();
    expect(combobox.value).toBe('42');
  });

  it('typing filters the suggestion list by prefix', async () => {
    const onPageChange = jest.fn();
    renderWithTheme(<TablePagination {...baseProps} onPageChange={onPageChange} />);
    const combobox = screen.getByRole('combobox', { name: /jump to page/i });
    combobox.focus();
    await userEvent.clear(combobox);
    await userEvent.type(combobox, '150');
    expect(screen.getByRole('option', { name: '150' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: '1500' })).toBeInTheDocument();
    expect(screen.queryByRole('option', { name: '42' })).not.toBeInTheDocument();
    expect(onPageChange).not.toHaveBeenCalled();
  });

  it('clicking a suggestion commits and calls onPageChange', async () => {
    const onPageChange = jest.fn();
    renderWithTheme(<TablePagination {...baseProps} onPageChange={onPageChange} />);
    const combobox = screen.getByRole('combobox', { name: /jump to page/i });
    combobox.focus();
    await userEvent.clear(combobox);
    await userEvent.type(combobox, '150');
    userEvent.click(screen.getByRole('option', { name: '150' }));
    expect(onPageChange).toHaveBeenCalledTimes(1);
    expect(onPageChange).toHaveBeenCalledWith(150);
  });

  it('ArrowDown + Enter selects the focused suggestion', async () => {
    const onPageChange = jest.fn();
    renderWithTheme(<TablePagination {...baseProps} onPageChange={onPageChange} />);
    const combobox = screen.getByRole('combobox', { name: /jump to page/i });
    combobox.focus();
    await userEvent.clear(combobox);
    await userEvent.type(combobox, '150{arrowdown}{enter}');
    expect(onPageChange).toHaveBeenCalledTimes(1);
    expect(onPageChange).toHaveBeenCalledWith(150);
  });

  it('arrow-navigating suggestions does not commit', async () => {
    const onPageChange = jest.fn();
    renderWithTheme(<TablePagination {...baseProps} onPageChange={onPageChange} />);
    const combobox = screen.getByRole('combobox', { name: /jump to page/i });
    combobox.focus();
    await userEvent.clear(combobox);
    await userEvent.type(combobox, '150{arrowdown}{arrowdown}{arrowdown}');
    expect(onPageChange).not.toHaveBeenCalled();
  });

  it('strips non-digit characters from typed input', async () => {
    const onPageChange = jest.fn();
    renderWithTheme(<TablePagination {...baseProps} onPageChange={onPageChange} />);
    const combobox = screen.getByRole('combobox', { name: /jump to page/i }) as HTMLInputElement;
    combobox.focus();
    await userEvent.clear(combobox);
    await userEvent.type(combobox, '1a2b3c');
    expect(combobox.value).toBe('123');
    expect(onPageChange).not.toHaveBeenCalled();
  });

  it('backspacing to empty reveals the ±50 window around currentPage', async () => {
    const onPageChange = jest.fn();
    renderWithTheme(
      <TablePagination {...baseProps} currentPage={500} onPageChange={onPageChange} />,
    );
    const combobox = screen.getByRole('combobox', { name: /jump to page/i });
    combobox.focus();
    await userEvent.clear(combobox);
    for (const label of ['450', '500', '550']) {
      expect(screen.getByRole('option', { name: label })).toBeInTheDocument();
    }
    expect(screen.queryByRole('option', { name: '449' })).not.toBeInTheDocument();
    expect(screen.queryByRole('option', { name: '551' })).not.toBeInTheDocument();
    expect(screen.queryByRole('option', { name: '2133' })).not.toBeInTheDocument();
  });

  it('shows a ±50 window of neighboring pages on initial focus (before typing)', async () => {
    renderWithTheme(<TablePagination {...baseProps} totalPages={1000} currentPage={500} />);
    const combobox = screen.getByRole('combobox', { name: /jump to page/i });
    fireEvent.focus(combobox);
    await screen.findByRole('option', { name: '500' });
    // Window: [450..550] around currentPage=500.
    for (const label of ['450', '500', '550']) {
      expect(screen.getByRole('option', { name: label })).toBeInTheDocument();
    }
    // Distant pages are not in the neighborhood window (they surface via prefix filter instead).
    expect(screen.queryByRole('option', { name: '1' })).not.toBeInTheDocument();
    expect(screen.queryByRole('option', { name: '1000' })).not.toBeInTheDocument();
  });

  it('re-seeds the input when currentPage changes externally', () => {
    const { rerender } = renderWithTheme(
      <TablePagination {...baseProps} onPageChange={jest.fn()} />,
    );
    const combobox = screen.getByRole('combobox', { name: /jump to page/i }) as HTMLInputElement;
    expect(combobox.value).toBe('42');
    rerender(
      <Theme>
        <TablePagination {...baseProps} currentPage={1500} onPageChange={jest.fn()} />
      </Theme>,
    );
    expect(combobox.value).toBe('1500');
  });

  it('does not fire onPageChange when the selected page equals currentPage', async () => {
    const onPageChange = jest.fn();
    renderWithTheme(
      <TablePagination {...baseProps} totalPages={100} onPageChange={onPageChange} />,
    );
    const combobox = screen.getByRole('combobox', { name: /jump to page/i });
    fireEvent.focus(combobox);
    const option = await screen.findByRole('option', { name: '42' });
    userEvent.click(option);
    expect(onPageChange).not.toHaveBeenCalled();
  });

  it('shows the surrounding "Page … of {totalPages}" text unchanged', () => {
    renderWithTheme(<TablePagination {...baseProps} totalPages={2133} />);
    expect(screen.getByText('Page')).toBeInTheDocument();
    expect(screen.getByText('of 2133')).toBeInTheDocument();
  });

  it('typed page number navigates on blur (tab out)', async () => {
    const onPageChange = jest.fn();
    renderWithTheme(<TablePagination {...baseProps} onPageChange={onPageChange} />);
    const combobox = screen.getByRole('combobox', { name: /jump to page/i });
    combobox.focus();
    await userEvent.clear(combobox);
    await userEvent.type(combobox, '1500');
    userEvent.tab();
    expect(onPageChange).toHaveBeenCalledTimes(1);
    expect(onPageChange).toHaveBeenCalledWith(1500);
  });

  it('empty input on blur reverts to currentPage without navigation', async () => {
    const onPageChange = jest.fn();
    renderWithTheme(<TablePagination {...baseProps} onPageChange={onPageChange} />);
    const combobox = screen.getByRole('combobox', { name: /jump to page/i }) as HTMLInputElement;
    combobox.focus();
    await userEvent.clear(combobox);
    userEvent.tab();
    expect(onPageChange).not.toHaveBeenCalled();
    expect(combobox.value).toBe('42');
  });

  it('Enter with a typed page number and no arrow-selection commits the typed value', async () => {
    const onPageChange = jest.fn();
    renderWithTheme(<TablePagination {...baseProps} onPageChange={onPageChange} />);
    const combobox = screen.getByRole('combobox', { name: /jump to page/i });
    combobox.focus();
    await userEvent.clear(combobox);
    await userEvent.type(combobox, '1500{enter}');
    expect(onPageChange).toHaveBeenCalledTimes(1);
    expect(onPageChange).toHaveBeenCalledWith(1500);
  });

  it('Enter with a value greater than totalPages clamps to totalPages', async () => {
    const onPageChange = jest.fn();
    renderWithTheme(<TablePagination {...baseProps} onPageChange={onPageChange} />);
    const combobox = screen.getByRole('combobox', { name: /jump to page/i });
    combobox.focus();
    await userEvent.clear(combobox);
    await userEvent.type(combobox, '9999{enter}');
    expect(onPageChange).toHaveBeenCalledTimes(1);
    expect(onPageChange).toHaveBeenCalledWith(2133);
  });

  it('Enter with an empty input does not fire onPageChange', async () => {
    const onPageChange = jest.fn();
    renderWithTheme(<TablePagination {...baseProps} onPageChange={onPageChange} />);
    const combobox = screen.getByRole('combobox', { name: /jump to page/i });
    combobox.focus();
    await userEvent.clear(combobox);
    await userEvent.type(combobox, '{enter}');
    expect(onPageChange).not.toHaveBeenCalled();
  });

  it('Enter with the current page typed does not fire onPageChange', async () => {
    const onPageChange = jest.fn();
    renderWithTheme(<TablePagination {...baseProps} onPageChange={onPageChange} />);
    const combobox = screen.getByRole('combobox', { name: /jump to page/i });
    combobox.focus();
    // Draft already equals "42" (currentPage). Hitting Enter should be a no-op.
    await userEvent.type(combobox, '{enter}');
    expect(onPageChange).not.toHaveBeenCalled();
  });

  it('ArrowDown + Enter does not double-fire onPageChange', async () => {
    const onPageChange = jest.fn();
    renderWithTheme(<TablePagination {...baseProps} onPageChange={onPageChange} />);
    const combobox = screen.getByRole('combobox', { name: /jump to page/i });
    combobox.focus();
    await userEvent.clear(combobox);
    await userEvent.type(combobox, '1500{arrowdown}{enter}');
    expect(onPageChange).toHaveBeenCalledTimes(1);
    expect(onPageChange).toHaveBeenCalledWith(1500);
  });

  it('selecting currentPage then typing a new page + Enter still commits', async () => {
    const onPageChange = jest.fn();
    renderWithTheme(
      <TablePagination {...baseProps} totalPages={100} onPageChange={onPageChange} />,
    );
    const combobox = screen.getByRole('combobox', { name: /jump to page/i });
    fireEvent.focus(combobox);
    const currentOption = await screen.findByRole('option', { name: '42' });
    userEvent.click(currentOption);
    expect(onPageChange).not.toHaveBeenCalled();
    // Guard-flag regression: after a no-op click on the current page, the next
    // typed Enter must not be silently swallowed.
    await userEvent.clear(combobox);
    await userEvent.type(combobox, '43{enter}');
    expect(onPageChange).toHaveBeenCalledTimes(1);
    expect(onPageChange).toHaveBeenCalledWith(43);
  });
});

describe('TablePagination — current page affordance in the jump-to list', () => {
  const baseProps = {
    currentPage: 42,
    totalPages: 2133,
    itemsPerPage: 250,
    totalItems: 533250,
    onPageChange: jest.fn(),
  };

  let scrollToSpy: jest.SpyInstance;
  let originalScrollIntoView: typeof Element.prototype.scrollIntoView;
  beforeEach(() => {
    scrollToSpy = jest.spyOn(window, 'scrollTo').mockImplementation(() => {});
    originalScrollIntoView = Element.prototype.scrollIntoView;
    Element.prototype.scrollIntoView = jest.fn();
  });
  afterEach(() => {
    scrollToSpy.mockRestore();
    Element.prototype.scrollIntoView = originalScrollIntoView;
  });

  it('marks the option matching currentPage with aria-current when the list opens', async () => {
    renderWithTheme(<TablePagination {...baseProps} totalPages={100} />);
    const combobox = screen.getByRole('combobox', { name: /jump to page/i });
    fireEvent.focus(combobox);
    const currentOption = await screen.findByRole('option', { name: '42' });
    expect(currentOption).toHaveAttribute('aria-current', 'page');
  });

  it('does not mark neighboring pages as current', async () => {
    renderWithTheme(<TablePagination {...baseProps} totalPages={100} />);
    const combobox = screen.getByRole('combobox', { name: /jump to page/i });
    fireEvent.focus(combobox);
    await screen.findByRole('option', { name: '42' });
    expect(screen.getByRole('option', { name: '41' })).not.toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('option', { name: '43' })).not.toHaveAttribute('aria-current', 'page');
  });

  it('scrolls the current page option into view when the list opens', async () => {
    renderWithTheme(<TablePagination {...baseProps} totalPages={100} />);
    const combobox = screen.getByRole('combobox', { name: /jump to page/i });
    fireEvent.focus(combobox);
    const currentOption = await screen.findByRole('option', { name: '42' });
    const scrollIntoViewMock = Element.prototype.scrollIntoView as jest.Mock;
    expect(scrollIntoViewMock.mock.instances).toContain(currentOption);
  });

  it('moves the current marker when currentPage changes externally', async () => {
    const { rerender } = renderWithTheme(<TablePagination {...baseProps} totalPages={100} />);
    const combobox = screen.getByRole('combobox', { name: /jump to page/i });
    fireEvent.focus(combobox);
    await screen.findByRole('option', { name: '42' });
    rerender(
      <Theme>
        <TablePagination {...baseProps} totalPages={100} currentPage={43} />
      </Theme>,
    );
    expect(screen.getByRole('option', { name: '43' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('option', { name: '42' })).not.toHaveAttribute('aria-current', 'page');
  });

  it('keeps marking the current page while typing when it survives the filter', async () => {
    renderWithTheme(<TablePagination {...baseProps} totalPages={100} />);
    const combobox = screen.getByRole('combobox', { name: /jump to page/i });
    combobox.focus();
    await userEvent.clear(combobox);
    await userEvent.type(combobox, '4');
    expect(screen.getByRole('option', { name: '42' })).toHaveAttribute('aria-current', 'page');
  });

  it('marks no option when the typed filter excludes the current page', async () => {
    renderWithTheme(<TablePagination {...baseProps} totalPages={100} />);
    const combobox = screen.getByRole('combobox', { name: /jump to page/i });
    combobox.focus();
    await userEvent.clear(combobox);
    await userEvent.type(combobox, '9');
    for (const option of screen.getAllByRole('option')) {
      expect(option).not.toHaveAttribute('aria-current', 'page');
    }
  });
});
