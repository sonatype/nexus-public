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

import { NodeIcon } from '../NodeIcon';
import { FormatBadge } from '../FormatBadge';
import { StatusIndicator } from '../StatusIndicator';

// Wrapper component for Radix Theme context
const ThemeWrapper = ({ children }: { children: React.ReactNode }) => (
  <Theme>{children}</Theme>
);

const renderWithTheme = (ui: React.ReactElement) =>
  render(ui, { wrapper: ThemeWrapper });

// =============================================================================
// NodeIcon Tests
// =============================================================================
describe('NodeIcon', () => {
  it('renders folder icon for folder type', () => {
    const { container } = renderWithTheme(<NodeIcon type="folder" />);
    const svg = container.querySelector('svg');
    expect(svg).toBeInTheDocument();
    expect(svg).toHaveAttribute('aria-hidden', 'true');
  });

  it('renders package icon for component type', () => {
    const { container } = renderWithTheme(<NodeIcon type="component" />);
    const svg = container.querySelector('svg');
    expect(svg).toBeInTheDocument();
  });

  it('renders file icon for asset type', () => {
    const { container } = renderWithTheme(<NodeIcon type="asset" />);
    const svg = container.querySelector('svg');
    expect(svg).toBeInTheDocument();
  });

  it('applies custom size', () => {
    const { container } = renderWithTheme(<NodeIcon type="folder" size={24} />);
    const svg = container.querySelector('svg');
    expect(svg).toHaveAttribute('width', '24');
    expect(svg).toHaveAttribute('height', '24');
  });

  it('uses default size of 16', () => {
    const { container } = renderWithTheme(<NodeIcon type="folder" />);
    const svg = container.querySelector('svg');
    expect(svg).toHaveAttribute('width', '16');
    expect(svg).toHaveAttribute('height', '16');
  });

  it('applies custom className', () => {
    const { container } = renderWithTheme(<NodeIcon type="folder" className="custom-class" />);
    const svg = container.querySelector('svg');
    expect(svg).toHaveClass('custom-class');
  });

  it('renders folder icon with appropriate styling', () => {
    const { container } = renderWithTheme(<NodeIcon type="folder" />);
    const svg = container.querySelector('svg');
    expect(svg).toBeInTheDocument();
    // The Folder icon from Lucide
    expect(svg).toHaveClass('lucide-folder');
  });

  it('renders component icon with appropriate styling', () => {
    const { container } = renderWithTheme(<NodeIcon type="component" />);
    const svg = container.querySelector('svg');
    expect(svg).toBeInTheDocument();
    // The Package icon from Lucide
    expect(svg).toHaveClass('lucide-package');
  });

  it('renders asset icon with appropriate styling', () => {
    const { container } = renderWithTheme(<NodeIcon type="asset" />);
    const svg = container.querySelector('svg');
    expect(svg).toBeInTheDocument();
    // The File icon from Lucide
    expect(svg).toHaveClass('lucide-file');
  });
});

// =============================================================================
// FormatBadge Tests
// =============================================================================
describe('FormatBadge', () => {
  it('renders maven2 as maven', () => {
    renderWithTheme(<FormatBadge format="maven2" />);
    expect(screen.getByText('maven')).toBeInTheDocument();
  });

  it('renders npm format', () => {
    renderWithTheme(<FormatBadge format="npm" />);
    expect(screen.getByText('npm')).toBeInTheDocument();
  });

  it('renders docker format', () => {
    renderWithTheme(<FormatBadge format="docker" />);
    expect(screen.getByText('docker')).toBeInTheDocument();
  });

  it('renders pypi format', () => {
    renderWithTheme(<FormatBadge format="pypi" />);
    expect(screen.getByText('pypi')).toBeInTheDocument();
  });

  it('renders nuget format', () => {
    renderWithTheme(<FormatBadge format="nuget" />);
    expect(screen.getByText('nuget')).toBeInTheDocument();
  });

  it('renders unknown format', () => {
    renderWithTheme(<FormatBadge format="unknownformat" />);
    expect(screen.getByText('unknownformat')).toBeInTheDocument();
  });

  it('handles uppercase format', () => {
    renderWithTheme(<FormatBadge format="MAVEN2" />);
    expect(screen.getByText('maven')).toBeInTheDocument();
  });

  it('handles mixed case format', () => {
    renderWithTheme(<FormatBadge format="Docker" />);
    expect(screen.getByText('docker')).toBeInTheDocument();
  });

  it('renders raw format correctly', () => {
    renderWithTheme(<FormatBadge format="raw" />);
    expect(screen.getByText('raw')).toBeInTheDocument();
  });

  it('renders apt format', () => {
    renderWithTheme(<FormatBadge format="apt" />);
    expect(screen.getByText('apt')).toBeInTheDocument();
  });

  it('renders yum format', () => {
    renderWithTheme(<FormatBadge format="yum" />);
    expect(screen.getByText('yum')).toBeInTheDocument();
  });

  it('renders the badge correctly', () => {
    renderWithTheme(<FormatBadge format="npm" />);
    // Badge should render the format text
    expect(screen.getByText('npm')).toBeInTheDocument();
  });
});

