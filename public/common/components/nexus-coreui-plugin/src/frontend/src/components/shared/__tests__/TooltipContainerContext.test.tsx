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
import { TooltipContainerProvider, usePortalContainer, Tooltip } from '../Tooltip/TooltipContainerContext';

// Test component to access and display the portal container
function PortalContainerConsumer() {
  const container = usePortalContainer();
  return (
    <div data-testid="consumer">
      {container ? 'has-container' : 'no-container'}
    </div>
  );
}

const renderWithTheme = (ui: React.ReactElement) => {
  return render(<Theme>{ui}</Theme>);
};

describe('TooltipContainerProvider', () => {
  it('renders a portal container with z-index 14999', () => {
    const { container } = renderWithTheme(
      <TooltipContainerProvider>
        <div data-testid="child">Child content</div>
      </TooltipContainerProvider>
    );

    // The portal container is the first div child with aria-hidden attribute
    const portalContainer = container.querySelector('[aria-hidden="true"]');
    expect(portalContainer).toBeInTheDocument();

    // Verify z-index is 14999 (above ExtJS animating components at 10000, below masks at 20000+)
    expect(portalContainer).toHaveStyle({ zIndex: '14999' });
  });

  it('renders children correctly', () => {
    renderWithTheme(
      <TooltipContainerProvider>
        <div data-testid="child">Child content</div>
      </TooltipContainerProvider>
    );

    expect(screen.getByTestId('child')).toBeInTheDocument();
    expect(screen.getByText('Child content')).toBeInTheDocument();
  });

  it('provides portal container via usePortalContainer hook', async () => {
    renderWithTheme(
      <TooltipContainerProvider>
        <PortalContainerConsumer />
      </TooltipContainerProvider>
    );

    // Wait for useEffect to set the container
    const consumer = await screen.findByTestId('consumer');
    expect(consumer).toHaveTextContent('has-container');
  });

  it('portal container has correct positioning styles', () => {
    const { container } = renderWithTheme(
      <TooltipContainerProvider>
        <div>Content</div>
      </TooltipContainerProvider>
    );

    const portalContainer = container.querySelector('[aria-hidden="true"]');
    expect(portalContainer).toHaveStyle({
      position: 'absolute',
      top: '0px',
      left: '0px',
      width: '0px',
      height: '0px',
      overflow: 'visible',
      pointerEvents: 'none',
    });
  });
});

describe('Tooltip', () => {
  it('renders without throwing when used outside TooltipContainerProvider', () => {
    // Tooltip should gracefully fall back to body when no provider is present
    expect(() => {
      renderWithTheme(
        <Tooltip content="Tooltip text">
          <button>Hover me</button>
        </Tooltip>
      );
    }).not.toThrow();

    expect(screen.getByRole('button', { name: 'Hover me' })).toBeInTheDocument();
  });

  it('renders inside TooltipContainerProvider without throwing', () => {
    expect(() => {
      renderWithTheme(
        <TooltipContainerProvider>
          <Tooltip content="Tooltip text">
            <button>Hover me</button>
          </Tooltip>
        </TooltipContainerProvider>
      );
    }).not.toThrow();

    expect(screen.getByRole('button', { name: 'Hover me' })).toBeInTheDocument();
  });
});
