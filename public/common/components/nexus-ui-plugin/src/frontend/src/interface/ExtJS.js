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
import {useEffect, useState, useRef} from 'react';

/**
 * Phase 1: React Shell - Bridge between React and ExtJS
 *
 * This class provides a facade for React components to access ExtJS functionality.
 * All methods now gracefully handle ExtJS not being loaded, providing fallbacks
 * where appropriate.
 */
export default class ExtJS {
  /**
   * Open a success message notification
   * @param text
   */
  static showSuccessMessage(text) {
    if (window.NX?.Messages?.success) {
      window.NX.Messages.success(text);
    } else {
      // Fallback: Console log when ExtJS not loaded
      console.info('[Success]', text);
    }
  }

  /**
   * Open an error message notification
   * @param text
   */
  static showErrorMessage(text) {
    if (window.NX?.Messages?.error) {
      window.NX.Messages.error(text);
    } else {
      // Fallback: Console error when ExtJS not loaded
      console.error('[Error]', text);
    }
  }

  /**
   *@returns a complete url for the current nexus instance
   */
  static urlOf(path) {
    if (window.NX?.util?.Url?.urlOf) {
      return window.NX.util.Url.urlOf(path);
    }
    // Fallback: Basic URL construction
    return window.location.origin + (path.startsWith('/') ? path : '/' + path);
  }

  /**
   * @returns an absolute path when given a relative path.
   */
  static absolutePath(path) {
    if (window.NX?.util?.Url?.absolutePath) {
      return window.NX.util.Url.absolutePath(path);
    }
    // Fallback: Ensure path starts with /
    return path.startsWith('/') ? path : '/' + path;
  }

  /**
   *@returns a complete url for the PRO-LICENSE.html
   */
  static proLicenseUrl() {
    return ExtJS.urlOf('/PRO-LICENSE.html');
  }

  /**
   * Set the global dirty status to prevent accidental navigation
   * @param key - a unique key for the view that is dirty
   * @param isDirty - whether the view is dirty or not
   */
  static setDirtyStatus(key, isDirty) {
    window.dirty = window.dirty || [];

    if (isDirty && window.dirty.indexOf(key) === -1) {
      window.dirty.push(key);
    }
    else if (!isDirty) {
      window.dirty = window.dirty.filter(it => it !== key);
    }
  }

  /**
   * @return {location: {pathname: string }}
   */
  static useHistory({basePath}) {
    const [path, setPath] = useState(Ext.History.getToken());

    useEffect(() => {
      // When the unmount is occurring due to a route change, Ext seems to already commit
      // to firing the change handler even though the useEffect cleanup function fires before it does.
      // This causes React memory leak warnings if the state mutator gets called at that point. So we need this
      // extra variable to check whether the unmount has in fact occurred
      let unmounted = false;

      function _setPath(p) {
        if (!unmounted) {
          setPath(p);
        }
      }

      Ext.History.on('change', _setPath);
      return () => {
        unmounted = true;
        Ext.History.un('change', _setPath);
      };
    }, []);

    return {
      location: {
        pathname: path.replace(basePath, '')
      }
    };
  }

  static requestConfirmation({title, message, yesButtonText = 'Yes', noButtonText = 'No'}) {
    const options = {
      buttonText: {
        yes: yesButtonText,
        no: noButtonText
      }
    };

    return new Promise((resolve, reject) => NX.Dialogs.askConfirmation(title, message, resolve, {
      ...options,
      onNoFn: reject
    }));
  }

  /**
   * Create a Promise that will fetch an authentication token using the
   * username and password supplied
   * @param username
   * @param password
   * @returns {Promise}
   */
  static fetchAuthenticationToken(username, password) {
    const b64u = NX.util.Base64.encode(username);
    const b64p = NX.util.Base64.encode(password);
    return new Promise((resolve) => {
      NX.direct.rapture_Security.authenticationToken(b64u, b64p, resolve);
    });
  }

