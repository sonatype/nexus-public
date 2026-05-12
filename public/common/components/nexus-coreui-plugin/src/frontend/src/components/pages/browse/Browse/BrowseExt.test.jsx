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

import { screen, within } from '@testing-library/react';
import { ExtJS } from '@sonatype/nexus-ui-plugin';
import FeatureFlags from '../../../../constants/FeatureFlags';
import { renderComponentRoute } from '../../../../testUtils/renderUtils';

jest.mock( '../../../widgets/ExtJsContainer/useExtComponent', () => ({
  useExtComponent: jest.fn()
}));

// Phase 1: Mock ExtJS loader to return loaded state in tests
jest.mock('../../../../utils/extJsLoader', () => ({
  isExtJSLoaded: jest.fn(() => true),
  loadExtJS: jest.fn(() => Promise.resolve()),
  onExtJSLoad: jest.fn((callback) => callback())
}));

describe("BrowseExt", () => {
  beforeEach(() => {
    givenExtJSState();
    givenBundleActiveStates();

    jest.spyOn(ExtJS, 'useUser').mockReturnValue({ administrator: true });
    jest.spyOn(ExtJS, 'isProEdition').mockReturnValue(true);

    const date = new Date('2024-12-02');
    jest.useFakeTimers().setSystemTime(date);
  })
  const Permissions = global.NX.Permissions;

  it("shows malicious components CTA when enabled", async () => {
    givenExtJSState({
      ...getDefaultExtJSStateValues(),
      'nexus.malware.count': { totalCount: 24 }
    });
    givenPermissions({ 'nexus:repository-view:*:*:*': true });
    global.NX.Security.hasUser = jest.fn().mockReturnValue(true);

    await renderComponentRoute('browse.browse');

    const main = await screen.findByRole('main');
    expect(main).toBeVisible();
    const alert = within(main).getByRole('alert', { name: 'Malicious Components Found' })
    expect(alert).toBeVisible();
    expect(within(alert).getByRole('heading', { name: '24 Malicious Packages Found'})).toBeVisible();
  });

  function givenExtJSState(values = getDefaultExtJSStateValues()) {
    jest.spyOn(ExtJS, 'state').mockReturnValue({
      getValue: jest.fn().mockImplementation((key) => {
         return values[key]
       })
     });

    global.NX.State.getValue.mockImplementation((key) => values[key]);
  }

  function getDefaultExtJSStateValues() {
    const {
      MALWARE_RISK_ENABLED
    } = FeatureFlags;

    return {
      'nexus.react.browse': false,
      'nexus.malware.count': true,
      [MALWARE_RISK_ENABLED]: true,
      browseableformats: [{ id: 'maven2' }, { id: 'nuget' }]
    }
  }

  function givenBundleActiveStates(state = getDefaultBundleActiveState()) {
    global.NX.app.Application.bundleActive.mockImplementation((key) => {
      return state[key]
    })
  }

  function getDefaultBundleActiveState() {
    return {
      'nexus-coreui-plugin': true
    }
  }

   function givenPermissions(permissionLookup) {
    Permissions.check.mockImplementation(key => {
      return permissionLookup[key] ?? false;
    });
    // Set up the permissions object for permissionPrefix checks
    Permissions.permissions = permissionLookup;
  }
});
