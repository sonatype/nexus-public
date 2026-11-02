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

import {
  classifyFirewallStatus,
  isFirewallActiveMessage,
  FIREWALL_MESSAGE_AUDIT_ENABLED,
  FIREWALL_MESSAGE_QUARANTINE_ENABLED,
} from '../firewallStatusKind';
import type { SecurityStatusData } from '../security.types';

const row = (message: string | null, extra: Partial<SecurityStatusData> = {}): SecurityStatusData => ({
  repositoryName: 'r',
  affectedComponentCount: 0,
  criticalComponentCount: 0,
  severeComponentCount: 0,
  moderateComponentCount: 0,
  quarantinedComponentCount: 0,
  message,
  ...extra,
});

describe('classifyFirewallStatus', () => {
  it('shouldReportLoadingBeforeSummaryFinishes', () => {
    expect(classifyFirewallStatus({ loaded: false, summaryReady: false })).toBe('loading');
  });

  it('shouldReportUnprotectedOnlyWhenSummaryLoadedAndRepoAbsent', () => {
    expect(classifyFirewallStatus({ loaded: true, summaryReady: true })).toBe('unprotected');
  });

  it('shouldReportUnknownWhenSummaryFailedOrUnreadable', () => {
    expect(classifyFirewallStatus({ loaded: true, summaryReady: false })).toBe('unknown');
  });

  it('shouldMatchModeByExactMessageEquality', () => {
    const quarantine = classifyFirewallStatus({ status: row(FIREWALL_MESSAGE_QUARANTINE_ENABLED), loaded: true, summaryReady: true });
    const audit = classifyFirewallStatus({ status: row(FIREWALL_MESSAGE_AUDIT_ENABLED), loaded: true, summaryReady: true });
    expect(quarantine).toBe('quarantine');
    expect(audit).toBe('audit');
  });

  it('shouldTreatOfflineRepoWhoseNameContainsEnabledAsInactive', () => {
    const status = row("Repository 'audit-enabled-proxy' is offline");
    expect(classifyFirewallStatus({ status, loaded: true, summaryReady: true })).toBe('inactive');
  });

  it('shouldTreatAnyOtherMessageOnListedRepoAsInactiveNotUnprotected', () => {
    expect(classifyFirewallStatus({ status: row('Something else'), loaded: true, summaryReady: true })).toBe('inactive');
  });

  it('shouldReportErrorWhenRowCarriesErrorMessage', () => {
    const status = row(null, { errorMessage: 'IQ Server unreachable' });
    expect(classifyFirewallStatus({ status, loaded: true, summaryReady: true })).toBe('error');
  });

  it('shouldNotTreatSubstringMessagesAsActive', () => {
    expect(isFirewallActiveMessage('Quarantine enabled')).toBe(true);
    expect(isFirewallActiveMessage("Repository 'enabled-audit' is offline")).toBe(false);
    expect(isFirewallActiveMessage(undefined)).toBe(false);
  });
});
