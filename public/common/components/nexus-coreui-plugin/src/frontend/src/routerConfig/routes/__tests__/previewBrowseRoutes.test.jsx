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

import { Permissions } from '@sonatype/nexus-ui-plugin';
import { UIRouter, memoryLocationPlugin, servicesPlugin } from '@uirouter/core';
import { previewBrowseRoutes } from '../previewBrowseRoutes';

const routeByName = (name) => previewBrowseRoutes.find((r) => r.name === name);

const COMPONENT_STATE = 'preview.browse.search.component';

/**
 * Register the real component-detail route definitions in a standalone UI-Router, rebased
 * under a local root so no ancestor states from other route files are needed. Only the URL
 * fragments and param declarations under test are taken from the shipped definitions.
 */
const componentDetailRouter = () => {
  const router = new UIRouter();
  router.plugin(servicesPlugin);
  router.plugin(memoryLocationPlugin);
  router.stateRegistry.register({ name: 'root', url: '', abstract: true });
  previewBrowseRoutes
    .filter((r) => r.name === COMPONENT_STATE || r.name.startsWith(`${COMPONENT_STATE}.`))
    .forEach(({ name, url, params }) =>
      router.stateRegistry.register({
        name: name.replace(COMPONENT_STATE, 'root.component'),
        url,
        ...(params ? { params } : {}),
      })
    );
  return router;
};

const matchState = (router, path) => {
  const match = router.urlService.match({ path, search: {}, hash: '' });
  return { name: match?.rule?.state?.name, params: match?.match };
};

describe('previewBrowseRoutes permission gating (NEXUS-54048)', () => {
  it('gates preview.browse.browse with repository-view/content-selector prefixes and browseableformats', () => {
    const route = routeByName('preview.browse.browse');
    expect(route).toBeDefined();
    const vr = route.data.visibilityRequirements;
    expect(vr).toBeDefined();
    expect(vr.anonymousAccessOrHasUser).toBe(true);
    expect(vr.permissionPrefixes).toEqual([
      'nexus:repository-view',
      'nexus:repository-content-selector',
    ]);
    expect(vr.statesEnabled).toContainEqual({ key: 'browseableformats', defaultValue: [] });
  });

  it('gates preview.browse.upload with COMPONENT.CREATE', () => {
    const route = routeByName('preview.browse.upload');
    expect(route).toBeDefined();
    expect(route.data.visibilityRequirements).toBeDefined();
    expect(route.data.visibilityRequirements.permissions).toContain(Permissions.COMPONENT.CREATE);
  });

  it('gates preview.browse.tags with TAGS.READ and PRO edition', () => {
    const route = routeByName('preview.browse.tags');
    expect(route).toBeDefined();
    const vr = route.data.visibilityRequirements;
    expect(vr).toBeDefined();
    expect(vr.permissions).toContain(Permissions.TAGS.READ);
    expect(vr.editions).toContain('PRO');
  });

  it('gates preview.browse.tagdetail with TAGS.READ and PRO edition (sibling of tags, no inherited gate)', () => {
    const route = routeByName('preview.browse.tagdetail');
    expect(route).toBeDefined();
    const vr = route.data.visibilityRequirements;
    expect(vr).toBeDefined();
    expect(vr.permissions).toContain(Permissions.TAGS.READ);
    expect(vr.editions).toContain('PRO');
  });

  it('gates preview.browse.api with SETTINGS.READ (matches Default UI / Classic)', () => {
    const route = routeByName('preview.browse.api');
    expect(route).toBeDefined();
    const vr = route.data.visibilityRequirements;
    expect(vr).toBeDefined();
    expect(vr.requiresUser).toBe(true);
    // API has no dedicated privilege; the Default UI (adminRoutes.js) and Classic UI
    // gate it on nexus:settings:read, so the Preview route mirrors that.
    expect(vr.permissions).toContain(Permissions.SETTINGS.READ);
  });
});

describe('component detail version param (NEXUS-54201)', () => {
  it('marks version as dynamic so changing it does not re-enter the state', () => {
    const route = routeByName('preview.browse.search.component');
    expect(route.params.version.dynamic).toBe(true);
  });

  it('keeps version squashed and defaulting to null', () => {
    const route = routeByName('preview.browse.search.component');
    expect(route.params.version.squash).toBe(true);
    expect(route.params.version.value).toBeNull();
  });
});

/**
 * gaId must survive a write/read cycle through the URL unchanged.
 *
 * A param whose value-in differs from its value-out never settles: gaId is not `dynamic`, so
 * UI-Router answers the mismatch with a state re-entry, remounting GADetailPage and re-firing
 * every request the page makes. That is the double-fetch in NEXUS-54201.
 *
 * These assertions run against the real UrlMatcher with the route's own param config, because
 * the defect lives in UI-Router's encode/decode contract — not in any component. A mocked
 * router cannot see it.
 */
