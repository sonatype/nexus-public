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

/**
 * Preview UI uses Radix AlertDialog via window.showPreviewUnsavedDialog (registered from coreui App).
 * Polls with escalating delays (up to ~216ms total) to handle mount timing edge cases.
 * Falls back to classic modal only as a last resort.
 */
async function promptUnsavedChangesForNavigation(isPreviewRoute) {
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
  router.urlService.rules.initial({state: initialRoute});

  // validate permissions and configuration on each route request
  router.transitionService.onBefore({}, async (transition) => {
    const redirectTo404 = () => {
      transition.abort();
      router.stateService.go(missingRoute.name);
    };

    const stateTo = transition.to();
    const stateFrom = transition.from();
    console.debug(`evaluating transition from ${stateFrom.name} to ${stateTo.name}`);

    if (stateTo.name === RouteNames.LOGIN && ExtJS.hasUser()) {
      console.debug('User is already authenticated, redirecting away from login page');
      transition.abort();
      router.stateService.go(RouteNames.WELCOME);
      return;
    }

    if (!isVisible(stateTo.data?.visibilityRequirements)) {
      if (!ExtJS.hasUser()) {
        transition.abort();
        const isAnonymousAccessEnabled = !!ExtJS.state().getValue('anonymousUsername');
        if (isAnonymousAccessEnabled && stateFrom.name === RouteNames.LOGIN) {
          console.warn('state is not visible for navigation after login, redirecting to 404');
          redirectTo404();
          return;
        }

        console.debug('Redirecting to login page with return URL');
        // Keep original requested URL and then encode to Base64
        const url = router.urlService.url();
        if (url) {
          const returnTo = btoa(`#${url}`);
          router.stateService.go(RouteNames.LOGIN, {returnTo});
        } else {
          router.stateService.go(RouteNames.LOGIN);
        }
      } else {
        console.warn('state is not visible for navigation, aborting transition', stateTo.name);
        redirectTo404();
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

    console.warn('url not recognized', matchValue, urlParts);
    router.stateService.go(missingRoute.name, {}, {location: false});
  });

  // Phase 1: Start the router to build the state tree
  // Without this, states may not have their parent chains/paths initialized
  router.urlService.deferIntercept();

  return router;
}
