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
 * NEXUS-54498 — Browse repository list: fix filtering UX.
 *
 * These tests drive the real repository-list hook and the real filter sidebar so the
 * request behaviour is exercised end to end from the user's interactions:
 *   AC2 — typing in the name filter must not issue a request per keystroke.
 *   AC3 — filter checkboxes stay usable while a filter request is in flight, and a
 *         selection made mid-flight is applied on top of the name filter.
 */

import React from 'react';
import { render, screen, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import '@testing-library/jest-dom';
import { Theme } from '@radix-ui/themes';

const mockAxiosGet = jest.fn();

jest.mock('axios', () => ({
  get: (...args) => mockAxiosGet(...args),
}));

jest.mock('@uirouter/react', () => ({
  useRouter: () => ({ stateService: { go: jest.fn() } }),
  useCurrentStateAndParams: () => ({ params: {} }),
}));

// The sidebar's filter options are built from the full repository set, which BrowsePage
// loads through restClient (not axios), so the two request paths stay distinguishable.
jest.mock('../../../../../interface/api', () => {
  const allRepos = [
    { name: 'maven-central', format: 'maven2', type: 'proxy', url: '/maven-central', status: { online: true } },
    { name: 'npm-hosted', format: 'npm', type: 'hosted', url: '/npm-hosted', status: { online: true } },
  ];
  return {
    __esModule: true,
    restClient: {
      get: jest.fn((url) =>
        Promise.resolve(
          String(url).includes('/repositories/details/filtered') ? { data: allRepos } : [],
        ),
      ),
      post: jest.fn(() => Promise.resolve({})),
    },
    ENDPOINTS: { IQ_AUDIT: '/service/rest/internal/ui/firewall/repositories' },
  };
});

const REPO_NAMES = ['maven-central', 'npm-hosted'];

jest.mock('../repository-list/useRepositoryList', () => ({
  isIqServerEnabled: () => false,
  useRepositoryList: () => ({
    state: { loading: false, error: null, healthCheck: {}, firewallStatus: {} },
    showHealthCheckColumn: false,
    showIqPolicyViolationsColumn: false,
    enableHealthCheck: jest.fn(),
  }),
}));

// The table itself is covered by RepositoryList.test.tsx; here it only needs to report
// the loading flag it was handed.
jest.mock('../repository-list/RepositoryListTable', () => ({
  RepositoryListTable: ({ loading, repositories }) => (
    <div
      data-testid="mock-repository-list"
      data-loading={String(!!loading)}
      data-count={repositories?.length ?? 0}
    />
  ),
}));

jest.mock('../../../config/featureFlags', () => ({
  ...jest.requireActual('../../../config/featureFlags'),
  isMockMode: () => false,
}));

import { BrowsePage } from '../BrowsePage';

const FILTERED_API = '/repositories/details/filtered';

/** Requests the repository-list hook made, percent-decoded, newest last. */
const listRequests = () =>
  mockAxiosGet.mock.calls
    .map(([url]) => decodeURIComponent(String(url)))
    .filter((url) => url.includes(FILTERED_API));

const renderBrowsePage = () => render(<Theme><BrowsePage /></Theme>);

/** userEvent 12 helpers, wrapped so React effects flush before assertions. */
const type = async (element, text) => {
  await act(async () => {
    userEvent.type(element, text);
  });
};

const click = async (element) => {
  await act(async () => {
    userEvent.click(element);
  });
};

const page = (names) => ({
  data: {
    data: names.map((name) => ({ name, format: 'maven2', type: 'hosted', url: `/${name}` })),
    totalCount: names.length,
    totalPages: 1,
  },
});

describe('BrowsePage filtering (NEXUS-54498)', () => {
  beforeEach(() => {
    jest.useFakeTimers();
    mockAxiosGet.mockReset();
    mockAxiosGet.mockResolvedValue(page(REPO_NAMES));
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  const settle = async () => {
    await act(async () => {
      jest.advanceTimersByTime(1000);
      await Promise.resolve();
    });
  };

  describe('AC2 — the name filter is applied on a debounce', () => {
    it('issues one request for a typed word instead of one per keystroke', async () => {
      renderBrowsePage();
      await settle();
      mockAxiosGet.mockClear();

      const input = screen.getByPlaceholderText(/filter/i);
      await type(input, 'abc');

      // Nothing has been applied yet while the typing is still settling.
      expect(listRequests()).toHaveLength(0);

      await settle();

      const requests = listRequests();
      expect(requests).toHaveLength(1);
      expect(requests[0]).toContain('nameFilter=abc');
    });

    it('keeps the input value immediate while typing', async () => {
      renderBrowsePage();
      await settle();

      const input = screen.getByPlaceholderText(/filter/i);
      await type(input, 'abc');

      expect(input).toHaveValue('abc');
    });

    it('applies the last typed value, not an intermediate one', async () => {
      renderBrowsePage();
      await settle();
      mockAxiosGet.mockClear();

      const input = screen.getByPlaceholderText(/filter/i);
      await type(input, 'ab');
      await type(input, 'c');
      await settle();

      const requests = listRequests();
      expect(requests).toHaveLength(1);
      expect(requests[0]).toContain('nameFilter=abc');
    });
  });

  describe('AC2 — clearing the name filter', () => {
    it('applies the cleared value exactly once', async () => {
      renderBrowsePage();
      await settle();

      const input = screen.getByPlaceholderText(/filter/i);
      await type(input, 'abc');
      await settle();
      mockAxiosGet.mockClear();

      await click(screen.getByRole('button', { name: /clear filter/i }));
      expect(input).toHaveValue('');
      await settle();

      const requests = listRequests();
      expect(requests).toHaveLength(1);
      expect(requests[0]).not.toContain('nameFilter=');
    });

    it('does not re-apply a cleared name filter after resetting the filters', async () => {
      renderBrowsePage();
      await settle();

      const input = screen.getByPlaceholderText(/filter/i);
      await type(input, 'abc');
      await settle();
      mockAxiosGet.mockClear();

      await click(screen.getByRole('button', { name: /reset filters/i }));
      await settle();

      // Reset clears the name filter immediately; the settling debounce must not
      // resurrect it afterwards.
      expect(listRequests().every((url) => !url.includes('nameFilter='))).toBe(true);
      expect(input).toHaveValue('');
    });
  });

  describe('AC3 — filters stay usable while a request is in flight', () => {
    it('does not disable the filter checkboxes while loading', async () => {
      // Hold the initial request open so the page is genuinely in a loading state.
      let resolveInitial = () => {};
      mockAxiosGet.mockImplementationOnce(
        () => new Promise((resolve) => { resolveInitial = resolve; }),
      );

      renderBrowsePage();
      await act(async () => { await Promise.resolve(); });

      expect(screen.getByTestId('mock-repository-list')).toHaveAttribute('data-loading', 'true');

      const checkboxes = screen.getAllByRole('checkbox');
      expect(checkboxes.length).toBeGreaterThan(0);
      checkboxes.forEach((checkbox) => {
        expect(checkbox).toBeEnabled();
      });

      await act(async () => {
        resolveInitial(page(['maven-central']));
        await Promise.resolve();
      });
      await settle();
    });

    it('applies a checkbox selected mid-flight on top of the pending name filter', async () => {
      renderBrowsePage();
      await settle();
      mockAxiosGet.mockClear();

      // Start typing a name filter, then select a format before the name filter has settled.
      const input = screen.getByPlaceholderText(/filter/i);
      await type(input, 'mav');

      const checkbox = screen.getAllByRole('checkbox')[0];
      expect(checkbox).toBeEnabled();
      await click(checkbox);
      expect(checkbox).toBeChecked();

      await settle();

      // The combined state is applied, and the settled name filter does not fire a
      // second, redundant request afterwards.
      const requests = listRequests();
      expect(requests).toHaveLength(1);
      expect(requests[0]).toContain('nameFilter=mav');
      expect(requests[0]).toMatch(/formats=[^&]+/);
    });
  });
});
