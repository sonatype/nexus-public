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

import { UIRouterReact, servicesPlugin, hashLocationPlugin } from '@uirouter/react';
import { previewBrowseRoutes } from '../previewBrowseRoutes';

/**
 * Deep-link matching for the component-detail routes (NEXUS-54428).
 *
 * On a browser refresh / freshly pasted URL the router boots via
 * `deferIntercept()` and then syncs the already-present hash — the same path
 * `createRouter` uses in production. That path exposed a bug the click-through
 * path (`stateService.go`) did not: the `:gaId` path param matched greedily and
 * swallowed the trailing `/overview` tab segment, so a URL like
 * `.../component/maven:g:a/overview?version=…&repository=…` matched the parent
 * `preview.browse.search.component` with `gaId = "maven:g:a/overview"` instead
 * of the `.overview` child with a clean gaId. Every data hook then queried a
 * nonexistent component and all tabs came up empty. Constraining the matcher to
 * a non-slash segment fixes it. These tests run against the real route
 * definitions so a regression in `previewBrowseRoutes` is caught here.
 */

const COMPONENT_ROUTE_NAMES = previewBrowseRoutes
  .map((r) => r.name)
  .filter((n) => n.startsWith('preview.browse.search.component'));

/** Build a real router with the abstract ancestors stubbed and the real component subtree registered. */
function buildRouter() {
  const router = new UIRouterReact();
  router.plugin(servicesPlugin);
  router.plugin(hashLocationPlugin);

  // Abstract ancestors live in other route files; stub them so the chain resolves.
  router.stateRegistry.register({ name: 'preview', url: '/preview', abstract: true });
  router.stateRegistry.register({ name: 'preview.browse', url: '/browse', abstract: true });
  router.stateRegistry.register({ name: 'preview.browse.search', url: '/search', abstract: true });

  // Register the real component routes, minus the React `component` (not rendered here).
  previewBrowseRoutes
    .filter((r) => COMPONENT_ROUTE_NAMES.includes(r.name))
    .forEach(({ component, ...rest }) => router.stateRegistry.register(rest));

  return router;
}

/** Simulate a fresh page load: hash is already set, router boots deferred, then syncs. */
async function freshLoad(hash) {
  window.location.hash = hash;
  const router = buildRouter();
  router.urlService.deferIntercept();
  router.urlService.listen();
  router.urlService.sync();
  await new Promise((resolve) => setTimeout(resolve, 30));
  return {
    state: router.globals.current?.name,
    params: router.globals.params,
  };
}

const GA = 'maven:com.example:demo';

describe('previewBrowseRoutes component-detail deep-links (NEXUS-54428)', () => {
  it('matches the .overview child (not the parent) when a tab segment follows the gaId', async () => {
    const { state, params } = await freshLoad(
      `#/preview/browse/search/component/${GA}/overview?version=2.0.0&repository=repo-maven-beta`
    );
    expect(state).toBe('preview.browse.search.component.overview');
    // The tab segment must not be swallowed into gaId.
    expect(params.gaId).toBe(GA);
    expect(params.version).toBe('2.0.0');
    expect(params.repository).toBe('repo-maven-beta');
  });

  it('matches each tab child with a clean gaId', async () => {
    const cases = [
      ['versions', 'preview.browse.search.component.versions'],
      ['repos', 'preview.browse.search.component.repos'],
      ['files', 'preview.browse.search.component.files'],
      ['security', 'preview.browse.search.component.security'],
    ];
    for (const [segment, expectedState] of cases) {
      const { state, params } = await freshLoad(
        `#/preview/browse/search/component/${GA}/${segment}?version=2.0.0`
      );
      expect(state).toBe(expectedState);
      expect(params.gaId).toBe(GA);
    }
  });

  it('still matches the parent component route when no tab segment is present', async () => {
    const { state, params } = await freshLoad(
      `#/preview/browse/search/component/${GA}?version=2.0.0&repository=repo-maven-beta`
    );
    expect(state).toBe('preview.browse.search.component');
    expect(params.gaId).toBe(GA);
    expect(params.repository).toBe('repo-maven-beta');
  });

  it('decodes a percent-encoded gaId back to its canonical coordinate', async () => {
    const { state, params } = await freshLoad(
      `#/preview/browse/search/component/${encodeURIComponent(GA)}/overview?version=2.0.0`
    );
    expect(state).toBe('preview.browse.search.component.overview');
    expect(params.gaId).toBe(GA);
  });
});
