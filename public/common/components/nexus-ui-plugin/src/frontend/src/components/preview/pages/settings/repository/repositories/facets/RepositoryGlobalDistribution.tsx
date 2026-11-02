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
import { Box, Button, Callout, Flex, Heading, Text, VisuallyHidden } from '@radix-ui/themes';
import { AlertTriangle, CheckCircle2, ExternalLink, Info } from 'lucide-react';

import { SettingsCheckbox } from '../../../../../shared/form';
import { EditionPill } from '../../../../../shared/Badges/EditionPill';
import { LicenseRequiredCallout, OPENS_IN_NEW_TAB } from '../../blob-stores/LicenseRequiredCallout';
import UIStrings from '../../../../../../../constants/pages/admin/repository/RepositoriesStrings';

const STRINGS = UIStrings.STORAGE.GLOBAL_DISTRIBUTION;
const BLOB_STORE_OFF_MESSAGE_ID = 'global-distribution-blob-store-off-message';
const NOT_S3_MESSAGE_ID = 'global-distribution-not-s3-message';

export type RepositoryGlobalDistributionMode = 'unlicensed' | 'blobStoreOff' | 'available';

export interface RepositoryGlobalDistributionProps {
  mode: RepositoryGlobalDistributionMode;
  checked?: boolean;
  onChange: (checked: boolean, event?: React.ChangeEvent<HTMLInputElement>) => void;
  /** Selected blob store; omitted on Cloud, where it isn't user-visible. */
  blobStoreName?: string;
  /** Edit page of the selected blob store; omitted when the user can't open it. */
  blobStoreHref?: string;
  /**
   * Whether the selected blob store is S3. Only consulted in 'blobStoreOff' mode: an S3 blob
   * store without Global Distribution turned on yet shows the actionable orange "not enabled"
   * callout, while a blob store type that can never support it (File, Group, Azure, Google)
   * shows a neutral blue informational callout instead. Defaults to true so existing callers
   * that always pass S3 blob stores keep their current behavior unchanged.
   */
  isS3BlobStore?: boolean;
}

/** Global Distribution block of the repository form's Storage section. */
export function RepositoryGlobalDistribution({
  mode,
  checked = false,
  onChange,
  blobStoreName,
  blobStoreHref,
  isS3BlobStore = true,
}: RepositoryGlobalDistributionProps): React.ReactElement {
  const hasSelectedBlobStore = mode === 'blobStoreOff' && Boolean(blobStoreName);
  const showBlobStoreOffCallout = hasSelectedBlobStore && isS3BlobStore;
  const showNotS3Callout = hasSelectedBlobStore && !isS3BlobStore;

  const describedById = showBlobStoreOffCallout
    ? BLOB_STORE_OFF_MESSAGE_ID
    : showNotS3Callout
      ? NOT_S3_MESSAGE_ID
      : undefined;

  return (
    <Box mb="4" data-testid="repository-global-distribution">
      <Flex align="center" gap="2" mb="3">
        <Heading as="h3" size="3" weight="medium">
          {STRINGS.title}
        </Heading>
        {mode === 'unlicensed' && <EditionPill tier="enterprise" />}
      </Flex>

      {mode === 'unlicensed' ? (
        <LicenseRequiredCallout feature="repositoryGlobalDistribution" />
      ) : (
        <>
          <SettingsCheckbox
            name="global-distribution"
            label={STRINGS.label}
            description={STRINGS.description}
            checked={mode === 'available' && checked}
            onChange={onChange}
            disabled={mode === 'blobStoreOff'}
            analyticsId="nxrm-repository-global-distribution-checkbox"
            ariaDescribedBy={describedById}
          />

          {mode === 'available' && checked && (
            <Callout.Root color="green" variant="surface" data-testid="global-distribution-enabled-callout">
              <Callout.Icon>
                <CheckCircle2 size={16} />
              </Callout.Icon>
              <Callout.Text as="div">
                <Text as="div" weight="medium">
                  {STRINGS.enabled}
                </Text>
                {blobStoreName && (
                  <Text as="div" size="2" color="gray">
                    {STRINGS.servedFrom(blobStoreName)}
                  </Text>
                )}
              </Callout.Text>
            </Callout.Root>
          )}

          {showBlobStoreOffCallout && blobStoreName && (
            <Callout.Root color="orange" variant="surface" data-testid="global-distribution-blob-store-off-callout">
              <Callout.Icon>
                <AlertTriangle size={16} />
              </Callout.Icon>
              <Callout.Text as="div">
                <Text as="div" weight="medium" id={BLOB_STORE_OFF_MESSAGE_ID}>
                  {STRINGS.blobStoreOff(blobStoreName)}
                </Text>
                {blobStoreHref && (
                  <Button asChild variant="soft" color="orange" size="1" mt="2">
                    <a
                      href={blobStoreHref}
                      target="_blank"
                      rel="noopener noreferrer"
                      data-analytics-id="nxrm-repository-global-distribution-configure-blob-store-link"
                    >
                      {STRINGS.configureBlobStore} <ExternalLink size={14} aria-hidden="true" />
                      <VisuallyHidden>{OPENS_IN_NEW_TAB}</VisuallyHidden>
                    </a>
                  </Button>
                )}
              </Callout.Text>
            </Callout.Root>
          )}

          {showNotS3Callout && (
            <Callout.Root color="blue" variant="surface" data-testid="global-distribution-not-s3-callout">
              <Callout.Icon>
                <Info size={16} />
              </Callout.Icon>
              <Callout.Text id={NOT_S3_MESSAGE_ID}>{STRINGS.notSupportedForBlobStore}</Callout.Text>
            </Callout.Root>
          )}
        </>
      )}
    </Box>
  );
}

export default RepositoryGlobalDistribution;
