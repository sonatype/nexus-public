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
import { Theme } from '@radix-ui/themes';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import GAVersionsTab from '../GAVersionsTab';
import type { ComponentVersionSort, GAVersion } from '../../core';
import { formatDateTime } from '../../../../shared';

const baseProps = {
  versions: [
    { version: '3.14.0', lastUpdated: '2026-01-02T00:00:00Z', repositories: ['maven-central'] },
    { version: '3.13.0', lastUpdated: '2026-01-01T00:00:00Z', repositories: ['maven-central'] },
  ],
  total: 2,
  totalPages: 1,
  currentPage: 1,
  itemsPerPage: 25,
  sortKey: 'version' as const,
  sortDirection: 'desc' as const,
  searchQuery: '',
  loading: false,
  error: null,
  onPageChange: jest.fn(),
  onItemsPerPageChange: jest.fn(),
  onSortChange: jest.fn(),
  onSearchQueryChange: jest.fn(),
  onRetry: jest.fn(),
};

const wrap = (ui: React.ReactElement) => render(<Theme>{ui}</Theme>);

describe('GAVersionsTab', () => {
  it('renders the version rows', () => {
    wrap(<GAVersionsTab {...baseProps} />);
    expect(screen.getByText('3.14.0')).toBeInTheDocument();
    expect(screen.getByText('3.13.0')).toBeInTheDocument();
  });

  it('selects a version when its row is clicked', () => {
    const onSelectVersion = jest.fn();
    wrap(<GAVersionsTab {...baseProps} onSelectVersion={onSelectVersion} />);
    fireEvent.click(screen.getByText('3.14.0'));
    expect(onSelectVersion).toHaveBeenCalledWith('3.14.0');
  });

  it('renders the version as a link affordance when selectable', () => {
    wrap(<GAVersionsTab {...baseProps} onSelectVersion={jest.fn()} />);
    // The version reads as a link so it is obviously clickable, even though the whole row is too.
    expect(screen.getByText('3.14.0').closest('a')).not.toBeNull();
  });

  it('gives the selectable version an inline blue link appearance that overrides the app-wide anchor reset', () => {
    // Nexus One neutralizes bare anchors with a high-specificity App.scss rule —
    // `.nxrm-main-content > .radix-themes:not(:has(.nxrm-ext-js-wrapper)) a:not([class*="x-"])`
    // sets `color: inherit; text-decoration: none`. That selector (~0,4,1) beats Radix's own
    // `.rt-Text[data-accent-color]` (0,1,0) rule, so a Radix <Link> renders black despite
    // data-accent-color="blue". Only an inline style outranks it, so the version link carries
    // its color and underline inline.
    wrap(<GAVersionsTab {...baseProps} onSelectVersion={jest.fn()} />);
    const link = screen.getByText('3.14.0').closest('a');
    expect(link).toHaveAttribute('data-accent-color', 'blue');
    // jsdom discards `color: var(--accent-11)` (it can't parse a var() color), so we assert the
    // co-applied, observable half of the same inline override: the explicit underline.
    expect(link).toHaveStyle({ textDecorationLine: 'underline' });
  });

  it('renders the version as plain text (no link) when read-only', () => {
    wrap(<GAVersionsTab {...baseProps} />);
    expect(screen.getByText('3.14.0').closest('a')).toBeNull();
  });

  it('marks the currently selected version row', () => {
    wrap(<GAVersionsTab {...baseProps} selectedVersion="3.14.0" onSelectVersion={jest.fn()} />);
    const selectedRow = document.querySelector('[data-version="3.14.0"]');
    const otherRow = document.querySelector('[data-version="3.13.0"]');
    expect(selectedRow).toHaveAttribute('aria-selected', 'true');
    expect(otherRow).toHaveAttribute('aria-selected', 'false');
    expect(screen.getByText('Selected')).toBeInTheDocument();
  });

  it('renders read-only rows when no selection handler is provided', () => {
    wrap(<GAVersionsTab {...baseProps} />);
    const row = document.querySelector('[data-version="3.14.0"]');
    expect(row).not.toBeNull();
    expect(row).not.toHaveAttribute('role', 'button');
  });

  it("shows an em dash, not 'Invalid Date', when a version's lastUpdated is missing or unparseable", () => {
    // new Date(undefined) yields an Invalid Date rather than throwing, so an unguarded formatDate
    // would render the literal "Invalid Date" in the Last Updated column. Both a missing and a
    // garbage timestamp must fall back to an em dash.
    wrap(
      <GAVersionsTab
        {...baseProps}
        versions={[
          { version: '2.0.0', lastUpdated: undefined as unknown as string, repositories: ['maven-central'] },
          { version: '1.0.0', lastUpdated: 'not-a-date', repositories: ['maven-central'] },
        ]}
        total={2}
      />,
    );
    expect(screen.queryByText(/invalid date/i)).not.toBeInTheDocument();
    const missing = document.querySelector('[data-version="2.0.0"]');
    const garbage = document.querySelector('[data-version="1.0.0"]');
    expect(missing).toHaveTextContent('—');
    expect(garbage).toHaveTextContent('—');
  });
});

