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
import {faExclamationCircle, faInfoCircle} from '@fortawesome/free-solid-svg-icons';
import {indexBy, pathOr, prop} from 'ramda';
import classNames from 'classnames';

import UIStrings from '../../../../constants/UIStrings';
import './UsageMetricsWithCircuitBreaker.scss';

const {
  WELCOME: {
    USAGE: {
      CIRCUIT_BREAKER,
      CARD_LINK_OSS,
      CARD_LINK_PRO,
      CARD_LINK_PRO_STARTER}}} = UIStrings;

const {
  TOTAL_COMPONENTS,
  UNIQUE_LOGINS,
  REQUESTS_PER_MINUTE,
  REQUESTS_PER_DAY,
  PERCENTAGE} = CIRCUIT_BREAKER;

const SOFT_LIMIT = 'SOFT_LIMIT';
const STARTER_HARD_LIMIT = 'STARTER_HARD_LIMIT';

function Card({card, usage}) {
  const {METRIC_NAME_PRO_POSTGRESQL, SUB_TITLE_PRO_POSTGRESQL, TITLE, TITLE_PRO_POSTGRESQL} = card;
  const cardData = usage.find(m => m.metricName === METRIC_NAME_PRO_POSTGRESQL);
  const {metricValue} = cardData;

  return <NxCard aria-label={TITLE_PRO_POSTGRESQL ?? TITLE}>
    <NxCard.Header>
      <NxH3>{TITLE_PRO_POSTGRESQL ?? TITLE}</NxH3>
    </NxCard.Header>
    <NxCard.Content>
      <NxCard.Text>
        <div className="nxrm-label">
          <span>{metricValue.toLocaleString()}</span>
          <span>{SUB_TITLE_PRO_POSTGRESQL ?? ''}</span>
        </div>
      </NxCard.Text>
    </NxCard.Content>
  </NxCard>
};

function CardWithThreshold({card, usage, link, tooltip, edition}) {
  const {AGGREGATE_PERIOD_30_D, HIGHEST_RECORDED_COUNT, THRESHOLD, METRIC_NAME, SUB_TITLE, TITLE} = card;
  const cardData = usage.find(m => m.metricName === METRIC_NAME);
  const {aggregates, limits, metricValue} = cardData;
  const softLimitValue = pathOr(0, [SOFT_LIMIT, 'limitValue'], indexBy(prop('limitName'), limits));
  const exceedsWarningLimit = metricValue >= softLimitValue * PERCENTAGE;
  const exceedsDangerLimit = metricValue >= softLimitValue;
  const highestRecordedCount = pathOr(0, [AGGREGATE_PERIOD_30_D, 'value'], indexBy(prop('period'), aggregates));
  const showErrorIcon = highestRecordedCount >= softLimitValue;
  const meterClassNames = classNames({
    'nxrm-meter-warning' : exceedsWarningLimit && !exceedsDangerLimit,
    'nxrm-meter-danger' : exceedsDangerLimit
  });
  const errorIconClassNames = classNames({
    'recorded-count-with-error-icon': showErrorIcon
  });

  return <NxCard aria-label={TITLE}>
    <NxCard.Header>
      <NxH3>
        {TITLE}
        <NxTooltip title={tooltip(softLimitValue.toLocaleString(), edition)}>
          <NxFontAwesomeIcon icon={faInfoCircle}/>
        </NxTooltip>
      </NxH3>
    </NxCard.Header>
    <NxCard.Content>
      <NxCard.Text>
        <NxMeter className={meterClassNames}
                 data-testid="meter"
                 value={metricValue}
                 max={softLimitValue}>
          {`${metricValue.toLocaleString()} out of ${softLimitValue.toLocaleString()}`}
        </NxMeter>
        <div className="nxrm-label-container">
          <div className="nxrm-label start">
            <span>{metricValue.toLocaleString()}</span>
            <span>{SUB_TITLE}</span>
          </div>
          <div className="nxrm-label end">
            <span>{softLimitValue.toLocaleString()}</span>
            <span>{THRESHOLD}</span>
          </div>
        </div>
      </NxCard.Text>
      <NxCard.Text className="nxrm-highest-records">
        <span className={errorIconClassNames}>
          {showErrorIcon && <NxFontAwesomeIcon icon={faExclamationCircle}/>}
          {highestRecordedCount.toLocaleString()}
        </span>
        <span>{HIGHEST_RECORDED_COUNT}</span>
        {exceedsWarningLimit && <NxTextLink external href={link.URL}>{link.TEXT}</NxTextLink>}
      </NxCard.Text>
    </NxCard.Content>
  </NxCard>
};

