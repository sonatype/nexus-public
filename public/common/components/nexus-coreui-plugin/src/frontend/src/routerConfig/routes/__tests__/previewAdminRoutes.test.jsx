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

import { UIView } from '@uirouter/react';
import { Permissions, PREVIEW_FEATURE_FLAGS, SETTINGS_SECTIONS } from '@sonatype/nexus-ui-plugin';
import { previewAdminRoutes } from '../previewAdminRoutes';

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
    const routesRequiringSettingsRead = [
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

    routesRequiringSettingsRead.forEach((routeName) => {
      const route = previewAdminRoutes.find((r) => r.name === routeName);
      expect(route).toBeDefined();
      expect(route.data.visibilityRequirements).toBeDefined();
    });
  });

  it('uses SETTINGS.READ for cleanup-policies and routing-rules (matches backend API)', () => {
    const cleanupRoute = previewAdminRoutes.find(
      (r) => r.name === 'preview.admin.repository.cleanuppolicies',
    );
    const routingRoute = previewAdminRoutes.find(
      (r) => r.name === 'preview.admin.repository.routingrules',
    );

    expect(cleanupRoute.data.visibilityRequirements.requiresPermission).toBe(
      Permissions.SETTINGS.READ,
    );
    expect(routingRoute.data.visibilityRequirements.requiresPermission).toBe(
      Permissions.SETTINGS.READ,
    );
  });

  it('uses USER_TOKENS_SETTINGS.READ for usertoken route', () => {
    const route = previewAdminRoutes.find((r) => r.name === 'preview.admin.security.usertoken');

    expect(route).toBeDefined();
    expect(route.data.visibilityRequirements.requiresPermission).toBe(
      Permissions.USER_TOKENS_SETTINGS.READ,
    );
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
});
