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
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';
import { ExtJS } from '../../../../../../interface/ExtJS';
import { NoIqBanner } from '../NoIqBanner';

const mockOnClick = jest.fn();

jest.mock('@uirouter/react', () => ({
  useSref: jest.fn(() => ({ href: '#/admin/iq', onClick: (...a: unknown[]) => mockOnClick(...a) })),
}));

jest.mock('../../../../shared/hooks/useHasUser', () => ({ useHasUser: () => true }));

const { useSref } = require('@uirouter/react');

let granted: string[] = [];

beforeEach(() => {
  jest.clearAllMocks();
  granted = [];
  jest.spyOn(ExtJS, 'usePermission').mockImplementation((getValue: () => boolean) => getValue());
  jest.spyOn(ExtJS, 'checkPermission').mockImplementation((p: string) => granted.includes(p));
});

const renderBanner = () =>
  render(
    <Theme>
      <NoIqBanner />
    </Theme>
  );

describe('NoIqBanner', () => {
  it('is a status region explaining that IQ Server is not configured', () => {
    renderBanner();
    expect(screen.getByRole('status')).toHaveTextContent(/IQ Server/i);
  });

  it('shows Configure IQ Server, linked to preview.admin.iq, with nexus:settings:read', () => {
    granted = ['nexus:settings:read'];
    renderBanner();

    expect(ExtJS.checkPermission).toHaveBeenCalledWith('nexus:settings:read');
    expect(useSref).toHaveBeenCalledWith('preview.admin.iq');
    const link = screen.getByRole('link', { name: 'Configure IQ Server' });
    expect(link).toHaveAttribute('href', '#/admin/iq');
    expect(link).toHaveAttribute('data-analytics-id', 'nxrm-security-configure-iq-btn');
    userEvent.click(link);
    expect(mockOnClick).toHaveBeenCalled();
  });

  it('does not show Configure IQ Server without nexus:settings:read', () => {
    granted = ['nexus:firewall-waivers:read'];
    renderBanner();

    expect(screen.queryByRole('link', { name: 'Configure IQ Server' })).not.toBeInTheDocument();
    expect(screen.getByRole('status')).toBeInTheDocument();
  });
});
