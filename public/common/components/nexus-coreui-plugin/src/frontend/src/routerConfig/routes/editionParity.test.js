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
 * AT-015: Route availability parity across licensed editions.
 *
 * NEXUS-54783 — Any route (or settings-hub card) visible on Pro must remain visible on
 * Pro Plus and Enterprise. This test imports the real route configuration modules and
 * calls the real {@link meetsEditionRequirement} predicate so a new route declared with
 * a Pro-only editions gate is caught here rather than at customer report time.
 *
 * On failure, the assertion reports the specific route names that are missing from the
 * target edition so the fix is a one-line addition rather than a bisect.
 */
import {
  meetsEditionRequirement,
  SETTINGS_SECTIONS,
  EDITION_COMMUNITY,
  EDITION_PRO,
  EDITION_PRO_PLUS,
  EDITION_ENTERPRISE,
  EDITION_CLOUD,
  EDITION_ID_STATE_KEY,
} from '@sonatype/nexus-ui-plugin';
import { adminRoutes } from './adminRoutes';
import { browseRoutes } from './browseRoutes';
import { previewAdminRoutes } from './previewAdminRoutes';
import { previewBrowseRoutes } from './previewBrowseRoutes';
import { userRoutes } from './userRoutes';

// Explicit set of routes/cards declared on Community. Kept as a literal so the migration
// from short names to identifiers is proven not to have altered Community's route set —
// Community is not asserted as a superset of Pro (Community lacks Pro-only pages by design).
const EXPECTED_COMMUNITY_VISIBLE = [
  'admin.repository.datastore',
  'preview.admin.repository.datastore',
  'settings-card:data-store',
];

function collectEditionGatedItems() {
  const items = [];

  const walkRoute = (route) => {
    const editions = route?.data?.visibilityRequirements?.editions;
    if (editions) {
      items.push({
        label: route.name ?? route.url ?? '(unnamed route)',
        editions,
      });
    }
    if (Array.isArray(route?.children)) {
      route.children.forEach(walkRoute);
    }
  };

  [
    ...adminRoutes,
    ...browseRoutes,
    ...previewAdminRoutes,
    ...previewBrowseRoutes,
    ...userRoutes,
  ].forEach(walkRoute);

  SETTINGS_SECTIONS.forEach((section) => {
    section.cards.forEach((card) => {
      const editions = card?.visibilityRequirements?.editions;
      if (editions) {
        items.push({ label: `settings-card:${card.id}`, editions });
      }
    });
  });

  return items;
}

function itemsVisibleFor(editionId, items) {
  NX.State.getValue.mockImplementation((key, defaultValue) => {
    if (key === EDITION_ID_STATE_KEY) {
      return editionId;
    }
    return defaultValue;
  });
  const visible = new Set();
  for (const item of items) {
    if (meetsEditionRequirement(item.editions)) {
      visible.add(item.label);
    }
  }
  return visible;
}

describe('AT-015: Route availability parity across licensed editions', () => {
  let items;

  beforeAll(() => {
    items = collectEditionGatedItems();
  });

  it('collects at least one edition-gated item so the parity check is not vacuously true', () => {
    expect(items.length).toBeGreaterThan(0);
  });

  it.each([
    [EDITION_PRO_PLUS, 'Pro Plus'],
    [EDITION_ENTERPRISE, 'Enterprise'],
  ])('every route visible on Pro must remain visible on %s', (targetEdition) => {
    const proVisible = itemsVisibleFor(EDITION_PRO, items);
    const targetVisible = itemsVisibleFor(targetEdition, items);
    const missing = [...proVisible].filter((label) => !targetVisible.has(label)).sort();
    // Naming the missing routes turns a red test into a one-line fix.
    expect(missing).toEqual([]);
  });

  it('Community visibility is unchanged from before the identifier migration', () => {
    const communityVisible = itemsVisibleFor(EDITION_COMMUNITY, items);
    expect([...communityVisible].sort()).toEqual([...EXPECTED_COMMUNITY_VISIBLE].sort());
  });

  it('preserves Cloud visibility for all pre-migration Pro-gated routes and cards', () => {
    // Cloud reports editionId 'nexus-cloud-edition' but formerly matched 'PRO' by short name,
    // so every declaration that carried 'PRO' before must include EDITION_CLOUD after migration.
    const cloudVisible = itemsVisibleFor(EDITION_CLOUD, items);
    const proVisible = itemsVisibleFor(EDITION_PRO, items);
    const missing = [...proVisible].filter((label) => !cloudVisible.has(label)).sort();
    expect(missing).toEqual([]);
  });
});
