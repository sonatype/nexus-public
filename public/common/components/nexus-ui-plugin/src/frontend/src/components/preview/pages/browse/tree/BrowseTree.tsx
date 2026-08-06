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

import React, { useCallback, useRef, useEffect, useImperativeHandle, forwardRef } from 'react';
import { Box, Flex, Text, Spinner, Button, Callout } from '@radix-ui/themes';
import { FolderOpen, RefreshCw } from 'lucide-react';

import { EmptyState } from '../../../shared';
import { BrowseTreeNode } from './BrowseTreeNode';
import { useBrowseTree } from './useBrowseTree';
import type { BrowseTreeProps, BrowseNode, BrowseTreeRef } from './browse-tree.types';

import './BrowseTree.scss';

/**
 * UI Strings for the browse tree.
 */
const STRINGS = {
  title: 'Repository Contents',
  emptyTitle: 'No items in this repository',
  emptyDescription:
    'This repository is empty. Upload components or configure a proxy repository to populate it.',
  loadingMessage: 'Loading...',
  errorPrefix: 'Error: ',
  retryButton: 'Retry',
};

/**
 * BrowseTree displays the lazy-loading tree of repository contents.
 *
 * Features:
 * - Lazy loading of children on expand
 * - Auto-expand single child folders
 * - Deep linking support (initialPath)
 * - Keyboard navigation (arrow keys, Enter)
 * - ARIA tree role for accessibility
 * - Imperative API via ref (expandToPath, scrollToNode)
 */
