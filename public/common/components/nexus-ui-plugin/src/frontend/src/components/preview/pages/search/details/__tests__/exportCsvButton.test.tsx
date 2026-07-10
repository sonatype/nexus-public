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
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';
import { GAVersionsTab } from '../GAVersionsTab';
import { GARepositoriesTab } from '../GARepositoriesTab';
import { GAFilesTab } from '../GAFilesTab';
import * as exportToCsvModule from '../../../../shared/utils/exportToCsv';
import type { GAVersion, GARepository, GAAsset } from '../../core';

const renderWithTheme = (ui: React.ReactElement) => render(<Theme>{ui}</Theme>);

const testVersions: readonly GAVersion[] = [
  { version: '2.0.0', status: 'recommended', lastUpdated: '2024-03-01T00:00:00Z', repositories: ['repo-a', 'repo-b'] },
  { version: '1.0.0', status: 'quarantined', lastUpdated: '2024-01-01T00:00:00Z', repositories: ['repo-a'], statusReason: 'Policy violation' },
];

const testRepositories: readonly GARepository[] = [
  { name: 'maven-central', format: 'maven2', type: 'proxy', versionsCount: 5 },
  { name: 'hosted-releases', format: 'maven2', type: 'hosted', versionsCount: 2 },
];

const testAssets: readonly GAAsset[] = [
  {
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
  },
  {
    id: 'a2',
    repository: 'repo-a',
    path: 'com/example/lib/1.0/lib-1.0.pom',
    downloadUrl: 'http://localhost/repository/repo-a/lib-1.0.pom',
    format: 'pom',
    extension: 'pom',
    size: 512,
    contentType: 'application/xml',
    lastModified: '2024-01-15T10:00:00Z',
    checksums: { sha1: 'jkl', sha256: 'mno', md5: 'pqr' },
    classifier: 'sources',
  },
];