function CardWithHardLimitThreshold({card, usage, link, tooltip, edition}) {
  const {AGGREGATE_PERIOD_30_D, HIGHEST_RECORDED_COUNT, THRESHOLD, METRIC_NAME, SUB_TITLE, TITLE} = card;
  const cardData = usage.find(m => m.metricName === METRIC_NAME);
  const {aggregates, metricValue, limits} = cardData;
  const hardLimitValue = limits.find(l => l.limitName === STARTER_HARD_LIMIT).limitValue;
  const exceedsWarningLimit = metricValue >= hardLimitValue * PERCENTAGE;
  const exceedsDangerLimit = metricValue >= hardLimitValue;
  const highestRecordedCount = pathOr(0, [AGGREGATE_PERIOD_30_D, 'value'], indexBy(prop('period'), aggregates));
  const showErrorIcon = highestRecordedCount >= hardLimitValue;
  const meterClassNames = classNames('pro-starter-edition', {
    'nxrm-meter-warning' : exceedsWarningLimit && !exceedsDangerLimit,
    'nxrm-meter-danger' : exceedsDangerLimit
  });
  const errorIconClassNames = classNames( 'pro-starter-edition', {
    'recorded-count-with-error-icon': showErrorIcon
  });

  return <NxCard aria-label={TITLE}>
    <NxCard.Header>
      <NxH3>
        {TITLE}
        <NxTooltip title={tooltip(hardLimitValue.toLocaleString(), edition)}>
          <NxFontAwesomeIcon icon={faInfoCircle}/>
        </NxTooltip>
      </NxH3>
    </NxCard.Header>
    <NxCard.Content>
      <NxCard.Text>
        <NxMeter className={meterClassNames}
                 data-testid="meter"
                 value={metricValue}
                 max={hardLimitValue}>
          {`${metricValue.toLocaleString()} out of ${hardLimitValue.toLocaleString()}`}
        </NxMeter>
        <div className="nxrm-label-container">
          <div className="nxrm-label start">
            <span>{metricValue.toLocaleString()}</span>
            <span>{SUB_TITLE}</span>
          </div>
          <div className="nxrm-label end">
            <span>{hardLimitValue.toLocaleString()}</span>
            <span>{THRESHOLD}</span>
          </div>
        </div>
      </NxCard.Text>
      <NxCard.Text className="nxrm-highest-records">
        <span className={errorIconClassNames}>
          {showErrorIcon && <NxFontAwesomeIcon icon={faExclamationCircle}/>}
          {highestRecordedCount.toLocaleString()}
        </span>
        <span>{HIGHEST_RECORDED_COUNT}</span>
        {exceedsWarningLimit && <NxTextLink external href={link.URL}>{link.TEXT}</NxTextLink>}
      </NxCard.Text>
    </NxCard.Content>
  </NxCard>
};

function CardWithoutThreshold({card, usage, tooltip}) {
  const {AGGREGATE_PERIOD_24_H, AGGREGATE_PERIOD_30_D, HIGHEST_RECORDED_COUNT, METRIC_NAME, SUB_TITLE, TITLE} = card;
  const cardData = usage.find(m => m.metricName === METRIC_NAME);
  const {aggregates} = cardData;
  const metricValue = pathOr(0, ['metricValue'], cardData);
  const peakRequestsLast24H = pathOr(0, [AGGREGATE_PERIOD_24_H, 'value'], indexBy(prop('period'), aggregates));
  const highestRecordedCount = pathOr(0, [AGGREGATE_PERIOD_30_D, 'value'], indexBy(prop('period'), aggregates));

  return <NxCard aria-label={TITLE}>
    <NxCard.Header>
      <NxH3>
        {TITLE}
        <NxTooltip title={tooltip}>
          <NxFontAwesomeIcon icon={faInfoCircle}/>
        </NxTooltip>
      </NxH3>
    </NxCard.Header>
    <NxCard.Content>
      <NxCard.Text>
        <div className="nxrm-label-container no-meter">
          <div className="nxrm-label start">
            <span>{TITLE === UNIQUE_LOGINS.TITLE ? metricValue.toLocaleString() : peakRequestsLast24H.toLocaleString()}</span>
            <span>{SUB_TITLE}</span>
          </div>
        </div>
      </NxCard.Text>
      <NxCard.Text className="nxrm-highest-records">
        <span>{highestRecordedCount.toLocaleString()}</span>
        <span>{HIGHEST_RECORDED_COUNT}</span>
      </NxCard.Text>
    </NxCard.Content>
  </NxCard>
};

