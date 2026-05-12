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

import React, { useState, useCallback, useMemo } from 'react';
import { useRouter, useCurrentStateAndParams } from '@uirouter/react';
import {
  Badge,
  Box,
  Flex,
  Grid,
  Heading,
  Text,
  Tabs,
  Spinner,
  Callout,
  Button,
  ScrollArea,
  Tooltip,
} from '@radix-ui/themes';
import { ArrowLeft, Copy, ExternalLink, Package } from 'lucide-react';

import type { GADetailTab } from '../core';
import {
  GA_SEARCH_ROUTE_NAMES,
  TAB_ROUTE_MAP,
  getTabFromRoute,
  parseGaId,
} from '../core';

import { Breadcrumbs } from './Breadcrumbs';
import { buildComponentPath, getMavenCentralUrl } from './detailHelpers';
import { buildGuideComponentUrlFromGaId } from '@/utils/guideIntegration';
import { useGADetail } from './useGADetail';
import { useComponentSecurity } from './useComponentSecurity';
import GAOverviewTab from './GAOverviewTab';
import GAVersionsTab from './GAVersionsTab';
import GARepositoriesTab from './GARepositoriesTab';
import GAFilesTab from './GAFilesTab';
import GASecurityTab from './GASecurityTab';

interface GADetailPageProps {
  /** Encoded GA identifier from route params */
  gaId: string;
  /** Selected version from query params (for files/security tabs) */
  version?: string;
}

/**
 * GADetailPage - Main container for GA detail view with tabs.
 *
 * Tab navigation is URL-driven via router.
 * Files and Security tabs require a version to be selected.
 */
