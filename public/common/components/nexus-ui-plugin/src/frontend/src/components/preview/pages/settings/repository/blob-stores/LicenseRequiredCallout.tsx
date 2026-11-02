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
import { Callout, Link, VisuallyHidden } from '@radix-ui/themes';
import { ExternalLink, Lock } from 'lucide-react';

const PRO_LEARN_MORE_URL = 'https://links.sonatype.com/products/repository-pro';
const ENTERPRISE_LEARN_MORE_URL = 'https://links.sonatype.com/products/nxrm3/enterprise-license';

/** Visually hidden suffix for links that open in a new tab. */
export const OPENS_IN_NEW_TAB = '(opens in a new tab)';

export type LicenseRequiredFeature =
  | 'presignedUrl'
  | 'globalDistribution'
  | 'globalDistributionSettings'
  | 'repositoryGlobalDistribution';

const CALLOUTS: Record<
  LicenseRequiredFeature,
  { message: string; testId: string; analyticsId: string; learnMoreUrl: string }
> = {
  presignedUrl: {
    message: 'Pre-Signed URL requires a Pro license.',
    testId: 'license-callout-presigned-url',
    analyticsId: 'nxrm-blobstore-presigned-url-license-learn-more-link',
    learnMoreUrl: PRO_LEARN_MORE_URL,
  },
  globalDistribution: {
    message: 'CloudFront and S3 Transfer Acceleration require an Enterprise license.',
    testId: 'license-callout-global-distribution',
    analyticsId: 'nxrm-blobstore-global-distribution-license-learn-more-link',
    learnMoreUrl: ENTERPRISE_LEARN_MORE_URL,
  },
  globalDistributionSettings: {
    message: 'Global Distribution requires an Enterprise license.',
    testId: 'license-callout-global-distribution-settings',
    analyticsId: 'nxrm-global-distribution-license-learn-more-link',
    learnMoreUrl: ENTERPRISE_LEARN_MORE_URL,
  },
  repositoryGlobalDistribution: {
    message: 'Global Distribution requires an Enterprise license.',
    testId: 'license-callout-repository-global-distribution',
    analyticsId: 'nxrm-repository-global-distribution-license-learn-more-link',
    learnMoreUrl: ENTERPRISE_LEARN_MORE_URL,
  },
};

export interface LicenseRequiredCalloutProps {
  feature: LicenseRequiredFeature;
  mb?: React.ComponentProps<typeof Callout.Root>['mb'];
}

export function LicenseRequiredCallout({ feature, mb }: LicenseRequiredCalloutProps): React.ReactElement {
  const { message, testId, analyticsId, learnMoreUrl } = CALLOUTS[feature];
  return (
    <Callout.Root color="amber" variant="surface" mb={mb} data-testid={testId}>
      <Callout.Icon>
        <Lock size={16} />
      </Callout.Icon>
      <Callout.Text>
        {message}{' '}
        <Link
          href={learnMoreUrl}
          target="_blank"
          rel="noopener noreferrer"
          data-analytics-id={analyticsId}
        >
          Learn more <ExternalLink size={10} aria-hidden="true" />
          <VisuallyHidden>{OPENS_IN_NEW_TAB}</VisuallyHidden>
        </Link>
      </Callout.Text>
    </Callout.Root>
  );
}

export default LicenseRequiredCallout;
