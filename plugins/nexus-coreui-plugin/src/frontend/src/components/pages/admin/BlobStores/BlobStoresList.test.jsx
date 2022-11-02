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
import {waitForElementToBeRemoved} from '@testing-library/react'
import userEvent from '@testing-library/user-event';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';

import BlobStoresList from './BlobStoresList';

jest.mock('axios', () => ({
  get: jest.fn()
}));

describe('BlobStoresList', function() {
  const rows = [{
    name: 'test',
    typeName: 'File',
    unavailable: false,
    unlimited: false,
    blobCount: 66206,
    totalSizeInBytes: 17377843903,
    availableSpaceInBytes: 42040307712
  }, {
    name: 'test2',
    typeName: 'File',
    unavailable: true,
    unlimited: false,
    blobCount: 0,
    totalSizeInBytes: 0,
    availableSpaceInBytes: 0
  }, {
    name: 'test3',
    typeName: 'S3',
    unavailable: false,
    unlimited: true,
    blobCount: 0,
    totalSizeInBytes: 0,
    availableSpaceInBytes: 0
  }];

  function render() {
    return TestUtils.render(<BlobStoresList/>, ({container, getByText, getByPlaceholderText}) => ({
      tableHeader: (text) => getByText(text, {selector: 'thead *'}),
      filter: () => getByPlaceholderText('Filter by name'),
      tableRow: (index) => container.querySelectorAll('tbody tr')[index],
      tableRows: () => container.querySelectorAll('tbody tr')
    }));
  }

  it('renders the loading spinner', async function() {
    axios.get.mockReturnValue(new Promise(() => {}));

    const {loadingMask} = render();

    expect(loadingMask()).toBeInTheDocument();
  });

  it('renders the resolved empty text', async function() {
    axios.get.mockResolvedValue({data: []});

    const {loadingMask, getByText} = render();

    await waitForElementToBeRemoved(loadingMask);

    expect(getByText('There are no blob stores available')).toBeInTheDocument();
  });

  it('renders the rows', async function() {
    axios.get.mockResolvedValue({data: rows});

    const {loadingMask, tableRow} = render();

    await waitForElementToBeRemoved(loadingMask);

    expect(tableRow(0).cells[0]).toHaveTextContent('test');
    expect(tableRow(0).cells[1]).toHaveTextContent('File');
    expect(tableRow(0).cells[2]).toHaveTextContent('Started');
    expect(tableRow(0).cells[3]).toHaveTextContent('66206');
    expect(tableRow(0).cells[4]).toHaveTextContent('16.18 GB');
    expect(tableRow(0).cells[5]).toHaveTextContent('39.15 GB');

    expect(tableRow(1).cells[0]).toHaveTextContent('test2');
    expect(tableRow(1).cells[1]).toHaveTextContent('File');
    expect(tableRow(1).cells[2]).toHaveTextContent('Failed');
    expect(tableRow(1).cells[3]).toHaveTextContent('Unavailable');
    expect(tableRow(1).cells[4]).toHaveTextContent('Unavailable');
    expect(tableRow(1).cells[5]).toHaveTextContent('Unavailable');

    expect(tableRow(2).cells[0]).toHaveTextContent('test3');
    expect(tableRow(2).cells[1]).toHaveTextContent('S3');
    expect(tableRow(2).cells[2]).toHaveTextContent('Started');
    expect(tableRow(2).cells[3]).toHaveTextContent('0');
    expect(tableRow(2).cells[4]).toHaveTextContent('0.00 Bytes');
    expect(tableRow(2).cells[5]).toHaveTextContent('Unlimited');
  });

  it('sorts the rows by name', async function () {
    axios.get.mockResolvedValue({data: rows});

    const {loadingMask, tableHeader, tableRow} = render();

    await waitForElementToBeRemoved(loadingMask);

    expect(tableRow(0).cells[0]).toHaveTextContent('test');
    expect(tableRow(1).cells[0]).toHaveTextContent('test2');
    expect(tableRow(2).cells[0]).toHaveTextContent('test3');

    userEvent.click(tableHeader('Name'));

    expect(tableRow(0).cells[0]).toHaveTextContent('test3');
    expect(tableRow(1).cells[0]).toHaveTextContent('test2');
    expect(tableRow(2).cells[0]).toHaveTextContent('test');
  });

  it('sorts the rows by type', async function () {
    axios.get.mockResolvedValue({data: rows});

    const {loadingMask, tableHeader, tableRow} = render();

    await waitForElementToBeRemoved(loadingMask);

    userEvent.click(tableHeader('Type'));

    expect(tableRow(0).cells[1]).toHaveTextContent('File');
    expect(tableRow(1).cells[1]).toHaveTextContent('File');
    expect(tableRow(2).cells[1]).toHaveTextContent('S3');

    userEvent.click(tableHeader('Type'));

    expect(tableRow(0).cells[1]).toHaveTextContent('S3');
    expect(tableRow(1).cells[1]).toHaveTextContent('File');
    expect(tableRow(2).cells[1]).toHaveTextContent('File');
  });

  it('sorts the rows by state', async function () {
    axios.get.mockResolvedValue({data: rows});

    const {loadingMask, tableHeader, tableRow} = render();

    await waitForElementToBeRemoved(loadingMask);

    userEvent.click(tableHeader('State'));

    expect(tableRow(0).cells[2]).toHaveTextContent('Failed');
    expect(tableRow(1).cells[2]).toHaveTextContent('Started');
    expect(tableRow(2).cells[2]).toHaveTextContent('Started');

    userEvent.click(tableHeader('State'));

    expect(tableRow(0).cells[2]).toHaveTextContent('Started');
    expect(tableRow(1).cells[2]).toHaveTextContent('Started');
    expect(tableRow(2).cells[2]).toHaveTextContent('Failed');
  });

  it('sorts the rows by blob count', async function () {
    axios.get.mockResolvedValue({data: rows});

    const {loadingMask, tableHeader, tableRow} = render();

    await waitForElementToBeRemoved(loadingMask);

    userEvent.click(tableHeader('Blob Count'));

    expect(tableRow(0).cells[3]).toHaveTextContent('Unavailable');
    expect(tableRow(1).cells[3]).toHaveTextContent('0');
    expect(tableRow(2).cells[3]).toHaveTextContent('66206');

    userEvent.click(tableHeader('Blob Count'));

    expect(tableRow(0).cells[3]).toHaveTextContent('66206');
    expect(tableRow(1).cells[3]).toHaveTextContent('0');
    expect(tableRow(2).cells[3]).toHaveTextContent('Unavailable');
  });

  it('sorts the rows by total size', async function () {
    axios.get.mockResolvedValue({data: rows});

    const {container, loadingMask, tableHeader} = render();

    await waitForElementToBeRemoved(loadingMask);

    userEvent.click(tableHeader('Total Size'));

    expect(container.querySelector('tbody tr:nth-child(1) td:nth-child(5)')).toHaveTextContent('Unavailable');
    expect(container.querySelector('tbody tr:nth-child(2) td:nth-child(5)')).toHaveTextContent('0.00 Bytes');
    expect(container.querySelector('tbody tr:nth-child(3) td:nth-child(5)')).toHaveTextContent('16.18 GB');

    userEvent.click(tableHeader('Total Size'));

    expect(container.querySelector('tbody tr:nth-child(1) td:nth-child(5)')).toHaveTextContent('16.18 GB');
    expect(container.querySelector('tbody tr:nth-child(2) td:nth-child(5)')).toHaveTextContent('0.00 Bytes');
    expect(container.querySelector('tbody tr:nth-child(3) td:nth-child(5)')).toHaveTextContent('Unavailable');
  });

  it('sorts the rows by available space', async function () {
    axios.get.mockResolvedValue({data: rows});

    const {container, loadingMask, tableHeader} = render();

    await waitForElementToBeRemoved(loadingMask);

    userEvent.click(tableHeader('Available Space'));

    expect(container.querySelector('tbody tr:nth-child(1) td:nth-child(6)')).toHaveTextContent('Unavailable');
    expect(container.querySelector('tbody tr:nth-child(2) td:nth-child(6)')).toHaveTextContent('39.15 GB');
    expect(container.querySelector('tbody tr:nth-child(3) td:nth-child(6)')).toHaveTextContent('Unlimited');

    userEvent.click(tableHeader('Available Space'));

    expect(container.querySelector('tbody tr:nth-child(1) td:nth-child(6)')).toHaveTextContent('Unlimited');
    expect(container.querySelector('tbody tr:nth-child(2) td:nth-child(6)')).toHaveTextContent('39.15 GB');
    expect(container.querySelector('tbody tr:nth-child(3) td:nth-child(6)')).toHaveTextContent('Unavailable');
  });

  it('filters by name', async function() {
    axios.get.mockResolvedValue({data: rows});

    const {filter, loadingMask, tableRow, tableRows} = render();

    await waitForElementToBeRemoved(loadingMask);

    await TestUtils.changeField(filter, '2');

    expect(tableRows().length).toBe(1);
    expect(tableRow(0).cells[0]).toHaveTextContent('test2');
  });
});
