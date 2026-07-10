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
import { render, screen, waitForElementToBeRemoved, fireEvent } from '@testing-library/react';
import LogsList from './LogsList';
import UIStrings from '../../../../constants/UIStrings';

jest.mock('axios', () => ({
  ...jest.requireActual('axios'),
  get: jest.fn()
}));

describe('LogsList', () => {
  const renderView = async (logs = []) => {
    axios.get.mockReturnValue(Promise.resolve({ data: logs }));
    const onEdit = jest.fn();
    const view = render(<LogsList onEdit={onEdit} />);
    await waitForElementToBeRemoved(() => screen.queryByRole('status'));
    return { ...view, onEdit };
  };

  it('renders the log files list', async () => {
    const logs = [
      { fileName: 'nexus.log', size: 1024, lastModified: Date.now() },
      { fileName: 'tasks.log', size: 2048, lastModified: Date.now() }
    ];

    const { container } = await renderView(logs);

    expect(container.querySelector('tbody tr:nth-child(1) td:nth-child(1)')).toHaveTextContent('nexus.log');
    expect(container.querySelector('tbody tr:nth-child(2) td:nth-child(1)')).toHaveTextContent('tasks.log');
  });

  it('has the filter input with analytics id on the underlying input element', async () => {
    const { container } = await renderView([]);

    // inputAttributes threads the attribute through NxFilterInput → NxTextInput → <input>
    const input = container.querySelector('input[data-analytics-id="nxrm-logs-filter"]');
    expect(input).toBeInTheDocument();
  });

  it('encodes plain log filenames when clicked', async () => {
    const logs = [{ fileName: 'nexus.log', size: 1024, lastModified: Date.now() }];
    const { container, onEdit } = await renderView(logs);

    fireEvent.click(container.querySelector('tbody tr'));

    expect(onEdit).toHaveBeenCalledWith('nexus.log');
  });

  it('preserves directory separators for task log paths when clicked', async () => {
    const taskLog = 'tasks/component.normalize.version-20260605161637239.log';
    const logs = [{ fileName: taskLog, size: 1024, lastModified: Date.now() }];
    const { container, onEdit } = await renderView(logs);

    fireEvent.click(container.querySelector('tbody tr'));

    // Each segment is encoded but the '/' separator is preserved
    expect(onEdit).toHaveBeenCalledWith('tasks/component.normalize.version-20260605161637239.log');
  });

  it('filters the log files list by file name', async () => {
    const logs = [
      { fileName: 'nexus.log', size: 1024, lastModified: Date.now() },
      { fileName: 'tasks.log', size: 2048, lastModified: Date.now() }
    ];

    const { container, queryByPlaceholderText } = await renderView(logs);
    const filterInput = queryByPlaceholderText(UIStrings.LOGS.LIST.FILTER_PLACEHOLDER);

    fireEvent.change(filterInput, { target: { value: 'nexus' } });

    expect(container.querySelector('tbody tr:nth-child(1) td:nth-child(1)')).toHaveTextContent('nexus.log');
    expect(container.querySelectorAll('tbody tr').length).toBe(1);
  });
});
