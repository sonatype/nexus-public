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
import { Box, Flex, Text } from '@radix-ui/themes';
import {
  Settings,
  HardDrive,
  Server,
  Globe,
  Users,
  Trash2,
  Copy,
  Lock,
} from 'lucide-react';
import type { RepositoryProfileData } from '../types';
import { ExtJS } from '@sonatype/nexus-ui-plugin';

// =============================================================================
// Types
// =============================================================================

interface ConfigurationTabProps {
  repository: RepositoryProfileData;
}

// =============================================================================
// Helper Components
// =============================================================================

interface ConfigSectionProps {
  title: string;
  icon: React.ElementType;
  children: React.ReactNode;
}

function ConfigSection({ title, icon: Icon, children }: ConfigSectionProps): JSX.Element {
  return (
    <Box className="profile-section__card" mb="4">
      <Flex align="center" gap="2" mb="4">
        <Icon size={18} />
        <Text weight="bold">{title}</Text>
      </Flex>
      {children}
    </Box>
  );
}

interface ConfigRowProps {
  label: string;
  value: string | number | boolean | undefined | null;
  isCode?: boolean;
  isList?: boolean;
  listItems?: string[];
}

function ConfigRow({ label, value, isCode, isList, listItems }: ConfigRowProps): JSX.Element | null {
  // Don't render if value is undefined/null and not a list
  if (!isList && (value === undefined || value === null)) return null;

  let displayValue: React.ReactNode;

  if (isList && listItems) {
    displayValue = listItems.length > 0 ? (
      <Flex direction="column" gap="1">
        {listItems.map((item, i) => (
          <Text key={i} className={isCode ? 'profile-section__value--code' : undefined}>
            {item}
          </Text>
        ))}
      </Flex>
    ) : '—';
  } else if (typeof value === 'boolean') {
    displayValue = value ? 'Yes' : 'No';
  } else if (isCode) {
    displayValue = <code className="profile-section__value--code">{String(value)}</code>;
  } else {
    displayValue = String(value);
  }

  return (
    <Box className="profile-section__row">
      <Text className="profile-section__label">{label}</Text>
      <Text className="profile-section__value">{displayValue}</Text>
    </Box>
  );
}

// =============================================================================
// Component
// =============================================================================

/**
 * ConfigurationTab - Displays all repository settings in read-only format
 *
 * Organized by section:
 * - General: Name, Format, Type, Online status
 * - Storage: Blob Store, Strict Content Validation, Write Policy
 * - Proxy (if applicable): Remote URL, HTTP settings, Caching
 * - Hosted (if applicable): Deployment Policy
 * - Group (if applicable): Member repositories
 * - Cleanup: Associated cleanup policies
 * - Format-specific settings
 */
