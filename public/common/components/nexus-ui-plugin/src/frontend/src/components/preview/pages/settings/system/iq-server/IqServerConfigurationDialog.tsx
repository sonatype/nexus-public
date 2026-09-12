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

import React, { useEffect, useState, useCallback, useMemo } from 'react';
import {
  Box,
  Button,
  Callout,
  Checkbox,
  Dialog,
  Flex,
  IconButton,
  Select,
  Separator,
  Text,
  TextArea,
  TextField,
} from '@radix-ui/themes';

import { ChevronDown, ChevronRight, CircleCheck, CircleAlert, Eye, EyeOff, Loader2 } from 'lucide-react';

import {
  fetchIqSettings,
  putIqSettings,
  toUpdatePayload,
} from './iqServerFormMachine';
import { verifyConnection as verifyIqConnection } from './iqConnectionMachine';
import { useIsCloud } from '../../../../shared/hooks/useIsCloud';
import { RequiredMark } from '../../../../shared';
import { formatErrorMessage } from './iqServerUtils';
import {
  IqServerConfiguration,
  DEFAULT_IQ_CONFIGURATION,
  PASSWORD_PLACEHOLDER,
} from './types';
// PASSWORD_PLACEHOLDER masks the stored password; passed back on Test/Save so backend resolves it.

import './IqServerConfigurationDialog.scss';

export interface IqServerConfigurationDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** Called after a successful Save with the new server-acknowledged configuration. */
  onSaved: (settings: IqServerConfiguration) => void;
  /** Called after a successful Disconnect (Save with enabled=false) with the response. */
  onDisconnected: (disconnectedConfig: IqServerConfiguration) => void;
  /** Parent hint: if true, render Disconnect on first paint without waiting for the /iq fetch. */
  initiallyConnected?: boolean;
}

