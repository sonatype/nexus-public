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
import { Box, Flex, Text, Skeleton } from '@radix-ui/themes';
import { HardDrive, AlertCircle, CheckCircle } from 'lucide-react';
import { useBlobStoreInfo } from '../../../../shared';
import './BlobStoreStorageCard.scss';

interface BlobStoreStorageCardProps {
  blobStoreName: string;
}

function formatBytes(bytes: number | undefined): string {
  if (bytes === undefined) return '—';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  let unitIndex = 0;
  let size = bytes;
  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024;
    unitIndex++;
  }
  return `${size.toFixed(1)} ${units[unitIndex]}`;
}

export function BlobStoreStorageCard({ blobStoreName }: BlobStoreStorageCardProps): JSX.Element | null {
  const { data, isLoading, error } = useBlobStoreInfo(blobStoreName);

  if (isLoading) {
    return (
      <Box className="blob-store-card">
        <Box className="blob-store-card__skeleton">
          <Skeleton height="20px" width="60%" />
          <Skeleton height="16px" width="40%" />
          <Skeleton height="16px" width="80%" />
          <Skeleton height="16px" width="50%" />
        </Box>
      </Box>
    );
  }

  if (error) {
    return (
      <Box className="blob-store-card">
        <Box className="blob-store-card__error">
          <Flex align="center" gap="2">
            <AlertCircle size={16} color="var(--red-11)" />
            <Text size="2" color="red">
              Failed to load blob store information
            </Text>
          </Flex>
        </Box>
      </Box>
    );
  }

  if (!data) {
    return null;
  }

  return (
    <Box className="blob-store-card">
      <Flex className="blob-store-card__header">
        <HardDrive size={18} />
        <Text weight="bold" size="3">
          Blob Store
        </Text>
      </Flex>

      <Box className="blob-store-card__grid">
        <Box className="blob-store-card__item">
          <Text className="blob-store-card__label">Name</Text>
          <Text className="blob-store-card__value">{data.name}</Text>
        </Box>

        <Box className="blob-store-card__item">
          <Text className="blob-store-card__label">Type</Text>
          <Text className="blob-store-card__value">{data.type}</Text>
        </Box>

        {data.totalSizeInBytes !== undefined && (
          <Box className="blob-store-card__item">
            <Text className="blob-store-card__label">Total Size</Text>
            <Text className="blob-store-card__value">{formatBytes(data.totalSizeInBytes)}</Text>
          </Box>
        )}

        {data.availableSpaceInBytes !== undefined && (
          <Box className="blob-store-card__item">
            <Text className="blob-store-card__label">Available Space</Text>
            <Text className="blob-store-card__value">{formatBytes(data.availableSpaceInBytes)}</Text>
          </Box>
        )}

        {data.blobCount !== undefined && (
          <Box className="blob-store-card__item">
            <Text className="blob-store-card__label">Blob Count</Text>
            <Text className="blob-store-card__value">{data.blobCount.toLocaleString()}</Text>
          </Box>
        )}

        <Box className="blob-store-card__item">
          <Text className="blob-store-card__label">Status</Text>
          <Flex className="blob-store-card__status">
            {data.unavailable ? (
              <>
                <AlertCircle size={14} color="var(--red-11)" />
                <Text size="2" color="red">
                  Unavailable
                </Text>
              </>
            ) : (
              <>
                <CheckCircle size={14} color="var(--green-11)" />
                <Text size="2" color="green">
                  Available
                </Text>
              </>
            )}
          </Flex>
        </Box>
      </Box>
    </Box>
  );
}
