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
import { Theme } from '@radix-ui/themes';
import { FirewallReportTab } from '../FirewallReportTab';

jest.mock('@/components/shared/security/SecurityReportPage', () => ({
  SecurityReportPage: ({ repositoryName }: { repositoryName: string }) => (
    <div data-testid="security-report-page">{repositoryName}</div>
  ),
}));

jest.mock('@/components/shared/security/useFirewallEnable', () => ({
  useFirewallEnable: () => ({
    enableAudit: jest.fn(),
    enableQuarantine: jest.fn(),
    loading: false,
  }),
}));

function renderWithTheme(ui: React.ReactElement) {
  return render(<Theme>{ui}</Theme>);
}

describe('FirewallReportTab', () => {
  describe('supported formats', () => {
    it('renders the SecurityReportPage for supported format', () => {
      renderWithTheme(
        <FirewallReportTab repositoryName="maven-central" repositoryFormat="maven2" />
      );
      expect(screen.getByTestId('security-report-page')).toBeInTheDocument();
      expect(screen.queryByTestId('firewall-report-tab-unsupported')).not.toBeInTheDocument();
    });

    it('renders the SecurityReportPage when no format is provided', () => {
      renderWithTheme(<FirewallReportTab repositoryName="unknown-repo" />);
      expect(screen.getByTestId('security-report-page')).toBeInTheDocument();
    });

    it('shows Enable buttons when firewallEnabled=false and hasFirewallLicense=true', () => {
      renderWithTheme(
        <FirewallReportTab
          repositoryName="maven-central"
          repositoryFormat="maven2"
          firewallEnabled={false}
          hasFirewallLicense={true}
        />
      );
      expect(screen.getByRole('button', { name: /enable audit/i })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /enable quarantine/i })).toBeInTheDocument();
    });

    it('does not show Enable buttons when firewallEnabled=true', () => {
      renderWithTheme(
        <FirewallReportTab
          repositoryName="maven-central"
          repositoryFormat="maven2"
          firewallEnabled={true}
          hasFirewallLicense={true}
        />
      );
      expect(screen.queryByRole('button', { name: /enable audit/i })).not.toBeInTheDocument();
    });
  });

  describe('unsupported formats', () => {
    const unsupportedFormats = ['terraform', 'apt', 'helm', 'gitlfs', 'swift'];

    unsupportedFormats.forEach((format) => {
      it(`renders the empty state instead of SecurityReportPage for ${format}`, () => {
        renderWithTheme(
          <FirewallReportTab repositoryName={`${format}-repo`} repositoryFormat={format} />
        );
        expect(screen.getByTestId('firewall-report-tab-unsupported')).toBeInTheDocument();
        expect(screen.getByTestId('firewall-not-supported-empty-state')).toBeInTheDocument();
        expect(screen.queryByTestId('security-report-page')).not.toBeInTheDocument();
      });
    });

    it('shows "Firewall Not Available" heading for unsupported format', () => {
      renderWithTheme(
        <FirewallReportTab repositoryName="terraform-repo" repositoryFormat="terraform" />
      );
      expect(screen.getByText('Firewall Not Available')).toBeInTheDocument();
    });

    it('does NOT show Enable Audit/Enable Quarantine buttons for unsupported format', () => {
      renderWithTheme(
        <FirewallReportTab
          repositoryName="terraform-repo"
          repositoryFormat="terraform"
          firewallEnabled={false}
          hasFirewallLicense={true}
        />
      );
      expect(screen.queryByRole('button', { name: /enable audit/i })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /enable quarantine/i })).not.toBeInTheDocument();
    });

    it('shows Learn about supported formats link', () => {
      renderWithTheme(
        <FirewallReportTab repositoryName="helm-repo" repositoryFormat="helm" />
      );
      const link = screen.getByRole('link', { name: /learn about supported formats/i });
      expect(link).toBeInTheDocument();
    });
  });
});
