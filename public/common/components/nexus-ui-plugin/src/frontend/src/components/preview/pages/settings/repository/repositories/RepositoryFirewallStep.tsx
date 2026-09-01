/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are
 * trademarks of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a
 * trademark of the Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

/**
 * Repository Creation wizard – Step 4 (proxy repos only).
 *
 * Enables Firewall (None / Audit / Quarantine / PCCS) if licensed, or shows cross-sell with
 * option to skip. PCCS is conditionally offered for npm/pypi proxies, gated by the
 * format-capabilities API ({@code GET /v1/repositories/firewall/format-capabilities}). Same
 * button treatment as Malware Defense with outline on selected.
 */
import React, { useEffect, useState } from 'react';
import { Box, Flex, Text, Card, Button, } from '@radix-ui/themes';
import { ExternalLink } from 'lucide-react';
import {
  fetchPccsSupportedFormats,
  useFirewallEnable,
} from '../../../../shared/security/useFirewallEnable';
import { ConfirmDialog } from '../../../../shared/form/ConfirmDialog';

export type ProtectionLevel = 'none' | 'audit' | 'quarantine' | 'pccs';

export interface RepositoryFirewallStepProps {
  /** Required for immediate mode; omit for deferred mode */
  repositoryName?: string;
  /** When false, show cross-sell (Learn more / Contact sales) instead of Enable buttons */
  hasFirewallLicense?: boolean;
  /**
   * Repository format. Determines whether PCCS is offered (npm/pypi). Optional for callers
   * that pre-date PCCS support; when omitted, PCCS is hidden.
   */
  format?: string;
  /** In immediate mode: called after API succeeds. In deferred: not used (parent advances via Next). */
  onComplete?: () => void;
  /** Deferred mode: store choice, no API calls. Parent advances via wizard Next. */
  mode?: 'immediate' | 'deferred';
  /** Deferred mode: current choice (controlled) */
  value?: ProtectionLevel;
  /** Deferred mode: when user selects a level */
  onChoice?: (level: ProtectionLevel) => void;
}

