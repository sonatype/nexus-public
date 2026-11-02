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

import React from 'react';
import { Box } from '@radix-ui/themes';
import { PageHeader } from '../../shared/PageHeader';
import type { FirewallSecurityTabId } from '../../shared/firewallSecurityPermissions';
import {
  useFirewallSecurityPagesState,
  useFirewallSecurityPagesTier,
} from '../../shared/useFirewallSecurityPagesTier';
import { useFirewallSecurityPagesVisible } from '../../shared/useFirewallSecurityPagesVisible';
import { MissingRoutePage } from '../MissingRoutePage';
import { FirewallSecurityPageShell } from './FirewallSecurityPageShell';
import { FirewallSecurityTabPanel } from './FirewallSecurityTabPanel';
import { FIREWALL_SECURITY_TABS } from './firewallSecurityTabs';
import { useVisibleFirewallSecurityTabs } from './useVisibleFirewallSecurityTabs';

export interface FirewallSecuritySectionPageProps {
  tabId: FirewallSecurityTabId;
}

/**
 * One Security section (Quarantine, Waivers, Malware, Evaluations or Policies) framed by the tier shell. Each
 * section has its own route and nav item.
 *
 * Renders the 404 page when the Security pages are not visible, or the user may not see this section, so the
 * route is unusable even when reached by URL. The notice tiers (upgrade-iq, error) also require the section's read
 * permission; the shell then explains what is wrong. Live content is mounted only in the live tier.
 */
export function FirewallSecuritySectionPage({ tabId }: FirewallSecuritySectionPageProps): JSX.Element {
  const tier = useFirewallSecurityPagesTier();
  const state = useFirewallSecurityPagesState();
  const visible = useFirewallSecurityPagesVisible();
  const tabs = useVisibleFirewallSecurityTabs(tier);

  const tab = FIREWALL_SECURITY_TABS.find((candidate) => candidate.id === tabId);
  const permitted = tabs.some((candidate) => candidate.id === tabId);

  if (!(visible && tab && permitted)) {
    return <MissingRoutePage />;
  }

  return (
    <Box
      data-testid="security-page"
      px={{ initial: '4', md: '6', lg: '6' }}
      py={{ initial: '4', md: '5', lg: '6' }}
      width="100%"
      style={{ minWidth: 0, boxSizing: 'border-box' }}
    >
      <Box mb="4">
        <PageHeader title={tab.label} />
      </Box>
      <FirewallSecurityPageShell tier={tier} minIqVersion={state?.minIqVersion}>
        <FirewallSecurityTabPanel tab={tab} tier={tier} />
      </FirewallSecurityPageShell>
    </Box>
  );
}

export const FirewallSecurityMalwarePage = () => <FirewallSecuritySectionPage tabId="malware" />;
export const FirewallSecurityWaiversPage = () => <FirewallSecuritySectionPage tabId="waivers" />;
export const FirewallSecurityQuarantinePage = () => <FirewallSecuritySectionPage tabId="quarantine" />;
export const FirewallSecurityPoliciesPage = () => <FirewallSecuritySectionPage tabId="policies" />;
export const FirewallSecurityEvaluationsPage = () => <FirewallSecuritySectionPage tabId="evaluations" />;
