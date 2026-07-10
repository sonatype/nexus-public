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

  it('does not render when health status is empty object (opt-out license)', () => {
    ExtJS.useUser.mockReturnValue({ administrator: true });
    ExtJS.state.mockReturnValue({
      getValue: jest.fn().mockReturnValue({}), // empty object = opt-out license
    });

    const { container } = render(<TelemetryWarningBanner />);
    expect(container.firstChild).toBeNull();
  });

  it('does not fetch tasks when banner is hidden (all flags false)', () => {
    ExtJS.useUser.mockReturnValue({ administrator: true });
    ExtJS.state.mockReturnValue({
      getValue: jest.fn().mockReturnValue({ showWarning: false, readOnly: false, introducedWarning: false }),
    });

    render(<TelemetryWarningBanner />);
    expect(restClient.get).not.toHaveBeenCalled();
  });

  it('renders with warning styling via SystemNotice', () => {
    ExtJS.useUser.mockReturnValue({ administrator: true });
    ExtJS.state.mockReturnValue({
      getValue: jest.fn().mockReturnValue({ showWarning: true }),
    });

    const { container } = render(<TelemetryWarningBanner />);
    expect(container.querySelector('.nx-alert--warning')).toBeInTheDocument();
  });

  it('renders with error styling for read-only mode via SystemNotice', () => {
    ExtJS.useUser.mockReturnValue({ administrator: true });
    ExtJS.state.mockReturnValue({
      getValue: jest.fn().mockReturnValue({ readOnly: true }),
    });

    const { container } = render(<TelemetryWarningBanner />);
    expect(container.querySelector('.nx-alert--error')).toBeInTheDocument();
  });

  it('renders with nxrm-telemetry-warning-banner class', () => {
    ExtJS.useUser.mockReturnValue({ administrator: true });
    ExtJS.state.mockReturnValue({
      getValue: jest.fn().mockReturnValue({ showWarning: true }),
    });

    const { container } = render(<TelemetryWarningBanner />);
    expect(container.querySelector('.nxrm-telemetry-warning-banner')).toBeInTheDocument();
  });

  it('link uses NxTextLink component', async () => {
    ExtJS.useUser.mockReturnValue({ administrator: true });
    ExtJS.state.mockReturnValue({
      getValue: jest.fn().mockReturnValue({ showWarning: true }),
    });

    render(<TelemetryWarningBanner />);
    const link = screen.getByRole('link', { name: /Go to Tasks/ });
    expect(link).toHaveClass('nx-text-link');
  });

  it('links to tasks list when no telemetry task exists', async () => {
    ExtJS.useUser.mockReturnValue({ administrator: true });
    ExtJS.state.mockReturnValue({
      getValue: jest.fn().mockReturnValue({ showWarning: true }),
    });
    restClient.get.mockResolvedValue({ items: [] });

    render(<TelemetryWarningBanner />);

    await waitFor(() => {
      const link = screen.getByRole('link', { name: /Go to Tasks/ });
      expect(link).toHaveAttribute('href', '#admin/system/tasks');
    });
  });

  it('links to tasks list when multiple telemetry tasks exist', async () => {
    ExtJS.useUser.mockReturnValue({ administrator: true });
    ExtJS.state.mockReturnValue({
      getValue: jest.fn().mockReturnValue({ showWarning: true }),
    });
    restClient.get.mockResolvedValue({
      items: [
        { id: 'task1', name: 'Telemetry Task 1', type: 'telemetry.upload.retry' },
        { id: 'task2', name: 'Telemetry Task 2', type: 'telemetry.upload.retry' },
      ],
    });

    render(<TelemetryWarningBanner />);

    await waitFor(() => {
      const link = screen.getByRole('link', { name: /Go to Tasks/ });
      expect(link).toHaveAttribute('href', '#admin/system/tasks');
    });
  });

  it('links to specific task when exactly one telemetry task exists', async () => {
    ExtJS.useUser.mockReturnValue({ administrator: true });
    ExtJS.state.mockReturnValue({
      getValue: jest.fn().mockReturnValue({ showWarning: true }),
    });
    restClient.get.mockResolvedValue({
      items: [
        { id: 'abc123', name: 'Telemetry Upload Retry', type: 'telemetry.upload.retry' },
        { id: 'xyz789', name: 'Some Other Task', type: 'other.type' },
      ],
    });

    render(<TelemetryWarningBanner />);

    await waitFor(() => {
      const link = screen.getByRole('link', { name: /Go to Tasks/ });
      expect(link).toHaveAttribute('href', '#admin/system/tasks:abc123');
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
      const link = screen.getByRole('link', { name: /Go to Tasks/ });
      expect(link).toHaveAttribute('href', '#admin/system/tasks');
    });
  });

  // Tests for {count} placeholder replacement with failedReportsThreshold
  describe('failedReportsThreshold placeholder', () => {
    it('displays custom threshold value in warning message', () => {
      ExtJS.useUser.mockReturnValue({ administrator: true });
      ExtJS.state.mockReturnValue({
        getValue: jest.fn().mockReturnValue({ showWarning: true, failedReportsThreshold: 7 }),
      });

      render(<TelemetryWarningBanner />);
      expect(screen.getByText(/failed for 7\+ days/)).toBeInTheDocument();
    });

    it('displays default threshold of 3 when failedReportsThreshold is undefined', () => {
      ExtJS.useUser.mockReturnValue({ administrator: true });
      ExtJS.state.mockReturnValue({
        getValue: jest.fn().mockReturnValue({ showWarning: true }),
      });

      render(<TelemetryWarningBanner />);
      expect(screen.getByText(/failed for 3\+ days/)).toBeInTheDocument();
    });

    it('displays custom threshold in introducedWarning mode', () => {
      ExtJS.useUser.mockReturnValue({ administrator: true });
      ExtJS.state.mockReturnValue({
        getValue: jest.fn().mockReturnValue({ introducedWarning: true, failedReportsThreshold: 10 }),
      });

      render(<TelemetryWarningBanner />);
      expect(screen.getByText(/failed for 10\+ consecutive days/)).toBeInTheDocument();
    });
  });

  // Tests for description content rendering
  describe('description content', () => {
    it('renders network configuration guidance in introducedWarning mode', () => {
      ExtJS.useUser.mockReturnValue({ administrator: true });
      ExtJS.state.mockReturnValue({
        getValue: jest.fn().mockReturnValue({ introducedWarning: true }),
      });

      render(<TelemetryWarningBanner />);
      expect(screen.getByText(/network configuration/)).toBeInTheDocument();
    });

    it('renders shared description in standard warning mode', () => {
      ExtJS.useUser.mockReturnValue({ administrator: true });
      ExtJS.state.mockReturnValue({
        getValue: jest.fn().mockReturnValue({ showWarning: true }),
      });

      render(<TelemetryWarningBanner />);
      expect(screen.getByText(/network configuration/)).toBeInTheDocument();
    });
  });

  // Tests for introducedWarning mode (TELEMETRY_MANDATORY_WARNING_ENABLED)
  describe('introducedWarning mode', () => {
    it('renders mandatory warning banner when introducedWarning is true', () => {
      ExtJS.useUser.mockReturnValue({ administrator: true });
      ExtJS.state.mockReturnValue({
        getValue: jest.fn().mockReturnValue({ introducedWarning: true, failedReportsThreshold: 3 }),
      });

      render(<TelemetryWarningBanner />);
      expect(screen.getByText(/Telemetry Required/)).toBeInTheDocument();
      expect(screen.getByText(/Telemetry will become mandatory/)).toBeInTheDocument();
    });

    it('renders mandatory warning banner with warning styling (not error)', () => {
      ExtJS.useUser.mockReturnValue({ administrator: true });
      ExtJS.state.mockReturnValue({
        getValue: jest.fn().mockReturnValue({ introducedWarning: true }),
      });

      const { container } = render(<TelemetryWarningBanner />);
      expect(container.querySelector('.nx-alert--warning')).toBeInTheDocument();
      expect(container.querySelector('.nx-alert--error')).not.toBeInTheDocument();
    });

    it('does not render when introducedWarning is false', () => {
      ExtJS.useUser.mockReturnValue({ administrator: true });
      ExtJS.state.mockReturnValue({
        getValue: jest.fn().mockReturnValue({ introducedWarning: false }),
      });

      const { container } = render(<TelemetryWarningBanner />);
      expect(container.firstChild).toBeNull();
    });
  });

  // Tests for read-only mode (TELEMETRY_MANDATORY_ENABLED)
  describe('read-only mode', () => {
    it('renders error banner when readOnly is true', () => {
      ExtJS.useUser.mockReturnValue({ administrator: true });
      ExtJS.state.mockReturnValue({
        getValue: jest.fn().mockReturnValue({ readOnly: true }),
      });

      const { container } = render(<TelemetryWarningBanner />);
      expect(container.querySelector('.nx-alert--error')).toBeInTheDocument();
      expect(screen.getByText(/Read-Only Mode/)).toBeInTheDocument();
    });

    it('read-only takes priority over warning', () => {
      ExtJS.useUser.mockReturnValue({ administrator: true });
      ExtJS.state.mockReturnValue({
        getValue: jest.fn().mockReturnValue({ showWarning: true, readOnly: true }),
      });

      const { container } = render(<TelemetryWarningBanner />);
      expect(container.querySelector('.nx-alert--error')).toBeInTheDocument();
      expect(container.querySelector('.nx-alert--warning')).not.toBeInTheDocument();
    });
  });
});
