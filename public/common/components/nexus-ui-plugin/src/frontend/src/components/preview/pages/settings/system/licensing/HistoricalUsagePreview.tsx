/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven a trademark of the Apache Software Foundation. M2Eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

import React, { useState, useEffect, useCallback, useRef } from 'react';
import {
  Box,
  Button,
  Flex,
  Heading,
  Spinner,
  Table,
  Text,
  Tooltip,
} from '@radix-ui/themes';
import { ExternalLink, HelpCircle, TrendingUp, TrendingDown, Minus } from 'lucide-react';
import { restClient, parseApiError } from '../../../../../../interface/api';
import HumanReadableUtils from '../../../../../../interface/HumanReadableUtils';

import './HistoricalUsagePreview.scss';

interface MonthlyMetric {
  metricDate: string;
  componentCount: number | string;
  percentageChangeComponent: number | string | null;
  requestCount: number | string;
  percentageChangeRequest: number | string | null;
  responseSize: number | string | null;
  egress: number | string | null;
  // API field present in Heritage UI table; not displayed in the current Preview UI column set
  percentageChangeEgress: number | string | null;
  peakStorage: number | string | null;
  storage: number | string | null;
  // API field present in Heritage UI table; not displayed in the current Preview UI column set
  percentageChangeStorage: number | string | null;
}

const STRINGS = {
  title: 'Historical Usage',
  description: 'Monitor your storage usage trends over time.',
  learnMore: 'Learn how usage is calculated',
  learnMoreUrl: 'http://links.sonatype.com/products/nxrm3/license/historical-usage',
  loading: 'Loading usage data...',
  empty: 'No historical usage data available',
  retry: 'Retry',
  columns: {
    month: 'Month',
    peakComponents: 'Peak Components',
    componentsChange: 'Components % Change',
    totalRequests: 'Total Requests',
    requestsChange: 'Requests % Change',
    totalEgress: 'Total Egress',
    peakStorage: 'Peak Storage',
  },
  tooltips: {
    componentsChange: 'Change rate of the peak component count from the previous month.',
    requestsChange: 'Change rate of the total monthly requests from the previous month.',
    totalEgress: 'Egress is based on application-level tracking and may differ from actual network transfer measured by your cloud provider.',
    peakStorage: 'Maximum storage consumed at any point during the month.',
  },
};

function formatMonth(dateStr: string): string {
  try {
    return new Intl.DateTimeFormat('en-US', {
      year: 'numeric',
      month: 'short',
      timeZone: 'UTC',
    }).format(new Date(dateStr));
  } catch {
    return '-';
  }
}

function formatBytes(value: number | string | null | undefined): string {
  if (value === 'N/A' || value === null || value === undefined) {
    return 'N/A';
  }
  if (typeof value !== 'number') {
    return 'N/A';
  }
  return HumanReadableUtils.bytesToString(value);
}

function formatNumber(value: number | string | null | undefined): string {
  if (value === 'N/A' || value === null || value === undefined) {
    return 'N/A';
  }
  if (typeof value !== 'number') {
    return 'N/A';
  }
  return value.toLocaleString();
}

function formatPercentage(value: number | string | null): string {
  if (value === 'N/A' || value === null || value === undefined) {
    return 'N/A';
  }
  if (typeof value !== 'number') {
    return 'N/A';
  }
  return `${Math.abs(value)}%`;
}

interface ChangeIconProps {
  value: number | string | null;
}

function ChangeIcon({ value }: ChangeIconProps) {
  if (value === 'N/A' || value === null || value === undefined || typeof value !== 'number') {
    return <Minus size={14} className="change-icon change-icon--na" />;
  }

  if (value > 0) {
    return <TrendingUp size={14} className="change-icon change-icon--up" />;
  }
  if (value < 0) {
    return <TrendingDown size={14} className="change-icon change-icon--down" />;
  }
  return <Minus size={14} className="change-icon change-icon--na" />;
}

interface ColumnHeaderProps {
  label: string;
  tooltip?: string;
  align?: 'left' | 'right';
}

function ColumnHeader({ label, tooltip, align = 'left' }: ColumnHeaderProps) {
  const justify = align === 'right' ? 'end' : 'start';
  return (
    <Table.ColumnHeaderCell justify={justify}>
      <Flex align="center" gap="1" justify={justify}>
        <Text size="2" weight="medium">{label}</Text>
        {tooltip && (
          <Tooltip content={tooltip}>
            <HelpCircle size={12} className="column-tooltip" />
          </Tooltip>
        )}
      </Flex>
    </Table.ColumnHeaderCell>
  );
}

