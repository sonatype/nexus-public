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
import React, { useState, useEffect, useMemo } from 'react';
import { Box, Flex, ScrollArea, Separator } from '@radix-ui/themes';
import {
  LayoutDashboard,
  Search,
  FolderOpen,
  HardDriveUpload,
  Tags,
  Settings,
  ShieldAlert,
  ChevronDown,
  ChevronUp,
  FileCode,
} from 'lucide-react';
import { useSref, useIsActive, useTransitionHook } from '@uirouter/react';
import {
  FORMATS,
  NavItemBox,
  PreviewUIContext,
  useContextAwareRouteName,
  useIsPreviewUI,
  useRepositories,
} from '@sonatype/nexus-ui-plugin';
import { ROUTE_NAMES } from '../../routerConfig/routeNames/routeNames';
import { useDefaultAdminRouteName } from './useDefaultAdminRouteName';
import useSideNavbarCollapsedState from '../../hooks/useSideNavbarCollapsedState';

import './LeftNavigationMenuRadix.scss';

const CLASSIC_SEARCH_FORMATS = [
  { id: 'alpine', apiFormat: 'alpine', label: FORMATS.alpine.label, routeKey: 'ALPINE' },
  { id: 'apt', apiFormat: 'apt', label: FORMATS.apt.label, routeKey: 'APT' },
  { id: 'cargo', apiFormat: 'cargo', label: FORMATS.cargo.label, routeKey: 'CARGO' },
  { id: 'cocoapods', apiFormat: 'cocoapods', label: FORMATS.cocoapods.label, routeKey: 'COCOAPODS' },
  { id: 'composer', apiFormat: 'composer', label: FORMATS.composer.label, routeKey: 'COMPOSER' },
  { id: 'conan', apiFormat: 'conan', label: FORMATS.conan.label, routeKey: 'CONAN' },
  { id: 'conda', apiFormat: 'conda', label: FORMATS.conda.label, routeKey: 'CONDA' },
  { id: 'docker', apiFormat: 'docker', label: FORMATS.docker.label, routeKey: 'DOCKER' },
  { id: 'gitlfs', apiFormat: 'gitlfs', label: FORMATS.gitlfs.label, routeKey: 'GITLFS' },
  { id: 'go', apiFormat: 'go', label: FORMATS.go.label, routeKey: 'GOLANG' },
  { id: 'helm', apiFormat: 'helm', label: FORMATS.helm.label, routeKey: 'HELM' },
  { id: 'huggingface', apiFormat: 'huggingface', label: FORMATS.huggingface.label, routeKey: 'HUGGING_FACE' },
  { id: 'maven', apiFormat: 'maven2', label: FORMATS.maven.label, routeKey: 'MAVEN' },
  { id: 'npm', apiFormat: 'npm', label: FORMATS.npm.label, routeKey: 'NPM' },
  { id: 'nuget', apiFormat: 'nuget', label: FORMATS.nuget.label, routeKey: 'NUGET' },
  { id: 'p2', apiFormat: 'p2', label: FORMATS.p2.label, routeKey: 'P2' },
  { id: 'pypi', apiFormat: 'pypi', label: FORMATS.pypi.label, routeKey: 'PYPI' },
  { id: 'pub', apiFormat: 'pub', label: FORMATS.pub.label, routeKey: 'PUB' },
  { id: 'r', apiFormat: 'r', label: FORMATS.r.label, routeKey: 'R' },
  { id: 'raw', apiFormat: 'raw', label: FORMATS.raw.label, routeKey: 'RAW' },
  { id: 'rubygems', apiFormat: 'rubygems', label: FORMATS.rubygems.label, routeKey: 'RUBYGEMS' },
  { id: 'swift', apiFormat: 'swift', label: FORMATS.swift.label, routeKey: 'SWIFT' },
  { id: 'terraform', apiFormat: 'terraform', label: FORMATS.terraform.label, routeKey: 'TERRAFORM' },
  { id: 'yum', apiFormat: 'yum', label: FORMATS.yum.label, routeKey: 'YUM' },
];

/**
 * Local NavItem for Classic UI search submenu.
 * Uses shared hooks from nexus-ui-plugin.
 */
