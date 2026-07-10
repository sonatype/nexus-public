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

import React, { useState } from 'react';
import { Text, Heading, Box, IconButton, Card, Flex, Badge, Button, Progress } from '@radix-ui/themes';
import { HelpCircle, AlertCircle, AlertTriangle } from 'lucide-react';
import type { InstanceTotalsPanelProps } from './simplified.types';
import { MonthlyMetricChart } from './MonthlyMetricChart';
import { formatBytesToGB, type MonthlyMetricsHistory } from './useMonthlyMetrics';
import type { MetricType } from './metricMethodology';
import { MetricHelpModal } from './MetricHelpModal';

import { CE_WARN_THRESHOLD } from './ceThresholds';

import './InstanceTotalsPanel.scss';

function computePercentChange(chartData: { value: number }[]): number | null {
  if (!chartData || chartData.length < 2) return null;
  const prev = chartData[chartData.length - 2]?.value ?? 0;
  const curr = chartData[chartData.length - 1]?.value ?? 0;
  if (prev === 0) return curr > 0 ? 100 : null;
  return Math.round(((curr - prev) / prev) * 100);
}

function hasMetricValue(value: string | number): boolean {
  if (typeof value === 'number') return value > 0;
  const s = String(value).trim().toUpperCase();
  if (s === 'TBD' || s === '') return false;
  const n = parseFloat(s);
  return !Number.isNaN(n) && n > 0;
}

/** Returns the display value, stripping redundant unit from value when unit is provided separately. */
function formatDisplayValue(value: string | number, unit?: string): string {
  if (typeof value === 'number') return value.toLocaleString();
  const s = String(value).trim();
  if (!unit || !s.endsWith(` ${unit}`)) return s;
  return s.slice(0, -(unit.length + 1)).trim();
}

interface MetricCardWithChartProps {
  value: string | number;
  label: string;
  unit?: string;
  chartData: { date: string; value: number }[];
  formatChartValue?: (v: number) => string;
  variant?: 'area' | 'line';
  color?: string;
  metricType?: MetricType;
  onHelpClick?: () => void;
  ctaLabel?: string;
  ctaHref?: string;
  /** CE hard limit for this metric. When > 0, shows a usage progress bar with color coding. */
  limitValue?: number;
}

function MetricCardWithChart({
  value,
  label,
  unit,
  chartData,
  formatChartValue,
  variant = 'area',
  color,
  metricType,
  onHelpClick,
  ctaLabel,
  ctaHref,
  limitValue,
}: MetricCardWithChartProps) {
  const hasChartData = chartData && chartData.length >= 1;
  const percentChange = hasChartData ? computePercentChange(chartData) : null;

  const numericValue = typeof value === 'number' ? value : parseFloat(String(value)) || 0;
  const hasLimit = limitValue != null && limitValue > 0;
  const usageRatio = hasLimit ? numericValue / limitValue : 0;
  const isExceeding = hasLimit && usageRatio >= 1;
  const isApproaching = hasLimit && !isExceeding && usageRatio >= CE_WARN_THRESHOLD;
  const limitBarColor = isExceeding ? 'red' : isApproaching ? 'orange' : 'blue';

  return (
    <Card className="nxrm-metric-card nxrm-metric-card--usage" size="3">
      <Box className="rt-reset rt-BaseCard rt-Card rt-r-size-3 rt-variant-surface nxrm-metric-card__body">
      <Flex direction="column" gap="3" className="nxrm-metric-card__content">
        <Box className="nxrm-metric-card__header">
          <Flex justify="between" align="start">
            <Flex direction="column" gap="1">
              <Heading as="h3" size="2" color="gray" weight="medium" className="nxrm-metric-card__title">
                {label}
              </Heading>
              {hasMetricValue(value) && (
                <Flex align="baseline" gap="2" wrap="nowrap">
                  {isExceeding && <AlertCircle size={16} style={{color: 'var(--red-9)', flexShrink: 0}} aria-label="Limit exceeded" />}
                  {isApproaching && <AlertTriangle size={16} style={{color: 'var(--orange-9)', flexShrink: 0}} aria-label="Approaching limit" />}
                  <Text size="6" weight="bold" className="nxrm-metric-card__value">
                    {formatDisplayValue(value, unit)}
                  </Text>
                  {unit && (
                    <Text size="2" color="gray" className="nxrm-metric-card__unit">
                      {unit}
                    </Text>
                  )}
                </Flex>
              )}
            </Flex>
            <Flex align="start" gap="2">
              {metricType != null && onHelpClick && (
                <IconButton
                  size="1"
                  variant="ghost"
                  color="gray"
                  onClick={onHelpClick}
                  aria-label={`How is ${label} calculated?`}
                  className="nxrm-metric-card__help"
                >
                  <HelpCircle size={16} />
                </IconButton>
              )}
              {percentChange != null && (
                <Badge
                  color={percentChange >= 0 ? 'green' : 'red'}
                  variant="solid"
                  size="1"
                  className="nxrm-metric-card__change"
                >
                  {percentChange >= 0 ? '+' : ''}{percentChange}%
                </Badge>
              )}
            </Flex>
          </Flex>
        </Box>

        {hasLimit && (
          <Box className="nxrm-metric-card__limit">
            <Progress
              value={Math.min(usageRatio * 100, 100)}
              max={100}
              color={limitBarColor}
              size="1"
            />
            <Flex justify="between" mt="1">
              <Text size="1" color="gray">
                {numericValue.toLocaleString()} of {(limitValue ?? 0).toLocaleString()}
              </Text>
              <Text size="1" color="gray">
                limit
              </Text>
            </Flex>
          </Box>
        )}

        {hasChartData ? (
          <div className="nxrm-metric-card__chart">
            <MonthlyMetricChart
              data={chartData}
              formatValue={formatChartValue}
              height={100}
              variant={variant}
              color={color}
            />
          </div>
        ) : (
          <Flex direction="column" gap="3" className="nxrm-metric-card__no-data">
            {!hasMetricValue(value) && (
              <Text size="2" color="gray" className="nxrm-metric-card__no-activity">
                No activity
              </Text>
            )}
            {ctaLabel && (
              <Button
                size="1"
                variant="outline"
                color="blue"
                onClick={
                  ctaHref
                    ? () => {
                        window.location.hash = ctaHref;
                      }
                    : undefined
                }
              >
                {ctaLabel}
              </Button>
            )}
          </Flex>
        )}
      </Flex>
      </Box>
    </Card>
  );
}

