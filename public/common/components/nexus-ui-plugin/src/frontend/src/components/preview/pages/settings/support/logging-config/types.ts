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
 * Represents a logger configuration
 */
export interface Logger {
  name: string;
  level: LogLevel;
  override?: boolean;
}

/**
 * Available log levels
 */
export type LogLevel = 'OFF' | 'ERROR' | 'WARN' | 'INFO' | 'DEBUG' | 'TRACE' | 'DEFAULT';

/**
 * Log levels in order of verbosity
 */
export const LOG_LEVELS: LogLevel[] = ['OFF', 'ERROR', 'WARN', 'INFO', 'DEBUG', 'TRACE'];

/**
 * Form data for creating/editing a logger
 */
export interface LoggerFormData {
  name: string;
  level: LogLevel;
}

/**
 * Sort field options for logger list
 */
export type LoggerSortField = 'name' | 'level';

/**
 * Sort direction
 */
export type SortDirection = 'asc' | 'desc';

/**
 * API URLs for logging configuration
 */
export const LOGGING_CONFIG_API = {
  LIST: '/service/rest/internal/ui/loggingConfiguration',
  GET: (name: string) => `/service/rest/internal/ui/loggingConfiguration/${encodeURIComponent(name)}`,
  UPDATE: (name: string) => `/service/rest/internal/ui/loggingConfiguration/${encodeURIComponent(name)}`,
  RESET: (name: string) => `/service/rest/internal/ui/loggingConfiguration/${encodeURIComponent(name)}/reset`,
  RESET_ALL: '/service/rest/internal/ui/loggingConfiguration/reset',
} as const;


