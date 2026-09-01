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
import {Box, Card, Flex, Grid, Heading, Text} from '@radix-ui/themes';
import {Info} from 'lucide-react';
import {LoadingState, ErrorState} from '../../shared';
import UIStrings from '../../../../constants/UIStrings';

const {
  WELCOME: {
    USAGE: {
      MENU,
      CARDS: {CLOUD_TILE_LABELS},
    },
  },
} = UIStrings;

function formatBytes(bytes) {
  if (bytes == null || bytes <= 0 || !Number.isFinite(bytes)) return '0.00 Bytes';
  const units = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB'];
  const i = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
  return `${(bytes / Math.pow(1024, i)).toFixed(2)} ${units[i]}`;
}

function CloudUsageTile({title, current, average, peak, tooltip}) {
  return (
    <Card style={{padding: 'var(--space-4)'}}>
      <Flex direction="column" gap="4">
        <Flex align="center" gap="2">
          <Heading size="4" weight="medium">{title}</Heading>
          <Box title={tooltip} style={{cursor: 'help'}}>
            <Info size={16} style={{color: 'var(--gray-9)'}} />
          </Box>
        </Flex>
        <Flex direction="column" gap="3">
          <Flex direction="column" gap="1">
            <Text size="5" weight="bold">{current}</Text>
            <Text size="2" color="gray">{CLOUD_TILE_LABELS.CURRENT_MONTH}</Text>
          </Flex>
          <Box style={{height: '1px', background: 'var(--gray-5)'}} />
          <Flex direction="column" gap="1">
            <Text size="5" weight="bold">{average}</Text>
            <Text size="2" color="gray">{CLOUD_TILE_LABELS.MONTHLY_AVERAGE}</Text>
          </Flex>
          <Box style={{height: '1px', background: 'var(--gray-5)'}} />
          <Flex direction="column" gap="1">
            <Text size="5" weight="bold">{peak}</Text>
            <Text size="2" color="gray">{CLOUD_TILE_LABELS.PEAK(title)}</Text>
          </Flex>
        </Flex>
      </Flex>
    </Card>
  );
}

export function CloudUsageCenterPanel({monthlyMetrics}) {
  if (monthlyMetrics.loading) {
    return <LoadingState message="Loading usage metrics..." />;
  }
  if (monthlyMetrics.error) {
    return <ErrorState title="Usage Metrics Error" message="Unable to load usage metrics. Please try again later." />;
  }

  const egressHistory = monthlyMetrics.history?.egress ?? [];
  const storageHistory = monthlyMetrics.history?.storage ?? [];

  // Filter out zero values: treat 0 as "no data" rather than a legitimate measurement.
  // This prevents newly created instances with zero traffic from skewing averages,
  // and aligns with API behavior where missing months may report as 0 or null.
  // Note: This means a month with legitimately zero egress is excluded, but the
  // trade-off is acceptable to avoid noise from API gaps.
  const egressValues = egressHistory.map((p) => p.value).filter((v) => v > 0);
  const storageValues = storageHistory.map((p) => p.value).filter((v) => v > 0);

  const currentEgress = egressValues[0] ?? 0;
  const avgEgress =
    egressValues.length > 1
      ? egressValues.slice(1).reduce((a, b) => a + b, 0) / (egressValues.length - 1)
      : 0;
  const peakEgress =
    egressValues.length > 1 ? egressValues.slice(1).reduce((max, v) => (v > max ? v : max), 0) : 0;

  const currentStorage = storageValues[0] ?? 0;
  const avgStorage =
    storageValues.length > 1
      ? storageValues.slice(1).reduce((a, b) => a + b, 0) / (storageValues.length - 1)
      : 0;
  const peakStorage =
    storageValues.length > 1 ? storageValues.slice(1).reduce((max, v) => (v > max ? v : max), 0) : 0;

  return (
    <Card size="3" style={{padding: 'var(--space-5)'}}>
      <Flex direction="column" gap="1" mb="4">
        <Heading size="5">{MENU.TITLE}</Heading>
        <Text size="2" color="gray">{MENU.SUB_TEXT}</Text>
        <Text size="2" weight="medium" mt="3">{MENU.SUB_TITLE}</Text>
      </Flex>

      <Grid columns={{initial: '1', md: '2'}} gap="4" style={{width: '100%'}}>
        <CloudUsageTile
          title={CLOUD_TILE_LABELS.EGRESS}
          current={formatBytes(currentEgress)}
          average={formatBytes(avgEgress)}
          peak={formatBytes(peakEgress)}
          tooltip={CLOUD_TILE_LABELS.EGRESS_TOOLTIP}
        />
        <CloudUsageTile
          title={CLOUD_TILE_LABELS.STORAGE}
          current={formatBytes(currentStorage)}
          average={formatBytes(avgStorage)}
          peak={formatBytes(peakStorage)}
          tooltip={CLOUD_TILE_LABELS.STORAGE_TOOLTIP}
        />
      </Grid>

      <Box mt="4">
        <Heading size="4" mb="1">{CLOUD_TILE_LABELS.HISTORICAL_USAGE_TITLE}</Heading>
        <Text size="2" color="gray">
          {CLOUD_TILE_LABELS.HISTORICAL_USAGE_TEXT}{' '}
          <a href="#preview/admin/system/usage">{CLOUD_TILE_LABELS.HISTORICAL_USAGE_LINK}</a>
        </Text>
      </Box>
    </Card>
  );
}

export default CloudUsageCenterPanel;
