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
}));

import { SecuritySummaryModal } from '../SecuritySummaryModal';

const baseData = {
  repositoryName: 'maven-central',
  affectedComponentCount: 100,
  criticalComponentCount: 5,
  severeComponentCount: 2,
  moderateComponentCount: 3,
  quarantinedComponentCount: 0,
};

function renderWithTheme(ui: React.ReactElement) {
  return render(<Theme>{ui}</Theme>);
}

describe('SecuritySummaryModal', () => {
  const onClose = jest.fn();
  const onViewFullReport = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockGet.mockResolvedValue(null);
    mockPut.mockResolvedValue(undefined);
  });

  it('renders health-check modal with metrics', () => {
    renderWithTheme(
      <SecuritySummaryModal
        repositoryName="maven-central"
        data={baseData}
        isOpen={true}
        onClose={onClose}
        type="health-check"
        onViewFullReport={onViewFullReport}
      />
    );

    expect(screen.getByText(/Health Check – maven-central/)).toBeInTheDocument();
    const modalContent = document.querySelector('.security-summary-modal');
    expect(modalContent).toBeTruthy();
    expect(modalContent!.textContent).toContain('Critical');
    expect(modalContent!.textContent).toContain('Severe');
    expect(modalContent!.textContent).toContain('Moderate');
    expect(modalContent!.textContent).toContain('5');
    expect(modalContent!.textContent).toContain('2');
    expect(modalContent!.textContent).toContain('3');
    expect(screen.getByText('View Full Report')).toBeInTheDocument();
  });

  it('renders empty state when all metrics are zero', () => {
    const emptyData = { ...baseData, criticalComponentCount: 0, severeComponentCount: 0, moderateComponentCount: 0 };
    renderWithTheme(
      <SecuritySummaryModal
        repositoryName="maven-central"
        data={emptyData}
        isOpen={true}
        onClose={onClose}
        type="health-check"
        onViewFullReport={onViewFullReport}
      />
    );

    expect(screen.getByText('No issues found')).toBeInTheDocument();
    expect(screen.getByText('View Full Report')).toBeInTheDocument();
  });

  it('calls onViewFullReport when View Full Report is clicked (no reportUrl)', () => {
    renderWithTheme(
      <SecuritySummaryModal
        repositoryName="maven-central"
        data={baseData}
        isOpen={true}
        onClose={onClose}
        type="health-check"
        onViewFullReport={onViewFullReport}
      />
    );

    fireEvent.click(screen.getByText('View Full Report'));
    expect(onViewFullReport).toHaveBeenCalledTimes(1);
  });

  it('shows View Full Report link opening reportUrl in new tab when reportUrl is provided', () => {
    renderWithTheme(
      <SecuritySummaryModal
        repositoryName="maven-central"
        data={{ ...baseData, reportUrl: 'https://iq.example.com/report' }}
        isOpen={true}
        onClose={onClose}
        type="health-check"
        onViewFullReport={onViewFullReport}
        reportUrl="https://iq.example.com/report"
      />
    );

    const viewReportLink = screen.getByRole('link', { name: /View Full Report/i });
    expect(viewReportLink).toHaveAttribute('href', 'https://iq.example.com/report');
    expect(viewReportLink).toHaveAttribute('target', '_blank');
  });

  it('shows loading state instead of No issues found while firewall report detail is loading', () => {
    const emptyData = {
      ...baseData,
      criticalComponentCount: 0,
      severeComponentCount: 0,
      moderateComponentCount: 0,
      affectedComponentCount: 0,
      quarantinedComponentCount: 0,
    };
    renderWithTheme(
      <SecuritySummaryModal
        repositoryName="maven-central"
        data={emptyData}
        isOpen={true}
        onClose={onClose}
        type="firewall"
        onViewFullReport={onViewFullReport}
        hasFirewallLicense={true}
        firewallStatus="protected"
        isFirewallReportDetailLoading={true}
      />
    );

    expect(screen.queryByText('No issues found')).not.toBeInTheDocument();
    expect(screen.getByText('Loading firewall report...')).toBeInTheDocument();
  });

  it('renders firewall modal with quarantine count when quarantinedComponentCount > 0', () => {
    renderWithTheme(
      <SecuritySummaryModal
        repositoryName="maven-central"
        data={{ ...baseData, quarantinedComponentCount: 47 }}
        isOpen={true}
        onClose={onClose}
        type="firewall"
        onViewFullReport={onViewFullReport}
        hasFirewallLicense={true}
        firewallStatus="quarantined"
      />
    );

    expect(screen.getByText(/Firewall Report – maven-central/)).toBeInTheDocument();
    const modalContent = document.querySelector('.security-summary-modal');
    expect(modalContent!.textContent).toContain('47');
    expect(modalContent!.textContent).toContain('QUARANTINED');
  });

  it('shouldShowEnableButtonWhenUnprotectedLicensedAndCanEdit', () => {
    renderWithTheme(
      <SecuritySummaryModal
        repositoryName="maven-central"
        data={baseData}
        isOpen={true}
        onClose={onClose}
        type="firewall"
        onViewFullReport={onViewFullReport}
        firewallStatus="unprotected"
        hasFirewallLicense={true}
        canEditFirewall={true}
      />
    );

    expect(screen.getByText('Unprotected')).toBeInTheDocument();
    expect(screen.getByText('Enable Firewall Protection')).toBeInTheDocument();
  });

  it('shouldHideEnableButtonWhenUserCannotEdit', () => {
    renderWithTheme(
      <SecuritySummaryModal
        repositoryName="maven-central"
        data={baseData}
        isOpen={true}
        onClose={onClose}
        type="firewall"
        firewallStatus="unprotected"
        hasFirewallLicense={true}
        canEditFirewall={false}
      />
    );

    expect(screen.getByText('Unprotected')).toBeInTheDocument();
    expect(screen.queryByText('Enable Firewall Protection')).not.toBeInTheDocument();
    expect(screen.getByText(/Learn More/)).toBeInTheDocument();
  });

  const CURRENT_REPO_URL = '/service/rest/v1/repositories/maven-central';
  const TYPED_REPO_URL = '/service/rest/v1/repositories/maven/proxy/maven-central';
  // setFirewallMode reads the repo from the internal UI endpoint (FIRE-1149) before the PUT.
  const INTERNAL_REPO_URL = '/service/rest/internal/ui/repositories/repository/maven-central';

  function mockRepoMode(mode: string) {
    mockGet.mockImplementation((url: string) => {
      if (url === CURRENT_REPO_URL) return Promise.resolve({ format: 'maven2', type: 'proxy' });
      if (url === TYPED_REPO_URL) return Promise.resolve({ name: 'maven-central', firewall: { mode } });
      if (url === INTERNAL_REPO_URL) {
        return Promise.resolve({ name: 'maven-central', format: 'maven2', type: 'proxy', firewall: { mode } });
      }
      return Promise.resolve([]);
    });
  }

  function renderUnprotected(extra: Record<string, unknown> = {}) {
    return renderWithTheme(
      <SecuritySummaryModal
        repositoryName="maven-central"
        repositoryFormat="maven2"
        data={baseData}
        isOpen={true}
        onClose={onClose}
        type="firewall"
        firewallStatus="unprotected"
        hasFirewallLicense={true}
        canEditFirewall={true}
        {...extra}
      />
    );
  }

  it('shouldOpenModePickerWithoutWritingWhenEnableClicked', async () => {
    mockRepoMode('DISABLED');
    renderUnprotected();

    fireEvent.click(screen.getByText('Enable Firewall Protection'));

    expect(await screen.findByText(/Choose a Firewall mode/)).toBeInTheDocument();
    expect(mockPut).not.toHaveBeenCalled();
  });

  it('shouldKeepConfirmDisabledUntilAModeIsChosen', async () => {
    mockRepoMode('DISABLED');
    renderUnprotected();

    fireEvent.click(screen.getByText('Enable Firewall Protection'));
    await screen.findByText(/Choose a Firewall mode/);

    expect(screen.getByRole('button', { name: 'Enable' })).toBeDisabled();
  });

  it('shouldWriteChosenModeAndFireCallbacksWhenConfirmed', async () => {
    mockRepoMode('DISABLED');
    const onEnableSuccess = jest.fn();
    renderUnprotected({ onEnableSuccess });

    fireEvent.click(screen.getByText('Enable Firewall Protection'));
    fireEvent.click(await screen.findByRole('radio', { name: /Audit/ }));
    fireEvent.click(screen.getByRole('button', { name: 'Enable' }));

    await waitFor(() =>
      expect(mockPut).toHaveBeenCalledWith(TYPED_REPO_URL, expect.objectContaining({ firewall: { mode: 'AUDIT' } })),
    );
    await waitFor(() => expect(onEnableSuccess).toHaveBeenCalledTimes(1));
    expect(onClose).toHaveBeenCalled();
  });

  it('shouldShowCurrentModeAndDisableItWhenRepoIsAlreadyEnabled', async () => {
    mockRepoMode('AUDIT');
    renderUnprotected();

    fireEvent.click(screen.getByText('Enable Firewall Protection'));

    expect(await screen.findByText('Current mode: AUDIT')).toBeInTheDocument();
    expect(screen.getByRole('radio', { name: /Audit/ })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Switch mode' })).toBeDisabled();
  });

  it('shouldNotOfferPccsForFormatsWithoutPccsSupport', async () => {
    mockRepoMode('DISABLED');
    renderUnprotected();

    fireEvent.click(screen.getByText('Enable Firewall Protection'));
    await screen.findByText(/Choose a Firewall mode/);

    expect(screen.queryByRole('radio', { name: /PCCS/ })).not.toBeInTheDocument();
  });

  it('shouldNotWriteAndOfferRetryWhenCurrentModeCannotBeLoaded', async () => {
    mockGet.mockResolvedValue(null);
    renderUnprotected();

    fireEvent.click(screen.getByText('Enable Firewall Protection'));

    expect(await screen.findByText(/Could not load the current Firewall mode/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Retry' })).toBeInTheDocument();
    expect(mockPut).not.toHaveBeenCalled();
  });

  it('shouldKeepModalOpenAndSkipCallbackWhenWriteFails', async () => {
    mockRepoMode('DISABLED');
    mockPut.mockRejectedValue(new Error('IQ unreachable'));
    const onEnableSuccess = jest.fn();
    renderUnprotected({ onEnableSuccess });

    fireEvent.click(screen.getByText('Enable Firewall Protection'));
    fireEvent.click(await screen.findByRole('radio', { name: /Quarantine/ }));
    fireEvent.click(screen.getByRole('button', { name: 'Enable' }));

    await waitFor(() => expect(mockPut).toHaveBeenCalled());
    expect(onEnableSuccess).not.toHaveBeenCalled();
    expect(onClose).not.toHaveBeenCalled();
  });

  it('shouldShowInactiveNoticeWithoutEnableButtonWhenFirewallInactive', () => {
    renderUnprotected({ firewallStatus: 'inactive' });

    expect(screen.getByText(/enabled for this repository but is not currently active/)).toBeInTheDocument();
    expect(screen.queryByText('Enable Firewall Protection')).not.toBeInTheDocument();
  });

  it('shouldShowCountsUnavailableInsteadOfNoIssuesWhenCountsFailed', () => {
    renderWithTheme(
      <SecuritySummaryModal
        repositoryName="maven-central"
        data={{ ...baseData, criticalComponentCount: 0, severeComponentCount: 0, moderateComponentCount: 0 }}
        isOpen={true}
        onClose={onClose}
        type="firewall"
        firewallStatus="protected"
        hasFirewallLicense={true}
        countsUnavailable={true}
      />
    );

    expect(screen.getByText(/Violation counts could not be loaded/)).toBeInTheDocument();
    expect(screen.queryByText('No issues found')).not.toBeInTheDocument();
  });

  it('shouldNotRenderShieldIconInFirewallDialogHeader', () => {
    renderWithTheme(
      <SecuritySummaryModal
        repositoryName="maven-central"
        data={{ ...baseData, quarantinedComponentCount: 5 }}
        isOpen={true}
        onClose={onClose}
        type="firewall"
        firewallStatus="quarantined"
        hasFirewallLicense={true}
      />
    );

    const header = document.querySelector('.security-summary-modal__header');
    expect(header).toBeTruthy();
    expect(header!.querySelector('svg.lucide-shield')).toBeNull();
    expect(header!.querySelector('svg.lucide-shield-check')).toBeNull();
  });

  it('shouldRenderShieldCheckIconInHealthCheckDialogHeader', () => {
    renderWithTheme(
      <SecuritySummaryModal
        repositoryName="maven-central"
        data={baseData}
        isOpen={true}
        onClose={onClose}
        type="health-check"
        onViewFullReport={onViewFullReport}
      />
    );

    const header = document.querySelector('.security-summary-modal__header');
    expect(header!.querySelector('svg.lucide-shield-check')).toBeTruthy();
  });

  it('shouldNotShowBrowseRepoLinkInFirewallDialog', () => {
    const onBrowseRepo = jest.fn();
    renderWithTheme(
      <SecuritySummaryModal
        repositoryName="maven-central"
        data={{ ...baseData, quarantinedComponentCount: 5, reportUrl: 'https://iq.example.com/report' }}
        isOpen={true}
        onClose={onClose}
        type="firewall"
        firewallStatus="quarantined"
        hasFirewallLicense={true}
        reportUrl="https://iq.example.com/report"
        onBrowseRepo={onBrowseRepo}
      />
    );

    expect(screen.queryByText('Browse Repo')).not.toBeInTheDocument();
  });

  it('shouldStillShowBrowseRepoLinkInHealthCheckDialog', () => {
    const onBrowseRepo = jest.fn();
    renderWithTheme(
      <SecuritySummaryModal
        repositoryName="maven-central"
        data={baseData}
        isOpen={true}
        onClose={onClose}
        type="health-check"
        onViewFullReport={onViewFullReport}
        onBrowseRepo={onBrowseRepo}
      />
    );

    expect(screen.getByText('Browse Repo')).toBeInTheDocument();
  });

  it('shows Learn More when hasFirewallLicense is false (firewall)', () => {
    renderWithTheme(
      <SecuritySummaryModal
        repositoryName="maven-central"
        data={baseData}
        isOpen={true}
        onClose={onClose}
        type="firewall"
        onViewFullReport={onViewFullReport}
        hasFirewallLicense={false}
      />
    );

    const modalContent = document.querySelector('.security-summary-modal');
    expect(modalContent!.textContent).toContain('Learn More');
  });

  it('does not render when isOpen is false', () => {
    const { container } = renderWithTheme(
      <SecuritySummaryModal
        repositoryName="maven-central"
        data={baseData}
        isOpen={false}
        onClose={onClose}
        type="health-check"
        onViewFullReport={onViewFullReport}
      />
    );

    expect(screen.queryByText(/Health Check – maven-central/)).not.toBeInTheDocument();
  });

  it('shouldRenderHealthCheckReportMetadataAndThreatBars', () => {
    renderWithTheme(
      <SecuritySummaryModal
        repositoryName="maven-central"
        data={{
          ...baseData,
          reportDate: '2026-10-01',
          reportAge: '7 days',
          componentsIdentified: 80,
          componentsTotal: 100,
          threatLevelCounts: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10],
        }}
        isOpen={true}
        onClose={onClose}
        type="health-check"
        onViewFullReport={onViewFullReport}
      />
    );

    const modalContent = document.querySelector('.security-summary-modal');
    expect(modalContent!.textContent).toContain('COMPONENTS IDENTIFIED');
    expect(modalContent!.textContent).toContain('2026-10-01');
    expect(modalContent!.textContent).toContain('7 days');
  });

  it('shouldRenderHealthCheckLicenseWarnings', () => {
    renderWithTheme(
      <SecuritySummaryModal
        repositoryName="maven-central"
        data={{
          ...baseData,
          licenseCopyleftCount: 3,
          licenseNonStandardCount: 2,
          licenseNotProvidedCount: 1,
          licenseWeakCopyleftCount: 4,
          licenseLiberalCount: 5,
        }}
        isOpen={true}
        onClose={onClose}
        type="health-check"
        onViewFullReport={onViewFullReport}
      />
    );

    const modalContent = document.querySelector('.security-summary-modal');
    expect(modalContent!.textContent).toContain('License Warnings');
    expect(modalContent!.textContent).toContain('Copyleft');
  });

  describe('terminology: vulnerabilities vs violations (fb5g)', () => {
    it('uses "Security Vulnerabilities" label for health-check type', () => {
      renderWithTheme(
        <SecuritySummaryModal
          repositoryName="maven-central"
          data={baseData}
          isOpen={true}
          onClose={onClose}
          type="health-check"
          onViewFullReport={onViewFullReport}
        />
      );

      const modalContent = document.querySelector('.security-summary-modal');
      expect(modalContent!.textContent).toContain('Security Vulnerabilities');
      expect(modalContent!.textContent).not.toContain('Policy Violations');
    });

    it('uses "Policy Violations" label for firewall type', () => {
      renderWithTheme(
        <SecuritySummaryModal
          repositoryName="maven-central"
          data={{ ...baseData, quarantinedComponentCount: 5 }}
          isOpen={true}
          onClose={onClose}
          type="firewall"
          onViewFullReport={onViewFullReport}
          firewallStatus="quarantined"
          hasFirewallLicense={true}
        />
      );

      const modalContent = document.querySelector('.security-summary-modal');
      expect(modalContent!.textContent).toContain('POLICY VIOLATIONS');
      expect(modalContent!.textContent).not.toContain('Security Vulnerabilities');
    });
  });
});
