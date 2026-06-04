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

/**
 * Metric Health types for Preview UI
 */

export type HealthStatus = 'healthy' | 'unhealthy' | 'unknown';

export interface HealthCheckResult {
  healthy: boolean;
  message?: string;
  error?: {
    message?: string;
    stack?: string;
  };
  duration?: number;
  timestamp?: string;
}

export interface HealthCheck {
  name: string;
  result: HealthCheckResult;
}

export interface MetricHealthData {
  [checkName: string]: HealthCheckResult;
}

/**
 * Node information for clustered mode
 */
export interface NodeInfo {
  nodeId: string;
  hostname?: string;
  healthy?: boolean;
  message?: string;
}

export interface MetricHealthPageProps {
  className?: string;
}

export interface MetricHealthListProps {
  checks: HealthCheck[];
  selectedCheck: string | null;
  onSelectCheck: (name: string) => void;
  className?: string;
}

export interface MetricHealthDetailProps {
  check: HealthCheck | null;
  className?: string;
}

/**
 * Props for Node List component in clustered mode
 */
export interface NodeListProps {
  nodes: NodeInfo[];
  selectedNode: string | null;
  onSelectNode: (nodeId: string) => void;
  className?: string;
}

/**
 * Format health check name for display
 * e.g., 'threadDeadlockHealthCheck' -> 'Thread Deadlock'
 */
export function formatCheckName(name: string): string {
  // Remove common suffixes
  let formatted = name
    .replace(/HealthCheck$/i, '')
    .replace(/Check$/i, '');

  // Convert camelCase to Title Case with spaces
  formatted = formatted
    .replace(/([A-Z])/g, ' $1')
    .replace(/^./, (str) => str.toUpperCase())
    .trim();

  return formatted || name;
}

/**
 * Get status from health check result
 */
export function getHealthStatus(result: HealthCheckResult): HealthStatus {
  if (result.healthy === true) return 'healthy';
  if (result.healthy === false) return 'unhealthy';
  return 'unknown';
}

/**
 * Sort health checks by status (unhealthy first) then by name
 */
export function sortHealthChecks(checks: HealthCheck[]): HealthCheck[] {
  return [...checks].sort((a, b) => {
    const statusA = getHealthStatus(a.result);
    const statusB = getHealthStatus(b.result);

    // Unhealthy first
    if (statusA === 'unhealthy' && statusB !== 'unhealthy') return -1;
    if (statusB === 'unhealthy' && statusA !== 'unhealthy') return 1;

    // Then unknown
    if (statusA === 'unknown' && statusB === 'healthy') return -1;
    if (statusB === 'unknown' && statusA === 'healthy') return 1;

    // Then alphabetically
    return formatCheckName(a.name).localeCompare(formatCheckName(b.name));
  });
}