  /**
   * Request a session for supplied credentials, reusing the ExtJS security flow
   * @param {string} username
   * @param {string} password
   * @returns {Promise}
   */
  static requestSession(username, password) {
    const b64u = NX.util.Base64.encode(username);
    const b64p = NX.util.Base64.encode(password);

    return new Promise((resolve, reject) => {
      NX.Security.requestSession(b64u, b64p).then(
        (response) => {
          const normalizedResponse = ExtJS.normalizeAjaxResponse(response);

          // Set user state on successful authentication
          if (normalizedResponse.status >= 200 && normalizedResponse.status < 300) {
            NX.State.setUser({id: username});
          }

          resolve({response: normalizedResponse});
        },
        (error) => reject({response: ExtJS.normalizeAjaxResponse(error)})
      );
    });
  }

  static normalizeAjaxResponse(response) {
    if (response?.response) {
      response = response.response;
    }

    if (!response) {
      return {status: undefined, data: undefined};
    }

    let data;

    if (response.responseJSON !== undefined) {
      data = response.responseJSON;
    }
    else if (response.responseText !== undefined) {
      const text = response.responseText;
      if (text) {
        try {
          data = JSON.parse(text);
        }
        catch {
          data = text;
        }
      }
    }
    else if (response.data !== undefined) {
      data = response.data;
    }

    const normalized = {
      status: response.status
    };

    if (data !== undefined) {
      normalized.data = data;
    }

    return normalized;
  }

  /**
   * Prompt the user to re-authenticate to fetch an authentication token
   * @param message prompt shown to user
   * @returns {Promise}
   */
  static requestAuthenticationToken(message) {
    return new Promise((resolve, reject) => {
      NX.Security.doWithAuthenticationToken(
          message,
          {
            success: function(authToken) {
              return resolve(authToken);
            },
            failure: function() {
              return reject();
            }
          }
      );
    });
  }

  /**
   * @deprecated - Use a link with the download attribute instead
   */
  static downloadUrl(url) {
    NX.util.DownloadHelper.downloadUrl(url);
  }

  static state() {
    // Only return real state if ExtJS application is fully initialized
    try {
      if (window.NX?.getApplication &&
          typeof window.NX.getApplication === 'function' &&
          window.NX.getApplication() &&
          window.NX?.State) {
        return window.NX.State;
      }
    } catch {
      // ExtJS not ready yet
    }
    // Fallback: Use REST bootstrap data if available, otherwise return empty defaults
    const rest = typeof window !== 'undefined' ? window.__nxRestBootstrap : null;
    return {
      getValue: (key, defaultValue) => {
        if (rest?.stateValues) {
          const val = rest.stateValues[key];
          return val !== undefined ? val : (defaultValue !== undefined ? defaultValue : null);
        }
        return defaultValue !== undefined ? defaultValue : null;
      },
      setValue: () => {},
      getUser: () => rest?.user || null,
      getEdition: () => rest?.edition || 'OSS',
      getVersion: () => rest?.version || null
    };
  }

  static formatDate(date, format) {
    if (window.Ext?.Date?.format) {
      return window.Ext.Date.format(date, format);
    }
    // Fallback: Use JavaScript Date toLocaleString
    return date.toLocaleString();
  }

  /**
   * @returns {boolean} true if the edition is PRO
   */
  static isProEdition() {
    return ExtJS.state().getEdition() === 'PRO';
  }

  /**
   * @param permission {string}
   * @returns {boolean} true if the user has the requested permission
   */
  static checkPermission(permission) {
    if (window.NX?.Permissions?.check) {
      return window.NX.Permissions.check(permission);
    }
    // Fallback: Use REST bootstrap permissions if available
    if (typeof window !== 'undefined' && window.__nxRestBootstrap?.checkPermission) {
      return window.__nxRestBootstrap.checkPermission(permission);
    }
    return false;
  }

  /**
   * @returns {{id: string, authenticated: boolean, administrator: boolean, authenticatedRealms: string[]} | undefined}
   */
  static useUser() {
    return ExtJS.useState(() => {
      try {
        if (window.NX?.State?.getUser) return window.NX.State.getUser();
      } catch {
        // ExtJS not ready
      }
      // Fallback: REST bootstrap data
      return window.__nxRestBootstrap?.user || undefined;
    });
  }

