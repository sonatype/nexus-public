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
import { render, screen, within, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';

// FindingsTable gates mutating controls via the provider-independent ExtJS.usePermission
// (coreui never mounts a <PermissionsProvider>, so the context hook returns false for
// everyone — NEXUS-54212). Spy on the real ExtJS singleton; ExtJS.usePermission evaluates
// its getter synchronously at render.
import { ExtJS } from '../../../../../interface/ExtJS';
import Permissions from '../../../../../constants/Permissions';
const mockCheckPermission = jest.spyOn(ExtJS, 'checkPermission');

import { FindingsTable } from '../FindingsTable';
import type { MaliciousFinding } from '../types';
import type { RemediateResponse, FindingsPage, TaskInfo } from '../useMaliciousPackagesData';

function renderWithTheme(ui: React.ReactElement) {
  return render(<Theme>{ui}</Theme>);
}

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

function makeRemediateResponse(overrides: Partial<RemediateResponse> = {}): RemediateResponse {
  return {
    totalRequested: 1,
    totalDeleted: 1,
    totalFailed: 0,
    results: [{ findingId: 1, repositoryName: 'npm-proxy', assetPath: '/some/path', success: true, error: null }],
    ...overrides,
  };
}

function makeFetchFindings(items: MaliciousFinding[], totalCount?: number) {
  return jest.fn<Promise<FindingsPage>, [number, number, number]>().mockResolvedValue({
    items,
    totalCount: totalCount ?? items.length,
  });
}

const defaultProps = {
  onRemediateFindings: jest.fn<Promise<RemediateResponse>, [number[]]>().mockResolvedValue(makeRemediateResponse()),
  onRemediateRepository: jest.fn<Promise<void>, [string]>().mockResolvedValue(undefined),
  onAcknowledge: jest.fn<Promise<void>, [number, string, string?]>().mockResolvedValue(undefined),
  onBulkAcknowledge: jest.fn<Promise<void>, [number[], string, string?]>().mockResolvedValue(undefined),
  fetchFindings: makeFetchFindings([]),
  signatureCount: 0,
};

beforeEach(() => {
  jest.clearAllMocks();
  // Default: nexus:* admin so pre-existing behavior (mutating controls visible) is exercised.
  mockCheckPermission.mockReturnValue(true);
});

describe('FindingsTable', () => {
  describe('empty state', () => {
    it('shows empty callout when no findings and no signatures', async () => {
      renderWithTheme(<FindingsTable {...defaultProps} fetchFindings={makeFetchFindings([])} signatureCount={0} />);

      await waitFor(() => {
        expect(screen.getByTestId('no-findings-callout')).toBeInTheDocument();
      });
      expect(screen.getByText('No findings in the selected time range.')).toBeInTheDocument();
    });

    it('shows signature callout when no findings but signatures exist', async () => {
      renderWithTheme(<FindingsTable {...defaultProps} fetchFindings={makeFetchFindings([])} signatureCount={6} />);

      await waitFor(() => {
        expect(screen.getByTestId('signatures-callout')).toBeInTheDocument();
      });
      expect(screen.getByText(/6 malicious package signatures/)).toBeInTheDocument();
      expect(screen.getByText(/Run remediation tasks/)).toBeInTheDocument();
    });
  });

  describe('heading, tabs, and date filter', () => {
    it('renders "Findings to Remediate" heading', async () => {
      renderWithTheme(<FindingsTable {...defaultProps} fetchFindings={makeFetchFindings([])} />);

      await waitFor(() => {
        expect(screen.getByText('Findings to Remediate')).toBeInTheDocument();
      });
    });

    it('renders all three view tabs: Findings, By Component, By Repository', async () => {
      const findings = [makeFinding()];
      renderWithTheme(<FindingsTable {...defaultProps} fetchFindings={makeFetchFindings(findings)} />);

      await waitFor(() => {
        expect(screen.getByTestId('tab-findings')).toBeInTheDocument();
      });
      expect(screen.getByTestId('tab-by-component')).toBeInTheDocument();
      expect(screen.getByTestId('tab-by-repository')).toBeInTheDocument();
    });

    it('renders date range select with 30-day default', async () => {
      renderWithTheme(<FindingsTable {...defaultProps} fetchFindings={makeFetchFindings([])} />);

      await waitFor(() => {
        expect(screen.getByTestId('date-range-select')).toBeInTheDocument();
      });
    });

    it('fetches findings with sinceDays=30 on initial load', async () => {
      const mockFetch = makeFetchFindings([]);
      renderWithTheme(<FindingsTable {...defaultProps} fetchFindings={mockFetch} />);

      await waitFor(() => {
        expect(mockFetch).toHaveBeenCalledWith(30, 20, 0, undefined);
      });
    });
  });

  describe('status badges', () => {
    it('shows Remediated badge for deleted findings', async () => {
      const findings = [
        makeFinding({ id: 1, deletedTime: '2026-03-20T10:00:00Z', deletedBy: 'admin' }),
      ];
      renderWithTheme(<FindingsTable {...defaultProps} fetchFindings={makeFetchFindings(findings)} initialStatusFilter="all" />);

      await waitFor(() => {
        expect(screen.getByText('Remediated')).toBeInTheDocument();
      });
    });

    it('shows Risk Accepted badge for acknowledged findings', async () => {
      const findings = [
        makeFinding({ id: 1, acknowledgedAt: '2026-03-20T10:00:00Z' }),
      ];
      renderWithTheme(<FindingsTable {...defaultProps} fetchFindings={makeFetchFindings(findings)} initialStatusFilter="all" />);

      await waitFor(() => {
        expect(screen.getAllByText('Risk Accepted').length).toBeGreaterThanOrEqual(1);
      });
    });

    it('shows Pending badge for active findings', async () => {
      const findings = [makeFinding({ id: 1 })];
      renderWithTheme(<FindingsTable {...defaultProps} fetchFindings={makeFetchFindings(findings)} />);

      await waitFor(() => {
        expect(screen.getByText('Pending')).toBeInTheDocument();
      });
    });
  });

  describe('pending findings banner', () => {
    it('shows pending count banner when there are pending findings', async () => {
      const findings = [
        makeFinding({ id: 1 }),
        makeFinding({ id: 2, deletedTime: '2026-03-20T10:00:00Z' }),
      ];
      renderWithTheme(<FindingsTable {...defaultProps} fetchFindings={makeFetchFindings(findings)} />);

      await waitFor(() => {
        expect(screen.getByTestId('pending-findings-banner')).toBeInTheDocument();
      });
      const banner = screen.getByTestId('pending-findings-banner');
      expect(within(banner).getByText(/require remediation/)).toBeInTheDocument();
    });
  });

  describe('findings view (default flat list)', () => {
    it('shows individual finding rows with repository, component, status', async () => {
      const findings = [
        makeFinding({ id: 1, repositoryName: 'npm-proxy' }),
        makeFinding({ id: 2, repositoryName: 'maven-central', componentName: 'evil-jar', format: 'maven2' }),
      ];

      renderWithTheme(<FindingsTable {...defaultProps} fetchFindings={makeFetchFindings(findings)} />);

      await waitFor(() => {
        expect(screen.getByTestId('finding-row-1')).toBeInTheDocument();
      });
      expect(screen.getByTestId('finding-row-2')).toBeInTheDocument();
      expect(screen.getByText('npm-proxy')).toBeInTheDocument();
      expect(screen.getByText('maven-central')).toBeInTheDocument();
    });

    it('shows per-finding Delete button for pending findings', async () => {
      const findings = [makeFinding({ id: 1, repositoryName: 'npm-proxy' })];

      renderWithTheme(<FindingsTable {...defaultProps} fetchFindings={makeFetchFindings(findings)} />);

      await waitFor(() => {
        expect(screen.getByTestId('delete-finding-btn-1')).toBeInTheDocument();
      });
    });

    it('shows deletion info for already-deleted findings', async () => {
      const findings = [
        makeFinding({ id: 1, deletedTime: '2026-03-20T10:00:00Z', deletedBy: 'admin' }),
      ];

      renderWithTheme(<FindingsTable {...defaultProps} fetchFindings={makeFetchFindings(findings)} initialStatusFilter="all" />);

      await waitFor(() => {
        expect(screen.getByText(/admin/)).toBeInTheDocument();
      });
    });

    it('opens delete confirmation modal for a single finding', async () => {
      const findings = [makeFinding({ id: 1, repositoryName: 'npm-proxy' })];

      renderWithTheme(<FindingsTable {...defaultProps} fetchFindings={makeFetchFindings(findings)} />);

      await waitFor(() => {
        expect(screen.getByTestId('delete-finding-btn-1')).toBeInTheDocument();
      });

      await userEvent.click(screen.getByTestId('delete-finding-btn-1'));

      const modal = screen.getByTestId('remediate-modal');
      expect(modal).toBeInTheDocument();
      expect(within(modal).getByText(/Delete malicious package/)).toBeInTheDocument();
      expect(within(modal).getAllByText('evil-package@1.0.0').length).toBeGreaterThanOrEqual(1);
      expect(within(modal).getAllByText('npm-proxy').length).toBeGreaterThanOrEqual(1);
    });

    it('calls onRemediateFindings when single delete is confirmed', async () => {
      const findings = [makeFinding({ id: 42, repositoryName: 'npm-proxy' })];
      const mockRemediate = jest.fn<Promise<RemediateResponse>, [number[]]>().mockResolvedValue(
        makeRemediateResponse({
          results: [{ findingId: 42, repositoryName: 'npm-proxy', assetPath: '/some/path', success: true, error: null }],
        })
      );

      renderWithTheme(
        <FindingsTable
          {...defaultProps}
          fetchFindings={makeFetchFindings(findings)}
          onRemediateFindings={mockRemediate}
        />
      );

      await waitFor(() => {
        expect(screen.getByTestId('delete-finding-btn-42')).toBeInTheDocument();
      });

      await userEvent.click(screen.getByTestId('delete-finding-btn-42'));
      await userEvent.click(screen.getByTestId('confirm-delete-finding'));

      expect(mockRemediate).toHaveBeenCalledWith([42]);
    });
  });

  describe('by-component view', () => {
    it('groups findings by component with format and repo count', async () => {
      const findings = [
        makeFinding({ id: 1, componentName: 'evil-package', componentVersion: '1.0.0', repositoryName: 'npm-proxy' }),
        makeFinding({ id: 2, componentName: 'evil-package', componentVersion: '1.0.0', repositoryName: 'npm-proxy-2' }),
        makeFinding({ id: 3, componentName: 'bad-lib', componentVersion: '2.0.0' }),
      ];

      renderWithTheme(<FindingsTable {...defaultProps} fetchFindings={makeFetchFindings(findings)} />);

      await waitFor(() => {
        expect(screen.getByTestId('tab-by-component')).toBeInTheDocument();
      });

      await userEvent.click(screen.getByTestId('tab-by-component'));

      expect(screen.getByTestId('component-group-evil-package')).toBeInTheDocument();
      expect(screen.getByTestId('component-group-bad-lib')).toBeInTheDocument();
      expect(screen.getByText('in 2 repos')).toBeInTheDocument();
    });

    it('shows Review and Delete buttons per component', async () => {
      const findings = [makeFinding({ id: 1 })];

      renderWithTheme(<FindingsTable {...defaultProps} fetchFindings={makeFetchFindings(findings)} />);

      await waitFor(() => {
        expect(screen.getByTestId('tab-by-component')).toBeInTheDocument();
      });

      await userEvent.click(screen.getByTestId('tab-by-component'));

      expect(screen.getByTestId('review-evil-package')).toBeInTheDocument();
      expect(screen.getByTestId('delete-evil-package')).toBeInTheDocument();
    });

    it('opens Review modal showing repos', async () => {
      const findings = [
        makeFinding({ id: 1, repositoryName: 'npm-proxy' }),
        makeFinding({ id: 2, repositoryName: 'npm-proxy-2' }),
      ];

      renderWithTheme(<FindingsTable {...defaultProps} fetchFindings={makeFetchFindings(findings)} />);

      await waitFor(() => {
        expect(screen.getByTestId('tab-by-component')).toBeInTheDocument();
      });

      await userEvent.click(screen.getByTestId('tab-by-component'));
      await userEvent.click(screen.getByTestId('review-evil-package'));

      const modal = screen.getByTestId('remediate-modal');
      expect(modal).toBeInTheDocument();
      expect(within(modal).getByText(/Found in 2 location/)).toBeInTheDocument();
    });

    it('opens Delete modal and calls onRemediateFindings for all repos', async () => {
      const findings = [
        makeFinding({ id: 10, repositoryName: 'npm-proxy' }),
        makeFinding({ id: 11, repositoryName: 'npm-proxy-2' }),
      ];

      const mockRemediate = jest.fn<Promise<RemediateResponse>, [number[]]>().mockResolvedValue(
        makeRemediateResponse({ totalRequested: 2, totalDeleted: 2, results: [] })
      );

      renderWithTheme(
        <FindingsTable
          {...defaultProps}
          fetchFindings={makeFetchFindings(findings)}
          onRemediateFindings={mockRemediate}
        />
      );

      await waitFor(() => {
        expect(screen.getByTestId('tab-by-component')).toBeInTheDocument();
      });

      await userEvent.click(screen.getByTestId('tab-by-component'));
      await userEvent.click(screen.getByTestId('delete-evil-package'));
      await userEvent.click(screen.getByTestId('confirm-delete-all-component'));

      expect(mockRemediate).toHaveBeenCalledWith(expect.arrayContaining([10, 11]));
    });
  });

  describe('by-repository view (pivot)', () => {
    it('shows one row per repository with component count and max threat', async () => {
      const findings = [
        makeFinding({ id: 1, repositoryName: 'npm-proxy', componentName: 'evil-package', threatLevel: 10 }),
        makeFinding({ id: 2, repositoryName: 'npm-proxy', componentName: 'bad-lib', threatLevel: 7 }),
        makeFinding({ id: 3, repositoryName: 'maven-central', componentName: 'evil-jar', threatLevel: 5 }),
      ];

      renderWithTheme(<FindingsTable {...defaultProps} fetchFindings={makeFetchFindings(findings)} />);

      await waitFor(() => {
        expect(screen.getByTestId('tab-by-repository')).toBeInTheDocument();
      });

      await userEvent.click(screen.getByTestId('tab-by-repository'));

      expect(screen.getByTestId('repo-group-npm-proxy')).toBeInTheDocument();
      expect(screen.getByTestId('repo-group-maven-central')).toBeInTheDocument();

      const npmRow = screen.getByTestId('repo-group-npm-proxy');
      expect(within(npmRow).getByText('2 Components')).toBeInTheDocument();
      expect(within(npmRow).getByText('Critical')).toBeInTheDocument();

      const mavenRow = screen.getByTestId('repo-group-maven-central');
      expect(within(mavenRow).getByText('1 Component')).toBeInTheDocument();
      expect(within(mavenRow).getByText('Moderate')).toBeInTheDocument();
    });

    it('shows Review and Delete buttons per repository', async () => {
      const findings = [makeFinding({ id: 1, repositoryName: 'npm-proxy' })];

      renderWithTheme(<FindingsTable {...defaultProps} fetchFindings={makeFetchFindings(findings)} />);

      await waitFor(() => {
        expect(screen.getByTestId('tab-by-repository')).toBeInTheDocument();
      });

      await userEvent.click(screen.getByTestId('tab-by-repository'));

      expect(screen.getByTestId('review-repo-npm-proxy')).toBeInTheDocument();
      expect(screen.getByTestId('delete-repo-npm-proxy')).toBeInTheDocument();
    });

    it('disables repo Delete when a Malicious Packages task is running for that repo', async () => {
      const findings = [makeFinding({ id: 1, repositoryName: 'npm-proxy' })];
      const busyTasks: TaskInfo[] = [
        {
          id: 't1',
          name: 'Remediate - npm-proxy',
          repositoryName: 'npm-proxy',
          mode: 'delete',
          enabled: true,
          lastRun: null,
          lastRunResult: null,
          nextRun: null,
          currentState: 'RUNNING',
          progress: null,
        },
      ];

      renderWithTheme(
        <FindingsTable {...defaultProps} fetchFindings={makeFetchFindings(findings)} tasks={busyTasks} />
      );

      await waitFor(() => {
        expect(screen.getByTestId('tab-by-repository')).toBeInTheDocument();
      });

      await userEvent.click(screen.getByTestId('tab-by-repository'));

      expect(screen.getByTestId('delete-repo-npm-proxy')).toBeDisabled();
      expect(screen.getByText('Task running')).toBeInTheDocument();
    });

    it('opens Review modal showing components in the repo', async () => {
      const findings = [
        makeFinding({ id: 1, repositoryName: 'npm-proxy', componentName: 'evil-package' }),
        makeFinding({ id: 2, repositoryName: 'npm-proxy', componentName: 'bad-lib' }),
      ];

      renderWithTheme(<FindingsTable {...defaultProps} fetchFindings={makeFetchFindings(findings)} />);

      await waitFor(() => {
        expect(screen.getByTestId('tab-by-repository')).toBeInTheDocument();
      });

      await userEvent.click(screen.getByTestId('tab-by-repository'));
      await userEvent.click(screen.getByTestId('review-repo-npm-proxy'));

      const modal = screen.getByTestId('remediate-modal');
      expect(modal).toBeInTheDocument();
      expect(within(modal).getByText(/npm-proxy/)).toBeInTheDocument();
      expect(within(modal).getByText(/2 malicious packages/)).toBeInTheDocument();
      expect(within(modal).getByTestId('modal-finding-1')).toBeInTheDocument();
      expect(within(modal).getByTestId('modal-finding-2')).toBeInTheDocument();
    });

    it('calls onRemediateRepository when Delete All is confirmed from repo pivot', async () => {
      const findings = [makeFinding({ id: 1, repositoryName: 'npm-proxy' })];
      const mockRemediateRepo = jest.fn<Promise<void>, [string]>().mockResolvedValue(undefined);

      renderWithTheme(
        <FindingsTable
          {...defaultProps}
          fetchFindings={makeFetchFindings(findings)}
          onRemediateRepository={mockRemediateRepo}
        />
      );

      await waitFor(() => {
        expect(screen.getByTestId('tab-by-repository')).toBeInTheDocument();
      });

      await userEvent.click(screen.getByTestId('tab-by-repository'));
      await userEvent.click(screen.getByTestId('delete-repo-npm-proxy'));
      await userEvent.click(screen.getByTestId('confirm-delete-all-repo'));

      expect(mockRemediateRepo).toHaveBeenCalledWith('npm-proxy');
    });
  });

  describe('per-finding delete in modal', () => {
    it('calls onRemediateFindings for individual finding in repo-review modal and shows success', async () => {
      const findings = [
        makeFinding({ id: 42, repositoryName: 'npm-proxy' }),
        makeFinding({ id: 43, repositoryName: 'npm-proxy', componentName: 'bad-lib' }),
      ];

      const mockFetch = makeFetchFindings(findings);
      const mockRemediate = jest.fn<Promise<RemediateResponse>, [number[]]>().mockResolvedValue(
        makeRemediateResponse({
          results: [{ findingId: 42, repositoryName: 'npm-proxy', assetPath: '/some/path', success: true, error: null }],
        })
      );

      renderWithTheme(
        <FindingsTable
          {...defaultProps}
          fetchFindings={mockFetch}
          onRemediateFindings={mockRemediate}
        />
      );

      await waitFor(() => {
        expect(screen.getByTestId('tab-by-repository')).toBeInTheDocument();
      });

      await userEvent.click(screen.getByTestId('tab-by-repository'));
      await userEvent.click(screen.getByTestId('review-repo-npm-proxy'));

      const modal = screen.getByTestId('remediate-modal');
      await userEvent.click(within(modal).getByTestId('delete-finding-42'));

      expect(mockRemediate).toHaveBeenCalledWith([42]);

      await waitFor(() => {
        expect(within(modal).getByText('Deleted')).toBeInTheDocument();
      });
    });

    it('shows error state when individual delete fails in modal', async () => {
      const findings = [makeFinding({ id: 42 })];

      const mockFetch = makeFetchFindings(findings);
      const mockRemediate = jest.fn<Promise<RemediateResponse>, [number[]]>().mockResolvedValue(
        makeRemediateResponse({
          totalDeleted: 0,
          totalFailed: 1,
          results: [{ findingId: 42, repositoryName: 'npm-proxy', assetPath: '/some/path', success: false, error: 'Permission denied' }],
        })
      );

      renderWithTheme(
        <FindingsTable
          {...defaultProps}
          fetchFindings={mockFetch}
          onRemediateFindings={mockRemediate}
        />
      );

      await waitFor(() => {
        expect(screen.getByTestId('tab-by-repository')).toBeInTheDocument();
      });

      await userEvent.click(screen.getByTestId('tab-by-repository'));
      await userEvent.click(screen.getByTestId('review-repo-npm-proxy'));

      const modal = screen.getByTestId('remediate-modal');
      await userEvent.click(within(modal).getByTestId('delete-finding-42'));

      await waitFor(() => {
        expect(within(modal).getByText('Failed')).toBeInTheDocument();
      });
    });
  });

  describe('pagination', () => {
    it('fetches with correct offset when page changes', async () => {
      const findings = Array.from({ length: 20 }, (_, i) => makeFinding({ id: i + 1 }));
      const mockFetch = makeFetchFindings(findings, 50);

      renderWithTheme(<FindingsTable {...defaultProps} fetchFindings={mockFetch} />);

      await waitFor(() => {
        expect(mockFetch).toHaveBeenCalledWith(30, 20, 0, undefined);
      });
    });
  });

  describe('write gating (NEXUS-54212)', () => {
    const renderTable = () =>
      renderWithTheme(<FindingsTable {...defaultProps} fetchFindings={makeFetchFindings([makeFinding()])} />);

    it('shows Delete and Accept Risk for nexus:* admin', async () => {
      mockCheckPermission.mockImplementation((p: string) => p === Permissions.ADMIN);
      renderTable();
      await waitFor(() => expect(screen.getByTestId('finding-row-1')).toBeInTheDocument());
      expect(screen.getByTestId('delete-finding-btn-1')).toBeInTheDocument();
      expect(screen.getByTestId('accept-risk-btn-1')).toBeInTheDocument();
      expect(screen.getByTestId('delete-all-malicious')).toBeInTheDocument();
    });

    it('hides Delete, Accept Risk, and Delete All for non-admin', async () => {
      mockCheckPermission.mockReturnValue(false);
      renderTable();
      await waitFor(() => expect(screen.getByTestId('finding-row-1')).toBeInTheDocument());
      expect(screen.queryByTestId('delete-finding-btn-1')).not.toBeInTheDocument();
      expect(screen.queryByTestId('accept-risk-btn-1')).not.toBeInTheDocument();
      expect(screen.queryByTestId('delete-all-malicious')).not.toBeInTheDocument();
    });

    it('hides component-view Delete/Accept Risk for non-admin but keeps Review', async () => {
      mockCheckPermission.mockReturnValue(false);
      renderTable();
      await waitFor(() => expect(screen.getByTestId('finding-row-1')).toBeInTheDocument());
      await userEvent.click(screen.getByTestId('tab-by-component'));
      expect(screen.getByTestId('review-evil-package')).toBeInTheDocument();
      expect(screen.queryByTestId('delete-evil-package')).not.toBeInTheDocument();
      expect(screen.queryByTestId('accept-risk-evil-package')).not.toBeInTheDocument();
    });

    it('keeps read-only tabs and expand visible for non-admin', async () => {
      mockCheckPermission.mockReturnValue(false);
      renderTable();
      await waitFor(() => expect(screen.getByTestId('finding-row-1')).toBeInTheDocument());
      expect(screen.getByTestId('tab-findings')).toBeInTheDocument();
      expect(screen.getByTestId('tab-by-repository')).toBeInTheDocument();
      expect(screen.getByTestId('expand-finding-1')).toBeInTheDocument();
    });
  });
});
