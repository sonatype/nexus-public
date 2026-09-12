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

import React, { useCallback, useEffect, useRef, useState } from 'react';
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
// The Enable checkboxes are intentionally excluded — they have no inline
// text error of their own, only a side effect of revealing these fields'.
const VALIDATED_FIELDS = ['url', 'applicationName', 'applicationPassword', 'timeout'] as const;

const LOADING_MESSAGE = 'Loading Crowd configuration...';

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
    isSaving,
    isVerifyingConnection,
    isClearingCache,
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

  // A11y (NEXUS-54024, following the NEXUS-53625 pattern in LdapForm.tsx):
  // screen-reader announcer for loading/busy/verify-failure transitions. The
  // node is mounted *fresh* on each announcement (conditional render, keyed by
  // an incrementing counter) rather than kept persistent with its text swapped
  // in place — VoiceOver reliably announces a newly-inserted node but
  // frequently misses a text change inside an already-present live region.
  // Always assertive/role="alert" (matching LdapForm.tsx's only
  // proven-in-VoiceOver announcement style, NEXUS-53625): a polite/
  // role="status" announcement here queues behind VoiceOver's own page-load
  // narration and is reliably dropped rather than spoken once that narration
  // finishes.
  const [status, setStatus] = useState<{ message: string; key: number }>({ message: '', key: 0 });
  const announce = useCallback((message: string) => {
    setStatus((prev) => ({ message, key: prev.key + 1 }));
  }, []);

  // Each announcing effect (F1/F2/F3) tracks the message *it* put up here, and
  // only clears the live region if that message is still showing — otherwise
  // a slow effect's cleanup could stomp a newer message from a different
  // effect. Shared across F1/F2/F3 rather than one ref per effect since at
  // most one of them is ever active at a time.
  const activeMessage = useRef<string | null>(null);
  const announceActive = useCallback(
    (message: string) => {
      activeMessage.current = message;
      announce(message);
    },
    [announce]
  );
  const clearIfActive = useCallback((message: string) => {
    if (activeMessage.current !== message) return;
    activeMessage.current = null;
    setStatus((prev) => (prev.message === message ? { message: '', key: prev.key } : prev));
  }, []);

  // F1: announce the loading state once, on mount, while the initial load is
  // in flight. The loading Box itself unmounts as soon as data arrives, so
  // this fresh-mount announcer is not tied to isInitialLoading's lifetime.
  // eslint-disable-next-line react-hooks/exhaustive-deps
  // biome-ignore lint/correctness/useExhaustiveDependencies: intentionally mount-only; see comment above.
  useEffect(() => {
    if (isInitialLoading) {
      announceActive(LOADING_MESSAGE);
    }
  }, []);

  // Clear the F1 loading announcement once the initial load completes, so it
  // doesn't linger in the live region after the form has rendered.
  useEffect(() => {
    if (!isInitialLoading) {
      clearIfActive(LOADING_MESSAGE);
    }
  }, [isInitialLoading, clearIfActive]);

  // F2: announce each busy operation's start, and clear it once the operation
  // settles (success or failure) so a stale "Saving..." message does not
  // linger — a failure replaces it with F3's error announcement below; a
  // success just clears it, since the visible toast covers that case for
  // sighted users.
  useEffect(() => {
    if (isSaving) {
      announceActive('Saving Crowd settings...');
    } else if (isVerifyingConnection) {
      announceActive('Verifying connection...');
    } else if (isClearingCache) {
      announceActive('Clearing cache...');
    } else {
      clearIfActive('Saving Crowd settings...');
      clearIfActive('Verifying connection...');
      clearIfActive('Clearing cache...');
    }
  }, [isSaving, isVerifyingConnection, isClearingCache, announceActive, clearIfActive]);

  // F3: announce a verify-connection/save failure assertively, matching the
  // visible error banner's text so both channels agree. Clear the announcer
  // once the error is dismissed/resolved so a stale failure message doesn't
  // linger after a later operation succeeds. Tracks the exact error string it
  // last announced (lastError) rather than reading activeMessage back, so
  // this effect only ever clears a message it put up itself and never stomps
  // a live F1/F2 announcement.
  const lastError = useRef<string | null>(null);
  useEffect(() => {
    if (error) {
      lastError.current = error;
      announceActive(error);
    } else if (lastError.current) {
      clearIfActive(lastError.current);
      lastError.current = null;
    }
  }, [error, announceActive, clearIfActive]);

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

  // The Enable checkboxes are part of the form like any other field, so
  // toggling either one reveals every field's inline error (not just its
  // own) rather than waiting for each field to be individually touched, so
  // the user sees the full set of required fields to fix in one pass.
  const changeEnabled = useCallback(
    (checked: boolean) => {
      handleChange('enabled', checked);
      markAllTouched();
    },
    [handleChange, markAllTouched]
  );

  const changeRealmActive = useCallback(
    (checked: boolean) => {
      handleChange('realmActive', checked);
      markAllTouched();
    },
    [handleChange, markAllTouched]
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

  // A11y (NEXUS-54024): visually-hidden announcer, mounted fresh per message
  // via `key` so VoiceOver reliably announces loading/busy/error transitions.
  // Rendered in both branches below — including the early loading return —
  // since the announce-on-mount effect for F1 fires before isInitialLoading
  // flips false, and this node must already be present in the DOM to catch it
  // reliably (a node inserted together with its first text is missed just as
  // often as one removed and replaced). See LdapForm.tsx:516-536 for the
  // pattern this mirrors.
  const announcer = status.message && (
    <Box
      key={status.key}
      role="alert"
      aria-live="assertive"
      aria-atomic="true"
      data-testid="crowd-page-status"
      className="crowd-page__sr-status"
    >
      {status.message}
    </Box>
  );

  if (isInitialLoading) {
    return (
      <Box
        className={`crowd-page ${className || ''}`.trim()}
        data-testid="crowd-page"
        data-view="edit"
        data-loading="true"
        aria-busy="true"
      >
        {announcer}
        <Flex align="center" justify="center" className="crowd-page__loading">
          <Loader2 size={24} className="crowd-page__spinner" />
          <Text size="2">{LOADING_MESSAGE}</Text>
        </Flex>
      </Box>
    );
  }

  return (
    <Box
      className={`crowd-page ${className || ''}`.trim()}
      data-testid="crowd-page"
      data-view="edit"
      data-loading={isBusy ? 'true' : 'false'}
      aria-busy={isBusy}
      px={{ initial: '4', md: '6', lg: '6' }}
      py={{ initial: '4', md: '5', lg: '6' }}
    >
      {announcer}

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
            onChange={changeEnabled}
            description="Enable Crowd Capability"
            disabled={!canUpdate}
            analyticsId="nxrm-crowd-toggle-enabled"
          />
          <SettingsCheckbox
            name="realmActive"
            label="Enable Crowd Realm for authentication"
            checked={config.realmActive}
            onChange={changeRealmActive}
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