  /**
   * @returns {{version: string, edition: string}}
   */
  static useStatus() {
    return ExtJS.useState(() => NX.State.getValue('status'));
  }

  /**
   * @returns {{daysToExpiry: number}}
   */
  static useLicense() {
    return ExtJS.useState(() => NX.State.getValue('license'));
  }

  // don't use directly, use useIsVisible, where possible
  static useVisiblityWithChanges(isVisibleMethod) {
    // Initial read is guarded so React can render even if ExtJS is not ready yet.
    const [isVisible, setIsVisible] = useState(() => {
      try {
        return isVisibleMethod();
      }
      catch {
        return false;
      }
    });

    // Track if we've connected to ExtJS
    const [extJsReady, setExtJsReady] = useState(() => !!window.Ext?.getApplication?.());

    // Keep a ref to isVisibleMethod so the event handler always uses the latest function
    // without needing to re-subscribe to events on every render
    const isVisibleMethodRef = useRef(isVisibleMethod);
    isVisibleMethodRef.current = isVisibleMethod;

    useEffect(() => {
      // Poll for ExtJS readiness if not ready yet
      if (!extJsReady) {
        const checkInterval = setInterval(() => {
          const app = window.Ext?.getApplication?.();
          const hasState = !!window.NX?.State;
          const hasPermissions = !!window.NX?.Permissions;
          const hasSecurity = !!window.NX?.Security;
          if (app && hasState && hasPermissions && hasSecurity) {
            setExtJsReady(true);
            try {
              const newValue = isVisibleMethodRef.current();
              setIsVisible(newValue);
            } catch {
              // Silently handle errors
            }
            clearInterval(checkInterval);
          }
        }, 100);

        return () => clearInterval(checkInterval);
      }
    }, [extJsReady]);

    useEffect(() => {
      // If ExtJS is not initialized, do not wire listeners. This avoids crashes during
      // early React render (e.g., login route) when ExtJS loads lazily.
      const app = window.Ext?.getApplication?.();
      if (!app) {
        return;
      }

      function handleChange() {
        // Use ref to always get the latest isVisibleMethod
        const newValue = isVisibleMethodRef.current();
        setIsVisible(prevVisible => {
          if (prevVisible !== newValue) {
            return newValue;
          }
          return prevVisible;
        });
      }

      const permissionsController = app.getController?.('Permissions');
      const stateController = app.getController?.('State');

      permissionsController?.on('changed', handleChange);
      stateController?.on('changed', handleChange);
      stateController?.on('userchanged', handleChange);

      // Also call handleChange immediately to ensure we have the latest value
      handleChange();

      return () => {
        // cleanup code
        permissionsController?.un('changed', handleChange);
        stateController?.un('changed', handleChange);
        stateController?.un('userchanged', handleChange);
      }
    }, [extJsReady]);

    return isVisible;
  }

  /**
   * A hook that automatically re-evaluates whenever any state is changed
   * @param getValue - A function to get the value from the state subsystem
   * @returns {unknown}
   */
  static useState(getValue) {
    const [value, setValue] = useState(() => {
      try {
        return getValue();
      }
      catch {
        return undefined;
      }
    });

    useEffect(() => {
      // If ExtJS not loaded, just return the current value
      try {
        if (!window.Ext?.getApplication ||
            typeof window.Ext.getApplication !== 'function' ||
            !window.Ext.getApplication()) {
          return;
        }
      } catch {
        return;
      }

      function handleChange() {
        try {
          const newValue = getValue();
          if (value !== newValue) {
            setValue(newValue);
          }
        } catch {
          // Ignore until ExtJS is ready
        }
      }

      try {
        const app = window.Ext?.getApplication?.();
        if (app && typeof app.getStore === 'function') {
          const state = app.getStore('State');
          if (state) {
            state.on('datachanged', handleChange);
            return () => state.un('datachanged', handleChange);
          }
        }
      } catch {
        // ExtJS not ready, skip setting up listener
      }
    }, [value]);

    return value;
  }

