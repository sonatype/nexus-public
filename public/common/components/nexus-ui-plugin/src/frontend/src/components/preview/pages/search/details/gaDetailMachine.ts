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
 * GA Detail Machine - XState state machine for the GA detail page.
 *
 * Models the detail page's lifecycle with parallel regions:
 * - `data`: the selected version's assets — idle -> loadingAssets -> loaded
 * - `tab`: 5 tab sub-states (overview, versions, repositories, files, security)
 *
 * Tab sub-states enable:
 * 1. Tests that verify every tab's metadata via it.each(ALL_TABS)
 * 2. Component reads active tab config from state.meta (no switch/case)
 * 3. Version-dependent tabs (files, security) declare requiresVersion in metadata
 * 4. Single source of truth for tab structure
 *
 * This follows the searchMachine parallel-region pattern adapted for detail pages.
 */

import { createMachine, assign } from 'xstate';
import type {
  GADetail,
  GAAsset,
  GADetailTab,
  ComponentVersionDetail,
} from '../core/search.types';

// =============================================================================
// CONTEXT & EVENT TYPES
// =============================================================================

export interface GaDetailMachineContext {
  /** GA identifier (format:group:name) */
  gaId: string;
  /** Loaded GA detail data */
  detail: GADetail | null;
  /** Currently selected version (for files/security tabs) */
  selectedVersion: string | null;
  /** Assets for the selected version */
  assets: readonly GAAsset[];
  /**
   * Repositories holding the selected version, from the same fetch as `assets`.
   *
   * In context rather than a ref: a ref read during render is only correct while `assets`
   * happens to change in the same commit that populates it, and an assets-identical result
   * would leave a consumer showing the previous version's repositories.
   */
  versionRepositories: readonly string[];
  /** The selected version's most recent asset timestamp, or null if none carries one. */
  versionLastUpdated: string | null;
  /** Whether version-specific assets are loading */
  assetsLoading: boolean;
  /** Last version we loaded assets for — prevents infinite loop when assets are empty */
  lastLoadedVersion: string | null;
}

export type GaDetailMachineEvent =
  | { type: 'SELECT_TAB'; tab: GADetailTab }
  | { type: 'SELECT_VERSION'; version: string };

// =============================================================================
// TAB METADATA
// =============================================================================

/**
 * Metadata for a tab sub-state.
 * Available via state.meta when the machine is in that tab.
 */
export interface TabStateMeta {
  /** Tab identifier matching GADetailTab */
  tabId: GADetailTab;
  /** Human-readable label for the tab */
  label: string;
  /** Whether this tab requires a version to be selected */
  requiresVersion: boolean;
}

/**
 * Tab definition used for building sub-states and test iteration.
 */
interface TabDefinition {
  tabId: GADetailTab;
  label: string;
  requiresVersion: boolean;
}

/**
 * All tab definitions - single source of truth for tab configuration.
 */
const TAB_DEFINITIONS: readonly TabDefinition[] = [
  { tabId: 'overview', label: 'Overview', requiresVersion: false },
  { tabId: 'versions', label: 'Versions', requiresVersion: false },
  { tabId: 'repositories', label: 'Repositories', requiresVersion: false },
  { tabId: 'files', label: 'Files', requiresVersion: true },
  { tabId: 'security', label: 'Security', requiresVersion: true },
] as const;

/**
 * All tab IDs for test iteration.
 * Exported so tests can use it.each(ALL_TABS) for exhaustive coverage.
 */
export const ALL_TABS: GADetailTab[] = TAB_DEFINITIONS.map((t) => t.tabId);

// =============================================================================
// TAB SUB-STATES (generated from TAB_DEFINITIONS)
// =============================================================================

/**
 * Build tab sub-state definitions from TAB_DEFINITIONS.
 * Each sub-state's meta declares the tab's identity, label, and requiresVersion flag.
 */
function buildTabStates(): Record<string, { meta: TabStateMeta }> {
  const states: Record<string, { meta: TabStateMeta }> = {};

  for (const def of TAB_DEFINITIONS) {
    states[def.tabId] = {
      meta: {
        tabId: def.tabId,
        label: def.label,
        requiresVersion: def.requiresVersion,
      },
    };
  }

  return states;
}

/**
 * Generate SELECT_TAB guard functions for each tab.
 * Each guard checks if event.tab matches the target tab.
 */
function buildTabGuards(): Record<
  string,
  (ctx: GaDetailMachineContext, event: GaDetailMachineEvent) => boolean
> {
  const guards: Record<
    string,
    (ctx: GaDetailMachineContext, event: GaDetailMachineEvent) => boolean
  > = {};

  for (const def of TAB_DEFINITIONS) {
    guards[`isTab_${def.tabId}`] = (_ctx, event) =>
      event.type === 'SELECT_TAB' && (event as { tab: string }).tab === def.tabId;
  }

  return guards;
}

/**
 * Generate SELECT_TAB transitions for the tab region.
 * Each transition targets a specific tab sub-state with a guard.
 */
function buildTabTransitions() {
  return TAB_DEFINITIONS.map((def) => ({
    target: `.${def.tabId}` as string,
    cond: `isTab_${def.tabId}`,
  }));
}

// Pre-build at module load time
const TAB_STATES = buildTabStates();
const TAB_GUARDS = buildTabGuards();
const TAB_TRANSITIONS = buildTabTransitions();

// =============================================================================
// GA DETAIL MACHINE FACTORY
// =============================================================================

