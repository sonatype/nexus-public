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
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';

import { MaliciousPackagesDataSnapshot } from '../useMaliciousPackagesData';
import { MaliciousFinding } from '../types';
import type { MaliciousPackagesDataSnapshot } from '../useMaliciousPackagesData';
import type { MaliciousFinding } from '../types';

const mockUseMaliciousPackagesData = jest.fn<MaliciousPackagesDataSnapshot, []>();

jest.mock('../useMaliciousPackagesData', () => ({
  useMaliciousPackagesData: () => mockUseMaliciousPackagesData(),
}));

jest.mock('../../Protect/useProtectData', () => ({
  useProtectData: () => ({
    repos: [],
    loading: false,
    error: null,
    refetch: jest.fn(),
    filterCounts: {
      formats: new Map(),
      protection: new Map(),
      healthCheck: { enabled: 0, disabled: 0, unsupported: 0 },
      cleanup: { delete: 0, audit: 0, off: 0 },
    },
    iqCapabilities: null,
    hasFirewall: false,
    hasIqConnection: false,
    canUpdateHealthCheck: false,
    iqAudit: { counts: null, loading: false, error: null },
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
    hcInstanceEnabled: true,
    lastAnalyzedByRepo: new Map(),
  }),
}));

import MaliciousPackagesPage from '../MaliciousPackagesPage';

function makeFinding(overrides: Partial<MaliciousFinding> = {}): MaliciousFinding {
  return {
    id: 1,
    repositoryName: 'npm-proxy',
    assetId: 'asset-1',
    path: '/some/path',
    format: 'npm',
    recordedTime: '2026-03-15T12:00:00Z',
    deletedTime: null,
    deletedBy: null,
    deletionMethod: null,
    acknowledgedAt: null,
    acknowledgedBy: null,
    acknowledgedReason: null,
    firstDetectedAt: '2026-03-15T10:00:00Z',
    hash: 'abc123def456',
    createdBy: 'anonymous',
    createdByIp: '10.0.0.1',
    componentName: 'evil-package',
    componentVersion: '1.0.0',
    componentFormat: 'npm',
    threatLevel: 10,
    threatSummary: 'Contains crypto miner',
    threatReference: 'https://example.com/cve-123',
    policyName: 'Malware-Block',
    ...overrides,
  };
}

function buildMockSnapshot(overrides: Partial<MaliciousPackagesDataSnapshot> = {}): MaliciousPackagesDataSnapshot {
  return {
    activeFindings: [],
    historyFindings: [],
    malwareCount: 0,
    countsByRepo: {},
    hasFirewall: false,
    hcEnabledRepos: [],
    totalProxyRepoCount: 0,
    loading: false,
    error: null,
    acknowledge: jest.fn().mockResolvedValue(undefined),
    deleteFinding: jest.fn().mockResolvedValue(undefined),
    bulkDelete: jest.fn().mockResolvedValue(undefined),
    bulkAcknowledge: jest.fn().mockResolvedValue(undefined),
    refetch: jest.fn(),
    fetchHistory: jest.fn().mockResolvedValue(undefined),
    tasks: [],
    tasksLoading: false,
    runTask: jest.fn().mockResolvedValue(undefined),
    enableTasksForRepos: jest.fn().mockResolvedValue(undefined),
    reEnableTask: jest.fn().mockResolvedValue(undefined),
    refetchTasks: jest.fn().mockResolvedValue(undefined),
    proxyRepos: [],
    enableRhc: jest.fn().mockResolvedValue(undefined),
    rhcScans: new Map(),
    bulkProgress: { total: 0, completed: 0, active: false },
    remediateFindings: jest.fn().mockResolvedValue({
      totalRequested: 0, totalDeleted: 0, totalFailed: 0, results: [],
    }),
    remediateRepository: jest.fn().mockResolvedValue(undefined),
    fetchFindings: jest.fn().mockResolvedValue({ items: [], totalCount: 0 }),
    createAndRunAuditTask: jest.fn().mockResolvedValue([]),
    ...overrides,
  };
}

function renderPage() {
  return render(
    <Theme>
      <MaliciousPackagesPage />
    </Theme>
  );
}

describe('MaliciousPackagesPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('loading state', () => {
    it('renders loading skeleton initially', () => {
      mockUseMaliciousPackagesData.mockReturnValue(buildMockSnapshot({ loading: true }));
      renderPage();
      expect(screen.getByTestId('malicious-packages-loading')).toBeInTheDocument();
    });
  });

  describe('error state', () => {
    it('renders error callout when error is present', () => {
      mockUseMaliciousPackagesData.mockReturnValue(
        buildMockSnapshot({ error: 'Network error' })
      );
      renderPage();
      expect(screen.getByTestId('malicious-packages-error')).toBeInTheDocument();
      expect(screen.getByText('Network error')).toBeInTheDocument();
    });
  });

  describe('tabbed dashboard', () => {
    it('renders tab bar and overview by default when healthy', () => {
      mockUseMaliciousPackagesData.mockReturnValue(
        buildMockSnapshot({ hasFirewall: false, malwareCount: 0 })
      );
      renderPage();
      expect(screen.getByRole('tab', { name: /Overview/i })).toBeInTheDocument();
      expect(screen.getByRole('tab', { name: /Detect/i })).toBeInTheDocument();
      expect(screen.getByRole('tab', { name: /Remediate/i })).toBeInTheDocument();
      expect(screen.getByRole('tab', { name: /Harden/i })).toBeInTheDocument();
      expect(screen.getByTestId('overview-no-firewall')).toBeInTheDocument();
    });

    it('shows detect table on Detect when signatures exist', async () => {
      mockUseMaliciousPackagesData.mockReturnValue(
        buildMockSnapshot({
          hasFirewall: true,
          malwareCount: 5,
          activeFindings: [
            makeFinding({ id: 1, repositoryName: 'npm-proxy' }),
            makeFinding({ id: 2, repositoryName: 'maven-central' }),
          ],
          countsByRepo: { 'npm-proxy': 3, 'maven-central': 2 },
          hcEnabledRepos: ['npm-proxy'],
          totalProxyRepoCount: 3,
          proxyRepos: [
            { name: 'npm-proxy', format: 'npm', rhcSupported: true, rhcEnabled: true },
            { name: 'maven-central', format: 'maven2', rhcSupported: true, rhcEnabled: true },
          ],
        })
      );
      renderPage();
      await userEvent.click(screen.getByRole('tab', { name: /Detect/i }));
      expect(screen.getByTestId('detect-table')).toBeInTheDocument();
    });
  });

  describe('remediation', () => {
    it('renders Delete All on Remediate tab for repo with findings', async () => {
      const finding = makeFinding({ id: 42 });
      const mockRemediateRepository = jest.fn().mockResolvedValue(undefined);
      mockUseMaliciousPackagesData.mockReturnValue(
        buildMockSnapshot({
          hasFirewall: true,
          malwareCount: 1,
          activeFindings: [finding],
          countsByRepo: { 'npm-proxy': 1 },
          remediateRepository: mockRemediateRepository,
          remediateFindings: jest.fn().mockResolvedValue({
            totalRequested: 1, totalDeleted: 1, totalFailed: 0, results: [],
          }),
          fetchFindings: jest.fn().mockResolvedValue({
            items: [finding],
            totalCount: 1,
          }),
        })
      );
      renderPage();
      await userEvent.click(screen.getByRole('tab', { name: /Remediate/i }));
      await waitFor(() => {
        expect(screen.getByTestId('delete-all-malicious')).toBeInTheDocument();
      });
    });
  });
});
