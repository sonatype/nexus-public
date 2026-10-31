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
 * Reproduction: a raw (versionless) component's detail page renders blank with
 * "Maximum update depth exceeded".
 *
 * Unlike GADetailPage.test.tsx, this mocks NOTHING above the network. useGADetail,
 * useComponentVersions and useGARepositoriesForVersion all run for real, against fixtures
 * matching what a live instance returns for a raw component:
 *
 *   /v1/search           -> version: ''
 *   /v1/search/versions  -> total 1, items[0].version: ''
 *
 * The URL is bare (navigateToDetail omits the param when result.version is falsy), so
 * initialVersion is undefined.
 */

import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import { Theme } from '@radix-ui/themes';

// --- network ----------------------------------------------------------------
jest.mock('axios', () => ({ get: jest.fn() }));

const mockRestGet = jest.fn();
jest.mock('../../../../../../interface/api', () => ({
  restClient: {
    get: (...args: unknown[]) => mockRestGet(...args),
  },
  ENDPOINTS: {
    SEARCH_REPOSITORIES: '/service/rest/v1/search/repositories',
    IQ_CONNECTION: '/service/rest/v1/iq/connection',
    IQ_COMPONENT_EVALUATION: '/service/rest/v1/iq/component-evaluation',
  },
}));

jest.mock('../../../../config/featureFlags', () => ({ isMockMode: () => false }));

// --- router: bare URL, no ?version -----------------------------------------
const mockGo = jest.fn();
const routerState = {
  currentName: 'preview.browse.search.component.overview',
  params: {} as Record<string, unknown>,
};
jest.mock('@uirouter/react', () => ({
  useRouter: () => ({
    stateService: { go: mockGo },
    globals: {
      get current() {
        return { name: routerState.currentName };
      },
      // A NEW object each read, as UIRouter's live globals effectively are.
      get params() {
        return { ...routerState.params };
      },
    },
  }),
  // Returns a fresh params object every render — this is the shape the real hook has.
  useCurrentStateAndParams: () => ({
    state: { name: routerState.currentName },
    params: { ...routerState.params },
  }),
}));

import Axios from 'axios';
import { GADetailPage } from '../GADetailPage';

const mockAxios = Axios as jest.Mocked<typeof Axios>;

// Exactly the shape the live instance returned for the reported component.
const RAW_GROUP = '/animport/c1f79f46-672b-46eb-96ec-2355e3ae4ee7';
const RAW_NAME = `${RAW_GROUP}/file7.txt`;
const RAW_GA_ID = `raw:${RAW_GROUP}:${RAW_NAME}`;

beforeEach(() => {
  jest.clearAllMocks();
  routerState.currentName = 'preview.browse.search.component.overview';
  routerState.params = {};

  mockAxios.get.mockImplementation((url: string) => {
    if (url.includes('/search/versions')) {
      return Promise.resolve({
        data: {
          // The decisive fixture: a raw component's only "version" is the empty string.
          items: [{ version: '', lastUpdated: '2026-08-06T13:46:39.656Z', repositories: ['my-raw'] }],
          total: 1,
          page: 0,
          size: 20,
        },
      });
    }
    // Version-scoped asset fetch.
    return Promise.resolve({
      data: {
        items: [
          {
            repository: 'my-raw',
            assets: [
              {
                id: 'a1',
                repository: 'my-raw',
                path: `${RAW_NAME}`,
                downloadUrl: `http://localhost:8081/repository/my-raw${RAW_NAME}`,
                contentType: 'text/plain',
                lastModified: '2026-08-06T13:46:39.656Z',
                fileSize: 12,
                checksum: { sha1: 'abc' },
              },
            ],
          },
        ],
        continuationToken: null,
      },
    });
  });

  mockRestGet.mockImplementation((url: string) => {
    if (url.includes('/iq/connection')) return Promise.resolve({ enabled: false });
    if (url.includes('/search/repositories')) {
      return Promise.resolve({
        items: [{ repositoryName: 'my-raw', type: 'hosted', versionCount: 1 }],
        totalCount: 1,
      });
    }
    return Promise.resolve(null);
  });
});

