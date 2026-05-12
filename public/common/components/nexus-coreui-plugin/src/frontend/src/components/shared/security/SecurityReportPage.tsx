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

import React, { useEffect, useState, useCallback } from 'react';
import { useRouter } from '@uirouter/react';
import { Flex, Text, Box, Button, Badge } from '@radix-ui/themes';
import { ArrowLeft, ExternalLink, ShieldCheck, Shield, AlertTriangle } from 'lucide-react';
import { restClient, ENDPOINTS } from '@/utils/api';
import { LoadingState, ErrorState } from '@/components/shared';
import { ArtifactTable } from './ArtifactTable';
import { useArtifactList } from './useArtifactList';
import type { SecurityStatusData } from './security.types';

import './SecurityReportPage.scss';

export type SecurityReportType = 'health-check' | 'firewall';

export interface SecurityReportPageProps {
  repositoryName: string;
  reportType: SecurityReportType;
  /** When true, hide Back button and breadcrumb (embedded in tab) */
  embedded?: boolean;
}

interface HealthCheckApiItem {
  repositoryName: string;
  enabled?: boolean;
  detailUrl?: string;
  summaryUrl?: string;
  securityIssueCount?: number;
  licenseIssueCount?: number;
  results?: { criticalCount?: number; severeCount?: number; totalCount?: number };
}

interface FirewallApiItem {
  repositoryName: string;
  affectedComponentCount?: number;
  criticalComponentCount?: number;
  severeComponentCount?: number;
  moderateComponentCount?: number;
  quarantinedComponentCount?: number;
  reportUrl?: string;
}

/**
 * SecurityReportPage - Full-page security report (Health Check or Firewall).
 * Replaces the cramped modal with a dedicated page; iframe fills viewport.
 */
