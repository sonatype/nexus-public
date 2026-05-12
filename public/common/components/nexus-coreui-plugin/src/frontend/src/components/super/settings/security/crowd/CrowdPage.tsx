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


import React, { useState, useEffect, useCallback } from 'react';
import { Box, Flex, Text } from '@radix-ui/themes';
import { Loader2, Trash2, CheckCircle, AlertTriangle } from 'lucide-react';
import { ExtJS } from '@sonatype/nexus-ui-plugin';

import {
  SettingsForm,
  SettingsTextInput,
  SettingsPasswordInput,
  SettingsCheckbox,
  SettingsButton,
  SettingsAlert,
  SettingsFormSection,
} from '../../../shared/form';
import { HelpSection, clearDirtyState, useToast, PageHeader } from '../../../../shared';
import { useCrowdApi } from './useCrowdApi';
import { CrowdConfig, DEFAULT_CROWD_CONFIG, CrowdPageProps } from './types';

import './CrowdPage.scss';

/**
 * CrowdPage - Atlassian Crowd configuration page for Preview UI
 */
export function CrowdPage({ className }: CrowdPageProps) {
  const { loading, error, setError, fetchConfig, saveConfig, verifyConnection, clearCache } = useCrowdApi();
  const [config, setConfig] = useState<CrowdConfig>(DEFAULT_CROWD_CONFIG);
  const [pristineConfig, setPristineConfig] = useState<CrowdConfig>(DEFAULT_CROWD_CONFIG);
  const [loadingInitial, setLoadingInitial] = useState(true);
  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({});

  // Toast notifications (app-level provider)
  const toast = useToast();

  const canUpdate = ExtJS.checkPermission('nexus:crowd:update');

  // Load configuration on mount
  useEffect(() => {
    const loadConfig = async () => {
      setLoadingInitial(true);
      try {
        const data = await fetchConfig();
        setConfig(data);
        setPristineConfig(data);
      } catch (err) {
        // Error handled by hook
      } finally {
        setLoadingInitial(false);
      }
    };

    loadConfig();
  }, [fetchConfig]);

  // Check if form is pristine
  const isPristine = JSON.stringify(config) === JSON.stringify(pristineConfig);

  // Calculate dirty state for unsaved changes warning
  const isDirty = !isPristine;

  // Handle field change
  const handleChange = useCallback((field: keyof CrowdConfig, value: string | boolean | number | undefined) => {
    setConfig((prev) => ({ ...prev, [field]: value }));
    // Clear validation error when field is modified
    setValidationErrors((prev) => {
      const next = { ...prev };
      delete next[field];
      return next;
    });
  }, []);

  // Validate form
  const validateForm = useCallback((): boolean => {
    const errors: Record<string, string> = {};

    // URL validation
    if (config.url) {
      const urlPattern = /^https?:\/\/.+/;
      if (!urlPattern.test(config.url)) {
        errors.url = 'URL is not valid';
      }
    }

    // Required fields
    if (!config.applicationName?.trim()) {
      errors.applicationName = 'Application name is required';
    }
    if (!config.applicationPassword?.trim()) {
      errors.applicationPassword = 'Application password is required';
    }

    // Timeout validation
    if (config.timeout !== undefined && config.timeout !== null) {
      const timeout = Number(config.timeout);
      if (isNaN(timeout) || timeout < 1 || timeout > 3600) {
        errors.timeout = 'Timeout must be between 1 and 3600 seconds';
      }
    }

    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  }, [config]);

  // Handle form submit
  const handleSubmit = useCallback(async (e: React.FormEvent) => {
    e.preventDefault();

    if (!validateForm()) {
      return;
    }

    try {
      await saveConfig(config);
      clearDirtyState('crowd-form');
      setPristineConfig(config);
      toast.success('Atlassian Crowd settings updated');
    } catch (err) {
      throw err;
    }
  }, [validateForm, saveConfig, config, toast]);

  // Handle discard
  const handleDiscard = useCallback(() => {
    setConfig(pristineConfig);
    setValidationErrors({});
    setError(null);
  }, [pristineConfig, setError]);

  // Handle verify connection
  const handleVerifyConnection = useCallback(async () => {
    if (!validateForm()) {
      return;
    }

    try {
      await verifyConnection(config);
      toast.success('Connection to Crowd server verified');
    } catch (err) {
      // Error handled by hook
    }
  }, [validateForm, verifyConnection, config, toast]);

  // Handle clear cache
  const handleClearCache = useCallback(async () => {
    try {
      await clearCache();
      toast.success('Crowd cache has been cleared');
    } catch (err) {
      // Error handled by hook
    }
  }, [clearCache, toast]);

  // Check if URL is HTTPS for truststore option
  const showTrustStore = config.url?.startsWith('https');

  // Calculate form validity
  const isFormValid = Object.keys(validationErrors).length === 0;

  if (loadingInitial) {
    return (
      <Box
        className={`crowd-page ${className || ''}`.trim()}
        data-testid="crowd-page"
        data-view="edit"
      >
        <Flex align="center" justify="center" className="crowd-page__loading">
          <Loader2 size={24} className="crowd-page__spinner" />
          <Text size="2">Loading Crowd configuration...</Text>
        </Flex>
      </Box>
    );
  }

  return (
    <Box
      className={`crowd-page ${className || ''}`.trim()}
      data-testid="crowd-page"
      data-view="edit"
      px={{ initial: '4', md: '6', lg: '6' }}
      py={{ initial: '4', md: '5', lg: '6' }}
    >
      {/* Header */}
      <Box mb="4">
        <PageHeader
          title="Atlassian Crowd"
          description="Manage Atlassian Crowd configuration for user authentication"
        
          breadcrumbs={[
            { label: 'Settings', onClick: () => navigateTo('#preview/admin/settings') },
            { label: 'Crowd' }
          ]}
/>
      </Box>

      {/* Alerts */}
      {error && (
        <Box className="crowd-page__alerts">
          <SettingsAlert type="error" onClose={() => setError(null)}>
            {error}
          </SettingsAlert>
        </Box>
      )}

      {/* Permission Warning */}
      {!canUpdate && (
        <Box className="crowd-page__alerts">
          <SettingsAlert type="warning">
            <Flex align="center" gap="2">
              <AlertTriangle size={16} />
              You don't have permission to edit this page. Contact your administrator to request access.
            </Flex>
          </SettingsAlert>
        </Box>
      )}

      {/* Form */}
      <SettingsForm
        title=""
        showHeader={false}
        onSubmit={handleSubmit}
        onCancel={handleDiscard}
        loading={loading}
        dirty={isDirty}
        submitDisabled={!isFormValid}
        showActions={canUpdate}
        testId="crowd-form"
        className="crowd-page__form"
        data-valid={isFormValid ? 'true' : 'false'}
        data-mode="edit"
        footerExtra={
          canUpdate && (
            <Flex gap="2">
              <SettingsButton
                type="button"
                variant="secondary"
                onClick={handleVerifyConnection}
                disabled={loading || Object.keys(validationErrors).length > 0}
                icon={CheckCircle}
              >
                Verify connection
              </SettingsButton>
              <SettingsButton
                type="button"
                variant="secondary"
                onClick={handleClearCache}
                disabled={loading}
                icon={Trash2}
              >
                Clear cache
              </SettingsButton>
            </Flex>
          )
        }
      >
        {/* Basic Settings */}
        <SettingsFormSection title="Settings">
          <SettingsCheckbox
            name="enabled"
            label="Enable Crowd"
            checked={config.enabled}
            onChange={(checked) => handleChange('enabled', checked)}
            helpText="Enable Crowd Capability"
            disabled={!canUpdate}
          />
          <SettingsCheckbox
            name="realmActive"
            label="Enable Crowd Realm for authentication"
            checked={config.realmActive}
            onChange={(checked) => handleChange('realmActive', checked)}
            helpText="To control ordering, go to the Realms page"
            disabled={!canUpdate}
          />
        </SettingsFormSection>

        {/* Connection Settings */}
        <SettingsFormSection title="Connection">
          <SettingsTextInput
            name="url"
            label="Crowd server URL"
            value={config.url}
            onChange={(val) => handleChange('url', val)}
            helpText="For example: http://localhost:8095/crowd"
            error={validationErrors.url}
            disabled={!canUpdate}
            type="url"
            placeholder="http://localhost:8095/crowd"
          />

          {showTrustStore && (
            <SettingsCheckbox
              name="useTrustStoreForUrl"
              label="Use the NXRM truststore"
              checked={config.useTrustStoreForUrl}
              onChange={(checked) => handleChange('useTrustStoreForUrl', checked)}
              helpText="Use certificates stored in the NXRM truststore to connect to external systems"
              disabled={!canUpdate}
            />
          )}

          <SettingsTextInput
            name="applicationName"
            label="Crowd application name"
            value={config.applicationName}
            onChange={(val) => handleChange('applicationName', val)}
            error={validationErrors.applicationName}
            required
            disabled={!canUpdate}
            autoComplete="off"
          />

          <SettingsPasswordInput
            name="applicationPassword"
            label="Crowd application password"
            value={config.applicationPassword}
            onChange={(val) => handleChange('applicationPassword', val)}
            error={validationErrors.applicationPassword}
            required
            disabled={!canUpdate}
            autoComplete="new-password"
          />

          <SettingsTextInput
            name="timeout"
            label="Connection timeout"
            value={config.timeout !== undefined ? String(config.timeout) : ''}
            onChange={(val) => handleChange('timeout', val ? Number(val) : undefined)}
            helpText="Seconds to wait for activity before stopping and retrying the connection. Leave blank to use the globally defined HTTP timeout."
            error={validationErrors.timeout}
            disabled={!canUpdate}
            type="number"
            min={1}
            max={3600}
          />
        </SettingsFormSection>

        {/* Help Section */}
        <HelpSection
          title="About Atlassian Crowd"
          content="Atlassian Crowd is a centralized identity management application that allows you to manage users from multiple directories (Active Directory, LDAP, Crowd, etc.) and control application authentication permissions in one place. When configured, Nexus Repository can authenticate users against your Crowd server."
          docLink={{
            label: 'View Crowd Documentation',
            href: 'http://links.sonatype.com/products/nxrm3/docs/crowd',
          }}
        />
      </SettingsForm>
    </Box>
  );
}

export default CrowdPage;



