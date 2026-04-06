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
import {render, screen, waitForElementToBeRemoved} from '@testing-library/react';
import {APIConstants} from '@sonatype/nexus-ui-plugin';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';
import {when} from 'jest-when';
import Axios from 'axios';
import NodeList from './NodeList';
import UIStrings from '../../../../constants/UIStrings';

const {
  NODES: {HELP}
} = UIStrings;

const {
  REST: {
    INTERNAL: {GET_SUPPORT_ZIP_ACTIVE_NODES}
  }
} = APIConstants;

jest.mock('axios', () => ({
  ...jest.requireActual('axios'),
  get: jest.fn()
}));

const selectors = {
  ...TestUtils.selectors,
  nodeHostname: (hostname) => screen.getByText(hostname),
  helpHeader: () => screen.getByText(HELP.TITLE)
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

  const renderAndWaitForLoad = async () => {
    render(<NodeList />);
    await waitForElementToBeRemoved(selectors.queryLoadingMask());
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
});
