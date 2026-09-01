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
import { Box, Button, Flex, Heading, Spinner, Text } from '@radix-ui/themes';
import { ExternalLink } from 'lucide-react';
import { SettingsAlert } from '../../../../shared/form';
import { PageHeader } from '../../../../shared';
import { useUsage } from './useUsage';
import { UsageTable } from './UsageTable';
import { UsageChart } from './UsageChart';
import { USAGE_STRINGS } from './usageStrings';

import './UsagePage.scss';

const navigateTo = (path: string) => {
  window.location.hash = path;
};

/**
 * Cloud Usage module. Mirrors the Classic UI Cloud Usage screen (#admin/usage):
 * a historical table of egress/storage/total-usage plus a daily Usage Insights
 * chart, an update-frequency notice, and a dismissible storage-calculation note.
 */
export default function UsagePage(): JSX.Element {
  const {
    loading,
    error,
    isPermissionError,
    metrics,
    retry,
    storageNoteVisible,
    dismissStorageNote,
    chartData,
    chartLoading,
    chartError,
    monthOptions,
    selectedMonth,
    selectMonth,
    retryChart,
  } = useUsage();

  return (
    <Box className="usage-page" p="5">
      <Box mb="4">
        <PageHeader
          title={USAGE_STRINGS.TITLE}
          description={USAGE_STRINGS.MENU_DESCRIPTION}
          breadcrumbs={[
            { label: 'Settings', onClick: () => navigateTo('#preview/admin/settings') },
            { label: USAGE_STRINGS.TITLE },
          ]}
        />
      </Box>

      <Box className="usage-page__section">
        <Heading as="h2" size="4" mb="3">{USAGE_STRINGS.HISTORICAL_TITLE}</Heading>

        <Box mb="4">
          <SettingsAlert type="info">{USAGE_STRINGS.UPDATE_FREQUENCY}</SettingsAlert>
        </Box>

        {isPermissionError ? (
          <SettingsAlert type="warning">{error}</SettingsAlert>
        ) : error ? (
          <SettingsAlert type="error">
            <Flex align="center" gap="3" wrap="wrap">
              <Text as="span" size="2">{error}</Text>
              <Button size="1" variant="soft" onClick={retry}>{USAGE_STRINGS.RETRY}</Button>
            </Flex>
          </SettingsAlert>
        ) : loading ? (
          <Flex align="center" justify="center" gap="3" p="9">
            <Spinner size="3" />
            <Text color="gray">Loading…</Text>
          </Flex>
        ) : (
          <Flex direction="column" gap="4">
            <Text size="2" color="gray">
              {USAGE_STRINGS.DESCRIPTION}{' '}
              <a
                className="usage-page__learn-more"
                href={USAGE_STRINGS.LEARN_MORE_URL}
                target="_blank"
                rel="noopener noreferrer"
              >
                {USAGE_STRINGS.LEARN_MORE}
                <ExternalLink size={12} style={{ marginLeft: 4, display: 'inline-block' }} />
              </a>
            </Text>

            {storageNoteVisible && (
              <SettingsAlert type="info" onClose={dismissStorageNote}>
                <strong>{USAGE_STRINGS.STORAGE_NOTE_LABEL}</strong> {USAGE_STRINGS.STORAGE_NOTE}
              </SettingsAlert>
            )}

            <UsageChart
              data={chartData}
              monthOptions={monthOptions}
              selectedMonth={selectedMonth}
              onSelectMonth={selectMonth}
              loading={chartLoading}
              error={chartError}
              onRetry={retryChart}
            />

            <UsageTable metrics={metrics} />
          </Flex>
        )}
      </Box>
    </Box>
  );
}
