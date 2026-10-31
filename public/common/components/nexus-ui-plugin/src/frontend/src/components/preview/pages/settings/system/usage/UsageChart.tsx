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
import { Box, Button, Flex, Heading, Select, Spinner, Text } from '@radix-ui/themes';
import { BarChart, Bar, XAxis, YAxis, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import HumanReadableUtils from '../../../../../../interface/HumanReadableUtils';
import { formatAsShortDate, getMaxValue, getValueTicks } from '../../../../../pages/admin/Usage/UsageInsightsUtils';
import { ChartDataPoint, MonthOption } from './types';
import { USAGE_STRINGS } from './usageStrings';

const EGRESS_COLOR = 'var(--accent-9)';
const STORAGE_COLOR = 'var(--purple-9)';
const CHART_HEIGHT = 320;
// Number of Y-axis gridline ticks, matching the Classic UI Usage Insights chart.
const Y_AXIS_TICKS = 10;
// Horizontal breathing room so the bars are not flush against the chart border.
const CHART_SIDE_PADDING = '10%';

export interface UsageChartProps {
  data: ChartDataPoint[];
  monthOptions: MonthOption[];
  selectedMonth: MonthOption | null;
  onSelectMonth: (m: MonthOption) => void;
  loading: boolean;
  error: string | null;
  onRetry: () => void;
}

export function UsageChart({
  data, monthOptions, selectedMonth, onSelectMonth, loading, error, onRetry,
}: UsageChartProps): JSX.Element {
  const handleValueChange = (key: string) => {
    const month = monthOptions.find((m) => m.key === key);
    if (month) onSelectMonth(month);
  };
  // Match Classic's evenly-spaced Y scale: a "nice" max plus 10 gridline ticks.
  const maxValue = getMaxValue(data);
  const yTicks = getValueTicks(maxValue, Y_AXIS_TICKS);
  return (
    <Box className="usage-chart">
      <Flex justify="between" align="center" mb="3">
        <Heading as="h3" size="4">{USAGE_STRINGS.CHART_TITLE}</Heading>
        <Flex align="center" gap="2">
          <Text size="1" color="gray">{USAGE_STRINGS.CHART_MONTH_LABEL}</Text>
          <Select.Root value={selectedMonth?.key ?? ''} onValueChange={handleValueChange}>
            <Select.Trigger aria-label={USAGE_STRINGS.CHART_MONTH_LABEL} />
            <Select.Content>
              {monthOptions.map((m) => (
                <Select.Item key={m.key} value={m.key}>{m.label}</Select.Item>
              ))}
            </Select.Content>
          </Select.Root>
        </Flex>
      </Flex>

      {error ? (
        <Flex direction="column" align="start" gap="3" p="4">
          <Text color="red">{error}</Text>
          <Button variant="soft" size="2" onClick={onRetry}>{USAGE_STRINGS.RETRY}</Button>
        </Flex>
      ) : loading ? (
        <Flex align="center" justify="center" gap="3" p="6">
          <Spinner size="3" />
          <Text color="gray" size="2">Loading…</Text>
        </Flex>
      ) : data.length === 0 ? (
        <Flex justify="center" p="6"><Text color="gray" size="2">{USAGE_STRINGS.EMPTY}</Text></Flex>
      ) : (
        <Box style={{ width: '100%', height: CHART_HEIGHT, paddingLeft: CHART_SIDE_PADDING, paddingRight: CHART_SIDE_PADDING }}>
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={data}>
              <XAxis dataKey="metricDate" tickFormatter={(d: string) => formatAsShortDate(d)} />
              <YAxis
                domain={[0, maxValue]}
                ticks={yTicks}
                tickFormatter={(v: number) => HumanReadableUtils.bytesToString(v)}
                width={80}
              />
              <Tooltip formatter={(v: number) => HumanReadableUtils.bytesToString(v)} labelFormatter={(d: string) => formatAsShortDate(d)} />
              <Legend />
              <Bar dataKey="egress" name={USAGE_STRINGS.CHART_LEGEND_EGRESS} stackId="a" fill={EGRESS_COLOR} />
              <Bar dataKey="storage" name={USAGE_STRINGS.CHART_LEGEND_STORAGE} stackId="a" fill={STORAGE_COLOR} />
            </BarChart>
          </ResponsiveContainer>
        </Box>
      )}
    </Box>
  );
}

export default UsageChart;
