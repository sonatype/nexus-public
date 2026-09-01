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
 * Sort integration tests — the REAL search machine and the REAL URL.
 *
 * `UnifiedSearchPage.test.tsx` mocks `useUnifiedSearch` and `useSearchUrlState`
 * wholesale, which is right for asserting how the page wires callbacks but
 * cannot prove the round trip actually closes. These tests mock only the edges
 * the page cannot reach in jsdom (HTTP, UIRouter navigation, repository and
 * dashboard fetches) and exercise the whole chain:
 *
 *   dropdown -> page handler -> machine -> shared state -> dropdown
 *                                       -> browser URL -> next request
 *
 * jest.mock is file-scoped, so keeping the real hooks requires a separate file
 * from the mocked-hook suite.
 */

import React from 'react';
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';
import Axios from 'axios';

import UnifiedSearchPage from '../UnifiedSearchPage';
import { SEARCH_RETURN_URL_KEY } from '../useSearchNavigation';

jest.mock('axios');
const mockedAxios = Axios as jest.Mocked<typeof Axios>;

const mockNavigateToDetail = jest.fn();

// UIRouter is not available in jsdom; the navigation target is asserted through
// this spy. The storage key is re-exported from the real module below.
jest.mock('../useSearchNavigation', () => ({
  ...jest.requireActual('../useSearchNavigation'),
  useSearchNavigation: () => ({
    navigateToDetail: mockNavigateToDetail,
    getDetailRoute: jest.fn(),
  }),
}));

jest.mock('../useRepositories', () => ({
  useRepositories: () => ({
    repositories: ['maven-central', 'npm-proxy'],
    availableFormats: new Set(['maven2', 'npm']),
    formatCounts: { maven2: 1, npm: 1 },
    loading: false,
    error: undefined,
  }),
}));

jest.mock('../../../Welcome/dashboard/useInstanceTotals', () => ({
  useInstanceTotals: () => ({ data: null, loading: false }),
}));

/** Same response shape as `useUnifiedSearch.test.ts`. */
const mockSearchResponse = {
  data: {
    items: [
      {
        id: 'comp-1',
        repository: 'npm-proxy',
        format: 'npm',
        group: null,
        name: 'lodash',
        version: '4.17.21',
        assets: [{ id: 'asset-1', path: '/lodash/-/lodash-4.17.21.tgz', downloadUrl: 'http://x' }],
      },
    ],
    continuationToken: undefined,
  },
};

/** Hash path the search page lives on. */
const HASH_PATH = '#preview/browse/search';

/** Query params currently in the browser URL. */
function urlParams(): URLSearchParams {
  const hash = window.location.hash;
  const queryIndex = hash.indexOf('?');
  return new URLSearchParams(queryIndex === -1 ? '' : hash.slice(queryIndex + 1));
}

/** Query params of the most recent search request. */
function lastRequestParams(): URLSearchParams {
  const calls = mockedAxios.get.mock.calls;
  const [url] = calls[calls.length - 1] as [string];
  return new URLSearchParams(url.slice(url.indexOf('?') + 1));
}

/**
 * The results header renders three responsive copies of the sort dropdown
 * (mobile/tablet/desktop), hidden with CSS rather than unmounted, so all three
 * are in the accessibility tree. They render identical state; assert on the
 * first.
 */
function sortTrigger(): HTMLElement {
  return screen.getAllByRole('combobox', { name: /^Sort:/ })[0];
}

/**
 * Direction labels repeat across fields ('A-Z' belongs to both Name and
 * Repository), so options are addressed by position within the open list. The
 * groups are ordered as SORT_OPTION_GROUPS declares them — Last updated, Name,
 * Repository — which puts Name's A-Z and Z-A first.
 */
async function openSortDropdown(): Promise<void> {
  await userEvent.click(sortTrigger());
  await waitFor(() =>
    expect(screen.getAllByRole('option', { name: 'A-Z' }).length).toBeGreaterThan(0),
  );
}

/** Open the dropdown and pick the first option with this direction label. */
async function selectSortOption(directionLabel: string): Promise<void> {
  await openSortDropdown();
  await userEvent.click(screen.getAllByRole('option', { name: directionLabel })[0]);
}

/** Render at a URL and wait for the mount search to settle. */
async function renderAt(queryString: string): Promise<void> {
  window.history.replaceState(
    {},
    '',
    queryString ? `/${HASH_PATH}?${queryString}` : `/${HASH_PATH}`,
  );
  render(
    <Theme>
      <UnifiedSearchPage />
    </Theme>,
  );
  await waitFor(() => expect(mockedAxios.get).toHaveBeenCalled());
}

