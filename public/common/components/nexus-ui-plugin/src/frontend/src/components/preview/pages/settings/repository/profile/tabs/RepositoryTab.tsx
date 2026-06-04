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

import React from 'react';
import {
  Box,
  Flex,
  Text,
  Card,
  Grid,
  Separator,
  Badge,
  Heading,
} from '@radix-ui/themes';
import {
  Database,
  Trash2,
  Route,
  HardDrive,
  Clock,
  Info,
} from 'lucide-react';
import { FormatBadge } from '../../../../../shared';
import { TypeBadge } from '../../repositories/TypeBadge';
import { ClassicSettingsLink } from './classicSettingsLink';
import type {
  RepositoryProfileData,
  BlobStoreInfo,
  CleanupPolicyInfo,
  RoutingRuleInfo,
} from '../hooks/useRepositoryProfile';

// =============================================================================
// Types
// =============================================================================

interface RepositoryTabProps {
  repository: RepositoryProfileData;
  blobStore: BlobStoreInfo | null;
  cleanupPolicies: CleanupPolicyInfo[];
  routingRule: RoutingRuleInfo | null;
}

// =============================================================================
// Helper Components
// =============================================================================

interface ProfileSectionProps {
  title: string;
  icon: React.ElementType;
  editPath?: string;
  children: React.ReactNode;
}

function ProfileSection({ title, icon: Icon, editPath, children }: ProfileSectionProps): JSX.Element {
  return (
    <Card size="2" mb="4">
      <Flex direction="column" gap="3">
        <Flex align="center" justify="between">
          <Flex align="center" gap="2">
            <Icon size={18} color="var(--accent-9)" />
            <Heading size="3" style={{ fontSize: 'var(--font-size-4)' }}>{title}</Heading>
          </Flex>
          {editPath && (
            <ClassicSettingsLink previewPath={editPath} label="Configure" />
          )}
        </Flex>
        <Separator size="4" />
        <Box pt="1">
          {children}
        </Box>
      </Flex>
    </Card>
  );
}

interface ConfigRowProps {
  label: string;
  value: React.ReactNode;
  isCode?: boolean;
  isList?: boolean;
  listItems?: string[];
  fullWidth?: boolean;
}

function ConfigRow({ label, value, isCode, isList, listItems, fullWidth }: ConfigRowProps): JSX.Element | null {
  if (!isList && (value === undefined || value === null)) return null;

  let displayValue: React.ReactNode;

  if (isList && listItems) {
    displayValue = listItems.length > 0 ? (
      <Flex direction="column" gap="1">
        {listItems.map((item, i) => (
          <Text key={i} size="2" style={isCode ? { fontFamily: 'var(--code-font-family)' } : undefined}>
            {item}
          </Text>
        ))}
      </Flex>
    ) : <Text size="2" color="gray">None</Text>;
  } else if (typeof value === 'boolean') {
    displayValue = (
      <Badge color={value ? 'green' : 'gray'} variant="soft">
        {value ? 'Enabled' : 'Disabled'}
      </Badge>
    );
  } else if (isCode) {
    displayValue = (
      <Text size="2" style={{ wordBreak: 'break-all', fontFamily: 'var(--code-font-family)', color: 'var(--accent-11)' }}>
        {String(value)}
      </Text>
    );
  } else {
    displayValue = React.isValidElement(value) ? value : (
      <Text size="2" weight="medium">
        {String(value)}
      </Text>
    );
  }

  return (
    <Box mb="3" style={fullWidth ? { gridColumn: 'span 2' } : undefined}>
      <Box mb="1">
        <Text size="1" color="gray" weight="bold" style={{ textTransform: 'uppercase', letterSpacing: '0.05em' }}>
          {label}
        </Text>
      </Box>
      {displayValue}
    </Box>
  );
}

// =============================================================================
// Helper Functions
// =============================================================================

function formatBytes(bytes: number | undefined): string {
  if (bytes === undefined || bytes === null) return '—';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  let unitIndex = 0;
  let size = bytes;
  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024;
    unitIndex++;
  }
  return `${size.toFixed(1)} ${units[unitIndex]}`;
}

// =============================================================================
// Component
// =============================================================================

