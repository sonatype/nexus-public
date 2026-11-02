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
 * Short-name edition code to display name for the System Information page's
 * nexus-status.edition field. The backend (SystemInformationGeneratorImpl)
 * emits the raw NexusEditionType short name (PRO, PROPLUS, ...); this maps it
 * to the name shown elsewhere in the product.
 *
 * This is intentionally separate from editionUtils.EDITION_DISPLAY_NAMES: that
 * table keys on editionId (nexus-professional-plus-edition) and renders
 * COMMUNITY as "Community Edition", whereas this page keys on the short name
 * and shows "Community". Add future short-name mappings here.
 */
export const EDITION_DISPLAY_NAMES_BY_SHORT_NAME = {
  COMMUNITY: 'Community',
  PRO: 'Pro',
  PROPLUS: 'Pro+',
  ENTERPRISE: 'Enterprise',
};

/**
 * Translates a raw edition short-name code to its display name, falling back to
 * the raw value for unmapped codes (e.g. CORE, future tiers) so nothing is hidden.
 */
export function getEditionDisplayNameByShortName(edition) {
  return EDITION_DISPLAY_NAMES_BY_SHORT_NAME[edition] ?? edition;
}

/**
 * Returns a copy of the nexus-status section with its edition field translated
 * to a display name. Any non-edition field, and a section without an edition
 * field, is returned unchanged so no other field on the page is affected.
 */
export function translateNexusStatusEdition(nexusStatus) {
  if (!nexusStatus || !('edition' in nexusStatus)) {
    return nexusStatus;
  }
  return {
    ...nexusStatus,
    edition: getEditionDisplayNameByShortName(nexusStatus.edition),
  };
}