export function RepositoryFirewallStep({
  repositoryName = '',
  hasFirewallLicense = true,
  format,
  onComplete,
  mode = 'immediate',
  value = 'none',
  onChoice,
}: RepositoryFirewallStepProps): JSX.Element {
  const [auditModalOpen, setAuditModalOpen] = useState(false);
  const [noneModalOpen, setNoneModalOpen] = useState(false);
  const [pccsSupported, setPccsSupported] = useState(false);
  const { enableAudit, enableQuarantine, enablePccs, loading } = useFirewallEnable(repositoryName);
  const isDeferred = mode === 'deferred';

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

  const handleLevelChange = async (level: ProtectionLevel) => {
    if (level === 'none') {
      setNoneModalOpen(true);
      return;
    }
    if (level === 'audit') {
      setAuditModalOpen(true);
      return;
    }
    if (level === 'pccs') {
      // No confirm dialog: PCCS is the strongest mode, advancing to it is intentional.
      if (isDeferred) {
        onChoice?.('pccs');
        return;
      }
      try {
        await enablePccs(onComplete!);
      }
      catch {
        // Error shown inline
      }
      return;
    }
    if (isDeferred) {
      onChoice?.('quarantine');
      return;
    }
    try {
      await enableQuarantine(onComplete!);
    } catch {
      // Error shown inline
    }
  };

  const handleNoneConfirm = () => {
    setNoneModalOpen(false);
    if (isDeferred) {
      onChoice?.('none');
    } else {
      onComplete?.();
    }
  };

  const handleAuditConfirm = async () => {
    setAuditModalOpen(false);
    if (isDeferred) {
      onChoice?.('audit');
    } else {
      try {
        await enableAudit(onComplete!);
      } catch {
        // Error shown inline
      }
    }
  };

  const handleEnableQuarantineFromAuditModal = async () => {
    setAuditModalOpen(false);
    if (isDeferred) {
      onChoice?.('quarantine');
    } else {
      try {
        await enableQuarantine(onComplete!);
      } catch {
        // Error shown inline
      }
    }
  };

  return (
    <Box p="4" className="repository-firewall-step">
      <Card size="2">
        <Flex direction="column" gap="4">
          <Box>
            <Text size="4" weight="bold" as="div" mb="1">
              Enable Repository Firewall
            </Text>
            <Text size="2" color="gray" as="div">
              Protect this proxy repository by enabling Firewall to audit or block policy violations and malware.
            </Text>
          </Box>

          {hasFirewallLicense ? (
            <Box>
              {isDeferred && (
                <Text size="2" color="gray" mb="2" as="div">
                  {value === 'none'
                    ? 'Current choice: Skip (no Firewall)'
                    : value === 'audit'
                      ? 'Firewall will be enabled in Audit mode'
                      : value === 'pccs'
                        ? 'Firewall will be enabled in PCCS mode'
                        : 'Firewall will be enabled in Quarantine mode'}
                </Text>
              )}
              <Flex gap="3" wrap="wrap">
                <Button
                  type="button"
                  variant={isDeferred && value === 'none' ? 'solid' : 'soft'}
                  color="gray"
                  size="3"
                  onClick={() => handleLevelChange('none')}
                  disabled={!isDeferred && loading}
                  aria-pressed={isDeferred && value === 'none'}
                >
                  Skip
                </Button>
                <Button
                  type="button"
                  variant={isDeferred && value === 'audit' ? 'solid' : 'soft'}
                  color="amber"
                  size="3"
                  onClick={() => handleLevelChange('audit')}
                  disabled={!isDeferred && loading}
                  aria-pressed={isDeferred && value === 'audit'}
                >
                  Audit
                </Button>
                <Button
                  type="button"
                  variant={isDeferred && value === 'quarantine' ? 'solid' : 'soft'}
                  color="green"
                  size="3"
                  onClick={() => handleLevelChange('quarantine')}
                  disabled={!isDeferred && loading}
                  aria-pressed={isDeferred && value === 'quarantine'}
                >
                  Quarantine
                </Button>
                {pccsSupported && (
                  <Button
                    type="button"
                    variant={isDeferred && value === 'pccs' ? 'solid' : 'soft'}
                    color="orange"
                    size="3"
                    onClick={() => handleLevelChange('pccs')}
                    disabled={!isDeferred && loading}
                    aria-pressed={isDeferred && value === 'pccs'}
                  >
                    PCCS
                  </Button>
                )}
              </Flex>
            </Box>
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
                <Button type="button" variant="soft" size="2" onClick={() => (isDeferred ? onChoice?.('none') : onComplete?.())}>
                  None
                </Button>
              </Flex>
            </Flex>
          )}
        </Flex>
      </Card>

      <ConfirmDialog
        open={noneModalOpen}
        onOpenChange={setNoneModalOpen}
        title="Repository Created Without Protection"
        message="This repository will not be protected by Firewall. Malicious components may enter unchecked. Enable Firewall later from the repository's Firewall tab if needed."
        confirmLabel="Continue Anyway"
        cancelLabel="Go Back"
        variant="warning"
        onConfirm={handleNoneConfirm}
        testId="firewall-skip-confirm"
      />

      <ConfirmDialog
        open={auditModalOpen}
        onOpenChange={setAuditModalOpen}
        title="Audit Mode – Not Fully Protected"
        message="Audit mode monitors components but does not block them. Malicious components can still enter this repository. Enable Quarantine for full protection."
        confirmLabel="Enable Audit Anyway"
        cancelLabel="Cancel"
        variant="warning"
        onConfirm={handleAuditConfirm}
        testId="firewall-audit-confirm"
      >
        <Flex gap="2" mt="3" wrap="wrap">
          <Button
            type="button"
            variant="soft"
            color="orange"
            size="2"
            onClick={handleEnableQuarantineFromAuditModal}
            disabled={loading}
          >
            Enable Quarantine Instead
          </Button>
        </Flex>
      </ConfirmDialog>
    </Box>
  );
}

export default RepositoryFirewallStep;