export function RepositoryTab({
  repository,
  blobStore,
  cleanupPolicies,
  routingRule,
}: RepositoryTabProps): JSX.Element {
  const attrs = repository.attributes || {};
  const storage = attrs.storage;
  const proxy = attrs.proxy;
  const negativeCache = attrs.negativeCache;
  const httpClient = attrs.httpClient;
  const group = attrs.group;
  const cleanup = attrs.cleanup;
  const component = attrs.component;
  const maven = attrs.maven;
  const docker = attrs.docker;
  const dockerProxy = attrs.dockerProxy;
  const npm = attrs.npm;
  const nugetProxy = attrs.nugetProxy;
  const apt = attrs.apt;
  const aptSigning = attrs.aptSigning;
  const yum = attrs.yum;
  const raw = attrs.raw;
  const replication = attrs.replication;
  const cocoapods = attrs.cocoapods;
  const conan = attrs.conan;
  const conda = attrs.conda;
  const goConfig = attrs.go;
  const gitlfs = attrs.gitlfs;
  const helm = attrs.helm;
  const p2 = attrs.p2;
  const r = attrs.r;
  const rubygems = attrs.rubygems;

  const isProxy = repository.type === 'proxy';
  const isHosted = repository.type === 'hosted';
  const isGroup = repository.type === 'group';

  return (
    <Box>
      <ProfileSection
        title="Repository Configuration"
        icon={Database}
        editPath={`preview/admin/repository/repositories/${encodeURIComponent(repository.name)}`}
      >
        <Grid columns="2" gapX="6" gapY="2">
          <Box style={{ gridColumn: 'span 2' }} mb="2">
            <Text size="2" weight="bold" color="accent">General</Text>
          </Box>
          <ConfigRow label="Name" value={repository.name} />
          <ConfigRow label="Format" value={<FormatBadge format={repository.format} />} />
          <ConfigRow label="Type" value={<TypeBadge type={repository.type as any} />} />
          <ConfigRow label="Recipe" value={repository.recipe} />
          <ConfigRow label="Online" value={repository.online} />
          <ConfigRow label="URL" value={repository.url} isCode fullWidth />
          
          {repository.routingRuleId && (
            <ConfigRow label="Routing Rule" value={repository.routingRuleId} />
          )}

          <Box style={{ gridColumn: 'span 2' }} mt="4" mb="2">
            <Text size="2" weight="bold" color="accent">Storage & Cleanup</Text>
          </Box>
          <ConfigRow label="Blob Store" value={storage?.blobStoreName} />
          <ConfigRow label="Strict Content Type Validation" value={storage?.strictContentTypeValidation} />
          {isHosted && <ConfigRow label="Write Policy" value={storage?.writePolicy} />}
          
          <ConfigRow
            label="Cleanup Policies"
            value={undefined}
            isList
            listItems={cleanup?.policyName || []}
          />

          {isProxy && (
            <>
              <Box style={{ gridColumn: 'span 2' }} mt="4" mb="2">
                <Text size="2" weight="bold" color="accent">Proxy Configuration</Text>
              </Box>
              <ConfigRow label="Remote Storage" value={proxy?.remoteUrl} isCode fullWidth />
              <ConfigRow label="Content Max Age" value={proxy?.contentMaxAge !== undefined ? `${proxy.contentMaxAge} minutes` : undefined} />
              <ConfigRow label="Metadata Max Age" value={proxy?.metadataMaxAge !== undefined ? `${proxy.metadataMaxAge} minutes` : undefined} />

              {negativeCache && (
                <>
                  <ConfigRow label="Negative Cache Enabled" value={negativeCache.enabled} />
                  <ConfigRow label="Negative Cache TTL" value={negativeCache.timeToLive !== undefined ? `${negativeCache.timeToLive} minutes` : undefined} />
                </>
              )}

              {httpClient && (
                <>
                  <Box style={{ gridColumn: 'span 2' }} mt="2" mb="2">
                    <Text size="1" weight="bold" color="gray">HTTP Client</Text>
                  </Box>
                  <ConfigRow label="Blocked" value={httpClient.blocked} />
                  <ConfigRow label="Auto Block" value={httpClient.autoBlock} />
                  {httpClient.connection && (
                    <>
                      <ConfigRow label="Retries" value={httpClient.connection.retries} />
                      <ConfigRow label="Timeout" value={httpClient.connection.timeout !== undefined ? `${httpClient.connection.timeout} seconds` : undefined} />
                      <ConfigRow label="Use Trust Store" value={httpClient.connection.useTrustStore} />
                    </>
                  )}
                  {httpClient.authentication && (
                    <ConfigRow label="Auth Type" value={httpClient.authentication.type} />
                  )}
                </>
              )}
            </>
          )}

          {isGroup && group && (
            <>
              <Box style={{ gridColumn: 'span 2' }} mt="4" mb="2">
                <Text size="2" weight="bold" color="accent">Group Settings</Text>
              </Box>
              <ConfigRow
                label="Member Repositories"
                value={undefined}
                isList
                listItems={group.memberNames || []}
                fullWidth
              />
              <ConfigRow label="Writable Member" value={group.writableMember || 'None'} />
            </>
          )}

          {/* Format Specific Settings */}
          {(maven || docker || npm || nugetProxy || apt || yum || raw || replication) && (
            <Box style={{ gridColumn: 'span 2' }} mt="4" mb="2">
              <Text size="2" weight="bold" color="accent">Format Settings ({repository.format})</Text>
            </Box>
          )}
          
          {maven && (
            <>
              <ConfigRow label="Version Policy" value={maven.versionPolicy} />
              <ConfigRow label="Layout Policy" value={maven.layoutPolicy} />
            </>
          )}
          
          {docker && (
            <>
              <ConfigRow label="HTTP Port" value={docker.httpPort} />
              <ConfigRow label="HTTPS Port" value={docker.httpsPort} />
            </>
          )}

          {npm && (
            <ConfigRow label="Remove Quarantined" value={npm.removeQuarantined} />
          )}
        </Grid>
      </ProfileSection>

      <ProfileSection
        title={`Blob Store: ${storage?.blobStoreName || '—'}`}
        icon={HardDrive}
        editPath={storage?.blobStoreName ? `preview/admin/repository/blobstores/${encodeURIComponent(storage.blobStoreName)}` : undefined}
      >
        {blobStore ? (
          <Grid columns="3" gap="4">
            <ConfigRow label="Name" value={blobStore.name} />
            <ConfigRow label="Type" value={blobStore.type} />
            <ConfigRow label="Status" value={blobStore.unavailable ? <Badge color="red">Unavailable</Badge> : <Badge color="green">Available</Badge>} />
            <ConfigRow label="Total Size" value={formatBytes(blobStore.totalSizeInBytes ?? 0)} />
            <ConfigRow label="Available Space" value={formatBytes(blobStore.availableSpaceInBytes ?? 0)} />
            <ConfigRow label="Blob Count" value={(blobStore.blobCount ?? 0).toLocaleString()} />
            {blobStore.path && <ConfigRow label="Location" value={blobStore.path} fullWidth />}
          </Grid>
        ) : (
          <Flex align="center" gap="2" p="3" style={{ backgroundColor: 'var(--gray-2)', borderRadius: '4px' }}>
            <Info size={16} color="var(--gray-8)" />
            <Text color="gray" size="2">Blob store details not available</Text>
          </Flex>
        )}
      </ProfileSection>

      {cleanupPolicies.length > 0 && (
        <ProfileSection
          title="Cleanup Policies"
          icon={Trash2}
          editPath="preview/admin/repository/cleanup-policies"
        >
          <Grid columns="2" gap="4">
            {cleanupPolicies.map((policy) => (
              <Card key={policy.name} variant="ghost" style={{ backgroundColor: 'var(--gray-2)' }}>
                <Flex direction="column" gap="1">
                  <Text size="2" weight="bold">{policy.name}</Text>
                  <Flex align="center" gap="2">
                    <Clock size={12} color="var(--gray-8)" />
                    <Text size="1" color="gray">
                      {policy.lastRun ? `Last run: ${new Date(policy.lastRun).toLocaleDateString()}` : 'Never run'}
                    </Text>
                  </Flex>
                </Flex>
              </Card>
            ))}
          </Grid>
        </ProfileSection>
      )}

      {routingRule && (
        <ProfileSection
          title="Routing Rules"
          icon={Route}
          editPath="preview/admin/repository/routing-rules"
        >
          <Grid columns="2" gap="4">
            <ConfigRow label="Name" value={routingRule.name} />
            <ConfigRow label="Mode" value={routingRule.mode} />
            <ConfigRow label="Matchers" value={undefined} isList listItems={routingRule.matchers} fullWidth />
          </Grid>
        </ProfileSection>
      )}
    </Box>
  );
}

export default RepositoryTab;
