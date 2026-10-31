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
 * Rapture state key for the edition identifier published by the backend.
 * See StatusStateContributor (self-hosted) and EditionStateContributor (cloud).
 */
export const EDITION_ID_STATE_KEY = 'editionId';

// Edition identifiers — match NexusEdition.getId() on the backend
export const EDITION_CORE = 'nexus-core-edition';
export const EDITION_COMMUNITY = 'nexus-community-edition';
export const EDITION_PRO = 'nexus-professional-edition';
export const EDITION_PRO_PLUS = 'nexus-professional-plus-edition';
export const EDITION_ENTERPRISE = 'nexus-enterprise-edition';
/** Cloud base edition — resolves to the same display name as Pro. */
export const EDITION_CLOUD = 'nexus-cloud-edition';

/** Union of the known editionId values published by the backend. */
export type EditionId =
  | typeof EDITION_CORE
  | typeof EDITION_COMMUNITY
  | typeof EDITION_PRO
  | typeof EDITION_PRO_PLUS
  | typeof EDITION_ENTERPRISE
  | typeof EDITION_CLOUD;

/**
 * Licensed Pro-family editions — the group referenced by most edition-gated routes.
 * Prefer this over ad-hoc inline arrays so a new tier can be added in one place.
 */
export const PRO_FAMILY_EDITIONS: readonly EditionId[] = [
  EDITION_PRO,
  EDITION_PRO_PLUS,
  EDITION_ENTERPRISE,
  EDITION_CLOUD,
];

/**
 * Union of Community plus every Pro-family edition — the group used by routes and settings
 * cards that are available on every licensed edition (e.g. the Data Store screen).
 */
export const PRO_AND_COMMUNITY_EDITIONS: readonly EditionId[] = [
  EDITION_COMMUNITY,
  ...PRO_FAMILY_EDITIONS,
];

/**
 * Legacy short-name → editionId map. Used as a fallback in {@link resolveEditionId} when the
 * backend's {@code editionId} state key is absent (StatusStateContributor wraps its publish
 * in a try-catch). Only lists short names the backend actually publishes; Cloud reports
 * {@code PRO} as its short name (Cloud is a distribution, not an edition), so it deliberately
 * has no map entry.
 * KEEP IN SYNC with NX.State's edition constants (nexus-rapture/.../NX/State.js).
 */
export const EDITION_ID_BY_SHORT_NAME: Record<string, EditionId> = {
  CORE: EDITION_CORE,
  COMMUNITY: EDITION_COMMUNITY,
  PRO: EDITION_PRO,
  PROPLUS: EDITION_PRO_PLUS,
  ENTERPRISE: EDITION_ENTERPRISE,
};

/** State-shaped input accepted by {@link resolveEditionId}. */
export type EditionIdStateSource = {
  getValue(key: string): unknown;
  getEdition?(): string | undefined;
};

/**
 * Resolves the current editionId from a Rapture state source. Prefers the stable
 * {@code editionId} state key and falls back to mapping the always-published short-name
 * edition when the state key is absent (StatusStateContributor wraps its {@code editionId}
 * publish in try/catch and silently omits the key on failure).
 *
 * Centralised so the fallback logic only needs to be reasoned about — and tested — in one
 * place; callers include {@code meetsEditionRequirement}, {@code ExtJS.isLicensedEdition},
 * and {@code CELimitsAlert}.
 */
export function resolveEditionId(
  state: EditionIdStateSource | null | undefined,
): EditionId | undefined {
  if (!state) return undefined;
  const stateEditionId = state.getValue?.(EDITION_ID_STATE_KEY) as EditionId | undefined;
  if (stateEditionId) return stateEditionId;
  const shortName = state.getEdition?.();
  return shortName ? EDITION_ID_BY_SHORT_NAME[shortName] : undefined;
}

// KEEP IN SYNC with the DISPLAY_NAMES table in NX.State.getEditionDisplayName
// (nexus-rapture/.../NX/State.js). ExtJS cannot import from this module, so both
// surfaces must be updated together when a new edition identifier is added.
const EDITION_DISPLAY_NAMES: Record<string, string> = {
  [EDITION_CORE]: 'Core',
  [EDITION_COMMUNITY]: 'Community Edition',
  [EDITION_PRO]: 'Pro',
  [EDITION_PRO_PLUS]: 'Pro+',
  [EDITION_ENTERPRISE]: 'Enterprise',
  [EDITION_CLOUD]: 'Pro',
};

/** Default display name used before the backend has published an edition identifier. */
export const DEFAULT_EDITION_DISPLAY_NAME = 'Core';

/**
 * Maps an edition identifier (from Rapture state key {@code editionId}) to its
 * human-readable display name. Returns the {@link DEFAULT_EDITION_DISPLAY_NAME}
 * during the bootstrap window before state resolves so aria-labels and alt text
 * remain well-formed, and falls back to the raw identifier for unknown future
 * editions rather than silently hiding.
 */
export function getEditionDisplayName(editionId: string | undefined | null): string {
  if (!editionId) return DEFAULT_EDITION_DISPLAY_NAME;
  return EDITION_DISPLAY_NAMES[editionId] ?? editionId;
}

// Logo assets — consolidated here so both self-hosted and cloud reference one copy
// Pro+ and Enterprise use the Pro logo as a stopgap until design provides new assets
import headerWordmarkLogo from '../art/logos/logo-header-wordmark.svg';
import headerWordmarkDarkLogo from '../art/logos/logo-header-wordmark-dark.svg';
import coreLogo from '../art/logos/logo-core-edition-header.svg';
import coreDarkLogo from '../art/logos/logo-core-edition-header-dark.svg';
import ceLogo from '../art/logos/logo-community-edition-header.svg';
import ceDarkLogo from '../art/logos/logo-community-edition-header-dark.svg';
import proLogo from '../art/logos/logo-pro-edition-header.svg';
import proDarkLogo from '../art/logos/logo-pro-edition-header-dark.svg';
import proPlusLogo from '../art/logos/logo-pro-plus-edition-header.svg';
import proPlusDarkLogo from '../art/logos/logo-pro-plus-edition-header-dark.svg';
import enterpriseLogo from '../art/logos/logo-enterprise-edition-header.svg';
import enterpriseDarkLogo from '../art/logos/logo-enterprise-edition-header-dark.svg';

/**
 * Returns the common "sonatype nexus repository" wordmark for use in the global
 * header. The wordmark contains no edition text — the edition is shown separately
 * as a badge next to the logo.
 */
export function getHeaderWordmarkLogo(isDark: boolean): string {
  return isDark ? headerWordmarkDarkLogo : headerWordmarkLogo;
}

/**
 * Returns the full edition-branded logo asset URL for a given edition identifier
 * and theme, for surfaces that embed the edition name directly in the graphic.
 * The global header and login surfaces instead use {@link getHeaderWordmarkLogo}
 * combined with an edition badge.
 */
export function getEditionLogo(editionId: string | undefined | null, isDark: boolean): string {
  switch (editionId) {
    case EDITION_COMMUNITY:
      return isDark ? ceDarkLogo : ceLogo;
    case EDITION_PRO:
    case EDITION_CLOUD:
      return isDark ? proDarkLogo : proLogo;
    case EDITION_PRO_PLUS:
      return isDark ? proPlusDarkLogo : proPlusLogo;
    case EDITION_ENTERPRISE:
      return isDark ? enterpriseDarkLogo : enterpriseLogo;
    default:
      return isDark ? coreDarkLogo : coreLogo;
  }
}
