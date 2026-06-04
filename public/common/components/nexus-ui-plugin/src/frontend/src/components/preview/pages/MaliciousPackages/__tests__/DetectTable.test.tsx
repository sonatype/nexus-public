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
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';

import { DetectTable } from '../DetectTable';
import { MaliciousFinding } from '../types';
import { TaskInfo, ProxyRepo, RhcScanInfo } from '../useMaliciousPackagesData';

function renderWithTheme(ui: React.ReactElement) {
  return render(<Theme>{ui}</Theme>);
}

function makeTask(overrides: Partial<TaskInfo> = {}): TaskInfo {
  return {
    id: 'task-npm',
    name: 'Malicious Packages - npm-proxy',
    repositoryName: 'npm-proxy',
    mode: 'audit',
    enabled: true,
    lastRun: '2026-03-01T12:00:00.000Z',
    lastRunResult: 'OK',
    nextRun: null,
    currentState: null,
    progress: null,
    ...overrides,
  };
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
    hash: 'abc123',
    createdBy: 'anonymous',
    createdByIp: '10.0.0.1',
    componentName: 'evil-package',
    componentVersion: '1.0.0',
    componentFormat: 'npm',
    threatLevel: 10,
    threatSummary: 'Malware',
    threatReference: null,
    policyName: 'Malware-Block',
    ...overrides,
  };
}

const defaultProps = {
  proxyRepos: [
    { name: 'npm-proxy', format: 'npm', rhcSupported: true, rhcEnabled: true },
    { name: 'maven-proxy', format: 'maven2', rhcSupported: true, rhcEnabled: false },
    { name: 'raw-proxy', format: 'raw', rhcSupported: false, rhcEnabled: false },
  ] as ProxyRepo[],
  hcEnabledRepos: ['npm-proxy'],
  countsByRepo: { 'npm-proxy': 3 } as Record<string, number>,
  rhcScans: new Map<string, RhcScanInfo>(),
  tasks: [] as TaskInfo[],
  activeFindings: [] as MaliciousFinding[],
  onEnableRhc: jest.fn().mockResolvedValue(undefined),
  onIdentify: jest.fn(),
  onNavigateToRemediate: jest.fn(),
};

