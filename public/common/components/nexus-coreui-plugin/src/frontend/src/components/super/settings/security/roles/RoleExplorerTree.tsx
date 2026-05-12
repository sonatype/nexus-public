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

import React, { useState, useMemo, useEffect } from 'react';
import { Box, Flex, Text, IconButton, ScrollArea, Tooltip, TextField, Button, Separator } from '@radix-ui/themes';
import { 
  Shield, 
  Key, 
  Filter, 
  ChevronRight, 
  ChevronDown, 
  AlertTriangle,
  Loader2,
  Search,
  Maximize2,
  Minimize2,
  ChevronsDown,
  ChevronsUp
} from 'lucide-react';
import { SecurityTreeNode } from './useRoleTree';

import './RoleExplorerTree.scss';

interface RoleExplorerTreeProps {
  tree: SecurityTreeNode[];
  loading: boolean;
  onToggleExpand: (nodeId: string) => void;
  onExpandAll?: () => void;
  onCollapseAll?: () => void;
  onSearchChange?: (term: string) => void;
}

export function RoleExplorerTree({ 
  tree, 
  loading, 
  onToggleExpand, 
  onExpandAll, 
  onCollapseAll,
  onSearchChange
}: RoleExplorerTreeProps) {
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');

  const handleSearch = (term: string) => {
    setSearchTerm(term);
    onSearchChange?.(term);
  };

  const toggleFullscreen = () => {
    setIsFullscreen(!isFullscreen);
  };

  useEffect(() => {
    if (isFullscreen && onExpandAll) {
      onExpandAll();
    }
  }, [isFullscreen, onExpandAll]);

  if (loading) {
    return (
      <Flex align="center" justify="center" className="role-explorer-tree__loading">
        <Loader2 className="role-explorer-tree__spinner" size={24} />
        <Text size="2">Building security tree...</Text>
      </Flex>
    );
  }

  return (
    <Box className={`role-explorer-tree-container ${isFullscreen ? 'role-explorer-tree-container--fullscreen' : ''}`}>
      <Flex className="role-explorer-tree__toolbar" align="center" gap="3">
        <Box style={{ flex: 1 }}>
          <TextField.Root 
            placeholder="Search tree (Name, Permission, Repo...)" 
            size="2"
            value={searchTerm}
            onChange={(e) => handleSearch(e.target.value)}
          >
            <TextField.Slot>
              <Search size={16} />
            </TextField.Slot>
          </TextField.Root>
        </Box>

        <Separator orientation="vertical" size="1" />

        <Flex gap="2" align="center">
          <Tooltip content="Expand All">
            <IconButton type="button" variant="ghost" size="2" onClick={onExpandAll}>
              <ChevronsDown size={16} />
            </IconButton>
          </Tooltip>
          <Tooltip content="Collapse All">
            <IconButton type="button" variant="ghost" size="2" onClick={onCollapseAll}>
              <ChevronsUp size={16} />
            </IconButton>
          </Tooltip>
        </Flex>

        <Separator orientation="vertical" size="1" />

        <Tooltip content={isFullscreen ? "Exit Fullscreen" : "Fullscreen Mode"}>
          <IconButton type="button" variant="ghost" size="2" onClick={toggleFullscreen}>
            {isFullscreen ? <Minimize2 size={16} /> : <Maximize2 size={16} />}
          </IconButton>
        </Tooltip>
      </Flex>

      <Box className="role-explorer-tree__header">
        <Flex gap="4" p="2" className="role-explorer-tree__header-row">
          <Text size="1" weight="bold" className="role-explorer-tree__col-name">NAME</Text>
          <Text size="1" weight="bold" className="role-explorer-tree__col-permissions">PERMISSIONS</Text>
          <Text size="1" weight="bold" className="role-explorer-tree__col-resource">RESOURCE</Text>
          <Text size="1" weight="bold" className="role-explorer-tree__col-description">DESCRIPTION</Text>
        </Flex>
      </Box>

      <ScrollArea scrollbars="vertical" className="role-explorer-tree__scroll">
        {tree.length === 0 ? (
          <Box className="role-explorer-tree__empty">
            <Text size="2" color="gray">No matching security nodes found.</Text>
          </Box>
        ) : (
          <Box role="tree" aria-label="Role Security Explorer">
            {tree.map((node) => (
              <TreeNode 
                key={node.id} 
                node={node} 
                onToggleExpand={onToggleExpand} 
                searchTerm={searchTerm}
              />
            ))}
          </Box>
        )}
      </ScrollArea>
    </Box>
  );
}

