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

/*
 * NEXUS-54427: the Component Detail lists render every timestamp through the one shared formatter,
 * so a row shows a calendar date, a 24-hour time and a timezone label — and a dash when the
 * timestamp is missing or unparseable.
 *
 * Matched by shape rather than by exact string: the wall clock and offset depend on the timezone of
 * the machine running the suite, jest.config.js pins none, and setting process.env.TZ inside a test
 * file does not take effect. Exact per-zone output — including half-hour offsets, day boundaries and
 * DST — is pinned in shared/__tests__/dateFormat.test.ts, which passes an explicit zone.
 *
 * These also assert the other half of the ticket: display formatting must not reach the values used
 * for ordering or for CSV export, which stay the raw ISO strings the API supplied.
 */

import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import '@testing-library/jest-dom';
import { Theme } from '@radix-ui/themes';

import { GAVersionsTab } from '../GAVersionsTab';
import { GAFilesTab } from '../GAFilesTab';
import * as exportToCsvModule from '../../../../shared/utils/exportToCsv';
import type { GAAsset, GAVersion } from '../../core';

// GAFilesTab's rows call useRouter for the "View in Browse" action (NEXUS-54430); this suite
// renders those rows only to assert their timestamp formatting, so a bare router stub is enough.
jest.mock('@uirouter/react', () => ({
  useRouter: () => ({ stateService: { go: jest.fn() } }),
}));

const renderWithTheme = (ui: React.ReactElement) => render(<Theme>{ui}</Theme>);

/** Date + 24-hour time + timezone label, in whatever zone the suite happens to run in. */
const DATE_TIME_TZ = /^[A-Z][a-z]{2} \d{1,2}, \d{4}, \d{2}:\d{2} GMT([+-]\d{1,2}(:\d{2})?)?$/;

const versions: readonly GAVersion[] = [
  { version: '2.0.0', lastUpdated: '2024-03-15T09:15:00Z', repositories: ['repo-a'] },
];

const versionsProps = {
  versions,
  total: versions.length,
  totalPages: 1,
  currentPage: 1,
  itemsPerPage: 20,
  sortKey: 'lastUpdated' as const,
  sortDirection: 'desc' as const,
  searchQuery: '',
  loading: false,
  error: null,
  onPageChange: jest.fn(),
  onItemsPerPageChange: jest.fn(),
  onSortChange: jest.fn(),
  onSearchQueryChange: jest.fn(),
  onRetry: jest.fn(),
  selectedVersion: null,
  onVersionSelect: jest.fn(),
};

const asset: GAAsset = {
  id: 'a1',
  repository: 'repo-a',
  path: 'com/example/lib/1.0/lib-1.0.jar',
  downloadUrl: 'http://localhost/repository/repo-a/lib-1.0.jar',
  format: 'jar',
  extension: 'jar',
  size: 1024,
  contentType: 'application/java-archive',
  lastModified: '2024-01-15T10:00:00Z',
  checksums: { sha1: 'abc', sha256: 'def', md5: 'ghi' },
};

const filesProps = {
  assets: [asset] as readonly GAAsset[],
  selectedVersion: '1.0.0',
  loading: false,
};

describe('Versions tab timestamps', () => {
  it('renders Last Updated with date, time and timezone', () => {
    renderWithTheme(<GAVersionsTab {...versionsProps} />);
    expect(screen.getByText(DATE_TIME_TZ)).toBeInTheDocument();
    expect(screen.getByText(/^Mar \d{1,2}, 2024, /)).toBeInTheDocument();
  });

  it.each([
    ['null', null],
    ['an empty string', ''],
    ['an unparseable string', 'not-a-date'],
  ])('renders an em dash, not "Invalid Date", when lastUpdated is %s', (_label, value) => {
    renderWithTheme(
      <GAVersionsTab
        {...versionsProps}
        versions={[{ ...versions[0], lastUpdated: value as string }]}
      />,
    );
    expect(screen.getByText('—')).toBeInTheDocument();
    expect(screen.queryByText('Invalid Date')).not.toBeInTheDocument();
  });

  it('renders the row without crashing when lastUpdated is malformed', () => {
    expect(() =>
      renderWithTheme(
        <GAVersionsTab
          {...versionsProps}
          versions={[{ ...versions[0], lastUpdated: '2024-13-45T99:99:99Z' }]}
        />,
      ),
    ).not.toThrow();
    expect(screen.getByText('2.0.0')).toBeInTheDocument();
  });

  it('exports the raw timestamp, not the formatted one', async () => {
    const spy = jest
      .spyOn(exportToCsvModule, 'exportToCsv')
      .mockImplementation(() => undefined);
    try {
      renderWithTheme(<GAVersionsTab {...versionsProps} />);
      await userEvent.click(screen.getByText('Export page'));
      const [rows] = spy.mock.calls[0];
      expect((rows as Array<Record<string, unknown>>)[0].lastUpdated).toBe(
        '2024-03-15T09:15:00Z',
      );
    } finally {
      spy.mockRestore();
    }
  });
});

describe('Files tab timestamps', () => {
  it('renders Last Modified with date, time and timezone', () => {
    renderWithTheme(<GAFilesTab {...filesProps} />);
    expect(screen.getByText(DATE_TIME_TZ)).toBeInTheDocument();
    expect(screen.getByText(/^Jan \d{1,2}, 2024, /)).toBeInTheDocument();
  });

  /*
   * Sorting stays on the raw timestamps. The two months are chosen so the two orderings disagree:
   * chronologically January precedes December, but as displayed text "Dec 1, 2024" precedes
   * "Jan 15, 2024". An ascending sort that had been handed the formatted strings would put the
   * December row first. Clicking the header once sorts ascending.
   */
  it('orders Last Modified by the underlying timestamp, not the displayed text', async () => {
    renderWithTheme(
      <GAFilesTab
        {...filesProps}
        assets={[
          { ...asset, id: 'december', path: 'a/december.jar', lastModified: '2024-12-01T10:00:00Z' },
          { ...asset, id: 'january', path: 'a/january.jar', lastModified: '2024-01-15T10:00:00Z' },
        ]}
      />,
    );
    await userEvent.click(screen.getByRole('button', { name: /last modified/i }));
    const rows = screen
      .getAllByRole('row')
      .slice(1)
      .map((row) => row.textContent ?? '');
    expect(rows[0]).toContain('january.jar');
    expect(rows[1]).toContain('december.jar');
  });
});
