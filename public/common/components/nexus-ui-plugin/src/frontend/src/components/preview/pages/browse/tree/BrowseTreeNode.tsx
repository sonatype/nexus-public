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

import React, { useCallback, useRef, useEffect } from 'react';
import { Box, Flex, Text, Spinner } from '@radix-ui/themes';
import { ChevronRight, ChevronDown, Folder, FolderOpen, Package, File } from 'lucide-react';

import type { BrowseTreeNodeProps, BrowseNodeType } from './browse-tree.types';
import './BrowseTree.scss';

/**
 * Get the appropriate icon for a node type.
 * Folder: FolderOpen when expanded or selected, else Folder.
 */
function NodeIcon({
  type,
  expanded,
  isSelected,
}: {
  type: BrowseNodeType;
  expanded?: boolean;
  isSelected?: boolean;
}): JSX.Element {
  const iconProps = { size: 16, className: 'browse-tree__node-icon' };

  switch (type) {
    case 'folder':
      return (expanded || isSelected) ? (
        <FolderOpen {...iconProps} data-testid="icon-folder-open" />
      ) : (
        <Folder {...iconProps} data-testid="icon-folder" />
      );
    case 'component':
      return <Package {...iconProps} data-testid="icon-component" />;
    default:
      return <File {...iconProps} data-testid="icon-asset" />;
  }
}

/**
 * Individual node in the browse tree.
 *
 * Handles:
 * - Expand/collapse for non-leaf nodes
 * - Lazy loading indicator
 * - Node selection and navigation
 * - Keyboard accessibility
 */
