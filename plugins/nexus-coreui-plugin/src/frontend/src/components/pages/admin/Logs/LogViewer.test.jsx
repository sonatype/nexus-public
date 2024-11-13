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
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import LogViewer from './LogViewer';
import UIStrings from '../../../../constants/UIStrings';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';

const {VIEW} = UIStrings.LOGS;

jest.mock('axios', () => ({
  ...jest.requireActual('axios'),
  get: jest.fn(),
  post: jest.fn()
}));

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  ExtJS: {
    urlOf: jest.fn().mockImplementation(() => 'service/rest/internal/logging/logs/test'),
  },
}));

describe('LogViewer', () => {
  const renderView = async (itemId, logs) => {
    axios.get.mockReturnValue(Promise.resolve({data: logs}));
    return render(<LogViewer itemId={itemId}/>);
  };

  it('has the correct logs', async () => {
    const {container} = await renderView('test', 'This is a test log');

    const logs = container.querySelector('.log-viewer-textarea');
    expect(logs).toHaveTextContent('This is a test log');
  });

  it('has the download button available', async () => {
    await renderView('test', 'This is a test log');

    expect(screen.getByText('Viewing test')).toBeInTheDocument();
    expect(screen.queryByText('Download')).not.toBeDisabled();
  });

  it('changes the refresh rate', async () => {
    await renderView('test', 'This is a test log');
    const rate = screen.getByLabelText(VIEW.REFRESH.RATE_LABEL);

    expect(rate).toBeInTheDocument();
    expect(rate).toHaveValue('0');
    expect(rate).toHaveDisplayValue('Manual');

    await userEvent.selectOptions(rate, '20');

    expect(rate).toHaveValue('20');
    expect(rate).toHaveDisplayValue('Every 20 seconds');
  });

  it('changes the viewing size', async () => {
    await renderView('test', 'This is a test log');
    const size = screen.getByLabelText(VIEW.REFRESH.SIZE_LABEL);

    expect(size).toBeInTheDocument();
    expect(size).toHaveValue('25');
    expect(size).toHaveDisplayValue('Last 25KB');

    await userEvent.selectOptions(size, '50');

    expect(size).toHaveValue('50');
    expect(size).toHaveDisplayValue('Last 50KB');
  });

  it('updates the marker after inserting', async () => {
    const {container} = await renderView('nexus.log', 'This is a test log');
    const marker = () => screen.getByRole('textbox', {name: 'Marker to insert into log'});
    const insertButton = container.querySelector('.nx-btn#insertMark');

    await TestUtils.changeField(marker, 'testing the mark');
    userEvent.click(insertButton);

    expect(axios.post).toHaveBeenCalledWith(
      'service/rest/internal/logging/log/mark',
      'testing the mark', {headers: {'Content-Type': 'text/plain'}});
  });
});
