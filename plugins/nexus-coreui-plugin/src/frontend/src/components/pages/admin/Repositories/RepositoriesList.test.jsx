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
import {fireEvent, waitForElementToBeRemoved} from '@testing-library/react'
import '@testing-library/jest-dom/extend-expect';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';

import RepositoriesList from './RepositoriesList';

jest.mock('axios', () => ({
  get: jest.fn()
}));
const mockCopyUrl = jest.fn((event) => event.stopPropagation());

describe('RepositoriesList', function() {
  const rows = [{
    name: 'maven-central',
    type: 'proxy',
    format: 'maven2',
    status: {online: true, description: 'Ready to Connect'},
    url: 'http://localhost:8081/repository/maven-central/'
  }, {
    name: 'maven-public',
    type: 'group',
    format: 'maven2',
    status: {online: true},
    url: 'http://localhost:8081/repository/maven-public/'
  }, {
    name: 'maven-releases',
    type: 'hosted',
    format: 'maven2',
    status: {online: true},
    url: 'http://localhost:8081/repository/maven-releases/'
  }, {
    name: 'maven-snapshots',
    type: 'hosted',
    format: 'maven2',
    status: {online: false},
    url: 'http://localhost:8081/repository/maven-snapshots/'
  }, {
    name: 'nuget-group',
    type: 'group',
    format: 'nuget',
    status: {online: true},
    url: 'http://localhost:8081/repository/nuget-group/'
  }, {
    name: 'nuget-hosted',
    type: 'hosted',
    format: 'nuget',
    status: {online: true},
    url: 'http://localhost:8081/repository/nuget-hosted/'
  }, {
    name: 'nuget.org-proxy',
    type: 'proxy',
    format: 'nuget',
    status: {
      online: true,
      description: 'Remote Auto Blocked and Unavailable',
      reason: 'java.net.UnknownHostException: api.example.org: nodename nor servname provided, or not known'
    },
    url: 'http://localhost:8081/repository/nuget.org-proxy/'
  }];

  function render() {
    return TestUtils.render(<RepositoriesList copyUrl={mockCopyUrl}/>, ({container, getByText, getByPlaceholderText}) => ({
      tableHeader: (text) => getByText(text, {selector: 'thead *'}),
      filter: () => getByPlaceholderText('Filter by name'),
      tableRow: (index) => container.querySelectorAll('tbody tr')[index],
      tableRows: () => container.querySelectorAll('tbody tr'),
      urlButton: (index) => container.querySelectorAll('button[title="Copy URL to Clipboard"]')[index]
    }));
  }

  it('renders the loading spinner', async function() {
    axios.get.mockReturnValue(new Promise(() => {}));

    const {loadingMask} = render();

    expect(loadingMask()).toBeInTheDocument();
  });

  it('renders the resolved empty text', async function() {
    axios.get.mockResolvedValue({data: []});

    const {loadingMask, getByText} = render();

    await waitForElementToBeRemoved(loadingMask);

    expect(getByText('There are no repositories available')).toBeInTheDocument();
  });


  it('matches a snapshot', async function() {
    axios.get.mockResolvedValue({data: rows});

    const {loadingMask, container} = render();

    await waitForElementToBeRemoved(loadingMask);

    expect(container).toMatchSnapshot();
  });

  it('renders the rows', async function() {
    axios.get.mockResolvedValue({data: rows});

    const {loadingMask, tableRow} = render();

    await waitForElementToBeRemoved(loadingMask);

    expect(tableRow(0).cells[0]).toHaveTextContent('maven-central');
    expect(tableRow(0).cells[1]).toHaveTextContent('proxy');
    expect(tableRow(0).cells[2]).toHaveTextContent('maven2');
    expect(tableRow(0).cells[3]).toHaveTextContent(/^Online - Ready to Connect$/);
    expect(tableRow(0).cells[4]).toHaveTextContent('http://localhost:8081/repository/maven-central/');

    expect(tableRow(1).cells[0]).toHaveTextContent('maven-public');
    expect(tableRow(1).cells[1]).toHaveTextContent('group');
    expect(tableRow(1).cells[2]).toHaveTextContent('maven2');
    expect(tableRow(1).cells[3]).toHaveTextContent(/^Online$/);
    expect(tableRow(1).cells[4]).toHaveTextContent('http://localhost:8081/repository/maven-public/');

    expect(tableRow(2).cells[0]).toHaveTextContent('maven-releases');
    expect(tableRow(2).cells[1]).toHaveTextContent('hosted');
    expect(tableRow(2).cells[2]).toHaveTextContent('maven2');
    expect(tableRow(2).cells[3]).toHaveTextContent(/^Online$/);
    expect(tableRow(2).cells[4]).toHaveTextContent('http://localhost:8081/repository/maven-releases/');

    expect(tableRow(3).cells[0]).toHaveTextContent('maven-snapshots');
    expect(tableRow(3).cells[1]).toHaveTextContent('hosted');
    expect(tableRow(3).cells[2]).toHaveTextContent('maven2');
    expect(tableRow(3).cells[3]).toHaveTextContent(/^Offline$/);
    expect(tableRow(3).cells[4]).toHaveTextContent('http://localhost:8081/repository/maven-snapshots/');

    expect(tableRow(4).cells[0]).toHaveTextContent('nuget-group');
    expect(tableRow(4).cells[1]).toHaveTextContent('group');
    expect(tableRow(4).cells[2]).toHaveTextContent('nuget');
    expect(tableRow(4).cells[3]).toHaveTextContent(/^Online$/);
    expect(tableRow(4).cells[4]).toHaveTextContent('http://localhost:8081/repository/nuget-group/');

    expect(tableRow(5).cells[0]).toHaveTextContent('nuget-hosted');
    expect(tableRow(5).cells[1]).toHaveTextContent('hosted');
    expect(tableRow(5).cells[2]).toHaveTextContent('nuget');
    expect(tableRow(5).cells[3]).toHaveTextContent(/^Online$/);
    expect(tableRow(5).cells[4]).toHaveTextContent('http://localhost:8081/repository/nuget-hosted/');

    expect(tableRow(6).cells[0]).toHaveTextContent('nuget.org-proxy');
    expect(tableRow(6).cells[1]).toHaveTextContent('proxy');
    expect(tableRow(6).cells[2]).toHaveTextContent('nuget');
    expect(tableRow(6).cells[3]).toHaveTextContent(
      /^Online - Remote Auto Blocked and Unavailable.*java.net.UnknownHostException: api.example.org: nodename nor servname provided, or not known$/
    );
    expect(tableRow(6).cells[4]).toHaveTextContent('http://localhost:8081/repository/nuget.org-proxy/');
  });

  it('sorts the rows by name', async function () {
    axios.get.mockResolvedValue({data: rows});

    const {loadingMask, tableHeader, tableRow} = render();

    await waitForElementToBeRemoved(loadingMask);

    expect(tableRow(0).cells[0]).toHaveTextContent('maven-central');
    expect(tableRow(1).cells[0]).toHaveTextContent('maven-public');
    expect(tableRow(2).cells[0]).toHaveTextContent('maven-releases');
    expect(tableRow(3).cells[0]).toHaveTextContent('maven-snapshots');
    expect(tableRow(4).cells[0]).toHaveTextContent('nuget-group');
    expect(tableRow(5).cells[0]).toHaveTextContent('nuget-hosted');
    expect(tableRow(6).cells[0]).toHaveTextContent('nuget.org-proxy');

    fireEvent.click(tableHeader('Name'));

    expect(tableRow(0).cells[0]).toHaveTextContent('nuget.org-proxy');
    expect(tableRow(1).cells[0]).toHaveTextContent('nuget-hosted');
    expect(tableRow(2).cells[0]).toHaveTextContent('nuget-group');
    expect(tableRow(3).cells[0]).toHaveTextContent('maven-snapshots');
    expect(tableRow(4).cells[0]).toHaveTextContent('maven-releases');
    expect(tableRow(5).cells[0]).toHaveTextContent('maven-public');
    expect(tableRow(6).cells[0]).toHaveTextContent('maven-central');
  });

  it('sorts the rows by type', async function () {
    axios.get.mockResolvedValue({data: rows});

    const {loadingMask, tableHeader, tableRow} = render();

    await waitForElementToBeRemoved(loadingMask);

    fireEvent.click(tableHeader('Type'));

    expect(tableRow(0).cells[1]).toHaveTextContent('group');
    expect(tableRow(1).cells[1]).toHaveTextContent('group');
    expect(tableRow(2).cells[1]).toHaveTextContent('hosted');
    expect(tableRow(3).cells[1]).toHaveTextContent('hosted');
    expect(tableRow(4).cells[1]).toHaveTextContent('hosted');
    expect(tableRow(5).cells[1]).toHaveTextContent('proxy');
    expect(tableRow(6).cells[1]).toHaveTextContent('proxy');

    fireEvent.click(tableHeader('Type'));

    expect(tableRow(0).cells[1]).toHaveTextContent('proxy');
    expect(tableRow(1).cells[1]).toHaveTextContent('proxy');
    expect(tableRow(2).cells[1]).toHaveTextContent('hosted');
    expect(tableRow(3).cells[1]).toHaveTextContent('hosted');
    expect(tableRow(4).cells[1]).toHaveTextContent('hosted');
    expect(tableRow(5).cells[1]).toHaveTextContent('group');
    expect(tableRow(6).cells[1]).toHaveTextContent('group');
  });

  it('sorts the rows by format', async function () {
    axios.get.mockResolvedValue({data: rows});

    const {loadingMask, tableHeader, tableRow} = render();

    await waitForElementToBeRemoved(loadingMask);

    fireEvent.click(tableHeader('Format'));

    expect(tableRow(0).cells[2]).toHaveTextContent('maven2');
    expect(tableRow(1).cells[2]).toHaveTextContent('maven2');
    expect(tableRow(2).cells[2]).toHaveTextContent('maven2');
    expect(tableRow(3).cells[2]).toHaveTextContent('maven2');
    expect(tableRow(4).cells[2]).toHaveTextContent('nuget');
    expect(tableRow(5).cells[2]).toHaveTextContent('nuget');
    expect(tableRow(6).cells[2]).toHaveTextContent('nuget');

    fireEvent.click(tableHeader('Format'));

    expect(tableRow(0).cells[2]).toHaveTextContent('nuget');
    expect(tableRow(1).cells[2]).toHaveTextContent('nuget');
    expect(tableRow(2).cells[2]).toHaveTextContent('nuget');
    expect(tableRow(3).cells[2]).toHaveTextContent('maven2');
    expect(tableRow(4).cells[2]).toHaveTextContent('maven2');
    expect(tableRow(5).cells[2]).toHaveTextContent('maven2');
    expect(tableRow(6).cells[2]).toHaveTextContent('maven2');
  });

  it('filters by name', async function() {
    axios.get.mockResolvedValue({data: rows});

    const {filter, loadingMask, tableRow, tableRows} = render();

    await waitForElementToBeRemoved(loadingMask);

    await TestUtils.changeField(filter, 'org');

    expect(tableRows().length).toBe(1);
    expect(tableRow(0).cells[0]).toHaveTextContent('nuget.org-proxy');
  });

  it('copies url on button press', async function() {
    axios.get.mockResolvedValue({data: rows});

    const {filter, loadingMask, tableRow, tableRows, urlButton} = render();

    await waitForElementToBeRemoved(loadingMask);

    fireEvent.click(urlButton(0));

    expect(mockCopyUrl).toBeCalled();
  });
});
