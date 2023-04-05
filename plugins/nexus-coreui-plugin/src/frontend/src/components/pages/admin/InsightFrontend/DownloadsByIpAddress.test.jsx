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
import {render, screen, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';

import DownloadsByIpAddress from './DownloadsByIpAddress';

const NUM_HEADERS = 2;
const ID = 0;
const COUNT = 1;

const selectors = {
  downloadCountHeader: () => screen.getByText('Downloads', {selector: 'thead *'}).closest('th'),
  filter: () => screen.getByRole('textbox'),
  ipAddress: (row) => within(selectors.bodyRows()[row]).getAllByRole('cell')[ID],
  downloadCount: (row) => within(selectors.bodyRows()[row]).getAllByRole('cell')[COUNT],
  bodyRows: () => screen.getAllByRole('row').slice(NUM_HEADERS)
}

describe('DownloadsByIpAddress', function() {
  const downloadsByIpAddress = [{
    identifier: '192.168.0.1',
    downloadCount: 1
  }, {
    identifier: '10.0.0.10',
    downloadCount: 22
  }, {
    identifier: '1.1.1.1',
    downloadCount: 11
  }];

  it('sorts by download count descending by default and ascending after clicking the header', async function() {
    render(<DownloadsByIpAddress downloadsByIpAddress={downloadsByIpAddress}/>);

    await waitFor(() => expect(selectors.downloadCountHeader()).toHaveAttribute('aria-sort', 'descending'));

    expect(selectors.ipAddress(0)).toHaveTextContent('10.0.0.10');
    expect(selectors.downloadCount(0)).toHaveTextContent('22');

    expect(selectors.ipAddress(1)).toHaveTextContent('1.1.1.1');
    expect(selectors.downloadCount(1)).toHaveTextContent('11');

    expect(selectors.ipAddress(2)).toHaveTextContent('192.168.0.1');
    expect(selectors.downloadCount(2)).toHaveTextContent('1');

    userEvent.click(selectors.downloadCountHeader());

    await waitFor(() => expect(selectors.downloadCountHeader()).toHaveAttribute('aria-sort', 'ascending'));

    expect(selectors.ipAddress(0)).toHaveTextContent('192.168.0.1');
    expect(selectors.downloadCount(0)).toHaveTextContent('1');

    expect(selectors.ipAddress(1)).toHaveTextContent('1.1.1.1');
    expect(selectors.downloadCount(1)).toHaveTextContent('11');

    expect(selectors.ipAddress(2)).toHaveTextContent('10.0.0.10');
    expect(selectors.downloadCount(2)).toHaveTextContent('22');
  });

  it('filters by ip address', async function() {
    render(<DownloadsByIpAddress downloadsByIpAddress={downloadsByIpAddress}/>);

    await waitFor(() => expect(selectors.bodyRows().length).toBe(3));

    await TestUtils.changeField(selectors.filter, '10');

    expect(selectors.bodyRows().length).toBe(1);

    expect(selectors.ipAddress(0)).toHaveTextContent('10.0.0.10');
    expect(selectors.downloadCount(0)).toHaveTextContent('22');
  });
});