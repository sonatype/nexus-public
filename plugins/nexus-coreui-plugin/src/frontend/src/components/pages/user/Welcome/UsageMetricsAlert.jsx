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
import {NxWarningAlert} from '@sonatype/react-shared-components';
import {ExtJS} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';

import './UsageMetricsAlert.scss';

const {WELCOME: {USAGE: {SOFT_LIMIT_ALERT_CONTENT}}} = UIStrings;
const SOFT_LIMIT = 'SOFT_LIMIT';
const PEAK_REQUESTS_PER_DAY = 'peak_requests_per_day';
const COMPONENT_TOTAL_COUNT = 'component_total_count';
const SUCCESSFUL_LOGINS = 'successful_last_30d';

export default function UsageMetricsAlert(props) {
  const metrics = ExtJS.state().getValue('contentUsageEvaluationResult');

  function close() {
    Ext.get('nxrm-warning-alert').setDisplayed(false);
    Ext.getCmp('nxrm-react-footer-container').updateLayout();
  }

  const WarningContent = function({metric}) {
    return metric.limitLevel === SOFT_LIMIT ?
        <>
          {metric.metricName === PEAK_REQUESTS_PER_DAY && <div>{SOFT_LIMIT_ALERT_CONTENT.REQUESTS_PER_DAY}</div>}
          {metric.metricName === COMPONENT_TOTAL_COUNT && <div>{SOFT_LIMIT_ALERT_CONTENT.TOTAL_COMPONENTS}</div>}
          {metric.metricName === SUCCESSFUL_LOGINS && <div>{SOFT_LIMIT_ALERT_CONTENT.UNIQUE_LOGINS}</div>}
        </> : null;
  };

  return <NxWarningAlert onClose={props.onClose} role="alert">
    <div>
      {metrics.map(m => <WarningContent key={m.metricName} metric={m}/>)}
    </div>
  </NxWarningAlert>
}
