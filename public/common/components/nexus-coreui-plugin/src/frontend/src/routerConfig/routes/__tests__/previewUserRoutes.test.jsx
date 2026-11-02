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

/**
 * NEXUS-55523 — the Preview User Token route shipped without visibilityRequirements, so
 * isVisible() returned true for everyone (absent requirements mean "no restrictions") and
 * direct navigation to #preview/user/usertoken rendered the backend's authorization failure
 * instead of redirecting. These tests drive the real isVisible() against the real route
 * config, so they fail if the gate is removed OR weakened to something that still admits
 * an unauthorized user.
 */

import { UIView } from '@uirouter/react';
import {
  isVisible,
  PRO_FAMILY_EDITIONS,
  EDITION_COMMUNITY,
  EDITION_PRO,
  EDITION_PRO_PLUS,
  EDITION_ENTERPRISE,
  EDITION_ID_STATE_KEY,
} from '@sonatype/nexus-ui-plugin';

import { ROUTE_NAMES } from '../../routeNames/routeNames';
import { previewUserRoutes } from '../previewUserRoutes';
import { userRoutes } from '../userRoutes';

const USER_TOKEN_BUNDLE = 'nexus-usertoken-plugin';
const USER_TOKEN_PERMISSION = 'nexus:usertoken-current:read';
const USER_TOKEN_REALM_STATE = 'userTokenRealmEnabled';

const findPreviewRoute = (name) => previewUserRoutes.find((route) => route.name === name);

const requirementsFor = (name) => findPreviewRoute(name)?.data?.visibilityRequirements;

/**
 * Puts the app in the one configuration where User Token is legitimately reachable:
 * Pro-family edition, user token bundle active, realm enabled, permission granted.
 * Individual tests then revoke exactly one of those to prove the gate denies.
 */
function grantFullyAuthorizedUserTokenAccess({
  bundleActive = true,
  realmEnabled = true,
  hasPermission = true,
  editionId = EDITION_PRO,
} = {}) {
  window.NX.getApplication.mockReturnValue({});
  window.NX.app.Application.bundleActive.mockImplementation(
    (bundle) => bundle === USER_TOKEN_BUNDLE && bundleActive
  );
  window.NX.Permissions.permissions = {};
  window.NX.Permissions.check.mockImplementation(
    (permission) => permission === USER_TOKEN_PERMISSION && hasPermission
  );
  window.NX.State.getValue.mockImplementation((key, defaultValue) => {
    if (key === EDITION_ID_STATE_KEY) {
      return editionId;
    }
    if (key === USER_TOKEN_REALM_STATE) {
      return realmEnabled;
    }
    return defaultValue;
  });
}

describe('previewUserRoutes', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    grantFullyAuthorizedUserTokenAccess();
  });

  it('shouldDefinePreviewUserAsTheAbstractRouteContainer', () => {
    const previewUserRoute = findPreviewRoute('preview.user');

    expect(previewUserRoute).toBeDefined();
    expect(previewUserRoute.abstract).toBe(true);
    expect(previewUserRoute.component).toBe(UIView);
  });

  it('shouldDeclareVisibilityRequirementsOnTheUserTokenRoute', () => {
    // The regression itself: the route previously had only {title}, so isVisible() short-circuited
    // to true and every user could reach the page.
    expect(requirementsFor('preview.user.usertoken')).toBeDefined();
  });

  it('shouldGateUserTokenWithTheSameRequirementsAsTheClassicRoute', () => {
    const classicRequirements = userRoutes.find(
      (route) => route.name === ROUTE_NAMES.USER.USER_TOKEN
    )?.data?.visibilityRequirements;

    expect(classicRequirements).toBeDefined();
    expect(requirementsFor('preview.user.usertoken')).toEqual(classicRequirements);
  });

  it('shouldShowUserTokenWhenEveryRequirementIsSatisfied', () => {
    expect(isVisible(requirementsFor('preview.user.usertoken'))).toBe(true);
  });

  it('shouldHideUserTokenWhenThePermissionIsMissing', () => {
    grantFullyAuthorizedUserTokenAccess({ hasPermission: false });

    expect(isVisible(requirementsFor('preview.user.usertoken'))).toBe(false);
  });

  it('shouldHideUserTokenWhenTheRealmIsDisabled', () => {
    grantFullyAuthorizedUserTokenAccess({ realmEnabled: false });

    expect(isVisible(requirementsFor('preview.user.usertoken'))).toBe(false);
  });

  it('shouldHideUserTokenWhenTheUserTokenBundleIsInactive', () => {
    grantFullyAuthorizedUserTokenAccess({ bundleActive: false });

    expect(isVisible(requirementsFor('preview.user.usertoken'))).toBe(false);
  });

  it('shouldHideUserTokenOnCommunityEdition', () => {
    grantFullyAuthorizedUserTokenAccess({ editionId: EDITION_COMMUNITY });

    expect(isVisible(requirementsFor('preview.user.usertoken'))).toBe(false);
  });

  it.each([
    [EDITION_PRO, 'Pro'],
    [EDITION_PRO_PLUS, 'Pro Plus'],
    [EDITION_ENTERPRISE, 'Enterprise'],
  ])('shouldShowUserTokenOnEveryProFamilyEdition_%s', (editionId) => {
    // AT-015 parity: a Pro-visible route must stay visible on the higher tiers.
    grantFullyAuthorizedUserTokenAccess({ editionId });

    expect(isVisible(requirementsFor('preview.user.usertoken'))).toBe(true);
  });

  it('shouldGateUserTokenByProFamilyEditionsRatherThanAnInlineList', () => {
    expect(requirementsFor('preview.user.usertoken').editions).toBe(PRO_FAMILY_EDITIONS);
  });
});
