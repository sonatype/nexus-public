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
import { Theme, Table } from '@radix-ui/themes';

import type { RepoWithProtection } from '../../MalwareRisk/useQuickActionsData';

jest.mock('../../../../../interface/api', () => {
  const actual = jest.requireActual('../../../../../interface/api');
  return {
    ...actual,
    restClient: { post: jest.fn(), delete: jest.fn() },
    ENDPOINTS: {
      HEALTH_CHECK_ANALYZE: (name: string) => `/v1/repositories/${name}/health-check`,
      REPOSITORY_HEALTH_CHECK: (name: string) => `/v1/repositories/${name}/health-check`,
    },
  };
});

jest.mock('../../../shared/security/useFirewallEnable', () => ({
  disableFirewall: jest.fn(),
  enableFirewallAudit: jest.fn(),
  enableFirewallQuarantine: jest.fn(),
}));

const mockToastError = jest.fn();
jest.mock('../../../shared', () => ({
  useToast: () => ({ success: jest.fn(), error: mockToastError }),
}));

jest.mock('../../../shared/security/malwareRemediatorTask', () => ({
  setMalwareRemediatorEnabledForRepository: jest.fn(),
}));

jest.mock('../ProtectChangeHistoryModal', () => ({
  __esModule: true,
  default: () => null,
}));

jest.mock('../../settings/repository/repositories/components/FormatIcon', () => ({
  FormatIcon: ({ format }: { format: string }) => <span data-testid="format-icon">{format}</span>,
}));

import { ExtJS } from '../../../../../interface/ExtJS';
import Permissions from '../../../../../constants/Permissions';
// ProtectRepoRow reads permissions through the provider-independent ExtJS.usePermission
// (NEXUS-54212); spy on checkPermission so tests keep driving behavior via permission strings.
const mockCheckPermission = jest.spyOn(ExtJS, 'checkPermission');

import ProtectRepoRow from '../ProtectRepoRow';
import { restClient } from '../../../../../interface/api';

const mockDelete = restClient.delete as jest.Mock;
const mockPost = restClient.post as jest.Mock;

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
  beforeEach(() => {
    jest.clearAllMocks();
    // Default: user has all write permissions so pre-existing behavior is exercised.
    mockCheckPermission.mockReturnValue(true);
  });

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

  it('toggling the health check switch off calls DELETE on REPOSITORY_HEALTH_CHECK', async () => {
    mockDelete.mockResolvedValue(undefined);
    renderRow({ repo: { ...MAVEN_REPO, rhcEnabled: true } });

    fireEvent.click(screen.getByRole('switch'));

    await waitFor(() => expect(mockDelete).toHaveBeenCalledWith('/v1/repositories/maven-central/health-check'));
  });

  it('toggling the health check switch on calls POST on HEALTH_CHECK_ANALYZE', async () => {
    mockPost.mockResolvedValue(undefined);
    renderRow({ repo: { ...MAVEN_REPO, rhcEnabled: false } });

    fireEvent.click(screen.getByRole('switch'));

    await waitFor(() =>
      expect(mockPost).toHaveBeenCalledWith('/v1/repositories/maven-central/health-check', {})
    );
  });

  it('toasts the unwrapped backend message when enabling health check 409s (capability disabled)', async () => {
    mockPost.mockRejectedValue({
      isAxiosError: true,
      response: {
        status: 409,
        data: { id: '*', message: '"Repository Health Check instance capability is not enabled"' },
      },
    });
    renderRow({ repo: { ...MAVEN_REPO, rhcEnabled: false } });

    fireEvent.click(screen.getByRole('switch'));

    await waitFor(() =>
      expect(mockToastError).toHaveBeenCalledWith('Repository Health Check instance capability is not enabled')
    );
  });

  describe('write gating (NEXUS-54212)', () => {
    const fwRadios = () =>
      screen.queryAllByRole('radio').filter((r) => r.getAttribute('name') === 'fw-maven-central');
    const mpRadios = () =>
      screen.queryAllByRole('radio').filter((r) => r.getAttribute('name') === 'mp-maven-central');

    it('shows protection + remediation radios with edit + tasks:create', () => {
      mockCheckPermission.mockImplementation(
        (p: string) => p === Permissions.REPOSITORY_ADMIN.EDIT || p === Permissions.TASKS.CREATE,
      );
      renderRow();
      expect(fwRadios()).toHaveLength(3);
      expect(mpRadios()).toHaveLength(3);
    });

    it('hides protection radios without repository-admin edit', () => {
      mockCheckPermission.mockImplementation((p: string) => p === Permissions.TASKS.CREATE);
      renderRow();
      expect(fwRadios()).toHaveLength(0);
      // Remediation radios remain because tasks:create is granted.
      expect(mpRadios()).toHaveLength(3);
    });

    it('hides remediation radios without tasks:create', () => {
      mockCheckPermission.mockImplementation((p: string) => p === Permissions.REPOSITORY_ADMIN.EDIT);
      renderRow();
      expect(mpRadios()).toHaveLength(0);
      // Protection radios remain because repository-admin edit is granted.
      expect(fwRadios()).toHaveLength(3);
    });

    it('hides both radio groups for a read-only user', () => {
      mockCheckPermission.mockReturnValue(false);
      renderRow();
      expect(fwRadios()).toHaveLength(0);
      expect(mpRadios()).toHaveLength(0);
    });
  });
});
