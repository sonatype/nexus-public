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
import '@testing-library/jest-dom';
import { DeepResearchLink } from '../DeepResearchLink';

const renderWithTheme = (ui: React.ReactElement) => {
  return render(<Theme>{ui}</Theme>);
};

describe('DeepResearchLink', () => {
  it('renders link with text for npm package', () => {
    renderWithTheme(
      <DeepResearchLink ecosystem="npm" packageName="lodash" version="4.17.21" />
    );
    
    const link = screen.getByRole('link', { name: /Research in Guide/ });
    expect(link).toBeInTheDocument();
    expect(link).toHaveAttribute('href', 'https://guide.sonatype.com/component/npm/lodash/4.17.21');
    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveAttribute('rel', 'noopener noreferrer');
  });

  it('renders icon-only link with tooltip', () => {
    renderWithTheme(
      <DeepResearchLink ecosystem="maven2" packageName="org.test:artifact" version="1.0.0" iconOnly />
    );
    
    expect(screen.getByTestId('deep-research-link')).toBeInTheDocument();
    const link = screen.getByRole('link');
    expect(link).toHaveAttribute('href');
    // Icon has aria-label when iconOnly=true
    const icon = link.querySelector('svg[aria-label]');
    expect(icon).toHaveAttribute('aria-label', 'Research in Guide');
  });

  it('returns null for unsupported ecosystem', () => {
    const { container } = renderWithTheme(
      <DeepResearchLink ecosystem="docker" packageName="nginx" version="latest" />
    );
    
    expect(screen.queryByTestId('deep-research-link')).not.toBeInTheDocument();
  });

  it('encodes special characters in URL', () => {
    renderWithTheme(
      <DeepResearchLink ecosystem="npm" packageName="@scope/pkg" version="1.0.0" />
    );
    
    const link = screen.getByRole('link');
    expect(link).toHaveAttribute('href', 'https://guide.sonatype.com/component/npm/%40scope%2Fpkg/1.0.0');
  });

  it('applies custom variant and size', () => {
    renderWithTheme(
      <DeepResearchLink ecosystem="npm" packageName="test" version="1.0.0" variant="solid" size="2" />
    );
    
    const button = screen.getByTestId('deep-research-link');
    expect(button).toHaveAttribute('data-accent-color');
  });

  it('applies custom className', () => {
    renderWithTheme(
      <DeepResearchLink ecosystem="npm" packageName="test" version="1.0.0" className="custom-class" />
    );

    expect(screen.getByTestId('deep-research-link')).toHaveClass('custom-class');
  });

  it('includes referrer parameter when provided', () => {
    renderWithTheme(
      <DeepResearchLink ecosystem="npm" packageName="lodash" version="4.17.21" referrer="repo-componentdetail" />
    );

    const link = screen.getByRole('link');
    expect(link).toHaveAttribute('href', 'https://guide.sonatype.com/component/npm/lodash/4.17.21?referrer=repo-componentdetail');
  });

  it('encodes special characters in referrer parameter', () => {
    renderWithTheme(
      <DeepResearchLink ecosystem="npm" packageName="test" version="1.0.0" referrer="source&page=1" />
    );

    const link = screen.getByRole('link');
    expect(link).toHaveAttribute('href', 'https://guide.sonatype.com/component/npm/test/1.0.0?referrer=source%26page%3D1');
  });
});
