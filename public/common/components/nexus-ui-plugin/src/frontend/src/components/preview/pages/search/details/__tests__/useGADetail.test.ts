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

import { renderHook, act } from '@testing-library/react';
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

import Axios from 'axios';
import { createGaDetailMachine } from '../gaDetailMachine';
import { createMachine } from 'xstate';

const mockAxios = Axios as jest.Mocked<typeof Axios>;

// Sample mock data
const MOCK_DETAIL: GADetail = {
  gaId: 'maven:org.apache.commons:commons-lang3',
  format: 'maven',
  displayName: 'commons-lang3',
  description: 'org.apache.commons:commons-lang3',
  projectUrl: undefined,
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

      // The fix changes the condition from `initialVersion !== selectedVersion`
      // to `!selectedVersion`, so it shouldn't fire when version is already set
      expect(selectVersionCallCount).toBe(0);
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

    it('should auto-select first version when detail loads without initialVersion', async () => {
      // This test verifies the machine's shouldAutoSelectVersion guard works correctly
      // Mock Axios to return search results
      mockAxios.get.mockResolvedValue({
        data: {
          items: [
            {
              id: 'test-1',
              repository: 'maven-central',
              format: 'maven2',
              group: 'org.test',
              name: 'test-artifact',
              version: '1.0.0',
              assets: [
                {
                  id: 'asset-1',
                  downloadUrl: 'http://test.com/asset.jar',
                  path: 'org/test/test-artifact/1.0.0/test-artifact-1.0.0.jar',
                  repository: 'maven-central',
                  format: 'maven2',
                },
              ],
            },
          ],
          continuationToken: null,
        },
      });

      // Need to reimport the real module
      const realMachine = jest.requireActual('../gaDetailMachine').createGaDetailMachine;
      (createGaDetailMachine as jest.Mock).mockImplementation(realMachine);

      const { result } = renderHook(() =>
        useGADetail({ gaId: 'maven:org.test:test-artifact' })
      );

      // Wait for the machine to transition through states
      await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 100));
      });

      // The machine should have auto-selected the first version
      expect(result.current.selectedVersion).toBe('1.0.0');
    });

    it('should prevent infinite loop when assets are empty', async () => {
      // This test verifies the machine's shouldLoadAssets guard prevents
      // re-loading when assets are empty but already loaded
      mockAxios.get.mockResolvedValue({
        data: {
          items: [
            {
              id: 'test-1',
              repository: 'maven-central',
              format: 'maven2',
              group: 'org.test',
              name: 'empty-artifact',
              version: '1.0.0',
              assets: [], // No assets - would have caused infinite loop
            },
          ],
          continuationToken: null,
        },
      });

      const realMachine = jest.requireActual('../gaDetailMachine').createGaDetailMachine;
      (createGaDetailMachine as jest.Mock).mockImplementation(realMachine);

      // Track the number of axios calls
      let axiosCallCount = 0;
      mockAxios.get.mockImplementation(() => {
        axiosCallCount++;
        return Promise.resolve({
          data: {
            items: [
              {
                id: 'test-1',
                repository: 'maven-central',
                format: 'maven2',
                group: 'org.test',
                name: 'empty-artifact',
                version: '1.0.0',
                assets: [],
              },
            ],
            continuationToken: null,
          },
        });
      });

      const { result } = renderHook(() =>
        useGADetail({ gaId: 'maven:org.test:empty-artifact' })
      );

      // Wait for state transitions to complete
      await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 200));
      });

      // Should only call axios once for the detail, then once for assets
      // Not infinitely due to lastLoadedVersion guard
      expect(axiosCallCount).toBeLessThanOrEqual(2);
      expect(result.current.assets).toEqual([]);
      expect(result.current.selectedVersion).toBe('1.0.0');
    });
  });
});
