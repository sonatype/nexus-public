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

export const USAGE_STRINGS = {
  TITLE: 'Usage',
  MENU_DESCRIPTION: 'Monitor historical usage trends.',
  HISTORICAL_TITLE: 'Historical Usage',
  DESCRIPTION: 'Monitor your storage usage trends over time.',
  LEARN_MORE: 'Learn how usage is calculated',
  LEARN_MORE_URL: 'http://links.sonatype.com/products/nxrm3/license/historical-usage',
  UPDATE_FREQUENCY:
    'Storage usage metrics may take up to 72 hours to update. Recent repository activity, such as publishing, downloading, or deleting components, may not appear immediately.',
  STORAGE_NOTE_LABEL: 'Note:',
  STORAGE_NOTE:
    'This value may differ from the sum of individual repository storage totals. The storage usage includes: version history retained for 45 days after deletion, overwritten file versions retained for 30 days, and tenant access logs from the past 90 days.',
  LOAD_ERROR: 'Unable to load historical usage data. Please try again later.',
  PERMISSION_ERROR:
    'Insufficient Permissions: You need administrator privileges to view usage data.',
  EMPTY: 'No historical usage data available',
  RETRY: 'Retry',
  CHART_TITLE: 'Usage Insights',
  CHART_MONTH_LABEL: 'Month',
  CHART_LEGEND_EGRESS: 'Total Egress',
  CHART_LEGEND_STORAGE: 'Peak Storage',
  columns: {
    month: 'Month',
    totalEgress: 'Total Egress',
    egressChange: 'Egress % Change',
    peakStorage: 'Peak Storage',
    storageChange: 'Storage % Change',
    totalUsage: 'Total Usage',
  },
  tooltips: {
    egressChange: 'Change rate of the total monthly egress from the previous month.',
    storageChange: 'Change rate of the total monthly storage from the previous month.',
    totalUsage: 'Combined total of egress and storage usage. This is your total usage value.',
  },
} as const;
