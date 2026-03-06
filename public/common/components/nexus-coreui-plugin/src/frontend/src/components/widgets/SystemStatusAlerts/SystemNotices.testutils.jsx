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

import { when } from 'jest-when';
import { ExtJS } from '@sonatype/nexus-ui-plugin';
import { USAGE_CENTER_CONTENT_CE } from '../../pages/user/Welcome/UsageCenter/UsageCenter.testdata';
import { UpgradeAlertFunctions } from './UpgradeAlert/UpgradeAlertHelper';
import { helperFunctions } from './CELimits/UsageHelper';
import { render, screen, within } from '@testing-library/react';
import SystemNotices from './SystemNotices';
import React from 'react';
import userEvent from '@testing-library/user-event';

const {
  useGracePeriodEndsDate,
  useThrottlingStatusValue
} = helperFunctions;

export function renderView(gracePeriodEnd, throttlingStatus, currentState, message, recoveryModeEnabled) {
  givenInitialState(gracePeriodEnd, throttlingStatus, currentState, message, recoveryModeEnabled)

  return render(<SystemNotices onClose={jest.fn()} />);
}

export function givenInitialState(gracePeriodEnd, throttlingStatus, currentState, message, recoveryModeEnabled) {
  givenGetValue(currentState, message, recoveryModeEnabled, throttlingStatus, gracePeriodEnd)
  givenUseState(throttlingStatus, gracePeriodEnd, currentState, message);

  when(ExtJS.usePermission)
    .calledWith(UpgradeAlertFunctions.checkPermissions)
    .mockReturnValue(true);

  // Mock NX.State.getUser to return a user so hasUser() returns true
  global.NX.State.getUser.mockReturnValue({ username: 'test-user' });
}

function givenUseState(throttlingStatus, gracePeriodEnd, currentState, message) {
  when(ExtJS.useState)
      .calledWith(useThrottlingStatusValue)
      .mockReturnValue(throttlingStatus);
  when(ExtJS.useState)
      .calledWith(UpgradeAlertFunctions.hasUser)
      .mockReturnValue(true);
  when(ExtJS.useState)
      .calledWith(useGracePeriodEndsDate)
      .mockReturnValue(new Date(gracePeriodEnd));

  when(ExtJS.useState)
      .calledWith(UpgradeAlertFunctions.currentState)
      .mockReturnValue(currentState);

  when(ExtJS.useState)
      .calledWith(UpgradeAlertFunctions.message)
      .mockReturnValue(message);

  when(ExtJS.useState)
      .calledWith(expect.any(Function))
      .mockImplementation((fn) => fn());

}

function givenGetValue(currentState, message, recoveryModeEnabled, throttlingStatus, gracePeriodEnd) {
  when(ExtJS.state().getValue)
      .calledWith('contentUsageEvaluationResult', [])
      .mockReturnValue(USAGE_CENTER_CONTENT_CE);
  when(ExtJS.state().getValue)
      .calledWith('dbUpgrade', expect.anything())
      .mockReturnValue({ currentState, message });
  when(ExtJS.state().getValue)
      .calledWith('nexus.node.id')
      .mockReturnValue('node-example-id');
  when(ExtJS.state().getValue)
      .calledWith('nexus.datastore.clustered.enabled')
      .mockReturnValue(false);
  when(ExtJS.state().getValue)
      .calledWith('nexus.malware.count')
      .mockReturnValue({ totalCount: 3 });
  when(ExtJS.state().getValue)
      .calledWith('recovery.mode.enabled')
      .mockReturnValue(recoveryModeEnabled);
  when(ExtJS.state().getValue)
      .calledWith('nexus.community.throttlingStatus')
      .mockReturnValue(throttlingStatus);
  when(ExtJS.state().getValue)
      .calledWith('nexus.community.gracePeriodEnds')
      .mockReturnValue(gracePeriodEnd);
}


export function assertCommunityEditionLimitMessageShowing(message, title) {
  const alert = screen.getByRole('complementary', { name: 'alert system notice'});
  expect(alert).toBeVisible();

  if (title) {
    expect(within(alert).getByRole('heading', {name: title})).toBeVisible();
  }

  expect(within(alert).getByText(message, { exact: false })).toBeVisible()

  return alert;
}

export async function assertCanDismissTheBanner(alert) {
  const closeButton = within(alert).getByRole('button', { name: 'Close' });
  expect(closeButton).toBeVisible();
  await userEvent.click(closeButton);

  expect(screen.queryByRole('complementary', { name: 'alert system notice'})).not.toBeInTheDocument();
}

export function assertHasLinkWithText(alert, text, href) {
  const lnk = within(alert).getByRole('link', {name: text});
  expect(lnk).toBeVisible();
  expect(lnk).toHaveAttribute('href', href);
}
