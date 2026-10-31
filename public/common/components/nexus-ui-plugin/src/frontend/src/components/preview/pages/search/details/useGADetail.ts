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
 * Owns the selected version and that version's assets, via gaDetailMachine.
 *
 * There is no longer any component-wide fetch here. The page shell is derived synchronously from
 * the gaId, and everything else has a bounded, per-version source: assets from
 * fetchComponentVersionDetail (NEXUS-54201), versions from componentVersionsMachine (NEXUS-54219),
 * repositories from useGARepositoriesForVersion (NEXUS-54220), security from IQ. The
 * every-page-of-/v1/search walk that used to back `detail.repositories` and `detail.versions` had
 * its last reader removed when those three landed, and is gone with it — so no request this hook
 * makes scales with a component's version count.
 */

import { useRef, useEffect, useCallback, useMemo } from 'react';
import { useMachine } from '@xstate/react';

import type { GADetail, GAAsset, GADetailTab } from '../core';
import type { ComponentVersionDetail } from '../core/search.types';
import { createGaDetailMachine } from './gaDetailMachine';
import { isMockMode } from '../../../config/featureFlags';
import { getMockAssets } from './mockData';
import { parseGaCoordinates } from './detailHelpers';
import { fetchComponentVersionDetail } from '../core/componentVersionDetailApi';

interface UseGADetailOptions {
  gaId: string;
  initialTab?: GADetailTab;
  /**
   * The version from the URL, or null/undefined when it carries none.
   *
   * Nullable deliberately: the route squashes `version` with `value: null`, so a bare URL resolves
   * it to null rather than undefined, and callers pass that straight through. Treat the two the
   * same — see the sync effect below.
   */
  initialVersion?: string | null;
}

interface UseGADetailReturn {
  /**
   * GA detail shell — gaId, format, displayName, description. Non-null from the first render and
   * never refetched, because every field is derived from the gaId.
   *
   * `repositories` and `versions` are always empty. They are kept on the type for the other format
   * detail pages that still populate them; on this page the Repositories and Versions tabs have
   * their own per-version sources and do not read these.
   */
  detail: GADetail | null;
  /** Currently selected version (for files/security tabs) */
  selectedVersion: string | null;
  /** Assets for the selected version */
  assets: readonly GAAsset[];
  /** Repositories holding the selected version. Empty until its detail resolves. */
  versionRepositories: readonly string[];
  /** The selected version's most recent asset timestamp, or null if none carries one. */
  versionLastUpdated: string | null;
  /** Assets loading state */
  assetsLoading: boolean;
  /** Select a version (triggers asset load for files/security) */
  selectVersion: (version: string) => void;
}

const EMPTY_VERSION_DETAIL: ComponentVersionDetail = {
  assets: [],
  repositories: [],
  lastUpdated: null,
};

/** Wraps a mock asset list in the ComponentVersionDetail shape the machine's setAssets expects. */
function mockVersionDetail(assets: readonly GAAsset[]): ComponentVersionDetail {
  return { assets, repositories: [], lastUpdated: null };
}

/**
 * The page shell, entirely from the gaId — no network.
 *
 * `repositories` and `versions` are deliberately empty: they used to be aggregated client-side by
 * walking every page of /v1/search, which cost one request per 50 component/repository rows and
 * was the reason time-to-first-render scaled with version count.
 */
function buildShellDetail(gaId: string): GADetail {
  const { format, group, name } = parseGaCoordinates(gaId);
  return {
    gaId,
    format: format as any,
    displayName: name,
    description: group ? `${group}:${name}` : name,
    license: undefined,
    repositories: [],
    versions: [],
  };
}

/**
 * useGADetail - React hook for the component detail page's version and asset state.
 *
 * Issues exactly one request, and only once a version is known: the selected version's assets
 * through fetchComponentVersionDetail, bounded by repository count rather than version count
 * (NEXUS-54201). The selected version comes from `initialVersion`, which the effect below keeps in
 * step with the URL for the life of the hook.
 */
