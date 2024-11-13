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
import axios from 'axios';
import {waitForElementToBeRemoved} from '@testing-library/react';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';

import CleanupPoliciesList from './CleanupPoliciesList';

import UIStrings from '../../../../constants/UIStrings';

jest.mock('axios', () => ({
  get: jest.fn()
}));

describe('CleanupPoliciesList', function() {
  function renderView(view = <CleanupPoliciesList/>) {
    return TestUtils.render(view, ({queryByPlaceholderText}) => ({
      filter: () => queryByPlaceholderText(UIStrings.CLEANUP_POLICIES.FILTER_PLACEHOLDER)
    }));
  }

  it('renders the resolved data', async function() {
    const rows = [
      {
        name: 'cleanup',
        format: 'testformat',
        notes: 'cleanup-description'
      },{
        name: 'test',
        format: 'testformat',
        notes: 'notes'
      }
    ];

    axios.get.mockImplementation((url) => {
      if (url === 'service/rest/internal/cleanup-policies') {
        return Promise.resolve({data: rows});
      }
      else if (url === 'service/rest/internal/cleanup-policies/criteria/formats') {
        return Promise.resolve({
          data: [{
            'id' : 'testformat',
            'name' : 'Test Format',
            'availableCriteria' : ['lastBlobUpdated', 'lastDownloaded', 'isPrerelease', 'regex']
          }]
        });
      }
    });

    const {container, loadingMask} = renderView();

    await waitForElementToBeRemoved(loadingMask());

    rows.forEach((row, i) => {
      expect(container.querySelector(`tbody tr:nth-child(${i+1}) td:nth-child(1)`)).toHaveTextContent(row.name);
      expect(container.querySelector(`tbody tr:nth-child(${i+1}) td:nth-child(2)`)).toHaveTextContent(row.format);
      expect(container.querySelector(`tbody tr:nth-child(${i+1}) td:nth-child(3)`)).toHaveTextContent(row.notes);
    });
  });

  it('renders an error message', async function() {
    axios.get.mockReturnValue(Promise.reject({message: 'Error'}));

    const {container, loadingMask} = renderView();

    await waitForElementToBeRemoved(loadingMask());

    expect(container.querySelector('.nx-cell--meta-info')).toHaveTextContent('Error');
  });
});
