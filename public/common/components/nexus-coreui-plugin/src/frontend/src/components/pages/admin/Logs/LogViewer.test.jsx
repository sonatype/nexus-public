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
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import LogViewer from './LogViewer';
import UIStrings from '../../../../constants/UIStrings';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';

const { VIEW } = UIStrings.LOGS;

jest.mock('axios', () => ({
  ...jest.requireActual('axios'),
  get: jest.fn(),
  post: jest.fn()
}));

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  ExtJS: {
    urlOf: jest.fn().mockImplementation(() => 'service/rest/internal/logging/logs/test')
  }
}));

describe('LogViewer', () => {
  const renderView = async (itemId, logs) => {
    axios.get.mockReturnValue(Promise.resolve({ data: logs }));
    return render(<LogViewer itemId={itemId} />);
  };

  const renderErrorView = async (itemId, errorMessage) => {
    axios.get.mockReturnValue(Promise.reject({ message: errorMessage }));
    return render(<LogViewer itemId={itemId} />);
  };

  it('has the correct logs', async () => {
    const { container } = await renderView('test', 'This is a test log');

    const logs = container.querySelector('.log-viewer-textarea');
    await waitFor(() => {
      expect(logs).toHaveTextContent('This is a test log');
    });
  });

  it('requests log content with Accept: text/plain to avoid a 406 from the server', async () => {
    await renderView('nexus.log', 'log content');

    await waitFor(() => {
      expect(axios.get).toHaveBeenCalledWith(
          expect.stringContaining('/nexus.log'),
          expect.objectContaining({headers: {'Accept': 'text/plain'}})
      );
    });
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
    const { container } = await renderView('nexus.log', 'This is a test log');
    const marker = () => screen.getByRole('textbox', { name: 'Marker to insert into log' });
    const insertButton = container.querySelector('.nx-btn#insertMark');

    await TestUtils.changeField(marker, 'testing the mark');
    userEvent.click(insertButton);

    expect(axios.post).toHaveBeenCalledWith('service/rest/internal/logging/log/mark', 'testing the mark', {
      headers: { 'Content-Type': 'text/plain' }
    });
  });

  it('renders an error message when log fetch fails', async () => {
    const { container } = await renderErrorView('test.log', 'Network Error');

    await waitFor(() => {
      expect(container.querySelector('.nx-alert--error')).toHaveTextContent('Network Error');
    });
  });

  it('shows a Retry button in error state', async () => {
    await renderErrorView('test.log', 'Network Error');

    await waitFor(() => {
      expect(screen.getByText('Retry')).toBeInTheDocument();
    });
  });

  it('recovers after clicking Retry', async () => {
    axios.get
        .mockReturnValueOnce(Promise.reject({message: 'Network Error'}))
        .mockReturnValueOnce(Promise.resolve({data: 'Recovered log content'}));

    const { container } = render(<LogViewer itemId="test.log" />);
    await waitFor(() => expect(screen.getByText('Retry')).toBeInTheDocument());

    userEvent.click(screen.getByText('Retry'));

    await waitFor(() => {
      expect(container.querySelector('.log-viewer-textarea')).toHaveTextContent('Recovered log content');
    });
    expect(screen.queryByText('Retry')).not.toBeInTheDocument();
  });

  it('recovers when refresh rate is changed to auto in error state', async () => {
    axios.get
        .mockReturnValueOnce(Promise.reject({message: 'Network Error'}))
        .mockReturnValueOnce(Promise.resolve({data: 'Log content after recovery'}));

    const { container } = render(<LogViewer itemId="test.log" />);
    await waitFor(() => expect(screen.getByText('Retry')).toBeInTheDocument());

    const rate = screen.getByLabelText(VIEW.REFRESH.RATE_LABEL);
    await userEvent.selectOptions(rate, '20');

    await waitFor(() => {
      expect(container.querySelector('.log-viewer-textarea')).toHaveTextContent('Log content after recovery');
    });
    expect(screen.queryByText('Retry')).not.toBeInTheDocument();
  });

  it('recovers when refresh rate is changed to manual in error state', async () => {
    // 1st fetch succeeds (machine settles into idle/manual state with period=0)
    // 2nd fetch fails when user switches to auto-refresh (machine enters error with period=20)
    // 3rd fetch succeeds when user switches back to Manual (sends MANUAL_REFRESH)
    axios.get
        .mockResolvedValueOnce({data: 'Initial content'})
        .mockRejectedValueOnce({message: 'Network Error'})
        .mockResolvedValueOnce({data: 'Manual recovery content'});

    const { container } = render(<LogViewer itemId="test.log" />);
    await waitFor(() => expect(container.querySelector('.log-viewer-textarea')).toHaveTextContent('Initial content'));

    const rate = screen.getByLabelText(VIEW.REFRESH.RATE_LABEL);

    // Switch to auto: UPDATE_PERIOD → retrieve → fails → error state with period=20
    await userEvent.selectOptions(rate, '20');
    await waitFor(() => expect(screen.getByText('Retry')).toBeInTheDocument());

    // Switch back to Manual: MANUAL_REFRESH → retrieve → succeeds → recovery
    await userEvent.selectOptions(rate, '0');
    await waitFor(() => {
      expect(container.querySelector('.log-viewer-textarea')).toHaveTextContent('Manual recovery content');
    });
    expect(screen.queryByText('Retry')).not.toBeInTheDocument();
  });

  it('has the download button with analytics id', async () => {
    const { container } = await renderView('test', 'This is a test log');

    const downloadButton = container.querySelector('[data-analytics-id="nxrm-logs-download"]');
    expect(downloadButton).toBeInTheDocument();
    expect(downloadButton).toHaveTextContent('Download');
  });

  it('has the mark button with analytics id', async () => {
    const { container } = await renderView('nexus.log', 'This is a test log');

    const markButton = container.querySelector('[data-analytics-id="nxrm-logs-mark"]');
    expect(markButton).toBeInTheDocument();
  });
});
