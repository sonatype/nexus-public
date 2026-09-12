/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 */

import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import TelemetryWarningBanner from '../TelemetryWarningBanner';

// Mock ExtJS + SystemAlert (SystemAlert is re-rendered here as a lightweight stub so we can assert props)
jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtJS: {
    useUser: jest.fn(),
    useState: jest.fn((fn) => fn()),
    state: jest.fn(),
  },
  SystemAlert: ({tier, title, message, dismissable}: any) => (
    <div
      data-testid="nxrm-system-alert"
      data-tier={tier}
      data-dismissable={dismissable ? 'true' : 'false'}
      role={tier === 'error' ? 'alert' : 'status'}
    >
      {title && <div data-testid="nxrm-system-alert-title">{title}</div>}
      <div data-testid="nxrm-system-alert-message">{message}</div>
    </div>
  ),
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

const HELPER_LINK_TEXT = 'base telemetry';
const HELPER_LINK_HREF = 'https://links.sonatype.com/products/nxrm3/nexus-telemetry';

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
    expect(screen.getByRole('link', { name: HELPER_LINK_TEXT })).toBeInTheDocument();
    expect(screen.getByText('Baseline Telemetry Upload Issue')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Go to Tasks/ })).toBeInTheDocument();
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

  it('does not fetch tasks when banner is hidden', () => {
    ExtJS.useUser.mockReturnValue({ administrator: true });
    ExtJS.state.mockReturnValue({
      getValue: jest.fn().mockReturnValue({ showWarning: false, readOnly: false, introducedWarning: false }),
    });

    render(<TelemetryWarningBanner />);
    expect(restClient.get).not.toHaveBeenCalled();
  });

  it('renders with warning tier when showWarning is true', () => {
    ExtJS.useUser.mockReturnValue({ administrator: true });
    ExtJS.state.mockReturnValue({
      getValue: jest.fn().mockReturnValue({ showWarning: true }),
    });

    render(<TelemetryWarningBanner />);
    expect(screen.getByTestId('nxrm-system-alert')).toHaveAttribute('data-tier', 'warning');
  });

  it('renders with error tier when readOnly is true', () => {
    ExtJS.useUser.mockReturnValue({ administrator: true });
    ExtJS.state.mockReturnValue({
      getValue: jest.fn().mockReturnValue({ readOnly: true }),
    });

    render(<TelemetryWarningBanner />);
    expect(screen.getByTestId('nxrm-system-alert')).toHaveAttribute('data-tier', 'error');
  });

  it('renders with warning tier when introducedWarning is true', () => {
    ExtJS.useUser.mockReturnValue({ administrator: true });
    ExtJS.state.mockReturnValue({
      getValue: jest.fn().mockReturnValue({ introducedWarning: true }),
    });

    render(<TelemetryWarningBanner />);
    expect(screen.getByTestId('nxrm-system-alert')).toHaveAttribute('data-tier', 'warning');
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

  it('links to tasks list on API error', async () => {
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

  describe('failedReportDays placeholder', () => {
    it('displays failed report days count in warning message', () => {
      ExtJS.useUser.mockReturnValue({ administrator: true });
      ExtJS.state.mockReturnValue({
        getValue: jest.fn().mockReturnValue({ introducedWarning: true, failedReportDays: 7 }),
      });

      render(<TelemetryWarningBanner />);
      expect(screen.getByText(/past 7 days/)).toBeInTheDocument();
    });

    it('displays default of 0 when failedReportDays is undefined', () => {
      ExtJS.useUser.mockReturnValue({ administrator: true });
      ExtJS.state.mockReturnValue({
        getValue: jest.fn().mockReturnValue({ introducedWarning: true }),
      });

      render(<TelemetryWarningBanner />);
      expect(screen.getByText(/past 0 days/)).toBeInTheDocument();
    });

    it('displays custom count in introducedWarning mode', () => {
      ExtJS.useUser.mockReturnValue({ administrator: true });
      ExtJS.state.mockReturnValue({
        getValue: jest.fn().mockReturnValue({ introducedWarning: true, failedReportDays: 10 }),
      });

      render(<TelemetryWarningBanner />);
      expect(screen.getByText(/past 10 days/)).toBeInTheDocument();
    });
  });

  describe('remainingGracePeriodDays placeholder', () => {
    it('renders the remaining grace period in bold in showWarning mode', () => {
      ExtJS.useUser.mockReturnValue({ administrator: true });
      ExtJS.state.mockReturnValue({
        getValue: jest.fn().mockReturnValue({ showWarning: true, remainingGracePeriodDays: 5 }),
      });

      render(<TelemetryWarningBanner />);
      const bold = screen.getByText('read-only mode in 5 day(s)');
      expect(bold.tagName).toBe('STRONG');
    });

    it('defaults to 0 when remainingGracePeriodDays is undefined', () => {
      ExtJS.useUser.mockReturnValue({ administrator: true });
      ExtJS.state.mockReturnValue({
        getValue: jest.fn().mockReturnValue({ showWarning: true }),
      });

      render(<TelemetryWarningBanner />);
      expect(screen.getByText('read-only mode in 0 day(s)')).toBeInTheDocument();
    });
  });

  describe('description content', () => {
    it('renders reachability guidance in introducedWarning mode', () => {
      ExtJS.useUser.mockReturnValue({ administrator: true });
      ExtJS.state.mockReturnValue({
        getValue: jest.fn().mockReturnValue({ introducedWarning: true }),
      });

      render(<TelemetryWarningBanner />);
      expect(
        screen.getByText(/Check that your server can reach the Sonatype telemetry service/)
      ).toBeInTheDocument();
      expect(screen.getByText(/contact Sonatype Support/)).toBeInTheDocument();
    });

    it('renders shared description in standard warning mode', () => {
      ExtJS.useUser.mockReturnValue({ administrator: true });
      ExtJS.state.mockReturnValue({
        getValue: jest.fn().mockReturnValue({ showWarning: true }),
      });

      render(<TelemetryWarningBanner />);
      expect(screen.getByText(/network configuration/)).toBeInTheDocument();
      expect(screen.getByText(/run the Upload Retry task/)).toBeInTheDocument();
    });

    it('renders shared description in read-only mode', () => {
      ExtJS.useUser.mockReturnValue({ administrator: true });
      ExtJS.state.mockReturnValue({
        getValue: jest.fn().mockReturnValue({ readOnly: true }),
      });

      render(<TelemetryWarningBanner />);
      expect(screen.getByText(/network configuration/)).toBeInTheDocument();
      expect(screen.getByText(/could not be uploaded within the grace period/)).toBeInTheDocument();
    });
  });

  describe('introducedWarning mode', () => {
    it('renders warning banner when introducedWarning is true', () => {
      ExtJS.useUser.mockReturnValue({ administrator: true });
      ExtJS.state.mockReturnValue({
        getValue: jest.fn().mockReturnValue({ introducedWarning: true, failedReportDays: 3 }),
      });

      render(<TelemetryWarningBanner />);
      expect(screen.getByRole('link', { name: HELPER_LINK_TEXT })).toBeInTheDocument();
      expect(screen.getByText('Baseline Telemetry Upload Issue')).toBeInTheDocument();
      expect(screen.getByText(/past 3 days/)).toBeInTheDocument();
    });

    it('renders with warning tier (not error)', () => {
      ExtJS.useUser.mockReturnValue({ administrator: true });
      ExtJS.state.mockReturnValue({
        getValue: jest.fn().mockReturnValue({ introducedWarning: true }),
      });

      render(<TelemetryWarningBanner />);
      const alert = screen.getByTestId('nxrm-system-alert');
      expect(alert).toHaveAttribute('data-tier', 'warning');
      expect(alert).not.toHaveAttribute('data-tier', 'error');
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

  describe('read-only mode', () => {
    it('renders error banner when readOnly is true', () => {
      ExtJS.useUser.mockReturnValue({ administrator: true });
      ExtJS.state.mockReturnValue({
        getValue: jest.fn().mockReturnValue({ readOnly: true }),
      });

      render(<TelemetryWarningBanner />);
      expect(screen.getByTestId('nxrm-system-alert')).toHaveAttribute('data-tier', 'error');
      expect(screen.getByText(/Read-Only Mode/)).toBeInTheDocument();
    });

    it('read-only takes priority over warning', () => {
      ExtJS.useUser.mockReturnValue({ administrator: true });
      ExtJS.state.mockReturnValue({
        getValue: jest.fn().mockReturnValue({ showWarning: true, readOnly: true }),
      });

      render(<TelemetryWarningBanner />);
      expect(screen.getByTestId('nxrm-system-alert')).toHaveAttribute('data-tier', 'error');
      expect(screen.getByText(/Read-Only Mode/)).toBeInTheDocument();
    });
  });

  describe('dismissable', () => {
    it('is dismissable in showWarning mode', () => {
      ExtJS.useUser.mockReturnValue({ administrator: true });
      ExtJS.state.mockReturnValue({
        getValue: jest.fn().mockReturnValue({ showWarning: true }),
      });

      render(<TelemetryWarningBanner />);
      expect(screen.getByTestId('nxrm-system-alert')).toHaveAttribute('data-dismissable', 'true');
    });

    it('is dismissable in introducedWarning mode', () => {
      ExtJS.useUser.mockReturnValue({ administrator: true });
      ExtJS.state.mockReturnValue({
        getValue: jest.fn().mockReturnValue({ introducedWarning: true }),
      });

      render(<TelemetryWarningBanner />);
      expect(screen.getByTestId('nxrm-system-alert')).toHaveAttribute('data-dismissable', 'true');
    });

    it('is NOT dismissable in read-only mode', () => {
      ExtJS.useUser.mockReturnValue({ administrator: true });
      ExtJS.state.mockReturnValue({
        getValue: jest.fn().mockReturnValue({ readOnly: true }),
      });

      render(<TelemetryWarningBanner />);
      expect(screen.getByTestId('nxrm-system-alert')).toHaveAttribute('data-dismissable', 'false');
    });
  });

  describe('accessibility roles', () => {
    it('has role="alert" when readOnly is true', () => {
      ExtJS.useUser.mockReturnValue({ administrator: true });
      ExtJS.state.mockReturnValue({
        getValue: jest.fn().mockReturnValue({ readOnly: true }),
      });

      render(<TelemetryWarningBanner />);
      expect(screen.getByRole('alert')).toBeInTheDocument();
    });

    it('has role="status" for warning modes', () => {
      ExtJS.useUser.mockReturnValue({ administrator: true });
      ExtJS.state.mockReturnValue({
        getValue: jest.fn().mockReturnValue({ showWarning: true }),
      });

      render(<TelemetryWarningBanner />);
      expect(screen.getByRole('status')).toBeInTheDocument();
    });
  });

  describe('helper link', () => {
    it('renders "base telemetry" as a link in the message for warning mode', () => {
      ExtJS.useUser.mockReturnValue({ administrator: true });
      ExtJS.state.mockReturnValue({
        getValue: jest.fn().mockReturnValue({ showWarning: true }),
      });

      render(<TelemetryWarningBanner />);
      const helperLink = screen.getByRole('link', { name: HELPER_LINK_TEXT });
      expect(helperLink).toBeInTheDocument();
      expect(helperLink).toHaveAttribute('href', HELPER_LINK_HREF);
      expect(helperLink).toHaveAttribute('data-analytics-id', 'nxrm-telemetry-message-link');
      expect(screen.getByText('Baseline Telemetry Upload Issue')).toBeInTheDocument();
    });

    it('renders "base telemetry" as a link in the message for introducedWarning mode', () => {
      ExtJS.useUser.mockReturnValue({ administrator: true });
      ExtJS.state.mockReturnValue({
        getValue: jest.fn().mockReturnValue({ introducedWarning: true }),
      });

      render(<TelemetryWarningBanner />);
      const helperLink = screen.getByRole('link', { name: HELPER_LINK_TEXT });
      expect(helperLink).toBeInTheDocument();
      expect(helperLink).toHaveAttribute('href', HELPER_LINK_HREF);
      expect(helperLink).toHaveAttribute('data-analytics-id', 'nxrm-telemetry-message-link');
      expect(screen.getByText('Baseline Telemetry Upload Issue')).toBeInTheDocument();
    });

    it('renders "base telemetry" as a link in the message for read-only mode', () => {
      ExtJS.useUser.mockReturnValue({ administrator: true });
      ExtJS.state.mockReturnValue({
        getValue: jest.fn().mockReturnValue({ readOnly: true }),
      });

      render(<TelemetryWarningBanner />);
      expect(screen.getByText('Read-Only Mode Enabled')).toBeInTheDocument();
      const helperLink = screen.getByRole('link', { name: HELPER_LINK_TEXT });
      expect(helperLink).toBeInTheDocument();
      expect(helperLink).toHaveAttribute('href', HELPER_LINK_HREF);
    });

    it('renders exactly one helper link even though the message mentions telemetry twice', () => {
      ExtJS.useUser.mockReturnValue({ administrator: true });
      ExtJS.state.mockReturnValue({
        getValue: jest.fn().mockReturnValue({ showWarning: true }),
      });

      render(<TelemetryWarningBanner />);
      expect(screen.getAllByRole('link', { name: HELPER_LINK_TEXT })).toHaveLength(1);
    });

    it('helper link has external attributes', () => {
      ExtJS.useUser.mockReturnValue({ administrator: true });
      ExtJS.state.mockReturnValue({
        getValue: jest.fn().mockReturnValue({ showWarning: true }),
      });

      render(<TelemetryWarningBanner />);
      const helperLink = screen.getByRole('link', { name: HELPER_LINK_TEXT });
      expect(helperLink).toHaveAttribute('target', '_blank');
      expect(helperLink).toHaveAttribute('rel', 'noopener noreferrer');
    });
  });
});
