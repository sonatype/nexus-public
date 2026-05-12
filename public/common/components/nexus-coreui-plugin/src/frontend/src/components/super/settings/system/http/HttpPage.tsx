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
import { Box, Flex, Text, Heading } from '@radix-ui/themes';
import { Globe, Loader2, Info, ExternalLink, Plus, Trash2 } from 'lucide-react';
import { ExtJS } from '@sonatype/nexus-ui-plugin';

import {
  SettingsForm,
  SettingsFormSection,
  SettingsCheckbox,
  SettingsTextInput,
  SettingsPasswordInput,
  SettingsButton,
  SettingsAlert,
} from '../../../shared/form';
import { useHttpForm } from './useHttpForm';
import {
  HttpConfiguration,
  HttpPageProps,
} from './types';

import './HttpPage.scss';

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

/**
 * HttpPage - HTTP Settings configuration page for Preview UI
 *
 * Configures HTTP proxy settings, timeouts, and retry behavior.
 * Uses XState form machine for state management.
 */
export function HttpPage({ className }: HttpPageProps) {
  // XState form hook handles load, save, dirty tracking, toast, validation
  const form = useHttpForm();

  // Local UI state for the "add non-proxy host" input
  const [newNonProxyHost, setNewNonProxyHost] = useState('');

  const canUpdate = ExtJS.checkPermission('nexus:settings:update');
  const formData = form.data as HttpConfiguration;
  const isProxyEnabled = formData.httpEnabled || formData.httpsEnabled;

  // Add non-proxy host
  const handleAddNonProxyHost = useCallback(() => {
    const trimmed = newNonProxyHost.trim();
    if (trimmed && !formData.nonProxyHosts.includes(trimmed)) {
      form.send({ type: 'UPDATE', name: 'nonProxyHosts', value: [...formData.nonProxyHosts, trimmed] } as any);
      setNewNonProxyHost('');
    }
  }, [newNonProxyHost, formData.nonProxyHosts, form]);

  // Remove non-proxy host
  const handleRemoveNonProxyHost = useCallback((index: number) => {
    form.send({
      type: 'UPDATE',
      name: 'nonProxyHosts',
      value: formData.nonProxyHosts.filter((_: string, i: number) => i !== index),
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
    form.send({ type: 'UPDATE', name: 'httpEnabled', value: !formData.httpEnabled } as any);
    if (formData.httpEnabled) {
      // Disabling - also disable auth
      form.send({ type: 'UPDATE', name: 'httpAuthEnabled', value: false } as any);
    }
  }, [form, formData.httpEnabled]);

  // Toggle HTTPS proxy
  const handleToggleHttpsProxy = useCallback(() => {
    form.send({ type: 'UPDATE', name: 'httpsEnabled', value: !formData.httpsEnabled } as any);
    if (formData.httpsEnabled) {
      // Disabling - also disable auth
      form.send({ type: 'UPDATE', name: 'httpsAuthEnabled', value: false } as any);
    }
  }, [form, formData.httpsEnabled]);

  // Discard changes and clear local UI state
  const handleDiscard = useCallback(() => {
    form.reset();
    setNewNonProxyHost('');
  }, [form]);

  // Loading state
  if (form.isLoading) {
    return (
      <Box className={`http-page ${className || ''}`.trim()}>
        <Flex align="center" justify="center" className="http-page__loading">
          <Loader2 size={24} className="http-page__spinner" />
          <Text size="2">Loading HTTP settings...</Text>
        </Flex>
      </Box>
    );
  }

  // Read-only view for users without update permission
  if (!canUpdate) {
    return (
      <Box className={`http-page ${className || ''}`.trim()}>
        <Flex align="center" gap="3" className="http-page__header">
          <Globe size={24} className="http-page__icon" />
          <Box>
            <Heading as="h1" size="6" weight="medium">HTTP Settings</Heading>
            <Text size="2" className="http-page__description">
              Configure HTTP proxy and connection settings
            </Text>
          </Box>
        </Flex>

        <SettingsFormSection title="Current Settings">
          <Box className="http-page__readonly">
            <Flex className="http-page__row">
              <Text size="2" weight="medium" className="http-page__label">HTTP Proxy</Text>
              <Text size="2">{formData.httpEnabled ? `${formData.httpHost}:${formData.httpPort}` : 'Disabled'}</Text>
            </Flex>
            <Flex className="http-page__row">
              <Text size="2" weight="medium" className="http-page__label">HTTPS Proxy</Text>
              <Text size="2">{formData.httpsEnabled ? `${formData.httpsHost}:${formData.httpsPort}` : 'Disabled'}</Text>
            </Flex>
          </Box>
        </SettingsFormSection>
      </Box>
    );
  }

  return (
    <Box className={`http-page ${className || ''}`.trim()}>
      {/* Header */}
      <Flex align="center" gap="3" className="http-page__header">
        <Globe size={24} className="http-page__icon" />
        <Box>
          <Heading as="h1" size="6" weight="medium">HTTP Settings</Heading>
          <Text size="2" className="http-page__description">
            Configure HTTP proxy and connection settings
          </Text>
        </Box>
      </Flex>

      {/* Content area */}
      <Box className="http-page__content">
        <SettingsForm
          testId="http-form"
          onSubmit={() => form.submit()}
          onCancel={handleDiscard}
          loading={form.isSaving}
          pristine={form.isPristine}
          error={form.saveError || undefined}
        >
          {form.saveError && (
            <Box mb="4">
              <SettingsAlert type="error">{form.saveError}</SettingsAlert>
            </Box>
          )}
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

              <SettingsFormSection title="HTTP Proxy Authentication" collapsible defaultCollapsed={!formData.httpAuthEnabled}>
                <SettingsCheckbox
                  {...form.checkbox('httpAuthEnabled')}
                  label="Enable Authentication"
                  description="Authenticate with the HTTP proxy server"
                />

                {formData.httpAuthEnabled && (
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

              <SettingsFormSection title="HTTPS Proxy Authentication" collapsible defaultCollapsed={!formData.httpsAuthEnabled}>
                <SettingsCheckbox
                  {...form.checkbox('httpsAuthEnabled')}
                  label="Enable Authentication"
                  description="Authenticate with the HTTPS proxy server"
                />

                {formData.httpsAuthEnabled && (
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
          <SettingsFormSection title="Hosts to Exclude from Proxy">
            <Text size="2" className="http-page__section-description">
              Specify hosts that should bypass the proxy. Use wildcards (*) for pattern matching.
            </Text>

            <Flex gap="2" align="end" className="http-page__add-host">
              <Box className="http-page__add-host-input">
                <SettingsTextInput
                  name="newNonProxyHost"
                  label="Host Pattern"
                  value={newNonProxyHost}
                  onChange={setNewNonProxyHost}
                  onKeyDown={handleNonProxyHostKeyDown}
                  placeholder="*.example.com"
                  helpText="Enter a hostname or pattern to exclude"
                />
              </Box>
              <SettingsButton
                type="button"
                variant="secondary"
                onClick={handleAddNonProxyHost}
                disabled={!newNonProxyHost.trim()}
                className="http-page__add-button"
              >
                <Plus size={16} />
                Add
              </SettingsButton>
            </Flex>

            {formData.nonProxyHosts.length > 0 && (
              <Box className="http-page__host-list">
                {formData.nonProxyHosts.map((host: string, index: number) => (
                  <Flex key={host} align="center" justify="between" className="http-page__host-item">
                    <Text size="2">{host}</Text>
                    <button
                      type="button"
                      className="http-page__remove-button"
                      onClick={() => handleRemoveNonProxyHost(index)}
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
              See our{' '}
              <a
                href="https://help.sonatype.com/en/http-configuration.html"
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
