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
import {render, screen} from '@testing-library/react';
import {CloudUsageCenterPanel} from '../CloudUsageCenterPanel';

const baseMetrics = {
  loading: false,
  error: null,
  history: {egress: [], storage: []},
};

describe('CloudUsageCenterPanel', () => {
  it('shows loading state', () => {
    render(<CloudUsageCenterPanel monthlyMetrics={{...baseMetrics, loading: true}} />);
    expect(screen.getByText('Loading usage metrics...')).toBeInTheDocument();
  });

  it('shows error state when error is set', () => {
    render(<CloudUsageCenterPanel monthlyMetrics={{...baseMetrics, error: 'Network error'}} />);
    expect(screen.getByText(/unable to load usage metrics/i)).toBeInTheDocument();
    expect(screen.queryByText('Usage Center')).not.toBeInTheDocument();
  });

  it('renders Usage Center heading when loaded with no data', () => {
    render(<CloudUsageCenterPanel monthlyMetrics={baseMetrics} />);
    expect(screen.getByText('Usage Center')).toBeInTheDocument();
  });

  it('renders "N/A" for empty history', () => {
    render(<CloudUsageCenterPanel monthlyMetrics={baseMetrics} />);
    const naValues = screen.getAllByText('N/A');
    expect(naValues.length).toBeGreaterThanOrEqual(6);
  });

  it('computes current, average, and peak egress from history', () => {
    const metrics = {
      ...baseMetrics,
      history: {
        // useMonthlyMetrics returns oldest-first; last element is current month
        egress: [{value: 100}, {value: 200}, {value: 300}],
        storage: [],
      },
    };
    render(<CloudUsageCenterPanel monthlyMetrics={metrics} />);
    // current = last element = 300
    expect(screen.getByText('300.00 Bytes')).toBeInTheDocument();
    // avg of prior months [100, 200] = 150
    expect(screen.getByText('150.00 Bytes')).toBeInTheDocument();
    // peak of prior months [100, 200] = 200
    expect(screen.getByText('200.00 Bytes')).toBeInTheDocument();
  });

  it('computes peak storage including current month (NEXUS-46933, matches Classic UI)', () => {
    const metrics = {
      ...baseMetrics,
      history: {
        egress: [],
        // useMonthlyMetrics returns oldest-first; last element (700) is current month.
        // Per NEXUS-46933, Peak Storage is max over ALL months (including current),
        // unlike Peak Egress which is prior-months-only.
        storage: [{value: 400}, {value: 900}, {value: 700}],
      },
    };
    render(<CloudUsageCenterPanel monthlyMetrics={metrics} />);
    // current = 700 (last element)
    expect(screen.getByText('700.00 Bytes')).toBeInTheDocument();
    // avg of prior [400, 900] = 650
    expect(screen.getByText('650.00 Bytes')).toBeInTheDocument();
    // peak = max of ALL [400, 900, 700] = 900 (includes current month)
    expect(screen.getByText('900.00 Bytes')).toBeInTheDocument();
  });

  it('preserves current-month position when zeros exist in earlier months', () => {
    // If a middle month is 0 (data gap), we must still read current from the
    // last position — filtering zeros first would shift indices and mis-identify
    // "current". Peak Storage on the full array still ignores zero as no-data.
    const metrics = {
      ...baseMetrics,
      history: {
        egress: [{value: 0}, {value: 200}, {value: 500}],
        storage: [{value: 400}, {value: 0}, {value: 700}],
      },
    };
    render(<CloudUsageCenterPanel monthlyMetrics={metrics} />);
    // Egress: current = 500 (position last), prior after zero-filter = [200] -> avg = 200, peak = 200
    expect(screen.getByText('500.00 Bytes')).toBeInTheDocument();
    expect(screen.getAllByText('200.00 Bytes').length).toBe(2); // avg + peak
    // Storage: current = 700, prior after zero-filter = [400] -> avg = 400, peak(all non-zero) = 700
    expect(screen.getAllByText('700.00 Bytes').length).toBe(2); // current + peak(all)
    expect(screen.getByText('400.00 Bytes')).toBeInTheDocument(); // avg
  });

  it('renders N/A for current when the latest month has zero data (does not fall back to prior month)', () => {
    // Regression for Issue 2: filtering zeros before selecting current would
    // silently promote the last non-zero month as "current"; positional selection
    // correctly reports N/A when the current month itself is 0.
    const metrics = {
      ...baseMetrics,
      history: {
        egress: [{value: 100}, {value: 200}, {value: 0}],
        storage: [],
      },
    };
    render(<CloudUsageCenterPanel monthlyMetrics={metrics} />);
    // current = 0 -> "N/A" (not 200)
    // Egress N/A count: current-egress + all three storage tile values = 4
    expect(screen.getAllByText('N/A').length).toBeGreaterThanOrEqual(4);
    // Avg of prior [100, 200] = 150, peak of prior = 200
    expect(screen.getByText('150.00 Bytes')).toBeInTheDocument();
    expect(screen.getByText('200.00 Bytes')).toBeInTheDocument();
  });

  it('reads current month from the end of oldest-first history (regression: not values[0])', () => {
    // useMonthlyMetrics returns history oldest-first. If the panel mistakenly reads
    // values[0], it would show ~year-old totals as "current month".
    const metrics = {
      ...baseMetrics,
      history: {
        egress: [
          {value: 1_000_000_000},   // oldest -> 1.00 GB
          {value: 2_000_000_000},
          {value: 5_000_000_000},   // newest -> 5.00 GB (current)
        ],
        storage: [],
      },
    };
    render(<CloudUsageCenterPanel monthlyMetrics={metrics} />);
    // Current should be the newest (last) value, not the oldest (first).
    expect(screen.getByText('5.00 GB')).toBeInTheDocument();
    expect(screen.queryByText('1.00 GB')).not.toBeInTheDocument();
  });

  it('renders "N/A" for negative byte values on every tile (never "Unavailable")', () => {
    // Regression for Diego Tovar's review on PR #17452: without the negative
    // guard in formatBytes, a negative current-month value falls through to
    // HumanReadableUtils.bytesToString and renders "Unavailable" (see
    // HumanReadableUtils.js:34), while the zero-filtered prior/peak tiles show
    // "N/A" — two different strings for the same "no data" condition.
    const metrics = {
      ...baseMetrics,
      history: {
        egress: [{value: -100}],
        storage: [{value: -50}],
      },
    };
    render(<CloudUsageCenterPanel monthlyMetrics={metrics} />);
    // All six tiles (current/avg/peak for egress + storage) must be N/A.
    expect(screen.getAllByText('N/A').length).toBe(6);
    // And "Unavailable" must never appear.
    expect(screen.queryByText(/Unavailable/i)).not.toBeInTheDocument();
  });

  it('renders the historical usage link', () => {
    render(<CloudUsageCenterPanel monthlyMetrics={baseMetrics} />);
    const link = screen.getByText('See historical usage data.');
    expect(link).toHaveAttribute('href', '#preview/admin/system/usage');
  });

  it('computes egress and storage independently without cross-tile contamination', () => {
    const metrics = {
      ...baseMetrics,
      history: {
        // Oldest-first ordering: last element is current month.
        // Egress: current=300, avg of prior [100, 200]=150, peak of prior=200
        egress: [{value: 100}, {value: 200}, {value: 300}],
        // Storage: current=700, avg of prior [400, 900]=650, peak of ALL=900 (NEXUS-46933)
        storage: [{value: 400}, {value: 900}, {value: 700}],
      },
    };
    render(<CloudUsageCenterPanel monthlyMetrics={metrics} />);

    // Verify egress values are correct
    expect(screen.getByText('300.00 Bytes')).toBeInTheDocument(); // egress current
    expect(screen.getByText('150.00 Bytes')).toBeInTheDocument(); // egress avg
    expect(screen.getByText('200.00 Bytes')).toBeInTheDocument(); // egress peak

    // Verify storage values are correct and didn't get egress data
    expect(screen.getByText('700.00 Bytes')).toBeInTheDocument(); // storage current
    expect(screen.getByText('650.00 Bytes')).toBeInTheDocument(); // storage avg
    expect(screen.getByText('900.00 Bytes')).toBeInTheDocument(); // storage peak (includes current)

    // Ensure no mixing - storage should NOT show 300, 150, or 200 (egress values)
    expect(screen.queryAllByText('300.00 Bytes').length).toBe(1); // only appears once for egress current
    expect(screen.queryAllByText('150.00 Bytes').length).toBe(1); // only appears once for egress avg
  });
});

