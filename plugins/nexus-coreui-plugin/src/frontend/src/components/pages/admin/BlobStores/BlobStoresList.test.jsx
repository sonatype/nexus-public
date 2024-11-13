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
import {waitForElementToBeRemoved, within, screen, render} from '@testing-library/react'

import userEvent from '@testing-library/user-event';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';

import BlobStoresList from './BlobStoresList';

jest.mock('axios', () => ({
  get: jest.fn()
}));

describe('BlobStoresList', function() {
  const rows = [{
    name: 'test',
    path: 'default',
    typeName: 'File',
    unavailable: false,
    unlimited: false,
    blobCount: 66206,
    totalSizeInBytes: 17377843903,
    availableSpaceInBytes: 42040307712
  }, {
    name: 'test2',
    path: 'default',
    typeName: 'File',
    unavailable: true,
    unlimited: false,
    blobCount: 0,
    totalSizeInBytes: 0,
    availableSpaceInBytes: 0
  }, {
    name: 'test3',
    typeName: 'S3',
    path: 'bucket',
    unavailable: false,
    unlimited: true,
    blobCount: 0,
    totalSizeInBytes: 0,
    availableSpaceInBytes: 0
  }];

  const NUM_HEADERS = 1;
  const NAME = 0;
  const PATH = 1;
  const TYPE = 2;
  const STATE = 3;
  const BLOB_COUNT = 4;
  const TOTAL_SIZE = 5;
  const AVAILABLE_SPACE = 6;

  const selectors = {
    ...TestUtils.selectors,
    bodyRows: () => screen.getAllByRole('row').slice(NUM_HEADERS),
    tableHeader: (text) => screen.getByText(text, {selector: 'thead *'}),
    blobStoreName: (row) => within(selectors.bodyRows()[row]).getAllByRole('cell')[NAME],
    blobStorePath: (row) => within(selectors.bodyRows()[row]).getAllByRole('cell')[PATH],
    blobStoreType: (row) => within(selectors.bodyRows()[row]).getAllByRole('cell')[TYPE],
    blobStoreState: (row) => within(selectors.bodyRows()[row]).getAllByRole('cell')[STATE],
    blobStoreBlobCount: (row) => within(selectors.bodyRows()[row]).getAllByRole('cell')[BLOB_COUNT],
    blobStoreTotalSize: (row) => within(selectors.bodyRows()[row]).getAllByRole('cell')[TOTAL_SIZE],
    blobStoreAvailableSpace: (row) => within(selectors.bodyRows()[row]).getAllByRole('cell')[AVAILABLE_SPACE],
    filter: () => screen.getByPlaceholderText('Filter by name')
  }

  it('renders the loading spinner', async function() {
    axios.get.mockReturnValue(new Promise(() => {}));

    render(<BlobStoresList/>);

    expect(selectors.queryLoadingMask()).toBeInTheDocument();
  });

  it('renders the resolved empty text', async function() {
    axios.get.mockResolvedValue({data: []});

    render(<BlobStoresList/>);

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    expect(screen.getByText('There are no blob stores available')).toBeInTheDocument();
  });

  it('renders the rows', async function() {
    const {
      blobStoreName,
      blobStorePath,
      blobStoreType,
      blobStoreState,
      blobStoreBlobCount,
      blobStoreTotalSize,
      blobStoreAvailableSpace,
      queryLoadingMask
    } = selectors;

    axios.get.mockResolvedValue({data: rows});

    render(<BlobStoresList/>);

    await waitForElementToBeRemoved(queryLoadingMask());

    expect(blobStoreName(0)).toHaveTextContent('test');
    expect(blobStorePath(0)).toHaveTextContent('default');
    expect(blobStoreType(0)).toHaveTextContent('File');
    expect(blobStoreState(0)).toHaveTextContent('Started');
    expect(blobStoreBlobCount(0)).toHaveTextContent('66206');
    expect(blobStoreTotalSize(0)).toHaveTextContent('16.18 GB');
    expect(blobStoreAvailableSpace(0)).toHaveTextContent('39.15 GB');

    expect(blobStoreName(1)).toHaveTextContent('test2');
    expect(blobStorePath(1)).toHaveTextContent('default');
    expect(blobStoreType(1)).toHaveTextContent('File');
    expect(blobStoreState(1)).toHaveTextContent('Failed');
    expect(blobStoreBlobCount(1)).toHaveTextContent('Unavailable');
    expect(blobStoreTotalSize(1)).toHaveTextContent('Unavailable');
    expect(blobStoreAvailableSpace(1)).toHaveTextContent('Unavailable');

    expect(blobStoreName(2)).toHaveTextContent('test3');
    expect(blobStorePath(2)).toHaveTextContent('bucket');
    expect(blobStoreType(2)).toHaveTextContent('S3');
    expect(blobStoreState(2)).toHaveTextContent('Started');
    expect(blobStoreBlobCount(2)).toHaveTextContent('0');
    expect(blobStoreTotalSize(2)).toHaveTextContent('0.00 Bytes');
    expect(blobStoreAvailableSpace(2)).toHaveTextContent('Unlimited');
  });

  it('sorts the rows by name', async function () {
    const {blobStoreName, tableHeader, queryLoadingMask} = selectors;

    axios.get.mockResolvedValue({data: rows});

    render(<BlobStoresList/>);

    await waitForElementToBeRemoved(queryLoadingMask());

    expect(blobStoreName(0)).toHaveTextContent('test');
    expect(blobStoreName(1)).toHaveTextContent('test2');
    expect(blobStoreName(2)).toHaveTextContent('test3');

    userEvent.click(tableHeader('Name'));

    expect(blobStoreName(0)).toHaveTextContent('test3');
    expect(blobStoreName(1)).toHaveTextContent('test2');
    expect(blobStoreName(2)).toHaveTextContent('test');
  });

  it('sorts the rows by path', async function () {
    const {blobStorePath, tableHeader, queryLoadingMask} = selectors;

    axios.get.mockResolvedValue({data: rows});

    render(<BlobStoresList/>);

    await waitForElementToBeRemoved(queryLoadingMask());

    expect(blobStorePath(0)).toHaveTextContent('default');
    expect(blobStorePath(1)).toHaveTextContent('default');
    expect(blobStorePath(2)).toHaveTextContent('bucket');

    userEvent.click(tableHeader('Path'));

    expect(blobStorePath(0)).toHaveTextContent('bucket');
    expect(blobStorePath(1)).toHaveTextContent('default');
    expect(blobStorePath(2)).toHaveTextContent('default');
  });

  it('sorts the rows by type', async function () {
    const {blobStoreType, tableHeader, queryLoadingMask} = selectors;

    axios.get.mockResolvedValue({data: rows});

    render(<BlobStoresList/>);

    await waitForElementToBeRemoved(queryLoadingMask());

    userEvent.click(tableHeader('Type'));

    expect(blobStoreType(0)).toHaveTextContent('File');
    expect(blobStoreType(1)).toHaveTextContent('File');
    expect(blobStoreType(2)).toHaveTextContent('S3');

    userEvent.click(tableHeader('Type'));

    expect(blobStoreType(0)).toHaveTextContent('S3');
    expect(blobStoreType(1)).toHaveTextContent('File');
    expect(blobStoreType(2)).toHaveTextContent('File');
  });

  it('sorts the rows by state', async function () {
    const {blobStoreState, tableHeader, queryLoadingMask} = selectors;

    axios.get.mockResolvedValue({data: rows});

    render(<BlobStoresList/>);

    await waitForElementToBeRemoved(queryLoadingMask());

    userEvent.click(tableHeader('State'));

    expect(blobStoreState(0)).toHaveTextContent('Failed');
    expect(blobStoreState(1)).toHaveTextContent('Started');
    expect(blobStoreState(2)).toHaveTextContent('Started');

    userEvent.click(tableHeader('State'));

    expect(blobStoreState(0)).toHaveTextContent('Started');
    expect(blobStoreState(1)).toHaveTextContent('Started');
    expect(blobStoreState(2)).toHaveTextContent('Failed');
  });

  it('sorts the rows by blob count', async function () {
    const {blobStoreBlobCount, tableHeader, queryLoadingMask} = selectors;

    axios.get.mockResolvedValue({data: rows});

    render(<BlobStoresList/>);

    await waitForElementToBeRemoved(queryLoadingMask());

    userEvent.click(tableHeader('Blob Count'));

    expect(blobStoreBlobCount(0)).toHaveTextContent('Unavailable');
    expect(blobStoreBlobCount(1)).toHaveTextContent('0');
    expect(blobStoreBlobCount(2)).toHaveTextContent('66206');

    userEvent.click(tableHeader('Blob Count'));

    expect(blobStoreBlobCount(0)).toHaveTextContent('66206');
    expect(blobStoreBlobCount(1)).toHaveTextContent('0');
    expect(blobStoreBlobCount(2)).toHaveTextContent('Unavailable');
  });

  it('sorts the rows by total size', async function () {
    const {blobStoreTotalSize, tableHeader, queryLoadingMask} = selectors;

    axios.get.mockResolvedValue({data: rows});

    render(<BlobStoresList/>);

    await waitForElementToBeRemoved(queryLoadingMask());

    userEvent.click(tableHeader('Total Size'));

    expect(blobStoreTotalSize(0)).toHaveTextContent('Unavailable');
    expect(blobStoreTotalSize(1)).toHaveTextContent('0.00 Bytes');
    expect(blobStoreTotalSize(2)).toHaveTextContent('16.18 GB');

    userEvent.click(tableHeader('Total Size'));

    expect(blobStoreTotalSize(0)).toHaveTextContent('16.18 GB');
    expect(blobStoreTotalSize(1)).toHaveTextContent('0.00 Bytes');
    expect(blobStoreTotalSize(2)).toHaveTextContent('Unavailable');
  });

  it('sorts the rows by available space', async function () {
    const {blobStoreAvailableSpace, tableHeader, queryLoadingMask} = selectors;

    axios.get.mockResolvedValue({data: rows});

    render(<BlobStoresList/>);

    await waitForElementToBeRemoved(queryLoadingMask());

    userEvent.click(tableHeader('Available Space'));

    expect(blobStoreAvailableSpace(0)).toHaveTextContent('Unavailable');
    expect(blobStoreAvailableSpace(1)).toHaveTextContent('39.15 GB');
    expect(blobStoreAvailableSpace(2)).toHaveTextContent('Unlimited');

    userEvent.click(tableHeader('Available Space'));

    expect(blobStoreAvailableSpace(0)).toHaveTextContent('Unlimited');
    expect(blobStoreAvailableSpace(1)).toHaveTextContent('39.15 GB');
    expect(blobStoreAvailableSpace(2)).toHaveTextContent('Unavailable');
  });

  it('filters by name', async function() {
    const {blobStoreName, filter, queryLoadingMask} = selectors;

    axios.get.mockResolvedValue({data: rows});

    render(<BlobStoresList/>);

    await waitForElementToBeRemoved(queryLoadingMask());

    await TestUtils.changeField(filter, '2');

    expect(selectors.bodyRows().length).toBe(1);
    expect(blobStoreName(0)).toHaveTextContent('test2');
  });
});
