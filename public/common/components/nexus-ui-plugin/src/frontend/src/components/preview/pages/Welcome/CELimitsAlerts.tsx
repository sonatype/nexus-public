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
import React, {useState} from 'react';
import {Callout, Flex, Text, Link, IconButton} from '@radix-ui/themes';
import {AlertTriangle, X} from 'lucide-react';
import { ExtJS } from '../../../../interface/ExtJS';
import { scrollToUsageCenter } from '../../../../interface/LocationUtils';
import {helperFunctions} from '../../../widgets/SystemStatusAlerts/CELimits/UsageHelper';

import UIStrings from '../../../../constants/UIStrings';

const {
  WELCOME: {
    USAGE: {
      BANNERS,
      HEADER
    }
  }
} = UIStrings;

const {
  useThrottlingStatus,
  useGracePeriodEndDate,
  useDaysUntilGracePeriodEnds,
  useViewPurchaseALicenseUrl,
  useViewLearnMoreUrl
} = helperFunctions;

const DISMISS_COOKIE_NAME = 'under_end_grace=dismissed';

export default function CELimitsAlerts(): React.ReactElement | null {
  const user = ExtJS.useUser();
  const isAdmin = user?.administrator ?? false;
  const isCommunityEdition = ExtJS.state().getEdition() === 'COMMUNITY';
  const isHa = ExtJS.state().getValue('nexus.datastore.clustered.enabled');

  const throttlingStatus = useThrottlingStatus();
  const gracePeriodEndDate = useGracePeriodEndDate();
  const daysUntilGracePeriodEnds = useDaysUntilGracePeriodEnds();
  const purchaseLicenseUrl = useViewPurchaseALicenseUrl();
  const learnMoreUrl = useViewLearnMoreUrl();

  const [isUnderEndGraceDismissed, setIsUnderEndGraceDismissed] = useState(
    document.cookie.includes(DISMISS_COOKIE_NAME)
  );

  // Don't render if not CE, HA mode, or no throttling
  if (isHa || !isCommunityEdition || throttlingStatus === 'NO_THROTTLING') {
    return null;
  }

  const dismissBelowLimitOutofGrace = () => {
    const expires = new Date();
    expires.setMonth(expires.getMonth() + 6);
    document.cookie = `${DISMISS_COOKIE_NAME}; expires=${expires.toUTCString()}; path=/`;
    setIsUnderEndGraceDismissed(true);
  };

  const renderAdminBanners = (): React.ReactElement | null => {
    const banners: React.ReactElement[] = [];

    // NEAR_LIMITS_NEVER_IN_GRACE - Soft limit warning (75% usage)
    if (throttlingStatus === 'NEAR_LIMITS_NEVER_IN_GRACE') {
      banners.push(
        <Callout.Root key="near-limits" color="yellow" size="1">
          <Flex gap="2" align="center">
            <AlertTriangle size={16} style={{flexShrink: 0}} />
            <Text size="2">
              {BANNERS.NEAR_LIMITS}{' '}
              <Link
                data-analytics-id="nxrm-ce-near-limit-banner-scroll-to-usage"
                onClick={scrollToUsageCenter}
              >
                Review your usage
              </Link>{' '}
              or{' '}
              <Link
                data-analytics-id="nxrm-ce-near-limit-banner-purchase-license"
                href={purchaseLicenseUrl}
                target="_blank"
              >
                purchase a license to remove limits.
              </Link>
            </Text>
          </Flex>
        </Callout.Root>
      );
    }

    // OVER_LIMITS_IN_GRACE - Hard limit with grace period active
    if (throttlingStatus === 'OVER_LIMITS_IN_GRACE') {
      banners.push(
        <Callout.Root key="over-limit-in-grace" color="red" size="1">
          <Flex gap="2" align="center">
            <AlertTriangle size={16} style={{flexShrink: 0}} />
            <Flex direction="column" gap="1">
              <Text weight="medium" size="2">
                {BANNERS.OVER_LIMIT_IN_GRACE_TITLE(daysUntilGracePeriodEnds)}
              </Text>
              <Text size="2">
                {BANNERS.OVER_LIMIT_IN_GRACE(gracePeriodEndDate)}{' '}
                <Link
                  data-analytics-id="nxrm-ce-over-limit-grace-banner-scroll-to-usage"
                  onClick={scrollToUsageCenter}
                >
                  Review your usage
                </Link>{' '}
                or{' '}
                <Link
                  data-analytics-id="nxrm-ce-over-limit-grace-banner-purchase-license"
                  href={purchaseLicenseUrl}
                  target="_blank"
                >
                  purchase a license to remove limits.
                </Link>
              </Text>
            </Flex>
          </Flex>
        </Callout.Root>
      );
    }

    // OVER_LIMITS_GRACE_PERIOD_ENDED - Hard limit after grace period
    if (throttlingStatus === 'OVER_LIMITS_GRACE_PERIOD_ENDED') {
      banners.push(
        <Callout.Root key="over-limit-grace-ended" color="red" size="1">
          <Flex gap="2" align="center">
            <AlertTriangle size={16} style={{flexShrink: 0}} />
            <Flex direction="column" gap="1">
              <Text weight="medium" size="2">{HEADER.OVER_LIMITS.TITLE}</Text>
              <Text size="2">
                {BANNERS.OVER_LIMIT_END_GRACE}{' '}
                <Link
                  data-analytics-id="nxrm-ce-over-limit-banner-scroll-to-usage"
                  onClick={scrollToUsageCenter}
                >
                  Review your usage
                </Link>{' '}
                or{' '}
                <Link
                  data-analytics-id="nxrm-ce-over-limit-banner-purchase-license"
                  href={purchaseLicenseUrl}
                  target="_blank"
                >
                  purchase a license to remove limits.
                </Link>
              </Text>
            </Flex>
          </Flex>
        </Callout.Root>
      );
    }

    // BELOW_LIMITS_GRACE_PERIOD_ENDED - Warning after grace period, dismissible
    if (throttlingStatus === 'BELOW_LIMITS_GRACE_PERIOD_ENDED' && !isUnderEndGraceDismissed) {
      banners.push(
        <Callout.Root key="below-limit-grace-ended" color="yellow" size="1">
          <Flex gap="2" align="center" justify="between">
            <Flex gap="2" align="center">
              <AlertTriangle size={16} style={{flexShrink: 0}} />
              <Text size="2">
                {BANNERS.BELOW_LIMIT_END_GRACE}{' '}
                <Link
                  data-analytics-id="nxrm-ce-below-limit-banner-scroll-to-usage"
                  onClick={scrollToUsageCenter}
                >
                  Review your usage
                </Link>{' '}
                or{' '}
                <Link
                  data-analytics-id="nxrm-ce-below-limit-banner-purchase-license"
                  href={purchaseLicenseUrl}
                  target="_blank"
                >
                  purchase a license to remove limits.
                </Link>
              </Text>
            </Flex>
            <IconButton
              data-analytics-id="nxrm-ce-below-limit-banner-dismiss"
              variant="ghost"
              size="1"
              onClick={dismissBelowLimitOutofGrace}
              aria-label="Dismiss"
            >
              <X size={14} />
            </IconButton>
          </Flex>
        </Callout.Root>
      );
    }

    return banners.length > 0 ? <Flex direction="column" gap="2">{banners}</Flex> : null;
  };

  const renderNonAdminBanners = (): React.ReactElement | null => {
    const banners: React.ReactElement[] = [];

    // NON_ADMIN_OVER_LIMITS_GRACE_PERIOD_ENDED
    if (throttlingStatus === 'NON_ADMIN_OVER_LIMITS_GRACE_PERIOD_ENDED') {
      banners.push(
        <Callout.Root key="non-admin-over-limit" color="red" size="1">
          <Flex gap="2" align="center">
            <AlertTriangle size={16} style={{flexShrink: 0}} />
            <Text size="2">
              {BANNERS.THROTTLING_NON_ADMIN}{' '}
              <Link
                data-analytics-id="nxrm-ce-non-admin-over-limit-learn-more"
                href={learnMoreUrl}
                target="_blank"
              >
                Learn about Nexus Repository Community Edition
              </Link>
            </Text>
          </Flex>
        </Callout.Root>
      );
    }

    // NEAR_LIMITS_NON_ADMIN
    if (throttlingStatus === 'NEAR_LIMITS_NON_ADMIN') {
      banners.push(
        <Callout.Root key="non-admin-near-limit" color="yellow" size="1">
          <Flex gap="2" align="center">
            <AlertTriangle size={16} style={{flexShrink: 0}} />
            <Text size="2">
              {BANNERS.NEARING_NON_ADMIN}{' '}
              <Link
                data-analytics-id="nxrm-ce-non-admin-near-limit-learn-more"
                href={learnMoreUrl}
                target="_blank"
              >
                Learn about Nexus Repository Community Edition
              </Link>
            </Text>
          </Flex>
        </Callout.Root>
      );
    }

    return banners.length > 0 ? <Flex direction="column" gap="2">{banners}</Flex> : null;
  };

  return isAdmin ? renderAdminBanners() : renderNonAdminBanners();
}
