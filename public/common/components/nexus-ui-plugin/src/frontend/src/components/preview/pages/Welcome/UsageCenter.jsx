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
import {Card, Flex, Heading, Text, Progress, Box, Grid} from '@radix-ui/themes';
import {AlertCircle, AlertTriangle, Info} from 'lucide-react';
import { ExtJS } from '../../../../interface/ExtJS';
import {Tooltip} from '../../shared/Tooltip';

import UIStrings from '../../../../constants/UIStrings';
import {helperFunctions} from '../../../widgets/SystemStatusAlerts/CELimits/UsageHelper';

// localStorage keys for Test Hub scenarios
const STORAGE_KEY_CE_THROTTLING_STATUS = 'SONATYPE_TEST_CE_THROTTLING_STATUS';
const STORAGE_KEY_CE_COMPONENTS = 'SONATYPE_TEST_CE_COMPONENTS';
const STORAGE_KEY_CE_REQUESTS = 'SONATYPE_TEST_CE_REQUESTS';

const {
  getMetricData,
} = helperFunctions;

function InfoTooltipTrigger({title, content}) {
  return (
    <Tooltip content={content}>
      <button type="button" aria-label={`${title} information`}
              style={{
                display: 'inline-flex',
                cursor: 'help',
                background: 'none',
                border: 'none',
                padding: 0,
              }}>
        <Info size={16} aria-hidden style={{color: 'var(--gray-9)'}} />
      </button>
    </Tooltip>
  );
}

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

const {OVER_LIMITS, APPROACHING_LIMITS, UNDER_LIMITS} = HEADER;

const STATUS_CONFIG = {
  over:  {label: OVER_LIMITS.STATUS_INDICATOR,       color: 'var(--red-9)',    bg: 'var(--red-2)',    border: 'var(--red-6)'},
  near:  {label: APPROACHING_LIMITS.STATUS_INDICATOR, color: 'var(--yellow-9)', bg: 'var(--yellow-2)', border: 'var(--yellow-6)'},
  below: {label: UNDER_LIMITS.STATUS_INDICATOR,       color: 'var(--green-9)',  bg: 'var(--green-2)',  border: 'var(--green-6)'},
};

function computeCEStatus(usage) {
  const metricsToCheck = [TOTAL_COMPONENTS.METRIC_NAME, REQUESTS_PER_DAY.METRIC_NAME];
  let anyOver = false;
  let anyNear = false;
  for (const metricName of metricsToCheck) {
    const {metricValue, thresholdValue} = getMetricData(usage, metricName);
    if (thresholdValue > 0) {
      if (metricValue >= thresholdValue) anyOver = true;
      else if (metricValue >= thresholdValue * PERCENTAGE) anyNear = true;
    }
  }
  return anyOver ? 'over' : anyNear ? 'near' : 'below';
}

function UsageStatusBadge({status}) {
  const {label, color, bg, border} = STATUS_CONFIG[status];
  return (
    <Flex
      align="center"
      gap="2"
      style={{
        display: 'inline-flex',
        padding: '2px 10px 2px 8px',
        borderRadius: '999px',
        border: `1px solid ${border}`,
        background: bg,
      }}
    >
      <Box
        style={{
          width: '8px',
          height: '8px',
          borderRadius: '50%',
          background: color,
          flexShrink: 0,
        }}
      />
      <Text size="1" style={{color, fontWeight: 500}}>{label}</Text>
    </Flex>
  );
}

function StatCard({card, usage}) {
  const isPostgres = ExtJS.state().getValue('datastore.isPostgresql');
  const {METRIC_NAME, METRIC_NAME_PRO_POSTGRESQL, SUB_TITLE_PRO_POSTGRESQL, TITLE, TITLE_PRO_POSTGRESQL, HIGHEST_RECORDED_COUNT} = card;
  const {metricValue, highestRecordedCount} = getMetricData(usage, isPostgres ? METRIC_NAME_PRO_POSTGRESQL : METRIC_NAME);

  return (
    <Card style={{padding: 'var(--space-4)'}}>
      <Flex direction="column" gap="3">
        <Heading size="4" weight="medium">{TITLE_PRO_POSTGRESQL ?? TITLE}</Heading>
        <Flex direction="column" gap="1">
          <Text size="8" weight="bold" style={{lineHeight: 1, fontSize: '2.5rem'}}>
            {metricValue.toLocaleString()}
          </Text>
          {(SUB_TITLE_PRO_POSTGRESQL) && (
            <Text size="2" color="gray">{SUB_TITLE_PRO_POSTGRESQL}</Text>
          )}
        </Flex>
        {HIGHEST_RECORDED_COUNT && (
          <Box style={{borderTop: '1px solid var(--gray-6)', paddingTop: 'var(--space-2)'}}>
            <Flex direction="column" gap="1">
              <Text size="5" weight="bold">{highestRecordedCount.toLocaleString()}</Text>
              <Text size="2" color="gray">{HIGHEST_RECORDED_COUNT}</Text>
            </Flex>
          </Box>
        )}
      </Flex>
    </Card>
  );
}

