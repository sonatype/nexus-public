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
import {render, screen} from '@testing-library/react';
import {Theme} from '@radix-ui/themes';
import '@testing-library/jest-dom';
import Welcome from '../Welcome';
import {isFeatureEnabled} from '../../../config/featureFlags';

jest.mock('../../../config/featureFlags', () => ({
  isFeatureEnabled: jest.fn(),
}));

const mockStateService = {go: jest.fn()};
let mockParams = {};
jest.mock('@uirouter/react', () => ({
  useRouter: () => ({
    globals: {params: mockParams},
    stateService: mockStateService,
  }),
}));

jest.mock('@xstate/react', () => ({
  useMachine: () => [
    {matches: () => false, context: {data: {}}},
    jest.fn(),
  ],
}));

jest.mock('../../../../../interface/ExtJS', () => ({
  ExtJS: {
    useUser: jest.fn().mockReturnValue({userId: 'admin', administrator: true}),
    useStatus: jest.fn().mockReturnValue({version: '3.90.0', edition: 'PRO'}),
    useLicense: jest.fn().mockReturnValue({daysToExpiry: 365}),
    // Mirror the real reactive hook: evaluate the getter immediately.
    usePermission: (fn) => fn(),
    checkPermission: jest.fn(() => true),
    state: jest.fn().mockReturnValue({
      getValue: jest.fn().mockReturnValue(false),
      getUser: jest.fn().mockReturnValue({id: 'admin'}),
    }),
  },
}));

jest.mock('../../../../../interface/urlUtil', () => ({toURIParams: () => ''}));
jest.mock('../../../../../interface/versionUtil', () => ({getVersionMajorMinor: () => '3.0'}));
jest.mock('../../../../pages/user/Welcome/WelcomeMachine', () => ({__esModule: true, default: {}}));

jest.mock('../../../shared', () => ({
  LoadingState: ({message}) => <div>{message}</div>,
  ErrorState: ({message}) => <div>{message}</div>,
  ErrorBoundary: ({children}) => <>{children}</>,
}));

jest.mock('../dashboard', () => ({
  RepositoriesByFormatPanel: () => <div data-testid="repos-by-format" />,
  QuickActionStatsPanel: () => <div data-testid="quick-action-stats" />,
  useRepositoriesByFormat: () => ({data: [], loading: false, error: null, refetch: jest.fn()}),
}));

jest.mock('../OutreachActions', () => {
  const {forwardRef} = require('react');
  return {
    __esModule: true,
    default: forwardRef(function MockOutreachActions(_props, _ref) {
      return <div data-testid="outreach-actions" />;
    }),
  };
});

jest.mock('../MalwareStatusCard', () => ({
  __esModule: true,
  default: () => <div data-testid="malware-card" />,
}));

jest.mock('../HealthCheckStatusCard', () => ({
  __esModule: true,
  default: () => <div data-testid="hc-card" />,
}));

jest.mock('../CELimitsAlerts', () => ({
  __esModule: true,
  default: () => <div data-testid="ce-limits-alerts">CE Limits Alerts</div>,
}));

jest.mock('../../../shared/security/MalwareBanner', () => ({
  __esModule: true,
  default: () => <div data-testid="malware-banner" />,
}));

jest.mock('../UsageMetricsTabContent', () => ({
  __esModule: true,
  default: () => <div data-testid="usage-metrics-tab-content">Usage Metrics Tab Content</div>,
}));

const {ExtJS} = require('../../../../../interface/ExtJS');

