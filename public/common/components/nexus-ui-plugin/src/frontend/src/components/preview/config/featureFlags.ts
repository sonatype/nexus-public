/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
/**
 * Preview UI Feature Flags
 * 
 * Controls which Settings pages show the actual implementation vs Coming Soon.
 * 
 * SECURITY MODEL:
 * - Production: Coming Soon pages ALWAYS show. No way to access WIP pages.
 * - Development: Coming Soon pages show with "View WIP" link. ?enableWip param works.
 * 
 * Set to `true` to show the implemented page in ALL environments.
 * Set to `false` to show Coming Soon (WIP accessible in dev only).
 * 
 * This allows us to:
 * 1. Keep ALL code in place (no deletion/zipping)
 * 2. Ship production with Coming Soon - WIP pages BLOCKED
 * 3. Enable pages one at a time as they're completed
 * 4. Still test WIP pages in development
 */

/**
 * Detect if we're in development mode
 * Development = localhost OR ?debug in URL
 */
export function isDevelopmentMode(): boolean {
  if (typeof window === 'undefined') return false;
  
  const hostname = window.location.hostname;
  const search = window.location.search;
  
  // localhost or ?debug param (Nexus QA/ops convention) — intentional trade-off, WIP pages are not harmful to view.
  const isLocalhost = hostname === 'localhost' || hostname === '127.0.0.1';
  const hasDebugParam = new URLSearchParams(search).has('debug');
  
  return isLocalhost || hasDebugParam;
}

/**
 * Check if mock data mode is active.
 * Only works in development mode. Add ?mock to the URL to enable.
 * Use for local UI testing when no repositories exist.
 * Remove ?mock before committing - mock data is never used in production.
 */
export function isMockMode(): boolean {
  if (typeof window === 'undefined') return false;
  if (!isDevelopmentMode()) return false;
  const params = new URLSearchParams(window.location.search);
  return params.has('mock') || params.get('mock') === '1';
}

export const PREVIEW_FEATURE_FLAGS = {
  // =============================================================================
  // REPOSITORY SECTION
  // =============================================================================
  'repository.repositories': true,
  'repository.blobstores': true,
  'repository.selectors': true,
  'repository.cleanuppolicies': true,
  'repository.routingrules': true,
  'repository.datastore': true,
  'repository.proprietary': true,

  // =============================================================================
  // SECURITY SECTION
  // =============================================================================
  'security.privileges': true,
  'security.roles': true,
  'security.users': true,
  'security.anonymous': true,
  'security.ldap': true,
  'security.saml': true,
  'security.oauth2': true,           // Enabled — NEXUS-54266
  'security.crowd': true,
  'security.realms': true,
  'security.sslcertificates': true,  // Enabled — NEXUS-54265
  'security.usertokens': true,

  // =============================================================================
  // SUPPORT SECTION - ALL ENABLED (read-only pages)
  // =============================================================================
  'support.logs': true,
  'support.logging': true,
  'support.systeminfo': true,
  'support.supportrequest': true,
  'support.supportzip': true,
  'support.metrics': true,

  // =============================================================================
  // SYSTEM SECTION
  // =============================================================================
  'system.tasks': true,
  'system.capabilities': true,
  'system.emailserver': true,
  'system.http': true,
  'system.licensing': true,
  'system.nodes': true,
  'system.upgrade': false,       // Coming Soon
  'system.api': true,

  // =============================================================================
  // IQ SERVER - ENABLED
  // =============================================================================
  'iqserver-connected': true,         

  // =============================================================================
  // USER ACCOUNT
  // =============================================================================
  'useraccount': true,

  // =============================================================================
  // WELCOME PAGE - Tab visibility
  // =============================================================================
  'welcome.usageMetrics': true,   // Enabled — NEXUS-54200

  // =============================================================================
  // REPOSITORY SETTINGS - Tab visibility
  // =============================================================================
  // Coming Soon — NEXUS-53941 shipped the Usage tab, but member-list, where-used,
  // and H2 metrics gaps remain. Flip to true when the follow-up ticket lands.
  'repository.settings.usageTab': false,
};

/**
 * Check if a feature is enabled
 * 
 * SECURITY: URL override ONLY works in development mode.
 * In production, Coming Soon pages cannot be bypassed.
 */
export function isFeatureEnabled(featureKey: string): boolean {
  // First check if feature is officially enabled (works in all environments)
  if (PREVIEW_FEATURE_FLAGS[featureKey] === true) {
    return true;
  }

  // DEVELOPMENT ONLY: Allow URL override for testing WIP pages
  if (isDevelopmentMode() && typeof window !== 'undefined') {
    const params = new URLSearchParams(window.location.search);
    const enableWip = params.get('enableWip');
    if (enableWip === featureKey || enableWip === 'all') {
      return true;
    }
  }

  // Production: Always return false for non-enabled features
  return false;
}

/**
 * Check if WIP preview is available for this feature
 * Returns true only in development mode for non-enabled features
 */
export function canPreviewWip(featureKey: string): boolean {
  // Only show WIP link if:
  // 1. Feature is NOT officially enabled (otherwise there's nothing to preview)
  // 2. We're in development mode
  return !PREVIEW_FEATURE_FLAGS[featureKey] && isDevelopmentMode();
}

/**
 * Get the URL to preview the WIP version of a feature
 */
export function getWipPreviewUrl(featureKey: string): string {
  if (typeof window === 'undefined') return '';
  
  const url = new URL(window.location.href);
  url.searchParams.set('enableWip', featureKey);
  return url.toString();
}

export default PREVIEW_FEATURE_FLAGS;

