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

/**
 * Shared HelpMenu for both self-hosted and cloud Preview UI headers.
 *
 * Discipline: differences between distributions live in this file, at the point of
 * difference, guarded by useIsCloud() with a comment stating why. Default is to render
 * on both distributions. Silent absence is how the pre-consolidation regression hid
 * for months (NEXUS-54847) - prefer a visible mistake over a silent one.
 *
 * Deliberate differences:
 * - Version label: self-hosted only. Cloud tenants do not choose a version, so
 *   showing one invites questions the customer cannot act on.
 * - Documentation link: self-hosted appends major.minor to /docs/, cloud uses the
 *   unversioned /docs endpoint for the same reason.
 * - Release Notes, Knowledge Base, Guides, Community, Issue Tracker, About, Support:
 *   rendered on both distributions.
 */

import React, {useState, useEffect} from 'react';
import {DropdownMenu, Flex, IconButton, Text} from '@radix-ui/themes';
import {HelpCircle, ExternalLink} from 'lucide-react';
import {ExtJS} from '../../../../interface/ExtJS';
import {Tooltip, usePortalContainer} from '../Tooltip';
import {useIsCloud} from '../hooks';
import {AboutModal} from '../AboutModal';
import {getEditionDisplayName, EDITION_ID_STATE_KEY} from '../../../../interface/editionUtils';

export const DocumentationUTMparams = {
  utm_medium: 'product',
  utm_source: 'nexus_repo',
  utm_campaign: 'menu-docs'
};
export const ReleaseNotesUTMparams = {
  utm_medium: 'product',
  utm_source: 'nexus_repo',
  utm_campaign: 'menu-release-notes'
};
export const KnowledgeBaseUTMparams = {
  utm_medium: 'product',
  utm_source: 'nexus_repo',
  utm_campaign: 'menu-knowledge'
};
export const SonatypeGuidesUTMparams = {
  utm_medium: 'product',
  utm_source: 'nexus_repo',
  utm_campaign: 'menu-guides'
};
export const CommunityUTMparams = {
  utm_medium: 'product',
  utm_source: 'nexus_repo',
  utm_campaign: 'menu-community'
};
export const IssueTrackerUTMparams = {
  utm_medium: 'product',
  utm_source: 'nexus_repo',
  utm_campaign: 'menu-issuetracker'
};
export const SupportUTMparams = {
  utm_medium: 'product',
  utm_source: 'nexus_repo',
  utm_campaign: 'menu-support'
};

const baseUrl = 'https://links.sonatype.com/products/nexus';

function buildUrl(path, utmParams) {
  const params = new URLSearchParams(utmParams);
  return `${baseUrl}/${path}?${params.toString()}`;
}

