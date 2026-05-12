/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
import React from 'react';
import {render, screen} from '@testing-library/react';
import {WelcomeDashboard} from '../WelcomeDashboard';
import type {WelcomeDashboardProps} from '../WelcomeDashboard';

jest.mock('../OutreachActions', () => ({
  __esModule: true,
  default: function MockOutreachActions() {
    return <div data-testid="mock-outreach-actions">Quick Actions</div>;
  },
}));

jest.mock('../dashboard/Sparkline', () => ({
  __esModule: true,
  Sparkline: function MockSparkline() {
    return <svg data-testid="mock-sparkline" />;
  },
  default: function MockSparkline() {
    return <svg data-testid="mock-sparkline" />;
  },
}));

function buildProps(overrides: Partial<WelcomeDashboardProps> = {}): WelcomeDashboardProps {
  return {
    user: {userId: 'admin', administrator: true},
    status: {version: '3.90.0', edition: 'PRO'},
    license: {daysToExpiry: 365},
    instanceTotals: {
      data: {
        totalComponents: 1234567,
        peakRequestsPerDay: 45300,
        peakRequestsPerMonth: 892000,
      },
      loading: false,
    },
    usageHistory: {
      requestsDaily: [
        {date: '2026-01-17', value: 100},
        {date: '2026-01-18', value: 150},
      ],
      requestsMonthly: [
        {date: '2025-12', value: 1000},
        {date: '2026-01', value: 1200},
      ],
      componentsDaily: [
        {date: '2026-01-17', value: 50000},
        {date: '2026-01-18', value: 51000},
      ],
      componentsMonthly: [
        {date: '2025-12', value: 45000},
        {date: '2026-01', value: 51000},
      ],
      loading: false,
      error: null,
      refresh: jest.fn(),
    },
    isAdmin: true,
    isAuthenticated: true,
    ...overrides,
  };
}