export function HistoricalUsagePreview(): JSX.Element {
  const [data, setData] = useState<MonthlyMetric[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const isMountedRef = useRef(true);

  useEffect(() => {
    isMountedRef.current = true;
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await restClient.get<MonthlyMetric[]>('service/rest/v1/monthly-metrics');
      if (isMountedRef.current) {
        setData(response || []);
      }
    } catch (err) {
      if (isMountedRef.current) {
        const apiError = parseApiError(err);
        setError(apiError.message);
      }
    } finally {
      if (isMountedRef.current) {
        setLoading(false);
      }
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  if (loading) {
    return (
      <Flex direction="column" align="center" justify="center" gap="3" p="9">
        <Spinner size="3" />
        <Text color="gray">{STRINGS.loading}</Text>
      </Flex>
    );
  }

  if (error) {
    return (
      <Flex direction="column" align="start" gap="3" p="4">
        <Text color="red">{error}</Text>
        <Button variant="soft" size="2" onClick={fetchData}>{STRINGS.retry}</Button>
      </Flex>
    );
  }

  return (
    <Box className="historical-usage-preview">
      <Heading as="h3" size="4" mb="2">
        {STRINGS.title}
      </Heading>
      <Text size="2" mb="4" className="historical-usage-preview__description">
        {STRINGS.description}{' '}
        <a
          href={STRINGS.learnMoreUrl}
          target="_blank"
          rel="noopener noreferrer"
          className="historical-usage-preview__learn-more"
        >
          {STRINGS.learnMore}
          <ExternalLink size={12} style={{ marginLeft: '4px', display: 'inline-block' }} />
        </a>
      </Text>

      <Box className="historical-usage-preview__table-scroll">
        <Table.Root size="2" variant="surface" className="historical-usage-preview__table">
          <Table.Header>
            <Table.Row>
              <ColumnHeader label={STRINGS.columns.month} />
              <ColumnHeader label={STRINGS.columns.peakComponents} align="right" />
              <ColumnHeader
                label={STRINGS.columns.componentsChange}
                tooltip={STRINGS.tooltips.componentsChange}
                align="right"
              />
              <ColumnHeader label={STRINGS.columns.totalRequests} align="right" />
              <ColumnHeader
                label={STRINGS.columns.requestsChange}
                tooltip={STRINGS.tooltips.requestsChange}
                align="right"
              />
              <ColumnHeader
                label={STRINGS.columns.totalEgress}
                tooltip={STRINGS.tooltips.totalEgress}
                align="right"
              />
              <ColumnHeader
                label={STRINGS.columns.peakStorage}
                tooltip={STRINGS.tooltips.peakStorage}
                align="right"
              />
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {data.length === 0 ? (
              <Table.Row>
                <Table.Cell colSpan={7}>
                  <Flex justify="center" p="6">
                    <Text color="gray" size="2">{STRINGS.empty}</Text>
                  </Flex>
                </Table.Cell>
              </Table.Row>
            ) : (
              data.map((item, index) => (
                <Table.Row key={item.metricDate || index}>
                  <Table.Cell>
                    <Text size="2">{formatMonth(item.metricDate)}</Text>
                  </Table.Cell>
                  <Table.Cell justify="end">
                    <Text size="2">{formatNumber(item.componentCount)}</Text>
                  </Table.Cell>
                  <Table.Cell justify="end">
                    <Flex align="center" gap="1" justify="end">
                      <ChangeIcon value={item.percentageChangeComponent} />
                      <Text size="2">{formatPercentage(item.percentageChangeComponent)}</Text>
                    </Flex>
                  </Table.Cell>
                  <Table.Cell justify="end">
                    <Text size="2">{formatNumber(item.requestCount)}</Text>
                  </Table.Cell>
                  <Table.Cell justify="end">
                    <Flex align="center" gap="1" justify="end">
                      <ChangeIcon value={item.percentageChangeRequest} />
                      <Text size="2">{formatPercentage(item.percentageChangeRequest)}</Text>
                    </Flex>
                  </Table.Cell>
                  <Table.Cell justify="end">
                    {/* Both fields represent total egress; the API sends exactly one. responseSize is the older name, egress the newer. */}
                    <Text size="2">{formatBytes(item.responseSize ?? item.egress)}</Text>
                  </Table.Cell>
                  <Table.Cell justify="end" className="historical-usage-preview__cell--last">
                    {/* Both fields represent peak storage; the API sends exactly one. peakStorage is the older name, storage the newer. */}
                    <Text size="2">{formatBytes(item.peakStorage ?? item.storage)}</Text>
                  </Table.Cell>
                </Table.Row>
              ))
            )}
          </Table.Body>
        </Table.Root>
      </Box>
    </Box>
  );
}

export default HistoricalUsagePreview;