const columnVersions: GAVersion[] = [
  {
    version: '3.9.6',
    lastUpdated: '2024-01-15T10:00:00Z',
    repositories: ['maven-central', 'maven-releases', 'maven-snapshots'],
  },
  {
    version: '2.0.0',
    lastUpdated: '2023-11-20T09:30:00Z',
    repositories: ['solo-repo'],
  },
  {
    version: '1.0.0',
    lastUpdated: '',
    repositories: [],
  },
];

const noop = () => {};

function renderTab(overrides: Partial<React.ComponentProps<typeof GAVersionsTab>> = {}) {
  return render(
    <Theme>
      <GAVersionsTab
        versions={columnVersions}
        total={columnVersions.length}
        totalPages={1}
        currentPage={1}
        itemsPerPage={20}
        sortKey={'version' as ComponentVersionSort}
        sortDirection="desc"
        searchQuery=""
        loading={false}
        error={null}
        onPageChange={noop}
        onItemsPerPageChange={noop}
        onSortChange={noop}
        onSearchQueryChange={noop}
        onRetry={noop}
        selectedVersion={null}
        {...overrides}
      />
    </Theme>,
  );
}

describe('GAVersionsTab repository column', () => {
  it('shows the repository count as a badge rather than the names inline', () => {
    renderTab();
    expect(
      screen.getByRole('button', { name: 'Repositories' }),
    ).toBeInTheDocument();
    // The count is the at-a-glance value: "3 repos" for the three-repo version, "1 repo" for the
    // single-repo one (singular, no trailing "s").
    expect(screen.getByText('3 repos')).toBeInTheDocument();
    expect(screen.getByText('1 repo')).toBeInTheDocument();
    // The names themselves are not rendered as inline text — they live in the tooltip / accessible
    // name only, so the column stays narrow.
    expect(screen.queryByText('maven-central')).not.toBeInTheDocument();
    expect(screen.queryByText('solo-repo')).not.toBeInTheDocument();
  });

  it('names the repositories in the badge text content, announced regardless of role or tooltip', () => {
    renderTab();
    // The names live in the badge's own (visually hidden) text content, not an aria-label on a
    // role-generic <span> that real assistive tech ignores. getByText matches on the badge's direct
    // text ("3 repos"); toHaveTextContent then sees the full content including the hidden names.
    expect(screen.getByText('3 repos')).toHaveTextContent(
      '3 repos: maven-central, maven-releases, maven-snapshots',
    );
    expect(screen.getByText('1 repo')).toHaveTextContent('1 repo: solo-repo');
  });

  it('reveals the repository names in a tooltip on hover', async () => {
    renderTab();
    await userEvent.hover(screen.getByText('3 repos'));
    await waitFor(() => {
      // Radix renders the content twice (visible + a11y mirror); assert at least one is present.
      expect(
        screen.getAllByText('maven-central, maven-releases, maven-snapshots').length,
      ).toBeGreaterThan(0);
    });
  });

  it('caps the name list at 10, collapsing the rest into a "+N more" suffix', () => {
    const many = Array.from({ length: 12 }, (_, i) => `repo-${i + 1}`);
    renderTab({
      versions: [
        { version: '5.0.0', lastUpdated: '2024-01-15T10:00:00Z', repositories: many },
      ],
      total: 1,
    });
    // The badge still reports the true total, even though only 10 names are spelled out.
    expect(screen.getByText('12 repos')).toHaveTextContent(
      '12 repos: repo-1, repo-2, repo-3, repo-4, repo-5, repo-6, repo-7, repo-8, repo-9, repo-10, +2 more',
    );
    // The 11th name onward is not enumerated — it is folded into "+2 more".
    expect(screen.queryByText(/repo-11/)).not.toBeInTheDocument();
  });

  it('renders an em dash when a version lives in no repository', () => {
    renderTab({
      versions: [{ version: '1.0.0', lastUpdated: '', repositories: [] }],
      total: 1,
    });
    // Both the empty repository cell and the empty timestamp cell render the em dash.
    expect(screen.getAllByText('—').length).toBeGreaterThanOrEqual(2);
  });
});

describe('GAVersionsTab last-updated timestamp', () => {
  it('renders the timestamp at T6 precision (date + hour + minute)', () => {
    renderTab();

    // The column renders through the shared formatDateTime (NEXUS-54427), so assert against that
    // exact output — it carries locale, 24-hour time and the timezone label the component shows.
    const expected = formatDateTime('2024-01-15T10:00:00Z');
    expect(screen.getByText(expected)).toBeInTheDocument();
    // A time component (hour:minute) must be present — a date-only rendering has no colon. This is
    // the T6-precision requirement (NEXUS-54430): Last Updated shows date + hour + minute.
    expect(expected).toMatch(/\d{2}:\d{2}/);
  });
});
