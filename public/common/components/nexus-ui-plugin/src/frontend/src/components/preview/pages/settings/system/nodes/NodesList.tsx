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

import React, { useState, useEffect } from 'react';
import { Box, Flex, Text, Table, Badge, Tooltip } from '@radix-ui/themes';
import { Loader2, AlertCircle } from 'lucide-react';
import { NodeInfo } from './types';
import { useNodesApi } from './useNodesApi';

import './NodesList.scss';

interface NodesListProps {
  refreshKey?: number;
}

/**
 * NodesList - Displays nodes in a table
 */
export function NodesList({ refreshKey = 0 }: NodesListProps) {
  const { fetchNodes } = useNodesApi();
  const [nodes, setNodes] = useState<NodeInfo[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Load nodes
  useEffect(() => {
    const loadNodes = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await fetchNodes();
        setNodes(data);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load nodes');
      } finally {
        setLoading(false);
      }
    };

    loadNodes();
  }, [fetchNodes, refreshKey]);

  if (loading) {
    return (
      <Flex align="center" justify="center" className="nodes-list__loading" aria-live="polite" aria-busy="true">
        <Loader2 size={24} className="nodes-list__spinner" aria-hidden="true" />
        <Text size="2">Loading nodes...</Text>
      </Flex>
    );
  }

  if (error) {
    return (
      <Box className="nodes-list__error">
        <AlertCircle size={20} aria-hidden="true" />
        <Text size="2">{error}</Text>
      </Box>
    );
  }

  return (
    <Box className="nodes-list">
      <Table.Root className="nodes-list__table" variant="surface">
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeaderCell className="nodes-list__th">Node Name</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell className="nodes-list__th">Node Identity</Table.ColumnHeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {nodes.length === 0 ? (
            <Table.Row>
              <Table.Cell colSpan={2} className="nodes-list__empty">
                No nodes found
              </Table.Cell>
            </Table.Row>
          ) : (
            nodes.map((node) => (
              <Table.Row key={node.name} className="nodes-list__row">
                <Table.Cell className="nodes-list__cell">
                  <Flex align="center" gap="2">
                    <Text weight="medium">{node.displayName || node.name}</Text>
                    {node.local && (
                      <Tooltip content="The node that you are currently connected to">
                        <Badge color="gray" variant="soft" size="1">
                          Current Node
                        </Badge>
                      </Tooltip>
                    )}
                  </Flex>
                </Table.Cell>
                <Table.Cell className="nodes-list__cell">
                  {node.name}
                </Table.Cell>
              </Table.Row>
            ))
          )}
        </Table.Body>
      </Table.Root>
    </Box>
  );
}

export default NodesList;