describe('DetectTable', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders the unified table with data-testid="detect-table"', () => {
    renderWithTheme(<DetectTable {...defaultProps} />);

    const attentionTable = screen.getByTestId('detect-table');
    expect(attentionTable).toBeInTheDocument();
    expect(within(attentionTable).getByText(/^Repository/)).toBeInTheDocument();
    expect(within(attentionTable).getByText('Initial Scan')).toBeInTheDocument();
    expect(within(attentionTable).getByText('Deep Scan')).toBeInTheDocument();
    expect(within(attentionTable).getByText('Findings')).toBeInTheDocument();
    expect(within(attentionTable).getByText('Action')).toBeInTheDocument();
  });

  it('shows unmonitored repos with Enable Detection in a separate table', () => {
    renderWithTheme(<DetectTable {...defaultProps} />);

    expect(screen.queryByTestId('detect-row-maven-proxy')).not.toBeInTheDocument();
    const unmonitoredTable = screen.getByTestId('detect-unmonitored-table');
    expect(unmonitoredTable).toBeInTheDocument();
    expect(within(unmonitoredTable).getByTestId('unmonitored-row-maven-proxy')).toBeInTheDocument();
    expect(within(unmonitoredTable).getByTestId('enable-rhc-maven-proxy')).toBeInTheDocument();
  });

  it('excludes unsupported format repos (e.g., raw with rhcSupported: false)', () => {
    renderWithTheme(<DetectTable {...defaultProps} />);

    expect(screen.queryByTestId('detect-row-raw-proxy')).not.toBeInTheDocument();
    expect(screen.queryByText('raw-proxy')).not.toBeInTheDocument();
  });

  it('shows repos with signatures and "Run One-Time Analysis" when no task exists', () => {
    renderWithTheme(<DetectTable {...defaultProps} />);

    const npmRow = screen.getByTestId('detect-row-npm-proxy');
    expect(within(npmRow).getByText(/3 signatures/)).toBeInTheDocument();
    expect(screen.getByTestId('identify-npm-proxy')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Run One-Time Analysis' })).toBeInTheDocument();
  });

  it('shows findings count as clickable link when deep scan completed (OK + active findings)', () => {
    const tasks = [makeTask()];
    const activeFindings = [
      makeFinding({ id: 1, repositoryName: 'npm-proxy' }),
      makeFinding({ id: 2, repositoryName: 'npm-proxy', componentName: 'other-evil' }),
    ];

    renderWithTheme(
      <DetectTable
        {...defaultProps}
        tasks={tasks}
        activeFindings={activeFindings}
      />
    );

    const link = screen.getByTestId('findings-link-npm-proxy');
    expect(link).toBeInTheDocument();
    expect(link).toHaveTextContent('2 malicious packages');
  });

  it('calls onNavigateToRemediate when findings link is clicked', async () => {
    const onNavigateToRemediate = jest.fn();
    const tasks = [makeTask()];
    const activeFindings = [makeFinding({ id: 1, repositoryName: 'npm-proxy' })];

    renderWithTheme(
      <DetectTable
        {...defaultProps}
        tasks={tasks}
        activeFindings={activeFindings}
        onNavigateToRemediate={onNavigateToRemediate}
      />
    );

    await userEvent.click(screen.getByTestId('findings-link-npm-proxy'));

    expect(onNavigateToRemediate).toHaveBeenCalledTimes(1);
    expect(onNavigateToRemediate).toHaveBeenCalledWith('npm-proxy');
  });

  it('shows Scanning state when RHC scan is in progress', () => {
    const rhcScans = new Map<string, RhcScanInfo>([
      ['npm-proxy', { phase: 'scanning', startedAt: 1_700_000_000_000 }],
    ]);

    renderWithTheme(<DetectTable {...defaultProps} rhcScans={rhcScans} />);

    const npmRow = screen.getByTestId('detect-row-npm-proxy');
    expect(within(npmRow).getByText('Scanning…')).toBeInTheDocument();
    expect(within(npmRow).getByText(/Started/)).toBeInTheDocument();
  });

  it('shows dash in Findings when identified but zero malicious packages', () => {
    const tasks = [makeTask()];

    renderWithTheme(
      <DetectTable
        {...defaultProps}
        tasks={tasks}
        activeFindings={[]}
      />
    );

    expect(screen.queryByTestId('findings-link-npm-proxy')).not.toBeInTheDocument();
  });

  it('shows error reason when analysis task failed', () => {
    const tasks = [makeTask({ lastRunResult: 'FAILED', currentState: null })];

    renderWithTheme(
      <DetectTable
        {...defaultProps}
        tasks={tasks}
      />
    );

    const npmRow = screen.getByTestId('detect-row-npm-proxy');
    expect(within(npmRow).getByText('Failed')).toBeInTheDocument();
    expect(within(npmRow).getByText(/Tasks for logs/)).toBeInTheDocument();
  });

  it('shows Not Analyzed when signatures exist but only a global "all" task ran (no repo-specific deep scan)', () => {
    const allTask = makeTask({ repositoryName: 'all', lastRunResult: 'OK' });

    renderWithTheme(
      <DetectTable
        {...defaultProps}
        tasks={[allTask]}
        activeFindings={[]}
      />
    );

    const npmRow = screen.getByTestId('detect-row-npm-proxy');
    expect(within(npmRow).getByText(/3 signatures/)).toBeInTheDocument();
    expect(within(npmRow).getByText('Not Analyzed')).toBeInTheDocument();
    expect(screen.getByTestId('identify-npm-proxy')).toBeInTheDocument();
  });

  it('shows Failed state with Retry button and troubleshoot link when RHC scan failed', () => {
    const rhcScans = new Map<string, RhcScanInfo>([
      ['maven-proxy', { phase: 'failed', startedAt: 1, error: 'scan failed' }],
    ]);

    renderWithTheme(<DetectTable {...defaultProps} rhcScans={rhcScans} />);

    const mavenRow = screen.getByTestId('detect-row-maven-proxy');
    expect(within(mavenRow).getByText('Failed')).toBeInTheDocument();
    expect(within(mavenRow).getByTestId('retry-rhc-maven-proxy')).toBeInTheDocument();
    expect(within(mavenRow).getByRole('button', { name: 'Retry' })).toBeInTheDocument();
    expect(within(mavenRow).getByText('scan failed')).toBeInTheDocument();
    expect(within(mavenRow).getByTestId('rhc-troubleshoot-maven-proxy')).toBeInTheDocument();
    expect(within(mavenRow).getByText('View repository settings')).toBeInTheDocument();
  });

  it('shows "Not Analyzed" for npm-proxy-central with 1 signature when no repo-specific task exists', () => {
    const props = {
      ...defaultProps,
      proxyRepos: [
        { name: 'npm-proxy-central', format: 'npm', rhcSupported: true, rhcEnabled: true },
      ] as ProxyRepo[],
      countsByRepo: { 'npm-proxy-central': 1 } as Record<string, number>,
      tasks: [makeTask({ repositoryName: 'all', lastRunResult: 'OK' })] as TaskInfo[],
      activeFindings: [] as MaliciousFinding[],
    };

    renderWithTheme(<DetectTable {...props} />);

    const row = screen.getByTestId('detect-row-npm-proxy-central');
    expect(within(row).getByText(/1 signatures/)).toBeInTheDocument();
    expect(within(row).getByText('Not Analyzed')).toBeInTheDocument();
  });

  it('ignores global "all" task completely — never uses it for per-repo state', () => {
    const allTask = makeTask({ id: 'all-task', repositoryName: 'all', lastRunResult: 'FAILED' });

    renderWithTheme(
      <DetectTable
        {...defaultProps}
        tasks={[allTask]}
        activeFindings={[]}
      />
    );

    const npmRow = screen.getByTestId('detect-row-npm-proxy');
    expect(within(npmRow).getByText('Not Analyzed')).toBeInTheDocument();
    expect(within(npmRow).queryByText('Failed')).not.toBeInTheDocument();
  });

  it('shows "No Threats Confirmed" when deep scan OK ran AFTER RHC completed (fresh task)', () => {
    const rhcCompletedAt = new Date('2026-03-01T10:00:00.000Z').getTime();
    const taskRanAfterRhc = '2026-03-01T12:00:00.000Z';

    const repoTask = makeTask({
      repositoryName: 'npm-proxy',
      lastRunResult: 'OK',
      lastRun: taskRanAfterRhc,
    });
    const rhcScans = new Map<string, RhcScanInfo>([
      ['npm-proxy', { phase: 'completed', startedAt: rhcCompletedAt - 60_000, completedAt: rhcCompletedAt }],
    ]);

    renderWithTheme(
      <DetectTable
        {...defaultProps}
        rhcScans={rhcScans}
        tasks={[repoTask]}
        activeFindings={[]}
      />
    );

    const npmRow = screen.getByTestId('detect-row-npm-proxy');
    expect(within(npmRow).getByText('No Threats Confirmed')).toBeInTheDocument();
    expect(within(npmRow).getByText(/signatures may take up to 6 hours/)).toBeInTheDocument();
  });

  it('shows "Not Analyzed" when task OK ran BEFORE RHC completed (stale task)', () => {
    const taskRanBeforeRhc = '2026-02-15T12:00:00.000Z';
    const rhcCompletedAt = new Date('2026-03-01T10:00:00.000Z').getTime();

    const repoTask = makeTask({
      repositoryName: 'npm-proxy',
      lastRunResult: 'OK',
      lastRun: taskRanBeforeRhc,
    });
    const rhcScans = new Map<string, RhcScanInfo>([
      ['npm-proxy', { phase: 'completed', startedAt: rhcCompletedAt - 60_000, completedAt: rhcCompletedAt }],
    ]);

    renderWithTheme(
      <DetectTable
        {...defaultProps}
        rhcScans={rhcScans}
        tasks={[repoTask]}
        activeFindings={[]}
      />
    );

    const npmRow = screen.getByTestId('detect-row-npm-proxy');
    expect(within(npmRow).getByText('Not Analyzed')).toBeInTheDocument();
    expect(within(npmRow).queryByText('No Threats Confirmed')).not.toBeInTheDocument();
    expect(screen.getByTestId('identify-npm-proxy')).toBeInTheDocument();
  });

  it('shows "Not Analyzed" when task OK exists but no RHC timing data (unknown freshness)', () => {
    const repoTask = makeTask({ repositoryName: 'npm-proxy', lastRunResult: 'OK' });

    renderWithTheme(
      <DetectTable
        {...defaultProps}
        tasks={[repoTask]}
        activeFindings={[]}
      />
    );

    const npmRow = screen.getByTestId('detect-row-npm-proxy');
    expect(within(npmRow).getByText('Not Analyzed')).toBeInTheDocument();
    expect(within(npmRow).queryByText('No Threats Confirmed')).not.toBeInTheDocument();
  });

  it('shows escalating error message on first failure', () => {
    const failedTask = makeTask({ repositoryName: 'npm-proxy', lastRunResult: 'FAILED' });

    renderWithTheme(
      <DetectTable
        {...defaultProps}
        tasks={[failedTask]}
      />
    );

    const npmRow = screen.getByTestId('detect-row-npm-proxy');
    expect(within(npmRow).getByText(/Tasks for logs/)).toBeInTheDocument();
  });

  it('shows Deep Scan progress and View task log while analyzing', () => {
    const rhcCompletedAt = new Date('2026-03-01T10:00:00.000Z').getTime();
    const repoTask = makeTask({
      id: 'identify-task-1',
      repositoryName: 'npm-proxy',
      currentState: 'RUNNING',
      progress: '17%',
      lastRunResult: null,
      lastRun: null,
    });
    const rhcScans = new Map<string, RhcScanInfo>([
      ['npm-proxy', { phase: 'completed', startedAt: rhcCompletedAt - 60_000, completedAt: rhcCompletedAt }],
    ]);

    renderWithTheme(
      <DetectTable
        {...defaultProps}
        rhcScans={rhcScans}
        tasks={[repoTask]}
        activeFindings={[]}
      />
    );

    const npmRow = screen.getByTestId('detect-row-npm-proxy');
    expect(within(npmRow).getByText(/Analyzing\s+17%/)).toBeInTheDocument();
    expect(within(npmRow).getByTestId('analyzing-task-log-link-npm-proxy')).toHaveTextContent('View task log');
  });
});