function SearchNavItem({
  name,
  text,
  icon: Icon,
  selectedState,
  params = {},
  isCollapsed,
}) {
  // Hooks must be called before any conditional returns (React Rules of Hooks)
  const contextAwareName = useContextAwareRouteName(name) || name;
  const contextAwareSelectedState = useContextAwareRouteName(selectedState);

  const sref = useSref(contextAwareName, params);
  const isActive = useIsActive(contextAwareSelectedState || contextAwareName);

  // Defensive check: ensure name is always a valid string for useSref
  if (!name) {
    console.warn('[SearchNavItem] name prop is required but received:', name);
    return null;
  }

  const content = (
    <a
      href={sref.href}
      onClick={sref.onClick}
      className={`guide-nav-item ${isActive ? 'guide-nav-item--active' : ''}`}
    >
      <Flex align="center" justify={isCollapsed ? 'center' : 'start'} gap="3" px="3" py="2">
        <span className="guide-nav-item__icon">
          {Icon && <Icon size={18} />}
        </span>
        {!isCollapsed && (
          <span className="guide-nav-item__text">{text}</span>
        )}
      </Flex>
    </a>
  );

  return content;
}

/**
 * Search format child item.
 * Visibility is determined by the parent component via availableFormats from REST API.
 */
function SearchFormatItem({ routeName, label }) {
  return (
    <SearchNavItem
      name={routeName}
      text={label}
      icon={null}
      params={{ keyword: null }}
      isCollapsed={false}
    />
  );
}

/**
 * Collapsible Search navigation for Classic UI.
 * Shows dynamically filtered format submenu based on configured repositories.
 */
