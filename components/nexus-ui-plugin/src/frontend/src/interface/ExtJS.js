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
/*global Ext, NX*/

import {useEffect, useState} from 'react';

/**
 * @since 3.22
 */
export default class {
  /**
   * Open a success message notification
   * @param text
   */
  static showSuccessMessage(text) {
    NX.Messages.success(text)
  }

  /**
   * Open an error message notification
   * @param text
   */
  static showErrorMessage(text) {
    NX.Messages.error(text);
  }

  /**
   *@returns a complete url for the current nexus instance
   */
  static urlOf(path) {
    return NX.util.Url.urlOf(path);
  }

  /**
   * @returns an absolute path when given a relative path.
   */
  static absolutePath(path) {
    return NX.util.Url.absolutePath(path); 
  }

  /**
   *@returns a complete url for the PRO-LICENSE.html
   */
  static proLicenseUrl() {
    return this.urlOf('/PRO-LICENSE.html');
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
    return new Promise((resolve, reject) => {
      NX.direct.rapture_Security.authenticationToken(b64u, b64p, resolve);
    });
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
    return NX.State;
  }

  static formatDate(date, format) {
    return Ext.Date.format(date, format);
  }

  /**
   * @returns {boolean} true if the edition is PRO
   */
  static isProEdition() {
    return this.state().getEdition() === 'PRO';
  }

  /**
   * @param permission {string}
   * @returns {boolean} true if the user has the requested permission
   */
  static checkPermission(permission) {
    return NX.Permissions.check(permission)
  }

  /**
   * @returns {{id: string, authenticated: boolean, administrator: boolean, authenticatedRealms: string[]} | undefined}
   */
  static useUser() {
    return this.useState(() => NX.State.getUser());
  }

  /**
   * @returns {{version: string, edition: string}}
   */
  static useStatus() {
    return this.useState(() => NX.State.getValue('status'));
  }

  /**
   * @returns {{daysToExpiry: number}}
   */
  static useLicense() {
    return this.useState(() => NX.State.getValue('license'));
  }

  /**
   * A hook that automatically re-evaluates whenever any state is changed
   * @param getValue - A function to get the value from the state subsystem
   * @returns {unknown}
   */
  static useState(getValue) {
    const [value, setValue] = useState(getValue());

    useEffect(() => {
      function handleChange() {
        const newValue = getValue();
        if (value !== newValue) {
          setValue(newValue);
        }
      }

      const state = Ext.getApplication().getStore('State');
      state.on('datachanged', handleChange);
      return () => state.un('datachanged', handleChange);
    }, [value]);

    return value;
  }
}