interface TreeNodeProps {
  node: SecurityTreeNode;
  onToggleExpand: (nodeId: string) => void;
  depth?: number;
  searchTerm?: string;
}

function TreeNode({ node, onToggleExpand, depth = 0, searchTerm }: TreeNodeProps) {
  const hasChildren = node.children && node.children.length > 0;
  const isExpanded = node.expanded;

  const getIcon = () => {
    switch (node.type) {
      case 'role': return <Shield size={16} className="role-explorer-tree__icon--role" />;
      case 'privilege': return <Key size={16} className="role-explorer-tree__icon--privilege" />;
      case 'content-selector': return <Filter size={16} className="role-explorer-tree__icon--selector" />;
      default: return null;
    }
  };

  const highlightText = (text: string | undefined) => {
    if (!text || !searchTerm) return text;
    const parts = text.split(new RegExp(`(${searchTerm})`, 'gi'));
    return parts.map((part, i) => 
      part.toLowerCase() === searchTerm.toLowerCase() 
        ? <span key={i} className="role-explorer-tree__highlight">{part}</span> 
        : part
    );
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'ArrowRight' && hasChildren && !isExpanded) {
      onToggleExpand(node.id);
    } else if (e.key === 'ArrowLeft' && hasChildren && isExpanded) {
      onToggleExpand(node.id);
    }
  };

  return (
    <Box 
      className={`role-explorer-tree__node ${node.inherited ? 'role-explorer-tree__node--inherited' : ''}`}
    >
      <Flex 
        align="flex-start" 
        gap="4" 
        className="role-explorer-tree__node-content"
        role="treeitem"
        aria-expanded={hasChildren ? isExpanded : undefined}
        tabIndex={0}
        onKeyDown={handleKeyDown}
      >
        {/* Column 1: Name + Icon */}
        <Flex className="role-explorer-tree__col-name" style={{ paddingLeft: `${depth * 20}px` }} align="center" gap="2">
          <Box className="role-explorer-tree__expander">
            {hasChildren && (
              <IconButton 
                type="button"
                variant="ghost" 
                size="1" 
                onClick={(e) => { e.stopPropagation(); onToggleExpand(node.id); }}
                aria-label={isExpanded ? 'Collapse' : 'Expand'}
              >
                {isExpanded ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
              </IconButton>
            )}
          </Box>

          <Box className="role-explorer-tree__type-icon">
            {getIcon()}
          </Box>

          <Flex align="center" gap="1">
            <Text size="2" weight="medium" className="role-explorer-tree__node-name">
              {highlightText(node.name)}
            </Text>
            {node.inherited && (
              <Text size="1" color="gray" className="role-explorer-tree__inherited-label">
                (inherited)
              </Text>
            )}
            {node.isCircular && (
              <Tooltip content="Circular role reference detected. Inheritance stops here.">
                <AlertTriangle size={14} className="role-explorer-tree__icon--warning" />
              </Tooltip>
            )}
          </Flex>
        </Flex>

        {/* Column 2: Permissions (Actions) */}
        <Box className="role-explorer-tree__col-permissions">
          {node.actions && (
            <Box className="role-explorer-tree__badge role-explorer-tree__badge--actions">
              <Text size="1">{highlightText(node.actions)}</Text>
            </Box>
          )}
        </Box>

        {/* Column 3: Resource (Repository) */}
        <Box className="role-explorer-tree__col-resource">
          {node.repository && (
            <Box className="role-explorer-tree__badge role-explorer-tree__badge--repo">
              <Text size="1">{highlightText(node.repository)}</Text>
            </Box>
          )}
          {node.type === 'content-selector' && node.csel && (
            <Tooltip content={node.csel}>
              <Box className="role-explorer-tree__badge role-explorer-tree__badge--csel">
                <Text size="1">{highlightText(node.csel)}</Text>
              </Box>
            </Tooltip>
          )}
        </Box>

        {/* Column 4: Description */}
        <Box className="role-explorer-tree__col-description">
          <Text size="1" color="gray" className="role-explorer-tree__description">
            {highlightText(node.description)}
          </Text>
        </Box>
      </Flex>

      {hasChildren && isExpanded && (
        <Box className="role-explorer-tree__children">
          {node.children!.map((child) => (
            <TreeNode 
              key={child.id} 
              node={child} 
              onToggleExpand={onToggleExpand} 
              depth={depth + 1} 
              searchTerm={searchTerm}
            />
          ))}
        </Box>
      )}
    </Box>
  );
}
