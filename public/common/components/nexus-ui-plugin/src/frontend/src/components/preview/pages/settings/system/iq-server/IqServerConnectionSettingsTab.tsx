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
  Card,
  Checkbox,
  Flex,
  IconButton,
  Select,
  Text,
  TextArea,
  TextField,
} from '@radix-ui/themes';

import {
  ChevronDown,
  ChevronRight,
  CircleAlert,
  CircleCheck,
  Eye,
  EyeOff,
  Loader2,
  Plug,
  Shield,
  TriangleAlert,
} from 'lucide-react';

import {
  disableIqServer,
  fetchIqSettings,
  putIqSettings,
  toUpdatePayload,
} from './iqServerFormMachine';
import { verifyConnection as verifyIqConnection } from './iqConnectionMachine';
import { useIsCloud } from '../../../../shared/hooks/useIsCloud';
import { RequiredMark } from '../../../../shared';
import { formatErrorMessage, isUntrustedCertificateError } from './iqServerUtils';
import { CertificateViewDialog } from '../../repository/repositories/facets/CertificateViewDialog';
import { DisconnectConfirmationModal } from './DisconnectConfirmationModal';
import {
  IqServerConfiguration,
  DEFAULT_IQ_CONFIGURATION,
  PASSWORD_PLACEHOLDER,
} from './types';

import './IqServerConnectionSettingsTab.scss';

/**
 * What the Password field shows while it still holds the stored credential.
 *
 * PASSWORD_PLACEHOLDER is a protocol value shared with the backend, not user data: it means "reuse
 * the persisted secret". The Classic UI keeps it in form state and simply never offers a way to
 * reveal it (IqServerForm.jsx renders type="password" with no visibility toggle), so it stays
 * internal. This tab does offer a visibility toggle, so the sentinel needs a display value of
 * its own — these bullets — and the toggle is withheld while the sentinel is what is being held.
 */
const STORED_PASSWORD_MASK = '••••••••';

export interface IqServerConnectionSettingsTabProps {
  /** Called after a successful Save with the new server-acknowledged configuration. */
  onSaved: (settings: IqServerConfiguration) => void;
  /**
   * Called after a successful Disconnect. The dedicated disable endpoint returns
   * no body, so no configuration is passed.
   */
  onDisconnected: (disconnectedConfig?: IqServerConfiguration) => void;
  /**
   * Called when the initial config has been loaded from the server.
   * Allows the parent page to reconcile bootstrap-derived state with actual server state.
   */
  onConfigLoaded?: (config: IqServerConfiguration | undefined | null) => void;
  /** Parent hint: if true, render Disconnect on first paint without waiting for the /iq fetch. */
  initiallyConnected?: boolean;
  /** True when the user has settings:update; when false, the form is disabled. */
  canEdit?: boolean;
}

