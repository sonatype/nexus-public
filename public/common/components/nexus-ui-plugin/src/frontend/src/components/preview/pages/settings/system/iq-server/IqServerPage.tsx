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
import { Box, Flex, Text, Heading, Card, IconButton, Tooltip, TextField, Separator, ScrollArea, Badge, AlertDialog, Button, Grid } from '@radix-ui/themes';
import { useRouter } from '@uirouter/react';
import { Shield, Loader2, Info, ExternalLink, CheckCircle, XCircle, ArrowLeft, List, Copy, Search, X, Minimize2, Check, Plus } from 'lucide-react';

import { ExtJS } from '../../../../../../interface/ExtJS';

import {
  SettingsForm,
  SettingsFormSection,
  SettingsCheckbox,
  SettingsTextInput,
  SettingsPasswordInput,
  SettingsSelect,
  SettingsTextArea,
  SettingsAlert,
  SettingsButton,
} from '../../../../shared/form';
import { useIqServerApi } from './useIqServerApi';
import { ConnectionIndicator, ConnectionStatus } from './ConnectionIndicator';
import { useToast } from '../../../../shared/Toast';
import {
  IqServerConfiguration,
  IqServerPageProps,
  DEFAULT_IQ_CONFIGURATION,
  IqValidationErrors,
  IqVerificationResult,
  PASSWORD_PLACEHOLDER,
  IqCapabilities,
  DEFAULT_IQ_CAPABILITIES,
} from './types';

import './IqServerPage.scss';

/**
 * Common IQ properties examples for the property selector tool
 */
const IQ_PROPERTY_EXAMPLES = [
  {
    label: 'Fail Open Mode',
    value: 'nexus.iq.failOpenModeEnabled=true',
    description: 'Allow component downloads if IQ Server is unreachable.',
  },
  {
    label: 'Malware Remediation',
    value: 'com.sonatype.insight.client.remediation.MalwareRemediationService.enabled=true',
    description: 'Enable automated malware remediation.',
  },
  {
    label: 'Malware Asset Limit',
    value: 'com.sonatype.insight.client.remediation.MalwareRemediationService.assetLimit=1000',
    description: 'Limit the number of assets remediated per task run.',
  },
];

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
 * Parses the verification reason into application names or a status message.
 * Backend returns either comma-separated app names or messages like "No applications configured yet."
 */
function parseApplicationReason(reason: string): { isList: boolean; items: string[] } {
  const trimmed = reason.trim();
  const appsPrefix = 'Applications: ';
  if (trimmed.includes(appsPrefix)) {
    const after = trimmed.split(appsPrefix)[1]?.trim() ?? '';
    const items = after.split(',').map((s) => s.trim()).filter(Boolean);
    return { isList: items.length > 0, items };
  }
  if (
    trimmed.toLowerCase().startsWith('no applications') ||
    (trimmed.startsWith('Connection successful') && !trimmed.includes(','))
  ) {
    return { isList: false, items: [trimmed] };
  }
  const items = trimmed.split(',').map((s) => s.trim()).filter(Boolean);
  return { isList: items.length > 0, items };
}

/**
 * Validates if a string is a valid URL
 */
function isValidUrl(url: string): boolean {
  try {
    new URL(url);
    return true;
  } catch {
    return false;
  }
}

/**
 * Validates IQ Server configuration and returns validation errors
 */
function validateIqConfig(config: IqServerConfiguration, pristineConfig: IqServerConfiguration): IqValidationErrors {
  const errors: IqValidationErrors = {};

  if (!config.enabled) {
    return errors;
  }

  if (!config.url?.trim()) {
    errors.url = 'IQ Server URL is required';
  } else if (!isValidUrl(config.url)) {
    errors.url = 'Please enter a valid URL';
  }

  if (!config.authenticationType) {
    errors.authenticationType = 'Authentication method is required';
  }

  if (config.authenticationType === 'USER') {
    if (!config.username?.trim()) {
      errors.username = 'Username is required';
    }

    // Password is required for new configs or when URL changes
    const urlChanged = pristineConfig.url && pristineConfig.url !== config.url;
    const isPlaceholder = config.password === PASSWORD_PLACEHOLDER;
    
    if (!config.password?.trim() && !isPlaceholder) {
      errors.password = 'Password is required';
    } else if (urlChanged && isPlaceholder) {
      errors.password = 'Password is required when changing the URL';
    }
  }

  if (config.timeoutSeconds !== null && (config.timeoutSeconds < 1 || config.timeoutSeconds > 3600)) {
    errors.timeoutSeconds = 'Timeout must be between 1 and 3600 seconds';
  }

  return errors;
}

