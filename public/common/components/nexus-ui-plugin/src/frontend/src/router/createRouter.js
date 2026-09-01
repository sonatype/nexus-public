/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
import {UIRouterReact, hashLocationPlugin, servicesPlugin} from '@uirouter/react';
import ExtJS from '../interface/ExtJS';
import {isVisible} from '../interface/NavigationUtils';
import {showUnsavedChangesModal} from './unsavedChangesDialog';
import {RouteNames} from "../constants/RouteNames";

/**
 * Checks visibility for a state AND all its ancestor states.
 * If any ancestor has visibilityRequirements that fail, the child is also not visible.
 * This ensures that protected parent routes (e.g., preview.admin.pages with permission checks)
 * also protect all their child routes even if the child doesn't declare its own requirements.
 *
 * Only walks ancestors for Preview UI routes. Classic admin routes already declare their own
 * permissions per-route, and their parent `admin` gate uses requiresAnyPermission with a broad
 * list — walking that ancestor would require every child test to satisfy the parent too, which
 * breaks existing behavior where each child is self-contained.
 */
function isVisibleIncludingAncestors(state) {
  const statesToCheck = [];

  if (state.data?.visibilityRequirements) {
    statesToCheck.push(state.data.visibilityRequirements);
  }

  // Only walk ancestors for preview routes — classic routes are self-contained
  if (state.name && state.name.startsWith('preview.')) {
    let current = state.parent;
    while (current && current.name) {
      if (current.data?.visibilityRequirements) {
        statesToCheck.push(current.data.visibilityRequirements);
      }
      current = current.parent;
    }
  }

  if (statesToCheck.length === 0) {
    return true;
  }

  return statesToCheck.every((requirements) => isVisible(requirements));
}

function getTransitionFromStateName(transition) {
  const from = transition.from();
  if (!from) {
    return '';
  }
  if (typeof from === 'string') {
    return from;
  }
  return from.name || (from.state && from.state.name) || '';
}

function isPreviewUIRouteName(stateName) {
  return stateName === 'preview' || (stateName && stateName.startsWith('preview.'));
}

/** How long to keep watching for a user after redirecting to login. */
const LOGIN_RECOVERY_TIMEOUT_MS = 15000;

/** How long to keep watching for permissions after a visibility redirect. */
const PERMISSIONS_RECOVERY_TIMEOUT_MS = 30000;

/**
 * How long to let permissions land before deciding visibility.
 *
 * Short on purpose. This exists only to avoid a visible welcome-page flash in the
 * common case where permissions are milliseconds away — correctness comes from
 * recoverWhenPermissionsArrive, not from this wait. An earlier revision blocked here
 * for the full waitForPermissions() budget of 30s, which stalls navigation outright
 * when the permissions endpoint is slow or broken, and is not something a wait can
 * fix anyway: arePermissionsReady() reads true over an empty map, so the case that
 * actually breaks never reaches this branch (NEXUS-54422).
 */
const PERMISSIONS_SETTLE_MS = 500;

/**
 * Navigates to a returnTo target, or to the welcome page when there is none.
 *
 * @param router
 * @param returnTo Base64-encoded '#...' hash, as produced by the login redirect below
 */
function goToReturnTo(router, returnTo) {
  if (returnTo) {
    let decoded = '';
    try {
      decoded = atob(returnTo);
    } catch {
      decoded = '';
    }
    if (decoded.startsWith('#') || decoded.startsWith('/')) {
      router.urlService.url(decoded);
      return;
    }
  }
  router.stateService.go(RouteNames.WELCOME);
}

/**
 * Preview UI uses Radix AlertDialog via window.showPreviewUnsavedDialog (registered from coreui App).
 * Polls with escalating delays (up to ~216ms total) to handle mount timing edge cases.
 * Falls back to classic modal only as a last resort.
 */
export async function promptUnsavedChangesForNavigation(isPreviewRoute) {
  if (!isPreviewRoute) {
    return showUnsavedChangesModal();
  }
  const tryPreview = () =>
      typeof window.showPreviewUnsavedDialog === 'function'
        ? window.showPreviewUnsavedDialog()
        : null;

  const delays = [0, 0, 16, 50, 150];
  for (const ms of delays) {
    if (ms === 0) {
      await Promise.resolve();
    } else {
      await new Promise((r) => setTimeout(r, ms));
    }
    const p = tryPreview();
    if (p) return p;
  }

  console.warn(
    '[UnsavedChanges] Preview route but showPreviewUnsavedDialog unavailable after retries. ' +
    'Falling back to classic modal. window.showPreviewUnsavedDialog:',
    typeof window.showPreviewUnsavedDialog
  );
  return showUnsavedChangesModal();
}