export function IqServerConnectionSettingsTab({
  onSaved,
  onDisconnected,
  onConfigLoaded,
  initiallyConnected = false,
  canEdit = true,
}: IqServerConnectionSettingsTabProps) {
  const [initialLoading, setInitialLoading] = useState(true);
  const [loading, setLoading] = useState(false);
  const [verifying, setVerifying] = useState(false);
  // On cloud, the URL is auto-synced from TENANT_IQ_SERVER_URL and reverts within seconds if edited.
  const isCloud = useIsCloud();

  const [settings, setSettings] = useState<IqServerConfiguration>(DEFAULT_IQ_CONFIGURATION);
  const [showPassword, setShowPassword] = useState(false);
  const [showAdvanced, setShowAdvanced] = useState(false);
  const [verifyMsg, setVerifyMsg] = useState<{ ok: boolean; text: string } | null>(null);
  const [certUntrusted, setCertUntrusted] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [touched, setTouched] = useState(false);
  const [settingsLoaded, setSettingsLoaded] = useState(false);
  const [showCertDialog, setShowCertDialog] = useState(false);
  const [showDisconnectModal, setShowDisconnectModal] = useState(false);

  // Snapshot of the last server-loaded configuration. Discard reverts the form to this so the
  // user can undo in-progress edits without reloading the page.
  const loadedSettingsRef = useRef<IqServerConfiguration>(DEFAULT_IQ_CONFIGURATION);

  useEffect(() => {
    let cancelled = false;
    setInitialLoading(true);
    setSettingsLoaded(false);
    fetchIqSettings()
      .then(formData => {
        if (cancelled) return;
        const data = toUpdatePayload(formData);
        const loaded = {
          ...data,
          // Mask the password the server will not return in clear text.
          password: data.password ? PASSWORD_PLACEHOLDER : '',
        };
        loadedSettingsRef.current = loaded;
        setSettings(loaded);
        setVerifyMsg(null);
        setCertUntrusted(false);
        setError(null);
        setTouched(false);
        setShowPassword(false);
        setShowAdvanced(false);
        // Notify parent of loaded config so it can reconcile bootstrap state with server state
        onConfigLoaded?.(data);
      })
      .catch(err => {
        if (cancelled) return;
        // Fall back to defaults so the user can still configure.
        setSettings(DEFAULT_IQ_CONFIGURATION);
        setError(err?.message || 'Failed to load IQ Server configuration');
        onConfigLoaded?.(null);
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
  }, [onConfigLoaded]);

  const update = useCallback(<K extends keyof IqServerConfiguration>(key: K, value: IqServerConfiguration[K]) => {
    setSettings(prev => ({ ...prev, [key]: value }));
    setTouched(true);
    setVerifyMsg(null);
    setCertUntrusted(false);
  }, []);

  // Classic UI parity (IqServerMachine.updateUrl): the stored credential belongs to the stored
  // URL, and the backend refuses the placeholder once the URL changes, so drop the mask and make
  // the user supply a password for the server they just pointed at.
  // Also clear useTrustStoreForUrl when moving off https — Classic UI's ValidationUtils.isSecureUrl
  // check does the same on the checkbox itself; matching that here keeps the persisted flag from
  // going stale after a scheme change.
  const updateUrl = useCallback((url: string) => {
    const trimmedUrl = url.trim();
    setSettings(prev => ({
      ...prev,
      url: trimmedUrl,
      ...(prev.password === PASSWORD_PLACEHOLDER ? { password: '' } : {}),
      ...(trimmedUrl.toLowerCase().startsWith('https://') ? {} : { useTrustStoreForUrl: false }),
    }));
    setTouched(true);
    setVerifyMsg(null);
    setCertUntrusted(false);
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

  // First-time state — no config ever saved; drives onboarding copy. PKI auth has no username by
  // design, so only treat a missing username as "unconfigured" under USER auth — otherwise a
  // configured-but-disabled PKI server would falsely show the onboarding copy on reconnect.
  const isFirstTime = !settings.enabled && !settings.url && (!isUserAuth || !settings.username);

  // Secure URL gate for the trust-store controls. Case-insensitive so "HTTPS://" also
  // qualifies, matching ValidationUtils.isSecureUrl (used by the Classic UseNexusTruststore).
  // Guard the value the same way canTest does below: a never-configured server can still
  // surface a null url, and trim() on null would crash the render.
  const isSecureUrl = useMemo(
    () => (settings.url ?? '').trim().toLowerCase().startsWith('https://'),
    [settings.url],
  );

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
    // Never send true for a non-https URL, even if the persisted flag is stale.
    useTrustStoreForUrl: isSecureUrl && settings.useTrustStoreForUrl,
    timeoutSeconds: settings.timeoutSeconds,
    properties: settings.properties,
    showLink: settings.showLink,
    ...(settings.failOpenModeEnabled === undefined
      ? {}
      : { failOpenModeEnabled: settings.failOpenModeEnabled }),
  }), [settings, isUserAuth, isSecureUrl]);

  const handleTest = useCallback(async () => {
    setVerifyMsg(null);
    setCertUntrusted(false);
    setError(null);
    setVerifying(true);
    try {
      const result = await verifyIqConnection(buildPayload(true));
      const ok = !!result.success;
      const untrusted = !ok && isUntrustedCertificateError(result.reason);
      setVerifyMsg({
        ok,
        text: ok
          ? 'Connection successful'
          : formatErrorMessage({ message: result.reason }, 'Connection failed'),
      });
      setCertUntrusted(untrusted);
    } finally {
      setVerifying(false);
    }
  }, [buildPayload]);

  const handleSave = useCallback(async () => {
    setError(null);
    setVerifyMsg(null);
    setCertUntrusted(false);
    // Pre-verify so auth failures surface inline instead of after the tab commits.
    setVerifying(true);
    let verify;
    try {
      verify = await verifyIqConnection(buildPayload(true));
    } catch (err: any) {
      // Verify threw instead of returning {success: false} - show the error and abort save
      setVerifyMsg({
        ok: false,
        text: formatErrorMessage(err, 'Connection verification failed'),
      });
      return;
    } finally {
      setVerifying(false);
    }
    if (!verify.success) {
      setVerifyMsg({
        ok: false,
        text: formatErrorMessage({ message: verify.reason }, 'Connection failed'),
      });
      setCertUntrusted(isUntrustedCertificateError(verify.reason));
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
  //
  // Confirmed through DisconnectConfirmationModal first: disconnecting stops Repository
  // Firewall from blocking components and halts policy enforcement, so the destructive
  // action is gated behind an explicit confirm rather than firing on the button's click.
  const handleDisconnect = useCallback(async () => {
    setError(null);
    setLoading(true);
    try {
      await disableIqServer();
      setShowDisconnectModal(false);
      // Reflect the now-disabled server in the tab's own state so it refreshes into its reconnect
      // view without a reload. wasConnected is `initiallyConnected || settings.enabled`: the parent
      // flips initiallyConnected to false, but this tab stays mounted (no refetch), so a stale
      // settings.enabled=true would keep the Disconnect button and connected layout stuck on screen.
      // The disable endpoint returns no body, so flip the flag on the last-loaded config directly
      // (loadedSettingsRef too, so a later Discard doesn't revert to the connected state).
      setSettings(prev => ({ ...prev, enabled: false }));
      loadedSettingsRef.current = { ...loadedSettingsRef.current, enabled: false };
      onDisconnected();
    } catch (err: any) {
      // Surface the failure on the page and close the modal so the error callout is visible.
      setShowDisconnectModal(false);
      setError(formatErrorMessage(err, 'Failed to disconnect from IQ Server'));
    } finally {
      setLoading(false);
    }
  }, [onDisconnected]);

  // Revert every in-progress edit back to the last server-loaded configuration. Nothing is sent
  // to the backend — this is a pure client-side undo, so it also clears any verify/cert/error state.
  const handleDiscard = useCallback(() => {
    setSettings(loadedSettingsRef.current);
    setTouched(false);
    setShowPassword(false);
    setShowAdvanced(false);
    setVerifyMsg(null);
    setCertUntrusted(false);
    setError(null);
  }, []);

  // Once initiallyConnected=true the Disconnect button must stay visible even if
  // fetchIqSettings races and returns enabled:false before the page has committed
  // the disconnect. initiallyConnected is the authoritative parent signal.
  const wasConnected = initiallyConnected || (settingsLoaded ? settings.enabled : false);

  return (
    <Box
      className="iq-connection-settings-tab"
      p="4"
      data-testid="iq-connection-settings-tab"
      aria-disabled={!canEdit || undefined}
    >
      {initialLoading ? (
        <Flex align="center" justify="center" gap="3" py="7" aria-busy="true" aria-live="polite">
          <Loader2 size={20} className="iq-tab-spinner" aria-hidden="true" />
          <Text size="2" color="gray">Loading configuration...</Text>
        </Flex>
      ) : (
        <fieldset
          className="iq-connection-settings-tab__fieldset"
          disabled={!canEdit}
          aria-label="IQ Server connection settings"
        >
          <Card size="3" variant="surface">
          <Flex direction="column" gap="4">
          {/* Action row sits at the top of the card: Disconnect on the left, Discard + Save on
              the right. Save inherits the page accent (no explicit color) to match the design. */}
          <Flex align="center" justify="between" gap="3">
            <Flex align="center" gap="3">
              {wasConnected && (
                <Button
                  type="button"
                  color="red"
                  variant="solid"
                  onClick={() => setShowDisconnectModal(true)}
                  disabled={loading}
                  data-testid="iq-tab-disconnect"
                >
                  Disconnect
                </Button>
              )}
            </Flex>
            <Flex align="center" gap="3">
              <Button
                type="button"
                variant="surface"
                color="gray"
                onClick={handleDiscard}
                disabled={loading || verifying || !touched}
                data-testid="iq-tab-discard"
              >
                Discard
              </Button>
              <Button
                type="button"
                onClick={handleSave}
                disabled={loading || verifying || !canTest || (!touched && !isReconnect)}
                data-testid="iq-tab-save"
              >
                {verifying ? 'Verifying...' : loading ? 'Saving...' : 'Save'}
              </Button>
            </Flex>
          </Flex>

          {isFirstTime && canEdit && (
            <Box>
              <Text size="2" color="gray">
                Get started by entering your IQ Server URL and credentials. Use Test Connection to verify before saving.
              </Text>
            </Box>
          )}
          {!canEdit && (
            <Box>
              <Callout.Root color="gray" variant="soft">
                <Callout.Icon>
                  <TriangleAlert size={16} aria-hidden="true" />
                </Callout.Icon>
                <Callout.Text>
                  You are viewing a read-only version of this page. You do not have permission to
                  edit. Contact your Administrator if you need additional permissions.
                </Callout.Text>
              </Callout.Root>
            </Box>
          )}

          {error && (
            <Box>
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
              <Text as="label" htmlFor="iq-tab-url" size="2" weight="medium">IQ Server URL<RequiredMark /></Text>
              <Text size="1" color="gray" as="div" mb="1">
                {isCloud
                  ? 'Managed by tenant configuration. Contact your administrator to change this.'
                  : 'The address of your IQ Server'}
              </Text>
              <TextField.Root
                id="iq-tab-url"
                value={settings.url}
                onChange={e => updateUrl(e.target.value)}
                required
                disabled={isCloud}
                readOnly={isCloud}
                aria-readonly={isCloud}
                title={isCloud ? 'URL is managed by tenant configuration' : undefined}
              />
            </Box>

            {isSecureUrl && (
              <Box className="iq-connection-settings-tab__trust-store">
                <Flex as="label" align="center" gap="2">
                  <Checkbox
                    checked={settings.useTrustStoreForUrl}
                    onCheckedChange={(v: boolean | 'indeterminate') => update('useTrustStoreForUrl', v === true)}
                    data-testid="iq-tab-use-truststore"
                  />
                  <Text size="2">Use certificates in the Nexus Repository Truststore for this URL</Text>
                </Flex>
                <Box mt="2">
                  <Button
                    type="button"
                    size="1"
                    variant="surface"
                    color="gray"
                    onClick={() => setShowCertDialog(true)}
                    data-testid="iq-tab-view-cert"
                  >
                    <Shield size={14} aria-hidden="true" />
                    View Certificate
                  </Button>
                </Box>
                {certUntrusted && (
                  <Flex
                    align="start"
                    gap="1"
                    mt="1"
                    className="iq-connection-settings-tab__cert-warning"
                    role="status"
                    data-testid="iq-tab-cert-warning"
                  >
                    <Box style={{ height: 20, display: 'flex', alignItems: 'center', flexShrink: 0 }}>
                      <TriangleAlert size={14} aria-hidden="true" />
                    </Box>
                    <Text size="2">
                      The server certificate is not trusted. View and add it to the truststore, then test again.
                    </Text>
                  </Flex>
                )}
              </Box>
            )}

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
                  setCertUntrusted(false);
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
                  <Text as="label" htmlFor="iq-tab-user" size="2" weight="medium">Username<RequiredMark /></Text>
                  <Text size="1" color="gray" as="div" mb="1">User with access to the IQ Server</Text>
                  <TextField.Root
                    id="iq-tab-user"
                    placeholder="e.g. admin"
                    value={settings.username}
                    onChange={e => update('username', e.target.value)}
                    autoComplete="off"
                    required
                  />
                </Box>
                <Box>
                  <Text as="label" htmlFor="iq-tab-pw" size="2" weight="medium">Password<RequiredMark /></Text>
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
                          id="iq-tab-pw"
                          type="password"
                          value={STORED_PASSWORD_MASK}
                          readOnly
                          aria-readonly
                          onChange={() => {}}
                          autoComplete="off"
                          data-testid="iq-tab-pw-stored"
                        />
                      </Box>
                      <Button
                        type="button"
                        variant="soft"
                        color="gray"
                        onClick={clearStoredPassword}
                        data-testid="iq-tab-pw-change"
                      >
                        Change
                      </Button>
                    </Flex>
                  ) : (
                    <TextField.Root
                      id="iq-tab-pw"
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
                variant="surface"
                onClick={handleTest}
                disabled={!canTest}
                data-testid="iq-tab-test"
              >
                <Plug size={14} aria-hidden="true" />
                {verifying ? 'Testing...' : 'Test Connection'}
              </Button>
              {verifyMsg && (
                <Flex
                  align="start"
                  gap="1"
                  className={`iq-connection-settings-tab__verify iq-connection-settings-tab__verify--${verifyMsg.ok ? 'ok' : 'fail'}`}
                  role="status"
                  data-testid="iq-tab-verify-msg"
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
                variant="outline"
                color="gray"
                onClick={() => setShowAdvanced(v => !v)}
                aria-expanded={showAdvanced}
                data-testid="iq-tab-advanced-toggle"
              >
                {showAdvanced ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
                Advanced Settings
              </Button>
              {showAdvanced && (
                <Box mt="2" pl="3" className="iq-connection-settings-tab__advanced">
                  <Box mb="3">
                    <Text as="label" htmlFor="iq-tab-timeout" size="2" weight="medium">Connection Timeout</Text>
                    <Text size="1" color="gray" as="div" mb="1">
                      Seconds to wait for activity before stopping and retrying. Leave blank to use the global HTTP timeout.
                    </Text>
                    <TextField.Root
                      id="iq-tab-timeout"
                      type="number"
                      min={0}
                      placeholder="e.g. 30"
                      value={settings.timeoutSeconds == null ? '' : String(settings.timeoutSeconds)}
                      onChange={e => {
                        const v = e.target.value;
                        if (v === '') {
                          update('timeoutSeconds', null);
                        } else {
                          const num = Number(v);
                          // Keep the previous value for NaN or negative input — a timeout
                          // must be a non-negative number of seconds.
                          if (!Number.isNaN(num) && num >= 0) {
                            update('timeoutSeconds', num);
                          }
                        }
                      }}
                    />
                  </Box>
                  <Box mb="3">
                    <Text as="label" htmlFor="iq-tab-props" size="2" weight="medium">Properties</Text>
                    <Text size="1" color="gray" as="div" mb="1">Additional properties to configure for IQ Server</Text>
                    <TextArea
                      id="iq-tab-props"
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
          </Flex>
          </Card>
        </fieldset>
      )}

      {showCertDialog && (
        <CertificateViewDialog
          remoteUrl={settings.url}
          onClose={() => setShowCertDialog(false)}
        />
      )}

      <DisconnectConfirmationModal
        open={showDisconnectModal}
        loading={loading}
        onClose={() => setShowDisconnectModal(false)}
        onConfirm={handleDisconnect}
      />
    </Box>
  );
}

export default IqServerConnectionSettingsTab;
