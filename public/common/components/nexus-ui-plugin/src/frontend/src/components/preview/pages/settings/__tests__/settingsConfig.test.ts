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

import { SETTINGS_SECTIONS, type SettingCard } from '../settingsConfig';

const cardById = (id: string): SettingCard | undefined =>
  SETTINGS_SECTIONS.flatMap((section) => section.cards).find((card) => card.id === id);

// SettingsHubPage.test.tsx mocks ../settingsConfig, so it exercises the filtering
// mechanism but never asserts the real config's gates. These tests run against the
// REAL config so a fix that removes a card's visibilityRequirements fails here
// instead of silently regressing (NEXUS-54048).
describe('settingsConfig permission gating (NEXUS-54048)', () => {
  it('gates the API card on nexus:settings:read', () => {
    // The API page has no dedicated privilege; the Default UI and Classic UI gate it on
    // nexus:settings:read, so the Settings Hub card must carry the same requirement or an
    // unauthorized user sees a card that then fails with a 403.
    const api = cardById('api');
    expect(api).toBeDefined();
    expect(api?.visibilityRequirements).toBeDefined();
    expect(api?.visibilityRequirements?.requiresPermission).toBe('nexus:settings:read');
  });

  // The Settings Hub cards must carry the SAME gate as their corresponding route, which in turn
  // mirrors the Default UI (adminRoutes.js). A card that is more permissive than its route shows
  // a tile that then dead-ends in a 403. These lock each card's gate to the Default UI value.
  it('gates the repositories card on the nexus:repository-admin prefix', () => {
    const card = cardById('repositories');
    expect(card?.visibilityRequirements?.permissionPrefix).toBe('nexus:repository-admin');
    expect(card?.visibilityRequirements?.requiresPermission).toBeUndefined();
  });

  it('gates the cleanup-policies card on nexus:*', () => {
    const card = cardById('cleanup-policies');
    expect(card?.visibilityRequirements?.requiresPermission).toBe('nexus:*');
  });

  it('gates the routing-rules card on nexus:*', () => {
    const card = cardById('routing-rules');
    expect(card?.visibilityRequirements?.requiresPermission).toBe('nexus:*');
  });

  // NEXUS-54048: datastore, saml, oauth2, and nodes are admin-only screens. Their Default UI
  // routes (adminRoutes.js DATASTORE/SAML/OAUTH2/NODES.ROOT) all gate on nexus:*, so a
  // settings:read-only user must not see the tile and then hit a 403 opening the page.
  it.each(['data-store', 'saml', 'oauth2', 'nodes'])(
    'gates the %s card on nexus:*',
    (cardId) => {
      const card = cardById(cardId);
      expect(card).toBeDefined();
      expect(card?.visibilityRequirements?.requiresPermission).toBe('nexus:*');
    },
  );

  it('requires both roles:read and privileges:read for the roles card', () => {
    const card = cardById('roles');
    expect(card?.visibilityRequirements?.permissions).toEqual([
      'nexus:roles:read',
      'nexus:privileges:read',
    ]);
  });

  it('requires both users:read and roles:read for the users card', () => {
    const card = cardById('users');
    expect(card?.visibilityRequirements?.permissions).toEqual([
      'nexus:users:read',
      'nexus:roles:read',
    ]);
  });

  it('gates the user-tokens card on usertoken-settings:read for PRO only', () => {
    const card = cardById('user-tokens');
    expect(card?.visibilityRequirements?.requiresPermission).toBe('nexus:usertoken-settings:read');
    expect(card?.visibilityRequirements?.editions).toEqual(['PRO']);
  });

  it('gates the usage card on nexus:settings:read (matches cloud Classic)', () => {
    const card = cardById('usage');
    expect(card?.visibilityRequirements?.requiresPermission).toBe('nexus:settings:read');
  });

  // NEXUS-54019: align Settings Hub cards with Classic/Default UI (adminRoutes.js) edition/bundle/
  // license/capability gates and correct three permission-string mismatches. Each card must carry
  // the SAME gate as its route or a tile dead-ends in a 403 / shows where the feature is absent.
  describe('NEXUS-54019 alignment with Default UI gates', () => {
    it('gates data-store on the pro-datastore bundle + PRO/COMMUNITY editions', () => {
      const card = cardById('data-store');
      expect(card?.visibilityRequirements?.bundle).toBe('nexus-pro-datastore-plugin');
      expect(card?.visibilityRequirements?.editions).toEqual(['PRO', 'COMMUNITY']);
    });

    it('gates saml on the saml bundle + PRO edition', () => {
      const card = cardById('saml');
      expect(card?.visibilityRequirements?.bundle).toBe('nexus-saml-plugin');
      expect(card?.visibilityRequirements?.editions).toEqual(['PRO']);
    });

    it('gates oauth2 on the oauth2 bundle + PRO edition + oauth2Available state', () => {
      const card = cardById('oauth2');
      expect(card?.visibilityRequirements?.bundle).toBe('nexus-oauth2-plugin');
      expect(card?.visibilityRequirements?.editions).toEqual(['PRO']);
      expect(card?.visibilityRequirements?.statesEnabled).toContainEqual({
        key: 'oauth2Available',
        defaultValue: false,
      });
    });

    it('gates crowd on the crowd bundle + a valid crowd license', () => {
      const card = cardById('crowd');
      expect(card?.visibilityRequirements?.bundle).toBe('nexus-crowd-plugin');
      expect(card?.visibilityRequirements?.licenseValid).toContainEqual({
        key: 'crowd',
        defaultValue: false,
      });
    });

    it('gates user-tokens on a valid usertoken license', () => {
      const card = cardById('user-tokens');
      expect(card?.visibilityRequirements?.licenseValid).toContainEqual({
        key: 'usertoken',
        defaultValue: false,
      });
    });

    it('gates service-account-tokens on PRO edition + serviceAccountEnabled state', () => {
      const card = cardById('service-account-tokens');
      expect(card?.visibilityRequirements?.editions).toEqual(['PRO']);
      expect(card?.visibilityRequirements?.statesEnabled).toContainEqual({
        key: 'serviceAccountEnabled',
        defaultValue: false,
      });
    });

    it('gates ldap on ldap:read with NO edition restriction (matches Default UI)', () => {
      const card = cardById('ldap');
      expect(card?.visibilityRequirements?.requiresPermission).toBe('nexus:ldap:read');
      expect(card?.visibilityRequirements?.editions).toBeUndefined();
    });

    it('gates support-request on atlas:create + PRO edition', () => {
      const card = cardById('support-request');
      expect(card?.visibilityRequirements?.requiresPermission).toBe('nexus:atlas:create');
      expect(card?.visibilityRequirements?.editions).toEqual(['PRO']);
    });

    it('gates support-zip on atlas:read', () => {
      const card = cardById('support-zip');
      expect(card?.visibilityRequirements?.requiresPermission).toBe('nexus:atlas:read');
    });

    // NEXUS-54237: informational page — no migration capability or migration:read requirement.
    it('gates upgrade on settings:read with no capability requirement', () => {
      const card = cardById('upgrade');
      expect(card?.visibilityRequirements?.requiresPermission).toBe('nexus:settings:read');
      expect(card?.visibilityRequirements?.capability).toBeUndefined();
    });
  });

  it('gates the ip-allowlist card on nexus:* + PRO (matches backend)', () => {
    // IpAllowListResource requires @RequiresPermissions("nexus:*") on every endpoint — IP Allow
    // List is admin-only by design (NEXUS-45598). A settings:read gate showed the tile to
    // non-admins who then hit a 403 opening the page.
    const card = cardById('ip-allowlist');
    expect(card?.visibilityRequirements?.requiresPermission).toBe('nexus:*');
    expect(card?.proOnly).toBe(true);
    // adminOnly is the fail-closed early guard: SettingsHubPage filters it before isVisible(),
    // whose NXSESSIONID cookie fast-path would otherwise flash the tile to non-admins on cold load.
    expect(card?.adminOnly).toBe(true);
  });
});
