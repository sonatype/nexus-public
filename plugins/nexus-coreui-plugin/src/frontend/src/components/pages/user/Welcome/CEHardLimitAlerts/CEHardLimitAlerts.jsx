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
import {useMachine} from '@xstate/react';

import {ExtJS} from '@sonatype/nexus-ui-plugin';
import {
  NxErrorAlert,
  NxH3,
  NxButtonBar,
  NxWarningAlert,
  NxInfoAlert,
  NxTextLink
} from '@sonatype/react-shared-components';

import {helperFunctions} from '../../../../widgets/CELimits/UsageHelper';
import UIStrings from '../../../../../constants/UIStrings';
import CEHardLimitAlertsMachine from './CEHardLimitAlertsMachine';
import './CEHardLimitAlerts.scss';

const {
  useGracePeriodEndDate,
  useViewPricingUrl,
  useThrottlingStatus
} = helperFunctions;

const {
  WELCOME: {
    USAGE: {
      HEADER,
    }}} = UIStrings;

export default function CEHardLimitAlerts() {
  const [state, send] = useMachine(CEHardLimitAlertsMachine, {
    devTools: true
  });

  const user = ExtJS.useUser();
  const isAdmin = user?.administrator;
  const isHa = ExtJS.state().getValue('nexus.datastore.clustered.enabled');
  const isCommunityEdition = ExtJS.state().getEdition() === 'COMMUNITY';

  const gracePeriodEndDate = useGracePeriodEndDate();
  const viewPricingUrl = useViewPricingUrl();
  const throttlingStatus = useThrottlingStatus();

  const isUnderEndGraceDismissed = document.cookie.includes('under_end_grace=dismissed');
  const dismissedBanners = state.context.dismissedBanners || [];

  const utmParams = {
    utm_medium: 'product',
    utm_source: 'nexus_repo_community',
    utm_campaign: 'repo_community_usage'
  };

  function PurchaseOrUploadHeaderLinks() {
    return(
      <>
        Purchase a license to remove limits, or if you have already purchased a license{' '}
        <NxTextLink className="ce-upload-license" href="#admin/system/licensing">upload it here</NxTextLink>.
        <NxButtonBar>
          <a
            className="nx-btn nx-btn--primary usage-view-pricing-button"
            target="_blank"
            href={viewPricingUrl}>
            {HEADER.BUTTONS.GET_STARTED}
          </a>
        </NxButtonBar>
      </>
    );
  }

  function UnderLimitsInGracePeriodLinks() {
    return (
      <>
        <NxTextLink target="_blank" className="usage-view-pricing-link" href={viewPricingUrl}>Purchase a license to remove limits</NxTextLink>
          , or if you have already purchased a license{' '}
        <NxTextLink className="ce-upload-license" href="#admin/system/licensing">upload it here</NxTextLink>.
      </>
    )
  }

  function ThrottlingHeaderAlertLinks() {
    const restoreUsageLink = `http://links.sonatype.com/products/nxrm3/how-to-restore-usage?${new URLSearchParams(utmParams).toString()}`;

    return(
      <>
        Purchase a license to remove limits, or if you have already purchased a license{' '}
        <NxTextLink className="ce-upload-license" href="#admin/system/licensing">upload it here</NxTextLink>.
        <NxButtonBar>
          <a className="nx-btn ce-restore-usage" target="_blank" href={restoreUsageLink}>{HEADER.BUTTONS.RESTORE_USAGE}</a>
          <a
            className="nx-btn nx-btn--primary usage-view-pricing-button"
            target="_blank"
            href={viewPricingUrl}>
            {HEADER.BUTTONS.PURCHASE_NOW}</a>
        </NxButtonBar>
      </>
    );
  }

  function dismiss(banner) {
    send({type: 'DISMISS', banner: banner});
  }

  return (!isHa && <div className='ce-alerts'>
    {isCommunityEdition && isAdmin &&
      <>
        {throttlingStatus === 'NEAR_LIMITS_NEVER_IN_GRACE' && !dismissedBanners.includes('near_limits') && <>
            <NxWarningAlert className="ce-alert-near-limit-never-in-grace-period" onClose={() => dismiss('near_limits')}>
              <NxH3>{HEADER.APPROACHING_LIMITS.TITLE}</NxH3>
              <div>
                {HEADER.APPROACHING_LIMITS.WARNING}{' '}
                <PurchaseOrUploadHeaderLinks />
              </div>
            </NxWarningAlert>
          </>
        }
        {throttlingStatus === 'OVER_LIMITS_IN_GRACE' && !dismissedBanners.includes('over_limits_in_grace') && <>
            <NxErrorAlert className="ce-alert-over-limit-in-grace-period" onClose={() => dismiss('over_limits_in_grace')}>
              <NxH3>{HEADER.GRACE_PERIOD.TITLE(gracePeriodEndDate)}</NxH3>
              <div>
                {HEADER.GRACE_PERIOD.OVER_WARNING(gracePeriodEndDate)}{' '}
                <PurchaseOrUploadHeaderLinks />
              </div>
            </NxErrorAlert>
          </>
        }
        {throttlingStatus === 'BELOW_LIMITS_IN_GRACE' && !dismissedBanners.includes('below_limits_in_grace') && <>
            <NxInfoAlert className="ce-alert-under-limit-in-grace-period" onClose={() => dismiss('below_limits_in_grace')}>
              <div>
                {HEADER.GRACE_PERIOD.UNDER_WARNING(gracePeriodEndDate)}{' '}
                <UnderLimitsInGracePeriodLinks />
              </div>
            </NxInfoAlert>
          </>
        }
        {throttlingStatus === 'OVER_LIMITS_GRACE_PERIOD_ENDED' && <>
            <NxErrorAlert className="ce-alert-over-limit-grace-period-ended">
              <NxH3>{HEADER.OVER_LIMITS.TITLE}</NxH3>
              <div>
                {HEADER.OVER_LIMITS.WARNING(gracePeriodEndDate)}{' '}
                <ThrottlingHeaderAlertLinks/>
              </div>
            </NxErrorAlert>
          </>
        }
        {throttlingStatus === 'BELOW_LIMITS_GRACE_PERIOD_ENDED' && !isUnderEndGraceDismissed && !dismissedBanners.includes('under_end_grace') && <>
            <NxWarningAlert className="ce-alert-under-limit-grace-period-ended" onClose={() => dismiss('under_end_grace')}>
              <NxH3>{HEADER.UNDER_LIMITS.TITLE}</NxH3>
              <div>
                {HEADER.UNDER_LIMITS.WARNING}{' '}
                <PurchaseOrUploadHeaderLinks />
              </div>
            </NxWarningAlert>
          </>
        }
      </>
    }
  </div>
  );
}
