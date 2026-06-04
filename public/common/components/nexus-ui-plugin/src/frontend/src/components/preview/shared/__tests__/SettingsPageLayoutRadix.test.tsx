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
import '@testing-library/jest-dom';
import SettingsPageLayoutRadix from '../SettingsPageLayoutRadix';

jest.mock('@uirouter/react', () => ({
  UIView: () => <div data-testid="ui-view">Router Content</div>,
}));

describe('SettingsPageLayoutRadix', () => {
  it('renders back link to Settings hub', () => {
    render(<SettingsPageLayoutRadix />);
    const back = screen.getByRole('link', { name: /back to settings/i });
    expect(back).toHaveAttribute('href', '#preview/settings');
  });

  it('renders router outlet for nested settings routes', () => {
    render(<SettingsPageLayoutRadix />);
    expect(screen.getByTestId('ui-view')).toBeInTheDocument();
  });

  it('renders the expected scroll container classes for settings pages', () => {
    const { container } = render(<SettingsPageLayoutRadix />);

    expect(container.querySelector('.settings-layout-radix')).toBeInTheDocument();
    expect(container.querySelector('.settings-layout-radix__content')).toBeInTheDocument();
  });
});
