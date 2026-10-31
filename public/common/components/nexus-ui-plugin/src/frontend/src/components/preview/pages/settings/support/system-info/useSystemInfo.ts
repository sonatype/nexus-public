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

import React, { useCallback, useEffect, useMemo, useRef } from 'react';
import { useMachine } from '@xstate/react';
import { systemInfoMachine, SECTION_ORDER } from './SystemInfoMachine';
import { useToast } from '../../../../shared';
import { SystemInformation, HANode } from './types';

function sortSections(sections: [string, any][]): [string, any][] {
  return [...sections].sort(([keyA], [keyB]) => {
    const indexA = SECTION_ORDER.indexOf(keyA);
    const indexB = SECTION_ORDER.indexOf(keyB);
    if (indexA !== -1 && indexB !== -1) return indexA - indexB;
    if (indexA !== -1) return -1;
    if (indexB !== -1) return 1;
    return keyA.localeCompare(keyB);
  });
}

export interface UseSystemInfoReturn {
  systemInfo: SystemInformation | null;
  nodes: HANode[];
  selectedNode: string | null;
  isHAMode: boolean;
  isLoading: boolean;
  isRefreshing: boolean;
  error: string | null;
  expandedSections: Set<string>;
  sections: [string, any][];
  sectionRefs: React.MutableRefObject<Record<string, HTMLDivElement | null>>;
  handleNodeSelect: (nodeId: string) => void;
  handleRefresh: () => void;
  handleDownload: () => void;
  handleCopy: () => Promise<void>;
  handleExpandAll: () => void;
  handleCollapseAll: () => void;
  handleSectionToggle: (sectionKey: string, isOpen: boolean) => void;
  handleJumpToSection: (sectionKey: string) => void;
  clearError: () => void;
}

export function useSystemInfo(): UseSystemInfoReturn {
  const toast = useToast();
  const [state, send] = useMachine(systemInfoMachine);
  const { systemInfo, nodes, selectedNode, isHAMode, expandedSections: expandedSectionsArray, error } =
    state.context;

  const sectionRefs = useRef<Record<string, HTMLDivElement | null>>({});
  const prevStateRef = useRef(state);

  const isLoading = state.matches('loading');
  const isRefreshing = state.matches('refreshing');

  // Toast on successful refresh.
  // `toast` is in deps so a reference change is picked up; extra runs are inert because
  // the prevState/state comparison only emits a toast on the specific transition below.
  useEffect(() => {
    const prevState = prevStateRef.current;
    if (prevState.matches('refreshing') && state.matches('loaded') && !state.context.error) {
      toast.success('System information refreshed');
    }
    prevStateRef.current = state;
  }, [state, toast]);

  const expandedSections = useMemo(() => new Set(expandedSectionsArray), [expandedSectionsArray]);

  const sections = useMemo(() => {
    if (!systemInfo) return [];
    return sortSections(
      Object.entries(systemInfo).filter(([_, value]) => value && typeof value === 'object')
    );
  }, [systemInfo]);

  const handleNodeSelect = useCallback(
    (nodeId: string) => {
      send({ type: 'SELECT_NODE', nodeId });
    },
    [send]
  );

  const handleRefresh = useCallback(() => {
    send({ type: 'REFRESH' });
  }, [send]);

  const handleDownload = useCallback(() => {
    if (!systemInfo) return;
    const filename =
      isHAMode && selectedNode
        ? `system-information-${selectedNode}.json`
        : 'system-information.json';
    const jsonStr = JSON.stringify(systemInfo, null, 2);
    const blob = new Blob([jsonStr], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    // Defer revocation to the next tick: Chromium reads the object URL asynchronously,
    // and revoking synchronously after click() can abort the download.
    setTimeout(() => URL.revokeObjectURL(url), 0);
    toast.success('System information downloaded');
  }, [systemInfo, isHAMode, selectedNode, toast]);

  const handleCopy = useCallback(async () => {
    if (!systemInfo) return;
    try {
      await navigator.clipboard.writeText(JSON.stringify(systemInfo, null, 2));
      toast.success('Copied to clipboard');
    } catch {
      send({ type: 'SET_ERROR', message: 'Failed to copy to clipboard' });
    }
  }, [systemInfo, toast, send]);

  const handleExpandAll = useCallback(() => {
    send({ type: 'EXPAND_ALL' });
  }, [send]);

  const handleCollapseAll = useCallback(() => {
    send({ type: 'COLLAPSE_ALL' });
  }, [send]);

  const handleSectionToggle = useCallback(
    (sectionKey: string, open: boolean) => {
      send({ type: 'TOGGLE_SECTION', sectionKey, open });
    },
    [send]
  );

  const handleJumpToSection = useCallback(
    (sectionKey: string) => {
      send({ type: 'TOGGLE_SECTION', sectionKey, open: true });
      // requestAnimationFrame fires after the next paint, so the section is guaranteed to be
      // visible in the DOM before scrollIntoView is called.
      requestAnimationFrame(() => {
        const ref = sectionRefs.current[sectionKey];
        if (ref) {
          ref.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
      });
    },
    [send]
  );

  const clearError = useCallback(() => {
    send({ type: 'CLEAR_ERROR' });
  }, [send]);

  return {
    systemInfo,
    nodes,
    selectedNode,
    isHAMode,
    isLoading,
    isRefreshing,
    error,
    expandedSections,
    sections,
    sectionRefs,
    handleNodeSelect,
    handleRefresh,
    handleDownload,
    handleCopy,
    handleExpandAll,
    handleCollapseAll,
    handleSectionToggle,
    handleJumpToSection,
    clearError,
  };
}
