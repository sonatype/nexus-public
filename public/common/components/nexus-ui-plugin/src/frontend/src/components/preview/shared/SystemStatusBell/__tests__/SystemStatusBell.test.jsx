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
import {render, screen, fireEvent} from '@testing-library/react';
import {Theme} from '@radix-ui/themes';
import '@testing-library/jest-dom';

const mockHealthChecksFailed = {value: false};
const mockUseIsPreviewUI = jest.fn(() => false);
const mockUseIsVisible = jest.fn(() => true);
const mockUseUnreadStatusFailure = jest.fn();
const mockMarkAcknowledged = jest.fn();
const mockStateServiceGo = jest.fn();
const mockStateRegistryGet = jest.fn(() => ({
  data: {visibilityRequirements: {requiresPermission: 'nexus:metrics:read'}},
}));

jest.mock('../../../../../interface/ExtJS', () => ({
  ExtJS: {
    useState: (fn) => fn(),
    state: () => ({
      getValue: (key, defaultVal) =>
        key === 'health_checks_failed' ? mockHealthChecksFailed.value : defaultVal,
    }),
  },
}));

jest.mock('../../../../../interface/NavigationUtils', () => ({
  useIsVisible: (...args) => mockUseIsVisible(...args),
}));

jest.mock('../../Navigation', () => ({
  useIsPreviewUI: (...args) => mockUseIsPreviewUI(...args),
}));

jest.mock('../../hooks', () => ({
  useUnreadStatusFailure: (...args) => mockUseUnreadStatusFailure(...args),
}));

jest.mock('@uirouter/react', () => ({
  useRouter: jest.fn(() => ({
    stateService: {go: mockStateServiceGo},
    stateRegistry: {get: mockStateRegistryGet},
  })),
}));

import SystemStatusBell from '../SystemStatusBell';

function renderBell(props = {}) {
  return render(
    <Theme>
      <SystemStatusBell className="nxrm-system-status-radix" {...props} />
    </Theme>,
  );
}

describe('SystemStatusBell', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockHealthChecksFailed.value = false;
    mockUseIsPreviewUI.mockReturnValue(false);
    mockUseIsVisible.mockReturnValue(true);
    mockUseUnreadStatusFailure.mockReturnValue({
      showDot: false,
      markAcknowledged: mockMarkAcknowledged,
    });
    mockStateRegistryGet.mockReturnValue({
      data: {visibilityRequirements: {requiresPermission: 'nexus:metrics:read'}},
    });
  });

  it('navigates to the Preview metric-health state when clicked in Nexus One UI', () => {
    mockUseIsPreviewUI.mockReturnValue(true);

    renderBell();
    fireEvent.click(screen.getByLabelText('System Status'));

    expect(mockStateRegistryGet).toHaveBeenCalledWith('preview.admin.support.metrichealth');
    expect(mockUseIsVisible).toHaveBeenCalledWith(
      expect.objectContaining({requiresPermission: 'nexus:metrics:read'}),
    );
    expect(mockStateServiceGo).toHaveBeenCalledWith('preview.admin.support.metrichealth');
  });

  it('navigates to the Classic support-status state when clicked in Classic UI', () => {
    mockUseIsPreviewUI.mockReturnValue(false);

    renderBell();
    fireEvent.click(screen.getByLabelText('System Status'));

    expect(mockStateRegistryGet).toHaveBeenCalledWith('admin.support.status');
    expect(mockUseIsVisible).toHaveBeenCalledWith(
      expect.objectContaining({requiresPermission: 'nexus:metrics:read'}),
    );
    expect(mockStateServiceGo).toHaveBeenCalledWith('admin.support.status');
  });

  it('renders nothing when the target state is not registered', () => {
    mockStateRegistryGet.mockReturnValue(undefined);

    const {container} = renderBell();

    expect(container.querySelector('.nxrm-system-status-radix')).toBeNull();
    expect(screen.queryByLabelText('System Status')).not.toBeInTheDocument();
  });

  it('renders nothing when the target state is not visible to the user', () => {
    mockUseIsVisible.mockReturnValue(false);

    const {container} = renderBell();

    expect(container.querySelector('.nxrm-system-status-radix')).toBeNull();
    expect(screen.queryByLabelText('System Status')).not.toBeInTheDocument();
  });

  it('passes the current health-check state to useUnreadStatusFailure', () => {
    mockHealthChecksFailed.value = true;

    renderBell();

    expect(mockUseUnreadStatusFailure).toHaveBeenLastCalledWith(true);
  });

  it('does not render the red dot when the hook reports no unread failure', () => {
    mockUseUnreadStatusFailure.mockReturnValue({
      showDot: false,
      markAcknowledged: mockMarkAcknowledged,
    });

    const {container} = renderBell();

    expect(container.querySelector('.nxrm-system-status-radix__badge')).toBeNull();
    expect(screen.getByLabelText('System Status')).toBeInTheDocument();
  });

  it('renders the red dot and unhealthy aria-label when the hook reports an unread failure', () => {
    mockUseUnreadStatusFailure.mockReturnValue({
      showDot: true,
      markAcknowledged: mockMarkAcknowledged,
    });

    const {container} = renderBell();

    expect(container.querySelector('.nxrm-system-status-radix__badge')).not.toBeNull();
    expect(screen.getByLabelText('System status -- unhealthy')).toBeInTheDocument();
  });

  it('calls markAcknowledged before navigating when clicked while unhealthy', () => {
    mockUseUnreadStatusFailure.mockReturnValue({
      showDot: true,
      markAcknowledged: mockMarkAcknowledged,
    });

    renderBell();
    fireEvent.click(screen.getByLabelText('System status -- unhealthy'));

    expect(mockMarkAcknowledged).toHaveBeenCalledTimes(1);
    expect(mockStateServiceGo).toHaveBeenCalledWith('admin.support.status');
    const ackOrder = mockMarkAcknowledged.mock.invocationCallOrder[0];
    const goOrder = mockStateServiceGo.mock.invocationCallOrder[0];
    expect(ackOrder).toBeLessThan(goOrder);
  });

  it('applies data-analytics-id when the prop is provided', () => {
    renderBell({dataAnalyticsId: 'nxrm-global-header-system-status'});

    expect(screen.getByLabelText('System Status')).toHaveAttribute(
      'data-analytics-id',
      'nxrm-global-header-system-status',
    );
  });

  it('omits data-analytics-id when the prop is not provided', () => {
    renderBell();

    expect(screen.getByLabelText('System Status')).not.toHaveAttribute('data-analytics-id');
  });
});