describe('Welcome', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockParams = {};
    ExtJS.checkPermission.mockReturnValue(true);
  });

  it('shows the Usage Metrics tab for an admin when the flag is enabled', () => {
    isFeatureEnabled.mockReturnValue(true);
    ExtJS.useUser.mockReturnValue({userId: 'admin', administrator: true});

    render(<Welcome />);

    expect(screen.getByRole('tab', {name: /Usage Metrics/})).toBeInTheDocument();
  });

  it('renders the CE alert exactly once inside the Usage Metrics tab', () => {
    isFeatureEnabled.mockReturnValue(true);
    ExtJS.useUser.mockReturnValue({userId: 'admin', administrator: true});
    mockParams = {tab: 'usage-metrics'};

    render(<Welcome />);

    expect(screen.getByTestId('usage-metrics-tab-content')).toBeInTheDocument();
    expect(screen.queryAllByTestId('ce-limits-alerts')).toHaveLength(0);
  });

  it('hides the Usage Metrics tab for a non-admin even when the flag is enabled', () => {
    isFeatureEnabled.mockReturnValue(true);
    ExtJS.useUser.mockReturnValue({userId: 'user', administrator: false});

    render(<Welcome />);

    expect(screen.queryByRole('tab', {name: 'Usage Metrics'})).not.toBeInTheDocument();
  });

  it('falls back to Overview when a non-admin deep-links to ?tab=usage-metrics', () => {
    isFeatureEnabled.mockReturnValue(true);
    ExtJS.useUser.mockReturnValue({userId: 'user', administrator: false});
    mockParams = {tab: 'usage-metrics'};

    render(<Welcome />);

    expect(screen.getByRole('tab', {name: /Overview/, selected: true})).toBeInTheDocument();
    expect(screen.queryByTestId('usage-metrics-tab-content')).not.toBeInTheDocument();
    // Non-admins see their CE alert on Overview instead (NEXUS-53219)
    expect(screen.getByTestId('ce-limits-alerts')).toBeInTheDocument();
  });

  it('corrects the URL to ?tab=overview when a non-admin deep-links to ?tab=usage-metrics', () => {
    isFeatureEnabled.mockReturnValue(true);
    ExtJS.useUser.mockReturnValue({userId: 'user', administrator: false});
    mockParams = {tab: 'usage-metrics'};

    render(<Welcome />);

    expect(mockStateService.go).toHaveBeenCalledWith(
      'preview.browse.welcome',
      {tab: 'overview'},
      {notify: false, location: 'replace'}
    );
  });

  it('does not redirect an admin whose user object is still resolving on first render', () => {
    isFeatureEnabled.mockReturnValue(true);
    ExtJS.useUser.mockReturnValue(undefined);
    mockParams = {tab: 'usage-metrics'};

    const {rerender} = render(<Welcome />);

    // Still resolving (isAuthenticated false): must not redirect a would-be admin away yet
    expect(mockStateService.go).not.toHaveBeenCalledWith(
      'preview.browse.welcome',
      {tab: 'overview'},
      expect.anything()
    );

    // User resolves to an admin
    ExtJS.useUser.mockReturnValue({userId: 'admin', administrator: true});
    rerender(<Welcome />);

    expect(mockStateService.go).not.toHaveBeenCalledWith(
      'preview.browse.welcome',
      {tab: 'overview'},
      expect.anything()
    );
    expect(screen.getByRole('tab', {name: /Usage Metrics/, selected: true})).toBeInTheDocument();
  });

  it('redirects to Overview once a resolving user settles as a non-admin', () => {
    isFeatureEnabled.mockReturnValue(true);
    ExtJS.useUser.mockReturnValue(undefined);
    mockParams = {tab: 'usage-metrics'};

    const {rerender} = render(<Welcome />);

    // Still resolving (isAuthenticated false): no redirect yet either way
    expect(mockStateService.go).not.toHaveBeenCalledWith(
      'preview.browse.welcome',
      {tab: 'overview'},
      expect.anything()
    );

    // User resolves to a non-admin
    ExtJS.useUser.mockReturnValue({userId: 'user', administrator: false});
    rerender(<Welcome />);

    expect(mockStateService.go).toHaveBeenCalledWith(
      'preview.browse.welcome',
      {tab: 'overview'},
      {notify: false, location: 'replace'}
    );
    expect(screen.getByRole('tab', {name: /Overview/, selected: true})).toBeInTheDocument();
  });

  it('hides the Usage Metrics tab entirely when the flag is disabled', () => {
    isFeatureEnabled.mockReturnValue(false);
    ExtJS.useUser.mockReturnValue({userId: 'admin', administrator: true});

    render(<Welcome />);

    expect(screen.queryByRole('tab', {name: 'Usage Metrics'})).not.toBeInTheDocument();
  });
});

// NEXUS-54212: The dashboard "Repository Security" section renders a Health Check card
// backed by GET /service/rest/internal/ui/healthcheck/summary, which requires
// nexus:healthcheck:read. Without it the card surfaced a raw "Request failed with status
// code 403". The section must be hidden entirely for users lacking that permission, so it
// matches Classic (which never shows repository-security status to unpermitted users).
describe('Welcome dashboard — Repository Security section gating', () => {
  const renderWelcome = () => render(<Theme><Welcome /></Theme>);

  beforeEach(() => {
    jest.clearAllMocks();
    mockParams = {};
    ExtJS.useUser.mockReturnValue({userId: 'admin', administrator: true});
    ExtJS.checkPermission.mockReturnValue(true);
    isFeatureEnabled.mockReturnValue(false);
  });

  it('shows the Repository Security section when the user has nexus:healthcheck:read', () => {
    ExtJS.checkPermission.mockReturnValue(true);

    renderWelcome();

    expect(screen.getByText('Repository Security')).toBeInTheDocument();
    expect(screen.getByTestId('hc-card')).toBeInTheDocument();
    expect(screen.getByTestId('malware-card')).toBeInTheDocument();
  });

  it('hides the Repository Security section when the user lacks nexus:healthcheck:read', () => {
    ExtJS.checkPermission.mockReturnValue(false);

    renderWelcome();

    expect(screen.queryByText('Repository Security')).not.toBeInTheDocument();
    expect(screen.queryByTestId('hc-card')).not.toBeInTheDocument();
    expect(screen.queryByTestId('malware-card')).not.toBeInTheDocument();
    // The Malware banner is separate from the gated section and remains visible.
    expect(screen.getByTestId('malware-banner')).toBeInTheDocument();
  });
});
