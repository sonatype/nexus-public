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
import { HardDrive, Package, FileBox } from 'lucide-react';

import { LoadingState, ErrorState } from '../../../../shared';
import HumanReadableUtils from '../../../../../../interface/HumanReadableUtils';
import { ExtJS } from '../../../../../../interface/ExtJS';
import UIStrings from '../../../../../../constants/UIStrings';
import { useRepositorySettingsUsageTab, type GroupMember } from './useRepositorySettingsUsageTab';
import type { RepositoryUsageKind } from './repositoryUsageMachine';
import { REPOSITORY_SETTINGS_USAGE_TAB_STRINGS as Strings } from './RepositorySettingsUsageTabStrings';
import { RepositoryTypeIcon } from './repositoryTypeIcon';

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
  items: Array<string | GroupMember>;
  emptyMessage: string;
  error: string | null;
  itemType: 'group' | 'member';
}

function MembershipSection({ title, items, emptyMessage, error, itemType }: MembershipSectionProps): JSX.Element {
  const showPlaceholder = error !== null || items.length === 0;
  const placeholderMessage = error ?? emptyMessage;

  function renderIcon(item: string | GroupMember): JSX.Element {
    if (itemType === 'group') {
      // Group Membership section - all items are groups
      return <RepositoryTypeIcon type="group" size={16} />;
    }

    // Member Repositories section - items are {name, type}
    const member = item as GroupMember;
    return <RepositoryTypeIcon type={member.type} size={16} />;
  }

  function getItemName(item: string | GroupMember): string {
    return typeof item === 'string' ? item : item.name;
  }

  function getRepositoryHref(name: string): string {
    return `#preview/admin/repository/repositories/${encodeURIComponent(name)}`;
  }

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
          {items.map((item) => (
            <a
              key={getItemName(item)}
              href={getRepositoryHref(getItemName(item))}
              className="repository-settings-usage-tab__membership-item"
            >
              <Flex align="center" gap="2" py="2" px="3">
                {renderIcon(item)}
                <Text weight="medium">{getItemName(item)}</Text>
              </Flex>
            </a>
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
    return <LoadingState message={Strings.LOADING} />;
  }

  if (error && !loaded) {
    return (
      <ErrorState
        message={error}
        retryText={Strings.RETRY}
        onRetry={retry}
      />
    );
  }

  const showGroupMembers = repositoryType === 'group';
  // Repository Size and Assets are populated by RepositoryMetricsFeatureCheck, which requires
  // Postgres (+ license + a finished CopyBlobSizeTask). On H2 those metrics never exist, so we
  // hide the cards instead of showing a permanently-pending "not yet calculated" tooltip that
  // reads as broken. Components is unaffected: the backend never populates componentCount on any
  // DB/edition yet, so it always shows its own pending tooltip regardless of this gate.
  const isPostgresql = Boolean(ExtJS.state().getValue('datastore.isPostgresql'));

  return (
    <Flex direction="column" gap="5" p="4">
      <Box>
        <Text size="3" weight="bold" as="div" mb="3">
          {Strings.STORAGE_METRICS.TITLE}
        </Text>
        <Grid columns={isPostgresql ? '3' : '1'} gap="4">
          {isPostgresql && (
            <Card size="2">
              <Flex direction="column" gap="2" align="center" py="2">
                <HardDrive size={22} color="var(--accent-9)" />
                <Tooltip content={totalSizePending ? Strings.STORAGE_METRICS.SIZE_PENDING_TOOLTIP : Strings.STORAGE_METRICS.SIZE_TOOLTIP}>
                  <Text size="5" weight="bold">
                    {formatBytes(metrics?.totalSize)}
                  </Text>
                </Tooltip>
                <Text size="1" color="gray">{Strings.STORAGE_METRICS.REPOSITORY_SIZE}</Text>
              </Flex>
            </Card>
          )}

          <Card size="2">
            <Flex direction="column" gap="2" align="center" py="2">
              <Package size={22} color="var(--accent-9)" />
              <Tooltip content={componentCountPending ? Strings.STORAGE_METRICS.COMPONENT_COUNT_PENDING_TOOLTIP : undefined}>
                <Text size="5" weight="bold">
                  {formatNumber(metrics?.componentCount)}
                </Text>
              </Tooltip>
              <Text size="1" color="gray">{Strings.STORAGE_METRICS.COMPONENTS}</Text>
            </Flex>
          </Card>

          {isPostgresql && (
            <Card size="2">
              <Flex direction="column" gap="2" align="center" py="2">
                <FileBox size={22} color="var(--accent-9)" />
                <Tooltip content={assetCountPending ? Strings.STORAGE_METRICS.ASSET_COUNT_PENDING_TOOLTIP : undefined}>
                  <Text size="5" weight="bold">
                    {formatNumber(metrics?.assetCount)}
                  </Text>
                </Tooltip>
                <Text size="1" color="gray">{Strings.STORAGE_METRICS.ASSETS}</Text>
              </Flex>
            </Card>
          )}
        </Grid>
      </Box>

      {showGroupMembers ? (
        <MembershipSection
          title={Strings.MEMBER_REPOSITORIES.TITLE}
          items={groupMembers}
          emptyMessage={Strings.MEMBER_REPOSITORIES.EMPTY_MESSAGE}
          error={membershipError}
          itemType="member"
        />
      ) : (
        <MembershipSection
          title={Strings.GROUP_MEMBERSHIP.TITLE}
          items={whereUsed}
          emptyMessage={Strings.GROUP_MEMBERSHIP.EMPTY_MESSAGE}
          error={membershipError}
          itemType="group"
        />
      )}
    </Flex>
  );
}

export default RepositorySettingsUsageTab;
