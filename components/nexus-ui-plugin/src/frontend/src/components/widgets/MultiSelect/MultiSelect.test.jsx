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
import '@testing-library/jest-dom/extend-expect';
import {fireEvent} from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import MultiSelect from './MultiSelect';
import TestUtils from '../../../interface/TestUtils';
import UIStrings from '../../../constants/UIStrings';


describe('MultiSelect', () => {
  function renderView(props) {
    return TestUtils.render(<MultiSelect fromLabel="Available" toLabel="Selected" {...props}/>,
        ({queryByLabelText, queryByText, queryByTitle}) => ({
          from: () => queryByLabelText('Available'),
          to: () => queryByLabelText('Selected'),
          option: (text) => queryByText(text, {selector: 'option'}),
          moveRight: () => queryByTitle(UIStrings.MULTI_SELECT.MOVE_RIGHT),
          moveLeft: () => queryByTitle(UIStrings.MULTI_SELECT.MOVE_LEFT),
          moveUp: () => queryByTitle(UIStrings.MULTI_SELECT.MOVE_UP),
          moveDown: () => queryByTitle(UIStrings.MULTI_SELECT.MOVE_DOWN)
        }));
  }

  it('renders a multiselect with available and selected options', () => {
    const fromOptions = ['a', 'b', 'c'];
    const value = ['a'];
    const {from, to, option} = renderView({
      fromOptions,
      value
    });

    expect(from()).not.toContain(option('a'));
    expect(from()).toContainElement(option('b'));
    expect(from()).toContainElement(option('c'));
    expect(to()).toContainElement(option('a'));
  });

  it('moves items from available to selected', () => {
    const fromOptions = ['a', 'b', 'c'];
    const value = [];
    const onChange = jest.fn();

    const {from, moveRight} = renderView({
      fromOptions,
      value,
      onChange: onChange
    });

    userEvent.selectOptions(from(), 'a');
    fireEvent.click(moveRight());

    expect(onChange).toBeCalledWith(['a']);
  });

  it('moves items from selected to available', () => {
    const fromOptions = ['a', 'b', 'c'];
    const value = ['a'];
    const onChange = jest.fn();

    const {to, moveLeft} = renderView({
      fromOptions,
      value,
      onChange: onChange
    });

    userEvent.selectOptions(to(), 'a');
    fireEvent.click(moveLeft());

    expect(onChange).toBeCalledWith([]);
  });

  it('moves items up in priority', () => {
    const fromOptions = ['a', 'b', 'c', 'd'];
    const value = ['a', 'b', 'c', 'd'];
    const onChange = jest.fn();

    const {to, moveUp} = renderView({
      fromOptions,
      value,
      onChange: onChange
    });

    userEvent.selectOptions(to(), ['a', 'b', 'd']);
    fireEvent.click(moveUp());

    expect(onChange).toBeCalledWith(['a', 'b', 'd', 'c']);
  });

  it('moves items down in priority', () => {
    const fromOptions = ['a', 'b', 'c', 'd'];
    const value = ['a', 'b', 'c', 'd'];
    const onChange = jest.fn();

    const {to, moveDown} = renderView({
      fromOptions,
      value,
      onChange: onChange
    });

    userEvent.selectOptions(to(), ['a', 'c', 'd']);
    fireEvent.click(moveDown());

    expect(onChange).toBeCalledWith(['b', 'a', 'c', 'd']);
  });

  it('moves the first item down in priority', () => {
    const fromOptions = ['a', 'b', 'c', 'd'];
    const value = ['a', 'b', 'c', 'd'];
    const onChange = jest.fn();

    const {to, moveDown} = renderView({
      fromOptions,
      value,
      onChange: onChange
    });

    userEvent.selectOptions(to(), ['a']);
    fireEvent.click(moveDown());

    expect(onChange).toBeCalledWith(['b', 'a', 'c', 'd']);
  });
});
