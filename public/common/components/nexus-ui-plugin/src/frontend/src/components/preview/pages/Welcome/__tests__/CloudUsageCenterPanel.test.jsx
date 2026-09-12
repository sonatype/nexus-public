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

  it('renders zero values as "0.00 Bytes" for empty history', () => {
    render(<CloudUsageCenterPanel monthlyMetrics={baseMetrics} />);
    const zeroValues = screen.getAllByText('0.00 Bytes');
    expect(zeroValues.length).toBeGreaterThanOrEqual(3);
  });

  it('computes current, average, and peak egress from history', () => {
    const metrics = {
      ...baseMetrics,
      history: {
        egress: [{value: 300}, {value: 200}, {value: 100}],
        storage: [],
      },
    };
    render(<CloudUsageCenterPanel monthlyMetrics={metrics} />);
    // current = egressValues[0] = 300
    expect(screen.getByText('300.00 Bytes')).toBeInTheDocument();
    // avg of [200, 100] = 150
    expect(screen.getByText('150.00 Bytes')).toBeInTheDocument();
    // peak of [200, 100] = 200
    expect(screen.getByText('200.00 Bytes')).toBeInTheDocument();
  });

  it('computes peak storage excluding current month (consistent with peakEgress)', () => {
    const metrics = {
      ...baseMetrics,
      history: {
        egress: [],
        // index 0 is current month (900) — should be excluded from peak
        storage: [{value: 900}, {value: 400}, {value: 600}],
      },
    };
    render(<CloudUsageCenterPanel monthlyMetrics={metrics} />);
    // current = 900
    expect(screen.getByText('900.00 Bytes')).toBeInTheDocument();
    // peak = max of past months [400, 600] = 600, NOT 900
    expect(screen.getByText('600.00 Bytes')).toBeInTheDocument();
  });

  it('renders "0.00 Bytes" for negative byte values (not "NaN undefined")', () => {
    const metrics = {
      ...baseMetrics,
      history: {
        egress: [{value: -100}],
        storage: [],
      },
    };
    render(<CloudUsageCenterPanel monthlyMetrics={metrics} />);
    // Negative values are filtered out by v > 0, so all should show 0.00 Bytes
    const outputs = screen.getAllByText('0.00 Bytes');
    expect(outputs.length).toBeGreaterThan(0);
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
        // Egress: current=300, avg of [200, 100]=150, peak=200
        egress: [{value: 300}, {value: 200}, {value: 100}],
        // Storage: current=900, avg of [400, 600]=500, peak=600
        storage: [{value: 900}, {value: 400}, {value: 600}],
      },
    };
    render(<CloudUsageCenterPanel monthlyMetrics={metrics} />);

    // Verify egress values are correct
    expect(screen.getByText('300.00 Bytes')).toBeInTheDocument(); // egress current
    expect(screen.getByText('150.00 Bytes')).toBeInTheDocument(); // egress avg
    expect(screen.getByText('200.00 Bytes')).toBeInTheDocument(); // egress peak

    // Verify storage values are correct and didn't get egress data
    expect(screen.getByText('900.00 Bytes')).toBeInTheDocument(); // storage current
    expect(screen.getByText('500.00 Bytes')).toBeInTheDocument(); // storage avg
    expect(screen.getByText('600.00 Bytes')).toBeInTheDocument(); // storage peak

    // Ensure no mixing - storage should NOT show 300, 150, or 200 (egress values)
    expect(screen.queryAllByText('300.00 Bytes').length).toBe(1); // only appears once for egress current
    expect(screen.queryAllByText('150.00 Bytes').length).toBe(1); // only appears once for egress avg
  });
});
