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
import {NxButtonBar, NxErrorAlert, NxTextLink, NxWarningAlert} from '@sonatype/react-shared-components';
import {ExtJS} from '@sonatype/nexus-ui-plugin';
import {isEmpty, replace} from 'ramda';

import UIStrings from '../../../../constants/UIStrings';

import './UsageMetricsAlert.scss';

const {
  WELCOME: {
    USAGE: {
      CIRCUIT_BREAKER: {
        PERCENTAGE,
        TOTAL_COMPONENTS,
        REQUESTS_PER_DAY,
        STARTER_THRESHOLD
      },
      ALERTS: {
        EXCEEDING_THRESHOLDS,
        APPROACHING_THRESHOLDS,
        LEARN_ABOUT_PRO,
        REVIEW_YOUR_USAGE,
        UPGRADING_PRO,
        SUFFIX}}}} = UIStrings;

const MessageContent = function({metricMessage, threshold}){
  const prefix = NX.I18n.get(metricMessage.PREFIX);
  const mid = NX.I18n.get(metricMessage.MID);
  const suffix = NX.I18n.get(SUFFIX);

  return <p>
    {replace('{}', threshold, prefix)}
    <NxTextLink external href={REVIEW_YOUR_USAGE.URL}>{REVIEW_YOUR_USAGE.TEXT}</NxTextLink>
    {mid}
    <NxTextLink external href={UPGRADING_PRO.URL}>{UPGRADING_PRO.TEXT}</NxTextLink>
    {suffix}
  </p>
}

const AlertContent = function({metric, content}) {
  const threshold = metric.thresholds.find(l => l.thresholdName === STARTER_THRESHOLD).thresholdValue;
  return <>
    {metric.metricName === REQUESTS_PER_DAY.METRIC_NAME && <MessageContent metricMessage={content.REQUESTS_PER_DAY}
                                                                           threshold={threshold.toLocaleString()}/>}
    {metric.metricName === TOTAL_COMPONENTS.METRIC_NAME && <MessageContent metricMessage={content.TOTAL_COMPONENTS}
                                                                           threshold={threshold.toLocaleString()}/>}
  </>
};

export default function UsageMetricsAlert({onClose}) {
  const metrics = ExtJS.state().getValue('contentUsageEvaluationResult', []);
  const isProStarterEdition = ExtJS.isProStarterEdition();

  const approachingThresholdMetrics = metrics.filter(m => {
    const thresholds = m.thresholds ?? [];
    const threshold = thresholds.find(l => l.thresholdName === STARTER_THRESHOLD);
    if (threshold && threshold.thresholdValue && m.metricName && m.metricValue && !isNaN(threshold.thresholdValue)) {
      return (m.metricName === REQUESTS_PER_DAY.METRIC_NAME && m.metricValue >= threshold.thresholdValue * PERCENTAGE) ||
          (m.metricName === TOTAL_COMPONENTS.METRIC_NAME && m.metricValue >= threshold.thresholdValue * PERCENTAGE)
    }
  });

  const exceedingThresholdMetrics = metrics.filter(m => {
    const thresholds = m.thresholds ?? [];
    const threshold = thresholds.find(l => l.thresholdName === STARTER_THRESHOLD);
    if (m.metricName && m.usageLevel && threshold && threshold.thresholdValue && !isNaN(threshold.thresholdValue)) {
      return (m.metricName === REQUESTS_PER_DAY.METRIC_NAME && m.usageLevel === STARTER_THRESHOLD) ||
          (m.metricName === TOTAL_COMPONENTS.METRIC_NAME && m.usageLevel === STARTER_THRESHOLD)
    }
  });

  const showApproachingThresholdAlert = isEmpty(exceedingThresholdMetrics) && !isEmpty(approachingThresholdMetrics);

  return isProStarterEdition && <>
    {!isEmpty(exceedingThresholdMetrics) && <NxErrorAlert className="nxrm-exceeding-threshold-alert">
      <div>
        {exceedingThresholdMetrics.map(m => <AlertContent key={m.metricName} metric={m} content={EXCEEDING_THRESHOLDS}/>)}
      </div>
      <NxButtonBar>
        <a className="nxrm-learn-about-link"
           href={LEARN_ABOUT_PRO.URL}
           target="_blank">
          {LEARN_ABOUT_PRO.TEXT}
        </a>
      </NxButtonBar>
    </NxErrorAlert>}
    {showApproachingThresholdAlert && <NxWarningAlert className="nxrm-approaching-threshold-alert" onClose={onClose} role="alert">
      <div>
        {approachingThresholdMetrics.map(m => <AlertContent key={m.metricName} metric={m} content={APPROACHING_THRESHOLDS}/>)}
      </div>
    </NxWarningAlert>}
  </>
};
