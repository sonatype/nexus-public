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
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';

import { LogViewer } from '../LogViewer';
import * as useLogsApiModule from '../useLogsApi';

// Mock the API hook
jest.mock('../useLogsApi');

// Mock nexus-ui-plugin
// Mock ExtJS - spread actual mock and override only what's needed
// Mock ExtJS - use createNexusUiPluginMock and extend with overrides
jest.mock('@sonatype/nexus-ui-plugin', () => {
  const { createNexusUiPluginMock } = require('../../../../../../../__jest__/mocks/nexusUiPluginMock');
  return createNexusUiPluginMock({
    ExtJS: {
      urlOf: jest.fn((url) => `http://localhost:8081/${url}`),
      downloadUrl: jest.fn(),
    },
  });
});

const mockedUseLogsApi = useLogsApiModule.useLogsApi as jest.MockedFunction<typeof useLogsApiModule.useLogsApi>;

// Wrapper component for Radix Theme
function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

const mockLogContent = `2024-01-01 12:00:00 INFO  [main] - Starting application
2024-01-01 12:00:01 INFO  [main] - Loading configuration
2024-01-01 12:00:02 INFO  [main] - Application started successfully`;

describe('LogViewer', () => {
  const mockFetchLogContent = jest.fn();
  const mockInsertMark = jest.fn();
  const mockGetDownloadUrl = jest.fn();
  const mockSetError = jest.fn();
  const mockOnBack = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    jest.useFakeTimers();

    mockFetchLogContent.mockResolvedValue(mockLogContent);
    mockGetDownloadUrl.mockReturnValue('/service/rest/internal/logging/logs/nexus.log');

    mockedUseLogsApi.mockReturnValue({
      loading: false,
      error: null,
      setError: mockSetError,
      fetchLogs: jest.fn(),
      fetchLogContent: mockFetchLogContent,
      insertMark: mockInsertMark,
      getDownloadUrl: mockGetDownloadUrl,
    });
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('renders log viewer with filename in title', async () => {
    render(<LogViewer filename="nexus.log" onBack={mockOnBack} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Viewing nexus.log')).toBeInTheDocument();
    });
  });

  it('loads and displays log content', async () => {
    render(<LogViewer filename="nexus.log" onBack={mockOnBack} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(mockFetchLogContent).toHaveBeenCalledWith('nexus.log', -25600); // 25KB * -1024
    });

    await waitFor(() => {
      const textarea = screen.getByLabelText('Log content for nexus.log');
      expect(textarea).toHaveValue(mockLogContent);
    });
  });

  it('shows loading state while fetching content', () => {
    mockFetchLogContent.mockImplementation(() => new Promise(() => {}));

    render(<LogViewer filename="nexus.log" onBack={mockOnBack} />, { wrapper: TestWrapper });

    expect(screen.getByText('Loading log content...')).toBeInTheDocument();
  });

  it('has download button', async () => {
    render(<LogViewer filename="nexus.log" onBack={mockOnBack} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Download')).toBeInTheDocument();
    });
  });

  it('calls download function when download button is clicked', async () => {
    const { ExtJS } = require('@sonatype/nexus-ui-plugin');

    render(<LogViewer filename="nexus.log" onBack={mockOnBack} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Download')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('Download'));

    expect(ExtJS.downloadUrl).toHaveBeenCalled();
  });

  it('shows mark input for nexus.log', async () => {
    render(<LogViewer filename="nexus.log" onBack={mockOnBack} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Marker to insert:')).toBeInTheDocument();
      expect(screen.getByPlaceholderText('MARK')).toBeInTheDocument();
      expect(screen.getByText('Insert')).toBeInTheDocument();
    });
  });

  it('does not show mark input for other log files', async () => {
    render(<LogViewer filename="request.log" onBack={mockOnBack} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Viewing request.log')).toBeInTheDocument();
    });

    expect(screen.queryByText('Marker to insert:')).not.toBeInTheDocument();
  });

  it('inserts mark when insert button is clicked', async () => {
    mockInsertMark.mockResolvedValue(undefined);

    render(<LogViewer filename="nexus.log" onBack={mockOnBack} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByPlaceholderText('MARK')).toBeInTheDocument();
    });

    const markInput = screen.getByPlaceholderText('MARK');
    await act(async () => {
      fireEvent.change(markInput, { target: { value: 'TEST_MARK' } });
    });
    await act(async () => {
      fireEvent.click(screen.getByText('Insert'));
    });

    await waitFor(() => {
      expect(mockInsertMark).toHaveBeenCalledWith('TEST_MARK');
    });
  });

  it('inserts mark when Enter is pressed in mark input', async () => {
    mockInsertMark.mockResolvedValue(undefined);

    render(<LogViewer filename="nexus.log" onBack={mockOnBack} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByPlaceholderText('MARK')).toBeInTheDocument();
    });

    const markInput = screen.getByPlaceholderText('MARK');
    await act(async () => {
      fireEvent.change(markInput, { target: { value: 'TEST_MARK' } });
      fireEvent.keyDown(markInput, { key: 'Enter' });
    });

    await waitFor(() => {
      expect(mockInsertMark).toHaveBeenCalledWith('TEST_MARK');
    });
  });

  it('has refresh rate selector', async () => {
    render(<LogViewer filename="nexus.log" onBack={mockOnBack} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Refresh Rate:')).toBeInTheDocument();
    });
  });

  it('has log size selector', async () => {
    render(<LogViewer filename="nexus.log" onBack={mockOnBack} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Size:')).toBeInTheDocument();
    });
  });

  it('has size selector available', async () => {
    render(<LogViewer filename="nexus.log" onBack={mockOnBack} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Size:')).toBeInTheDocument();
    });

    // Size selector should be present (Radix UI Select interaction is complex in JSDOM)
    const comboboxes = screen.getAllByRole('combobox');
    expect(comboboxes.length).toBeGreaterThan(0);
  });

  it('displays error alert when fetch fails', async () => {
    mockedUseLogsApi.mockReturnValue({
      loading: false,
      error: 'Failed to load log content',
      setError: mockSetError,
      fetchLogs: jest.fn(),
      fetchLogContent: mockFetchLogContent.mockRejectedValue(new Error('Failed')),
      insertMark: mockInsertMark,
      getDownloadUrl: mockGetDownloadUrl,
    });

    render(<LogViewer filename="nexus.log" onBack={mockOnBack} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Failed to load log content')).toBeInTheDocument();
    });
  });

  it('decodes URL-encoded filenames', async () => {
    render(<LogViewer filename="test%20log.log" onBack={mockOnBack} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Viewing test log.log')).toBeInTheDocument();
    });
  });

  it('has refresh rate selector available', async () => {
    render(<LogViewer filename="nexus.log" onBack={mockOnBack} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Refresh Rate:')).toBeInTheDocument();
    });

    // Refresh rate selector should be present
    const comboboxes = screen.getAllByRole('combobox');
    expect(comboboxes.length).toBeGreaterThan(0);
  });
});

