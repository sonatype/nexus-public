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
import {when} from 'jest-when';
import {render, screen, waitForElementToBeRemoved, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {sort, prop, descend, ascend} from 'ramda';
import {APIConstants} from '@sonatype/nexus-ui-plugin';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';

import UploadList from './UploadList';
import{REPOS, FORMATS, FIELDS} from './UploadList.testdata';
import UIStrings from '../../../../constants/UIStrings';

const {COLUMNS} = UIStrings.UPLOAD.LIST;
const {EXT: {URL, UPLOAD, REPOSITORY}} = APIConstants;

jest.mock('axios', () => ({
  ...jest.requireActual('axios'),
  post: jest.fn(),
}));

const selectors = {
  ...TestUtils.selectors,
  ...TestUtils.tableSelectors,
  getEmptyMessage: () => screen.getByText('No repositories found.'),
  getFilterInput: () => screen.getByPlaceholderText('Filter'),
};

const REPOS_REQUEST = expect.objectContaining({
  action: REPOSITORY.ACTION,
  method: REPOSITORY.METHODS.READ_REFERENCES,
});

const FORMATS_REQUEST = expect.objectContaining({
  action: UPLOAD.ACTION,
  method: UPLOAD.METHODS.GET_UPLOAD_DEFINITIONS,
});

describe('UploadList', function() {
  const mockApiResponses = (data) => {
    when(axios.post).calledWith(URL, REPOS_REQUEST).mockResolvedValue({data: TestUtils.makeExtResult(data)});
    when(axios.post).calledWith(URL, FORMATS_REQUEST).mockResolvedValue({data: TestUtils.makeExtResult(FORMATS)});
  };

  async function renderView(data = REPOS) {
    mockApiResponses(data);
    render(<UploadList />);
    await waitForElementToBeRemoved(selectors.queryLoadingMask());
  }

  const sortRepos = (field, order = ascend) => sort(order(prop(field)), REPOS);

  it('renders the resolved empty text', async function() {
    await renderView([])

    expect(selectors.getEmptyMessage()).toBeInTheDocument();
  });

  it('renders the error message', async function() {
    const message = 'Error Message!';
    when(axios.post).calledWith(URL, REPOS_REQUEST).mockRejectedValue({message});

    render(<UploadList />);
    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    const error = selectors.tableAlert();

    expect(error).toBeInTheDocument();
    expect(error).toHaveTextContent('Error');
  });

  it('renders the resolved data', async function() {
    await renderView();

    TestUtils.expectTableHeaders(Object.values(COLUMNS));
    TestUtils.expectTableRows(REPOS, Object.values(FIELDS));
  });

  it('renders copy button in each row with tooltips of "Copy URL to Clipboard" when hovering',
    async function() {
      await renderView();
      const rows = selectors.rows(),
        row1CopyBtn = within(rows[0]).getAllByRole('button')[0],
        row2CopyBtn = within(rows[1]).getAllByRole('button')[0],
        row3CopyBtn = within(rows[2]).getAllByRole('button')[0];

      expect(row1CopyBtn).toBeInTheDocument();
      expect(row2CopyBtn).toBeInTheDocument();
      expect(row3CopyBtn).toBeInTheDocument();
      await TestUtils.expectToSeeTooltipOnHover(row1CopyBtn, 'Copy URL to Clipboard');
      await TestUtils.expectToSeeTooltipOnHover(row2CopyBtn, 'Copy URL to Clipboard');
      await TestUtils.expectToSeeTooltipOnHover(row3CopyBtn, 'Copy URL to Clipboard');
  });

  it('calls onCopyUrl when copy button is clicked', async function () {
    const onCopyUrl = jest.fn((event) => event.stopPropagation());

    mockApiResponses(REPOS);

    render(<UploadList copyUrl={onCopyUrl} />);
    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    const copyBtn = within(selectors.rows()[1]).getAllByRole('button')[0];
    await TestUtils.expectToSeeTooltipOnHover(copyBtn, 'Copy URL to Clipboard');

    userEvent.click(copyBtn);

    expect(onCopyUrl).toBeCalled();
  });

  it('renders a "Filter" text input', async function() {
    await renderView();

    expect(selectors.getFilterInput()).toBeInTheDocument();
  });

  it('filters by the text value when the user types into the filter', async function() {
    await renderView();

    await TestUtils.expectProperFilteredItemsCount(selectors.getFilterInput, '', REPOS.length);
    await TestUtils.expectProperFilteredItemsCount(selectors.getFilterInput, 'maven', 2);
    await TestUtils.expectProperFilteredItemsCount(selectors.getFilterInput, 'hosted', 1);
  });

  it('unfilters when the clear button is pressed', async function() {
    await renderView();

    await TestUtils.expectProperFilteredItemsCount(selectors.getFilterInput, '', REPOS.length);
    await TestUtils.expectProperFilteredItemsCount(selectors.getFilterInput, 'maven', 2);

    const clearBtn = await screen.findByRole('button', {name: 'Clear filter'});

    userEvent.click(clearBtn);
    expect(selectors.rows()).toHaveLength(3);
  });

  it('unfilters when the ESC key is pressed', async function() {
    await renderView();

    await TestUtils.expectProperFilteredItemsCount(selectors.getFilterInput, '', REPOS.length);
    await TestUtils.expectProperFilteredItemsCount(selectors.getFilterInput, 'maven', 2);

    userEvent.type(selectors.getFilterInput(), '{esc}');
    expect(selectors.rows()).toHaveLength(3);
  });

  it('sorts the rows by name', async function() {
    await renderView();

    const nameHeader = selectors.headerCell(COLUMNS.NAME);

    TestUtils.expectProperRowsOrder(REPOS, FIELDS.NAME);

    userEvent.click(nameHeader);
    let sortedRepos = sortRepos(FIELDS.NAME, descend);
    TestUtils.expectProperRowsOrder(sortedRepos, FIELDS.NAME);

    userEvent.click(nameHeader);
    sortedRepos = sortRepos(FIELDS.NAME);
    TestUtils.expectProperRowsOrder(sortedRepos, FIELDS.NAME);
  });

  it('sorts the rows by format', async function() {
    await renderView();

    const nameHeader = selectors.headerCell(COLUMNS.FORMAT);

    TestUtils.expectProperRowsOrder(REPOS, FIELDS.NAME);

    userEvent.click(nameHeader);
    let sortedRepos = sortRepos(FIELDS.FORMAT);
    TestUtils.expectProperRowsOrder(sortedRepos, FIELDS.NAME);

    userEvent.click(nameHeader);
    sortedRepos = sortRepos(FIELDS.FORMAT, descend);
    TestUtils.expectProperRowsOrder(sortedRepos, FIELDS.NAME);
  });

  it('renders tooltips of sorting direction when hovering', async function() {
    await renderView();

    const headerBtn = within(selectors.headerCell(COLUMNS.FORMAT)).getByRole('button');

    await TestUtils.expectToSeeTooltipOnHover(headerBtn, 'Format unsorted');

    userEvent.click(headerBtn);
    await TestUtils.expectToSeeTooltipOnHover(headerBtn, 'Format ascending');

    userEvent.click(headerBtn);
    await TestUtils.expectToSeeTooltipOnHover(headerBtn, 'Format descending');
  });
});
