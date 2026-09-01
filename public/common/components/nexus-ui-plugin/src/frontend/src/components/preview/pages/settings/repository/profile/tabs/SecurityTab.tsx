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
import { Box, Flex, Text, Badge } from '@radix-ui/themes';
import {
  Shield,
  Heart,
  AlertTriangle,
  CheckCircle,
  ExternalLink,
} from 'lucide-react';
import type {
  RepositoryProfileData,
  HealthCheckData,
  FirewallData,
} from '../types';

// =============================================================================
// Types
// =============================================================================

interface SecurityTabProps {
  repository: RepositoryProfileData;
  healthCheck: HealthCheckData | null;
  firewall: FirewallData | null;
}

// =============================================================================
// Component
// =============================================================================

/**
 * SecurityTab - Displays security status including Firewall and Health Check
 */
export function SecurityTab({
  repository,
  healthCheck,
  firewall,
}: SecurityTabProps): JSX.Element {
  const hasSecurityData = healthCheck || firewall;

  if (!hasSecurityData) {
    return (
      <Box className="profile-empty-state">
        <Shield size={64} className="profile-empty-state__icon" />
        <Text className="profile-empty-state__title">Security Features Not Configured</Text>
        <Text className="profile-empty-state__message">
          Repository Health Check and Firewall are not enabled for this repository.
          Enable these features in Settings to monitor security status.
        </Text>
      </Box>
    );
  }

  return (
    <Box className="profile-section__grid">
      {/* Firewall Status */}
      <Box className="status-card">
        <Box className="status-card__header">
          <Flex align="center" gap="2" className="status-card__title">
            <Shield
              size={18}
              className={firewall?.enabled ? 'status-card__icon--enabled' : 'status-card__icon--disabled'}
            />
            Repository Firewall
          </Flex>
          <Badge color={firewall?.enabled ? 'green' : 'gray'}>
            {firewall?.enabled ? 'Enabled' : 'Disabled'}
          </Badge>
        </Box>

        {firewall?.enabled ? (
          <Box className="status-card__content">
            <Box className="status-card__row">
              <Text className="status-card__label">Quarantine Policy</Text>
              <Text className="status-card__value">{firewall.quarantinePolicy || '—'}</Text>
            </Box>

            <Box className="status-card__row">
              <Text className="status-card__label">Policy Violations</Text>
              <Text className="status-card__value">
                {firewall.policyViolations ?? 0}
                {(firewall.criticalCount ?? 0) > 0 && (
                  <Badge color="red" ml="2" size="1">
                    {firewall.criticalCount} Critical
                  </Badge>
                )}
              </Text>
            </Box>

          </Box>
        ) : (
          <Box className="profile-empty-state" style={{ padding: '20px' }}>
            <Text color="gray" size="2">
              Firewall is not enabled for this repository.
              Configure it in Settings to protect against vulnerable components.
            </Text>
          </Box>
        )}
      </Box>

      {/* Health Check Status */}
      <Box className="status-card">
        <Box className="status-card__header">
          <Flex align="center" gap="2" className="status-card__title">
            <Heart
              size={18}
              className={healthCheck?.enabled ? 'status-card__icon--enabled' : 'status-card__icon--disabled'}
            />
            Repository Health Check
          </Flex>
          <Badge color={healthCheck?.enabled ? 'green' : 'gray'}>
            {healthCheck?.enabled ? (healthCheck.analyzing ? 'Analyzing' : 'Enabled') : 'Disabled'}
          </Badge>
        </Box>

        {healthCheck?.enabled ? (
          <Box className="status-card__content">
            {healthCheck.analyzing ? (
              <Box className="status-card__row">
                <Text className="status-card__label">Status</Text>
                <Flex align="center" gap="2">
                  <Text className="status-card__value">Analyzing components...</Text>
                </Flex>
              </Box>
            ) : (
              <>
                <Box className="status-card__row">
                  <Text className="status-card__label">Security Issues</Text>
                  <Flex align="center" gap="1">
                    {(healthCheck.securityIssueCount ?? 0) > 0 ? (
                      <>
                        <AlertTriangle size={14} color="var(--yellow-11)" />
                        <Text className="status-card__value">{healthCheck.securityIssueCount}</Text>
                      </>
                    ) : (
                      <>
                        <CheckCircle size={14} color="var(--green-11)" />
                        <Text className="status-card__value">None</Text>
                      </>
                    )}
                  </Flex>
                </Box>

                <Box className="status-card__row">
                  <Text className="status-card__label">License Issues</Text>
                  <Flex align="center" gap="1">
                    {(healthCheck.licenseIssueCount ?? 0) > 0 ? (
                      <>
                        <AlertTriangle size={14} color="var(--yellow-11)" />
                        <Text className="status-card__value">{healthCheck.licenseIssueCount}</Text>
                      </>
                    ) : (
                      <>
                        <CheckCircle size={14} color="var(--green-11)" />
                        <Text className="status-card__value">None</Text>
                      </>
                    )}
                  </Flex>
                </Box>

                {healthCheck.detailUrl && (
                  <Box className="status-card__row">
                    <Text className="status-card__label">Report</Text>
                    <a
                      href={healthCheck.detailUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="status-card__link"
                    >
                      <ExternalLink size={14} />
                      View Detailed Report
                    </a>
                  </Box>
                )}
              </>
            )}
          </Box>
        ) : (
          <Box className="profile-empty-state" style={{ padding: '20px' }}>
            <Text color="gray" size="2">
              Health Check is not enabled for this repository.
              Enable it to scan for security and license issues.
            </Text>
          </Box>
        )}
      </Box>
    </Box>
  );
}

export default SecurityTab;


