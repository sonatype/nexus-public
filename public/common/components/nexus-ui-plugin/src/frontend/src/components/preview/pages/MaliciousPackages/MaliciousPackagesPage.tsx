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

import React, { useCallback, useMemo, useState } from 'react';
import { Box, Callout, Container, Flex, Heading, Link, Skeleton } from '@radix-ui/themes';
import { AlertTriangle, ShieldOff, Unplug } from 'lucide-react';

import { useMaliciousPackagesData } from './useMaliciousPackagesData';
import { useProtectData } from '../Protect/useProtectData';
import { TabBar } from './TabBar';
import { OverviewTab } from './OverviewTab';
import { DetectTab } from './DetectTab';
import { useDetectRows } from './DetectTable';
import { RemediateTab } from './RemediateTab';
import { HardenTab, computeHardenCount } from './HardenTab';
import { ReportTab } from './ReportTab';
import { IdentifyTaskModal } from './IdentifyTaskModal';
import { computeNistPhase, getFindingStatus, type FindingStatus, type MaliciousFinding, type TabCounts, type TabId, type ViewMode } from './types';
import { NistStepper } from './NistStepper';
import { NextStepCallout } from './NextStepCallout';
import { MetricCards } from './MetricCards';
import { FindingsTable } from './FindingsTable';
import { AcknowledgeDialog } from './AcknowledgeDialog';

import './MaliciousPackagesPage.scss';

export default function MaliciousPackagesPage(): React.ReactElement {
  const [activeTab, setActiveTab] = useState<TabId>(() => {
    const hash = window.location.hash;
    const match = hash.match(/[?&]tab=([^&]*)/);
    const tab = match?.[1];
    if (tab === 'overview' || tab === 'detect' || tab === 'remediate' || tab === 'harden' || tab === 'report') return tab;
    return 'overview';
  });

  const [identifyTarget, setIdentifyTarget] = useState<{ repoName: string; signatureCount: number } | null>(null);
  const [remediateRepoFilter, setRemediateRepoFilter] = useState<string | null>(null);

  const data = useMaliciousPackagesData();
  const protectData = useProtectData();

  const detectRows = useDetectRows(
    data.proxyRepos, data.hcEnabledRepos, data.countsByRepo,
    data.rhcScans, data.tasks, data.activeFindings, data.identifyFailures,
  );

  const tabCounts = useMemo<TabCounts>(() => {
    const pendingCount = data.activeFindings.filter((f) => getFindingStatus(f) === 'pending').length;
    const hardenCount = computeHardenCount(protectData);
    return {
      overview: 0,
      detect: detectRows.length,
      remediate: pendingCount,
      harden: hardenCount,
      report: 0,
    };
  }, [detectRows, data.activeFindings, protectData]);

  const protectedRepoCount = useMemo(
    () => protectData.repos.filter((r) => r.protection === 'quarantine' && r.rhcEnabled && r.taskEnabled).length,
    [protectData.repos]
  );

  const handleTabChange = useCallback((tab: TabId) => {
    setActiveTab(tab);
    const hash = window.location.hash;
    const cleaned = hash.replace(/([?&])tab=[^&]*/g, '$1').replace(/[?&]$/, '').replace(/\?&/, '?');
    const separator = cleaned.includes('?') ? '&' : '?';
    window.location.hash = `${cleaned}${separator}tab=${tab}`;
  }, []);

  const handleIdentify = useCallback((repoName: string, signatureCount: number) => {
    setIdentifyTarget({ repoName, signatureCount });
  }, []);

  const handleIdentifyClose = useCallback(() => {
    setIdentifyTarget(null);
  }, []);

  const handleIdentifyComplete = useCallback(() => {
    data.refetch();
  }, [data.refetch]);

  if (data.loading) {
    return (
      <Container size="4" className="malicious-packages-page">
        <Box data-testid="malicious-packages-loading" aria-busy="true" aria-label="Loading malicious packages">
          <Skeleton height={32} width="40%" />
          <Box mt="4">
            <Skeleton height={60} />
          </Box>
          <Box mt="4">
            <Skeleton height={100} />
          </Box>
          <Box mt="4">
            <Skeleton height={200} />
          </Box>
        </Box>
      </Container>
    );
  }

  if (data.error) {
    return (
      <Container size="4" className="malicious-packages-page">
        <Heading size="6" className="page-header">
          Protect
        </Heading>
        <Callout.Root color="red" data-testid="malicious-packages-error">
          <Callout.Icon>
            <AlertTriangle size={16} />
          </Callout.Icon>
          <Callout.Text>{data.error}</Callout.Text>
        </Callout.Root>
      </Container>
    );
  }

  return (
    <Container size="4" className="malicious-packages-page">
      <Heading size="6" className="page-header">
        Protect
      </Heading>

      {(!protectData.hasIqConnection || !protectData.hasFirewall) && (
        <Flex direction="column" gap="2" mb="3" data-testid="protect-status-banners">
          {!protectData.hasIqConnection && (
            <Callout.Root color="red" size="1" data-testid="protect-iq-disconnected">
              <Callout.Icon><Unplug size={16} /></Callout.Icon>
              <Callout.Text>
                IQ Server is not connected. Identify and Remediate workflows are unavailable.{' '}
                <Link href="#preview/admin/iq" weight="bold">Configure IQ Server</Link>
              </Callout.Text>
            </Callout.Root>
          )}
          {protectData.hasIqConnection && !protectData.hasFirewall && (
            <Callout.Root color="amber" size="1" data-testid="protect-no-firewall">
              <Callout.Icon><ShieldOff size={16} /></Callout.Icon>
              <Callout.Text>
                Repository Firewall is not active. Automatic quarantine and real-time protection are unavailable.
                Contact Sonatype to enable Firewall on your IQ Server license.
              </Callout.Text>
            </Callout.Root>
          )}
        </Flex>
      )}

      <TabBar activeTab={activeTab} counts={tabCounts} onTabChange={handleTabChange} />

      <Box className="tab-content">
        {activeTab === 'overview' && (
          <OverviewTab
            data={data}
            hasFirewall={protectData.hasFirewall}
            detectCount={tabCounts.detect}
            hardenGapCount={tabCounts.harden}
            protectedRepoCount={protectedRepoCount}
            totalRepoCount={protectData.repos.length}
            onNavigate={handleTabChange}
          />
        )}
        {activeTab === 'detect' && (
          <DetectTab
            data={data}
            onIdentify={handleIdentify}
            onNavigateToRemediate={(repoName) => {
              setRemediateRepoFilter(repoName);
              handleTabChange('remediate');
            }}
          />
        )}
        {activeTab === 'remediate' && (
          <RemediateTab
            data={data}
            onNavigateToDetect={() => handleTabChange('detect')}
            repoFilter={remediateRepoFilter}
            onClearRepoFilter={() => setRemediateRepoFilter(null)}
          />
        )}
        {activeTab === 'harden' && <HardenTab protectData={protectData} />}
        {activeTab === 'report' && <ReportTab />}
      </Box>

      <IdentifyTaskModal
        open={identifyTarget !== null}
        repoName={identifyTarget?.repoName ?? ''}
        signatureCount={identifyTarget?.signatureCount ?? 0}
        onCreateAndRunAuditTask={data.createAndRunAuditTask}
        onClose={handleIdentifyClose}
        onComplete={handleIdentifyComplete}
      />
    </Container>
  );
}

export function downloadCsv(): void {
  const link = document.createElement('a');
  link.href = '/service/rest/v1/malicious-risk/malware-components-csv';
  link.download = 'malicious-packages-report.csv';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
}
