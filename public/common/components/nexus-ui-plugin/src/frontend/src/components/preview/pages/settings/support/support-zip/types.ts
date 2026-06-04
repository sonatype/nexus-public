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
 * Parameters for creating a support ZIP
 */
export interface SupportZipParams {
  systemInformation: boolean;
  threadDump: boolean;
  configuration: boolean;
  security: boolean;
  log: boolean;
  taskLog: boolean;
  auditLog: boolean;
  metrics: boolean;
  jmx: boolean;
  replication: boolean;
  archivedLog: number;
  limitFileSizes: boolean;
  limitZipSize: boolean;
}

/**
 * Response from creating a support ZIP
 */
export interface SupportZipResponse {
  file: string;
  name: string;
  size: string;
  truncated: boolean;
}

/**
 * HA Node information
 */
export interface NodeInfo {
  nodeId: string;
  hostname: string;
  status: 'ACTIVE' | 'INACTIVE' | 'OFFLINE';
  supportZip?: SupportZipResponse | null;
  lastUpdated?: number;
  error?: string;
}

/**
 * Default support ZIP parameters
 */
export const DEFAULT_SUPPORT_ZIP_PARAMS: SupportZipParams = {
  systemInformation: true,
  threadDump: true,
  configuration: true,
  security: true,
  log: true,
  taskLog: true,
  auditLog: true,
  metrics: true,
  jmx: true,
  replication: true,
  archivedLog: 0,
  limitFileSizes: true,
  limitZipSize: true,
};

/**
 * Archived log options
 */
export const ARCHIVED_LOG_OPTIONS = [
  { value: 0, label: 'None' },
  { value: 1, label: '1 Day' },
  { value: 2, label: '2 Days' },
  { value: 3, label: '3 Days' },
] as const;

/**
 * API URLs for support ZIP
 */
export const SUPPORT_ZIP_API = {
  CREATE: '/service/rest/v1/support/supportzippath',
  CREATE_HA: '/service/rest/v1/nodes/supportzips',
  DOWNLOAD: (filename: string) => `service/rest/wonderland/download/${filename}`,
} as const;


