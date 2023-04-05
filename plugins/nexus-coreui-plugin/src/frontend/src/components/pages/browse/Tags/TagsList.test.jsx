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
import {render, screen, waitForElementToBeRemoved, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import axios from 'axios';
import {sort, prop, descend, ascend} from 'ramda';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';

import TagsList from './TagsList';
import UIStrings from '../../../../constants/UIStrings';

jest.mock('axios', () => ({
  post: jest.fn()
}));

describe('TagsList', function() {
  const tags = [
    {
      id: 'tag1',
      firstCreatedTime: '1/1/2020, 1:00:00 AM',
      lastUpdatedTime: '1/2/2020, 2:00:00 AM'
    },
    {
      id: 'tag2',
      firstCreatedTime: '2/2/2020, 1:00:00 AM',
      lastUpdatedTime: '2/3/2020, 2:00:00 AM'
    }
  ],
  emptyTag = [];

  const FIELDS = {
    ID: 'id',
    FIRST_CREATED: 'firstCreatedTime',
    LAST_UPDATED: 'lastUpdatedTime'
  };

  const {NAME, FIRST_CREATED, LAST_UPDATED} = UIStrings.TAGS.LIST.COLUMNS;
  const {COLUMNS} = UIStrings.TAGS.LIST;

  const sortTags = (field, order = ascend) => sort(order(prop(field)), tags);

  const selectors = {
    ...TestUtils.selectors,
    ...TestUtils.tableSelectors,
    getFilterInput: () => screen.getByPlaceholderText('Filter by Name'),
    getEmptyMessage: () => screen.getByText('No tags found.')
  };

  async function renderView(data) {
    axios.post.mockResolvedValue({data: TestUtils.makeExtResult(data)});

    render(<TagsList/>);
    await waitForElementToBeRemoved(selectors.queryLoadingMask());
  };

  it('renders the resolved empty text', async function() {
    await renderView(emptyTag);
    expect(selectors.getEmptyMessage()).toBeInTheDocument();
  });

  it('renders the error message', async function() {
    axios.post.mockRejectedValue({message: 'Error'});
    render(<TagsList/>);
    await waitForElementToBeRemoved(selectors.queryLoadingMask());
    const error = selectors.tableAlert();

    expect(error).toBeInTheDocument();
    expect(error).toHaveTextContent('Error');
  });

  it('renders the resolved data', async function() {
    await renderView(tags);

    TestUtils.expectTableHeaders(Object.values(COLUMNS));
    TestUtils.expectTableRows(tags, Object.values(FIELDS));
  });

  it('renders a "Filter" text input', async function() {
    await renderView(tags);

    expect(selectors.getFilterInput()).toBeInTheDocument();
  });

  it('filters by the text value when the user types into the filter', async function() {
    await renderView(tags);

    await TestUtils.expectProperFilteredItemsCount(selectors.getFilterInput, '', tags.length);
    await TestUtils.expectProperFilteredItemsCount(selectors.getFilterInput, 'tag1', 1);
    await TestUtils.expectProperFilteredItemsCount(selectors.getFilterInput, 'tag2', 1);
  });

  it('unfilters when the clear button is pressed', async function() {
    await renderView(tags);

    await TestUtils.expectProperFilteredItemsCount(selectors.getFilterInput, '', tags.length);
    await TestUtils.expectProperFilteredItemsCount(selectors.getFilterInput, 'tag1', 1);

    const clearBtn = await screen.findByRole('button', {name: 'Clear filter'});

    userEvent.click(clearBtn);
    expect(selectors.rows()).toHaveLength(2);
  });

  it('unfilters when the ESC key is pressed', async function() {
    await renderView(tags);

    await TestUtils.expectProperFilteredItemsCount(selectors.getFilterInput, '', tags.length);
    await TestUtils.expectProperFilteredItemsCount(selectors.getFilterInput, 'tag1', 1);

    userEvent.type(selectors.getFilterInput(), '{esc}');
    expect(selectors.rows()).toHaveLength(2);
  });

  it('sorts the rows by name', async function() {
    await renderView(tags);

    const nameHeader = selectors.headerCell(NAME);

    TestUtils.expectProperRowsOrder(tags, FIELDS.ID);

    userEvent.click(nameHeader);
    let sortedTags = sortTags(FIELDS.ID, descend);
    TestUtils.expectProperRowsOrder(sortedTags, FIELDS.ID);

    userEvent.click(nameHeader);
    sortedTags = sortTags(FIELDS.ID);
    TestUtils.expectProperRowsOrder(sortedTags, FIELDS.ID);
  });

  it('sorts the rows by firstCreated', async function() {
    await renderView(tags);

    const firstCreatedHeader = selectors.headerCell(FIRST_CREATED);

    TestUtils.expectProperRowsOrder(tags, FIELDS.ID);

    userEvent.click(firstCreatedHeader);
    let sortedTags = sortTags(FIELDS.FIRST_CREATED);
    TestUtils.expectProperRowsOrder(sortedTags, FIELDS.ID);

    userEvent.click(firstCreatedHeader);
    sortedTags = sortTags(FIELDS.FIRST_CREATED, descend);
    TestUtils.expectProperRowsOrder(sortedTags, FIELDS.ID);
  });

  it('sorts the rows by lastUpdated', async function() {
    await renderView(tags);

    const lastUpdatedHeader = selectors.headerCell(LAST_UPDATED);

    TestUtils.expectProperRowsOrder(tags, FIELDS.ID);

    userEvent.click(lastUpdatedHeader);
    let sortedTags = sortTags(FIELDS.LAST_UPDATED);
    TestUtils.expectProperRowsOrder(sortedTags, FIELDS.ID);

    userEvent.click(lastUpdatedHeader);
    sortedTags = sortTags(FIELDS.LAST_UPDATED, descend);
    TestUtils.expectProperRowsOrder(sortedTags, FIELDS.ID);
  });

  it('renders tooltips of sorting direction when hovering', async function() {
    await renderView(tags);

    const headerBtn = within(selectors.headerCell(FIRST_CREATED)).getByRole('button');

    await TestUtils.expectToSeeTooltipOnHover(headerBtn, 'First created time unsorted');

    userEvent.click(headerBtn);
    await TestUtils.expectToSeeTooltipOnHover(headerBtn, 'First created time ascending');

    userEvent.click(headerBtn);
    await TestUtils.expectToSeeTooltipOnHover(headerBtn, 'First created time descending');
  });
});