describe('WelcomeDashboard', () => {
  describe('Hero Banner', () => {
    it('renders greeting with username for authenticated users', () => {
      render(<WelcomeDashboard {...buildProps()} />);
      expect(screen.getByText('Welcome back, admin')).toBeInTheDocument();
    });

    it('renders generic greeting for anonymous users', () => {
      render(<WelcomeDashboard {...buildProps({user: null})} />);
      expect(screen.getByText('Welcome to Nexus Repository')).toBeInTheDocument();
    });

    it('renders edition badge', () => {
      render(<WelcomeDashboard {...buildProps()} />);
      expect(screen.getByText('Pro Edition')).toBeInTheDocument();
    });

    it('renders Community Edition badge', () => {
      render(
        <WelcomeDashboard
          {...buildProps({status: {version: '3.90.0', edition: 'COMMUNITY'}})}
        />
      );
      expect(screen.getByText('Community Edition')).toBeInTheDocument();
    });

    it('renders version number', () => {
      render(<WelcomeDashboard {...buildProps()} />);
      expect(screen.getByText('v3.90.0')).toBeInTheDocument();
    });
  });

  describe('Usage Metrics Cards', () => {
    it('renders metric cards with compact formatted numbers for admin', () => {
      render(<WelcomeDashboard {...buildProps()} />);
      expect(screen.getByText('1.2M')).toBeInTheDocument();
      expect(screen.getByText('45.3K')).toBeInTheDocument();
      expect(screen.getByText('892K')).toBeInTheDocument();
    });

    it('renders metric labels', () => {
      render(<WelcomeDashboard {...buildProps()} />);
      expect(screen.getByText('Total Components')).toBeInTheDocument();
      expect(screen.getByText('Peak Requests / Day')).toBeInTheDocument();
      expect(screen.getByText('Peak Requests / Month')).toBeInTheDocument();
    });

    it('renders sparklines when data is available', () => {
      render(<WelcomeDashboard {...buildProps()} />);
      const sparklines = screen.getAllByTestId('mock-sparkline');
      expect(sparklines.length).toBeGreaterThanOrEqual(3);
    });

    it('hides metrics section for non-admin users', () => {
      render(<WelcomeDashboard {...buildProps({isAdmin: false})} />);
      expect(screen.queryByTestId('dashboard-metrics')).not.toBeInTheDocument();
    });

    it('shows loading state when metrics are loading', () => {
      render(
        <WelcomeDashboard
          {...buildProps({instanceTotals: {data: null, loading: true}})}
        />
      );
      expect(screen.getByText('Loading metrics...')).toBeInTheDocument();
    });

    it('shows empty state when no metrics data', () => {
      render(
        <WelcomeDashboard
          {...buildProps({instanceTotals: {data: null, loading: false}})}
        />
      );
      expect(screen.getByText('No usage data available yet')).toBeInTheDocument();
    });
  });

  describe('Usage Trends', () => {
    it('renders trends section for admin', () => {
      render(<WelcomeDashboard {...buildProps()} />);
      expect(screen.getByTestId('dashboard-trends')).toBeInTheDocument();
      expect(screen.getByText('Requests Over Time')).toBeInTheDocument();
      expect(screen.getByText('Components Over Time')).toBeInTheDocument();
    });

    it('hides trends for non-admin', () => {
      render(<WelcomeDashboard {...buildProps({isAdmin: false})} />);
      expect(screen.queryByTestId('dashboard-trends')).not.toBeInTheDocument();
    });

    it('shows error state with retry button when trends fail', () => {
      const refresh = jest.fn();
      render(
        <WelcomeDashboard
          {...buildProps({
            usageHistory: {
              requestsDaily: [],
              requestsMonthly: [],
              componentsDaily: [],
              componentsMonthly: [],
              loading: false,
              error: 'Network error',
              refresh,
            },
          })}
        />
      );
      expect(screen.getByText('Failed to load usage trends')).toBeInTheDocument();
      expect(screen.getByText('Retry')).toBeInTheDocument();
    });

    it('shows loading state when trends are loading', () => {
      render(
        <WelcomeDashboard
          {...buildProps({
            usageHistory: {
              requestsDaily: [],
              requestsMonthly: [],
              componentsDaily: [],
              componentsMonthly: [],
              loading: true,
              error: null,
              refresh: jest.fn(),
            },
          })}
        />
      );
      expect(screen.getByText('Loading trends...')).toBeInTheDocument();
    });
  });

  describe('Quick Actions', () => {
    it('renders quick actions section', () => {
      render(<WelcomeDashboard {...buildProps()} />);
      expect(screen.getByTestId('dashboard-quick-actions')).toBeInTheDocument();
      expect(screen.getByTestId('mock-outreach-actions')).toBeInTheDocument();
    });

    it('shows quick actions for non-admin users too', () => {
      render(<WelcomeDashboard {...buildProps({isAdmin: false})} />);
      expect(screen.getByTestId('mock-outreach-actions')).toBeInTheDocument();
    });
  });

  describe('System Health', () => {
    it('renders system health card for authenticated users', () => {
      render(<WelcomeDashboard {...buildProps()} />);
      expect(screen.getByTestId('dashboard-health')).toBeInTheDocument();
      expect(screen.getByText('All systems operational')).toBeInTheDocument();
    });

    it('shows details link for admin', () => {
      render(<WelcomeDashboard {...buildProps()} />);
      expect(screen.getByText('View details')).toBeInTheDocument();
    });

    it('hides health card for unauthenticated users', () => {
      render(
        <WelcomeDashboard {...buildProps({isAuthenticated: false})} />
      );
      expect(screen.queryByTestId('dashboard-health')).not.toBeInTheDocument();
    });
  });

  describe('Notifications', () => {
    it('shows no notifications when license is healthy', () => {
      render(<WelcomeDashboard {...buildProps()} />);
      expect(screen.getByText('No new notifications')).toBeInTheDocument();
    });

    it('shows license expiry warning when expiring soon', () => {
      render(
        <WelcomeDashboard {...buildProps({license: {daysToExpiry: 15}})} />
      );
      expect(screen.getByText('License expires in 15 days')).toBeInTheDocument();
    });

    it('shows license expired error', () => {
      render(
        <WelcomeDashboard {...buildProps({license: {daysToExpiry: 0}})} />
      );
      expect(
        screen.getByText(/license has expired/)
      ).toBeInTheDocument();
    });

    it('hides notifications for unauthenticated users', () => {
      render(
        <WelcomeDashboard {...buildProps({isAuthenticated: false})} />
      );
      expect(
        screen.queryByTestId('dashboard-notifications')
      ).not.toBeInTheDocument();
    });
  });

  describe('Number Formatting', () => {
    it('formats zero correctly', () => {
      render(
        <WelcomeDashboard
          {...buildProps({
            instanceTotals: {
              data: {
                totalComponents: 0,
                peakRequestsPerDay: 0,
                peakRequestsPerMonth: 0,
              },
              loading: false,
            },
          })}
        />
      );
      const zeros = screen.getAllByText('0');
      expect(zeros.length).toBe(3);
    });

    it('formats small numbers without compact notation', () => {
      render(
        <WelcomeDashboard
          {...buildProps({
            instanceTotals: {
              data: {
                totalComponents: 42,
                peakRequestsPerDay: 100,
                peakRequestsPerMonth: 500,
              },
              loading: false,
            },
          })}
        />
      );
      expect(screen.getByText('42')).toBeInTheDocument();
      expect(screen.getByText('100')).toBeInTheDocument();
      expect(screen.getByText('500')).toBeInTheDocument();
    });
  });

  describe('Dashboard wrapper', () => {
    it('renders the dashboard root element', () => {
      render(<WelcomeDashboard {...buildProps()} />);
      expect(screen.getByTestId('welcome-dashboard')).toBeInTheDocument();
    });
  });
});
