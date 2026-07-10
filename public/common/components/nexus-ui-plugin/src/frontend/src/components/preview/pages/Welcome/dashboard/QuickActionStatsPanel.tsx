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

import React, { useEffect, useState, useMemo } from 'react';
import { Text, Heading, Box, Card, Flex, Button, Progress } from '@radix-ui/themes';
import { AlertCircle, AlertTriangle, RefreshCw } from 'lucide-react';
import { ExtJS } from '../../../../../interface/ExtJS';
import { Permissions } from '../../../../../constants/Permissions';
import { useMetricHealthApi } from '../../settings/support/metric-health/useMetricHealthApi';
import { getHealthStatus } from '../../settings/support/metric-health/types';
import { useCleanupPoliciesApi } from '../../settings/repository/cleanup/useCleanupPoliciesApi';
import { useRepositoriesByFormat } from './useRepositoriesByFormat';
import { useInstanceTotals } from './useInstanceTotals';
import { CE_WARN_THRESHOLD } from './ceThresholds';
import UIStrings from '../../../../../constants/UIStrings';

import './QuickActionStatsPanel.scss';

const {
  WELCOME: {
    ACTIONS: { SYSTEM_HEALTH, CLEANUP_POLICIES, BROWSE, SEARCH, CONNECT },
  },
} = UIStrings;

const SYSTEM_HEALTH_HREF = '#preview/admin/support/metrichealth';
const BROWSE_HREF = '#preview/browse';
const SEARCH_HREF = '#preview/browse/search';
const CLEANUP_POLICIES_HREF = '#preview/admin/repository/cleanup-policies';

interface StatCardProps {
  title: string;
  href?: string;
  onClick?: () => void;
  children: React.ReactNode;
  className?: string;
}

function StatCard({ title, href, onClick, children, className }: StatCardProps) {
  const content = (
    <Box p="3" className="nxrm-quick-action-stat-card__content">
      <Text size="2" color="gray" mb="1" as="span" className="nxrm-quick-action-stat-card__title">
        {title}
      </Text>
      <Box className="nxrm-quick-action-stat-card__value">{children}</Box>
    </Box>
  );

  const cardProps = {
    className: ['nxrm-quick-action-stat-card', className].filter(Boolean).join(' '),
    size: '1' as const,
    variant: 'surface' as const,
  };

  if (href) {
    return (
      <Card {...cardProps} asChild>
        <a
          href={href}
          className={['nxrm-quick-action-stat-card__link', className].filter(Boolean).join(' ')}
        >
          {content}
        </a>
      </Card>
    );
  }

  if (onClick) {
    return (
      <Card
        {...cardProps}
        tabIndex={0}
        role="button"
        onClick={onClick}
        onKeyDown={(e) => (e.key === 'Enter' || e.key === ' ') && (e.preventDefault(), onClick())}
      >
        {content}
      </Card>
    );
  }

  return <Card {...cardProps}>{content}</Card>;
}

export interface QuickActionStatsPanelProps {
  onConnectClick?: () => void;
}

