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

import React, {useState, useEffect} from 'react';
import {DropdownMenu, Flex, IconButton, Text} from '@radix-ui/themes';
import {Tooltip, usePortalContainer} from '@sonatype/nexus-ui-plugin';
import {HelpCircle, ExternalLink} from 'lucide-react';
import {ExtJS} from '@sonatype/nexus-ui-plugin';

const DocumentationUTMparams = {
  utm_medium: 'product',
  utm_source: 'nexus_repo',
  utm_campaign: 'menu-docs'
};
const ReleaseNotesUTMparams = {
  utm_medium: 'product',
  utm_source: 'nexus_repo',
  utm_campaign: 'menu-release-notes'
};
const KnowledgeBaseUTMparams = {
  utm_medium: 'product',
  utm_source: 'nexus_repo',
  utm_campaign: 'menu-knowledge'
};
const SonatypeGuidesUTMparams = {
  utm_medium: 'product',
  utm_source: 'nexus_repo',
  utm_campaign: 'menu-guides'
};
const CommunityUTMparams = {
  utm_medium: 'product',
  utm_source: 'nexus_repo',
  utm_campaign: 'menu-community'
};
const IssueTrackerUTMparams = {
  utm_medium: 'product',
  utm_source: 'nexus_repo',
  utm_campaign: 'menu-issuetracker'
};

const baseUrl = 'https://links.sonatype.com/products/nexus';

function buildUrl(path, utmParams) {
  const params = new URLSearchParams(utmParams);
  return `${baseUrl}/${path}?${params.toString()}`;
}

export default function HelpMenuRadix() {
  const portalContainer = usePortalContainer();
  // Use regular useState instead of ExtJS.useState to avoid hooks issues during logout
  const [version, setVersion] = useState('');

  useEffect(() => {
    try {
      const state = ExtJS.state();
      if (state) {
        setVersion(state.getValue('status')?.version || '');
      }
    } catch (e) {
      setVersion('');
    }
  }, []);

  const getVersionMajorMinor = () => {
    const match = version.match(/^(\d+\.\d+)/);
    return match ? match[1] : '';
  };

  const documentationUrl = buildUrl('docs/' + getVersionMajorMinor(), DocumentationUTMparams);
  const releaseNotesUrl = 'https://links.sonatype.com/products/nxrm3/release-notes?' + new URLSearchParams(ReleaseNotesUTMparams).toString();
  const knowledgeBaseUrl = buildUrl('kb', KnowledgeBaseUTMparams);
  const guidesUrl = 'https://links.sonatype.com/products/nxrm3/guides?' + new URLSearchParams(SonatypeGuidesUTMparams).toString();
  const communityUrl = buildUrl('community', CommunityUTMparams);
  const issueTrackerUrl = buildUrl('issues', IssueTrackerUTMparams);

  return (
    <DropdownMenu.Root>
      <Tooltip content="Help & Documentation">
        <DropdownMenu.Trigger>
          <IconButton variant="outline" size="2" color="gray" aria-label="Help & Documentation">
            <HelpCircle size={16} />
          </IconButton>
        </DropdownMenu.Trigger>
      </Tooltip>

      <DropdownMenu.Content align="end" container={portalContainer} color="gray" variant="soft">
        <DropdownMenu.Item asChild>
          <a href={documentationUrl} target="_blank" rel="noopener noreferrer">
            <Flex align="center" justify="start" gap="3">
              <Text size="2">Documentation</Text>
              <ExternalLink size={16} />
            </Flex>
          </a>
        </DropdownMenu.Item>

        <DropdownMenu.Item asChild>
          <a href={releaseNotesUrl} target="_blank" rel="noopener noreferrer">
            <Flex align="center" justify="start" gap="3">
              <Text size="2">Release Notes</Text>
              <ExternalLink size={16} />
            </Flex>
          </a>
        </DropdownMenu.Item>

        <DropdownMenu.Item asChild>
          <a href={knowledgeBaseUrl} target="_blank" rel="noopener noreferrer">
            <Flex align="center" justify="start" gap="3">
              <Text size="2">Knowledge Base</Text>
              <ExternalLink size={16} />
            </Flex>
          </a>
        </DropdownMenu.Item>

        <DropdownMenu.Item asChild>
          <a href={guidesUrl} target="_blank" rel="noopener noreferrer">
            <Flex align="center" justify="start" gap="3">
              <Text size="2">Sonatype Guides</Text>
              <ExternalLink size={16} />
            </Flex>
          </a>
        </DropdownMenu.Item>

        <DropdownMenu.Separator />

        <DropdownMenu.Item asChild>
          <a href={communityUrl} target="_blank" rel="noopener noreferrer">
            <Flex align="center" justify="start" gap="3">
              <Text size="2">Community</Text>
              <ExternalLink size={16} />
            </Flex>
          </a>
        </DropdownMenu.Item>

        <DropdownMenu.Item asChild>
          <a href={issueTrackerUrl} target="_blank" rel="noopener noreferrer">
            <Flex align="center" justify="start" gap="3">
              <Text size="2">Issue Tracker</Text>
              <ExternalLink size={16} />
            </Flex>
          </a>
        </DropdownMenu.Item>

        <DropdownMenu.Separator />

        <DropdownMenu.Label>
          <Text size="1" color="gray">
            Version: {version}
          </Text>
        </DropdownMenu.Label>
      </DropdownMenu.Content>
    </DropdownMenu.Root>
  );
}





