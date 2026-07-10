/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
import React, {useState} from 'react';
import {Callout, Flex, Text, Button} from '@radix-ui/themes';
import {AlertTriangle, AlertCircle, X} from 'lucide-react';
import {ExtJS} from '../../../../interface/ExtJS';
import {scrollToUsageCenter} from '../../../../interface/LocationUtils';
import {helperFunctions} from '../../../widgets/SystemStatusAlerts/CELimits/UsageHelper';

import UIStrings from '../../../../constants/UIStrings';

const {
  useGracePeriodEndDate,
  useViewPurchaseALicenseUrl,
  useThrottlingStatus,
} = helperFunctions;

const {WELCOME: {USAGE: {BANNERS, HEADER}}} = UIStrings;

function ReviewUsageLink() {
  return (
    <button
      className="nx-text-link"
      style={{background: 'none', border: 'none', padding: 0, cursor: 'pointer', textDecoration: 'underline', color: 'inherit', fontSize: 'inherit'}}
      onClick={scrollToUsageCenter}
    >
      Review your usage
    </button>
  );
}

function PurchaseLicenseLink() {
  return (
    <a
      href={useViewPurchaseALicenseUrl()}
      target="_blank"
      rel="noopener noreferrer"
      style={{color: 'inherit', textDecoration: 'underline'}}
    >
      purchase a license to remove limits
    </a>
  );
}

export default function CEHardLimitAlerts() {
  const isHa = ExtJS.state().getValue('nexus.datastore.clustered.enabled');
  const isCommunityEdition = ExtJS.state().getEdition() === 'COMMUNITY';
  const user = ExtJS.useUser();
  const isAdmin = user?.administrator;

  const throttlingStatus = useThrottlingStatus();
  const gracePeriodEndDate = useGracePeriodEndDate();

  const [belowLimitDismissed, setBelowLimitDismissed] = useState(
    () => document.cookie.includes('under_end_grace=dismissed')
  );

  if (isHa || !isCommunityEdition) {
    return null;
  }

  function dismissBelowLimitOutofGrace() {
    const expires = new Date();
    expires.setMonth(expires.getMonth() + 6);
    document.cookie = `under_end_grace=dismissed; expires=${expires.toUTCString()}; path=/`;
    setBelowLimitDismissed(true);
  }

  if (isAdmin) {
    return (
      <>
        {throttlingStatus === 'NEAR_LIMITS_NEVER_IN_GRACE' && (
          <Callout.Root color="orange" size="1">
            <Flex gap="2" align="start">
              <AlertTriangle size={16} style={{flexShrink: 0, marginTop: 2}} aria-label="Warning" />
              <Text size="2">
                {BANNERS.NEAR_LIMITS}{' '}
                <ReviewUsageLink /> or{' '}
                <PurchaseLicenseLink />.
              </Text>
            </Flex>
          </Callout.Root>
        )}
        {throttlingStatus === 'OVER_LIMITS_IN_GRACE' && (
          <Callout.Root color="red" size="1">
            <Flex gap="2" align="start">
              <AlertCircle size={16} style={{flexShrink: 0, marginTop: 2}} aria-label="Error" />
              <Flex direction="column" gap="1">
                <Text weight="bold" size="2">{HEADER.GRACE_PERIOD.OVER_WARNING(gracePeriodEndDate)}</Text>
                <Text size="2">
                  {BANNERS.OVER_LIMIT_IN_GRACE(gracePeriodEndDate)}{' '}
                  <ReviewUsageLink /> or{' '}
                  <PurchaseLicenseLink />.
                </Text>
              </Flex>
            </Flex>
          </Callout.Root>
        )}
        {throttlingStatus === 'OVER_LIMITS_GRACE_PERIOD_ENDED' && (
          <Callout.Root color="red" size="1">
            <Flex gap="2" align="start">
              <AlertCircle size={16} style={{flexShrink: 0, marginTop: 2}} aria-label="Error" />
              <Text size="2">
                {BANNERS.OVER_LIMIT_END_GRACE}{' '}
                <ReviewUsageLink /> or{' '}
                <PurchaseLicenseLink />.
              </Text>
            </Flex>
          </Callout.Root>
        )}
        {throttlingStatus === 'BELOW_LIMITS_GRACE_PERIOD_ENDED' && !belowLimitDismissed && (
          <Callout.Root color="orange" size="1">
            <Flex gap="2" align="start" style={{width: '100%'}}>
              <AlertTriangle size={16} style={{flexShrink: 0, marginTop: 2}} aria-label="Warning" />
              <Text size="2" style={{flex: 1}}>
                {BANNERS.BELOW_LIMIT_END_GRACE}{' '}
                <ReviewUsageLink /> or{' '}
                <PurchaseLicenseLink />.
              </Text>
              <Button
                variant="ghost"
                size="1"
                color="orange"
                style={{flexShrink: 0, padding: '2px'}}
                onClick={dismissBelowLimitOutofGrace}
                aria-label="Dismiss"
              >
                <X size={14} />
              </Button>
            </Flex>
          </Callout.Root>
        )}
      </>
    );
  } else {
    return (
      <>
        {throttlingStatus === 'NON_ADMIN_OVER_LIMITS_GRACE_PERIOD_ENDED' && (
          <Callout.Root color="red" size="1">
            <Flex gap="2" align="start">
              <AlertCircle size={16} style={{flexShrink: 0, marginTop: 2}} aria-label="Error" />
              <Text size="2">{BANNERS.THROTTLING_NON_ADMIN}</Text>
            </Flex>
          </Callout.Root>
        )}
        {throttlingStatus === 'NEAR_LIMITS_NON_ADMIN' && (
          <Callout.Root color="orange" size="1">
            <Flex gap="2" align="start">
              <AlertTriangle size={16} style={{flexShrink: 0, marginTop: 2}} aria-label="Warning" />
              <Text size="2">{BANNERS.NEARING_NON_ADMIN}</Text>
            </Flex>
          </Callout.Root>
        )}
      </>
    );
  }
}
