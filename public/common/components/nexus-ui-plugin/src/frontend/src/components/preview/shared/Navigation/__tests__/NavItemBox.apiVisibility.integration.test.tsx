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
import React from 'react';
import { render, screen } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import {
  UIRouterReact,
  UIRouter,
  servicesPlugin,
  memoryLocationPlugin,
} from '@uirouter/react';

import { NavItemBox } from '../NavItemBox';
import { PreviewUIContext } from '../PreviewUIContext';

// NEXUS-54212: End-to-end guard that the "API" left-nav item is visible for a user
// holding only nexus:settings:read. This exercises the REAL chain:
//   NavItemBox -> useRouteVisibility -> useContextAwareRouteName -> router.stateRegistry
//   -> useIsVisible -> isVisible(route.data.visibilityRequirements)
// against a real uirouter registry seeded with the real preview.browse.api route data,
// rather than the fully-mocked unit tests. The Settings hub tile and this nav item both
// gate on nexus:settings:read via the same isVisible(), so this proves parity between them.

/** Mirrors the real preview.browse.api route data (previewBrowseRoutes.js). */
const API_VISIBILITY_REQUIREMENTS = {
  requiresUser: true,
  permissions: ['nexus:settings:read'],
};

function buildRouterWithApiRoute() {
  const router = new UIRouterReact();
  router.plugin(servicesPlugin);
  router.plugin(memoryLocationPlugin);

  router.stateRegistry.register({ name: 'preview', abstract: true });
  router.stateRegistry.register({ name: 'preview.browse', abstract: true });
  router.stateRegistry.register({
    name: 'preview.browse.api',
    url: '/api',
    data: {
      title: 'API',
      visibilityRequirements: API_VISIBILITY_REQUIREMENTS,
    },
  });

  // Note: the <UIRouter> component calls router.start() itself. Visibility resolution
  // only reads stateRegistry.get(), so no active transition/initial rule is needed.
  return router;
}

/** Grants exactly the supplied permissions (Shiro-agnostic: exact-match for the test). */
function grantPermissions(granted: string[]) {
  const set = new Set(granted);
  (global as any).NX.Permissions.check.mockImplementation((perm: string) => set.has(perm));
  (global as any).NX.Permissions.permissions = Object.fromEntries(granted.map((p) => [p, true]));
}

const renderApiNavItem = (router: UIRouterReact) =>
  render(
    <Theme>
      <UIRouter router={router}>
        <PreviewUIContext.Provider value={true}>
          <NavItemBox name="preview.browse.api" text="API" icon={null} isCollapsed={false} />
        </PreviewUIContext.Provider>
      </UIRouter>
    </Theme>
  );

describe('NavItemBox — preview.browse.api visibility (integration)', () => {
  let router: UIRouterReact;

  beforeEach(() => {
    router = buildRouterWithApiRoute();
    // Logged-in user (Security.hasUser() -> true satisfies requiresUser).
    (global as any).NX.Security.hasUser.mockReturnValue(true);
  });

  it('renders the API item for a logged-in user with only nexus:settings:read', () => {
    grantPermissions(['nexus:settings:read']);

    renderApiNavItem(router);

    expect(screen.getByText('API')).toBeInTheDocument();
  });

  it('hides the API item for a logged-in user without nexus:settings:read', () => {
    grantPermissions(['nexus:component:read']);

    renderApiNavItem(router);

    expect(screen.queryByText('API')).not.toBeInTheDocument();
  });
});
