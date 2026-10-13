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
 * Canonical UI-Router state names for the Preview / Nexus One Browse experience.
 *
 * Single source of truth (NEXUS-54430): `BrowsePage` registers and consumes these, and the
 * deep-link builders elsewhere — e.g. {@link buildBrowseTarget} on the component-detail Files tab
 * and header — target them. Both sides must agree, so neither may re-declare the literals; a
 * rename here is the one place it happens. The router resolves these edition-agnostic state names
 * to each edition's own URL (`#browse/...` on Self-Hosted/CE, `#preview/browse/...` on Cloud).
 */
export const BROWSE_ROUTE_STATES = {
  /** Browse landing (repository list). */
  base: 'preview.browse.browse',
  /** A repository root within the Browse tree. */
  repo: 'preview.browse.browse.repo',
  /** A specific node (folder/asset) within the Browse tree. */
  path: 'preview.browse.browse.repo.path',
} as const;
