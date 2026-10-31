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

/**
 * Direct unit tests for the pure helpers `deriveServerRhcScans` and `mergeRhcScans`.
 * These functions carry non-trivial policy (stuck-threshold detection, optimistic
 * entry preservation, failure entry persistence across polls) that was previously
 * only exercised end-to-end through the machine tests.
 */

import {
  deriveServerRhcScans,
  mergeRhcScans,
  RHC_STUCK_ERROR,
  RHC_STUCK_THRESHOLD_MS,
  type HealthCheckSummaryItem,
} from '../maliciousPackagesMachine';
import type { RhcScanInfo } from '../maliciousPackagesUtils';

describe('deriveServerRhcScans', () => {
  const now = Date.now();

  it('reports scanning while the summary is analyzing', () => {
    const initiatedAt = now - 30_000;
    const hcSummary: HealthCheckSummaryItem[] = [
      { repositoryName: 'r1', enabled: true, analyzing: true },
    ];
    const result = deriveServerRhcScans(hcSummary, { r1: initiatedAt }, {}, []);
    expect(result.get('r1')).toEqual({ phase: 'scanning', startedAt: initiatedAt });
  });

  it('reports completed with signature count once analysis lands', () => {
    const initiatedAt = now - 60_000;
    const completedAt = now - 5_000;
    const hcSummary: HealthCheckSummaryItem[] = [
      {
        repositoryName: 'r1',
        enabled: true,
        analyzing: false,
        lastAnalyzedDate: completedAt,
        malwareCount: 3,
      },
    ];
    // counts arg takes precedence over hc.malwareCount
    const result = deriveServerRhcScans(hcSummary, { r1: initiatedAt }, { r1: 7 }, ['r1']);
    expect(result.get('r1')).toEqual({
      phase: 'completed',
      startedAt: initiatedAt,
      completedAt,
      signatureCount: 7,
    });
  });

  it('falls back to hc.malwareCount when counts is missing an entry', () => {
    const initiatedAt = now - 60_000;
    const completedAt = now - 5_000;
    const hcSummary: HealthCheckSummaryItem[] = [
      {
        repositoryName: 'r1',
        enabled: true,
        analyzing: false,
        lastAnalyzedDate: completedAt,
        malwareCount: 4,
      },
    ];
    const result = deriveServerRhcScans(hcSummary, { r1: initiatedAt }, {}, ['r1']);
    expect(result.get('r1')?.phase).toBe('completed');
    expect((result.get('r1') as { signatureCount: number }).signatureCount).toBe(4);
  });

  it('emits failed with RHC_STUCK_ERROR when a repo remains unenabled past the stuck threshold', () => {
    const initiatedAt = now - (RHC_STUCK_THRESHOLD_MS + 1_000);
    const hcSummary: HealthCheckSummaryItem[] = [];
    const result = deriveServerRhcScans(hcSummary, { r1: initiatedAt }, {}, []);
    const entry = result.get('r1');
    expect(entry?.phase).toBe('failed');
    expect((entry as { error: string }).error).toBe(RHC_STUCK_ERROR);
    expect(entry?.startedAt).toBe(initiatedAt);
  });

  it('still reports scanning below the stuck threshold when the repo is not yet enabled', () => {
    const initiatedAt = now - 10_000;
    const result = deriveServerRhcScans([], { r1: initiatedAt }, {}, []);
    expect(result.get('r1')).toEqual({ phase: 'scanning', startedAt: initiatedAt });
  });

  it('does not flag as stuck once the repo is enabled (waiting on analyzing to flip)', () => {
    const initiatedAt = now - (RHC_STUCK_THRESHOLD_MS + 1_000);
    const hcSummary: HealthCheckSummaryItem[] = [
      { repositoryName: 'r1', enabled: true, analyzing: false, lastAnalyzedDate: null },
    ];
    const result = deriveServerRhcScans(hcSummary, { r1: initiatedAt }, {}, ['r1']);
    // Enabled but no lastAnalyzedDate → falls to the default 'scanning' branch.
    expect(result.get('r1')).toEqual({ phase: 'scanning', startedAt: initiatedAt });
  });
});

describe('mergeRhcScans', () => {
  it('replaces prior entries with server-provided entries for the same repo', () => {
    const prev = new Map<string, RhcScanInfo>([
      ['r1', { phase: 'scanning', startedAt: 1_000 }],
    ]);
    const server = new Map<string, RhcScanInfo>([
      [
        'r1',
        { phase: 'completed', startedAt: 1_000, completedAt: 5_000, signatureCount: 2 },
      ],
    ]);
    const merged = mergeRhcScans(prev, new Set(), server);
    expect(merged.get('r1')).toEqual({
      phase: 'completed',
      startedAt: 1_000,
      completedAt: 5_000,
      signatureCount: 2,
    });
  });

  it('preserves an optimistic scanning entry when it is in enablePending and absent from server', () => {
    const prev = new Map<string, RhcScanInfo>([
      ['r1', { phase: 'scanning', startedAt: 1_000 }],
    ]);
    const merged = mergeRhcScans(prev, new Set(['r1']), new Map());
    expect(merged.get('r1')).toEqual({ phase: 'scanning', startedAt: 1_000 });
  });

  it('drops an optimistic scanning entry once the repo leaves enablePending', () => {
    const prev = new Map<string, RhcScanInfo>([
      ['r1', { phase: 'scanning', startedAt: 1_000 }],
    ]);
    const merged = mergeRhcScans(prev, new Set(), new Map());
    expect(merged.has('r1')).toBe(false);
  });

  it('preserves a failure entry across polls until the server overwrites it', () => {
    const prev = new Map<string, RhcScanInfo>([
      [
        'r1',
        { phase: 'failed', startedAt: 1_000, completedAt: 2_000, error: 'boom' },
      ],
    ]);
    // enablePending is empty — failure entries persist regardless.
    const merged = mergeRhcScans(prev, new Set(), new Map());
    expect(merged.get('r1')).toEqual({
      phase: 'failed',
      startedAt: 1_000,
      completedAt: 2_000,
      error: 'boom',
    });
  });

  it('lets a server entry overwrite a stale failure entry when the server has fresh state', () => {
    const prev = new Map<string, RhcScanInfo>([
      [
        'r1',
        { phase: 'failed', startedAt: 1_000, completedAt: 2_000, error: 'boom' },
      ],
    ]);
    const server = new Map<string, RhcScanInfo>([
      [
        'r1',
        { phase: 'completed', startedAt: 1_000, completedAt: 3_000, signatureCount: 1 },
      ],
    ]);
    const merged = mergeRhcScans(prev, new Set(), server);
    expect(merged.get('r1')?.phase).toBe('completed');
  });

  it('does not resurrect a completed prior entry when the server no longer reports it', () => {
    const prev = new Map<string, RhcScanInfo>([
      [
        'r1',
        { phase: 'completed', startedAt: 1_000, completedAt: 2_000, signatureCount: 3 },
      ],
    ]);
    const merged = mergeRhcScans(prev, new Set(), new Map());
    expect(merged.has('r1')).toBe(false);
  });
});
