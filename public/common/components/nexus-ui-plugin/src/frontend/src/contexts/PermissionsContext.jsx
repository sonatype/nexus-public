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

import React, {createContext, useContext, useEffect, useRef, useState, useCallback} from 'react';
import {isExtJSLoaded, onExtJSLoad} from '../utils/extJsLoader';

/**
 * PermissionsContext provides permission checking for the application.
 *
 * Phase 2 (NEXUS-52583): Non-blocking permission loading.
 * - UI renders immediately without waiting for permissions
 * - Permissions load asynchronously in background
 * - Components can check loading state via usePermissionsLoading()
 * - Permission checks return false (restrictive) until permissions are loaded
 */

const PermissionsContext = createContext({
  checkPermission: () => false,
  hasPermission: () => false
});

/**
 * Context for tracking permission loading state.
 * Components can use this to show loading indicators or handle the
 * async nature of permission loading (NEXUS-52583).
 */
const PermissionsLoadingContext = createContext({
  isLoading: true,
  error: null
});

/**
 * Check if permissions are ready (loaded from backend).
 * @returns {boolean}
 */
function arePermissionsReady() {
  return window.NX?.Permissions?.permissions !== undefined;
}

export function PermissionsProvider({ children }) {
  const [permissionsReady, setPermissionsReady] = useState(isExtJSLoaded() && arePermissionsReady());
  const [isLoading, setIsLoading] = useState(!arePermissionsReady());
  const [error, setError] = useState(null);

  useEffect(() => {
    let mounted = true;
    // Track ExtJS cleanup so listeners registered asynchronously are still removed on unmount.
    let cleanupExtJS = null;

    const setupExtJSListeners = () => {
      // Guard: component may have unmounted before ExtJS fired onExtJSLoad callback.
      if (!mounted) return;
      const app = window.Ext?.getApplication?.();
      if (!app) return;

      const permissionsController = app.getController?.('Permissions');
      const stateController = app.getController?.('State');

      // Named handlers so they can be un-registered in the cleanup function.
      const onLoading = () => { setIsLoading(true); setError(null); };
      const onChanged = () => { setIsLoading(false); setPermissionsReady(true); setError(null); };
      const onLoaderror = (operation) => {
        setIsLoading(false);
        setError(operation?.error || 'Failed to load permissions');
      };
      const onUserchanged = () => { setIsLoading(true); setPermissionsReady(false); };

      permissionsController?.on?.('loading', onLoading);
      permissionsController?.on?.('changed', onChanged);
      permissionsController?.on?.('loaderror', onLoaderror);
      stateController?.on?.('userchanged', onUserchanged);

      cleanupExtJS = () => {
        permissionsController?.un?.('loading', onLoading);
        permissionsController?.un?.('changed', onChanged);
        permissionsController?.un?.('loaderror', onLoaderror);
        stateController?.un?.('userchanged', onUserchanged);
      };
    };

    if (isExtJSLoaded()) {
      setupExtJSListeners();
    } else {
      onExtJSLoad(setupExtJSListeners);
    }

    return () => {
      mounted = false;
      cleanupExtJS?.();
    };
  }, []);

  // Reads window globals at call time — no state deps needed, identity is stable.
  const checkPermission = useCallback((permission) => {
    if (isExtJSLoaded() && window.NX?.Permissions?.check) {
      return window.NX.Permissions.check(permission);
    }
    return false;
  }, []);

  const hasPermission = checkPermission;

  const value = {
    checkPermission,
    hasPermission
  };

  const loadingValue = {
    isLoading,
    error,
    permissionsReady
  };

  return (
    <PermissionsLoadingContext.Provider value={loadingValue}>
      <PermissionsContext.Provider value={value}>
        {children}
      </PermissionsContext.Provider>
    </PermissionsLoadingContext.Provider>
  );
}

export function usePermissions() {
  const context = useContext(PermissionsContext);
  if (!context) {
    throw new Error('usePermissions must be used within a PermissionsProvider');
  }
  return context;
}

/**
 * Hook to check permission loading state.
 * Use this to show loading indicators while permissions are being fetched.
 *
 * @returns {{isLoading: boolean, error: Error|null, permissionsReady: boolean}}
 */
export function usePermissionsLoading() {
  return useContext(PermissionsLoadingContext);
}

/**
 * Reactive hook for permission checking. Re-evaluates when permissions change
 * (user login/logout). Returns false (restrictive) while permissions are loading.
 *
 * @param {string} permission - The permission string to check
 * @returns {boolean} Whether the current user has the permission
 */
export function usePermission(permission) {
  const { checkPermission } = usePermissions();
  // Ref keeps the latest checkPermission without triggering re-subscription on every render.
  const checkPermissionRef = useRef(checkPermission);
  checkPermissionRef.current = checkPermission;

  const [hasPermissionValue, setHasPermissionValue] = useState(() => checkPermission(permission));

  useEffect(() => {
    // cleanupListeners is assigned by registerListeners() once ExtJS is available.
    // It may be set synchronously (ExtJS already loaded) or asynchronously (via onExtJSLoad).
    let cleanupListeners = null;

    function registerListeners() {
      const app = window.Ext?.getApplication?.();
      if (!app) return;

      const permissionsController = app.getController('Permissions');
      const stateController = app.getController('State');

      function handleChange() {
        setHasPermissionValue(checkPermissionRef.current(permission));
      }

      // Re-evaluate now that ExtJS is available (covers the pre-load mount window).
      setHasPermissionValue(checkPermissionRef.current(permission));

      permissionsController.on('changed', handleChange);
      stateController.on('userchanged', handleChange);

      cleanupListeners = () => {
        permissionsController.un('changed', handleChange);
        stateController.un('userchanged', handleChange);
      };
    }

    if (isExtJSLoaded()) {
      registerListeners();
    } else {
      // Component mounted before ExtJS was ready — defer listener registration so that
      // login/logout events after ExtJS loads are still handled (NEXUS-52583).
      onExtJSLoad(registerListeners);
    }

    return () => {
      cleanupListeners?.();
    };
  }, [permission]); // checkPermission intentionally omitted — accessed via stable ref above

  return hasPermissionValue;
}

export default PermissionsContext;
