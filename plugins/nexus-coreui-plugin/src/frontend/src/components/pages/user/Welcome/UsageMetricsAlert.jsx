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
import {NxButtonBar, NxErrorAlert, NxTextLink} from '@sonatype/react-shared-components';
import {ExtJS} from '@sonatype/nexus-ui-plugin';
import {isEmpty} from 'ramda';

import UIStrings from '../../../../constants/UIStrings';

import './UsageMetricsAlert.scss';

const {
  WELCOME: {
    USAGE: {
      ALERTS: {
        HARD_LIMITS,
        LEARN_ABOUT_PRO,
        REVIEW_YOUR_USAGE,
        UPGRADING_PRO,
        SUFFIX}}}} = UIStrings;

const PEAK_REQUESTS_PER_DAY = 'peak_requests_per_day';
const COMPONENT_TOTAL_COUNT = 'component_total_count';
const REQUESTS_PER_DAY_HARD_LIMIT = 200_000;
const COMPONENT_COUNT_HARD_LIMIT = 120_000;

const MessageContent = function({metricMessage}){
  return <p>
    {metricMessage.PREFIX}
    <NxTextLink external href={REVIEW_YOUR_USAGE.URL}>{REVIEW_YOUR_USAGE.TEXT}</NxTextLink>
    {metricMessage.MID}
    <NxTextLink external href={UPGRADING_PRO.URL}>{UPGRADING_PRO.TEXT}</NxTextLink>
    {SUFFIX}
  </p>
}

const AlertContent = function({metric, content}) {
  return <>
    {metric.metricName === PEAK_REQUESTS_PER_DAY && <MessageContent metricMessage={content.REQUESTS_PER_DAY}/>}
    {metric.metricName === COMPONENT_TOTAL_COUNT && <MessageContent metricMessage={content.TOTAL_COMPONENTS}/>}
  </>
};

export default function UsageMetricsAlert() {
  const metrics = ExtJS.state().getValue('contentUsageEvaluationResult');
  const hardLimitMetrics = metrics.filter(
      m => (m.metricName === PEAK_REQUESTS_PER_DAY && m.metricValue >= REQUESTS_PER_DAY_HARD_LIMIT) ||
          (m.metricName === COMPONENT_TOTAL_COUNT && m.metricValue >= COMPONENT_COUNT_HARD_LIMIT));
  return <>
    {!isEmpty(hardLimitMetrics) && <NxErrorAlert>
      <div>
        {hardLimitMetrics.map(m => <AlertContent key={m.metricName} metric={m} content={HARD_LIMITS}/>)}
      </div>
      <NxButtonBar>
        <a className="nxrm-learn-about-link"
           href={LEARN_ABOUT_PRO.URL}
           target="_blank">
          {LEARN_ABOUT_PRO.TEXT}
        </a>
      </NxButtonBar>
    </NxErrorAlert>}
  </>
};
