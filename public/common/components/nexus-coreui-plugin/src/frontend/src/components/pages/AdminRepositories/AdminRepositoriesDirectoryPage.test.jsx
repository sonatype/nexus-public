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

import { ROUTE_NAMES } from '../../../routerConfig/routeNames/routeNames';
import UIStrings from '../../../constants/UIStrings';
import { Permissions } from '@sonatype/nexus-ui-plugin';
import givenExtJSState from '../../../testUtils/givenExtJSState';
import { runLinkNotVisibleTest, runLinkVisiblityTest, runDirectoryPage404Test } from '../../../testUtils/directoryPageTestUtils';
import givenBundleActiveStates from '../../../testUtils/givenBundleActiveStates';
import givenPermissions from '../../../testUtils/givenPermissions';
import givenUserLoggedIn from '../../../testUtils/givenUserLoggedIn';

describe('AdminRepositoriesDirectoryPage', () => {
  beforeEach(() => {
    givenUserLoggedIn();

    givenBundleActiveStates({
      'nexus-coreui-plugin': true,
      'nexus-pro-datastore-plugin': true
    });

    givenExtJSState({
      usertoken: { licenseValid: true }
    });
  });

  it('redirects to 404 when user has no permissions for any child routes', async () => {
    givenPermissions({});

    await runDirectoryPage404Test(ROUTE_NAMES.ADMIN.REPOSITORY.DIRECTORY);
  });

  describe('Repositories Link', () => {
    it('shows given permissions', async () => {
      givenPermissions({ 'nexus:repository-admin:*:*:read': true });

      await runLinkVisiblityTestForRepositoriesPage(UIStrings.REPOSITORIES.MENU);
    });

    it('does not show without permissions', async () => {
      givenPermissions({
        'nexus:repository-admin:*:*:read': false,
        [Permissions.BLOB_STORES.READ]: true  // Grant at least one permission so page renders
      });

      await runLinkNotVisibleTestForRepositoriesPage( UIStrings.REPOSITORIES.MENU);
    });
  });

  describe('Blob Stores Link', () => {
    it('shows given permissions', async () => {
      givenPermissions({[Permissions.BLOB_STORES.READ]: true });

      await runLinkVisiblityTestForRepositoriesPage(UIStrings.BLOB_STORES.MENU);
    });

    it('does not show without permissions', async () => {
      givenPermissions({
        [Permissions.BLOB_STORES.READ]: false,
        'nexus:repository-admin:*:*:read': true  // Grant at least one permission so page renders
      });

      await runLinkNotVisibleTestForRepositoriesPage(UIStrings.BLOB_STORES.MENU);
    });
  });

  describe('Datastore Link', () => {
    it('shows given permissions', async () => {
      givenPermissions({ 'nexus:*': true});
      await runLinkVisiblityTestForRepositoriesPage(UIStrings.DATASTORE_CONFIGURATION.MENU);
    });

    it('does not show without permissions', async () => {
      givenPermissions({
        'nexus:*': false,
        [Permissions.BLOB_STORES.READ]: true  // Grant at least one permission so page renders
      });
      await runLinkNotVisibleTestForRepositoriesPage(UIStrings.DATASTORE_CONFIGURATION.MENU);
    });
  });

  describe('Proprietary Repositories', () => {
    it('shows given permissions', async () => {
      givenPermissions({[Permissions.SETTINGS.READ]: true});

      await runLinkVisiblityTestForRepositoriesPage(UIStrings.PROPRIETARY_REPOSITORIES.MENU);
    });

    it('does not show without permissions', async () => {
      givenPermissions({
        [Permissions.SETTINGS.READ]: false,
        [Permissions.BLOB_STORES.READ]: true  // Grant at least one permission so page renders
      });

      await runLinkNotVisibleTestForRepositoriesPage(UIStrings.PROPRIETARY_REPOSITORIES.MENU);
    });
  });

  describe('Content Selectors', () => {
    it('shows given permissions', async () => {
      givenPermissions({ [Permissions.SELECTORS.READ]: true });

      await runLinkVisiblityTestForRepositoriesPage(UIStrings.CONTENT_SELECTORS.MENU)
    });

    it('does not show without permissions', async () => {
      givenPermissions({
        [Permissions.SELECTORS.READ]: false,
        [Permissions.BLOB_STORES.READ]: true  // Grant at least one permission so page renders
      });

      await runLinkNotVisibleTestForRepositoriesPage(UIStrings.CONTENT_SELECTORS.MENU);
    });
  });

  describe('Cleanup Policies', () => {
    it('shows given permissions', async () => {
      givenPermissions({ [Permissions.ADMIN]: true });

      await runLinkVisiblityTestForRepositoriesPage(UIStrings.CLEANUP_POLICIES.MENU);
    });

    it('does not show without permissions', async () => {
      givenPermissions({
        [Permissions.ADMIN]: false,
        [Permissions.BLOB_STORES.READ]: true  // Grant at least one permission so page renders
      });

      await runLinkNotVisibleTestForRepositoriesPage(UIStrings.CLEANUP_POLICIES.MENU);
    });
  });

  describe('Routing Rules', () => {
    it('shows given permissions', async () => {
      givenPermissions({ [Permissions.ADMIN]: true });

      await runLinkVisiblityTestForRepositoriesPage(UIStrings.ROUTING_RULES.MENU);
    });

    it('does not show without permissions', async () => {
      givenPermissions({
        [Permissions.ADMIN]: false,
        [Permissions.BLOB_STORES.READ]: true  // Grant at least one permission so page renders
      });
      await runLinkNotVisibleTestForRepositoriesPage(UIStrings.ROUTING_RULES.MENU);
    });
  });

  async function runLinkVisiblityTestForRepositoriesPage(menu) {
    await runLinkVisiblityTest(ROUTE_NAMES.ADMIN.REPOSITORY.DIRECTORY, 'Repository', menu);
  }

  async function runLinkNotVisibleTestForRepositoriesPage(menu) {
    await runLinkNotVisibleTest(ROUTE_NAMES.ADMIN.REPOSITORY.DIRECTORY, 'Repository', menu);
  }
});
