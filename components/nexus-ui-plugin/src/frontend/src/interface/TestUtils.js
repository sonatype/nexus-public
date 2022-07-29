/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
import {fireEvent, render, screen, waitFor, within} from '@testing-library/react';
import {pick} from 'ramda';

/**
 * @since 3.24
 */
export default class TestUtils {
  static get UNRESOLVED() {
    return new Promise(() => {});
  }

  /**
   * @deprecated call render directly and use the {selectors} in this class instead
   */
  static render(view, extraSelectors) {
    const selectors = render(view);
    const {queryByText} = selectors;
    return {
      ...selectors,
      loadingMask: () => queryByText('Loading…'),
      savingMask: () => queryByText(/saving/i),
      submittingMask: () => queryByText(/submitting/i),
      ...extraSelectors(selectors)
    };
  }

  static async changeField(fieldSelector, value) {
    fireEvent.change(fieldSelector(), {
      currentTarget: {
        name: fieldSelector().name,
        value
      },
      target: {
        name: fieldSelector().name,
        value
      }
    });
    try {
      await waitFor(() => expect(fieldSelector()).toHaveValue(value));
    } catch(error) {
      throw new Error(`${fieldSelector().name} with value ${fieldSelector().value} did not match the expected value \n ${error.message}`);
    }
  }

  static selectors = {
    queryLoadingMask: () => screen.queryByText('Loading…'),
    querySavingMask: () => screen.queryByText(/saving/i),
    querySubmittingMask: () => screen.queryByText(/submitting/i),
  }

  static tableSelectors = {
    table: () => screen.getByRole('table'),
    headerCells: () => screen.getAllByRole('columnheader'),
    headerCell: (name) => screen.getByRole('columnheader', {name}),
    tableBody: () => screen.getAllByRole('rowgroup')[1],
    rows: () => within(screen.getAllByRole('rowgroup')[1]).getAllByRole('row'),
    tableAlert: () => within(screen.getAllByRole('rowgroup')[1]).getByRole('alert'),
  };

  static expectTableHeaders(names) {
    const cells = this.tableSelectors.headerCells();
    names.forEach((name, index) => {
      expect(cells[index]).toHaveTextContent(name);
    });
  }

  static expectTableRows(data, keys) {
    const rows = this.tableSelectors.rows();

    expect(rows).toHaveLength(data.length);

    data.forEach((item, index) => {
      const row = rows[index];
      Object.values(pick(keys, item)).forEach((value, index)  => {
        expect(row.cells[index]).toHaveTextContent(value);
      })
    });
  }

  static expectProperRowsOrder(data, fieldName = 'name', columnIndex = 0) {
    const rows = this.tableSelectors.rows();
    data.forEach((item, index) => {
      expect(rows[index].cells[columnIndex]).toHaveTextContent(item[fieldName]);
    });
  }

  static async expectProperFilteredItemsCount(filter, query, count) {
    await this.changeField(filter, query);
    expect(this.tableSelectors.rows()).toHaveLength(count);
  }

  static makeExtResult(data) {
    return {
      result: {
        data,
        success: true
      }
    };
  }
}