// =============================================================================
// StatusIndicator Tests
// =============================================================================
describe('StatusIndicator', () => {
  it('renders status indicator for online status', () => {
    renderWithTheme(<StatusIndicator status={{ online: true }} />);
    const indicator = screen.getByTestId('status-indicator');
    expect(indicator).toBeInTheDocument();
    const circle = screen.getByTestId('status-indicator-circle');
    expect(circle).toBeInTheDocument();
  });

  it('renders status indicator for offline status', () => {
    renderWithTheme(<StatusIndicator status={{ online: false }} />);
    const indicator = screen.getByTestId('status-indicator');
    expect(indicator).toBeInTheDocument();
  });

  it('shows label when showLabel is true', () => {
    renderWithTheme(<StatusIndicator status={{ online: true }} showLabel />);
    expect(screen.getByText('Online')).toBeInTheDocument();
  });

  it('shows Offline label when offline and showLabel is true', () => {
    renderWithTheme(<StatusIndicator status={{ online: false }} showLabel />);
    expect(screen.getByText('Offline')).toBeInTheDocument();
  });

  it('hides label when showLabel is false', () => {
    renderWithTheme(<StatusIndicator status={{ online: true }} showLabel={false} />);
    expect(screen.queryByText('Online')).not.toBeInTheDocument();
  });

  it('hides label by default (showLabel not provided)', () => {
    renderWithTheme(<StatusIndicator status={{ online: true }} />);
    expect(screen.queryByText('Online')).not.toBeInTheDocument();
    expect(screen.queryByText('Offline')).not.toBeInTheDocument();
  });

  it('handles undefined status gracefully (defaults to offline)', () => {
    // @ts-expect-error - Testing undefined handling
    renderWithTheme(<StatusIndicator status={undefined} showLabel />);
    // Should show Offline when status is undefined
    expect(screen.getByText('Offline')).toBeInTheDocument();
  });

  it('handles status with only description', () => {
    renderWithTheme(
      <StatusIndicator status={{ online: true, description: 'Ready' }} showLabel />
    );
    expect(screen.getByText('Online')).toBeInTheDocument();
  });

  it('handles status with reason', () => {
    renderWithTheme(
      <StatusIndicator
        status={{ online: false, description: 'Unavailable', reason: 'Proxy connection failed' }}
        showLabel
      />
    );
    expect(screen.getByText('Offline')).toBeInTheDocument();
  });

  it('renders the circle icon', () => {
    renderWithTheme(<StatusIndicator status={{ online: true }} />);
    const circle = screen.getByTestId('status-indicator-circle');
    expect(circle).toBeInTheDocument();
    // The Circle component from lucide-react should be inside
    const svg = circle.querySelector('svg');
    expect(svg).toBeInTheDocument();
  });

  it('renders circle with aria-hidden for accessibility', () => {
    renderWithTheme(<StatusIndicator status={{ online: true }} />);
    const circle = screen.getByTestId('status-indicator-circle');
    const svg = circle.querySelector('svg');
    expect(svg).toHaveAttribute('aria-hidden', 'true');
  });
});

