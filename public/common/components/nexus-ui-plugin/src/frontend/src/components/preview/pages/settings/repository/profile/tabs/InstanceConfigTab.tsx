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

import React, { useCallback, useState } from 'react';
import { Box, Flex, Text, Code, Card, Grid, Badge, Button, Separator, Tooltip, IconButton } from '@radix-ui/themes';
import {
  Shield,
  Info,
  ExternalLink,
  CheckCircle,
  XCircle,
  Zap,
  Copy,
  Check,
} from 'lucide-react';
import { ClassicSettingsLink } from './classicSettingsLink';
import type {
  IqCapabilities,
  CapabilityInfo,
} from '../types';

interface InstanceConfigTabProps {
  iqCapabilities: IqCapabilities | null;
  capabilities: CapabilityInfo[];
}

/**
 * InstanceConfigTab - Read-only view of instance-level configurations (IQ, Health Check, Firewall)
 */
export function InstanceConfigTab({
  iqCapabilities,
  capabilities,
}: InstanceConfigTabProps): JSX.Element {
  const [deploymentIdCopied, setDeploymentIdCopied] = useState(false);

  const healthCheckCapability = capabilities.find(c => c.type === 'healthcheck');
  const isHealthCheckConfigured = !!healthCheckCapability;
  const isHealthCheckEnabled = healthCheckCapability?.enabled ?? false;

  const deploymentId = iqCapabilities?.deploymentId?.trim() ?? '';
  const showFirewallManagementLink =
    Boolean(
      iqCapabilities?.hasFirewall &&
        iqCapabilities?.connected &&
        iqCapabilities?.url &&
        deploymentId
    );

  const firewallManagementUrl =
    showFirewallManagementLink && iqCapabilities?.url
      ? `${iqCapabilities.url.replace(/\/+$/, '')}/assets/index.html#/firewall/management/view/repository_manager/${deploymentId}`
      : '';

  const copyDeploymentId = useCallback(async () => {
    if (!deploymentId) {
      return;
    }
    try {
      await navigator.clipboard.writeText(deploymentId);
      setDeploymentIdCopied(true);
      window.setTimeout(() => setDeploymentIdCopied(false), 2000);
    } catch {
      // Clipboard may be unavailable (e.g. non-secure context)
    }
  }, [deploymentId]);

  const openFirewallManagement = useCallback(() => {
    if (firewallManagementUrl) {
      window.open(firewallManagementUrl, '_blank');
    }
  }, [firewallManagementUrl]);

  return (
    <Box>
      <Grid columns="2" gap="6">
        {/* IQ Server Connection */}
        <Card size="2">
          <Flex direction="column" gap="3">
            <Flex align="center" justify="between">
              <Flex align="center" gap="2">
                <Shield size={18} color="var(--accent-9)" />
                <Text weight="bold">IQ Server Connection</Text>
              </Flex>
              <ClassicSettingsLink previewPath="preview/admin/iq" label="Configure" />
            </Flex>
            <Separator size="4" />
            
            <Flex direction="column" gap="2">
              <Flex align="center" justify="between">
                <Text size="2" color="gray">Status</Text>
                {iqCapabilities?.connected ? (
                  <Badge color="green" variant="soft">
                    <Flex align="center" gap="1">
                      <CheckCircle size={12} /> Connected
                    </Flex>
                  </Badge>
                ) : (
                  <Badge color="red" variant="soft">
                    <Flex align="center" gap="1">
                      <XCircle size={12} /> Disconnected
                    </Flex>
                  </Badge>
                )}
              </Flex>
              
              <Flex align="center" justify="between">
                <Text size="2" color="gray">URL</Text>
                <Text size="2" style={{ fontFamily: 'var(--code-font-family)' }}>
                  {iqCapabilities?.url || 'Not configured'}
                </Text>
              </Flex>

              {iqCapabilities?.connected && (
                <Flex align="center" justify="between" gap="2">
                  <Text size="2" color="gray">Repository Manager ID</Text>
                  <Flex align="center" gap="2" style={{ minWidth: 0 }}>
                    <Code size="2" variant="ghost" style={{ overflow: 'hidden', textOverflow: 'ellipsis' }}>
                      {deploymentId || '—'}
                    </Code>
                    {deploymentId ? (
                      <Tooltip content={deploymentIdCopied ? 'Copied!' : 'Copy'}>
                        <IconButton
                          variant="ghost"
                          size="1"
                          onClick={copyDeploymentId}
                          aria-label="Copy Repository Manager ID"
                        >
                          {deploymentIdCopied ? (
                            <Check size={14} color="var(--green-9)" />
                          ) : (
                            <Copy size={14} />
                          )}
                        </IconButton>
                      </Tooltip>
                    ) : null}
                  </Flex>
                </Flex>
              )}

              {showFirewallManagementLink && (
                <Flex justify="end" mt="1">
                  <Button variant="soft" size="2" onClick={openFirewallManagement}>
                    <ExternalLink size={14} />
                    Open Firewall Management
                  </Button>
                </Flex>
              )}
            </Flex>

            <Box mt="2">
              <Text size="2" weight="medium" mb="2" display="block">License Features</Text>
              <Grid columns="2" gap="2">
                <Card variant="surface" size="1">
                  <Flex direction="column" gap="1">
                    <Text size="1" color="gray">Lifecycle</Text>
                    <Flex align="center" gap="2">
                      {iqCapabilities?.hasLifecycle ? (
                        <CheckCircle size={14} color="var(--green-9)" />
                      ) : (
                        <XCircle size={14} color="var(--gray-7)" />
                      )}
                      <Text size="2">{iqCapabilities?.hasLifecycle ? 'Active' : 'Not Available'}</Text>
                    </Flex>
                    {!iqCapabilities?.hasLifecycle && (
                      <a href="https://links.sonatype.com/products/nxrm3/browse/lc-learn" target="_blank" rel="noopener" className="iq-server-page__link" style={{ fontSize: '10px' }}>
                        Learn More <ExternalLink size={8} />
                      </a>
                    )}
                  </Flex>
                </Card>
                <Card variant="surface" size="1">
                  <Flex direction="column" gap="1">
                    <Text size="1" color="gray">Firewall</Text>
                    <Flex align="center" gap="2">
                      {iqCapabilities?.hasFirewall ? (
                        <CheckCircle size={14} color="var(--green-9)" />
                      ) : (
                        <XCircle size={14} color="var(--gray-7)" />
                      )}
                      <Text size="2">{iqCapabilities?.hasFirewall ? 'Active' : 'Not Available'}</Text>
                    </Flex>
                    {!iqCapabilities?.hasFirewall && (
                      <a href="https://links.sonatype.com/nexus-repository-firewall" target="_blank" rel="noopener" className="iq-server-page__link" style={{ fontSize: '10px' }}>
                        Learn More <ExternalLink size={8} />
                      </a>
                    )}
                  </Flex>
                </Card>
              </Grid>
            </Box>
          </Flex>
        </Card>

        {/* Global Capabilities */}
        <Card size="2">
          <Flex direction="column" gap="3">
            <Flex align="center" justify="between">
              <Flex align="center" gap="2">
                <Zap size={18} color="var(--yellow-9)" />
                <Text weight="bold">Global Capabilities</Text>
              </Flex>
              <ClassicSettingsLink previewPath="preview/admin/system/capabilities" label="Manage" />
            </Flex>
            <Separator size="4" />

            <Flex direction="column" gap="4">
              {/* Health Check Instance Config */}
              <Box>
                <Flex align="center" justify="between" mb="1">
                  <Text size="2" weight="medium">Repository Health Check</Text>
                  {isHealthCheckConfigured ? (
                    <Badge color={isHealthCheckEnabled ? 'green' : 'yellow'} variant="soft">
                      {isHealthCheckEnabled ? 'Active' : 'Disabled'}
                    </Badge>
                  ) : (
                    <Badge color="gray" variant="soft">Not Configured</Badge>
                  )}
                </Flex>
                <Text size="1" color="gray">
                  Instance-level capability to analyze proxy repositories for security and license issues.
                </Text>
              </Box>

              {/* Firewall Instance Config */}
              <Box>
                <Flex align="center" justify="between" mb="1">
                  <Text size="2" weight="medium">Repository Firewall</Text>
                  {iqCapabilities?.hasFirewall ? (
                    <Badge color="green" variant="soft">Available</Badge>
                  ) : (
                    <Badge color="gray" variant="soft">Requires License</Badge>
                  )}
                </Flex>
                <Text size="1" color="gray">
                  Keep bad code out of your repository. Blocks malicious and vulnerable components before they ever enter your repo.
                </Text>
              </Box>
            </Flex>

            <Box mt="auto" pt="2">
              <Card variant="ghost" size="1" style={{ backgroundColor: 'var(--gray-2)' }}>
                <Flex gap="2">
                  <Info size={14} color="var(--gray-9)" />
                  <Text size="1" color="gray">
                    These settings affect all supported repositories across the entire Nexus Repository instance.
                  </Text>
                </Flex>
              </Card>
            </Box>
          </Flex>
        </Card>
      </Grid>
    </Box>
  );
}

export default InstanceConfigTab;
