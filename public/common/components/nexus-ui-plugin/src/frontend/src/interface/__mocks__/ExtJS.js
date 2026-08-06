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

/**
 * Shared mock for ExtJS interface used across tests in nexus-ui-plugin
 * This mock provides default implementations for commonly used ExtJS methods
 * Individual tests can override specific methods as needed using jest.spyOn() or direct assignment
 */
export default class ExtJS {
  static showSuccessMessage = jest.fn();
  static showErrorMessage = jest.fn();
  static urlOf = jest.fn((path) => `https://testurl${path}`);
  static absolutePath = jest.fn((path) => `/absolute${path}`);
  static proLicenseUrl = jest.fn(() => 'https://testurl/PRO-LICENSE.html');

  static setDirtyStatus = jest.fn((key, isDirty) => {
    window.dirty = window.dirty || [];
    if (isDirty && window.dirty.indexOf(key) === -1) {
      window.dirty.push(key);
    } else if (!isDirty) {
      window.dirty = window.dirty.filter(it => it !== key);
    }
  });

  static useHistory = jest.fn(() => ({
    location: { pathname: '/test-path' }
  }));

  static requestConfirmation = jest.fn(() => Promise.resolve());
  static fetchAuthenticationToken = jest.fn(() => Promise.resolve('mock-token'));
  static requestSession = jest.fn(() => Promise.resolve({status: 200}));
  static normalizeAjaxResponse = jest.fn((response) => response);
  static requestAuthenticationToken = jest.fn(() => Promise.resolve('mock-auth-token'));
  static downloadUrl = jest.fn();

  static state = jest.fn(() => ExtJS._stateInstance);

  // Consistent state instance for when() calls - WITH USERTOKEN CAPABILITY
  static _stateInstance = {
    getValue: jest.fn((key) => {
      if (key === 'capabilityActiveTypes') return ['usertoken'];
      if (key === 'anonymousUsername') return 'anonymous';
      return undefined;
    }),
    getEdition: jest.fn(() => 'OSS'),
    getUser: jest.fn(() => ({ id: 'test-user', authenticated: true }))
  };

  static formatDate = jest.fn((_date, _format) => '2024-01-01');
  static isProEdition = jest.fn(() => true);
  static checkPermission = jest.fn(() => true);

  static useUser = jest.fn(() => ({
    id: 'test-user',
    authenticated: true,
    administrator: false,
    authenticatedRealms: []
  }));

  static useStatus = jest.fn(() => ({
    version: '3.0.0',
    edition: 'OSS'
  }));

  static useLicense = jest.fn(() => ({
    daysToExpiry: 30
  }));

  static useVisiblityWithChanges = jest.fn(() => true);
  static useState = jest.fn((getValue) => getValue());
  static useCriteria = jest.fn();
  static useSearchFilterModel = jest.fn(() => ({}));
  static usePermission = jest.fn((getValue) => getValue());

  static hasUser = jest.fn(() => true);
  static signOut = jest.fn();
  static showAbout = jest.fn();
  static refresh = jest.fn();
  static search = jest.fn();
  static isExtJsRendered = jest.fn(() => true);
  static waitForExtJs = jest.fn((callback) => callback());
  static waitForPermissions = jest.fn().mockResolvedValue(undefined);
}
