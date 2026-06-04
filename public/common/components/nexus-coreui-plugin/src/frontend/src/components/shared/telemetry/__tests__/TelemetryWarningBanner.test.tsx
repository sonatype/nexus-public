/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 */

import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import TelemetryWarningBanner from '../TelemetryWarningBanner';

// Mock ExtJS
jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtJS: {
    useUser: jest.fn(),
    useState: jest.fn((fn) => fn()),
    state: jest.fn(),
  },
}));

// Mock API modules
jest.mock('@/utils/api', () => ({
  restClient: {
    get: jest.fn(),
  },
  urlBuilder: {
    tasks: {
      list: jest.fn(() => '/service/rest/v1/tasks'),
    },
  },
}));

describe('TelemetryWarningBanner', () => {
  const { ExtJS } = require('@sonatype/nexus-ui-plugin');
  const { restClient } = require('@/utils/api');

  beforeEach(() => {
    jest.clearAllMocks();
    restClient.get.mockResolvedValue({ items: [] });
  });

  it('does not render for non-admin users', () => {
    ExtJS.useUser.mockReturnValue({ administrator: false });
    ExtJS.state.mockReturnValue({
      getValue: jest.fn().mockReturnValue({ showWarning: true }),
    });

    const { container } = render(<TelemetryWarningBanner />);
    expect(container.firstChild).toBeNull();
  });

  it('renders when showWarning is true', async () => {
    ExtJS.useUser.mockReturnValue({ administrator: true });
    ExtJS.state.mockReturnValue({
      getValue: jest.fn().mockReturnValue({ showWarning: true }),
    });

    render(<TelemetryWarningBanner />);
    expect(screen.getByText(/Telemetry Required/)).toBeInTheDocument();
    expect(screen.getByText('Go to Tasks')).toBeInTheDocument();
    expect(screen.getByText(/Telemetry Upload Retry/)).toBeInTheDocument();
  });

  it('does not render when showWarning is false', () => {
    ExtJS.useUser.mockReturnValue({ administrator: true });
    ExtJS.state.mockReturnValue({
      getValue: jest.fn().mockReturnValue({ showWarning: false }),
    });

    const { container } = render(<TelemetryWarningBanner />);
    expect(container.firstChild).toBeNull();
  });

  it('does not render when telemetry health is undefined', () => {
    ExtJS.useUser.mockReturnValue({ administrator: true });
    ExtJS.state.mockReturnValue({
      getValue: jest.fn().mockReturnValue(undefined),
    });

    const { container } = render(<TelemetryWarningBanner />);
    expect(container.firstChild).toBeNull();
  });

  it('renders with NxWarningAlert styling', () => {
    ExtJS.useUser.mockReturnValue({ administrator: true });
    ExtJS.state.mockReturnValue({
      getValue: jest.fn().mockReturnValue({ showWarning: true }),
    });

    const { container } = render(<TelemetryWarningBanner />);
    expect(container.querySelector('.nx-alert--warning')).toBeInTheDocument();
  });

  it('button has primary styling', () => {
    ExtJS.useUser.mockReturnValue({ administrator: true });
    ExtJS.state.mockReturnValue({
      getValue: jest.fn().mockReturnValue({ showWarning: true }),
    });

    render(<TelemetryWarningBanner />);
    const button = screen.getByText('Go to Tasks');
    expect(button).toHaveClass('nx-btn--primary');
  });

  it('links to tasks list when no telemetry task exists', async () => {
    ExtJS.useUser.mockReturnValue({ administrator: true });
    ExtJS.state.mockReturnValue({
      getValue: jest.fn().mockReturnValue({ showWarning: true }),
    });
    restClient.get.mockResolvedValue({ items: [] });

    render(<TelemetryWarningBanner />);

    await waitFor(() => {
      const button = screen.getByText('Go to Tasks');
      expect(button).toHaveAttribute('href', '#admin/system/tasks');
    });
  });

  it('links to tasks list when multiple telemetry tasks exist', async () => {
    ExtJS.useUser.mockReturnValue({ administrator: true });
    ExtJS.state.mockReturnValue({
      getValue: jest.fn().mockReturnValue({ showWarning: true }),
    });
    // REST API returns 'type' field, not 'typeId'
    restClient.get.mockResolvedValue({
      items: [
        { id: 'task1', name: 'Telemetry Task 1', type: 'telemetry.upload.retry' },
        { id: 'task2', name: 'Telemetry Task 2', type: 'telemetry.upload.retry' },
      ],
    });

    render(<TelemetryWarningBanner />);

    await waitFor(() => {
      const button = screen.getByText('Go to Tasks');
      expect(button).toHaveAttribute('href', '#admin/system/tasks');
    });
  });

  it('links to specific task when exactly one telemetry task exists', async () => {
    ExtJS.useUser.mockReturnValue({ administrator: true });
    ExtJS.state.mockReturnValue({
      getValue: jest.fn().mockReturnValue({ showWarning: true }),
    });
    // REST API returns 'type' field, not 'typeId'
    restClient.get.mockResolvedValue({
      items: [
        { id: 'abc123', name: 'Telemetry Upload Retry', type: 'telemetry.upload.retry' },
        { id: 'xyz789', name: 'Some Other Task', type: 'other.type' },
      ],
    });

    render(<TelemetryWarningBanner />);

    await waitFor(() => {
      const button = screen.getByText('Go to Tasks');
      expect(button).toHaveAttribute('href', '#admin/system/tasks:abc123');
    });
  });

  it('links to tasks list when API call fails', async () => {
    ExtJS.useUser.mockReturnValue({ administrator: true });
    ExtJS.state.mockReturnValue({
      getValue: jest.fn().mockReturnValue({ showWarning: true }),
    });
    restClient.get.mockRejectedValue(new Error('Network error'));

    render(<TelemetryWarningBanner />);

    await waitFor(() => {
      const button = screen.getByText('Go to Tasks');
      expect(button).toHaveAttribute('href', '#admin/system/tasks');
    });
  });
});
