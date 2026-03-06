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
import UIStrings from '../../../constants/UIStrings';
import {
  assertCanDismissTheBanner,
  assertCommunityEditionLimitMessageShowing, assertHasLinkWithText,
  renderView
} from './SystemNotices.testutils';
import UpgradeAlertStrings from '../../../constants/UpgradeAlertStrings';
import userEvent from '@testing-library/user-event';
import axios from 'axios';

jest.mock('axios', () => ({
  ...jest.requireActual('axios'),
  get: jest.fn(),
  delete: jest.fn()
}));

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  ExtJS: {
    isProEdition: jest.fn().mockReturnValue(false),
    state: jest.fn().mockReturnValue({
      getValue: jest.fn(),
      getEdition: jest.fn().mockReturnValue('COMMUNITY')
    }),
    useState: jest.fn(),
    usePermission: jest.fn(),
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

const {UPGRADE_ALERT: {PENDING, PROGRESS, ERROR, COMPLETE, WARN}} = UIStrings;

describe('SystemNotices - Upgrade Alert', () => {
  it('renders the upgrade alert for pending upgrade', async () => {
    await renderComponent('needsUpgrade');

    const expectedMessage = UpgradeAlertStrings.UPGRADE_ALERT.PENDING.TEXT;
    const expectedTitle = UpgradeAlertStrings.UPGRADE_ALERT.PENDING.LABEL;
    const alert = assertCommunityEditionLimitMessageShowing(expectedMessage, expectedTitle);

    assertDismissButtonNotShown(alert);

    const finalizeBtn =
        within(alert).getByRole('button', { name: UpgradeAlertStrings.UPGRADE_ALERT.PENDING.FINALIZE_BUTTON });
    expect(finalizeBtn).toBeVisible();

    // should show modal when user clicks finalzie
    await userEvent.click(finalizeBtn);
    const modal = await screen.findByRole('dialog', { name: 'Proceed with upgrade?' })
    expect(modal).toBeVisible();

    // should dismiss modal on cancel
    const cancelBtn = await within(modal).getByRole('button', { name:  'Cancel' });
    expect(cancelBtn).toBeVisible();
    await userEvent.click(cancelBtn);
    expect(screen.queryByRole('dialog', { name: 'Proceed with upgrade?' })).not.toBeInTheDocument();
  });

  it('renders the upgrade alert for node version mismatch', async () => {
    renderComponent('versionMismatch');

    const alert = assertCommunityEditionLimitMessageShowing(WARN.TEXT, WARN.LABEL);
    await assertCanDismissTheBanner(alert);
    expect(axios.delete).toHaveBeenCalledWith('service/rest/v1/clustered/upgrade-database-schema');
  });

  it('renders the upgrade alert for upgrade in progress', async () => {
    renderComponent('nexusUpgradeInProgress');

    const alert = assertCommunityEditionLimitMessageShowing(PROGRESS.LABEL);
    expect(alert).toBeVisible();

    assertDismissButtonNotShown(alert);
  });

  it('renders the upgrade alert for upgrade error', async () => {
    renderComponent('nexusUpgradeError', "Failed, retry");

    const alertMessage = 'Failed, retry ' + ERROR.TEXT;
    const alert = assertCommunityEditionLimitMessageShowing(alertMessage, ERROR.LABEL);

    assertHasContactSupportLink(alert)
    await assertCanDismissTheBanner(alert);
  });

  it('renders the upgrade alert for completed upgrade', async () => {
    renderComponent('nexusUpgradeComplete');

    const alert = assertCommunityEditionLimitMessageShowing(COMPLETE.TEXT, COMPLETE.LABEL);
    await assertCanDismissTheBanner(alert);
    expect(axios.delete).toHaveBeenCalledWith('service/rest/v1/clustered/upgrade-database-schema');
  });

  it('hides upgrade alert when recovery mode is enabled', () => {
    renderComponent('needsUpgrade', null, true);

    expect(screen.queryByText(UpgradeAlertStrings.UPGRADE_ALERT.PENDING.TEXT)).not.toBeInTheDocument();
  });

  it('shows upgrade alert when recovery mode is disabled', async () => {
    renderComponent('needsUpgrade', null, false);

    const expectedMessage = UpgradeAlertStrings.UPGRADE_ALERT.PENDING.TEXT;
    const expectedTitle = UpgradeAlertStrings.UPGRADE_ALERT.PENDING.LABEL;
    assertCommunityEditionLimitMessageShowing(expectedMessage, expectedTitle);
  });

  function renderComponent(currentState, message, recoveryModeEnabled) {
    return renderView(null, null, currentState, message, recoveryModeEnabled);
  }

  function assertDismissButtonNotShown(alert) {
    expect(within(alert).queryByRole('button', { name: 'Close' })).not.toBeInTheDocument();
  }

  function assertHasContactSupportLink(alert) {
    assertHasLinkWithText(
        alert,
        ERROR.CONTACT_SUPPORT.TEXT,
        ERROR.CONTACT_SUPPORT.HREF
    );
  }
});
