/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 */

import React from 'react';
import { act, render, screen } from '@testing-library/react';
import TelemetryWarningBanner from '../TelemetryWarningBanner';

// Mock ExtJS + SystemAlert (SystemAlert is re-rendered here as a lightweight stub so we can assert props)
jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtJS: {
    useUser: jest.fn(),
    useState: jest.fn((fn) => fn()),
    state: jest.fn(),
  },
  // Use the real hook so the banner's links follow the actual URL hash
  useIsPreviewUI: jest.requireActual(
    '../../../../../../../../nexus-ui-plugin/src/frontend/src/components/preview/shared/Navigation/useIsPreviewUI'
  ).useIsPreviewUI,
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

const HELPER_LINK_TEXT = 'base telemetry';
const HELPER_LINK_HREF = 'https://links.sonatype.com/products/nxrm3/nexus-telemetry';

describe('TelemetryWarningBanner', () => {
  const { ExtJS } = require('@sonatype/nexus-ui-plugin');

  beforeEach(() => {
    jest.clearAllMocks();
    window.location.hash = '';
  });

  afterEach(() => {
    window.location.hash = '';
  });

  it('does not render for non-admin users', () => {
    ExtJS.useUser.mockReturnValue({ administrator: false });
    ExtJS.state.mockReturnValue({
      getValue: jest.fn().mockReturnValue({ showWarning: true }),
    });

    const { container } = render(<TelemetryWarningBanner />);
    expect(container.firstChild).toBeNull();
  });

  it('renders when showWarning is true', () => {
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

  describe('keeps the current UI', () => {
    const BANNERS = [
      { banner: 'Warning Banner', health: { showWarning: true }, title: 'Baseline Telemetry Upload Issue' },
      { banner: 'Read-Only Mode Banner', health: { readOnly: true }, title: 'Read-Only Mode Enabled' },
    ];

    const UIS = [
      { ui: 'Preview UI', hash: '#preview/browse/welcome', tasksHref: '#preview/admin/system/tasks' },
      { ui: 'Classic UI', hash: '#browse/welcome', tasksHref: '#admin/system/tasks' },
    ];

    function renderBanner(health: object, hash: string) {
      window.location.hash = hash;
      ExtJS.useUser.mockReturnValue({ administrator: true });
      ExtJS.state.mockReturnValue({ getValue: jest.fn().mockReturnValue(health) });
      return render(<TelemetryWarningBanner />);
    }

    describe.each(BANNERS)('$banner', ({ health, title }) => {
      describe.each(UIS)('from the $ui', ({ hash, tasksHref }) => {
        it('links "Go to Tasks" to the tasks list in the same UI', () => {
          renderBanner(health, hash);

          expect(screen.getByText(title)).toBeInTheDocument();
          expect(screen.getByRole('link', { name: /Go to Tasks/ })).toHaveAttribute('href', tasksHref);
        });

        it('leaves the external "base telemetry" link unchanged', () => {
          renderBanner(health, hash);

          const helperLink = screen.getByRole('link', { name: HELPER_LINK_TEXT });
          expect(helperLink).toHaveAttribute('href', HELPER_LINK_HREF);
          expect(helperLink).toHaveAttribute('target', '_blank');
          expect(helperLink).toHaveAttribute('rel', 'noopener noreferrer');
        });
      });
    });

    it('updates "Go to Tasks" when the user switches UI while the banner is shown', () => {
      renderBanner({ showWarning: true }, '#browse/welcome');
      expect(screen.getByRole('link', { name: /Go to Tasks/ })).toHaveAttribute('href', '#admin/system/tasks');

      act(() => {
        window.location.hash = '#preview/browse/welcome';
        window.dispatchEvent(new HashChangeEvent('hashchange'));
      });

      expect(screen.getByRole('link', { name: /Go to Tasks/ })).toHaveAttribute('href', '#preview/admin/system/tasks');
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