export default function HelpMenu() {
  const portalContainer = usePortalContainer();
  const isCloud = useIsCloud();

  // Regular useState (not ExtJS.useState) to avoid hooks issues during logout.
  const [version, setVersion] = useState('');
  const [edition, setEdition] = useState('');
  const [buildRevision, setBuildRevision] = useState('');
  const [buildTimestamp, setBuildTimestamp] = useState('');
  const [aboutOpen, setAboutOpen] = useState(false);

  useEffect(() => {
    try {
      const state = ExtJS.state();
      if (state) {
        const status = state.getValue('status') || {};
        setVersion(status.version || '');
        // status.edition is the backend short name (PROPLUS); map the edition id to its display name instead.
        setEdition(getEditionDisplayName(state.getValue(EDITION_ID_STATE_KEY)));
        setBuildRevision(status.buildRevision || '');
        setBuildTimestamp(status.buildTimestamp || '');
      }
    } catch (e) {
      setVersion('');
      setEdition('');
      setBuildRevision('');
      setBuildTimestamp('');
    }
  }, []);

  const getVersionMajorMinor = () => {
    const match = version.match(/^(\d+\.\d+)/);
    return match ? match[1] : '';
  };

  const versionSegment = getVersionMajorMinor();
  const documentationPath = isCloud || !versionSegment ? 'docs' : `docs/${versionSegment}`;
  const documentationUrl = buildUrl(documentationPath, DocumentationUTMparams);
  const releaseNotesUrl = `https://links.sonatype.com/products/nxrm3/release-notes?${new URLSearchParams(ReleaseNotesUTMparams).toString()}`;
  const knowledgeBaseUrl = buildUrl('kb', KnowledgeBaseUTMparams);
  const guidesUrl = `https://links.sonatype.com/products/nxrm3/guides?${new URLSearchParams(SonatypeGuidesUTMparams).toString()}`;
  const communityUrl = buildUrl('community', CommunityUTMparams);
  const issueTrackerUrl = buildUrl('issues', IssueTrackerUTMparams);
  const supportUrl = buildUrl('support', SupportUTMparams);

  return (
    <>
    <DropdownMenu.Root>
      <Tooltip content="Help & Documentation">
        <DropdownMenu.Trigger>
          <IconButton variant="outline" size="2" color="gray" aria-label="Help & Documentation" data-analytics-id="nxrm-header-help">
            <HelpCircle size={16} />
          </IconButton>
        </DropdownMenu.Trigger>
      </Tooltip>

      <DropdownMenu.Content align="end" container={portalContainer} color="gray" variant="soft">
        <DropdownMenu.Item onSelect={() => setAboutOpen(true)} data-analytics-id="nxrm-global-header-help-about">
          <Flex align="center" justify="start" gap="3">
            <Text size="2">About</Text>
          </Flex>
        </DropdownMenu.Item>

        <DropdownMenu.Item asChild data-analytics-id="nxrm-global-header-help-documentation">
          <a href={documentationUrl} target="_blank" rel="noopener noreferrer">
            <Flex align="center" justify="start" gap="3">
              <Text size="2">Documentation</Text>
              <ExternalLink size={16} />
            </Flex>
          </a>
        </DropdownMenu.Item>

        <DropdownMenu.Item asChild data-analytics-id="nxrm-global-header-help-release-notes">
          <a href={releaseNotesUrl} target="_blank" rel="noopener noreferrer">
            <Flex align="center" justify="start" gap="3">
              <Text size="2">Release Notes</Text>
              <ExternalLink size={16} />
            </Flex>
          </a>
        </DropdownMenu.Item>

        <DropdownMenu.Item asChild data-analytics-id="nxrm-global-header-help-knowledge-base">
          <a href={knowledgeBaseUrl} target="_blank" rel="noopener noreferrer">
            <Flex align="center" justify="start" gap="3">
              <Text size="2">Knowledge Base</Text>
              <ExternalLink size={16} />
            </Flex>
          </a>
        </DropdownMenu.Item>

        <DropdownMenu.Item asChild data-analytics-id="nxrm-global-header-help-guides">
          <a href={guidesUrl} target="_blank" rel="noopener noreferrer">
            <Flex align="center" justify="start" gap="3">
              <Text size="2">Sonatype Guides</Text>
              <ExternalLink size={16} />
            </Flex>
          </a>
        </DropdownMenu.Item>

        <DropdownMenu.Separator />

        <DropdownMenu.Item asChild data-analytics-id="nxrm-global-header-help-community">
          <a href={communityUrl} target="_blank" rel="noopener noreferrer">
            <Flex align="center" justify="start" gap="3">
              <Text size="2">Community</Text>
              <ExternalLink size={16} />
            </Flex>
          </a>
        </DropdownMenu.Item>

        <DropdownMenu.Item asChild data-analytics-id="nxrm-global-header-help-issue-tracker">
          <a href={issueTrackerUrl} target="_blank" rel="noopener noreferrer">
            <Flex align="center" justify="start" gap="3">
              <Text size="2">Issue Tracker</Text>
              <ExternalLink size={16} />
            </Flex>
          </a>
        </DropdownMenu.Item>

        <DropdownMenu.Separator />

        <DropdownMenu.Item asChild data-analytics-id="nxrm-global-header-help-support">
          <a href={supportUrl} target="_blank" rel="noopener noreferrer">
            <Flex align="center" justify="start" gap="3">
              <Text size="2">Support</Text>
              <ExternalLink size={16} />
            </Flex>
          </a>
        </DropdownMenu.Item>

        {!isCloud && (
          <>
            <DropdownMenu.Separator />
            <DropdownMenu.Label>
              <Text size="1" color="gray">
                Version: {version}
              </Text>
            </DropdownMenu.Label>
          </>
        )}
      </DropdownMenu.Content>
    </DropdownMenu.Root>

    <AboutModal
      open={aboutOpen}
      onOpenChange={setAboutOpen}
      isCloud={isCloud}
      version={version}
      edition={edition}
      buildRevision={buildRevision}
      buildTimestamp={buildTimestamp}
    />
    </>
  );
}