const DATE_FORMATTER = new Intl.DateTimeFormat('en-US', {
  month: 'short',
  day: 'numeric',
  year: 'numeric',
  hour: 'numeric',
  minute: 'numeric',
  hour12: true
});

function formatDate(timestamp) {
  if (!timestamp) return '';
  const date = new Date(timestamp);
  if (isNaN(date)) return '';
  return DATE_FORMATTER.format(date);
}

function StatCardWithThreshold({card, usage, tooltip, edition, date}) {
  const {HIGHEST_RECORDED_COUNT, METRIC_NAME, SUB_TITLE, TITLE} = card;
  const {metricValue, thresholdValue, highestRecordedCount} = getMetricData(usage, METRIC_NAME);
  const thresholdLabel = edition === COMMUNITY ? USAGE_LIMIT : THRESHOLD;
  const percentage = thresholdValue > 0 ? Math.min((metricValue / thresholdValue) * 100, 100) : 0;
  const approachingThreshold = metricValue >= thresholdValue * PERCENTAGE;
  const exceedingThreshold = metricValue >= thresholdValue;

  const showError = exceedingThreshold;
  const showWarning = approachingThreshold && !exceedingThreshold;
  
  const tooltipText = typeof tooltip === 'function'
    ? tooltip(thresholdValue)
    : (tooltip || '').replace('{}', thresholdValue.toLocaleString());

  return (
    <Card style={{padding: 'var(--space-4)'}}>
      <Flex direction="column" gap="4">
        <Flex align="center" gap="2">
          <Heading size="4" weight="medium">{TITLE}</Heading>
          <InfoTooltipTrigger title={TITLE} content={tooltipText} />
        </Flex>
        
        <Box>
          <Progress
            value={percentage}
            max={100}
            color={showError ? 'red' : showWarning ? 'orange' : 'blue'}
            size="3"
            style={{height: '12px'}}
          />
          <Flex justify="between" mt="2">
            <Text size="1" color="gray">
              {metricValue.toLocaleString()} of {thresholdValue.toLocaleString()}
            </Text>
          </Flex>
        </Box>
        
        <Flex justify="between" align="start">
          <Flex direction="column" gap="1">
            <Flex align="center" gap="2">
              {showError && <AlertCircle size={18} style={{color: 'var(--red-9)'}} />}
              {showWarning && <AlertTriangle size={18} style={{color: 'var(--orange-9)'}} />}
              <Text size="6" weight="bold">
                {metricValue.toLocaleString()}
              </Text>
            </Flex>
            <Text size="2" color="gray">{SUB_TITLE}</Text>
          </Flex>
          
          <Flex direction="column" gap="1" align="end">
            <Text size="4" weight="medium">
              {thresholdValue.toLocaleString()}
            </Text>
            <Text size="2" color="gray">{thresholdLabel}</Text>
          </Flex>
        </Flex>
        
        <Box style={{
          borderTop: '1px solid var(--gray-6)',
          paddingTop: 'var(--space-3)',
          marginTop: 'var(--space-2)'
        }}>
          <Flex direction="column" gap="2">
            <Flex direction="column" gap="1">
              <Text size="5" weight="bold">{highestRecordedCount.toLocaleString()}</Text>
              <Text size="2" color="gray">{HIGHEST_RECORDED_COUNT}</Text>
            </Flex>

            {date && (
              <Flex justify="between">
                <Text size="2" color="gray">{LAST_EXCEEDED_DATE_LABEL}</Text>
                <Text size="2" weight="medium">{date}</Text>
              </Flex>
            )}
          </Flex>
        </Box>
      </Flex>
    </Card>
  );
}

