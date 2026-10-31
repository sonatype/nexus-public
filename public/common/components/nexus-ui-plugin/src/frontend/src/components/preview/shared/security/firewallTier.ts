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
 * Firewall Tier Detection Utility
 *
 * Distinguishes between:
 * - None: No firewall protection
 * - Pro: Malware-only protection
 * - Enterprise: Full IQ Server integration with policy enforcement
 */

import { ExtJS } from '../../../../interface/ExtJS';
import {
  FIREWALL_SUPPORTED_FORMATS,
  isFirewallAnalyzableFormat as isFirewallEnabledFormat,
} from '../../../../utils/firewallFormats';

/**
 * State key published by MaliciousRiskStateContributor (backend). True when Repository Firewall is
 * available to this instance — the always-on capability on Pro+/Enterprise, or a configured CLM
 * firewall on plain Pro (NEXUS-54874). It is the authoritative "included in your license" signal:
 * derive card copy and upgrade prompts from this capability flag rather than from the edition short
 * name. Independent of whether an IQ Server connection is currently live (clm.hasFirewall) — an
 * edition that includes Firewall must never be pitched an "upgrade" even before IQ is connected.
 */
export const FIREWALL_AVAILABLE_STATE_KEY = 'nexus.firewall.available';

export type FirewallTier = 'none' | 'pro' | 'enterprise';

export interface FirewallTierInfo {
  tier: FirewallTier;
  hasFirewall: boolean;
  hasFirewallPro: boolean;
  hasFirewallEnterprise: boolean;
  hasIqConnection: boolean;
  displayName: string;
  description: string;
}

/**
 * Check if IQ Server is connected (Firewall Enterprise).
 */
export function isIqServerConnected(): boolean {
  const clm = ExtJS.state()?.getValue?.('clm');
  return Boolean((clm?.enabled));
}

/**
 * Check if the IQ Server has Firewall license/capability.
 */
export function hasIqFirewallCapability(): boolean {
  const clm = ExtJS.state()?.getValue?.('clm');
  return Boolean((clm?.hasFirewall));
}

/**
 * Determine the current firewall tier.
 *
 * Priority:
 * 1. If IQ Server connected with Firewall capability → Enterprise
 * 2. Otherwise → None
 */
export function getFirewallTier(): FirewallTier {
  if (isIqServerConnected() && hasIqFirewallCapability()) {
    return 'enterprise';
  }
  return 'none';
}

/**
 * Get detailed firewall tier information.
 */
export function getFirewallTierInfo(): FirewallTierInfo {
  const tier = getFirewallTier();
  const iqConnected = isIqServerConnected();

  const tierInfo: Record<FirewallTier, Pick<FirewallTierInfo, 'displayName' | 'description'>> = {
    none: {
      displayName: 'No Firewall',
      description: 'Repository is not protected from malicious components.',
    },
    pro: {
      displayName: 'Firewall Pro',
      description: 'Blocks known malicious packages.',
    },
    enterprise: {
      displayName: 'Firewall Enterprise',
      description: 'Full IQ Server integration with policy enforcement and SCA governance.',
    },
  };

  return {
    tier,
    hasFirewall: tier !== 'none',
    hasFirewallPro: tier === 'pro',
    hasFirewallEnterprise: tier === 'enterprise',
    hasIqConnection: iqConnected,
    ...tierInfo[tier],
  };
}

/**
 * React hook to get firewall tier with ExtJS state reactivity.
 */
export function useFirewallTier(): FirewallTierInfo {
  // Subscribe to CLM state changes - handle test environment where ExtJS.useState may not exist
  let clm: { enabled?: boolean; hasFirewall?: boolean } | undefined;
  try {
    clm = ExtJS.useState?.(() => ExtJS.state()?.getValue?.('clm'));
  } catch {
    // In test environment, ExtJS.useState may not be available
    clm = ExtJS.state?.()?.getValue?.('clm');
  }

  const iqConnected = Boolean((clm?.enabled));
  const hasIqFirewall = Boolean((clm?.hasFirewall));

  const tier: FirewallTier = (iqConnected && hasIqFirewall) ? 'enterprise' : 'none';

  const tierInfo: Record<FirewallTier, Pick<FirewallTierInfo, 'displayName' | 'description'>> = {
    none: {
      displayName: 'No Firewall',
      description: 'Repository is not protected from malicious components.',
    },
    pro: {
      displayName: 'Firewall Pro',
      description: 'Blocks known malicious packages.',
    },
    enterprise: {
      displayName: 'Firewall Enterprise',
      description: 'Full IQ Server integration with policy enforcement and SCA governance.',
    },
  };

  return {
    tier,
    hasFirewall: tier !== 'none',
    hasFirewallPro: tier === 'pro',
    hasFirewallEnterprise: tier === 'enterprise',
    hasIqConnection: iqConnected,
    ...tierInfo[tier],
  };
}