export function ConfigurationTab({ repository }: ConfigurationTabProps): JSX.Element {
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
  const npm = attrs.npm;
  const nugetProxy = attrs.nugetProxy;
  const apt = attrs.apt;
  const yum = attrs.yum;
  const raw = attrs.raw;
  const replication = attrs.replication;

  const isProxy = repository.type === 'proxy';
  const isHosted = repository.type === 'hosted';
  const isGroup = repository.type === 'group';

  const isCloud = ExtJS.useState?.(() => ExtJS.state()?.getValue?.('isCloud'));

  return (
    <Box>
      {/* General Settings */}
      <ConfigSection title="General" icon={Settings}>
        <ConfigRow label="Name" value={repository.name} />
        <ConfigRow label="Format" value={repository.format} />
        <ConfigRow label="Type" value={repository.type} />
        <ConfigRow label="Recipe" value={repository.recipe} />
        <ConfigRow label="Online" value={repository.online} />
        <ConfigRow label="URL" value={repository.url} isCode />
        {repository.routingRuleId && (
          <ConfigRow label="Routing Rule" value={repository.routingRuleId} />
        )}
      </ConfigSection>

      {/* Storage Settings */}
      <ConfigSection title="Storage" icon={HardDrive}>
        <ConfigRow label="Blob Store" value={storage?.blobStoreName} />
        <ConfigRow label="Strict Content Type Validation" value={storage?.strictContentTypeValidation} />
        {isHosted && <ConfigRow label="Write Policy" value={storage?.writePolicy} />}
      </ConfigSection>

      {/* Proxy Settings */}
      {isProxy && (
        <ConfigSection title="Proxy" icon={Globe}>
          <ConfigRow label="Remote Storage" value={proxy?.remoteUrl} isCode />
          <ConfigRow label="Content Max Age" value={proxy?.contentMaxAge !== undefined ? `${proxy.contentMaxAge} minutes` : undefined} />
          <ConfigRow label="Metadata Max Age" value={proxy?.metadataMaxAge !== undefined ? `${proxy.metadataMaxAge} minutes` : undefined} />

          {negativeCache && (
            <>
              <Text size="2" weight="medium" mt="3" mb="2" style={{ color: 'var(--gray-11)' }}>
                Negative Cache
              </Text>
              <ConfigRow label="Enabled" value={negativeCache.enabled} />
              <ConfigRow label="Time to Live" value={negativeCache.timeToLive !== undefined ? `${negativeCache.timeToLive} minutes` : undefined} />
            </>
          )}

          {httpClient && (
            <>
              <Text size="2" weight="medium" mt="3" mb="2" style={{ color: 'var(--gray-11)' }}>
                HTTP Connection
              </Text>
              <ConfigRow label="Blocked" value={httpClient.blocked} />
              <ConfigRow label="Auto Block" value={httpClient.autoBlock} />
              {httpClient.connection && (
                <>
                  <ConfigRow label="Retries" value={httpClient.connection.retries} />
                  <ConfigRow label="Timeout" value={httpClient.connection.timeout !== undefined ? `${httpClient.connection.timeout} seconds` : undefined} />
                  <ConfigRow label="Use Trust Store" value={httpClient.connection.useTrustStore} />
                  <ConfigRow label="Enable Circular Redirects" value={httpClient.connection.enableCircularRedirects} />
                  <ConfigRow label="Enable Cookies" value={httpClient.connection.enableCookies} />
                </>
              )}
              {httpClient.authentication && (
                <>
                  <Text size="2" weight="medium" mt="2" mb="2" style={{ color: 'var(--gray-11)' }}>
                    Authentication
                  </Text>
                  <ConfigRow label="Type" value={httpClient.authentication.type} />
                  <ConfigRow label="Username" value={httpClient.authentication.username} />
                </>
              )}
            </>
          )}
        </ConfigSection>
      )}

      {/* Hosted Settings */}
      {isHosted && component && (
        <ConfigSection title="Hosted" icon={Server}>
          <ConfigRow label="Proprietary Components" value={component.proprietaryComponents} />
        </ConfigSection>
      )}

      {/* Group Settings */}
      {isGroup && group && (
        <ConfigSection title="Group" icon={Users}>
          <ConfigRow
            label="Member Repositories"
            value={undefined}
            isList
            listItems={group.memberNames || []}
          />
        </ConfigSection>
      )}

      {/* Cleanup Policies */}
      {cleanup && (
        <ConfigSection title="Cleanup" icon={Trash2}>
          <ConfigRow
            label="Cleanup Policies"
            value={undefined}
            isList
            listItems={cleanup.policyName || []}
          />
        </ConfigSection>
      )}

      {/* Replication */}
      {replication && (
        <ConfigSection title="Replication" icon={Copy}>
          <ConfigRow label="Preemptive Pull Enabled" value={replication.preemptivePullEnabled} />
        </ConfigSection>
      )}

      {/* Format-Specific Settings */}
      {maven && (
        <ConfigSection title="Maven" icon={Settings}>
          <ConfigRow label="Version Policy" value={maven.versionPolicy} />
          <ConfigRow label="Layout Policy" value={maven.layoutPolicy} />
          {!isCloud && <ConfigRow label="Content Disposition" value={maven.contentDisposition} />}
        </ConfigSection>
      )}

      {docker && (
        <ConfigSection title="Docker" icon={Settings}>
          <ConfigRow label="HTTP Port" value={docker.httpPort} />
          <ConfigRow label="HTTPS Port" value={docker.httpsPort} />
          <ConfigRow label="V1 Enabled" value={docker.v1Enabled} />
          <ConfigRow label="Force Basic Auth" value={docker.forceBasicAuth} />
          <ConfigRow label="Subdomain" value={docker.subdomain} />
        </ConfigSection>
      )}

      {npm && (
        <ConfigSection title="npm" icon={Settings}>
          <ConfigRow label="Remove Quarantined" value={npm.removeQuarantined} />
        </ConfigSection>
      )}

      {nugetProxy && (
        <ConfigSection title="NuGet" icon={Settings}>
          <ConfigRow label="Query Cache Item Max Age" value={nugetProxy.queryCacheItemMaxAge !== undefined ? `${nugetProxy.queryCacheItemMaxAge} seconds` : undefined} />
          <ConfigRow label="NuGet Version" value={nugetProxy.nugetVersion} />
        </ConfigSection>
      )}

      {apt && (
        <ConfigSection title="APT" icon={Settings}>
          <ConfigRow label="Distribution" value={apt.distribution} />
          <ConfigRow label="Flat" value={apt.flat} />
        </ConfigSection>
      )}

      {yum && (
        <ConfigSection title="Yum" icon={Settings}>
          <ConfigRow label="Repodata Depth" value={yum.repodataDepth} />
          <ConfigRow label="Deploy Policy" value={yum.deployPolicy} />
        </ConfigSection>
      )}

      {raw && !isCloud && (
        <ConfigSection title="Raw" icon={Settings}>
          <ConfigRow label="Content Disposition" value={raw.contentDisposition} />
        </ConfigSection>
      )}
    </Box>
  );
}

export default ConfigurationTab;


