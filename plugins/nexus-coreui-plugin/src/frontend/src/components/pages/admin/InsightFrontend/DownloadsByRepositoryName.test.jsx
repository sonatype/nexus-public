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

import DownloadsByRepositoryName from './DownloadsByRepositoryName';

const NUM_HEADERS = 2;
const ID = 0;
const COUNT = 1;

const selectors = {
  downloadCountHeader: () => screen.getByText('Downloads', {selector: 'thead *'}).closest('th'),
  repositoryHeader: () => screen.getByText('Repository', {selector: 'thead *'}).closest('th'),
  filter: () => screen.getByRole('textbox'),
  repositoryName: (row) => within(selectors.bodyRows()[row]).getAllByRole('cell')[ID],
  downloadCount: (row) => within(selectors.bodyRows()[row]).getAllByRole('cell')[COUNT],
  bodyRows: () => screen.getAllByRole('row').slice(NUM_HEADERS)
}

describe('DownloadsByRepositoryName', function() {
  const downloadsByRepositoryName = [{
    identifier: 'ca',
    downloadCount: 1
  }, {
    identifier: 'ab',
    downloadCount: 22
  }, {
    identifier: 'bc',
    downloadCount: 11
  }];

  it('sorts by download count descending by default and ascending after clicking the header', async function() {
    render(<DownloadsByRepositoryName downloadsByRepositoryName={downloadsByRepositoryName}/>);

    await waitFor(() => expect(selectors.downloadCountHeader()).toHaveAttribute('aria-sort', 'descending'));

    expect(selectors.repositoryName(0)).toHaveTextContent('ab');
    expect(selectors.downloadCount(0)).toHaveTextContent('22');

    expect(selectors.repositoryName(1)).toHaveTextContent('bc');
    expect(selectors.downloadCount(1)).toHaveTextContent('11');

    expect(selectors.repositoryName(2)).toHaveTextContent('ca');
    expect(selectors.downloadCount(2)).toHaveTextContent('1');

    userEvent.click(selectors.downloadCountHeader());

    await waitFor(() => expect(selectors.downloadCountHeader()).toHaveAttribute('aria-sort', 'ascending'));

    expect(selectors.repositoryName(0)).toHaveTextContent('ca');
    expect(selectors.downloadCount(0)).toHaveTextContent('1');

    expect(selectors.repositoryName(1)).toHaveTextContent('bc');
    expect(selectors.downloadCount(1)).toHaveTextContent('11');

    expect(selectors.repositoryName(2)).toHaveTextContent('ab');
    expect(selectors.downloadCount(2)).toHaveTextContent('22');
  });

  it('sorts repository name ascending, then descending', async function() {
    render(<DownloadsByRepositoryName downloadsByRepositoryName={downloadsByRepositoryName}/>);

    userEvent.click(selectors.repositoryHeader());

    await waitFor(() => expect(selectors.repositoryHeader()).toHaveAttribute('aria-sort', 'ascending'));

    expect(selectors.repositoryName(0)).toHaveTextContent('ab');
    expect(selectors.downloadCount(0)).toHaveTextContent('22');

    expect(selectors.repositoryName(1)).toHaveTextContent('bc');
    expect(selectors.downloadCount(1)).toHaveTextContent('11');

    expect(selectors.repositoryName(2)).toHaveTextContent('ca');
    expect(selectors.downloadCount(2)).toHaveTextContent('1');

    userEvent.click(selectors.repositoryHeader());

    await waitFor(() => expect(selectors.repositoryHeader()).toHaveAttribute('aria-sort', 'descending'));

    expect(selectors.repositoryName(0)).toHaveTextContent('ca');
    expect(selectors.downloadCount(0)).toHaveTextContent('1');

    expect(selectors.repositoryName(1)).toHaveTextContent('bc');
    expect(selectors.downloadCount(1)).toHaveTextContent('11');

    expect(selectors.repositoryName(2)).toHaveTextContent('ab');
    expect(selectors.downloadCount(2)).toHaveTextContent('22');
  });

  it('filters by repository name', async function() {
    render(<DownloadsByRepositoryName downloadsByRepositoryName={downloadsByRepositoryName}/>);

    await waitFor(() => expect(selectors.bodyRows().length).toBe(3));

    await TestUtils.changeField(selectors.filter, 'b');

    expect(selectors.bodyRows().length).toBe(2);

    expect(selectors.repositoryName(0)).toHaveTextContent('ab');
    expect(selectors.downloadCount(0)).toHaveTextContent('22');

    expect(selectors.repositoryName(1)).toHaveTextContent('bc');
    expect(selectors.downloadCount(1)).toHaveTextContent('11');
  });
});