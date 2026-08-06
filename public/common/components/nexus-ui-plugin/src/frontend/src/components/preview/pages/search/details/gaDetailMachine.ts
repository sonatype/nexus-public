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
 * Models the full detail lifecycle with parallel regions:
 * - `data`: loading -> loaded/error (async data fetch)
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
import type { GADetail, GAAsset, GADetailTab } from '../core/search.types';

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
  /** Whether the main detail data is loading */
  loading: boolean;
  /** Whether version-specific assets are loading */
  assetsLoading: boolean;
  /** Error message from last operation */
  error: string | null;
  /** Last version we loaded assets for — prevents infinite loop when assets are empty */
  lastLoadedVersion: string | null;
}

export type GaDetailMachineEvent =
  | { type: 'LOAD' }
  | { type: 'SELECT_TAB'; tab: GADetailTab }
  | { type: 'SELECT_VERSION'; version: string }
  | { type: 'BACK_TO_SEARCH' }
  | { type: 'RETRY' };

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
 * - `data`: loading -> loaded/error (handles async data fetch + version assets)
 * - `tab`: 5 tab sub-states with metadata (overview, versions, repositories, files, security)
 *
 * Services (override via interpret/withConfig):
 * - `loadDetail`: fetches GADetail for the given gaId
 * - `loadAssets`: fetches assets for a selected version
 */
export function createGaDetailMachine(gaId: string, initialVersion?: string) {
  return createMachine<GaDetailMachineContext, GaDetailMachineEvent>(
    {
      id: `ga-detail-${gaId}`,
      type: 'parallel',
      context: {
        gaId,
        detail: null,
        selectedVersion: initialVersion ?? null,
        assets: [],
        loading: true,
        assetsLoading: false,
        error: null,
        lastLoadedVersion: null,
      },

      states: {
        // ==================================================
        // Data Region - async load lifecycle
        // ==================================================
        data: {
          initial: 'loading',
          states: {
            loading: {
              entry: assign({ loading: true, error: null, lastLoadedVersion: null }),
              invoke: {
                src: 'loadDetail',
                onDone: {
                  target: 'loaded',
                  actions: 'setDetail',
                },
                onError: {
                  target: 'error',
                  actions: 'setError',
                },
              },
            },

            loaded: {
              entry: assign({ loading: false }),
              always: [
                // Auto-select first version if none selected (after detail loads)
                {
                  cond: 'shouldAutoSelectVersion',
                  target: 'loadingAssets',
                  actions: 'autoSelectFirstVersion',
                },
                // Load assets if version selected but assets not yet loaded
                {
                  cond: 'shouldLoadAssets',
                  target: 'loadingAssets',
                },
              ],
              on: {
                SELECT_VERSION: {
                  target: 'loadingAssets',
                  actions: 'setVersion',
                },
              },
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
                  }),
                },
              },
              on: {
                SELECT_VERSION: {
                  target: 'loadingAssets',
                  actions: 'setVersion',
                },
              },
            },

            error: {
              entry: assign({ loading: false }),
              on: {
                RETRY: 'loading',
                LOAD: 'loading',
              },
            },
          },

          on: {
            LOAD: '.loading',
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
        setDetail: assign((_ctx, event) => {
          const data = (event as any).data;
          return {
            detail: data as GADetail,
            loading: false,
            error: null,
          };
        }),

        setError: assign((_ctx, event) => {
          const err = (event as any).data;
          return {
            error: err instanceof Error ? err.message : 'Failed to load component detail',
            loading: false,
            detail: null,
          };
        }),

        setVersion: assign((_ctx, event) => {
          const e = event as { type: 'SELECT_VERSION'; version: string };
          return {
            selectedVersion: e.version,
            assets: [] as readonly GAAsset[],
            lastLoadedVersion: null,
          };
        }),

        setAssets: assign((_ctx, event) => {
          const data = (event as any).data;
          return {
            assets: (data as readonly GAAsset[]) ?? [],
            assetsLoading: false,
            lastLoadedVersion: _ctx.selectedVersion,
          };
        }),

        autoSelectFirstVersion: assign((ctx) => {
          const detail = ctx.detail;
          if (!detail || detail.versions.length === 0) return {};
          const firstVersion = detail.versions[0].version;
          return {
            selectedVersion: firstVersion,
          };
        }),
      },

      // ==================================================
      // Guards
      // ==================================================
      guards: {
        ...TAB_GUARDS,

        // Guard: Should auto-select first version when detail loads without a version
        shouldAutoSelectVersion: (ctx) => {
          const detail = ctx.detail;
          return (
            !ctx.selectedVersion &&
            Boolean(detail) &&
            detail.versions.length > 0
          );
        },

        // Guard: Should load assets when version is selected but not yet loaded
        shouldLoadAssets: (ctx) => {
          return (
            Boolean(ctx.selectedVersion) &&
            ctx.assets.length === 0 &&
            ctx.lastLoadedVersion !== ctx.selectedVersion
          );
        },
      },

      // ==================================================
      // Services (override via withConfig for testing / hooks)
      // ==================================================
      services: {
        loadDetail: (_ctx) => {
          // Default: reject. Override in useMachine/withConfig.
          return Promise.reject(new Error('loadDetail service not configured'));
        },
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

/**
 * Get a specific tab's definition by tabId.
 */
export function getTabDefinition(tabId: GADetailTab): TabDefinition | undefined {
  return TAB_DEFINITIONS.find((t) => t.tabId === tabId);
}