export function BrowseTreeNode({
  nodeState,
  repositoryName,
  depth,
  onToggle,
  onSelect,
  isFocused,
  focusedNodeId,
  tabStopNodeId,
  selectedNodeId,
  onKeyDown,
  baseUrl,
  siblingCount,
  siblingIndex,
}: BrowseTreeNodeProps): JSX.Element {
  const { node, expanded, children, loading, error } = nodeState;
  const nodeRef = useRef<HTMLDivElement>(null);

  // `isFocused` is honoured when passed explicitly; otherwise it is derived from
  // focusedNodeId, which is threaded all the way down the recursion so nested
  // nodes can hold the roving tab stop too (previously every child was rendered
  // with isFocused={false}, making deep nodes unreachable).
  const focused = isFocused ?? focusedNodeId === node.id;
  const selected = selectedNodeId === node.id;
  // Roving tab stop: one tabbable row per tree. It follows focus once the user
  // is navigating, and otherwise rests on whichever node the tree nominates
  // (the first visible one), which must not pull DOM focus.
  const tabStop = focused || tabStopNodeId === node.id;

  // Focus management
  useEffect(() => {
    if (focused && nodeRef.current) {
      nodeRef.current.focus();
    }
  }, [focused]);

  /**
   * Handle click on the expand/collapse chevron.
   */
  const handleToggleClick = useCallback(
    (e: React.MouseEvent) => {
      e.preventDefault();
      e.stopPropagation();
      if (!node.leaf) {
        onToggle(node.id);
      }
    },
    [node.id, node.leaf, onToggle]
  );

  /**
   * Handle a click anywhere on the row.
   *
   * The whole row is the hit target, not just the icon and label: the row already
   * spans the full visible width and shows a pointer cursor, so a click landing on
   * the indent or on the empty space past the label must select too. Restricting
   * it to the text made the row look clickable while parts of it were inert.
   *
   * Uses a non-navigating element to prevent the browser from
   * following an href and corrupting the URL hash (see 9mw3).
   */
  const handleNodeClick = useCallback(
    (e: React.MouseEvent) => {
      e.preventDefault();
      e.stopPropagation();
      if (onSelect) {
        onSelect(node);
      }
    },
    [node, onSelect]
  );

  /**
   * Handle double click on the row: expand a collapsed folder, collapse an
   * expanded one. Leaves are inert — a double click on an
   * asset must not attempt to load children.
   */
  const handleDoubleClick = useCallback(
    (e: React.MouseEvent) => {
      e.preventDefault();
      e.stopPropagation();
      if (node.leaf) {
        return;
      }
      // The chevron already toggled once per click, and `dblclick` is delivered
      // in addition to those clicks rather than instead of them. Toggling again
      // here would make a double click on the chevron flip the folder three
      // times — visible flicker, plus a redundant children fetch.
      if ((e.target as HTMLElement | null)?.closest('.browse-tree__toggle')) {
        return;
      }
      onToggle(node.id);
    },
    [node.id, node.leaf, onToggle]
  );

  /**
   * Handle keyboard events.
   */
  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      // Descendant treeitems are nested inside this element, so their key events
      // bubble through here. Only the treeitem the event actually came from may
      // act on it, or one keystroke would drive every ancestor as well.
      const origin = (e.target as HTMLElement | null)?.closest('[role="treeitem"]');
      if (origin && origin !== e.currentTarget) return;

      onKeyDown(e, node.id);
    },
    [node.id, onKeyDown]
  );

  // Calculate indentation based on depth
  const indentPx = depth * 20;

  return (
    // The treeitem is the focusable element: role, ARIA state, the roving
    // tabIndex, the key handler and DOM focus all sit on this one node, so what
    // assistive technology reports and what receives the keystroke can never
    // drift apart. The row below carries presentation and the pointer hit target;
    // it deliberately does not wrap the child group, so a click on a nested row
    // cannot be read as a click on its ancestor.
    <div
      ref={nodeRef}
      className="browse-tree__node-container"
      data-node-id={node.id}
      role="treeitem"
      aria-expanded={node.leaf ? undefined : expanded}
      aria-selected={selected}
      aria-level={depth + 1}
      aria-setsize={siblingCount}
      aria-posinset={siblingIndex}
      tabIndex={tabStop ? 0 : -1}
      onKeyDown={handleKeyDown}
      onDoubleClick={handleDoubleClick}
      data-testid={`tree-node-${node.id}`}
    >
      <Flex
        className={`browse-tree__node ${focused ? 'browse-tree__node--focused' : ''} ${selected ? 'browse-tree__node--selected' : ''}`}
        align="center"
        gap="1"
        py="1"
        onClick={handleNodeClick}
        style={{ paddingLeft: `${indentPx}px` }}
        data-testid={`tree-row-${node.id}`}
      >
        {/* Expand/Collapse Toggle */}
        <Box
          className="browse-tree__toggle"
          // Leaves have no toggle, so this 20px column is invisible spacing; leaving
          // a handler on it would stop propagation and make that strip of the row dead.
          onClick={node.leaf ? undefined : handleToggleClick}
          style={{ 
            width: '20px',
            cursor: node.leaf ? 'default' : 'pointer',
            opacity: node.leaf ? 0 : 1,
          }}
          /* Decorative: aria-expanded on the treeitem already conveys the state,
             and keyboard users expand with ArrowRight. Exposing a second
             unlabelled control here would only add noise for screen readers. */
          aria-hidden="true"
          tabIndex={-1}
          data-testid={`toggle-${node.id}`}
        >
          {!node.leaf && (
            loading ? (
              <Spinner size="1" />
            ) : expanded ? (
              <ChevronDown size={16} />
            ) : (
              <ChevronRight size={16} />
            )
          )}
        </Box>

        {/* Node Icon — the row handles the click */}
        <Box style={{ display: 'flex', alignItems: 'center' }}>
          <NodeIcon
            type={node.type}
            expanded={expanded}
            isSelected={selected}
          />
        </Box>

        {/* Node Label — intentionally NOT an <a href> to prevent
            the browser from following a hash link and corrupting the
            URL with duplicate path segments (bug 9mw3). Navigation is
            handled programmatically by BrowsePage.handleSelectNode. */}
        <Text
          size="2"
          className="browse-tree__node-link"
          tabIndex={-1}
          data-testid={`link-${node.id}`}
        >
          {node.text}
        </Text>
      </Flex>

      {/* Error message */}
      {error && (
        <Box className="browse-tree__error" pl={`${indentPx + 20}px`}>
          <Text size="1" color="red">
            {error}
          </Text>
        </Box>
      )}

      {/* Children (if expanded) */}
      {expanded && children && children.length > 0 && (
        <div role="group" className="browse-tree__children">
          {children.map((childState, index) => (
            <BrowseTreeNode
              key={childState.node.id}
              nodeState={childState}
              repositoryName={repositoryName}
              depth={depth + 1}
              onToggle={onToggle}
              onSelect={onSelect}
              focusedNodeId={focusedNodeId}
              tabStopNodeId={tabStopNodeId}
              selectedNodeId={selectedNodeId}
              onKeyDown={onKeyDown}
              baseUrl={baseUrl}
              siblingCount={children.length}
              siblingIndex={index + 1}
            />
          ))}
        </div>
      )}
    </div>
  );
}

export default BrowseTreeNode;