  /**
   * A hook to add search criteria to the search subsystem
   * @param {*} criterias
   */
  static useCriteria(criterias) {
    if (!window.Ext?.getApplication) {
      console.warn('[ExtJS.useCriteria] ExtJS not loaded');
      return;
    }

    const app = window.Ext.getApplication();
    if (!app) {
      console.warn('[ExtJS.useCriteria] ExtJS application not initialized');
      return;
    }

    const state = app.getStore('SearchCriteria');
    if (!state) {
      console.warn('[ExtJS.useCriteria] SearchCriteria store not found');
      return;
    }

    const models = state.add(criterias);

    useEffect(() => {
      return () => {
        if (state && models) {
          state.remove(models);
        }
      }
    }, []);
  }

  /**
   * A hook to use a search filter model
   * @param {*} filter
   * @returns
   */
  static useSearchFilterModel(filter) {
    if (!window.Ext?.getApplication) {
      console.warn('[ExtJS.useSearchFilterModel] ExtJS not loaded');
      return null;
    }

    const app = window.Ext.getApplication();
    if (!app) {
      console.warn('[ExtJS.useSearchFilterModel] ExtJS application not initialized');
      return null;
    }

    try {
      return app.getModel("NX.coreui.model.SearchFilter").create(filter);
    } catch (error) {
      console.error('[ExtJS.useSearchFilterModel] Failed to create SearchFilter model:', error);
      return null;
    }
  }

  /**
   * A hook that automatically re-evaluates whenever any state is changed
   * @param getValue - A function to get the value from the state subsystem
   * @returns {unknown}
   */
  static usePermission(getValue, dependencies) {
    const [value, setValue] = useState(getValue());

    useEffect(() => {
      function handleChange() {
        const newValue = getValue();
        if (value !== newValue) {
          setValue(newValue);
        }
      }

      const app = window.Ext?.getApplication?.();
      if (!app) {
        console.warn('[ExtJS.usePermission] ExtJS application not initialized; skipping subscriptions');
        return undefined;
      }

      const permissionsController = app.getController?.('Permissions');
      const stateController = app.getController?.('State');

      if (!permissionsController || !stateController) {
        console.warn('[ExtJS.usePermission] Controllers not available; skipping subscriptions');
        return undefined;
      }

      permissionsController.on?.('changed', handleChange);
      stateController.on?.('userchanged', handleChange);

      return () => {
        permissionsController.un?.('changed', handleChange);
        stateController.un?.('userchanged', handleChange);
      };
    }, [value, ...(dependencies ?? [])]);

    return value;
  }

  static hasUser() {
    // Wait for ExtJS application to be fully initialized
    try {
      if (window.NX?.getApplication &&
          typeof window.NX.getApplication === 'function' &&
          window.NX.getApplication() &&
          window.NX?.Security?.hasUser) {
        const result = window.NX.Security.hasUser();
        console.debug('[ExtJS.hasUser] Via NX.Security.hasUser():', result);
        return result;
      }
    } catch {
      // ExtJS not ready yet
    }
    // Fallback 1: Check if user exists in NX.State (after State controller initializes)
    if (window.NX?.State?.getUser) {
      const user = window.NX.State.getUser();
      if (user) {
        console.debug('[ExtJS.hasUser] Via NX.State.getUser():', true);
        return true;
      }
    }
    // Fallback 2: Check initial app state from server (before State controller initializes)
    // This works when app.js has loaded but ExtJS app hasn't fully initialized
    if (window.NX?.app?.state?.user?.value) {
      console.debug('[ExtJS.hasUser] Via NX.app.state.user.value:', true);
      return true;
    }
    // Fallback 3: Check session cookie exists (critical for debug mode)
    // In debug mode, React loads before app.js, so NX.app.state isn't set yet.
    // The NXSESSIONID cookie is the most reliable indicator of an authenticated session.
    const hasCookie = document.cookie.includes('NXSESSIONID');
    if (hasCookie) {
      console.debug('[ExtJS.hasUser] Via NXSESSIONID cookie:', true);
      return true;
    }
    console.debug('[ExtJS.hasUser] No user found. Cookie:', document.cookie.substring(0, 100));
    return false;
  }