export function SearchCollapsibleNav({ isCollapsed }) {
  const { BROWSE } = ROUTE_NAMES;
  const [isExpanded, setIsExpanded] = useState(false);

  // Check active states for parent search item
  const isGenericActive = useIsActive(BROWSE.SEARCH.GENERIC);
  const isCustomActive = useIsActive(BROWSE.SEARCH.CUSTOM);
  const isParentActive = isGenericActive || isCustomActive;

  const sref = useSref(BROWSE.SEARCH.GENERIC, { keyword: null });

  // Auto-expand when transitioning to a search route using UI-Router hook
  useTransitionHook('onSuccess', {}, (transition) => {
    const toState = transition.to();
    if (toState.name?.startsWith(BROWSE.SEARCH.ROOT)) {
      setIsExpanded(true);
    }
  });

  // Get formats from REST API
  const { availableFormats, loading } = useRepositories();

  // Sync React-detected formats to ExtJS BrowseableFormats controller
  useEffect(() => {
    if (loading || !availableFormats || availableFormats.size === 0) {
      return;
    }

    const app = window.Ext?.getApplication?.();
    const controller = app?.getController?.('NX.coreui.controller.BrowseableFormats');

    if (controller) {
      const formatData = Array.from(availableFormats).map(format => ({ id: format }));
      controller.setFormats(formatData);
    }
  }, [availableFormats, loading]);

  // Filter formats based on available repositories
  const visibleFormats = useMemo(() => {
    if (loading || !availableFormats) return [];

    return CLASSIC_SEARCH_FORMATS
      .filter(({ apiFormat }) => availableFormats.has(apiFormat))
      .map(({ id, label, routeKey }) => ({
        id,
        label,
        routeName: BROWSE.SEARCH[routeKey],
      }));
  }, [availableFormats, loading, BROWSE.SEARCH]);

  const handleParentClick = (e) => {
    if (isCollapsed) {
      return;
    }
    e.preventDefault();
    setIsExpanded(!isExpanded);
  };

  const content = (
    <div className={`search-collapsible ${isExpanded ? 'search-collapsible--expanded' : ''}`}>
      <a
        href={sref.href}
        onClick={handleParentClick}
        aria-expanded={isCollapsed ? undefined : isExpanded}
        className={`guide-nav-item ${isParentActive ? 'guide-nav-item--active' : ''}`}
      >
        <Flex align="center" justify={isCollapsed ? 'center' : 'space-between'} gap="3" px="3" py="2">
          <Flex align="center" gap="3">
            <span className="guide-nav-item__icon">
              <Search size={18} />
            </span>
            {!isCollapsed && <span className="guide-nav-item__text">Search</span>}
          </Flex>
          {!isCollapsed && (
            <span className="search-collapsible__chevron">
              {isExpanded ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
            </span>
          )}
        </Flex>
      </a>

      {!isCollapsed && isExpanded && (
        <Box className="search-collapsible__submenu">
          <SearchNavItem
            name={BROWSE.SEARCH.CUSTOM}
            text="Custom"
            icon={null}
            params={{ keyword: null }}
            isCollapsed={false}
          />
          {visibleFormats.map((format) => (
            <SearchFormatItem
              key={format.id}
              routeName={format.routeName}
              label={format.label}
            />
          ))}
        </Box>
      )}
    </div>
  );

  if (isCollapsed) {
    return (
      <Box>
        <a
          href={sref.href}
          onClick={sref.onClick}
          className="guide-nav-item"
        >
          <Flex align="center" justify="center" gap="3" px="3" py="2">
            <span className="guide-nav-item__icon">
              <Search size={18} />
            </span>
          </Flex>
        </a>
      </Box>
    );
  }

  return <Box>{content}</Box>;
}

/**
 * Modern Left Navigation Menu - SUPER UX with Radix UI
 * Self-hosted distribution: Dashboard, Search, Browse, Upload, Tags, Malware Risk, API, Settings
 */
export default function LeftNavigationMenuRadix() {
  const { BROWSE, ADMIN } = ROUTE_NAMES;
  const [isCollapsed] = useSideNavbarCollapsedState(false);
  const isPreviewUI = useIsPreviewUI();

  const adminInitialRouteName = useDefaultAdminRouteName();
  const isSettingsVisible = !!adminInitialRouteName;

  return (
    <PreviewUIContext.Provider value={isPreviewUI}>
    <nav className={`guide-sidebar ${isCollapsed ? 'guide-sidebar--collapsed' : ''}`} data-testid="left-nav">
      <ScrollArea className="guide-sidebar__nav" scrollbars="vertical">
        <Box {...(isCollapsed ? { pl: '1', pr: '2', py: '4' } : { p: '4' })} className="guide-sidebar__nav-content">
          <Flex direction="column" gap="2">
            {/* Dashboard */}
            <NavItemBox
              name={BROWSE.WELCOME.ROOT}
              text="Dashboard"
              icon={LayoutDashboard}
              isCollapsed={isCollapsed}
            />

            {/* Search - Preview UI: unified single link; Classic UI: collapsible format submenu */}
            {isPreviewUI ? (
              <NavItemBox
                name={BROWSE.SEARCH.UNIFIED}
                visibilityRoute={BROWSE.SEARCH.ROOT}
                text="Search"
                icon={Search}
                selectedState={BROWSE.SEARCH.ROOT}
                isCollapsed={isCollapsed}
              />
            ) : (
              <SearchCollapsibleNav isCollapsed={isCollapsed} />
            )}

            {/* Browse */}
            <NavItemBox
              name={isPreviewUI ? 'preview.browse.browse' : BROWSE.BROWSE.ROOT}
              text="Browse"
              icon={FolderOpen}
              isCollapsed={isCollapsed}
              params={isPreviewUI ? {} : { repo: null }}
            />

            {/* Upload */}
            <NavItemBox
              name={BROWSE.UPLOAD.LIST}
              visibilityRoute={BROWSE.UPLOAD.ROOT}
              text="Upload"
              icon={HardDriveUpload}
              selectedState={BROWSE.UPLOAD.ROOT}
              isCollapsed={isCollapsed}
            />

            {/* Tags */}
            <NavItemBox
              name={BROWSE.TAGS.ROOT}
              text="Tags"
              icon={Tags}
              isCollapsed={isCollapsed}
              params={{ itemId: null }}
            />

            {/* Malware Risk */}
            <NavItemBox
              name={isPreviewUI ? 'preview.browse.malwarerisk' : (BROWSE.MALWARERISK?.ROOT || 'browse.malwarerisk')}
              visibilityRoute={isPreviewUI ? 'preview.browse.malwarerisk' : (BROWSE.MALWARERISK?.ROOT || 'browse.malwarerisk')}
              text="Malware Risk"
              icon={ShieldAlert}
              isCollapsed={isCollapsed}
            />

            {/* API */}
            {isPreviewUI && (
              <NavItemBox
                name="preview.browse.api"
                text="API"
                icon={FileCode}
                isCollapsed={isCollapsed}
              />
            )}

            {/* Admin Section */}
            {isSettingsVisible && (
              <>
                <Box>
                  <Separator size="4" my="2" />
                </Box>
                <NavItemBox
                  name={isPreviewUI ? 'preview.settings' : adminInitialRouteName}
                  selectedState={ADMIN.DIRECTORY}
                  text="Settings"
                  icon={Settings}
                  isCollapsed={isCollapsed}
                />
              </>
            )}

          </Flex>
        </Box>
      </ScrollArea>
    </nav>
    </PreviewUIContext.Provider>
  );
}