export const BrowseTree = forwardRef<BrowseTreeRef, BrowseTreeProps>(function BrowseTree(
  {
    repositoryName,
    initialPath,
    onSelect,
    baseUrl = 'preview/browse',
    className = '',
  },
  ref
) {
  const { state, actions } = useBrowseTree(repositoryName, initialPath);
  const treeRef = useRef<HTMLDivElement>(null);
  const lastExpandedPathRef = useRef<string | undefined>();

  /**
   * Scroll a specific node into view.
   */
  const scrollToNode = useCallback((nodeId: string) => {
    if (!treeRef.current) {
      return;
    }

    // Find the node element by data-node-id attribute
    const selector = `[data-node-id="${CSS.escape(nodeId)}"]`;
    const nodeElement = treeRef.current.querySelector(selector);

    if (nodeElement) {
      nodeElement.scrollIntoView({
        behavior: 'smooth',
        block: 'center',
        inline: 'nearest',
      });
    }
  }, []);

  /**
   * Expose methods to parent via ref.
   */
  useImperativeHandle(
    ref,
    () => ({
      expandToPath: async (path: string) => {
        await actions.expandToPath(path);
        // After expanding, scroll to the node
        scrollToNode(path);
      },
      scrollToNode,
      removeNode: (nodeId: string) => actions.removeNode(nodeId),
    }),
    [actions, scrollToNode]
  );

  /**
   * Watch for initialPath changes and expand when it changes.
   * This handles the case where search results update the URL path.
   *
   * Guard: when expandToPath fuzzy-matches a node whose ID differs from the
   * requested path (e.g. NPM `talkjs/0.3.0` → `talkjs/-/talkjs-0.3.0.tgz`),
   * the programmatic selection callback updates the URL to the *actual* node ID.
   * That URL change re-triggers this effect.  Without the selectedNodeId guard
   * the second expandToPath would fail (the `-` segment is filtered from the
   * browse tree) and overwrite the correct selection.
   */
  useEffect(() => {
    if (initialPath && state.nodes.length > 0 && !state.loading && initialPath !== lastExpandedPathRef.current) {
      // If the tree already has this path selected (e.g. URL updated after fuzzy
      // match resolved to the actual node ID), just record it and skip expansion.
      // Compare decoded versions because tree node IDs contain URL-encoded segments
      // (e.g. %40v, %21data%21dog) while initialPath is already decoded from the URL.
      const decodedSelectedId = safeDecodeURI(state.selectedNodeId);
      const decodedInitialPath = safeDecodeURI(initialPath);
      if (decodedSelectedId && decodedSelectedId === decodedInitialPath) {
        lastExpandedPathRef.current = initialPath;
        return;
      }

      lastExpandedPathRef.current = initialPath;

      // Small delay to let the route fully settle
      const timer = setTimeout(() => {
        actions.expandToPath(initialPath);
      }, 50);
      return () => clearTimeout(timer);
    }
  }, [initialPath, state.nodes.length, state.loading, actions, state.selectedNodeId]);

  /**
   * Scroll to selected node after DOM updates (GitHub pattern).
   * This fires AFTER React renders the newly expanded nodes.
   */
  useEffect(() => {
    if (state.selectedNodeId && !state.loading) {
      // Wait for next frame to ensure DOM is updated
      requestAnimationFrame(() => {
        scrollToNode(state.selectedNodeId);
      });
    }
  }, [state.selectedNodeId, state.loading, scrollToNode]);

  /**
   * Trigger onSelect callback when selection changes from expandToPath.
   * This ensures the detail panel loads when navigating from search results.
   * Only triggers for programmatic selection (expandToPath), not user clicks.
   */
  const lastSelectedNodeRef = useRef<string | null>(null);
  const isUserInteractionRef = useRef(false);

  useEffect(() => {
    // Skip if this was a user interaction (handleSelect already called onSelect)
    if (isUserInteractionRef.current) {
      isUserInteractionRef.current = false;
      lastSelectedNodeRef.current = state.selectedNodeId;
      return;
    }

    if (state.selectedNodeId && state.selectedNodeId !== lastSelectedNodeRef.current) {
      lastSelectedNodeRef.current = state.selectedNodeId;

      // Find the node and trigger onSelect for programmatic selection
      const node = findNodeInState(state.nodes, state.selectedNodeId);
      if (node && onSelect) {
        onSelect(node.node);
      } else if (!node && onSelect && state.selectedNodeId) {
        // Node not in tree (e.g., npm package not yet loaded) - construct minimal node from path
        const pathParts = state.selectedNodeId.split('/');
        const isFolder = pathParts.length < 2; // Root level items are folders
        onSelect({
          id: state.selectedNodeId,
          text: pathParts[pathParts.length - 1],
          type: isFolder ? 'folder' : 'component',
          leaf: !isFolder,
        });
      }
    }
  }, [state.selectedNodeId, state.nodes, onSelect]);

  /**
   * Handle node toggle (expand/collapse).
   */
  const handleToggle = useCallback(
    (nodeId: string) => {
      actions.toggle(nodeId);
    },
    [actions]
  );

  /**
   * Handle node selection.
   */
  const handleSelect = useCallback(
    (node: BrowseNode) => {
      // Mark as user interaction to prevent double onSelect call
      isUserInteractionRef.current = true;
      actions.select(node.id);
      if (onSelect) {
        onSelect(node);
      }
    },
    [actions, onSelect]
  );

  /**
   * Handle keyboard navigation.
   */
  const handleKeyDown = useCallback(
    (event: React.KeyboardEvent, nodeId: string) => {
      switch (event.key) {
        case 'ArrowDown':
          event.preventDefault();
          actions.focusNext();
          break;
        case 'ArrowUp':
          event.preventDefault();
          actions.focusPrevious();
          break;
        case 'ArrowRight':
          event.preventDefault();
          actions.toggle(nodeId);
          break;
        case 'ArrowLeft':
          event.preventDefault();
          actions.focusParent();
          break;
        case 'Enter':
        case ' ': {
          event.preventDefault();
          const node = findNodeInState(state.nodes, nodeId);
          if (node) {
            handleSelect(node.node);
          }
          break;
        }
        default:
          break;
      }
    },
    [actions, handleSelect, state.nodes]
  );

  /**
   * Handle retry on error.
   */
  const handleRetry = useCallback(() => {
    actions.refresh();
  }, [actions]);

  // Build the base URL for node links
  const fullBaseUrl = `${baseUrl}/${repositoryName}`;

  // Loading state
  if (state.loading && state.nodes.length === 0) {
    return (
      <Flex
        direction="column"
        align="center"
        justify="center"
        gap="3"
        className="browse-tree__loading"
        p="6"
      >
        <Spinner size="3" />
        <Text color="gray" size="2">{STRINGS.loadingMessage}</Text>
      </Flex>
    );
  }

  // Error state
  if (state.error && state.nodes.length === 0) {
    return (
      <Box p="4" className="browse-tree__error-container">
        <Callout.Root color="red" mb="3">
          <Callout.Text>
            {STRINGS.errorPrefix}
            {state.error}
          </Callout.Text>
        </Callout.Root>
        <Button variant="outline" size="2" onClick={handleRetry}>
          <RefreshCw size={14} />
          {STRINGS.retryButton}
        </Button>
      </Box>
    );
  }

  // Empty state — shared EmptyState component (Nexus One pattern)
  if (state.nodes.length === 0) {
    return (
      <Box className="browse-tree__empty" p="6">
        <EmptyState
          icon={FolderOpen}
          title={STRINGS.emptyTitle}
          description={STRINGS.emptyDescription}
          action={{
            label: STRINGS.retryButton,
            onClick: handleRetry,
            icon: RefreshCw,
          }}
          size="small"
        />
      </Box>
    );
  }

  return (
    <Box
      ref={treeRef}
      className={`browse-tree ${className}`}
      role="tree"
      aria-label={`${repositoryName} contents`}
      data-testid="browse-tree"
    >
      {state.nodes.map((nodeState) => (
        <BrowseTreeNode
          key={nodeState.node.id}
          nodeState={nodeState}
          repositoryName={repositoryName}
          depth={0}
          onToggle={handleToggle}
          onSelect={handleSelect}
          isFocused={state.focusedNodeId === nodeState.node.id}
          selectedNodeId={state.selectedNodeId}
          onKeyDown={handleKeyDown}
          baseUrl={fullBaseUrl}
        />
      ))}
    </Box>
  );
});

/**
 * Safely decode a URI component, returning the original string if decoding fails.
 * Tree node IDs contain URL-encoded segments (e.g. %40v for @v, %21data%21dog for !data!dog)
 * while initialPath from the URL is already decoded. This helper normalizes both for comparison.
 */
function safeDecodeURI(value: string | undefined): string | undefined {
  if (!value) return value;
  try {
    return decodeURIComponent(value);
  } catch {
    return value;
  }
}

/**
 * Helper to find a node in the tree state by ID.
 */
function findNodeInState(
  nodes: readonly { node: BrowseNode; children: readonly any[] | null }[],
  nodeId: string
): { node: BrowseNode } | null {
  for (const nodeState of nodes) {
    if (nodeState.node.id === nodeId) {
      return nodeState;
    }
    if (nodeState.children) {
      const found = findNodeInState(nodeState.children, nodeId);
      if (found) return found;
    }
  }
  return null;
}

export default BrowseTree;
