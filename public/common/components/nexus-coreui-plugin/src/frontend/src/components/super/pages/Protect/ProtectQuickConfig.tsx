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

import React, { useMemo, useState, useCallback } from 'react';
import { Box, Button, Callout, Flex, Heading, Skeleton, Table, Text, TextField, Tooltip } from '@radix-ui/themes';
import { Info, Search, ShieldOff } from 'lucide-react';
import { Box, Button, Callout, Flex, Heading, Skeleton, Table, Text, TextField } from '@radix-ui/themes';
import { Search, ShieldOff } from 'lucide-react';
import type { ProtectDataSnapshot } from './useProtectData';
import ProtectFilterSidebar, { type ProtectFilterState } from './ProtectFilterSidebar';
import ProtectRepoRow from './ProtectRepoRow';
import ProtectBulkActionModal, { type ProtectBulkAction } from './ProtectBulkActionModal';
import { isFirewallSupportedFormat } from '@/utils/firewallFormats';
import { isHealthCheckSupportedFormat } from '@/utils/healthCheckFormats';

const emptyFilters: ProtectFilterState = {
  format: [],
  protection: [],
  healthCheck: [],
  cleanup: [],
};

export interface ProtectQuickConfigProps {
  protectData: ProtectDataSnapshot;
  onRepoChanged?: (repoName: string) => void;
  hardenedRepos?: Set<string>;
}

