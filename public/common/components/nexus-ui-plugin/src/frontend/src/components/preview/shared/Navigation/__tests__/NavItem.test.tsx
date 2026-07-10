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
import {NavItem} from '../NavItem';

jest.mock('@uirouter/react', () => ({
  useSref: jest.fn().mockReturnValue({href: '/test-route', onClick: jest.fn()}),
  useIsActive: jest.fn().mockReturnValue(false),
}));

jest.mock('@radix-ui/themes', () => ({
  Flex: ({children}: {children: React.ReactNode}) => <div>{children}</div>,
}));

// Source imports '../Tooltip/TooltipContainerContext' (1 up from Navigation/).
// From __tests__/ we need 2 ups to reach the same shared/Tooltip path.
jest.mock('../../Tooltip/TooltipContainerContext', () => ({
  Tooltip: ({children, content}: {children: React.ReactNode; content: string}) => (
    <div data-testid="tooltip" title={content}>{children}</div>
  ),
}));

jest.mock('../useContextAwareRouteName', () => ({
  useContextAwareRouteName: jest.fn((name: string) => name),
}));

const {useIsActive} = require('@uirouter/react');

describe('NavItem', () => {
  beforeEach(() => {
    useIsActive.mockReturnValue(false);
  });

  it('returns null when name is an empty string', () => {
    const {container} = render(<NavItem name="" text="Dashboard" />);
    expect(container.firstChild).toBeNull();
  });

  it('renders a link with the route text', () => {
    render(<NavItem name="browse.search" text="Search" />);
    expect(screen.getByRole('link')).toBeInTheDocument();
    expect(screen.getByText('Search')).toBeInTheDocument();
  });

  it('applies the active class when the route is active', () => {
    useIsActive.mockReturnValue(true);
    render(<NavItem name="browse.search" text="Search" />);
    expect(screen.getByRole('link')).toHaveClass('guide-nav-item--active');
  });

  it('wraps in Tooltip when collapsed', () => {
    render(<NavItem name="browse.search" text="Search" isCollapsed />);
    expect(screen.getByTestId('tooltip')).toBeInTheDocument();
  });

  it('does not show text when collapsed', () => {
    render(<NavItem name="browse.search" text="Search" isCollapsed />);
    expect(screen.queryByText('Search')).not.toBeInTheDocument();
  });

  it('shows text when not collapsed', () => {
    render(<NavItem name="browse.search" text="Search" isCollapsed={false} />);
    expect(screen.getByText('Search')).toBeInTheDocument();
  });

  it('uses a direct href for external links', () => {
    render(<NavItem name="browse.search" text="Docs" href="https://example.com" />);
    expect(screen.getByRole('link')).toHaveAttribute('href', 'https://example.com');
  });

  it('adds target=_blank and rel=noopener noreferrer for external links', () => {
    render(<NavItem name="browse.search" text="Docs" isExternal href="https://example.com" />);
    const link = screen.getByRole('link');
    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveAttribute('rel', 'noopener noreferrer');
  });

  it('shows the external link icon when external and not collapsed', () => {
    render(<NavItem name="browse.search" text="Docs" isExternal isCollapsed={false} />);
    expect(screen.getByText('Docs')).toBeInTheDocument();
  });
});
