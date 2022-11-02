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
import {render, screen, waitForElementToBeRemoved} from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';

import BundlesList from './BundlesList';
import BundlesListMachine from './BundlesListMachine';
import {interpret} from 'xstate';
import mockData from './bundles.testdata';

const DEFAULT_RESPONSE = () => Promise.resolve({data: mockData});
const ERROR_RESPONSE = () => Promise.reject({message: 'Error'});

const selectors = {
  ...TestUtils.selectors,
  getTableHeader: (text) => screen.getByRole('columnheader', {name: text}),
  getTableRow: (index) => screen.getAllByRole('row')[index],
  getTableRows: () => screen.getAllByRole('row')
}

fdescribe('BundlesList', function() {
  let service;
  beforeEach(() => {
    service = interpret(BundlesListMachine.withConfig({
      services: {
        fetchData: DEFAULT_RESPONSE
      }
    })).start();
  })
  it('renders the resolved data', async function() {
    render(<BundlesList service={service} />);

    mockData.forEach((row, i) => {
      expect(selectors.getTableRow(i+1).cells[0]).toHaveTextContent(row.id);
      expect(selectors.getTableRow(i+1).cells[1]).toHaveTextContent(row.state);
      expect(selectors.getTableRow(i+1).cells[2]).toHaveTextContent(row.startLevel);
      expect(selectors.getTableRow(i+1).cells[3]).toHaveTextContent(row.name);
      expect(selectors.getTableRow(i+1).cells[4]).toHaveTextContent(row.version);
    });
  });

  it('renders an error message', async function() {
    service = interpret(BundlesListMachine.withConfig({
      services: {
        fetchData: ERROR_RESPONSE
      }
    })).start();

    const { container } = render(<BundlesList service={service} />);
    await waitForElementToBeRemoved(selectors.queryLoadingMask());
    expect(container.querySelector('.nx-cell--meta-info')).toHaveTextContent('Error');
  });

  it('filters by name', async function() {
    const { queryByPlaceholderText } = render(<BundlesList service={service} />);
    const filter = () => queryByPlaceholderText('Enter a filter value');
    await TestUtils.changeField(filter, 'testNameData2ForTestingFilter');

    expect(selectors.getTableRows().length).toBe(2);
    expect(selectors.getTableRow(1).cells[3]).toHaveTextContent('testNameData2ForTestingFilter');
  });

  it('sorts the rows by id', async function () {
    render(<BundlesList service={service} />);

    expect(selectors.getTableRow(1).cells[0]).toHaveTextContent('1');
    expect(selectors.getTableRow(2).cells[0]).toHaveTextContent('2');

    userEvent.click(selectors.getTableHeader('ID'));
    userEvent.click(selectors.getTableHeader('ID'));

    expect(selectors.getTableRow(1).cells[0]).toHaveTextContent('2');
    expect(selectors.getTableRow(2).cells[0]).toHaveTextContent('1');
  });

  it('sorts the rows by state', async function () {
    render(<BundlesList service={service} />);

    expect(selectors.getTableRow(1).cells[1]).toHaveTextContent('testStateData');
    expect(selectors.getTableRow(2).cells[1]).toHaveTextContent('testStateData2');

    userEvent.click(selectors.getTableHeader('State'));
    userEvent.click(selectors.getTableHeader('State'));

    expect(selectors.getTableRow(1).cells[1]).toHaveTextContent('testStateData2');
    expect(selectors.getTableRow(2).cells[1]).toHaveTextContent('testStateData');
  });

  it('sorts the rows by level', async function () {
    render(<BundlesList service={service} />);

    expect(selectors.getTableRow(1).cells[2]).toHaveTextContent('testStartLevelData');
    expect(selectors.getTableRow(2).cells[2]).toHaveTextContent('testStartLevelData2');

    userEvent.click(selectors.getTableHeader('Level'));
    userEvent.click(selectors.getTableHeader('Level'));

    expect(selectors.getTableRow(1).cells[2]).toHaveTextContent('testStartLevelData2');
    expect(selectors.getTableRow(2).cells[2]).toHaveTextContent('testStartLevelData');
  });

  it('sorts the rows by name', async function () {
    render(<BundlesList service={service} />);

    expect(selectors.getTableRow(1).cells[3]).toHaveTextContent('testNameData');
    expect(selectors.getTableRow(2).cells[3]).toHaveTextContent('testNameData2ForTestingFilter');

    userEvent.click(selectors.getTableHeader('Name'));

    expect(selectors.getTableRow(1).cells[3]).toHaveTextContent('testNameData2ForTestingFilter');
    expect(selectors.getTableRow(2).cells[3]).toHaveTextContent('testNameData');
  });

  it('sorts the rows by version', async function () {
    render(<BundlesList service={service} />);

    expect(selectors.getTableRow(1).cells[4]).toHaveTextContent('testVersionData');
    expect(selectors.getTableRow(2).cells[4]).toHaveTextContent('testVersionData2');

    userEvent.click(selectors.getTableHeader('Version'));
    userEvent.click(selectors.getTableHeader('Version'));

    expect(selectors.getTableRow(1).cells[4]).toHaveTextContent('testVersionData2');
    expect(selectors.getTableRow(2).cells[4]).toHaveTextContent('testVersionData');
  });
});
