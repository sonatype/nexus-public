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
import { Box, Flex } from '@radix-ui/themes';
import { Plus, RotateCcw } from 'lucide-react';
import { ExtJS } from '../../../../../../interface/ExtJS';

import { SettingsButton, SettingsAlert, ConfirmDialog } from '../../../../shared/form';
import { HelpSection, PageHeader } from '../../../../shared';
import { LoggersList } from './LoggersList';
import { LoggerForm } from './LoggerForm';
import { useLoggingConfig } from './useLoggingConfig';

import './LoggingConfigPage.scss';

/**
 * LoggingConfigPage - Main Logging Configuration page for Preview UI
 *
 * Displays logger list with search/filter, and allows creating, editing, and resetting loggers.
 */
export function LoggingConfigPage() {
  const {
    viewMode,
    selectedLogger,
    deleteDialogOpen,
    resetAllDialogOpen,
    isDeleting,
    isResettingAll,
    error,
    refreshKey,
    handleSelectLogger,
    handleCreate,
    handleBack,
    handleSave,
    handleDeleteClick,
    handleDeleteConfirm,
    handleCancelDelete,
    handleResetAll,
    handleResetAllConfirm,
    handleCancelResetAll,
    clearError,
  } = useLoggingConfig();

  const canUpdate = ExtJS.checkPermission('nexus:logging:update');

  const navigateToSettings = () => {
    window.location.hash = '#preview/admin/settings';
  };

  const renderHeader = () => {
    if (viewMode === 'list') {
      const breadcrumbs = [
        { label: 'Settings', onClick: navigateToSettings },
        { label: 'Logging' },
      ];
      const actions = canUpdate ? (
        <Flex gap="2">
          <SettingsButton variant="secondary" onClick={handleResetAll} disabled={isResettingAll} icon={RotateCcw}>
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
      { label: viewMode === 'create' ? 'Create' : selectedLogger || 'Logger' },
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
          <SettingsAlert type="error" onClose={clearError}>
            {error}
          </SettingsAlert>
        </Box>
      )}

      {/* Content */}
      <Box className="logging-config-page__content">
        {viewMode === 'list' && <LoggersList key={refreshKey} onSelect={handleSelectLogger} />}

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
        onOpenChange={(open) => !open && handleCancelDelete()}
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
        onOpenChange={(open) => !open && handleCancelResetAll()}
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