export function useGADetail({ gaId, initialVersion }: UseGADetailOptions): UseGADetailReturn {
  // Refs for dynamic values the services need (stable across renders)
  const gaIdRef = useRef(gaId);
  gaIdRef.current = gaId;

  /**
   * Created once, and deliberately not keyed on gaId or initialVersion.
   *
   * `useMachine` captures the machine with useConstant and only warns when a later render
   * passes a different one (@xstate/react/lib/useInterpret.js), so listing deps here would
   * change nothing except to emit that warning on every version switch. Neither value needs
   * to recreate it: `gaId` is a non-dynamic route param, so a change re-enters the state and
   * remounts this hook with a fresh machine, and `initialVersion` reaches the running machine
   * through the SELECT_VERSION effect below.
   */
  // biome-ignore lint/correctness/useExhaustiveDependencies: create-once by design; see above.
  const machine = useMemo(
    () => createGaDetailMachine(gaId, initialVersion, buildShellDetail(gaId)),
    [],
  );

  // Wire the machine with service overrides
  const [machineState, send] = useMachine(machine, {
    services: {
      loadAssets: async (ctx): Promise<ComponentVersionDetail> => {
        const version = ctx.selectedVersion;
        // `=== null` not falsy: '' is a valid version for versionless formats (raw).
        if (version === null) {
          return EMPTY_VERSION_DETAIL;
        }

        // When mock mode: always use mock assets so Files tab has content
        if (isMockMode()) {
          return mockVersionDetail(getMockAssets(gaIdRef.current, version) as readonly GAAsset[]);
        }

        const { format, name, group } = parseGaCoordinates(gaIdRef.current);
        return fetchComponentVersionDetail({
          format,
          group: group || undefined,
          name,
          version,
        });
      },
    },
  });

  const { selectedVersion } = machineState.context;

  /**
   * The URL is the sole source of truth for the selected version, so a change to it has to reach
   * the machine. Nothing else carries it: `version` is a `dynamic` route param (previewBrowseRoutes),
   * so a change no longer re-enters the state and this hook is never remounted, and the machine
   * captured `initialVersion` only at creation.
   *
   * Compared against `selectedVersion` rather than guarded on truthiness — that is what makes it
   * both idempotent and safe for '', the valid selected version of a versionless format. Without
   * it, Back after a version switch leaves the header and the Files and Security tabs showing the
   * version the user just navigated away from.
   */
  /**
   * The URL value this effect has already adopted. Seeded with the value the machine was created
   * from, so mount adopts nothing — the machine already holds it.
   */
  const adoptedUrlVersionRef = useRef<string | null | undefined>(initialVersion);

  useEffect(() => {
    // `== null`, covering null as well as undefined. The route declares
    // `version: { value: null, squash: true }`, so a URL with no version resolves the param to
    // *null*, not undefined. Testing only for undefined made this effect push that null into the
    // machine, undoing the SELECT_VERSION('') that GADetailPage sends for a versionless format —
    // which sent the two effects into an infinite ping-pong and killed the page with "Maximum
    // update depth exceeded". Either way the meaning is the same: the URL carries no version, so
    // there is nothing here to sync and the in-context resolution owns it.
    if (initialVersion == null) return;

    /*
     * Adopt only when the URL itself changed — never merely because the machine did.
     *
     * `handleVersionSelect` writes the new version to the machine synchronously and to the URL
     * asynchronously (`stateService.go`). Comparing `initialVersion` against `selectedVersion`
     * meant that during the window between those two writes this effect saw a URL value that was
     * simply lagging, treated it as authoritative, and pushed the *previous* version back into the
     * machine. Every version click cost three /search/repositories requests — new, old, new — and
     * flashed the old version's data (NEXUS-54201, found in T9).
     *
     * Keying on a change in the URL value distinguishes the two directions the old comparison
     * conflated: a genuine URL-originated change (deep link, Back/Forward, canonicalisation) still
     * reaches the machine, while the machine's own updates no longer bounce off a stale URL.
     */
    if (initialVersion === adoptedUrlVersionRef.current) return;
    adoptedUrlVersionRef.current = initialVersion;

    if (initialVersion === selectedVersion) return;
    send({ type: 'SELECT_VERSION', version: initialVersion });
  }, [initialVersion, selectedVersion, send]);

  // ---------------------------------------------------------------------------
  // Derive return values from machine state + context
  // ---------------------------------------------------------------------------

  const selectVersion = useCallback(
    (version: string) => {
      send({ type: 'SELECT_VERSION', version });
    },
    [send],
  );

  return {
    detail: machineState.context.detail,
    selectedVersion,
    assets: machineState.context.assets,
    versionRepositories: machineState.context.versionRepositories,
    versionLastUpdated: machineState.context.versionLastUpdated,
    assetsLoading: machineState.context.assetsLoading,
    selectVersion,
  };
}

export default useGADetail;
