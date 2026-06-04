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

const mockGetValue = jest.fn().mockReturnValue(false);
jest.mock('../../../../../interface/ExtJS', () => ({
  ExtJS: {
    state: () => ({ getValue: mockGetValue }),
  },
}));

const MOCK_REPOS: RepoWithProtection[] = [
  {
    name: 'maven-central',
    format: 'maven2',
    type: 'proxy',
    url: '',
    rhcEnabled: true,
    protection: 'quarantine',
    taskEnabled: true,
    malwareCount: 3,
  },
  {
    name: 'npm-proxy',
    format: 'npm',
    type: 'proxy',
    url: '',
    rhcEnabled: false,
    protection: 'audit',
    taskEnabled: false,
    malwareCount: 0,
  },
];

const DEFAULT_PROTECT_DATA = {
  repos: MOCK_REPOS,
  loading: false,
  refetch: jest.fn(),
  error: null,
  filterCounts: {
    formats: new Map(),
    protection: new Map(),
    healthCheck: { enabled: 0, disabled: 0, unsupported: 0 },
    cleanup: { active: 0, off: 0 },
  },
  hcSummary: {
    repos: [],
    enabledCount: 1,
    totalProxyCount: 2,
    totalSecurityIssues: 5,
    totalLicenseIssues: 2,
    loading: false,
    error: null,
    refetch: jest.fn(),
  },
  iqAudit: {
    counts: {
      reposProtected: 1,
      reposInAudit: 1,
      reposUnprotected: 0,
      isPartial: true,
      hasUnsupportedFormats: false,
    },
    loading: false,
    error: null,
  },
  hasFirewall: true,
  hasIqConnection: true,
  hcInstanceEnabled: true,
  canUpdateHealthCheck: true,
  lastAnalyzedByRepo: new Map(),
  iqCapabilities: {
    connected: true,
    hasFirewall: true,
    hasLifecycle: true,
    url: 'https://iq.example.com',
    deploymentId: 'dep-1',
  },
};

import ProtectOverview from '../ProtectOverview';

const renderWithTheme = (ui: React.ReactElement) => render(<Theme>{ui}</Theme>);

