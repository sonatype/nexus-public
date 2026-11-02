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

import type React from 'react';
import { FIREWALL_SECURITY_PERMISSIONS, type FirewallSecurityTabId } from '../../shared/firewallSecurityPermissions';
import { EvaluationsLivePlaceholder } from './tabs/evaluations/EvaluationsLivePlaceholder';
import { EvaluationsPreviewView } from './tabs/evaluations/EvaluationsPreviewView';
import { MalwareLivePlaceholder } from './tabs/malware/MalwareLivePlaceholder';
import { MalwarePreviewView } from './tabs/malware/MalwarePreviewView';
import { PoliciesLivePlaceholder } from './tabs/policies/PoliciesLivePlaceholder';
import { PoliciesPreviewView } from './tabs/policies/PoliciesPreviewView';
import { QuarantineLivePlaceholder } from './tabs/quarantine/QuarantineLivePlaceholder';
import { QuarantinePreviewView } from './tabs/quarantine/QuarantinePreviewView';
import { WaiversLivePlaceholder } from './tabs/waivers/WaiversLivePlaceholder';
import { WaiversPreviewView } from './tabs/waivers/WaiversPreviewView';

export interface FirewallSecurityTab {
  id: FirewallSecurityTabId;
  label: string;
  /** Read permission required to see this tab. Only the 'preview-pro' tier ignores it (Pro has no such privileges), and still requires a signed-in user. */
  permission: string;
  /** Fixture-driven example content shown in the preview tiers. */
  Preview: React.ComponentType;
  /** Real content, mounted only in the 'live' tier. */
  Live: React.ComponentType;
}

/** Tab order is the display order. */
export const FIREWALL_SECURITY_TABS: FirewallSecurityTab[] = [
  {
    id: 'malware',
    label: 'Malware',
    permission: FIREWALL_SECURITY_PERMISSIONS.malware,
    Preview: MalwarePreviewView,
    Live: MalwareLivePlaceholder,
  },
  {
    id: 'waivers',
    label: 'Waivers',
    permission: FIREWALL_SECURITY_PERMISSIONS.waivers,
    Preview: WaiversPreviewView,
    Live: WaiversLivePlaceholder,
  },
  {
    id: 'quarantine',
    label: 'Quarantine',
    permission: FIREWALL_SECURITY_PERMISSIONS.quarantine,
    Preview: QuarantinePreviewView,
    Live: QuarantineLivePlaceholder,
  },
  {
    id: 'policies',
    label: 'Policies',
    permission: FIREWALL_SECURITY_PERMISSIONS.policies,
    Preview: PoliciesPreviewView,
    Live: PoliciesLivePlaceholder,
  },
  {
    id: 'evaluations',
    label: 'Evaluations',
    permission: FIREWALL_SECURITY_PERMISSIONS.evaluations,
    Preview: EvaluationsPreviewView,
    Live: EvaluationsLivePlaceholder,
  },
];
