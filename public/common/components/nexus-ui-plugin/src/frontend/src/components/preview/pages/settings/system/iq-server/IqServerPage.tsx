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

import React, { useState, useMemo, useEffect, useCallback } from 'react';
import { Box, Flex, Text, Heading, Card, IconButton, Tooltip, TextField, Separator, ScrollArea, Badge, Grid } from '@radix-ui/themes';
import { Shield, ShieldCheck, Loader2, Info, ExternalLink, CheckCircle, XCircle, List, Copy, Search, X, Minimize2, Check } from 'lucide-react';

import {
  SettingsForm,
  SettingsFormSection,
  SettingsCheckbox,
  SettingsTextInput,
  SettingsPasswordInput,
  SettingsSelect,
  SettingsAlert,
  SettingsButton,
} from '../../../../shared/form';
import { PageHeader } from '../../../../shared';
import { useIqServerForm } from './useIqServerForm';
import { ConnectionIndicator } from './ConnectionIndicator';
import { parseApplicationReason } from './iqServerUtils';
import { IqServerPageProps } from './types';
import { CertificateViewDialog } from '../../repository/repositories/facets/CertificateViewDialog';
import { PropertyListEditor } from './PropertyListEditor';

import './IqServerPage.scss';

/**
 * Full-screen application list — uses the exact same fixed-position container pattern
 * as RepositoryStructureTree (position:fixed 100vw/100vh, CSS class toggle, no Dialog overlay).
 * Toolbar mirrors the tree: search left, icon buttons right. Escape key closes.
 */
function IqApplicationListModal({
  items,
  message,
  isOpen,
  onClose,
}: {
  items: string[];
  message?: string;
  isOpen: boolean;
  onClose: () => void;
}) {
  const [filterText, setFilterText] = useState('');
  const [copied, setCopied] = useState(false);

  const filteredItems = useMemo(() => {
    if (!filterText.trim()) return items;
    const term = filterText.toLowerCase();
    return items.filter((name) => name.toLowerCase().includes(term));
  }, [items, filterText]);

  // Collapse sidebar on open — same as tree fullscreen
  useEffect(() => {
    if (isOpen) {
      window.dispatchEvent(new CustomEvent('nx-sidebar-toggle', { detail: { open: false } }));
      return () => {
        window.dispatchEvent(new CustomEvent('nx-sidebar-toggle', { detail: { open: true } }));
      };
    }
  }, [isOpen]);

  // Escape key closes
  useEffect(() => {
    if (!isOpen) return;
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [isOpen, onClose]);

  const handleCopy = useCallback(() => {
    navigator.clipboard?.writeText(filteredItems.join('\n')).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    });
  }, [filteredItems]);

  if (!isOpen) return null;

  return (
    <Box
      className="iq-app-list-fullscreen"
      role="dialog"
      aria-modal="true"
      aria-label="IQ Server Applications"
      data-testid="iq-application-list-modal"
    >
      {/* Sticky header bar — matches tree fullscreen header */}
      <Flex align="center" justify="between" className="iq-app-list-fullscreen__header">
        <Flex align="center" gap="3">
          <Shield size={20} className="iq-server-page__icon" aria-hidden="true" />
          <Box>
            <Flex align="center" gap="2">
              <Heading size="4" weight="bold">IQ Server Applications</Heading>
              {items.length > 0 && (
                <Badge color="blue" variant="soft" size="2" data-testid="app-count-badge">
                  {filteredItems.length === items.length
                    ? `${items.length} ${items.length === 1 ? 'application' : 'applications'}`
                    : `${filteredItems.length} of ${items.length}`}
                </Badge>
              )}
            </Flex>
            <Text size="2" color="gray">Applications registered with this IQ Server instance</Text>
          </Box>
        </Flex>

        {/* Toolbar buttons — same pattern as RepositoryStructureTree */}
        <Flex align="center" gap="2">
          {items.length > 0 && (
            <Tooltip content={copied ? 'Copied!' : 'Copy list to clipboard'}>
              <IconButton
                variant="ghost"
                size="2"
                onClick={handleCopy}
                aria-label="Copy application list"
                data-testid="copy-app-list"
              >
                {copied
                  ? <Check size={16} color="var(--green-9)" />
                  : <Copy size={16} />
                }
              </IconButton>
            </Tooltip>
          )}
          <Separator orientation="vertical" size="1" />
          <Tooltip content="Close (Esc)">
            <IconButton
              variant="ghost"
              size="2"
              onClick={onClose}
              aria-label="Close application list"
              data-testid="close-app-list"
            >
              <Minimize2 size={16} />
            </IconButton>
          </Tooltip>
        </Flex>
      </Flex>

      {/* Search toolbar — same sticky pattern as tree toolbar */}
      <Box className="iq-app-list-fullscreen__toolbar">
        <Flex align="center" gap="2">
          <TextField.Root
            placeholder="Search applications..."
            value={filterText}
            onChange={(e) => setFilterText(e.target.value)}
            size="2"
            style={{ flex: 1, maxWidth: 480 }}
            aria-label="Search applications"
            data-testid="app-list-search"
          >
            <TextField.Slot>
              <Search size={16} aria-hidden="true" />
            </TextField.Slot>
          </TextField.Root>
          {filterText && (
            <IconButton
              variant="ghost"
              size="1"
              onClick={() => setFilterText('')}
              aria-label="Clear search"
            >
              <X size={14} />
            </IconButton>
          )}
        </Flex>
      </Box>

      {/* Scrollable content — flex:1 min-height:0, same as tree scroll area */}
      <ScrollArea scrollbars="vertical" className="iq-app-list-fullscreen__scroll">
        <Box className="iq-app-list-fullscreen__list">
          {message ? (
            <Flex align="center" gap="2" p="5">
              <Info size={16} color="var(--gray-9)" aria-hidden="true" />
              <Text size="2" color="gray">{message}</Text>
            </Flex>
          ) : filterText && filteredItems.length === 0 ? (
            <Flex direction="column" align="center" gap="3" p="8">
              <Search size={32} color="var(--gray-7)" aria-hidden="true" />
              <Text size="2" color="gray">No applications match &quot;{filterText}&quot;</Text>
            </Flex>
          ) : filteredItems.length > 0 ? (
            filteredItems.map((name) => (
              <Flex
                key={name}
                className="iq-app-list-fullscreen__row"
                align="center"
                gap="3"
                data-testid="app-list-row"
              >
                <Shield size={14} color="var(--accent-9)" aria-hidden="true" className="iq-app-list-fullscreen__row-icon" />
                <Text size="2" style={{ fontFamily: 'var(--code-font-family)', wordBreak: 'break-all' }}>
                  {name}
                </Text>
              </Flex>
            ))
          ) : (
            <Flex direction="column" align="center" gap="3" p="8">
              <Shield size={32} color="var(--gray-7)" aria-hidden="true" />
              <Text size="2" color="gray">No applications to display.</Text>
            </Flex>
          )}
        </Box>
      </ScrollArea>
    </Box>
  );
}

