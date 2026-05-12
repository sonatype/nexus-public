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

import React, { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import {Flex, Box, IconButton, Button, Dialog, Text, Popover, TextArea, Heading} from '@radix-ui/themes';
import {Tooltip} from '../shared';
import {RefreshCw, Repeat, Menu} from 'lucide-react';
import {ExtJS, handleExtJsUnsavedChanges, restClient} from '@sonatype/nexus-ui-plugin';
import {useRouter} from '@uirouter/react';
import {useTheme} from '../../contexts/ThemeContext';

import proLogo from "../../../../art/logos/logo-pro-edition-header.svg";
import proDarkLogo from "../../../../art/logos/logo-pro-edition-header-dark.svg";
import ceLogo from "../../../../art/logos/logo-community-edition-header.svg";
import ceDarkLogo from "../../../../art/logos/logo-community-edition-header-dark.svg";
import coreLogo from "../../../../art/logos/logo-core-edition-header.svg";
import coreDarkLogo from "../../../../art/logos/logo-core-edition-header-dark.svg";

import SearchRadix from './SearchRadix';
import ThemeSwitcher from '../ThemeSwitcher/ThemeSwitcher';
import HelpMenuRadix from './HelpMenuRadix';
import LoginAndUserButtonRadix from './LoginAndUserButtonRadix';
import useSideNavbarCollapsedState from '../../hooks/useSideNavbarCollapsedState';
import {refreshReactPage} from '../../routerConfig/routerUtils';
import {getHeritageEquivalent, heritageToPreviewPath} from './previewHeritageNavigation';

import './GlobalHeaderRadix.scss';

const COMMUNITY = "COMMUNITY";
const PRO = "PRO";
const MAX_FEEDBACK_LENGTH = 500;

export default function GlobalHeaderRadix() {
  const router = useRouter();
  const {effectiveTheme, setTheme} = useTheme();
  
  // Use useStatus().edition which is more reliable than state().getEdition()
  const status = ExtJS.useStatus();
  const edition = status?.edition;
  const version = status?.version;
  
  // Use useUser for authentication state - this is a stable hook
  const user = ExtJS.useUser();
  const isLoggedIn = user?.authenticated === true;
  const isAnonymous = !isLoggedIn;

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
  const [classicUiFeedback, setClassicUiFeedback] = useState('');
  const useClassicUiButtonRef = useRef(null);

  const isFeedbackTooLong = classicUiFeedback.length > MAX_FEEDBACK_LENGTH;

  useEffect(() => {
    try {
      const state = ExtJS.state();
      if (!state) return;

      const disableLegacyUi = state.getValue('disableLegacyUi', false) ?? false;
      setDisableLegacyUi(disableLegacyUi);
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
  
  // Redirect logic for rollout controls (Sprint 17)
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
      const hashPath = `#${newPath}`;
      window.location.hash = hashPath;
      return;
    }

    // 2. SOFT DEFAULT: Check defaultToPreviewUi second
    if (isDefaultToPreviewUi && !isPreviewUI && isLandingPage) {
      const userRequestedLegacy = sessionStorage.getItem('user_requested_legacy') === 'true';
      if (!userRequestedLegacy) {
        // Use hash-based navigation for Preview UI
        const hashPath = '#preview/browse/welcome';
        window.location.hash = hashPath;
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
  // granted → revoked. Never on initial render (accessEverGranted is false).
  useEffect(() => {
    if (isPreviewUI && !canAccessPreviewUi && accessEverGranted.current) {
      setTheme('light');
      const defaultPath = currentPath.replace(/^preview\//, '') || 'browse/welcome';
      window.location.hash = defaultPath;
    }
  }, [isPreviewUI, canAccessPreviewUi, currentPath, setTheme]);
  
  const switchToClassicUI = useCallback(() => {
    const heritagePath = getHeritageEquivalent(currentPath);
    if (heritagePath === null) {
      setHeritageUnavailableOpen(true);
      setUiSwitchPopoverOpen(false);
      return;
    }

    if (!isDisableSwitchFeedback) {
      restClient.post('/service/rest/internal/ui/switch-feedback', {
        edition: edition ?? 'Unknown',
        version: version ?? 'Unknown',
        feedback: classicUiFeedback.trim().slice(0, 500),
      }).catch(() => {});
    }

    sessionStorage.setItem('user_requested_legacy', 'true');
    setTheme('light');
    setUiSwitchPopoverOpen(false);
    setClassicUiFeedback('');
    window.location.hash = heritagePath;
  }, [currentPath, classicUiFeedback, isDisableSwitchFeedback, setTheme, edition, version]);

  const switchToNexusOneUI = useCallback(() => {
    const newPath = heritageToPreviewPath(currentPath);
    window.location.hash = `#${newPath}`;
    sessionStorage.removeItem('user_requested_legacy');
  }, [currentPath]);

  const handleUiSwitchPopoverOpenChange = useCallback((open) => {
    setUiSwitchPopoverOpen(open);
    if (!open) {
      setClassicUiFeedback('');
    }
  }, []);

  const handlePopoverOpenAutoFocus = useCallback((e) => {
    e.preventDefault();
    // Use requestAnimationFrame so the button is mounted when we focus
    requestAnimationFrame(() => {
      useClassicUiButtonRef.current?.focus();
    });
  }, []);

  const onRefreshClick = () => {
    const menuCtrl =
      window.Ext && Ext.getApplication && Ext.getApplication().getController
        ? Ext.getApplication().getController('Menu')
        : null;

    handleExtJsUnsavedChanges(menuCtrl, () => {
      if (ExtJS.isExtJsRendered()) {
        ExtJS.refresh();
      } else {
        refreshReactPage(router);
      }
    });
  };

  function getLogo() {
    const isDark = effectiveTheme === 'dark';
    
    if (edition === COMMUNITY) {
      return isDark ? ceDarkLogo : ceLogo;
    } else if (edition === PRO) {
      return isDark ? proDarkLogo : proLogo;
    } else {
      return isDark ? coreDarkLogo : coreLogo;
    }
  }

  function getEditionText() {
    return edition === COMMUNITY ? "Community"
        : edition === PRO ? "Professional"
        : "Core";
  }

  const [isSidebarOpen] = useSideNavbarCollapsedState(false);
  const onSidebarToggle = useCallback(() => {
    window.dispatchEvent(new CustomEvent('nx-sidebar-toggle'));
  }, []);

  return (
    <div className="nxrm-global-header-radix nx-global-header-2" data-testid="global-header">
      <Box
        asChild
        style={{
          position: 'sticky',
          top: 0,
          zIndex: 50,
          width: '100%',
          paddingTop: 'var(--space-3)',
          paddingBottom: 'var(--space-3)',
          borderBottom: '1px solid var(--gray-6)',
          backgroundColor: 'var(--color-surface)',
        }}
      >
        <Flex align="center" justify="between" gap="4" px="4" style={{width: '100%'}}>
          {/* Left: Sidebar toggle + Logo */}
          <Flex align="center" gap="4" style={{flexShrink: 0}}>
            <Tooltip content={isSidebarOpen ? 'Expand Sidebar' : 'Collapse Sidebar'}>
              <IconButton
                variant="outline"
                size="2"
                color="gray"
                aria-label={isSidebarOpen ? 'Expand Sidebar' : 'Collapse Sidebar'}
                onClick={onSidebarToggle}
              >
                <Menu size={16} />
              </IconButton>
            </Tooltip>
            <Box asChild>
              <a href={contextPath || "/"} title="Home">
                <img
                  src={getLogo()}
                  alt={`Sonatype Nexus Repository ${getEditionText()}`}
                  style={{height: '32px', display: 'block'}}
                />
              </a>
            </Box>
          </Flex>

          {/* Center: Search Bar */}
          <Box
            style={{
              flex: '1 1 0',
              maxWidth: '600px',
              minWidth: '200px',
              display: 'flex',
              justifyContent: 'center',
            }}
          >
            <SearchRadix isPreviewUI={isPreviewUI} />
          </Box>

          {/* Right: Utility buttons */}
          <Flex align="center" gap="3" style={{flexShrink: 0}}>
            {canAccessPreviewUi && !isDisableLegacyUi && (
              isPreviewUI ? (
                isDisableSwitchFeedback ? (
                  <Tooltip content="Switch to Classic UI">
                    <Button
                      variant="soft"
                      color="green"
                      style={{cursor: 'pointer'}}
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
                          style={{cursor: 'pointer'}}
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
                            ref={useClassicUiButtonRef}
                            variant="soft"
                            color="green"
                            size="2"
                            style={{cursor: isFeedbackTooLong ? 'not-allowed' : 'pointer'}}
                            onClick={switchToClassicUI}
                            disabled={isFeedbackTooLong}
                            aria-label="Use Classic UI"
                            title="Use Classic UI"
                            data-analytics-id="nxrm-dialog-use-classic"
                          >
                            Use Classic UI
                          </Button>
                        </Flex>
                      </Flex>
                    </Popover.Content>
                  </Popover.Root>
                )
              ) : (
                <Tooltip content="Switch to Nexus One UI">
                  <Button
                    variant="soft"
                    color="green"
                    onClick={switchToNexusOneUI}
                    style={{cursor: 'pointer'}}
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
            )}

            {isPreviewUI && <ThemeSwitcher />}

            <Tooltip content="Refresh">
              <IconButton
                variant="outline"
                size="2"
                color="gray"
                aria-label="Refresh"
                onClick={onRefreshClick}
              >
                <RefreshCw size={16} />
              </IconButton>
            </Tooltip>

            <HelpMenuRadix />

            <LoginAndUserButtonRadix />
          </Flex>
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
    </div>
  );
}
