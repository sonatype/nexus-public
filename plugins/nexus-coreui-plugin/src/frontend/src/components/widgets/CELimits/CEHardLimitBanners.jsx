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
import PropTypes from "prop-types";

import {ExtJS} from '@sonatype/nexus-ui-plugin';
import {
  NxTextLink,
  NxErrorAlert,
  NxWarningAlert,
  NxH3
} from '@sonatype/react-shared-components';
import './CEHardLimitsBanners.scss';
import CEHardLimitBannersMachine from './CEHardLimitBannersMachine';

import {helperFunctions} from './UsageHelper';

const {
  useGracePeriodEndDate,
  useViewPurchaseALicenseUrl,
  useThrottlingStatus,
  useDaysUntilGracePeriodEnds
} = helperFunctions;

import UIStrings from '../../../constants/UIStrings';

const {
  WELCOME: {
    USAGE: {
      HEADER,
      BANNERS
    }
  }
} = UIStrings;

export default function CEHardLimitBanners({onClose}) {
  const [state, send] = useMachine(CEHardLimitBannersMachine, {
    devTools: true
  });

  const user = ExtJS.useUser();
  const isAdmin = user?.administrator;
  const isHa = ExtJS.state().getValue('nexus.datastore.clustered.enabled');
  const isCommunityEdition = ExtJS.state().getEdition() === 'COMMUNITY';

  const gracePeriodEndDate = useGracePeriodEndDate();
  const throttlingStatus = useThrottlingStatus();
  const daysUntilGracePeriodEnds = useDaysUntilGracePeriodEnds();

  const {isUnderEndGraceDismissed} = state.context;

  function scrollToUsageCenter() {
    const targetPath = '#browse/welcome';
    const targetElementId = 'nxrm-usage-center';
  
    function scrollToElement() {
      const usageCenterElement = document.getElementById(targetElementId);
      if (usageCenterElement) {
        usageCenterElement.scrollIntoView({ behavior: 'smooth' });
      }
    }
  
    if (window.location.hash !== targetPath) {
      window.location.hash = targetPath;
      setTimeout(scrollToElement, 200);
    } else {
      scrollToElement();
    }
  }

  function ContactLinks() {
    return <>
      <NxTextLink className="review-usage-link" onClick={scrollToUsageCenter}>
        Review your usage
      </NxTextLink> or <NxTextLink className="usage-view-pricing-link" href={useViewPurchaseALicenseUrl()} target="_blank">
        purchase a license to remove limits.
      </NxTextLink>
    </>
  }

  function NonAdminContactLink() {
    const params = {
      utm_medium: 'product',
      utm_source: 'nexus_repo_community',
      utm_campaign: 'repo_community_usage'
    };
    const learnLink = `http://links.sonatype.com/products/nxrm3/learn-about-community-edition?${new URLSearchParams(params).toString()}`;

    return <>
      <NxTextLink className="ce-learn-more-link" href={learnLink} target="_blank">
        Learn about Nexus Repository Community Edition.
      </NxTextLink>
    </>
  }

  function dismissBelowLimitOutofGrace() {
    onClose();
    send({type: 'DISMISS'});
  }

  return (!isHa && <div className='ce-banners'>
    {isCommunityEdition && isAdmin &&
      <>
        {throttlingStatus === 'NEAR_LIMITS_NEVER_IN_GRACE' &&
          <NxWarningAlert className="ce-banner-near-limit-never-in-grace-period" onClose={onClose}>
            <p>{BANNERS.NEAR_LIMITS} <ContactLinks /></p>
          </NxWarningAlert>
        }
        {throttlingStatus === 'OVER_LIMITS_IN_GRACE' &&
          <NxErrorAlert className="ce-banner-over-limit-in-grace-period" onClose={onClose}>
            <p>{BANNERS.OVER_LIMIT_IN_GRACE(daysUntilGracePeriodEnds, gracePeriodEndDate)} <ContactLinks /></p>
          </NxErrorAlert>
        }
        {throttlingStatus === 'OVER_LIMITS_GRACE_PERIOD_ENDED' &&
          <NxErrorAlert className="ce-banner-over-limit-grace-period-ended">
            <NxH3 className="banner-header">{HEADER.OVER_LIMITS.TITLE}</NxH3>
            <p>{BANNERS.OVER_LIMIT_END_GRACE} <ContactLinks /></p>
          </NxErrorAlert>
        }
        {throttlingStatus === 'BELOW_LIMITS_GRACE_PERIOD_ENDED' && !isUnderEndGraceDismissed &&
          <NxWarningAlert className="ce-banner-under-limit-grace-period-ended" onClose={() => dismissBelowLimitOutofGrace()}>
            <p>{BANNERS.BELOW_LIMIT_END_GRACE} <ContactLinks /></p>
          </NxWarningAlert>
        }
      </>
    }
    {isCommunityEdition && !isAdmin &&
      <>
        {throttlingStatus === 'NON_ADMIN_OVER_LIMITS_GRACE_PERIOD_ENDED' &&
          <NxErrorAlert className="ce-banner-over-limit-non-admin" onClose={onClose}>
            <p>{BANNERS.THROTTLING_NON_ADMIN} <NonAdminContactLink /></p>
          </NxErrorAlert>
        }
        {throttlingStatus === 'NEAR_LIMITS_NON_ADMIN' && 
          <NxWarningAlert className="ce-banner-nearing-limit-non-admin" onClose={onClose}>
            <p>{BANNERS.NEARING_NON_ADMIN} <NonAdminContactLink /></p>
          </NxWarningAlert>
        }
      </>
    }
  </div>
  );
}

CEHardLimitBanners.propTypes = {
  onClose: PropTypes.func
};
