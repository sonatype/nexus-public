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
    case 'asset':
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
  selectedNodeId,
  onKeyDown,
  baseUrl,
}: BrowseTreeNodeProps): JSX.Element {
  const { node, expanded, children, loading, error } = nodeState;
  const nodeRef = useRef<HTMLDivElement>(null);

  // Focus management
  useEffect(() => {
    if (isFocused && nodeRef.current) {
      nodeRef.current.focus();
    }
  }, [isFocused]);

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
   * Handle click on the node content.
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
   * Handle keyboard events.
   */
  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      onKeyDown(e, node.id);
    },
    [node.id, onKeyDown]
  );

  // Calculate indentation based on depth
  const indentPx = depth * 20;

  return (
    <div
      className="browse-tree__node-container"
      data-node-id={node.id}
      role="treeitem"
      aria-expanded={node.leaf ? undefined : expanded}
      aria-selected={isFocused}
      aria-level={depth + 1}
    >
      <Flex
        ref={nodeRef}
        className={`browse-tree__node ${isFocused ? 'browse-tree__node--focused' : ''} ${selectedNodeId === node.id ? 'browse-tree__node--selected' : ''}`}
        align="center"
        gap="1"
        py="1"
        tabIndex={isFocused ? 0 : -1}
        onKeyDown={handleKeyDown}
        style={{ paddingLeft: `${indentPx}px` }}
        data-testid={`tree-node-${node.id}`}
      >
        {/* Expand/Collapse Toggle */}
        <Box
          className="browse-tree__toggle"
          onClick={handleToggleClick}
          style={{ 
            width: '20px',
            cursor: node.leaf ? 'default' : 'pointer',
            opacity: node.leaf ? 0 : 1,
          }}
          aria-label={expanded ? 'Collapse' : 'Expand'}
          aria-hidden={node.leaf}
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

        {/* Node Icon — clickable, same as label */}
        <Box
          onClick={handleNodeClick}
          style={{ cursor: 'pointer', display: 'flex', alignItems: 'center' }}
        >
          <NodeIcon
            type={node.type}
            expanded={expanded}
            isSelected={selectedNodeId === node.id}
          />
        </Box>

        {/* Node Label — intentionally NOT an <a href> to prevent
            the browser from following a hash link and corrupting the
            URL with duplicate path segments (bug 9mw3). Navigation is
            handled programmatically by BrowsePage.handleSelectNode. */}
        <Text
          size="2"
          onClick={handleNodeClick}
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
          {children.map((childState) => (
            <BrowseTreeNode
              key={childState.node.id}
              nodeState={childState}
              repositoryName={repositoryName}
              depth={depth + 1}
              onToggle={onToggle}
              onSelect={onSelect}
              isFocused={false}
              selectedNodeId={selectedNodeId}
              onKeyDown={onKeyDown}
              baseUrl={baseUrl}
            />
          ))}
        </div>
      )}
    </div>
  );
}

export default BrowseTreeNode;

