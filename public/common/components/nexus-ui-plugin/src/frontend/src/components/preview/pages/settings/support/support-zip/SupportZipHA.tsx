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

import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Box, Flex, Text, Heading, Grid, Button } from '@radix-ui/themes';
import { Server, Loader2 } from 'lucide-react';

import { restClient, parseApiError } from '../../../../../../interface/api';
import { APIConstants } from '../../../../../../constants/APIConstants';
import { SettingsAlert } from '../../../../shared/form';
import { SupportZipNodeCard } from './SupportZipNodeCard';
import { SupportZipHaModal } from './SupportZipHaModal';
import { useSupportZipApi } from './useSupportZipApi';
import { NodeInfo, SupportZipParams } from './types';

import './SupportZipHA.scss';

const { REST } = APIConstants;
const POLL_INTERVAL_MS = 2000;

interface SupportZipHAProps {
  params: SupportZipParams;
  onParamChange: (name: keyof SupportZipParams, value: boolean | number) => void;
  disabled?: boolean;
}

/**
 * SupportZipHA - HA (clustered) support ZIP interface.
 *
 * Mirrors Classic UI's HA flow: fetches active nodes + blob-stores on mount,
 * renders a card per node, and supports per-node and parallel-all-nodes ZIP
 * generation via the internal /service/rest/internal/ui/supportzip/ endpoints.
 */