/**
 * Formats supported by Firewall Pro.
 */
export const FIREWALL_PRO_SUPPORTED_FORMATS = ['npm', 'nuget', 'pypi', 'maven2'] as const;

/**
 * Check if a format is supported by Firewall Pro.
 */
export function isFirewallProSupportedFormat(format: string): boolean {
  return FIREWALL_PRO_SUPPORTED_FORMATS.includes(format.toLowerCase() as any);
}

/**
 * Formats supported by Firewall Enterprise (IQ Server).
 *
 * Enterprise supports every format Repository Firewall supports, so this is derived from
 * FIREWALL_SUPPORTED_FORMATS — the single frontend mirror of the backend
 * FirewallSupportedFormat enum — rather than maintained as a second list here. The
 * hand-maintained list it replaces had drifted: it used 'golang' (the NXRM format id is
 * 'go') and omitted yum, docker, raw, conda, r, p2 and huggingface.
 *
 * Exposed as a function rather than a module-level constant so formats still behind a
 * rollout flag (see firewallFormats FLAG_GATED_FORMATS, e.g. 'swift', 'pub') are excluded
 * while their flag is off: flag state lives in NX.State and must be read at call time,
 * not baked in at import time.
 */
export function getFirewallEnterpriseSupportedFormats(): readonly string[] {
  return [...FIREWALL_SUPPORTED_FORMATS].filter((format) =>
    isFirewallEnabledFormat(format)
  );
}

/**
 * Check if a format is supported by Firewall Enterprise.
 *
 * Routed through firewallFormats.isFirewallAnalyzableFormat so flag-gated formats report
 * unsupported while their rollout flag is off, mirroring the enterprise branch of the
 * tier-aware isFirewallSupportedFormat below.
 *
 * Deliberately NOT firewallFormats.isFirewallSupportedFormat: that is the UI-visibility gate and
 * carries the FIRE-956 blanket limited-support override, which lifts the backend's *configuration*
 * restriction without giving IQ any new analysis capability. Routing this through it would report
 * Enterprise protection for a format IQ cannot analyze.
 */
export function isFirewallEnterpriseSupportedFormat(format: string): boolean {
  return isFirewallEnabledFormat(format);
}

/**
 * Tier/license-aware check: returns true when the given format is supported by the
 * current Firewall tier (Enterprise or Pro), taking the active license into account.
 *
 * Use this function for Firewall status display, protection-level controls, and other
 * logic that depends on the active license tier.
 *
 * See also: firewallFormats.isFirewallSupportedFormat for the format-only, tier-agnostic
 * variant used for UI-visibility gating (wizard step, edit tab, etc.).
 *
 * The enterprise branch goes through firewallFormats.isFirewallAnalyzableFormat rather than
 * the plain enum-membership check, so formats still behind a rollout flag (see
 * FLAG_GATED_FORMATS) report unsupported while their flag is off — otherwise turning the
 * flag off would leave this reporting supported for a format the backend rejects.
 *
 * It must not go through the visibility gate isFirewallSupportedFormat, which additionally honours
 * the FIRE-956 blanket limited-support override. That override makes a format *configurable*, not
 * *analyzable*, so letting it reach this tier path would overstate the protection level.
 */
export function isFirewallSupportedFormat(format: string, tier?: FirewallTier): boolean {
  const currentTier = tier ?? getFirewallTier();

  switch (currentTier) {
    case 'enterprise':
      return isFirewallEnabledFormat(format);
    case 'pro':
      return isFirewallProSupportedFormat(format);
    default:
      return false;
  }
}
