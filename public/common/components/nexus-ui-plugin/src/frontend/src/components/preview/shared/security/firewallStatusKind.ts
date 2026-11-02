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

import type { SecurityStatusData } from './security.types';

/** Exact `message` values the backend emits for an active firewall (FirewallRepositoryStatusComponent). */
export const FIREWALL_MESSAGE_QUARANTINE_ENABLED = 'Quarantine enabled';
export const FIREWALL_MESSAGE_AUDIT_ENABLED = 'Audit enabled';

export type FirewallStatusKind =
  | 'loading'
  | 'unknown'
  | 'unprotected'
  | 'quarantine'
  | 'audit'
  | 'inactive'
  | 'error';

export interface ClassifyFirewallStatusInput {
  /** The repository's row in the firewall summary, if any */
  status?: SecurityStatusData;
  /** The summary request has finished (successfully or not) */
  loaded: boolean;
  /** The summary request succeeded, so a missing row really means firewall is not enabled */
  summaryReady: boolean;
}

/** True only for the two messages that mean firewall is enabled and working. */
export function isFirewallActiveMessage(message?: string | null): boolean {
  return message === FIREWALL_MESSAGE_QUARANTINE_ENABLED || message === FIREWALL_MESSAGE_AUDIT_ENABLED;
}

/**
 * Classify a repository's firewall state. "Unprotected" is only claimed when the summary loaded
 * successfully and omits the repository (the backend omits repositories without firewall enabled).
 * Any other message on a listed repository (e.g. "Repository 'x' is offline") is enabled-but-inactive.
 */
export function classifyFirewallStatus({
  status,
  loaded,
  summaryReady,
}: ClassifyFirewallStatusInput): FirewallStatusKind {
  if (!status) {
    if (!loaded) return 'loading';
    return summaryReady ? 'unprotected' : 'unknown';
  }
  if (status.errorMessage) return 'error';
  if (status.message === FIREWALL_MESSAGE_QUARANTINE_ENABLED) return 'quarantine';
  if (status.message === FIREWALL_MESSAGE_AUDIT_ENABLED) return 'audit';
  return 'inactive';
}
