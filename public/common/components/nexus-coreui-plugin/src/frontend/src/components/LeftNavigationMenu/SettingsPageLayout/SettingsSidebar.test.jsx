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
import React from 'react';
import { render, screen, within } from '@testing-library/react';
import { UIRouter, UIView } from '@uirouter/react';
import SettingsSidebar from './SettingsSidebar';
import { getRouter } from '../../../routerConfig/routerConfig';
import userEvent from '@testing-library/user-event';
import givenExtJSState from '../../../testUtils/givenExtJSState';

// This is used by the API view, it's not really something we need to
// test here, but importing it trips up jest, it's simplest to just bypass it
// with a mock
jest.mock('swagger-ui-react', () => {
  return jest.fn().mockReturnValue(null);
});

// mocking out the Welcome page to avoid having to mock all the various ExtJs functions/state required to render it
jest.mock('../../pages/user/Welcome/Welcome', () => {
  return () => (
      <main>
        <h1>Welcome Test Mock</h1>
      </main>
  );
});

describe('SettingsSidebar', () => {
  const Application = global.NX.app.Application;
  const State = global.NX.State;
  const Permissions = global.NX.Permissions;
  const Security = global.NX.Security;

  beforeEach(() => {
    // default behavior is that of an unauthenticated user on a server with some repos
    // it should show a welcome link and a browse link
    givenExtApplicationState();
    givenActiveBundles();
    givenStateValues();
    givenNoUserLoggedIn();
    givenNoPermissions();
    givenProEdition();

    // router state can get persisted between tests on the location has, clear it out each time
    // so that the tests don't influence each other when they use navigation
    window.location.hash = '';
  });

  describe('Admin links', () => {
    describe('repository links', () => {
      describe('repository link', () => {
        it('should render if user is logged in', async () => {
          givenUserLoggedIn();
          renderComponent();
          await assertLinkVisible('Repository', '/#admin/repository');
        });

        it('should not render if user is not logged in', async () => {
          renderComponent();
          await assertLinkNotVisible('Repository');
        });
      });

      describe('repositories link', () => {
        it('should render if user has permission', async () => {
          givenUserLoggedIn();
          givenPermissions({ 'nexus:repository-admin:*:*:read': true });
          renderComponent();
          await assertLinkVisible('Repositories', '/#admin/repository', 'Repository');
        });

        // We test this isn't shown if the user is not logged in, but can't currently test the scenario
        // where the user is logged in but does not have permissions to see anything under the collapsible item
        // https://sonatype.atlassian.net/browse/NEXUS-46835
        it('should not render if user has no permission', async () => {
          givenUserLoggedIn();
          renderComponent();
          await assertLinkNotVisible('Repositories', 'Repository');
        });
      });

      describe('blobstores link', () => {
        it('should render if user has permission', async () => {
          givenUserLoggedIn();
          givenPermissions({ 'nexus:blobstores:read': true });
          renderComponent();
          await assertLinkVisible('Blob Stores', '/#admin/repository/blobstores', 'Repository');
        });

        it('should not render if user has no permission', async () => {
          givenUserLoggedIn();
          renderComponent();
          await assertLinkNotVisible('Blob Stores', 'Repository');
        });
      });

      describe('data stores link', () => {
        it('should render if user has permission and has active bundles', async () => {
          givenUserLoggedIn();
          givenPermissions({ 'nexus:*': true });
          givenActiveBundles({
            ...getDefaultActiveBundleState(),
            'nexus-pro-datastore-plugin': true,
          });
          renderComponent();
          await assertLinkVisible('Data Store', '/#admin/repository/datastore', 'Repository');
        });

        it('should not render if user has no permission', async () => {
          givenUserLoggedIn();
          givenActiveBundles({
            ...getDefaultActiveBundleState(),
            'nexus-pro-datastore-plugin': true,
          });
          renderComponent();
          await assertLinkNotVisible('Data Store', 'Repository');
        });
      });

      describe('proprietary repositories link', () => {
        it('should render if user has permission', async () => {
          givenUserLoggedIn();
          givenPermissions({ 'nexus:settings:read': true });
          renderComponent();
          await assertLinkVisible('Proprietary Repositories', '/#admin/repository/proprietary', 'Repository');
        });

        it('should not render if user has no permission', async () => {
          givenUserLoggedIn();
          renderComponent();
          await assertLinkNotVisible('Proprietary Repositories', 'Repository');
        });
      });

      describe('content selectors link', () => {
        it('should render if user has permission and active bundle', async () => {
          givenUserLoggedIn();
          givenPermissions({ 'nexus:selectors:read': true });
          renderComponent();
          await assertLinkVisible('Content Selectors', '/#admin/repository/selectors', 'Repository');
        });

        it('should not render if user has no permission', async () => {
          givenUserLoggedIn();
          renderComponent();
          await assertLinkNotVisible('Content Selectors', 'Repository');
        });
      });

      describe('cleanup policies link', () => {
        it('should render if user has permission and has active bundles', async () => {
          givenUserLoggedIn();
          givenPermissions({ 'nexus:*': true });
          renderComponent();
          await assertLinkVisible('Cleanup Policies', '/#admin/repository/cleanuppolicies', 'Repository');
        });

        it('should not render if user has no permission', async () => {
          givenUserLoggedIn();
          renderComponent();
          await assertLinkNotVisible('Cleanup Policies', 'Repository');
        });
      });

      describe('routing rules link', () => {
        it('should render if user has permission and has active bundles', async () => {
          givenUserLoggedIn();
          givenPermissions({ 'nexus:*': true });
          renderComponent();
          await assertLinkVisible('Routing Rules', '/#admin/repository/routingrules', 'Repository');
        });

        it('should not render if user has no permission', async () => {
          givenUserLoggedIn();
          renderComponent();
          await assertLinkNotVisible('Routing Rules', 'Repository');
        });
      });
    });

    describe('security links', () => {
      describe('security link', () => {
        it('should render if user is logged in', async () => {
          givenUserLoggedIn();
          renderComponent();
          await assertLinkVisible('Security', '/#admin/security');
        });

        // We test this isn't shown if the user is not logged in, but can't currently test the scenario
        // where the user is logged in but does not have permissions to see anything under the collapsible item
        // https://sonatype.atlassian.net/browse/NEXUS-46835
        it('should not render if user is not logged in', async () => {
          renderComponent();
          await assertLinkNotVisible('Security');
        });
      });

      describe('privileges link', () => {
        it('should render if user has permission and has active bundles', async () => {
          givenUserLoggedIn();
          givenPermissions({ 'nexus:privileges:read': true });
          renderComponent();
          await assertLinkVisible('Privileges', '/#admin/security/privileges', 'Security');
        });

        it('should not render if user has no permission', async () => {
          givenUserLoggedIn();
          renderComponent();
          await assertLinkNotVisible('Privileges', 'Security');
        });
      });

      describe('roles link', () => {
        it('should render if user has permission and has active bundles', async () => {
          givenUserLoggedIn();
          givenPermissions({ 'nexus:privileges:read': true, 'nexus:roles:read': true });
          renderComponent();
          await assertLinkVisible('Roles', '/#admin/security/roles', 'Security');
        });

        it('should not render if user has no roles permission', async () => {
          givenUserLoggedIn();
          givenPermissions({ 'nexus:privileges:read': true });
          renderComponent();
          await assertLinkNotVisible('Roles', 'Security');
        });

        it('should not render if user has no privileges permission', async () => {
          givenUserLoggedIn();
          givenPermissions({ 'nexus:roles:read': true });
          renderComponent();
          await assertLinkNotVisible('Roles', 'Security');
        });
      });

      describe('SslCertificates link', () => {
        it('should render if user has permission and has active bundles', async () => {
          givenUserLoggedIn();
          givenPermissions({ 'nexus:ssl-truststore:read': true });
          renderComponent();
          await assertLinkVisible('SSL Certificates', '/#admin/security/sslcertificates', 'Security');
        });

        it('should not render if user has no roles permission', async () => {
          givenUserLoggedIn();
          renderComponent();
          await assertLinkNotVisible('SSL Certificates', 'Security');
        });
      });

      describe('ldap link', () => {
        it('should render if user has permission and has active bundles', async () => {
          givenUserLoggedIn();
          givenPermissions({ 'nexus:ldap:read': true });
          renderComponent();
          await assertLinkVisible('LDAP', '/#admin/security/ldap', 'Security');
        });

        it('should not render if user has no roles permission', async () => {
          givenUserLoggedIn();
          renderComponent();
          await assertLinkNotVisible('LDAP', 'Security');
        });
      });

      describe('users link', () => {
        it('should render if user has permission and has active bundles', async () => {
          givenUserLoggedIn();
          givenPermissions({ 'nexus:users:read': true, 'nexus:roles:read': true });
          renderComponent();
          await assertLinkVisible('Users', '/#admin/security/users', 'Security');
        });

        it('should not render if user has no roles permission', async () => {
          givenUserLoggedIn();
          givenPermissions({ 'nexus:users:read': true });
          renderComponent();
          await assertLinkNotVisible('Users', 'Security');
        });

        it('should not render if user has no privileges permission', async () => {
          givenUserLoggedIn();
          givenPermissions({ 'nexus:roles:read': true });
          renderComponent();
          await assertLinkNotVisible('Users', 'Security');
        });
      });

      describe('anonymous link', () => {
        it('should render if user has permission and has active bundles', async () => {
          givenUserLoggedIn();
          givenPermissions({ 'nexus:settings:read': true });
          renderComponent();
          await assertLinkVisible('Anonymous Access', '/#admin/security/anonymous', 'Security');
        });

        it('should not render if user has no roles permission', async () => {
          givenUserLoggedIn();
          renderComponent();
          await assertLinkNotVisible('Anonymous Settings', 'Security');
        });
      });

      describe('realms link', () => {
        it('should render if user has permission and has active bundles', async () => {
          givenUserLoggedIn();
          givenPermissions({ 'nexus:settings:read': true });
          renderComponent();
          await assertLinkVisible('Realms', '/#admin/security/realms', 'Security');
        });

        it('should not render if user has no roles permission', async () => {
          givenUserLoggedIn();
          renderComponent();
          await assertLinkNotVisible('Realms', 'Security');
        });
      });

      describe('user tokens link', () => {
        it('should render if user has permission pro edition and valid license', async () => {
          givenUserLoggedIn();
          givenPermissions({ 'nexus:usertoken-settings:read': true });
          givenProEdition();
          givenStateValues({
            ...getDefaultStateValues(),
            usertoken: { licenseValid: true },
          });
          renderComponent();
          await assertLinkVisible('User Tokens', '/#admin/security/usertoken', 'Security');
        });

        it('should not render if user has no usertoken-settings read permission', async () => {
          givenUserLoggedIn();
          givenProEdition();
          givenStateValues({
            ...getDefaultStateValues(),
            usertoken: { licenseValid: true },
          });
          renderComponent();
          await assertLinkNotVisible('User Tokens', 'Security');
        });

        it('should not render if user has community edition', async () => {
          givenUserLoggedIn();
          givenPermissions({ 'nexus:usertoken-settings:read': true });
          givenCommunityEdition();
          givenStateValues({
            ...getDefaultStateValues(),
            usertoken: { licenseValid: true },
          });
          renderComponent();
          await assertLinkNotVisible('User Tokens', 'Security');
        });

        it('should not render if user has no valid license', async () => {
          givenUserLoggedIn();
          givenPermissions({ 'nexus:usertoken-settings:read': true });
          givenProEdition();
          renderComponent();
          await assertLinkNotVisible('User Tokens', 'Security');
        });
      });

      describe('atlassian crowd link', () => {
        it('should render if user has permission, valid bundle and valid license', async () => {
          givenUserLoggedIn();
          givenPermissions({ 'nexus:crowd:read': true });
          givenActiveBundles({ 'nexus-crowd-plugin': true });
          givenStateValues({
            ...getDefaultStateValues(),
            crowd: { licenseValid: true },
          });
          renderComponent();
          await assertLinkVisible('Atlassian Crowd', '/#admin/security/atlassiancrowd', 'Security');
        });

        it('should not render if user has no crowd read permission', async () => {
          givenUserLoggedIn();
          givenStateValues({
            ...getDefaultStateValues(),
            crowd: { licenseValid: true },
          });
          givenActiveBundles({ 'nexus-crowd-plugin': true });
          renderComponent();
          await assertLinkNotVisible('Atlassian Crowd', 'Security');
        });

        it('should not render if user has no active bundle active bundle', async () => {
          givenUserLoggedIn();
          givenPermissions({ 'nexus:crowd:read': true });
          givenStateValues({
            ...getDefaultStateValues(),
            crowd: { licenseValid: true },
          });
          givenActiveBundles({});
          renderComponent();
          await assertLinkNotVisible('Atlassian Crowd', 'Security');
        });

        it('should not render if user has no valid license', async () => {
          givenUserLoggedIn();
          givenPermissions({ 'nexus:crowd:read': true });
          givenActiveBundles({ 'nexus-crowd-plugin': true });
          givenStateValues({ ...getDefaultStateValues(), crowd: { licenseValue: true} })
          renderComponent();
          await assertLinkNotVisible('Atlassian Crowd', 'Security');
        });
      });

      describe('saml link', () => {
        it('should render if user has permission and has active bundles', async () => {
          givenUserLoggedIn();
          givenPermissions({ 'nexus:*': true });
          givenProEdition();
          givenActiveBundles({
            ...getDefaultActiveBundleState(),
            'nexus-saml-plugin': true,
          });
          renderComponent();
          await assertLinkVisible('SAML', '/#admin/security/saml', 'Security');
        });

        it('should not render if user has no admin permission', async () => {
          givenUserLoggedIn();
          givenProEdition();
          givenActiveBundles({
            ...getDefaultActiveBundleState(),
            'nexus-saml-plugin': true,
          });
          renderComponent();
          await assertLinkNotVisible('SAML', 'Security');
        });

        it('should not render if user has no active bundle', async () => {
          givenUserLoggedIn();
          givenPermissions({ 'nexus:*': true });
          givenProEdition();
          givenActiveBundles({});
          renderComponent();
          await assertLinkNotVisible('SAML', 'Security');
        });

        it('should not render if user has no PRO edition', async () => {
          givenUserLoggedIn();
          givenPermissions({ 'nexus:*': true });
          givenActiveBundles({
            ...getDefaultActiveBundleState(),
            'nexus-saml-plugin': true,
          });
          givenCommunityEdition();
          renderComponent();
          await assertLinkNotVisible('SAML', 'Security');
        });
      });
    });

    describe('support links', () => {
      describe('support link', () => {
        it('should render if user is logged in', async () => {
          givenUserLoggedIn();
          renderComponent();
          await assertLinkVisible('Support', '/#admin/support');
        });

        // We test this isn't shown if the user is not logged in, but can't currently test the scenario
        // where the user is logged in but does not have permissions to see anything under the collapsible item
        // https://sonatype.atlassian.net/browse/NEXUS-46835
        it('should not render if user is not logged in', async () => {
          renderComponent();
          await assertLinkNotVisible('Support');
        });
      });

      describe('Support Request link', () => {
        it('should render if user has permission and has active bundles', async () => {
          givenUserLoggedIn();
          givenPermissions({ 'nexus:atlas:create': true });
          givenProEdition();
          renderComponent();
          await assertLinkVisible('Support Request', '/#admin/support/supportrequest', 'Support');
        });

        it('should not render if user has no atlas create permission', async () => {
          givenUserLoggedIn();
          renderComponent();
          givenProEdition();
          await assertLinkNotVisible('Support Request', 'Support');
        });

        it('should not render if user has Community version', async () => {
          givenUserLoggedIn();
          givenPermissions({ 'nexus:atlas:create': true });
          givenCommunityEdition();
          renderComponent();
          await assertLinkNotVisible('Support Request', 'Support');
        });
      });

      describe('System Information link', () => {
        it('should render if user has permission and has active bundles', async () => {
          givenUserLoggedIn();
          givenPermissions({ 'nexus:atlas:read': true });
          renderComponent();
          await assertLinkVisible('System Information', '/#admin/support/systeminformation', 'Support');
        });

        it('should not render if user has no atlas read permission', async () => {
          givenUserLoggedIn();
          renderComponent();
          await assertLinkNotVisible('System Information', 'Support');
        });
      });

      describe('System Status link', () => {
        it('should render if user has permission and has active bundles', async () => {
          givenUserLoggedIn();
          givenPermissions({ 'nexus:metrics:read': true });
          renderComponent();
          await assertLinkVisible('Status', '/#admin/support/status', 'Support');
        });

        it('should not render if user has no metrics read permission', async () => {
          givenUserLoggedIn();
          renderComponent();
          await assertLinkNotVisible('Status', 'Support');
        });
      });

      describe('Support Zip link', () => {
        it('should render if user has permission and has active bundles', async () => {
          givenUserLoggedIn();
          givenPermissions({ 'nexus:atlas:read': true });
          renderComponent();
          await assertLinkVisible('Support ZIP', '/#admin/support/supportzip', 'Support');
        });

        it('should not render if user has no atlas read permission', async () => {
          givenUserLoggedIn();
          renderComponent();
          await assertLinkNotVisible('Support ZIP', 'Support');
        });
      });

      describe('Logs link', () => {
        it('should render if user has permission and has active bundles', async () => {
          givenUserLoggedIn();
          givenPermissions({ 'nexus:logging:read': true });
          renderComponent();
          await assertLinkVisible('Logs', '/#admin/support/logs', 'Support');
        });

        it('should not render if user has no logging read permission', async () => {
          givenUserLoggedIn();
          renderComponent();
          await assertLinkNotVisible('Logs', 'Support');
        });

        it('should not render if clustering is enabled on the system', async () => {
          givenUserLoggedIn();
          givenPermissions({ 'nexus:logging:read': true });
          givenExtJSState({
            ...getDefaultStateValues(),
            'nexus.datastore.clustered.enabled': true
          });

          renderComponent();

          await assertLinkNotVisible('Logs', 'Support');
        });
      });

      describe('Logging link', () => {
        it('should render if user has permission and has active bundles', async () => {
          givenUserLoggedIn();
          givenPermissions({ 'nexus:logging:read': true });
          renderComponent();
          await assertLinkVisible('Logging', '/#admin/support/logging', 'Support');
        });

        it('should not render if user has no logging read permission', async () => {
          givenUserLoggedIn();
          renderComponent();
          await assertLinkNotVisible('Logging', 'Support');
        });
      });
    });

    describe('system links', () => {
      describe('system link', () => {
        it('should render if user is logged in', async () => {
          givenUserLoggedIn();
          renderComponent();
          await assertLinkVisible('System', '/#admin/system');
        });

        // We test this isn't shown if the user is not logged in, but can't currently test the scenario
        // where the user is logged in but does not have permissions to see anything under the collapsible item
        // https://sonatype.atlassian.net/browse/NEXUS-46835
        it('should not render if user is not logged in', async () => {
          renderComponent();
          await assertLinkNotVisible('System');
        });
      });


      describe('API link', () => {
        it('should render if user has permission', async () => {
          givenUserLoggedIn();
          givenPermissions({ 'nexus:settings:read': true });
          renderComponent();
          await assertLinkVisible('API', '/#admin/system/api', 'System');
        });

        it('should not render if user has no permission', async () => {
          givenUserLoggedIn();
          renderComponent();
          await assertLinkNotVisible('API', 'System');
        });
      });

      describe('email server link', () => {
        it('should render if user has permission and has active bundles', async () => {
          givenUserLoggedIn();
          givenPermissions({ 'nexus:settings:read': true });
          renderComponent();

          await assertLinkVisible('Email Server', '/#admin/system/emailserver', 'System');
        });

        it('should not render if user has no permission', async () => {
          givenUserLoggedIn();
          renderComponent();

          await assertLinkNotVisible('Email Server', 'System');
        });
      });

      describe('Tasks link', () => {
        it('should render if user has permission and has active bundles', async () => {
          givenUserLoggedIn();
          givenPermissions({ 'nexus:tasks:read': true });
          renderComponent();
          await assertLinkVisible('Tasks', '/#admin/system/tasks', 'System');
        });

        it('should not render if user has no tasks read permission', async () => {
          givenUserLoggedIn();
          renderComponent();
          await assertLinkNotVisible('Tasks', 'System');
        });
      });

      describe('capabilities link', () => {
        it('renders both extjs and react links given sufficient permissions and feature flags enabled', async () => {
          givenUserLoggedIn();
          givenPermissions({ 'nexus:capabilities:read': true });

          renderComponent();
          await expandMenu('System');
          const extJsLink = screen.getAllByRole('link', { name: 'Capabilities' })[0];
          expect(extJsLink).toBeVisible();
          expect(extJsLink.href).toContain('/#admin/system/capabilities-extjs');

          const reactLink = screen.getAllByRole('link', { name: 'Capabilities' })[1];
          expect(reactLink).toBeVisible();
          expect(reactLink.href).toContain('/#admin/system/capabilities');
        });

        it('does not render any link when user does not have permissions', async () => {
          givenUserLoggedIn();
          renderComponent();
          await assertLinkNotVisible('Capabilities', 'System');
        });

        it('does not render react link when react feature flag is disabled', async () => {
          givenUserLoggedIn();
          givenPermissions({ 'nexus:capabilities:read': true });
          givenExtJSState({
            ...getDefaultStateValues(),
            'nexus.react.capabilities.enabled': false
          });
          renderComponent();
          await expandMenu('System');
          const links = screen.getAllByRole('link', { name: 'Capabilities' });
          expect(links).toHaveLength(1);

          const extJsLink = links[0];
          expect(extJsLink).toBeVisible();
          expect(extJsLink.href).toContain('/#admin/system/capabilities-extjs');
        });

        it('does not render ExtJs link when ExtJs feature flag is disabled', async () => {
          givenUserLoggedIn();
          givenPermissions({ 'nexus:capabilities:read': true });
          givenExtJSState({
            ...getDefaultStateValues(),
            'nexus.extjs.capabilities.enabled': false
          });
          renderComponent();
          await expandMenu('System');
          const links = screen.getAllByRole('link', { name: 'Capabilities' });
          expect(links).toHaveLength(1);

          const reactLink = links[0];
          expect(reactLink).toBeVisible();
          expect(reactLink.href).toContain('/#admin/system/capabilities');
        });

        it('does not render any link when both ExtJs and React feature flag is disabled', async () => {
          givenUserLoggedIn();
          givenPermissions({ 'nexus:capabilities:read': true });
          givenExtJSState({
            ...getDefaultStateValues(),
            'nexus.extjs.capabilities.enabled': false,
            'nexus.react.capabilities.enabled': false
          });
          renderComponent();
          await assertLinkNotVisible('Capabilities', 'System');
        });
      });

      describe('Http link', () => {
        it('should render if user has permission and has active bundles', async () => {
          givenUserLoggedIn();
          givenPermissions({ 'nexus:settings:read': true });
          renderComponent();
          await assertLinkVisible('HTTP', '/#admin/system/http', 'System');
        });

        it('should not render if user has no settings read permission', async () => {
          givenUserLoggedIn();
          renderComponent();
          await assertLinkNotVisible('HTTP', 'System');
        });
      });

      describe('nodes link', () => {
        it('should render if user has permission', async () => {
          givenUserLoggedIn();
          givenPermissions({ 'nexus:*': true });
          renderComponent();
          await assertLinkVisible('Nodes', '/#admin/system/nodes', 'System');
        });

        it('should not render if user has no admin permission', async () => {
          givenUserLoggedIn();
          renderComponent();
          await assertLinkNotVisible('Nodes', 'System');
        });
      });

      describe('upgrade link', () => {
        it('should render if user is admin and has read permissions', async () => {
          givenUserLoggedIn();
          givenPermissions({ 'nexus:migration:read': true });
          givenExtJSState({
            ...getDefaultStateValues(),
            capabilityActiveTypes: ['migration'],
            capabilityCreatedTypes: ['migration']
          });

          renderComponent();

          await assertLinkVisible('Upgrade', '/#admin/system/upgrade', 'System');
        });

        it('should not render if user is admin but does not have read permissions', async () => {
          givenUserLoggedIn();
          givenExtJSState({
            ...getDefaultStateValues(),
            capabilityActiveTypes: ['migration'],
            capabilityCreatedTypes: ['migration']
          });
          renderComponent();
          await assertLinkNotVisible('Upgrade', 'System');
        });
      });
    });

    describe('IQ link', () => {
      it('should render if user has permission', async () => {
        givenPermissions({ 'nexus:settings:read': true });
        renderComponent();
        await assertLinkVisible('IQ Server', '/#admin/iq');
      });

      it('should not render if user has no admin permission', async () => {
        renderComponent();
        await assertLinkNotVisible('IQ Server');
      });
    });
  });

  function renderComponent() {
    return render(
      <UIRouter router={getRouter()}>
        <SettingsSidebar />

        <UIView />
      </UIRouter>
    );
  }

  function givenExtApplicationState({ mockAppStateStore, mockPermissionController } = {}) {
    mockAppStateStore = !!mockAppStateStore ? mockAppStateStore : getDefaultAppStateStoreMock();

    mockPermissionController = !!mockPermissionController
      ? mockPermissionController
      : getDefaultPermissionControllerMock();

    global.Ext.getApplication.mockReturnValue({
      getStore: jest.fn().mockReturnValue(mockAppStateStore),
      getController: jest.fn().mockReturnValue(mockPermissionController),
    });
  }

  function getDefaultAppStateStoreMock() {
    return {
      on: jest.fn(),
      un: jest.fn(),
    };
  }

  function getDefaultPermissionControllerMock() {
    return {
      on: jest.fn(),
      un: jest.fn(),
    };
  }

  function givenActiveBundles(activeBundleState = {}) {
    Application.bundleActive.mockImplementation(key => activeBundleState[key] ?? false);
  }

  function getDefaultActiveBundleState() {
    return {};
  }

  function givenStateValues(stateValues = getDefaultStateValues()) {
    State.getValue.mockImplementation((key, defaultValue) => stateValues[key] ?? defaultValue);
  }

  function getDefaultStateValues() {
    return {
      'nexus.react.welcome': true,
      'nexus.extjs.capabilities.enabled': true,
      'nexus.react.capabilities.enabled': true,
      usertoken: { licenseValid: false },
    };
  }

  function givenNoPermissions() {
    givenPermissions({});
  }

  function givenPermissions(permissionLookup) {
    Permissions.check.mockImplementation(key => {
      return permissionLookup[key] ?? false;
    });

    Permissions.permissions = permissionLookup;
  }

  function givenNoUserLoggedIn() {
    Security.hasUser.mockReturnValue(false);
  }

  function givenUserLoggedIn() {
    Security.hasUser.mockReturnValue(true);
  }

  function givenProEdition() {
    State.getEdition.mockReturnValue('PRO');
  }

  function givenCommunityEdition() {
    State.getEdition.mockReturnValue('COMMUNITY');
  }

  async function assertLinkVisible(name, path, parentLinkName) {
    if (parentLinkName) {
      await expandMenu(parentLinkName);
    }

    const link = screen.getByRole('link', { name });
    expect(link).toBeVisible();
    expect(link.href).toContain(path);

    return link;
  }

  async function assertLinkNotVisible(name, parentLinkName) {
    if (parentLinkName) {
      await expandMenu(parentLinkName);
    }

    const link = screen.queryByRole('link', { name });
    expect(link).not.toBeInTheDocument();
  }

  async function expandMenu(collapsibleMenuName) {
    // expand the parent menu
    const collapsibleMenu = screen.getByRole('link', { name: collapsibleMenuName });
    const expandButton = within(collapsibleMenu).getByRole('button', { name: 'Expand Menu'});
    expect(expandButton).toBeVisible();
    await userEvent.click(expandButton);
    expect(await within(collapsibleMenu).findByRole('button', { name: 'Collapse Menu'})).toBeVisible();
  }
});
