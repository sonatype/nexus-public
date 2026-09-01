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
import React, {useState, useReducer, useEffect} from 'react';
import {Callout, Flex, Text, Link, IconButton, Button} from '@radix-ui/themes';
import {AlertTriangle, X} from 'lucide-react';
import { ExtJS } from '../../../../interface/ExtJS';
import { scrollToUsageCenter } from '../../../../interface/LocationUtils';
import {helperFunctions} from '../../../widgets/SystemStatusAlerts/CELimits/UsageHelper';

import UIStrings from '../../../../constants/UIStrings';

const {
  WELCOME: {
    USAGE: {
      BANNERS,
      HEADER,
      HEADER: {BUTTONS}
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
  const [, forceUpdate] = useReducer((x: number) => x + 1, 0);

  useEffect(() => {
    const TEST_KEYS = [
      'SONATYPE_TEST_CE_THROTTLING_STATUS',
      'SONATYPE_TEST_CE_GRACE_PERIOD_ENDS',
      'SONATYPE_TEST_CE_COMPONENTS',
      'SONATYPE_TEST_CE_REQUESTS',
    ];
    const handleStorageChange = (e: StorageEvent) => {
      if (TEST_KEYS.includes(e.key ?? '')) {
        forceUpdate();
      }
    };
    window.addEventListener('storage', handleStorageChange);
    return () => window.removeEventListener('storage', handleStorageChange);
  }, []);

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
  // Note: BELOW_LIMITS_IN_GRACE is intentionally not shown in the admin banners
  // because the CE banner is only shown once grace ends (BELOW_LIMITS_GRACE_PERIOD_ENDED).
  // During grace period, if below limits, there's no user-actionable alert needed.
  if (isHa || !isCommunityEdition || throttlingStatus === 'NO_THROTTLING') {
    return null;
  }

  const dismissBelowLimitOutofGrace = () => {
    const expires = new Date();
    expires.setMonth(expires.getMonth() + 6);
    const secureFlag = window.location.protocol === 'https:' ? '; Secure' : '';
    document.cookie = `${DISMISS_COOKIE_NAME}; expires=${expires.toUTCString()}; path=/; SameSite=Lax${secureFlag}`;
    setIsUnderEndGraceDismissed(true);
  };

  // Right-aligned CTA button row rendered below the alert body copy.
  // "Learn More" uses surface/tertiary styling; "Purchase Now" uses primary
  // (solid, blue background / white text) styling and is only shown when
  // includePurchaseNow is set (i.e. the write-restricted state), mirroring the
  // Classic UI CTA set per state.
  // The app defines global anchor styling that leaks into Radix Buttons rendered
  // `asChild` around an <a>, overriding the button's own text color/decoration.
  // Set the intended color explicitly (inline, to beat the global rule) so the
  // surface/tertiary "Learn More" keeps its accent text and the primary
  // "Purchase Now" keeps white text on its blue background.
  const surfaceCtaStyle = {color: 'var(--accent-a11)', textDecoration: 'none'};
  const solidCtaStyle = {color: 'var(--accent-contrast)', textDecoration: 'none'};

  const renderCtaRow = (analyticsKey: string, includePurchaseNow: boolean): React.ReactElement => (
    <Flex justify="end" gap="2" align="center">
      <Button
        asChild
        variant="surface"
        color="gray"
        size="1"
        data-analytics-id={`nxrm-ce-${analyticsKey}-learn-more-btn`}
      >
        <a href={learnMoreUrl} target="_blank" rel="noopener noreferrer" style={surfaceCtaStyle}>
          {BUTTONS.LEARN_MORE}
        </a>
      </Button>
      {includePurchaseNow && (
        <Button
          asChild
          variant="solid"
          color="blue"
          size="1"
          data-analytics-id={`nxrm-ce-${analyticsKey}-purchase-now-btn`}
        >
          <a href={purchaseLicenseUrl} target="_blank" rel="noopener noreferrer" style={solidCtaStyle}>
            {BUTTONS.PURCHASE_NOW}
          </a>
        </Button>
      )}
    </Flex>
  );

  const renderAdminBanners = (): React.ReactElement | null => {
    const banners: React.ReactElement[] = [];

    // NEAR_LIMITS_NEVER_IN_GRACE - Soft limit warning (75% usage)
    if (throttlingStatus === 'NEAR_LIMITS_NEVER_IN_GRACE') {
      banners.push(
        <Callout.Root key="near-limits" color="yellow" size="1" role="status">
          <Flex direction="column" gap="2">
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
            {renderCtaRow('near-limit', false)}
          </Flex>
        </Callout.Root>
      );
    }

    // OVER_LIMITS_IN_GRACE - Hard limit with grace period active.
    // Uses error (red) styling to match the global banner (CELimitsAlert.tsx) —
    // confirmed by Design (NEXUS-54200): this is an over-usage state and must
    // read as an error, not a warning.
    if (throttlingStatus === 'OVER_LIMITS_IN_GRACE') {
      banners.push(
        <Callout.Root key="over-limit-in-grace" color="red" size="1" role="alert">
          <Flex direction="column" gap="2">
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
            {renderCtaRow('over-limit-grace', false)}
          </Flex>
        </Callout.Root>
      );
    }

    // OVER_LIMITS_GRACE_PERIOD_ENDED - Hard limit after grace period (write restricted)
    if (throttlingStatus === 'OVER_LIMITS_GRACE_PERIOD_ENDED') {
      banners.push(
        <Callout.Root key="over-limit-grace-ended" color="red" size="1" role="alert">
          <Flex direction="column" gap="2">
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
            {renderCtaRow('over-limit', true)}
          </Flex>
        </Callout.Root>
      );
    }

    // BELOW_LIMITS_GRACE_PERIOD_ENDED - Warning after grace period, dismissible
    if (throttlingStatus === 'BELOW_LIMITS_GRACE_PERIOD_ENDED' && !isUnderEndGraceDismissed) {
      banners.push(
        <Callout.Root key="below-limit-grace-ended" color="yellow" size="1" role="status">
          <Flex direction="column" gap="2">
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
            {renderCtaRow('below-limit', false)}
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
        <Callout.Root key="non-admin-over-limit" color="red" size="1" role="alert">
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
        <Callout.Root key="non-admin-near-limit" color="yellow" size="1" role="status">
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