describe('component detail gaId param round-trips (NEXUS-54201)', () => {
  const route = routeByName('preview.browse.search.component');
  const factory = new UIRouter().urlMatcherFactory;
  const compile = (url) => factory.compile(url, { state: { params: route.params } });
  // Path only. Compiling the `?version` query too would make format() resolve version's
  // squashed default, which needs an injector that only exists at runtime.
  const detailPath = route.url.split('?')[0];

  it.each([
    ['unscoped npm', 'npm:dummy:dummy-npm-lib-shared02-200'],
    ['scoped npm (name contains a slash)', 'npm:@dummy/dummy-npm-lib'],
    ['three-part maven', 'maven2:org.apache.commons:commons-lang3'],
    ['versionless raw', 'raw:package'],
  ])('round-trips %s unchanged', (_label, gaId) => {
    const matcher = compile(detailPath);
    const written = matcher.format({ gaId });

    expect(matcher.exec(written, {}).gaId).toBe(gaId);
  });

  it('does not swallow a trailing tab segment into gaId', () => {
    const gaId = 'npm:dummy:dummy-npm-lib-shared02-200';
    const parent = compile(detailPath);
    // `type: 'string'` carries UI-Router's default pattern /.*/, which matches across `/` and
    // absorbs the tab segment. Only `type: 'path'` (/[^/]*/) stops at the segment boundary.
    const deepLink = `${parent.format({ gaId })}/overview`;

    expect(parent.exec(deepLink, {})).toBeNull();
  });

  it('lets the tab child route claim a deep link with gaId intact', () => {
    const gaId = 'npm:dummy:dummy-npm-lib-shared02-200';
    const parent = compile(detailPath);
    const child = compile(`${detailPath}/overview`);
    const deepLink = `${parent.format({ gaId })}/overview`;

    expect(child.exec(deepLink, {}).gaId).toBe(gaId);
  });
});

/**
 * Every tab child state re-declares its own `gaId`/`version` resolves, because UI-Router
 * resolves are per-state and are not inherited from the parent's. An omission on one of them is
 * silent: GADetailPage renders with that prop undefined, and only that one tab misbehaves.
 */
describe('component detail child routes carry the gaId and version resolves (NEXUS-54201)', () => {
  const CHILD_ROUTES = [
    'preview.browse.search.component.overview',
    'preview.browse.search.component.versions',
    'preview.browse.search.component.repos',
    'preview.browse.search.component.files',
    'preview.browse.search.component.security',
  ];

  it.each(CHILD_ROUTES)('%s resolves both params from $stateParams', (name) => {
    const route = routeByName(name);
    expect(route).toBeDefined();

    const tokens = (route.resolve ?? []).map((r) => r.token);
    expect(tokens).toContain('gaId');
    expect(tokens).toContain('version');

    for (const token of ['gaId', 'version']) {
      const resolve = route.resolve.find((r) => r.token === token);
      expect(resolve.deps).toEqual(['$stateParams']);
      // Read straight through, no re-encoding or decoding on the way in.
      expect(resolve.resolveFn({ [token]: 'sentinel' })).toBe('sentinel');
    }
  });
});

describe('component detail tab URLs survive a cold parse (NEXUS-54431)', () => {
  const GA_ID = 'maven2:org.apache.commons:commons-lang3';
  const ENCODED_GA_ID = encodeURIComponent(GA_ID);
  const TABS = ['overview', 'versions', 'repos', 'files', 'security'];

  it('declares gaId with a segment-bounded type so it cannot swallow the tab segment', () => {
    // UI-Router's `string` type matches /.*/ — across slashes. `path` matches /[^/]*/.
    expect(routeByName(COMPONENT_STATE).params.gaId.type).toBe('path');
    // `raw` must stay off: the router then encodes on write and decodes on read, so a caller
    // passing the plain 'format:group:name' gets a value that round-trips (NEXUS-54201). An
    // earlier fix for this ticket kept `raw: true` for pre-encoded callers; those callers now
    // pass plain values, and re-enabling `raw` would break the symmetry both tickets rely on.
    expect(routeByName(COMPONENT_STATE).params.gaId.raw).toBeUndefined();
  });

  it.each(TABS)('resolves /component/{gaId}/%s to that tab state, not to the parent', (tab) => {
    const router = componentDetailRouter();

    const { name, params } = matchState(router, `/component/${ENCODED_GA_ID}/${tab}`);

    // The defect: the parent state matched instead, with gaId = "…commons-lang3/security",
    // so a refresh on any tab fell back to Overview for a component that does not exist.
    expect(name).toBe(`root.component.${tab}`);
    expect(params.gaId).toBe(GA_ID);
  });

  it('still resolves the tab-less component URL to the parent state', () => {
    const router = componentDetailRouter();

    const { name, params } = matchState(router, `/component/${ENCODED_GA_ID}`);

    expect(name).toBe('root.component');
    expect(params.gaId).toBe(GA_ID);
  });

  it('writes the tab URL with a single round of encoding', () => {
    const router = componentDetailRouter();

    // Plain in, encoded once out. Passing an already-encoded gaId here would double-encode it.
    const url = router.stateService.href('root.component.security', { gaId: GA_ID });

    expect(url).toContain(`/component/${ENCODED_GA_ID}/security`);
    expect(url).not.toContain('%253A');
  });

  it('keeps a gaId whose name contains an encoded slash intact', () => {
    const router = componentDetailRouter();
    const dockerGaId = 'docker:library/nginx';

    const { name, params } = matchState(
      router,
      `/component/${encodeURIComponent(dockerGaId)}/security`
    );

    expect(name).toBe('root.component.security');
    expect(params.gaId).toBe(dockerGaId);
  });
});
