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
import React, { useCallback } from 'react';
import { Box, Flex, Text } from '@radix-ui/themes';
import { AlertTriangle } from 'lucide-react';
import { useCurrentStateAndParams, useRouter } from '@uirouter/react';
import { ExtJS } from '../../../../../../interface/ExtJS';

import { SettingsAlert } from '../../../../shared/form';
import { HelpSection, PageHeader } from '../../../../shared';
import { LogsList } from './LogsList';
import { LogViewer } from './LogViewer';

import './LogsPage.scss';

const ROUTE_LIST = 'preview.admin.support.logs.list';
const ROUTE_DETAIL = 'preview.admin.support.logs.detail';

/**
 * LogsPage - Main Logs management page for Preview UI
 *
 * Displays log file list and allows viewing individual log files.
 * Note: Logs are not available via the UI in clustered (HA) mode.
 *
 * The selected log file is stored in the route URL (/*filename param with
 * raw: true so task log paths like tasks/foo.log are preserved). This means
 * the global header Refresh button — which calls router.stateService.reload()
 * — remounts the component with the same filename param and keeps the viewer
 * open instead of navigating back to the list.
 */
export function LogsPage() {
  const router = useRouter();
  const { params } = useCurrentStateAndParams();

  // filename is the raw path from the URL; null/undefined when on the list route
  const selectedLog: string | null = (params.filename as string) ?? null;
  const viewMode = selectedLog ? 'detail' : 'list';

  // Permission check
  const canRead = ExtJS.checkPermission('nexus:logging:read');

  // Check if clustered (HA) mode
  const isClustered = ExtJS.state().getValue('nexus.datastore.clustered.enabled');

  const handleSelectLog = useCallback((filename: string) => {
    router.stateService.go(ROUTE_DETAIL, {filename});
  }, []);

  const handleBack = useCallback(() => {
    router.stateService.go(ROUTE_LIST);
  }, []);

  // Navigation helper for Settings breadcrumb
  const navigateToSettings = () => {
    window.location.hash = '#preview/admin/settings';
  };

  // Render header based on view mode
  const renderHeader = () => {
    if (viewMode === 'list') {
      const breadcrumbs = [
        { label: 'Settings', onClick: navigateToSettings },
        { label: 'Logs' },
      ];
      return (
        <PageHeader
          title="Logs"
          description="View the current log contents"
          breadcrumbs={breadcrumbs}
          className="logs-page__header"
        />
      );
    }

    const decodedLog = selectedLog ?? '';
    const breadcrumbs = [
      { label: 'Settings', onClick: navigateToSettings },
      { label: 'Logs', onClick: handleBack },
      { label: decodedLog },
    ];

    return (
      <PageHeader
        title="Log Viewer"
        description={decodedLog}
        breadcrumbs={breadcrumbs}
        className="logs-page__header"
      />
    );
  };

  // No permission state
  if (!canRead) {
    const breadcrumbs = [
      { label: 'Settings', onClick: navigateToSettings },
      { label: 'Logs' },
    ];
    return (
      <Box className="logs-page" data-testid="logs-page">
        <PageHeader
          title="Logs"
          description="View the current log contents"
          breadcrumbs={breadcrumbs}
          className="logs-page__header"
        />

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