export function QuickActionStatsPanel({ onConnectClick }: QuickActionStatsPanelProps) {
  const { fetchMetricHealth } = useMetricHealthApi();
  const { fetchCleanupPolicies } = useCleanupPoliciesApi();
  const reposByFormat = useRepositoriesByFormat();
  const instanceTotals = useInstanceTotals();

  const [healthChecks, setHealthChecks] = useState<{ healthy: number; unhealthy: number } | null>(null);
  const [cleanupCount, setCleanupCount] = useState<number | null>(null);

  // Check if user has permission to view cleanup policies (requires admin permission)
  const canViewCleanupPolicies = ExtJS.checkPermission(Permissions.ADMIN) && ExtJS.state().getUser();
  const isCloud: boolean = ExtJS.state?.()?.getValue?.('isCloud', false) ?? false;

  useEffect(() => {
    // On cloud the API returns 403; the catch handler sets counts to 0/0 so the card
    // still renders — the href guard above prevents navigation to the missing route.
    let cancelled = false;
    fetchMetricHealth()
      .then((checks) => {
        if (cancelled) return;
        const counts = checks.reduce(
          (acc, check) => {
            const status = getHealthStatus(check.result);
            if (status === 'healthy') acc.healthy++;
            else if (status === 'unhealthy') acc.unhealthy++;
            return acc;
          },
          { healthy: 0, unhealthy: 0 }
        );
        setHealthChecks(counts);
      })
      .catch(() => {
        if (!cancelled) setHealthChecks({ healthy: 0, unhealthy: 0 });
      });
    return () => {
      cancelled = true;
    };
  }, [fetchMetricHealth]);

  useEffect(() => {
    // Only fetch cleanup policies if user has permission
    if (!canViewCleanupPolicies) {
      return;
    }

    let cancelled = false;
    fetchCleanupPolicies()
      .then((policies) => {
        if (!cancelled) setCleanupCount(Array.isArray(policies) ? policies.length : 0);
      })
      .catch(() => {
        if (!cancelled) setCleanupCount(0);
      });
    return () => {
      cancelled = true;
    };
  }, [fetchCleanupPolicies, canViewCleanupPolicies]);

  const repoCount = useMemo(() => {
    if (reposByFormat.loading || reposByFormat.error) return null;
    return reposByFormat.data?.reduce((sum, f) => sum + f.totalCount, 0) ?? 0;
  }, [reposByFormat.loading, reposByFormat.error, reposByFormat.data]);

  const componentCount = instanceTotals.data?.totalComponents ?? null;
  const componentLimit = instanceTotals.data?.totalComponentsLimit || 0;
  const hasComponentLimit = componentLimit > 0;
  const componentRatio = hasComponentLimit && componentCount != null ? componentCount / componentLimit : 0;
  const componentExceeding = hasComponentLimit && componentRatio >= 1;
  const componentApproaching = hasComponentLimit && !componentExceeding && componentRatio >= CE_WARN_THRESHOLD;
  const componentBarColor = componentExceeding ? 'red' : componentApproaching ? 'orange' : 'blue';
  // peakRequestsPerDayLimit is intentionally not shown here — space constraints in the compact stat card.
  // InstanceTotalsPanel shows the full progress bar for both limits.

  const systemHealthClassName =
    healthChecks != null
      ? healthChecks.unhealthy > 0
        ? 'nxrm-quick-action-stat-card--unhealthy'
        : 'nxrm-quick-action-stat-card--healthy'
      : undefined;

  return (
    <div className="nxrm-quick-action-stats">
      <div className="nxrm-quick-action-stats__grid">
        <StatCard
          title={SYSTEM_HEALTH.title}
          href={isCloud ? undefined : SYSTEM_HEALTH_HREF}
          className={systemHealthClassName}
        >
          {healthChecks != null ? (
            <Flex direction="column" gap="2" className="nxrm-system-health__progress">
              <Flex justify="between" align="baseline">
                <Heading
                  as="h1"
                  size="6"
                  className={`nxrm-quick-action-stat-card__number ${healthChecks.unhealthy > 0 ? 'nxrm-system-health__number--unhealthy' : ''}`}
                >
                  {healthChecks.unhealthy.toLocaleString()}
                </Heading>
                <Heading as="h1" size="6" className="nxrm-quick-action-stat-card__number">
                  {healthChecks.healthy.toLocaleString()}
                </Heading>
              </Flex>
              <Flex
                className={`nxrm-system-health__progress-bar ${
                  healthChecks.unhealthy + healthChecks.healthy === 0
                    ? 'nxrm-system-health__progress-bar--empty'
                    : ''
                }`}
              >
                <Box
                  className="nxrm-system-health__progress-segment nxrm-system-health__progress-segment--unhealthy"
                  style={{
                    flex: healthChecks.unhealthy + healthChecks.healthy === 0 ? 1 : healthChecks.unhealthy,
                  }}
                />
                <Box
                  className="nxrm-system-health__progress-segment nxrm-system-health__progress-segment--healthy"
                  style={{
                    flex: healthChecks.unhealthy + healthChecks.healthy === 0 ? 1 : healthChecks.healthy,
                  }}
                />
              </Flex>
              <Flex justify="between">
                <Text size="1" color="gray">
                  Unhealthy
                </Text>
                <Text size="1" color="gray">
                  Healthy
                </Text>
              </Flex>
            </Flex>
          ) : (
            <Text size="2" color="gray">
              —
            </Text>
          )}
        </StatCard>

        <StatCard title="Repositories">
          {repoCount != null ? (
            <Flex direction="column" gap="3">
              <Heading as="h1" size="6" className="nxrm-quick-action-stat-card__number">
                {repoCount.toLocaleString()}
              </Heading>
              <Flex gap="2" wrap="wrap">
                <Button
                  size="2"
                  variant="surface"
                  color="blue"
                  onClick={() => {
                    window.location.hash = BROWSE_HREF;
                  }}
                >
                  {BROWSE.title}
                </Button>
                <Button size="2" variant="surface" color="blue" onClick={onConnectClick}>
                  {CONNECT.title}
                </Button>
              </Flex>
            </Flex>
          ) : (
            <Text size="2" color="gray">
              —
            </Text>
          )}
        </StatCard>

        <StatCard title="Components">
          {instanceTotals.loading ? (
            <Flex align="center" gap="2">
              <RefreshCw size={16} className="nxrm-quick-action-stat-card__spinner" />
              <Text size="2" color="gray">
                Loading…
              </Text>
            </Flex>
          ) : componentCount != null ? (
            <Flex direction="column" gap="3">
              <Flex align="baseline" gap="2">
                {componentExceeding && <AlertCircle size={16} style={{color: 'var(--red-9)', flexShrink: 0}} />}
                {componentApproaching && <AlertTriangle size={16} style={{color: 'var(--orange-9)', flexShrink: 0}} />}
                <Heading as="h1" size="6" className="nxrm-quick-action-stat-card__number">
                  {componentCount.toLocaleString()}
                </Heading>
              </Flex>
              {hasComponentLimit && (
                <Box>
                  <Progress
                    value={Math.min(componentRatio * 100, 100)}
                    max={100}
                    color={componentBarColor}
                    size="1"
                  />
                  <Flex justify="between" mt="1">
                    <Text size="1" color="gray">
                      {componentCount.toLocaleString()} of {componentLimit.toLocaleString()}
                    </Text>
                    <Text size="1" color="gray">
                      limit
                    </Text>
                  </Flex>
                </Box>
              )}
              <Button
                size="2"
                variant="surface"
                color="blue"
                onClick={() => {
                  window.location.hash = SEARCH_HREF;
                }}
              >
                {SEARCH.title}
              </Button>
            </Flex>
          ) : (
            <Text size="2" color="gray">
              —
            </Text>
          )}
        </StatCard>

        {canViewCleanupPolicies && (cleanupCount === 0 ? (
          <StatCard
            title={CLEANUP_POLICIES.title}
            className="nxrm-quick-action-stat-card--zero-cleanup"
          >
            <Flex direction="column" gap="3">
              <Heading as="h1" size="6" className="nxrm-quick-action-stat-card__number">
                0
              </Heading>
              <Button
                size="2"
                variant="surface"
                color="blue"
                onClick={() => {
                  window.location.hash = CLEANUP_POLICIES_HREF;
                }}
              >
                Add Cleanup Policy
              </Button>
            </Flex>
          </StatCard>
        ) : (
          <StatCard title={CLEANUP_POLICIES.title} href={CLEANUP_POLICIES_HREF}>
            {cleanupCount != null ? (
              <Heading as="h1" size="6" className="nxrm-quick-action-stat-card__number">
                {cleanupCount.toLocaleString()}
              </Heading>
            ) : (
              <Text size="2" color="gray">
                —
              </Text>
            )}
          </StatCard>
        ))}
      </div>
    </div>
  );
}

export default QuickActionStatsPanel;