describe('ProtectOverview', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockGetValue.mockReturnValue(false);
  });

  it('renders loading state', () => {
    renderWithTheme(
      <ProtectOverview
        protectData={{ ...DEFAULT_PROTECT_DATA, loading: true }}
        onGoToQuickConfig={jest.fn()}
      />
    );
    expect(screen.getByTestId('protect-overview-skeleton')).toBeInTheDocument();
  });

  it('renders three cards: Health Check, Firewall, Malware', () => {
    renderWithTheme(
      <ProtectOverview protectData={DEFAULT_PROTECT_DATA} onGoToQuickConfig={jest.fn()} />
    );
    expect(screen.getByText('Health Check')).toBeInTheDocument();
    expect(screen.getByText('Firewall Protection')).toBeInTheDocument();
    expect(screen.getByText('OSS Malware Cleanup')).toBeInTheDocument();
  });

  it('shows Firewall Active summary when IQ capabilities include Firewall license', () => {
    renderWithTheme(
      <ProtectOverview protectData={DEFAULT_PROTECT_DATA} onGoToQuickConfig={jest.fn()} />
    );
    expect(screen.getByTestId('protect-overview-firewall-active')).toBeInTheDocument();
    expect(screen.getByText('Firewall Active')).toBeInTheDocument();
    expect(screen.getByText(/Quarantine enabled on 1\/2 proxies/)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /View Firewall Dashboard/i })).toHaveAttribute(
      'href',
      'https://iq.example.com/assets/index.html#/firewall/dashboard/'
    );
  });

  it('shows upgrade callout when IQ capabilities report no Firewall license', () => {
    renderWithTheme(
      <ProtectOverview
        protectData={{
          ...DEFAULT_PROTECT_DATA,
          iqCapabilities: { connected: true, hasFirewall: false, hasLifecycle: true },
        }}
        onGoToQuickConfig={jest.fn()}
      />
    );
    expect(screen.getByTestId('protect-overview-firewall-upsell')).toBeInTheDocument();
    expect(screen.getByText(/Upgrade your protection with Sonatype Repository Firewall/)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Learn About Firewall/i })).toHaveAttribute(
      'href',
      'https://links.sonatype.com/nexus-repository-firewall'
    );
  });

  it('shows health check enabled count', () => {
    renderWithTheme(
      <ProtectOverview protectData={DEFAULT_PROTECT_DATA} onGoToQuickConfig={jest.fn()} />
    );
    expect(screen.getAllByText('1 / 2').length).toBe(2);
    expect(screen.getByText(/proxy repositories with Health Check enabled/)).toBeInTheDocument();
  });

  it('shows security and license issue counts', () => {
    renderWithTheme(
      <ProtectOverview protectData={DEFAULT_PROTECT_DATA} onGoToQuickConfig={jest.fn()} />
    );
    expect(screen.getByText(/5 security issues/)).toBeInTheDocument();
    expect(screen.getByText(/2 license issues/)).toBeInTheDocument();
  });

  it('shows firewall protection counts', () => {
    renderWithTheme(
      <ProtectOverview protectData={DEFAULT_PROTECT_DATA} onGoToQuickConfig={jest.fn()} />
    );
    expect(screen.getByText(/1 Quarantine/)).toBeInTheDocument();
    expect(screen.getByText(/1 Audit/)).toBeInTheDocument();
  });

  it('shows cleanup stats', () => {
    renderWithTheme(
      <ProtectOverview protectData={DEFAULT_PROTECT_DATA} onGoToQuickConfig={jest.fn()} />
    );
    expect(screen.getAllByText('1 / 2').length).toBe(2);
    expect(screen.getByText(/repos with OSS malware cleanup task active/)).toBeInTheDocument();
  });

  it('shows pending malware count', () => {
    renderWithTheme(
      <ProtectOverview protectData={DEFAULT_PROTECT_DATA} onGoToQuickConfig={jest.fn()} />
    );
    expect(screen.getByText(/3 pending malware components/)).toBeInTheDocument();
  });

  it('shows IQ not connected message when hasIqConnection is false', () => {
    renderWithTheme(
      <ProtectOverview
        protectData={{ ...DEFAULT_PROTECT_DATA, hasIqConnection: false }}
        onGoToQuickConfig={jest.fn()}
      />
    );
    expect(screen.getByText(/IQ Server is not connected/)).toBeInTheDocument();
  });

  it('shows HC capability disabled warning when hcInstanceEnabled is false', () => {
    renderWithTheme(
      <ProtectOverview
        protectData={{ ...DEFAULT_PROTECT_DATA, hcInstanceEnabled: false }}
        onGoToQuickConfig={jest.fn()}
      />
    );
    expect(screen.getByText(/Health Check capability is disabled/)).toBeInTheDocument();
  });

  it('shows no-permission message when canUpdateHealthCheck is false', () => {
    renderWithTheme(
      <ProtectOverview
        protectData={{ ...DEFAULT_PROTECT_DATA, canUpdateHealthCheck: false }}
        onGoToQuickConfig={jest.fn()}
      />
    );
    expect(screen.getByText(/You do not have permission/)).toBeInTheDocument();
  });

  it('shows hcSummary error when present', () => {
    renderWithTheme(
      <ProtectOverview
        protectData={{
          ...DEFAULT_PROTECT_DATA,
          hcSummary: { ...DEFAULT_PROTECT_DATA.hcSummary, error: 'Health check unavailable' },
        }}
        onGoToQuickConfig={jest.fn()}
      />
    );
    expect(screen.getByText('Health check unavailable')).toBeInTheDocument();
  });

  it('calls onGoToQuickConfig when button clicked', async () => {
    const handler = jest.fn();
    renderWithTheme(
      <ProtectOverview protectData={DEFAULT_PROTECT_DATA} onGoToQuickConfig={handler} />
    );
    await userEvent.click(screen.getByRole('button', { name: /Go to Quick Config/ }));
    expect(handler).toHaveBeenCalledTimes(1);
  });

  describe('cloud mode', () => {
    beforeEach(() => {
      mockGetValue.mockReturnValue(true);
    });

    it('hides Connect IQ button in cloud when IQ not connected', () => {
      renderWithTheme(
        <ProtectOverview
          protectData={{ ...DEFAULT_PROTECT_DATA, hasIqConnection: false }}
          onGoToQuickConfig={jest.fn()}
        />
      );
      expect(screen.getByText(/Contact your administrator/)).toBeInTheDocument();
      expect(screen.queryByRole('link', { name: /Connect IQ/ })).not.toBeInTheDocument();
    });

    it('hides Open Capabilities button in cloud when HC disabled', () => {
      renderWithTheme(
        <ProtectOverview
          protectData={{ ...DEFAULT_PROTECT_DATA, hcInstanceEnabled: false }}
          onGoToQuickConfig={jest.fn()}
        />
      );
      expect(screen.getByText(/contact your administrator/)).toBeInTheDocument();
      expect(screen.queryByRole('link', { name: /Open Capabilities/ })).not.toBeInTheDocument();
    });
  });
});
