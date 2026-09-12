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
import { Box, Flex, Text, Card, Grid, Tooltip } from '@radix-ui/themes';
import { HardDrive, Package, FileBox, FolderTree } from 'lucide-react';

import { LoadingState, ErrorState } from '../../../../shared';
import HumanReadableUtils from '../../../../../../interface/HumanReadableUtils';
import UIStrings from '../../../../../../constants/UIStrings';
import { useRepositorySettingsUsageTab } from './useRepositorySettingsUsageTab';
import type { RepositoryUsageKind } from './repositoryUsageMachine';

import './RepositorySettingsUsageTab.scss';

export interface RepositorySettingsUsageTabProps {
  repositoryName: string;
  repositoryType: RepositoryUsageKind;
}

function formatBytes(bytes: number | undefined | null): string {
  if (bytes === undefined || bytes === null || Number.isNaN(bytes)) return UIStrings.UNAVAILABLE;
  return HumanReadableUtils.bytesToString(bytes);
}

function formatNumber(value: number | undefined | null): string {
  if (value === undefined || value === null || Number.isNaN(value)) return UIStrings.UNAVAILABLE;
  return value.toLocaleString();
}

interface MembershipSectionProps {
  title: string;
  items: string[];
  emptyMessage: string;
  error: string | null;
}

function MembershipSection({ title, items, emptyMessage, error }: MembershipSectionProps): JSX.Element {
  const showPlaceholder = error !== null || items.length === 0;
  const placeholderMessage = error ?? emptyMessage;

  return (
    <Box>
      <Text size="3" weight="bold" as="div" mb="3">
        {title}
      </Text>
      {showPlaceholder ? (
        <Flex
          direction="column"
          align="center"
          justify="center"
          p="6"
          className="repository-settings-usage-tab__membership-placeholder"
        >
          <Text color="gray" size="2">{placeholderMessage}</Text>
        </Flex>
      ) : (
        <Flex direction="column" gap="2">
          {items.map((name) => (
            <Flex
              key={name}
              align="center"
              gap="2"
              py="2"
              px="3"
              className="repository-settings-usage-tab__membership-item"
            >
              <FolderTree size={16} color="var(--blue-9)" />
              <Text weight="medium">{name}</Text>
            </Flex>
          ))}
        </Flex>
      )}
    </Box>
  );
}

export function RepositorySettingsUsageTab({
  repositoryName,
  repositoryType,
}: RepositorySettingsUsageTabProps): JSX.Element {
  const {
    metrics,
    groupMembers,
    whereUsed,
    loading,
    loaded,
    error,
    membershipError,
    componentCountPending,
    assetCountPending,
    totalSizePending,
    retry,
  } = useRepositorySettingsUsageTab(repositoryName, repositoryType);

  if (loading && !loaded) {
    return <LoadingState message="Loading usage data..." />;
  }

  if (error && !loaded) {
    return (
      <ErrorState
        message={error}
        retryText="Retry"
        onRetry={retry}
      />
    );
  }

  const showGroupMembers = repositoryType === 'group';

  return (
    <Flex direction="column" gap="5" p="4">
      <Box>
        <Text size="3" weight="bold" as="div" mb="3">
          Storage Metrics
        </Text>
        <Grid columns="3" gap="4">
          <Card size="2">
            <Flex direction="column" gap="2" align="center" py="2">
              <HardDrive size={22} color="var(--accent-9)" />
              <Tooltip content={totalSizePending ? "Repository size not yet calculated. The repository metrics task runs periodically." : "Total size of all assets in this repository"}>
                <Text size="5" weight="bold">
                  {formatBytes(metrics?.totalSize)}
                </Text>
              </Tooltip>
              <Text size="1" color="gray">Repository Size</Text>
            </Flex>
          </Card>

          <Card size="2">
            <Flex direction="column" gap="2" align="center" py="2">
              <Package size={22} color="var(--accent-9)" />
              <Tooltip content={componentCountPending ? "Component count not yet calculated. The repository metrics task runs periodically." : undefined}>
                <Text size="5" weight="bold">
                  {formatNumber(metrics?.componentCount)}
                </Text>
              </Tooltip>
              <Text size="1" color="gray">Components</Text>
            </Flex>
          </Card>

          <Card size="2">
            <Flex direction="column" gap="2" align="center" py="2">
              <FileBox size={22} color="var(--accent-9)" />
              <Tooltip content={assetCountPending ? "Asset count not yet calculated. The repository metrics task runs periodically." : undefined}>
                <Text size="5" weight="bold">
                  {formatNumber(metrics?.assetCount)}
                </Text>
              </Tooltip>
              <Text size="1" color="gray">Assets</Text>
            </Flex>
          </Card>
        </Grid>
      </Box>

      {showGroupMembers ? (
        <MembershipSection
          title="Member Repositories"
          items={groupMembers}
          emptyMessage="This group has no member repositories."
          error={membershipError}
        />
      ) : (
        <MembershipSection
          title="Group Membership"
          items={whereUsed}
          emptyMessage="This repository is not a member of any groups."
          error={membershipError}
        />
      )}
    </Flex>
  );
}

export default RepositorySettingsUsageTab;
