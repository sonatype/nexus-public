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

import { renderHook, act, waitFor } from '@testing-library/react';
import { useGADetail } from '../useGADetail';
import type { GADetail, GAAsset } from '../../core/search.types';

// =============================================================================
// MOCKS
// =============================================================================

// Mock Axios for API calls
jest.mock('axios', () => ({
  get: jest.fn(),
}));

// Mock featureFlags
jest.mock('../../../../config/featureFlags', () => ({
  isMockMode: () => false,
}));

// Mock the machine to avoid async timing issues
jest.mock('../gaDetailMachine', () => ({
  createGaDetailMachine: jest.fn(),
}));

// Mock the version-scoped detail API (NEXUS-54201)
jest.mock('../../core/componentVersionDetailApi', () => ({
  fetchComponentVersionDetail: jest.fn(),
  MAX_DETAIL_PAGES: 5,
}));

import Axios from 'axios';
import { createGaDetailMachine } from '../gaDetailMachine';
import { fetchComponentVersionDetail } from '../../core/componentVersionDetailApi';
import { createMachine } from 'xstate';

const mockAxios = Axios as jest.Mocked<typeof Axios>;
const mockFetchDetail = fetchComponentVersionDetail as jest.MockedFunction<
  typeof fetchComponentVersionDetail
>;

// Sample mock data
const MOCK_DETAIL: GADetail = {
  gaId: 'maven:org.apache.commons:commons-lang3',
  format: 'maven',
  displayName: 'commons-lang3',
  description: 'org.apache.commons:commons-lang3',
  license: 'Apache-2.0',
  repositories: [
    { name: 'maven-central', format: 'maven2', type: 'proxy', versionsCount: 3 },
  ],
  versions: [
    { version: '3.14.0', lastUpdated: '2024-03-01T00:00:00Z', repositories: ['maven-central'], status: 'none' },
    { version: '3.13.0', lastUpdated: '2023-11-01T00:00:00Z', repositories: ['maven-central'], status: 'none' },
  ],
};

const MOCK_ASSETS: readonly GAAsset[] = [
  {
    id: 'asset-1',
    repository: 'maven-central',
    path: '/org/apache/commons/commons-lang3/3.14.0/commons-lang3-3.14.0.jar',
    downloadUrl: 'http://localhost:8081/repository/maven-central/commons-lang3.jar',
    format: 'application/java-archive',
    classifier: undefined,
    extension: 'jar',
    size: 654321,
    contentType: 'application/java-archive',
    lastModified: '2024-03-01T00:00:00Z',
    checksums: { sha1: 'abc123', md5: 'def456' },
  },
];

// =============================================================================
// TESTS
// =============================================================================

