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
import { Box, Flex, Table, Text, Tooltip } from '@radix-ui/themes';
import { HelpCircle, TrendingUp, TrendingDown } from 'lucide-react';
import HumanReadableUtils from '../../../../../../interface/HumanReadableUtils';
import { MonthlyMetric } from './types';
import { USAGE_STRINGS } from './usageStrings';

function formatMonth(dateStr: string): string {
  try {
    return new Intl.DateTimeFormat('en-US', { year: 'numeric', month: 'short', timeZone: 'UTC' })
      .format(new Date(dateStr));
  } catch {
    return '-';
  }
}

/**
 * Coerce a metric value to a finite number, tolerating numeric strings from the
 * API. Returns null for null/undefined, empty strings, and non-numeric values
 * (e.g. 'N/A'), which callers render as an empty cell.
 */
function toNumber(v: number | string | null | undefined): number | null {
  if (typeof v === 'number') return Number.isFinite(v) ? v : null;
  if (typeof v === 'string' && v.trim() !== '') {
    const n = Number(v);
    return Number.isFinite(n) ? n : null;
  }
  return null;
}

const ZERO_BYTES = '0.00 Bytes';

/** Byte cells show "0.00 Bytes" when there is no value (null / zero / non-numeric). */
function formatBytes(v: number | string | null | undefined): string {
  const n = toNumber(v);
  if (n === null || n <= 0) return ZERO_BYTES;
  return HumanReadableUtils.bytesToString(n);
}

function ChangeIcon({ value }: { value: number }) {
  if (value > 0) return <TrendingUp size={14} className="change-icon change-icon--up" />;
  if (value < 0) return <TrendingDown size={14} className="change-icon change-icon--down" />;
  return null;
}

/** Percentage-change cell: empty when null/non-numeric, else an icon + absolute %. */
function ChangeCell({ value }: { value: number | string | null }) {
  const n = toNumber(value);
  if (n === null) return null;
  return (
    <Flex align="center" gap="1" justify="end">
      <ChangeIcon value={n} />
      <Text size="2">{`${Math.abs(n)}%`}</Text>
    </Flex>
  );
}

function ColumnHeader({ label, tooltip, align = 'left' }: { label: string; tooltip?: string; align?: 'left' | 'right' }) {
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

function totalUsage(item: MonthlyMetric): string {
  const egress = toNumber(item.egress) ?? 0;
  const storage = toNumber(item.storage) ?? 0;
  return formatBytes(egress + storage);
}

export function UsageTable({ metrics }: { metrics: MonthlyMetric[] }): JSX.Element {
  const c = USAGE_STRINGS.columns;
  const t = USAGE_STRINGS.tooltips;
  return (
    <Box className="usage-table">
      <Box className="usage-table__scroll">
        <Table.Root size="2" variant="surface">
          <Table.Header>
            <Table.Row>
              <ColumnHeader label={c.month} />
              <ColumnHeader label={c.totalEgress} align="right" />
              <ColumnHeader label={c.egressChange} tooltip={t.egressChange} align="right" />
              <ColumnHeader label={c.peakStorage} align="right" />
              <ColumnHeader label={c.storageChange} tooltip={t.storageChange} align="right" />
              <ColumnHeader label={c.totalUsage} tooltip={t.totalUsage} align="right" />
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {metrics.length === 0 ? (
              <Table.Row>
                <Table.Cell colSpan={6}>
                  <Flex justify="center" p="6">
                    <Text color="gray" size="2">{USAGE_STRINGS.EMPTY}</Text>
                  </Flex>
                </Table.Cell>
              </Table.Row>
            ) : (
              metrics.map((item, index) => (
                <Table.Row key={item.metricDate || index}>
                  <Table.Cell>
                    <Text size="2">{formatMonth(item.metricDate)}</Text>
                  </Table.Cell>
                  <Table.Cell justify="end">
                    <Text size="2">{formatBytes(item.egress)}</Text>
                  </Table.Cell>
                  <Table.Cell justify="end">
                    <ChangeCell value={item.percentageChangeEgress} />
                  </Table.Cell>
                  <Table.Cell justify="end">
                    <Text size="2">{formatBytes(item.storage)}</Text>
                  </Table.Cell>
                  <Table.Cell justify="end">
                    <ChangeCell value={item.percentageChangeStorage} />
                  </Table.Cell>
                  <Table.Cell justify="end">
                    <Text size="2">{totalUsage(item)}</Text>
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

export default UsageTable;
