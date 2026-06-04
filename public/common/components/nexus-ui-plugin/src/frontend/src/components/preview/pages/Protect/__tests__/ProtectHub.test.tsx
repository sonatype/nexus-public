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
import { render, screen } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';

jest.mock('../../../../../interface/ExtJS', () => ({
  ExtJS: {
    useState: jest.fn(() => undefined),
    state: jest.fn(() => ({ getValue: jest.fn(() => ({ totalCount: 0 })) })),
    checkPermission: jest.fn(() => true),
  },
}));

jest.mock('../useProtectData', () => ({
  useProtectData: () => ({
    repos: [],
    loading: false,
    error: null,
    refetch: jest.fn(),
    filterCounts: {
      formats: new Map(),
      protection: new Map(),
      healthCheck: { enabled: 0, disabled: 0, unsupported: 0 },
      cleanup: { active: 0, off: 0 },
    },
    iqCapabilities: null,
    hasFirewall: false,
    hasIqConnection: false,
    canUpdateHealthCheck: false,
    iqAudit: { counts: null, loading: false, error: null },
    hcSummary: {
      loading: false,
      error: null,
      enabledCount: 0,
      totalProxyCount: 0,
      totalSecurityIssues: 0,
      totalLicenseIssues: 0,
      repos: [],
      refetch: jest.fn(),
    },
    hcInstanceEnabled: true,
    lastAnalyzedByRepo: new Map(),
  }),
}));

jest.mock('../../../shared/security/MalwareBanner', () => ({
  __esModule: true,
  default: () => null,
}));

jest.mock('../ProtectOverview', () => {
  const React = require('react');
  return {
    __esModule: true,
    default: () => React.createElement('div', { 'data-testid': 'protect-overview' }, 'Overview'),
  };
});

jest.mock('../ProtectQuickConfig', () => {
  const React = require('react');
  return {
    __esModule: true,
    default: () => React.createElement('div', { 'data-testid': 'protect-quick-config' }, 'Quick Config'),
  };
});

import ProtectHub from '../ProtectHub';

const renderWithTheme = (ui: React.ReactElement) => render(<Theme>{ui}</Theme>);

describe('ProtectHub', () => {
  it('renders Overview tab by default', () => {
    renderWithTheme(<ProtectHub />);
    expect(screen.getAllByText('Overview').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('Quick Config').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByTestId('protect-overview')).toBeInTheDocument();
  });
});
