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

import React from 'react';
import { Box, Flex, Text, Select } from '@radix-ui/themes';
import { Loader2, Download, Copy, RefreshCw, Info, ExternalLink, ChevronsDownUp, ChevronsUpDown } from 'lucide-react';
import { ExtJS } from '../../../../../../interface/ExtJS';

import { SettingsAlert, SettingsButton } from '../../../../shared/form';
import { PageHeader } from '../../../../shared';
import { SystemInfoSection } from './SystemInfoSection';
import { NodeSelector } from './NodeSelector';
import { useSystemInfo } from './useSystemInfo';
import { SystemInfoPageProps, formatSectionTitle } from './types';

import './SystemInfoPage.scss';

/**
 * SystemInfoPage - System Information page for Preview UI
 *
 * Displays detailed system information including Nexus status,
 * configuration, and various system properties.
 */
export function SystemInfoPage({ className }: SystemInfoPageProps) {
  const {
    systemInfo,
    nodes,
    selectedNode,
    isHAMode,
    isLoading,
    isRefreshing,
    error,
    expandedSections,
    sections,
    sectionRefs,
    handleNodeSelect,
    handleRefresh,
    handleDownload,
    handleCopy,
    handleExpandAll,
    handleCollapseAll,
    handleSectionToggle,
    handleJumpToSection,
    clearError,
  } = useSystemInfo();

  const canRead = ExtJS.checkPermission('nexus:atlas:read');

  // Loading state
  if (isLoading) {
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
              disabled={isRefreshing || isLoading}
              aria-label="Refresh"
              icon={RefreshCw}
              className={isRefreshing ? 'system-info-page__spinning' : ''}
              data-analytics-id="nxrm-system-info-refresh"
            >
              Refresh
            </SettingsButton>
            <SettingsButton
              variant="ghost"
              onClick={handleCopy}
              disabled={!systemInfo || isLoading}
              aria-label="Copy to clipboard"
              icon={Copy}
              data-analytics-id="nxrm-system-info-copy"
            >
              Copy
            </SettingsButton>
            <SettingsButton
              variant="ghost"
              onClick={handleDownload}
              disabled={!systemInfo || isLoading}
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
          <SettingsAlert type="error" onClose={clearError}>
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
