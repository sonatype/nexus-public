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

import React, { useState, useCallback, useEffect } from 'react';
import { Box, Flex, IconButton, Tooltip, Button, Spinner } from '@radix-ui/themes';
import { Maximize2, Minimize2, Shield, ShieldOff, AlertCircle } from 'lucide-react';
import { SecurityReportPage } from '../../../../../shared/security/SecurityReportPage';
import { useFirewallEnable } from '../../../../../shared/security/useFirewallEnable';
import { FirewallNotSupportedEmptyState } from '../../../../../shared/FirewallNotSupportedEmptyState';
import { isFirewallSupportedFormat } from '../../../../../../../utils/firewallFormats';
import { useIqConnectionStatus } from '../../../../../shared/security/useIqConnectionStatus';
import { ExtJS } from '../../../../../../../interface/ExtJS';
import Permissions from '../../../../../../../constants/Permissions';

import './FirewallReportTab.scss';

export interface FirewallReportTabProps {
  repositoryName: string;
  /** Repository format — used to show N/A empty state for unsupported formats */
  repositoryFormat?: string;
  /** When false and hasFirewallLicense, show Enable Audit/Quarantine buttons */
  firewallEnabled?: boolean;
  /** Whether user has Firewall license */
  hasFirewallLicense?: boolean;
  /** Called after successful enable (parent refreshes) */
  onEnableSuccess?: () => void;
}

/**
 * FirewallReportTab - Full Firewall report in a tab, with fullscreen toggle.
 * When firewall not enabled and licensed, shows Enable Audit / Enable Quarantine.
 * When repositoryFormat is unsupported by Firewall, shows an informational empty state.
 * When no Firewall license at all, shows upgrade prompt.
 *
 * Connection awareness (BDD Scenario 9):
 * - When IQ Server is configured but unreachable, shows error with link to settings
 * - Does not show stale data or fail silently
 */
