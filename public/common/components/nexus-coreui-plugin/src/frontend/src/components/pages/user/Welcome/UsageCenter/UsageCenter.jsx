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
import {replace} from 'ramda';
import classNames from 'classnames';

import UIStrings from '../../../../../constants/UIStrings';
import {helperFunctions, useTestOverrideDetection, STORAGE_KEY_CE_THROTTLING_STATUS, STORAGE_KEY_CE_COMPONENTS, STORAGE_KEY_CE_REQUESTS} from '../../../../widgets/SystemStatusAlerts/CELimits/UsageHelper';
import './UsageCenter.scss';

const {
  getMetricData,
  OVER_LIMITS,
  NEAR_LIMITS,
  UNDER_LIMITS,
  useCommunityEdition
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
  REQUESTS_PER_DAY,
  MONTHLY_REQUESTS,
  PERCENTAGE,
  COMMUNITY,
  CARD_PRO_LABELS: {
    THRESHOLD,
  },
  CARD_CE_LABELS: {
    USAGE_LIMIT
  },
  CARD_SHARED_LABELS: {
    LAST_EXCEEDED_DATE_LABEL}} = CARDS;

// Module-level date formatter for CE limit exceeded dates (performance optimization)
const DATE_FORMATTER = new Intl.DateTimeFormat('en-US', {
  month: 'short',
  day: 'numeric',
  year: 'numeric',
  hour: 'numeric',
  minute: 'numeric',
  hour12: true
});

function Card({card, usage}) {
  const isPostgres = ExtJS.state().getValue('datastore.isPostgresql');
  const {METRIC_NAME, METRIC_NAME_PRO_POSTGRESQL, SUB_TITLE_PRO_POSTGRESQL, TITLE, TITLE_PRO_POSTGRESQL} = card;
  const {metricValue} = getMetricData(usage, isPostgres ? METRIC_NAME_PRO_POSTGRESQL : METRIC_NAME);

  const displayTitle = isPostgres ? (TITLE_PRO_POSTGRESQL ?? TITLE) : TITLE;

  return (
    <NxCard aria-label={displayTitle}>
      <NxCard.Header>
        <NxH3>{displayTitle}</NxH3>
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
  const tooltipText = typeof tooltip === 'function' ? tooltip(thresholdValue) : replace('{}', thresholdValue.toLocaleString(), tooltip || '');

  return (
    <NxCard aria-label={TITLE}>
      <NxCard.Header>
        <NxH3>
          {TITLE}
          <NxTooltip title={tooltipText}>
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

function MonthlyMetricsCard({usage}) {
  const {metricValue: totalMetricValue} = getMetricData(usage, MONTHLY_REQUESTS.TOTAL.METRIC_NAME);
  const {metricValue: averageMetricValue} = getMetricData(usage, MONTHLY_REQUESTS.AVERAGE.METRIC_NAME);
  const {metricValue: highestMetricValue} = getMetricData(usage, MONTHLY_REQUESTS.HIGHEST.METRIC_NAME);

  const currentMonth = React.useMemo(
    () => new Intl.DateTimeFormat('en-US', { month: 'long' }).format(new Date()),
    []
  );

  return (
    <NxCard aria-label={MONTHLY_REQUESTS.TITLE}>
      <NxCard.Header>
        <NxH3>
          {MONTHLY_REQUESTS.TITLE}
          <NxTooltip title={MONTHLY_REQUESTS.TOOLTIP}>
            <NxFontAwesomeIcon icon={faInfoCircle}/>
          </NxTooltip>
        </NxH3>
      </NxCard.Header>
      <NxCard.Content>
        <NxCard.Text className="nxrm-highest-records">
          <span>{totalMetricValue.toLocaleString()}</span>
          <span>{MONTHLY_REQUESTS.TOTAL.SUB_TITLE(currentMonth)}</span>
        </NxCard.Text>
        <NxCard.Text className="nxrm-highest-records">
          <span>{averageMetricValue.toLocaleString()}</span>
          <span>{MONTHLY_REQUESTS.AVERAGE.SUB_TITLE}</span>
        </NxCard.Text>
        <NxCard.Text className="nxrm-highest-records">
          <span>{highestMetricValue.toLocaleString()}</span>
          <span>{MONTHLY_REQUESTS.HIGHEST.SUB_TITLE}</span>
        </NxCard.Text>
      </NxCard.Content>
    </NxCard>
  );
}

function UsageCenterHeader() {
  const isProEdition = ExtJS.isProEdition();
  const isCommunityEdition = useCommunityEdition();
  const throttlingStatus = ExtJS.useState(helperFunctions.useThrottlingStatusValue);

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
              <NxTextLink className="usage-center-learn-more" external href={reviewUsageLink}>Learn more about the usage center</NxTextLink>.
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
  // Force re-render when test overrides change (Test Hub scenarios)
  useTestOverrideDetection([
    STORAGE_KEY_CE_THROTTLING_STATUS,
    STORAGE_KEY_CE_COMPONENTS,
    STORAGE_KEY_CE_REQUESTS,
  ]);

  const isProEdition = ExtJS.isProEdition();
  const isCommunityEdition = useCommunityEdition();
  const usage = ExtJS.state().getValue('contentUsageEvaluationResult', []);
  const componentCountLimitDateLastExceeded = ExtJS.state().getValue('nexus.community.componentCountLimitDateLastExceeded');
  const requestPer24HoursLimitDateLastExceeded = ExtJS.state().getValue('nexus.community.requestPer24HoursLimitDateLastExceeded');

  const formatDate = (dateString) => {
    const date = new Date(dateString);
    if (isNaN(date)) {
      return '';
    }
    return DATE_FORMATTER.format(date);
  };

  const componentFormattedDate = componentCountLimitDateLastExceeded
    ? formatDate(componentCountLimitDateLastExceeded)
    : '';

  const requestFormattedDate = requestPer24HoursLimitDateLastExceeded
    ? formatDate(requestPer24HoursLimitDateLastExceeded)
    : '';

  return (
    <div className="nxrm-usage-center" id="nxrm-usage-center">
      <NxTile>
        <UsageCenterHeader/>
        {isProEdition &&
          <NxCard.Container>
            <Card key={TOTAL_COMPONENTS.TITLE} card={TOTAL_COMPONENTS} usage={usage}/>
            <Card key={REQUESTS_PER_DAY.TITLE} card={REQUESTS_PER_DAY} usage={usage}/>
            <Card key={MONTHLY_REQUESTS.TITLE} card={MONTHLY_REQUESTS} usage={usage}/>

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
            <MonthlyMetricsCard 
              usage={usage} 
            />
          </NxCard.Container>
        }
      </NxTile>
    </div>
  );
}
