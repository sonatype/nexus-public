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
import { Sparkline } from '../Sparkline';

describe('Sparkline', () => {
  const mockDataUpTrend = [
    { date: '2026-01-12', value: 100 },
    { date: '2026-01-13', value: 120 },
    { date: '2026-01-14', value: 150 },
    { date: '2026-01-15', value: 180 },
    { date: '2026-01-16', value: 200 },
  ];

  const mockDataDownTrend = [
    { date: '2026-01-12', value: 200 },
    { date: '2026-01-13', value: 180 },
    { date: '2026-01-14', value: 150 },
    { date: '2026-01-15', value: 120 },
    { date: '2026-01-16', value: 100 },
  ];

  const mockDataFlat = [
    { date: '2026-01-12', value: 100 },
    { date: '2026-01-13', value: 101 },
    { date: '2026-01-14', value: 100 },
    { date: '2026-01-15', value: 99 },
    { date: '2026-01-16', value: 100 },
  ];

  it('renders empty state when no data', () => {
    const { container } = render(<Sparkline data={[]} />);
    expect(container.querySelector('.sparkline--empty')).toBeInTheDocument();
    expect(container.querySelector('.sparkline__no-data')).toHaveTextContent('—');
  });

  it('renders empty state when only one data point', () => {
    const { container } = render(<Sparkline data={[{ date: '2026-01-12', value: 100 }]} />);
    expect(container.querySelector('.sparkline--empty')).toBeInTheDocument();
  });

  it('renders SVG chart with valid data', () => {
    const { container } = render(<Sparkline data={mockDataUpTrend} />);
    const svg = container.querySelector('svg.sparkline__chart');
    expect(svg).toBeInTheDocument();
    expect(svg.querySelector('path')).toBeInTheDocument();
  });

  it('shows up trend indicator for increasing data', () => {
    const { container } = render(<Sparkline data={mockDataUpTrend} showTrend={true} />);
    const trend = container.querySelector('.sparkline__trend--up');
    expect(trend).toBeInTheDocument();
    expect(trend.textContent).toContain('↑');
  });

  it('shows down trend indicator for decreasing data', () => {
    const { container } = render(<Sparkline data={mockDataDownTrend} showTrend={true} />);
    const trend = container.querySelector('.sparkline__trend--down');
    expect(trend).toBeInTheDocument();
    expect(trend.textContent).toContain('↓');
  });

  it('hides trend indicator for flat data', () => {
    const { container } = render(<Sparkline data={mockDataFlat} showTrend={true} />);
    expect(container.querySelector('.sparkline__trend')).not.toBeInTheDocument();
  });

  it('respects showTrend=false', () => {
    const { container } = render(<Sparkline data={mockDataUpTrend} showTrend={false} />);
    expect(container.querySelector('.sparkline__trend')).not.toBeInTheDocument();
  });

  it('applies custom dimensions', () => {
    const { container } = render(<Sparkline data={mockDataUpTrend} width={100} height={30} />);
    const svg = container.querySelector('svg');
    expect(svg).toHaveAttribute('width', '100');
    expect(svg).toHaveAttribute('height', '30');
  });

  it('applies custom color', () => {
    const { container } = render(<Sparkline data={mockDataUpTrend} color="#ff0000" />);
    const path = container.querySelector('path');
    expect(path).toHaveAttribute('stroke', '#ff0000');
  });
});

