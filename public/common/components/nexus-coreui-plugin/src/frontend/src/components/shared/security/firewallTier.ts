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

import { ExtJS } from '@sonatype/nexus-ui-plugin';

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
  return !!(clm?.enabled);
}

/**
 * Check if the IQ Server has Firewall license/capability.
 */
export function hasIqFirewallCapability(): boolean {
  const clm = ExtJS.state()?.getValue?.('clm');
  return !!(clm?.hasFirewall);
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

  const iqConnected = !!(clm?.enabled);
  const hasIqFirewall = !!(clm?.hasFirewall);

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
 * Enterprise supports all formats that IQ Server can analyze.
 */
export const FIREWALL_ENTERPRISE_SUPPORTED_FORMATS = [
  'maven2',
  'npm',
  'nuget',
  'pypi',
  'rubygems',
  'golang',
  'conan',
  'cargo',
  'cocoapods',
  'composer',
] as const;

/**
 * Check if a format is supported by Firewall Enterprise.
 */
export function isFirewallEnterpriseSupportedFormat(format: string): boolean {
  return FIREWALL_ENTERPRISE_SUPPORTED_FORMATS.includes(format.toLowerCase() as any);
}

/**
 * Check if a format is supported by the current firewall tier.
 */
export function isFirewallSupportedFormat(format: string, tier?: FirewallTier): boolean {
  const currentTier = tier ?? getFirewallTier();

  switch (currentTier) {
    case 'enterprise':
      return isFirewallEnterpriseSupportedFormat(format);
    case 'pro':
      return isFirewallProSupportedFormat(format);
    default:
      return false;
  }
}