/**
 * IqServerPage - IQ Server configuration page for Preview UI
 *
 * Configures Sonatype IQ Server integration for Repository Firewall and Lifecycle.
 */
export function IqServerPage({ className }: IqServerPageProps) {
  const router = useRouter();
  const { loading, verifying, error, setError, fetchSettings, fetchCapabilities, fetchCapabilitiesWithConfig, saveSettings, verifyConnection } = useIqServerApi();

  const [settings, setSettings] = useState<IqServerConfiguration>(DEFAULT_IQ_CONFIGURATION);
  const [pristineSettings, setPristineSettings] = useState<IqServerConfiguration>(DEFAULT_IQ_CONFIGURATION);
  const [capabilities, setCapabilities] = useState<IqCapabilities>(DEFAULT_IQ_CAPABILITIES);
  const [loadingInitial, setLoadingInitial] = useState(true);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [touched, setTouched] = useState<Record<string, boolean>>({});
  const [verificationResult, setVerificationResult] = useState<IqVerificationResult | null>(null);
  const [showApplicationListModal, setShowApplicationListModal] = useState(false);
  const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus>('idle');
  const [connectionMessage, setConnectionMessage] = useState<string | undefined>(undefined);
  const [hasAutoTested, setHasAutoTested] = useState(false);
  const [showSaveConfirmModal, setShowSaveConfirmModal] = useState(false);

  const toast = useToast();
  const canUpdate = ExtJS.checkPermission('nexus:settings:update');
  const isCloud = ExtJS.state?.()?.getValue?.('isCloud', false) ?? false;

  // Load settings on mount
  useEffect(() => {
    const loadData = async () => {
      setLoadingInitial(true);
      try {
        const [iqSettings, iqCapabilities] = await Promise.all([
          fetchSettings(),
          fetchCapabilities()
        ]);
        setSettings(iqSettings);
        setPristineSettings(iqSettings);
        setCapabilities(iqCapabilities);
      } catch (err) {
        // Error is set by the hook
      } finally {
        setLoadingInitial(false);
      }
    };

    loadData();
  }, [fetchSettings, fetchCapabilities]);

  // Clear success message after delay
  useEffect(() => {
    if (successMessage) {
      const timer = setTimeout(() => setSuccessMessage(null), 5000);
      return () => clearTimeout(timer);
    }
  }, [successMessage]);

  // Auto-test connection on page load when settings exist
  useEffect(() => {
    const autoTestConnection = async () => {
      // Only auto-test if:
      // 1. Not already tested
      // 2. Not loading
      // 3. Settings have a URL configured (indicating IQ Server is set up)
      // 4. IQ Server is enabled
      if (hasAutoTested || loadingInitial || !pristineSettings.url?.trim() || !pristineSettings.enabled) {
        return;
      }

      setHasAutoTested(true);
      setConnectionStatus('testing');
      setConnectionMessage(undefined);

      try {
        const result = await verifyConnection(pristineSettings);
        if (result.success) {
          // Extract version from reason if available (format: "Connected to IQ Server vX.Y.Z" or similar)
          const versionMatch = result.reason?.match(/v?(\d+\.\d+(?:\.\d+)?)/);
          const versionText = versionMatch ? ` (v${versionMatch[1]})` : '';
          setConnectionStatus('connected');
          setConnectionMessage(`Connected to IQ Server${versionText}`);
        } else {
          setConnectionStatus('failed');
          // Format the error message
          const errorMessage = result.reason || 'Unknown error';
          setConnectionMessage(`Connection failed: ${errorMessage}`);
        }
      } catch (err: any) {
        setConnectionStatus('failed');
        setConnectionMessage(`Connection failed: ${err?.message || 'Unknown error'}`);
      }
    };

    autoTestConnection();
  }, [loadingInitial, pristineSettings, hasAutoTested, verifyConnection]);

  // Check if pristine
  const isPristine = useMemo(() => {
    // Robust comparison that ignores minor type differences (null/undefined/empty string)
    const normalize = (config: IqServerConfiguration) => ({
      ...config,
      url: config.url || '',
      username: config.username || '',
      password: config.password || '',
      properties: config.properties || '',
      timeoutSeconds: config.timeoutSeconds === null ? null : Number(config.timeoutSeconds),
    });
    return JSON.stringify(normalize(settings)) === JSON.stringify(normalize(pristineSettings));
  }, [settings, pristineSettings]);

  // Can open dashboard?
  const canOpenDashboard = pristineSettings.enabled && isValidUrl(pristineSettings.url);

  // Validation errors
  const validationErrors = useMemo(() => {
    const allErrors = validateIqConfig(settings, pristineSettings);
    const visibleErrors: IqValidationErrors = {};

    Object.keys(allErrors).forEach((key) => {
      if (touched[key]) {
        visibleErrors[key as keyof IqValidationErrors] = allErrors[key as keyof IqValidationErrors];
      }
    });

    return visibleErrors;
  }, [settings, pristineSettings, touched]);

  // Check if form has validation errors
  const hasValidationErrors = useMemo(() => {
    const allErrors = validateIqConfig(settings, pristineSettings);
    return Object.keys(allErrors).length > 0;
  }, [settings, pristineSettings]);

  // Handle field change
  const handleChange = useCallback(<K extends keyof IqServerConfiguration>(
    field: K,
    value: IqServerConfiguration[K]
  ) => {
    setSettings((prev) => ({ ...prev, [field]: value }));
    setTouched((prev) => ({ ...prev, [field]: true }));
    setVerificationResult(null);
    
    // Reset connection status if disabled or if significant settings change
    if (field === 'enabled' && !value) {
      setConnectionStatus('idle');
      setConnectionMessage(undefined);
    } else if (connectionStatus !== 'idle' && connectionStatus !== 'testing') {
      // Clear connection status when form values change (stale result)
      setConnectionStatus('idle');
      setConnectionMessage(undefined);
    }
  }, [connectionStatus]);

  // Handle URL change - clear password if it's a placeholder
  const handleUrlChange = useCallback((value: string) => {
    setSettings((prev) => {
      const newSettings = { ...prev, url: value };

      // If URL changes and password is placeholder, clear it
      if (prev.password === PASSWORD_PLACEHOLDER && pristineSettings.url !== value) {
        newSettings.password = '';
      }

      // Also handle trust store - only valid for HTTPS
      if (!value?.startsWith('https://')) {
        newSettings.useTrustStoreForUrl = false;
      }

      return newSettings;
    });
    setTouched((prev) => ({ ...prev, url: true, password: true }));
    setVerificationResult(null);
    // Clear connection status when URL changes (stale result)
    if (connectionStatus !== 'idle' && connectionStatus !== 'testing') {
      setConnectionStatus('idle');
      setConnectionMessage(undefined);
    }
  }, [pristineSettings.url, connectionStatus]);

  // Handle authentication type change
  const handleAuthTypeChange = useCallback((value: string) => {
    setSettings((prev) => ({
      ...prev,
      authenticationType: value as IqServerConfiguration['authenticationType'],
      username: '',
      password: '',
    }));
    setTouched((prev) => ({ ...prev, authenticationType: true }));
    setVerificationResult(null);
    // Clear connection status when auth type changes (stale result)
    if (connectionStatus !== 'idle' && connectionStatus !== 'testing') {
      setConnectionStatus('idle');
      setConnectionMessage(undefined);
    }
  }, [connectionStatus]);

  // Handle blur
  const handleBlur = useCallback((field: keyof IqServerConfiguration) => {
    setTouched((prev) => ({ ...prev, [field]: true }));
  }, []);

  // Verify connection - uses current form values (not saved values)
  const handleVerifyConnection = useCallback(async () => {
    setVerificationResult(null);
    setShowApplicationListModal(false);
    setConnectionStatus('testing');
    setConnectionMessage(undefined);

    const result = await verifyConnection(settings);
    setVerificationResult(result);

    if (result.success) {
      const versionMatch = result.reason?.match(/v?(\d+\.\d+(?:\.\d+)?)/);
      const versionText = versionMatch ? ` (v${versionMatch[1]})` : '';
      setConnectionStatus('connected');
      setConnectionMessage(`Connected to IQ Server${versionText}`);

      // Re-fetch capabilities on successful test using the same unsaved settings
      const iqCapabilities = await fetchCapabilitiesWithConfig(settings);
      setCapabilities(iqCapabilities);

      // Show save confirmation modal if form has unsaved changes
      if (!isPristine) {
        setShowSaveConfirmModal(true);
      }
    } else {
      setConnectionStatus('failed');
      const errorMessage = result.reason || 'Unknown error';
      setConnectionMessage(`Connection failed: ${errorMessage}`);
    }
  }, [settings, verifyConnection, isPristine]);

  // Save handler - returns true on success, false on failure
  const handleSubmit = useCallback(async (): Promise<boolean> => {
    // Mark all required fields as touched
    setTouched({
      url: true,
      authenticationType: true,
      username: true,
      password: true,
    });

    if (hasValidationErrors) {
      return false;
    }

    try {
      const savedSettings = await saveSettings(settings);
      setSettings(savedSettings);
      setPristineSettings(savedSettings);
      
      // Re-fetch capabilities after save
      const iqCapabilities = await fetchCapabilities();
      setCapabilities(iqCapabilities);

      // Use toast notification instead of inline success message
      toast.success('IQ Server configuration saved successfully');
      setSuccessMessage(null);
      setTouched({});
      setVerificationResult(null);
      // Show saved connection status without auto-retesting (avoids multi-minute
      // blocking when the IQ server is slow to respond on cloud)
      if (savedSettings.enabled && savedSettings.url?.trim()) {
        setConnectionStatus('idle');
        setConnectionMessage('Saved. Click "Test Connection" to verify.');
      } else {
        setConnectionStatus('idle');
        setConnectionMessage(undefined);
      }
      // Clear window.dirty so navigation guards don't think the form is still dirty
      if (typeof window !== 'undefined' && window.dirty) {
        window.dirty.length = 0;
      }
      return true;
    } catch (err) {
      throw err;
    }
  }, [settings, hasValidationErrors, saveSettings, toast]);

  // Discard changes
  const handleDiscard = useCallback(() => {
    setSettings(pristineSettings);
    setTouched({});
    setError(null);
    setVerificationResult(null);
    setShowApplicationListModal(false);
    // Reset connection indicator to show last known state from pristine settings
    // The auto-test effect will re-run if needed
  }, [pristineSettings, setError]);

  // Back: prefer browser history so user returns to where they came from; otherwise System > Tasks
  const handleBack = useCallback(() => {
    if (window.history.length > 2) {
      window.history.back();
    } else {
      router.stateService.go('preview.admin.system.tasks.list');
    }
  }, [router]);

  // Insert property helper
  const insertProperty = useCallback((propValue: string) => {
    setSettings((prev) => {
      const currentProps = prev.properties || '';
      const lines = currentProps.split('\n').map(l => l.trim()).filter(Boolean);
      
      // Check if property already exists
      const propKey = propValue.split('=')[0];
      const existingIndex = lines.findIndex(l => l.startsWith(`${propKey}=`));
      
      let newProps = '';
      if (existingIndex >= 0) {
        lines[existingIndex] = propValue;
        newProps = lines.join('\n');
      } else {
        newProps = currentProps ? `${currentProps}\n${propValue}` : propValue;
      }
      
      return { ...prev, properties: newProps };
    });
    setTouched((prev) => ({ ...prev, properties: true }));
  }, []);

  // Loading state
  if (loadingInitial) {
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
        <Flex align="center" gap="3" className="iq-server-page__header">
          <Shield size={24} className="iq-server-page__icon" />
          <Box>
            <Heading as="h1" size="6" weight="medium">IQ Server</Heading>
            <Text size="2" className="iq-server-page__description">
              Manage Sonatype Repository Firewall and Lifecycle configuration
            </Text>
          </Box>
        </Flex>

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

  return (
    <Box className={`iq-server-page ${className || ''}`.trim()}>
      {/* Header */}
      <Flex align="center" gap="3" className="iq-server-page__header">
        <SettingsButton variant="ghost" onClick={handleBack} className="iq-server-page__back" icon={ArrowLeft}>
          Back
        </SettingsButton>
        <Shield size={24} className="iq-server-page__icon" />
        <Box>
          <Heading as="h1" size="6" weight="medium">IQ Server</Heading>
          <Text size="2" className="iq-server-page__description">
            Manage Sonatype Repository Firewall and Lifecycle configuration
          </Text>
        </Box>
      </Flex>

      {/* Connection Indicator - auto-tests on load */}
      <ConnectionIndicator
        status={connectionStatus}
        message={connectionMessage}
        className="iq-server-page__connection-indicator"
      />

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

      {/* Dashboard Link */}
      {canOpenDashboard && (
        <Box className="iq-server-page__dashboard-link">
          <a
            href={pristineSettings.url}
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
      {error && (
        <Box className="iq-server-page__alerts">
          <SettingsAlert type="error" onClose={() => setError(null)}>
            {error}
          </SettingsAlert>
        </Box>
      )}
      {successMessage && (
        <Box className="iq-server-page__alerts">
          <SettingsAlert type="success" onClose={() => setSuccessMessage(null)}>
            {successMessage}
          </SettingsAlert>
        </Box>
      )}

      {/* Form */}
      <Box className={showSaveConfirmModal ? 'iq-server-page__form-wrapper--modal-open' : ''}>
        <SettingsForm
          title=""
          onSubmit={handleSubmit}
          onCancel={handleDiscard}
          loading={loading}
          pristine={isPristine}
          submitDisabled={hasValidationErrors}
          showActions={!showSaveConfirmModal}
        >
        <SettingsFormSection title="IQ Server Configuration">
          <Box className="iq-server-page__config-grid">
            {/* Left column: form fields */}
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
            name="enabled"
            label="Enable IQ Server"
            checked={settings.enabled}
            onChange={(checked) => handleChange('enabled', checked)}
            description="Enable the use of IQ Server"
          />

          <SettingsTextInput
            name="url"
            label="IQ Server URL"
            value={settings.url}
            onChange={handleUrlChange}
            onBlur={() => handleBlur('url')}
            error={validationErrors.url}
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
            <SettingsCheckbox
              name="useTrustStoreForUrl"
              label="Use Nexus Repository Trust Store"
              checked={settings.useTrustStoreForUrl}
              onChange={(checked) => handleChange('useTrustStoreForUrl', checked)}
              description="Use certificates from the Nexus Repository trust store"
            />
          )}

          <SettingsSelect
            name="authenticationType"
            label="Authentication Method"
            value={settings.authenticationType}
            onChange={handleAuthTypeChange}
            error={validationErrors.authenticationType}
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
                name="username"
                label="Username"
                value={settings.username}
                onChange={(value) => handleChange('username', value)}
                onBlur={() => handleBlur('username')}
                error={validationErrors.username}
                helpText="User with access to the IQ Server"
                required
              />

              <SettingsPasswordInput
                name="password"
                label="Password"
                value={settings.password}
                onChange={(value) => handleChange('password', value)}
                onBlur={() => handleBlur('password')}
                error={validationErrors.password}
                helpText="Credentials for the IQ Server user"
                autoComplete="new-password"
                required
              />
            </>
          )}
            </Box>

            {/* Right column: Test Connection - greyed out until valid data */}
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
                        onClick={handleVerifyConnection}
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
          </Box>
        </SettingsFormSection>

        <SettingsFormSection title="Advanced Settings" collapsible defaultCollapsed>
          <SettingsTextInput
            name="timeoutSeconds"
            label="Connection Timeout (seconds)"
            type="number"
            value={settings.timeoutSeconds !== null ? String(settings.timeoutSeconds) : ''}
            onChange={(value) => handleChange('timeoutSeconds', value ? parseInt(value, 10) : null)}
            onBlur={() => handleBlur('timeoutSeconds')}
            error={validationErrors.timeoutSeconds}
            helpText="Seconds to wait for activity before stopping and retrying. Leave blank for globally defined HTTP timeout."
            placeholder="Globally Defined"
            min={1}
            max={3600}
          />

          <SettingsTextArea
            name="properties"
            label="Properties"
            value={settings.properties}
            onChange={(value) => handleChange('properties', value)}
            helpText="Additional properties to configure for IQ Server"
            rows={4}
          />

          {/* Property Selector Tool */}
          <Box className="iq-server-page__property-selector" mb="4">
            <Flex align="center" gap="2" mb="2">
              <Text size="1" weight="medium" color="gray">Common Properties</Text>
              <Tooltip content="Click to insert or update property. See documentation for more details.">
                <Info size={12} color="var(--gray-8)" />
              </Tooltip>
            </Flex>
            <Flex wrap="wrap" gap="2">
              {IQ_PROPERTY_EXAMPLES.map((example) => (
                <Box
                  key={example.value}
                  className="iq-server-page__property-chip"
                  onClick={() => insertProperty(example.value)}
                  title={example.description}
                >
                  <Plus size={10} className="iq-server-page__property-icon" />
                  <code style={{ fontSize: 'var(--font-size-1)', color: 'var(--gray-12)' }}>{example.label}</code>
                </Box>
              ))}
              <Separator orientation="vertical" size="1" />
              <a
                href="https://links.sonatype.com/products/nxc/docs/properties"
                target="_blank"
                rel="noopener noreferrer"
                className="iq-server-page__property-docs-link"
              >
                <Text size="1">Documentation <ExternalLink size={10} /></Text>
              </a>
            </Flex>
          </Box>

          <SettingsCheckbox
            name="showLink"
            label="Show IQ Server Link"
            checked={settings.showLink}
            onChange={(checked) => handleChange('showLink', checked)}
            description="Show IQ Server link in the Browse menu when the server is enabled"
          />
        </SettingsFormSection>

        {/* Full-screen Application List - same control pattern as Repository Structure Tree */}
        {(() => {
          const parsed = verificationResult?.reason ? parseApplicationReason(verificationResult.reason) : null;
          const appItems = parsed?.isList ? parsed.items : [];
          const appMessage = parsed && (!parsed.isList || parsed.items.length === 0) ? (parsed.items[0] || verificationResult?.reason) : undefined;
          return (
            <IqApplicationListModal
              isOpen={showApplicationListModal && !!verificationResult?.reason}
              items={appItems}
              message={appMessage}
              onClose={() => setShowApplicationListModal(false)}
            />
          );
        })()}

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
              href="http://links.sonatype.com/products/nxrm3/docs/iq"
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

        {/* Save Configuration Confirmation Modal - shown after successful connection test with unsaved changes */}
        <AlertDialog.Root open={showSaveConfirmModal}>
          <AlertDialog.Content maxWidth="450px">
            <Flex align="center" gap="3" mb="3">
              <Box
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  width: 36,
                  height: 36,
                  borderRadius: 'var(--radius-2)',
                  backgroundColor: 'var(--green-3)',
                  color: 'var(--green-11)',
                  flexShrink: 0,
                }}
              >
                <CheckCircle size={20} aria-hidden="true" />
              </Box>
              <AlertDialog.Title>Connection Test Successful</AlertDialog.Title>
            </Flex>

            <AlertDialog.Description size="2">
              The connection to IQ Server was successful. Would you like to save this configuration?
            </AlertDialog.Description>

            <Flex gap="3" mt="4" justify="between">
              <Button
                variant="solid"
                color="green"
                size="2"
                onClick={async () => {
                  await handleSubmit();
                  setShowSaveConfirmModal(false);
                }}
                disabled={loading}
                data-testid="save-config-confirm"
              >
                {loading && <Loader2 size={16} style={{ animation: 'spin 1s linear infinite' }} />}
                Save
              </Button>
              <Button
                variant="surface"
                color="gray"
                size="2"
                disabled={loading}
                onClick={() => {
                  setShowSaveConfirmModal(false);
                  handleDiscard();
                }}
                data-testid="save-config-cancel"
              >
                Don't Save
              </Button>
            </Flex>
          </AlertDialog.Content>
        </AlertDialog.Root>
      </SettingsForm>
      </Box>
    </Box>
  );
}

export default IqServerPage;


