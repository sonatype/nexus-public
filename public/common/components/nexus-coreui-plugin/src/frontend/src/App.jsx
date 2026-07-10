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
import React, { Suspense, useState, useEffect, useLayoutEffect } from 'react';
import DOMPurify from 'dompurify';
import { createRoot } from 'react-dom/client';
import { UIRouter, UIView, useRouter } from '@uirouter/react';
import { ExtJS, UnsavedChangesModal, RouteLoadingFallback, ToastProvider, useToast, bootstrapFromREST, SessionExpiryModal, useSessionExpiry, TooltipContainerProvider as NuiTooltipContainerProvider } from '@sonatype/nexus-ui-plugin';
import LeftNavigationMenuRadix from './components/LeftNavigationMenu/LeftNavigationMenuRadix';
import { Theme, AlertDialog, Button, Flex } from '@radix-ui/themes';
import { TooltipContainerProvider } from './components/shared/Tooltip/TooltipContainerContext';
import { getRouter } from './routerConfig/routerConfig';
import { ROUTE_NAMES } from './routerConfig/routeNames/routeNames';
import GlobalHeaderRadix from './components/GlobalHeader/GlobalHeaderRadix';
import { ThemeProvider, useTheme } from './contexts/ThemeContext';

import './App.scss';
import './components/shared/Toast.scss';
import SystemNotices from './components/widgets/SystemStatusAlerts/SystemNotices';
import UpgradeModal from './components/pages/user/Welcome/UpgradeModal';
import { useRedirectOnLogout } from './hooks/useRedirectOnLogout';
import usePreventPushStateOnHash from './hooks/usePreventPushStateOnHash';

// WHOLE_DOCUMENT allows DOMPurify to parse <head> and preserve <style> tags that customers
// place there; FORCE_BODY returns only body content so no <html>/<head> wrapper leaks into the DOM.
const BRANDING_SANITIZE_OPTIONS = Object.freeze({
  WHOLE_DOCUMENT: true,
  FORCE_BODY: true,
});

/**
 * Nexus One unsaved changes dialog for Preview UI router navigation.
 * Uses same Radix AlertDialog pattern as SettingsForm.jsx.
 */
let resolveUnsavedDialog = null;
let setUnsavedDialogOpen = () => {};

function PreviewUnsavedDialog() {
  const [open, setOpen] = useState(false);

  // useLayoutEffect so window.showPreviewUnsavedDialog exists before paint; UI-Router
  // onBefore can run in the same turn as a nav click and would otherwise fall back to ExtJS-style modal.
  useLayoutEffect(() => {
    setUnsavedDialogOpen = setOpen;
    window.showPreviewUnsavedDialog = () => {
      return new Promise((resolve) => {
        resolveUnsavedDialog = resolve;
        setOpen(true);
      });
    };
    return () => {
      delete window.showPreviewUnsavedDialog;
    };
  }, []);

  const handleLeave = () => {
    if (resolveUnsavedDialog) {
      resolveUnsavedDialog(true);
      resolveUnsavedDialog = null;
    }
    // Don't call setOpen(false) - AlertDialog.Action closes automatically
  };

  const handleStay = () => {
    if (resolveUnsavedDialog) {
      resolveUnsavedDialog(false);
      resolveUnsavedDialog = null;
    }
    // Don't call setOpen(false) - AlertDialog.Cancel closes automatically
  };

  return (
    <AlertDialog.Root open={open} onOpenChange={setOpen}>
      <AlertDialog.Content maxWidth="450px">
        <AlertDialog.Title>Unsaved Changes</AlertDialog.Title>
        <AlertDialog.Description size="2">
          You have unsaved changes. Are you sure you want to leave? Your changes will be lost.
        </AlertDialog.Description>
        <Flex gap="3" mt="4" justify="end">
          <AlertDialog.Cancel>
            <Button variant="soft" color="gray" onClick={handleStay}>Stay</Button>
          </AlertDialog.Cancel>
          <AlertDialog.Action>
            <Button variant="solid" color="red" onClick={handleLeave}>Leave</Button>
          </AlertDialog.Action>
        </Flex>
      </AlertDialog.Content>
    </AlertDialog.Root>
  );
}

/**
 * Global session-expired modal (REST 401). Mounted for all routes so interceptors can always notify.
 */
function SessionExpiryHost() {
  const { isExpired, message, hideExpiryModal } = useSessionExpiry();
  return (
    <SessionExpiryModal
      isOpen={isExpired}
      onClose={hideExpiryModal}
      message={message}
    />
  );
}

/**
 * AppWithRadixTheme - Connects ThemeProvider to Radix Theme.
 * Keeps the Radix-based UI improvements (GlobalHeaderRadix, LeftNavigationMenuRadix)
 * while using ExtJS for session/auth/state management.
 */
function AppWithRadixTheme() {
  const {effectiveTheme} = useTheme();

  return (
    <Theme appearance={effectiveTheme} accentColor="green" grayColor="slate" radius="medium">
      <TooltipContainerProvider>
        <NuiTooltipContainerProvider>
          <ToastProvider>
            <SessionExpiryHost />
            <App />
          </ToastProvider>
        </NuiTooltipContainerProvider>
      </TooltipContainerProvider>
    </Theme>
  );
}

