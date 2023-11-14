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
import {NxButtonBar, NxErrorAlert, NxWarningAlert} from '@sonatype/react-shared-components';
import {ExtJS} from '@sonatype/nexus-ui-plugin';
import {isEmpty} from 'ramda';

import UIStrings from '../../../../constants/UIStrings';

import './UsageMetricsAlert.scss';

const {WELCOME: {USAGE: {LEARN_ABOUT_PRO, HARD_LIMIT_ALERT_CONTENT, SOFT_LIMIT_ALERT_CONTENT}}} = UIStrings;
const HARD_LIMIT = 'HARD_LIMIT';
const SOFT_LIMIT = 'SOFT_LIMIT';
const PEAK_REQUESTS_PER_DAY = 'peak_requests_per_day';
const COMPONENT_TOTAL_COUNT = 'component_total_count';
const SUCCESSFUL_LOGINS = 'successful_last_30d';

const AlertContent = function({metric, content}) {
  return <>
    {metric.metricName === PEAK_REQUESTS_PER_DAY && <p>{content.REQUESTS_PER_DAY}</p>}
    {metric.metricName === COMPONENT_TOTAL_COUNT && <p>{content.TOTAL_COMPONENTS}</p>}
    {metric.metricName === SUCCESSFUL_LOGINS && <p>{content.UNIQUE_LOGINS}</p>}
  </>
  };

export default function UsageMetricsAlert(props) {
  const metrics = ExtJS.state().getValue('contentUsageEvaluationResult');
  const hardLimitMetrics = metrics.filter(m => m.limitLevel === HARD_LIMIT);
  const softLimitMetrics = metrics.filter(m => m.limitLevel === SOFT_LIMIT);

  return <>
    {!isEmpty(hardLimitMetrics) &&
      <NxErrorAlert>
        <div>
          {hardLimitMetrics.map(m => <AlertContent key={m.metricName} metric={m} content={HARD_LIMIT_ALERT_CONTENT}/>)}
        </div>
        <NxButtonBar>
          <a className="nxrm-learn-about-link"
            href={LEARN_ABOUT_PRO.URL}
            target="_blank">
            {LEARN_ABOUT_PRO.TEXT}
          </a>
        </NxButtonBar>
      </NxErrorAlert>}
    {!isEmpty(softLimitMetrics) &&
      <NxWarningAlert onClose={props.onClose} role="alert">
        <div>
          {softLimitMetrics.map(m => <AlertContent key={m.metricName} metric={m} content={SOFT_LIMIT_ALERT_CONTENT}/>)}
        </div>
      </NxWarningAlert>}
  </>
};
