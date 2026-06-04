/*
 * Copyright (c) 2008-present Sonatype, Inc.
 *
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/pro/attributions
 * Sonatype and Sonatype Nexus are trademarks of Sonatype, Inc. Apache Maven is a trademark of the Apache Foundation.
 * M2Eclipse is a trademark of the Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
import React, { useMemo } from 'react';
import type { DataPoint } from './useUsageHistory';

import './Sparkline.scss';

export interface SparklineProps {
  data: DataPoint[];
  width?: number;
  height?: number;
  color?: string;
  showTrend?: boolean;
}

/**
 * A simple SVG sparkline component for displaying trend data.
 * 
 * Shows a line chart with optional trend indicator (up/down arrow).
 */
export function Sparkline({ 
  data, 
  width = 80, 
  height = 24,
  color = '#3b82f6',
  showTrend = true
}: SparklineProps): React.ReactElement | null {
  const { path, trend, trendPercent } = useMemo(() => {
    if (!data || data.length < 2) {
      return { path: '', trend: 'neutral' as const, trendPercent: 0 };
    }

    const values = data.map(d => d.value);
    const minValue = Math.min(...values);
    const maxValue = Math.max(...values);
    const range = maxValue - minValue || 1;

    // Calculate padding to keep line away from edges
    const padding = 2;
    const chartWidth = width - padding * 2;
    const chartHeight = height - padding * 2;

    // Generate SVG path
    const points = values.map((value, index) => {
      const x = padding + (index / (values.length - 1)) * chartWidth;
      const y = padding + chartHeight - ((value - minValue) / range) * chartHeight;
      return `${x},${y}`;
    });

    const pathD = `M ${points.join(' L ')}`;

    // Calculate trend
    const firstValue = values[0];
    const lastValue = values[values.length - 1];
    const percentChange = firstValue > 0 
      ? ((lastValue - firstValue) / firstValue) * 100 
      : 0;

    let trendDir: 'up' | 'down' | 'neutral' = 'neutral';
    if (percentChange > 1) trendDir = 'up';
    else if (percentChange < -1) trendDir = 'down';

    return { 
      path: pathD, 
      trend: trendDir, 
      trendPercent: Math.round(Math.abs(percentChange)) 
    };
  }, [data, width, height]);

  if (!data || data.length < 2) {
    return (
      <div className="sparkline sparkline--empty">
        <span className="sparkline__no-data">—</span>
      </div>
    );
  }

  const trendColor = trend === 'up' ? '#22c55e' : trend === 'down' ? '#ef4444' : '#94a3b8';

  return (
    <div className="sparkline">
      <svg 
        width={width} 
        height={height} 
        className="sparkline__chart"
        viewBox={`0 0 ${width} ${height}`}
      >
        <path
          d={path}
          fill="none"
          stroke={color}
          strokeWidth={1.5}
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
      {showTrend && trend !== 'neutral' && (
        <span className={`sparkline__trend sparkline__trend--${trend}`} style={{ color: trendColor }}>
          {trend === 'up' ? '↑' : '↓'}
          {trendPercent > 0 && <span className="sparkline__percent">{trendPercent}%</span>}
        </span>
      )}
    </div>
  );
}

export default Sparkline;