export function SecurityReportPage({ repositoryName, reportType, embedded = false }: SecurityReportPageProps): JSX.Element {
  const router = useRouter();
  const [data, setData] = useState<SecurityStatusData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const title = reportType === 'health-check' ? 'Health Check Report' : 'Firewall Report';
  const breadcrumbReportLabel = reportType === 'health-check' ? 'Health Check Report' : 'Firewall Report';

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      if (reportType === 'health-check') {
        const items = await restClient.get<HealthCheckApiItem[]>(ENDPOINTS.HEALTH_CHECK);
        const item = (items || []).find((i) => i.repositoryName === repositoryName);
        if (!item) {
          setError(`No health check data found for ${repositoryName}`);
          setData(null);
          return;
        }
        const reportUrl = item.detailUrl || item.summaryUrl;
        setData({
          repositoryName,
          affectedComponentCount: item.results?.totalCount ?? (item.securityIssueCount || 0) + (item.licenseIssueCount || 0),
          criticalComponentCount: item.securityIssueCount ?? item.results?.criticalCount ?? 0,
          severeComponentCount: item.results?.severeCount ?? 0,
          moderateComponentCount: item.licenseIssueCount ?? 0,
          quarantinedComponentCount: 0,
          reportUrl: reportUrl || undefined,
        });
      } else {
        const items = await restClient.get<FirewallApiItem[]>(ENDPOINTS.FIREWALL_STATUS);
        const item = (items || []).find((i) => i.repositoryName === repositoryName);
        if (!item) {
          setError(`No firewall data found for ${repositoryName}`);
          setData(null);
          return;
        }
        setData({
          repositoryName,
          affectedComponentCount: item.affectedComponentCount ?? 0,
          criticalComponentCount: item.criticalComponentCount ?? 0,
          severeComponentCount: item.severeComponentCount ?? 0,
          moderateComponentCount: item.moderateComponentCount ?? 0,
          quarantinedComponentCount: item.quarantinedComponentCount ?? 0,
          reportUrl: item.reportUrl,
        });
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load report data');
      setData(null);
    } finally {
      setLoading(false);
    }
  }, [repositoryName, reportType]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleBack = useCallback(() => {
    router.stateService.go('preview.browse.browse');
  }, [router]);

  const hasIssues =
    data &&
    (data.criticalComponentCount > 0 || data.severeComponentCount > 0 || data.moderateComponentCount > 0);

  const artifactList = useArtifactList(repositoryName, reportType);
  const useArtifactTable = artifactList.endpointAvailable === true;

  if (loading) {
    return (
      <Box className="security-report-page">
        <LoadingState message="Loading report..." />
      </Box>
    );
  }

  if (error) {
    return (
      <Box className="security-report-page">
        {!embedded && (
          <Box className="security-report-page__header">
            <Button variant="ghost" onClick={handleBack}>
              <ArrowLeft size={16} />
              Back
            </Button>
          </Box>
        )}
        <ErrorState title="Report Unavailable" message={error} action={embedded ? undefined : { label: 'Back to Browse', onClick: handleBack }} />
      </Box>
    );
  }

  if (!data) {
    return null;
  }

  return (
    <Box className="security-report-page">
      <Box className="security-report-page__header">
        <Flex align="center" gap="3" wrap="wrap">
          {!embedded && (
            <>
              <Button variant="ghost" color="gray" onClick={handleBack} className="security-report-page__back">
                <ArrowLeft size={16} />
                Back
              </Button>
              <Box className="security-report-page__breadcrumb">
                <Text size="1" color="gray">
                  Browse → {repositoryName} → {breadcrumbReportLabel}
                </Text>
              </Box>
            </>
          )}
          {data.reportUrl && (
            <Button variant="soft" asChild>
              <a href={data.reportUrl} target="_blank" rel="noopener noreferrer" className="security-report-page__open-iq">
                <ExternalLink size={16} />
                Open in IQ Server
              </a>
            </Button>
          )}
        </Flex>
        <Flex align="center" gap="3" mt="2">
          {reportType === 'health-check' ? (
            <ShieldCheck size={24} color="var(--accent-9)" />
          ) : (
            <Shield size={24} color="var(--orange-9)" />
          )}
          <Box>
            <Text size="5" weight="bold">{title}</Text>
            <Text size="2" color="gray" as="p">{repositoryName}</Text>
          </Box>
        </Flex>
      </Box>

      <Box className="security-report-page__body">
        <Flex direction="column" gap="4" className="security-report-page__content">
            {/* Quarantine badge (Firewall only) */}
            {reportType === 'firewall' && data.quarantinedComponentCount > 0 && (
              <Badge color="orange" size="2" variant="soft" className="security-report-page__quarantine">
                <AlertTriangle size={16} />
                {data.quarantinedComponentCount} components are currently in quarantine
              </Badge>
            )}

            {/* Summary row – three severity cards + artifacts identified */}
            <Flex gap="4" wrap="wrap" className="security-report-page__summary">
              <SummaryCard
                label="Critical"
                value={data.criticalComponentCount}
                color="red"
              />
              <SummaryCard
                label="Severe"
                value={data.severeComponentCount}
                color="orange"
              />
              <SummaryCard
                label="Moderate"
                value={data.moderateComponentCount}
                color="yellow"
              />
              <SummaryCard
                label="Artifacts identified"
                value={data.affectedComponentCount}
                color="gray"
              />
            </Flex>

            {/* Report content: native ArtifactTable when backend supports it, else iframe */}
            <Box className="security-report-page__iframe-container">
              {useArtifactTable ? (
                <ArtifactTable
                  items={artifactList.items}
                  loading={artifactList.loading}
                  error={artifactList.error}
                  hasMore={artifactList.hasMore}
                  onLoadMore={artifactList.loadMore}
                />
              ) : data.reportUrl ? (
                <iframe
                  src={data.reportUrl}
                  title={`${title} for ${repositoryName}`}
                  className="security-report-page__iframe"
                  sandbox="allow-same-origin allow-scripts"
                />
              ) : (
                <Flex direction="column" align="center" justify="center" p="8" className="security-report-page__empty">
                  <ShieldCheck size={48} color="var(--gray-7)" />
                  <Text size="3" weight="medium" color="gray" mt="3">
                    {hasIssues ? 'Full report data is being processed.' : 'No security issues detected.'}
                  </Text>
                  <Text size="2" color="gray" mt="1">
                    Run a new analysis to update these results.
                  </Text>
                </Flex>
              )}
            </Box>
        </Flex>
      </Box>
    </Box>
  );
}

function SummaryCard({
  label,
  value,
  color,
}: {
  label: string;
  value: number;
  color: 'red' | 'orange' | 'yellow' | 'gray';
}): JSX.Element {
  const colorStyle = color === 'gray'
    ? { color: 'var(--gray-11)' }
    : { color: `var(--${color}-9)` };
  return (
    <Box className={`security-report-page__card security-report-page__card--${color}`}>
      <Flex direction="column" align="center" gap="1">
        <Text size="2" weight="bold" style={color === 'gray' ? { color: 'var(--gray-11)' } : { color: `var(--${color}-9)`, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
          {label}
        </Text>
        <Text size="6" weight="bold" style={colorStyle}>
          {value.toLocaleString()}
        </Text>
      </Flex>
    </Box>
  );
}

export default SecurityReportPage;
