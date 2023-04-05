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
import {render, screen, waitForElementToBeRemoved, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ExtJS, APIConstants} from '@sonatype/nexus-ui-plugin';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';
import {when} from 'jest-when';
import Axios from 'axios';
import NodeList from './NodeList';
import UIStrings from '../../../../constants/UIStrings';

const {
  NODES: {READ_ONLY, HELP}
} = UIStrings;

const {
  EXT,
  REST: {
    INTERNAL: {GET_SUPPORT_ZIP_ACTIVE_NODES}
  }
} = APIConstants;

jest.mock('axios', () => ({
  ...jest.requireActual('axios'),
  get: jest.fn(),
  post: jest.fn()
}));

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  ExtJS: {
    showErrorMessage: jest.fn(),
    showSuccessMessage: jest.fn(),
    state: jest.fn().mockReturnValue({
      getValue: jest.fn()
    })
  }
}));

const selectors = {
  ...TestUtils.selectors,
  enableReadOnlyButton: () => screen.getByRole('button', {name: READ_ONLY.ENABLE.BUTTON}),
  disableReadOnlyButton: () => screen.getByRole('button', {name: READ_ONLY.DISABLE.BUTTON}),
  nodeHostname: (hostname) => screen.getByText(hostname),
  helpHeader: () => screen.getByText(HELP.TITLE),
  enableReadOnlyModal: () => screen.getByRole('dialog', {name: READ_ONLY.ENABLE.TITLE}),
  disableReadOnlyModal: () => screen.getByRole('dialog', {name: READ_ONLY.DISABLE.TITLE}),
  enableReadOnlyModalButton: () =>
    within(selectors.enableReadOnlyModal()).getByRole('button', {name: READ_ONLY.ENABLE.BUTTON}),
  disableReadOnlyModalButton: () =>
    within(selectors.disableReadOnlyModal()).getByRole('button', {name: READ_ONLY.DISABLE.BUTTON}),
  disableReadOnlyForciblyModal: () =>
    screen.getByRole('dialog', {name: READ_ONLY.DISABLE.FORCIBLY.TITLE}),
  disableReadOnlyForciblyModalButton: () =>
    within(selectors.disableReadOnlyForciblyModal()).getByRole('button', {
      name: READ_ONLY.DISABLE.FORCIBLY.BUTTON
    }),
  togglingReadOnlyMask: () => screen.queryByRole('status')
};

describe('NodeList', () => {
  const nodes = [
    {
      nodeId: '111',
      hostname: 'cluster-node-1',
      status: 'NOT_CREATED',
      blobRef: null,
      lastUpdated: null
    },
    {
      nodeId: '222',
      hostname: 'cluster-node-2',
      status: 'NOT_CREATED',
      blobRef: null,
      lastUpdated: null
    },
    {
      nodeId: '333',
      hostname: 'cluster-node-3',
      status: 'NOT_CREATED',
      blobRef: null,
      lastUpdated: null
    }
  ];

  const freezeExtReqBody = {
    action: EXT.FREEZE.ACTION,
    method: EXT.FREEZE.METHODS.UPDATE,
    data: [{frozen: true}],
    type: 'rpc',
    tid: 1
  };

  const freezeExtResBody = {
    result: {
      data: {frozen: true},
      success: true
    }
  };

  const unfreezeExtReqBody = {
    action: EXT.FREEZE.ACTION,
    method: EXT.FREEZE.METHODS.UPDATE,
    data: [{frozen: false}],
    type: 'rpc',
    tid: 1
  };

  const unfreezeExtResBody = {
    result: {
      data: {frozen: false},
      success: true
    }
  };

  const unfreezeForciblyExtReqBody = {
    action: EXT.FREEZE.ACTION,
    method: EXT.FREEZE.METHODS.FORCE_RELEASE,
    data: null,
    type: 'rpc',
    tid: 1
  };

  const renderAndWaitForLoad = async () => {
    render(<NodeList />);
    await waitForElementToBeRemoved(selectors.queryLoadingMask());
  };

  const waitForTogglingMaskToBeRemoved = async () => {
    await waitFor(() => expect(selectors.togglingReadOnlyMask()).not.toBeInTheDocument());
  };

  beforeEach(() => {
    when(Axios.get).calledWith(GET_SUPPORT_ZIP_ACTIVE_NODES).mockResolvedValue({data: nodes});
  });

  it('renders node cards and help', async () => {
    await renderAndWaitForLoad();

    const {helpHeader, nodeHostname} = selectors;

    expect(helpHeader()).toBeInTheDocument();

    nodes.forEach(({hostname}) => {
      expect(nodeHostname(hostname)).toBeInTheDocument();
    });
  });

  it('enables/disables read-only mode', async () => {
    when(ExtJS.state().getValue).calledWith('frozen').mockReturnValue(false);
    when(ExtJS.state().getValue).calledWith('frozenManually').mockReturnValue(false);

    await renderAndWaitForLoad();

    const {
      enableReadOnlyButton,
      disableReadOnlyButton,
      enableReadOnlyModalButton,
      disableReadOnlyModalButton
    } = selectors;

    // freeze
    expect(enableReadOnlyButton()).toBeInTheDocument();
    when(Axios.post)
      .calledWith(EXT.URL, freezeExtReqBody)
      .mockResolvedValue({data: freezeExtResBody});
    userEvent.click(enableReadOnlyButton());
    userEvent.click(enableReadOnlyModalButton());
    expect(Axios.post).toBeCalledWith(EXT.URL, freezeExtReqBody);
    await waitForTogglingMaskToBeRemoved();

    // unfreeze
    when(Axios.post)
      .calledWith(EXT.URL, unfreezeExtReqBody)
      .mockResolvedValue({data: unfreezeExtResBody});
    when(ExtJS.state().getValue).calledWith('frozenManually').mockReturnValue(true);
    userEvent.click(disableReadOnlyButton());
    userEvent.click(disableReadOnlyModalButton());
    expect(Axios.post).toBeCalledWith(EXT.URL, unfreezeExtReqBody);
    await waitForTogglingMaskToBeRemoved();
    expect(enableReadOnlyButton()).toBeInTheDocument();
  });

  it('disables read-only mode set by system task', async () => {
    when(ExtJS.state().getValue).calledWith('frozen').mockReturnValue(true);
    when(ExtJS.state().getValue).calledWith('frozenManually').mockReturnValue(false);

    await renderAndWaitForLoad();

    const {enableReadOnlyButton, disableReadOnlyButton, disableReadOnlyForciblyModalButton} =
      selectors;

    expect(disableReadOnlyButton()).toBeInTheDocument();

    when(Axios.post)
      .calledWith(EXT.URL, unfreezeForciblyExtReqBody)
      .mockResolvedValue({data: unfreezeExtResBody});

    userEvent.click(disableReadOnlyButton());
    userEvent.click(disableReadOnlyForciblyModalButton());

    expect(Axios.post).toBeCalledWith(EXT.URL, unfreezeForciblyExtReqBody);

    await waitForTogglingMaskToBeRemoved();

    expect(enableReadOnlyButton()).toBeInTheDocument();
  });
});
