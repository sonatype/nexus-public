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

import React, { useState } from 'react';
import { Flex, Text, Badge, Tooltip } from '@radix-ui/themes';
import { Shield, ShieldAlert } from 'lucide-react';
import { SecurityStatusData, SecurityRepositoryInfo } from './security.types';
import { SecuritySummaryModal } from './SecuritySummaryModal';
import { FirewallNotSupportedEmptyState } from '../FirewallNotSupportedEmptyState';
import { isFirewallSupportedFormat } from '../../../../utils/firewallFormats';
import { ExtJS } from '../../../../interface/ExtJS';
import Permissions from '../../../../constants/Permissions';

import { classifyFirewallStatus } from './firewallStatusKind';
import FirewallStrings from './FirewallStrings';

import './FirewallCell.scss';

const { cell: CELL_STRINGS } = FirewallStrings.FIREWALL;

export interface ProxyProtectionSummary {
  totalProxy: number;
  protectedProxy: number;
}

export interface FirewallCellProps {
  /** Repository data */
  repository: SecurityRepositoryInfo;
  /** Firewall status from API */
  firewallStatus?: SecurityStatusData;
  /** Whether the firewall summary API has finished loading */
  firewallLoaded?: boolean;
  /**
   * Whether the summary loaded successfully. Only then does a missing row mean "firewall not
   * enabled"; after a failure or without read permission the state is unknown, not Unprotected.
   */
  firewallSummaryReady?: boolean;
  /** Per-repository violation counts, fetched by the list ViewModel */
  firewallCounts?: SecurityStatusData;
  /** The counts request for this repository is still in flight or queued */
  firewallCountsLoading?: boolean;
  /** Whether user has Firewall license (shows Enable buttons when unprotected) */
  hasFirewallLicense?: boolean;
  /** Called after successful enable (parent can refresh) */
  onEnableSuccess?: () => void;
  /** Summary of proxy protection status (for N/A modal context) */
  proxyProtectionSummary?: ProxyProtectionSummary;
}

/**
 * FirewallCell displays the firewall protection status for a repository.
 * Click cell → center modal. "View full report" navigates to repo profile Firewall tab.
 */
