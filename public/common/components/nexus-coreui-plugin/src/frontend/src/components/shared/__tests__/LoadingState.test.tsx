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
import { LoadingState } from '../LoadingState';

const renderWithTheme = (ui: React.ReactElement) => {
  return render(<Theme>{ui}</Theme>);
};

describe('LoadingState', () => {
  it('renders without message', () => {
    renderWithTheme(<LoadingState />);

    expect(screen.getByTestId('loading-state')).toBeInTheDocument();
    expect(screen.getByRole('status')).toBeInTheDocument();
  });

  it('renders with message', () => {
    renderWithTheme(<LoadingState message="Loading repositories..." />);

    expect(screen.getByText('Loading repositories...')).toBeInTheDocument();
  });

  it('has aria-busy attribute', () => {
    renderWithTheme(<LoadingState />);

    expect(screen.getByRole('status')).toHaveAttribute('aria-busy', 'true');
  });

  it('has aria-live attribute', () => {
    renderWithTheme(<LoadingState />);

    expect(screen.getByRole('status')).toHaveAttribute('aria-live', 'polite');
  });

  it('applies small size class', () => {
    const { container } = renderWithTheme(<LoadingState size="small" />);

    expect(container.querySelector('.loading-state--small')).toBeInTheDocument();
  });

  it('applies large size class', () => {
    const { container } = renderWithTheme(<LoadingState size="large" />);

    expect(container.querySelector('.loading-state--large')).toBeInTheDocument();
  });

  it('applies inline class when inline prop is true', () => {
    const { container } = renderWithTheme(<LoadingState inline />);

    expect(container.querySelector('.loading-state--inline')).toBeInTheDocument();
  });

  it('applies custom className', () => {
    const { container } = renderWithTheme(
      <LoadingState className="custom-class" />
    );

    expect(container.querySelector('.custom-class')).toBeInTheDocument();
  });

  it('includes screen reader text', () => {
    renderWithTheme(<LoadingState message="Loading data" />);

    expect(screen.getByText('Loading: Loading data')).toBeInTheDocument();
  });
});


