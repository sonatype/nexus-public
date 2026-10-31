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
import { GAFilesTab } from '../GAFilesTab';
import * as exportToCsvModule from '../../../../shared/utils/exportToCsv';
import { deriveFileType, type GAAsset } from '../../core';

// Mock Radix UI DropdownMenu to avoid portal issues in tests (portals don't render reliably in
// jsdom — same pattern as RepositoriesList.test.tsx). The menu content renders inline, so the
// action items are queryable as buttons without opening a real popover.
jest.mock('@radix-ui/themes', () => {
  const actual = jest.requireActual('@radix-ui/themes');
  return {
    ...actual,
    DropdownMenu: {
      Root: ({ children, ...props }: any) => (
        <div data-testid="dropdown-root" {...props}>
          {children}
        </div>
      ),
      Trigger: ({ children, ...props }: any) => <div {...props}>{children}</div>,
      Content: ({ children, align, ...props }: any) => (
        <div data-testid="dropdown-content" data-align={align} {...props}>
          {children}
        </div>
      ),
      Item: ({ children, onClick, color, ...props }: any) => (
        <button {...props} onClick={onClick} data-color={color} type="button">
          {children}
        </button>
      ),
    },
  };
});

const mockGo = jest.fn();
jest.mock('@uirouter/react', () => ({
  useRouter: () => ({
    stateService: { go: mockGo },
  }),
}));

const renderWithProviders = (ui: React.ReactElement) =>
  render(<Theme>{ui}</Theme>);

const asset: GAAsset = {
  id: 'asset-jar',
  repository: 'maven-central',
  path: 'org/apache/maven/maven-core/3.9.6/maven-core-3.9.6.jar',
  downloadUrl:
    'http://localhost/repository/maven-central/org/apache/maven/maven-core/3.9.6/maven-core-3.9.6.jar',
  format: 'application/java-archive',
  extension: 'jar',
  size: 1234567,
  contentType: 'application/java-archive',
  lastModified: '2024-01-15T10:00:00Z',
  checksums: {},
} as GAAsset;

describe('GAFilesTab row actions', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('labels the browse action "View in Browse", not "Browse"', () => {
    renderWithProviders(
      <GAFilesTab assets={[asset]} selectedVersion="3.9.6" loading={false} />,
    );

    expect(
      screen.getByRole('button', { name: /view .* in browse/i }),
    ).toBeInTheDocument();
    // The old, ambiguous "Browse" label must be gone.
    expect(
      screen.queryByRole('button', { name: /^Browse$/ }),
    ).not.toBeInTheDocument();
  });

  it('gives the actions accessible names that include the file name', () => {
    renderWithProviders(
      <GAFilesTab assets={[asset]} selectedVersion="3.9.6" loading={false} />,
    );

    expect(
      screen.getByRole('button', {
        name: 'View maven-core-3.9.6.jar in Browse',
      }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'Download maven-core-3.9.6.jar' }),
    ).toBeInTheDocument();
  });

  // Reproduces the NEXUS-54430 bug: the browse action used to set
  // window.location.hash = '#browse/browse:<repo>:<path>' (the Classic UI bookmark), dropping the
  // user out of the new UI. It must instead navigate the router to the new Preview Browse route.
  it('navigates to the new Preview Browse route, never the Classic UI hash', async () => {
    const initialHash = window.location.hash;

    renderWithProviders(
      <GAFilesTab assets={[asset]} selectedVersion="3.9.6" loading={false} />,
    );

    await userEvent.click(
      screen.getByRole('button', { name: /view .* in browse/i }),
    );

    expect(mockGo).toHaveBeenCalledWith('preview.browse.browse.repo.path', {
      repoName: 'maven-central',
      path: 'org/apache/maven/maven-core/3.9.6/maven-core-3.9.6.jar',
      tab: 'summary',
    });
    // The Classic UI hash must never be written.
    expect(window.location.hash).toBe(initialHash);
    expect(window.location.hash).not.toContain('#browse/browse:');
  });

  // When the view aggregates assets across repositories (no repo fixed in the header), each row's
  // browse action must resolve to its OWN asset's repository — not a single shared repo — so a
  // component present in several repositories deep-links each file correctly (NEXUS-54430).
  it('deep-links each row to its own asset repository when assets span repositories', async () => {
    const assetB: GAAsset = {
      ...asset,
      id: 'asset-jar-2',
      repository: 'maven-releases',
      path: 'org/apache/maven/maven-core/3.9.6/maven-core-3.9.6-sources.jar',
      downloadUrl:
        'http://localhost/repository/maven-releases/org/apache/maven/maven-core/3.9.6/maven-core-3.9.6-sources.jar',
    } as GAAsset;

    renderWithProviders(
      <GAFilesTab assets={[asset, assetB]} selectedVersion="3.9.6" loading={false} />,
    );

    await userEvent.click(
      screen.getByRole('button', {
        name: 'View maven-core-3.9.6-sources.jar in Browse',
      }),
    );
    expect(mockGo).toHaveBeenLastCalledWith('preview.browse.browse.repo.path', {
      repoName: 'maven-releases',
      path: 'org/apache/maven/maven-core/3.9.6/maven-core-3.9.6-sources.jar',
      tab: 'summary',
    });

    await userEvent.click(
      screen.getByRole('button', { name: 'View maven-core-3.9.6.jar in Browse' }),
    );
    expect(mockGo).toHaveBeenLastCalledWith('preview.browse.browse.repo.path', {
      repoName: 'maven-central',
      path: 'org/apache/maven/maven-core/3.9.6/maven-core-3.9.6.jar',
      tab: 'summary',
    });
  });

  it('opens the download URL when Download is chosen', async () => {
    const openSpy = jest.spyOn(window, 'open').mockImplementation(() => null);

    renderWithProviders(
      <GAFilesTab assets={[asset]} selectedVersion="3.9.6" loading={false} />,
    );

    await userEvent.click(
      screen.getByRole('button', { name: /^Download / }),
    );

    expect(openSpy).toHaveBeenCalledWith(asset.downloadUrl, '_blank');
    openSpy.mockRestore();
  });
});