describe('CloudUsageCenterPanel byte formatting (NEXUS-52116)', () => {
  const withHistory = (storageBytes, egressBytes) => ({
    peakStorage: storageBytes,
    responseSize: egressBytes,
    history: {
      storage: [{date: '2026-08-01', value: storageBytes}],
      egress:  [{date: '2026-08-01', value: egressBytes}],
      requests: [],
      components: [],
    },
    loading: false,
    error: null,
  });

  it('formats 500,000,000 bytes as "500.00 MB" (SI, matches Classic UI)', () => {
    render(<CloudUsageCenterPanel monthlyMetrics={withHistory(500_000_000, 500_000_000)} />);
    // Two tiles (Storage + Egress), each showing current = 500.00 MB.
    // avg and peak require >1 data point; with a single-month history they are 0 -> "N/A".
    const matches = screen.getAllByText('500.00 MB');
    expect(matches.length).toBeGreaterThanOrEqual(2);
  });

  it('formats 1,000,000,000 bytes as "1.00 GB" (SI, not 0.93 GB IEC)', () => {
    render(<CloudUsageCenterPanel monthlyMetrics={withHistory(1_000_000_000, 1_000_000_000)} />);
    const matches = screen.getAllByText('1.00 GB');
    expect(matches.length).toBeGreaterThanOrEqual(2);
  });

  it('renders "N/A" (not "0.00 Bytes") when history is empty', () => {
    // empty history => currentEgress/currentStorage default to 0 => wrapper returns "N/A"
    render(<CloudUsageCenterPanel monthlyMetrics={{
      peakStorage: null, responseSize: null,
      history: {storage: [], egress: [], requests: [], components: []},
      loading: false, error: null,
    }} />);
    // Expect NO occurrence of "0.00 Bytes" anywhere in the panel.
    expect(screen.queryByText(/0\.00 Bytes/)).toBeNull();
    // Expect at least one "N/A" for the current-month tile of each metric.
    const naMatches = screen.getAllByText('N/A');
    expect(naMatches.length).toBeGreaterThanOrEqual(2);
  });
});
