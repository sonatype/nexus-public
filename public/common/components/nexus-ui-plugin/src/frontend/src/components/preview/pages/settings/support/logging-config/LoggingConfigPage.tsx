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
import React, { useState, useCallback } from 'react';
import { Box, Flex } from '@radix-ui/themes';
import { Plus, RotateCcw } from 'lucide-react';
import { ExtJS } from '../../../../../../interface/ExtJS';

import { SettingsButton, SettingsAlert, ConfirmDialog } from '../../../../shared/form';
import { HelpSection, useToast, PageHeader } from '../../../../shared';
import { LoggersList } from './LoggersList';
import { LoggerForm } from './LoggerForm';
import { useLoggingConfigApi } from './useLoggingConfigApi';

import './LoggingConfigPage.scss';

type ViewMode = 'list' | 'create' | 'detail';

/**
 * LoggingConfigPage - Main Logging Configuration page for Preview UI
 *
 * Displays logger list with search/filter, and allows creating, editing, and resetting loggers.
 */
export function LoggingConfigPage() {
  const [viewMode, setViewMode] = useState<ViewMode>('list');
  const [selectedLogger, setSelectedLogger] = useState<string | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);
  const [resetAllDialogOpen, setResetAllDialogOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);

  const { resetLogger, resetAllLoggers, loading, error, setError } = useLoggingConfigApi();
  const toast = useToast();

  const canUpdate = ExtJS.checkPermission('nexus:logging:update');

  const handleSelectLogger = useCallback((name: string) => {
    setSelectedLogger(name);
    setError(null);
    setViewMode('detail');
  }, [setError]);

  const handleCreate = useCallback(() => {
    setSelectedLogger(null);
    setError(null);
    setViewMode('create');
  }, [setError]);

  const handleBack = useCallback(() => {
    setSelectedLogger(null);
    setError(null);
    setViewMode('list');
  }, [setError]);

  const handleSave = useCallback(() => {
    setRefreshKey((k) => k + 1);
    handleBack();
  }, [handleBack]);

  const handleDeleteClick = useCallback(() => {
    setDeleteDialogOpen(true);
  }, []);

  const handleDeleteConfirm = useCallback(async () => {
    if (!selectedLogger) return;

    setIsDeleting(true);
    try {
      await resetLogger(selectedLogger);
      toast.success(`Logger override removed for "${selectedLogger}"`);
      setDeleteDialogOpen(false);
      handleSave();
    } catch (err) {
      // Error is handled by the hook
    } finally {
      setIsDeleting(false);
    }
  }, [selectedLogger, resetLogger, toast, handleSave]);

  const handleResetAll = useCallback(() => {
    setResetAllDialogOpen(true);
  }, []);

  const handleResetAllConfirm = useCallback(async () => {
    setResetAllDialogOpen(false);
    try {
      await resetAllLoggers();
      toast.success('All loggers reset to default levels');
      setRefreshKey((k) => k + 1);
    } catch (err) {
      // Error is handled by the hook
    }
  }, [resetAllLoggers, toast]);

  // Navigation helper for Settings breadcrumb
  const navigateToSettings = () => {
    window.location.hash = '#preview/admin/settings';
  };

  // Render header based on view mode
  const renderHeader = () => {
    if (viewMode === 'list') {
      const breadcrumbs = [
        { label: 'Settings', onClick: navigateToSettings },
        { label: 'Logging' },
      ];
      const actions = canUpdate ? (
        <Flex gap="2">
          <SettingsButton variant="secondary" onClick={handleResetAll} disabled={loading} icon={RotateCcw}>
            Reset to Default Levels
          </SettingsButton>
          <SettingsButton variant="primary" onClick={handleCreate} icon={Plus}>
            Create Logger
          </SettingsButton>
        </Flex>
      ) : undefined;
      return (
        <PageHeader
          title="Logging"
          description="Control logging levels"
          breadcrumbs={breadcrumbs}
          actions={actions}
          className="logging-config-page__header"
        />
      );
    }

    const title = viewMode === 'create' ? 'Create Logger' : 'Edit Logger';
    const breadcrumbs = [
      { label: 'Settings', onClick: navigateToSettings },
      { label: 'Logging', onClick: handleBack },
      { label: viewMode === 'create' ? 'Create' : (selectedLogger || 'Logger') },
    ];

    return (
      <PageHeader
        title={title}
        description={selectedLogger && viewMode === 'detail' ? selectedLogger : undefined}
        breadcrumbs={breadcrumbs}
        className="logging-config-page__header"
      />
    );
  };

  return (
    <Box className="logging-config-page" data-testid="logging-config-page">
      {renderHeader()}

      {/* Alerts */}
      {error && (
        <Box className="logging-config-page__alerts">
          <SettingsAlert type="error" onClose={() => setError(null)}>
            {error}
          </SettingsAlert>
        </Box>
      )}

      {/* Content */}
      <Box className="logging-config-page__content">
        {viewMode === 'list' && (
          <LoggersList
            key={refreshKey}
            onSelect={handleSelectLogger}
          />
        )}

        {viewMode === 'create' && (
          <LoggerForm isCreate={true} onSave={handleSave} onCancel={handleBack} />
        )}

        {viewMode === 'detail' && selectedLogger && (
          <LoggerForm
            loggerName={selectedLogger}
            onSave={handleSave}
            onCancel={handleBack}
            onDelete={canUpdate ? handleDeleteClick : undefined}
          />
        )}
      </Box>

      {/* Help Section - only shown in list view */}
      {viewMode === 'list' && (
        <HelpSection
          title="About Logging Configuration"
          content="Configure logging levels for different packages and classes in Nexus Repository. Increasing the logging level can help diagnose issues but may impact performance. Reset to defaults will restore all loggers to their original levels."
          docLink={{
            label: 'View Documentation',
            href: 'https://help.sonatype.com/en/logging-configuration.html',
          }}
        />
      )}

      {/* Delete Confirmation Dialog */}
      <ConfirmDialog
        open={deleteDialogOpen}
        testId="delete-logger-override-dialog"
        onOpenChange={setDeleteDialogOpen}
        title="Delete Logger Override"
        message={`Remove the custom log level for "${selectedLogger}"? The logger will inherit its level from the parent logger configuration. This effectively removes it from the custom loggers list.`}
        confirmLabel={isDeleting ? 'Deleting...' : 'Delete'}
        cancelLabel="Cancel"
        variant="danger"
        onConfirm={handleDeleteConfirm}
      />

      {/* Reset All Confirmation Dialog */}
      <ConfirmDialog
        open={resetAllDialogOpen}
        testId="reset-all-loggers-dialog"
        onOpenChange={setResetAllDialogOpen}
        title="Reset All Loggers"
        message="Are you sure you want to reset all loggers to their default levels? This action cannot be undone."
        confirmLabel="Reset All"
        cancelLabel="Cancel"
        variant="warning"
        onConfirm={handleResetAllConfirm}
      />
    </Box>
  );
}

export default LoggingConfigPage;


