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
  NxTooltip,
  NxTextLink,
  NxErrorStatusIndicator,
  NxPositiveStatusIndicator,
  NxTile} from '@sonatype/react-shared-components';
import {faExclamationCircle, faExclamationTriangle, faInfoCircle} from '@fortawesome/free-solid-svg-icons';
import {indexBy, pathOr, prop, replace} from 'ramda';
import classNames from 'classnames';

import UIStrings from '../../../../../constants/UIStrings';
import {helperFunctions} from '../../../../widgets/CELimits/UsageHelper';
import './UsageCenter.scss';

const {
  getMetricData,
  OVER_LIMITS,
  NEAR_LIMITS,
  UNDER_LIMITS
} = helperFunctions;

const {
  WELCOME: {
    USAGE: {
      MENU,
      HEADER,
      CARDS
    }}} = UIStrings;

const {
  TOTAL_COMPONENTS,
  UNIQUE_LOGINS,
  REQUESTS_PER_MINUTE,
  REQUESTS_PER_DAY,
  PERCENTAGE,
  COMMUNITY,
  CARD_PRO_LABELS: {
    THRESHOLD,
  },
  CARD_CE_LABELS: {
    USAGE_LIMIT
  },
  CARD_SHARED_LABELS: {
    PERIOD,
    VALUE,
    LAST_EXCEEDED_DATE_LABEL}} = CARDS;

function Card({card, usage}) {
  const {METRIC_NAME_PRO_POSTGRESQL, SUB_TITLE_PRO_POSTGRESQL, TITLE, TITLE_PRO_POSTGRESQL} = card;
  const {metricValue} = getMetricData(usage, METRIC_NAME_PRO_POSTGRESQL);

  return (
    <NxCard aria-label={TITLE_PRO_POSTGRESQL ?? TITLE}>
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
  );
}

function CardWithThreshold({card, usage, tooltip, edition, date}) {
  const {HIGHEST_RECORDED_COUNT, METRIC_NAME, SUB_TITLE, TITLE} = card;
  const {metricValue, thresholdValue, highestRecordedCount} = getMetricData(usage, METRIC_NAME);
  const thresholdLabel = edition === COMMUNITY ? USAGE_LIMIT : THRESHOLD;
  const approachingThreshold = metricValue >= thresholdValue * PERCENTAGE;
  const exceedingThreshold = metricValue >= thresholdValue;
  const showError = exceedingThreshold;
  const showWarning = approachingThreshold && !exceedingThreshold;
  const meterClassNames = classNames({
    'community-edition': edition === COMMUNITY,
    'nxrm-meter-approaching' : showWarning,
    'nxrm-meter-exceeding' : showError
  });
  const errorIconClassNames = classNames({
    'community-edition': edition === COMMUNITY,
    'recorded-count-with-error-icon': showError,
    'recorded-count-with-warning-icon': showWarning
  });

  return (
    <NxCard aria-label={TITLE}>
      <NxCard.Header>
        <NxH3>
          {TITLE}
          <NxTooltip title={replace('{}', thresholdValue.toLocaleString(), tooltip || '')}>
            <NxFontAwesomeIcon icon={faInfoCircle}/>
          </NxTooltip>
        </NxH3>
      </NxCard.Header>
      <NxCard.Content>
        <NxCard.Text>
          <NxMeter className={meterClassNames}
                   data-testid="meter"
                   value={metricValue}
                   max={thresholdValue}>
            {`${metricValue.toLocaleString()} out of ${thresholdValue.toLocaleString()}`}
          </NxMeter>
          <div className="nxrm-label-container">
            <div className="nxrm-label start">
              <span className={errorIconClassNames}>
                {showError && <NxFontAwesomeIcon icon={faExclamationCircle}/>}
                {showWarning && <NxFontAwesomeIcon icon={faExclamationTriangle}/>}
                <span className={!showError ? 'highest-recorded-count' : ''}>
                  {metricValue.toLocaleString()}
                </span>
              </span>
              <span>{SUB_TITLE}</span>
            </div>
            <div className="nxrm-label end">
              <span>{thresholdValue.toLocaleString()}</span>
              <span>{thresholdLabel}</span>
            </div>
          </div>
        </NxCard.Text>
        <NxCard.Text className="nxrm-highest-records">
          <span>{highestRecordedCount.toLocaleString()}</span>
          <span>{HIGHEST_RECORDED_COUNT}</span>
        </NxCard.Text>
        {date && (
          <NxCard.Text className="nxrm-date-exceeded">
            <span>{date}</span>
            <span>{LAST_EXCEEDED_DATE_LABEL}</span>
          </NxCard.Text>
        )}
      </NxCard.Content>
    </NxCard>
  );
}

function CardWithoutThreshold({card, usage, tooltip}) {
  const {AGGREGATE_PERIOD_24_H, AGGREGATE_PERIOD_30_D, HIGHEST_RECORDED_COUNT, METRIC_NAME, SUB_TITLE, TITLE} = card;
  const {metricValue, aggregates} = getMetricData(usage, METRIC_NAME);
  const peakRequestsLast24H = pathOr(0, [AGGREGATE_PERIOD_24_H, VALUE], indexBy(prop(PERIOD), aggregates));
  const highestRecordedCount = pathOr(0, [AGGREGATE_PERIOD_30_D, VALUE], indexBy(prop(PERIOD), aggregates));

  return (
    <NxCard aria-label={TITLE}>
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
  );
}

