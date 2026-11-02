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
 * Shared global header shell for both self-hosted and cloud (NEXUS-54869).
 *
 * Discipline: differences between distributions live in this file, at the point of
 * difference, guarded by useIsCloud() with a comment stating why. Default is to render
 * identically on both distributions (see HelpMenu for the template this follows).
 *
 * Zero required props. `routes` and `accountMenuLabel` are host-supplied (each host owns
 * its own router state names / UIStrings) and threaded down to SearchRadix and
 * LoginAndUserMenu, which already accept them in this shape from prior PRs on this stack.
 *
 * No useIsCloud() differences currently — both distributions render identically.
 *
 * Ported unconditionally to both hosts, despite originating from only one:
 * - onRefreshClick's `!ExtJS.isExtJsRendered()` bypass guard. Both distributions load the
 *   same shared nexus-rapture ExtJS runtime and NX.controller.Menu; menuCtrl.warnBeforeNavigate()
 *   crashes whenever called with no nx-feature-content DOM node mounted (its internal
 *   getFeatureContent() resolves via a live ComponentQuery and returns undefined, and the
 *   next line unconditionally reads `.discardUnsavedChanges` off it). Self-hosted already
 *   guarded against this (NEXUS-53775); cloud's own plain-React admin forms can set
 *   window.dirty via ExtJS.setDirtyStatus() with no ExtJS view mounted, so cloud had this
 *   same latent crash — porting the guard here is a real bugfix for cloud, not a no-op.
 * - The defaultToPreviewUi soft-default / disableLegacyUi hard-kill-switch redirect effect.
 *   Cloud's pre-merge header read disableLegacyUi only to gate the toggle button's
 *   visibility, and never read defaultToPreviewUi at all — so an admin who enabled either
 *   setting had no effect on cloud beyond hiding/showing the switch button. Both settings
 *   are published by the shared PreviewUiStateContributor (NEXUS-53957) and already honored
 *   by the shared login redirect (loginUtils.resolveDefaultLandingRoute), which documents
 *   itself as mirroring this exact gate in "GlobalHeaderRadix" — i.e. the two were always
 *   meant to agree. Porting this here is a second real bugfix for cloud.
 */

import React, {useState, useEffect, useCallback, useMemo, useRef} from 'react';
import {Flex, Box, IconButton, Button, Badge, Dialog, Text, Popover, TextArea, Heading} from '@radix-ui/themes';
import {RefreshCw, Repeat, Menu} from 'lucide-react';
import {useRouter} from '@uirouter/react';

import {ExtJS} from '../../../../interface/ExtJS';
import {handleExtJsUnsavedChanges} from '../../../../router/extJsUnsavedChanges';
import {hasUnsavedChanges} from '../hooks/useUnsavedChangesWarning';
import {restClient} from '../../../../interface/api';
import {useTheme} from '../../../../contexts/ThemeContext';
import {useSideNavbarOpenState} from '../../../../hooks/useSideNavbarOpenState';
import {useMediaQuery} from '../../../../hooks/useMediaQuery';
import {getHeritageEquivalent} from '../../../../utils/previewHeritageNavigation';
import {getHeaderWordmarkLogo, getHeaderMarkLogo, getEditionDisplayName, getEditionShortDisplayName, EDITION_ID_STATE_KEY} from '../../../../interface/editionUtils';
import {Tooltip} from '../Tooltip';
import {HelpMenu} from '../HelpMenu';
import ThemeSwitcher from '../ThemeSwitcher';
import {LoginAndUserMenu} from '../LoginAndUserMenu';
import {SearchRadix} from '../SearchRadix';
import SystemStatusBell from '../SystemStatusBell/SystemStatusBell';

import './GlobalHeader.scss';

const MAX_FEEDBACK_LENGTH = 500;

// Below md the secondary controls fold into the user menu; below xs the logo goes mark-only and Refresh hides.
// xs is 768 so the mark-only logo holds until the full wordmark plus the widest edition badge actually fit the row.
const MD_UP_QUERY = '(min-width: 1024px)';
const XS_UP_QUERY = '(min-width: 768px)';

