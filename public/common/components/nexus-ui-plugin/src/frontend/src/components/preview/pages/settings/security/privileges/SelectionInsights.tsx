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

import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { createPortal } from 'react-dom';
import { Box, Button, Card, Flex, Text, Heading, Badge, ScrollArea, Table, TextField, IconButton, Tooltip } from '@radix-ui/themes';
import {
  ExternalLink,
  Activity,
  Server,
  ChevronRight,
  ChevronDown,
  Loader2,
  File,
  Folder,
  Eye,
  Search as SearchIcon,
  FileText,
  Minimize2,
} from 'lucide-react';
import { FormatIcon } from '../../repository/repositories/components/FormatIcon';
import { interpretExpression } from '../../repository/selectors/cselValidator';
import { useContentSelectorsApi } from '../../repository/selectors/useContentSelectorsApi';
import { fetchBrowseNodes } from '../../../browse/browse.api';

import './SelectionInsights.scss';

interface SelectionInsightsProps {
  repository?: {
    name: string;
    format: string;
    type: string;
    status?: { online: boolean } | null;
  } | null;
  allRepositories?: boolean;
  selectedFormat?: string | null;
  contentSelector?: {
    name: string;
    expression: string;
    description: string;
  } | null;
}

// --- Content Tree Components ---

interface FileTreeNode {
  id: string;
  text: string;
  type: 'folder' | 'component' | 'asset';
  leaf: boolean;
  children?: FileTreeNode[];
  expanded?: boolean;
}

const ContentTreeNode = ({ 
  node, 
  depth, 
  matchingPaths, 
  onToggle, 
  repositoryName 
}: { 
  node: FileTreeNode, 
  depth: number, 
  matchingPaths: Set<string>, 
  onToggle: (nodeId: string) => void,
  repositoryName: string
}) => {
  const isMatched = matchingPaths.has(node.id) || matchingPaths.has(`/${node.id}`);
  const hasChildren = !node.leaf;
  const isExpanded = node.expanded;

  return (
    <Box>
      <Flex 
        className={`selection-insights__file-node ${isMatched ? 'selection-insights__file-node--matched' : ''}`}
        align="center"
        gap="2"
        py="1"
        px="2"
        style={{ paddingLeft: `${depth * 16 + 8}px` }}
        onClick={() => hasChildren && onToggle(node.id)}
      >
        {node.type === 'folder' ? <Folder size={14} color="var(--gray-9)" /> : <File size={14} color="var(--blue-9)" />}
        <Text size="1" className="selection-insights__file-node-name">
          {node.text}
        </Text>
        {isMatched && <Badge color="green" size="1">Match</Badge>}
        <Box className="selection-insights__file-node-chevron-wrapper">
          {hasChildren && (isExpanded ? <ChevronDown size={12} /> : <ChevronRight size={12} />)}
        </Box>
      </Flex>
      {isExpanded && node.children && (
        <Box>
          {node.children.map(child => (
            <ContentTreeNode 
              key={child.id} 
              node={child} 
              depth={depth + 1} 
              matchingPaths={matchingPaths} 
              onToggle={onToggle}
              repositoryName={repositoryName}
            />
          ))}
        </Box>
      )}
    </Box>
  );
};

/**
 * FullscreenMatchTable — polished table of matching paths inside the fullscreen overlay.
 * Features: search/filter, alphabetical sort, file icon, row count, empty state.
 */
