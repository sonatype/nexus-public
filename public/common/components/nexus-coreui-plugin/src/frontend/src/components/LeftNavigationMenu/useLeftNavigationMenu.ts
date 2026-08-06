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
import type { MouseEventHandler } from 'react';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { useIsActive, useSref, useTransitionHook } from '@uirouter/react';
import {
  ExtJS,
  isSonatypeDevMode,
  useIsPreviewUI,
  useRepositories,
  useSideNavbarOpenState,
} from '@sonatype/nexus-ui-plugin';
import { ROUTE_NAMES } from '../../routerConfig/routeNames/routeNames';
import { useDefaultAdminRouteName } from './useDefaultAdminRouteName';
import { CLASSIC_SEARCH_FORMATS, VisibleFormat } from './types';

/**
 * Layer 2 (Integration) hooks for the Preview UI left navigation menu.
 *
 * All business logic for `LeftNavigationMenuRadix` lives here: data fetching,
 * derived state, and side effects. The presentation components in
 * `LeftNavigationMenuRadix.tsx` consume these hooks and contain no
 * `useEffect`/`useMemo`/data-fetching of their own.
 */

/** Return value of {@link useSearchCollapsibleNav}. */
export interface UseSearchCollapsibleNavResult {
  /** Whether the Classic UI search submenu is currently expanded. */
  isExpanded: boolean;
  /** Whether the parent "Search" entry should render as active. */
  isParentActive: boolean;
  /** Router-generated href for the generic search route. */
  parentHref: string;
  /** Navigate to the generic search route (used by the collapsed variant). */
  navigateToSearch: MouseEventHandler<HTMLAnchorElement>;
  /** Toggle expansion when expanded; no-op when collapsed (preserves prior behavior). */
  handleParentClick: MouseEventHandler<HTMLAnchorElement>;
  /** Route name for the "Custom" search submenu entry. */
  customRouteName: string;
  /** Formats filtered down to the repositories the user actually has. */
  visibleFormats: VisibleFormat[];
}

/**
 * Integration hook for the Classic UI collapsible "Search" navigation.
 *
 * Owns: repository data fetching, format filtering, expansion state and its
 * toggle, auto-expansion on search-route transitions, and the ExtJS
 * `BrowseableFormats` controller sync. Behavior is identical to the previous
 * in-component implementation; only its location changed.
 *
 * @param isCollapsed - Whether the sidebar is collapsed (controls click behavior).
 */
export function useSearchCollapsibleNav(isCollapsed: boolean): UseSearchCollapsibleNavResult {
  const { BROWSE } = ROUTE_NAMES;
  const [isExpanded, setIsExpanded] = useState(false);

  // Active state for the parent "Search" entry (generic or custom search).
  const isGenericActive = useIsActive(BROWSE.SEARCH.GENERIC);
  const isCustomActive = useIsActive(BROWSE.SEARCH.CUSTOM);
  const isParentActive = isGenericActive || isCustomActive;

  const sref = useSref(BROWSE.SEARCH.GENERIC, { keyword: null });

  // Auto-expand when transitioning to a search route using the UI-Router hook.
  const handleSearchTransition = useCallback(
    (transition: { to: () => { name?: string } }) => {
      const toState = transition.to();
      if (toState.name?.startsWith(BROWSE.SEARCH.ROOT)) {
        setIsExpanded(true);
      }
    },
    [BROWSE.SEARCH.ROOT]
  );

  useTransitionHook('onSuccess', {}, handleSearchTransition);

  // Formats available to the user, derived from the repositories REST API.
  const { availableFormats, loading } = useRepositories();

  // Sync React-detected formats to the ExtJS BrowseableFormats controller.
  useEffect(() => {
    if (loading || !availableFormats || availableFormats.size === 0) {
      return;
    }

    const app = (window as { Ext?: { getApplication?: () => unknown } }).Ext?.getApplication?.();
    const controller = (
      app as { getController?: (name: string) => { setFormats: (formats: { id: string }[]) => void } } | undefined
    )?.getController?.('NX.coreui.controller.BrowseableFormats');

    if (controller) {
      const formatData = Array.from(availableFormats).map((format) => ({ id: format }));
      // Call unconditionally: a missing setFormats should throw and surface an
      // ExtJS regression immediately, matching the pre-refactor behavior.
      controller.setFormats(formatData);
    }
  }, [availableFormats, loading]);

  // Filter the catalogue down to formats the user has repositories for.
  const visibleFormats = useMemo<VisibleFormat[]>(() => {
    if (loading || !availableFormats) {
      return [];
    }

    return CLASSIC_SEARCH_FORMATS.filter(({ apiFormat }) => availableFormats.has(apiFormat)).map(
      ({ id, label, routeKey }) => ({
        id,
        label,
        routeName: BROWSE.SEARCH[routeKey],
      })
    );
  }, [availableFormats, loading, BROWSE.SEARCH]);

  const handleParentClick = useCallback<MouseEventHandler<HTMLAnchorElement>>(
    (event) => {
      if (isCollapsed) {
        return;
      }
      event.preventDefault();
      setIsExpanded((expanded) => !expanded);
    },
    [isCollapsed]
  );

  return {
    isExpanded,
    isParentActive,
    parentHref: sref.href,
    navigateToSearch: sref.onClick,
    handleParentClick,
    customRouteName: BROWSE.SEARCH.CUSTOM,
    visibleFormats,
  };
}

/** Return value of {@link useLeftNavigationMenu}. */
export interface UseLeftNavigationMenuResult {
  /** Whether the sidebar is collapsed. */
  isCollapsed: boolean;
  /** Whether the Preview UI (Nexus One) is active. */
  isPreviewUI: boolean;
  /** First visible admin route, or `null` when no admin section is visible. */
  adminInitialRouteName: string | null;
  /** Whether the "Settings" entry should be shown. */
  isSettingsVisible: boolean;
  /** Whether the Sonatype-internal "Test Hub" entry should be shown (Preview UI only). */
  showTestHub: boolean;
  /** Whether the instance runs the Pro edition (gates Pro-only entries like Tags). */
  isProEdition: boolean;
}

/**
 * Integration hook for the top-level left navigation menu.
 *
 * Owns the derived state the presentation component needs: collapsed state,
 * Preview UI detection, the default admin route, and whether the Settings
 * entry should be visible.
 */
export function useLeftNavigationMenu(): UseLeftNavigationMenuResult {
  const [isOpen] = useSideNavbarOpenState(false);
  const isPreviewUI = useIsPreviewUI();
  const adminInitialRouteName = useDefaultAdminRouteName();
  const isSettingsVisible = !!adminInitialRouteName;

  // Test Hub is visible in Preview UI when the SONATYPE_INTERNAL build flag or localStorage flag is set
  const showTestHub = isPreviewUI && isSonatypeDevMode();
  const isProEdition = ExtJS.isProEdition?.() ?? false;

  return {
    isCollapsed: !isOpen,
    isPreviewUI,
    adminInitialRouteName,
    isSettingsVisible,
    showTestHub,
    isProEdition,
  };
}