export function IqServerConfigurationDialog({
  open,
  onOpenChange,
  onSaved,
  onDisconnected,
  initiallyConnected = false,
}: IqServerConfigurationDialogProps) {
  const [initialLoading, setInitialLoading] = useState(false);
  const [loading, setLoading] = useState(false);
  const [verifying, setVerifying] = useState(false);
  // On cloud, the URL is auto-synced from TENANT_IQ_SERVER_URL and reverts within seconds if edited.
  const isCloud = useIsCloud();

  const [settings, setSettings] = useState<IqServerConfiguration>(DEFAULT_IQ_CONFIGURATION);
  const [showPassword, setShowPassword] = useState(false);
  const [showAdvanced, setShowAdvanced] = useState(false);
  const [verifyMsg, setVerifyMsg] = useState<{ ok: boolean; text: string } | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [touched, setTouched] = useState(false);
  const [settingsLoaded, setSettingsLoaded] = useState(false);

  useEffect(() => {
    if (!open) return;
    let cancelled = false;
    setInitialLoading(true);
    setSettingsLoaded(false);
    fetchIqSettings()
      .then(formData => {
        if (cancelled) return;
        const data = toUpdatePayload(formData);
        setSettings({
          ...data,
          // Mask the password the server will not return in clear text.
          password: data.password ? PASSWORD_PLACEHOLDER : '',
        });
        setVerifyMsg(null);
        setError(null);
        setTouched(false);
        setShowPassword(false);
        setShowAdvanced(false);
      })
      .catch(err => {
        if (cancelled) return;
        // Fall back to defaults so the user can still configure.
        setSettings(DEFAULT_IQ_CONFIGURATION);
        setError(err?.message || 'Failed to load IQ Server configuration');
      })
      .finally(() => {
        if (!cancelled) {
          setInitialLoading(false);
          setSettingsLoaded(true);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [open]);

  const update = useCallback(<K extends keyof IqServerConfiguration>(key: K, value: IqServerConfiguration[K]) => {
    setSettings(prev => ({ ...prev, [key]: value }));
    setTouched(true);
    setVerifyMsg(null);
  }, []);

  const isUserAuth = !settings.authenticationType || settings.authenticationType === 'USER';

  // First-time state — no config ever saved; drives onboarding copy.
  const isFirstTime = !settings.enabled && !settings.url && !settings.username;

  const canTest = useMemo(() => {
    if (verifying) return false;
    if (!settings.url || !settings.url.trim()) return false;
    if (isUserAuth) {
      if (!settings.username || !settings.username.trim()) return false;
      if (!settings.password) return false;
    }
    return true;
  }, [verifying, settings.url, settings.username, settings.password, isUserAuth]);

  // Save/Disconnect: backend accepts PASSWORD_PLACEHOLDER as "reuse stored password".
  // Test Connection: backend rejects the placeholder — user must retype password.
  const passwordIsPlaceholder = settings.password === PASSWORD_PLACEHOLDER;

  // Build payload explicitly to avoid dropping password via spread.
  const buildPayload = useCallback((enabled: boolean): IqServerConfiguration => ({
    enabled,
    url: settings.url,
    authenticationType: settings.authenticationType,
    username: isUserAuth ? settings.username : '',
    password: isUserAuth ? settings.password : '',
    useTrustStoreForUrl: settings.useTrustStoreForUrl,
    timeoutSeconds: settings.timeoutSeconds,
    properties: settings.properties,
    showLink: settings.showLink,
  }), [settings, isUserAuth]);

  const handleTest = useCallback(async () => {
    setVerifyMsg(null);
    setError(null);
    if (isUserAuth && passwordIsPlaceholder) {
      setVerifyMsg({
        ok: false,
        text: 'Type your password again to test the connection.',
      });
      return;
    }
    setVerifying(true);
    try {
      const result = await verifyIqConnection(buildPayload(true));
      setVerifyMsg({
        ok: !!result.success,
        text: result.success ? 'Connection successful' : formatErrorMessage({ message: result.reason }, 'Connection failed'),
      });
    } finally {
      setVerifying(false);
    }
  }, [isUserAuth, passwordIsPlaceholder, buildPayload]);

  const handleSave = useCallback(async () => {
    setError(null);
    setVerifyMsg(null);
    // Pre-verify so auth failures surface inline instead of after the dialog closes.
    if (isUserAuth && passwordIsPlaceholder) {
      setVerifyMsg({
        ok: false,
        text: 'Type your password again to save.',
      });
      return;
    }
    setVerifying(true);
    let verify;
    try {
      verify = await verifyIqConnection(buildPayload(true));
    } finally {
      setVerifying(false);
    }
    if (!verify.success) {
      setVerifyMsg({
        ok: false,
        text: formatErrorMessage({ message: verify.reason }, 'Connection failed'),
      });
      return;
    }
    setLoading(true);
    try {
      const savedConfig = await putIqSettings(buildPayload(true));
      onSaved(savedConfig);
    } catch (err: any) {
      setError(formatErrorMessage(err, 'Failed to save IQ Server configuration'));
    } finally {
      setLoading(false);
    }
  }, [onSaved, isUserAuth, passwordIsPlaceholder, buildPayload]);

  const handleDisconnect = useCallback(async () => {
    setError(null);
    setLoading(true);
    try {
      const disconnectedConfig = await putIqSettings(buildPayload(false));
      onDisconnected(disconnectedConfig);
    } catch (err: any) {
      setError(formatErrorMessage(err, 'Failed to disconnect from IQ Server'));
    } finally {
      setLoading(false);
    }
  }, [onDisconnected, buildPayload]);

  // Once initiallyConnected=true the Disconnect button must stay visible even if
  // fetchIqSettings races and returns enabled:false before the page has committed
  // the disconnect. initiallyConnected is the authoritative parent signal.
  const wasConnected = initiallyConnected || (settingsLoaded ? settings.enabled : false);

  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Content
        maxWidth="560px"
        className="iq-connection-settings-dialog"
        onOpenAutoFocus={e => e.preventDefault()}
      >
        {/* Let Radix auto-wire Description ↔ Content via its internal context — explicit id + aria-describedby bypasses that registration and triggers the "missing Description" warning even when both are present. */}
        <Dialog.Title>IQ Server Connection Settings</Dialog.Title>
        <Dialog.Description size="2" color="gray" mb="4">
          {isFirstTime
            ? 'Get started by entering your IQ Server URL and credentials. Use Test Connection to verify before saving.'
            : 'IQ Server can evaluate application and organizing policies. Configure the URL, authentication method, and credentials below.'}
        </Dialog.Description>

        {initialLoading ? (
          <Flex align="center" justify="center" gap="3" py="7" aria-busy="true" aria-live="polite">
            <Loader2 size={20} className="iq-dlg-spinner" aria-hidden="true" />
            <Text size="2" color="gray">Loading configuration...</Text>
          </Flex>
        ) : (
        <>
        {error && (
          <Box mb="3">
            <Callout.Root color="red" variant="soft" role="alert">
              <Callout.Icon>
                <CircleAlert size={16} aria-hidden="true" />
              </Callout.Icon>
              <Callout.Text>{error}</Callout.Text>
            </Callout.Root>
          </Box>
        )}

        <Flex direction="column" gap="3">
          <Box>
            <Text as="label" htmlFor="iq-dlg-url" size="2" weight="medium">IQ Server URL<RequiredMark /></Text>
            <Text size="1" color="gray" as="div" mb="1">
              {isCloud
                ? 'Managed by tenant configuration. Contact your administrator to change this.'
                : 'The address of your IQ Server'}
            </Text>
            <TextField.Root
              id="iq-dlg-url"
              value={settings.url}
              onChange={e => update('url', e.target.value)}
              required
              disabled={isCloud}
              readOnly={isCloud}
              aria-readonly={isCloud}
              title={isCloud ? 'URL is managed by tenant configuration' : undefined}
            />
          </Box>

          <Box>
            <Text as="label" size="2" weight="medium">Authentication Method<RequiredMark /></Text>
            <Text size="1" color="gray" as="div" mb="1">How Nexus Repository authenticates with IQ Server</Text>
            <Select.Root
              value={settings.authenticationType || 'USER'}
              onValueChange={v => {
                const authType = v as 'USER' | 'PKI';
                setSettings(prev => ({
                  ...prev,
                  authenticationType: authType,
                  ...(authType === 'PKI' ? { username: '', password: '' } : {}),
                }));
                setTouched(true);
                setVerifyMsg(null);
              }}
            >
              <Select.Trigger style={{ width: '100%' }} />
              <Select.Content position="popper" side="bottom" align="start" sideOffset={4}>
                <Select.Item value="USER">User Authentication</Select.Item>
                <Select.Item value="PKI">PKI Authentication</Select.Item>
              </Select.Content>
            </Select.Root>
          </Box>

          {isUserAuth && (
            <>
              <Box>
                <Text as="label" htmlFor="iq-dlg-user" size="2" weight="medium">Username<RequiredMark /></Text>
                <Text size="1" color="gray" as="div" mb="1">User with access to the IQ Server</Text>
                <TextField.Root
                  id="iq-dlg-user"
                  placeholder="e.g. admin"
                  value={settings.username}
                  onChange={e => update('username', e.target.value)}
                  autoComplete="off"
                  required
                />
              </Box>
              <Box>
                <Text as="label" htmlFor="iq-dlg-pw" size="2" weight="medium">Password<RequiredMark /></Text>
                <Text size="1" color="gray" as="div" mb="1">Credentials for the IQ Server user</Text>
                <TextField.Root
                  id="iq-dlg-pw"
                  type={showPassword ? 'text' : 'password'}
                  placeholder="Enter your IQ Server password"
                  value={settings.password}
                  onChange={e => update('password', e.target.value)}
                  autoComplete="new-password"
                  required
                >
                  <TextField.Slot side="right">
                    <IconButton
                      type="button"
                      variant="ghost"
                      size="1"
                      aria-label={showPassword ? 'Hide password' : 'Show password'}
                      onClick={() => setShowPassword(v => !v)}
                    >
                      {showPassword ? <EyeOff size={14} /> : <Eye size={14} />}
                    </IconButton>
                  </TextField.Slot>
                </TextField.Root>
              </Box>
            </>
          )}

          <Flex align="start" gap="3">
            <Button
              type="button"
              variant="outline"
              color="blue"
              onClick={handleTest}
              disabled={!canTest}
              data-testid="iq-dlg-test"
            >
              {verifying ? 'Testing...' : 'Test Connection'}
            </Button>
            {verifyMsg && (
              <Flex
                align="start"
                gap="1"
                className={`iq-connection-settings-dialog__verify iq-connection-settings-dialog__verify--${verifyMsg.ok ? 'ok' : 'fail'}`}
                role="status"
              >
                {/* Icon wrapper matches one line-height (Text size="2" ≈ 20px)
                    and centers the 14px icon within, so it aligns with the
                    optical center of the first line whether the message wraps
                    or not. */}
                <Box style={{ height: 20, display: 'flex', alignItems: 'center', flexShrink: 0 }}>
                  {verifyMsg.ok ? <CircleCheck size={14} aria-hidden="true" /> : <CircleAlert size={14} aria-hidden="true" />}
                </Box>
                <Text size="2">{verifyMsg.text}</Text>
              </Flex>
            )}
          </Flex>

          <Box>
            <Button
              type="button"
              variant="ghost"
              color="gray"
              onClick={() => setShowAdvanced(v => !v)}
              aria-expanded={showAdvanced}
              data-testid="iq-dlg-advanced-toggle"
            >
              {showAdvanced ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
              <Text size="2">Advanced Settings</Text>
            </Button>
            {showAdvanced && (
              <Box mt="2" pl="3" className="iq-connection-settings-dialog__advanced">
                <Box mb="3">
                  <Text as="label" htmlFor="iq-dlg-timeout" size="2" weight="medium">Connection Timeout</Text>
                  <Text size="1" color="gray" as="div" mb="1">
                    Seconds to wait for activity before stopping and retrying. Leave blank to use the global HTTP timeout.
                  </Text>
                  <TextField.Root
                    id="iq-dlg-timeout"
                    type="number"
                    placeholder="e.g. 30"
                    value={settings.timeoutSeconds == null ? '' : String(settings.timeoutSeconds)}
                    onChange={e => {
                      const v = e.target.value;
                      update('timeoutSeconds', v === '' ? null : Number(v));
                    }}
                  />
                </Box>
                <Box mb="3">
                  <Text as="label" htmlFor="iq-dlg-props" size="2" weight="medium">Properties</Text>
                  <Text size="1" color="gray" as="div" mb="1">Additional properties to configure for IQ Server</Text>
                  <TextArea
                    id="iq-dlg-props"
                    placeholder="key=value (one per line)"
                    rows={3}
                    value={settings.properties}
                    onChange={e => update('properties', e.target.value)}
                  />
                </Box>
                <Box>
                  <Flex as="label" align="center" gap="2">
                    <Checkbox
                      checked={settings.showLink}
                      onCheckedChange={(v: boolean | 'indeterminate') => update('showLink', v === true)}
                    />
                    <Text size="2">Show IQ Server link in the Browse menu when the server is enabled</Text>
                  </Flex>
                </Box>
              </Box>
            )}
          </Box>
        </Flex>
        </>
        )}

        <Separator my="4" size="4" />

        <Flex align="center" justify="between" gap="3">
          <Box>
            {wasConnected && (
              <Button
                type="button"
                color="red"
                variant="solid"
                onClick={handleDisconnect}
                disabled={loading}
                data-testid="iq-dlg-disconnect"
              >
                Disconnect
              </Button>
            )}
          </Box>
          <Flex gap="2">
            <Dialog.Close>
              <Button type="button" variant="soft" color="gray" data-testid="iq-dlg-cancel">
                Cancel
              </Button>
            </Dialog.Close>
            <Button
              type="button"
              color="blue"
              onClick={handleSave}
              disabled={loading || verifying || !touched}
              data-testid="iq-dlg-save"
            >
              {verifying ? 'Verifying...' : loading ? 'Saving...' : 'Save'}
            </Button>
          </Flex>
        </Flex>
      </Dialog.Content>
    </Dialog.Root>
  );
}

export default IqServerConfigurationDialog;
