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


const navigateTo = (path: string) => {
  window.location.hash = path;
}


import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { Box, Flex, Text, Heading, Grid } from '@radix-ui/themes';
import { Activity, Loader2, Download, RefreshCw, Info, ExternalLink, ArrowLeft, CheckCircle, XCircle } from 'lucide-react';
import { ExtJS } from '@sonatype/nexus-ui-plugin';

import { SettingsAlert, SettingsButton } from '../../../shared/form';
import { useToast } from '../../../../shared';
import { MetricHealthList } from './MetricHealthList';
import { MetricHealthDetail } from './MetricHealthDetail';
import { useMetricHealthApi } from './useMetricHealthApi';
import { MetricHealthPageProps, HealthCheck, NodeInfo } from './types';

import './MetricHealthPage.scss';

/**
 * MetricHealthPage - Metric Health page for Preview UI
 *
 * Displays health check status for various system components.
 * Supports both single-node and clustered modes.
 */
export function MetricHealthPage({ className }: MetricHealthPageProps) {
  const {
    loading,
    error,
    setError,
    fetchMetricHealth,
    fetchClusterNodes,
    fetchNodeMetricHealth,
    downloadMetricHealth,
  } = useMetricHealthApi();

  const toast = useToast();

  const [checks, setChecks] = useState<HealthCheck[]>([]);
  const [selectedCheck, setSelectedCheck] = useState<string | null>(null);
  const [loadingInitial, setLoadingInitial] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  // Clustered mode state
  const [isClusteredMode, setIsClusteredMode] = useState(false);
  const [nodes, setNodes] = useState<NodeInfo[]>([]);
  const [selectedNode, setSelectedNode] = useState<string | null>(null);

  const canRead = ExtJS.checkPermission('nexus:metrics:read');

  // Check if instance is clustered
  const isClustered = useCallback(() => {
    return ExtJS.state().getValue('nexus.datastore.clustered.enabled') === true;
  }, []);

  // Load health data on mount
  const loadData = useCallback(async () => {
    setLoadingInitial(true);
    setError(null);

    try {
      const clustered = isClustered();
      setIsClusteredMode(clustered);

      if (clustered) {
        // Clustered mode: fetch nodes list
        const clusterNodes = await fetchClusterNodes();
        setNodes(clusterNodes);
        // Don't auto-select a node - user must click to view details
        setSelectedNode(null);
      } else {
        // Single node mode: fetch health checks directly
        const healthChecks = await fetchMetricHealth();
        setChecks(healthChecks);

        // Select first unhealthy check, or first check if all healthy
        const unhealthyCheck = healthChecks.find((c) => !c.result.healthy);
        const firstCheck = healthChecks[0];
        setSelectedCheck(unhealthyCheck?.name || firstCheck?.name || null);
      }
    } catch (err) {
      // Error is already set by the hook
    } finally {
      setLoadingInitial(false);
    }
  }, [fetchMetricHealth, fetchClusterNodes, isClustered, setError]);

  useEffect(() => {
    if (canRead) {
      loadData();
    } else {
      setLoadingInitial(false);
    }
  }, [canRead, loadData]);

  // Handle node selection in clustered mode
  const handleNodeSelect = useCallback(async (nodeId: string) => {
    setSelectedNode(nodeId);
    try {
      const healthChecks = await fetchNodeMetricHealth(nodeId);
      setChecks(healthChecks);

      // Select first unhealthy check, or first check if all healthy
      const unhealthyCheck = healthChecks.find((c) => !c.result.healthy);
      const firstCheck = healthChecks[0];
      setSelectedCheck(unhealthyCheck?.name || firstCheck?.name || null);
    } catch (err) {
      // Error is set by the hook
    }
  }, [fetchNodeMetricHealth]);

  // Handle back to node list in clustered mode
  const handleBackToNodes = useCallback(() => {
    setSelectedNode(null);
    setChecks([]);
    setSelectedCheck(null);
  }, []);

  // Handle refresh
  const handleRefresh = useCallback(async () => {
    setRefreshing(true);
    try {
      if (isClusteredMode) {
        if (selectedNode) {
          // Refresh current node's health checks
          const healthChecks = await fetchNodeMetricHealth(selectedNode);
          setChecks(healthChecks);
        } else {
          // Refresh nodes list
          const clusterNodes = await fetchClusterNodes();
          setNodes(clusterNodes);
        }
      } else {
        const healthChecks = await fetchMetricHealth();
        setChecks(healthChecks);
      }
      toast.success('Health checks refreshed');
    } catch (err) {
      // Error is set by the hook
    } finally {
      setRefreshing(false);
    }
  }, [fetchMetricHealth, fetchClusterNodes, fetchNodeMetricHealth, isClusteredMode, selectedNode]);

  // Handle download
  const handleDownload = useCallback(() => {
    if (checks.length === 0) return;
    const filename = selectedNode ? `metric-health-${selectedNode}.json` : 'metric-health.json';
    downloadMetricHealth(checks, filename);
    toast.success('Health check data downloaded');
  }, [checks, downloadMetricHealth, selectedNode]);

  // Get selected check
  const selectedCheckData = useMemo(() => {
    return checks.find((c) => c.name === selectedCheck) || null;
  }, [checks, selectedCheck]);

  // Loading state
  if (loadingInitial) {
    return (
      <Box className={`metric-health-page ${className || ''}`.trim()} data-testid="metric-health-page">
        <Flex align="center" justify="center" className="metric-health-page__loading">
          <Loader2 size={24} className="metric-health-page__spinner" />
          <Text size="2">Loading health checks...</Text>
        </Flex>
      </Box>
    );
  }

  // No permission state
  if (!canRead) {
    return (
      <Box className={`metric-health-page ${className || ''}`.trim()} data-testid="metric-health-page">
        <Flex align="center" gap="3" className="metric-health-page__header">
          <Activity size={24} className="metric-health-page__icon" />
          <Box>
            <Heading as="h1" size="6" weight="medium">Status</Heading>
            <Text size="2" className="metric-health-page__description">
              View system health checks and diagnostics
            </Text>
          </Box>
        </Flex>

        <SettingsAlert type="warning">
          You do not have permission to view metric health.
        </SettingsAlert>
      </Box>
    );
  }

  // Render node list for clustered mode when no node is selected
  const renderNodeList = () => (
    <Box className="metric-health-page__nodes">
      <Text size="2" weight="medium" className="metric-health-page__nodes-title">
        Select a node to view health checks
      </Text>
      <Box className="metric-health-page__nodes-list">
        {nodes.length === 0 ? (
          <Text size="2" className="metric-health-page__empty">
            No cluster nodes available
          </Text>
        ) : (
          nodes.map((node) => (
            <button
              key={node.nodeId}
              type="button"
              className="metric-health-page__node-item"
              onClick={() => handleNodeSelect(node.nodeId)}
              data-testid={`node-item-${node.nodeId}`}
            >
              <Flex align="center" gap="3">
                {node.healthy !== undefined && (
                  node.healthy ? (
                    <CheckCircle size={20} className="metric-health-page__node-icon metric-health-page__node-icon--healthy" />
                  ) : (
                    <XCircle size={20} className="metric-health-page__node-icon metric-health-page__node-icon--unhealthy" />
                  )
                )}
                <Box>
                  <Text size="2" weight="medium" className="metric-health-page__node-name">
                    {node.hostname || node.nodeId}
                  </Text>
                  {node.message && (
                    <Text size="1" className="metric-health-page__node-message">
                      {node.message}
                    </Text>
                  )}
                </Box>
              </Flex>
            </button>
          ))
        )}
      </Box>
    </Box>
  );

  return (
    <Box className={`metric-health-page ${className || ''}`.trim()} data-testid="metric-health-page" data-loading={loading}>
      {/* Header */}
      <Flex align="center" justify="between" className="metric-health-page__header">
        <Flex align="center" gap="3">
          {isClusteredMode && selectedNode && (
            <SettingsButton
              variant="ghost"
              onClick={handleBackToNodes}
              aria-label="Back to nodes"
              data-testid="back-to-nodes-button"
            >
              <ArrowLeft size={16} />
            </SettingsButton>
          )}
          <Activity size={24} className="metric-health-page__icon" />
          <Box>
            <Heading as="h1" size="6" weight="medium">Status</Heading>
            <Text size="2" className="metric-health-page__description">
              {isClusteredMode && selectedNode
                ? `Health checks for ${nodes.find(n => n.nodeId === selectedNode)?.hostname || selectedNode}`
                : 'View system health checks and diagnostics'}
            </Text>
          </Box>
        </Flex>

        {/* Action buttons */}
        <Flex gap="2" className="metric-health-page__actions">
          <SettingsButton
            variant="ghost"
            onClick={handleRefresh}
            disabled={refreshing || loading}
            aria-label="Refresh"
            icon={RefreshCw}
            className={refreshing ? 'metric-health-page__spinning' : ''}
            data-testid="refresh-button"
          >
            Refresh
          </SettingsButton>
          {(!isClusteredMode || selectedNode) && (
            <SettingsButton
              variant="ghost"
              onClick={handleDownload}
              disabled={checks.length === 0 || loading}
              aria-label="Download"
              icon={Download}
              data-testid="download-button"
            >
              Download
            </SettingsButton>
          )}
        </Flex>
      </Flex>

      {/* Error Alert */}
      {error && (
        <Box className="metric-health-page__alerts">
          <SettingsAlert type="error" onClose={() => setError(null)}>
            {error}
          </SettingsAlert>
        </Box>
      )}

      {/* Content */}
      {isClusteredMode && !selectedNode ? (
        // Clustered mode: show node list
        renderNodeList()
      ) : checks.length === 0 ? (
        <Text size="2" className="metric-health-page__empty">
          No health checks available
        </Text>
      ) : (
        // Single node or node selected: show health checks
        <Grid columns="1fr 1fr" gap="4" className="metric-health-page__content">
          <MetricHealthList
            checks={checks}
            selectedCheck={selectedCheck}
            onSelectCheck={setSelectedCheck}
          />
          <MetricHealthDetail check={selectedCheckData} />
        </Grid>
      )}

      {/* Help Section */}
      <Box className="metric-health-page__help">
        <Flex align="center" gap="2" className="metric-health-page__help-header">
          <Info size={16} />
          <Text size="2" weight="medium">About Status</Text>
        </Flex>
        <Text size="2" className="metric-health-page__help-text">
          This page displays the status of various health checks that monitor
          critical system components. Red indicators show components that may
          require attention.
        </Text>
        <Text size="2" className="metric-health-page__help-text">
          See our{' '}
          <a
            href="http://links.sonatype.com/products/nxrm3/docs/metrics"
            target="_blank"
            rel="noopener noreferrer"
            className="metric-health-page__help-link"
          >
            documentation
            <ExternalLink size={12} />
          </a>
          {' '}for more information.
        </Text>
      </Box>
    </Box>
  );
}

export default MetricHealthPage;