export function FirewallCell({
  repository,
  firewallStatus,
  firewallLoaded = false,
  firewallSummaryReady = firewallLoaded,
  firewallCounts,
  firewallCountsLoading = false,
  hasFirewallLicense = true,
  onEnableSuccess,
}: FirewallCellProps): JSX.Element {
  // Enable mutates firewall config, so gate it on repository-admin edit (NEXUS-54499/NEXUS-54212).
  const canEditFirewall = ExtJS.usePermission(
    () => ExtJS.checkPermission(Permissions.REPOSITORY_ADMIN.EDIT),
    [],
  );
  const [showModal, setShowModal] = useState(false);

  const summaryMessage = firewallStatus?.message ?? undefined;
  // Overlay the counts fetched by the list ViewModel onto the summary status for inline chiclets.
  const statusWithCounts = firewallCounts
    ? { ...firewallStatus, ...firewallCounts, message: summaryMessage }
    : firewallStatus;

  const modalData = firewallCounts ?? firewallStatus ?? {
    repositoryName: repository.name,
    affectedComponentCount: 0,
    criticalComponentCount: 0,
    severeComponentCount: 0,
    moderateComponentCount: 0,
    quarantinedComponentCount: 0,
  };

  // Unsupported proxy formats (e.g. terraform, helm, apt, gitlfs, swift) show N/A
  // This must come BEFORE the type check so proxy repos with unsupported formats
  // never fall through to the Unprotected/Protected paths.
  //
  // FIRE-956, deliberate: this stays on the visibility gate (isFirewallSupportedFormat) so the cell
  // reports the repository's real Firewall mode. Using the capability check instead would render
  // "Not supported" over a Helm proxy that is actively quarantining downloads under the override,
  // which is a worse inaccuracy than showing the mode it is running in. The proper fix is FIRE-895's
  // "Component Unknown policy support only" disclosure, which can state the reduced capability
  // without contradicting that mode. Do not "correct" this to the capability check on its own.
  if (repository.type === 'proxy' && !isFirewallSupportedFormat(repository.format)) {
    return (
      <FirewallNotSupportedEmptyState
        format={repository.format}
        context="cell"
      />
    );
  }

  if (repository.type !== 'proxy') {
    return (
      <Flex
        align="center"
        gap="1"
        className="firewall-cell"
        aria-label="Firewall: N/A for non-proxy repositories"
      >
        <Text size="1" color="gray">N/A</Text>
      </Flex>
    );
  }

  const kind = classifyFirewallStatus({ status: firewallStatus, loaded: firewallLoaded, summaryReady: firewallSummaryReady });
  const stillLoading = kind === 'loading';
  const hasError = kind === 'error';
  const isUnprotected = kind === 'unprotected';
  const isUnknown = kind === 'unknown';
  const isInactive = kind === 'inactive';
  const isAudit = kind === 'audit';
  // An enabled repository with no counts and none on the way must never read as a clean report,
  // whether the request failed or no caller supplies counts at all.
  const countsUnavailable = (kind === 'quarantine' || kind === 'audit') && !firewallCounts && !firewallCountsLoading;
  const openModal = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (stillLoading) return;
    setShowModal(true);
  };

  return (
    <>
      <Flex
        align="center"
        gap="1"
        className={`firewall-cell ${hasError ? 'firewall-cell--error' : ''} ${stillLoading ? 'firewall-cell--loading' : 'firewall-cell--clickable'}`}
        onClick={openModal}
        role="button"
        tabIndex={stillLoading ? -1 : 0}
        onKeyDown={(e) => (e.key === 'Enter' || e.key === ' ') && openModal(e as unknown as React.MouseEvent)}
        aria-label={
          stillLoading ? 'Firewall: Loading status...'
            : hasError || isUnknown ? 'Firewall: Unavailable. Click for details.'
            : isInactive ? 'Firewall: Inactive. Click for details.'
            : isUnprotected ? 'Firewall: Unprotected. Click for details.'
            : isAudit ? 'Firewall: Audited. Click for details.'
            : 'Firewall: Quarantine. Click for details.'
        }
      >
        {stillLoading ? (
          <Tooltip content="Loading firewall status...">
            <Flex align="center" gap="1">
              <Shield size={12} color="var(--gray-8)" />
              <Badge variant="soft" color="gray" size="1">Loading...</Badge>
            </Flex>
          </Tooltip>
        ) : hasError ? (
          <Tooltip content={firewallStatus?.errorMessage ?? 'IQ Server unreachable'}>
            <Flex align="center" gap="1">
              <ShieldAlert size={14} color="var(--amber-9)" />
              <Badge variant="soft" color="amber" size="1">Unavailable</Badge>
            </Flex>
          </Tooltip>
        ) : isUnknown ? (
          <Tooltip content={CELL_STRINGS.unknownTooltip}>
            <Flex align="center" gap="1">
              <ShieldAlert size={14} color="var(--gray-9)" />
              <Badge variant="soft" color="gray" size="1">{CELL_STRINGS.unknown}</Badge>
            </Flex>
          </Tooltip>
        ) : isInactive ? (
          <Tooltip content={CELL_STRINGS.inactiveTooltip}>
            <Badge variant="soft" color="gray" size="1">{CELL_STRINGS.inactive}</Badge>
          </Tooltip>
        ) : isUnprotected ? (
          <Tooltip content={hasFirewallLicense ? 'Click to configure Firewall protection' : 'Firewall license not available'}>
            <Badge variant="soft" color="red" size="1">Unprotected</Badge>
          </Tooltip>
        ) : isAudit ? (
          <Tooltip content="Audit mode - violations reported but not blocked">
            <Flex align="center" gap="1">
              <Badge variant="soft" color="amber" size="1">Audited</Badge>
              <ViolationChiclets status={statusWithCounts} />
            </Flex>
          </Tooltip>
        ) : (
          <Tooltip content="Quarantine mode - risky components blocked">
            <Flex align="center" gap="1">
              <Badge variant="soft" color="green" size="1">Quarantine</Badge>
              <ViolationChiclets status={statusWithCounts} />
            </Flex>
          </Tooltip>
        )}
      </Flex>
      {showModal && (
        <SecuritySummaryModal
          repositoryName={repository.name}
          repositoryFormat={repository.format}
          data={modalData}
          isOpen={showModal}
          onClose={() => setShowModal(false)}
          type="firewall"
          isFirewallReportDetailLoading={firewallCountsLoading && !firewallCounts}
          countsUnavailable={countsUnavailable}
          reportUrl={firewallCounts?.reportUrl ?? firewallStatus?.reportUrl}
          onViewFullReport={(firewallCounts?.reportUrl ?? firewallStatus?.reportUrl) ? () => {
            window.open(firewallCounts?.reportUrl ?? firewallStatus?.reportUrl, '_blank', 'noopener,noreferrer');
          } : undefined}
          canEditFirewall={canEditFirewall}
          firewallStatus={
            isUnprotected
              ? 'unprotected'
              : isInactive
                ? 'inactive'
                : isUnknown || firewallCounts?.errorMessage || firewallStatus?.errorMessage
                  ? 'unavailable'
                  : isAudit
                    ? 'audit'
                    : 'protected'
          }
          hasFirewallLicense={hasFirewallLicense}
          onEnableSuccess={onEnableSuccess}
        />
      )}
    </>
  );
}

/**
 * Inline violation counts for the firewall column, mirroring Classic's chiclets
 * (critical/severe/moderate + quarantined). Renders nothing until the per-repo
 * counts have loaded, so the summary-only first paint stays clean.
 */
function ViolationChiclets({ status }: { status?: SecurityStatusData }): JSX.Element | null {
  const critical = status?.criticalComponentCount ?? 0;
  const severe = status?.severeComponentCount ?? 0;
  const moderate = status?.moderateComponentCount ?? 0;
  const quarantined = status?.quarantinedComponentCount ?? 0;
  if (critical === 0 && severe === 0 && moderate === 0 && quarantined === 0) {
    return null;
  }
  return (
    <>
      {critical > 0 && <Badge variant="soft" color="red" size="1">{critical}</Badge>}
      {severe > 0 && <Badge variant="soft" color="orange" size="1">{severe}</Badge>}
      {moderate > 0 && <Badge variant="soft" color="yellow" size="1">{moderate}</Badge>}
      {quarantined > 0 && (
        <Badge variant="soft" color="amber" size="1" aria-label={`${quarantined} quarantined`}>
          Q{quarantined}
        </Badge>
      )}
    </>
  );
}

export default FirewallCell;
