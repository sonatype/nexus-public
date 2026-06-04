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
import { Box, Text } from '@radix-ui/themes';
import type { HealthCheckData, RepositoryMetrics } from './hooks/useRepositoryProfile';

// =============================================================================
// Types
// =============================================================================

interface HealthScoreCardProps {
  healthCheck: HealthCheckData | null;
  metrics: RepositoryMetrics | null;
  repositoryName: string;
}

// =============================================================================
// Component
// =============================================================================

/**
 * HealthScoreCard - Displays a health score and key metrics for a repository
 *
 * Follows the Sonatype Guide "Developer Trust Score" pattern.
 */
export function HealthScoreCard({
  healthCheck,
  metrics,
  repositoryName,
}: HealthScoreCardProps): JSX.Element {
  // Calculate health score (simplified)
  // In a real implementation, this would come from the health check API
  const calculateScore = (): number | null => {
    if (!healthCheck?.enabled) return null;

    // Simple scoring: start at 100, subtract for issues
    let score = 100;
    if (healthCheck.securityIssueCount) {
      score -= healthCheck.securityIssueCount * 5;
    }
    if (healthCheck.licenseIssueCount) {
      score -= healthCheck.licenseIssueCount * 2;
    }
    return Math.max(0, Math.min(100, score));
  };

  const score = calculateScore();

  const getScoreClass = (score: number | null): string => {
    if (score === null) return 'unknown';
    if (score >= 90) return 'excellent';
    if (score >= 70) return 'good';
    if (score >= 50) return 'fair';
    return 'poor';
  };

  const scoreClass = getScoreClass(score);

  return (
    <Box className="health-score-card">
      <Text className="health-score-card__title">Health Score</Text>

      <Text className={`health-score-card__score health-score-card__score--${scoreClass}`}>
        {score !== null ? score : '—'}
      </Text>

      <Text className="health-score-card__label">
        {score !== null ? 'out of 100' : 'Not available'}
      </Text>

      <Box className="health-score-card__divider" />

      <Box className="health-score-card__stats">
        <Box className="health-score-card__stat">
          <Text className="health-score-card__stat-label">Components</Text>
          <Text className="health-score-card__stat-value">
            {metrics?.componentCount?.toLocaleString() ?? '—'}
          </Text>
        </Box>

        <Box className="health-score-card__stat">
          <Text className="health-score-card__stat-label">Assets</Text>
          <Text className="health-score-card__stat-value">
            {metrics?.assetCount?.toLocaleString() ?? '—'}
          </Text>
        </Box>

        {healthCheck?.securityIssueCount !== undefined && (
          <Box className="health-score-card__stat">
            <Text className="health-score-card__stat-label">Security Issues</Text>
            <Text className="health-score-card__stat-value">
              {healthCheck.securityIssueCount}
            </Text>
          </Box>
        )}
      </Box>
    </Box>
  );
}

export default HealthScoreCard;


