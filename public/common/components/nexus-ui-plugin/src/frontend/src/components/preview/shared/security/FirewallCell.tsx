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

import React, { useState, useEffect } from 'react';
import { Flex, Text, Badge, Tooltip } from '@radix-ui/themes';
import { Shield, ShieldAlert } from 'lucide-react';
import { useRouter } from '@uirouter/react';
import { SecurityStatusData, SecurityRepositoryInfo } from './security.types';
import { SecuritySummaryModal } from './SecuritySummaryModal';
import { FirewallNotSupportedEmptyState } from '../FirewallNotSupportedEmptyState';
import { isFirewallSupportedFormat } from '../../../../utils/firewallFormats';
import { restClient, ENDPOINTS } from '../../../../interface/api';

import './FirewallCell.scss';

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
  hasFirewallLicense = true,
  onEnableSuccess,
}: FirewallCellProps): JSX.Element {
  let router: ReturnType<typeof useRouter> | null = null;
  try { router = useRouter(); } catch { /* no UIRouter context */ }
  const [showModal, setShowModal] = useState(false);
  const [repoDetail, setRepoDetail] = useState<SecurityStatusData | null>(null);
  const [repoDetailLoading, setRepoDetailLoading] = useState(false);

  useEffect(() => {
    if (!showModal) {
      setRepoDetail(null);
      setRepoDetailLoading(false);
      return;
    }
    let cancelled = false;
    setRepoDetailLoading(true);
    restClient.get<SecurityStatusData>(ENDPOINTS.FIREWALL_STATUS_REPO(repository.name))
      .then((data) => { if (!cancelled && data) setRepoDetail(data); })
      .catch(() => {})
      .finally(() => { if (!cancelled) setRepoDetailLoading(false); });
    return () => { cancelled = true; };
  }, [showModal, repository.name]);

  const modalData = repoDetail ?? firewallStatus ?? {
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

  const stillLoading = !firewallLoaded && !firewallStatus;
  const { message } = firewallStatus ?? {};
  const hasError = !!(firewallStatus?.errorMessage);

  const isUnprotected = !firewallStatus || (!!message && !message.includes('enabled'));
  const isAudit = !!message?.toLowerCase().includes('audit');
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
            : hasError ? 'Firewall: Unavailable. Click for details.'
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
        ) : isUnprotected ? (
          <Tooltip content={hasFirewallLicense ? 'Click to configure Firewall protection' : 'Firewall license not available'}>
            <Badge variant="soft" color="red" size="1">Unprotected</Badge>
          </Tooltip>
        ) : isAudit ? (
          <Tooltip content="Audit mode - violations reported but not blocked">
            <Flex align="center" gap="1">
              <Badge variant="soft" color="amber" size="1">Audited</Badge>
              {(firewallStatus?.criticalComponentCount ?? 0) > 0 && (
                <Badge variant="soft" color="red" size="1">{firewallStatus?.criticalComponentCount}</Badge>
              )}
            </Flex>
          </Tooltip>
        ) : (
          <Tooltip content="Quarantine mode - risky components blocked">
            <Flex align="center" gap="1">
              <Badge variant="soft" color="green" size="1">Quarantine</Badge>
              {(firewallStatus?.quarantinedComponentCount ?? 0) > 0 && (
                <Badge variant="soft" color="orange" size="1">{firewallStatus?.quarantinedComponentCount}</Badge>
              )}
            </Flex>
          </Tooltip>
        )}
      </Flex>
      {showModal && (
        <SecuritySummaryModal
          repositoryName={repository.name}
          data={modalData}
          isOpen={showModal}
          onClose={() => setShowModal(false)}
          type="firewall"
          isFirewallReportDetailLoading={repoDetailLoading}
          reportUrl={repoDetail?.reportUrl ?? firewallStatus?.reportUrl}
          onViewFullReport={(repoDetail?.reportUrl ?? firewallStatus?.reportUrl) ? () => {
            window.open(repoDetail?.reportUrl ?? firewallStatus?.reportUrl, '_blank', 'noopener,noreferrer');
          } : undefined}
          onBrowseRepo={() => {
            setShowModal(false);
            router?.stateService.go('preview.browse.browse.repo', { repoName: repository.name });
          }}
          onConfigureFirewall={() => {
            setShowModal(false);
            router?.stateService.go('preview.browse.repository-profile', { repositoryName: repository.name, tab: 'firewall' });
          }}
          firewallStatus={
            isUnprotected
              ? 'unprotected'
              : (repoDetail?.errorMessage ?? firewallStatus?.errorMessage)
                ? 'unavailable'
                : isAudit
                  ? 'audit'
                  : 'protected'
          }
          hasFirewallLicense={hasFirewallLicense}
          onEnableSuccess={() => {
            onEnableSuccess?.();
          }}
        />
      )}
    </>
  );
}

export default FirewallCell;
