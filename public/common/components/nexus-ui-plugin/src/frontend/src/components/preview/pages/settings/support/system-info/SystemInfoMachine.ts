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

import { assign, createMachine } from 'xstate';
import { APIConstants } from '../../../../../../constants/APIConstants';
import { restClient, parseApiError } from '../../../../../../interface/api';
import { SystemInformation, HASystemInformation, HANode } from './types';

const { REST } = APIConstants;

export const SECTION_ORDER = [
  'nexus-status',
  'nexus-node',
  'nexus-license',
  'nexus-configuration',
  'nexus-properties',
  'system-time',
  'system-properties',
  'system-environment',
  'system-runtime',
  'system-network',
  'system-filestores',
];

function transposeHASystemInfo(raw: Record<string, Record<string, unknown>>): HASystemInformation {
  const result: HASystemInformation = {};
  for (const [section, nodeMap] of Object.entries(raw)) {
    if (nodeMap && typeof nodeMap === 'object') {
      for (const [nodeId, sectionData] of Object.entries(nodeMap)) {
        if (!result[nodeId]) result[nodeId] = {};
        (result[nodeId] as Record<string, unknown>)[section] = sectionData;
      }
    }
  }
  return result;
}

export interface SystemInfoContext {
  systemInfo: SystemInformation | null;
  haSystemInfo: HASystemInformation | null;
  nodes: HANode[];
  selectedNode: string | null;
  isHAMode: boolean;
  expandedSections: string[];
  error: string | null;
}

interface SystemInfoFetchResult {
  systemInfo: SystemInformation | null;
  haSystemInfo: HASystemInformation | null;
  nodes: HANode[];
  selectedNode: string | null;
  isHAMode: boolean;
}

type SystemInfoEvent =
  | { type: 'REFRESH' }
  | { type: 'RETRY' }
  | { type: 'SELECT_NODE'; nodeId: string }
  | { type: 'TOGGLE_SECTION'; sectionKey: string; open: boolean }
  | { type: 'EXPAND_ALL' }
  | { type: 'COLLAPSE_ALL' }
  | { type: 'SET_ERROR'; message: string }
  | { type: 'CLEAR_ERROR' }
  | { type: 'done.invoke.fetchData'; data: SystemInfoFetchResult }
  | { type: 'error.platform.fetchData'; data: Error }
  | { type: 'done.invoke.refreshData'; data: SystemInfoFetchResult }
  | { type: 'error.platform.refreshData'; data: Error };

// Typed event aliases used in assign actions below
type FetchDoneEvent = Extract<SystemInfoEvent, { type: 'done.invoke.fetchData' | 'done.invoke.refreshData' }>;
type FetchErrorEvent = Extract<SystemInfoEvent, { type: 'error.platform.fetchData' | 'error.platform.refreshData' }>;
type SetErrorEvent = Extract<SystemInfoEvent, { type: 'SET_ERROR' }>;
type SelectNodeEvent = Extract<SystemInfoEvent, { type: 'SELECT_NODE' }>;
type ToggleSectionEvent = Extract<SystemInfoEvent, { type: 'TOGGLE_SECTION' }>;

