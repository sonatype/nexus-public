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


import React, { useState, useCallback } from 'react';
import { Box, Flex, Text, ScrollArea, Heading } from '@radix-ui/themes';
import { FileText, ArrowLeft, AlertTriangle } from 'lucide-react';
import { ExtJS } from '../../../../../../interface/ExtJS';

import { SettingsButton, SettingsAlert } from '../../../../shared/form';
import { HelpSection } from '../../../../shared';
import { LogsList } from './LogsList';
import { LogViewer } from './LogViewer';

import './LogsPage.scss';

type ViewMode = 'list' | 'detail';

/**
 * LogsPage - Main Logs management page for Preview UI
 *
 * Displays log file list and allows viewing individual log files.
 * Note: Logs are not available via the UI in clustered (HA) mode.
 */
export function LogsPage() {
  const [viewMode, setViewMode] = useState<ViewMode>('list');
  const [selectedLog, setSelectedLog] = useState<string | null>(null);

  // Permission check
  const canRead = ExtJS.checkPermission('nexus:logging:read');

  // Check if clustered (HA) mode
  const isClustered = ExtJS.state().getValue('nexus.datastore.clustered.enabled');

  const handleSelectLog = useCallback((filename: string) => {
    setSelectedLog(encodeURIComponent(filename));
    setViewMode('detail');
  }, []);

  const handleBack = useCallback(() => {
    setSelectedLog(null);
    setViewMode('list');
  }, []);

  // Render header based on view mode
  const renderHeader = () => {
    if (viewMode === 'list') {
      return (
        <Flex align="center" gap="3" className="logs-page__header">
          <FileText size={24} className="logs-page__icon" />
          <Box>
            <Heading as="h1" size="6" weight="medium">
              Logs
            </Heading>
            <Text size="2" className="logs-page__description">
              View the current log contents
            </Text>
          </Box>
        </Flex>
      );
    }

    return (
      <Flex align="center" gap="3" className="logs-page__header">
        <SettingsButton
          variant="ghost"
          onClick={handleBack}
          className="logs-page__back"
          icon={ArrowLeft}
        />
        <Box>
          <Heading as="h1" size="6" weight="medium">
            Log Viewer
          </Heading>
          {selectedLog && (
            <Text size="2" className="logs-page__description">
              {decodeURIComponent(selectedLog)}
            </Text>
          )}
        </Box>
      </Flex>
    );
  };

  // No permission state
  if (!canRead) {
    return (
      <Box className="logs-page" data-testid="logs-page">
        <Flex align="center" gap="3" className="logs-page__header">
          <FileText size={24} className="logs-page__icon" />
          <Box>
            <Heading as="h1" size="6" weight="medium">
              Logs
            </Heading>
            <Text size="2" className="logs-page__description">
              View the current log contents
            </Text>
          </Box>
        </Flex>

        <SettingsAlert type="warning">
          You do not have permission to view logs.
        </SettingsAlert>
      </Box>
    );
  }

  return (
    <Box className="logs-page" data-testid="logs-page">
      {renderHeader()}

      {/* Clustered mode warning */}
      {isClustered && (
        <Box className="logs-page__clustered-warning" mb="4">
          <SettingsAlert type="warning">
            <Flex align="center" gap="2">
              <AlertTriangle size={16} />
              <Text>
                Logs are not available via the UI in a clustered environment.
                Please access log files directly from the file system on each node.
              </Text>
            </Flex>
          </SettingsAlert>
        </Box>
      )}

      {/* Main content - only shown when not in clustered mode */}
      {!isClustered && (
        <Box className="logs-page__content">
          {viewMode === 'list' && <LogsList onSelect={handleSelectLog} />}

          {viewMode === 'detail' && selectedLog && (
            <LogViewer filename={selectedLog} onBack={handleBack} />
          )}
        </Box>
      )}

      {/* Help Section - only shown in list view */}
      {viewMode === 'list' && (
        <HelpSection
          title="About Logs"
          content="View and download log files from the Nexus Repository server. The main log file is nexus.log which contains application logs. You can insert markers into the log to help identify specific events."
          docLink={{
            label: 'View Documentation',
            href: 'https://help.sonatype.com/en/logging-configuration.html',
          }}
        />
      )}
    </Box>
  );
}

export default LogsPage;


