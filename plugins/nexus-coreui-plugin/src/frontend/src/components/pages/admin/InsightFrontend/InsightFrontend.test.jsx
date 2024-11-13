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
import Axios from 'axios';
import {render, screen, waitForElementToBeRemoved} from '@testing-library/react';
import {when} from 'jest-when';

import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';

import InsightFrontend from './InsightFrontend';
import UIStrings from '../../../../constants/UIStrings';

jest.mock('axios', () => ({
  get: jest.fn()
}));

global.NX = {
  Permissions: {
    check: jest.fn(() => true)
  },
  State: {
    getEdition: jest.fn(() => 'PRO')
  }
}

const selectors = {
  ...TestUtils.selectors
}

describe('Render Log4 Visualizer page', function() {
  it('renders empty page', async function() {
    when(Axios.get).calledWith('service/rest/v1/vulnerability/count-by-repository-name').mockResolvedValue({
      data: []
    });
    when(Axios.get).calledWith('service/rest/v1/vulnerability/count-by-username').mockResolvedValue({
      data: []
    });
    when(Axios.get).calledWith('service/rest/v1/vulnerability/count-by-day').mockResolvedValue({
      data: []
    });
    when(Axios.get).calledWith('service/rest/v1/vulnerability/count-by-day-non-vulnerable').mockResolvedValue({
      data: []
    });
    when(Axios.get).calledWith('service/rest/v1/vulnerability/count-total').mockResolvedValue({
      data: []
    });

    render(<InsightFrontend/>);
    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    expect(screen.getByText(UIStrings.LOG4J_VISUALIZER.MENU.text)).toBeInTheDocument();
    expect(screen.getByText("Organization Insights")).toBeInTheDocument();
  });

  it('renders the resolved data', async function() {
    when(Axios.get).calledWith('service/rest/v1/vulnerability/count-by-repository-name').mockResolvedValue({
      data: []
    });
    when(Axios.get).calledWith('service/rest/v1/vulnerability/count-by-username').mockResolvedValue({
      data: [ {
        "identifier" : "admin",
        "downloadCount" : 28
      }, {
        "identifier" : "anonymous",
        "downloadCount" : 27
      } ]
    });
    when(Axios.get).calledWith('service/rest/v1/vulnerability/count-by-day').mockResolvedValue({
      data: []
    });
    when(Axios.get).calledWith('service/rest/v1/vulnerability/count-by-day-non-vulnerable').mockResolvedValue({
      data: []
    });
    when(Axios.get).calledWith('service/rest/v1/vulnerability/count-total').mockResolvedValue({
      data: 55
    });

    render(<InsightFrontend/>);
    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    expect(screen.getByText(UIStrings.LOG4J_VISUALIZER.MENU.text)).toBeInTheDocument();
    expect(screen.getByText("admin")).toBeInTheDocument();
    expect(screen.getByText("28")).toBeInTheDocument();
    expect(screen.getByText("anonymous")).toBeInTheDocument();
    expect(screen.getByText("27")).toBeInTheDocument();

    expect(screen.getByText("55")).toBeInTheDocument();
  });
});
