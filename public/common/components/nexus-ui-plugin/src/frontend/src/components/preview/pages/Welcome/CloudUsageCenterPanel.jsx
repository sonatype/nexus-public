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
import {Box, Card, Flex, Text, Heading} from '@radix-ui/themes';
import {LoadingState, ErrorState} from '../../shared';

function formatBytes(bytes) {
  if (bytes == null || bytes <= 0 || !Number.isFinite(bytes)) return '0.00 Bytes';
  const units = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB'];
  const i = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
  return `${(bytes / Math.pow(1024, i)).toFixed(2)} ${units[i]}`;
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

  const egressValues = egressHistory.map((p) => p.value).filter((v) => v > 0);
  const storageValues = storageHistory.map((p) => p.value).filter((v) => v > 0);

  const currentEgress = egressValues[0] ?? 0;
  const avgEgress = egressValues.length > 1
    ? egressValues.slice(1).reduce((a, b) => a + b, 0) / (egressValues.length - 1)
    : 0;
  const peakEgress = egressValues.length > 1
    ? egressValues.slice(1).reduce((max, v) => (v > max ? v : max), 0)
    : 0;

  const currentStorage = storageValues[0] ?? 0;
  const avgStorage = storageValues.length > 1
    ? storageValues.slice(1).reduce((a, b) => a + b, 0) / (storageValues.length - 1)
    : 0;
  const peakStorage = storageValues.reduce((max, v) => (v > max ? v : max), 0);

  return (
    <Flex direction="column" gap="5">
      <Box>
        <Heading as="h2" size="5" mb="1">Usage Center</Heading>
        <Text size="2" color="gray">
          Monitor this instance&#39;s usage to ensure your deployment is appropriate for your needs.
        </Text>
      </Box>

      <Box>
        <Heading as="h3" size="4" mb="3">Usage Metrics Overview</Heading>
        <Flex gap="4" wrap="wrap">
          <Card style={{flex: '1 1 280px'}}>
            <Box p="4">
              <Text size="3" weight="bold" mb="3" as="div">Egress</Text>
              <Flex direction="column" gap="2">
                <Flex justify="between">
                  <Text size="2" color="gray">Current Month</Text>
                  <Text size="2" weight="medium">{formatBytes(currentEgress)}</Text>
                </Flex>
                <Flex justify="between">
                  <Text size="2" color="gray">Monthly Average</Text>
                  <Text size="2" weight="medium">{formatBytes(avgEgress)}</Text>
                </Flex>
                <Flex justify="between">
                  <Text size="2" color="gray">Peak Egress</Text>
                  <Text size="2" weight="medium">{formatBytes(peakEgress)}</Text>
                </Flex>
              </Flex>
            </Box>
          </Card>

          <Card style={{flex: '1 1 280px'}}>
            <Box p="4">
              <Text size="3" weight="bold" mb="3" as="div">Storage</Text>
              <Flex direction="column" gap="2">
                <Flex justify="between">
                  <Text size="2" color="gray">Current Month</Text>
                  <Text size="2" weight="medium">{formatBytes(currentStorage)}</Text>
                </Flex>
                <Flex justify="between">
                  <Text size="2" color="gray">Monthly Average</Text>
                  <Text size="2" weight="medium">{formatBytes(avgStorage)}</Text>
                </Flex>
                <Flex justify="between">
                  <Text size="2" color="gray">Peak Storage</Text>
                  <Text size="2" weight="medium">{formatBytes(peakStorage)}</Text>
                </Flex>
              </Flex>
            </Box>
          </Card>
        </Flex>
      </Box>

      <Box>
        <Heading as="h3" size="4" mb="1">Historical Usage</Heading>
        <Text size="2" color="gray">
          Monitor how your repository&apos;s usage has changed month by month.{' '}
          <a href="#preview/admin/system/usage">See historical usage data.</a>
        </Text>
      </Box>
    </Flex>
  );
}

export default CloudUsageCenterPanel;