/*
  This is where we register all of the client side routes. Routing is handled using
  UIRouter. For more info see https://github.com/ui-router/react.

  See also private/developer-documentation/frontend/client-side-routing.md
 */
export function createRouter({initialRoute, menuRoutes, missingRoute}) {
  const router = new UIRouterReact();
  router.plugin(servicesPlugin);
  router.plugin(hashLocationPlugin);

  // Only set the initial route when there is no meaningful hash. On browser refresh
  // the hash is already present, and hashLocationPlugin will sync it to router state;
  // setting initial() here would override that and redirect to the welcome page.
  const currentHash = window.location.hash;
  if (!currentHash || currentHash === '#' || currentHash === '#/') {
    router.urlService.rules.initial({state: initialRoute});
  }

  let recoveryArmed = false;

  /**
   * Watches for a user arriving after the router already sent them to login, then resumes the
   * original destination (NEXUS-54290).
   *
   * @param returnTo Base64-encoded '#...' hash of the destination to resume, if any
   */
  function recoverWhenUserArrives(returnTo) {
    if (recoveryArmed) {
      return;
    }

    let timer;
    let unsubscribe;

    function stop() {
      recoveryArmed = false;
      clearTimeout(timer);
      unsubscribe?.();
    }

    function onUserChanged() {
      if (!ExtJS.hasUser()) {
        return;
      }
      stop();
      if (router.stateService.current.name !== RouteNames.LOGIN) {
        return;
      }
      console.debug('User state arrived after the login redirect, resuming original destination');
      goToReturnTo(router, returnTo);
    }

    unsubscribe = ExtJS.onStateChange(['userchanged'], onUserChanged);
    if (!unsubscribe) {
      return;
    }
    recoveryArmed = true;
    timer = setTimeout(stop, LOGIN_RECOVERY_TIMEOUT_MS);
  }

  let permissionsRecoveryArmed = false;

  /**
   * Watches for permissions arriving after the router already redirected away from a
   * permission-gated route, then resumes the original destination (NEXUS-54422).
   *
   * The visibility check in the hook below is a single evaluation made at a
   * nondeterministic point in bootstrap, and a permission-gated route can lose it for
   * reasons that have nothing to do with the user's actual rights:
   *
   *  - `NX.Permissions.permissions` is still `undefined`, so every check() is false; or
   *  - an empty map has been installed, which makes arePermissionsReady() read true
   *    while every check() is still false.
   *
   * Waiting before deciding (see the gate in the hook) narrows the first case but cannot
   * close either, because "are permissions loaded" is not answerable from a boolean that
   * reads true over an empty map. So rather than trying to win that race, this restores
   * the destination once permissions actually change — the same shape as
   * {@link recoverWhenUserArrives} for auth state, and immune to the timing entirely.
   *
   * The captured hash is replayed rather than `stateService.go(name, params)`: the search
   * page writes undeclared params (e.g. `nameOrVersion`) with raw pushState, and
   * UI-Router rebuilds a URL from declared params only, so a `go` would silently drop
   * them. Assigning the hash reaches the router as a URL-sourced sync, which it does not
   * rewrite — the same reason GADetailPage's breadcrumb assigns the hash directly.
   *
   * @param stateTo the StateObject the router refused to enter
   * @param originalHash window.location.hash captured before the redirect
   * @param landedOn state name the redirect sent the user to
   */
  function recoverWhenPermissionsArrive(stateTo, originalHash, landedOn) {
    if (permissionsRecoveryArmed) {
      return;
    }

    let timer;
    let unsubscribe;

    function stop() {
      permissionsRecoveryArmed = false;
      clearTimeout(timer);
      unsubscribe?.();
    }

    function onPermissionsChanged() {
      if (!isVisibleIncludingAncestors(stateTo)) {
        // Permissions changed but still do not grant this route. Keep waiting: an
        // empty install fires 'changed' too, and the real set may follow it.
        return;
      }
      stop();
      // Do not hijack navigation if the user has already moved on themselves.
      if (router.stateService.current.name !== landedOn) {
        return;
      }
      console.debug('Permissions arrived after the visibility redirect, resuming', stateTo.name);
      window.location.hash = originalHash;
    }

    unsubscribe = ExtJS.onPermissionsChange(onPermissionsChanged);
    if (!unsubscribe) {
      return;
    }
    permissionsRecoveryArmed = true;
    timer = setTimeout(stop, PERMISSIONS_RECOVERY_TIMEOUT_MS);
  }

  // validate permissions and configuration on each route request
  router.transitionService.onBefore({}, async (transition) => {
    // $to() returns the internal StateObject with a resolved .parent chain;
    // .to() returns only the StateDeclaration (no .parent), which would break
    // isVisibleIncludingAncestors' ancestor walk.
    const stateTo = transition.$to();
    const stateFrom = transition.from();
    console.debug(`evaluating transition from ${stateFrom.name} to ${stateTo.name}`);

    if (stateTo.name === RouteNames.LOGIN) {
      if (ExtJS.hasUser()) {
        console.debug('User is already authenticated, redirecting away from login page');
        transition.abort();
        router.stateService.go(RouteNames.WELCOME);
        return;
      }
      // Stay on login while the auth state is in flight, but leave it if a user turns up
      if (!ExtJS.isAuthStateResolved()) {
        recoverWhenUserArrives(transition.params()?.returnTo);
      }
    }

    if (!isVisibleIncludingAncestors(stateTo)) {
      // Wait for an authoritative auth state before treating the visit as unauthenticated,
      // since a user-gated route is not resolvable without one (NEXUS-54290).
      const authResolved = ExtJS.isAuthStateResolved();
      if (!authResolved) {
        console.debug('Auth state unresolved, waiting for it before evaluating visibility');
        await ExtJS.whenAuthStateResolved();
        if (isVisibleIncludingAncestors(stateTo)) {
          return;
        }
      }

      // isAuthStateResolved() answers "is a user signed in", not "have their permissions
      // loaded" — NEXUS-52583's fast-render bootstrap deliberately lets the router run
      // before NX.Permissions is populated, so a permission-gated route (e.g. unified
      // search's nexus:search:read) can fail this check purely because the answer to
      // that permission has not arrived yet (NEXUS-54422).
      //
      // Bounded deliberately: this only smooths over the case where permissions are
      // milliseconds away, so the user does not see a welcome-page flash. It is NOT the
      // correctness mechanism — recoverWhenPermissionsArrive below is. Blocking here for
      // waitForPermissions()'s full 30s default stalled navigation whenever the
      // permissions endpoint was slow, and could not fix the observed failure anyway:
      // that one installs an empty map, which makes arePermissionsReady() read true and
      // skips this branch entirely.
      if (!ExtJS.arePermissionsReady()) {
        console.debug('Permissions not yet loaded, briefly waiting before evaluating visibility');
        await ExtJS.waitForPermissions(PERMISSIONS_SETTLE_MS).catch(() => {});
        if (isVisibleIncludingAncestors(stateTo)) {
          return;
        }
      }

      if (!ExtJS.hasUser()) {
        transition.abort();

        // If the user is already on the login page, silently abort the transition.
        // No redirect or feedback needed — the user is already where they should be
        // and can authenticate to gain access. This avoids redirect loops.
        if (stateFrom.name === RouteNames.LOGIN) {
          return;
        }

        console.debug('Redirecting to login page with return URL');
        // urlService.url() returns the path without '#' (e.g., '/admin/security/roles').
        // We prepend '#' before Base64-encoding so the decoded value can be used directly
        // as window.location.hash to restore the full route after login.
        const url = router.urlService.url();
        const returnTo = url ? btoa(`#${url}`) : null;
        if (returnTo) {
          router.stateService.go(RouteNames.LOGIN, {returnTo});
        } else {
          router.stateService.go(RouteNames.LOGIN);
        }

        // The wait above expired without an answer; keep watching for a late user
        if (!authResolved) {
          recoverWhenUserArrives(returnTo);
        }
      } else {
        console.warn('state is not visible for navigation, redirecting to dashboard', stateTo.name);
        // Captured before the redirect rewrites it, so the destination can be restored
        // verbatim (including params the route does not declare) if permissions turn
        // out to grant this route after all (NEXUS-54422).
        const originalHash = window.location.hash;
        transition.abort();
        const welcomeRoute = isPreviewUIRouteName(stateTo.name) ? 'preview.browse.welcome' : RouteNames.WELCOME;
        // Arm before navigating: permissions can land during the redirect itself.
        recoverWhenPermissionsArrive(stateTo, originalHash, welcomeRoute);
        router.stateService.go(welcomeRoute);
      }
    }
  });

  // show the unsaved changes dialog when navigating away from a page with unsaved changes
  router.transitionService.onBefore(
      {},
      async (transition) => {
        const hasUnsavedChanges = window.dirty && window.dirty.length > 0;
        if (!hasUnsavedChanges) {
          return true;
        }

        const fromState = getTransitionFromStateName(transition);
        const isPreviewRoute = isPreviewUIRouteName(fromState);

        console.debug(
          '[UnsavedChanges] Dirty forms:', window.dirty,
          '| from:', fromState,
          '| isPreview:', isPreviewRoute,
          '| hasRadixDialog:', typeof window.showPreviewUnsavedDialog === 'function'
        );

        const confirm = await promptUnsavedChangesForNavigation(isPreviewRoute);
        if (!confirm) {
          return false;
        }
        window.dirty = [];
        return true;
      },
      {priority: 1000}
  ); // this hook given a high priority to ensure it runs first

  // validate permissions and configuration on each route request
  router.transitionService.onSuccess({}, async (transition) => {
    const customTitle = ExtJS.state().getValue('uiSettings')?.title || 'Nexus Repository Manager';
    const {title: currentPageTitle} = transition.to().data;
    const title = currentPageTitle ? `${currentPageTitle} - ${customTitle}` : customTitle;
    document.title = title;
  });

  menuRoutes.forEach((route) => router.stateRegistry.register(route));

  // explicitely register the missing route
  router.stateRegistry.register(missingRoute);
  // send any unrecognized routes to the 404 page, but first try to salvage SPA paths without a hash
  router.urlService.rules.otherwise((matchValue, urlParts, router) => {
    // If the browser URL has a path but no hash (e.g., /admin/system/tasks), rewrite to a hash URL once.
    if (typeof window !== 'undefined' && window.location && !window.location.hash && matchValue?.path) {
      const rawPath = matchValue.path.startsWith('/') ? matchValue.path : `/${matchValue.path}`;
      const newHash = `#${rawPath}`;
      console.warn('url not recognized, rewriting to hash route:', newHash, matchValue);
      window.location.hash = newHash;
      return;
    }

    // ExtJS compatibility: URLs with = in the first path segment (e.g., browse/search=format%3Dnpm:repo:id)
    // are ExtJS bookmark URLs that React Router can't match. Let ExtJS handle them instead of showing 404.
    const hash = window.location?.hash?.replace(/^#\/?/, '') || '';
    if (hash.includes('=') && !hash.startsWith('preview/')) {
      console.debug('Unrecognized URL appears to be an ExtJS bookmark, deferring to ExtJS:', hash);
      return;
    }

    // Unrecognized URL in a protected area: if user is NOT authenticated,
    // redirect to login so they can sign in and be routed back.
    // We prefer NX.Security.hasUser() because it reflects runtime auth state
    // (updated after SPA login). We only fall back to bootstrap data when
    // NX.Security is not yet available (very early page load).
    const rawPath = (typeof matchValue === 'object' && matchValue?.path) || hash;
    const path = (rawPath || '').replace(/^\//, '');
    const isProtectedPath = path && (path.startsWith('admin') || path.startsWith('preview/') || path.startsWith('user/'));
    const isAuthenticated = (typeof window.NX?.Security?.hasUser === 'function')
        ? window.NX.Security.hasUser()
        : (window.__nxRestBootstrap?.user?.authenticated === true);

    if (isProtectedPath && !isAuthenticated) {
      console.debug('Unrecognized protected URL, redirecting to login:', path);
      const url = router.urlService.url();
      if (url) {
        const returnTo = btoa(`#${url}`);
        router.stateService.go(RouteNames.LOGIN, {returnTo});
      } else {
        router.stateService.go(RouteNames.LOGIN);
      }
      return;
    }

    console.warn('url not recognized', matchValue, urlParts);
    router.stateService.go(missingRoute.name, {}, {location: false});
  });

  // Phase 1: Start the router to build the state tree
  // Without this, states may not have their parent chains/paths initialized
  router.urlService.deferIntercept();

  return router;
}
