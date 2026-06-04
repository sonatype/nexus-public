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
import {Box, Flex, Text, Heading, Badge} from '@radix-ui/themes';
import {
  Activity,
  Gauge,
  Calendar,
  TrendingUp,
  BarChart3,
  CheckCircle,
  AlertTriangle,
  Bell,
  Shield,
  ChevronRight,
  RefreshCw,
} from 'lucide-react';

import {Sparkline} from './dashboard/Sparkline';
import type {InstanceTotals} from './dashboard/simplified.types';
import type {DataPoint} from './dashboard/useUsageHistory';
import OutreachActions from './OutreachActions';

import './WelcomeDashboard.scss';

export interface WelcomeDashboardProps {
  user: {userId?: string; administrator?: boolean} | null;
  status: {version?: string; edition?: string} | null;
  license: {daysToExpiry?: number} | null;
  instanceTotals: {
    data: InstanceTotals | null;
    loading: boolean;
  };
  usageHistory: {
    requestsDaily: DataPoint[];
    requestsMonthly: DataPoint[];
    componentsDaily: DataPoint[];
    componentsMonthly: DataPoint[];
    loading: boolean;
    error: string | null;
    refresh: () => void;
  };
  isAdmin: boolean;
  isAuthenticated: boolean;
}

function formatCompact(n: number | undefined | null): string {
  if (n == null || isNaN(n)) return '\u2014';
  if (n === 0) return '0';
  return new Intl.NumberFormat('en-US', {
    notation: 'compact',
    maximumFractionDigits: 1,
  }).format(n);
}

function getEditionColor(edition?: string): 'green' | 'blue' | 'gray' | 'orange' {
  if (!edition) return 'gray';
  const upper = edition.toUpperCase();
  if (upper.includes('PRO')) return 'green';
  if (upper.includes('STARTER')) return 'blue';
  if (upper.includes('OSS') || upper.includes('CORE')) return 'gray';
  if (upper.includes('COMMUNITY') || upper === 'CE') return 'orange';
  return 'green';
}

function getEditionLabel(edition?: string): string {
  if (!edition) return 'Nexus Repository';
  const upper = edition.toUpperCase();
  if (upper.includes('PRO')) return 'Pro Edition';
  if (upper.includes('OSS') || upper.includes('CORE')) return 'Core Edition';
  if (upper.includes('STARTER')) return 'Starter Edition';
  if (upper.includes('COMMUNITY') || upper === 'CE') return 'Community Edition';
  return edition;
}

interface MetricCardProps {
  icon: React.ReactNode;
  iconGradient: string;
  value: number | undefined | null;
  label: string;
  sparklineData?: DataPoint[];
  sparklineColor?: string;
  delay?: number;
}

function MetricCard({
  icon,
  iconGradient,
  value,
  label,
  sparklineData,
  sparklineColor = '#3b82f6',
  delay = 0,
}: MetricCardProps) {
  return (
    <div
      className="nxrm-dashboard__metric-card"
      style={{animationDelay: `${delay}ms`}}
    >
      <div className="nxrm-dashboard__metric-card-inner">
        <div
          className="nxrm-dashboard__metric-icon"
          style={{background: iconGradient}}
        >
          {icon}
        </div>
        <div className="nxrm-dashboard__metric-content">
          <div className="nxrm-dashboard__metric-value-row">
            <span className="nxrm-dashboard__metric-value">
              {formatCompact(value)}
            </span>
            {sparklineData && sparklineData.length > 1 && (
              <Sparkline
                data={sparklineData}
                width={72}
                height={24}
                color={sparklineColor}
                showTrend={true}
              />
            )}
          </div>
          <span className="nxrm-dashboard__metric-label">{label}</span>
        </div>
      </div>
    </div>
  );
}

interface TrendCardProps {
  title: string;
  icon: React.ReactNode;
  data: DataPoint[];
  color: string;
  emptyMessage?: string;
}

