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
import { Box, Flex, Text, Badge } from '@radix-ui/themes';
import {
  Database,
  HardDrive,
  Package,
  FileBox,
  Globe,
  CheckCircle,
  XCircle,
} from 'lucide-react';
import type {
  RepositoryProfileData,
  BlobStoreInfo,
  RepositoryMetrics,
} from '../hooks/useRepositoryProfile';

// =============================================================================
// Types
// =============================================================================

interface OverviewTabProps {
  repository: RepositoryProfileData;
  blobStore: BlobStoreInfo | null;
  metrics: RepositoryMetrics | null;
}

// =============================================================================
// Helper Functions
// =============================================================================

function formatBytes(bytes: number | undefined): string {
  if (bytes === undefined || bytes === null) return '—';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  let unitIndex = 0;
  let size = bytes;
  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024;
    unitIndex++;
  }
  return `${size.toFixed(1)} ${units[unitIndex]}`;
}

// =============================================================================
// Component
// =============================================================================

/**
 * OverviewTab - Displays repository details and storage metrics
 */
export function OverviewTab({
  repository,
  blobStore,
  metrics,
}: OverviewTabProps): JSX.Element {
  const storage = repository.attributes?.storage;
  const maven = repository.attributes?.maven;
  const proxy = repository.attributes?.proxy;
  const isOnline = repository.status?.online ?? repository.online ?? true;

  return (
    <Box>
      <Box className="profile-section__grid">
        {/* Repository Details */}
        <Box className="profile-section__card">
          <Flex align="center" gap="2" mb="4">
            <Database size={18} />
            <Text weight="bold">Repository Details</Text>
          </Flex>

          <Box className="profile-section__row">
            <Text className="profile-section__label">Type</Text>
            <Badge color={repository.type === 'hosted' ? 'blue' : repository.type === 'proxy' ? 'purple' : 'orange'}>
              {repository.type.charAt(0).toUpperCase() + repository.type.slice(1)}
            </Badge>
          </Box>

          <Box className="profile-section__row">
            <Text className="profile-section__label">Format</Text>
            <Text className="profile-section__value">{repository.format}</Text>
          </Box>

          <Box className="profile-section__row">
            <Text className="profile-section__label">Status</Text>
            <Flex align="center" gap="1">
              {isOnline ? (
                <>
                  <CheckCircle size={14} color="var(--green-11)" />
                  <Text className="profile-section__value profile-section__value--success">Online</Text>
                </>
              ) : (
                <>
                  <XCircle size={14} color="var(--red-11)" />
                  <Text className="profile-section__value profile-section__value--error">Offline</Text>
                </>
              )}
            </Flex>
          </Box>

          <Box className="profile-section__row">
            <Text className="profile-section__label">URL</Text>
            <Text className="profile-section__value profile-section__value--code">
              {repository.url}
            </Text>
          </Box>

          {maven?.versionPolicy && (
            <Box className="profile-section__row">
              <Text className="profile-section__label">Version Policy</Text>
              <Text className="profile-section__value">{maven.versionPolicy}</Text>
            </Box>
          )}

          {maven?.layoutPolicy && (
            <Box className="profile-section__row">
              <Text className="profile-section__label">Layout Policy</Text>
              <Text className="profile-section__value">{maven.layoutPolicy}</Text>
            </Box>
          )}

          {proxy?.remoteUrl && (
            <Box className="profile-section__row">
              <Text className="profile-section__label">
                <Globe size={14} />
                Remote URL
              </Text>
              <Text className="profile-section__value profile-section__value--code">
                {proxy.remoteUrl}
              </Text>
            </Box>
          )}
        </Box>

        {/* Storage & Metrics */}
        <Box className="profile-section__card">
          <Flex align="center" gap="2" mb="4">
            <HardDrive size={18} />
            <Text weight="bold">Storage & Metrics</Text>
          </Flex>

          <Box className="profile-section__row">
            <Text className="profile-section__label">
              <Package size={14} />
              Components
            </Text>
            <Text className="profile-section__value">
              {metrics?.componentCount?.toLocaleString() ?? '—'}
            </Text>
          </Box>

          <Box className="profile-section__row">
            <Text className="profile-section__label">
              <FileBox size={14} />
              Assets
            </Text>
            <Text className="profile-section__value">
              {metrics?.assetCount?.toLocaleString() ?? '—'}
            </Text>
          </Box>

          <Box className="profile-section__row">
            <Text className="profile-section__label">Blob Store</Text>
            <Text className="profile-section__value">{storage?.blobStoreName ?? '—'}</Text>
          </Box>

          {blobStore && (
            <>
              <Box className="profile-section__row">
                <Text className="profile-section__label">Blob Store Type</Text>
                <Text className="profile-section__value">{blobStore.type}</Text>
              </Box>

              {blobStore.totalSizeInBytes !== undefined && (
                <Box className="profile-section__row">
                  <Text className="profile-section__label">Total Size</Text>
                  <Text className="profile-section__value">
                    {formatBytes(blobStore.totalSizeInBytes)}
                  </Text>
                </Box>
              )}

              {blobStore.availableSpaceInBytes !== undefined && (
                <Box className="profile-section__row">
                  <Text className="profile-section__label">Available Space</Text>
                  <Text className="profile-section__value">
                    {formatBytes(blobStore.availableSpaceInBytes)}
                  </Text>
                </Box>
              )}
            </>
          )}

          <Box className="profile-section__row">
            <Text className="profile-section__label">Strict Content Validation</Text>
            <Text className="profile-section__value">
              {storage?.strictContentTypeValidation ? 'Yes' : 'No'}
            </Text>
          </Box>

          {storage?.writePolicy && (
            <Box className="profile-section__row">
              <Text className="profile-section__label">Write Policy</Text>
              <Text className="profile-section__value">{storage.writePolicy}</Text>
            </Box>
          )}
        </Box>
      </Box>
    </Box>
  );
}

export default OverviewTab;


