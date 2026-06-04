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

import ProtectFilterSidebar, { type ProtectFilterState } from '../ProtectFilterSidebar';
import type { ProtectFilterCounts } from '../useProtectData';

const MOCK_COUNTS: ProtectFilterCounts = {
  formats: new Map([
    ['maven2', 5],
    ['npm', 3],
  ]),
  protection: new Map([
    ['quarantine', 2],
    ['audit', 3],
    ['none', 3],
  ]),
  healthCheck: { enabled: 4, disabled: 4 },
  cleanup: { delete: 2, audit: 0, off: 6 },
};

const EMPTY_FILTERS: ProtectFilterState = {
  format: [],
  protection: [],
  healthCheck: [],
  cleanup: [],
};

const renderWithTheme = (ui: React.ReactElement) => render(<Theme>{ui}</Theme>);

describe('ProtectFilterSidebar', () => {
  it('renders with Filters title', () => {
    renderWithTheme(
      <ProtectFilterSidebar
        counts={MOCK_COUNTS}
        value={EMPTY_FILTERS}
        onChange={jest.fn()}
        hasFirewall
      />
    );
    expect(screen.getByText('Filters')).toBeInTheDocument();
  });

  it('renders Format section with sorted format options', () => {
    renderWithTheme(
      <ProtectFilterSidebar
        counts={MOCK_COUNTS}
        value={EMPTY_FILTERS}
        onChange={jest.fn()}
        hasFirewall
      />
    );
    expect(screen.getByText('Format')).toBeInTheDocument();
    expect(screen.getByText('maven2')).toBeInTheDocument();
    expect(screen.getByText('npm')).toBeInTheDocument();
  });

  it('renders Protection section with human-readable labels', () => {
    renderWithTheme(
      <ProtectFilterSidebar
        counts={MOCK_COUNTS}
        value={EMPTY_FILTERS}
        onChange={jest.fn()}
        hasFirewall
      />
    );
    expect(screen.getByText('Protection')).toBeInTheDocument();
    expect(screen.getByText('Quarantine')).toBeInTheDocument();
    expect(screen.getByText('Audit')).toBeInTheDocument();
    expect(screen.getByText('None')).toBeInTheDocument();
  });

  it('renders Health Check section', () => {
    renderWithTheme(
      <ProtectFilterSidebar
        counts={MOCK_COUNTS}
        value={EMPTY_FILTERS}
        onChange={jest.fn()}
        hasFirewall
      />
    );
    expect(screen.getByText('Health Check')).toBeInTheDocument();
  });

  it('renders Auto Remediation section', () => {
    renderWithTheme(
      <ProtectFilterSidebar
        counts={MOCK_COUNTS}
        value={EMPTY_FILTERS}
        onChange={jest.fn()}
        hasFirewall
      />
    );
    expect(screen.getByText('Auto Remediation')).toBeInTheDocument();
  });

  it('accepts disabled prop', () => {
    renderWithTheme(
      <ProtectFilterSidebar
        counts={MOCK_COUNTS}
        value={EMPTY_FILTERS}
        onChange={jest.fn()}
        disabled
        hasFirewall
      />
    );
    expect(screen.getByText('Filters')).toBeInTheDocument();
  });
});
