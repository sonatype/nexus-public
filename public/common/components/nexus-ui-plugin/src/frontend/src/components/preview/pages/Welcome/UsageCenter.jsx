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
import {replace} from 'ramda';

import UIStrings from '../../../../constants/UIStrings';
import {helperFunctions} from '../../../widgets/SystemStatusAlerts/CELimits/UsageHelper';

const {
  getMetricData,
} = helperFunctions;

const {
  WELCOME: {
    USAGE: {
      MENU,
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

function StatCard({card, usage}) {
  const isPostgres = ExtJS.state().getValue('datastore.isPostgresql');
  const {METRIC_NAME, METRIC_NAME_PRO_POSTGRESQL, SUB_TITLE_PRO_POSTGRESQL, TITLE, TITLE_PRO_POSTGRESQL} = card;
  const {metricValue} = getMetricData(usage, isPostgres ? METRIC_NAME_PRO_POSTGRESQL : METRIC_NAME);

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
      </Flex>
    </Card>
  );
}

function formatDate(timestamp) {
  if (!timestamp) return '';
  const date = new Date(timestamp);
  return date.toLocaleDateString();
}

function StatCardWithThreshold({card, usage, tooltip, edition, date}) {
  const {HIGHEST_RECORDED_COUNT, METRIC_NAME, SUB_TITLE, TITLE} = card;
  const {metricValue, thresholdValue, highestRecordedCount} = getMetricData(usage, METRIC_NAME);
  const thresholdLabel = edition === COMMUNITY ? USAGE_LIMIT : THRESHOLD;
  const percentage = (metricValue / thresholdValue) * 100;
  const approachingThreshold = metricValue >= thresholdValue * PERCENTAGE;
  const exceedingThreshold = metricValue >= thresholdValue;
  
  const showError = exceedingThreshold;
  const showWarning = approachingThreshold && !exceedingThreshold;
  
  const tooltipText = typeof tooltip === 'function' 
    ? tooltip(thresholdValue) 
    : replace('{}', thresholdValue.toLocaleString(), tooltip || '');

  return (
    <Card style={{padding: 'var(--space-4)'}}>
      <Flex direction="column" gap="4">
        <Flex align="center" gap="2">
          <Heading size="4" weight="medium">{TITLE}</Heading>
          <Box title={tooltipText} style={{cursor: 'help'}}>
            <Info size={16} style={{color: 'var(--gray-9)'}} />
          </Box>
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
            <Flex justify="between">
              <Text size="2" color="gray">{HIGHEST_RECORDED_COUNT}</Text>
              <Text size="2" weight="medium">{highestRecordedCount.toLocaleString()}</Text>
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

function MonthlyMetricsCard({usage}) {
  const {MONTHLY_METRICS, MONTHLY_METRICS_TITLE} = CARDS;
  const {metricValue: monthlyDownloads} = getMetricData(usage, MONTHLY_METRICS.MONTHLY_DOWNLOADS);
  const {metricValue: monthlySuccessfulDownloads} = getMetricData(usage, MONTHLY_METRICS.MONTHLY_SUCCESSFUL_DOWNLOADS);
  const {metricValue: monthlyUniqueIps} = getMetricData(usage, MONTHLY_METRICS.MONTHLY_UNIQUE_IPS);

  return (
    <Card style={{padding: 'var(--space-4)'}}>
      <Flex direction="column" gap="4">
        <Heading size="4" weight="medium">{MONTHLY_METRICS_TITLE}</Heading>
        
        <Flex direction="column" gap="3">
          <Flex justify="between" align="center">
            <Text size="2" color="gray">{MONTHLY_METRICS.MONTHLY_DOWNLOADS_LABEL}</Text>
            <Text size="4" weight="bold">{monthlyDownloads.toLocaleString()}</Text>
          </Flex>
          
          <Box style={{height: '1px', background: 'var(--gray-5)'}} />
          
          <Flex justify="between" align="center">
            <Text size="2" color="gray">{MONTHLY_METRICS.MONTHLY_SUCCESSFUL_DOWNLOADS_LABEL}</Text>
            <Text size="4" weight="bold">{monthlySuccessfulDownloads.toLocaleString()}</Text>
          </Flex>
          
          <Box style={{height: '1px', background: 'var(--gray-5)'}} />
          
          <Flex justify="between" align="center">
            <Text size="2" color="gray">{MONTHLY_METRICS.MONTHLY_UNIQUE_IPS_LABEL}</Text>
            <Text size="4" weight="bold">{monthlyUniqueIps.toLocaleString()}</Text>
          </Flex>
        </Flex>
      </Flex>
    </Card>
  );
}

export default function UsageCenter() {
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

  return (
    <Card size="3" style={{padding: 'var(--space-5)'}}>
      {/* Header with subtitle like Default UI */}
      <Flex direction="column" gap="1" mb="4">
        <Heading size="5">{MENU.TITLE}</Heading>
        <Text size="2" color="gray">
          Monitor this instance&apos;s usage to optimize your deployments.
        </Text>
        <Text size="2" weight="medium" mt="3">Usage Metrics Overview</Text>
      </Flex>
      
      {/* Full-width metric cards */}
      <Grid columns={{initial: '1', md: '3'}} gap="4" style={{width: '100%'}}>
        {isProEdition && (
          <>
            <StatCard card={TOTAL_COMPONENTS} usage={usage} />
            <StatCard card={REQUESTS_PER_DAY} usage={usage} />
            <StatCard card={MONTHLY_REQUESTS} usage={usage} />
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
            <MonthlyMetricsCard usage={usage} />
          </>
        )}
      </Grid>
    </Card>
  );
}

