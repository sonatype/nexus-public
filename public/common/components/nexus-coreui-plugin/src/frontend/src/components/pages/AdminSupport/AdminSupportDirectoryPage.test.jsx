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


import { runLinkNotVisibleTest, runLinkVisiblityTest, runDirectoryPage404Test } from '../../../testUtils/directoryPageTestUtils';
import { ROUTE_NAMES } from '../../../routerConfig/routeNames/routeNames';
import UIStrings from '../../../constants/UIStrings';
import givenUserLoggedIn from '../../../testUtils/givenUserLoggedIn';
import givenBundleActiveStates from '../../../testUtils/givenBundleActiveStates';
import givenExtJSState from '../../../testUtils/givenExtJSState';
import givenPermissions from '../../../testUtils/givenPermissions';
import { Permissions } from '@sonatype/nexus-ui-plugin';

describe('AdminSupportDirectoryPage', () => {
  beforeEach(() => {
    givenUserLoggedIn();

    givenBundleActiveStates({
      'nexus-coreui-plugin': true
    });

    givenExtJSState(defaultExtState());
  });

  it('redirects to 404 when user has no permissions for any child routes', async () => {
    givenPermissions({});

    await runDirectoryPage404Test(ROUTE_NAMES.ADMIN.SUPPORT.DIRECTORY);
  });

  describe('Loging Link', () => {
    it('shows given permissions', async () => {
      givenPermissions({ [Permissions.LOGGING.READ]: true });
      await runLinkVisiblityTestForSupportPage(UIStrings.LOGGING.MENU);
    });

    it('does not show without permissions', async () => {
      givenPermissions({
        [Permissions.LOGGING.READ]: false,
        [Permissions.ATLAS.READ]: true  // Grant at least one permission so page renders
      });
      await runLinkNotVisibleTestForSupportPage(UIStrings.LOGGING.MENU);
    });
  });


  describe('Logs Link', () => {
    it('shows given permissions', async () => {
      givenPermissions({ [Permissions.LOGGING.READ]: true });
      await runLinkVisiblityTestForSupportPage(UIStrings.LOGS.MENU);
    });

    it('does not show without permissions', async () => {
      givenPermissions({
        [Permissions.LOGGING.READ]: false,
        [Permissions.ATLAS.READ]: true  // Grant at least one permission so page renders
      });
      await runLinkNotVisibleTestForSupportPage(UIStrings.LOGS.MENU);
    });
  });

  describe('Metric Health Link', () => {
    it('shows given permissions', async () => {
      givenPermissions({ [Permissions.METRICS.READ]: true });
      await runLinkVisiblityTestForSupportPage(UIStrings.METRIC_HEALTH.MENU);
    });

    it('does not show without permissions', async () => {
      givenPermissions({
        [Permissions.METRICS.READ]: false,
        [Permissions.ATLAS.READ]: true  // Grant at least one permission so page renders
      });
      await runLinkNotVisibleTestForSupportPage(UIStrings.METRIC_HEALTH.MENU);
    });
  });

  describe('Support Request Link', () => {
    beforeEach(() => {
      givenExtJSState(defaultExtState(), 'PRO')
    });

    it('shows given permissions', async () => {
      givenPermissions({ [Permissions.ATLAS.CREATE]: true });
      await runLinkVisiblityTestForSupportPage(UIStrings.SUPPORT_REQUEST.MENU);
    });

    it('does not show without permissions', async () => {
      givenPermissions({
        [Permissions.ATLAS.CREATE]: false,
        [Permissions.ATLAS.READ]: true  // Grant at least one permission so page renders
      });
      await runLinkNotVisibleTestForSupportPage(UIStrings.SUPPORT_REQUEST.MENU);
    });
  });

  describe('Support Zip Link', () => {
    it('shows given permissions', async () => {
      givenPermissions({ [Permissions.ATLAS.READ]:  true })
      await runLinkVisiblityTestForSupportPage(UIStrings.SUPPORT_ZIP.MENU);
    });

    it('does not show without permissions', async () => {
      givenPermissions({
        [Permissions.ATLAS.READ]:  false,
        [Permissions.LOGGING.READ]: true  // Grant at least one permission so page renders
      })
      await runLinkNotVisibleTestForSupportPage(UIStrings.SUPPORT_ZIP.MENU);
    });
  });

  describe('System Information Link', () => {
    it('shows given permissions', async () => {
      givenPermissions({ [Permissions.ATLAS.READ]:  true });
      await runLinkVisiblityTestForSupportPage(UIStrings.SYSTEM_INFORMATION.MENU);
    });

    it('does not show without permissions', async () => {
      givenPermissions({
        [Permissions.ATLAS.READ]:  false,
        [Permissions.LOGGING.READ]: true  // Grant at least one permission so page renders
      });
      await runLinkNotVisibleTestForSupportPage(UIStrings.SYSTEM_INFORMATION.MENU);
    });
  });

  async function runLinkVisiblityTestForSupportPage(menu) {
    await runLinkVisiblityTest(ROUTE_NAMES.ADMIN.SUPPORT.DIRECTORY, 'Support', menu);
  }

  async function runLinkNotVisibleTestForSupportPage(menu) {
    await runLinkNotVisibleTest(ROUTE_NAMES.ADMIN.SUPPORT.DIRECTORY, 'Support', menu);
  }

  function defaultExtState() {
    return { usertoken: { licenseValid: true } };
  }
});
