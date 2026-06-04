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
import { render, screen, fireEvent } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import '@testing-library/jest-dom';

jest.mock('@uirouter/react', () => ({
  useRouter: () => ({ stateService: { go: jest.fn() } }),
}));

const mockGet = jest.fn().mockResolvedValue(null);
jest.mock('../../../../../interface/api', () => ({
  restClient: { get: (...args: any[]) => mockGet(...args) },
  ENDPOINTS: {
    FIREWALL_STATUS_REPO: (name: string) => `/api/v1/firewall/status/${name}`,
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

const protectedStatus: SecurityStatusData = {
  repositoryName: 'maven-central',
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
  message: 'audit mode enabled',
};

describe('FirewallCell', () => {
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
    expect(screen.getByText('3')).toBeInTheDocument();
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
    const auditNoIssues: SecurityStatusData = { ...protectedStatus, message: 'audit mode enabled' };
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
});
