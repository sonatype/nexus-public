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
import SettingsNotAvailablePage from '../SettingsNotAvailablePage';

const renderWithTheme = (ui: React.ReactElement) => {
  return render(<Theme>{ui}</Theme>);
};

describe('SettingsNotAvailablePage', () => {
  it('renders the heading', () => {
    renderWithTheme(<SettingsNotAvailablePage featureName="Users" />);

    expect(screen.getByRole('heading', { name: 'Not available in preview' })).toBeInTheDocument();
  });

  it('renders featureName in the message', () => {
    renderWithTheme(<SettingsNotAvailablePage featureName="Users" />);

    expect(screen.getByText(/Users is still being prepared for the Nexus One UI/)).toBeInTheDocument();
  });

  it('renders back link to settings hub', () => {
    renderWithTheme(<SettingsNotAvailablePage featureName="Users" />);

    const backLink = screen.getByRole('link', { name: /Back to Settings/ });
    expect(backLink).toHaveAttribute('href', '#preview/admin/settings');
  });

  it('renders with different featureName', () => {
    renderWithTheme(<SettingsNotAvailablePage featureName="LDAP" />);

    expect(screen.getByText(/LDAP is still being prepared for the Nexus One UI/)).toBeInTheDocument();
  });

  it('instructs user to use classic admin panel', () => {
    renderWithTheme(<SettingsNotAvailablePage featureName="Realms" />);

    expect(screen.getByText(/classic UI administration panel/)).toBeInTheDocument();
  });
});
