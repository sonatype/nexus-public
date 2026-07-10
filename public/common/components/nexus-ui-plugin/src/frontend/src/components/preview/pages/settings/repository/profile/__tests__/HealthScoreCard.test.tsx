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
import { render, screen } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';

import { HealthScoreCard } from '../HealthScoreCard';
import type { HealthCheckData, RepositoryMetrics } from '../types';

// Wrapper component for Radix Theme
function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

describe('HealthScoreCard', () => {
  const mockRepositoryName = 'test-repository';

  describe('renders correctly with various health check states', () => {
    it('renders health score when health check is enabled', () => {
      const healthCheck: HealthCheckData = {
        enabled: true,
        securityIssueCount: 0,
        licenseIssueCount: 0,
      };

      const metrics: RepositoryMetrics = {
        componentCount: 50,
        assetCount: 500,
      } as RepositoryMetrics;

      const { container } = render(
        <HealthScoreCard
          healthCheck={healthCheck}
          metrics={metrics}
          repositoryName={mockRepositoryName}
        />,
        { wrapper: TestWrapper }
      );

      expect(screen.getByText('Health Score')).toBeInTheDocument();
      // Use container query to find the score specifically by class
      const scoreElement = container.querySelector('.health-score-card__score');
      expect(scoreElement).toHaveTextContent('100');
      expect(screen.getByText('out of 100')).toBeInTheDocument();
    });

    it('renders degraded score when there are security issues', () => {
      const healthCheck: HealthCheckData = {
        enabled: true,
        securityIssueCount: 5,
        licenseIssueCount: 2,
      };

      const metrics: RepositoryMetrics = {
        componentCount: 100,
        assetCount: 500,
      } as RepositoryMetrics;

      render(
        <HealthScoreCard
          healthCheck={healthCheck}
          metrics={metrics}
          repositoryName={mockRepositoryName}
        />,
        { wrapper: TestWrapper }
      );

      // Score = 100 - (5 * 5) - (2 * 2) = 100 - 25 - 4 = 71
      expect(screen.getByText('71')).toBeInTheDocument();
    });

    it('renders dash when health check is disabled', () => {
      const healthCheck: HealthCheckData = {
        enabled: false,
      };

      const metrics = {
        componentCount: 100,
        assetCount: 500,
      } as RepositoryMetrics;

      render(
        <HealthScoreCard
          healthCheck={healthCheck}
          metrics={metrics}
          repositoryName={mockRepositoryName}
        />,
        { wrapper: TestWrapper }
      );

      expect(screen.getByText('—')).toBeInTheDocument();
      expect(screen.getByText('Not available')).toBeInTheDocument();
    });

    it('renders dash when health check is null', () => {
      const metrics = {
        componentCount: 100,
        assetCount: 500,
      } as RepositoryMetrics;

      render(
        <HealthScoreCard
          healthCheck={null}
          metrics={metrics}
          repositoryName={mockRepositoryName}
        />,
        { wrapper: TestWrapper }
      );

      expect(screen.getByText('—')).toBeInTheDocument();
      expect(screen.getByText('Not available')).toBeInTheDocument();
    });
  });

  describe('renders metrics correctly', () => {
    it('displays component and asset counts', () => {
      const healthCheck: HealthCheckData = {
        enabled: true,
        securityIssueCount: 0,
        licenseIssueCount: 0,
      };

      const metrics = {
        componentCount: 1234,
        assetCount: 5678,
      } as RepositoryMetrics;

      render(
        <HealthScoreCard
          healthCheck={healthCheck}
          metrics={metrics}
          repositoryName={mockRepositoryName}
        />,
        { wrapper: TestWrapper }
      );

      expect(screen.getByText('Components')).toBeInTheDocument();
      expect(screen.getByText('1,234')).toBeInTheDocument();
      expect(screen.getByText('Assets')).toBeInTheDocument();
      expect(screen.getByText('5,678')).toBeInTheDocument();
    });

    it('displays dash when metrics are null', () => {
      render(
        <HealthScoreCard
          healthCheck={null}
          metrics={null}
          repositoryName={mockRepositoryName}
        />,
        { wrapper: TestWrapper }
      );

      // All numeric fields should show dash
      const dashes = screen.getAllByText('—');
      expect(dashes.length).toBeGreaterThan(0);
    });

    it('displays security issues count when available', () => {
      const healthCheck: HealthCheckData = {
        enabled: true,
        securityIssueCount: 3,
      };

      render(
        <HealthScoreCard
          healthCheck={healthCheck}
          metrics={null}
          repositoryName={mockRepositoryName}
        />,
        { wrapper: TestWrapper }
      );

      expect(screen.getByText('Security Issues')).toBeInTheDocument();
      expect(screen.getByText('3')).toBeInTheDocument();
    });
  });

  describe('score classification', () => {
    it('shows excellent class for score >= 90', () => {
      const healthCheck: HealthCheckData = {
        enabled: true,
        securityIssueCount: 1,
        licenseIssueCount: 0,
      };

      const { container } = render(
        <HealthScoreCard
          healthCheck={healthCheck}
          metrics={null}
          repositoryName={mockRepositoryName}
        />,
        { wrapper: TestWrapper }
      );

      // Score = 100 - 5 = 95 (excellent)
      const scoreElement = container.querySelector('.health-score-card__score--excellent');
      expect(scoreElement).toBeTruthy();
      expect(scoreElement).toHaveTextContent('95');
    });

    it('shows good class for score 70-89', () => {
      const healthCheck: HealthCheckData = {
        enabled: true,
        securityIssueCount: 4, // -20 points
        licenseIssueCount: 3, // -6 points
      };

      const { container } = render(
        <HealthScoreCard
          healthCheck={healthCheck}
          metrics={null}
          repositoryName={mockRepositoryName}
        />,
        { wrapper: TestWrapper }
      );

      // Score = 100 - 20 - 6 = 74 (good)
      const scoreElement = container.querySelector('.health-score-card__score--good');
      expect(scoreElement).toBeTruthy();
      expect(scoreElement).toHaveTextContent('74');
    });

    it('shows fair class for score 50-69', () => {
      const healthCheck: HealthCheckData = {
        enabled: true,
        securityIssueCount: 8, // -40 points
        licenseIssueCount: 5, // -10 points
      };

      const { container } = render(
        <HealthScoreCard
          healthCheck={healthCheck}
          metrics={null}
          repositoryName={mockRepositoryName}
        />,
        { wrapper: TestWrapper }
      );

      // Score = 100 - 40 - 10 = 50 (fair)
      const scoreElement = container.querySelector('.health-score-card__score--fair');
      expect(scoreElement).toBeTruthy();
      expect(scoreElement).toHaveTextContent('50');
    });

    it('shows poor class for score < 50', () => {
      const healthCheck: HealthCheckData = {
        enabled: true,
        securityIssueCount: 15, // -75 points
        licenseIssueCount: 10, // -20 points
      };

      const { container } = render(
        <HealthScoreCard
          healthCheck={healthCheck}
          metrics={null}
          repositoryName={mockRepositoryName}
        />,
        { wrapper: TestWrapper }
      );

      // Score = max(0, 100 - 75 - 20) = 5 (poor)
      const scoreElement = container.querySelector('.health-score-card__score--poor');
      expect(scoreElement).toBeTruthy();
      expect(scoreElement).toHaveTextContent('5');
    });

    it('clamps score to minimum of 0', () => {
      const healthCheck: HealthCheckData = {
        enabled: true,
        securityIssueCount: 50, // -250 points
        licenseIssueCount: 50, // -100 points
      };

      const { container } = render(
        <HealthScoreCard
          healthCheck={healthCheck}
          metrics={null}
          repositoryName={mockRepositoryName}
        />,
        { wrapper: TestWrapper }
      );

      // Score should be clamped to 0
      const scoreElement = container.querySelector('.health-score-card__score');
      expect(scoreElement).toHaveTextContent('0');
    });
  });
});