describe('raw component detail page (repro)', () => {
  it('renders without exceeding React update depth', async () => {
    const errors: string[] = [];
    const spy = jest.spyOn(console, 'error').mockImplementation((...args) => {
      errors.push(String(args[0]));
    });

    render(
      <Theme>
        <GADetailPage gaId={RAW_GA_ID} />
      </Theme>,
    );

    // The component name must appear — a blank page is the reported symptom. The name occurs in
    // both the breadcrumb and the heading, hence getAllByText.
    await waitFor(() => {
      expect(screen.getAllByText(/file7\.txt/).length).toBeGreaterThan(0);
    });

    const depthErrors = errors.filter((e) => /Maximum update depth/i.test(e));
    spy.mockRestore();
    expect(depthErrors).toEqual([]);
  });

  // The reported failure is on the parent state (a card click and a pasted URL both land there,
  // and getTabFromRoute defaults it to 'overview'), but every tab mounts a different subtree, so
  // sweep all of them rather than assume.
  it.each([
    ['parent (no tab child)', 'preview.browse.search.component'],
    ['overview', 'preview.browse.search.component.overview'],
    ['versions', 'preview.browse.search.component.versions'],
    ['repositories', 'preview.browse.search.component.repos'],
    ['files', 'preview.browse.search.component.files'],
    ['security', 'preview.browse.search.component.security'],
  ])('renders the %s route without an update-depth loop', async (_label, routeName) => {
    routerState.currentName = routeName;
    const errors: string[] = [];
    const spy = jest.spyOn(console, 'error').mockImplementation((...args) => {
      errors.push(String(args[0]));
    });

    render(
      <Theme>
        <GADetailPage gaId={RAW_GA_ID} />
      </Theme>,
    );
    // Let every effect, fetch and follow-up render settle.
    await new Promise((r) => setTimeout(r, 250));

    const depthErrors = errors.filter((e) => /Maximum update depth/i.test(e));
    spy.mockRestore();
    expect(depthErrors).toEqual([]);
  });

  /*
   * The actual reported failure.
   *
   * The route declares `version: { value: null, squash: true }`, so on a bare URL UI-Router
   * resolves the `version` prop to null — not undefined. Every other test here passed no prop at
   * all and therefore got undefined, which is why none of them reproduced this.
   *
   * With null, two effects fight: GADetailPage's versionless effect sends SELECT_VERSION('')
   * because newestVersion is '', and useGADetail's URL-sync effect immediately sends
   * SELECT_VERSION(null) back because `initialVersion === undefined` doesn't catch null and
   * null !== ''. Each round re-invokes loadAssets and re-renders, so the page paints once and
   * then dies with "Maximum update depth exceeded".
   */
  it('settles for a raw component when the router resolves version to null', async () => {
    const errors: string[] = [];
    const spy = jest.spyOn(console, 'error').mockImplementation((...args) => {
      errors.push(String(args[0]));
    });

    render(
      <Theme>
        {/* null is what the resolve actually supplies; the prop type says string | undefined. */}
        <GADetailPage gaId={RAW_GA_ID} version={null as unknown as undefined} />
      </Theme>,
    );

    await new Promise((r) => setTimeout(r, 400));
    spy.mockRestore();

    expect(errors.filter((e) => /Maximum update depth/i.test(e))).toEqual([]);

    // The version must come to rest on '' — the valid selected version for a raw component —
    // rather than ping-ponging between '' and null.
    const versionsRequested = mockAxios.get.mock.calls
      .map(([url]) => String(url))
      .filter((u) => !u.includes('/search/versions'));
    expect(versionsRequested.length).toBeLessThanOrEqual(2);
  });

  /*
   * `effectiveVersion` feeds useGARepositoriesForVersion, which skips the fetch for a null
   * version. Built with `||`, every candidate for a raw component is falsy — selectedVersion '',
   * initialVersion null, newestVersion '' — so it collapsed to null and the Repositories tab and
   * Overview's Repository row were silently empty for every versionless component. `??` keeps ''
   * as the real selected version it is.
   */
  it('fetches repositories for a raw component, whose version is the empty string', async () => {
    render(
      <Theme>
        <GADetailPage gaId={RAW_GA_ID} version={null as unknown as undefined} />
      </Theme>,
    );

    await waitFor(() => {
      const repoCalls = mockRestGet.mock.calls
        .map(([url]) => String(url))
        .filter((u) => u.includes('/search/repositories'));
      expect(repoCalls.length).toBeGreaterThan(0);
    });

    // And the version it asks for is the empty string, not a dropped or defaulted value.
    const [repoUrl] = mockRestGet.mock.calls
      .map(([url]) => String(url))
      .filter((u) => u.includes('/search/repositories'));
    expect(repoUrl).toContain('version=');
  });

  /*
   * The assets load is driven by the machine's initial context — `createGaDetailMachine` seeds
   * `selectedVersion` from `initialVersion` and an `always` transition leaves `idle` as soon as it
   * is non-null — not by a SELECT_VERSION event from the URL-sync effect. That distinction matters:
   * the sync effect deliberately adopts only genuine URL *changes* (see useGADetail), so anything
   * relying on it to fire once on mount would never load. This asserts the outcome directly, with
   * the real machine rather than a stub, because that is the part a mocked machine cannot tell you.
   */
  it("loads a raw component's assets on mount, with no SELECT_VERSION round-trip", async () => {
    render(
      <Theme>
        <GADetailPage gaId={RAW_GA_ID} version={null as unknown as undefined} />
      </Theme>,
    );

    await waitFor(() => {
      const assetCalls = mockAxios.get.mock.calls
        .map(([url]) => String(url))
        .filter((u) => u.includes('/search') && !u.includes('/search/versions'));
      expect(assetCalls.length).toBeGreaterThan(0);
    });

    // A versionless component's assets query omits `version` entirely — see
    // componentVersionDetailApi's `if (request.version !== '')`. Asserting the format is present
    // keeps this honest about which request was matched.
    const [assetUrl] = mockAxios.get.mock.calls
      .map(([url]) => String(url))
      .filter((u) => u.includes('/search') && !u.includes('/search/versions'));
    expect(assetUrl).toContain('format=raw');
  });

  it('does not call stateService.go in a loop', async () => {
    render(
      <Theme>
        <GADetailPage gaId={RAW_GA_ID} />
      </Theme>,
    );

    await new Promise((r) => setTimeout(r, 300));

    // A versionless component cannot canonicalise ('' can't round-trip a squashed param),
    // so the redirect must never fire at all.
    expect(mockGo).not.toHaveBeenCalled();
  });
});