export default function ProtectQuickConfig({ protectData, onRepoChanged, hardenedRepos }: ProtectQuickConfigProps) {
  const {
    repos,
    loading,
    error,
    refetch,
    filterCounts,
    hasFirewall,
    hasIqConnection,
    hcInstanceEnabled,
    canUpdateHealthCheck,
    lastAnalyzedByRepo,
  } = protectData;

  const [search, setSearch] = useState('');
  const [filters, setFilters] = useState<ProtectFilterState>(emptyFilters);
  const [bulkOpen, setBulkOpen] = useState(false);
  const [bulkAction, setBulkAction] = useState<ProtectBulkAction | null>(null);

  const filtered = useMemo(() => {
    const result = repos.filter((r) => {
      if (search && !r.name.toLowerCase().includes(search.toLowerCase())) return false;
      if (filters.format.length && !filters.format.includes(r.format)) return false;
      if (filters.protection.length) {
        const p = isFirewallSupportedFormat(r.format) ? r.protection : 'unsupported';
        if (!filters.protection.includes(p)) return false;
      }
      if (filters.healthCheck.length) {
        const bucket = !isHealthCheckSupportedFormat(r.format)
          ? 'unsupported'
          : r.rhcEnabled
            ? 'enabled'
            : 'disabled';
        if (!filters.healthCheck.includes(bucket)) return false;
      }
      if (filters.cleanup.length) {
        const bucket = r.taskEnabled
          ? (r.taskCleanupEnabled ? 'delete' : 'audit')
          : 'off';
        if (!filters.cleanup.includes(bucket)) return false;
      }
      return true;
    });
    result.sort((a, b) => a.name.localeCompare(b.name));
    return result;
  }, [repos, search, filters]);

  /** Bulk actions only affect repos currently in the table (search + sidebar filters). */
  const hcCandidates = useMemo(
    () =>
      filtered.filter(
        (r) =>
          isHealthCheckSupportedFormat(r.format) &&
          !r.rhcEnabled &&
          hcInstanceEnabled &&
          canUpdateHealthCheck
      ),
    [filtered, hcInstanceEnabled, canUpdateHealthCheck]
  );
  const fwCandidates = useMemo(
    () =>
      filtered.filter(
        (r) => isFirewallSupportedFormat(r.format) && hasFirewall && r.protection !== 'quarantine'
      ),
    [filtered, hasFirewall]
  );
  const clCandidates = useMemo(
    () =>
      filtered.filter((r) => isFirewallSupportedFormat(r.format) && hasFirewall && !r.taskEnabled),
    [filtered, hasFirewall]
  );

  const openBulk = useCallback((a: ProtectBulkAction) => {
    setBulkAction(a);
    setBulkOpen(true);
  }, []);

  return (
    <>
    <style>{`
      @keyframes row-highlight-fade {
        0% { background-color: var(--yellow-4); }
        100% { background-color: transparent; }
      }
    `}</style>
    <Box p="4" className="nxrm-protect-hub__quick-layout">
      <Box className="nxrm-protect-hub__sidebar-wrap">
        <ProtectFilterSidebar counts={filterCounts} value={filters} onChange={setFilters} disabled={loading} hasFirewall={hasFirewall} />
      </Box>
      <Box className="nxrm-protect-hub__main">
        <Flex className="nxrm-protect-hub__toolbar" gap="2" wrap="wrap" align="center" justify="between">
          <Heading size="4">Quick Config</Heading>
          <Flex gap="2" wrap="wrap">
            <Button
              size="2"
              variant="soft"
              disabled={loading || !canUpdateHealthCheck || !hcInstanceEnabled || hcCandidates.length === 0}
              onClick={() => openBulk('healthcheck')}
            >
              Enable Health Check on visible
            </Button>
            {hasFirewall && (
              <Button
                size="2"
                variant="soft"
                disabled={loading || fwCandidates.length === 0}
                onClick={() => openBulk('firewall')}
              >
                Enable Firewall on visible
              </Button>
            )}
            {hasFirewall && (
              <Button
                size="2"
                variant="soft"
                disabled={loading || clCandidates.length === 0}
                onClick={() => openBulk('cleanup')}
              >
                Enable Auto Remediation on visible
              </Button>
            )}
          </Flex>
        </Flex>

        <Flex mb="3" gap="3" align="center" wrap="wrap">
          <Box style={{ maxWidth: 400, flex: '1 1 200px' }}>
            <TextField.Root
              placeholder="Filter by name…"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            >
              <TextField.Slot>
                <Search size={16} />
              </TextField.Slot>
            </TextField.Root>
          </Box>
        </Flex>

        {!hasFirewall && !loading && (
          <Callout.Root color="amber" variant="surface" mb="3">
            <Callout.Icon><ShieldOff size={18} /></Callout.Icon>
            <Callout.Text>
              <Text weight="bold">Repository Firewall is not enabled.</Text>{' '}
              Firewall protection, quarantine, and Auto Remediation require a Firewall license
              and IQ Server connection. Health Check is available independently.{' '}
              <a
                href="https://links.sonatype.com/nexus-repository-firewall"
                target="_blank"
                rel="noopener noreferrer"
              >
                Learn about Repository Firewall
              </a>
            </Callout.Text>
          </Callout.Root>
        )}

        {error && (
          <Text size="2" color="red" mb="2">
            {error}
          </Text>
        )}
        {loading ? (
          <Box data-testid="protect-quick-config-skeleton" aria-busy="true" aria-label="Loading Protect Quick Config">
            <Table.Root variant="surface" size="1" mb="2">
              <Table.Header>
                <Table.Row>
                  <Table.ColumnHeaderCell>Repository</Table.ColumnHeaderCell>
                  <Table.ColumnHeaderCell>Format</Table.ColumnHeaderCell>
                  <Table.ColumnHeaderCell style={{ textAlign: 'center' }}>
                    <Flex align="center" justify="center" gap="1">
                      Health Check
                      <Tooltip content="Automated analysis that identifies known vulnerabilities and malicious package signatures in your proxy repository components. Enable per-repo to start scanning.">
                        <Info size={13} color="var(--gray-9)" style={{ cursor: 'help' }} />
                      </Tooltip>
                    </Flex>
                  </Table.ColumnHeaderCell>
                  {hasFirewall && (
                    <Table.ColumnHeaderCell style={{ textAlign: 'center' }}>
                      <Flex align="center" justify="center" gap="1">
                        Firewall
                        <Tooltip content="Controls how malicious packages are handled. None: no protection. Audit: log detections only. Quarantine: block downloads of flagged packages.">
                          <Info size={13} color="var(--gray-9)" style={{ cursor: 'help' }} />
                        </Tooltip>
                      </Flex>
                    </Table.ColumnHeaderCell>
                  )}
                  {hasFirewall && (
                    <Table.ColumnHeaderCell style={{ textAlign: 'center' }}>
                      <Flex align="center" justify="center" gap="1">
                        Auto Remediation
                        <Tooltip content="Automatically handles known malicious packages. Off: manual only. Audit: log detections. Delete: automatically remove malicious packages from the repository.">
                          <Info size={13} color="var(--gray-9)" style={{ cursor: 'help' }} />
                        </Tooltip>
                      </Flex>
                    </Table.ColumnHeaderCell>
                  )}
                </Table.Row>
              </Table.Header>
              <Table.Body>
                {[0, 1, 2, 3, 4].map((i) => (
                  <Table.Row key={i}>
                    <Table.Cell>
                      <Skeleton height={18} width="75%" />
                    </Table.Cell>
                    <Table.Cell>
                      <Skeleton height={18} width={48} />
                    </Table.Cell>
                    <Table.Cell style={{ textAlign: 'center' }}>
                      <Skeleton height={18} width={32} style={{ margin: '0 auto' }} />
                    </Table.Cell>
                    {hasFirewall && (
                      <Table.Cell style={{ textAlign: 'center' }}>
                        <Skeleton height={18} width={32} style={{ margin: '0 auto' }} />
                      </Table.Cell>
                    )}
                    {hasFirewall && (
                      <Table.Cell style={{ textAlign: 'center' }}>
                        <Skeleton height={18} width={32} style={{ margin: '0 auto' }} />
                      </Table.Cell>
                    )}
                  </Table.Row>
                ))}
              </Table.Body>
            </Table.Root>
          </Box>
        ) : (
          <>
            <Text size="2" color="gray" mb="2">
              Showing {filtered.length} of {repos.length} proxy repositories
            </Text>
            <Table.Root variant="surface" size="1">
              <Table.Header>
                <Table.Row>
                  <Table.ColumnHeaderCell>Repository</Table.ColumnHeaderCell>
                  <Table.ColumnHeaderCell>Format</Table.ColumnHeaderCell>
                  <Table.ColumnHeaderCell style={{ textAlign: 'center' }}>
                    <Flex align="center" justify="center" gap="1">
                      Health Check
                      <Tooltip content="Automated analysis that identifies known vulnerabilities and malicious package signatures in your proxy repository components. Enable per-repo to start scanning.">
                        <Info size={13} color="var(--gray-9)" style={{ cursor: 'help' }} />
                      </Tooltip>
                    </Flex>
                  </Table.ColumnHeaderCell>
                  {hasFirewall && (
                    <Table.ColumnHeaderCell style={{ textAlign: 'center' }}>
                      <Flex align="center" justify="center" gap="1">
                        Firewall
                        <Tooltip content="Controls how malicious packages are handled. None: no protection. Audit: log detections only. Quarantine: block downloads of flagged packages.">
                          <Info size={13} color="var(--gray-9)" style={{ cursor: 'help' }} />
                        </Tooltip>
                      </Flex>
                    </Table.ColumnHeaderCell>
                  )}
                  {hasFirewall && (
                    <Table.ColumnHeaderCell style={{ textAlign: 'center' }}>
                      <Flex align="center" justify="center" gap="1">
                        Auto Remediation
                        <Tooltip content="Automatically handles known malicious packages. Off: manual only. Audit: log detections. Delete: automatically remove malicious packages from the repository.">
                          <Info size={13} color="var(--gray-9)" style={{ cursor: 'help' }} />
                        </Tooltip>
                      </Flex>
                    </Table.ColumnHeaderCell>
                  )}
                </Table.Row>
              </Table.Header>
              <Table.Body>
                {filtered.map((repo) => (
                  <ProtectRepoRow
                    key={repo.name}
                    repo={repo}
                    hasFirewallLicense={hasFirewall}
                    hasIqConnection={hasIqConnection}
                    canUpdateHealthCheck={canUpdateHealthCheck}
                    hcInstanceEnabled={hcInstanceEnabled}
                    onRefetch={refetch}
                    onRepoChanged={onRepoChanged}
                    hardened={hardenedRepos?.has(repo.name)}
                  />
                ))}
              </Table.Body>
            </Table.Root>
          </>
        )}
      </Box>

      <ProtectBulkActionModal
        open={bulkOpen}
        onOpenChange={setBulkOpen}
        action={bulkAction}
        candidates={
          bulkAction === 'healthcheck' ? hcCandidates : bulkAction === 'firewall' ? fwCandidates : clCandidates
        }
        onComplete={() => void refetch()}
      />
    </Box>
    </>
  );
}