export interface InstanceTotalsPanelWithSparklineProps extends InstanceTotalsPanelProps {
  /** 12-month history for line charts. From useMonthlyMetrics().history */
  monthlyMetricsHistory?: MonthlyMetricsHistory;
  /** Storage (GB) and Egress (GB) - only include when we have real values; Egress may be "TBD" for new instances */
  monthlyMetrics?: {
    peakStorageGB?: string;
    responseSizeGB?: string;
    isEgressTbd?: boolean;
  };
}

/**
 * InstanceTotalsPanel displays global usage metrics in cards matching OutreachActions style.
 *
 * Data comes from the contentUsageEvaluationResult via useInstanceTotals hook.
 * Storage and Egress from monthly-metrics API via useMonthlyMetrics hook (optional).
 * Optional sparkline data comes from useUsageHistory hook.
 */
export function InstanceTotalsPanel({
  data,
  loading,
  monthlyMetricsHistory,
  monthlyMetrics,
}: InstanceTotalsPanelWithSparklineProps) {
  const [helpModalMetric, setHelpModalMetric] = useState<MetricType | null>(null);

  // Don't render while loading
  if (loading) {
    return null;
  }

  // Don't render if no data available
  if (!data) {
    return null;
  }

  const history = monthlyMetricsHistory;

  const openHelp = (type: MetricType) => () => setHelpModalMetric(type);
  const closeHelp = () => setHelpModalMetric(null);

  return (
    <Box className="nxrm-instance-totals">
      <Heading as="h2" size="4" weight="bold" className="nxrm-instance-totals__heading">
        Usage Metrics
      </Heading>
      <div className="nxrm-metrics-grid">
        {monthlyMetrics?.peakStorageGB != null && history && (
          <MetricCardWithChart
            value={monthlyMetrics.peakStorageGB}
            label="Storage"
            unit="GB"
            chartData={history.storage}
            formatChartValue={(v) => formatBytesToGB(v)}
            variant="area"
            metricType="storage"
            onHelpClick={openHelp('storage')}
          />
        )}
        {monthlyMetrics?.responseSizeGB != null && history && (
          <MetricCardWithChart
            value={monthlyMetrics.responseSizeGB}
            label="Egress"
            unit="GB"
            chartData={history.egress}
            formatChartValue={(v) => formatBytesToGB(v)}
            variant="area"
            metricType="egress"
            onHelpClick={openHelp('egress')}
          />
        )}
        <MetricCardWithChart
          value={data.totalComponents}
          label="Total Components"
          chartData={history?.components ?? []}
          variant="area"
          metricType="totalComponents"
          onHelpClick={openHelp('totalComponents')}
          limitValue={data.totalComponentsLimit || undefined}
        />
        <MetricCardWithChart
          value={data.peakRequestsPerDay}
          label="Peak Requests/Day"
          chartData={history?.requests ?? []}
          variant="area"
          metricType="peakRequestsPerDay"
          onHelpClick={openHelp('peakRequestsPerDay')}
          limitValue={data.peakRequestsPerDayLimit || undefined}
        />
        <MetricCardWithChart
          value={data.peakRequestsPerMonth}
          label="Peak Requests/Month"
          chartData={history?.requests ?? []}
          variant="area"
          metricType="peakRequestsPerMonth"
          onHelpClick={openHelp('peakRequestsPerMonth')}
        />
      </div>
      <MetricHelpModal
        metricType={helpModalMetric}
        isOpen={helpModalMetric != null}
        onClose={closeHelp}
        isEgressTbd={monthlyMetrics?.isEgressTbd}
      />
    </Box>
  );
}

export default InstanceTotalsPanel;
