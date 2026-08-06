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
 * GA Detail Hook (XState-backed)
 *
 * State management via gaDetailMachine. Replaces useState + useCallback + useEffect
 * with a deterministic XState machine that handles detail loading, version selection,
 * and asset loading.
 *
 * The public interface (UseGADetailReturn) is unchanged — callers don't need changes.
 */

import { useRef, useEffect, useCallback, useMemo } from 'react';
import { useMachine } from '@xstate/react';
import Axios from 'axios';

import type { GADetail, GAAsset, GAVersion, GARepository, GADetailTab, VersionStatus } from '../core';
import { createGaDetailMachine } from './gaDetailMachine';
import { isMockMode } from '../../../config/featureFlags';
import { getMockDetail, getMockAssets } from './mockData';

interface UseGADetailOptions {
  gaId: string;
  initialTab?: GADetailTab;
  initialVersion?: string;
}

interface UseGADetailReturn {
  /** GA detail data */
  detail: GADetail | null;
  /** Currently selected version (for files/security tabs) */
  selectedVersion: string | null;
  /** Assets for the selected version */
  assets: readonly GAAsset[];
  /** Loading state */
  loading: boolean;
  /** Assets loading state */
  assetsLoading: boolean;
  /** Error message */
  error: string | null;
  /** Select a version (triggers asset load for files/security) */
  selectVersion: (version: string) => void;
  /** Refresh detail data */
  refresh: () => void;
}

// =============================================================================
// API Types
// =============================================================================

interface SearchItem {
  id: string;
  repository: string;
  format: string;
  group: string | null;
  name: string;
  version: string;
  assets: Array<{
    downloadUrl: string;
    path: string;
    id: string;
    repository: string;
    format: string;
    checksum?: Record<string, string>;
    contentType?: string;
    lastModified?: string;
    fileSize?: number;
  }>;
  tags?: string[];
}

interface SearchResponse {
  items: SearchItem[];
  continuationToken: string | null;
}

// =============================================================================
// Helper: Parse gaId
// =============================================================================

function parseGaId(gaId: string): { format: string; group: string; name: string } {
  const parts = gaId.split(':');
  if (parts.length >= 3) {
    return { format: parts[0], group: parts[1], name: parts[2] };
  }
  if (parts.length === 2) {
    return { format: parts[0], group: '', name: parts[1] };
  }
  return { format: '', group: '', name: gaId };
}

// =============================================================================
// Helper: Build GADetail from search results
// =============================================================================

function buildDetail(gaId: string, items: SearchItem[]): GADetail {
  const { format, group, name } = parseGaId(gaId);

  // Aggregate versions: group by version string
  const versionMap = new Map<string, { repos: Set<string>; lastUpdated: string }>();
  const repoMap = new Map<string, { format: string; type: string; versions: Set<string> }>();

  for (const item of items) {
    // Version aggregation
    const existing = versionMap.get(item.version);
    if (existing) {
      existing.repos.add(item.repository);
    } else {
      versionMap.set(item.version, {
        repos: new Set([item.repository]),
        lastUpdated: item.assets?.[0]?.lastModified || new Date().toISOString(),
      });
    }

    // Repository aggregation
    const repoEntry = repoMap.get(item.repository);
    if (repoEntry) {
      repoEntry.versions.add(item.version);
    } else {
      repoMap.set(item.repository, {
        format: item.format,
        type: 'proxy', // Default; API doesn't return repo type
        versions: new Set([item.version]),
      });
    }
  }

  // Sort versions by semver-like ordering (newest first)
  const versions: GAVersion[] = Array.from(versionMap.entries())
    .sort((a, b) => compareVersions(b[0], a[0]))
    .map(([ver, data]) => ({
      version: ver,
      lastUpdated: data.lastUpdated,
      repositories: Array.from(data.repos),
      status: 'none' as VersionStatus,
      statusReason: undefined,
    }));

  // Build repositories list
  const repositories: GARepository[] = Array.from(repoMap.entries()).map(
    ([repoName, data]) => ({
      name: repoName,
      format: data.format,
      type: data.type as 'hosted' | 'proxy' | 'group',
      versionsCount: data.versions.size,
    }),
  );

  return {
    gaId,
    format: format as any,
    displayName: name,
    description: group ? `${group}:${name}` : name,
    projectUrl: undefined,
    license: undefined,
    repositories,
    versions,
  };
}

/**
 * Simple version comparison (handles semver-like strings).
 * Returns negative if a < b, positive if a > b, 0 if equal.
 */
function compareVersions(a: string, b: string): number {
  const aParts = a.split('.').map((p) => parseInt(p, 10) || 0);
  const bParts = b.split('.').map((p) => parseInt(p, 10) || 0);
  const maxLen = Math.max(aParts.length, bParts.length);
  for (let i = 0; i < maxLen; i++) {
    const diff = (aParts[i] || 0) - (bParts[i] || 0);
    if (diff !== 0) return diff;
  }
  return 0;
}

// =============================================================================
// Hook
// =============================================================================

/**
 * useGADetail - React hook for GA detail state management (XState-backed).
 *
 * Fetches real data from the Nexus search API:
 * - GET /service/rest/v1/search?name={name}&format={format}
 * - Aggregates versions and repositories from search results
 * - Loads assets for selected version from cached search item data
 *
 * The machine auto-starts in data.loading, invoking loadDetail immediately.
 * When gaId changes, we send a LOAD event to re-fetch.
 */
