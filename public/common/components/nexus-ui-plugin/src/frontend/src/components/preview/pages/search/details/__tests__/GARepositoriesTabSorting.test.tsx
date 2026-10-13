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

/**
 * Repositories tab sorting after removal of the left-side sort control
 * (NEXUS-54433 Part B / BDD-027).
 *
 * Column-header sorting is now the single sorting mechanism on this tab, so
 * these assertions are all about observable table behaviour: which rows are
 * rendered, and in what order. Nothing here inspects hook state or handler
 * calls.
 *
 * The fixture (see gaRepositoriesTestHelpers) is chosen so every column
 * produces a different, unambiguous ordering.
 */

import { screen, within, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import type { RepoRow } from '../gaRepositoriesMachine';
import { SORTING_REPO_ROWS as ROWS, makeRenderTab, tabElement } from './gaRepositoriesTestHelpers';

const renderTab = makeRenderTab(ROWS);

/** Repository names in the order the table currently renders them. */
function renderedOrder(): string[] {
  const rows = screen.getAllByRole('row').slice(1); // drop the header row
  return rows.map((row) => (within(row).getAllByRole('cell')[0].textContent ?? '').trim());
}

function header(name: RegExp | string): HTMLElement {
  return screen.getByRole('columnheader', { name });
}

/** The header's activatable control (SortableTableHeader renders a role=button inside the cell). */
function headerButton(name: RegExp | string): HTMLElement {
  return within(header(name)).getByRole('button');
}

const REPOSITORY = /repository/i;
const TYPE = /^type$/i;
const VERSIONS = /versions/i;

describe('GARepositoriesTab sorting', () => {
  beforeEach(() => {
    // TablePagination scrolls to the top on every page change; jsdom has no
    // implementation.
    window.scrollTo = jest.fn();
  });

  it('no longer renders a separate left-side sort control', () => {
    renderTab();
    // The sidebar sort label is gone (the filter sidebar renders twice — once
    // for desktop, once inside the mobile drawer — so query for every match).
    expect(screen.queryAllByText('Sort')).toHaveLength(0);
    expect(screen.queryAllByText('Ascending')).toHaveLength(0);
    expect(screen.queryAllByText('Descending')).toHaveLength(0);
    // The only remaining comboboxes belong to pagination; none of them is a
    // sort control offering a column or a direction.
    const sortLike = screen
      .queryAllByRole('combobox')
      .filter((cb) => /repository|versions in repo|ascending|descending/i.test(cb.textContent ?? ''));
    expect(sortLike).toHaveLength(0);
  });

  it('keeps the rest of the filter sidebar intact', () => {
    renderTab();
    // Removing the sort block must not leave the sidebar empty or dead.
    expect(screen.getAllByRole('button', { name: /reset filters/i }).length).toBeGreaterThan(0);
    expect(screen.getAllByText('Type').length).toBeGreaterThan(0);
    expect(screen.getAllByLabelText('Hosted').length).toBeGreaterThan(0);
  });

  it('offers column sorting on repository, type and versions', () => {
    renderTab();
    expect(header(REPOSITORY)).toBeInTheDocument();
    expect(header(TYPE)).toBeInTheDocument();
    expect(header(VERSIONS)).toBeInTheDocument();
  });

  it('defaults to repository name ascending', () => {
    renderTab();
    expect(renderedOrder()).toEqual(['alpha-proxy', 'beta-hosted', 'gamma-group']);
  });

  it('sorts repository name descending on the first click of the active column', async () => {
    renderTab();
    await userEvent.click(headerButton(REPOSITORY));
    expect(renderedOrder()).toEqual(['gamma-group', 'beta-hosted', 'alpha-proxy']);
  });

  it('toggles repository name back to ascending on the next click', async () => {
    renderTab();
    await userEvent.click(headerButton(REPOSITORY));
    await userEvent.click(headerButton(REPOSITORY));
    expect(renderedOrder()).toEqual(['alpha-proxy', 'beta-hosted', 'gamma-group']);
  });

  it('sorts by type ascending then descending', async () => {
    renderTab();
    await userEvent.click(headerButton(TYPE));
    // group < hosted < proxy
    expect(renderedOrder()).toEqual(['gamma-group', 'beta-hosted', 'alpha-proxy']);

    await userEvent.click(headerButton(TYPE));
    expect(renderedOrder()).toEqual(['alpha-proxy', 'beta-hosted', 'gamma-group']);
  });

  it('sorts versions numerically ascending, not lexicographically', async () => {
    renderTab();
    await userEvent.click(headerButton(VERSIONS));
    // 3 < 7 < 21 — a string comparison would put 21 before 3.
    expect(renderedOrder()).toEqual(['gamma-group', 'alpha-proxy', 'beta-hosted']);
  });

  it('sorts versions numerically descending', async () => {
    renderTab();
    await userEvent.click(headerButton(VERSIONS));
    await userEvent.click(headerButton(VERSIONS));
    expect(renderedOrder()).toEqual(['beta-hosted', 'alpha-proxy', 'gamma-group']);
  });

  it('communicates the active column and direction through aria-sort', async () => {
    renderTab();
    expect(header(REPOSITORY)).toHaveAttribute('aria-sort', 'ascending');
    expect(header(TYPE)).toHaveAttribute('aria-sort', 'none');
    expect(header(VERSIONS)).toHaveAttribute('aria-sort', 'none');

    await userEvent.click(headerButton(VERSIONS));
    expect(header(VERSIONS)).toHaveAttribute('aria-sort', 'ascending');
    expect(header(REPOSITORY)).toHaveAttribute('aria-sort', 'none');

    await userEvent.click(headerButton(VERSIONS));
    expect(header(VERSIONS)).toHaveAttribute('aria-sort', 'descending');
  });

  it('is operable from the keyboard with Enter and Space', () => {
    renderTab();
    const versions = headerButton(VERSIONS);
    expect(versions).toHaveAttribute('tabindex', '0');

    fireEvent.keyDown(versions, { key: 'Enter' });
    expect(renderedOrder()).toEqual(['gamma-group', 'alpha-proxy', 'beta-hosted']);

    fireEvent.keyDown(headerButton(VERSIONS), { key: ' ' });
    expect(renderedOrder()).toEqual(['beta-hosted', 'alpha-proxy', 'gamma-group']);
  });

  it('sorts the rows that survive filtering', async () => {
    renderTab();
    // Exclude the group repository, then sort the remaining two descending.
    fireEvent.click(screen.getByLabelText('Hosted'));
    fireEvent.click(screen.getByLabelText('Proxy'));
    expect(renderedOrder()).toEqual(['alpha-proxy', 'beta-hosted']);

    await userEvent.click(headerButton(REPOSITORY));
    expect(renderedOrder()).toEqual(['beta-hosted', 'alpha-proxy']);
  });

  it('sorts across the whole result set and returns to the first page', async () => {
    // 25 rows so the default 20-per-page pagination is active.
    const many: RepoRow[] = Array.from({ length: 25 }, (_, i) => ({
      repositoryName: `repo-${String(i + 1).padStart(2, '0')}`,
      type: 'hosted',
      versionCount: i + 1,
    }));
    renderTab({ rows: many });

    expect(renderedOrder()[0]).toBe('repo-01');

    // Move to page 2, then sort: the sort applies to the full set and the view
    // returns to page 1, so the highest-numbered repo leads.
    await userEvent.click(screen.getAllByRole('button', { name: '2' })[0]);
    expect(renderedOrder()[0]).toBe('repo-21');

    await userEvent.click(headerButton(REPOSITORY));
    expect(renderedOrder()[0]).toBe('repo-25');
  });

  it('holds the chosen order across a rerender', async () => {
    const { rerender } = renderTab();
    await userEvent.click(headerButton(VERSIONS));
    expect(renderedOrder()).toEqual(['gamma-group', 'alpha-proxy', 'beta-hosted']);

    rerender(tabElement(ROWS));
    expect(renderedOrder()).toEqual(['gamma-group', 'alpha-proxy', 'beta-hosted']);
  });

  it('orders rows with equal sort values deterministically', async () => {
    const ties: RepoRow[] = [
      { repositoryName: 'a-repo', type: 'hosted', versionCount: 5 },
      { repositoryName: 'b-repo', type: 'hosted', versionCount: 5 },
      { repositoryName: 'c-repo', type: 'hosted', versionCount: 5 },
    ];
    renderTab({ rows: ties });

    // All three tie on type and versionCount; the order must be stable and
    // identical on repeated sorts of the same column.
    await userEvent.click(headerButton(TYPE));
    const first = renderedOrder();
    await userEvent.click(headerButton(TYPE));
    await userEvent.click(headerButton(TYPE));
    expect(renderedOrder()).toEqual(first);
    expect(first).toHaveLength(3);
  });
});
