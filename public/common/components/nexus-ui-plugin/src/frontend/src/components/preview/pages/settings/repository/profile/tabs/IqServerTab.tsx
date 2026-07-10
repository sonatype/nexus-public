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
import { Box, Flex, Text, Badge, Button, Spinner } from '@radix-ui/themes';
import { restClient } from '../../../../../../../interface/api';
import {
  Shield,
  ShieldCheck,
  ShieldOff,
  AlertTriangle,
  CheckCircle,
  XCircle,
  ExternalLink,
  Briefcase,
  AlertCircle,
} from 'lucide-react';
import { useIqConnectionStatus } from '../../../../../shared/security/useIqConnectionStatus';
import type {
  RepositoryProfileData,
  HealthCheckData,
  FirewallData,
  IqApplicationMapping,
} from '../types';

// =============================================================================
// Types
// =============================================================================

interface IqServerTabProps {
  repository: RepositoryProfileData;
  healthCheck: HealthCheckData | null;
  firewall: FirewallData | null;
  iqMapping?: IqApplicationMapping | null;
}

// =============================================================================
// Helper Components
// =============================================================================

interface ProfileSectionProps {
  title: string;
  icon: React.ElementType;
  badge?: JSX.Element;
  editPath?: string;
  children: React.ReactNode;
}

function ProfileSection({ title, icon: Icon, badge, editPath, children }: ProfileSectionProps): JSX.Element {
  return (
    <Box className="profile-section__card" mb="4">
      <Flex align="center" justify="between" mb="4" className="profile-section__header">
        <Flex align="center" gap="2">
          <Icon size={18} />
          <Text weight="bold">{title}</Text>
          {badge}
        </Flex>
        {editPath && (
          <Button
            variant="ghost"
            size="1"
            onClick={() => { window.location.hash = editPath; }}
            className="profile-section__edit-btn"
          >
            Edit <ExternalLink size={12} />
          </Button>
        )}
      </Flex>
      {children}
    </Box>
  );
}

// =============================================================================
// Component
// =============================================================================

/**
 * IqServerTab - Displays IQ Server integration status
 *
 * Maps to Settings → IQ Server:
 * - Repository Firewall: Quarantine status and policy violations
 * - Repository Health Check: Security and license analysis
 * - IQ Application Mapping: Application, Organization, Stage
 *
 * Connection awareness (BDD Scenario 10):
 * - When IQ Server is configured but unreachable, shows error with specific message
 * - Links to IQ Server settings for configuration
 */
