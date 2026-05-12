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
import givenPermissions from '../../../testUtils/givenPermissions';
import { Permissions } from '@sonatype/nexus-ui-plugin';
import UIStrings from '../../../constants/UIStrings';
import { runLinkNotVisibleTest, runLinkVisiblityTest, runDirectoryPage404Test } from '../../../testUtils/directoryPageTestUtils';
import { ROUTE_NAMES } from '../../../routerConfig/routeNames/routeNames';
import {screen} from "@testing-library/react";
import {renderComponentRoute} from "../../../testUtils/renderUtils";

describe('AdminSystemDirectoryPage', () => {
  beforeEach(() => {
    givenUserLoggedIn();

    givenBundleActiveStates({
      'nexus-coreui-plugin': true
    });

    givenExtJSState(defaultExtState());
  });

  it('redirects to 404 when user has no permissions for any child routes', async () => {
    givenPermissions({});

    await runDirectoryPage404Test(ROUTE_NAMES.ADMIN.SYSTEM.DIRECTORY);
  });

  describe('API Link', () => {
    it('shows given permissions', async () => {
      givenPermissions({ [Permissions.SETTINGS.READ]: true });
      await runLinkVisiblityTestForSystemPage(UIStrings.API.MENU);
    });

    it('does not show without permissions', async () => {
      givenPermissions({
        [Permissions.SETTINGS.READ]: false,
        [Permissions.TASKS.READ]: true  // Grant at least one permission so page renders
      });
      await runLinkNotVisibleTestForSystemPage(UIStrings.API.MENU);
    });
  });

  describe('Capabilities Link', () => {
    it('renders both extjs and react links given sufficient permissions and feature flags enabled', async () => {
      givenPermissions({ 'nexus:capabilities:read': true });

      await renderComponentRoute(ROUTE_NAMES.ADMIN.SYSTEM.DIRECTORY);

      const extJsLink = screen.getAllByRole('link', { name: /^Capabilities\b/ })[0];
      expect(extJsLink).toBeVisible();
      expect(extJsLink.href).toContain('/#admin/system/capabilities-extjs');

      const reactLink = screen.getAllByRole('link', { name: /^Capabilities\b/ })[1];
      expect(reactLink).toBeVisible();
      expect(reactLink.href).toContain('/#admin/system/capabilities');
    });

    it('does not render any link when user does not have permissions', async () => {
      givenPermissions({
        ['nexus:capabilities:read']: false,
        [Permissions.TASKS.READ]: true  // Grant at least one permission so page renders
      });
      await runLinkNotVisibleTestForSystemPage(UIStrings.CAPABILITIES.MENU);
    })

    it('does not render react link when react feature flag is disabled', async () => {
      givenUserLoggedIn();
      givenPermissions({ 'nexus:capabilities:read': true });
      givenExtJSState({
        ...defaultExtState(),
        'nexus.react.capabilities.enabled': false
      })

      await renderComponentRoute(ROUTE_NAMES.ADMIN.SYSTEM.DIRECTORY);

      const links = screen.getAllByRole('link', { name: /^Capabilities\b/ });
      // Expect nav sidebar link + content list link (both extjs since react is disabled)
      expect(links).toHaveLength(2);
      links.forEach(link => {
        expect(link).toBeVisible();
        expect(link.href).toContain('/#admin/system/capabilities-extjs');
      });
    });

    it('does not render ExtJs link when ExtJs feature flag is disabled', async () => {
      givenUserLoggedIn();
      givenPermissions({ 'nexus:capabilities:read': true });
      givenExtJSState({
        ...defaultExtState(),
        'nexus.extjs.capabilities.enabled': false
      });

      await renderComponentRoute(ROUTE_NAMES.ADMIN.SYSTEM.DIRECTORY);

      const links = screen.getAllByRole('link', { name: /^Capabilities\b/ });
      // Expect nav sidebar link + content list link (both react since extjs is disabled)
      expect(links).toHaveLength(2);
      links.forEach(link => {
        expect(link).toBeVisible();
        expect(link.href).toContain('/#admin/system/capabilities');
        expect(link.href).not.toContain('capabilities-extjs');
      });
    });

    it('does not render any link when both ExtJs and React feature flag is disabled', async () => {
      givenPermissions({
        ['nexus:capabilities:read']: true,
        [Permissions.TASKS.READ]: true  // Grant at least one permission so page renders
      });
      givenExtJSState({
        ...defaultExtState(),
        'nexus.extjs.capabilities.enabled': false,
        'nexus.react.capabilities.enabled': false
      });
      await runLinkNotVisibleTestForSystemPage(UIStrings.CAPABILITIES.MENU);
    })
  });

  describe('Email Server Link', () => {
    it('shows given permissions', async () => {
      givenPermissions({ [Permissions.SETTINGS.READ]: true });
      await runLinkVisiblityTestForSystemPage(UIStrings.EMAIL_SERVER.MENU);
    });

    it('does not show without permissions', async () => {
      givenPermissions({
        [Permissions.SETTINGS.READ]: false,
        [Permissions.TASKS.READ]: true  // Grant at least one permission so page renders
      });
      await runLinkNotVisibleTestForSystemPage(UIStrings.EMAIL_SERVER.MENU);
    });
  });

  describe('HTTP Link', () => {
    it('shows given permissions', async () => {
      givenPermissions({ [Permissions.SETTINGS.READ]: true });
      await runLinkVisiblityTestForSystemPage(UIStrings.HTTP.MENU);
    });

    it('does not show without permissions', async () => {
      givenPermissions({
        [Permissions.SETTINGS.READ]: false,
        [Permissions.TASKS.READ]: true  // Grant at least one permission so page renders
      });
      await runLinkNotVisibleTestForSystemPage(UIStrings.HTTP.MENU);
    });
  });

  describe('Licensing Link', () => {
    it('shows given permissions', async () => {
      givenPermissions({ [Permissions.LICENSING.READ]: true });
      await runLinkVisiblityTestForSystemPage(UIStrings.LICENSING.MENU);
    });

    it('does not show without permissions', async () => {
      givenPermissions({
        [Permissions.LICENSING.READ]: false,
        [Permissions.TASKS.READ]: true  // Grant at least one permission so page renders
      });
      await runLinkNotVisibleTestForSystemPage(UIStrings.LICENSING.MENU);
    });
  });

  describe('Nodes Link', () => {
    it('shows given permissions', async () => {
      givenPermissions({ [Permissions.ADMIN]: true });
      await runLinkVisiblityTestForSystemPage(UIStrings.NODES.MENU);
    });

    it('does not show without permissions', async () => {
      givenPermissions({
        [Permissions.ADMIN]: false,
        [Permissions.TASKS.READ]: true  // Grant at least one permission so page renders
      });
      await runLinkNotVisibleTestForSystemPage(UIStrings.NODES.MENU);
    });
  });

  describe('Tasks Link', () => {
    it('shows given permissions', async () => {
      givenPermissions({ [Permissions.TASKS.READ]: true });
      await runLinkVisiblityTestForSystemPage(UIStrings.TASKS.MENU);
    });

    it('does not show without permissions', async () => {
      givenPermissions({
        [Permissions.TASKS.READ]: false,
        [Permissions.SETTINGS.READ]: true  // Grant at least one permission so page renders
      });
      await runLinkNotVisibleTestForSystemPage(UIStrings.TASKS.MENU);
    });
  });

  describe('Upgrade Link', () => {
    beforeEach(() => {
      givenExtJSState({
        ...defaultExtState(),
        capabilityActiveTypes: ['migration'],
        capabilityCreatedTypes: ['migration']
      })
    });

    it('shows given permissions', async () => {
      givenPermissions({ [Permissions.MIGRATION.READ]: true });
      await runLinkVisiblityTestForSystemPage(UIStrings.UPGRADE.MENU);
    });

    it('does not show without permissions', async () => {
      givenPermissions({
        [Permissions.MIGRATION.READ]: false,
        [Permissions.TASKS.READ]: true  // Grant at least one permission so page renders
      });
      await runLinkNotVisibleTestForSystemPage(UIStrings.UPGRADE.MENU);
    });
  });

  function defaultExtState() {
    return {
      'nexus.extjs.capabilities.enabled': true,
      'nexus.react.capabilities.enabled': true,
      usertoken: { licenseValid: true }
    };
  }

  async function runLinkVisiblityTestForSystemPage(menu) {
    await runLinkVisiblityTest(ROUTE_NAMES.ADMIN.SYSTEM.DIRECTORY, 'System', menu);
  }

  async function runLinkNotVisibleTestForSystemPage(menu) {
    await runLinkNotVisibleTest(ROUTE_NAMES.ADMIN.SYSTEM.DIRECTORY, 'System', menu);
  }
});
