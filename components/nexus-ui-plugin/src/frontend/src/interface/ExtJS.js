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
   * Set the global dirty status to prevent accidental navigation
   * @param key - a unique key for the view that is dirty
   * @param isDirty - whether the view is dirty or not
   */
  static setDirtyStatus(key, isDirty) {
    window.dirty = window.dirty || [];

    if (isDirty && window.dirty.indexOf(key) === -1) {
      window.dirty.push(key);
    }
    else {
      window.dirty = window.dirty.filter(it => it !== key);
    }
  }

  /**
   * @return {location: {pathname: string }}
   */
  static useHistory({basePath}) {
    const [path, setPath] = useState(Ext.History.getToken());

    useEffect(() => {
      Ext.History.on('change', setPath);
      return () => Ext.History.un('change', setPath);
    });

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

  static downloadUrl(url) {
    NX.util.DownloadHelper.downloadUrl(url);
  }

  static state() {
    return NX.State;
  }
}
