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

  it('shows enable options when firewall unprotected and hasFirewallLicense', () => {
    const mockConfigureFirewall = jest.fn();
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
        onConfigureFirewall={mockConfigureFirewall}
      />
    );

    expect(screen.getByText('Unprotected')).toBeInTheDocument();
    expect(screen.getByText('Enable Firewall Protection')).toBeInTheDocument();
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
