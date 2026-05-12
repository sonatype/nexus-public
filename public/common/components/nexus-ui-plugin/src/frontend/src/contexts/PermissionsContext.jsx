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

import React, {createContext, useContext, useEffect, useState, useCallback} from 'react';
import {isExtJSLoaded, onExtJSLoad} from '../utils/extJsLoader';

/**
 * PermissionsContext provides permission checking for the application.
 *
 * Phase 1: React Shell - This context bridges React and ExtJS permissions.
 * When ExtJS is loaded, it delegates to NX.Permissions.check().
 * When ExtJS is not loaded, permissions default to restrictive (false).
 *
 * Future phases will fetch permissions directly from the API.
 */

const PermissionsContext = createContext({
  checkPermission: () => false,
  hasPermission: () => false,
  usePermission: () => false
});

export function PermissionsProvider({ children }) {
  const [permissionsReady, setPermissionsReady] = useState(isExtJSLoaded());

  useEffect(() => {
    onExtJSLoad(() => {
      setPermissionsReady(true);
    });
  }, []);

  const checkPermission = useCallback((permission) => {
    if (isExtJSLoaded() && window.NX?.Permissions?.check) {
      return window.NX.Permissions.check(permission);
    }
    // Default to false when ExtJS not loaded (restrictive)
    return false;
  }, [permissionsReady]);

  const hasPermission = checkPermission;

  // Hook for permission checking with reactivity
  const usePermission = (permission) => {
    const [hasPermissionValue, setHasPermissionValue] = useState(() => checkPermission(permission));

    useEffect(() => {
      if (!isExtJSLoaded()) {
        // Wait for ExtJS to load
        onExtJSLoad(() => {
          setHasPermissionValue(checkPermission(permission));
        });
        return;
      }

      function handleChange() {
        setHasPermissionValue(checkPermission(permission));
      }

      const permissionsController = window.Ext.getApplication().getController('Permissions');
      permissionsController.on('changed', handleChange);

      const stateController = window.Ext.getApplication().getController('State');
      stateController.on('userchanged', handleChange);

      return () => {
        permissionsController.un('changed', handleChange);
        stateController.un('userchanged', handleChange);
      };
    }, [permission]);

    return hasPermissionValue;
  };

  const value = {
    checkPermission,
    hasPermission,
    usePermission
  };

  return (
    <PermissionsContext.Provider value={value}>
      {children}
    </PermissionsContext.Provider>
  );
}

export function usePermissions() {
  const context = useContext(PermissionsContext);
  if (!context) {
    throw new Error('usePermissions must be used within a PermissionsProvider');
  }
  return context;
}

export default PermissionsContext;