// NEXUS-53863: replaces broken MonthlyMetricsCard (used non-existent CARDS.MONTHLY_METRICS);
// shows the 3 standard sub-metrics from MONTHLY_REQUESTS for both Pro and CE editions
function MonthlyRequestsCard({usage, tooltip}) {
  const currentMonth = new Intl.DateTimeFormat('en-US', {month: 'long'}).format(new Date());
  const {metricValue: totalValue} = getMetricData(usage, MONTHLY_REQUESTS.TOTAL.METRIC_NAME);
  const {metricValue: averageValue} = getMetricData(usage, MONTHLY_REQUESTS.AVERAGE.METRIC_NAME);
  const {metricValue: highestValue} = getMetricData(usage, MONTHLY_REQUESTS.HIGHEST.METRIC_NAME);

  return (
    <Card style={{padding: 'var(--space-4)'}}>
      <Flex direction="column" gap="4">
        <Flex align="center" gap="2">
          <Heading size="4" weight="medium">{MONTHLY_REQUESTS.TITLE}</Heading>
          {tooltip && <InfoTooltipTrigger title={MONTHLY_REQUESTS.TITLE} content={tooltip} />}
        </Flex>
        <Flex direction="column" gap="3">
          <Flex direction="column" gap="1">
            <Text size="5" weight="bold">{totalValue.toLocaleString()}</Text>
            <Text size="2" color="gray">{MONTHLY_REQUESTS.TOTAL.SUB_TITLE(currentMonth)}</Text>
          </Flex>
          <Box style={{height: '1px', background: 'var(--gray-5)'}} />
          <Flex direction="column" gap="1">
            <Text size="5" weight="bold">{averageValue.toLocaleString()}</Text>
            <Text size="2" color="gray">{MONTHLY_REQUESTS.AVERAGE.SUB_TITLE}</Text>
          </Flex>
          <Box style={{height: '1px', background: 'var(--gray-5)'}} />
          <Flex direction="column" gap="1">
            <Text size="5" weight="bold">{highestValue.toLocaleString()}</Text>
            <Text size="2" color="gray">{MONTHLY_REQUESTS.HIGHEST.SUB_TITLE}</Text>
          </Flex>
        </Flex>
      </Flex>
    </Card>
  );
}

export default function UsageCenter() {
  // Force re-render when test overrides change (Test Hub scenarios)
  const [, forceUpdate] = React.useReducer(x => x + 1, 0);

  React.useEffect(() => {
    const handleStorageChange = (e) => {
      if (e.key === STORAGE_KEY_CE_THROTTLING_STATUS ||
          e.key === STORAGE_KEY_CE_COMPONENTS ||
          e.key === STORAGE_KEY_CE_REQUESTS) {
        forceUpdate();
      }
    };
    window.addEventListener('storage', handleStorageChange);
    return () => window.removeEventListener('storage', handleStorageChange);
  }, []);

  const isProEdition = ExtJS.isProEdition();
  const isCommunityEdition = ExtJS.state().getEdition() === COMMUNITY;
  const usage = ExtJS.state().getValue('contentUsageEvaluationResult', []);
  const componentCountLimitDateLastExceeded = ExtJS.state().getValue('nexus.community.componentCountLimitDateLastExceeded');
  const requestPer24HoursLimitDateLastExceeded = ExtJS.state().getValue('nexus.community.requestPer24HoursLimitDateLastExceeded');
  
  const componentFormattedDate = componentCountLimitDateLastExceeded 
    ? formatDate(componentCountLimitDateLastExceeded)
    : '';
  const requestFormattedDate = requestPer24HoursLimitDateLastExceeded 
    ? formatDate(requestPer24HoursLimitDateLastExceeded)
    : '';

  if (!usage || usage.length === 0) {
    return null;
  }

  const badgeStatus = isCommunityEdition ? computeCEStatus(usage) : 'below';

  return (
    <Card size="3" style={{padding: 'var(--space-5)'}}>
      <Flex direction="column" gap="1" mb="4">
        <Flex align="center" gap="3">
          <Heading size="5">{MENU.TITLE}</Heading>
          <UsageStatusBadge status={badgeStatus} />
        </Flex>
        <Text size="2" color="gray">{HEADER.PRO_POSTGRES.TEXT}</Text>
        <Text size="2" weight="medium" mt="3">{MENU.SUB_TITLE}</Text>
      </Flex>
      
      {/* Full-width metric cards */}
      <Grid columns={{initial: '1', md: '3'}} gap="4" style={{width: '100%'}}>
        {isProEdition && (
          <>
            <StatCard card={TOTAL_COMPONENTS} usage={usage} />
            <StatCard card={REQUESTS_PER_DAY} usage={usage} />
            <MonthlyRequestsCard usage={usage} />
          </>
        )}

        {isCommunityEdition && (
          <>
            <StatCardWithThreshold
              card={TOTAL_COMPONENTS}
              usage={usage}
              tooltip={TOTAL_COMPONENTS.TOOLTIP_CE}
              edition={COMMUNITY}
              date={componentFormattedDate}
            />
            <StatCardWithThreshold
              card={REQUESTS_PER_DAY}
              usage={usage}
              tooltip={REQUESTS_PER_DAY.TOOLTIP_CE}
              edition={COMMUNITY}
              date={requestFormattedDate}
            />
            <MonthlyRequestsCard usage={usage} tooltip={MONTHLY_REQUESTS.TOOLTIP_PREVIEW} />
          </>
        )}
      </Grid>
    </Card>
  );
}
