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
import { Globe } from 'lucide-react';
import {
  SettingsFormSection,
  SettingsTextInput,
  SettingsPasswordInput,
  SettingsTextArea,
  SettingsSelect,
} from '../../../../shared/form';
import { useLicenseLevel, LicenseLevel } from '../../../../shared/hooks/useLicenseLevel';
import { getGlobalDistributionTypeOptions } from './globalDistributionTypeOptions';
import { LicenseRequiredCallout } from './LicenseRequiredCallout';
import type { BlobStoreFormData, S3CloudFrontConfig } from './types';

const STRINGS = {
  GLOBAL_DISTRIBUTION: {
    title: 'Global Distribution (Optional)',
    description: 'Configure content distribution for improved download performance across regions',
    TYPE: {
      label: 'Distribution Type',
      helpText: 'Select a global distribution method. Options are mutually exclusive.'
    },
    CLOUDFRONT: {
      DISTRIBUTION_URL: {
        label: 'Distribution URL',
        helpText: 'The CloudFront distribution URL',
        placeholder: 'https://d111111abcdef8.cloudfront.net/'
      },
      KEY_ID: {
        label: 'Key ID',
        helpText: 'The identifier in CloudFront for the provided private key'
      },
      PRIVATE_KEY: {
        label: 'Private Key',
        helpText: 'The private key used to create pre-signed URLs (PEM PKCS#8 format)',
        placeholder: '-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----'
      },
      PRIVATE_KEY_PASSWORD: {
        label: 'Private Key Password',
        helpText: 'The password for the private key (optional)'
      }
    }
  }
};

export interface BlobStoreWizardStepGlobalDistributionProps {
  data: BlobStoreFormData;
  onChange: (path: string, value: unknown) => void;
}

export function BlobStoreWizardStepGlobalDistribution({
  data,
  onChange,
}: BlobStoreWizardStepGlobalDistributionProps) {
  const isEnterprise = useLicenseLevel(LicenseLevel.ENTERPRISE);

  const updateGlobalDistribution = (type: string) => {
    if (type === '') {
      onChange('globalDistribution', undefined);
    } else if (type === 'CLOUDFRONT') {
      const current = data.globalDistribution as S3CloudFrontConfig;
      onChange('globalDistribution', {
        type: 'CLOUDFRONT',
        distributionUrl: current?.distributionUrl || '',
        keyId: current?.keyId || '',
        privateKey: current?.privateKey || '',
        privateKeyPassword: current?.privateKeyPassword || ''
      });
    } else if (type === 'TRANSFER_ACCELERATION') {
      onChange('globalDistribution', { type: 'TRANSFER_ACCELERATION' });
    }
  };

  const updateCloudFront = (field: keyof S3CloudFrontConfig, value: string) => {
    onChange(`globalDistribution.${field}`, value);
  };

  return (
    <>
      {!isEnterprise && <LicenseRequiredCallout feature="globalDistribution" mb="4" />}
      <SettingsFormSection
        title={STRINGS.GLOBAL_DISTRIBUTION.title}
        description={STRINGS.GLOBAL_DISTRIBUTION.description}
        icon={<Globe size={20} />}
      >
        <SettingsSelect
          name="s3-global-distribution-type"
          label={STRINGS.GLOBAL_DISTRIBUTION.TYPE.label}
          value={data.globalDistribution?.type || ''}
          onChange={(value) => updateGlobalDistribution(value)}
          options={getGlobalDistributionTypeOptions(isEnterprise)}
          helpText={STRINGS.GLOBAL_DISTRIBUTION.TYPE.helpText}
        />

        {data.globalDistribution?.type === 'CLOUDFRONT' && (
          <>
            <SettingsTextInput
              label={STRINGS.GLOBAL_DISTRIBUTION.CLOUDFRONT.DISTRIBUTION_URL.label}
              value={(data.globalDistribution as S3CloudFrontConfig)?.distributionUrl || ''}
              onChange={(value) => updateCloudFront('distributionUrl', value)}
              helpText={STRINGS.GLOBAL_DISTRIBUTION.CLOUDFRONT.DISTRIBUTION_URL.helpText}
              placeholder={STRINGS.GLOBAL_DISTRIBUTION.CLOUDFRONT.DISTRIBUTION_URL.placeholder}
              required
            />

            <SettingsTextInput
              label={STRINGS.GLOBAL_DISTRIBUTION.CLOUDFRONT.KEY_ID.label}
              value={(data.globalDistribution as S3CloudFrontConfig)?.keyId || ''}
              onChange={(value) => updateCloudFront('keyId', value)}
              helpText={STRINGS.GLOBAL_DISTRIBUTION.CLOUDFRONT.KEY_ID.helpText}
              required
            />

            <SettingsTextArea
              label={STRINGS.GLOBAL_DISTRIBUTION.CLOUDFRONT.PRIVATE_KEY.label}
              value={(data.globalDistribution as S3CloudFrontConfig)?.privateKey || ''}
              onChange={(value) => updateCloudFront('privateKey', value)}
              helpText={STRINGS.GLOBAL_DISTRIBUTION.CLOUDFRONT.PRIVATE_KEY.helpText}
              placeholder={STRINGS.GLOBAL_DISTRIBUTION.CLOUDFRONT.PRIVATE_KEY.placeholder}
              required
              monospace
              rows={6}
            />

            <SettingsPasswordInput
              label={STRINGS.GLOBAL_DISTRIBUTION.CLOUDFRONT.PRIVATE_KEY_PASSWORD.label}
              value={(data.globalDistribution as S3CloudFrontConfig)?.privateKeyPassword || ''}
              onChange={(value) => updateCloudFront('privateKeyPassword', value)}
              helpText={STRINGS.GLOBAL_DISTRIBUTION.CLOUDFRONT.PRIVATE_KEY_PASSWORD.helpText}
              autoComplete="new-password"
            />
          </>
        )}
      </SettingsFormSection>
    </>
  );
}
