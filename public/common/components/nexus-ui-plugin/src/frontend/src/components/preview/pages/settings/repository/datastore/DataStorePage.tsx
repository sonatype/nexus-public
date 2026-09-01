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
import { Box, Flex, Text, Table } from '@radix-ui/themes';
import { Database, AlertCircle } from 'lucide-react';
import { ExtJS } from '../../../../../../interface/ExtJS';

import {
  SettingsForm,
  SettingsFormSection,
  SettingsTextInput,
  SettingsButton,
  SettingsAlert,
} from '../../../../shared/form';
import { PageHeader } from '../../../../shared/PageHeader';
import { LoadingState } from '../../../../shared/LoadingState';
import { ErrorState } from '../../../../shared/ErrorState';
import { useDataStoreForm } from './useDataStoreForm';
import { JdbcParameterEditor, ParameterValidation } from './JdbcParameterEditor';

import './DataStorePage.scss';

const navigateTo = (path: string) => {
  window.location.hash = path;
};

/**
 * DataStorePage - DataStore Configuration settings page for Preview UI
 *
 * Structured as:
 * 1. Connection Overview (read-only)
 * 2. Connection Pool Settings
 * 3. Advanced JDBC Parameters (key-value editor)
 * 4. Effective Configuration Preview
 * 5. Save / Discard actions
 */