describe('useGADetail', () => {
  // ===========================================================================
  // INFINITE LOOP PREVENTION TESTS
  // ===========================================================================

  describe('infinite loop prevention (NEXUS-52207)', () => {
    it('should not cause infinite re-renders when assets are empty', async () => {
      // This test verifies the machine-based fix for the infinite loop bug.
      // The machine now uses `lastLoadedVersion` in context to prevent repeated
      // asset loading when assets are empty (e.g., version has no files).

      // Create a machine that returns empty assets (triggering the loop condition)
      let selectVersionCallCount = 0;

      const mockMachine = createMachine({
        id: 'test-ga-detail',
        initial: 'loaded',
        context: {
          gaId: 'maven:test:component',
          detail: MOCK_DETAIL,
          selectedVersion: '3.14.0',
          assets: [] as readonly GAAsset[], // Empty assets - the loop trigger
          loading: false,
          assetsLoading: false,
          error: null,
          lastLoadedVersion: '3.14.0', // Already loaded - prevents re-trigger
        },
        states: {
          loaded: {
            on: {
              SELECT_VERSION: {
                actions: () => {
                  selectVersionCallCount++;
                },
              },
            },
          },
        },
      });

      (createGaDetailMachine as jest.Mock).mockReturnValue(mockMachine);

      const { rerender } = renderHook(() =>
        useGADetail({ gaId: 'maven:test:component' })
      );

      // Force multiple re-renders to simulate the loop condition
      await act(async () => {
        rerender();
        rerender();
        rerender();
      });

      // With the machine-based fix, SELECT_VERSION should never be called
      // because the machine's shouldLoadAssets guard checks lastLoadedVersion
      expect(selectVersionCallCount).toBe(0);
    });

    it('should not re-trigger version sync when selectedVersion already matches', async () => {
      let selectVersionCallCount = 0;

      const mockMachine = createMachine({
        id: 'test-ga-detail',
        initial: 'loaded',
        context: {
          gaId: 'maven:test:component',
          detail: MOCK_DETAIL,
          selectedVersion: '3.14.0', // Already selected
          assets: MOCK_ASSETS,
          loading: false,
          assetsLoading: false,
          error: null,
          lastLoadedVersion: '3.14.0',
        },
        states: {
          loaded: {
            on: {
              SELECT_VERSION: {
                actions: () => {
                  selectVersionCallCount++;
                },
              },
            },
          },
        },
      });

      (createGaDetailMachine as jest.Mock).mockReturnValue(mockMachine);

      const { rerender } = renderHook(() =>
        useGADetail({ gaId: 'maven:test:component', initialVersion: '3.14.0' })
      );

      await act(async () => {
        rerender();
        rerender();
      });

      // The sync compares `initialVersion === selectedVersion`, so an initialVersion that already
      // matches the machine's context is a no-op no matter how many times we re-render.
      expect(selectVersionCallCount).toBe(0);
    });

    /*
     * The regression `dynamic: true` on the `version` route param exposes.
     *
     * With the param dynamic, a version change no longer re-enters the state, so this hook is
     * never remounted — and `useMachine` captured the machine (and its initial selectedVersion)
     * with useConstant, so recreating it on an initialVersion change is ignored with a warning.
     * The effect is therefore the only path from the URL to the machine. Without it, Back after a
     * version switch leaves the header and the Files and Security tabs on the previous version.
     */
    it('sends SELECT_VERSION when the URL version changes after mount', async () => {
      const selectedVersions: string[] = [];

      const mockMachine = createMachine({
        id: 'test-ga-detail',
        initial: 'loaded',
        context: {
          gaId: 'maven:test:component',
          detail: MOCK_DETAIL,
          // Static: this stands in for a machine whose context the mount-time version set, and
          // which therefore cannot see a later URL change on its own.
          selectedVersion: '1.0.0',
          assets: MOCK_ASSETS,
          versionRepositories: [] as readonly string[],
          versionLastUpdated: null,
          loading: false,
          assetsLoading: false,
          error: null,
          lastLoadedVersion: '1.0.0',
        },
        states: {
          loaded: {
            on: {
              SELECT_VERSION: {
                actions: (_ctx, event: any) => {
                  selectedVersions.push(event.version);
                },
              },
            },
          },
        },
      });

      (createGaDetailMachine as jest.Mock).mockReturnValue(mockMachine);

      const { rerender } = renderHook(
        ({ initialVersion }: { initialVersion?: string }) =>
          useGADetail({ gaId: 'maven:test:component', initialVersion }),
        { initialProps: { initialVersion: '1.0.0' as string | undefined } }
      );

      // Mount-time version already matches the machine's context — nothing to send.
      expect(selectedVersions).toEqual([]);

      await act(async () => {
        rerender({ initialVersion: '2.0.0' });
      });

      expect(selectedVersions).toEqual(['2.0.0']);
    });

    // '' is the selected version of a versionless format (raw), and it is falsy. A truthiness
    // guard here would silently drop it and leave the Files tab permanently empty.
    it('sends SELECT_VERSION for the empty-string version of a versionless component', async () => {
      const selectedVersions: string[] = [];

      const mockMachine = createMachine({
        id: 'test-ga-detail',
        initial: 'loaded',
        context: {
          gaId: 'raw:some/path:file.txt',
          detail: MOCK_DETAIL,
          selectedVersion: null,
          assets: [] as readonly GAAsset[],
          versionRepositories: [] as readonly string[],
          versionLastUpdated: null,
          loading: false,
          assetsLoading: false,
          error: null,
          lastLoadedVersion: null,
        },
        states: {
          loaded: {
            on: {
              SELECT_VERSION: {
                actions: (_ctx, event: any) => {
                  selectedVersions.push(event.version);
                },
              },
            },
          },
        },
      });

      (createGaDetailMachine as jest.Mock).mockReturnValue(mockMachine);

      const { rerender } = renderHook(
        ({ initialVersion }: { initialVersion: string | null }) =>
          useGADetail({ gaId: 'raw:some/path:file.txt', initialVersion }),
        { initialProps: { initialVersion: null as string | null } },
      );

      /*
       * Mount adopts nothing: the real `createGaDetailMachine` seeds `selectedVersion` from
       * `initialVersion` (see its context initializer), and an `always` transition starts the load
       * from there, so a mount-time SELECT_VERSION would be redundant. The sync effect's job is
       * narrower and more specific — adopt URL *changes*. Asserting a mount send here is what let
       * the effect fire inside the window where the URL lags a just-selected version, pushing the
       * previous version back into the machine (three requests per version click, NEXUS-54201).
       *
       * The versionless coverage this test exists for now lives in
       * versionlessComponentDetail.test.tsx, against the real machine, where it asserts the load
       * itself rather than the event.
       */
      expect(selectedVersions).toEqual([]);

      // The original protection, at the point it actually applies: a URL that *changes* to '' must
      // be adopted. '' is falsy, so any truthiness guard reintroduced here would drop it and leave
      // a versionless component on the previous version.
      rerender({ initialVersion: '' });

      expect(selectedVersions).toEqual(['']);
    });

    it('should reset lastLoadedVersion when gaId changes', async () => {
      // When gaId changes, the machine resets lastLoadedVersion to null,
      // allowing asset loading for the new GA.
      let selectVersionCallCount = 0;

      const createMockMachine = (gaId: string) =>
        createMachine({
          id: `test-ga-detail-${gaId}`,
          initial: 'loaded',
          context: {
            gaId,
            detail: MOCK_DETAIL,
            selectedVersion: null,
            assets: [] as readonly GAAsset[],
            loading: false,
            assetsLoading: false,
            error: null,
            lastLoadedVersion: null,
          },
          states: {
            loaded: {
              on: {
                SELECT_VERSION: {
                  actions: () => {
                    selectVersionCallCount++;
                  },
                },
              },
            },
          },
        });

      (createGaDetailMachine as jest.Mock).mockImplementation((gaId: string) =>
        createMockMachine(gaId)
      );

      const { rerender } = renderHook(
        ({ gaId }: { gaId: string }) => useGADetail({ gaId }),
        { initialProps: { gaId: 'maven:group:artifact1' } }
      );

      await act(async () => {
        rerender({ gaId: 'maven:group:artifact1' });
      });

      const firstCallCount = selectVersionCallCount;

      // Change gaId - machine resets lastLoadedVersion via LOAD event
      await act(async () => {
        rerender({ gaId: 'maven:group:artifact2' });
        rerender({ gaId: 'maven:group:artifact2' });
      });

      // After gaId change, should have exactly one additional call per gaId change
      // (not unlimited due to lastLoadedVersion guard)
      expect(selectVersionCallCount).toBeLessThanOrEqual(firstCallCount + 1);
    });
  });

  // ===========================================================================
  // PROPERTY NAME FIX TESTS
  // ===========================================================================

  describe('GAAsset property names', () => {
    it('should use checksums (plural) property name matching GAAsset type', () => {
      // This verifies the fix for the checksum vs checksums property mismatch
      // The GAAsset type defines `checksums` (plural) but the code was using `checksum` (singular)

      const asset: GAAsset = {
        id: 'test-asset',
        repository: 'test-repo',
        path: '/test/path.jar',
        downloadUrl: 'http://test.com/path.jar',
        format: 'application/java-archive',
        extension: 'jar',
        size: 1000,
        contentType: 'application/java-archive',
        lastModified: '2024-01-01T00:00:00Z',
        checksums: {
          sha1: 'abc123',
          md5: 'def456',
        },
      };

      // TypeScript will catch if we try to access asset.checksum (doesn't exist)
      expect(asset.checksums).toBeDefined();
      expect(asset.checksums.sha1).toBe('abc123');
      expect(asset.checksums.md5).toBe('def456');
    });
  });

  // ===========================================================================
  // LOADING STATE TESTS
  // ===========================================================================

  describe('loading state guards', () => {
    it('should not trigger effects during loading state', async () => {
      let selectVersionCallCount = 0;

      const mockMachine = createMachine({
        id: 'test-ga-detail',
        initial: 'loading',
        context: {
          gaId: 'maven:test:component',
          detail: null,
          selectedVersion: null,
          assets: [] as readonly GAAsset[],
          loading: true, // Still loading
          assetsLoading: false,
          error: null,
          lastLoadedVersion: null,
        },
        states: {
          loading: {
            on: {
              SELECT_VERSION: {
                actions: () => {
                  selectVersionCallCount++;
                },
              },
            },
          },
        },
      });

      (createGaDetailMachine as jest.Mock).mockReturnValue(mockMachine);

      const { rerender } = renderHook(() =>
        useGADetail({ gaId: 'maven:test:component' })
      );

      await act(async () => {
        rerender();
        rerender();
      });

      // Machine guards prevent re-triggering during loading state
      expect(selectVersionCallCount).toBe(0);
    });

    it('should not trigger effects during assetsLoading state', async () => {
      let selectVersionCallCount = 0;

      const mockMachine = createMachine({
        id: 'test-ga-detail',
        initial: 'loadingAssets',
        context: {
          gaId: 'maven:test:component',
          detail: MOCK_DETAIL,
          selectedVersion: '3.14.0',
          assets: [] as readonly GAAsset[],
          loading: false,
          assetsLoading: true, // Assets are loading
          error: null,
          lastLoadedVersion: null,
        },
        states: {
          loadingAssets: {
            on: {
              SELECT_VERSION: {
                actions: () => {
                  selectVersionCallCount++;
                },
              },
            },
          },
        },
      });

      (createGaDetailMachine as jest.Mock).mockReturnValue(mockMachine);

      const { rerender } = renderHook(() =>
        useGADetail({ gaId: 'maven:test:component' })
      );

      await act(async () => {
        rerender();
        rerender();
      });

      // Machine guards prevent re-triggering during assetsLoading state
      expect(selectVersionCallCount).toBe(0);
    });
  });

  // ===========================================================================
  // REAL MACHINE TESTS (not mocked)
  // ===========================================================================

  describe('real machine integration', () => {
    // These tests use the actual gaDetailMachine to verify the guards work correctly
    // without mocking. This catches bugs that could be introduced by changing the
    // real machine while tests mock it.

    beforeEach(() => {
      // Restore the real machine for these tests
      jest.dontMock('../gaDetailMachine');
    });

    afterEach(() => {
      // Re-mock for other test suites
      jest.mock('../gaDetailMachine', () => ({
        createGaDetailMachine: jest.fn(),
      }));
    });

    /*
     * The complaint this guards: opening a component detail page fired one /v1/search request per
     * 50 component/repository rows — ~101 on the 5,005-version depth fixture — before anything
     * rendered, because loadDetail walked every page on mount.
     *
     * It was first made lazy behind a needsAggregates flag, then deleted outright once both of its
     * readers had bounded per-version sources (NEXUS-54201 for Files, NEXUS-54220 for
     * Repositories). So the assertion is now unconditional: mounting the hook without a version
     * issues no request at all, and no prop can make it issue one.
     */
    it('issues no request at all when mounted without a version', async () => {
      const realMachine = jest.requireActual('../gaDetailMachine').createGaDetailMachine;
      (createGaDetailMachine as jest.Mock).mockImplementation(realMachine);

      let axiosCallCount = 0;
      mockAxios.get.mockImplementation(() => {
        axiosCallCount++;
        return Promise.resolve({
          data: { items: [], continuationToken: null },
        });
      });

      const { result, rerender } = renderHook(() =>
        useGADetail({ gaId: 'maven:org.test:test-artifact' }),
      );

      // Fixed delay, not waitFor: this asserts an absence, and waitFor cannot prove a
      // negative — it would pass on the first tick and never see a request arriving late.
      await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 100));
      });

      expect(axiosCallCount).toBe(0);
      expect(mockFetchDetail).not.toHaveBeenCalled();
      // The shell is still fully usable: these come from the gaId, not the network.
      expect(result.current.detail?.displayName).toBe('test-artifact');
      expect(result.current.detail?.repositories).toEqual([]);
      expect(result.current.detail?.versions).toEqual([]);

      // Re-rendering cannot conjure a fetch either — only a selected version can.
      await act(async () => {
        rerender();
      });
      expect(axiosCallCount).toBe(0);
      expect(mockFetchDetail).not.toHaveBeenCalled();
    });

    it('should prevent infinite loop when assets are empty', async () => {
      // The machine's shouldLoadAssets guard must not re-invoke once assets have resolved empty
      // for a version — an empty result is a legitimate answer, not a reason to retry. The
      // version is supplied via initialVersion, matching how GADetailPage sources it (from the
      // URL, resolved from the versions machine's first page); the machine never invents one.
      mockFetchDetail.mockResolvedValue({ assets: [], repositories: [], lastUpdated: null });

      const realMachine = jest.requireActual('../gaDetailMachine').createGaDetailMachine;
      (createGaDetailMachine as jest.Mock).mockImplementation(realMachine);

      // No axios call is expected at all now: the drain that used it is gone, and assets go
      // through the mocked fetchComponentVersionDetail.
      let axiosCallCount = 0;
      mockAxios.get.mockImplementation(() => {
        axiosCallCount++;
        return Promise.resolve({ data: { items: [], continuationToken: null } });
      });

      const { result } = renderHook(() =>
        useGADetail({
          gaId: 'maven:org.test:empty-artifact',
          initialVersion: '1.0.0',
        })
      );

      // Fixed delay, not waitFor: the assertions below are all "did not happen more than
      // once", which waitFor would satisfy on its first tick before any re-invoke could occur.
      await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 200));
      });

      expect(axiosCallCount).toBe(0);
      // The lastLoadedVersion guard should stop fetchComponentVersionDetail from being
      // re-invoked once assets have resolved empty for this version.
      expect(mockFetchDetail).toHaveBeenCalledTimes(1);
      expect(result.current.assets).toEqual([]);
      expect(result.current.selectedVersion).toBe('1.0.0');
    });
  });

  describe('version-scoped detail (NEXUS-54201)', () => {
    // Real machine: verifying loadAssets' real wiring to fetchComponentVersionDetail, not
    // the mocked-out createGaDetailMachine the other describe blocks use.
    beforeEach(() => {
      jest.dontMock('../gaDetailMachine');
      mockFetchDetail.mockResolvedValue({
        assets: MOCK_ASSETS,
        repositories: ['maven-hosted-1', 'maven-hosted-3'],
        lastUpdated: '2026-06-01T00:00:00Z',
      });
    });

    afterEach(() => {
      jest.mock('../gaDetailMachine', () => ({
        createGaDetailMachine: jest.fn(),
      }));
    });

    it('requests only the selected version, never the whole component', async () => {
      const realMachine = jest.requireActual('../gaDetailMachine').createGaDetailMachine;
      (createGaDetailMachine as jest.Mock).mockImplementation(realMachine);

      renderHook(() =>
        useGADetail({ gaId: 'maven:org.sonatype.test:depth-fixture-v3', initialVersion: '1.0.500' }),
      );

      await waitFor(() =>
        expect(mockFetchDetail).toHaveBeenCalledWith({
          format: 'maven',
          group: 'org.sonatype.test',
          name: 'depth-fixture-v3',
          version: '1.0.500',
        }),
      );
    });

    it('exposes the selected version repositories and timestamp', async () => {
      const realMachine = jest.requireActual('../gaDetailMachine').createGaDetailMachine;
      (createGaDetailMachine as jest.Mock).mockImplementation(realMachine);

      const { result } = renderHook(() =>
        useGADetail({ gaId: 'maven:org.sonatype.test:depth-fixture-v3', initialVersion: '1.0.500' }),
      );

      await waitFor(() =>
        expect(result.current.versionRepositories).toEqual([
          'maven-hosted-1',
          'maven-hosted-3',
        ]),
      );
      expect(result.current.versionLastUpdated).toBe('2026-06-01T00:00:00Z');
    });

    // AT-018: '' is a valid selected version, not "unresolved".
    it('fetches for a versionless component', async () => {
      const realMachine = jest.requireActual('../gaDetailMachine').createGaDetailMachine;
      (createGaDetailMachine as jest.Mock).mockImplementation(realMachine);

      renderHook(() => useGADetail({ gaId: 'raw::my-file.txt', initialVersion: '' }));

      await waitFor(() =>
        expect(mockFetchDetail).toHaveBeenCalledWith(
          expect.objectContaining({ version: '' }),
        ),
      );
    });

    it('does not fetch before a version is selected', async () => {
      const realMachine = jest.requireActual('../gaDetailMachine').createGaDetailMachine;
      (createGaDetailMachine as jest.Mock).mockImplementation(realMachine);

      renderHook(() => useGADetail({ gaId: 'maven:org.sonatype.test:depth-fixture-v3' }));

      // Fixed delay, not waitFor: this asserts an absence, and waitFor cannot prove a
      // negative — it would pass on the first tick and never see a request arriving late.
      await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 50));
      });

      expect(mockFetchDetail).not.toHaveBeenCalled();
    });

    it('keeps the shell rendered when the fetch fails', async () => {
      const realMachine = jest.requireActual('../gaDetailMachine').createGaDetailMachine;
      (createGaDetailMachine as jest.Mock).mockImplementation(realMachine);
      mockFetchDetail.mockRejectedValue(new Error('boom'));

      const { result } = renderHook(() =>
        useGADetail({ gaId: 'maven:org.sonatype.test:depth-fixture-v3', initialVersion: '1.0.500' }),
      );

      await waitFor(() => expect(result.current.assetsLoading).toBe(false));
      expect(result.current.detail).not.toBeNull();
      expect(result.current.assets).toEqual([]);
    });
  });
});
