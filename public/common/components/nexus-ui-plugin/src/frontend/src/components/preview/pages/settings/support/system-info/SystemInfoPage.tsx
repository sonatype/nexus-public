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

import React, { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { Box, Flex, Text, Heading, Select } from '@radix-ui/themes';
import { Loader2, Download, Copy, RefreshCw, Info, ExternalLink, ChevronsDownUp, ChevronsUpDown } from 'lucide-react';
import { ExtJS } from '../../../../../../interface/ExtJS';

import { SettingsAlert, SettingsButton } from '../../../../shared/form';
import { useToast, PageHeader } from '../../../../shared';
import { SystemInfoSection } from './SystemInfoSection';
import { NodeSelector } from './NodeSelector';
import { useSystemInfoApi } from './useSystemInfoApi';
import {
  SystemInformation,
  HASystemInformation,
  HANode,
  SystemInfoPageProps,
  formatSectionTitle,
} from './types';

import './SystemInfoPage.scss';

// Define the preferred order of sections
const SECTION_ORDER = [
  'nexus-status',
  'nexus-node',
  'nexus-license',
  'nexus-configuration',
  'nexus-properties',
  'system-time',
  'system-properties',
  'system-environment',
  'system-runtime',
  'system-network',
  'system-filestores',
];

/**
 * Sort sections by preferred order
 */
function sortSections(sections: [string, any][]): [string, any][] {
  return sections.sort(([keyA], [keyB]) => {
    const indexA = SECTION_ORDER.indexOf(keyA);
    const indexB = SECTION_ORDER.indexOf(keyB);
    
    // If both are in the order array, sort by index
    if (indexA !== -1 && indexB !== -1) {
      return indexA - indexB;
    }
    // If only one is in the order array, it comes first
    if (indexA !== -1) return -1;
    if (indexB !== -1) return 1;
    // Otherwise, sort alphabetically
    return keyA.localeCompare(keyB);
  });
}

/**
 * SystemInfoPage - System Information page for Preview UI
 *
 * Displays detailed system information including Nexus status,
 * configuration, and various system properties.
 */
export function SystemInfoPage({ className }: SystemInfoPageProps) {
  const {
    loading,
    error,
    setError,
    fetchSystemInfo,
    fetchSystemInfoHA,
    fetchActiveNodes,
    downloadSystemInfo,
    copyToClipboard,
  } = useSystemInfoApi();

  const toast = useToast();

  const [systemInfo, setSystemInfo] = useState<SystemInformation | null>(null);
  const [haSystemInfo, setHASystemInfo] = useState<HASystemInformation | null>(null);
  const [nodes, setNodes] = useState<HANode[]>([]);
  const [selectedNode, setSelectedNode] = useState<string | null>(null);
  const [loadingInitial, setLoadingInitial] = useState(true);
  const [isHAMode, setIsHAMode] = useState(false);
  const [refreshing, setRefreshing] = useState(false);

  // State for controlling expanded sections
  const [expandedSections, setExpandedSections] = useState<Set<string>>(() => {
    // Default: first 3 sections are expanded
    return new Set(SECTION_ORDER.slice(0, 3));
  });

  // Refs for scrolling to sections
  const sectionRefs = useRef<Record<string, HTMLDivElement | null>>({});

  const canRead = ExtJS.checkPermission('nexus:atlas:read');

  // Load system information on mount
  const loadData = useCallback(async () => {
    setLoadingInitial(true);
    setError(null);
    
    try {
      // First check if we're in HA mode by trying to fetch nodes
      const activeNodes = await fetchActiveNodes();
      
      if (activeNodes.length > 1) {
        // HA mode
        setIsHAMode(true);
        setNodes(activeNodes);
        
        // Select local node by default, or first node
        const localNode = activeNodes.find(n => n.local);
        const defaultNode = localNode?.nodeId || activeNodes[0]?.nodeId;
        setSelectedNode(defaultNode);
        
        // Fetch HA system info
        const haInfo = await fetchSystemInfoHA();
        setHASystemInfo(haInfo);
        setSystemInfo(haInfo[defaultNode] || null);
      } else {
        // Non-HA mode
        setIsHAMode(false);
        const info = await fetchSystemInfo();
        setSystemInfo(info);
      }
    } catch (err) {
      // Error is already set by the hook
    } finally {
      setLoadingInitial(false);
    }
  }, [fetchActiveNodes, fetchSystemInfo, fetchSystemInfoHA, setError]);

  useEffect(() => {
    if (canRead) {
      loadData();
    } else {
      setLoadingInitial(false);
    }
  }, [canRead, loadData]);

  // Handle node selection
  const handleNodeSelect = useCallback((nodeId: string) => {
    setSelectedNode(nodeId);
    if (haSystemInfo && haSystemInfo[nodeId]) {
      setSystemInfo(haSystemInfo[nodeId]);
    }
  }, [haSystemInfo]);

  // Handle refresh
  const handleRefresh = useCallback(async () => {
    setRefreshing(true);
    try {
      if (isHAMode) {
        const haInfo = await fetchSystemInfoHA();
        setHASystemInfo(haInfo);
        if (selectedNode && haInfo[selectedNode]) {
          setSystemInfo(haInfo[selectedNode]);
        }
      } else {
        const info = await fetchSystemInfo();
        setSystemInfo(info);
      }
      toast.success('System information refreshed');
    } catch (err) {
      // Error is set by the hook
    } finally {
      setRefreshing(false);
    }
  }, [isHAMode, selectedNode, fetchSystemInfo, fetchSystemInfoHA]);

  // Handle download
  const handleDownload = useCallback(() => {
    if (!systemInfo) return;
    
    const filename = isHAMode && selectedNode
      ? `system-information-${selectedNode}.json`
      : 'system-information.json';
    
    downloadSystemInfo(systemInfo, filename);
    toast.success('System information downloaded');
  }, [systemInfo, isHAMode, selectedNode, downloadSystemInfo]);

  // Handle copy to clipboard
  const handleCopy = useCallback(async () => {
    if (!systemInfo) return;
    
    const success = await copyToClipboard(systemInfo);
    if (success) {
      toast.success('Copied to clipboard');
    } else {
      setError('Failed to copy to clipboard');
    }
  }, [systemInfo, copyToClipboard, setError]);

  // Get sorted sections (must be defined before handlers that use it)
  const sections = useMemo(() => {
    if (!systemInfo) return [];
    return sortSections(Object.entries(systemInfo).filter(([_, value]) => value && typeof value === 'object'));
  }, [systemInfo]);

  // Handle section toggle
  const handleSectionToggle = useCallback((sectionKey: string, isOpen: boolean) => {
    setExpandedSections(prev => {
      const next = new Set(prev);
      if (isOpen) {
        next.add(sectionKey);
      } else {
        next.delete(sectionKey);
      }
      return next;
    });
  }, []);

  // Handle jump to section
  const handleJumpToSection = useCallback((sectionKey: string) => {
    // Expand the section
    setExpandedSections(prev => {
      const next = new Set(prev);
      next.add(sectionKey);
      return next;
    });

    // Scroll to section with smooth behavior
    setTimeout(() => {
      const ref = sectionRefs.current[sectionKey];
      if (ref) {
        ref.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }
    }, 100); // Small delay to allow expansion animation
  }, []);

  // Handle expand all
  const handleExpandAll = useCallback(() => {
    const allSectionKeys = sections.map(([key]) => key);
    setExpandedSections(new Set(allSectionKeys));
  }, [sections]);

  // Handle collapse all
  const handleCollapseAll = useCallback(() => {
    setExpandedSections(new Set());
  }, []);

  // Loading state
  if (loadingInitial) {
    return (
      <Box className={`system-info-page ${className || ''}`.trim()}>
        <Flex align="center" justify="center" className="system-info-page__loading">
          <Loader2 size={24} className="system-info-page__spinner" />
          <Text size="2">Loading system information...</Text>
        </Flex>
      </Box>
    );
  }

  // No permission state
  if (!canRead) {
    return (
      <Box className={`system-info-page ${className || ''}`.trim()}>
        <PageHeader
          title="System Information"
          description="View detailed system and server information"
          breadcrumbs={[
            { label: 'Settings', onClick: () => { window.location.hash = '#preview/admin/settings'; } },
            { label: 'System Information' },
          ]}
        />

        <SettingsAlert type="warning">
          You do not have permission to view system information.
        </SettingsAlert>
      </Box>
    );
  }

  return (
    <Box className={`system-info-page ${className || ''}`.trim()}>
      {/* Header */}
      <PageHeader
        title="System Information"
        description="View detailed system and server information"
        breadcrumbs={[
          { label: 'Settings', onClick: () => { window.location.hash = '#preview/admin/settings'; } },
          { label: 'System Information' },
        ]}
        actions={
          <Flex gap="2" className="system-info-page__actions">
            <SettingsButton
              variant="ghost"
              onClick={handleRefresh}
              disabled={refreshing || loading}
              aria-label="Refresh"
              icon={RefreshCw}
              className={refreshing ? 'system-info-page__spinning' : ''}
              data-analytics-id="nxrm-system-info-refresh"
            >
              Refresh
            </SettingsButton>
            <SettingsButton
              variant="ghost"
              onClick={handleCopy}
              disabled={!systemInfo || loading}
              aria-label="Copy to clipboard"
              icon={Copy}
              data-analytics-id="nxrm-system-info-copy"
            >
              Copy
            </SettingsButton>
            <SettingsButton
              variant="ghost"
              onClick={handleDownload}
              disabled={!systemInfo || loading}
              aria-label="Download"
              icon={Download}
              data-analytics-id="nxrm-system-info-download"
            >
              Download
            </SettingsButton>
          </Flex>
        }
      />

      {/* Error Alert */}
      {error && (
        <Box className="system-info-page__alerts">
          <SettingsAlert type="error" onClose={() => setError(null)}>
            {error}
          </SettingsAlert>
        </Box>
      )}

      {/* HA Node Selector */}
      {isHAMode && nodes.length > 1 && (
        <NodeSelector
          nodes={nodes}
          selectedNode={selectedNode}
          onNodeSelect={handleNodeSelect}
        />
      )}

      {/* Navigation Bar */}
      {sections.length > 0 && (
        <Flex
          align="center"
          justify="between"
          className="system-info-page__nav"
          data-testid="system-info-nav"
        >
          {/* Jump to Section Dropdown */}
          <Flex align="center" gap="2">
            <Text size="2" weight="medium">Jump to:</Text>
            <Select.Root
              value=""
              onValueChange={handleJumpToSection}
            >
              <Select.Trigger
                placeholder="Select section..."
                data-testid="system-info-jump-to"
              >
                <Flex align="center" gap="2">
                  <ChevronsUpDown size={14} />
                  <span>Select section...</span>
                </Flex>
              </Select.Trigger>
              <Select.Content>
                {sections.map(([key]) => (
                  <Select.Item key={key} value={key}>
                    {formatSectionTitle(key)}
                  </Select.Item>
                ))}
              </Select.Content>
            </Select.Root>
          </Flex>

          {/* Expand/Collapse Buttons */}
          <Flex gap="2">
            <SettingsButton
              variant="ghost"
              onClick={handleExpandAll}
              aria-label="Expand all sections"
              icon={ChevronsDownUp}
              data-testid="system-info-expand-all"
            >
              Expand All
            </SettingsButton>
            <SettingsButton
              variant="ghost"
              onClick={handleCollapseAll}
              aria-label="Collapse all sections"
              icon={ChevronsUpDown}
              data-testid="system-info-collapse-all"
            >
              Collapse All
            </SettingsButton>
          </Flex>
        </Flex>
      )}

      {/* Content */}
      <Box className="system-info-page__content">
        {sections.length === 0 ? (
          <Text size="2" className="system-info-page__empty">
            No system information available
          </Text>
        ) : (
          sections.map(([key, data]) => (
            <SystemInfoSection
              key={key}
              ref={(el) => { sectionRefs.current[key] = el; }}
              title={formatSectionTitle(key)}
              data={data}
              open={expandedSections.has(key)}
              onToggle={(isOpen) => handleSectionToggle(key, isOpen)}
            />
          ))
        )}
      </Box>

      {/* Help Section */}
      <Box className="system-info-page__help">
        <Flex align="center" gap="2" className="system-info-page__help-header">
          <Info size={16} />
          <Text size="2" weight="medium">About System Information</Text>
        </Flex>
        <Text size="2" className="system-info-page__help-text">
          This page displays detailed information about your Nexus Repository instance,
          including version information, system properties, and runtime configuration.
          Use the Download button to save this information for support requests.
        </Text>
        <Text size="2" className="system-info-page__help-text">
          See our{' '}
          <a
            href="https://help.sonatype.com/en/system-information.html"
            target="_blank"
            rel="noopener noreferrer"
            className="system-info-page__help-link"
          >
            documentation
            <ExternalLink size={12} aria-hidden="true" />
          </a>
          {' '}for more information.
        </Text>
      </Box>
    </Box>
  );
}

export default SystemInfoPage;
