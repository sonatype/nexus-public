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

import React, { useCallback, useEffect, useRef } from 'react';
import { Box, Flex, Text, Tooltip, ScrollArea, Callout, TextField, IconButton, Switch, Separator } from '@radix-ui/themes';
import { 
  FolderTree, 
  Database, 
  Globe, 
  ChevronRight, 
  ChevronDown, 
  AlertTriangle,
  RefreshCw,
  Loader2,
  Search,
  ExternalLink,
  Settings,
  Copy,
  Check,
  Maximize2,
  Minimize2,
  Download,
  ChevronsDown,
  ChevronsUp
} from 'lucide-react';
import { 
  StatusBadge, 
  LoadingState, 
  ErrorState,
  useToast
} from '../../../../shared';
import { useRepositoryTree, RepositoryTreeNode } from './useRepositoryTree';
import {
  useStructureComponentSearch,
  getAllMemberRepos,
} from './useStructureComponentSearch';
import './RepositoryStructureTree.scss';

export interface RepositoryStructureTreeProps {
  repositoryName: string;
}

/**
 * Maps repository node status to StatusBadge status types.
 */
function mapStatus(status: string): any {
  switch (status) {
    case 'online': return 'online';
    case 'offline': return 'offline';
    case 'blocked': return 'warning';
    case 'out-of-service': return 'error';
    default: return 'unknown';
  }
}

/**
 * Renders an icon based on repository type.
 */
function TypeIcon({ type, size = 16 }: { type: string, size?: number }) {
  const baseClass = 'repo-type-icon';
  switch (type) {
    case 'group': return <FolderTree size={size} className={`${baseClass} ${baseClass}--group`} />;
    case 'hosted': return <Database size={size} className={`${baseClass} ${baseClass}--hosted`} />;
    case 'proxy': return <Globe size={size} className={`${baseClass} ${baseClass}--proxy`} />;
    default: return <Database size={size} className={baseClass} />;
  }
}

interface TreeNodeProps {
  node: RepositoryTreeNode;
  depth: number;
  expandedIds: Set<string>;
  onToggle: (id: string) => void;
  focusedId?: string;
  setFocusedId: (id: string) => void;
  filterText: string;
  highlightedNodeIds: Set<string>;
  showIssuesOnly: boolean;
}

