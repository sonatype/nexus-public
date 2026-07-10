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

import { screen, render } from '@testing-library/react';
import React from 'react';
import { renderComponentRoute } from '../../../testUtils/renderUtils';
import { ROUTE_NAMES } from '../../../routerConfig/routeNames/routeNames';
import { MissingRoutePage } from './MissingRoutePage';

jest.mock('../user/Welcome/Welcome', () => {
  return () => (
      <main>
        <h1>Welcome Test Mock</h1>
      </main>
  );
});

jest.mock('@uirouter/react', () => ({
  ...jest.requireActual('@uirouter/react'),
  useSref: jest.fn(() => ({ href: '#browse/welcome' })),
}));

describe('MissingRoutePage', () => {
  afterEach(() => {
    Object.defineProperty(window, 'location', {
      value: { ...window.location, hash: '' },
      writable: true,
      configurable: true,
    });
  });

  it('should render 404 page when requested', async () => {
    await renderComponentRoute(ROUTE_NAMES.MISSING_ROUTE);

    await assertMissingRoutePageRendered();
  });

  it('Return to Dashboard links to Classic UI when on a classic route', () => {
    Object.defineProperty(window, 'location', {
      value: { ...window.location, hash: '#browse/somewhere' },
      writable: true,
      configurable: true,
    });
    render(<MissingRoutePage />);

    const dashboardLink = screen.getByRole('link', { name: 'Return to Dashboard' });
    expect(dashboardLink.getAttribute('href')).toContain('browse/welcome');
    expect(dashboardLink.getAttribute('href')).not.toContain('preview/browse/welcome');
  });

  it('Return to Dashboard links to Preview UI when on a preview route', () => {
    Object.defineProperty(window, 'location', {
      value: { ...window.location, hash: '#preview/admin/support/metrichealth' },
      writable: true,
      configurable: true,
    });
    render(<MissingRoutePage />);

    const dashboardLink = screen.getByRole('link', { name: 'Return to Dashboard' });
    expect(dashboardLink.getAttribute('href')).toBe('#preview/browse/welcome');
  });

  it('Return to Dashboard links to Classic UI when hash is empty', () => {
    Object.defineProperty(window, 'location', {
      value: { ...window.location, hash: '' },
      writable: true,
      configurable: true,
    });
    render(<MissingRoutePage />);

    const dashboardLink = screen.getByRole('link', { name: 'Return to Dashboard' });
    expect(dashboardLink.getAttribute('href')).toContain('browse/welcome');
    expect(dashboardLink.getAttribute('href')).not.toContain('preview/browse/welcome');
  });

  async function assertMissingRoutePageRendered() {
    expect(screen.getByText('404')).toBeVisible();
    expect(screen.getByRole('heading', { name: 'Resource Not Found' })).toBeVisible();

    const dashboardLink = screen.getByRole('link', { name: 'Return to Dashboard' });
    expect(dashboardLink).toBeVisible();
    expect(dashboardLink.getAttribute('href')).toContain('browse/welcome');

    const helpCenterLink = screen.getByRole('link', { name: 'Visit Documentation' });
    expect(helpCenterLink).toBeVisible();
    expect(helpCenterLink.href).toBe('https://links.sonatype.com/products/nexus/docs');
  }
});
