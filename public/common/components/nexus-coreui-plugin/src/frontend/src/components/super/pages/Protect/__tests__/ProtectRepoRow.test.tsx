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
import { Theme, Table } from '@radix-ui/themes';

import type { RepoWithProtection } from '../../MalwareRisk/useQuickActionsData';

jest.mock('@/utils/api', () => ({
  restClient: { post: jest.fn(), delete: jest.fn() },
  ENDPOINTS: {
    HEALTH_CHECK_ANALYZE: (name: string) => `/v1/repositories/${name}/health-check`,
    REPOSITORY_HEALTH_CHECK: (name: string) => `/v1/repositories/${name}/health-check`,
  },
}));

jest.mock('../../../../shared/security/useFirewallEnable', () => ({
  disableFirewall: jest.fn(),
  enableFirewallAudit: jest.fn(),
  enableFirewallQuarantine: jest.fn(),
}));

jest.mock('../../../../shared', () => ({
  useToast: () => ({ success: jest.fn(), error: jest.fn() }),
}));

jest.mock('../../../../shared/security/malwareRemediatorTask', () => ({
  setMalwareRemediatorEnabledForRepository: jest.fn(),
}));

jest.mock('../ProtectChangeHistoryModal', () => ({
  __esModule: true,
  default: () => null,
}));

jest.mock('../../../settings/repository/repositories/components/FormatIcon', () => ({
  FormatIcon: ({ format }: { format: string }) => <span data-testid="format-icon">{format}</span>,
}));

import ProtectRepoRow from '../ProtectRepoRow';

const MAVEN_REPO: RepoWithProtection = {
  name: 'maven-central',
  format: 'maven2',
  type: 'proxy',
  url: '',
  rhcEnabled: true,
  rhcSupported: true,
  rhcAnalyzing: false,
  rhcSecurityIssues: null,
  rhcLicenseIssues: null,
  protection: 'quarantine',
  taskEnabled: true,
  taskCleanupEnabled: false,
  malwareCount: 0,
  malware: [],
};

const renderRow = (props: Partial<React.ComponentProps<typeof ProtectRepoRow>> = {}) =>
  render(
    <Theme>
      <Table.Root>
        <Table.Body>
          <ProtectRepoRow
            repo={MAVEN_REPO}
            hasFirewallLicense={true}
            hasIqConnection={true}
            canUpdateHealthCheck={true}
            hcInstanceEnabled={true}
            lastAnalyzedMs={null}
            onRefetch={jest.fn()}
            {...props}
          />
        </Table.Body>
      </Table.Root>
    </Theme>
  );

describe('ProtectRepoRow', () => {
  it('renders the repository name', () => {
    renderRow();
    expect(screen.getByText('maven-central')).toBeInTheDocument();
  });

  it('renders the repository format', () => {
    renderRow();
    expect(screen.getAllByText('maven2').length).toBeGreaterThanOrEqual(1);
  });

  it('renders test id with repo name', () => {
    renderRow();
    expect(screen.getByTestId('protect-repo-row-maven-central')).toBeInTheDocument();
  });

  it('renders health check switch when user has permission and HC is enabled', () => {
    renderRow();
    const switches = screen.getAllByRole('switch');
    expect(switches.length).toBeGreaterThanOrEqual(1);
  });

  it('shows dash when canUpdateHealthCheck is false', () => {
    renderRow({ canUpdateHealthCheck: false });
    const dashes = screen.getAllByText('—');
    expect(dashes.length).toBeGreaterThanOrEqual(1);
  });

  it('shows dash when hcInstanceEnabled is false', () => {
    renderRow({ hcInstanceEnabled: false });
    const dashes = screen.getAllByText('—');
    expect(dashes.length).toBeGreaterThanOrEqual(1);
  });

  it('renders firewall and auto remediation radio buttons when IQ connected and licensed', () => {
    renderRow();
    const radios = screen.getAllByRole('radio');
    expect(radios).toHaveLength(6);
  });

  it('shows Not supported for unsupported formats', () => {
    const terraformRepo: RepoWithProtection = {
      ...MAVEN_REPO,
      name: 'terraform-proxy',
      format: 'terraform',
    };
    renderRow({ repo: terraformRepo });
    expect(screen.getAllByText('Not supported').length).toBeGreaterThanOrEqual(1);
  });

  it('shows dash when IQ is not connected', () => {
    renderRow({ hasIqConnection: false });
    const dashes = screen.getAllByText('—');
    expect(dashes.length).toBeGreaterThanOrEqual(1);
  });
});