/**
 * Create a GA detail machine configured for a specific gaId.
 *
 * Uses parallel states:
 * - `data`: the selected version's asset lifecycle
 * - `tab`: 5 tab sub-states with metadata (overview, versions, repositories, files, security)
 *
 * Services (override via interpret/withConfig):
 * - `loadAssets`: resolves a ComponentVersionDetail for the selected version — its assets, the
 *   repositories holding it, and its newest asset timestamp
 */
export function createGaDetailMachine(
  gaId: string,
  initialVersion?: string,
  shellDetail?: GADetail,
) {
  return createMachine<GaDetailMachineContext, GaDetailMachineEvent>(
    {
      id: `ga-detail-${gaId}`,
      type: 'parallel',
      context: {
        gaId,
        detail: shellDetail ?? null,
        selectedVersion: initialVersion ?? null,
        assets: [],
        versionRepositories: [],
        versionLastUpdated: null,
        assetsLoading: false,
        lastLoadedVersion: null,
      },

      states: {
        // ==================================================
        // Data Region — the selected version's assets, and nothing else.
        //
        // This used to hold a second, sibling `aggregate` region driving a walk over every page
        // of /v1/search to build detail.repositories and detail.versions. Its last two readers
        // were replaced by bounded per-version sources (NEXUS-54201 for Files, NEXUS-54220 for
        // Repositories), so the region and its NEED_AGGREGATES/LOAD/RETRY events are gone. The
        // region nesting is kept: `data` stays a parallel node so adding a second independent
        // lifecycle later does not have to re-open the question of whether it can starve this one.
        // ==================================================
        data: {
          type: 'parallel',
          states: {
            /**
             * `fetchComponentVersionDetail` is bounded by repository count, not version count, so
             * there is no cost reason to gate it behind an explicit "a tab needs this" event. A
             * version selected, whether at creation via the URL or later via SELECT_VERSION, loads
             * its assets on its own schedule. No auto-select branch: the version comes from the URL
             * (GADetailPage resolves it from the versions machine's first page) and the machine
             * never invents one.
             */
            asset: {
              initial: 'idle',
              states: {
                idle: {
                  always: [
                    {
                      cond: 'shouldLoadAssets',
                      target: 'loadingAssets',
                    },
                  ],
                },

                loadingAssets: {
                  entry: assign({ assetsLoading: true }),
                  invoke: {
                    src: 'loadAssets',
                    onDone: {
                      target: 'loaded',
                      actions: 'setAssets',
                    },
                    onError: {
                      target: 'loaded',
                      actions: assign({
                        assetsLoading: false,
                        assets: [] as readonly GAAsset[],
                        versionRepositories: [] as readonly string[],
                        versionLastUpdated: null,
                      }),
                    },
                  },
                },

                loaded: {},
              },

              on: {
                // Region-level, not per-state: a version change loads its assets whether we
                // were idle, mid-fetch for the previous version, or already loaded.
                SELECT_VERSION: {
                  target: '.loadingAssets',
                  actions: 'setVersion',
                },
              },
            },
          },
        },

        // ==================================================
        // Tab Region - 5 tab sub-states with metadata
        // ==================================================
        tab: {
          initial: 'overview',
          states: TAB_STATES,
          on: {
            SELECT_TAB: TAB_TRANSITIONS,
          },
        },
      },
    },
    {
      // ==================================================
      // Actions
      // ==================================================
      actions: {
        setVersion: assign((_ctx, event) => {
          const e = event as { type: 'SELECT_VERSION'; version: string };
          return {
            selectedVersion: e.version,
            assets: [] as readonly GAAsset[],
            versionRepositories: [] as readonly string[],
            versionLastUpdated: null,
            lastLoadedVersion: null,
          };
        }),

        // `ctx`, not `_ctx`: assign receives the context as it was before this assignment, which
        // is what makes selectedVersion here the version loadAssets was actually invoked for.
        setAssets: assign((ctx, event) => {
          const data = (event as any).data as ComponentVersionDetail | undefined;
          return {
            assets: data?.assets ?? [],
            versionRepositories: data?.repositories ?? [],
            versionLastUpdated: data?.lastUpdated ?? null,
            assetsLoading: false,
            lastLoadedVersion: ctx.selectedVersion,
          };
        }),

      },

      // ==================================================
      // Guards
      // ==================================================
      guards: {
        ...TAB_GUARDS,

        // `!== null` deliberately, not truthiness: '' is a valid selected version for
        // versionless formats (raw). lastLoadedVersion alone prevents a re-invoke loop when a
        // version legitimately has no assets — an `assets.length === 0` clause would be dead
        // weight, since setVersion always clears assets and this `idle` is never re-entered.
        shouldLoadAssets: (ctx) => {
          return (
            ctx.selectedVersion !== null && ctx.lastLoadedVersion !== ctx.selectedVersion
          );
        },
      },

      // ==================================================
      // Services (override via withConfig for testing / hooks)
      // ==================================================
      services: {
        loadAssets: (_ctx) => {
          // Default: reject. Override in useMachine/withConfig.
          return Promise.reject(new Error('loadAssets service not configured'));
        },
      },
    }
  );
}

// =============================================================================
// HELPERS
// =============================================================================

/**
 * Get the active tab's metadata from a machine state.
 * Reads from state.meta which contains metadata from all active state nodes.
 */
export function getActiveTabMeta(state: any): TabStateMeta | undefined {
  if (!state?.meta) return undefined;
  return Object.values(state.meta).find(
    (m: any) => m && typeof m === 'object' && 'tabId' in m
  ) as TabStateMeta | undefined;
}