function FullscreenMatchTable({
  paths,
  expression,
}: {
  paths: Set<string>;
  expression: string;
}) {
  const [filter, setFilter] = useState('');

  const sorted = useMemo(
    () => Array.from(paths).sort((a, b) => a.localeCompare(b)),
    [paths]
  );

  const filtered = useMemo(() => {
    const q = filter.trim().toLowerCase();
    return q ? sorted.filter((p) => p.toLowerCase().includes(q)) : sorted;
  }, [sorted, filter]);

  return (
    <Box className="selection-insights__fullscreen-table-wrap">
      {/* Toolbar */}
      <Flex
        align="center"
        gap="3"
        className="selection-insights__fullscreen-toolbar"
        justify="between"
      >
        <TextField.Root
          placeholder="Filter paths…"
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
          className="selection-insights__fullscreen-search"
          aria-label="Filter matching paths"
          data-testid="match-filter-input"
        >
          <TextField.Slot>
            <SearchIcon size={14} aria-hidden="true" />
          </TextField.Slot>
        </TextField.Root>
        {filter && (
          <Text size="1" color="gray">
            {filtered.length} of {paths.size} shown
          </Text>
        )}
      </Flex>

      {/* Table */}
      <ScrollArea scrollbars="vertical" className="selection-insights__fullscreen-scroll">
        <Table.Root
          variant="surface"
          className="selection-insights__fullscreen-table"
          data-testid="match-items-table"
        >
          <Table.Header>
            <Table.Row>
              <Table.ColumnHeaderCell>Path</Table.ColumnHeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {filtered.length === 0 ? (
              <Table.Row>
                <Table.Cell>
                  <Flex align="center" justify="center" py="6" gap="2" direction="column">
                    <SearchIcon size={24} color="var(--gray-8)" aria-hidden="true" />
                    <Text size="2" color="gray" data-testid="match-empty-state">
                      {filter
                        ? `No paths match "${filter}"`
                        : 'No matching items'}
                    </Text>
                  </Flex>
                </Table.Cell>
              </Table.Row>
            ) : (
              filtered.map((path) => (
                <Table.Row key={path} className="selection-insights__fullscreen-table-row">
                  <Table.Cell>
                    <Flex align="center" gap="2">
                      <File
                        size={14}
                        color="var(--accent-9)"
                        aria-hidden="true"
                        className="selection-insights__fullscreen-file-icon"
                      />
                      <Text
                        size="2"
                        className="selection-insights__fullscreen-path-text"
                        data-testid="match-path-row"
                      >
                        {path}
                      </Text>
                    </Flex>
                  </Table.Cell>
                </Table.Row>
              ))
            )}
          </Table.Body>
        </Table.Root>
      </ScrollArea>
    </Box>
  );
}