function TrendCard({
  title,
  icon,
  data,
  color,
  emptyMessage = 'No data yet',
}: TrendCardProps) {
  return (
    <div className="nxrm-dashboard__trend-card">
      <div className="nxrm-dashboard__trend-header">
        {icon}
        <span className="nxrm-dashboard__trend-title">{title}</span>
      </div>
      <div className="nxrm-dashboard__trend-chart">
        {data && data.length > 1 ? (
          <Sparkline
            data={data}
            width={400}
            height={120}
            color={color}
            showTrend={false}
          />
        ) : (
          <div className="nxrm-dashboard__trend-empty">
            <BarChart3 size={32} strokeWidth={1} />
            <span>{emptyMessage}</span>
          </div>
        )}
      </div>
    </div>
  );
}

export function WelcomeDashboard({
  user,
  status,
  license,
  instanceTotals,
  usageHistory,
  isAdmin,
  isAuthenticated,
}: WelcomeDashboardProps) {
  const userName = user?.userId || 'there';
  const greeting = user
    ? `Welcome back, ${userName}`
    : 'Welcome to Nexus Repository';
  const editionLabel = getEditionLabel(status?.edition);
  const editionColor = getEditionColor(status?.edition);
  const version = status?.version;

  const hasMetrics = !!instanceTotals.data;
  const licenseExpiringSoon =
    license?.daysToExpiry != null &&
    license.daysToExpiry <= 30 &&
    license.daysToExpiry > 0;
  const licenseExpired =
    license?.daysToExpiry != null && license.daysToExpiry <= 0;

  return (
    <Box
      px={{ initial: '4', md: '6', lg: '6' }}
      py={{ initial: '4', md: '5', lg: '6' }}
      width="100%"
      style={{ minWidth: 0, boxSizing: 'border-box' }}
    >
      <div className="nxrm-dashboard" data-testid="welcome-dashboard">
      {/* Hero Banner */}
      <section className="nxrm-dashboard__hero" data-testid="dashboard-hero">
        <div className="nxrm-dashboard__hero-accent" />
        <Heading
          size="7"
          weight="bold"
          className="nxrm-dashboard__hero-heading"
        >
          {greeting}
        </Heading>
        <Flex align="center" gap="3" mt="1">
          <Badge color={editionColor} size="2" variant="soft">
            {editionLabel}
          </Badge>
          {version && (
            <Text size="2" className="nxrm-dashboard__hero-version">
              v{version}
            </Text>
          )}
        </Flex>
      </section>

      {/* Usage Metrics Cards */}
      {isAdmin && (
        <section
          className="nxrm-dashboard__section"
          data-testid="dashboard-metrics"
        >
          <div className="nxrm-dashboard__section-header">
            <Activity size={18} />
            <Heading size="4" weight="medium">
              Usage Metrics
            </Heading>
          </div>

          {instanceTotals.loading ? (
            <div className="nxrm-dashboard__metrics-loading">
              <RefreshCw size={20} className="nxrm-dashboard__spinner" />
              <Text size="2" color="gray">
                Loading metrics...
              </Text>
            </div>
          ) : hasMetrics ? (
            <div className="nxrm-dashboard__metrics-grid">
              <MetricCard
                icon={<Activity size={22} color="white" />}
                iconGradient="linear-gradient(135deg, #6366f1, #8b5cf6)"
                value={instanceTotals.data!.totalComponents}
                label="Total Components"
                sparklineData={usageHistory.componentsDaily}
                sparklineColor="#6366f1"
                delay={50}
              />
              <MetricCard
                icon={<Gauge size={22} color="white" />}
                iconGradient="linear-gradient(135deg, #10b981, #059669)"
                value={instanceTotals.data!.peakRequestsPerDay}
                label="Peak Requests / Day"
                sparklineData={usageHistory.requestsDaily}
                sparklineColor="#10b981"
                delay={100}
              />
              <MetricCard
                icon={<Calendar size={22} color="white" />}
                iconGradient="linear-gradient(135deg, #f59e0b, #d97706)"
                value={instanceTotals.data!.peakRequestsPerMonth}
                label="Peak Requests / Month"
                sparklineData={usageHistory.requestsMonthly}
                sparklineColor="#f59e0b"
                delay={150}
              />
            </div>
          ) : (
            <div className="nxrm-dashboard__metrics-empty">
              <BarChart3 size={40} strokeWidth={1} />
              <Text size="2" color="gray">
                No usage data available yet
              </Text>
              <Text size="1" color="gray">
                Metrics will appear once your instance has activity
              </Text>
            </div>
          )}
        </section>
      )}

      {/* Usage Trends */}
      {isAdmin && (
        <section
          className="nxrm-dashboard__section nxrm-usage-trends"
          data-testid="dashboard-trends"
        >
          <div className="nxrm-dashboard__section-header">
            <TrendingUp size={18} />
            <Heading size="4" weight="medium">
              Usage Trends
            </Heading>
          </div>

          {usageHistory.loading ? (
            <div className="nxrm-dashboard__trends-loading">
              <RefreshCw size={20} className="nxrm-dashboard__spinner" />
              <Text size="2" color="gray">
                Loading trends...
              </Text>
            </div>
          ) : usageHistory.error ? (
            <div className="nxrm-dashboard__trends-error">
              <AlertTriangle size={24} />
              <Text size="2" color="red">
                Failed to load usage trends
              </Text>
              <button
                className="nxrm-dashboard__retry-btn"
                onClick={usageHistory.refresh}
              >
                <RefreshCw size={14} />
                Retry
              </button>
            </div>
          ) : (
            <div className="nxrm-dashboard__trends-grid">
              <TrendCard
                title="Requests Over Time"
                icon={<TrendingUp size={16} />}
                data={usageHistory.requestsDaily}
                color="#00bb6c"
                emptyMessage="Request data will appear here once your instance has activity"
              />
              <TrendCard
                title="Components Over Time"
                icon={<BarChart3 size={16} />}
                data={usageHistory.componentsDaily}
                color="#6366f1"
                emptyMessage="Component data will appear here once your instance has activity"
              />
            </div>
          )}
        </section>
      )}

      {/* Quick Actions */}
      <section
        className="nxrm-dashboard__section"
        data-testid="dashboard-quick-actions"
      >
        <div className="nxrm-dashboard__section-header">
          <Shield size={18} />
          <Heading size="4" weight="medium">
            Quick Actions
          </Heading>
        </div>
        <OutreachActions />
      </section>

      {/* System Health + Notifications */}
      {isAuthenticated && (
        <div className="nxrm-dashboard__bottom-row">
          <section
            className="nxrm-dashboard__health-card"
            data-testid="dashboard-health"
          >
            <div className="nxrm-dashboard__card-header">
              <Shield size={16} />
              <span className="nxrm-dashboard__card-title">System Health Check</span>
            </div>
            <div className="nxrm-dashboard__health-status">
              <CheckCircle
                size={20}
                className="nxrm-dashboard__health-icon--ok"
              />
              <span className="nxrm-dashboard__health-text">
                All systems operational
              </span>
            </div>
            {isAdmin && (
              <a
                href="#preview/admin/support/metrichealth"
                className="nxrm-dashboard__health-link"
              >
                View details
                <ChevronRight size={14} />
              </a>
            )}
          </section>

          <section
            className="nxrm-dashboard__notifications-card"
            data-testid="dashboard-notifications"
          >
            <div className="nxrm-dashboard__card-header">
              <Bell size={16} />
              <span className="nxrm-dashboard__card-title">Notifications</span>
            </div>
            <div className="nxrm-dashboard__notifications-body">
              {licenseExpired && (
                <div className="nxrm-dashboard__notification nxrm-dashboard__notification--error">
                  <AlertTriangle size={16} />
                  <span>
                    Your license has expired. Please renew to continue using all
                    features.
                  </span>
                </div>
              )}
              {licenseExpiringSoon && !licenseExpired && (
                <div className="nxrm-dashboard__notification nxrm-dashboard__notification--warning">
                  <AlertTriangle size={16} />
                  <span>
                    License expires in {license!.daysToExpiry} days
                  </span>
                </div>
              )}
              {!licenseExpired && !licenseExpiringSoon && (
                <div className="nxrm-dashboard__notification nxrm-dashboard__notification--empty">
                  <Text size="2" color="gray">
                    No new notifications
                  </Text>
                </div>
              )}
            </div>
          </section>
        </div>
      )}
    </div>
    </Box>
  );
}

export default WelcomeDashboard;
