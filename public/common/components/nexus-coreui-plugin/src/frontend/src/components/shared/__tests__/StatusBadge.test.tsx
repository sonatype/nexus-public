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
import { StatusBadge } from '../StatusBadge';

const renderWithTheme = (ui: React.ReactElement) => {
  return render(<Theme>{ui}</Theme>);
};

describe('StatusBadge', () => {
  it('renders online status with default label', () => {
    renderWithTheme(<StatusBadge status="online" />);

    expect(screen.getByText('Online')).toBeInTheDocument();
  });

  it('renders offline status with default label', () => {
    renderWithTheme(<StatusBadge status="offline" />);

    expect(screen.getByText('Offline')).toBeInTheDocument();
  });

  it('renders warning status', () => {
    renderWithTheme(<StatusBadge status="warning" />);

    expect(screen.getByText('Warning')).toBeInTheDocument();
  });

  it('renders unknown status', () => {
    renderWithTheme(<StatusBadge status="unknown" />);

    expect(screen.getByText('Unknown')).toBeInTheDocument();
  });

  it('renders custom label', () => {
    renderWithTheme(<StatusBadge status="online" label="Server Running" />);

    expect(screen.getByText('Server Running')).toBeInTheDocument();
  });

  it('renders description', () => {
    renderWithTheme(
      <StatusBadge status="online" description="All systems operational" />
    );

    expect(screen.getByText('- All systems operational')).toBeInTheDocument();
  });

  it('renders reason on second line', () => {
    renderWithTheme(
      <StatusBadge status="warning" reason="High memory usage detected" />
    );

    expect(screen.getByText('High memory usage detected')).toBeInTheDocument();
  });

  it('applies correct status class for online', () => {
    const { container } = renderWithTheme(<StatusBadge status="online" />);

    expect(container.querySelector('.status-badge--online')).toBeInTheDocument();
  });

  it('applies correct status class for offline', () => {
    const { container } = renderWithTheme(<StatusBadge status="offline" />);

    expect(container.querySelector('.status-badge--offline')).toBeInTheDocument();
  });

  it('applies small size class', () => {
    const { container } = renderWithTheme(
      <StatusBadge status="online" size="small" />
    );

    expect(container.querySelector('.status-badge--small')).toBeInTheDocument();
  });

  it('has accessible aria-label', () => {
    renderWithTheme(<StatusBadge status="online" />);

    expect(screen.getByLabelText('Status: Online')).toBeInTheDocument();
  });

  it('renders success status', () => {
    renderWithTheme(<StatusBadge status="success" />);

    expect(screen.getByText('Success')).toBeInTheDocument();
  });

  it('renders error status', () => {
    renderWithTheme(<StatusBadge status="error" />);

    expect(screen.getByText('Error')).toBeInTheDocument();
  });

  it('renders info status', () => {
    renderWithTheme(<StatusBadge status="info" />);

    expect(screen.getByText('Info')).toBeInTheDocument();
  });
});


