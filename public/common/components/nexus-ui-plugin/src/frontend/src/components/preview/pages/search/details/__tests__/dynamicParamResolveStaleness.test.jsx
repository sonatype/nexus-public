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

/*
 * Pins one behaviour of UI-Router itself, using the same shape as
 * previewBrowseRoutes' component-detail states: a parent declaring `version` as a `dynamic` param,
 * and a child state that reads it through a `resolve`.
 *
 * Does changing a dynamic param re-run a child state's resolve?
 *
 * If it does not, the resolved value reaches the component once and then freezes, and any effect
 * that treats it as "the current URL version" will push a stale version back — which is what
 * NEXUS-54201's T9 trace shows (three /search/repositories calls per click, the middle one for the
 * version just left).
 */

import React from 'react';
import { render, waitFor } from '@testing-library/react';
import {
  UIRouter,
  UIView,
  UIRouterReact,
  servicesPlugin,
  memoryLocationPlugin,
  useCurrentStateAndParams,
} from '@uirouter/react';

/** Every value each source produced, in order, across all renders. */
const seen = { resolve: [], params: [] };

function Probe({ version }) {
  // The reactive alternative: params read straight off the router.
  const { params } = useCurrentStateAndParams();

  seen.resolve.push(version);
  seen.params.push(params?.version);

  return (
    <div>
      <span data-testid="from-resolve">{String(version)}</span>
      <span data-testid="from-params">{String(params?.version)}</span>
    </div>
  );
}

function buildRouter() {
  const router = new UIRouterReact();
  router.plugin(servicesPlugin);
  router.plugin(memoryLocationPlugin);

  // Mirrors previewBrowseRoutes.js: gaId non-dynamic, version dynamic + squashed.
  router.stateRegistry.register({
    name: 'detail',
    url: '/component/:gaId?version',
    component: UIView,
    params: {
      gaId: { type: 'path' },
      version: { type: 'string', value: null, squash: true, dynamic: true },
    },
  });

  // Mirrors preview.browse.search.component.versions: reads version through a resolve.
  router.stateRegistry.register({
    name: 'detail.versions',
    url: '/versions',
    component: Probe,
    resolve: [
      {
        token: 'version',
        deps: ['$stateParams'],
        resolveFn: ($stateParams) => $stateParams.version,
      },
    ],
  });

  router.urlService.rules.initial({ state: 'detail.versions' });
  // Deliberately no router.start() — <UIRouter> starts it as the last step of its own init, and
  // calling it here too throws "start() has been called more than once".
  return router;
}

describe('UI-Router: dynamic param vs child resolve', () => {
  beforeEach(() => {
    seen.resolve = [];
    seen.params = [];
  });

  it('does NOT re-run a child resolve when a dynamic param changes, while useCurrentStateAndParams does update', async () => {
    const router = buildRouter();

    const { getByTestId } = render(
      <UIRouter router={router}>
        <UIView />
      </UIRouter>,
    );

    await router.stateService.go('detail.versions', { gaId: 'npm:dummy:lib', version: '3.0.3' });
    await waitFor(() => expect(getByTestId('from-resolve').textContent).toBe('3.0.3'));

    // The forward case: user clicks a different version.
    await router.stateService.go('detail.versions', { version: '3.0.18' });

    await waitFor(() => {
      // Reactive source tracks the change...
      expect(seen.params[seen.params.length - 1]).toBe('3.0.18');
    });

    // ...and this is the finding: the resolved prop is still the entry-time value.

    expect(getByTestId('from-params').textContent).toBe('3.0.18');
    expect(getByTestId('from-resolve').textContent).toBe('3.0.3'); // stale
  });

  it('also leaves the resolve stale on a Back-style param change (the T16 direction)', async () => {
    const router = buildRouter();

    render(
      <UIRouter router={router}>
        <UIView />
      </UIRouter>,
    );

    await router.stateService.go('detail.versions', { gaId: 'npm:dummy:lib', version: '1.0.0' });
    await waitFor(() => expect(seen.resolve[seen.resolve.length - 1]).toBe('1.0.0'));

    await router.stateService.go('detail.versions', { version: '2.0.0' });
    await waitFor(() => expect(seen.params[seen.params.length - 1]).toBe('2.0.0'));

    // Simulate Back: the URL returns to the earlier version.
    await router.stateService.go('detail.versions', { version: '1.0.0' });
    await waitFor(() => expect(seen.params[seen.params.length - 1]).toBe('1.0.0'));


    // Every resolve value across the whole run — if this is a single repeated value, the resolve
    // never refreshed in either direction.
    const distinctResolveValues = [...new Set(seen.resolve)];
    expect(distinctResolveValues).toEqual(['1.0.0']);
  });
});
