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
import {fireEvent, waitForElementToBeRemoved} from '@testing-library/react';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';
import axios from 'axios';

import LoggingConfigurationList from './LoggingConfigurationList';

import UIStrings from '../../../../constants/UIStrings';

jest.mock('axios', () => ({
  ...jest.requireActual('axios'), // Use most functions from actual axios
  get: jest.fn()
}));

describe('LoggingConfigurationList', function() {
  function renderView(view = <LoggingConfigurationList/>) {
    return TestUtils.render(view, ({queryByPlaceholderText}) => ({
      filter: () => queryByPlaceholderText(UIStrings.LOGGING.FILTER_PLACEHOLDER)
    }));
  }

  it('renders the resolved data', async function() {
    const rows = [
      {name: 'ROOT', level: 'INFO'},
      {name: 'org.sonatype.nexus', level: 'DEBUG'}
    ];

    axios.get.mockReturnValue(Promise.resolve({
      data: rows
    }));

    const {container, loadingMask} = renderView();

    await waitForElementToBeRemoved(loadingMask);

    rows.forEach((row, i) => {
      expect(container.querySelector(`tbody tr:nth-child(${i+1}) td:nth-child(1)`)).toHaveTextContent(row.name);
      expect(container.querySelector(`tbody tr:nth-child(${i+1}) td:nth-child(2)`)).toHaveTextContent(row.level);
    });
  });

  it('renders an error message', async function() {
    axios.get.mockReturnValue(Promise.reject({message: 'Error'}));

    const {container, loadingMask} = renderView();

    await waitForElementToBeRemoved(loadingMask);

    expect(container.querySelector('.nx-cell--meta-info')).toHaveTextContent('Error');
  });

  it('searches for lower-cased strings in the logger names', async function() {
    axios.get.mockReturnValue(Promise.resolve({
      data: [
        {name: 'ROOT', level: 'INFO'},
        {name: 'org.sonatype.nexus', level: 'DEBUG'}
      ]
    }));

    const {container, loadingMask, filter} = renderView();

    await waitForElementToBeRemoved(loadingMask);

    fireEvent.change(filter(), {target: {value: 'sonatype'}});

    expect(container.querySelector('tbody tr:nth-child(1) td:nth-child(1)')).toHaveTextContent('org.sonatype.nexus');
  });
});
