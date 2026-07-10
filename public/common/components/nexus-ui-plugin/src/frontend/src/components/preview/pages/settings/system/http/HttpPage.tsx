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
import { Box, Flex, Text } from '@radix-ui/themes';
import { Loader2, Info, ExternalLink, Plus, Trash2 } from 'lucide-react';
import { ExtJS } from '../../../../../../interface/ExtJS';

import { PageHeader } from '../../../../shared';
import {
  SettingsForm,
  SettingsFormSection,
  SettingsCheckbox,
  SettingsTextInput,
  SettingsPasswordInput,
  SettingsSelect,
  SettingsButton,
  SettingsAlert,
} from '../../../../shared/form';
import { useHttpForm } from './useHttpForm';
import {
  HttpConfiguration,
  HttpPageProps,
} from './types';

import './HttpPage.scss';

const navigateTo = (path: string) => {
  window.location.hash = path;
};

const ALLOWED_NUMBER_KEYS = new Set([
  'Backspace', 'Delete', 'Tab', 'Escape', 'Enter',
  'ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown',
  'Home', 'End',
]);

function handleNumberKeyDown(e: React.KeyboardEvent) {
  if (ALLOWED_NUMBER_KEYS.has(e.key)) return;
  if (e.ctrlKey || e.metaKey) return;
  if (!/^[0-9]$/.test(e.key)) {
    e.preventDefault();
  }
}

function Row({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <Flex className="http-page__row" gap="3">
      <Text size="2" weight="medium" className="http-page__label">{label}</Text>
      <Text size="2">{value}</Text>
    </Flex>
  );
}

/**
 * HttpPage - HTTP Settings configuration page for Preview UI
 *
 * Configures HTTP proxy settings, timeouts, and retry behavior.
 * Uses XState form machine for state management.
 */
