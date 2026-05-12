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

import React, {createContext, useContext, useEffect, useState} from 'react';
import Axios from 'axios';
import {isExtJSLoaded, onExtJSLoad} from '../utils/extJsLoader';

/**
 * AuthContext provides authentication state and methods for the application.
 *
 * Phase 1: React Shell - This context bridges React and ExtJS authentication.
 * When ExtJS is loaded, it delegates to NX.State.getUser().
 * When ExtJS is not loaded, it fetches user state directly from the API.
 *
 * Future phases will fully migrate away from ExtJS dependencies.
 */

const AuthContext = createContext({
  user: null,
  isLoading: true,
  isAuthenticated: false,
  hasUser: () => false,
  refreshUser: () => Promise.resolve()
});

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  const fetchUserFromAPI = async () => {
    try {
      const response = await Axios.get('/service/rest/internal/ui/user');
      return response.data;
    } catch (error) {
      // 401/403 means not authenticated
      if (error.response?.status === 401 || error.response?.status === 403) {
        return null;
      }
      console.error('[AuthContext] Failed to fetch user from API:', error);
      return null;
    }
  };

  const fetchUser = async () => {
    if (isExtJSLoaded() && window.NX?.State?.getUser) {
      // ExtJS is loaded - use its user state
      const extUser = window.NX.State.getUser();
      setUser(extUser);
      return extUser;
    } else {
      // ExtJS not loaded - fetch directly from API
      const apiUser = await fetchUserFromAPI();
      setUser(apiUser);
      return apiUser;
    }
  };

  useEffect(() => {
    // Initial fetch
    fetchUser().finally(() => setIsLoading(false));

    // If ExtJS loads later, sync with its user state
    onExtJSLoad(() => {
      if (window.NX?.State?.getUser) {
        const extUser = window.NX.State.getUser();
        setUser(extUser);
      }
    });

    // Subscribe to ExtJS user changes if it's loaded
    if (isExtJSLoaded() && window.Ext?.getApplication) {
      const stateController = window.Ext.getApplication().getController('State');
      const handleUserChange = () => {
        const extUser = window.NX.State.getUser();
        setUser(extUser);
      };

      stateController.on('userchanged', handleUserChange);
      return () => {
        stateController.un('userchanged', handleUserChange);
      };
    }
  }, []);

  const hasUser = () => {
    if (isExtJSLoaded() && window.NX?.State?.getUser) {
      return Boolean(window.NX.State.getUser());
    }
    return Boolean(user);
  };

  const refreshUser = async () => {
    return fetchUser();
  };

  const value = {
    user,
    isLoading,
    isAuthenticated: hasUser(),
    hasUser,
    refreshUser
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

export default AuthContext;
