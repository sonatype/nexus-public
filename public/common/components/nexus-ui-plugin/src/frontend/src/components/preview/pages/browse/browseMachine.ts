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
 * Browse Machine - XState state machine for the Browse page.
 *
 * Models the two-level browse flow:
 * 1. Repository List: filter + paginate + select a repository
 * 2. Tree View: expand/collapse tree + select node + view detail
 *
 * The tree sub-states model node expansion as deterministic state transitions,
 * eliminating the mutation bugs that plagued the useState-based useBrowseTree.
 *
 * Events:
 * - SELECT_REPO: transition from repoList to treeView
 * - BACK: transition from treeView to repoList
 * - EXPAND_NODE / COLLAPSE_NODE: tree expansion
 * - SELECT_NODE: node selection + detail loading
 * - FILTER: repository list filtering
 */

import { createMachine, assign } from 'xstate';
import { ExtJS } from '../../../../interface/ExtJS';
import type { BrowseNode } from './tree/browse-tree.types';
import type { AssetData, ComponentData } from './detail/DetailPanel';

// =============================================================================
// CONTEXT & EVENT TYPES
// =============================================================================

export interface BrowseMachineContext {
  /** Selected repository name */
  selectedRepository: string | null;
  /** Repository URL for copy */
  repositoryUrl: string;
  /** Selected tree node */
  selectedNode: BrowseNode | null;
  /** Detail panel data */
  detailData: {
    asset: AssetData | null;
    component: ComponentData | null;
    loading: boolean;
    error: string | null;
  };
  /** Repository list filters */
  filters: {
    formats: string[];
    types: string[];
    statuses: string[];
    nameFilter: string;
  };
}

export type BrowseMachineEvent =
  | { type: 'SELECT_REPO'; repoName: string }
  | { type: 'BACK' }
  | { type: 'SELECT_NODE'; node: BrowseNode }
  | { type: 'CLEAR_NODE' }
  | { type: 'NODE_DELETED' }
  | { type: 'SET_FILTER'; section: string; value: string | string[] }
  | { type: 'SET_NAME_FILTER'; value: string }
  | { type: 'CLEAR_FILTERS' };

// =============================================================================
// BROWSE MACHINE
// =============================================================================

export function createBrowseMachine(initialRepo?: string) {
  return createMachine<BrowseMachineContext, BrowseMachineEvent>(
    {
      id: 'browse',
      initial: initialRepo ? 'treeView' : 'repoList',
      context: {
        selectedRepository: initialRepo || null,
        repositoryUrl: initialRepo ? ExtJS.urlOf(`/repository/${initialRepo}/`) : '',
        selectedNode: null,
        detailData: {
          asset: null,
          component: null,
          loading: false,
          error: null,
        },
        filters: {
          formats: [],
          types: [],
          statuses: [],
          nameFilter: '',
        },
      },

      states: {
        // ============================================
        // Repository List (Step 1)
        // ============================================
        repoList: {
          meta: {
            view: 'list',
            description: 'Repository list with filters and pagination',
          },
          on: {
            SELECT_REPO: {
              target: 'treeView',
              actions: 'selectRepository',
            },
            SET_FILTER: {
              actions: 'updateFilter',
            },
            SET_NAME_FILTER: {
              actions: 'updateNameFilter',
            },
            CLEAR_FILTERS: {
              actions: 'clearFilters',
            },
          },
        },

        // ============================================
        // Tree View (Step 2) - compound state
        // ============================================
        treeView: {
          meta: {
            view: 'tree',
            description: 'Tree browser with detail panel',
          },
          initial: 'browsing',
          states: {
            browsing: {
              meta: {
                detailState: 'empty',
              },
            },
            loadingDetail: {
              meta: {
                detailState: 'loading',
              },
              invoke: {
                src: 'loadNodeDetail',
                onDone: {
                  target: 'viewingDetail',
                  actions: 'setDetailData',
                },
                onError: {
                  target: 'viewingDetail',
                  actions: 'setDetailError',
                },
              },
            },
            viewingDetail: {
              meta: {
                detailState: 'loaded',
              },
            },
          },
          on: {
            BACK: {
              target: 'repoList',
              actions: 'clearRepository',
            },
            SELECT_NODE: {
              target: '.loadingDetail',
              actions: 'selectNode',
            },
            CLEAR_NODE: {
              target: '.browsing',
              actions: 'clearNode',
            },
            NODE_DELETED: {
              target: '.browsing',
              actions: 'clearNode',
            },
          },
        },
      },
    },
    {
      actions: {
        selectRepository: assign((_ctx, event) => {
          const e = event as { type: 'SELECT_REPO'; repoName: string };
          return {
            selectedRepository: e.repoName,
            repositoryUrl: ExtJS.urlOf(`/repository/${e.repoName}/`),
            selectedNode: null,
            detailData: { asset: null, component: null, loading: false, error: null },
          };
        }),

        clearRepository: assign({
          selectedRepository: null as string | null,
          repositoryUrl: '',
          selectedNode: null as BrowseNode | null,
          detailData: { asset: null, component: null, loading: false, error: null },
        }),

        selectNode: assign((_ctx, event) => {
          const e = event as { type: 'SELECT_NODE'; node: BrowseNode };
          return {
            selectedNode: e.node,
            detailData: { asset: null, component: null, loading: true, error: null },
          };
        }),

        clearNode: assign({
          selectedNode: null as BrowseNode | null,
          detailData: { asset: null, component: null, loading: false, error: null },
        }),

        setDetailData: assign((_ctx, event) => {
          const data = (event as any).data;
          return {
            detailData: {
              asset: data?.asset || null,
              component: data?.component || null,
              loading: false,
              error: null,
            },
          };
        }),

        setDetailError: assign((_ctx, event) => {
          const err = (event as any).data;
          return {
            detailData: {
              asset: null,
              component: null,
              loading: false,
              error: err instanceof Error ? err.message : 'Failed to load details',
            },
          };
        }),

        updateFilter: assign((ctx, event) => {
          const e = event as { type: 'SET_FILTER'; section: string; value: string | string[] };
          return {
            filters: {
              ...ctx.filters,
              [e.section]: e.value,
            },
          };
        }),

        updateNameFilter: assign((ctx, event) => {
          const e = event as { type: 'SET_NAME_FILTER'; value: string };
          return {
            filters: {
              ...ctx.filters,
              nameFilter: e.value,
            },
          };
        }),

        clearFilters: assign({
          filters: {
            formats: [] as string[],
            types: [] as string[],
            statuses: [] as string[],
            nameFilter: '',
          },
        }),
      },

      services: {
        loadNodeDetail: (_ctx) => Promise.resolve({ asset: null, component: null }),
      },
    }
  );
}

/**
 * Get the active view metadata from machine state.
 */
export function getActiveViewMeta(state: any): { view: string; description: string } | undefined {
  if (!state?.meta) return undefined;
  return Object.values(state.meta).find(
    (m: any) => m && typeof m === 'object' && 'view' in m
  ) as any;
}