export function GADetailPage({ gaId: encodedGaId, version }: GADetailPageProps) {
  const router = useRouter();
  const { params: routeParams } = useCurrentStateAndParams();
  /** Live $stateParams — UIView resolve props can stay stale when only `version` changes (UIView useMemo deps). */
  const initialVersion = useMemo(() => {
    const fromRoute = routeParams?.version as string | null | undefined;
    if (fromRoute != null && fromRoute !== '') {
      return fromRoute;
    }
    return version;
  }, [routeParams, version]);

  const gaId = parseGaId(encodedGaId || '');
  const [copiedPath, setCopiedPath] = useState(false);

  const {
    detail,
    selectedVersion,
    assets,
    loading,
    assetsLoading,
    error,
    selectVersion,
  } = useGADetail({ gaId, initialVersion });

  const securityResult = useComponentSecurity({ gaId, version: selectedVersion });
  const securityCount =
    securityResult.data &&
    (securityResult.data.criticalCount +
      securityResult.data.severeCount +
      securityResult.data.moderateCount +
      securityResult.data.lowCount);

  const handleCopyPath = useCallback(() => {
    const ver =
      selectedVersion || initialVersion || detail?.versions[0]?.version;
    if (!ver) return;
    const path = buildComponentPath(gaId, ver);
    if (path) {
      navigator.clipboard.writeText(path);
      setCopiedPath(true);
      setTimeout(() => setCopiedPath(false), 2000);
    }
  }, [gaId, selectedVersion, initialVersion, detail?.versions]);

  // Get current tab from route name
  const currentTab = getTabFromRoute(router.globals.current?.name || '');

  // Handle tab change via router
  const handleTabChange = (tab: string) => {
    const routeName = TAB_ROUTE_MAP[tab as GADetailTab];
    if (routeName) {
      router.stateService.go(routeName, { 
        gaId: encodedGaId, 
        version: selectedVersion || undefined,
      });
    }
  };

  /**
   * Selecting a version updates the header (and downstream tabs). Stay on the current
   * component-detail child state when possible so the Versions tab does not jump to Overview.
   * Parent-only `preview.browse.search.component` has no tab segment — use Overview there.
   */
  const handleVersionSelect = (versionStr: string) => {
    selectVersion(versionStr);
    const currentName = router.globals.current?.name ?? '';
    const isNestedComponentTab =
      currentName.startsWith(`${GA_SEARCH_ROUTE_NAMES.MAVEN_DETAIL}.`) &&
      currentName !== GA_SEARCH_ROUTE_NAMES.MAVEN_DETAIL;
    const targetState = isNestedComponentTab
      ? currentName
      : GA_SEARCH_ROUTE_NAMES.MAVEN_DETAIL_OVERVIEW;
    router.stateService.go(targetState, {
      gaId: encodedGaId,
      version: versionStr,
    });
  };

  // Handle back navigation - go to unified search, not Maven search
  const handleBackClick = () => {
    router.stateService.go('preview.browse.search.unified');
  };

  // Navigate to unified search; stored search state is restored by UnifiedSearchPage on mount
  const handleBreadcrumbSearchClick = useCallback(() => {
    router.stateService.go('preview.browse.search.unified');
  }, [router]);

  if (loading) {
    return (
      <Box p="6">
        <Flex justify="center" align="center" style={{ minHeight: '400px' }}>
          <Flex direction="column" align="center" gap="3">
            <Spinner size="3" />
            <Text color="gray">Loading component details...</Text>
          </Flex>
        </Flex>
      </Box>
    );
  }

  if (error || !detail) {
    return (
      <Box p="6">
        <Button variant="ghost" onClick={handleBackClick} mb="4">
          <ArrowLeft size={16} />
          Back to Search
        </Button>
        <Callout.Root color="red">
          <Callout.Icon>
            <Package size={16} />
          </Callout.Icon>
          <Callout.Text>{error || 'Component not found'}</Callout.Text>
        </Callout.Root>
      </Box>
    );
  }

  const displayName = detail.displayName;
  const ver =
    selectedVersion || initialVersion || detail.versions[0]?.version || '';
  const mavenCentralUrl = getMavenCentralUrl(gaId);
  const componentPath = ver ? buildComponentPath(gaId, ver) : null;

  return (
    <ScrollArea scrollbars="vertical" style={{ height: '100%' }}>
      <Box px="4" pt="0" pb="6">
        <Box maxWidth="1280px" mx="auto" width="100%" pb="9">
          {/* Breadcrumbs */}
          <Breadcrumbs
            items={[
              {
                label: 'Search',
                onClick: handleBreadcrumbSearchClick,
              },
              {
                label: detail.displayName,
              },
              {
                label: ver,
              },
            ]}
          />

          {/* Component Header */}
          <Box p="4">
            <Grid columns={{ initial: '1', md: '1fr auto' }} gap="6" align="start">
              <Flex direction="column" gap="3">
                <Heading size="6">
                  {displayName} v{ver}
                </Heading>
                <Flex align="end" gap="4" wrap="wrap">
                  {componentPath && (
                    <Tooltip content={copiedPath ? 'Copied!' : 'Copy path'}>
                      <Button
                        variant="soft"
                        size="2"
                        color="blue"
                        onClick={handleCopyPath}
                      >
                        <Copy size={14} />
                        Path
                      </Button>
                    </Tooltip>
                  )}
                  {mavenCentralUrl && (
                    <Button
                      variant="soft"
                      size="2"
                      color="gray"
                      asChild
                    >
                      <a href={mavenCentralUrl} target="_blank" rel="noopener noreferrer">
                        <ExternalLink size={14} />
                        Registry
                      </a>
                    </Button>
                  )}
                  {ver &&
                    detail &&
                    buildGuideComponentUrlFromGaId(gaId, ver) != null && (
                    <Button
                      variant="soft"
                      size="2"
                      onClick={() => {
                        const guideUrl = buildGuideComponentUrlFromGaId(
                          gaId,
                          ver,
                          'search-component-detail'
                        );
                        if (guideUrl) {
                          window.open(guideUrl, '_blank', 'noopener,noreferrer');
                        }
                      }}
                    >
                      <ExternalLink size={14} />
                      Research in Guide
                    </Button>
                  )}
                </Flex>
              </Flex>
            </Grid>
          </Box>

          {/* Tabs */}
          <Tabs.Root value={currentTab} onValueChange={handleTabChange}>
              <Box px="4" pt="0" pb="2">
                <Tabs.List size="2" style={{ borderBottom: 'none' }}>
                  <Tabs.Trigger value="overview">Overview</Tabs.Trigger>
                  <Tabs.Trigger value="versions">
                    <Flex align="center" gap="2">
                      Versions
                      <Badge color="gray" variant="soft" size="1" radius="full">
                        {detail.versions.length}
                      </Badge>
                    </Flex>
                  </Tabs.Trigger>
                  <Tabs.Trigger value="repositories">
                    <Flex align="center" gap="2">
                      Repositories
                      <Badge color="gray" variant="soft" size="1" radius="full">
                        {detail.repositories.length}
                      </Badge>
                    </Flex>
                  </Tabs.Trigger>
                  <Tabs.Trigger value="files">
                    <Flex align="center" gap="2">
                      Files
                      <Badge color="gray" variant="soft" size="1" radius="full">
                        {selectedVersion ? assets.length : 0}
                      </Badge>
                    </Flex>
                  </Tabs.Trigger>
                  <Tabs.Trigger value="security">
                    <Flex align="center" gap="2">
                      Security
                      {typeof securityCount === 'number' && (
                        <Badge color="gray" variant="soft" size="1" radius="full">
                          {securityCount}
                        </Badge>
                      )}
                    </Flex>
                  </Tabs.Trigger>
                </Tabs.List>
              </Box>

              <Box p="4">
                <Tabs.Content value="overview">
                  <GAOverviewTab
                    detail={detail}
                    selectedVersion={selectedVersion || detail.versions[0]?.version || null}
                  />
                </Tabs.Content>

                <Tabs.Content value="versions">
                  <GAVersionsTab
                    versions={detail.versions}
                    selectedVersion={selectedVersion}
                    onVersionSelect={handleVersionSelect}
                  />
                </Tabs.Content>

                <Tabs.Content value="repositories">
                  <GARepositoriesTab
                    repositories={detail.repositories}
                    selectedVersion={selectedVersion}
                    versions={detail.versions}
                  />
                </Tabs.Content>

                <Tabs.Content value="files">
                  <GAFilesTab
                    assets={assets}
                    selectedVersion={selectedVersion}
                    loading={assetsLoading}
                  />
                </Tabs.Content>

                <Tabs.Content value="security">
                  <GASecurityTab
                    gaId={gaId}
                    selectedVersion={selectedVersion}
                    securityData={securityResult.data}
                    securityLoading={securityResult.loading}
                    securityError={securityResult.error}
                    iqConnected={securityResult.iqConnected}
                    onRefetch={securityResult.refetch}
                  />
                </Tabs.Content>
              </Box>
            </Tabs.Root>
        </Box>
      </Box>
    </ScrollArea>
  );
}

export default GADetailPage;

