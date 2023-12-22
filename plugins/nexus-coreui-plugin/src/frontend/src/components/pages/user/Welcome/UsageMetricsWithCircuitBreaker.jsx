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
import {ExtJS} from '@sonatype/nexus-ui-plugin';
import {
  NxCard,
  NxFontAwesomeIcon,
  NxH3,
  NxMeter,
  NxTextLink,
  NxTooltip} from '@sonatype/react-shared-components';
import {faExclamationCircle, faExclamationTriangle, faInfoCircle} from "@fortawesome/free-solid-svg-icons";
import {isEmpty} from 'ramda';

import UIStrings from '../../../../constants/UIStrings';
import './UsageMetricsWithCircuitBreaker.scss';

const {
  WELCOME: {
    USAGE: {
      CIRCUIT_BREAKER,
      UPGRADE_TO_PRO}}} = UIStrings;

const {
  TOTAL_COMPONENTS,
  UNIQUE_LOGINS,
  REQUESTS_PER_DAY} = CIRCUIT_BREAKER;

const HARD_LIMIT = 'HARD_LIMIT';
const SOFT_LIMIT = 'SOFT_LIMIT';

function Card({card, usage}) {
  const {HIGHEST_RECORDED_COUNT, METRIC_NAME_PRO, SUB_TITLE, TITLE} = card;
  const cardData = usage?.find(m => m.metricName === METRIC_NAME_PRO);
  const metricValue =
      cardData && typeof cardData === 'object' && 'metricValue' in cardData ? cardData.metricValue : 0;

  return (
    <NxCard aria-label={TITLE}>
      <NxCard.Header>
        <NxH3>{TITLE}</NxH3>
      </NxCard.Header>
      <NxCard.Content>
        <NxCard.Text>
          <div className="nxrm-label">
            <span>{metricValue.toLocaleString()}</span>
            <span>{card === TOTAL_COMPONENTS ? SUB_TITLE : HIGHEST_RECORDED_COUNT}</span>
          </div>
        </NxCard.Text>
      </NxCard.Content>
    </NxCard>
  )
};

function CardWithMeter({card, usage}) {
  const {AGGREGATE_NAME, HIGHEST_RECORDED_COUNT, LIMIT, METRIC_NAME, SUB_TITLE, TITLE, TOOLTIP} = card;
  const cardData = usage?.find(m => m.metricName === METRIC_NAME);
  const {aggregates, limitLevel, limits, metricValue} = cardData;
  const isHardLimit = limitLevel === HARD_LIMIT;
  const isSoftLimit = limitLevel === SOFT_LIMIT;
  const hardLimit = limits.find(l => l.limitName === HARD_LIMIT);
  const softLimit = limits.find(l => l.limitName === SOFT_LIMIT);
  const highestRecordedCount = aggregates.find(a => a.name === AGGREGATE_NAME).value;
  const showErrorIcon = highestRecordedCount >= hardLimit.limitValue;
  const showWarningIcon = highestRecordedCount >= softLimit.limitValue;

  return (
    <NxCard aria-label={TITLE}>
      <NxCard.Header>
        <NxH3>
          {TITLE}
          <NxTooltip title={TOOLTIP}>
            <NxFontAwesomeIcon icon={faInfoCircle}/>
          </NxTooltip>
        </NxH3>
      </NxCard.Header>
      <NxCard.Content>
        <NxCard.Text>
          <NxMeter className={isHardLimit ? 'nxrm-hard-limit' : isSoftLimit ? 'nxrm-soft-limit' : ''}
                   data-testid="meter"
                   value={metricValue}
                   max={hardLimit.limitValue}>
            {`${metricValue.toLocaleString()} out of ${hardLimit.limitValue.toLocaleString()}`}
          </NxMeter>
          <div className="nxrm-label-container">
            <div className="nxrm-label start">
              <span>{metricValue.toLocaleString()}</span>
              <span>{SUB_TITLE}</span>
            </div>
            <div className="nxrm-label end">
              <span>{hardLimit.limitValue.toLocaleString()}</span>
              <span>{LIMIT}</span>
            </div>
          </div>
        </NxCard.Text>
        <NxCard.Text className="nxrm-highest-records">
          <span>{HIGHEST_RECORDED_COUNT}</span>
          <span className={`recorded-count${showErrorIcon ? '-hard-limit' : showWarningIcon ? '-soft-limit' : ''}`}>
            {
              showErrorIcon ? <NxFontAwesomeIcon icon={faExclamationCircle}/> :
              showWarningIcon ? <NxFontAwesomeIcon icon={faExclamationTriangle}/> : null
            }
            {highestRecordedCount.toLocaleString()}
          </span>
          <NxTextLink external href={UPGRADE_TO_PRO.URL}>{UPGRADE_TO_PRO.TEXT}</NxTextLink>
        </NxCard.Text>
      </NxCard.Content>
    </NxCard>
  )
};

export default function UsageMetricsWithCircuitBreaker() {
  const isProEdition = ExtJS.isProEdition();
  const proMetricsCards = [TOTAL_COMPONENTS, REQUESTS_PER_DAY];
  const ossMetricsCards = [TOTAL_COMPONENTS, UNIQUE_LOGINS, REQUESTS_PER_DAY];
  const usage = ExtJS.state().getValue('contentUsageEvaluationResult');

  return !isEmpty(usage) && (isProEdition
      ? proMetricsCards.map(c => <Card key={c.TITLE} card={c} usage={usage}/>)
      : ossMetricsCards.map(c => <CardWithMeter key={c.TITLE} card={c} usage={usage}/>))
};
