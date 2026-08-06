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

import { interpret } from 'xstate';
import { waitFor } from 'xstate/lib/waitFor';

import { createTagDetailMachine } from '../tagDetailMachine';

/**
 * Regression guard for the load-more / navigation race:
 *
 * When the user clicks "Load More" on tag A and navigates to tag B before the
 * request resolves, SET_TAG_NAME transitions the machine out of
 * `loaded.loadingMore` into `loading`. Because `loadMoreComponents` is an XState
 * invoked service, exiting its state stops the actor, so A's late result is never
 * routed to `appendComponents` and cannot leak into tag B's component list.
 *
 * This test locks that behavior in: it would fail if SET_TAG_NAME were ever
 * changed to a transition that does not exit `loaded` (e.g. an internal
 * transition), which would reintroduce the stale-append bug.
 */
const flush = () => new Promise((resolve) => setTimeout(resolve, 0));

const tagResult = (tagName: string) => ({
  tagDetail: { name: tagName, firstCreated: null, lastUpdated: null, attributes: {} },
  components: [{ id: `${tagName}-initial`, name: tagName, format: 'x', repository: 'r' }],
  continuationToken: 'token',
  totalComponentCount: 5,
});

describe('tagDetailMachine — load-more race on navigation', () => {
  it('does not append a previous tag\'s load-more result after SET_TAG_NAME', async () => {
    let resolveLoadMore: (value: unknown) => void = () => {};
    const loadMorePromise = new Promise((resolve) => {
      resolveLoadMore = resolve;
    });

    let loadCall = 0;
    let resolveBLoad: () => void = () => {};

    const machine = createTagDetailMachine('A').withConfig({
      services: {
        // A resolves immediately (with a continuationToken so LOAD_COMPONENTS is
        // valid); B stays in flight so A's late load-more resolves while the
        // machine is still in `loading`.
        loadTagDetail: (context) => {
          loadCall += 1;
          if (loadCall === 1) return Promise.resolve(tagResult(context.tagName));
          return new Promise((resolve) => {
            resolveBLoad = () => resolve(tagResult(context.tagName));
          });
        },
        loadMoreComponents: () => loadMorePromise as Promise<never>,
      },
    });

    const service = interpret(machine).start();
    await waitFor(service, (state) => state.matches('loaded'));

    // Load More on A -> loaded.loadingMore, request in flight.
    service.send({ type: 'LOAD_COMPONENTS', append: true });
    expect(service.state.matches({ loaded: 'loadingMore' })).toBe(true);

    // Navigate to B mid-flight; B's load is deferred, so we sit in `loading`.
    service.send({ type: 'SET_TAG_NAME', tagName: 'B' });
    expect(service.state.matches('loading')).toBe(true);

    // A's stale load-more resolves — must be ignored.
    resolveLoadMore({
      components: [{ id: 'A-leaked', name: 'A-leaked', format: 'x', repository: 'r' }],
      continuationToken: null,
    });
    await flush();
    await flush();
    expect(service.state.context.components.map((c) => c.id)).not.toContain('A-leaked');

    // B finishes and shows only its own data.
    resolveBLoad();
    await flush();
    await flush();
    const finalIds = service.state.context.components.map((c) => c.id);
    expect(finalIds).not.toContain('A-leaked');
    expect(finalIds).toContain('B-initial');

    service.stop();
  });
});