const TreeNode = ({
  node,
  depth,
  expandedIds,
  onToggle,
  focusedId,
  setFocusedId,
  filterText,
  highlightedNodeIds,
  showIssuesOnly,
}: TreeNodeProps) => {
  const isExpanded = expandedIds.has(node.id);
  const isFocused = focusedId === node.id;
  const hasChildren = node.children && node.children.length > 0;
  const toast = useToast();
  const [copied, setCopied] = React.useState(false);

  const textMatch =
    filterText &&
    (node.name.toLowerCase().includes(filterText.toLowerCase()) ||
      node.blobStore?.toLowerCase().includes(filterText.toLowerCase()));
  const componentMatch = highlightedNodeIds.has(node.id);
  const isMatch = textMatch || componentMatch;

  // Issues are nodes that are not 'online'
  const isIssue = node.status !== 'online';

  // Logic: Show node if:
  // 1. Issues only is OFF
  // 2. OR Issues only is ON AND (the node is an issue OR it has children that contain an issue)
  const hasIssueInDescendants = (n: RepositoryTreeNode): boolean => {
    if (n.status !== 'online') return true;
    if (n.children) {
      return n.children.some(child => hasIssueInDescendants(child));
    }
    return false;
  };

  const isVisible = !showIssuesOnly || isIssue || hasIssueInDescendants(node);

  if (!isVisible) return null;

  const handleToggle = (e: React.MouseEvent) => {
    if (node.type === 'group') {
      e.stopPropagation();
      onToggle(node.id);
    }
  };

  const handleFocus = () => {
    setFocusedId(node.id);
  };

  const goToProfile = (e: React.MouseEvent) => {
    e.stopPropagation();
    window.location.hash = `preview/admin/repository/repositories/${encodeURIComponent(node.name)}/profile`;
  };

  const goToSettings = (e: React.MouseEvent) => {
    e.stopPropagation();
    window.location.hash = `preview/admin/repository/repositories/${encodeURIComponent(node.name)}`;
  };

  const copyUrl = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (node.remoteUrl || node.name) {
      const url = node.remoteUrl || `${window.location.origin}/repository/${node.name}`;
      navigator.clipboard.writeText(url);
      setCopied(true);
      toast.success(`URL copied for ${node.name}`);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  const goToBlobStore = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (node.blobStore) {
      window.location.hash = `preview/admin/repository/blobstores/${encodeURIComponent(node.blobStore)}`;
    }
  };

  return (
    <Box className={`repository-tree-node-container ${isMatch ? 'repository-tree-node--match' : ''}`} role="none">
      <Flex 
        className={`repository-tree-node ${isFocused ? 'repository-tree-node--focused' : ''}`}
        align="center"
        gap="2"
        p="2"
        style={{ paddingLeft: `${depth * 20 + 8}px` }}
        role="treeitem"
        aria-expanded={node.type === 'group' ? isExpanded : undefined}
        aria-level={depth + 1}
        tabIndex={isFocused ? 0 : -1}
        onClick={handleToggle}
        onFocus={handleFocus}
      >
        <div style={{ width: '16px', height: '16px' }} className="repository-tree-node__chevron">
          {node.type === 'group' && (
            node.isLoading ? (
              <Loader2 size={12} className="repository-tree-node__spinner" />
            ) : (
              isExpanded ? <ChevronDown size={14} /> : <ChevronRight size={14} />
            )
          )}
        </div>
        
        <TypeIcon type={node.type} />
        
        <Text weight={isMatch ? "bold" : "medium"} size="2" className="repository-tree-node__name">
          {node.name}
        </Text>
        
        <StatusBadge status={mapStatus(node.status)} size="small" />

        {node.blobStore && (
          <Tooltip content="Go to Blob Store settings">
            <Text 
              color="gray" 
              size="1" 
              className="repository-tree-node__blobstore"
              onClick={goToBlobStore}
              style={{ cursor: 'pointer', textDecoration: 'underline' }}
            >
              [{node.blobStore}]
            </Text>
          </Tooltip>
        )}

        {node.type === 'proxy' && node.remoteUrl && (
          <Tooltip content="Open remote URL in new tab">
            <a
              href={node.remoteUrl}
              target="_blank"
              rel="noopener noreferrer"
              onClick={(e) => e.stopPropagation()}
              aria-label="Open remote URL in new tab"
              title={node.remoteUrl.length > 40 ? node.remoteUrl : undefined}
              className="repository-tree-node__remote-url-link"
            >
              {node.remoteUrl.length > 40
                ? `${node.remoteUrl.slice(0, 40)}…`
                : node.remoteUrl}
              <ExternalLink size={12} style={{ marginLeft: '4px', flexShrink: 0 }} aria-hidden />
            </a>
          </Tooltip>
        )}

        <Flex gap="2" className="repository-tree-node__actions" ml="auto" pr="2">

          <Tooltip content="Copy URL">
            <Box onClick={copyUrl} style={{ cursor: 'pointer', display: 'flex', alignItems: 'center' }}>
              {copied ? <Check size={14} color="var(--green-9)" /> : <Copy size={14} className="repository-tree-node__action-icon" />}
            </Box>
          </Tooltip>

          <Tooltip content="View Profile">
            <IconButton variant="ghost" size="1" onClick={goToProfile} className="repository-tree-node__action-btn">
              <ExternalLink size={14} />
            </IconButton>
          </Tooltip>

          <Tooltip content="Edit Settings">
            <IconButton variant="ghost" size="1" onClick={goToSettings} className="repository-tree-node__action-btn">
              <Settings size={14} />
            </IconButton>
          </Tooltip>

          {node.isCircular && (
            <Tooltip content="Circular dependency detected">
              <AlertTriangle size={14} color="var(--orange-9)" data-testid="circular-warning" />
            </Tooltip>
          )}
        </Flex>
      </Flex>

      {hasChildren && isExpanded && (
        <Box role="group">
          {node.children!.map((child) => (
            <TreeNode
              key={child.id}
              node={child}
              depth={depth + 1}
              expandedIds={expandedIds}
              onToggle={onToggle}
              focusedId={focusedId}
              setFocusedId={setFocusedId}
              filterText={filterText}
              highlightedNodeIds={highlightedNodeIds}
              showIssuesOnly={showIssuesOnly}
            />
          ))}
        </Box>
      )}
    </Box>
  );
};

