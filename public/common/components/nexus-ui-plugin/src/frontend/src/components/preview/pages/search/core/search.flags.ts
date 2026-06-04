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
 * GA Search UI Mode Detection
 * 
 * GA search exists ONLY in Preview UI.
 * Default UI continues to use ExtJS-based search.
 * 
 * This is NOT a feature flag - it's simply detecting which UI the user is in:
 * - Preview UI (#preview/browse/search/*) → GA-aggregated Radix search
 * - Default UI (#browse/search/*)         → Legacy ExtJS search (unchanged)
 * 
 * Users switch between UIs via the UI toggle, not a feature flag.
 */

// =============================================================================
// UI MODE DETECTION
// =============================================================================

/**
 * Route prefix for Preview UI.
 */
export const PREVIEW_UI_PREFIX = 'preview.';

/**
 * Checks if the current route is in Preview UI.
 * 
 * @param routeName - Current route name (e.g., 'preview.browse.search.maven')
 * @returns true if in Preview UI
 */
export function isPreviewUI(routeName: string): boolean {
  return routeName.startsWith(PREVIEW_UI_PREFIX);
}

/**
 * Checks if we're in the GA search context (Preview UI Maven search).
 * 
 * @param routeName - Current route name
 * @returns true if in GA search (Preview Maven search)
 */
export function isGASearchContext(routeName: string): boolean {
  return routeName.startsWith('preview.browse.search.maven');
}

// =============================================================================
// URL HELPERS
// =============================================================================

/**
 * Converts a Default UI search URL to Preview UI equivalent.
 * 
 * Example:
 * #browse/search/maven → #preview/browse/search/maven
 */
export function toPreviewSearchUrl(defaultUrl: string): string {
  if (defaultUrl.startsWith('#browse/search/')) {
    return defaultUrl.replace('#browse/search/', '#preview/browse/search/');
  }
  return defaultUrl;
}

/**
 * Converts a Preview UI search URL to Default UI equivalent.
 * 
 * Example:
 * #preview/browse/search/maven → #browse/search/maven
 */
export function toDefaultSearchUrl(previewUrl: string): string {
  if (previewUrl.startsWith('#preview/browse/search/')) {
    return previewUrl.replace('#preview/browse/search/', '#browse/search/');
  }
  return previewUrl;
}

// =============================================================================
// ROUTE VISIBILITY
// =============================================================================

/**
 * Visibility requirements for GA search routes.
 * GA search routes are always visible in Preview UI (no special requirements).
 */
export const GA_SEARCH_VISIBILITY = {
  // No special requirements - visible to all users in Preview UI
};

/**
 * Placeholder for potential future feature flags.
 * Currently unused since GA search is simply Preview UI.
 */
export const GA_SEARCH_FEATURE_FLAGS = {
  // Reserved for future use
  // e.g., 'nexus.search.ga.security.enabled' for security tab
} as const;
