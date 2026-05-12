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


const navigateTo = (path: string) => {
  window.location.hash = path;
}


import React from 'react';
import { Box, Flex, Text, Heading, Card, Badge } from '@radix-ui/themes';
import { ArrowUpCircle, ExternalLink, CheckCircle, Info, BookOpen } from 'lucide-react';
import { ExtJS } from '@sonatype/nexus-ui-plugin';

import { SettingsButton, SettingsAlert } from '../../../shared/form';

import './UpgradePage.scss';

interface UpgradePageProps {
  className?: string;
}

/**
 * UpgradePage - Upgrade information page for Preview UI
 *
 * Displays current version and upgrade information.
 */
export function UpgradePage({ className }: UpgradePageProps) {
  const status = ExtJS.useStatus();
  const license = ExtJS.useLicense();

  const currentVersion = status?.version || 'Unknown';
  const edition = status?.edition || 'OSS';
  const isPro = edition === 'PRO';

  return (
    <Box className={`upgrade-page ${className || ''}`.trim()}>
      {/* Header */}
      <Flex align="center" gap="3" className="upgrade-page__header">
        <ArrowUpCircle size={24} className="upgrade-page__icon" />
        <Box>
          <Heading as="h1" size="6" weight="medium">Upgrade</Heading>
          <Text size="2" className="upgrade-page__description">
            Version information and upgrade options
          </Text>
        </Box>
      </Flex>

      {/* Current Version Card */}
      <Card className="upgrade-page__version-card">
        <Flex align="center" justify="between">
          <Box>
            <Text size="1" className="upgrade-page__version-label">Current Version</Text>
            <Heading as="h2" size="5" weight="bold" className="upgrade-page__version">
              Nexus Repository {currentVersion}
            </Heading>
            <Flex align="center" gap="2" className="upgrade-page__edition">
              <Badge 
                color={isPro ? 'blue' : 'gray'} 
                variant="soft"
                size="2"
              >
                {isPro ? 'Professional Edition' : 'OSS Edition'}
              </Badge>
              {license?.daysToExpiry !== undefined && license.daysToExpiry > 0 && (
                <Badge color="green" variant="outline" size="1">
                  License valid for {license.daysToExpiry} days
                </Badge>
              )}
            </Flex>
          </Box>
          <CheckCircle size={48} className="upgrade-page__check-icon" />
        </Flex>
      </Card>

      {/* Upgrade Information */}
      <Box className="upgrade-page__info-section">
        <Heading as="h3" size="3" weight="medium" className="upgrade-page__section-title">
          Upgrade Information
        </Heading>

        <SettingsAlert type="info">
          <Flex direction="column" gap="2">
            <Text size="2">
              Before upgrading, please review the release notes and upgrade guide for important
              information about new features, breaking changes, and migration steps.
            </Text>
          </Flex>
        </SettingsAlert>

        <Flex direction="column" gap="3" className="upgrade-page__links">
          <Card className="upgrade-page__link-card">
            <Flex align="center" gap="3">
              <BookOpen size={20} className="upgrade-page__link-icon" />
              <Box className="upgrade-page__link-content">
                <Text size="2" weight="medium">Release Notes</Text>
                <Text size="1" className="upgrade-page__link-description">
                  View detailed release notes for the latest version
                </Text>
              </Box>
              <SettingsButton
                variant="ghost"
                onClick={() => window.open('http://links.sonatype.com/products/nxrm3/release-notes', '_blank')}
              >
                View <ExternalLink size={14} />
              </SettingsButton>
            </Flex>
          </Card>

          <Card className="upgrade-page__link-card">
            <Flex align="center" gap="3">
              <ArrowUpCircle size={20} className="upgrade-page__link-icon" />
              <Box className="upgrade-page__link-content">
                <Text size="2" weight="medium">Upgrade Guide</Text>
                <Text size="1" className="upgrade-page__link-description">
                  Step-by-step instructions for upgrading Nexus Repository
                </Text>
              </Box>
              <SettingsButton
                variant="ghost"
                onClick={() => window.open('http://links.sonatype.com/products/nxrm3/docs/upgrade', '_blank')}
              >
                View <ExternalLink size={14} />
              </SettingsButton>
            </Flex>
          </Card>

          <Card className="upgrade-page__link-card">
            <Flex align="center" gap="3">
              <Info size={20} className="upgrade-page__link-icon" />
              <Box className="upgrade-page__link-content">
                <Text size="2" weight="medium">Downloads</Text>
                <Text size="1" className="upgrade-page__link-description">
                  Download the latest version of Nexus Repository
                </Text>
              </Box>
              <SettingsButton
                variant="ghost"
                onClick={() => window.open('http://links.sonatype.com/products/nxrm3/download', '_blank')}
              >
                Download <ExternalLink size={14} />
              </SettingsButton>
            </Flex>
          </Card>
        </Flex>
      </Box>

      {/* Pro Edition Upgrade (for OSS users) */}
      {!isPro && (
        <Card className="upgrade-page__pro-card">
          <Flex align="start" gap="4">
            <Box className="upgrade-page__pro-icon-wrapper">
              <ArrowUpCircle size={32} />
            </Box>
            <Box className="upgrade-page__pro-content">
              <Heading as="h3" size="4" weight="medium">
                Upgrade to Professional Edition
              </Heading>
              <Text size="2" className="upgrade-page__pro-description">
                Get advanced features like staging, LDAP/SSO integration, high availability,
                and premium support with Nexus Repository Professional.
              </Text>
              <SettingsButton
                variant="primary"
                onClick={() => window.open('http://links.sonatype.com/products/nxrm3/pro', '_blank')}
                className="upgrade-page__pro-button"
              >
                Learn More <ExternalLink size={14} />
              </SettingsButton>
            </Box>
          </Flex>
        </Card>
      )}
    </Box>
  );
}

export default UpgradePage;


