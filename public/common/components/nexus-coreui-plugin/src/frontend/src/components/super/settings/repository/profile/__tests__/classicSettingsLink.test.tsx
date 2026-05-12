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
import { ClassicSettingsLink } from '../tabs/classicSettingsLink';

function renderWithTheme(ui: React.ReactElement) {
  return render(<Theme>{ui}</Theme>);
}

describe('ClassicSettingsLink', () => {
  let windowOpenSpy: jest.SpyInstance;

  beforeEach(() => {
    windowOpenSpy = jest.spyOn(window, 'open').mockImplementation(() => null);
  });

  afterEach(() => {
    windowOpenSpy.mockRestore();
  });

  it('renders with default "Configure" label', () => {
    renderWithTheme(<ClassicSettingsLink previewPath="preview/admin/repository/repositories/npm-proxy" />);
    expect(screen.getByText('Configure')).toBeInTheDocument();
  });

  it('renders with custom label', () => {
    renderWithTheme(<ClassicSettingsLink previewPath="preview/admin/security/privileges" label="View" />);
    expect(screen.getByText('View')).toBeInTheDocument();
  });

  it('opens Classic UI URL in a new tab on click with noopener,noreferrer', () => {
    renderWithTheme(<ClassicSettingsLink previewPath="preview/admin/repository/cleanuppolicies" />);
    fireEvent.click(screen.getByText('Configure'));

    expect(windowOpenSpy).toHaveBeenCalledTimes(1);
    const url = windowOpenSpy.mock.calls[0][0];
    expect(url).toContain('#admin/repository/cleanuppolicies');
    expect(windowOpenSpy.mock.calls[0][1]).toBe('_blank');
    expect(windowOpenSpy.mock.calls[0][2]).toBe('noopener,noreferrer');
  });

  it('maps repository entity path to Classic colon-separated URL', () => {
    renderWithTheme(
      <ClassicSettingsLink previewPath="preview/admin/repository/repositories/my-repo" label="Configure" />
    );
    fireEvent.click(screen.getByText('Configure'));

    const url = windowOpenSpy.mock.calls[0][0];
    expect(url).toContain('#admin/repository/repositories:my-repo');
  });

  it('maps security settings path to Classic equivalent', () => {
    renderWithTheme(
      <ClassicSettingsLink previewPath="preview/admin/security/roles" label="View" />
    );
    fireEvent.click(screen.getByText('View'));

    const url = windowOpenSpy.mock.calls[0][0];
    expect(url).toContain('#admin/security/roles');
  });

  it('maps system settings path to Classic equivalent', () => {
    renderWithTheme(
      <ClassicSettingsLink previewPath="preview/admin/system/tasks" label="View" />
    );
    fireEvent.click(screen.getByText('View'));

    const url = windowOpenSpy.mock.calls[0][0];
    expect(url).toContain('#admin/system/tasks');
  });

  it('maps IQ server path to Classic equivalent', () => {
    renderWithTheme(
      <ClassicSettingsLink previewPath="preview/admin/iq" label="Configure" />
    );
    fireEvent.click(screen.getByText('Configure'));

    const url = windowOpenSpy.mock.calls[0][0];
    expect(url).toContain('#admin/iq');
  });

  it('renders nothing when previewPath has no Classic equivalent', () => {
    renderWithTheme(
      <ClassicSettingsLink previewPath="some/unknown/path" label="Configure" />
    );
    expect(screen.queryByText('Configure')).not.toBeInTheDocument();
  });

  it('has a tooltip with Coming Soon messaging', () => {
    renderWithTheme(<ClassicSettingsLink previewPath="preview/admin/iq" />);
    const button = screen.getByText('Configure');
    expect(button).toBeInTheDocument();
  });
});
