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
import { render, screen, fireEvent } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import { ErrorState } from '../ErrorState';

const renderWithTheme = (ui: React.ReactElement) => {
  return render(<Theme>{ui}</Theme>);
};

describe('ErrorState', () => {
  it('renders with default title', () => {
    renderWithTheme(<ErrorState message="Network error occurred" />);

    expect(screen.getByTestId('error-state')).toBeInTheDocument();
    expect(screen.getByText('Something went wrong')).toBeInTheDocument();
    expect(screen.getByText('Network error occurred')).toBeInTheDocument();
  });

  it('renders with custom title', () => {
    renderWithTheme(
      <ErrorState
        title="Connection Failed"
        message="Unable to reach server"
      />
    );

    expect(screen.getByText('Connection Failed')).toBeInTheDocument();
    expect(screen.getByText('Unable to reach server')).toBeInTheDocument();
  });

  it('renders retry button when onRetry is provided', () => {
    const mockOnRetry = jest.fn();

    renderWithTheme(
      <ErrorState message="Error occurred" onRetry={mockOnRetry} />
    );

    const retryButton = screen.getByRole('button', { name: /try again/i });
    expect(retryButton).toBeInTheDocument();

    fireEvent.click(retryButton);
    expect(mockOnRetry).toHaveBeenCalled();
  });

  it('hides retry button when onRetry is not provided', () => {
    renderWithTheme(<ErrorState message="Error occurred" />);

    expect(screen.queryByRole('button', { name: /try again/i })).not.toBeInTheDocument();
  });

  it('renders custom retry text', () => {
    renderWithTheme(
      <ErrorState
        message="Error occurred"
        onRetry={() => {}}
        retryText="Retry"
      />
    );

    expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument();
  });

  it('has role alert for accessibility', () => {
    renderWithTheme(<ErrorState message="Error occurred" />);

    expect(screen.getByRole('alert')).toBeInTheDocument();
  });

  it('applies small size class', () => {
    const { container } = renderWithTheme(
      <ErrorState message="Error" size="small" />
    );

    expect(container.querySelector('.error-state--small')).toBeInTheDocument();
  });

  it('applies large size class', () => {
    const { container } = renderWithTheme(
      <ErrorState message="Error" size="large" />
    );

    expect(container.querySelector('.error-state--large')).toBeInTheDocument();
  });

  it('renders inline variant', () => {
    const { container } = renderWithTheme(
      <ErrorState message="Error" variant="inline" />
    );

    expect(container.querySelector('.error-state--inline')).toBeInTheDocument();
  });

  it('applies custom className', () => {
    const { container } = renderWithTheme(
      <ErrorState message="Error" className="custom-class" />
    );

    expect(container.querySelector('.custom-class')).toBeInTheDocument();
  });
});