export function SupportZipHA({
  params,
  onParamChange,
  disabled = false,
}: SupportZipHAProps): React.ReactElement {
  const { fetchActiveNodes, fetchNodeStatus, generateForNode, clearNode } = useSupportZipApi();

  const [nodes, setNodes] = useState<NodeInfo[]>([]);
  const [isBlobStoreConfigured, setIsBlobStoreConfigured] = useState(false);
  const [initialLoading, setInitialLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [modalNode, setModalNode] = useState<NodeInfo | null>(null);
  const [modalAllNodes, setModalAllNodes] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);

  const pollersRef = useRef<Map<string, ReturnType<typeof setInterval>>>(new Map());

  const stopPolling = useCallback((nodeId: string) => {
    const handle = pollersRef.current.get(nodeId);
    if (handle) {
      clearInterval(handle);
      pollersRef.current.delete(nodeId);
    }
  }, []);

  const startPolling = useCallback(
    (nodeId: string) => {
      if (pollersRef.current.has(nodeId)) return;
      const handle = setInterval(async () => {
        try {
          const updated = await fetchNodeStatus(nodeId);
          setNodes((prev) => prev.map((n) => (n.nodeId === nodeId ? { ...n, ...updated } : n)));
          if (updated.status !== 'CREATING') {
            stopPolling(nodeId);
          }
        } catch {
          stopPolling(nodeId);
        }
      }, POLL_INTERVAL_MS);
      pollersRef.current.set(nodeId, handle);
    },
    [fetchNodeStatus, stopPolling]
  );

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const [activeNodes, blobStores] = await Promise.all([
          fetchActiveNodes(),
          restClient.get<unknown[]>(REST.PUBLIC.BLOB_STORES).catch(() => []),
        ]);
        if (cancelled) return;
        setNodes(activeNodes ?? []);
        setIsBlobStoreConfigured(Array.isArray(blobStores) && blobStores.length > 0);
        (activeNodes ?? []).forEach((n) => {
          if (n.status === 'CREATING') startPolling(n.nodeId);
        });
      } catch (err) {
        if (cancelled) return;
        setLoadError(parseApiError(err).message || 'Failed to load HA support ZIP data');
      } finally {
        if (!cancelled) setInitialLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [fetchActiveNodes, startPolling]);

  useEffect(() => {
    return () => {
      pollersRef.current.forEach((handle) => clearInterval(handle));
      pollersRef.current.clear();
    };
  }, []);

  const setNodeStatus = useCallback((nodeId: string, status: NodeInfo['status']) => {
    setNodes((prev) => prev.map((n) => (n.nodeId === nodeId ? { ...n, status } : n)));
  }, []);

  const generateOne = useCallback(
    async (node: NodeInfo) => {
      setNodeStatus(node.nodeId, 'CREATING');
      try {
        await clearNode(node.nodeId);
        const updated = await generateForNode(node.nodeId, params, node.hostname);
        setNodes((prev) =>
          prev.map((n) => (n.nodeId === node.nodeId ? { ...n, ...updated } : n))
        );
        if (updated.status === 'CREATING') {
          startPolling(node.nodeId);
        }
      } catch (err) {
        setNodeStatus(node.nodeId, 'FAILED');
        throw err;
      }
    },
    [clearNode, generateForNode, params, setNodeStatus, startPolling]
  );

  const handleOpenSingle = useCallback((node: NodeInfo) => {
    setModalNode(node);
    setModalAllNodes(false);
    setModalOpen(true);
  }, []);

  const handleOpenAll = useCallback(() => {
    setModalNode(null);
    setModalAllNodes(true);
    setModalOpen(true);
  }, []);

  const handleModalSubmit = useCallback(() => {
    setModalOpen(false);
    if (modalAllNodes) {
      const targets = nodes.filter((n) => n.status !== 'NODE_UNAVAILABLE');
      void Promise.all(targets.map((n) => generateOne(n).catch(() => undefined)));
    } else if (modalNode) {
      void generateOne(modalNode).catch(() => undefined);
    }
  }, [modalAllNodes, modalNode, nodes, generateOne]);

  const hasActiveNode = useMemo(
    () => nodes.some((n) => n.status !== 'NODE_UNAVAILABLE'),
    [nodes]
  );

  if (initialLoading) {
    return (
      <Box
        className="support-zip-ha"
        data-testid="support-zip-ha"
        aria-live="polite"
        aria-busy="true"
      >
        <Flex align="center" justify="center" gap="3" py="9">
          <Loader2 size={24} className="support-zip-ha__spinner" aria-hidden="true" />
          <Text size="3">Loading HA nodes...</Text>
        </Flex>
      </Box>
    );
  }

  if (loadError) {
    return (
      <Box className="support-zip-ha" data-testid="support-zip-ha">
        <SettingsAlert type="error" data-testid="support-zip-ha-error">
          {loadError}
        </SettingsAlert>
      </Box>
    );
  }

  return (
    <Box className="support-zip-ha" data-testid="support-zip-ha">
      <Flex align="center" gap="2" mb="4" className="support-zip-ha__header">
        <Server size={20} className="support-zip-ha__icon" aria-hidden="true" />
        <Heading as="h3" size="4" weight="medium">
          High Availability Mode
        </Heading>
      </Flex>

      <Text as="p" size="2" color="gray" mb="4">
        You are running in a clustered environment. Generate a support ZIP for any node, or for
        all nodes at once.
      </Text>

      {!isBlobStoreConfigured && (
        <Box mb="4">
          <SettingsAlert type="warning" data-testid="support-zip-ha-no-blob-store">
            No blob store configured for this cluster. Configure a blob store before generating
            support ZIPs.
          </SettingsAlert>
        </Box>
      )}

      <Flex justify="end" mb="4">
        <Button
          type="button"
          variant="solid"
          onClick={handleOpenAll}
          disabled={disabled || !isBlobStoreConfigured || !hasActiveNode}
          data-testid="support-zip-create-all-button"
          data-analytics-id="nxrm-support-zip-create-all"
        >
          <Server size={16} aria-hidden="true" />
          Create support ZIP (all nodes)
        </Button>
      </Flex>

      <Heading as="h4" size="3" weight="medium" mb="3">
        Available nodes
      </Heading>

      {nodes.length === 0 ? (
        <Text size="2" color="gray" data-testid="support-zip-ha-no-nodes">
          No nodes available.
        </Text>
      ) : (
        <Grid columns={{ initial: '1', sm: '2' }} gap="3" data-testid="support-zip-ha-node-grid">
          {nodes.map((node) => (
            <SupportZipNodeCard
              key={node.nodeId}
              node={node}
              isBlobStoreConfigured={isBlobStoreConfigured}
              onGenerate={handleOpenSingle}
              disabled={disabled}
            />
          ))}
        </Grid>
      )}

      <SupportZipHaModal
        open={modalOpen}
        onOpenChange={setModalOpen}
        targetNode={modalNode}
        allNodes={modalAllNodes}
        params={params}
        onParamChange={onParamChange}
        onSubmit={handleModalSubmit}
        disabled={disabled}
      />
    </Box>
  );
}

export default SupportZipHA;