/**
 * IqServerPage - IQ Server configuration page for Preview UI
 *
 * Configures Sonatype IQ Server integration for Repository Firewall and Lifecycle.
 * Render-only component — all business logic is in useIqServerForm hook.
 */
export function IqServerPage({ className }: IqServerPageProps) {
  const {
    data: settings,
    field,
    checkbox,
    select,
    hasValidationErrors,
    isLoading,
    isSaving,
    isPristine,
    saveError,
    handleFieldChange,
    handleUrlChange,
    verify,
    connectionStatus,
    connectionMessage,
    verificationResult,
    capabilities,
    isCloud,
    canUpdate,
    canOpenDashboard,
    dashboardUrl,
    submit,
    reset,
    clearSaveError,
    setProperties,
    propertyValidations,
    hasPropertyErrors,
    showAllValidation,
    showClearAllConfirm,
    requestClearAllProperties,
    confirmClearAllProperties,
    cancelClearAllProperties,
    propertiesDroppedLineCount,
    showPropertiesDroppedWarning,
    dismissPropertiesDroppedWarning,
  } = useIqServerForm();

  // Application-list modal is pure UI state — stays local.
  const [showApplicationListModal, setShowApplicationListModal] = useState(false);
  const [showCertDialog, setShowCertDialog] = useState(false);

  // Handle auth type change — clear username/password when switching
  // Must be defined before early returns to follow React hooks rules
  const handleAuthTypeChange = useCallback((value: string) => {
    handleFieldChange('authenticationType', value);
    handleFieldChange('username', '');
    handleFieldChange('password', '');
  }, [handleFieldChange]);

  const parsedApplicationReason = useMemo(
    () => (verificationResult?.reason ? parseApplicationReason(verificationResult.reason) : null),
    [verificationResult?.reason],
  );
  const applicationListItems = parsedApplicationReason?.isList ? parsedApplicationReason.items : [];
  const applicationListMessage = parsedApplicationReason && (!parsedApplicationReason.isList || parsedApplicationReason.items.length === 0)
    ? (parsedApplicationReason.items[0] || verificationResult?.reason)
    : undefined;

  // Loading state
  if (isLoading) {
    return (
      <Box className={`iq-server-page ${className || ''}`.trim()}>
        <Flex align="center" justify="center" className="iq-server-page__loading">
          <Loader2 size={24} className="iq-server-page__spinner" />
          <Text size="2">Loading IQ Server settings...</Text>
        </Flex>
      </Box>
    );
  }

  // Read-only view for users without update permission
  if (!canUpdate) {
    return (
      <Box className={`iq-server-page ${className || ''}`.trim()}>
        <PageHeader
          title="IQ Server"
          breadcrumbs={[
            { label: 'Settings', onClick: () => { window.location.hash = '#preview/admin/settings'; } },
            { label: 'IQ Server' },
          ]}
        />

        <SettingsFormSection title="Current Settings">
          <Box className="iq-server-page__readonly">
            <Flex className="iq-server-page__row">
              <Text size="2" weight="medium" className="iq-server-page__label">Enabled</Text>
              <Text size="2">{settings.enabled ? 'Yes' : 'No'}</Text>
            </Flex>
            <Flex className="iq-server-page__row">
              <Text size="2" weight="medium" className="iq-server-page__label">IQ Server URL</Text>
              <Text size="2">{settings.url || 'Not configured'}</Text>
            </Flex>
            <Flex className="iq-server-page__row">
              <Text size="2" weight="medium" className="iq-server-page__label">Authentication</Text>
              <Text size="2">{settings.authenticationType || 'Not configured'}</Text>
            </Flex>
          </Box>
        </SettingsFormSection>
      </Box>
    );
  }

  const verifying = connectionStatus === 'testing';

  return (
    <Box className={`iq-server-page ${className || ''}`.trim()}>
      {/* Header */}
      <PageHeader
        title="IQ Server"
        breadcrumbs={[
          { label: 'Settings', onClick: () => { window.location.hash = '#preview/admin/settings'; } },
          { label: 'IQ Server' },
        ]}
      />


      {/* Connection Indicator - auto-tests on load */}
      <ConnectionIndicator
        status={connectionStatus}
        message={connectionMessage}
        className="iq-server-page__connection-indicator"
      />

      {/* Dashboard Link */}
      {canOpenDashboard && (
        <Box className="iq-server-page__dashboard-link">
          <a
            href={dashboardUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="iq-server-page__link"
          >
            <ExternalLink size={16} />
            Open IQ Server Dashboard
          </a>
        </Box>
      )}

      {/* Alerts */}
      {saveError && (
        <Box className="iq-server-page__alerts">
          <SettingsAlert type="error" onClose={clearSaveError}>
            {saveError}
          </SettingsAlert>
        </Box>
      )}

      {showPropertiesDroppedWarning && (
        <Box className="iq-server-page__alerts">
          <SettingsAlert type="warning" onClose={dismissPropertiesDroppedWarning}>
            {propertiesDroppedLineCount} {propertiesDroppedLineCount === 1 ? 'line' : 'lines'} in the existing
            Properties configuration weren't recognized (comments or non "name=value" syntax) and will be
            removed if you save.
          </SettingsAlert>
        </Box>
      )}

      {/* Form */}
      <Box>
        <SettingsForm
          title=""
          onSubmit={submit}
          onCancel={reset}
          loading={isSaving}
          pristine={isPristine}
          submitDisabled={hasValidationErrors || hasPropertyErrors}
        >
        <SettingsFormSection title="IQ Server Configuration">
          {/* License & Features - Read Only Indicators */}
          {connectionStatus === 'connected' && (
            <Box className="iq-server-page__capabilities" mb="4">
              <Card size="2">
                <Flex direction="column" gap="3">
                  <Flex align="center" gap="2">
                    <Info size={16} color="var(--gray-9)" />
                    <Text size="2" weight="bold">Licensed IQ Features</Text>
                  </Flex>
                  <Separator size="4" />
                  <Grid columns={{ initial: '1', sm: '2' }} gap="4">
                    {/* Lifecycle Indicator */}
                    <Flex direction="column" gap="2">
                      <Flex align="center" justify="between" wrap="wrap" gap="2">
                        <Text size="2" weight="medium">Sonatype Lifecycle</Text>
                        {capabilities.hasLifecycle ? (
                          <Badge color="green" variant="soft">
                            <Flex align="center" gap="1">
                              <CheckCircle size={12} />
                              Active
                            </Flex>
                          </Badge>
                        ) : (
                          <Badge color="gray" variant="soft">
                            <Flex align="center" gap="1">
                              <XCircle size={12} />
                              Not Available
                            </Flex>
                          </Badge>
                        )}
                      </Flex>
                      {!capabilities.hasLifecycle && (
                        <Text size="1" color="gray">
                          <a
                            href="https://links.sonatype.com/products/nxrm3/browse/lc-learn"
                            target="_blank"
                            rel="noopener noreferrer"
                            className="iq-server-page__link"
                          >
                            Learn More <ExternalLink size={10} />
                          </a>
                        </Text>
                      )}
                    </Flex>

                    {/* Firewall Indicator */}
                    <Flex direction="column" gap="2">
                      <Flex align="center" justify="between" wrap="wrap" gap="2">
                        <Text size="2" weight="medium">Repository Firewall</Text>
                        {capabilities.hasFirewall ? (
                          <Badge color="green" variant="soft">
                            <Flex align="center" gap="1">
                              <CheckCircle size={12} />
                              Active
                            </Flex>
                          </Badge>
                        ) : (
                          <Badge color="gray" variant="soft">
                            <Flex align="center" gap="1">
                              <XCircle size={12} />
                              Not Available
                            </Flex>
                          </Badge>
                        )}
                      </Flex>
                      {!capabilities.hasFirewall && (
                        <Text size="1" color="gray">
                          <a
                            href="https://links.sonatype.com/nexus-repository-firewall"
                            target="_blank"
                            rel="noopener noreferrer"
                            className="iq-server-page__link"
                          >
                            Learn More <ExternalLink size={10} />
                          </a>
                        </Text>
                      )}
                    </Flex>
                  </Grid>
                </Flex>
              </Card>
            </Box>
          )}

          <Box className="iq-server-page__form-column">
          <Text size="2" className="iq-server-page__intro">
            <a
              href="https://links.sonatype.com/products/nxrm3/browse/lc-learn"
              target="_blank"
              rel="noopener noreferrer"
              className="iq-server-page__link"
            >
              IQ Server
            </a>
            {' '}can evaluate application and organizing policies.
          </Text>

          <Text size="2" className="iq-server-page__help-text">
            To enable this feature configure the IQ Server URL, username and password.
          </Text>

          <SettingsCheckbox
            {...checkbox('enabled')}
            label="Enable IQ Server"
            description="Enable the use of IQ Server"
          />

          <SettingsTextInput
            {...field('url')}
            label="IQ Server URL"
            onChange={handleUrlChange}
            helpText={
              isCloud
                ? 'The IQ Server URL is set for your cloud environment and cannot be changed here.'
                : 'This is the address of your IQ server'
            }
            placeholder="https://iq.example.com"
            required
            readOnly={isCloud}
          />

          {settings.url?.startsWith('https://') && (
            <Box>
              <SettingsCheckbox
                {...checkbox('useTrustStoreForUrl')}
                label="Use Nexus Repository Trust Store"
                description="Use certificates from the Nexus Repository trust store"
              />
              <Box mt="2" ml="6">
                <SettingsButton
                  type="button"
                  variant="secondary"
                  size="small"
                  onClick={() => setShowCertDialog(true)}
                  icon={ShieldCheck}
                >
                  View Certificate
                </SettingsButton>
              </Box>
            </Box>
          )}

          {showCertDialog && (
            <CertificateViewDialog
              remoteUrl={settings.url}
              onClose={() => setShowCertDialog(false)}
            />
          )}

          <SettingsSelect
            {...select('authenticationType')}
            label="Authentication Method"
            onChange={handleAuthTypeChange}
            required
            placeholder="Select authentication method..."
            options={[
              { value: 'USER', label: 'User Authentication' },
              { value: 'PKI', label: 'PKI Authentication' },
            ]}
          />

          {settings.authenticationType === 'USER' && (
            <>
              <SettingsTextInput
                {...field('username')}
                label="Username"
                helpText="User with access to the IQ Server"
                required
              />

              <SettingsPasswordInput
                {...field('password')}
                label="Password"
                helpText="Credentials for the IQ Server user"
                autoComplete="new-password"
                required
              />
            </>
          )}
          </Box>
        </SettingsFormSection>

        <SettingsFormSection title="Advanced Settings" collapsible defaultCollapsed>
          <SettingsTextInput
            {...field('timeoutSeconds')}
            label="Connection Timeout (seconds)"
            type="number"
            value={settings.timeoutSeconds !== null ? String(settings.timeoutSeconds) : ''}
            onChange={(value) => handleFieldChange('timeoutSeconds', value ? parseInt(value, 10) : null)}
            helpText="Seconds to wait for activity before stopping and retrying. Leave blank for globally defined HTTP timeout."
            placeholder="Globally Defined"
            min={1}
            max={3600}
          />

          <Box className="iq-server-page__properties">
            <Text size="2" weight="medium" className="iq-server-page__properties-label">Properties</Text>
            <Text size="2" color="gray" className="iq-server-page__properties-help">
              Additional properties to configure for IQ Server
            </Text>

            {showClearAllConfirm && (
              <SettingsAlert type="warning">
                <Flex justify="between" align="center" gap="3">
                  <Text size="2">Clear all properties? This will remove all configured properties.</Text>
                  <Flex gap="2">
                    <SettingsButton variant="secondary" onClick={cancelClearAllProperties}>
                      Cancel
                    </SettingsButton>
                    <SettingsButton variant="danger" onClick={confirmClearAllProperties}>
                      Clear All
                    </SettingsButton>
                  </Flex>
                </Flex>
              </SettingsAlert>
            )}

            <PropertyListEditor
              properties={settings.properties}
              onChange={setProperties}
              onClearAll={requestClearAllProperties}
              disabled={!canUpdate || isSaving}
              validations={propertyValidations}
              showAllValidation={showAllValidation}
            />
          </Box>

          <SettingsCheckbox
            {...checkbox('showLink')}
            label="Show IQ Server Link"
            description="Show IQ Server link in the Browse menu when the server is enabled"
          />
        </SettingsFormSection>

        {/* Test Connection - greyed out until valid data */}
        <Box className="iq-server-page__sidecar">
          <Card className={`iq-server-page__verify-card ${hasValidationErrors ? 'iq-server-page__verify-card--disabled' : ''}`}>
            <Flex direction="column" gap="3">
              <Text size="2" weight="medium">Test Connection</Text>
              {hasValidationErrors ? (
                <Text size="2" color="gray">
                  Enter IQ Server URL and credentials to test the connection.
                </Text>
              ) : (
                <>
                  <SettingsButton
                    type="button"
                    variant="secondary"
                    onClick={verify}
                    disabled={verifying}
                    loading={verifying}
                  >
                    Test Connection
                  </SettingsButton>

                  {verificationResult && (
                    <Box className="iq-server-page__verify-result">
                      {verificationResult.success ? (
                        <>
                          <Flex align="center" gap="2" className="iq-server-page__verify-success">
                            <CheckCircle size={16} />
                            <Text size="2">Connection successful.</Text>
                          </Flex>
                          {verificationResult.reason && (
                            <SettingsButton
                              type="button"
                              variant="secondary"
                              size="1"
                              onClick={() => setShowApplicationListModal(true)}
                              className="iq-server-page__view-app-list-trigger"
                              icon={List}
                            >
                              View application list
                            </SettingsButton>
                          )}
                        </>
                      ) : (
                        <>
                          <Flex align="center" gap="2" className="iq-server-page__verify-error">
                            <XCircle size={16} />
                            <Text size="2">Connection failed.</Text>
                          </Flex>
                          {verificationResult.reason && (
                            <Text size="2" as="p" className="iq-server-page__error-reason">
                              {verificationResult.reason}
                            </Text>
                          )}
                        </>
                      )}
                    </Box>
                  )}
                </>
              )}
            </Flex>
          </Card>
        </Box>

        {/* Full-screen Application List - same control pattern as Repository Structure Tree */}
        <IqApplicationListModal
          isOpen={showApplicationListModal && !!verificationResult?.reason}
          items={applicationListItems}
          message={applicationListMessage}
          onClose={() => setShowApplicationListModal(false)}
        />

        {/* Help Section */}
        <Box className="iq-server-page__help">
          <Flex align="center" gap="2" className="iq-server-page__help-header">
            <Info size={16} />
            <Text size="2" weight="medium">About IQ Server</Text>
          </Flex>
          <Text size="2" className="iq-server-page__help-text">
            IQ Server provides component intelligence, policy management, and security vulnerability
            analysis for your software supply chain.
          </Text>
          <Text size="2" className="iq-server-page__help-text">
            See our{' '}
            <a
              href="http://links.sonatype.com/products/nxrm3/browse/lc-learn"
              target="_blank"
              rel="noopener noreferrer"
              className="iq-server-page__help-link"
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

export default IqServerPage;
