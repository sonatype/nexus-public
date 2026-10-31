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

import { FORMAT_LABELS } from '../components/preview/shared/Badges/formatConstants';

/**
 * Hosted repository formats for which Hosted Repository Scanning (HRS) monitoring can be enabled.
 *
 * Used by the Preview and legacy React UIs.
 * Values are repository format IDs (maven2, not Maven).
 */
export const HRS_SUPPORTED_FORMATS: readonly string[] = [
  'maven2', 'npm', 'pypi', 'rubygems', 'nuget', 'conda', 'r', 'go', 'pub', 'helm',
];

/**
 * Display name of a repository format, for HRS messages in both UIs. An unknown format falls back to its ID
 * rather than a guessed capitalisation (that would turn "oci" into "Oci"); add the format to FORMAT_LABELS instead.
 */
export function hostedRepoEvaluationFormatLabel(format: string): string {
  return FORMAT_LABELS[format] || format;
}

/** Display names of HRS_SUPPORTED_FORMATS, in the same order, for user-facing messages. */
export const HRS_SUPPORTED_FORMAT_NAMES = HRS_SUPPORTED_FORMATS.map(hostedRepoEvaluationFormatLabel).join(', ');

const SUPPORTED = new Set(HRS_SUPPORTED_FORMATS);

/**
 * Returns true when HRS monitoring can be enabled for repositories of this format (hosted only; call sites
 * filter type).
 */
export function isHostedRepoEvaluationSupportedFormat(format: string | null | undefined): boolean {
  return SUPPORTED.has(format?.toLowerCase?.() ?? '');
}

/** Explains why a repository of this format can't be monitored. */
export function hostedRepoEvaluationUnsupportedMessage(formatLabel: string): string {
  return `Hosted repository evaluation is not available for ${formatLabel} repositories. ` +
    `Supported formats: ${HRS_SUPPORTED_FORMAT_NAMES}.`;
}