  static signOut() {
    if (window.NX?.Security?.signOut) {
      window.NX.Security.signOut();
    } else {
      // Fallback: Navigate to logout endpoint
      window.location.href = '/service/rapture/session';
    }
  }

  static showAbout() {
    Ext.widget('nx-aboutwindow')
  }

  static refresh() {
    Ext.getApplication().getController('Refresh').refresh();
  }

  static search(query) {
    Ext.getApplication().getController('Search').onQuickSearch(null, query);
  }

  static isExtJsRendered() {
    return document.getElementsByClassName('nxrm-ext-js-wrapper').length > 0;
  }

  /**
   * This function will wait for the ExtJS application to be fully loaded before executing the callback
   * @param callback
   */
  static waitForExtJs(callback) {
    const interval = setInterval(() => {
      try {
        if (Ext.getApplication() && NX.Permissions.permissions !== undefined) {
          clearInterval(interval);
          callback();
        }
      } catch {
        // Ext/NX not defined yet, keep polling
      }
    }, 50);
  }

  /**
   * Wait for ExtJS to be fully initialized.
   * Returns a Promise that resolves when ExtJS is ready.
   * @returns {Promise<void>}
   */
  static waitForExtJsReady() {
    return new Promise((resolve, reject) => {
      // Check if already loaded
      try {
        if (window.Ext?.getApplication &&
            typeof window.NX?.getApplication === 'function' &&
            window.NX.getApplication() &&
            window.NX.Permissions?.permissions !== undefined) {
          resolve();
          return;
        }
      } catch {
        // Not ready yet, continue polling
      }

      // Poll for ExtJS to be ready
      const checkInterval = setInterval(() => {
        try {
          if (window.Ext?.getApplication &&
              typeof window.NX?.getApplication === 'function' &&
              window.NX.getApplication() &&
              window.NX.Permissions?.permissions !== undefined) {
            clearInterval(checkInterval);
            resolve();
          }
        } catch {
          // Keep polling
        }
      }, 100);

      // Timeout after 30 seconds
      setTimeout(() => {
        clearInterval(checkInterval);
        reject(new Error('ExtJS failed to initialize within 30 seconds'));
      }, 30000);
    });
  }

  static calculateTimeout() {
    const uiSettings = ExtJS.state().getValue('uiSettings', {});
    const statusInterval = ExtJS.hasUser()
      ? (uiSettings.statusIntervalAuthenticated || 5)
      : (uiSettings.statusIntervalAnonymous || 60);

    return Math.max(statusInterval * 2 * 1000, 2000);
  }

  /**
   * Wait for the next permission change event from ExtJS.
   * Useful after login to ensure permissions are updated before navigation.
   * 
   * The timeout is calculated based on the UI settings status interval to account for network latency
   * and server load. Uses 2x the status interval as a buffer, with a minimum of 2 seconds.
   * 
   * @returns {Promise<void>} Resolves when permissions change, rejects after timeout
   */
  static waitForNextPermissionChange() {
    return new Promise((resolve, reject) => {
      const handleChange = () => {
        console.debug('received permission changes');
        clearTimeout(timeout);
        resolve();
      };

      console.debug('setting up event handler to wait for permission changes');
      const permissionsController = Ext.getApplication().getController('Permissions');
      permissionsController.on("changed", handleChange);

      const timeoutMs = ExtJS.calculateTimeout();

      const timeout = setTimeout(() => {
        console.debug('removing event handler, permission changes have timed out');
        permissionsController.un('changed', handleChange);
        reject(new Error('timed out waiting for permissions to update'));
      }, timeoutMs);
    });
  }
}