export function DataStorePage() {
  const {
    data,
    field,
    isLoading,
    isSaving,
    isPristine,
    saveError,
    loadError,
    databaseType,
    effectiveConfig,
    parameterValidations,
    hasParameterErrors,
    canSave,
    showAllValidation,
    setParameters,
    setMaxPool,
    showResetConfirm,
    requestResetParams,
    confirmResetParams,
    cancelResetParams,
    submit,
    reset,
  } = useDataStoreForm();

  const canUpdate = ExtJS.checkPermission('nexus:settings:update');
  const error = saveError || loadError;

  return (
    <Box className="datastore-page">
      {/* Header - using shared PageHeader for consistent UX */}
      <PageHeader
        icon={Database}
        title="Data Store"
        description="Configure the connection used for the database"
        breadcrumbs={[
          { label: 'Settings', onClick: () => navigateTo('#preview/admin/settings') },
          { label: 'Data Store' }
        ]}
      />

      {/* Error Alert */}
      {error && (
        <Box className="datastore-page__alerts">
          <ErrorState
            variant="inline"
            title="Error"
            message={error}
          />
        </Box>
      )}

      {/* Reset Confirmation Dialog */}
      {showResetConfirm && (
        <Box className="datastore-page__alerts">
          <SettingsAlert type="warning">
            <Flex justify="between" align="center" gap="3">
              <Text size="2">
                Reset all advanced JDBC parameters to defaults? This will remove all custom parameters.
              </Text>
              <Flex gap="2">
                <SettingsButton variant="secondary" onClick={cancelResetParams}>
                  Cancel
                </SettingsButton>
                <SettingsButton variant="danger" onClick={confirmResetParams}>
                  Reset to Defaults
                </SettingsButton>
              </Flex>
            </Flex>
          </SettingsAlert>
        </Box>
      )}

      {/* Content */}
      {isLoading ? (
        <LoadingState message="Loading configuration..." />
      ) : !loadError ? (
        <SettingsForm
          testId="datastore-form"
          onSubmit={submit}
          onCancel={reset}
          loading={isSaving}
          pristine={isPristine}
          submitDisabled={!(canUpdate && canSave)}
        >
          {/* 1. Connection Overview (Read-Only) */}
          <SettingsFormSection title="Connection Overview">
            <Box className="datastore-page__readonly-fields">
              <Box className="datastore-page__readonly-field">
                <Text size="2" weight="medium" className="datastore-page__readonly-label">
                  Database Type
                </Text>
                <Text size="2" className="datastore-page__readonly-value">
                  {databaseType}
                </Text>
              </Box>
              <Box className="datastore-page__readonly-field">
                <Text size="2" weight="medium" className="datastore-page__readonly-label">
                  JDBC URL
                </Text>
                <Text size="2" className="datastore-page__readonly-value datastore-page__readonly-value--mono">
                  {data.jdbcUrl || 'Not configured'}
                </Text>
              </Box>
              <Box className="datastore-page__readonly-field">
                <Text size="2" weight="medium" className="datastore-page__readonly-label">
                  Schema
                </Text>
                <Text size="2" className="datastore-page__readonly-value">
                  {data.schema || 'Not configured'}
                </Text>
              </Box>
              <Box className="datastore-page__readonly-field">
                <Text size="2" weight="medium" className="datastore-page__readonly-label">
                  Username
                </Text>
                <Text size="2" className="datastore-page__readonly-value">
                  {data.username || 'Not configured'}
                </Text>
              </Box>
            </Box>
          </SettingsFormSection>

          {/* 2. Connection Pool Settings */}
          <SettingsFormSection title="Connection Pool Settings">
            <SettingsTextInput
              name="maximumConnectionPool"
              label="Maximum Connection Pool Size"
              value={String(data.maximumConnectionPool)}
              onChange={setMaxPool}
              type="number"
              min={1}
              max={3000}
              helpText="Maximum number of database connections in the pool. Default: 10. Valid range: 1-3000."
              error={field('maximumConnectionPool').error}
              disabled={!canUpdate || isSaving}
            />
          </SettingsFormSection>

          {/* 3. Advanced JDBC Parameters */}
          <SettingsFormSection
            title="Advanced JDBC Parameters"
            description="Configure additional JDBC connection parameters. Each parameter is validated before save."
          >
            {showAllValidation && hasParameterErrors && (
              <Flex align="center" gap="2" className="datastore-page__section-error">
                <AlertCircle size={16} />
                <Text size="2">Fix parameter errors before saving</Text>
              </Flex>
            )}
            <JdbcParameterEditor
              parameters={data.jdbcParameters}
              onChange={setParameters}
              showAllValidation={showAllValidation}
              onReset={requestResetParams}
              disabled={!canUpdate || isSaving}
              validations={parameterValidations as ParameterValidation[]}
            />
          </SettingsFormSection>

          {/* 4. Effective Configuration Preview */}
          {effectiveConfig.length > 0 && (
            <SettingsFormSection
              title="Effective JDBC Configuration"
              description="Preview of the final resolved parameter list that will be applied after save."
            >
              <Box className="datastore-page__effective-config">
                <Table.Root>
                  <Table.Header>
                    <Table.Row>
                      <Table.ColumnHeaderCell>Parameter</Table.ColumnHeaderCell>
                      <Table.ColumnHeaderCell>Value</Table.ColumnHeaderCell>
                      <Table.ColumnHeaderCell>Source</Table.ColumnHeaderCell>
                    </Table.Row>
                  </Table.Header>
                  <Table.Body>
                    {effectiveConfig.map((param) => (
                      <Table.Row key={param.name}>
                        <Table.Cell>
                          <Text size="2" className="datastore-page__effective-name">
                            {param.name}
                          </Text>
                        </Table.Cell>
                        <Table.Cell>
                          <Text size="2" className="datastore-page__effective-value">
                            {param.value}
                          </Text>
                        </Table.Cell>
                        <Table.Cell>
                          <Text
                            size="1"
                            className={`datastore-page__effective-source datastore-page__effective-source--${param.source.toLowerCase()}`}
                          >
                            {param.source}
                          </Text>
                        </Table.Cell>
                      </Table.Row>
                    ))}
                  </Table.Body>
                </Table.Root>
              </Box>
            </SettingsFormSection>
          )}

          {/* Actions are in the sticky header bar via SettingsForm */}
        </SettingsForm>
      ) : (
        <ErrorState
          title="Failed to Load Configuration"
          message="Unable to load data store configuration. Please try again."
          onRetry={() => window.location.reload()}
        />
      )}
    </Box>
  );
}

export default DataStorePage;
