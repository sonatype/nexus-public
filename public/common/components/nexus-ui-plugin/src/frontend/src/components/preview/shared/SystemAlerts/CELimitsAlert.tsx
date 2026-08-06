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
import {Link} from '@radix-ui/themes';

import {ExtJS} from '../../../../interface/ExtJS';
import {scrollToUsageCenter} from '../../../../interface/LocationUtils';
import {helperFunctions} from '../../../widgets/SystemStatusAlerts/CELimits/UsageHelper';
import UIStrings from '../../../../constants/UIStrings';
import {SystemAlert} from './SystemAlert';

const {
  WELCOME: {
    USAGE: {BANNERS, HEADER},
  },
} = UIStrings;

const {
  useThrottlingStatus,
  useGracePeriodEndDate,
  useDaysUntilGracePeriodEnds,
  useViewPurchaseALicenseUrl,
  buildLearnMoreUrl,
} = helperFunctions;

const DISMISS_COOKIE = 'under_end_grace=dismissed';

/** Review-usage + purchase-license inline links (admin banners). */
function UsagePurchaseLinks({purchaseUrl}: {purchaseUrl: string}): React.ReactElement {
  return (
    <>
      {' '}
      <Link
        data-analytics-id="nxrm-ce-hard-limit-banner-scroll-to-usage"
        onClick={scrollToUsageCenter}
        style={{cursor: 'pointer'}}
      >
        Review your usage
      </Link>{' '}
      or{' '}
      <Link
        data-analytics-id="nxrm-ce-hard-limit-banner-usage-view-pricing-link"
        href={purchaseUrl}
        target="_blank"
      >
        purchase a license to remove limits.
      </Link>
    </>
  );
}

/** Learn-more inline link (non-admin banners). */
function LearnMoreLink({learnMoreUrl}: {learnMoreUrl: string}): React.ReactElement {
  return (
    <>
      {' '}
      <Link
        data-analytics-id="nxrm-ce-hard-limit-banner-learn-more-link"
        href={learnMoreUrl}
        target="_blank"
      >
        Learn about Nexus Repository Community Edition
      </Link>
    </>
  );
}

/**
 * CELimitsAlert - global Community Edition usage banner (Radix).
 *
 * Self-gating: renders nothing unless the instance is Community Edition, not in
 * HA mode, and the resolved throttling status maps to a banner. Rendered in both
 * Classic and Preview UI by SystemNoticesSwitch. Built on the design-system
 * SystemAlert primitive.
 */
export default function CELimitsAlert(): React.ReactElement | null {
  const throttlingStatus = useThrottlingStatus();
  const gracePeriodEndDate = useGracePeriodEndDate();
  const daysUntilGracePeriodEnds = useDaysUntilGracePeriodEnds();
  const purchaseUrl = useViewPurchaseALicenseUrl();
  const learnMoreUrl = buildLearnMoreUrl(throttlingStatus);

  const isCommunityEdition = ExtJS.useState(() => ExtJS.state().getEdition() === 'COMMUNITY');
  const isHa = ExtJS.useState(() => ExtJS.state().getValue('nexus.datastore.clustered.enabled'));

  const [belowLimitDismissed, setBelowLimitDismissed] = useState(
    document.cookie.includes(DISMISS_COOKIE)
  );

  if (isHa || !isCommunityEdition || throttlingStatus === 'NO_THROTTLING') {
    return null;
  }

  const dismissBelowLimit = () => {
    const expires = new Date();
    expires.setMonth(expires.getMonth() + 6);
    document.cookie = `${DISMISS_COOKIE}; expires=${expires.toUTCString()}; path=/; SameSite=Lax`;
    setBelowLimitDismissed(true);
  };

  switch (throttlingStatus) {
    case 'NEAR_LIMITS_NEVER_IN_GRACE':
      return (
        <SystemAlert
          tier="warning"
          title={BANNERS.NEAR_LIMITS_TITLE}
          message={<>{BANNERS.NEAR_LIMITS}<UsagePurchaseLinks purchaseUrl={purchaseUrl} /></>}
        />
      );

    case 'OVER_LIMITS_IN_GRACE':
      return (
        <SystemAlert
          tier="error"
          title={BANNERS.OVER_LIMIT_IN_GRACE_TITLE(Math.ceil(daysUntilGracePeriodEnds))}
          message={<>{BANNERS.OVER_LIMIT_IN_GRACE(gracePeriodEndDate)}<UsagePurchaseLinks purchaseUrl={purchaseUrl} /></>}
        />
      );

    case 'OVER_LIMITS_GRACE_PERIOD_ENDED':
      return (
        <SystemAlert
          tier="error"
          title={HEADER.OVER_LIMITS.TITLE}
          message={<>{BANNERS.OVER_LIMIT_END_GRACE}<UsagePurchaseLinks purchaseUrl={purchaseUrl} /></>}
        />
      );

    case 'BELOW_LIMITS_GRACE_PERIOD_ENDED':
      if (belowLimitDismissed) {
        return null;
      }
      return (
        <SystemAlert
          tier="warning"
          dismissable
          onDismiss={dismissBelowLimit}
          message={<>{BANNERS.BELOW_LIMIT_END_GRACE}<UsagePurchaseLinks purchaseUrl={purchaseUrl} /></>}
        />
      );

    case 'NON_ADMIN_OVER_LIMITS_GRACE_PERIOD_ENDED':
      return (
        <SystemAlert
          tier="error"
          message={<>{BANNERS.THROTTLING_NON_ADMIN}<LearnMoreLink learnMoreUrl={learnMoreUrl} /></>}
        />
      );

    case 'NEAR_LIMITS_NON_ADMIN':
      return (
        <SystemAlert
          tier="warning"
          message={<>{BANNERS.NEARING_NON_ADMIN}<LearnMoreLink learnMoreUrl={learnMoreUrl} /></>}
        />
      );

    // BELOW_LIMITS_IN_GRACE is intentionally a no-op: the admin is under limits and
    // still within the grace period, so there is nothing to warn about yet.
    default:
      return null;
  }
}
