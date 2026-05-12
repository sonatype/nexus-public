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
 * Formats supported by Repository Firewall.
 *
 * Source of truth: FirewallSupportedFormat.java
 *
 * NOTE: p2 and raw are in the backend enum but not in official docs
 * (https://help.sonatype.com/en/repository-firewall.html).
 * They are treated as supported until product team clarification.
 *
 * UNSUPPORTED (show N/A): terraform, apt, helm, gitlfs, swift
 */
export const FIREWALL_SUPPORTED_FORMATS = new Set([
  'r',
  'go',
  'p2',
  'rubygems',
  'npm',
  'yum',
  'pypi',
  'cargo',
  'conan',
  'conda',
  'nuget',
  'maven2',
  'composer',
  'cocoapods',
  'huggingface',
  'docker',
  'raw',
]);

/**
 * Returns true when Repository Firewall supports the given format.
 * Case-insensitive. Returns false for null/undefined/empty.
 */
export function isFirewallSupportedFormat(format: string | null | undefined): boolean {
  return FIREWALL_SUPPORTED_FORMATS.has(format?.toLowerCase?.() ?? '');
}
