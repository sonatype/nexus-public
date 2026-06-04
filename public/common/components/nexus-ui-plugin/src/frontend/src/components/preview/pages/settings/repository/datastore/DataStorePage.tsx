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
import { Box, Flex, Text, ScrollArea, Table } from '@radix-ui/themes';
import { Database, Save, RotateCcw, AlertCircle } from 'lucide-react';
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
import { useToast } from '../../../../shared';
import { useDataStoreApi } from './useDataStoreApi';
import { 
  DataStoreConfig, 
  DataStoreFormErrors,
  EffectiveParameter,
  validateConnectionPool,
  validateJdbcParameters,
  parseAdvancedString,
  serializeParameters,
  calculateEffectiveConfig,
} from './types';
import { JdbcParameterEditor, JdbcParameter, ParameterValidation } from './JdbcParameterEditor';

import './DataStorePage.scss';

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
  const [config, setConfig] = useState<DataStoreConfig | null>(null);
  const [initialConfig, setInitialConfig] = useState<DataStoreConfig | null>(null);
  const [loadingData, setLoadingData] = useState(true);
  const [errors, setErrors] = useState<DataStoreFormErrors>({});

  // Toast notifications (app-level provider)
  const toast = useToast();
  
  // JDBC parameters as structured data
  const [jdbcParameters, setJdbcParameters] = useState<JdbcParameter[]>([]);
  const [initialJdbcParameters, setInitialJdbcParameters] = useState<JdbcParameter[]>([]);
  const [parameterValidations, setParameterValidations] = useState<ParameterValidation[]>([]);
  const [hasParameterErrors, setHasParameterErrors] = useState(false);

  // Show all validation errors (triggered on save attempt)
  const [showAllValidation, setShowAllValidation] = useState(false);

  // Reset confirmation state
  const [showResetConfirm, setShowResetConfirm] = useState(false);

  const {
    loading,
    error,
    setError,
    fetchConfig,
    updateConfig,
  } = useDataStoreApi();

  const canUpdate = ExtJS.checkPermission('nexus:settings:update');

  // Load data on mount
  useEffect(() => {
    const loadData = async () => {
      setLoadingData(true);
      setError(null);
      try {
        const data = await fetchConfig();
        setConfig(data);
        setInitialConfig(data);
        
        // Parse advanced string into parameters
        const params = parseAdvancedString(data.advanced || '');
        setJdbcParameters(params);
        setInitialJdbcParameters(params);
      } catch (err: unknown) {
        const message = err instanceof Error ? err.message : 'Failed to load configuration';
        setError(message);
      } finally {
        setLoadingData(false);
      }
    };

    loadData();
  }, [fetchConfig, setError]);

  // Validate parameters when they change
  useEffect(() => {
    const { validations, hasBlockingErrors } = validateJdbcParameters(jdbcParameters);
    setParameterValidations(validations);
    setHasParameterErrors(hasBlockingErrors);
  }, [jdbcParameters]);

  // Check if form has changes
  const isDirty = useMemo(() => {
    if (!config || !initialConfig) return false;
    
    const poolChanged = config.maximumConnectionPool !== initialConfig.maximumConnectionPool;
    const paramsChanged = serializeParameters(jdbcParameters) !== serializeParameters(initialJdbcParameters);
    
    return poolChanged || paramsChanged;
  }, [config, initialConfig, jdbcParameters, initialJdbcParameters]);

  // Calculate effective configuration for preview (includes pool size + JDBC params)
  const effectiveConfig = useMemo<EffectiveParameter[]>(() => {
    const jdbcParams = calculateEffectiveConfig(jdbcParameters);
    
    // Include connection pool size at the top
    const poolSize = config?.maximumConnectionPool;
    const poolSizeChanged = initialConfig && String(poolSize) !== String(initialConfig.maximumConnectionPool);
    
    const poolEntry: EffectiveParameter = {
      name: 'maximumConnectionPool',
      value: String(poolSize || 10),
      source: poolSizeChanged ? 'Custom' : 'Default',
    };
    
    return [poolEntry, ...jdbcParams];
  }, [jdbcParameters, config?.maximumConnectionPool, initialConfig]);

  const handlePoolChange = useCallback((value: string) => {
    if (!config) return;
    setConfig({ ...config, maximumConnectionPool: value });
    
    // Validate inline
    const poolError = validateConnectionPool(value);
    if (poolError) {
      setErrors(prev => ({ ...prev, maximumConnectionPool: poolError }));
    } else {
      setErrors(prev => {
        const next = { ...prev };
        delete next.maximumConnectionPool;
        return next;
      });
    }
  }, [config]);

  const handleParametersChange = useCallback((params: JdbcParameter[]) => {
    setJdbcParameters(params);
  }, []);

  const handleResetParameters = useCallback(() => {
    setShowResetConfirm(true);
  }, []);

  const confirmResetParameters = useCallback(() => {
    // Remove all custom parameters, keep only defaults
    const defaultsOnly = jdbcParameters.filter(p => p.isDefault && !p.isCustom);
    setJdbcParameters(defaultsOnly);
    setShowResetConfirm(false);
  }, [jdbcParameters]);

  const handleSave = useCallback(async () => {
    if (!config) return;

    // Validate connection pool
    const poolError = validateConnectionPool(config.maximumConnectionPool);
    if (poolError) {
      setErrors({ maximumConnectionPool: poolError });
      return;
    }

    // Check for parameter validation errors - show all validation on save attempt
    if (hasParameterErrors) {
      setShowAllValidation(true);
      return;
    }

    // Serialize parameters back to advanced string
    const advancedString = serializeParameters(jdbcParameters);

    try {
      // Send the complete config object to the backend, updating only the editable fields
      // - maximumConnectionPool (Integer)
      // - advanced (String)
      // We explicitly construct the payload to ensure only required fields are sent
      // as some read-only fields might cause issues if sent back (Task 2)
      const payload = {
        maximumConnectionPool: parseInt(String(config.maximumConnectionPool), 10) || 10,
        advanced: advancedString || '',
      };
      
      const result = await updateConfig(payload as any);
      setInitialConfig(result);
      setConfig(result);
      setInitialJdbcParameters([...jdbcParameters]);
      setShowAllValidation(false);
      toast.success('Data store configuration saved successfully');
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : String(err);
      if (message.includes('invalid or contains unknown')) {
        setError('One or more advanced JDBC parameters are not recognized by the database. Remove unknown parameters and try again.');
      } else {
        setError(message);
      }
      throw err;
    }
  }, [config, jdbcParameters, hasParameterErrors, updateConfig, toast, setError]);

  const handleDiscard = useCallback(() => {
    if (initialConfig) {
      setConfig({ ...initialConfig });
      setJdbcParameters([...initialJdbcParameters]);
      setErrors({});
      setShowAllValidation(false);
    }
  }, [initialConfig, initialJdbcParameters]);

  // Determine database type from JDBC URL
  const databaseType = useMemo(() => {
    if (!config?.jdbcUrl) return 'Not configured';
    if (config.jdbcUrl.includes('postgresql')) return 'PostgreSQL';
    if (config.jdbcUrl.includes('h2')) return 'H2';
    if (config.jdbcUrl.includes('mysql')) return 'MySQL';
    if (config.jdbcUrl.includes('oracle')) return 'Oracle';
    if (config.jdbcUrl.includes('sqlserver')) return 'SQL Server';
    return 'Unknown';
  }, [config?.jdbcUrl]);

  const canSave = isDirty && !hasParameterErrors && !errors.maximumConnectionPool;

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
            onRetry={() => setError(null)}
            retryText="Dismiss"
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
                <SettingsButton variant="secondary" onClick={() => setShowResetConfirm(false)}>
                  Cancel
                </SettingsButton>
                <SettingsButton variant="danger" onClick={confirmResetParameters}>
                  Reset to Defaults
                </SettingsButton>
              </Flex>
            </Flex>
          </SettingsAlert>
        </Box>
      )}

      {/* Content */}
      <Box className="datastore-page__content">
        {loadingData ? (
          <LoadingState message="Loading configuration..." />
        ) : config ? (
          <SettingsForm
            testId="datastore-form"
            onSubmit={handleSave}
            onCancel={handleDiscard}
            loading={loading}
            pristine={!isDirty}
            submitDisabled={!canUpdate || !canSave}
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
                    {config.jdbcUrl || 'Not configured'}
                  </Text>
                </Box>
                <Box className="datastore-page__readonly-field">
                  <Text size="2" weight="medium" className="datastore-page__readonly-label">
                    Schema
                  </Text>
                  <Text size="2" className="datastore-page__readonly-value">
                    {config.schema || 'Not configured'}
                  </Text>
                </Box>
                <Box className="datastore-page__readonly-field">
                  <Text size="2" weight="medium" className="datastore-page__readonly-label">
                    Username
                  </Text>
                  <Text size="2" className="datastore-page__readonly-value">
                    {config.username || 'Not configured'}
                  </Text>
                </Box>
              </Box>
            </SettingsFormSection>

            {/* 2. Connection Pool Settings */}
            <SettingsFormSection title="Connection Pool Settings">
              <SettingsTextInput
                name="maximumConnectionPool"
                label="Maximum Connection Pool Size"
                value={String(config.maximumConnectionPool)}
                onChange={handlePoolChange}
                type="number"
                min={1}
                max={3000}
                helpText="Maximum number of database connections in the pool. Default: 10. Valid range: 1-3000."
                error={errors.maximumConnectionPool}
                disabled={!canUpdate || loading}
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
                parameters={jdbcParameters}
                onChange={handleParametersChange}
                showAllValidation={showAllValidation}
                onReset={handleResetParameters}
                disabled={!canUpdate || loading}
                validations={parameterValidations}
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
    </Box>
  );
}

export default DataStorePage;
