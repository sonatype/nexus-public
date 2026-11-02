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
import { Box, Button, Callout, Card, Flex, Heading, Text } from '@radix-ui/themes';
import { CheckCircle2 } from 'lucide-react';
import { ExtJS } from '../../../../../../interface/ExtJS';

import {
  PageHeader,
  LoadingState,
  ErrorState,
} from '../../../../shared';
import { SettingsCheckbox } from '../../../../shared/form';
import { useIsCloud, useIsEnterpriseEdition } from '../../../../shared/hooks';
import { LicenseRequiredCallout } from '../blob-stores/LicenseRequiredCallout';
import { useCdnDefaults } from './useCdnDefaults';
import { useBlobStoreCdnStatuses } from './useBlobStoreCdnStatuses';
import { RepositoryCdnStatusCard } from './RepositoryCdnStatusCard';
import { BlobStoreCdnStatusCard, NO_CDN_BLOB_STORES_STRINGS } from './BlobStoreCdnStatusCard';

import './GlobalDistributionPage.scss';

const navigateTo = (path: string) => {
  window.location.hash = path;
};

/**
 * Global Distribution settings page. Without an Enterprise license it shows the license callout and
 * keeps the sections visible with live data but inert. On self-hosted with an Enterprise license and
 * no S3 blob store yet, it shows a pristine empty state instead of the configuration sections.
 */
export function GlobalDistributionPage() {
  const isEnterprise = useIsEnterpriseEdition();
  const isCloud = useIsCloud();
  const canRead = ExtJS.checkPermission('nexus:settings:read');
  const canUpdate = ExtJS.checkPermission('nexus:settings:update');
  const { defaultForNewRepositories, isLoading, hasLoaded, error, setDefault, setError } = useCdnDefaults({
    enabled: isEnterprise && canRead,
  });
  const {
    blobStores,
    isLoading: blobStoresLoading,
    error: blobStoresError,
    canReadBlobStores,
  } = useBlobStoreCdnStatuses();

  // Self-hosted only: cloud provisions its own blob store per repository and has no blob store
  // admin pages, so this state never applies there. A failed fetch also leaves the list empty, so
  // it must fall through to the sections, where BlobStoreCdnStatusCard reports the failure. Without
  // blob store read permission the list is always empty, so emptiness says nothing.
  const isPristine =
    isEnterprise && !isCloud && canReadBlobStores && !blobStoresLoading && !blobStoresError && blobStores.length === 0;

  const header = (
    <PageHeader
      title="Global Distribution"
      description="Distribute repository content globally"
      breadcrumbs={[
        { label: 'Settings', onClick: () => navigateTo('#preview/admin/settings') },
        { label: 'Global Distribution' },
      ]}
    />
  );

  if (isLoading) {
    return (
      <Box className="global-distribution-page">
        {header}
        <LoadingState message="Loading settings..." />
      </Box>
    );
  }

  if (error && !hasLoaded) {
    return (
      <Box className="global-distribution-page">
        {header}
        <ErrorState
          title="Failed to Load"
          message={error.message}
        />
      </Box>
    );
  }

  return (
    <Box className="global-distribution-page">
      {header}

      {error && (
        <Box className="global-distribution-page__alerts">
          <ErrorState
            variant="inline"
            title="Error"
            message={error.message}
            onRetry={() => setError(null)}
            retryText="Dismiss"
          />
        </Box>
      )}

      <Box className="global-distribution-page__content">
        {!isEnterprise && <LicenseRequiredCallout feature="globalDistributionSettings" mb="5" />}

        {isEnterprise && !isPristine && (
          <Callout.Root color="green" variant="surface" mb="5" data-testid="global-distribution-enabled-callout">
            <Callout.Icon>
              <CheckCircle2 size={16} />
            </Callout.Icon>
            {/* Callout.Text renders a <p>, so children must be inline-level. */}
            <Callout.Text>
              <Flex as="span" direction="column">
                <Text as="span" weight="medium">
                  Global Distribution is enabled
                </Text>
                <Text as="span" size="2" color="gray">
                  Repository content can be distributed through CloudFront edge locations.
                </Text>
              </Flex>
            </Callout.Text>
          </Callout.Root>
        )}

        {isPristine ? (
          <Card size="3" variant="surface" data-testid="global-distribution-pristine">
            <Box p="6" style={{ textAlign: 'center' }}>
              <Heading as="h2" size="4" mb="2">
                {NO_CDN_BLOB_STORES_STRINGS.heading}
              </Heading>
              <Text size="2" color="gray" as="div" mb="4" style={{ maxWidth: 480, margin: '0 auto' }}>
                {NO_CDN_BLOB_STORES_STRINGS.body}
              </Text>
              <Button
                size="3"
                onClick={() => navigateTo('#preview/admin/repository/blobstores')}
              >
                {NO_CDN_BLOB_STORES_STRINGS.buttonLabel}
              </Button>
            </Box>
          </Card>
        ) : (
          <Box
            data-testid="global-distribution-sections"
            className={isEnterprise ? undefined : 'global-distribution-page__locked'}
            inert={!isEnterprise}
          >
            {canRead && (
              <Card size="3" variant="surface" mb="5">
                <Box p="4">
                  <Heading size="3" mb="4">
                    Global Configuration
                  </Heading>
                  <SettingsCheckbox
                    name="defaultForNewRepositories"
                    label="Enable Global Distribution for new repositories by default"
                    description="Applies when the repository's blob store has Global Distribution enabled."
                    checked={defaultForNewRepositories}
                    onChange={(value) => setDefault(value)}
                    disabled={!canUpdate || !isEnterprise}
                  />
                </Box>
              </Card>
            )}

            <BlobStoreCdnStatusCard />

            <RepositoryCdnStatusCard />
          </Box>
        )}
      </Box>
    </Box>
  );
}

export default GlobalDistributionPage;