export default function UsageMetricsWithCircuitBreaker() {
  const isProEdition = ExtJS.isProEdition();
  const isProStarterEdition = ExtJS.isProStarterEdition();
  const isPostgresql = ExtJS.state().getValue('datastore.isPostgresql');
  const usage = ExtJS.state().getValue('contentUsageEvaluationResult');

  if (isProEdition && isPostgresql) {
    return <>
      <Card key={TOTAL_COMPONENTS.TITLE} card={TOTAL_COMPONENTS} usage={usage}/>
      <Card key={REQUESTS_PER_MINUTE.TITLE} card={REQUESTS_PER_MINUTE} usage={usage}/>
      <Card key={REQUESTS_PER_DAY.TITLE} card={REQUESTS_PER_DAY} usage={usage}/>
    </>
  } else if (isProEdition && !isPostgresql) {
    return <>
      <CardWithThreshold key={TOTAL_COMPONENTS.TITLE} card={TOTAL_COMPONENTS} usage={usage} link={CARD_LINK_PRO} tooltip={TOTAL_COMPONENTS.TOOLTIP} edition="PRO"/>
      <CardWithoutThreshold key={REQUESTS_PER_MINUTE.TITLE} card={REQUESTS_PER_MINUTE} usage={usage} tooltip={REQUESTS_PER_MINUTE.TOOLTIP_PRO}/>
      <CardWithThreshold key={REQUESTS_PER_DAY.TITLE} card={REQUESTS_PER_DAY} usage={usage} link={CARD_LINK_PRO} tooltip={REQUESTS_PER_DAY.TOOLTIP} edition="PRO"/>
    </>
  } else if (isProStarterEdition) {
    return <>
      <CardWithHardLimitThreshold key={TOTAL_COMPONENTS.TITLE} card={TOTAL_COMPONENTS} usage={usage} link={CARD_LINK_PRO_STARTER} tooltip={TOTAL_COMPONENTS.TOOLTIP} edition="PRO-STARTER"/>
      <CardWithoutThreshold key={UNIQUE_LOGINS.TITLE} card={UNIQUE_LOGINS} usage={usage} tooltip={UNIQUE_LOGINS.TOOLTIP_PRO_STARTER}/>
      <CardWithHardLimitThreshold key={REQUESTS_PER_DAY.TITLE} card={REQUESTS_PER_DAY} usage={usage} link={CARD_LINK_PRO_STARTER} tooltip={REQUESTS_PER_DAY.TOOLTIP} edition="PRO-STARTER"/>
    </>
  } else {
    return <>
      <CardWithThreshold key={TOTAL_COMPONENTS.TITLE} card={TOTAL_COMPONENTS} usage={usage} link={CARD_LINK_OSS} tooltip={TOTAL_COMPONENTS.TOOLTIP} edition="OSS"/>
      <CardWithoutThreshold key={UNIQUE_LOGINS.TITLE} card={UNIQUE_LOGINS} usage={usage} tooltip={UNIQUE_LOGINS.TOOLTIP}/>
      <CardWithThreshold key={REQUESTS_PER_DAY.TITLE} card={REQUESTS_PER_DAY} usage={usage} link={CARD_LINK_OSS} tooltip={REQUESTS_PER_DAY.TOOLTIP} edition="OSS"/>
    </>
  }
};
