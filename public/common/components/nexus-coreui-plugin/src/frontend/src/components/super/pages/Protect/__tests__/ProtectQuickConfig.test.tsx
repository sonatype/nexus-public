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
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';

import type { RepoWithProtection } from '../../MalwareRisk/useQuickActionsData';

const MOCK_REPOS: RepoWithProtection[] = [
  {
    name: 'maven-central',
    format: 'maven2',
    type: 'proxy',
    url: '',
    rhcEnabled: true,
    protection: 'quarantine',
    taskEnabled: true,
    taskCleanupEnabled: true,
    malwareCount: 0,
  },
  {
    name: 'npm-proxy',
    format: 'npm',
    type: 'proxy',
    url: '',
    rhcEnabled: false,
    protection: 'none',
    taskEnabled: false,
    taskCleanupEnabled: false,
    malwareCount: 2,
  },
];

const DEFAULT_PROTECT_DATA = {
  repos: MOCK_REPOS,
  loading: false,
  error: null,
  refetch: jest.fn(),
  filterCounts: {
    formats: new Map([
      ['maven2', 1],
      ['npm', 1],
    ]),
    protection: new Map([
      ['quarantine', 1],
      ['none', 1],
    ]),
    healthCheck: { enabled: 1, disabled: 1, unsupported: 0 },
    cleanup: { delete: 1, audit: 0, off: 1 },
  },
  iqCapabilities: null,
  hasFirewall: true,
  hasIqConnection: true,
  hcInstanceEnabled: true,
  canUpdateHealthCheck: true,
  lastAnalyzedByRepo: new Map(),
  hcSummary: {
    loading: false,
    error: null,
    enabledCount: 0,
    totalProxyCount: 0,
    totalSecurityIssues: 0,
    totalLicenseIssues: 0,
    repos: [],
    refetch: jest.fn(),
  },
  iqAudit: { counts: null, loading: false, error: null },
};

jest.mock('../ProtectFilterSidebar', () => ({
  __esModule: true,
  default: ({ disabled }: any) => (
    <div data-testid="protect-filter-sidebar" data-disabled={String(!!disabled)}>
      Filter Sidebar
    </div>
  ),
}));

jest.mock('../ProtectRepoRow', () => ({
  __esModule: true,
  default: ({ repo }: any) => (
    <tr data-testid={`protect-repo-row-${repo.name}`}>
      <td>{repo.name}</td>
    </tr>
  ),
}));

jest.mock('../ProtectBulkActionModal', () => ({
  __esModule: true,
  default: () => null,
}));

jest.mock('@/utils/firewallFormats', () => ({
  isFirewallSupportedFormat: (f: string) => ['maven2', 'npm'].includes(f),
}));

import ProtectQuickConfig from '../ProtectQuickConfig';

const renderWithTheme = (ui: React.ReactElement) => render(<Theme>{ui}</Theme>);

describe('ProtectQuickConfig', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders the Quick Config heading', () => {
    renderWithTheme(<ProtectQuickConfig protectData={DEFAULT_PROTECT_DATA} />);
    expect(screen.getByText('Quick Config')).toBeInTheDocument();
  });

  it('renders filter sidebar', () => {
    renderWithTheme(<ProtectQuickConfig protectData={DEFAULT_PROTECT_DATA} />);
    expect(screen.getByTestId('protect-filter-sidebar')).toBeInTheDocument();
  });

  it('renders search input', () => {
    renderWithTheme(<ProtectQuickConfig protectData={DEFAULT_PROTECT_DATA} />);
    expect(screen.getByPlaceholderText('Filter by name…')).toBeInTheDocument();
  });

  it('shows repo count', () => {
    renderWithTheme(<ProtectQuickConfig protectData={DEFAULT_PROTECT_DATA} />);
    expect(screen.getByText(/Showing 2 of 2 proxy repositories/)).toBeInTheDocument();
  });

  it('renders all repository rows', () => {
    renderWithTheme(<ProtectQuickConfig protectData={DEFAULT_PROTECT_DATA} />);
    expect(screen.getByTestId('protect-repo-row-maven-central')).toBeInTheDocument();
    expect(screen.getByTestId('protect-repo-row-npm-proxy')).toBeInTheDocument();
  });

  it('filters repos by search text', async () => {
    renderWithTheme(<ProtectQuickConfig protectData={DEFAULT_PROTECT_DATA} />);
    const searchInput = screen.getByPlaceholderText('Filter by name…');
    await userEvent.type(searchInput, 'maven');
    expect(screen.getByTestId('protect-repo-row-maven-central')).toBeInTheDocument();
    expect(screen.queryByTestId('protect-repo-row-npm-proxy')).not.toBeInTheDocument();
  });

  it('renders loading state', () => {
    renderWithTheme(<ProtectQuickConfig protectData={{ ...DEFAULT_PROTECT_DATA, loading: true }} />);
    expect(screen.getByTestId('protect-quick-config-skeleton')).toBeInTheDocument();
  });

  it('renders error state', () => {
    renderWithTheme(
      <ProtectQuickConfig protectData={{ ...DEFAULT_PROTECT_DATA, error: 'Something went wrong' }} />
    );
    expect(screen.getByText('Something went wrong')).toBeInTheDocument();
  });

  it('shows Enable Health Check button', () => {
    renderWithTheme(<ProtectQuickConfig protectData={DEFAULT_PROTECT_DATA} />);
    expect(screen.getByRole('button', { name: /Enable Health Check on visible/ })).toBeInTheDocument();
  });

  it('shows Enable Firewall button when hasFirewall', () => {
    renderWithTheme(<ProtectQuickConfig protectData={DEFAULT_PROTECT_DATA} />);
    expect(screen.getByText(/Enable Firewall on visible/)).toBeInTheDocument();
  });

  it('hides Firewall/cleanup buttons when hasFirewall is false', () => {
    renderWithTheme(
      <ProtectQuickConfig protectData={{ ...DEFAULT_PROTECT_DATA, hasFirewall: false }} />
    );
    expect(screen.queryByText(/Enable Firewall on visible/)).not.toBeInTheDocument();
    expect(screen.queryByText(/Enable cleanup on visible/)).not.toBeInTheDocument();
  });
});
