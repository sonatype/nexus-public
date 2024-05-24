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
import {render, screen} from '@testing-library/react';
import {when} from 'jest-when';
import {ExtJS} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../constants/UIStrings';
import UpgradeAlert from './UpgradeAlert';
import {UpgradeAlertFunctions} from './UpgradeAlertHelper';

const {UPGRADE_ALERT: {PENDING, PROGRESS, ERROR, COMPLETE}} = UIStrings;

jest.mock('axios', () => ({
  ...jest.requireActual('axios'),
  get: jest.fn()
}));

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  ExtJS: {
    state: jest.fn().mockReturnValue({
      getValue: jest.fn(),
    }),
    useState: jest.fn(),
    usePermission: jest.fn(),
  },
}));

const selectors = {
  getAlert: () => screen.getByRole('alert'),
  getCompleteUpgradeButton: () => screen.getByRole('button', {name: 'Finalize Upgrade'}),
  getCloseButton: () => screen.getByRole('button', {name: 'Dismiss'})
};

describe('Upgrade Alert', () => {
  async function renderView(currentState, message, onClose = null) {
    when(ExtJS.state().getValue)
        .calledWith('dbUpgrade')
        .mockReturnValue({currentState: currentState, message: message});

    when(ExtJS.useState)
        .calledWith(UpgradeAlertFunctions.hasUser)
        .mockReturnValue('true');
        
    when(ExtJS.useState)
        .calledWith(UpgradeAlertFunctions.currentState)
        .mockReturnValue(currentState);

    when(ExtJS.useState)
        .calledWith(UpgradeAlertFunctions.message)
        .mockReturnValue(message);

    when(ExtJS.usePermission)
        .calledWith(UpgradeAlertFunctions.checkPermissions)
        .mockReturnValue(true);

    return render(<UpgradeAlert onClose={onClose}/>);
  };

  it('renders the upgrade alert for pending upgrade', async () => {
    const {getCompleteUpgradeButton} = selectors;
    const {container} = await renderView('needsUpgrade');

    const alert = container.querySelector('.nx-alert');
    const alertMessage = PENDING.LABEL + ' ' + PENDING.TEXT;

    expect(alert).toBeInTheDocument();
    expect(alert).toHaveTextContent(alertMessage);
    expect(getCompleteUpgradeButton()).toBeInTheDocument();
  });

  it('renders the upgrade alert for node version mismatch', async () => {
    const {getAlert} = selectors;
    await renderView('versionMismatch');

    const alertMessage = ERROR.LABEL + ' (1) ' + ERROR.TEXT_MISMATCH;

    expect(getAlert()).toBeInTheDocument();
    expect(getAlert()).toHaveTextContent(alertMessage);
  });

  it('renders the upgrade alert for upgrade in progress', async () => {
    const {container} = await renderView('nexusUpgradeInProgress');
    const alert = container.querySelector('.nx-alert');
    const alertMessage = PROGRESS.LABEL;

    expect(alert).toBeInTheDocument();
    expect(alert).toHaveTextContent(alertMessage);
  });

  it('renders the upgrade alert for upgrade error', async () => {
    const {getAlert} = selectors;
    await renderView('nexusUpgradeError', "Failed, retry");

    const alertMessage = ERROR.LABEL + ' [Failed, retry] ' + ERROR.TEXT;

    expect(getAlert()).toBeInTheDocument();
    expect(getAlert()).toHaveTextContent(alertMessage);
  });

  it('renders the upgrade alert for completed upgrade', async () => {
    const {getCloseButton} = selectors;
    const onClose = jest.fn();

    const {container} = await renderView('nexusUpgradeComplete', onClose);

    const alert = container.querySelector('.nx-alert');
    const alertMessage = COMPLETE.LABEL + ' ' + COMPLETE.TEXT;

    expect(alert).toBeInTheDocument();
    expect(alert).toHaveTextContent(alertMessage);
    expect(getCloseButton()).toBeInTheDocument();
  });
});
