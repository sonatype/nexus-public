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
 * Represents a log file in the system
 */
export interface LogFile {
  fileName: string;
  size: number;
  lastModified: number;
}

/**
 * Refresh rate options for log viewer (in seconds)
 */
export const REFRESH_RATES = [
  { value: 0, label: 'Manual' },
  { value: 20, label: 'Every 20 seconds' },
  { value: 60, label: 'Every minute' },
  { value: 120, label: 'Every 2 minutes' },
  { value: 300, label: 'Every 5 minutes' },
] as const;

/**
 * Log view size options (in KB)
 */
export const LOG_SIZES = [
  { value: 25, label: 'Last 25KB' },
  { value: 50, label: 'Last 50KB' },
  { value: 100, label: 'Last 100KB' },
] as const;

/**
 * Sort field options for log list
 */
export type LogSortField = 'fileName' | 'size' | 'lastModified';

/**
 * Sort direction
 */
export type SortDirection = 'asc' | 'desc';

/**
 * API URLs for logs
 */
const encodeLogFilename = (filename: string) => filename.split('/').map(encodeURIComponent).join('/');

export const LOGS_API = {
  LIST: '/service/rest/internal/logging/logs',
  VIEW: (filename: string) => `/service/rest/internal/logging/logs/${encodeLogFilename(filename)}`,
  MARK: '/service/rest/internal/logging/log/mark',
} as const;

