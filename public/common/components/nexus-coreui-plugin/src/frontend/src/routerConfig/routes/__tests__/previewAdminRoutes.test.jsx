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

import { UIView, UIRouterReact, servicesPlugin } from '@uirouter/react';
import { Permissions, PREVIEW_FEATURE_FLAGS, SETTINGS_SECTIONS } from '@sonatype/nexus-ui-plugin';
import { previewAdminRoutes } from '../previewAdminRoutes';
import { previewBrowseRoutes } from '../previewBrowseRoutes';

describe('previewAdminRoutes', () => {
  it('defines preview.admin as the abstract admin route container', () => {
    const previewAdminRoute = previewAdminRoutes.find((route) => route.name === 'preview.admin');

    expect(previewAdminRoute).toBeDefined();
    expect(previewAdminRoute.abstract).toBe(true);
    expect(previewAdminRoute.component).toBe(UIView);
  });

  it('keeps preview.admin.pages as the shared settings layout wrapper', () => {
    const pagesRoute = previewAdminRoutes.find((route) => route.name === 'preview.admin.pages');

    expect(pagesRoute).toBeDefined();
    expect(pagesRoute.component).toBeDefined();
    expect(pagesRoute.component).not.toBe(UIView);
  });

  it('defines the Nexus One UI settings route with correct permissions', () => {
    const route = previewAdminRoutes.find((r) => r.name === 'preview.admin.system.previewui');

    expect(route).toBeDefined();
    expect(route.url).toBe('/previewui');
    expect(route.component).toBeDefined();
    expect(route.data.visibilityRequirements.permissions).toContain(Permissions.SETTINGS.READ);
  });

  it('defines the settings hub route', () => {
    const route = previewAdminRoutes.find((r) => r.name === 'preview.settings');

    expect(route).toBeDefined();
    expect(route.url).toBe('/settings');
    expect(route.component).toBeDefined();
  });

  it('ensures all admin routes that need permission guards have visibilityRequirements', () => {
    const routesRequiringPermissionGuards = [
      'preview.admin.repository.cleanuppolicies',
      'preview.admin.repository.routingrules',
      'preview.admin.repository.datastore',
      'preview.admin.repository.proprietary',
      'preview.admin.security.anonymous',
      'preview.admin.security.realms',
      'preview.admin.security.saml',
      'preview.admin.security.oauth2',
      'preview.admin.security.crowd',
      'preview.admin.system.emailserver',
      'preview.admin.system.http',
      'preview.admin.system.nodes',
      'preview.admin.system.upgrade',
    ];

    routesRequiringPermissionGuards.forEach((routeName) => {
      const route = previewAdminRoutes.find((r) => r.name === routeName);
      expect(route).toBeDefined();
      expect(route.data.visibilityRequirements).toBeDefined();
    });
  });

  // NEXUS-54048: datastore, saml, oauth2, and nodes are admin-only screens. Their Default UI
  // routes (adminRoutes.js DATASTORE/SAML/OAUTH2/NODES.ROOT) all gate on nexus:*, so a
  // settings:read-only user must not see the tile and then hit a 403 opening the page.
  it('gates datastore, saml, oauth2, and nodes on nexus:* (matches Default UI)', () => {
    const adminOnlyRouteNames = [
      'preview.admin.repository.datastore',
      'preview.admin.security.saml',
      'preview.admin.security.oauth2',
      'preview.admin.system.nodes',
    ];

    adminOnlyRouteNames.forEach((routeName) => {
      const route = previewAdminRoutes.find((r) => r.name === routeName);
      expect(route).toBeDefined();
      expect(route.data.visibilityRequirements.requiresPermission).toBe(Permissions.ADMIN);
    });
  });

  // NEXUS-54048: cleanup-policies and routing-rules gate on nexus:* to match the Default UI
  // (adminRoutes.js CLEANUPPOLICIES.ROOT / ROUTINGRULES.ROOT). The RoutingRules/CleanupPolicy
  // write endpoints require admin, and RoutingRulesResource requires nexus:* even on the GET/list,
  // so a settings:read-only user would see the menu and then hit a 403.
  it('gates cleanup-policies and routing-rules on nexus:* (matches Default UI)', () => {
    const cleanupRoute = previewAdminRoutes.find(
      (r) => r.name === 'preview.admin.repository.cleanuppolicies',
    );
    const routingRoute = previewAdminRoutes.find(
      (r) => r.name === 'preview.admin.repository.routingrules',
    );

    expect(cleanupRoute.data.visibilityRequirements.requiresPermission).toBe(Permissions.ADMIN);
    expect(routingRoute.data.visibilityRequirements.requiresPermission).toBe(Permissions.ADMIN);
  });

  // NEXUS-54048: repositories uses a prefix match (any repository-admin grant) to match the
  // Default UI (adminRoutes.js REPOSITORIES.ROOT), not a specific wildcard-level read that a
  // narrower per-repo grant would fail to satisfy.
  it('gates repositories on the nexus:repository-admin prefix (matches Default UI)', () => {
    const route = previewAdminRoutes.find(
      (r) => r.name === 'preview.admin.repository.repositories',
    );

    expect(route).toBeDefined();
    expect(route.data.visibilityRequirements.requiresPermission).toBeUndefined();
    expect(route.data.visibilityRequirements.permissionPrefix).toBe('nexus:repository-admin');
  });

  // NEXUS-54048: roles and users require BOTH reads (permissions[] is AND) to match the
  // Default UI (adminRoutes.js ROLES.ROOT / USERS.ROOT).
  it('requires both roles:read and privileges:read for roles (matches Default UI)', () => {
    const route = previewAdminRoutes.find((r) => r.name === 'preview.admin.security.roles');

    expect(route).toBeDefined();
    expect(route.data.visibilityRequirements.permissions).toEqual([
      Permissions.ROLES.READ,
      Permissions.PRIVILEGES.READ,
    ]);
  });

  it('requires both users:read and roles:read for users (matches Default UI)', () => {
    const route = previewAdminRoutes.find((r) => r.name === 'preview.admin.security.users');

    expect(route).toBeDefined();
    expect(route.data.visibilityRequirements.permissions).toEqual([
      Permissions.USERS.READ,
      Permissions.ROLES.READ,
    ]);
  });

  // NEXUS-54048: usertoken is gated to PRO edition to match the Default UI (adminRoutes.js
  // USERTOKEN.ROOT).
  it('uses USER_TOKENS_SETTINGS.READ gated to PRO for usertoken route', () => {
    const route = previewAdminRoutes.find((r) => r.name === 'preview.admin.security.usertoken');

    expect(route).toBeDefined();
    expect(route.data.visibilityRequirements.requiresPermission).toBe(
      Permissions.USER_TOKENS_SETTINGS.READ,
    );
    expect(route.data.visibilityRequirements.editions).toEqual(['PRO']);
  });

  it('uses SERVICE_ACCOUNTS.READ and statesEnabled for serviceaccounttokens route', () => {
    const route = previewAdminRoutes.find((r) => r.name === 'preview.admin.security.serviceaccounttokens');

    expect(route).toBeDefined();
    expect(route.url).toBe('/service-account-tokens');
    expect(route.data.visibilityRequirements.requiresPermission).toBe(
      Permissions.SERVICE_ACCOUNTS.READ,
    );
    expect(route.data.visibilityRequirements.statesEnabled).toBeDefined();
    expect(route.data.visibilityRequirements.statesEnabled).toContainEqual({
      key: 'serviceAccountEnabled',
      defaultValue: false,
    });
  });

  it('uses LICENSING.READ for licensing route', () => {
    const route = previewAdminRoutes.find((r) => r.name === 'preview.admin.system.licensing');

    expect(route).toBeDefined();
    expect(route.data.visibilityRequirements.requiresPermission).toBe(Permissions.LICENSING.READ);
  });

  it('uses nexus:crowd:read permission for crowd route', () => {
    const route = previewAdminRoutes.find((r) => r.name === 'preview.admin.security.crowd');

    expect(route).toBeDefined();
    expect(route.data.visibilityRequirements.permissions).toContain('nexus:crowd:read');
  });

  it('gates ip-allowlist on nexus:* + PRO (matches backend @RequiresPermissions)', () => {
    // IpAllowListResource requires nexus:* on every endpoint (admin-only by design, NEXUS-45598).
    // A settings:read gate let non-admins open the page and then hit a 403.
    const route = previewAdminRoutes.find((r) => r.name === 'preview.admin.security.ip-allowlist');

    expect(route).toBeDefined();
    expect(route.data.visibilityRequirements.requiresPermission).toBe(Permissions.ADMIN);
    expect(route.data.visibilityRequirements.editions).toEqual(['PRO']);
  });

  // NEXUS-54019: align Preview routes with Classic/Default UI (adminRoutes.js) edition/bundle/
  // license/capability gates and correct three permission-string mismatches. Each assertion mirrors
  // the exact gate declared on the corresponding Classic route.
  describe('NEXUS-54019 alignment with Default UI gates', () => {
    it('gates datastore on the pro-datastore bundle + PRO/COMMUNITY editions', () => {
      const route = previewAdminRoutes.find((r) => r.name === 'preview.admin.repository.datastore');
      expect(route.data.visibilityRequirements.bundle).toBe('nexus-pro-datastore-plugin');
      expect(route.data.visibilityRequirements.editions).toEqual(['PRO', 'COMMUNITY']);
    });

    it('gates saml on the saml bundle + PRO edition', () => {
      const route = previewAdminRoutes.find((r) => r.name === 'preview.admin.security.saml');
      expect(route.data.visibilityRequirements.bundle).toBe('nexus-saml-plugin');
      expect(route.data.visibilityRequirements.editions).toEqual(['PRO']);
    });

    it('gates oauth2 on the oauth2 bundle + PRO edition + oauth2Available state', () => {
      const route = previewAdminRoutes.find((r) => r.name === 'preview.admin.security.oauth2');
      expect(route.data.visibilityRequirements.bundle).toBe('nexus-oauth2-plugin');
      expect(route.data.visibilityRequirements.editions).toEqual(['PRO']);
      expect(route.data.visibilityRequirements.statesEnabled).toContainEqual({
        key: 'oauth2Available',
        defaultValue: false,
      });
    });

    it('gates crowd on the crowd bundle + a valid crowd license', () => {
      const route = previewAdminRoutes.find((r) => r.name === 'preview.admin.security.crowd');
      expect(route.data.visibilityRequirements.bundle).toBe('nexus-crowd-plugin');
      expect(route.data.visibilityRequirements.licenseValid).toContainEqual({
        key: 'crowd',
        defaultValue: false,
      });
    });

    it('gates usertoken on a valid usertoken license', () => {
      const route = previewAdminRoutes.find((r) => r.name === 'preview.admin.security.usertoken');
      expect(route.data.visibilityRequirements.licenseValid).toContainEqual({
        key: 'usertoken',
        defaultValue: false,
      });
    });

    it('gates ldap on ldap:read with NO edition restriction (matches Default UI)', () => {
      const route = previewAdminRoutes.find((r) => r.name === 'preview.admin.security.ldap');
      expect(route.data.visibilityRequirements.requiresPermission).toBe(Permissions.LDAP.READ);
      expect(route.data.visibilityRequirements.editions).toBeUndefined();
    });

    it('gates supportrequest on atlas:create + PRO edition', () => {
      const route = previewAdminRoutes.find((r) => r.name === 'preview.admin.support.supportrequest');
      expect(route.data.visibilityRequirements.requiresPermission).toBe(Permissions.ATLAS.CREATE);
      expect(route.data.visibilityRequirements.editions).toEqual(['PRO']);
    });

    it('gates supportzip on atlas:read', () => {
      const route = previewAdminRoutes.find((r) => r.name === 'preview.admin.support.supportzip');
      expect(route.data.visibilityRequirements.requiresPermission).toBe(Permissions.ATLAS.READ);
    });

    // NEXUS-54237: informational page — no migration capability or migration:read requirement.
    it('gates upgrade on settings:read with no capability requirement', () => {
      const route = previewAdminRoutes.find((r) => r.name === 'preview.admin.system.upgrade');
      expect(route.data.visibilityRequirements.requiresPermission).toBe(Permissions.SETTINGS.READ);
      expect(route.data.visibilityRequirements.capability).toBeUndefined();
    });
  });

  it('gates all route components whose SETTINGS_SECTIONS featureKey resolves to false', () => {
    const allCards = SETTINGS_SECTIONS.flatMap((section) => section.cards);
    const ungatedCards = allCards.filter(
      (card) => card.featureKey && PREVIEW_FEATURE_FLAGS[card.featureKey] === false,
    );

    expect(ungatedCards.length).toBeGreaterThan(0);

    ungatedCards.forEach((card) => {
      // Build route name prefix from the card path — strip hyphens because
      // internal route names use concatenated segments (e.g. 'cleanuppolicies')
      // while URL slugs use hyphens (e.g. '/cleanup-policies').
      // Also try singular form for paths ending in 's' (e.g. user-tokens → usertoken).
      const normalized = card.path.replace(/\//g, '.').replace(/-/g, '');
      const routeNamePrefix = `preview.admin.${normalized}`;
      let leafRoutes = previewAdminRoutes.filter(
        (route) => route.name.startsWith(routeNamePrefix) && route.component && !route.abstract,
      );

      if (leafRoutes.length === 0 && normalized.endsWith('s')) {
        const singular = `preview.admin.${normalized.slice(0, -1)}`;
        leafRoutes = previewAdminRoutes.filter(
          (route) => route.name.startsWith(singular) && route.component && !route.abstract,
        );
      }

      expect(leafRoutes.length).toBeGreaterThan(0);
      leafRoutes.forEach((route) => {
        expect(route.component.displayName).toMatch(/^FeatureGate\(/);
      });
    });
  });

  /**
   * NEXUS-52167. These assertions go through the real UrlMatcher rather than
   * comparing the declared `url` string, because two earlier fixes encoded the tab
   * as a path segment and that looks fine in jsdom, where no router is mounted.
   * Only the matcher shows that `/:privilegeId/profile` is terminal and an extra
   * segment matches no route at all.
   */
  describe('privilege profile tab is a matchable query param (NEXUS-52167)', () => {
    const PROFILE_STATE = 'preview.admin.security.privileges.profile';
    // No leading slash: the root `preview` state declares `url: 'preview'` without
    // one, so the joined path the matcher sees does not start with `/`.
    const PROFILE_URL = 'preview/admin/security/privileges/test-priv/profile';

    const profileRoute = () => previewAdminRoutes.find((r) => r.name === PROFILE_STATE);

    /**
     * previewBrowseRoutes must be registered too: it defines the abstract `preview`
     * root every `preview.admin.*` name hangs off. Without it UI-Router silently
     * queues the admin routes as orphans and every URL is a non-match.
     */
    const matchUrl = (path, search = {}) => {
      const router = new UIRouterReact();
      router.plugin(servicesPlugin);
      [...previewBrowseRoutes, ...previewAdminRoutes].forEach((route) =>
        router.stateRegistry.register(route),
      );

      return router.urlService.match({ path, search, hash: '' });
    };

    // Without this guard, a registration failure would make every match-based
    // assertion below pass vacuously (undefined is not the profile state).
    it('registers the profile state, so the assertions below are not vacuous', () => {
      const router = new UIRouterReact();
      router.plugin(servicesPlugin);
      [...previewBrowseRoutes, ...previewAdminRoutes].forEach((route) =>
        router.stateRegistry.register(route),
      );

      expect(router.stateRegistry.get(PROFILE_STATE)).toBeTruthy();
    });

    it('matches /{id}/profile?tab=users and extracts the tab', () => {
      const match = matchUrl(PROFILE_URL, { tab: 'users' });

      expect(match).not.toBeNull();
      expect(match.rule.state.name).toBe(PROFILE_STATE);
      expect(match.match.privilegeId).toBe('test-priv');
      expect(match.match.tab).toBe('users');
    });

    it('still matches /{id}/profile with no tab, so existing links keep working', () => {
      const match = matchUrl(PROFILE_URL);

      expect(match).not.toBeNull();
      expect(match.rule.state.name).toBe(PROFILE_STATE);
      expect(match.match.privilegeId).toBe('test-priv');
    });

    // Regression guard: this is what the two failed fixes produced, and why Back
    // landed on Overview.
    it('does NOT resolve the tab-as-path-segment URL to the profile state', () => {
      const match = matchUrl(`${PROFILE_URL}/users`);

      expect(match?.rule?.state?.name).not.toBe(PROFILE_STATE);
    });

    it('declares tab as dynamic so switching tabs does not re-enter the state', () => {
      // Without `dynamic: true` a tab change is a state transition: the page
      // remounts and refetches the profile on every single tab click.
      expect(profileRoute().params.tab.dynamic).toBe(true);
    });

    it('matches the repository profile route it was modelled on', () => {
      const repoRoute = previewAdminRoutes.find(
        (r) => r.name === 'preview.admin.repository.repositories.profile',
      );

      expect(repoRoute.url).toContain('?tab');
      expect(repoRoute.params.tab.dynamic).toBe(true);
    });
  });
});