/**
 * Refreshes the current React page, handling URL parameter encoding. Moved in from each
 * host's identical (and header-only) routerConfig/routerUtils.js `refreshReactPage` — both
 * hosts' copies had exactly one caller (their GlobalHeader), so this consolidates rather
 * than duplicating a third time.
 * @param {Object} router - UI-Router instance from useRouter()
 */
function refreshReactPage(router) {
  if (!router) {
    console.error('refreshReactPage: router is required');
    return;
  }

  const currentHash = window.location.hash;
  const colonIndex = currentHash.indexOf(':');

  // If URL has parameters after ':', re-encode them and reload via router
  if (colonIndex > 0) {
    const currentState = router.stateService.current;
    const currentParams = router.stateService.params;
    const param = currentHash.substring(colonIndex + 1);
    const encodedParam = encodeURIComponent(decodeURIComponent(param));

    // Find the parameter name from current route params (e.g., itemId, keyword, etc.)
    const paramName = Object.keys(currentParams).find(key => currentParams[key] && key !== '#');

    if (paramName) {
      router.stateService.go(currentState.name, {
        [paramName]: `:${encodedParam}`
      }, {
        reload: true,
        inherit: false,
        location: 'replace'
      });
      return;
    }
  }
  router.stateService.reload();
}

export default function GlobalHeader({routes = {}, accountMenuLabel}) {
  const router = useRouter();
  const {effectiveTheme, setTheme} = useTheme();

  const status = ExtJS.useStatus();
  const version = status?.version;
  const editionId = ExtJS.useState?.(() => ExtJS.state()?.getValue?.(EDITION_ID_STATE_KEY));

  const user = ExtJS.useUser();
  const isLoggedIn = Boolean(user?.authenticated ?? user);

  // Context path (static, safe to read any time)
  const contextPath = useMemo(() => {
    try {
      const state = ExtJS.state();
      return state?.getValue('nexus-context-path', '') || '';
    } catch {
      return '';
    }
  }, []);

  // Cached access flag — single effect handles all auth states.
  //
  // Rules:
  //   Logged-in:  grant when ExtJS flags confirm access; NEVER revoke
  //               while isLoggedIn is true (immune to flag flaps).
  //   Anonymous:  re-evaluate every time (no caching — anonymous state
  //               is stable, no login race condition).
  //   Logout→Anon: compute anonymous access in the SAME effect that
  //               handles logout so there's one setState, no flash.
  const [canAccessPreviewUi, setCanAccessPreviewUi] = useState(false);
  const [isDefaultToPreviewUi, setDefaultToPreviewUi] = useState(false);
  const [isDisableLegacyUi, setDisableLegacyUi] = useState(false);
  const [isDisableSwitchFeedback, setDisableSwitchFeedback] = useState(false);
  const [heritageUnavailableOpen, setHeritageUnavailableOpen] = useState(false);
  const [uiSwitchPopoverOpen, setUiSwitchPopoverOpen] = useState(false);
  // Folded switch has no header button to anchor a Popover to, so it shows the feedback form in a Dialog.
  const [uiSwitchDialogOpen, setUiSwitchDialogOpen] = useState(false);
  const [classicUiFeedback, setClassicUiFeedback] = useState('');
  const useClassicUiButtonRef = useRef(null);

  const isFeedbackTooLong = classicUiFeedback.length > MAX_FEEDBACK_LENGTH;

  useEffect(() => {
    try {
      const state = ExtJS.state();
      if (!state) return;

      setDisableLegacyUi(state.getValue('disableLegacyUi', false) ?? false);
      setDefaultToPreviewUi(state.getValue('defaultToPreviewUi', false) ?? false);
      setDisableSwitchFeedback(state.getValue('disableSwitchFeedback', false) ?? false);

      if (isLoggedIn) {
        // Logged-in path: only upgrade false→true, never downgrade.
        if (!canAccessPreviewUi) {
          const loggedInEnabled = state.getValue('loggedInEnabled', false) ?? false;
          if (loggedInEnabled) {
            setCanAccessPreviewUi(true);
          }
        }
      } else {
        // Anonymous path (includes fresh page load AND logout→anon).
        // Always re-evaluate — no caching for anonymous.
        const anonymousEnabled = state.getValue('anonymousEnabled', false) ?? false;
        setCanAccessPreviewUi(anonymousEnabled);
      }
    } catch {
      if (!isLoggedIn) {
        setCanAccessPreviewUi(false);
      }
    }
  }, [isLoggedIn, user, canAccessPreviewUi]);

  // Phase 4.0: UI Toggle between DEFAULT and PREVIEW
  // Both UIs use hash-based routing (e.g., #browse/welcome or #preview/browse/welcome)
  const [currentPath, setCurrentPath] = useState(() => {
    const hash = window.location.hash.substring(1);
    return hash || '';
  });

  // Listen for hash changes (entire app uses hash-based routing)
  useEffect(() => {
    const handleLocationChange = () => {
      const hash = window.location.hash.substring(1);
      setCurrentPath(hash || '');
    };

    window.addEventListener('hashchange', handleLocationChange);
    window.addEventListener('popstate', handleLocationChange);
    return () => {
      window.removeEventListener('hashchange', handleLocationChange);
      window.removeEventListener('popstate', handleLocationChange);
    };
  }, []);

  const isPreviewUI = currentPath.startsWith('preview');

  // Redirect logic for rollout controls (Sprint 17). Both settings are published by the
  // shared PreviewUiStateContributor for both distributions (see file header comment).
  useEffect(() => {
    if (!canAccessPreviewUi) return;

    const cleanPath = currentPath.replace(/^\//, '');
    const landingPages = ['', '/', 'browse/welcome'];
    const isLandingPage = landingPages.includes(cleanPath);

    // 1. HARD KILL SWITCH: Check disableLegacyUi first
    if (isDisableLegacyUi && !isPreviewUI) {
      // Force redirect to preview equivalent or welcome
      let newPath = 'preview/browse/welcome';

      // Try to find equivalent mapping if it's not a landing page
      if (!isLandingPage) {
        if (cleanPath.startsWith('admin/')) {
          newPath = 'preview/' + cleanPath;
        } else if (cleanPath === 'user/account' || cleanPath.startsWith('user/account/')) {
          // User Account is a standalone page — not inside the Settings layout
          newPath = 'preview/user/account';
        } else if (cleanPath === 'user/NuGetApiToken' || cleanPath.startsWith('user/NuGetApiToken')) {
          newPath = 'preview/user/nugetapitoken';
        } else if (cleanPath === 'user/usertoken' || cleanPath.startsWith('user/usertoken')) {
          newPath = 'preview/user/usertoken';
        } else if (cleanPath.startsWith('user/')) {
          newPath = 'preview/user/' + cleanPath.replace('user/', '');
        } else {
          newPath = 'preview/' + cleanPath;
        }
      }

      // Use hash-based navigation for Preview UI
      window.location.hash = `#${newPath}`;
      return;
    }

    // 2. SOFT DEFAULT: Check defaultToPreviewUi second
    if (isDefaultToPreviewUi && !isPreviewUI && isLandingPage) {
      const userRequestedLegacy = sessionStorage.getItem('user_requested_legacy') === 'true';
      if (!userRequestedLegacy) {
        window.location.hash = '#preview/browse/welcome';
      }
    }
  }, [canAccessPreviewUi, isDisableLegacyUi, isDefaultToPreviewUi, isPreviewUI, currentPath]);

  // Track whether access was ever granted so we don't redirect on
  // the very first render (before the grant effect has run).
  const accessEverGranted = useRef(false);
  if (canAccessPreviewUi) {
    accessEverGranted.current = true;
  }

  // Redirect out of Preview UI only when access transitions from
  // granted → revoked while the user is still authenticated.
  // On logout, useRedirectOnLogout handles the redirect to login — we must not
  // interfere by stripping preview/ (which creates invalid classic routes like #settings).
  useEffect(() => {
    if (isPreviewUI && !canAccessPreviewUi && accessEverGranted.current && isLoggedIn) {
      setTheme('light');
      const defaultPath = currentPath.replace(/^preview\//, '') || 'browse/welcome';
      window.location.hash = defaultPath;
    }
  }, [isPreviewUI, canAccessPreviewUi, currentPath, setTheme, isLoggedIn]);

  const switchToClassicUI = useCallback(() => {
    // Consulted only to detect Preview-only views (null = no Classic equivalent); the
    // switch itself always lands on the Classic home page below.
    if (getHeritageEquivalent(currentPath) === null) {
      setHeritageUnavailableOpen(true);
      setUiSwitchPopoverOpen(false);
      setUiSwitchDialogOpen(false);
      return;
    }

    if (!isDisableSwitchFeedback) {
      restClient.post('/service/rest/internal/ui/switch-feedback', {
        edition: editionId ?? 'Unknown',
        version: version ?? 'Unknown',
        feedback: classicUiFeedback.trim().slice(0, MAX_FEEDBACK_LENGTH),
      }).catch(() => {});
    }

    sessionStorage.setItem('user_requested_legacy', 'true');
    setTheme('light');
    setUiSwitchPopoverOpen(false);
    setUiSwitchDialogOpen(false);
    setClassicUiFeedback('');
    // Land on the Classic home page rather than the mapped equivalent of the current
    // route. getHeritageEquivalent is still consulted above so a Preview-only view keeps
    // showing the "Not Available in Classic UI" dialog instead of switching silently.
    window.location.hash = 'browse/welcome';
  }, [currentPath, classicUiFeedback, isDisableSwitchFeedback, setTheme, editionId, version]);

  // Always land on the Preview dashboard rather than attempting to map the
  // current Classic route to its Preview equivalent. heritageToPreviewPath has
  // a generic fallback that prefixes "preview/" to unmapped paths, which can
  // produce routes that don't exist in Preview UI and result in 404s.
  const switchToNexusOneUI = useCallback(() => {
    window.location.hash = '#preview/browse/welcome';
    sessionStorage.removeItem('user_requested_legacy');
  }, []);

  const handleUiSwitchPopoverOpenChange = useCallback((open) => {
    setUiSwitchPopoverOpen(open);
    if (!open) {
      setClassicUiFeedback('');
    }
  }, []);

  const handleUiSwitchDialogOpenChange = useCallback((open) => {
    setUiSwitchDialogOpen(open);
    if (!open) {
      setClassicUiFeedback('');
    }
  }, []);

  // Folded menu item: show the same feedback prompt the header button shows, or switch directly when feedback is disabled.
  const onFoldedUiSwitch = useCallback(() => {
    if (isDisableSwitchFeedback) {
      switchToClassicUI();
    } else {
      setUiSwitchDialogOpen(true);
    }
  }, [isDisableSwitchFeedback, switchToClassicUI]);

  const handlePopoverOpenAutoFocus = useCallback((e) => {
    e.preventDefault();
    // Use requestAnimationFrame so the button is mounted when we focus
    requestAnimationFrame(() => {
      useClassicUiButtonRef.current?.focus();
    });
  }, []);

  // Unconditional guard for both distributions — see file header comment for why.
  const onRefreshClick = () => {
    if (!ExtJS.isExtJsRendered()) {
      // No ExtJS controller backing this view: ExtJS's warnBeforeNavigate crashes when
      // there is no nx-feature-content node mounted, so handle React dirty state
      // directly here instead (NEXUS-53775).
      if (hasUnsavedChanges() && !window.confirm('You have unsaved changes. Refreshing will discard them. Continue?')) {
        return;
      }
      refreshReactPage(router);
      return;
    }

    const menuCtrl =
      window.Ext && Ext.getApplication && Ext.getApplication().getController
        ? Ext.getApplication().getController('Menu')
        : null;

    handleExtJsUnsavedChanges(menuCtrl, () => {
      ExtJS.refresh();
    });
  };

  function getLogo() {
    const isDark = effectiveTheme === 'dark';
    // Below xs the full wordmark does not fit; use the mark-only logo.
    return isXsUp ? getHeaderWordmarkLogo(isDark) : getHeaderMarkLogo(isDark);
  }

  function getEditionText() {
    // Below xs the full "Community Edition" label overflows 320px; use the short form.
    return isXsUp ? getEditionDisplayName(editionId) : getEditionShortDisplayName(editionId);
  }

  const [isSidebarOpen] = useSideNavbarOpenState(false);
  const onSidebarToggle = useCallback(() => {
    window.dispatchEvent(new CustomEvent('nx-sidebar-toggle'));
  }, []);

  // Interactive part of the feedback prompt, shared so the md+ Popover and the folded-menu Dialog behave identically.
  function renderUiSwitchFeedbackControls({buttonRef} = {}) {
    return (
      <Flex direction="column" gap="3">
        <Flex direction="column" gap="1">
          <TextArea
            id="ui-feedback-textarea"
            placeholder="Share your feedback..."
            value={classicUiFeedback}
            onChange={(e) => setClassicUiFeedback(e.target.value)}
            rows={3}
            aria-label="Optional feedback"
            maxLength={MAX_FEEDBACK_LENGTH + 1}
            color={isFeedbackTooLong ? 'red' : undefined}
          />
          {isFeedbackTooLong && (
            <Text size="1" color="red">
              Feedback must be {MAX_FEEDBACK_LENGTH} characters or less ({classicUiFeedback.length - MAX_FEEDBACK_LENGTH} characters too many)
            </Text>
          )}
        </Flex>
        <Button
          ref={buttonRef}
          variant="soft"
          color="green"
          size="2"
          className={isFeedbackTooLong ? 'nxrm-global-header-radix__clickable--disabled' : 'nxrm-global-header-radix__clickable'}
          onClick={switchToClassicUI}
          disabled={isFeedbackTooLong}
          aria-label="Use Classic UI"
          title="Use Classic UI"
          data-analytics-id="nxrm-dialog-use-classic"
        >
          Use Classic UI
        </Button>
      </Flex>
    );
  }

  // Folded controls are conditionally rendered, not CSS-hidden: a hidden DropdownMenu.Item stays keyboard-focusable, an a11y defect.
  const isMdUp = useMediaQuery(MD_UP_QUERY);
  const isXsUp = useMediaQuery(XS_UP_QUERY);
  const canShowUiSwitch = canAccessPreviewUi && !isDisableLegacyUi && isPreviewUI;
  const canShowNexusOneSwitch = canAccessPreviewUi && !isDisableLegacyUi && !isPreviewUI;
  const foldUiSwitchIntoMenu = canShowUiSwitch && !isMdUp;
  const foldNexusOneSwitchIntoMenu = canShowNexusOneSwitch && !isMdUp;
  const foldThemeIntoMenu = isPreviewUI && !isMdUp;

  const searchRoutes = {
    searchGeneric: routes.searchGeneric,
    previewSearchUnified: routes.previewSearchUnified,
    previewSearchComponent: routes.previewSearchComponent,
  };

  return (
    <div className="nxrm-global-header nxrm-global-header-radix nx-global-header-2" role="banner" data-testid="global-header">
      <Box asChild className="nxrm-global-header-radix__sticky-bar">
        <Flex direction="column" gap="2" px="4" className="nxrm-global-header-radix__bar">
          <Flex align="center" justify="between" gap="4" className="nxrm-global-header-radix__row">
            {/* Left: Sidebar toggle + Logo */}
            <Flex align="center" gap="4" className="nxrm-global-header-radix__side">
              <Tooltip content={isSidebarOpen ? 'Expand Sidebar' : 'Collapse Sidebar'}>
                <IconButton
                  variant="outline"
                  size="2"
                  color="gray"
                  aria-label={isSidebarOpen ? 'Expand Sidebar' : 'Collapse Sidebar'}
                  onClick={onSidebarToggle}
                  data-testid="sidebar-toggle-button"
                >
                  <Menu size={16} />
                </IconButton>
              </Tooltip>
              <Flex align="center" gap="2">
                <Box asChild>
                  <a href={`${contextPath}#${isPreviewUI ? 'preview/browse/welcome' : 'browse/welcome'}`} title="Home" aria-label="Home">
                    <img
                      src={getLogo()}
                      alt="Sonatype Nexus Repository"
                      className="nxrm-global-header-radix__logo"
                    />
                  </a>
                </Box>
                {Boolean(editionId) && (
                  <Badge color="green" variant="solid" size="1">
                    {getEditionText()}
                  </Badge>
                )}
              </Flex>
            </Flex>

            {/* Search: inline slot at md and up; own-row slot below md (rendered at the end of the bar) */}
            {isMdUp && (
              <Box className="nxrm-global-header-radix__search">
                <SearchRadix isPreviewUI={isPreviewUI} routes={searchRoutes} />
              </Box>
            )}

            {/* Right: Utility buttons */}
            <Flex align="center" gap="3" className="nxrm-global-header-radix__side">
              {canAccessPreviewUi && !isDisableLegacyUi && (
                isPreviewUI ? (
                  // Below md the switch folds into the user menu; above md it stays a header button.
                  !isMdUp ? null : isDisableSwitchFeedback ? (
                    <Tooltip content="Switch to Classic UI">
                      <Button
                        variant="soft"
                        color="green"
                        className="nxrm-global-header-radix__clickable"
                        aria-label="Switch to Classic UI"
                        title="Switch to Classic UI"
                        data-testid="ui-toggle-button"
                        data-analytics-id="nxrm-global-switch-classic"
                        onClick={switchToClassicUI}
                      >
                        <Flex align="center" gap="2">
                          <Repeat size={16} aria-hidden />
                          Switch to Classic UI
                        </Flex>
                      </Button>
                    </Tooltip>
                  ) : (
                    <Popover.Root open={uiSwitchPopoverOpen} onOpenChange={handleUiSwitchPopoverOpenChange}>
                      <Tooltip content={uiSwitchPopoverOpen ? undefined : "Switch to Classic UI"}>
                        <Popover.Trigger asChild>
                          <Button
                            variant="soft"
                            color="green"
                            className="nxrm-global-header-radix__clickable"
                            aria-label="Switch to Classic UI"
                            aria-haspopup="dialog"
                            title="Switch to Classic UI"
                            data-testid="ui-toggle-button"
                            data-analytics-id="nxrm-global-switch-classic"
                          >
                            <Flex align="center" gap="2">
                              <Repeat size={16} aria-hidden />
                              Switch to Classic UI
                            </Flex>
                          </Button>
                        </Popover.Trigger>
                      </Tooltip>
                      <Popover.Content
                        width="360px"
                        aria-labelledby="ui-switch-popover-title"
                        aria-describedby="ui-switch-popover-desc"
                        onOpenAutoFocus={handlePopoverOpenAutoFocus}
                      >
                        <Flex direction="column" gap="4">
                          <Box>
                            <Heading id="ui-switch-popover-title" size="3" mb="2">Trying the new Nexus One UI</Heading>
                            <Text id="ui-switch-popover-desc" size="2" color="gray" as="p">
                              Prefer the classic experience? Let us know why you&apos;re switching back.
                            </Text>
                          </Box>
                          {renderUiSwitchFeedbackControls({buttonRef: useClassicUiButtonRef})}
                        </Flex>
                      </Popover.Content>
                    </Popover.Root>
                  )
                ) : (
                  // Below md the switch folds into the user menu; above md it stays a header button.
                  !isMdUp ? null : (
                    <Tooltip content="Switch to Nexus One UI">
                      <Button
                        variant="soft"
                        color="green"
                        onClick={switchToNexusOneUI}
                        className="nxrm-global-header-radix__clickable"
                        aria-label="Switch to Nexus One UI"
                        title="Switch to Nexus One UI"
                        data-testid="ui-toggle-button"
                        data-analytics-id="nxrm-global-switch-preview"
                      >
                        <Flex align="center" gap="2">
                          <Repeat size={16} aria-hidden />
                          Switch to Nexus One UI
                        </Flex>
                      </Button>
                    </Tooltip>
                  )
                )
              )}

              {isPreviewUI && isMdUp && <ThemeSwitcher />}

              <SystemStatusBell className="nxrm-system-status" dataAnalyticsId="nxrm-global-header-system-status" />

              {/* Refresh is hidden below xs by design: at that width users reload the browser instead. */}
              {isXsUp && (
                <Tooltip content="Refresh">
                  <IconButton
                    variant="outline"
                    size="2"
                    color="gray"
                    aria-label="Refresh"
                    data-analytics-id="nxrm-header-refresh"
                    onClick={onRefreshClick}
                  >
                    <RefreshCw size={16} />
                  </IconButton>
                </Tooltip>
              )}

              <HelpMenu />

              <LoginAndUserMenu
                routes={routes}
                accountMenuLabel={accountMenuLabel}
                showThemeItem={foldThemeIntoMenu}
                showUiSwitchItem={foldUiSwitchIntoMenu}
                onUiSwitch={onFoldedUiSwitch}
                showNexusOneSwitchItem={foldNexusOneSwitchIntoMenu}
                onNexusOneSwitch={switchToNexusOneUI}
              />
            </Flex>
          </Flex>

          {/* Search own-row slot: below md only (the inline slot above renders it at md+) */}
          {!isMdUp && (
            <Box className="nxrm-global-header-radix__search nxrm-global-header-radix__search--own-row">
              <SearchRadix isPreviewUI={isPreviewUI} routes={searchRoutes} />
            </Box>
          )}
        </Flex>
      </Box>

      {/* Classic UI unavailable – shown when current Preview route has no Classic equivalent */}
      <Dialog.Root open={heritageUnavailableOpen} onOpenChange={(open) => !open && setHeritageUnavailableOpen(false)}>
        <Dialog.Content maxWidth="450px" aria-describedby={undefined}>
          <Dialog.Title>Not Available in Classic UI</Dialog.Title>
          <Text size="2" color="gray" as="p" mt="2">
            This view is only available in the Nexus One UI. The Classic UI doesn&apos;t have an equivalent page for this feature.
          </Text>
          <Flex gap="3" mt="4" justify="end">
            <Dialog.Close>
              <Button variant="soft" color="gray" size="2">Stay on Nexus One UI</Button>
            </Dialog.Close>
            <Button
              variant="solid"
              color="blue"
              size="2"
              onClick={() => {
                setHeritageUnavailableOpen(false);
                sessionStorage.setItem('user_requested_legacy', 'true');
                setTheme('light');
                window.location.hash = 'browse/welcome';
              }}
            >
              Go to Classic Dashboard
            </Button>
          </Flex>
        </Dialog.Content>
      </Dialog.Root>

      {/* Feedback prompt for the folded switch, mirroring the md+ header button's Popover. */}
      <Dialog.Root open={uiSwitchDialogOpen} onOpenChange={handleUiSwitchDialogOpenChange}>
        <Dialog.Content maxWidth="360px">
          <Flex direction="column" gap="4">
            <Box>
              <Dialog.Title size="3" mb="2">Trying the new Nexus One UI</Dialog.Title>
              <Dialog.Description size="2" color="gray">
                Prefer the classic experience? Let us know why you&apos;re switching back.
              </Dialog.Description>
            </Box>
            {renderUiSwitchFeedbackControls()}
          </Flex>
        </Dialog.Content>
      </Dialog.Root>
    </div>
  );
}
