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

import React, { useId, useMemo } from 'react';
import {
  ResponsiveContainer,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  Tooltip,
} from 'recharts';
import type { MonthlyDataPoint } from './useMonthlyMetrics';

import './MonthlyMetricChart.scss';

export interface MonthlyMetricChartProps {
  data: MonthlyDataPoint[];
  /** Format Y-axis ticks (e.g. bytes to GB, or number locale) */
  formatValue?: (value: number) => string;
  color?: string;
  /** Chart height in pixels or CSS value (e.g. "100%" to fill parent) */
  height?: number | string;
  /** @deprecated All charts use area with gradient. Kept for API compatibility. */
  variant?: 'area' | 'line';
}

/** All charts use Radix blue for consistency with design */
const CHART_BLUE = 'var(--accent-9)';

const DEFAULT_COLORS = {
  storage: CHART_BLUE,
  egress: CHART_BLUE,
  components: CHART_BLUE,
  requests: CHART_BLUE,
  requestsMonth: CHART_BLUE,
};

function formatMonthLabel(dateStr: string): string {
  if (!dateStr) return '';
  const [y, m] = dateStr.split('-');
  const monthNames = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
  const monthIdx = parseInt(m || '0', 10) - 1;
  return monthIdx >= 0 ? `${monthNames[monthIdx]} ${y}` : dateStr;
}

/**
 * Gorgeous Recharts-based metric chart. Area variant for Storage/Egress, Line for trends.
 * Interactive tooltips, smooth curves, gradient fills.
 */
export function MonthlyMetricChart({
  data,
  formatValue = (v) => v.toLocaleString(),
  color = CHART_BLUE,
  height = 120,
  variant = 'line',
}: MonthlyMetricChartProps): React.ReactElement | null {
  const gradientId = useId().replace(/:/g, '');
  const chartData = useMemo(() => {
    if (!data || data.length < 1) return [];
    const mapped = data.map((p) => ({
      month: formatMonthLabel(p.date),
      shortMonth: formatMonthLabel(p.date).split(' ')[0] || '',
      value: p.value,
    }));
    // Trim trailing zero-value months (e.g. current incomplete month like March)
    while (mapped.length > 0 && mapped[mapped.length - 1].value === 0) {
      mapped.pop();
    }
    // Recharts AreaChart needs at least 2 points; duplicate single point for flat line
    if (mapped.length === 1) {
      mapped.push({ ...mapped[0] });
    }
    return mapped;
  }, [data]);

  const heightStyle =
    typeof height === 'number' ? { height: `${height}px` } : typeof height === 'string' ? { height } : undefined;

  if (!chartData.length) {
    return (
      <div className="nxrm-monthly-chart nxrm-monthly-chart--empty" style={heightStyle}>
        <span>No activity</span>
      </div>
    );
  }

  const CustomTooltip = ({ active, payload, label }: { active?: boolean; payload?: Array<{ value: number }>; label?: string }) => {
    if (!active || !payload?.length) return null;
    const val = payload[0]?.value;
    return (
      <div className="nxrm-monthly-chart__tooltip">
        <strong>{label}</strong>
        <span>{formatValue(val ?? 0)}</span>
      </div>
    );
  };

  const chartColor = color?.startsWith('var(') ? color : CHART_BLUE;

  return (
    <div className="nxrm-monthly-chart nxrm-monthly-chart--recharts" style={heightStyle}>
      <ResponsiveContainer width="100%" height="100%">
        <AreaChart data={chartData} margin={{ top: 0, right: 0, bottom: 0, left: 0 }}>
          <defs>
            <linearGradient id={`areaGradient-${gradientId}`} x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor={chartColor} stopOpacity={0.35} />
              <stop offset="100%" stopColor={chartColor} stopOpacity={0.02} />
            </linearGradient>
          </defs>
          <XAxis
            dataKey="shortMonth"
            axisLine={false}
            tickLine={false}
            tick={{ fill: 'var(--gray-11)', fontSize: 10 }}
            dy={8}
          />
          <YAxis
            dataKey="value"
            hide
            domain={[0, 'auto']}
          />
          <Tooltip content={<CustomTooltip />} cursor={{ stroke: chartColor, strokeWidth: 1, strokeDasharray: '4 4' }} />
          <Area
            type="monotone"
            dataKey="value"
            stroke={chartColor}
            strokeWidth={2.5}
            fill={`url(#areaGradient-${gradientId})`}
            dot={false}
            activeDot={{ r: 5, fill: chartColor, stroke: 'var(--color-panel)', strokeWidth: 2 }}
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
}

export { DEFAULT_COLORS };
export default MonthlyMetricChart;