export function FirewallReportTab({
  repositoryName,
  repositoryFormat,
  firewallEnabled = false,
  hasFirewallLicense = true,
  onEnableSuccess,
}: FirewallReportTabProps): JSX.Element {
  const [isFullscreen, setIsFullscreen] = useState(false);
  const { enableAudit, enableQuarantine, loading: enableLoading } = useFirewallEnable(repositoryName);

  // Hide firewall enable actions for users without repository-admin edit (NEXUS-54212).
  // coreui never mounts a <PermissionsProvider>, so context usePermission returns false for
  // everyone; use the provider-independent ExtJS.usePermission.
  const hasUser = ExtJS.useUser() ?? false;
  const canEditFirewall = ExtJS.usePermission(
    () => ExtJS.checkPermission(Permissions.REPOSITORY_ADMIN.EDIT),
    [hasUser],
  );

  // Connection status for IQ Server awareness
  const iqConnection = useIqConnectionStatus();

  const toggleFullscreen = useCallback(() => {
    setIsFullscreen((prev) => !prev);
  }, []);

  useEffect(() => {
    if (isFullscreen) {
      window.dispatchEvent(new CustomEvent('nx-sidebar-toggle', { detail: { open: true } }));
    } else {
      window.dispatchEvent(new CustomEvent('nx-sidebar-toggle', { detail: { open: false } }));
    }
  }, [isFullscreen]);

  // Show connection testing state
  if (iqConnection.isConfigured && iqConnection.isTesting) {
    return (
      <Box data-testid="firewall-report-tab-testing" p="6">
        <Flex direction="column" align="center" gap="4" style={{ textAlign: 'center', maxWidth: 480, margin: '0 auto' }}>
          <Spinner size="3" />
          <Box>
            <h3 style={{ margin: '0 0 8px 0', fontSize: '18px', fontWeight: 600 }}>Testing IQ Server Connection...</h3>
          </Box>
        </Flex>
      </Box>
    );
  }

  // Show disconnected state (BDD Scenario 9)
  if (iqConnection.isConfigured && iqConnection.state === 'disconnected') {
    return (
      <Box data-testid="firewall-report-tab-disconnected" p="6">
        <Flex direction="column" align="center" gap="4" style={{ textAlign: 'center', maxWidth: 480, margin: '0 auto' }}>
          <ShieldOff size={48} color="var(--red-9)" />
          <Box>
            <Flex align="center" gap="2" justify="center" mb="2">
              <AlertCircle size={18} color="var(--red-9)" />
              <h3 style={{ margin: 0, fontSize: '18px', fontWeight: 600, color: 'var(--red-11)' }}>
                IQ Server Connection Unavailable
              </h3>
            </Flex>
            <p style={{ margin: 0, color: 'var(--gray-11)', fontSize: '14px' }}>
              {iqConnection.errorMessage || 'Unable to reach IQ Server. Check configuration.'}
            </p>
          </Box>
          <Button
            variant="soft"
            size="2"
            onClick={() => { window.location.hash = '#preview/admin/iq'; }}
          >
            Go to IQ Server Settings
          </Button>
        </Flex>
      </Box>
    );
  }

  // No Firewall license: show upgrade prompt instead of loading forever
  if (!hasFirewallLicense) {
    return (
      <Box data-testid="firewall-report-tab-no-license" p="6">
        <Flex direction="column" align="center" gap="4" style={{ textAlign: 'center', maxWidth: 480, margin: '0 auto' }}>
          <Shield size={48} color="var(--gray-8)" />
          <Box>
            <h3 style={{ margin: '0 0 8px 0', fontSize: '18px', fontWeight: 600 }}>Repository Firewall Not Available</h3>
            <p style={{ margin: 0, color: 'var(--gray-11)', fontSize: '14px' }}>
              Repository Firewall protects your repositories from malicious open source components.
              Upgrade to Sonatype Repository Enterprise to enable Firewall protection.
            </p>
          </Box>
          <Button
            variant="soft"
            size="2"
            onClick={() => window.open('https://links.sonatype.com/nexus-repository-firewall', '_blank')}
          >
            Learn More
          </Button>
        </Flex>
      </Box>
    );
  }

  // Unsupported format: replace the tab content entirely with the empty state
  if (repositoryFormat && !isFirewallSupportedFormat(repositoryFormat)) {
    return (
      <Box data-testid="firewall-report-tab-unsupported">
        <FirewallNotSupportedEmptyState format={repositoryFormat} context="tab" />
      </Box>
    );
  }

  const showEnableButtons = !firewallEnabled && hasFirewallLicense;

  const handleEnableAudit = async () => {
    try {
      await enableAudit(onEnableSuccess);
    } catch {
      // Error handled by hook
    }
  };

  const handleEnableQuarantine = async () => {
    try {
      await enableQuarantine(onEnableSuccess);
    } catch {
      // Error handled by hook
    }
  };

  return (
    <Box
      className={`firewall-report-tab-container ${isFullscreen ? 'firewall-report-tab-container--fullscreen' : ''}`}
    >
      <Flex gap="3" align="center" className="firewall-report-tab-container__toolbar">
        {showEnableButtons && canEditFirewall ? (
          <>
            <Button variant="soft" size="2" onClick={handleEnableAudit} disabled={enableLoading}>
              Enable Audit
            </Button>
            <Button variant="soft" color="orange" size="2" onClick={handleEnableQuarantine} disabled={enableLoading}>
              Enable Quarantine
            </Button>
          </>
        ) : null}
        <Tooltip content={isFullscreen ? 'Exit Fullscreen' : 'Fullscreen Mode'}>
          <IconButton variant="soft" size="2" onClick={toggleFullscreen} aria-label={isFullscreen ? 'Exit fullscreen' : 'Enter fullscreen'}>
            {isFullscreen ? <Minimize2 size={16} /> : <Maximize2 size={16} />}
          </IconButton>
        </Tooltip>
      </Flex>
      <Box className="firewall-report-tab-container__content">
        <SecurityReportPage repositoryName={repositoryName} reportType="firewall" embedded />
      </Box>
    </Box>
  );
}

export default FirewallReportTab;
