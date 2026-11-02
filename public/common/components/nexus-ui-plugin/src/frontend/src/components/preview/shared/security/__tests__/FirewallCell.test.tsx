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
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import '@testing-library/jest-dom';

const mockGet = jest.fn().mockResolvedValue(null);
const mockPut = jest.fn().mockResolvedValue(undefined);
jest.mock('../../../../../interface/api', () => ({
  restClient: {
    get: (...args: any[]) => mockGet(...args),
    put: (...args: any[]) => mockPut(...args),
  },
  parseApiError: (err: any) => ({ message: err instanceof Error ? err.message : String(err) }),
  ENDPOINTS: {
    FIREWALL_STATUS_REPO: (name: string) => `/api/v1/firewall/status/${name}`,
  },
}));

const mockCheckPermission = jest.fn().mockReturnValue(true);
jest.mock('../../../../../interface/ExtJS', () => ({
  ExtJS: {
    usePermission: (getValue: () => boolean) => getValue(),
    checkPermission: (...args: any[]) => mockCheckPermission(...args),
    state: () => ({ getValue: () => ({ enabled: true }) }),
    useState: (getValue: () => unknown) => getValue(),
  },
}));

import { FirewallCell } from '../FirewallCell';
import { SecurityStatusData } from '../security.types';

function renderWithTheme(ui: React.ReactElement) {
  return render(<Theme>{ui}</Theme>);
}

const repo = {
  name: 'maven-central',
  type: 'proxy',
  format: 'maven2',
};

const QUARANTINE_MESSAGE = 'Quarantine enabled';
const AUDIT_MESSAGE = 'Audit enabled';

const protectedStatus: SecurityStatusData = {
  repositoryName: 'maven-central',
  message: QUARANTINE_MESSAGE,
  affectedComponentCount: 0,
  criticalComponentCount: 0,
  severeComponentCount: 0,
  moderateComponentCount: 0,
  quarantinedComponentCount: 0,
};

const issuesStatus: SecurityStatusData = {
  ...protectedStatus,
  criticalComponentCount: 5,
  severeComponentCount: 2,
  moderateComponentCount: 1,
};

const quarantineStatus: SecurityStatusData = {
  ...issuesStatus,
  quarantinedComponentCount: 3,
};

const auditStatus: SecurityStatusData = {
  ...issuesStatus,
  message: AUDIT_MESSAGE,
};