export function HttpPage({ className }: HttpPageProps) {
  // XState form hook handles load, save, dirty tracking, toast, validation
  const form = useHttpForm();

  const canUpdate = ExtJS.checkPermission('nexus:settings:update');
  const formData = form.data as HttpConfiguration;
  const isProxyEnabled = formData.httpEnabled || formData.httpsEnabled;

  // Local UI state for the "add non-proxy host" input
  const [newNonProxyHost, setNewNonProxyHost] = useState('');
  const [nonProxyHostError, setNonProxyHostError] = useState<string | null>(null);


  // Add non-proxy host(s) — supports comma-separated paste
  const handleAddNonProxyHost = useCallback(() => {
    const raw = newNonProxyHost;
    if (!raw.trim()) return;

    const allTokens = raw.split(',').map((t) => t.trim()).filter((t) => t.length > 0);
    const validTokens = allTokens.filter((t) => !/\s/.test(t));
    const invalidTokens = allTokens.filter((t) => /\s/.test(t));

    if (invalidTokens.length > 0) {
      setNonProxyHostError(
        `Skipped ${invalidTokens.length} invalid entr${invalidTokens.length === 1 ? 'y' : 'ies'} containing spaces: ${invalidTokens.map((t) => `"${t}"`).join(', ')}`
      );
    } else {
      setNonProxyHostError(null);
    }

    if (validTokens.length === 0) return;

    const existing = formData.nonProxyHosts;
    const additions = validTokens.filter((t: string) => !existing.includes(t));
    if (additions.length === 0) {
      setNewNonProxyHost('');
      return;
    }

    const next = [...existing, ...additions];
    form.send({ type: 'UPDATE', name: 'nonProxyHosts', value: next } as any);
    setNewNonProxyHost('');
  }, [newNonProxyHost, formData.nonProxyHosts, form]);

  // Remove non-proxy host by value (display order is sorted, so index would be wrong)
  const handleRemoveNonProxyHost = useCallback((host: string) => {
    form.send({
      type: 'UPDATE',
      name: 'nonProxyHosts',
      value: formData.nonProxyHosts.filter((h: string) => h !== host),
    } as any);
  }, [formData.nonProxyHosts, form]);

  // Handle Enter key for adding non-proxy host
  const handleNonProxyHostKeyDown = useCallback((e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      handleAddNonProxyHost();
    }
  }, [handleAddNonProxyHost]);

  // Toggle HTTP proxy
  const handleToggleHttpProxy = useCallback(() => {
    const enabling = !formData.httpEnabled;
    form.send({ type: 'UPDATE', name: 'httpEnabled', value: enabling } as any);
    if (!enabling) {
      form.send({ type: 'UPDATE', name: 'httpAuthType', value: '' } as any);
    }
  }, [form, formData.httpEnabled]);

  // Toggle HTTPS proxy
  const handleToggleHttpsProxy = useCallback(() => {
    const enabling = !formData.httpsEnabled;
    form.send({ type: 'UPDATE', name: 'httpsEnabled', value: enabling } as any);
    if (!enabling) {
      form.send({ type: 'UPDATE', name: 'httpsAuthType', value: '' } as any);
    }
  }, [form, formData.httpsEnabled]);

  // Discard changes and clear local UI state
  const handleDiscard = useCallback(() => {
    form.reset();
    setNewNonProxyHost('');
    setNonProxyHostError(null);
  }, [form]);

  // Loading state
  if (form.isLoading) {
    return (
      <Box
        className={`http-page ${className || ''}`.trim()}
        role="status"
        aria-busy="true"
        aria-live="polite"
        data-testid="http-page-loading"
      >
        <Flex align="center" justify="center" gap="3" className="http-page__loading">
          <Loader2 size={24} className="http-page__spinner" />
          <Text size="2">Loading HTTP settings...</Text>
        </Flex>
      </Box>
    );
  }

  // Read-only view for users without update permission — full-config parity with legacy HttpReadOnly
  if (!canUpdate) {
    const showHttpProxy = formData.httpEnabled;
    const showHttpsProxy = formData.httpsEnabled;
    const showNonProxyHosts = (showHttpProxy || showHttpsProxy) && formData.nonProxyHosts.length > 0;

    return (
      <Box className={`http-page ${className || ''}`.trim()}>
        <PageHeader
          title="HTTP Settings"
          description="Configure HTTP proxy and connection settings"
          breadcrumbs={[
            { label: 'Settings', onClick: () => navigateTo('#preview/admin/settings') },
            { label: 'HTTP' }
          ]}
        />

        <Box className="http-page__readonly-banner" mb="3">
          <SettingsAlert type="info">
            You are viewing a read-only version of this page. Some fields are hidden when they
            are at their default values or when authentication is disabled.
          </SettingsAlert>
        </Box>

        <SettingsFormSection title="Connection Settings">
          {formData.userAgentSuffix && <Row label="User-Agent Suffix" value={formData.userAgentSuffix} />}
          {formData.timeout != null && <Row label="Connection Timeout" value={formData.timeout} />}
          {formData.retries != null && <Row label="Connection Retries" value={formData.retries} />}
        </SettingsFormSection>

        {showHttpProxy && (
          <SettingsFormSection title="HTTP Proxy">
            <Row label="Host" value={formData.httpHost || '—'} />
            <Row label="Port" value={formData.httpPort ?? '—'} />
            {formData.httpAuthType === 'username' && (
              <>
                <Row label="Auth Username" value={formData.httpAuthUsername || '—'} />
                {formData.httpAuthNtlmHost && <Row label="NTLM Host" value={formData.httpAuthNtlmHost} />}
                {formData.httpAuthNtlmDomain && <Row label="NTLM Domain" value={formData.httpAuthNtlmDomain} />}
              </>
            )}
          </SettingsFormSection>
        )}

        {showHttpsProxy && (
          <SettingsFormSection title="HTTPS Proxy">
            <Row label="Host" value={formData.httpsHost || '—'} />
            <Row label="Port" value={formData.httpsPort ?? '—'} />
            {formData.httpsAuthType === 'username' && (
              <>
                <Row label="Auth Username" value={formData.httpsAuthUsername || '—'} />
                {formData.httpsAuthNtlmHost && <Row label="NTLM Host" value={formData.httpsAuthNtlmHost} />}
                {formData.httpsAuthNtlmDomain && <Row label="NTLM Domain" value={formData.httpsAuthNtlmDomain} />}
              </>
            )}
          </SettingsFormSection>
        )}

        {showNonProxyHosts && (
          <SettingsFormSection title="Hosts to Exclude from Proxy">
            <Box className="http-page__host-list">
              {[...formData.nonProxyHosts]
                .sort((a: string, b: string) => a.localeCompare(b, undefined, { sensitivity: 'base' }))
                .map((host: string) => (
                  <Text key={host} as="div" size="2">{host}</Text>
              ))}
            </Box>
          </SettingsFormSection>
        )}
      </Box>
    );
  }

  return (
    <Box className={`http-page ${className || ''}`.trim()}>
      <PageHeader
        title="HTTP Settings"
        description="Configure HTTP proxy and connection settings"
        breadcrumbs={[
          { label: 'Settings', onClick: () => navigateTo('#preview/admin/settings') },
          { label: 'HTTP' }
        ]}
      />

      {/* Content area */}
      <Box className="http-page__content">
        <SettingsForm
          testId="http-form"
          onSubmit={() => form.submit()}
          onCancel={handleDiscard}
          loading={form.isSaving}
          pristine={form.isPristine}
          error={form.saveError || undefined}
          submitAnalyticsId="nxrm-http-save"
        >
        <SettingsFormSection title="Connection Settings">
          <SettingsTextInput
            {...form.field('userAgentSuffix')}
            label="User-Agent Suffix"
            helpText="Custom suffix to append to HTTP User-Agent header"
            placeholder="MyCompany/1.0"
          />

          <SettingsTextInput
            name="timeout"
            label="Connection Timeout"
            type="number"
            value={formData.timeout != null ? String(formData.timeout) : ''}
            onChange={(value: string) => form.send({ type: 'UPDATE', name: 'timeout', value: value ? parseInt(value, 10) : null } as any)}
            onBlur={() => form.send({ type: 'BLUR', name: 'timeout' } as any)}
            onKeyDown={handleNumberKeyDown}
            error={form.touched?.timeout ? form.validationErrors?.timeout : undefined}
            helpText="Time in seconds to wait for a connection (1-3600). Leave blank for system default."
            min={1}
            max={3600}
            placeholder="System default"
          />

          <SettingsTextInput
            name="retries"
            label="Connection Retries"
            type="number"
            value={formData.retries != null ? String(formData.retries) : ''}
            onChange={(value: string) => form.send({ type: 'UPDATE', name: 'retries', value: value ? parseInt(value, 10) : null } as any)}
            onBlur={() => form.send({ type: 'BLUR', name: 'retries' } as any)}
            onKeyDown={handleNumberKeyDown}
            error={form.touched?.retries ? form.validationErrors?.retries : undefined}
            helpText="Number of retry attempts (0-10). Leave blank for system default."
            min={0}
            max={10}
            placeholder="System default"
          />
        </SettingsFormSection>

        <SettingsFormSection title="HTTP Proxy">
          <SettingsCheckbox
            name="httpEnabled"
            label="Enable HTTP Proxy"
            checked={formData.httpEnabled}
            onChange={handleToggleHttpProxy}
            description="Route HTTP requests through a proxy server"
          />

          {formData.httpEnabled && (
            <>
              <SettingsTextInput
                {...form.field('httpHost')}
                label="HTTP Proxy Host"
                helpText="Hostname or IP address of the HTTP proxy"
                placeholder="proxy.example.com"
                required
              />

              <SettingsTextInput
                name="httpPort"
                label="HTTP Proxy Port"
                type="number"
                value={formData.httpPort != null ? String(formData.httpPort) : ''}
                onChange={(value: string) => form.send({ type: 'UPDATE', name: 'httpPort', value: value ? parseInt(value, 10) : null } as any)}
                onBlur={() => form.send({ type: 'BLUR', name: 'httpPort' } as any)}
                onKeyDown={handleNumberKeyDown}
                error={form.touched?.httpPort ? form.validationErrors?.httpPort : undefined}
                min={1}
                max={65535}
                required
                placeholder="8080"
              />

              <SettingsFormSection
                key={`http-auth-${formData.httpAuthType !== ''}`}
                title="HTTP Proxy Authentication"
                collapsible
                defaultCollapsed={formData.httpAuthType === ''}
              >
                <SettingsSelect
                  name="httpAuthType"
                  label="Authentication"
                  value={formData.httpAuthType}
                  onChange={(v: string) => form.send({ type: 'UPDATE', name: 'httpAuthType', value: v } as any)}
                  options={[
                    { value: '', label: 'No authentication' },
                    { value: 'username', label: 'Username' },
                  ]}
                  helpText="Type of authentication used to connect to the proxy"
                />

                {formData.httpAuthType === 'username' && (
                  <>
                    <SettingsTextInput
                      {...form.field('httpAuthUsername')}
                      label="Username"
                      helpText="Username for HTTP proxy authentication"
                      required
                      placeholder="proxy-user"
                    />

                    <SettingsPasswordInput
                      {...form.field('httpAuthPassword')}
                      label="Password"
                      helpText="Password for HTTP proxy authentication"
                      autoComplete="new-password"
                    />

                    <SettingsTextInput
                      {...form.field('httpAuthNtlmHost')}
                      label="NTLM Host"
                      helpText="Windows host for NTLM authentication (optional)"
                      placeholder="WORKSTATION01"
                    />

                    <SettingsTextInput
                      {...form.field('httpAuthNtlmDomain')}
                      label="NTLM Domain"
                      helpText="Windows domain for NTLM authentication (optional)"
                      placeholder="COMPANY"
                    />
                  </>
                )}
              </SettingsFormSection>
            </>
          )}
        </SettingsFormSection>

        <SettingsFormSection title="HTTPS Proxy">
          <SettingsCheckbox
            name="httpsEnabled"
            label="Enable HTTPS Proxy"
            checked={formData.httpsEnabled}
            onChange={handleToggleHttpsProxy}
            description="Route HTTPS requests through a proxy server"
          />

          {formData.httpsEnabled && (
            <>
              <SettingsTextInput
                {...form.field('httpsHost')}
                label="HTTPS Proxy Host"
                helpText="Hostname or IP address of the HTTPS proxy"
                placeholder="proxy.example.com"
                required
              />

              <SettingsTextInput
                name="httpsPort"
                label="HTTPS Proxy Port"
                type="number"
                value={formData.httpsPort != null ? String(formData.httpsPort) : ''}
                onChange={(value: string) => form.send({ type: 'UPDATE', name: 'httpsPort', value: value ? parseInt(value, 10) : null } as any)}
                onBlur={() => form.send({ type: 'BLUR', name: 'httpsPort' } as any)}
                onKeyDown={handleNumberKeyDown}
                error={form.touched?.httpsPort ? form.validationErrors?.httpsPort : undefined}
                min={1}
                max={65535}
                required
                placeholder="8443"
              />

              <SettingsFormSection
                key={`https-auth-${formData.httpsAuthType !== ''}`}
                title="HTTPS Proxy Authentication"
                collapsible
                defaultCollapsed={formData.httpsAuthType === ''}
              >
                <SettingsSelect
                  name="httpsAuthType"
                  label="Authentication"
                  value={formData.httpsAuthType}
                  onChange={(v: string) => form.send({ type: 'UPDATE', name: 'httpsAuthType', value: v } as any)}
                  options={[
                    { value: '', label: 'No authentication' },
                    { value: 'username', label: 'Username' },
                  ]}
                  helpText="Type of authentication used to connect to the proxy"
                />

                {formData.httpsAuthType === 'username' && (
                  <>
                    <SettingsTextInput
                      {...form.field('httpsAuthUsername')}
                      label="Username"
                      required
                      placeholder="proxy-user"
                    />

                    <SettingsPasswordInput
                      {...form.field('httpsAuthPassword')}
                      label="Password"
                      autoComplete="new-password"
                    />

                    <SettingsTextInput
                      {...form.field('httpsAuthNtlmHost')}
                      label="NTLM Host"
                      helpText="Windows host for NTLM authentication (optional)"
                      placeholder="WORKSTATION01"
                    />

                    <SettingsTextInput
                      {...form.field('httpsAuthNtlmDomain')}
                      label="NTLM Domain"
                      helpText="Windows domain for NTLM authentication (optional)"
                      placeholder="COMPANY"
                    />
                  </>
                )}
              </SettingsFormSection>
            </>
          )}
        </SettingsFormSection>

        {/* Non-Proxy Hosts */}
        {isProxyEnabled && (
          <SettingsFormSection
            title={
              formData.nonProxyHosts.length > 0
                ? `Hosts to Exclude from Proxy (${formData.nonProxyHosts.length})`
                : 'Hosts to Exclude from Proxy'
            }
          >
            <Text size="2" className="http-page__section-description">
              Specify hosts that should bypass the proxy. Use wildcards (*) for pattern matching.
            </Text>

            <Flex gap="2" align="end" className="http-page__add-host">
              <Box className="http-page__add-host-input">
                <SettingsTextInput
                  name="newNonProxyHost"
                  label="Host Pattern"
                  value={newNonProxyHost}
                  onChange={(v: string) => { setNewNonProxyHost(v); setNonProxyHostError(null); }}
                  onKeyDown={handleNonProxyHostKeyDown}
                  placeholder="*.example.com"
                  helpText="Enter a hostname or pattern to exclude"
                  error={nonProxyHostError || undefined}
                />
              </Box>
              <SettingsButton
                type="button"
                variant="secondary"
                onClick={handleAddNonProxyHost}
                disabled={!newNonProxyHost.trim()}
                className="http-page__add-button"
                icon={Plus}
              >
                Add
              </SettingsButton>
            </Flex>

            {formData.nonProxyHosts.length > 0 && (
              <Box className="http-page__host-list">
                {[...formData.nonProxyHosts]
                  .sort((a: string, b: string) => a.localeCompare(b, undefined, { sensitivity: 'base' }))
                  .map((host: string) => (
                    <Flex key={host} align="center" justify="between" className="http-page__host-item">
                      <Text size="2">{host}</Text>
                      <button
                        type="button"
                        className="http-page__remove-button"
                        onClick={() => handleRemoveNonProxyHost(host)}
                        aria-label={`Remove ${host}`}
                      >
                        <Trash2 size={16} />
                      </button>
                    </Flex>
                ))}
              </Box>
            )}
          </SettingsFormSection>
        )}

          {/* Help Section */}
          <Box className="http-page__help">
            <Flex align="center" gap="2" className="http-page__help-header">
              <Info size={16} />
              <Text size="2" weight="medium">About HTTP Settings</Text>
            </Flex>
            <Text size="2" className="http-page__help-text">
              HTTP settings control how Nexus Repository makes outbound HTTP connections,
              including proxy configuration for accessing remote repositories.
            </Text>
            <Text size="2" className="http-page__help-text">
              {' '}See our{' '}
              <a
                href="http://links.sonatype.com/products/nxrm3/docs/http-request-and-proxy-settings"
                target="_blank"
                rel="noopener noreferrer"
                className="http-page__help-link"
              >
                documentation
                <ExternalLink size={12} />
              </a>
              {' '}for more information.
            </Text>
          </Box>
        </SettingsForm>
      </Box>
    </Box>
  );
}

export default HttpPage;
