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

import FeatureFlags from '../../../constants/FeatureFlags';
import { ExtJS } from '../../../interface/ExtJS';
import { EDITION_ENTERPRISE, EDITION_PRO, EDITION_PRO_PLUS } from '../../../interface/editionUtils';
import { isLocalhost } from '../config/featureFlags';
import { useEditionId } from './hooks/useEditionId';

export type FirewallSecurityPagesTier = 'hidden' | 'preview-pro' | 'preview-no-iq' | 'upgrade-iq' | 'error' | 'live';

const TIER_OVERRIDES: readonly FirewallSecurityPagesTier[] = [
  'hidden',
  'preview-pro',
  'preview-no-iq',
  'upgrade-iq',
  'error',
  'live',
];

/** Shape of the backend state published by FirewallSecurityPagesStateContributor under 'nexus.firewall.security.pages'. */
export interface FirewallSecurityPagesState {
  featureEnabled: boolean;
  entitled: boolean;
  iqConfigured: boolean;
  /** null when IQ is not configured or has not been probed yet. */
  iqReachable: boolean | null;
  iqVersionSupported: boolean;
  minIqVersion: string;
}

/**
 * The raw backend state for the Firewall Security pages, or null when the key is absent (Core and
 * Community have no contributor). Read reactively.
 */
export function useFirewallSecurityPagesState(): FirewallSecurityPagesState | null {
  const state = ExtJS.useState(
    () => ExtJS.state()?.getValue?.(FeatureFlags.FIREWALL_SECURITY_PAGES_STATE) ?? null
  ) as FirewallSecurityPagesState | null;
  return state ?? null;
}

/**
 * Tier for the Firewall Security pages (Malware, Waivers, Quarantine, Policies, Evaluations).
 * Rules, first match wins:
 *
 * 1. flag off, state missing, or edition not Pro/Pro+/Enterprise -> 'hidden'
 * 2. Pro and clm.enabled (the Pro IQ server integration is already in use) -> 'hidden'
 * 3. Pro -> 'preview-pro'
 * 4. IQ Server not configured -> 'preview-no-iq'
 * 5. IQ Server version below the minimum -> 'upgrade-iq'
 * 6. IQ Server unreachable -> 'error'
 * 7. otherwise -> 'live'
 *
 * The state comes from FirewallSecurityPagesStateContributor (Pro, Pro+ and Enterprise only). The edition
 * comes from useEditionId. All are read reactively.
 *
 * The ?tier=<one of the six values> URL override is for local demos and is honoured ONLY on localhost. It
 * deliberately ignores ?debug, which anyone can add to a production URL, so a deployed instance always
 * follows the flag, edition and IQ status. Either way the tier gates what is displayed, never actual
 * access: backend privileges and conditions still apply.
 *
 * This is unrelated to useFirewallTier in preview/shared/security/firewallTier.ts, which is clm-driven.
 */
export function useFirewallSecurityPagesTier(): FirewallSecurityPagesTier {
  const editionId = useEditionId();
  const state = useFirewallSecurityPagesState();
  const clmEnabled = ExtJS.useState(() => ExtJS.state()?.getValue?.('clm')?.enabled === true);

  if (typeof window !== 'undefined' && isLocalhost()) {
    const override = new URLSearchParams(window.location.search).get('tier') as FirewallSecurityPagesTier | null;
    if (override && TIER_OVERRIDES.includes(override)) {
      return override;
    }
  }

  const isPro = editionId === EDITION_PRO;
  const isProPlusOrEnterprise = editionId === EDITION_PRO_PLUS || editionId === EDITION_ENTERPRISE;

  // Rule 1
  if (state?.featureEnabled !== true || !(isPro || isProPlusOrEnterprise)) return 'hidden';
  // Rule 2
  if (isPro && clmEnabled) return 'hidden';
  // Rule 3
  if (isPro) return 'preview-pro';
  // Rule 4
  if (!state.iqConfigured) return 'preview-no-iq';
  // Rule 5
  if (state.iqVersionSupported === false) return 'upgrade-iq';
  // Rule 6
  if (state.iqReachable === false) return 'error';
  // Rule 7
  return 'live';
}
