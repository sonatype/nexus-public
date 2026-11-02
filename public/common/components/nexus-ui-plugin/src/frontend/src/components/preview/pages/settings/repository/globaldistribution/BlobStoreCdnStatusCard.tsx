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
import {
  Badge,
  Box,
  Button,
  Card,
  Flex,
  Heading,
  Inset,
  Link,
  Table,
  Text,
} from '@radix-ui/themes';

import { useIsCloud } from '../../../../shared/hooks';
import { useBlobStoreCdnStatuses } from './useBlobStoreCdnStatuses';

/**
 * Copy for the "no blob stores support Global Distribution" empty state. Shared with
 * GlobalDistributionPage, which renders the same wording as its pristine card when an Enterprise
 * license is present. This card's own branch stays reachable without that license, so both
 * renderings are permanent and must not drift apart.
 */
export const NO_CDN_BLOB_STORES_STRINGS = {
  heading: 'No blob stores have Global Distribution enabled',
  body: 'Enable Global Distribution for an S3 blob store before enabling it for its repositories.',
  buttonLabel: 'View blob stores',
};

/**
 * Blob Store Distribution Status card - shows CDN configuration for supported blob stores.
 *
 * Only rendered for self-hosted (not cloud) editions. Only shows supported blob stores,
 * which are the only type that currently supports CDN/global distribution.
 */
export function BlobStoreCdnStatusCard() {
  const { blobStores, isLoading, error, canReadBlobStores } = useBlobStoreCdnStatuses();
  const isCloud = useIsCloud();

  // Cloud has no blob store admin pages, so the card's links and empty state have nowhere to go.
  // Without blob store read permission there is nothing to show.
  if (isCloud || !canReadBlobStores) {
    return null;
  }

  if (isLoading) {
    return (
      <Card size="3" variant="surface" mb="5">
        <Box p="4">
          <Text size="2" color="gray">
            Loading blob stores…
          </Text>
        </Box>
      </Card>
    );
  }

  if (error) {
    return (
      <Card size="3" variant="surface" mb="5">
        <Box p="4">
          <Text size="2" color="red">
            Failed to load blob stores: {error}
          </Text>
        </Box>
      </Card>
    );
  }

  // Show helpful message if no supported blob stores (other types don't support CDN)
  if (blobStores.length === 0) {
    return (
      <Card size="3" variant="surface" mb="5">
        <Box p="4">
          <Heading size="3" mb="1">
            {NO_CDN_BLOB_STORES_STRINGS.heading}
          </Heading>
          <Text size="2" color="gray" as="div" mb="4">
            {NO_CDN_BLOB_STORES_STRINGS.body}
          </Text>
          <Button
            size="2"
            variant="solid"
            color="blue"
            onClick={() => window.location.hash = '#preview/admin/repository/blobstores'}
          >
            {NO_CDN_BLOB_STORES_STRINGS.buttonLabel}
          </Button>
        </Box>
      </Card>
    );
  }

  return (
    <Card size="3" variant="surface" mb="5">
      <Box p="4">
        <Heading size="3" mb="1">
          Blob Store Distribution Status
        </Heading>
        <Text size="2" color="gray" as="div" mb="4">
          Enable Global Distribution for an S3 blob store before enabling it for its repositories.
        </Text>

        <Inset side="x">
          <Table.Root size="2" variant="surface">
            <Table.Header>
              <Table.Row>
                <Table.ColumnHeaderCell style={{ minWidth: 180 }}>
                  <Text size="2" weight="medium">
                    Blob Store
                  </Text>
                </Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell style={{ minWidth: 180 }}>
                  <Text size="2" weight="medium">
                    Type
                  </Text>
                </Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell style={{ minWidth: 140 }}>
                  <Text size="2" weight="medium">
                    Distribution Status
                  </Text>
                </Table.ColumnHeaderCell>
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {blobStores.map((blobStore) => (
                <Table.Row key={blobStore.name}>
                  <Table.Cell>
                    <Link
                      href={`#preview/admin/repository/blobstores/${encodeURIComponent(blobStore.type)}/${encodeURIComponent(blobStore.name)}`}
                      size="2"
                      weight="medium"
                    >
                      {blobStore.name}
                    </Link>
                  </Table.Cell>
                  <Table.Cell>
                    <Text size="2">
                      {blobStore.type}
                    </Text>
                  </Table.Cell>
                  <Table.Cell>
                    <Flex align="center" gap="2">
                      {blobStore.cdnEnabled ? (
                        <Badge size="1" variant="soft" color="green" radius="full">
                          Enabled
                        </Badge>
                      ) : (
                        <>
                          <Badge size="1" variant="soft" color="gray" radius="full">
                            Disabled
                          </Badge>
                          <Link
                            href={`#preview/admin/repository/blobstores/${encodeURIComponent(blobStore.type)}/${encodeURIComponent(blobStore.name)}`}
                            size="2"
                            ml="1"
                          >
                            Configure
                          </Link>
                        </>
                      )}
                    </Flex>
                  </Table.Cell>
                </Table.Row>
              ))}
            </Table.Body>
          </Table.Root>
        </Inset>
      </Box>
    </Card>
  );
}

export default BlobStoreCdnStatusCard;