function UsageCenterHeader() {
  const isProEdition = ExtJS.isProEdition();
  const isCommunityEdition = ExtJS.state().getEdition() === COMMUNITY;
  const throttlingStatus = ExtJS.useState(() => ExtJS.state().getValue('nexus.community.throttlingStatus'));

  const utmParams = {
    utm_medium: 'product',
    utm_source: 'nexus_repo_community',
    utm_campaign: 'repo_community_usage'
  };

  const reviewUsageLink = `https://links.sonatype.com/products/nxrm3/docs/review-usage?${new URLSearchParams(utmParams).toString()}`;

  return (<>
    {isCommunityEdition &&
      <NxTile.Header>
        <NxTile.Headings>
          <NxTile.HeaderTitle>
            <span className="nx-h2">{MENU.TITLE}</span>
            {throttlingStatus === OVER_LIMITS && 
              <NxErrorStatusIndicator>{HEADER.OVER_LIMITS.STATUS_INDICATOR}</NxErrorStatusIndicator>
            }
            {throttlingStatus === NEAR_LIMITS && 
              <NxPositiveStatusIndicator className="warning-status-indicator">{HEADER.APPROACHING_LIMITS.STATUS_INDICATOR}</NxPositiveStatusIndicator>
            }
            {throttlingStatus === UNDER_LIMITS && 
              <NxPositiveStatusIndicator>{HEADER.UNDER_LIMITS.STATUS_INDICATOR}</NxPositiveStatusIndicator>
            }
          </NxTile.HeaderTitle>
          <NxTile.HeaderSubtitle>
            <div className="usage-text">
              {MENU.SUB_TEXT}{' '}
              <NxTextLink className="usage-center-learn-more" target='_blank' href={reviewUsageLink}>Learn more about the usage center</NxTextLink>.
            </div>
            <span className="subtitle">{MENU.SUB_TITLE}</span>
          </NxTile.HeaderSubtitle>
        </NxTile.Headings>
      </NxTile.Header>
    }
    {isProEdition && 
      <NxTile.Header>
        <NxTile.HeaderTitle>
          <span className="nx-h2">{MENU.TITLE}</span>
        </NxTile.HeaderTitle>
        <NxTile.HeaderSubtitle>
          <div className="usage-text">{HEADER.PRO_POSTGRES.TEXT}</div>
          <span className="subtitle">{MENU.SUB_TITLE}</span>
        </NxTile.HeaderSubtitle>
      </NxTile.Header>
    }
  </>);
}


export default function UsageCenter() {
  const isProEdition = ExtJS.isProEdition();
  const isCommunityEdition = ExtJS.state().getEdition() === COMMUNITY;
  const usage = ExtJS.state().getValue('contentUsageEvaluationResult', []);
  const componentCountLimitDateLastExceeded = ExtJS.state().getValue('nexus.community.componentCountLimitDateLastExceeded');
  const requestPer24HoursLimitDateLastExceeded = ExtJS.state().getValue('nexus.community.requestPer24HoursLimitDateLastExceeded');

  const isHa = ExtJS.state().getValue('nexus.datastore.clustered.enabled');

  const formatDate = (dateString) => {
    const date = new Date(dateString);
    if (isNaN(date)) {
      return '';
    }
    return new Intl.DateTimeFormat('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: 'numeric',
      minute: 'numeric',
      hour12: true
    }).format(date);
  };

  const componentFormattedDate = componentCountLimitDateLastExceeded
    ? formatDate(componentCountLimitDateLastExceeded)
    : '';

  const requestFormattedDate = requestPer24HoursLimitDateLastExceeded
    ? formatDate(requestPer24HoursLimitDateLastExceeded)
    : '';
  
  return !isHa && (
    <div className="nxrm-usage-center" id="nxrm-usage-center">
      <NxTile>
        <UsageCenterHeader/>
        {isProEdition &&
          <NxCard.Container>
            <Card key={TOTAL_COMPONENTS.TITLE} card={TOTAL_COMPONENTS} usage={usage}/>
            <Card key={REQUESTS_PER_MINUTE.TITLE} card={REQUESTS_PER_MINUTE} usage={usage}/>
            <Card key={REQUESTS_PER_DAY.TITLE} card={REQUESTS_PER_DAY} usage={usage}/>
          </NxCard.Container> 
        }
        {isCommunityEdition &&
          <NxCard.Container>
            <CardWithThreshold 
              key={TOTAL_COMPONENTS.TITLE} 
              card={TOTAL_COMPONENTS} 
              usage={usage} 
              tooltip={TOTAL_COMPONENTS.TOOLTIP_CE} 
              edition={COMMUNITY} 
              date={componentFormattedDate}
            />
            <CardWithThreshold 
              key={REQUESTS_PER_DAY.TITLE} 
              card={REQUESTS_PER_DAY} 
              usage={usage} 
              tooltip={REQUESTS_PER_DAY.TOOLTIP_CE} 
              edition={COMMUNITY} 
              date={requestFormattedDate}
            />
            <CardWithoutThreshold 
              key={UNIQUE_LOGINS.TITLE} 
              card={UNIQUE_LOGINS} 
              usage={usage} 
              tooltip={UNIQUE_LOGINS.TOOLTIP_CE}
            />
          </NxCard.Container>
        }
      </NxTile>
    </div>
  );
}