/** Collect node ids where node.name is in repoNames (for component-match highlight). */
function getNodeIdsForRepos(
  nodes: RepositoryTreeNode[],
  repoNames: Set<string>,
  result: Set<string> = new Set()
): Set<string> {
  for (const node of nodes) {
    if (repoNames.has(node.name)) {
      result.add(node.id);
    }
    if (node.children) {
      getNodeIdsForRepos(node.children, repoNames, result);
    }
  }
  return result;
}

/** Collect node ids that match filterText (name or blob store). */
function getTextMatchNodeIds(
  nodes: RepositoryTreeNode[],
  filterText: string,
  result: Set<string> = new Set()
): Set<string> {
  if (!filterText.trim()) return result;
  const q = filterText.toLowerCase();
  for (const node of nodes) {
    if (
      node.name.toLowerCase().includes(q) ||
      node.blobStore?.toLowerCase().includes(q)
    ) {
      result.add(node.id);
    }
    if (node.children) {
      getTextMatchNodeIds(node.children, filterText, result);
    }
  }
  return result;
}

/**
 * RepositoryStructureTree displays a recursive membership tree for group repositories.
 */
export function RepositoryStructureTree({ repositoryName }: RepositoryStructureTreeProps) {
  const {
    tree,
    loading,
    expanding,
    error,
    expandedIds,
    toggleExpand,
    expandAll,
    collapseAll,
    revealIssues,
    setExpandedIds,
    refresh,
  } = useRepositoryTree(repositoryName);
  const [focusedId, setFocusedId] = React.useState<string | undefined>(undefined);
  const [filterText, setFilterText] = React.useState('');
  const [showIssuesOnly, setShowIssuesOnly] = React.useState(false);
  const [isFullscreen, setIsFullscreen] = React.useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  const memberRepos = React.useMemo(
    () => getAllMemberRepos(tree),
    [tree]
  );
  const { reposWithMatches, loading: componentSearchLoading, error: componentSearchError } =
    useStructureComponentSearch(repositoryName, filterText, memberRepos, tree);

  const highlightedNodeIds = React.useMemo(() => {
    const textIds = getTextMatchNodeIds(tree, filterText);
    const componentIds = getNodeIdsForRepos(tree, reposWithMatches);
    const merged = new Set(textIds);
    componentIds.forEach((id) => merged.add(id));
    return merged;
  }, [tree, filterText, reposWithMatches]);

  const toggleFullscreen = useCallback(() => {
    setIsFullscreen((prev) => !prev);
  }, []);

  // Automatically collapse/expand left nav when entering/leaving full screen
  useEffect(() => {
    if (isFullscreen) {
      window.dispatchEvent(new CustomEvent('nx-sidebar-toggle', { detail: { open: true } }));
    } else {
      window.dispatchEvent(new CustomEvent('nx-sidebar-toggle', { detail: { open: false } }));
    }
  }, [isFullscreen]);

  // Auto-expand full tree when entering fullscreen (only on transition, not when expandAll identity changes)
  const prevFullscreenRef = useRef(false);
  useEffect(() => {
    if (isFullscreen && !prevFullscreenRef.current && tree.length > 0) {
      expandAll();
    }
    prevFullscreenRef.current = isFullscreen;
  }, [isFullscreen, tree.length, expandAll]);

  // Handle "Issues only" toggle
  const handleIssuesToggle = useCallback((checked: boolean) => {
    setShowIssuesOnly(checked);
    if (checked) {
      revealIssues();
    }
  }, [revealIssues]);

  // Initialize focusedId once tree is loaded
  React.useEffect(() => {
    if (tree.length > 0 && !focusedId) {
      setFocusedId(tree[0].id);
    }
  }, [tree, focusedId]);

  // Flatten tree for CSV export
  const exportToCSV = useCallback(() => {
    const rows: string[][] = [['Level', 'Name', 'Type', 'Format', 'Status', 'Blob Store', 'Remote URL']];
    
    const traverse = (nodes: RepositoryTreeNode[], depth: number) => {
      for (const node of nodes) {
        rows.push([
          depth.toString(),
          node.name,
          node.type,
          node.format,
          node.status,
          node.blobStore || '',
          node.remoteUrl || ''
        ]);
        if (node.children) {
          traverse(node.children, depth + 1);
        }
      }
    };

    traverse(tree, 0);

    const csvContent = rows.map(e => e.map(cell => `"${cell.replace(/"/g, '""')}"`).join(",")).join("\n");
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement("a");
    const url = URL.createObjectURL(blob);
    link.setAttribute("href", url);
    link.setAttribute("download", `${repositoryName}_structure.csv`);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }, [tree, repositoryName]);

  // Flatten visible nodes for keyboard navigation
  const getVisibleNodes = useCallback((nodes: RepositoryTreeNode[], result: RepositoryTreeNode[] = []) => {
    for (const node of nodes) {
      result.push(node);
      if (expandedIds.has(node.id) && node.children) {
        getVisibleNodes(node.children, result);
      }
    }
    return result;
  }, [expandedIds]);

  const handleKeyDown = (e: React.KeyboardEvent) => {
    const visibleNodes = getVisibleNodes(tree);
    const currentIndex = visibleNodes.findIndex(n => n.id === focusedId);

    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault();
        if (currentIndex < visibleNodes.length - 1) {
          setFocusedId(visibleNodes[currentIndex + 1].id);
        }
        break;
      case 'ArrowUp':
        e.preventDefault();
        if (currentIndex > 0) {
          setFocusedId(visibleNodes[currentIndex - 1].id);
        }
        break;
      case 'ArrowRight':
        e.preventDefault();
        if (focusedId) {
          const node = visibleNodes.find(n => n.id === focusedId);
          if (node?.type === 'group') {
            if (!expandedIds.has(focusedId)) {
              toggleExpand(focusedId);
            } else if (currentIndex < visibleNodes.length - 1) {
              setFocusedId(visibleNodes[currentIndex + 1].id);
            }
          }
        }
        break;
      case 'ArrowLeft':
        e.preventDefault();
        if (focusedId) {
          const node = visibleNodes.find(n => n.id === focusedId);
          if (node?.type === 'group' && expandedIds.has(focusedId)) {
            toggleExpand(focusedId);
          } else {
            // Focus parent
            const parts = focusedId.split('::');
            if (parts.length > 1) {
              parts.pop();
              setFocusedId(parts.join('::'));
            }
          }
        }
        break;
    }
  };

  if (loading) return <LoadingState message="Loading structure..." />;
  if (error) return <ErrorState message={error} onRetry={refresh} />;

  const showEmptyState =
    filterText.trim().length > 0 &&
    !componentSearchLoading &&
    highlightedNodeIds.size === 0;

  const treeContent = (
    <Box
      ref={containerRef}
      role="tree"
      aria-label={`${repositoryName} structure`}
      onKeyDown={handleKeyDown}
      tabIndex={0}
      className="repository-tree-container"
    >
      {showEmptyState ? (
        <Box p="4" style={{ textAlign: 'center' }}>
          <Text size="2" color="gray">
            No matches for &quot;{filterText}&quot; in repo names, blob stores, or components
          </Text>
        </Box>
      ) : (
        <>
      {tree.map((node) => (
        <TreeNode
          key={node.id}
          node={node}
          depth={0}
          expandedIds={expandedIds}
          onToggle={toggleExpand}
          focusedId={focusedId}
          setFocusedId={setFocusedId}
          filterText={filterText}
          highlightedNodeIds={highlightedNodeIds}
          showIssuesOnly={showIssuesOnly}
        />
      ))}
        </>
      )}
    </Box>
  );

  const sharedToolbar = (
    <Flex gap="3" align="center" className={isFullscreen ? '' : 'repository-tree-toolbar'}>
      <Box style={{ flex: 1, position: 'relative' }}>
        <TextField.Root
          placeholder="Search structure (repo, blob store, or component)..."
          value={filterText}
          onChange={(e) => setFilterText(e.target.value)}
          size="2"
        >
          <TextField.Slot>
            {componentSearchLoading ? (
              <Loader2 size={16} className="repository-tree-node__spinner" />
            ) : (
              <Search size={16} />
            )}
          </TextField.Slot>
        </TextField.Root>
      </Box>

      <Flex align="center" gap="2">
        <Text size="1" color="gray">Issues only</Text>
        <Switch 
          size="1" 
          checked={showIssuesOnly} 
          onCheckedChange={handleIssuesToggle} 
        />
      </Flex>

      <Separator orientation="vertical" size="1" />

      <Flex gap="2" align="center">
        <Tooltip content="Expand All">
          <IconButton variant="ghost" size="2" onClick={expandAll} disabled={expanding}>
            {expanding ? <Loader2 size={16} className="repository-tree-node__spinner" /> : <ChevronsDown size={16} />}
          </IconButton>
        </Tooltip>
        <Tooltip content="Collapse All">
          <IconButton variant="ghost" size="2" onClick={collapseAll}>
            <ChevronsUp size={16} />
          </IconButton>
        </Tooltip>
        <Separator orientation="vertical" size="1" />
        <Tooltip content="Export to CSV">
          <IconButton variant="ghost" size="2" onClick={exportToCSV}>
            <Download size={16} />
          </IconButton>
        </Tooltip>
        <Tooltip content={isFullscreen ? "Exit Fullscreen" : "Fullscreen Mode"}>
          <IconButton variant="ghost" size="2" onClick={toggleFullscreen}>
            {isFullscreen ? <Minimize2 size={16} /> : <Maximize2 size={16} />}
          </IconButton>
        </Tooltip>
        <Tooltip content="Refresh tree">
          <IconButton variant="ghost" onClick={refresh} size="2">
            <RefreshCw size={16} />
          </IconButton>
        </Tooltip>
      </Flex>
    </Flex>
  );

  return (
    <Box className="repository-structure-tree" p="4">
      <Box
        className={`repository-structure-tree-container ${isFullscreen ? 'repository-structure-tree-container--fullscreen' : ''}`}
      >
        <Flex direction="column" gap="3" style={{ height: isFullscreen ? '100%' : undefined }}>
          {!isFullscreen && (
            <Callout.Root color="blue" size="1">
              <Callout.Icon>
                <FolderTree size={16} />
              </Callout.Icon>
              <Callout.Text>
                Visualizing membership hierarchy for group <strong>{repositoryName}</strong>.
                Use arrow keys to navigate and expand/collapse levels.
              </Callout.Text>
            </Callout.Root>
          )}

          <Box
            style={
              isFullscreen
                ? { padding: 'var(--space-2)', borderBottom: '1px solid var(--gray-4)', background: 'var(--color-surface)' }
                : { position: 'sticky', top: '-16px', zIndex: 10, background: 'var(--color-surface)', pt: '2px', pb: '16px' }
            }
          >
            {sharedToolbar}
          </Box>

          <ScrollArea
            scrollbars="vertical"
            style={{
              flex: isFullscreen ? 1 : undefined,
              minHeight: isFullscreen ? 0 : undefined,
              maxHeight: isFullscreen ? undefined : '600px',
            }}
            className="repository-structure-tree__scroll"
          >
            {treeContent}
          </ScrollArea>
        </Flex>
      </Box>
    </Box>
  );
}

export default RepositoryStructureTree;
