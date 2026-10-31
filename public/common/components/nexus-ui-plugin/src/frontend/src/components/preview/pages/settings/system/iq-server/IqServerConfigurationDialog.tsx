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

import React, { useEffect, useState, useCallback, useMemo, useRef } from 'react';
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
  disableIqServer,
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

import './IqServerConfigurationDialog.scss';

/**
 * What the Password field shows while it still holds the stored credential.
 *
 * PASSWORD_PLACEHOLDER is a protocol value shared with the backend, not user data: it means "reuse
 * the persisted secret". The Classic UI keeps it in form state and simply never offers a way to
 * reveal it (IqServerForm.jsx renders type="password" with no visibility toggle), so it stays
 * internal. This dialog does offer a visibility toggle, so the sentinel needs a display value of
 * its own — these bullets — and the toggle is withheld while the sentinel is what is being held.
 */
const STORED_PASSWORD_MASK = '\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022';

export interface IqServerConfigurationDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** Called after a successful Save with the new server-acknowledged configuration. */
  onSaved: (settings: IqServerConfiguration) => void;
  /**
   * Called after a successful Disconnect. The dedicated disable endpoint returns
   * no body, so no configuration is passed.
   */
  onDisconnected: (disconnectedConfig?: IqServerConfiguration) => void;
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

  // Classic UI parity (IqServerMachine.updateUrl): the stored credential belongs to the stored
  // URL, and the backend refuses the placeholder once the URL changes, so drop the mask and make
  // the user supply a password for the server they just pointed at.
  const updateUrl = useCallback((url: string) => {
    setSettings(prev => ({
      ...prev,
      url,
      ...(prev.password === PASSWORD_PLACEHOLDER ? { password: '' } : {}),
    }));
    setTouched(true);
    setVerifyMsg(null);
  }, []);

  const passwordInputRef = useRef<HTMLInputElement>(null);

  const isUserAuth = !settings.authenticationType || settings.authenticationType === 'USER';

  /**
   * True while the Password field still stands for the persisted credential rather than something
   * the user typed. Derived from the value itself so there is one source of truth: `settings.password`
   * remains exactly what buildPayload sends, and the three states the UI must tell apart fall out of
   * it — sentinel (stored, untouched), '' (explicitly cleared, or never configured), anything else
   * (a new password).
   */
  const storedPasswordRetained = isUserAuth && settings.password === PASSWORD_PLACEHOLDER;

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

  // Reconnecting a configured-but-disabled instance needs no field edit: the URL, username and
  // (via the placeholder) the stored credential are all loaded already, so Save must not require
  // the user to touch something just to re-enable IQ Server.
  /**
   * Discards the stored-credential representation so the user can type a replacement. Deliberate and
   * explicit: an empty field afterwards means "no password supplied", never "fall back to the stored
   * one", which is why the sentinel is not something a stray keystroke or focus can erode.
   */
  const clearStoredPassword = useCallback(() => {
    update('password', '');
    setShowPassword(false);
    // Ref may be absent for one paint while the editable field replaces the read-only one.
    requestAnimationFrame(() => passwordInputRef.current?.focus());
  }, [update]);

  const isReconnect = settingsLoaded && !settings.enabled && !isFirstTime;

  // Build payload explicitly to avoid dropping password via spread.
  // failOpenModeEnabled has no UI control, but PUT /v1/iq re-derives the
  // stored flag from the payload, so it must be echoed back or a stored `true` is reset
  // to `false`. Only sent when the GET actually returned it (self-hosted); cloud never
  // does, so the key is omitted there rather than sending `false`.
  const buildPayload = useCallback((enabled: boolean): IqServerConfiguration => ({
    enabled,
    url: settings.url,
    // The dropdown renders `settings.authenticationType || 'USER'`, so an absent value still
    // displays User Authentication. Send what is displayed: the API rejects a null/blank
    // authenticationType, and a failed load leaves the '' default in state.
    authenticationType: settings.authenticationType || 'USER',
    username: isUserAuth ? settings.username : '',
    // May be PASSWORD_PLACEHOLDER, which stands for the persisted credential: the backend resolves
    // it to the stored secret for both Test Connection and Save (IqUiResource.getPassword,
    // IqApiResource.getPassword / updateConfiguration), so the clear-text password never has to
    // reach the browser and the user never has to retype a password we already hold. It is refused
    // in exactly one case — the submitted URL differs from the stored one — which updateUrl
    // prevents at its source by clearing the field whenever the URL is edited.
    password: isUserAuth ? settings.password : '',
    useTrustStoreForUrl: settings.useTrustStoreForUrl,
    timeoutSeconds: settings.timeoutSeconds,
    properties: settings.properties,
    showLink: settings.showLink,
    ...(settings.failOpenModeEnabled === undefined
      ? {}
      : { failOpenModeEnabled: settings.failOpenModeEnabled }),
  }), [settings, isUserAuth]);

  const handleTest = useCallback(async () => {
    setVerifyMsg(null);
    setError(null);
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
  }, [buildPayload]);

  const handleSave = useCallback(async () => {
    setError(null);
    setVerifyMsg(null);
    // Pre-verify so auth failures surface inline instead of after the dialog closes.
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
  }, [onSaved, buildPayload]);

  // Disconnect through POST /v1/iq/disable rather than a rebuilt PUT
  // payload. The backend reuses the stored configuration, so settings the GET never
  // exposes (failOpenModeEnabled) are preserved, and no remote IQ Server call is
  // made — disconnecting works while the server is unreachable.
  const handleDisconnect = useCallback(async () => {
    setError(null);
    setLoading(true);
    try {
      await disableIqServer();
      onDisconnected();
    } catch (err: any) {
      setError(formatErrorMessage(err, 'Failed to disconnect from IQ Server'));
    } finally {
      setLoading(false);
    }
  }, [onDisconnected]);

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
              onChange={e => updateUrl(e.target.value)}
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
                <Text size="1" color="gray" as="div" mb="1">
                  {storedPasswordRetained
                    ? 'A password is already saved for this IQ Server. Change it to use a different one.'
                    : 'Credentials for the IQ Server user'}
                </Text>
                {storedPasswordRetained ? (
                  /* Stored credential: a status display, not an editable secret. Read-only, always
                     masked, and with no visibility toggle, because the value behind it is the
                     sentinel and the browser has no password to reveal in the first place. */
                  <Flex align="center" gap="2">
                    <Box flexGrow="1">
                      <TextField.Root
                        id="iq-dlg-pw"
                        type="password"
                        value={STORED_PASSWORD_MASK}
                        readOnly
                        aria-readonly
                        onChange={() => {}}
                        autoComplete="off"
                        data-testid="iq-dlg-pw-stored"
                      />
                    </Box>
                    <Button
                      type="button"
                      variant="soft"
                      color="gray"
                      onClick={clearStoredPassword}
                      data-testid="iq-dlg-pw-change"
                    >
                      Change
                    </Button>
                  </Flex>
                ) : (
                  <TextField.Root
                    id="iq-dlg-pw"
                    ref={passwordInputRef}
                    type={showPassword ? 'text' : 'password'}
                    placeholder="Enter your IQ Server password"
                    value={settings.password}
                    onChange={e => update('password', e.target.value)}
                    autoComplete="new-password"
                    required
                  >
                    {/* Toggle only once there is a typed value to reveal, matching
                        SettingsPasswordInput (`showToggle && value`). */}
                    {settings.password && (
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
                    )}
                  </TextField.Root>
                )}
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
              disabled={loading || verifying || !canTest || (!touched && !isReconnect)}
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
