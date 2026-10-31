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
 * Selecting a version must issue requests for that version only.
 *
 * `handleVersionSelect` writes the new version to two places with different timing: the machine
 * synchronously via `selectVersion`, and the URL asynchronously via `stateService.go`. In the window
 * between them the machine holds the new version while the route params still hold the old one. The
 * URL->machine sync effect in `useGADetail` listed `selectedVersion` in its deps, so it woke inside
 * that window, read the lagging URL value as authoritative, and pushed the *previous* version back
 * into the machine — producing three /search/repositories calls per click (new, old, new) and a
 * flash of the wrong version's data.
 *
 * The mock below reproduces that timing faithfully: `go` updates the route params on a later
 * microtask, exactly as the real router does. A synchronous mock hides the bug entirely, which is
 * why the existing suite passed while a browser showed three requests per click.
 */

const mockRestGet = jest.fn();

jest.mock('../../../../../../interface/api/rest-client', () => ({
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

const routerState = {
  currentName: 'preview.browse.search.component.versions',
  params: { gaId: 'npm:dummy:dummy-npm-lib-shared02-200', version: '3.0.3' } as Record<
    string,
    unknown
  >,
};

/**
 * Asynchronous by design — `stateService.go` returns a promise and the params it writes are not
 * visible to the very next render. That lag is the bug's whole substance.
 */
const mockGo = jest.fn((_state: string, params?: Record<string, unknown>) => {
  const applied = Promise.resolve().then(() => {
    if (params && 'version' in params) {
      routerState.params = { ...routerState.params, ...params };
    }
  });
  return applied;
});

jest.mock('@uirouter/react', () => ({
  useRouter: () => ({
    stateService: { go: mockGo },
    globals: {
      get current() {
        return { name: routerState.currentName };
      },
      get params() {
        return { ...routerState.params };
      },
    },
  }),
  useCurrentStateAndParams: () => ({
    state: { name: routerState.currentName },
    params: { ...routerState.params },
  }),
}));

import React from 'react';
import { render, screen, waitFor, fireEvent, act } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import Axios from 'axios';
import { GADetailPage } from '../GADetailPage';

const mockAxios = Axios as jest.Mocked<typeof Axios>;

const GA_ID = 'npm:dummy:dummy-npm-lib-shared02-200';
const VERSIONS = ['3.0.18', '3.0.17', '3.0.3', '3.0.2'];

/** Versions requested on /search/repositories, in call order. */
function repositoryVersionsRequested(): string[] {
  return mockRestGet.mock.calls
    .map(([url]) => String(url))
    .filter((u) => u.includes('/search/repositories'))
    .map((u) => new URL(u, 'http://localhost').searchParams.get('version') ?? '<none>');
}

beforeEach(() => {
  jest.clearAllMocks();
  routerState.currentName = 'preview.browse.search.component.versions';
  routerState.params = { gaId: GA_ID, version: '3.0.3' };

  mockRestGet.mockImplementation((url: string) => {
    if (url.includes('/search/repositories')) {
      return Promise.resolve({
        items: [{ repositoryName: 'npm-hosted', type: 'hosted', versionCount: VERSIONS.length }],
        totalCount: 1,
      });
    }
    if (url.includes('/iq/')) {
      return Promise.resolve({ connected: false });
    }
    return Promise.resolve({ items: [], totalCount: 0 });
  });

  mockAxios.get.mockImplementation((url: string) => {
    if (url.includes('/search/versions')) {
      return Promise.resolve({
        data: {
          items: VERSIONS.map((version) => ({
            version,
            lastUpdated: '2026-08-06T13:46:39.656Z',
            repositories: ['npm-hosted'],
          })),
          total: VERSIONS.length,
          page: 0,
          size: 20,
        },
      });
    }
    return Promise.resolve({ data: { items: [] } });
  });
});

describe('selecting a version', () => {
  it('requests repositories for the newly selected version only, never the previous one', async () => {
    render(
      <Theme>
        <GADetailPage gaId={GA_ID} version="3.0.3" />
      </Theme>,
    );

    // Let the initial load settle — a request for 3.0.3 here is correct, it is the current version.
    await waitFor(() => expect(repositoryVersionsRequested()).toContain('3.0.3'));
    await waitFor(() => expect(screen.getByTestId('versions-table')).toBeInTheDocument());

    // Everything from here on is attributable to the click alone.
    mockRestGet.mockClear();

    const row = document.querySelector('[data-version="3.0.18"]');
    expect(row).not.toBeNull();

    await act(async () => {
      fireEvent.click(row as Element);
      // Flush the microtask in which `go` applies the new params.
      await Promise.resolve();
    });

    await waitFor(() => expect(repositoryVersionsRequested()).toContain('3.0.18'));
    // Give any lagging pushback a chance to land, so this fails loudly rather than racing green.
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 50));
    });

    const requested = repositoryVersionsRequested();

    // The regression: the version just navigated away from must never be re-requested.
    expect(requested).not.toContain('3.0.3');
    // And the new version is asked for once, not once per sync round-trip.
    expect(requested.filter((v) => v === '3.0.18')).toHaveLength(1);
  });
});