export function IqServerTab({
  repository,
  healthCheck,
  firewall,
  iqMapping,
}: IqServerTabProps): JSX.Element {
  const hasIqData = healthCheck || firewall;
  const [isIqConfigured, setIsIqConfigured] = useState<boolean | null>(null);

  // Use connection status hook for real-time awareness
  const iqConnection = useIqConnectionStatus();

  useEffect(() => {
    if (!hasIqData) {
      restClient
        .get<{ enabled?: boolean; url?: string }>('/service/rest/v1/iq')
        .then((config) => {
          setIsIqConfigured(Boolean(config?.enabled && config?.url));
        })
        .catch(() => {
          setIsIqConfigured(false);
        });
    } else {
      setIsIqConfigured(null);
    }
  }, [hasIqData]);

  // Show connection testing state
  if (iqConnection.isConfigured && iqConnection.isTesting) {
    return (
      <Box className="profile-empty-state">
        <Spinner size="3" />
        <Text className="profile-empty-state__title" style={{ marginTop: 16 }}>
          Testing IQ Server Connection...
        </Text>
      </Box>
    );
  }

  // Show disconnected state (BDD Scenario 10)
  if (iqConnection.isConfigured && iqConnection.state === 'disconnected') {
    return (
      <Box className="profile-empty-state">
        <ShieldOff size={64} className="profile-empty-state__icon" style={{ color: 'var(--red-9)' }} />
        <Flex align="center" gap="2" mb="2">
          <AlertCircle size={18} color="var(--red-9)" />
          <Text className="profile-empty-state__title" style={{ color: 'var(--red-11)' }}>
            IQ Server Connection Unavailable
          </Text>
        </Flex>
        <Text className="profile-empty-state__message">
          {iqConnection.errorMessage || 'Unable to reach IQ Server. Check configuration.'}
        </Text>
        <Flex gap="2" mt="4" justify="center" wrap="wrap">
          <Button
            variant="soft"
            size="2"
            onClick={() => { window.location.hash = '#preview/admin/iq'; }}
          >
            Configure IQ Server
          </Button>
        </Flex>
      </Box>
    );
  }

  if (!hasIqData) {
    return (
      <Box className="profile-empty-state">
        <Shield size={64} className="profile-empty-state__icon" />
        <Text className="profile-empty-state__title">
          {isIqConfigured
            ? 'Health Check and Firewall Not Enabled for This Repository'
            : 'IQ Server Features Not Configured'}
        </Text>
        <Text className="profile-empty-state__message">
          {isIqConfigured
            ? `IQ Server is configured. Enable the Firewall Audit capability for this repository to see firewall and health check data.`
            : 'Configure IQ Server integration in Settings to enable Repository Health Check and Firewall features.'}
        </Text>
        <Flex gap="2" mt="4" justify="center" wrap="wrap">
          {isIqConfigured ? (
            <Button
              variant="soft"
              size="2"
              onClick={() => { window.location.hash = '#preview/admin/system/capabilities'; }}
            >
              Open Capabilities
            </Button>
          ) : (
            <Button
              variant="soft"
              size="2"
              onClick={() => { window.location.hash = '#preview/admin/iq'; }}
            >
              Configure IQ Server
            </Button>
          )}
        </Flex>
      </Box>
    );
  }

  return (
    <Box>
      {/* Firewall Status */}
      <ProfileSection
        title="Repository Firewall"
        icon={Shield}
        badge={
          <Badge color={firewall?.enabled ? 'green' : 'gray'} size="1">
            {firewall?.enabled ? 'Enabled' : 'Disabled'}
          </Badge>
        }
      >
        {firewall?.enabled ? (
          <Flex direction="column" gap="3">
            <Box className="profile-section__row">
              <Text className="profile-section__label">Quarantine Policy</Text>
              <Text className="profile-section__value">{firewall.quarantinePolicy || '—'}</Text>
            </Box>

            <Box className="profile-section__row">
              <Text className="profile-section__label">Auto-Release</Text>
              <Text className="profile-section__value">
                {firewall.autoRelease ? 'Enabled' : 'Disabled'}
              </Text>
            </Box>

            <Box className="profile-section__row">
              <Text className="profile-section__label">Quarantined Components</Text>
              <Text className="profile-section__value">{firewall.quarantinedCount ?? 0}</Text>
            </Box>

            {/* Policy Violations Breakdown */}
            <Box className="profile-section__violations">
              <Text size="2" weight="medium" mb="2" style={{ color: 'var(--gray-11)' }}>
                Policy Violations
              </Text>
              <Flex gap="4" wrap="wrap">
                <Flex align="center" gap="1">
                  <Badge color="red" size="1">Critical</Badge>
                  <Text weight="medium">{firewall.criticalCount ?? 0}</Text>
                </Flex>
                <Flex align="center" gap="1">
                  <Badge color="orange" size="1">High</Badge>
                  <Text weight="medium">{firewall.severeCount ?? 0}</Text>
                </Flex>
                <Flex align="center" gap="1">
                  <Badge color="yellow" size="1">Medium</Badge>
                  <Text weight="medium">{firewall.moderateCount ?? 0}</Text>
                </Flex>
                <Flex align="center" gap="1">
                  <Badge color="gray" size="1">Low</Badge>
                  <Text weight="medium">{firewall.lowCount ?? 0}</Text>
                </Flex>
              </Flex>
            </Box>

            {firewall.reportUrl && (
              <Button
                variant="soft"
                size="1"
                onClick={() => window.open(firewall.reportUrl, '_blank')}
              >
                <ExternalLink size={14} />
                View in Lifecycle
              </Button>
            )}
          </Flex>
        ) : (
          <Box className="profile-empty-state" style={{ padding: '20px' }}>
            <Text color="gray" size="2">
              Firewall is not enabled for this repository.
              Configure it in Settings to protect against vulnerable components.
            </Text>
          </Box>
        )}
      </ProfileSection>

      {/* Health Check Status */}
      <ProfileSection
        title="Repository Health Check"
        icon={ShieldCheck}
        badge={
          <Badge color={healthCheck?.enabled ? 'green' : 'gray'} size="1">
            {healthCheck?.enabled ? (healthCheck.analyzing ? 'Analyzing' : 'Enabled') : 'Disabled'}
          </Badge>
        }
      >
        {healthCheck?.enabled ? (
          <Flex direction="column" gap="3">
            {healthCheck.analyzing ? (
              <Box className="profile-section__row">
                <Text className="profile-section__label">Status</Text>
                <Flex align="center" gap="2">
                  <Text className="profile-section__value">Analyzing components...</Text>
                </Flex>
              </Box>
            ) : (
              <>
                <Box className="profile-section__row">
                  <Text className="profile-section__label">Status</Text>
                  <Flex align="center" gap="1">
                    <CheckCircle size={14} color="var(--green-11)" />
                    <Text className="profile-section__value">Complete</Text>
                  </Flex>
                </Box>

                <Box className="profile-section__row">
                  <Text className="profile-section__label">Components Analyzed</Text>
                  <Text className="profile-section__value">
                    {healthCheck.componentsAnalyzed?.toLocaleString() ?? '—'}
                  </Text>
                </Box>

                <Box className="profile-section__row">
                  <Text className="profile-section__label">Security Issues</Text>
                  <Flex align="center" gap="1">
                    {(healthCheck.securityIssueCount ?? 0) > 0 ? (
                      <>
                        <AlertTriangle size={14} color="var(--yellow-11)" />
                        <Text className="profile-section__value">{healthCheck.securityIssueCount}</Text>
                      </>
                    ) : (
                      <>
                        <CheckCircle size={14} color="var(--green-11)" />
                        <Text className="profile-section__value">None</Text>
                      </>
                    )}
                  </Flex>
                </Box>

                <Box className="profile-section__row">
                  <Text className="profile-section__label">License Issues</Text>
                  <Flex align="center" gap="1">
                    {(healthCheck.licenseIssueCount ?? 0) > 0 ? (
                      <>
                        <AlertTriangle size={14} color="var(--yellow-11)" />
                        <Text className="profile-section__value">{healthCheck.licenseIssueCount}</Text>
                      </>
                    ) : (
                      <>
                        <CheckCircle size={14} color="var(--green-11)" />
                        <Text className="profile-section__value">None</Text>
                      </>
                    )}
                  </Flex>
                </Box>

                {healthCheck.detailUrl && (
                  <Button
                    variant="soft"
                    size="1"
                    onClick={() => window.open(healthCheck.detailUrl, '_blank')}
                  >
                    <ExternalLink size={14} />
                    View Detailed Report
                  </Button>
                )}
              </>
            )}
          </Flex>
        ) : (
          <Box className="profile-empty-state" style={{ padding: '20px' }}>
            <Text color="gray" size="2">
              Health Check is not enabled for this repository.
              Enable it to scan for security and license issues.
            </Text>
          </Box>
        )}
      </ProfileSection>

      {/* IQ Application Mapping */}
      <ProfileSection
        title="IQ Application Mapping"
        icon={Briefcase}
      >
        {iqMapping ? (
          <Flex direction="column" gap="2">
            <Box className="profile-section__row">
              <Text className="profile-section__label">Application</Text>
              <Text className="profile-section__value">{iqMapping.applicationName || '—'}</Text>
            </Box>
            <Box className="profile-section__row">
              <Text className="profile-section__label">Organization</Text>
              <Text className="profile-section__value">{iqMapping.organization || '—'}</Text>
            </Box>
            <Box className="profile-section__row">
              <Text className="profile-section__label">Stage</Text>
              <Text className="profile-section__value">{iqMapping.stage || '—'}</Text>
            </Box>
            {iqMapping.iqServerUrl && (
              <Button
                variant="soft"
                size="1"
                onClick={() => window.open(iqMapping.iqServerUrl, '_blank')}
              >
                <ExternalLink size={14} />
                View in IQ Server
              </Button>
            )}
          </Flex>
        ) : (
          <Text color="gray" size="2">
            No IQ Server application mapping configured for this repository.
          </Text>
        )}
      </ProfileSection>
    </Box>
  );
}

export default IqServerTab;


