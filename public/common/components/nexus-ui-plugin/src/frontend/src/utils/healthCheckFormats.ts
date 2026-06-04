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
 * Proxy repository formats that support Repository Health Check in the UI (Browse / Protect).
 *
 * Keep in sync with HealthCheckCell. Backend may further exclude some repos (e.g. Maven snapshot policy);
 * details API does not expose that, so we use format-level eligibility here.
 */
const HEALTH_CHECK_SUPPORTED_FORMATS = new Set([
  'maven2',
  'npm',
  'nuget',
  'pypi',
  'rubygems',
  'cocoapods',
  'conan',
  'conda',
  'go',
  'r',
  'apt',
]);

/**
 * Returns true when Health Check can be enabled for this repository format (proxy only — call sites filter type).
 */
export function isHealthCheckSupportedFormat(format: string | null | undefined): boolean {
  return HEALTH_CHECK_SUPPORTED_FORMATS.has(format?.toLowerCase?.() ?? '');
}
