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

import givenUserLoggedIn from '../../../testUtils/givenUserLoggedIn';
import givenBundleActiveStates from '../../../testUtils/givenBundleActiveStates';
import givenExtJSState from '../../../testUtils/givenExtJSState';
import { runLinkNotVisibleTest, runLinkVisiblityTest, runDirectoryPage404Test } from '../../../testUtils/directoryPageTestUtils';
import { ROUTE_NAMES } from '../../../routerConfig/routeNames/routeNames';
import UIStrings from '../../../constants/UIStrings';
import givenPermissions from '../../../testUtils/givenPermissions';
import { Permissions } from '@sonatype/nexus-ui-plugin';

describe('AdminSecurityDirectoryPage', () => {
  beforeEach(() => {
    givenUserLoggedIn();

    givenBundleActiveStates({
      'nexus-coreui-plugin': true
    });

    givenExtJSState(defaultExtState());
  });

  it('redirects to 404 when user has no permissions for any child routes', async () => {
    givenPermissions({});

    await runDirectoryPage404Test(ROUTE_NAMES.ADMIN.SECURITY.DIRECTORY);
  });

  describe('Privileges Link', () => {
    it('shows given permissions', async () => {
      givenPermissions({ [Permissions.PRIVILEGES.READ]: true });
      await runLinkVisiblityTestForSecurityPage(UIStrings.PRIVILEGES.MENU);
    });

    it('does not show without permissions', async () => {
      givenPermissions({
        [Permissions.PRIVILEGES.READ]: false,
        [Permissions.SETTINGS.READ]: true  // Grant at least one permission so page renders
      });
      await runLinkNotVisibleTestForSecurityPage(UIStrings.PRIVILEGES.MENU);
    });
  });

  describe('Roles Link', () => {
    it('shows given permissions', async () => {
      givenPermissions({
        'nexus:roles:read': true,
        'nexus:privileges:read': true
      });
      await runLinkVisiblityTestForSecurityPage(UIStrings.ROLES.MENU);
    });

    it('does not show without permissions', async () => {
      givenPermissions({
        'nexus:roles:read': true,
        'nexus:privileges:read': false,
        [Permissions.SETTINGS.READ]: true  // Grant at least one permission so page renders
      });
      await runLinkNotVisibleTestForSecurityPage(UIStrings.ROLES.MENU);
    });
  });

  describe('Users Link', () => {
    it('shows given permissions', async () => {
      givenPermissions({
        [Permissions.USERS.READ]: true,
        [Permissions.ROLES.READ]: true
      });
      await runLinkVisiblityTestForSecurityPage(UIStrings.USERS.MENU)
    });

    it('does not show without permissions', async () => {
      givenPermissions({
        [Permissions.USERS.READ]: true,
        [Permissions.ROLES.READ]: false,
        [Permissions.SETTINGS.READ]: true  // Grant at least one permission so page renders
      });
      await runLinkNotVisibleTestForSecurityPage(UIStrings.USERS.MENU);
    });
  });

  describe('Anonymous Settings Link', () => {
    it('shows given permissions', async () => {
      givenPermissions({ [Permissions.SETTINGS.READ]: true });
      await runLinkVisiblityTestForSecurityPage(UIStrings.ANONYMOUS_SETTINGS.MENU)
    });

    it('does not show without permissions', async () => {
      givenPermissions({
        [Permissions.SETTINGS.READ]: false,
        [Permissions.PRIVILEGES.READ]: true  // Grant at least one permission so page renders
      });
      await runLinkNotVisibleTestForSecurityPage(UIStrings.ANONYMOUS_SETTINGS.MENU);
    });
  });

  describe('Crowd Settings Link', () => {
    beforeEach(() => {
      givenBundleActiveStates({
        'nexus-crowd-plugin': true
      });

      givenExtJSState({ ...defaultExtState(), crowd: { licenseValid: true }  });
    })

    it('shows given permissions', async () => {
      givenPermissions({ 'nexus:crowd:read': true });
      await runLinkVisiblityTestForSecurityPage(UIStrings.CROWD_SETTINGS.MENU)
    });

    it('does not show without permissions', async () => {
      givenPermissions({
        'nexus:crowd:read': false,
        [Permissions.SETTINGS.READ]: true  // Grant at least one permission so page renders
      });
      await runLinkNotVisibleTestForSecurityPage(UIStrings.CROWD_SETTINGS.MENU);
    });
  });

  describe('LDAP Servers Link', () => {
    it('shows given permissions', async () => {
      givenPermissions({ [Permissions.LDAP.READ]: true });
      await runLinkVisiblityTestForSecurityPage(UIStrings.LDAP_SERVERS.MENU)
    });

    it('does not show without permissions', async () => {
      givenPermissions({
        [Permissions.LDAP.READ]: false,
        [Permissions.SETTINGS.READ]: true  // Grant at least one permission so page renders
      });
      await runLinkNotVisibleTestForSecurityPage(UIStrings.LDAP_SERVERS.MENU);
    });
  });

  describe('Realms Link', () => {
    it('shows given permissions', async () => {
      givenPermissions({ [Permissions.SETTINGS.READ]: true });
      await runLinkVisiblityTestForSecurityPage(UIStrings.REALMS.MENU)
    });

    it('does not show without permissions', async () => {
      givenPermissions({
        [Permissions.SETTINGS.READ]: false,
        [Permissions.PRIVILEGES.READ]: true  // Grant at least one permission so page renders
      });
      await runLinkNotVisibleTestForSecurityPage(UIStrings.REALMS.MENU);
    });
  });

  describe('SAML Configuration Link', () => {
    beforeEach(() => {
      givenBundleActiveStates({ 'nexus-saml-plugin': true })
      givenExtJSState(defaultExtState(), 'PRO')
    })
    it('shows given permissions', async () => {
      givenPermissions({ 'nexus:*': true });
      await runLinkVisiblityTestForSecurityPage(UIStrings.SAML_CONFIGURATION.MENU)
    });

    it('does not show without permissions', async () => {
      givenPermissions({
        'nexus:*': false,
        [Permissions.SETTINGS.READ]: true  // Grant at least one permission so page renders
      });
      await runLinkNotVisibleTestForSecurityPage(UIStrings.SAML_CONFIGURATION.MENU);
    });
  });

  describe('SSL Certificates Link', () => {
    it('shows given permissions', async () => {
      givenPermissions({ [Permissions.SSL_TRUSTSTORE.READ]: true });
      await runLinkVisiblityTestForSecurityPage(UIStrings.SSL_CERTIFICATES.MENU);
    });

    it('does not show without permissions', async () => {
      givenPermissions({
        [Permissions.SSL_TRUSTSTORE.READ]: false,
        [Permissions.SETTINGS.READ]: true  // Grant at least one permission so page renders
      });
      await runLinkNotVisibleTestForSecurityPage(UIStrings.SSL_CERTIFICATES.MENU);
    });
  });

  describe('User Token Configuration Link', () => {
    beforeEach(() => {
      givenExtJSState(defaultExtState(),  'PRO');
    });

    it('shows given permissions', async () => {
      givenPermissions({ [Permissions.USER_TOKENS_SETTINGS.READ]: true});
      await runLinkVisiblityTestForSecurityPage(UIStrings.USER_TOKEN_CONFIGURATION.MENU)
    });

    it('does not show without permissions', async () => {
      givenPermissions({
        [Permissions.USER_TOKENS_SETTINGS.READ]: false,
        [Permissions.SETTINGS.READ]: true  // Grant at least one permission so page renders
      });
      await runLinkNotVisibleTestForSecurityPage(UIStrings.USER_TOKEN_CONFIGURATION.MENU);
    });
  });

  describe('Service Account Tokens Link', () => {
    it('shows when serviceAccountEnabled flag is true', async () => {
      givenPermissions({ [Permissions.SERVICE_ACCOUNTS.READ]: true });
      givenExtJSState({ ...defaultExtState(), serviceAccountEnabled: true }, 'PRO');

      await runLinkVisiblityTestForSecurityPage(UIStrings.SERVICE_ACCOUNT_TOKENS.MENU);
    });

    it('does not show when serviceAccountEnabled flag is false', async () => {
      givenPermissions({ [Permissions.SERVICE_ACCOUNTS.READ]: true, [Permissions.SETTINGS.READ]: true });
      givenExtJSState({ ...defaultExtState(), serviceAccountEnabled: false }, 'PRO');

      await runLinkNotVisibleTestForSecurityPage(UIStrings.SERVICE_ACCOUNT_TOKENS.MENU);
    });

    it('does not show without service accounts read permission', async () => {
      givenPermissions({ [Permissions.SERVICE_ACCOUNTS.READ]: false, [Permissions.SETTINGS.READ]: true });
      givenExtJSState({ ...defaultExtState(), serviceAccountEnabled: true }, 'PRO');

      await runLinkNotVisibleTestForSecurityPage(UIStrings.SERVICE_ACCOUNT_TOKENS.MENU);
    });
  });

  async function runLinkVisiblityTestForSecurityPage(menu) {
    await runLinkVisiblityTest(ROUTE_NAMES.ADMIN.SECURITY.DIRECTORY, 'Security', menu);
  }

  async function runLinkNotVisibleTestForSecurityPage(menu) {
    await runLinkNotVisibleTest(ROUTE_NAMES.ADMIN.SECURITY.DIRECTORY, 'Security', menu);
  }

  function defaultExtState() {
    return { 
        usertoken: { licenseValid: true }
    };
  }
});
