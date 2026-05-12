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
 * Dashboard Components - REAL DATA ONLY
 * 
 * No MOCK data. All panels display data from real APIs.
 */

// Types
export type {
  Repository,
  RepositoryFormatSummary,
  InstanceTotals,
  SimplifiedPanelProps,
  RepositoriesByFormatPanelProps,
  InstanceTotalsPanelProps,
} from './simplified.types';

export type { DataPoint, UsageHistoryState } from './useUsageHistory';

// Components
export { RepositoriesByFormatPanel } from './RepositoriesByFormatPanel';
export { InstanceTotalsPanel, type InstanceTotalsPanelWithSparklineProps } from './InstanceTotalsPanel';
export { QuickActionStatsPanel, type QuickActionStatsPanelProps } from './QuickActionStatsPanel';
export { Sparkline, type SparklineProps } from './Sparkline';

// Hooks
export { useRepositoriesByFormat } from './useRepositoriesByFormat';
export { useInstanceTotals } from './useInstanceTotals';
export { useUsageHistory } from './useUsageHistory';
export { useMonthlyMetrics, formatBytesToGB } from './useMonthlyMetrics';
export { useInstanceStorage } from './useInstanceStorage';