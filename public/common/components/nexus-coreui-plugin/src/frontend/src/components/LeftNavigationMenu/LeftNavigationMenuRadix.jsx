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
import React, { useState, useEffect, createContext, useContext, useMemo } from 'react';
import { Box, Flex, ScrollArea, Separator } from '@radix-ui/themes';
import { Tooltip } from '../shared';
import { useSref, useIsActive, useRouter } from '@uirouter/react';
import {
  LayoutDashboard,
  Search,
  FolderOpen,
  HardDriveUpload,
  Tags,
  Settings,
  ExternalLink,
  ShieldAlert,
  FlaskConical,
  FileCode,
  ScrollText,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';
import { useIsVisible } from '@sonatype/nexus-ui-plugin';
import { ROUTE_NAMES } from '../../routerConfig/routeNames/routeNames';
import { useDefaultAdminRouteName } from './useDefaultAdminRouteName';
import { useClmDashboardVisibility } from './useClmDashboardVisibility';
import useSideNavbarCollapsedState from '../../hooks/useSideNavbarCollapsedState';
import { FORMATS } from '../super/search/unified/searchFilters';
import { useRepositories } from '../super/search/unified/useRepositories';

import './LeftNavigationMenuRadix.scss';

const CLASSIC_SEARCH_FORMATS = [
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
  { id: 'pub', apiFormat: 'pub', label: 'Pub', routeKey: 'PUB' }, // pub is absent from FORMATS in searchFilters.ts
  { id: 'r', apiFormat: 'r', label: FORMATS.r.label, routeKey: 'R' },
  { id: 'raw', apiFormat: 'raw', label: FORMATS.raw.label, routeKey: 'RAW' },
  { id: 'rubygems', apiFormat: 'rubygems', label: FORMATS.rubygems.label, routeKey: 'RUBYGEMS' },
  { id: 'swift', apiFormat: 'swift', label: FORMATS.swift.label, routeKey: 'SWIFT' },
  { id: 'terraform', apiFormat: 'terraform', label: FORMATS.terraform.label, routeKey: 'TERRAFORM' },
  { id: 'yum', apiFormat: 'yum', label: FORMATS.yum.label, routeKey: 'YUM' },
];

// Context for Preview UI mode
const PreviewUIContext = createContext(false);

/**
 * Hook to detect if we're in Preview UI mode
 */
function useIsPreviewUI() {
  const [isPreview, setIsPreview] = useState(false);

  useEffect(() => {
    function checkPreview() {
      const hash = window.location.hash;
      setIsPreview(hash.startsWith('#preview'));
    }

    checkPreview();
    window.addEventListener('hashchange', checkPreview);
    return () => window.removeEventListener('hashchange', checkPreview);
  }, []);

  return isPreview;
}

/**
 * Hook to get context-aware route name
 * Prefixes with 'preview.' when in Preview UI mode
 */
function useContextAwareRouteName(name) {
  const isPreviewUI = useContext(PreviewUIContext);

  if (!name) return name;

  // If already has preview prefix, return as is
  if (name.startsWith('preview.')) {
    return isPreviewUI ? name : name.replace('preview.', '');
  }

  // Add preview prefix if in preview mode
  return isPreviewUI ? `preview.${name}` : name;
}

// Sentinel value to indicate route doesn't exist (distinct from undefined visibilityRequirements)
const ROUTE_NOT_FOUND = Symbol('ROUTE_NOT_FOUND');

/**
 * Hook to resolve route state and visibilityRequirements from a route.
 * Returns ROUTE_NOT_FOUND if the route doesn't exist, or the requirements (which may be undefined).
 */
function useRouteVisibilityRequirements(baseName) {
  const router = useRouter();
  const contextAwareName = useContextAwareRouteName(baseName);

  try {
    // Try context-aware name first, fall back to base name
    let state = router.stateRegistry.get(contextAwareName);
    if (!state) {
      state = router.stateRegistry.get(baseName);
    }

    if (!state) {
      return ROUTE_NOT_FOUND; // Route doesn't exist
    }

    const data = state?.data || {};
    // May be undefined if route has no visibility requirements (meaning always visible)
    return data.visibilityRequirements;
  } catch {
    return ROUTE_NOT_FOUND;
  }
}

/**
 * Hook to check if a route is visible based on permissions, editions, etc.
 * Uses the reactive useIsVisible hook from nexus-ui-plugin which subscribes to
 * Permissions#changed, State#changed, and State#userchanged events.
 */
function useRouteVisibility(baseName) {
  const visibilityRequirements = useRouteVisibilityRequirements(baseName);
  const routeExists = visibilityRequirements !== ROUTE_NOT_FOUND;

  // useIsVisible handles permission/state change events reactively
  // If route has no requirements (undefined), useIsVisible returns true
  const isRouteVisible = useIsVisible(routeExists ? visibilityRequirements : undefined);

  // Route must exist to be visible. Missing routes return false (fail closed).
  return routeExists ? isRouteVisible : false;
}

/**
 * Navigation Item Component using Radix + uirouter
 * Context-aware: uses preview.* routes when in Preview UI
 */
function NavItem({
  name,
  text,
  icon: Icon,
  selectedState,
  params = {},
  isCollapsed,
  href: directHref,
  isExternal,
}) {
  const contextAwareName = useContextAwareRouteName(name);
  const contextAwareSelectedState = useContextAwareRouteName(selectedState);

  const sref = useSref(contextAwareName, params);
  const isActive = useIsActive(contextAwareSelectedState || contextAwareName);

  // Use direct href for external links, otherwise use router-generated href
  const finalHref = directHref || sref.href;

  // External links use href only (no onClick). Internal links use sref.onClick for router handling.
  const handleClick = directHref ? undefined : sref.onClick;

  const content = (
    <a
      href={finalHref}
      onClick={handleClick}
      target={isExternal ? '_blank' : undefined}
      rel={isExternal ? 'noopener noreferrer' : undefined}
      className={`guide-nav-item ${isActive ? 'guide-nav-item--active' : ''}`}
    >
      <Flex align="center" justify={isCollapsed ? 'center' : 'start'} gap="3" px="3" py="2">
        <span className="guide-nav-item__icon">
          {Icon && <Icon size={18} />}
        </span>
        {!isCollapsed && (
          <span className="guide-nav-item__text">{text}</span>
        )}
        {isExternal && !isCollapsed && (
          <ExternalLink size={18} className="guide-nav-item__external" />
        )}
      </Flex>
    </a>
  );

  if (isCollapsed) {
    return (
      <Tooltip content={text} side="right">
        {content}
      </Tooltip>
    );
  }

  return content;
}

/**
 * Wraps NavItem in Box only when visible. Prevents empty Box elements from
 * participating in Flex layout and creating extra gaps when NavItem returns null.
 */
function NavItemBox(props) {
  const { visibilityRoute, name, href, ...restProps } = props;
  const routeForVisibility = visibilityRoute || name;
  const routeIsVisible = useRouteVisibility(routeForVisibility);

  // For external links (href), always render. For routes, check visibility.
  if (!routeIsVisible && !href) return null;

  return (
    <Box>
      <NavItem name={name} href={href} {...restProps} />
    </Box>
  );
}

/**
 * Search format child item.
 * Visibility is determined by the parent component via availableFormats from REST API.
 * Format data is synced to ExtJS BrowseableFormats controller so router visibility
 * checks pass when navigating.
 */
function SearchFormatItem({ routeName, label }) {
  return (
    <NavItem
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
 * Note: Uses useRepositories hook (REST API) instead of ExtJS browseableformats state
 * which may not be properly initialized in some environments.
 * Syncs detected formats to ExtJS BrowseableFormats controller so router visibility
 * checks pass when navigating to format search routes.
 */
export function SearchCollapsibleNav({ isCollapsed }) {
  const { BROWSE } = ROUTE_NAMES;
  const [isExpanded, setIsExpanded] = useState(false);
  const router = useRouter();

  // Check active states for parent search item (must be separate hook calls, no short-circuit)
  const isGenericActive = useIsActive(BROWSE.SEARCH.GENERIC);
  const isCustomActive = useIsActive(BROWSE.SEARCH.CUSTOM);
  // Parent is active only on generic/custom search, not on format-specific searches
  const isParentActive = isGenericActive || isCustomActive;

  const sref = useSref(BROWSE.SEARCH.GENERIC, { keyword: null });

  // Auto-expand if on a search route
  useEffect(() => {
    const currentRoute = router.globals.current?.name;
    if (currentRoute?.startsWith(BROWSE.SEARCH.ROOT)) {
      setIsExpanded(true);
    }
  }, [router.globals.current?.name]);

  // Get formats from REST API (more reliable than ExtJS state)
  const { availableFormats, loading } = useRepositories();

  // Sync React-detected formats to ExtJS BrowseableFormats controller
  // This ensures router visibility checks pass for format routes
  useEffect(() => {
    if (loading || !availableFormats || availableFormats.size === 0) {
      return;
    }

    // TODO: NEXUS-51995 - Replace with ExtJS interface method once available
    const app = window.Ext?.getApplication?.();
    const controller = app?.getController?.('NX.coreui.controller.BrowseableFormats');

    if (controller) {
      // ExtJS store expects array of {id: string} objects
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
      // When collapsed, navigate to search
      return;
    }
    e.preventDefault();
    setIsExpanded(!isExpanded);
  };

  const handleKeyDown = (e) => {
    if (isCollapsed) return;
    // Toggle on Enter or Space
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      setIsExpanded(!isExpanded);
    }
    // Expand on ArrowDown when focused
    if (e.key === 'ArrowDown' && !isExpanded) {
      e.preventDefault();
      setIsExpanded(true);
    }
    // Collapse on ArrowUp when expanded
    if (e.key === 'ArrowUp' && isExpanded) {
      e.preventDefault();
      setIsExpanded(false);
    }
  };

  const content = (
    <div className={`search-collapsible ${isExpanded ? 'search-collapsible--expanded' : ''}`}>
      {/* Parent Search Item */}
      <a
        href={sref.href}
        onClick={handleParentClick}
        onKeyDown={handleKeyDown}
        aria-expanded={isCollapsed ? undefined : isExpanded}
        aria-haspopup="true"
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

      {/* Submenu */}
      {!isCollapsed && isExpanded && (
        <Box className="search-collapsible__submenu">
          {/* Always show Custom first */}
          <NavItem
            name={BROWSE.SEARCH.CUSTOM}
            text="Custom"
            icon={null}
            params={{ keyword: null }}
            isCollapsed={false}
          />

          {/* Format-specific searches */}
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
      <Tooltip content="Search" side="right">
        {content}
      </Tooltip>
    );
  }

  return <Box>{content}</Box>;
}

/**
 * Modern Left Navigation Menu - SUPER UX with Radix UI
 * Fully migrated from RSC/ExtJS to Radix + Lucide + @uirouter/react
 * Context-aware: automatically uses preview.* routes when in Preview UI
 */
export default function LeftNavigationMenuRadix() {
  const { BROWSE, ADMIN } = ROUTE_NAMES;
  const [isCollapsed, setIsCollapsed] = useSideNavbarCollapsedState(false);
  const isPreviewUI = useIsPreviewUI();

  const adminInitialRouteName = useDefaultAdminRouteName();
  const isSettingsVisible = !!adminInitialRouteName;
  const clmState = useClmDashboardVisibility();

  return (
    <PreviewUIContext.Provider value={isPreviewUI}>
    <nav className={`guide-sidebar ${isCollapsed ? 'guide-sidebar--collapsed' : ''}`} data-testid="left-nav">
      {/* Navigation Items */}
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

            {/* Browse — Preview UI: new BrowsePage (filter sidebar + table); Heritage: legacy Browse */}
            <NavItemBox
              name={isPreviewUI ? 'preview.browse.browse' : BROWSE.BROWSE.ROOT}
              text="Browse"
              icon={FolderOpen}
              isCollapsed={isCollapsed}
              params={isPreviewUI ? {} : { repo: null }}
            />

            {/* Upload - navigate to LIST, but check visibility against ROOT (which has permissions) */}
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

            {/* Malware Risk — Nexus One UI shows new Malware Risk page; Default UI keeps Malicious Packages */}
            <NavItemBox
              name={isPreviewUI ? 'preview.browse.malwarerisk' : (BROWSE.MALWARERISK?.ROOT || 'browse.malwarerisk')}
              visibilityRoute={isPreviewUI ? 'preview.browse.malwarerisk' : (BROWSE.MALWARERISK?.ROOT || 'browse.malwarerisk')}
              text="Malware Risk"
              icon={ShieldAlert}
              isCollapsed={isCollapsed}
            />

            {/* Audit - System-wide audit log (Preview UI only) */}
            {isPreviewUI && (
              <NavItemBox
                name="preview.browse.audit"
                text="Audit"
                icon={ScrollText}
                isCollapsed={isCollapsed}
              />
            )}

            {/* API - Swagger API documentation (Preview UI only) */}
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

            {/* SONATYPE INTERNAL: Test Hub — below Settings, only in Preview UI */}
            {isPreviewUI && ((typeof __SONATYPE_INTERNAL__ !== 'undefined' && __SONATYPE_INTERNAL__)
              || (typeof localStorage !== 'undefined' && localStorage.getItem('SONATYPE_INTERNAL') === 'true')) && (
              <Box style={{ color: 'var(--amber-9)' }}>
                <NavItemBox
                  name="preview.test"
                  text="[Test] Hub"
                  icon={FlaskConical}
                  isCollapsed={isCollapsed}
                />
              </Box>
            )}

          </Flex>
        </Box>
      </ScrollArea>
    </nav>
    </PreviewUIContext.Provider>
  );
}