export function SelectionInsights({ repository, allRepositories, selectedFormat, contentSelector }: SelectionInsightsProps) {
  const [matchCount, setMatchCount] = useState<number | null>(null);
  const [matchingPaths, setMatchingPaths] = useState<Set<string>>(new Set());
  const [loadingMetrics, setLoadingMetrics] = useState(false);
  const [showFlyout, setShowFlyout] = useState(false);

  // Collapse sidebar when Full Match List opens (like Repository Tree fullscreen)
  useEffect(() => {
    if (showFlyout) {
      window.dispatchEvent(new CustomEvent('nx-sidebar-toggle', { detail: { open: false } }));
    } else {
      window.dispatchEvent(new CustomEvent('nx-sidebar-toggle', { detail: { open: true } }));
    }
    return () => {
      window.dispatchEvent(new CustomEvent('nx-sidebar-toggle', { detail: { open: true } }));
    };
  }, [showFlyout]);

  // Escape key closes Full Match List
  useEffect(() => {
    if (!showFlyout) return;
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setShowFlyout(false);
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [showFlyout]);
  
  const [fileTree, setFileTree] = useState<FileTreeNode[]>([]);
  const [treeLoading, setTreeLoading] = useState(false);

  const { previewContentSelector } = useContentSelectorsApi();

  const repoName = allRepositories ? '*' : repository?.name;

  // Load root file tree nodes
  useEffect(() => {
    if (repository?.name) {
      setTreeLoading(true);
      fetchBrowseNodes({ repositoryName: repository.name, node: '/' })
        .then(nodes => {
          setFileTree(nodes.map(n => ({ ...n, expanded: false } as FileTreeNode)));
        })
        .finally(() => setTreeLoading(false));
    } else {
      setFileTree([]);
    }
  }, [repository?.name]);

  // Load matches and highlighting
  useEffect(() => {
    if (repoName && contentSelector?.expression) {
      setLoadingMetrics(true);
      previewContentSelector(repoName, 'csel', contentSelector.expression)
        .then((results) => {
          if (Array.isArray(results)) {
            setMatchCount(results.length);
            setMatchingPaths(new Set(results));
          }
        })
        .catch(() => {
          setMatchCount(null);
          setMatchingPaths(new Set());
        })
        .finally(() => setLoadingMetrics(false));
    } else {
      setMatchCount(null);
      setMatchingPaths(new Set());
    }
  }, [repoName, contentSelector?.expression, previewContentSelector]);

  const handleToggleFileNode = useCallback(async (nodeId: string) => {
    setFileTree(prev => {
      const updateNodes = (nodes: FileTreeNode[]): FileTreeNode[] => {
        return nodes.map(node => {
          if (node.id === nodeId) {
            const nextExpanded = !node.expanded;
            if (nextExpanded && !node.children && repository?.name) {
              // Trigger async fetch but return node with expanded: true
              fetchBrowseNodes({ repositoryName: repository.name, node: nodeId })
                .then(children => {
                  setFileTree(current => {
                    const addChildren = (currentNodes: FileTreeNode[]): FileTreeNode[] => {
                      return currentNodes.map(n => {
                        if (n.id === nodeId) return { ...n, children: children as FileTreeNode[] };
                        if (n.children) return { ...n, children: addChildren(n.children) };
                        return n;
                      });
                    };
                    return addChildren(current);
                  });
                });
            }
            return { ...node, expanded: nextExpanded };
          }
          if (node.children) return { ...node, children: updateNodes(node.children) };
          return node;
        });
      };
      return updateNodes(prev);
    });
  }, [repository?.name]);

  const interpretation = contentSelector?.expression
    ? interpretExpression(contentSelector.expression)
    : null;
  const isOnline = allRepositories ? true : (repository?.status?.online ?? true);

  return (
    <Box className="selection-insights__sidecar">
      <Flex align="center" gap="2" mb="4">
        <Eye size={18} color="var(--accent-9)" />
        <Heading size="3">Selection Insights</Heading>
      </Flex>
      
      <Flex direction="column" gap="4">
        {/* 1. Context Card */}
        {repoName ? (
          <Card size="2">
            <Flex align="center" gap="3" mb="3">
              {allRepositories ? (
                <div className="selection-insights__all-repos-icon">
                  <Server size={20} />
                </div>
              ) : (
                <FormatIcon format={repository?.format || ''} type={repository?.type as any} size={20} />
              )}
              <Box style={{ flex: 1 }}>
                <Text size="2" weight="bold" style={{ display: 'block' }}>
                  {allRepositories ? 'All Repositories' : repository?.name}
                </Text>
                <Flex align="center" gap="2">
                  <Text size="1" color="gray">
                    {selectedFormat && selectedFormat !== '*' 
                      ? `${selectedFormat.toUpperCase()} Format` 
                      : 'Global Scope'}
                  </Text>
                  {!allRepositories && (
                    <Badge color={isOnline ? 'green' : 'red'} size="1">
                      {isOnline ? 'Online' : 'Offline'}
                    </Badge>
                  )}
                </Flex>
              </Box>
            </Flex>

            {/* Content Browser (Live File Tree) */}
            {!allRepositories && (
              <Box className="selection-insights__content-tree">
                <Box className="selection-insights__content-tree-header">
                  <Flex align="center" gap="2">
                    <SearchIcon size={12} color="var(--gray-9)" />
                    <Text size="1" weight="bold" color="gray">LIVE CONTENT BROWSER</Text>
                  </Flex>
                </Box>
                <ScrollArea scrollbars="vertical" className="selection-insights__content-tree-scroll">
                  {treeLoading ? (
                    <Flex align="center" justify="center" p="4">
                      <Loader2 size={16} className="selection-insights__spinner" />
                    </Flex>
                  ) : fileTree.length === 0 ? (
                    <Box p="4" style={{ textAlign: 'center' }}>
                      <Text size="1" color="gray">Repository is empty or inaccessible.</Text>
                    </Box>
                  ) : (
                    <Box py="1">
                      {fileTree.map(node => (
                        <ContentTreeNode 
                          key={node.id} 
                          node={node} 
                          depth={0} 
                          matchingPaths={matchingPaths} 
                          onToggle={handleToggleFileNode}
                          repositoryName={repository?.name || ''}
                        />
                      ))}
                    </Box>
                  )}
                </ScrollArea>
              </Box>
            )}
          </Card>
        ) : (
          <Box className="selection-insights__placeholder">
            <Text size="2">Select Format and Repository to preview content.</Text>
          </Box>
        )}

        {/* 2. Selector Interpretation */}
        {contentSelector && (
          <Card size="2">
            <Text size="1" weight="bold" color="gray" mb="2" style={{ display: 'block', textTransform: 'uppercase' }}>
              Selector Logic
            </Text>
            <Box mb="3">
              <Text size="2" weight="bold" mb="1" style={{ display: 'block' }}>{contentSelector.name}</Text>
              {interpretation?.success && (
                <Text size="2" color="gray" style={{ lineHeight: '1.4' }}>{interpretation.text}</Text>
              )}
            </Box>

            <Box className="selection-insights__expression">
              <pre className="selection-insights__code">
                <code>{contentSelector.expression}</code>
              </pre>
            </Box>
          </Card>
        )}

        {/* 3. Live Impact Analysis */}
        {repoName && contentSelector && (
          <Card size="2" style={{ border: '1px solid var(--accent-6)', background: 'var(--accent-2)' }}>
            <Text size="1" weight="bold" color="indigo" mb="2" style={{ display: 'block', textTransform: 'uppercase' }}>
              Live Impact Analysis
            </Text>
            <Flex align="center" gap="3">
              <Activity size={20} color="var(--accent-9)" />
              <Box>
                {loadingMetrics ? (
                  <Text size="2" color="gray">Calculating matches...</Text>
                ) : (
                  <>
                    <Text size="3" weight="bold">{matchCount ?? 0} matches</Text>
                    <Text size="1" color="gray" style={{ display: 'block' }}>
                      Items matching in <strong>{allRepositories ? 'all repositories' : repository?.name}</strong>.
                    </Text>
                  </>
                )}
              </Box>
            </Flex>

            {(matchCount ?? 0) > 0 && (
              <>
                <Button
                  type="button"
                  variant="soft"
                  color="gray"
                  mt="3"
                  style={{ width: '100%', justifyContent: 'center' }}
                  disabled={loadingMetrics}
                  onClick={() => setShowFlyout(true)}
                >
                  <ExternalLink size={14} />
                  {' '}
                  Full Match List
                </Button>
                {showFlyout && createPortal(
                  <div
                    className="selection-insights__fullscreen-overlay"
                    role="dialog"
                    aria-modal="true"
                    aria-labelledby="selection-insights-fullscreen-title"
                  >
                    <Box className="selection-insights__fullscreen-content">
                      {/* Header */}
                      <Flex
                        justify="between"
                        align="center"
                        className="selection-insights__fullscreen-header"
                      >
                        <Flex align="center" gap="3">
                          <FileText size={20} color="var(--accent-9)" aria-hidden="true" />
                          <Box>
                            <Flex align="center" gap="2">
                              <Heading id="selection-insights-fullscreen-title" size="4">
                                Matching Items
                              </Heading>
                              <Badge color="blue" variant="soft" size="2" data-testid="match-count-badge">
                                {matchingPaths.size} {matchingPaths.size === 1 ? 'item' : 'items'}
                              </Badge>
                            </Flex>
                            <Text size="2" color="gray" mt="1" as="p">
                              Items in{' '}
                              {allRepositories ? 'all repositories' : <strong>{repository?.name}</strong>}{' '}
                              matching <code className="selection-insights__inline-code">{contentSelector.expression}</code>
                            </Text>
                          </Box>
                        </Flex>
                        <Tooltip content="Exit fullscreen">
                          <IconButton
                            variant="ghost"
                            size="2"
                            onClick={() => setShowFlyout(false)}
                            aria-label="Close matching items"
                            className="selection-insights__fullscreen-close"
                          >
                            <Minimize2 size={16} aria-hidden="true" />
                          </IconButton>
                        </Tooltip>
                      </Flex>

                      {/* Toolbar: search + count */}
                      <FullscreenMatchTable
                        paths={matchingPaths}
                        expression={contentSelector.expression}
                      />
                    </Box>
                  </div>,
                  document.body
                )}
              </>
            )}
          </Card>
        )}
      </Flex>
    </Box>
  );
}

export default SelectionInsights;
