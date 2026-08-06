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

import React, { useCallback, useEffect, useState } from 'react';
import { Box, Flex, Text } from '@radix-ui/themes';
import { Loader2, Trash2, CheckCircle } from 'lucide-react';
import { ExtJS } from '../../../../../../interface/ExtJS';

import {
  SettingsForm,
  SettingsTextInput,
  SettingsPasswordInput,
  SettingsCheckbox,
  SettingsButton,
  SettingsAlert,
  SettingsFormSection,
} from '../../../../shared/form';
import { HelpSection, PageHeader } from '../../../../shared';
import { useCrowdSettings } from './useCrowdSettings';
import { CrowdConfig, CrowdPageProps } from './types';

import './CrowdPage.scss';

const navigateTo = (path: string) => {
  window.location.hash = path;
};

// Fields with inline validation errors that participate in touched-gating.
const VALIDATED_FIELDS = ['url', 'applicationName', 'applicationPassword', 'timeout'] as const;

/**
 * CrowdPage - Atlassian Crowd configuration page for Preview UI
 *
 * State/validation/async are owned by the XState machine (via useCrowdSettings).
 * This component adds two presentation-only concerns retained from upstream:
 * - touched-gating: validation errors are only shown for fields the user has
 *   interacted with (or after a save attempt reveals all);
 * - a raw timeout string so non-numeric/blank input never renders as "NaN".
 */