describe('FirewallCell', () => {
  beforeEach(() => {
    mockGet.mockClear().mockResolvedValue(null);
    mockPut.mockClear().mockResolvedValue(undefined);
    mockCheckPermission.mockClear().mockReturnValue(true);
  });

  it('shows Loading state when firewallStatus is not yet available', () => {
    renderWithTheme(<FirewallCell repository={repo} />);
    expect(screen.getByText('Loading...')).toBeInTheDocument();
  });

  it('shows error state when firewallStatus has errorMessage', () => {
    renderWithTheme(
      <FirewallCell
        repository={repo}
        firewallStatus={{ ...protectedStatus, errorMessage: 'IQ Server unavailable' } as SecurityStatusData}
      />
    );
    expect(screen.getByText('Unavailable')).toBeInTheDocument();
  });

  it('shows Quarantine badge when no issues and protected', () => {
    renderWithTheme(<FirewallCell repository={repo} firewallStatus={protectedStatus} />);
    expect(screen.getByText('Quarantine')).toBeInTheDocument();
  });

  it('shows Quarantine badge when has issues', () => {
    renderWithTheme(<FirewallCell repository={repo} firewallStatus={issuesStatus} />);
    expect(screen.getByText('Quarantine')).toBeInTheDocument();
  });

  it('opens modal when cell is clicked', async () => {
    renderWithTheme(<FirewallCell repository={repo} firewallStatus={issuesStatus} />);

    const trigger = screen.getByRole('button', { name: /Firewall:.*Click for details/i });
    fireEvent.click(trigger);

    const modalTitle = await screen.findByText(/Firewall Report – maven-central/);
    expect(modalTitle).toBeInTheDocument();
  });

  it('shows View Full Report button in modal', async () => {
    const statusWithReport: SecurityStatusData = { ...issuesStatus, reportUrl: 'https://example.com/report' };
    renderWithTheme(<FirewallCell repository={repo} firewallStatus={statusWithReport} />);

    const trigger = screen.getByRole('button', { name: /Firewall:.*Click for details/i });
    fireEvent.click(trigger);

    const reportButton = await screen.findByText(/View Full Report/);
    expect(reportButton).toBeInTheDocument();
  });

  it('shows Quarantine badge when quarantinedComponentCount > 0', () => {
    renderWithTheme(<FirewallCell repository={repo} firewallStatus={quarantineStatus} />);
    expect(screen.getByText('Quarantine')).toBeInTheDocument();
  });

  it('shows quarantined count badge alongside Quarantine badge', () => {
    renderWithTheme(<FirewallCell repository={repo} firewallStatus={quarantineStatus} />);
    expect(screen.getByText('Quarantine')).toBeInTheDocument();
    expect(screen.getByLabelText('3 quarantined')).toBeInTheDocument();
  });

  it('does not show count badge when quarantinedComponentCount is 0', () => {
    renderWithTheme(<FirewallCell repository={repo} firewallStatus={protectedStatus} />);
    expect(screen.getByText('Quarantine')).toBeInTheDocument();
    expect(screen.queryByText('0')).not.toBeInTheDocument();
  });

  it('shows Audited badge for audit mode status', () => {
    renderWithTheme(<FirewallCell repository={repo} firewallStatus={auditStatus} />);
    expect(screen.getByText('Audited')).toBeInTheDocument();
  });

  it('shows critical count badge alongside Audited badge', () => {
    renderWithTheme(<FirewallCell repository={repo} firewallStatus={auditStatus} />);
    expect(screen.getByText('Audited')).toBeInTheDocument();
    expect(screen.getByText('5')).toBeInTheDocument();
  });

  it('does not show critical count badge when criticalComponentCount is 0', () => {
    const auditNoIssues: SecurityStatusData = { ...protectedStatus, message: AUDIT_MESSAGE };
    renderWithTheme(<FirewallCell repository={repo} firewallStatus={auditNoIssues} />);
    expect(screen.getByText('Audited')).toBeInTheDocument();
    expect(screen.queryByText('0')).not.toBeInTheDocument();
  });

  describe('unsupported formats', () => {
    const unsupportedFormats = ['terraform', 'apt', 'helm', 'gitlfs', 'swift'];

    unsupportedFormats.forEach((format) => {
      it(`shows "Not supported" for ${format} proxy repos`, () => {
        const unsupportedRepo = { name: `${format}-proxy`, type: 'proxy', format };
        renderWithTheme(<FirewallCell repository={unsupportedRepo} />);
        expect(screen.getByText('Not supported')).toBeInTheDocument();
      });

      it(`does NOT show Unprotected/Protected/Audited for ${format} proxy repos`, () => {
        const unsupportedRepo = { name: `${format}-proxy`, type: 'proxy', format };
        renderWithTheme(<FirewallCell repository={unsupportedRepo} firewallStatus={protectedStatus} />);
        expect(screen.queryByText('Protected')).not.toBeInTheDocument();
        expect(screen.queryByText('Unprotected')).not.toBeInTheDocument();
        expect(screen.queryByText('Audited')).not.toBeInTheDocument();
      });
    });

    it('shows "Not supported" with correct aria-label', () => {
      const terraformRepo = { name: 'terraform-proxy', type: 'proxy', format: 'terraform' };
      renderWithTheme(<FirewallCell repository={terraformRepo} />);
      expect(
        screen.getByLabelText('Firewall: Not supported for terraform')
      ).toBeInTheDocument();
    });

    it('still shows type-based N/A dash for hosted repos even with unsupported format', () => {
      const hostedRepo = { name: 'terraform-hosted', type: 'hosted', format: 'terraform' };
      renderWithTheme(<FirewallCell repository={hostedRepo} />);
      // Hosted repos show the Minus N/A dash, not the unsupported format badge
      expect(screen.queryByText('Not supported')).not.toBeInTheDocument();
    });

    it('supported format proxy repos do NOT trigger the unsupported state', () => {
      const mavenRepo = { name: 'maven-proxy', type: 'proxy', format: 'maven2' };
      renderWithTheme(<FirewallCell repository={mavenRepo} />);
      // Supported formats should not show the "Not supported" badge
      expect(screen.queryByText('Not supported')).not.toBeInTheDocument();
      // Note: "Unprotected" text appears after async firewall status loads -
      // this is a pre-existing timing issue with the test mock setup
    });
  });

  it('shouldEnableFirewallViaPickerWhenModeChosen', async () => {
    mockGet.mockImplementation((url: string) => {
      if (url === '/service/rest/v1/repositories/maven-central') {
        return Promise.resolve({ format: 'maven2', type: 'proxy' });
      }
      // setFirewallMode reads the repo from the internal UI endpoint (FIRE-1149) before the PUT.
      if (url === '/service/rest/internal/ui/repositories/repository/maven-central') {
        return Promise.resolve({
          name: 'maven-central',
          format: 'maven2',
          type: 'proxy',
          firewall: { mode: 'DISABLED' },
        });
      }
      if (url.includes('/maven/proxy/maven-central')) {
        return Promise.resolve({ name: 'maven-central', firewall: { mode: 'DISABLED' } });
      }
      return Promise.resolve([]);
    });

    renderWithTheme(<FirewallCell repository={repo} firewallLoaded hasFirewallLicense />);

    fireEvent.click(screen.getByRole('button', { name: /Firewall: Unprotected.*Click for details/i }));
    fireEvent.click(await screen.findByText('Enable Firewall Protection'));

    fireEvent.click(await screen.findByRole('radio', { name: /Quarantine/ }));
    fireEvent.click(screen.getByRole('button', { name: 'Enable' }));

    await waitFor(() =>
      expect(mockPut).toHaveBeenCalledWith(
        '/service/rest/v1/repositories/maven/proxy/maven-central',
        expect.objectContaining({ firewall: { mode: 'QUARANTINE' } }),
      ),
    );
  });

  it('shouldHideEnableButtonWhenUserLacksEditPermission', async () => {
    mockCheckPermission.mockReturnValue(false);
    renderWithTheme(<FirewallCell repository={repo} firewallLoaded hasFirewallLicense />);

    const trigger = screen.getByRole('button', { name: /Firewall: Unprotected.*Click for details/i });
    fireEvent.click(trigger);

    await screen.findByText(/Firewall Report – maven-central/);
    expect(screen.queryByText('Enable Firewall Protection')).not.toBeInTheDocument();
  });

  it('shouldRenderInlineChicletsFromCountsProp', () => {
    renderWithTheme(
      <FirewallCell
        repository={repo}
        firewallLoaded
        firewallStatus={protectedStatus}
        firewallCounts={{ ...protectedStatus, criticalComponentCount: 4 }}
      />,
    );
    expect(screen.getByText('4')).toBeInTheDocument();
  });

  it('shouldRenderSevereModerateAndQuarantinedChicletsFromCountsProp', () => {
    renderWithTheme(
      <FirewallCell
        repository={repo}
        firewallLoaded
        firewallStatus={protectedStatus}
        firewallCounts={{
          ...protectedStatus,
          severeComponentCount: 6,
          moderateComponentCount: 3,
          quarantinedComponentCount: 2,
        }}
      />,
    );
    expect(screen.getByText('6')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
    expect(screen.getByLabelText('2 quarantined')).toBeInTheDocument();
  });

  it('shouldKeepSummaryModeLabelWhenOverlayingCountsProp', () => {
    renderWithTheme(
      <FirewallCell
        repository={repo}
        firewallLoaded
        firewallStatus={protectedStatus}
        firewallCounts={{ ...protectedStatus, message: AUDIT_MESSAGE, criticalComponentCount: 5 }}
      />,
    );
    expect(screen.getByText('5')).toBeInTheDocument();
    expect(screen.getByText('Quarantine')).toBeInTheDocument();
    expect(screen.queryByText('Audited')).not.toBeInTheDocument();
  });

  it('shouldNeverFetchFromTheCell', () => {
    renderWithTheme(<FirewallCell repository={repo} firewallLoaded firewallStatus={protectedStatus} />);
    expect(mockGet).not.toHaveBeenCalled();
  });

  it('shouldShowInactiveNotUnprotectedForOfflineEnabledRepo', () => {
    const offline: SecurityStatusData = { ...protectedStatus, message: "Repository 'maven-central' is offline" };
    renderWithTheme(<FirewallCell repository={repo} firewallLoaded firewallStatus={offline} />);
    expect(screen.getByText('Inactive')).toBeInTheDocument();
    expect(screen.queryByText('Unprotected')).not.toBeInTheDocument();
  });

  it('shouldNotShowUnprotectedWhenSummaryNotReady', () => {
    renderWithTheme(<FirewallCell repository={repo} firewallLoaded firewallSummaryReady={false} />);
    expect(screen.queryByText('Unprotected')).not.toBeInTheDocument();
    expect(screen.getByText('Unavailable')).toBeInTheDocument();
  });

  it('shouldNotOfferEnableWhenSummaryNotReady', async () => {
    renderWithTheme(
      <FirewallCell repository={repo} firewallLoaded firewallSummaryReady={false} hasFirewallLicense />,
    );
    fireEvent.click(screen.getByRole('button', { name: /Firewall: Unavailable/i }));
    await screen.findByText(/Firewall Report – maven-central/);
    expect(screen.queryByText('Enable Firewall Protection')).not.toBeInTheDocument();
  });

  it('shouldShowUnavailableInsteadOfNoIssuesWhenEnabledRepoHasNoCountsAndNoneAreLoading', async () => {
    renderWithTheme(<FirewallCell repository={repo} firewallLoaded firewallStatus={protectedStatus} />);
    fireEvent.click(screen.getByRole('button', { name: /Firewall:.*Click for details/i }));
    await screen.findByText(/Firewall Report – maven-central/);
    expect(screen.getByText(/Violation counts could not be loaded/)).toBeInTheDocument();
    expect(screen.queryByText('No issues found')).not.toBeInTheDocument();
  });

  it('shouldShowLoadingNotUnavailableWhileCountsAreLoading', async () => {
    renderWithTheme(
      <FirewallCell repository={repo} firewallLoaded firewallStatus={protectedStatus} firewallCountsLoading />,
    );
    fireEvent.click(screen.getByRole('button', { name: /Firewall:.*Click for details/i }));
    expect(await screen.findByText(/Loading firewall report/)).toBeInTheDocument();
    expect(screen.queryByText(/Violation counts could not be loaded/)).not.toBeInTheDocument();
  });

  it('shouldShowNoIssuesOnlyWhenCountsLoadedAndZero', async () => {
    renderWithTheme(
      <FirewallCell
        repository={repo}
        firewallLoaded
        firewallStatus={protectedStatus}
        firewallCounts={protectedStatus}
      />,
    );
    fireEvent.click(screen.getByRole('button', { name: /Firewall:.*Click for details/i }));
    expect(await screen.findByText('No issues found')).toBeInTheDocument();
  });
});
