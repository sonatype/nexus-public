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
import { Box, Flex, Text, Heading } from '@radix-ui/themes';
import {
  HardDrive,
  Layers,
  ChevronRight,
} from 'lucide-react';
import { AwsS3Icon, AzureBlobIcon, GoogleCloudIcon } from './CloudProviderIcons';
import { BLOB_STORE_TYPE_IDS } from './blobStoreFormMachine';

import './BlobStoreTypeSelector.scss';

export type BlobStoreTypeId =
  | typeof BLOB_STORE_TYPE_IDS.FILE
  | typeof BLOB_STORE_TYPE_IDS.S3
  | typeof BLOB_STORE_TYPE_IDS.AZURE
  | typeof BLOB_STORE_TYPE_IDS.GOOGLE
  | typeof BLOB_STORE_TYPE_IDS.GROUP;

export interface BlobStoreTypeOption {
  id: BlobStoreTypeId;
  label: string;
  description: string;
  icon: React.ComponentType<{ size?: number; className?: string }>;
  accentClass: string;
}

const BLOB_STORE_OPTIONS: BlobStoreTypeOption[] = [
  {
    id: BLOB_STORE_TYPE_IDS.FILE,
    label: 'File',
    description: 'Local filesystem storage. Simple and fast for single-node deployments.',
    icon: HardDrive,
    accentClass: 'blob-type-selector__card--file',
  },
  {
    id: BLOB_STORE_TYPE_IDS.S3,
    label: 'Amazon S3',
    description: 'AWS object storage. Scalable, durable, with IAM and encryption support.',
    icon: AwsS3Icon,
    accentClass: 'blob-type-selector__card--s3',
  },
  {
    id: BLOB_STORE_TYPE_IDS.AZURE,
    label: 'Azure Blob',
    description: 'Microsoft Azure Blob Storage. Enterprise cloud storage with managed identity.',
    icon: AzureBlobIcon,
    accentClass: 'blob-type-selector__card--azure',
  },
  {
    id: BLOB_STORE_TYPE_IDS.GOOGLE,
    label: 'Google Cloud',
    description: 'Google Cloud Storage. Object storage with application-default credentials.',
    icon: GoogleCloudIcon,
    accentClass: 'blob-type-selector__card--google',
  },
  {
    id: BLOB_STORE_TYPE_IDS.GROUP,
    label: 'Group',
    description: 'Aggregate multiple blob stores. Supports fill policies like round-robin.',
    icon: Layers,
    accentClass: 'blob-type-selector__card--group',
  },
];

export interface BlobStoreTypeSelectorProps {
  selectedType: BlobStoreTypeId | null;
  onSelect: (type: BlobStoreTypeId) => void;
  disabled?: boolean;
}

export function BlobStoreTypeSelector({
  selectedType,
  onSelect,
  disabled = false,
}: BlobStoreTypeSelectorProps) {
  return (
    <Box className="blob-type-selector">
      <Heading size="4" mb="2" weight="medium">
        Choose storage type
      </Heading>
      <Text size="2" color="gray" mb="6" as="p">
        Select the blob store type that matches your infrastructure. You can configure credentials and
        advanced options in the next steps.
      </Text>

      <Box className="blob-type-selector__grid" role="listbox" aria-label="Blob store types">
        {BLOB_STORE_OPTIONS.map((opt) => {
          const Icon = opt.icon;
          const isSelected = selectedType === opt.id;
          return (
            <button
              type="button"
              key={opt.id}
              role="option"
              aria-selected={isSelected}
              className={`blob-type-selector__card ${opt.accentClass} ${
                isSelected ? 'blob-type-selector__card--selected' : ''
              }`}
              onClick={() => !disabled && onSelect(opt.id)}
              disabled={disabled}
            >
              <Flex align="start" gap="4">
                <Box className="blob-type-selector__icon-wrap">
                  <Icon size={45} className="blob-type-selector__icon" />
                </Box>
                <Box style={{ flex: 1, minWidth: 0 }}>
                  <Text weight="bold" size="3" mb="1" as="p">
                    {opt.label}
                  </Text>
                  <Text size="2" color="gray" style={{ lineHeight: 1.5 }} as="p">
                    {opt.description}
                  </Text>
                </Box>
                <ChevronRight
                  size={20}
                  className={`blob-type-selector__chevron ${
                    isSelected ? 'blob-type-selector__chevron--visible' : ''
                  }`}
                />
              </Flex>
            </button>
          );
        })}
      </Box>
    </Box>
  );
}

export default BlobStoreTypeSelector;