export function CrowdPage({ className }: CrowdPageProps) {
  const {
    config,
    validationErrors,
    isDirty,
    isFormValid,
    isInitialLoading,
    isBusy,
    error,
    handleChange,
    handleSubmit,
    handleDiscard,
    handleVerifyConnection,
    handleClearCache,
    clearError,
  } = useCrowdSettings();

  const canRead = ExtJS.checkPermission('nexus:crowd:read');
  const canUpdate = ExtJS.checkPermission('nexus:crowd:update');

  // Presentation-only: which fields to reveal errors for.
  const [touched, setTouched] = useState<Record<string, boolean>>({});
  // Presentation-only: raw string for the timeout input so NaN/null never reach the DOM value.
  const [timeoutRaw, setTimeoutRaw] = useState<string>('');

  // Keep the raw timeout string in sync with the machine's config whenever the
  // form is pristine (initial load, after discard, after a successful save).
  // While the form is dirty the user's raw input is authoritative.
  useEffect(() => {
    if (!isDirty) {
      const t = config.timeout;
      setTimeoutRaw(t != null && !Number.isNaN(t) ? String(t) : '');
    }
  }, [isDirty, config.timeout]);

  const markTouched = useCallback((field: string) => {
    setTouched((prev) => (prev[field] ? prev : { ...prev, [field]: true }));
  }, []);

  const markAllTouched = useCallback(() => {
    setTouched(Object.fromEntries(VALIDATED_FIELDS.map((f) => [f, true])));
  }, []);

  // Change handler that also records the field as touched (fallback for
  // environments where blur may not fire).
  const changeField = useCallback(
    (field: keyof CrowdConfig, value: string | boolean | number | undefined) => {
      handleChange(field, value);
      markTouched(field as string);
    },
    [handleChange, markTouched]
  );

  const changeTimeout = useCallback(
    (val: string) => {
      setTimeoutRaw(val);
      markTouched('timeout');
      // Empty clears the value; non-numeric input flows through as NaN so the
      // machine's validation surfaces "Timeout must be a number".
      handleChange('timeout', val === '' ? undefined : Number(val));
    },
    [handleChange, markTouched]
  );

  const onSubmit = useCallback(() => {
    // Reveal all field errors on a save attempt; the machine's guard still
    // blocks the actual save while the form is invalid.
    markAllTouched();
    handleSubmit();
  }, [markAllTouched, handleSubmit]);

  const onVerify = useCallback(() => {
    markAllTouched();
    handleVerifyConnection();
  }, [markAllTouched, handleVerifyConnection]);

  const onDiscard = useCallback(() => {
    handleDiscard();
    setTouched({});
  }, [handleDiscard]);

  // Only show errors for fields the user has interacted with.
  const visibleErrors: Record<string, string> = Object.fromEntries(
    Object.entries(validationErrors).filter(([field]) => touched[field])
  );

  // Check if URL is HTTPS for truststore option
  const showTrustStore = config.url?.startsWith('https');

  // Show aggregate error banner when the form is dirty with validation errors
  const showValidationSummary = isDirty && !isFormValid;
  const validationErrorCount = Object.keys(validationErrors).length;

  if (isInitialLoading) {
    return (
      <Box
        className={`crowd-page ${className || ''}`.trim()}
        data-testid="crowd-page"
        data-view="edit"
        data-loading="true"
        aria-busy="true"
        aria-live="polite"
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
          <SettingsAlert type="error" onClose={clearError}>
            {error}
          </SettingsAlert>
        </Box>
      )}

      {showValidationSummary && (
        <Box className="crowd-page__alerts">
          <SettingsAlert type="error">
            {`Please fix ${validationErrorCount} validation error${validationErrorCount !== 1 ? 's' : ''} to continue.`}
          </SettingsAlert>
        </Box>
      )}

      {/* Permission Warning */}
      {!canUpdate && (
        <Box className="crowd-page__alerts">
          <SettingsAlert type="warning">
            You don't have permission to edit this page. Contact your administrator to request access.
          </SettingsAlert>
        </Box>
      )}

      {/* Form */}
      <SettingsForm
        title=""
        showHeader={false}
        onSubmit={canUpdate ? onSubmit : undefined}
        onCancel={canUpdate ? onDiscard : undefined}
        loading={isBusy}
        dirty={isDirty}
        showActions={canRead || canUpdate}
        testId="crowd-form"
        className="crowd-page__form"
        data-valid={isFormValid ? 'true' : 'false'}
        data-mode="edit"
        submitAnalyticsId="nxrm-crowd-save"
        cancelAnalyticsId="nxrm-crowd-discard"
        footerExtra={
          canRead && (
            <Flex gap="2">
              <SettingsButton
                type="button"
                variant="secondary"
                onClick={onVerify}
                disabled={isBusy || !isFormValid}
                icon={CheckCircle}
                data-analytics-id="nxrm-crowd-verify-connection"
              >
                Verify connection
              </SettingsButton>
              {canUpdate && (
                <SettingsButton
                  type="button"
                  variant="secondary"
                  onClick={handleClearCache}
                  disabled={isBusy}
                  icon={Trash2}
                  data-analytics-id="nxrm-crowd-clear-cache"
                >
                  Clear cache
                </SettingsButton>
              )}
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
            onChange={(checked) => changeField('enabled', checked)}
            description="Enable Crowd Capability"
            disabled={!canUpdate}
            analyticsId="nxrm-crowd-toggle-enabled"
          />
          <SettingsCheckbox
            name="realmActive"
            label="Enable Crowd Realm for authentication"
            checked={config.realmActive}
            onChange={(checked) => changeField('realmActive', checked)}
            description={<>To control ordering, go to the <a href="#preview/admin/security/realms">Realms</a> page.</>}
            disabled={!canUpdate}
            analyticsId="nxrm-crowd-toggle-realm-active"
          />
        </SettingsFormSection>

        {/* Connection Settings */}
        <SettingsFormSection title="Connection">
          <SettingsTextInput
            name="url"
            label="Crowd server URL"
            value={config.url}
            onChange={(val) => changeField('url', val)}
            onBlur={() => markTouched('url')}
            helpText="For example: http://localhost:8095/crowd"
            error={visibleErrors.url}
            disabled={!canUpdate}
            type="url"
            placeholder="http://localhost:8095/crowd"
            required
          />

          {showTrustStore && (
            <SettingsCheckbox
              name="useTrustStoreForUrl"
              label="Use the NXRM truststore"
              checked={config.useTrustStoreForUrl}
              onChange={(checked) => changeField('useTrustStoreForUrl', checked)}
              description={<>Use certificates stored in the NXRM truststore to connect to external systems. <a href="#preview/admin/security/sslcertificates">Configure the NXRM truststore</a></>}
              disabled={!canUpdate}
              analyticsId="nxrm-crowd-toggle-truststore"
            />
          )}

          <SettingsTextInput
            name="applicationName"
            label="Crowd application name"
            value={config.applicationName}
            onChange={(val) => changeField('applicationName', val)}
            onBlur={() => markTouched('applicationName')}
            error={visibleErrors.applicationName}
            required
            disabled={!canUpdate}
            autoComplete="off"
          />

          <SettingsPasswordInput
            name="applicationPassword"
            label="Crowd application password"
            value={config.applicationPassword}
            onChange={(val) => changeField('applicationPassword', val)}
            onBlur={() => markTouched('applicationPassword')}
            error={visibleErrors.applicationPassword}
            required
            disabled={!canUpdate}
            autoComplete="new-password"
          />

          <SettingsTextInput
            name="timeout"
            label="Connection timeout"
            value={timeoutRaw}
            onChange={changeTimeout}
            onBlur={() => markTouched('timeout')}
            helpText="Seconds to wait for activity before stopping and retrying the connection. Leave blank to use the globally defined HTTP timeout."
            error={visibleErrors.timeout}
            disabled={!canUpdate}
            type="text"
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