export function useGADetail({ gaId, initialVersion }: UseGADetailOptions): UseGADetailReturn {
  // Refs for dynamic values the services need (stable across renders)
  const gaIdRef = useRef(gaId);
  gaIdRef.current = gaId;

  // Cache all search items for asset derivation (not part of machine context)
  const allItemsRef = useRef<SearchItem[]>([]);

  // Track whether this is the first render (machine auto-loads on mount)
  const isFirstRenderRef = useRef(true);

  // Create machine once with initial gaId and version
  // Empty deps is intentional: machine identity must be stable across renders
  const machine = useMemo(
    () => createGaDetailMachine(gaId, initialVersion),
    [gaId, initialVersion], 
  );

  // Wire the machine with service overrides
  const [machineState, send] = useMachine(machine, {
    services: {
      loadDetail: async () => {
        const currentGaId = gaIdRef.current;
        if (!currentGaId) {
          return Promise.reject(new Error('No GA ID provided'));
        }

        const { format, name, group } = parseGaId(currentGaId);

        // Build search URL with available params
        // Use q as primary search term (matches unified search behavior)
        const params = new URLSearchParams();
        const qTerm = group ? `${group}:${name}` : name;
        if (qTerm) params.set('q', qTerm);
        if (name) params.set('name', name);
        if (format) params.set('format', format);
        if (group) params.set('group', group);

        const fetchAllItems = async (): Promise<SearchItem[]> => {
          let items: SearchItem[] = [];
          let continuationToken: string | null = null;

          do {
            const url = continuationToken
              ? `/service/rest/v1/search?${params.toString()}&continuationToken=${continuationToken}`
              : `/service/rest/v1/search?${params.toString()}`;

            const response = await Axios.get<SearchResponse>(url);
            items = items.concat(response.data.items || []);
            continuationToken = response.data.continuationToken || null;
          } while (continuationToken);

          return items;
        };

        // When mock mode: try real API first; fall back to mock only if instance is empty
        if (isMockMode()) {
          try {
            const items = await fetchAllItems();
            if (items.length > 0) {
              allItemsRef.current = items;
              return buildDetail(currentGaId, items);
            }
          } catch {
            // API error (e.g. 404, network) - fall through to mock
          }
          allItemsRef.current = [];
          return getMockDetail(currentGaId);
        }

        const items = await fetchAllItems();
        allItemsRef.current = items;
        return buildDetail(currentGaId, items);
      },

      loadAssets: (ctx) => {
        const version = ctx.selectedVersion;
        if (!version) {
          return Promise.resolve([] as GAAsset[]);
        }

        // When mock mode: always use mock assets so Files tab has content
        if (isMockMode()) {
          return Promise.resolve(getMockAssets(gaIdRef.current, version) as readonly GAAsset[]);
        }

        const versionItems = allItemsRef.current.filter((item) => item.version === version);
        const assetList: GAAsset[] = [];

        for (const item of versionItems) {
          for (const asset of item.assets || []) {
            const ext = (asset.path.split('.').pop() || 'jar').toLowerCase();
            assetList.push({
              id: asset.id,
              repository: asset.repository || item.repository,
              path: asset.path,
              downloadUrl: asset.downloadUrl,
              format: asset.contentType || asset.format || 'application/octet-stream',
              extension: ext,
              classifier: undefined,
              size: asset.fileSize ?? 0,
              contentType: asset.contentType || 'application/octet-stream',
              lastModified: asset.lastModified || new Date().toISOString(),
              checksums: asset.checksum || {},
            } as GAAsset);
          }
        }

        return Promise.resolve(assetList as readonly GAAsset[]);
      },
    },
  });

  // When gaId changes (after initial mount), reload detail data
  useEffect(() => {
    if (isFirstRenderRef.current) {
      isFirstRenderRef.current = false;
      return; // Machine auto-loads on mount — skip first render
    }
    allItemsRef.current = [];
    send({ type: 'LOAD' });
  }, [send]);

  // Sync initialVersion from URL to machine when it changes
  // Uses a ref guard to prevent re-sending for the same version
  const lastSyncedVersionRef = useRef<string | undefined>(initialVersion);
  useEffect(() => {
    // Only sync if version changed and machine doesn't have it selected yet
    if (
      initialVersion &&
      initialVersion !== lastSyncedVersionRef.current &&
      !machineState.context.selectedVersion
    ) {
      lastSyncedVersionRef.current = initialVersion;
      send({ type: 'SELECT_VERSION', version: initialVersion });
    }
  }, [initialVersion, machineState.context.selectedVersion, send]);

  // ---------------------------------------------------------------------------
  // Derive return values from machine state + context
  // ---------------------------------------------------------------------------

  const selectVersion = useCallback(
    (version: string) => {
      send({ type: 'SELECT_VERSION', version });
    },
    [send],
  );

  const refresh = useCallback(() => {
    allItemsRef.current = [];
    send({ type: 'LOAD' });
  }, [send]);

  return {
    detail: machineState.context.detail,
    selectedVersion: machineState.context.selectedVersion,
    assets: machineState.context.assets,
    loading: machineState.context.loading,
    assetsLoading: machineState.context.assetsLoading,
    error: machineState.context.error,
    selectVersion,
    refresh,
  };
}

export default useGADetail;