export function App() {
  useRedirectOnLogout();
  usePreventPushStateOnHash();
  const {effectiveTheme} = useTheme();
  const toast = useToast();
  useLayoutEffect(() => {
    window.__nexusToast = toast;
    return () => { delete window.__nexusToast; };
  }, [toast]);
  const router = useRouter();
  const currentStateName = router.globals.$current.name || '';

  const isLoginRoute = currentStateName === ROUTE_NAMES.LOGIN;

  // Test hub routes are standalone - no sidebar, no header
  const isTestHubRoute = currentStateName.startsWith('preview.test');

  // Read branding from ExtJS state (available because we wait for ExtJS before rendering)
  const branding = ExtJS.state().getValue('branding');

  const headerEnabled = branding?.headerEnabled;
  const headerHtml = branding?.headerHtml;
  const footerEnabled = branding?.footerEnabled;
  const footerHtml = branding?.footerHtml;

  // Render minimal layout for login route
  if (isLoginRoute) {
    return <UIView />;
  }

  // Render minimal layout for test hub routes (standalone, no navigation)
  // SystemNotices renders ABOVE content (not in grid) since test hub uses full-width layout
  if (isTestHubRoute) {
    return (
      <div className="nxrm-page nxrm-test-hub-page">
        <SystemNotices />
        <div className="nxrm-main-content nxrm-main-content--full-width">
          <Theme appearance={effectiveTheme} accentColor="green" grayColor="slate" radius="medium">
            <CoreuiToastProvider>
              <Suspense fallback={<RouteLoadingFallback />}>
                <UIView />
              </Suspense>
            </CoreuiToastProvider>
          </Theme>
        </div>
      </div>
    );
  }

  // Render standard layout for all other routes
  return (
    <>
      <SystemNotices />

      {headerEnabled && (
        <div
          className="nxrm-branding-header"
          data-testid="nxrm-branding-header"
          dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(headerHtml, BRANDING_SANITIZE_OPTIONS) }}
        />
      )}

      <GlobalHeaderRadix />

      <LeftNavigationMenuRadix />

      <UpgradeModal />

      <UnsavedChangesModal/>

      <PreviewUnsavedDialog />

      <div className="nxrm-main-content">
        <Theme appearance={effectiveTheme} accentColor="blue" grayColor="slate" radius="medium">
          <Suspense fallback={<RouteLoadingFallback />}>
            <UIView />
          </Suspense>
        </Theme>
      </div>

      {footerEnabled && (
        <div
          className="nxrm-branding-footer"
          data-testid="nxrm-branding-footer"
          dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(footerHtml, BRANDING_SANITIZE_OPTIONS) }}
        />
      )}
    </>
  );
}

/**
 * Phase 2: React-First Bootstrap with REST Auth (NEXUS-52583)
 *
 * 1. ExtJS.waitForExtJs() now returns as soon as ExtJS app initializes
 *    (does NOT wait for permissions to load)
 * 2. React renders immediately with empty/REST-bootstrap permissions
 * 3. Permissions load asynchronously in background
 * 4. Components update via permission change events
 *
 * This enables UI interactivity within ~2 seconds even for users with 400+ roles,
 * where permission loading can take 10-15 seconds.
 */
function renderApp() {
  if (window.__nxAppRendered) {
    return;
  }
  window.__nxAppRendered = true;

  try {
    const router = getRouter();
    const el = document.createElement('div');
    el.className = 'nx-page nxrm-page';
    document.body.appendChild(el);
    const root = createRoot(el);

    root.render(
    <ThemeProvider>
      <UIRouter router={router}>
        <AppWithRadixTheme />
      </UIRouter>
    </ThemeProvider>
  );
  } catch (err) {
    window.__nxAppRendered = false;
  }
}

/**
 * Phase 2: React Shell with Non-Blocking Bootstrap (NEXUS-52583)
 *
 * Boot sequence uses ExtJS.waitForExtJs() which now returns immediately
 * when ExtJS initializes (not waiting for permissions).
 *
 * Fallbacks in ExtJS.js handle permission checks during loading:
 *   - ExtJS.checkPermission() reads from __nxRestBootstrap when NX.Permissions isn't ready
 *   - Components can use ExtJS.arePermissionsReady() to check loading state
 *
 * This enables immediate UI rendering even for users with 400+ roles
 * where permission loading can take 10-15 seconds on cold cache.
 */
// Build identifier - check in browser console: window.__nxBuild
window.__nxBuild = typeof __NX_BUILD_COMMIT__ !== 'undefined' ? __NX_BUILD_COMMIT__ : 'dev';

/** Timeout (ms) before falling back to REST bootstrap when ExtJS fails to load */
const EXTJS_TIMEOUT_MS = 15000;

let extJsResolved = false;

function bootstrapAndRender() {
  if (extJsResolved) return;
  extJsResolved = true;
  renderApp();
}

ExtJS.waitForExtJs(() => bootstrapAndRender());

// Fallback: if ExtJS never loads (e.g. app.js 404, script error), bootstrap from REST and render
setTimeout(() => {
  if (extJsResolved) return;
  bootstrapFromREST()
    .then(() => bootstrapAndRender())
    .catch((err) => {
      bootstrapAndRender();
    });
}, EXTJS_TIMEOUT_MS);
