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
import {
  render,
  waitForElementToBeRemoved,
  screen,
} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {values, pick, ascend, descend, sort, prop} from 'ramda';
import axios from 'axios';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';
import {APIConstants, ExtJS} from '@sonatype/nexus-ui-plugin';
import MetricHealthList from './MetricHealthList';
import {nodes} from './MetricHealth.testdata';
import {convert} from './MetricHealthListMachine';

jest.mock('axios', () => ({
  ...jest.requireActual('axios'),
  get: jest.fn(),
}));

import UIStrings from '../../../../constants/UIStrings';

const {METRIC_HEALTH: LABELS} = UIStrings;
const {
  SORT_DIRECTIONS: {DESC, ASC},
} = APIConstants;

const sortNodes = (field, order = ASC) =>
  sort((order === ASC ? ascend : descend)(prop(field)), convert(nodes));

describe('MetricHealthList', () => {
  const selectors = {
    ...TestUtils.selectors,
    ...TestUtils.tableSelectors,
    emptyMessage: () => screen.getByText(LABELS.EMPTY_NODE_LIST),
  };
  const columns = [
    '',
    LABELS.NAME_HEADER,
    LABELS.ERROR_HEADER,
    LABELS.MESSAGE_HEADER,
  ];
  const fields = {
    NAME: 'name',
    ERROR: 'error',
    MESSAGE: 'message',
  };

  const renderView = async () => {
    jest.spyOn(ExtJS, 'useState').mockReturnValue({});
    jest.spyOn(ExtJS, 'usePermission').mockReturnValue({});

    const result = render(<MetricHealthList />);
    await waitForElementToBeRemoved(TestUtils.selectors.queryLoadingMask());
    return result;
  };

  const expectTableRows = (data, keys) => {
    const rows = TestUtils.tableSelectors.rows();

    expect(rows).toHaveLength(data.length);

    data.forEach((item, index) => {
      const row = rows[index];

      keys.forEach((key) => expect(item.hasOwnProperty(key)).toBeTruthy());

      values(pick(keys, item)).forEach((value, index) => {
        expect(row.cells[index + 1]).toHaveTextContent(value || '');
      });
    });
  };

  it('renders the resolved data', async () => {
    axios.get.mockReturnValue(
      Promise.resolve({
        data: nodes,
      })
    );

    await renderView();

    TestUtils.expectTableHeaders(columns);
    expectTableRows(convert(nodes), Object.values(fields));
  });

  it('renders message if there is an error', async () => {
    const errorMessage = 'Error';
    axios.get.mockRejectedValue({message: errorMessage});

    await renderView();

    expect(screen.queryByText(errorMessage)).toBeInTheDocument();
  });

  it('renders the resolved empty data', async () => {
    axios.get.mockReturnValue(
      Promise.resolve({
        data: [],
      })
    );

    await renderView();

    expect(selectors.emptyMessage()).toBeInTheDocument();
  });

  describe('Sorting', () => {
    const expectProperRowsOrder = (
      data,
      fieldName = 'name',
      columnIndex = 0
    ) => {
      const rows = TestUtils.tableSelectors.rows();
      data.forEach((item, index) => {
        expect(rows[index].cells[columnIndex + 1]).toHaveTextContent(
          item[fieldName]
        );
      });
    };

    const expectProperOrder = async (fieldName, columnName, direction) => {
      const {headerCell} = selectors;

      const sortedNodes = sortNodes(fieldName, direction);
      userEvent.click(headerCell(columnName));

      expectProperRowsOrder(sortedNodes);
    };

    it('sorts the rows by each columns', async () => {
      axios.get.mockReturnValue(
        Promise.resolve({
          data: nodes,
        })
      );

      await renderView();

      await expectProperOrder(fields.NAME, LABELS.NAME_HEADER, DESC);
      await expectProperOrder(fields.NAME, LABELS.NAME_HEADER, ASC);
      await expectProperOrder(fields.ERROR, LABELS.ERROR_HEADER, ASC);
      await expectProperOrder(fields.ERROR, LABELS.ERROR_HEADER, DESC);
      await expectProperOrder(fields.MESSAGE, LABELS.MESSAGE_HEADER, ASC);
      await expectProperOrder(fields.MESSAGE, LABELS.MESSAGE_HEADER, DESC);
    });
  });
});
