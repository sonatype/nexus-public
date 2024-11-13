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
import {waitForElementToBeRemoved} from '@testing-library/react';
import {when} from 'jest-when';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';

import axios from 'axios';

import ReplicationList from './ReplicationList';

jest.mock('axios', () => ({
  get: jest.fn()
}));

describe('ReplicationList', function() {
  function renderView(view = <ReplicationList/>) {
    return TestUtils.render(view, ({container}) => ({
      cell: (row, column) => container.querySelector(`.nx-clickable:nth-child(${row}) td:nth-child(${column})`)
    }));
  }

  it('renders the resolved data', async function() {
    const rows = [
      {
        name: 'test1',
        sourceRepositoryName: 'repo1',
        destinationInstanceUrl: 'http://108.838.8',
        destinationRepositoryName: 'cleanup-description'
      }, {
        name: 'test2',
        sourceRepositoryName: 'repo2',
        destinationInstanceUrl: 'http://www.amazon.com',
        destinationRepositoryName: 'cleanup-description'
      }
    ];

    when(axios.get).calledWith('service/rest/beta/replication/connection').mockResolvedValue({data: rows});

    const {loadingMask, cell} = renderView();

    await waitForElementToBeRemoved(loadingMask);

    rows.forEach((row, i) => {
      const rowNum = i + 1;
      expect(cell(rowNum, 1)).toHaveTextContent(row.name);
      expect(cell(rowNum, 2)).toHaveTextContent(row.sourceRepositoryName);
      expect(cell(rowNum, 3)).toHaveTextContent(row.destinationInstanceUrl);
      expect(cell(rowNum, 4)).toHaveTextContent(row.destinationRepositoryName);
    });
  });

  it('renders a loading spinner', async function() {
    when(axios.get).calledWith('service/rest/beta/replication/connection').mockReturnValue(TestUtils.UNRESOLVED);

    const {loadingMask} = renderView();

    expect(loadingMask()).toBeInTheDocument();
  });

  it('renders an error message', async function() {
    axios.get.mockReturnValue(Promise.reject({message: 'Error'}));

    const {container, loadingMask} = renderView();

    await waitForElementToBeRemoved(loadingMask);

    expect(container.querySelector('.nx-cell--meta-info')).toHaveTextContent('Error');
  });
});
