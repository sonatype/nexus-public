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
import HumanReadableUtils from '../../../../interface/HumanReadableUtils';

const {
  WELCOME: {
    USAGE: {
      MENU,
      CARDS: {CLOUD_TILE_LABELS},
    },
  },
} = UIStrings;

function formatBytes(value) {
  // Guard non-positive/non-numeric inputs uniformly so a negative current-month
  // value renders "N/A" instead of falling through to bytesToString, which
  // returns "Unavailable" for negatives (HumanReadableUtils.js:34) — two
  // different strings for effectively the same "no usable data" condition.
  if (typeof value !== 'number' || value <= 0) return 'N/A';
  return HumanReadableUtils.bytesToString(value);
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

  // useMonthlyMetrics returns history oldest-first, so the current month is the
  // last entry. Read current from that position on the raw ordered history, then
  // filter zeros only when aggregating prior months. Filtering before slicing
  // would let a zero in an earlier month shift what "current" points to.
  // Zeros are treated as "no data" for averaging to avoid API-gap noise.
  const currentEgress = egressHistory[egressHistory.length - 1]?.value ?? 0;
  const priorEgress = egressHistory.slice(0, -1).map((p) => p.value).filter((v) => v > 0);
  const avgEgress =
    priorEgress.length > 0 ? priorEgress.reduce((a, b) => a + b, 0) / priorEgress.length : 0;
  const peakEgress = priorEgress.length > 0 ? Math.max(...priorEgress) : 0;

  const currentStorage = storageHistory[storageHistory.length - 1]?.value ?? 0;
  const priorStorage = storageHistory.slice(0, -1).map((p) => p.value).filter((v) => v > 0);
  const avgStorage =
    priorStorage.length > 0 ? priorStorage.reduce((a, b) => a + b, 0) / priorStorage.length : 0;
  // Peak Storage includes the current month (NEXUS-46933); Peak Egress does not.
  const allStorage = storageHistory.map((p) => p.value).filter((v) => v > 0);
  const peakStorage = allStorage.length > 0 ? Math.max(...allStorage) : 0;

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