export const systemInfoMachine = createMachine<SystemInfoContext, SystemInfoEvent>(
  {
    id: 'systemInfo',
    initial: 'loading',
    predictableActionArguments: true,
    context: {
      systemInfo: null,
      haSystemInfo: null,
      nodes: [],
      selectedNode: null,
      isHAMode: false,
      expandedSections: SECTION_ORDER.slice(0, 3),
      error: null,
    },
    states: {
      loading: {
        invoke: {
          id: 'fetchData',
          src: 'fetchData',
          onDone: {
            target: 'loaded',
            actions: 'setData',
          },
          onError: {
            target: 'loadError',
            actions: 'setError',
          },
        },
      },
      loaded: {
        on: {
          REFRESH: 'refreshing',
          SELECT_NODE: { actions: 'selectNode' },
          TOGGLE_SECTION: { actions: 'toggleSection' },
          EXPAND_ALL: { actions: 'expandAll' },
          COLLAPSE_ALL: { actions: 'collapseAll' },
          SET_ERROR: { actions: 'setErrorMessage' },
          CLEAR_ERROR: { actions: 'clearError' },
        },
      },
      refreshing: {
        invoke: {
          id: 'refreshData',
          src: 'refreshData',
          onDone: {
            target: 'loaded',
            actions: 'setData',
          },
          onError: {
            target: 'loaded',
            actions: 'setError',
          },
        },
      },
      loadError: {
        on: {
          RETRY: 'loading',
        },
      },
    },
  },
  {
    actions: {
      setData: assign({
        systemInfo: (_, event) => (event as FetchDoneEvent).data?.systemInfo ?? null,
        haSystemInfo: (_, event) => (event as FetchDoneEvent).data?.haSystemInfo ?? null,
        nodes: (_, event) => (event as FetchDoneEvent).data?.nodes ?? [],
        selectedNode: (_, event) => (event as FetchDoneEvent).data?.selectedNode ?? null,
        isHAMode: (_, event) => (event as FetchDoneEvent).data?.isHAMode ?? false,
        error: (_) => null,
      }),
      setError: assign({
        error: (_, event) => (event as FetchErrorEvent).data?.message ?? 'Failed to load system information',
      }),
      setErrorMessage: assign({
        error: (_, event) => (event as SetErrorEvent).message ?? 'An error occurred',
      }),
      selectNode: assign((context, event) => {
        const { nodeId } = event as SelectNodeEvent;
        // When nodeId is not found in haSystemInfo, return null so the UI renders the empty-data path
        // rather than silently showing stale data from the previously selected node.
        const systemInfo = context.haSystemInfo?.[nodeId] ?? null;
        return { ...context, selectedNode: nodeId, systemInfo };
      }),
      toggleSection: assign({
        expandedSections: (context, event) => {
          const { sectionKey, open } = event as ToggleSectionEvent;
          if (open) {
            return context.expandedSections.includes(sectionKey)
              ? context.expandedSections
              : [...context.expandedSections, sectionKey];
          }
          return context.expandedSections.filter((k) => k !== sectionKey);
        },
      }),
      expandAll: assign({
        expandedSections: (context) => {
          if (!context.systemInfo) return context.expandedSections;
          // In HA mode, section keys are consistent across nodes (same server instance).
          // expandedSections is derived from the currently selected node's systemInfo.
          return Object.keys(context.systemInfo).filter(
            (key) => context.systemInfo![key] && typeof context.systemInfo![key] === 'object'
          );
        },
      }),
      collapseAll: assign({
        expandedSections: (_) => [],
      }),
      clearError: assign({
        error: (_) => null,
      }),
    },
    services: {
      fetchData: async (): Promise<SystemInfoFetchResult> => {
        try {
          const nodes = await (async () => {
            try {
              const data = await restClient.get<HANode[]>(REST.INTERNAL.GET_SUPPORT_ZIP_ACTIVE_NODES);
              return Array.isArray(data) ? data : [];
            } catch {
              return [];
            }
          })();

          if (nodes.length > 1) {
            const raw = await restClient.get<Record<string, Record<string, unknown>>>(
              REST.SYSTEM_INFORMATION_HA
            );
            const haSystemInfo = transposeHASystemInfo(raw || {});
            const localNode = nodes.find((n) => n.local);
            const selectedNode = localNode?.nodeId || nodes[0]?.nodeId || null;
            const systemInfo = selectedNode ? haSystemInfo[selectedNode] || null : null;
            return { systemInfo, haSystemInfo, nodes, selectedNode, isHAMode: true };
          } else {
            const data = await restClient.get<SystemInformation>(REST.SYSTEM_INFORMATION);
            return {
              systemInfo: data || null,
              haSystemInfo: null,
              nodes,
              selectedNode: null,
              isHAMode: false,
            };
          }
        } catch (err: any) {
          const apiError = parseApiError(err);
          throw new Error(apiError.message || 'Failed to load system information');
        }
      },
      refreshData: async (context): Promise<SystemInfoFetchResult> => {
        try {
          if (context.isHAMode) {
            const raw = await restClient.get<Record<string, Record<string, unknown>>>(
              REST.SYSTEM_INFORMATION_HA
            );
            const haSystemInfo = transposeHASystemInfo(raw || {});
            const { selectedNode } = context;
            const systemInfo = selectedNode ? haSystemInfo[selectedNode] || null : null;
            return { systemInfo, haSystemInfo, nodes: context.nodes, selectedNode, isHAMode: true };
          } else {
            const data = await restClient.get<SystemInformation>(REST.SYSTEM_INFORMATION);
            return {
              systemInfo: data || null,
              haSystemInfo: null,
              nodes: context.nodes,
              selectedNode: null,
              isHAMode: false,
            };
          }
        } catch (err: any) {
          const apiError = parseApiError(err);
          throw new Error(apiError.message || 'Failed to refresh system information');
        }
      },
    },
  }
);
