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
import { act } from 'react-dom/test-utils';
import {fireEvent, render, wait} from '@testing-library/react';
import '@testing-library/jest-dom/extend-expect';
import TestUtils from 'nexus-ui-plugin/src/frontend/src/interface/TestUtils';
import axios from 'axios';

import LogViewer from './LogViewer';

jest.mock('axios', () => ({
  ...jest.requireActual('axios'), // Use most functions from actual axios
  get: jest.fn(),
  put: jest.fn(),
  post: jest.fn()
}));

describe('LogViewer', function() {
  const renderView = async () => {
    var selectors;
    await act(async () => {
      selectors = TestUtils.render(<LogViewer/>, ({queryByPlaceholderText}) => ({
        markInput: () => queryByPlaceholderText("Marker to insert into log")
      }));
    });
    return selectors;
  };

  const changeFieldAndAssertValue = async (fieldSelector, value) => {
    fireEvent.change(fieldSelector(), {target: {value: value}});
    await wait(() => expect(fieldSelector()).toHaveValue(value));
  };

  it('renders the log into the text area', async function() {
    axios.get.mockReturnValue(Promise.resolve({
      data: 'log data'
    }));

    const {container} = await renderView();

    act(() => {
      expect(container.querySelector('textarea')).toHaveValue("log data");
    });

    expect(container).toMatchSnapshot();
  });

  it('creates a mark when insert mark clicked', async function() {
    axios.get.mockReturnValue(Promise.resolve({
      data: 'log data'
    }));

    const {container, markInput, insertMarkButton} = await renderView();

    await changeFieldAndAssertValue(markInput, 'mymark');

    fireEvent.click(container.querySelector("#insertMark"));

    expect(axios.post).toBeCalled();
  });

  it('retrieves with specified size when selected', async function() {
    axios.get.mockReturnValue(Promise.resolve({
      data: 'log data'
    }));

    const {container} = await renderView();

    await changeFieldAndAssertValue(() => container.querySelector("select#size"), "100");

    expect(axios.get.mock.calls[1][1]).toMatchObject({params: {bytesCount: -102400}});
  });
});
