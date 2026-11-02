/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are
 * trademarks of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark
 * of the Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

/**
 * Firewall config tab for Edit repository form – proxy repos only.
 * Protection level (None | Audit | Quarantine | PCCS) is a dropdown; selecting an item only
 * stages a pending change, which the parent form's Save Changes / Cancel buttons commit or
 * discard (FIRE-958) via onSaveRef / onCancelRef.
 * Includes Repository Health Check section (enable/disable) — that section stays immediate-apply.
 */
import React, { useState, useEffect, useCallback, useRef } from 'react';
import { Box, Flex, Text, Card, Button, Separator, Callout } from '@radix-ui/themes';
import { ShieldCheck, ExternalLink, AlertCircle } from 'lucide-react';
import {
  useFirewallEnable,
  fetchIqAuditStatus,
  fetchPccsSupportedFormats,
  type IqAuditStatus,
} from '../../../../shared/security/useFirewallEnable';
import { ProtectionLevelSelector, type ProtectionLevel } from './ProtectionLevelSelector';
import { ConfirmDialog } from '../../../../shared/form/ConfirmDialog';
import { useRepositoriesApi } from './useRepositoriesApi';
import { restClient, ENDPOINTS } from '../../../../../../interface/api';
import { useToast } from '../../../../shared';
import { ExtJS } from '../../../../../../interface/ExtJS';
import Permissions from '../../../../../../constants/Permissions';
import UIStrings from '../../../../../../constants/pages/admin/repository/RepositoriesStrings';

export interface RepositoryFirewallConfigTabProps {
  repositoryName: string;
  /** When false, show cross-sell instead of Enable buttons */
  hasFirewallLicense?: boolean;
  /** Called after successful enable (parent can refresh) */
  onEnableSuccess?: () => void;
  /** Show the Firewall section (default true) */
  showFirewall?: boolean;
  /** Show the Health Check section (default true) */
  showHealthCheck?: boolean;
  /**
   * Repository format (e.g. 'maven2', 'npm', 'pypi'). Used to decide whether to offer the
   * PCCS protection level. Optional for backward compatibility — when omitted, PCCS is hidden
   * (matching the pre-PCCS behaviour). Future callers should always pass this through.
   */
  format?: string;
  /**
   * When rendered inside the Repository edit form, the parent form's top-level Save Changes /
   * Cancel buttons drive the pending protection-level change instead of the selector committing
   * it directly on select (FIRE-958). Mirrors the RepositoryEvaluationTab contract. Only pass
   * these from the mount that actually shows the selector (showFirewall=true) — this component
   * is also mounted with showFirewall=false for the Health Check tab, and that sibling mount
   * must not receive them or it will falsely report a dirty firewall selection.
   */
  onDirtyChange?: (dirty: boolean) => void;
  onSavingChange?: (saving: boolean) => void;
  /**
   * The exposed save handler accepts an optional `onCommitted` callback, invoked only once the
   * pending level has actually been applied (after any None/Quarantine confirmation), or right
   * away when nothing is pending. The parent uses it to save its own pending edits afterwards.
   */
  onSaveRef?: React.MutableRefObject<((onCommitted?: () => void) => Promise<void>) | null>;
  onCancelRef?: React.MutableRefObject<(() => void) | null>;
  /**
   * When true (XET is enabled for this HuggingFace repository), Firewall must remain disabled.
   * Shows a warning and disables the protection-level selector.
   */
  xetEnabled?: boolean;
}

interface HealthCheckStatus {
  enabled?: boolean;
  analyzing?: boolean;
}

interface HealthCheckListEntry extends HealthCheckStatus {
  repositoryName: string;
}

/**
 * Caller responsibility: only mount this component when isFirewallSupportedFormat(format)
 * is true for proxy repositories — there is no internal format guard.
 * See NEXUS-54536.
 */

