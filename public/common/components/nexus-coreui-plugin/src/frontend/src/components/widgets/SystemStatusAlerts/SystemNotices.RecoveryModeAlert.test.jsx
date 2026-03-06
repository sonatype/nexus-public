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
import { when } from 'jest-when';
import { render, screen, within } from '@testing-library/react';
import UIStrings from '../../../constants/UIStrings';
import {
  assertCommunityEditionLimitMessageShowing
} from './SystemNotices.testutils';
import RecoveryModeAlert from './RecoveryModeAlert/RecoveryModeAlert';

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  ExtJS: {
    state: jest.fn().mockReturnValue({
      getValue: jest.fn()
    }),
    usePermission: jest.fn(),
    useState: jest.fn(),
    useUser: jest.fn().mockReturnValue({ administrator: true })
  },
}));

jest.mock('@uirouter/react', () => ({
  ...jest.requireActual('@uirouter/react'),
  useRouter: jest.fn(() => ({
    stateService: {
      go: jest.fn()
    }
  }))
}));

const {RECOVERY_MODE_ALERT: {LABEL, TEXT}} = UIStrings;

describe('SystemNotices - Recovery Mode Alert', () => {
  const ExtJS = require('@sonatype/nexus-ui-plugin').ExtJS;

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders the recovery mode alert when enabled', () => {
    givenRecoveryModeEnabled(true);
    renderComponent();

    const alert = assertCommunityEditionLimitMessageShowing(
      'While Recovery Mode is on, data repair conflicting tasks are blocked to protect data consistency.',
      LABEL
    );

    // Verify it's non-dismissable
    expect(within(alert).queryByRole('button', { name: 'Close' })).not.toBeInTheDocument();
  });

  it('does not render when recovery mode is disabled', () => {
    givenRecoveryModeEnabled(false);
    renderComponent();

    expect(screen.queryByRole('complementary', { name: 'alert system notice' })).not.toBeInTheDocument();
  });

  it('does not render when user is not admin', () => {
    ExtJS.useUser.mockReturnValue({ administrator: false });
    givenRecoveryModeEnabled(true);
    renderComponent();

    expect(screen.queryByRole('complementary', { name: 'alert system notice' })).not.toBeInTheDocument();
  });

  function renderComponent() {
    return render(<RecoveryModeAlert />);
  }

  function givenRecoveryModeEnabled(enabled) {
    when(ExtJS.state().getValue)
      .calledWith('recovery.mode.enabled')
      .mockReturnValue(enabled);
    when(ExtJS.useState)
      .calledWith(expect.any(Function))
      .mockImplementation((fn) => fn());
    when(ExtJS.usePermission)
      .calledWith(expect.any(Function), [true])
      .mockReturnValue(true);
  }
});