describe('sort integration (real machine + real URL)', () => {
  beforeAll(() => {
    // Radix Select needs these; jsdom implements none of them.
    Element.prototype.hasPointerCapture = jest.fn();
    Element.prototype.releasePointerCapture = jest.fn();
    Element.prototype.scrollIntoView = jest.fn();
  });

  beforeEach(() => {
    jest.clearAllMocks();
    sessionStorage.clear();
    mockedAxios.get.mockResolvedValue(mockSearchResponse);
    mockedAxios.isCancel = jest.fn(() => false) as never;
  });

  it('sends the default sort and shows it in the dropdown on a parameterless URL', async () => {
    await renderAt('');

    expect(sortTrigger()).toHaveTextContent('Last updated — Newest first');
    expect(lastRequestParams().get('sort')).toBe('last_updated');
    expect(lastRequestParams().get('direction')).toBe('desc');
    // Default sort is omitted from the URL.
    expect(urlParams().get('sort')).toBeNull();
  });

  it('propagates a dropdown selection to the URL and the next request', async () => {
    await renderAt('');
    await selectSortOption('A-Z');

    await waitFor(() => expect(sortTrigger()).toHaveTextContent('Name — A-Z'));

    // The request the machine issued carries the API aliases.
    await waitFor(() => expect(lastRequestParams().get('sort')).toBe('name'));
    expect(lastRequestParams().get('direction')).toBe('asc');

    // ...and the URL catches up once the write debounce elapses.
    await waitFor(() => expect(urlParams().get('sort')).toBe('name'));
    expect(urlParams().get('direction')).toBe('asc');
  });

  it('omits a default direction from the URL while still sending it to the API', async () => {
    await renderAt('');

    await selectSortOption('Z-A');

    await waitFor(() => expect(sortTrigger()).toHaveTextContent('Name — Z-A'));

    await waitFor(() => expect(lastRequestParams().get('sort')).toBe('name'));
    expect(lastRequestParams().get('direction')).toBe('desc');

    await waitFor(() => expect(urlParams().get('sort')).toBe('name'));
    // `desc` is the default direction, so the URL omits it — the field alone is
    // enough for rehydration to resolve the same state. Only a non-default
    // direction is written (asserted in the case above).
    expect(urlParams().get('direction')).toBeNull();
  });

  it('restores a non-default sort from the URL on load', async () => {
    await renderAt('sort=name&direction=asc');

    expect(sortTrigger()).toHaveTextContent('Name — A-Z');
    expect(lastRequestParams().get('sort')).toBe('name');
    expect(lastRequestParams().get('direction')).toBe('asc');
  });

  it('restores a non-default direction on the default field from a direction-only URL', async () => {
    // `lastUpdated/asc` serializes to `?direction=asc` with no `sort` param,
    // because field and direction are omitted from the URL independently. That
    // makes a direction-only URL a reachable state, so the whole round trip has
    // to close on it: dropdown label, outgoing request, and the URL itself.
    await renderAt('direction=asc');

    expect(sortTrigger()).toHaveTextContent('Last updated — Oldest first');

    expect(lastRequestParams().get('sort')).toBe('last_updated');
    expect(lastRequestParams().get('direction')).toBe('asc');

    // The URL must survive unchanged: `sort` stays absent (lastUpdated is the
    // default field) and `direction` is not collapsed away. Wait past the 350ms
    // write debounce so a late rewrite would be caught.
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 500));
    });
    expect(urlParams().get('sort')).toBeNull();
    expect(urlParams().get('direction')).toBe('asc');
  });

  it('falls back to the default sort for an unsupported URL sort field', async () => {
    // `version` is not offered by the controls, so it must not reach the API
    // either — otherwise the dropdown would display one sort while the request
    // used another.
    await renderAt('sort=version&direction=desc');

    expect(sortTrigger()).toHaveTextContent('Last updated — Newest first');
    expect(lastRequestParams().get('sort')).toBe('last_updated');
  });

  describe('browser history', () => {
    it('restores the default sort when Back reaches a URL without sort params', async () => {
      await renderAt('sort=name&direction=asc');
      expect(sortTrigger()).toHaveTextContent('Name — A-Z');

      // Simulate Back to the parameterless entry: the browser replaces the URL,
      // then fires popstate.
      window.history.replaceState({}, '', `/${HASH_PATH}`);
      // The listener rehydrates the machine synchronously, so the dispatch has
      // to be inside act() or React reports the update as unwrapped.
      act(() => {
        window.dispatchEvent(new PopStateEvent('popstate'));
      });

      // Machine/UI state resolves back to the canonical default...
      await waitFor(() =>
        expect(sortTrigger()).toHaveTextContent('Last updated — Newest first'),
      );

      // ...the re-run search uses the default...
      await waitFor(() => expect(lastRequestParams().get('sort')).toBe('last_updated'));

      // ...and the stale sort is never written back onto the entry we navigated
      // to. Wait past the 350ms URL-write debounce to catch a late write.
      await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 500));
      });
      expect(urlParams().get('sort')).toBeNull();
      expect(urlParams().get('direction')).toBeNull();
    });

    it('restores a non-default sort when Back reaches a URL that carries one', async () => {
      await renderAt('');
      await selectSortOption('Z-A');
      await waitFor(() => expect(urlParams().get('sort')).toBe('name'));

      window.history.replaceState({}, '', `/${HASH_PATH}?sort=name&direction=asc`);
      act(() => {
        window.dispatchEvent(new PopStateEvent('popstate'));
      });

      await waitFor(() => expect(sortTrigger()).toHaveTextContent('Name — A-Z'));
    });
  });

  describe('component detail round trip', () => {
    // These originally asserted sort preservation through
    // COMPONENT_DETAIL_RETURN_SEARCH_KEY, a sessionStorage copy of machine state.
    // That channel is gone: it was read ahead of the URL on every mount, and
    // browsers copy sessionStorage into a tab opened from a link, so a stale
    // payload beat a fresh deep link (NEXUS-54503 Defect 1). The URL is now the
    // only carrier, so the round trip is asserted through it instead.

    it('leaves the active sort in the URL when navigating to a component', async () => {
      await renderAt('');
      await selectSortOption('Z-A');
      await waitFor(() => expect(sortTrigger()).toHaveTextContent('Name — Z-A'));

      await userEvent.click(
        await screen.findByRole('button', { name: /view details for lodash/i }),
      );

      // Single argument now — the detail page reads the search state back out of
      // the URL rather than being handed a copy of it.
      expect(mockNavigateToDetail).toHaveBeenCalledWith(
        expect.objectContaining({ name: 'lodash' }),
      );
      // The click flushes the pending debounced write, so the URL captured as the
      // breadcrumb's return target already carries the sort. `direction` stays out
      // of the URL because desc is the default — see the dedicated test above — but
      // it still reaches the API, which is what makes the round trip faithful.
      expect(urlParams().get('sort')).toBe('name');
      expect(urlParams().get('direction')).toBeNull();
      expect(lastRequestParams().get('direction')).toBe('desc');
    });

    it('restores the sort when the breadcrumb returns to the captured URL', async () => {
      // What consumeSearchReturnUrl() replays on a breadcrumb click.
      await renderAt('sort=name&direction=desc');

      expect(sortTrigger()).toHaveTextContent('Name — Z-A');
      expect(lastRequestParams().get('sort')).toBe('name');
      expect(lastRequestParams().get('direction')).toBe('desc');
      await waitFor(() => expect(urlParams().get('sort')).toBe('name'));
    });

    it('ignores a stale sort left in sessionStorage (NEXUS-54503 Defect 1)', async () => {
      // Pins the removal: a payload from the retired channel, or one copied into a
      // new tab, must not override the URL the user actually opened.
      sessionStorage.setItem(
        'nexus-component-detail-return-search',
        JSON.stringify({
          query: 'stale',
          format: 'maven',
          filters: {},
          sortField: 'name',
          sortDirection: 'desc',
        }),
      );

      await renderAt('');

      expect(sortTrigger()).toHaveTextContent('Last updated — Newest first');
      expect(lastRequestParams().get('sort')).toBe('last_updated');
      expect(urlParams().get('q')).toBeNull();
    });

    it('does not let a stored return URL override a fresh deep link', async () => {
      // The replacement channel is single-use and only read on an explicit
      // breadcrumb click, never on mount — so a leftover value is inert here.
      sessionStorage.setItem(SEARCH_RETURN_URL_KEY, `${HASH_PATH}?sort=name&direction=desc`);

      await renderAt('');

      expect(sortTrigger()).toHaveTextContent('Last updated — Newest first');
      expect(lastRequestParams().get('sort')).toBe('last_updated');
    });
  });
});