describe('GAFilesTab Type column (NEXUS-54612)', () => {
  let exportToCsvSpy: jest.SpyInstance;

  beforeEach(() => {
    jest.clearAllMocks();
    exportToCsvSpy = jest.spyOn(exportToCsvModule, 'exportToCsv').mockImplementation(() => {});
  });

  afterEach(() => {
    exportToCsvSpy.mockRestore();
  });

  /**
   * A NuGet package as the mappers now produce it: the path carries no extension, so the
   * type comes from the format. Before the fix this column showed "16343" — the tail of the
   * version — for exactly this shape.
   */
  const nugetAsset = {
    ...asset,
    id: 'asset-nupkg',
    repository: 'nuget-hosted',
    path: '/System.Net.Http/2.0.20126.16343',
    downloadUrl: 'http://localhost/repository/nuget-hosted/System.Net.Http/2.0.20126.16343',
    format: 'application/octet-stream',
    extension: deriveFileType({ path: '/System.Net.Http/2.0.20126.16343', format: 'nuget' }),
    contentType: 'application/octet-stream',
  } as GAAsset;

  /** The row for the fixture, located by the file-name cell (the path's last segment). */
  const nugetRow = () => {
    const fileCell = screen.getByText('2.0.20126.16343');
    const row = fileCell.closest('tr');
    if (!row) {
      throw new Error('file name cell is not inside a row');
    }
    return row;
  };

  it('renders the derived type and never a version fragment (AT-004)', () => {
    renderWithProviders(
      <GAFilesTab assets={[nugetAsset]} selectedVersion="2.0.20126.16343" loading={false} />,
    );

    // Scoped to the row: "nupkg" also appears as the Type filter option beside the table.
    expect(within(nugetRow()).getByText('nupkg')).toBeInTheDocument();
    // The defect value, as its own cell. The full version still shows in the file-name cell,
    // so this asserts the bare fragment is gone rather than that "16343" appears nowhere.
    expect(within(nugetRow()).queryByText('16343')).not.toBeInTheDocument();
  });

  it('offers the derived type as a filter option and keeps the row when selected (AT-004)', async () => {
    renderWithProviders(
      <GAFilesTab assets={[nugetAsset]} selectedVersion="2.0.20126.16343" loading={false} />,
    );

    // The only checkbox is the Type filter, built from the assets' derived types.
    const [typeCheckbox] = screen.getAllByRole('checkbox');
    await userEvent.click(typeCheckbox);

    expect(within(nugetRow()).getByText('nupkg')).toBeInTheDocument();
  });

  it('exports the same type the column displays (AT-005)', async () => {
    renderWithProviders(
      <GAFilesTab assets={[nugetAsset]} selectedVersion="2.0.20126.16343" loading={false} />,
    );

    await userEvent.click(screen.getByText('Export CSV'));

    const [rows] = exportToCsvSpy.mock.calls[0] as [Array<{ extension: string }>];
    expect(rows[0].extension).toBe('nupkg');
  });
});