export function RepositoryFirewallConfigTab({
  repositoryName,
  hasFirewallLicense = true,
  onEnableSuccess,
  showFirewall = true,
  showHealthCheck = true,
  format,
  onDirtyChange,
  onSavingChange,
  onSaveRef,
  onCancelRef,
  xetEnabled,
}: RepositoryFirewallConfigTabProps): JSX.Element {
  const [status, setStatus] = useState<IqAuditStatus | null>(null);
  const [loadingStatus, setLoadingStatus] = useState(true);
  // True only when fetchIqAuditStatus actually failed (not when it was skipped because
  // hasFirewallLicense is false). Distinguishes "we don't know the real mode" from "there is no
  // mode" so the selector doesn't silently present None as a confirmed value when it's really
  // just the fallback for a status we couldn't load.
  const [statusError, setStatusError] = useState(false);
  const [healthCheck, setHealthCheck] = useState<HealthCheckStatus | null>(null);
  const [healthCheckLoading, setHealthCheckLoading] = useState(true);
  const [pccsSupported, setPccsSupported] = useState(false);
  // Selection made in the dropdown but not yet committed — null means "no pending change,
  // display the server-reported mode". Selecting a level only sets this; it does not call the
  // server (FIRE-958). Save Changes commits it (see handleSave below); Cancel discards it.
  const [pendingLevel, setPendingLevel] = useState<ProtectionLevel | null>(null);
  const [confirmDialog, setConfirmDialog] = useState<'none' | 'quarantine' | null>(null);
  // onCommitted callback passed to handleSave, held while a confirmation dialog is open.
  const afterCommitRef = useRef<(() => void) | null>(null);
  const { enableAudit, enableQuarantine, enablePccs, disable, loading, error } =
    useFirewallEnable(repositoryName, xetEnabled);
  const { enableHealthCheck, disableHealthCheck } = useRepositoriesApi();
  const toast = useToast();

  // Hide health-check enable/disable actions for users without healthcheck:update (NEXUS-54212).
  // Use the provider-independent ExtJS.usePermission: the context-based usePermission returns
  // false without a <PermissionsProvider> ancestor, which coreui (self-hosted) never mounts, so
  // the actions would be hidden even from admins. Depend on hasUser so the check re-evaluates
  // once the user and their permissions load (they arrive asynchronously after mount).
  const hasUser = ExtJS.useUser() ?? false;
  const canUpdateHealthCheck = ExtJS.usePermission(
    () => ExtJS.checkPermission(Permissions.HEALTHCHECK.UPDATE),
    [hasUser],
  );
  // Changing the protection level calls disable()/enableAudit()/enableQuarantine()/enablePccs(),
  // all of which mutate firewall config, so gate the selector on repository-admin edit — mirroring
  // the sibling FirewallCard. Without it a repository-admin:read user could pick a mode from the
  // Preview UI and only hit a backend 403, the exact behaviour NEXUS-54212 removes.
  const canEditFirewall = ExtJS.usePermission(
    () => ExtJS.checkPermission(Permissions.REPOSITORY_ADMIN.EDIT),
    [hasUser],
  );

  const fetchStatus = useCallback(async () => {
    if (!hasFirewallLicense) {
      setStatus(null);
      setStatusError(false);
      setLoadingStatus(false);
      return;
    }
    setLoadingStatus(true);
    try {
      // fetchIqAuditStatus catches its own errors and resolves null rather than rejecting, so
      // a null result (not a thrown exception) is the real failure signal here.
      const data = await fetchIqAuditStatus(repositoryName);
      setStatus(data);
      setStatusError(data === null);
    } catch {
      setStatus(null);
      setStatusError(true);
    } finally {
      setLoadingStatus(false);
    }
  }, [repositoryName, hasFirewallLicense]);

  useEffect(() => {
    fetchStatus();
  }, [fetchStatus]);

  // Resolve whether this repository's format supports PCCS. We do not gate on
  // `hasFirewallLicense` here — the format-capabilities query is a cheap server lookup that
  // is independent of license state, and gating PCCS visibility on format alone matches the
  // legacy ExtJS form behaviour.
  useEffect(() => {
    if (!format) {
      setPccsSupported(false);
      return;
    }
    let cancelled = false;
    fetchPccsSupportedFormats().then((formats) => {
      if (!cancelled) {
        setPccsSupported(formats.includes(format));
      }
    });
    return () => {
      cancelled = true;
    };
  }, [format]);

  useEffect(() => {
    if (!showHealthCheck) {
      setHealthCheck(null);
      setHealthCheckLoading(false);
      return;
    }
    let cancelled = false;
    setHealthCheckLoading(true);
    restClient
      .get<HealthCheckListEntry[]>(ENDPOINTS.HEALTH_CHECK)
      .then((data) => {
        if (cancelled) return;
        const entry = Array.isArray(data)
          ? data.find((r) => r.repositoryName === repositoryName)
          : undefined;
        setHealthCheck(entry ?? null);
      })
      .catch(() => {
        if (!cancelled) setHealthCheck(null);
      })
      .finally(() => {
        if (!cancelled) setHealthCheckLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [repositoryName, showHealthCheck]);

  // Map the 4-mode firewall state down to the protection level the selector understands.
  // We deliberately read `status.mode` (post-migration full state) when available and only
  // fall back to the boolean view for legacy callers — otherwise QUARANTINE and PCCS would
  // both render as 'quarantine'.
  const protectionLevel: ProtectionLevel =
    status?.mode === 'PCCS'
      ? 'pccs'
      : status?.mode === 'QUARANTINE'
        ? 'quarantine'
        : status?.mode === 'AUDIT'
          ? 'audit'
          : status?.enabled && status?.enabledQuarantine
            ? 'quarantine'
            : status?.enabled
              ? 'audit'
              : 'none';

  // Displayed value: a pending (unsaved) selection takes precedence over the server-reported
  // mode. The selector's onChange only reaches here — it never calls the server directly
  // (FIRE-958: the previous button group committed on click, bypassing Save/Cancel).
  // When XET is enabled (possibly as an unsaved edit on the Settings tab), Firewall is forced off
  // — saving XET sends firewall.mode=DISABLED — so show None rather than the stale server mode.
  const displayedLevel = xetEnabled ? 'none' : (pendingLevel ?? protectionLevel);
  // Guard against the dual-mount hazard documented on onDirtyChange above: the selector (and its
  // dirty state) only exists when showFirewall is true, so a caller that mistakenly wires
  // onDirtyChange/onSaveRef into the showFirewall=false (Health Check) mount can't falsely report
  // a dirty firewall selection.
  const isDirty =
    showFirewall && !xetEnabled && pendingLevel !== null && pendingLevel !== protectionLevel;

  // A level staged before XET was turned on can no longer be applied; drop it.
  useEffect(() => {
    if (xetEnabled) setPendingLevel(null);
  }, [xetEnabled]);

  useEffect(() => {
    onDirtyChange?.(isDirty);
  }, [isDirty, onDirtyChange]);

  useEffect(() => {
    onSavingChange?.(loading);
  }, [loading, onSavingChange]);

  // Resolves true when the level was applied, false when the server call failed.
  const commitLevel = useCallback(
    async (level: ProtectionLevel): Promise<boolean> => {
      // Optimistically update the local status so the selector reflects the committed level
      // before the next fetch round-trips. The server-side mode is the source of truth on
      // subsequent reads.
      const applyOptimistic = (mode: 'DISABLED' | 'AUDIT' | 'QUARANTINE' | 'PCCS') => {
        setStatus((prev) => ({
          repositoryName: prev?.repositoryName ?? repositoryName,
          enabled: mode !== 'DISABLED',
          enabledQuarantine: mode === 'QUARANTINE' || mode === 'PCCS',
          mode,
        }));
        onEnableSuccess?.();
      };
      try {
        if (level === 'none') await disable(() => applyOptimistic('DISABLED'));
        else if (level === 'audit') await enableAudit(() => applyOptimistic('AUDIT'));
        else if (level === 'quarantine') await enableQuarantine(() => applyOptimistic('QUARANTINE'));
        else await enablePccs(() => applyOptimistic('PCCS'));
        setPendingLevel(null);
        return true;
      } catch {
        // Error shown inline via useFirewallEnable error state. Leave pendingLevel set so
        // Save Changes can be retried without the user having to re-select the level.
        return false;
      }
    },
    [disable, enableAudit, enableQuarantine, enablePccs, repositoryName, onEnableSuccess],
  );

  // Selecting a level in the dropdown only records the pending choice; it never commits.
  const handleSelectorChange = useCallback((level: ProtectionLevel) => {
    setPendingLevel(level);
  }, []);

  // Exposed to the parent form's Save Changes button via onSaveRef. Going to None or
  // Quarantine prompts for confirmation first (a single misclick must not change enforcement);
  // Audit and PCCS commit immediately since neither disables nor newly blocks traffic.
  // onCommitted runs only after the level is actually applied, so a cancelled confirmation or a
  // failed commit never lets the parent go on to save its own edits.
  const handleSave = useCallback(async (onCommitted?: () => void) => {
    if (pendingLevel === null || pendingLevel === protectionLevel) {
      onCommitted?.();
      return;
    }
    if (pendingLevel === 'none' || pendingLevel === 'quarantine') {
      afterCommitRef.current = onCommitted ?? null;
      setConfirmDialog(pendingLevel);
      return;
    }
    if (await commitLevel(pendingLevel)) onCommitted?.();
  }, [pendingLevel, protectionLevel, commitLevel]);

  // Exposed to the parent form's Cancel button via onCancelRef. Pure discard — no server call.
  const handleCancel = useCallback(() => {
    setPendingLevel(null);
  }, []);

  // Runs before the dialog's onOpenChange(false), so the callback is captured before it is cleared.
  const handleConfirmedCommit = useCallback(() => {
    const onCommitted = afterCommitRef.current;
    afterCommitRef.current = null;
    if (pendingLevel) {
      void commitLevel(pendingLevel).then((committed) => {
        if (committed) onCommitted?.();
      });
    }
  }, [pendingLevel, commitLevel]);

  const handleConfirmOpenChange = useCallback((dialog: 'none' | 'quarantine', open: boolean) => {
    if (!open) afterCommitRef.current = null;
    setConfirmDialog(open ? dialog : null);
  }, []);

  useEffect(() => {
    if (onSaveRef) onSaveRef.current = handleSave;
    return () => {
      if (onSaveRef && onSaveRef.current === handleSave) onSaveRef.current = null;
    };
  }, [handleSave, onSaveRef]);

  useEffect(() => {
    if (onCancelRef) onCancelRef.current = handleCancel;
    return () => {
      if (onCancelRef && onCancelRef.current === handleCancel) onCancelRef.current = null;
    };
  }, [handleCancel, onCancelRef]);

  const handleEnableHealthCheck = async () => {
    try {
      await enableHealthCheck(repositoryName);
      setHealthCheck((p) => (p ? { ...p, enabled: true, analyzing: false } : { enabled: true, analyzing: false }));
      onEnableSuccess?.();
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to enable Health Check';
      toast.warning(`Health Check could not be enabled for "${repositoryName}": ${message}`);
    }
  };

  const handleDisableHealthCheck = async () => {
    try {
      await disableHealthCheck(repositoryName);
      setHealthCheck((p) => (p ? { ...p, enabled: false, analyzing: false } : { enabled: false }));
      onEnableSuccess?.();
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to disable Health Check';
      toast.error(`Health Check could not be disabled for "${repositoryName}": ${message}`);
    }
  };

  if (loadingStatus) {
    return (
      <Box p="4">
        <Text size="2" color="gray">
          Loading Firewall status…
        </Text>
      </Box>
    );
  }

  return (
    <Box p="4" className="repository-firewall-config-tab">
      {showFirewall && (
        <Card size="2">
          <Flex direction="column" gap="4">
            <Box>
              <Text size="4" weight="bold" as="div" mb="1">
                Sonatype Repository Firewall
              </Text>
              <Text size="2" color="gray" as="div">
                Audit and automatically quarantine components with policy violations before they reach your developers
                and builds.
              </Text>
            </Box>

            {error && (
              <Text size="2" color="red">
                {error}
              </Text>
            )}

            {hasFirewallLicense && xetEnabled && (
              <Callout.Root color="yellow" mb="3">
                <Callout.Icon>
                  <AlertCircle size={16} />
                </Callout.Icon>
                <Callout.Text>
                  {UIStrings.HUGGINGFACE.FIREWALL_STEP_XET_DISABLED_WARNING}
                </Callout.Text>
              </Callout.Root>
            )}

            {hasFirewallLicense ? (
              canEditFirewall && (
                <Box>
                  {statusError && (
                    <Text size="2" color="red" as="div" mb="2">
                      Could not load the current Firewall mode. Showing None, which may not reflect
                      the actual setting — refresh before making a change.
                    </Text>
                  )}
                  <Text size="2" weight="medium" mb="2" as="p" id="protection-level-label">
                    Protection level
                  </Text>
                  <ProtectionLevelSelector
                    value={displayedLevel}
                    onChange={handleSelectorChange}
                    disabled={loading || statusError || xetEnabled}
                    size="2"
                    pccsSupported={pccsSupported}
                    aria-labelledby="protection-level-label"
                  />
                  {isDirty && (
                    <Text size="1" color="orange" mt="2" as="div">
                      Unsaved change — click Save Changes to apply, or Cancel to discard.
                    </Text>
                  )}
                </Box>
              )
            ) : (
              <Flex direction="column" gap="3">
                <Text size="2" color="gray">
                  Protect this repository with Repository Firewall. Requires a Firewall license.
                </Text>
                <Flex gap="2" wrap="wrap">
                  <Button variant="ghost" size="2" asChild>
                    <a
                      href="https://links.sonatype.com/nexus-repository-firewall"
                      target="_blank"
                      rel="noopener noreferrer"
                    >
                      <ExternalLink size={14} />
                      Learn more
                    </a>
                  </Button>
                  <Button variant="ghost" size="2" asChild>
                    <a href="https://links.sonatype.com/contact-sales" target="_blank" rel="noopener noreferrer">
                      Contact sales
                    </a>
                  </Button>
                </Flex>
              </Flex>
            )}
          </Flex>
        </Card>
      )}

      {showFirewall && showHealthCheck && <Separator size="4" my="4" />}

      {showHealthCheck && (
        <Card size="2">
          <Flex direction="column" gap="4">
            <Flex align="center" gap="3">
              <ShieldCheck size={24} color="var(--blue-9)" aria-hidden />
              <Box>
                <Text size="4" weight="bold" as="div" mb="1">
                  Repository Health Check
                </Text>
                <Text size="2" color="gray" as="div">
                  Repository Health Check analyzes components for security vulnerabilities and license issues.
                </Text>
              </Box>
            </Flex>

            {healthCheckLoading ? (
              <Text size="2" color="gray">
                Loading…
              </Text>
            ) : healthCheck?.enabled ? (
              <Flex direction="column" gap="3">
                <Flex align="center" gap="2">
                  <Box
                    style={{
                      width: 8, height: 8, borderRadius: '50%',
                      backgroundColor: healthCheck.analyzing ? 'var(--orange-9)' : 'var(--green-9)',
                    }}
                  />
                  <Text size="2" weight="medium" color={healthCheck.analyzing ? 'orange' : 'green'}>
                    {healthCheck.analyzing ? 'Analyzing…' : 'Enabled'}
                  </Text>
                </Flex>
                {canUpdateHealthCheck && (
                  <Flex gap="2">
                    <Button
                      type="button"
                      variant="soft"
                      color="red"
                      size="2"
                      onClick={handleDisableHealthCheck}
                    >
                      Disable Health Check
                    </Button>
                  </Flex>
                )}
              </Flex>
            ) : (
              <Flex direction="column" gap="3">
                <Flex align="center" gap="2">
                  <Box
                    style={{
                      width: 8, height: 8, borderRadius: '50%',
                      backgroundColor: 'var(--gray-8)',
                    }}
                  />
                  <Text size="2" weight="medium" color="gray">
                    Disabled
                  </Text>
                </Flex>
                {canUpdateHealthCheck && (
                  <Flex gap="2">
                    <Button
                      type="button"
                      variant="solid"
                      color="blue"
                      size="2"
                      onClick={handleEnableHealthCheck}
                    >
                      Enable Health Check
                    </Button>
                  </Flex>
                )}
              </Flex>
            )}
          </Flex>
        </Card>
      )}

      <ConfirmDialog
        open={confirmDialog === 'none'}
        onOpenChange={(open) => handleConfirmOpenChange('none', open)}
        title="Disable Firewall Protection"
        message="This will disable Firewall on this repository. Malicious components may enter unchecked once you save. This takes effect only after you click Save Changes."
        confirmLabel="Disable on Save"
        cancelLabel="Keep Current Selection"
        variant="warning"
        onConfirm={handleConfirmedCommit}
        loading={loading}
        testId="firewall-disable-confirm"
      />

      <ConfirmDialog
        open={confirmDialog === 'quarantine'}
        onOpenChange={(open) => handleConfirmOpenChange('quarantine', open)}
        title="Enable Quarantine Mode"
        message="Quarantine mode will block policy-violating and malicious components from this proxy once you save. This takes effect only after you click Save Changes."
        confirmLabel="Enable on Save"
        cancelLabel="Keep Current Selection"
        variant="warning"
        onConfirm={handleConfirmedCommit}
        loading={loading}
        testId="firewall-quarantine-confirm"
      />
    </Box>
  );
}

export default RepositoryFirewallConfigTab;