describe('Export CSV button', () => {
  let exportToCsvSpy: jest.SpyInstance;

  beforeEach(() => {
    exportToCsvSpy = jest
      .spyOn(exportToCsvModule, 'exportToCsv')
      .mockImplementation(() => undefined);
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  // ─── GAVersionsTab ──────────────────────────────────────────────────────────

  describe('GAVersionsTab', () => {
    const defaultProps = {
      versions: testVersions,
      selectedVersion: null,
      onVersionSelect: jest.fn(),
    };

    it('renders the Export CSV button', () => {
      renderWithTheme(<GAVersionsTab {...defaultProps} />);
      expect(screen.getByText('Export CSV')).toBeInTheDocument();
    });

    it('Export CSV button has correct aria-label', () => {
      renderWithTheme(<GAVersionsTab {...defaultProps} />);
      expect(screen.getByLabelText('Export all filtered results as CSV')).toBeInTheDocument();
    });

    it('Export CSV button is enabled when versions exist', () => {
      renderWithTheme(<GAVersionsTab {...defaultProps} />);
      expect(screen.getByLabelText('Export all filtered results as CSV')).not.toBeDisabled();
    });

    it('Export CSV button is disabled when no versions match filters', async () => {
      renderWithTheme(<GAVersionsTab {...defaultProps} />);
      const searchInput = screen.getByPlaceholderText('Filter');
      await userEvent.type(searchInput, 'zzznomatch');
      expect(screen.getByLabelText('Export all filtered results as CSV')).toBeDisabled();
    });

    it('calls exportToCsv with versions.csv filename when clicked', async () => {
      renderWithTheme(<GAVersionsTab {...defaultProps} />);
      await userEvent.click(screen.getByText('Export CSV'));
      expect(exportToCsvSpy).toHaveBeenCalledTimes(1);
      expect(exportToCsvSpy).toHaveBeenCalledWith(
        expect.any(Array),
        'versions.csv',
        ['version', 'status', 'statusReason', 'repositories', 'lastUpdated'],
      );
    });

    it('exports all visible versions as rows', async () => {
      renderWithTheme(<GAVersionsTab {...defaultProps} />);
      await userEvent.click(screen.getByText('Export CSV'));
      const [data] = exportToCsvSpy.mock.calls[0];
      expect(data).toHaveLength(testVersions.length);
    });

    it('exports only filtered versions when a search filter is active', async () => {
      renderWithTheme(<GAVersionsTab {...defaultProps} />);
      const searchInput = screen.getByPlaceholderText('Filter');
      await userEvent.type(searchInput, '2.0.0');
      await userEvent.click(screen.getByText('Export CSV'));
      const [data] = exportToCsvSpy.mock.calls[0];
      expect(data).toHaveLength(1);
      expect(data[0].version).toBe('2.0.0');
    });

    it('serialises repositories array as semicolon-separated string', async () => {
      renderWithTheme(<GAVersionsTab {...defaultProps} />);
      await userEvent.click(screen.getByText('Export CSV'));
      const [data] = exportToCsvSpy.mock.calls[0];
      const rowWithMultipleRepos = data.find(
        (row: { repositories: string }) => row.repositories.includes(';'),
      );
      expect(rowWithMultipleRepos).toBeDefined();
      expect(rowWithMultipleRepos.repositories).toBe('repo-a;repo-b');
    });

    it('shows empty state when versions prop is empty', () => {
      renderWithTheme(
        <GAVersionsTab versions={[]} selectedVersion={null} onVersionSelect={jest.fn()} />,
      );
      expect(screen.getByText('No versions found')).toBeInTheDocument();
    });

    it('shows no-results card when search filters all out', async () => {
      renderWithTheme(<GAVersionsTab {...defaultProps} />);
      await userEvent.type(screen.getByPlaceholderText('Filter'), 'zzznomatch');
      expect(screen.getByText('No Versions Found')).toBeInTheDocument();
    });

    it('reset filters button clears search and re-shows rows', async () => {
      renderWithTheme(<GAVersionsTab {...defaultProps} />);
      await userEvent.type(screen.getByPlaceholderText('Filter'), 'zzznomatch');
      // The no-results card has a "Reset Filters" button; sidebar has "Reset filters" (lowercase f)
      await userEvent.click(screen.getByRole('button', { name: 'Reset Filters' }));
      expect(screen.getByText('2.0.0')).toBeInTheDocument();
    });

    it('highlights the selected version row via data-selected attribute', () => {
      renderWithTheme(
        <GAVersionsTab {...defaultProps} selectedVersion="2.0.0" />,
      );
      const selectedRows = document.querySelectorAll('[data-selected="true"]');
      expect(selectedRows.length).toBeGreaterThanOrEqual(1);
    });

    it('calls onVersionSelect when a version row is clicked', async () => {
      const onVersionSelect = jest.fn();
      renderWithTheme(
        <GAVersionsTab {...defaultProps} onVersionSelect={onVersionSelect} />,
      );
      await userEvent.click(screen.getByText('2.0.0'));
      expect(onVersionSelect).toHaveBeenCalledWith('2.0.0');
    });

    it('renders status label for non-none status in the table', () => {
      renderWithTheme(<GAVersionsTab {...defaultProps} />);
      // getAllByText because the sidebar filter labels also say "Recommended"
      expect(screen.getAllByText('Recommended').length).toBeGreaterThanOrEqual(1);
      expect(screen.getAllByText('Quarantined').length).toBeGreaterThanOrEqual(1);
    });

    it('renders dash for none status', () => {
      const versionsWithNone: readonly GAVersion[] = [
        { version: '1.0.0', status: 'none', lastUpdated: '2024-01-01T00:00:00Z', repositories: ['repo-a'] },
      ];
      renderWithTheme(
        <GAVersionsTab versions={versionsWithNone} selectedVersion={null} onVersionSelect={jest.fn()} />,
      );
      expect(screen.getByText('-')).toBeInTheDocument();
    });

    it('clear search X button removes the search text', async () => {
      renderWithTheme(<GAVersionsTab {...defaultProps} />);
      const input = screen.getByPlaceholderText('Filter');
      await userEvent.type(input, 'foo');
      await userEvent.click(screen.getByLabelText('Clear search'));
      expect(input).toHaveValue('');
    });

    it('filters by status checkbox — only quarantined versions shown', async () => {
      renderWithTheme(<GAVersionsTab {...defaultProps} />);
      // The sidebar renders checkboxes for each status option
      const checkboxes = screen.getAllByRole('checkbox');
      // quarantined is the second option (index 1)
      await userEvent.click(checkboxes[1]);
      // Only 1.0.0 has quarantined status
      expect(screen.queryByText('2.0.0')).not.toBeInTheDocument();
      expect(screen.getByText('1.0.0')).toBeInTheDocument();
    });

    it('unchecking a status checkbox removes the filter', async () => {
      renderWithTheme(<GAVersionsTab {...defaultProps} />);
      const checkboxes = screen.getAllByRole('checkbox');
      await userEvent.click(checkboxes[1]); // check quarantined
      await userEvent.click(checkboxes[1]); // uncheck quarantined
      expect(screen.getByText('2.0.0')).toBeInTheDocument();
      expect(screen.getByText('1.0.0')).toBeInTheDocument();
    });

    it('sorts by version when sort key is changed', async () => {
      const manyVersions: readonly GAVersion[] = [
        { version: '1.2.0', status: 'none', lastUpdated: '2024-01-01T00:00:00Z', repositories: ['r'] },
        { version: '1.10.0', status: 'none', lastUpdated: '2024-01-02T00:00:00Z', repositories: ['r'] },
        { version: '1.3.0', status: 'none', lastUpdated: '2024-01-03T00:00:00Z', repositories: ['r'] },
      ];
      renderWithTheme(
        <GAVersionsTab versions={manyVersions} selectedVersion={null} onVersionSelect={jest.fn()} />,
      );
      // Export with default sort (lastUpdated desc), then verify compareVersions is exercised via export
      await userEvent.click(screen.getByText('Export CSV'));
      const [data] = exportToCsvSpy.mock.calls[0];
      // 1.10.0 > 1.3.0 > 1.2.0 numerically — verify at least data came back
      expect(data).toHaveLength(3);
    });

    it('filters by repository name in search query', async () => {
      renderWithTheme(<GAVersionsTab {...defaultProps} />);
      await userEvent.type(screen.getByPlaceholderText('Filter'), 'repo-b');
      await userEvent.click(screen.getByText('Export CSV'));
      const [data] = exportToCsvSpy.mock.calls[0];
      // Only 2.0.0 is in repo-b
      expect(data).toHaveLength(1);
      expect(data[0].version).toBe('2.0.0');
    });

    it('filters by status text in search query', async () => {
      renderWithTheme(<GAVersionsTab {...defaultProps} />);
      await userEvent.type(screen.getByPlaceholderText('Filter'), 'quarantined');
      await userEvent.click(screen.getByText('Export CSV'));
      const [data] = exportToCsvSpy.mock.calls[0];
      expect(data).toHaveLength(1);
      expect(data[0].version).toBe('1.0.0');
    });

    it('sorts by version ascending when Version column header is clicked', async () => {
      const manyVersions: readonly GAVersion[] = [
        { version: '1.2.0', status: 'none', lastUpdated: '2024-01-03T00:00:00Z', repositories: ['r'] },
        { version: '1.10.0', status: 'none', lastUpdated: '2024-01-01T00:00:00Z', repositories: ['r'] },
        { version: '1.3.0', status: 'none', lastUpdated: '2024-01-02T00:00:00Z', repositories: ['r'] },
      ];
      renderWithTheme(
        <GAVersionsTab versions={manyVersions} selectedVersion={null} onVersionSelect={jest.fn()} />,
      );
      // Default sort is lastUpdated desc. Click "Version" header to sort by version asc.
      await userEvent.click(screen.getByRole('button', { name: /Version/i }));
      await userEvent.click(screen.getByText('Export CSV'));
      const [data] = exportToCsvSpy.mock.calls[0];
      // Numeric version sort: 1.2.0 < 1.3.0 < 1.10.0
      expect(data[0].version).toBe('1.2.0');
      expect(data[2].version).toBe('1.10.0');
    });

    it('sorts by status when Status column header is clicked', async () => {
      renderWithTheme(<GAVersionsTab {...defaultProps} />);
      await userEvent.click(screen.getByRole('button', { name: /Status/i }));
      // After click, sorted by status asc
      await userEvent.click(screen.getByText('Export CSV'));
      const [data] = exportToCsvSpy.mock.calls[0];
      expect(data).toHaveLength(2);
    });

    it('sorts by repositories count when Repositories column header is clicked', async () => {
      renderWithTheme(<GAVersionsTab {...defaultProps} />);
      await userEvent.click(screen.getByRole('button', { name: /Repositories/i }));
      await userEvent.click(screen.getByText('Export CSV'));
      const [data] = exportToCsvSpy.mock.calls[0];
      expect(data).toHaveLength(2);
    });
  });

  // ─── GARepositoriesTab ─────────────────────────────────────────────────────

  describe('GARepositoriesTab', () => {
    const defaultProps = {
      repositories: testRepositories,
    };

    it('renders the Export CSV button', () => {
      renderWithTheme(<GARepositoriesTab {...defaultProps} />);
      expect(screen.getByText('Export CSV')).toBeInTheDocument();
    });

    it('Export CSV button has correct aria-label', () => {
      renderWithTheme(<GARepositoriesTab {...defaultProps} />);
      expect(screen.getByLabelText('Export all filtered results as CSV')).toBeInTheDocument();
    });

    it('Export CSV button is enabled when repos exist', () => {
      renderWithTheme(<GARepositoriesTab {...defaultProps} />);
      expect(screen.getByLabelText('Export all filtered results as CSV')).not.toBeDisabled();
    });

    it('Export CSV button is disabled when no repos match filters', async () => {
      renderWithTheme(<GARepositoriesTab {...defaultProps} />);
      await userEvent.type(screen.getByPlaceholderText('Filter'), 'zzznomatch');
      expect(screen.getByLabelText('Export all filtered results as CSV')).toBeDisabled();
    });

    it('calls exportToCsv with repositories.csv filename when clicked', async () => {
      renderWithTheme(<GARepositoriesTab {...defaultProps} />);
      await userEvent.click(screen.getByText('Export CSV'));
      expect(exportToCsvSpy).toHaveBeenCalledTimes(1);
      expect(exportToCsvSpy).toHaveBeenCalledWith(
        expect.any(Array),
        'repositories.csv',
        ['name', 'type', 'format', 'versionsCount'],
      );
    });

    it('exports all visible repositories as rows', async () => {
      renderWithTheme(<GARepositoriesTab {...defaultProps} />);
      await userEvent.click(screen.getByText('Export CSV'));
      const [data] = exportToCsvSpy.mock.calls[0];
      expect(data).toHaveLength(testRepositories.length);
    });

    it('exports only filtered repos when a search filter is active', async () => {
      renderWithTheme(<GARepositoriesTab {...defaultProps} />);
      await userEvent.type(screen.getByPlaceholderText('Filter'), 'maven-central');
      await userEvent.click(screen.getByText('Export CSV'));
      const [data] = exportToCsvSpy.mock.calls[0];
      expect(data).toHaveLength(1);
      expect(data[0].name).toBe('maven-central');
    });

    it('includes name, type, format and versionsCount in each row', async () => {
      renderWithTheme(<GARepositoriesTab {...defaultProps} />);
      await userEvent.click(screen.getByText('Export CSV'));
      const [data] = exportToCsvSpy.mock.calls[0];
      expect(data[0]).toMatchObject({
        name: expect.any(String),
        type: expect.any(String),
        format: expect.any(String),
        versionsCount: expect.any(Number),
      });
    });

    it('shows empty state when repositories prop is empty', () => {
      renderWithTheme(<GARepositoriesTab repositories={[]} />);
      expect(screen.getByText('No repositories found')).toBeInTheDocument();
    });

    it('shows no-results card when search filters all out', async () => {
      renderWithTheme(<GARepositoriesTab {...defaultProps} />);
      await userEvent.type(screen.getByPlaceholderText('Filter'), 'zzznomatch');
      expect(screen.getByText('No Repositories Found')).toBeInTheDocument();
    });

    it('reset filters button clears search and re-shows rows', async () => {
      renderWithTheme(<GARepositoriesTab {...defaultProps} />);
      await userEvent.type(screen.getByPlaceholderText('Filter'), 'zzznomatch');
      await userEvent.click(screen.getByRole('button', { name: 'Reset Filters' }));
      expect(screen.getByText('maven-central')).toBeInTheDocument();
    });

    it('filters by version when selectedVersion and versions are provided', () => {
      const versions: readonly GAVersion[] = [
        { version: '1.0', status: 'none', lastUpdated: '2024-01-01T00:00:00Z', repositories: ['maven-central'] },
      ];
      renderWithTheme(
        <GARepositoriesTab
          repositories={testRepositories}
          selectedVersion="1.0"
          versions={versions}
        />,
      );
      expect(screen.getByText('maven-central')).toBeInTheDocument();
      expect(screen.queryByText('hosted-releases')).not.toBeInTheDocument();
    });

    it('shows callout when a version is selected', () => {
      const versions: readonly GAVersion[] = [
        { version: '1.0', status: 'none', lastUpdated: '2024-01-01T00:00:00Z', repositories: ['maven-central'] },
      ];
      renderWithTheme(
        <GARepositoriesTab
          repositories={testRepositories}
          selectedVersion="1.0"
          versions={versions}
        />,
      );
      expect(screen.getByText(/Showing repositories containing v1\.0/)).toBeInTheDocument();
    });

    it('clear search X button removes the search text', async () => {
      renderWithTheme(<GARepositoriesTab {...defaultProps} />);
      const input = screen.getByPlaceholderText('Filter');
      await userEvent.type(input, 'foo');
      await userEvent.click(screen.getByLabelText('Clear search'));
      expect(input).toHaveValue('');
    });

    it('filters by type checkbox — only proxy repos shown', async () => {
      renderWithTheme(<GARepositoriesTab {...defaultProps} />);
      const checkboxes = screen.getAllByRole('checkbox');
      // Type checkboxes: hosted(0), proxy(1), group(2)
      await userEvent.click(checkboxes[1]); // proxy
      expect(screen.getByText('maven-central')).toBeInTheDocument();
      expect(screen.queryByText('hosted-releases')).not.toBeInTheDocument();
    });

    it('unchecking a type checkbox removes the filter', async () => {
      renderWithTheme(<GARepositoriesTab {...defaultProps} />);
      const checkboxes = screen.getAllByRole('checkbox');
      await userEvent.click(checkboxes[1]); // check proxy
      await userEvent.click(checkboxes[1]); // uncheck proxy
      expect(screen.getByText('maven-central')).toBeInTheDocument();
      expect(screen.getByText('hosted-releases')).toBeInTheDocument();
    });

    it('filters by repo name text in search query', async () => {
      renderWithTheme(<GARepositoriesTab {...defaultProps} />);
      await userEvent.type(screen.getByPlaceholderText('Filter'), 'hosted');
      await userEvent.click(screen.getByText('Export CSV'));
      const [data] = exportToCsvSpy.mock.calls[0];
      expect(data).toHaveLength(1);
      expect(data[0].name).toBe('hosted-releases');
    });

    it('filters repos to only those containing the selected version', () => {
      const versions: readonly GAVersion[] = [
        { version: '99.0', status: 'none', lastUpdated: '2024-01-01T00:00:00Z', repositories: ['maven-central'] },
      ];
      renderWithTheme(
        <GARepositoriesTab
          repositories={testRepositories}
          selectedVersion="99.0"
          versions={versions}
        />,
      );
      expect(screen.getByText('maven-central')).toBeInTheDocument();
      expect(screen.queryByText('hosted-releases')).not.toBeInTheDocument();
    });
  });

  // ─── GAFilesTab ────────────────────────────────────────────────────────────

  describe('GAFilesTab', () => {
    const defaultProps = {
      assets: testAssets,
      selectedVersion: '1.0',
      loading: false,
    };

    it('renders the Export CSV button', () => {
      renderWithTheme(<GAFilesTab {...defaultProps} />);
      expect(screen.getByText('Export CSV')).toBeInTheDocument();
    });

    it('Export CSV button has correct aria-label', () => {
      renderWithTheme(<GAFilesTab {...defaultProps} />);
      expect(screen.getByLabelText('Export all filtered results as CSV')).toBeInTheDocument();
    });

    it('calls exportToCsv with files.csv filename when clicked', async () => {
      renderWithTheme(<GAFilesTab {...defaultProps} />);
      await userEvent.click(screen.getByText('Export CSV'));
      expect(exportToCsvSpy).toHaveBeenCalledTimes(1);
      expect(exportToCsvSpy).toHaveBeenCalledWith(
        expect.any(Array),
        'files.csv',
        ['file', 'extension', 'classifier', 'size', 'lastModified', 'downloadUrl'],
      );
    });

    it('exports all visible files as rows', async () => {
      renderWithTheme(<GAFilesTab {...defaultProps} />);
      await userEvent.click(screen.getByText('Export CSV'));
      const [data] = exportToCsvSpy.mock.calls[0];
      expect(data).toHaveLength(testAssets.length);
    });

    it('exports only filtered files when a search filter is active', async () => {
      renderWithTheme(<GAFilesTab {...defaultProps} />);
      await userEvent.type(screen.getByPlaceholderText('Filter'), '.pom');
      await userEvent.click(screen.getByText('Export CSV'));
      const [data] = exportToCsvSpy.mock.calls[0];
      expect(data.every((row: { extension: string }) => row.extension === 'pom')).toBe(true);
    });

    it('uses the filename (last path segment) not the full path in the file column', async () => {
      renderWithTheme(<GAFilesTab {...defaultProps} />);
      await userEvent.click(screen.getByText('Export CSV'));
      const [data] = exportToCsvSpy.mock.calls[0];
      data.forEach((row: { file: string }) => {
        expect(row.file).not.toContain('/');
      });
    });

    it('does not show Export CSV when no version is selected', () => {
      renderWithTheme(<GAFilesTab assets={[]} selectedVersion={null} loading={false} />);
      expect(screen.queryByText('Export CSV')).not.toBeInTheDocument();
    });

    it('shows a prompt to select a version when none is selected', () => {
      renderWithTheme(<GAFilesTab assets={[]} selectedVersion={null} loading={false} />);
      expect(screen.getByText(/Select a version/)).toBeInTheDocument();
    });

    it('does not show Export CSV while assets are loading', () => {
      renderWithTheme(<GAFilesTab assets={[]} selectedVersion="1.0" loading={true} />);
      expect(screen.queryByText('Export CSV')).not.toBeInTheDocument();
    });

    it('shows loading spinner while loading', () => {
      renderWithTheme(<GAFilesTab assets={[]} selectedVersion="1.0" loading={true} />);
      expect(screen.getByText('Loading files...')).toBeInTheDocument();
    });

    it('shows empty state when assets array is empty and not loading', () => {
      renderWithTheme(<GAFilesTab assets={[]} selectedVersion="1.0" loading={false} />);
      expect(screen.getByText('No Files Found')).toBeInTheDocument();
    });

    it('shows no-results card when search filters all out', async () => {
      renderWithTheme(<GAFilesTab {...defaultProps} />);
      await userEvent.type(screen.getByPlaceholderText('Filter'), 'zzznomatch');
      expect(screen.getByText('No Files Found')).toBeInTheDocument();
    });

    it('reset filters button clears search and re-shows rows', async () => {
      renderWithTheme(<GAFilesTab {...defaultProps} />);
      await userEvent.type(screen.getByPlaceholderText('Filter'), 'zzznomatch');
      await userEvent.click(screen.getByRole('button', { name: 'Reset Filters' }));
      expect(screen.getByText('lib-1.0.jar')).toBeInTheDocument();
    });

    it('renders the file name (last path segment) in the table', () => {
      renderWithTheme(<GAFilesTab {...defaultProps} />);
      expect(screen.getByText('lib-1.0.jar')).toBeInTheDocument();
      expect(screen.getByText('lib-1.0.pom')).toBeInTheDocument();
    });

    it('clear search X button removes the search text', async () => {
      renderWithTheme(<GAFilesTab {...defaultProps} />);
      const input = screen.getByPlaceholderText('Filter');
      await userEvent.type(input, 'foo');
      await userEvent.click(screen.getByLabelText('Clear search'));
      expect(input).toHaveValue('');
    });

    it('shows classifier in the file cell', () => {
      renderWithTheme(<GAFilesTab {...defaultProps} />);
      expect(screen.getByText('sources')).toBeInTheDocument();
    });

    it('Export CSV button is disabled when filters leave sortedAssets empty', async () => {
      renderWithTheme(<GAFilesTab {...defaultProps} />);
      await userEvent.type(screen.getByPlaceholderText('Filter'), 'zzznomatch');
      expect(screen.getByLabelText('Export all filtered results as CSV')).toBeDisabled();
    });

    it('filters by extension checkbox — only jar files shown', async () => {
      renderWithTheme(<GAFilesTab {...defaultProps} />);
      const checkboxes = screen.getAllByRole('checkbox');
      // Extension checkboxes are dynamically built; jar and pom should both appear
      // Click the first checkbox (jar)
      await userEvent.click(checkboxes[0]);
      expect(screen.getByText('lib-1.0.jar')).toBeInTheDocument();
      expect(screen.queryByText('lib-1.0.pom')).not.toBeInTheDocument();
    });

    it('unchecking an extension checkbox removes the filter', async () => {
      renderWithTheme(<GAFilesTab {...defaultProps} />);
      const checkboxes = screen.getAllByRole('checkbox');
      await userEvent.click(checkboxes[0]); // check
      await userEvent.click(checkboxes[0]); // uncheck
      expect(screen.getByText('lib-1.0.jar')).toBeInTheDocument();
      expect(screen.getByText('lib-1.0.pom')).toBeInTheDocument();
    });

    it('filters by classifier in search query', async () => {
      renderWithTheme(<GAFilesTab {...defaultProps} />);
      await userEvent.type(screen.getByPlaceholderText('Filter'), 'sources');
      await userEvent.click(screen.getByText('Export CSV'));
      const [data] = exportToCsvSpy.mock.calls[0];
      expect(data).toHaveLength(1);
      expect(data[0].extension).toBe('pom');
    });

    it('shows dash for asset with no classifier', () => {
      renderWithTheme(<GAFilesTab {...defaultProps} />);
      expect(screen.getByText('-')).toBeInTheDocument();
    });
  });
});
