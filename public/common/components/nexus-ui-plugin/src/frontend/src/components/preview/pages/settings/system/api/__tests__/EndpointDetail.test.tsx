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

import { EndpointDetail } from '../EndpointDetail';
import type { MergedApiEndpoint } from '../utils/mergeSwaggerPermissions';

jest.mock('../tabs/TryItTab', () => ({
  TryItTab: ({ accessDenied }: any) => (
    <div data-testid="try-it-tab" data-denied={String(accessDenied)}>TryItTab</div>
  ),
}));

jest.mock('../tabs/WhoHasAccessTab', () => ({
  WhoHasAccessTab: ({ active }: any) => (
    <div data-testid="who-has-access-tab" data-active={String(active)}>WhoHasAccessTab</div>
  ),
}));

jest.mock('../tabs/GrantAccessTab', () => ({
  GrantAccessTab: ({ active }: any) => (
    <div data-testid="grant-access-tab" data-active={String(active)}>GrantAccessTab</div>
  ),
}));

function makeRow(overrides: Partial<MergedApiEndpoint> = {}): MergedApiEndpoint {
  return {
    httpMethod: 'GET',
    swaggerPathKey: '/v1/status',
    fullPath: '/service/rest/v1/status',
    tag: 'Status',
    summary: 'Check application health',
    permission: {
      httpMethod: 'GET',
      pathPattern: '/service/rest/v1/status',
      permissions: [],
      description: null,
      tag: null,
      authenticated: false,
    },
    ...overrides,
  };
}

function renderWithTheme(ui: React.ReactElement) {
  return render(<Theme>{ui}</Theme>);
}

describe('EndpointDetail', () => {
  describe('Empty State', () => {
    it('renders placeholder when no row is selected', () => {
      renderWithTheme(<EndpointDetail row={null} fullSwagger={null} access="unknown" />);
      expect(screen.getByText('Choose an endpoint')).toBeInTheDocument();
      expect(screen.getByText(/Click any operation/)).toBeInTheDocument();
    });

    it('does not render detail panel when row is null', () => {
      renderWithTheme(<EndpointDetail row={null} fullSwagger={null} access="unknown" />);
      expect(screen.queryByTestId('api-endpoint-detail')).not.toBeInTheDocument();
    });
  });

  describe('With Selected Endpoint', () => {
    const row = makeRow();

    it('renders the detail panel', () => {
      renderWithTheme(<EndpointDetail row={row} fullSwagger={null} access="granted" />);
      expect(screen.getByTestId('api-endpoint-detail')).toBeInTheDocument();
    });

    it('displays HTTP method and path', () => {
      renderWithTheme(<EndpointDetail row={row} fullSwagger={null} access="granted" />);
      expect(screen.getByText('GET')).toBeInTheDocument();
      expect(screen.getByText('/service/rest/v1/status')).toBeInTheDocument();
    });

    it('displays the summary', () => {
      renderWithTheme(<EndpointDetail row={row} fullSwagger={null} access="granted" />);
      expect(screen.getByText('Check application health')).toBeInTheDocument();
    });

    it('shows "None (may allow anonymous)" for unauthenticated endpoints without permissions', () => {
      renderWithTheme(<EndpointDetail row={row} fullSwagger={null} access="granted" />);
      expect(screen.getByText(/None \(may allow anonymous\)/)).toBeInTheDocument();
    });

    it('shows "None mapped" for authenticated endpoints without permissions', () => {
      const authRow = makeRow({
        permission: {
          httpMethod: 'GET',
          pathPattern: '/service/rest/v1/status',
          permissions: [],
          description: null,
          tag: null,
          authenticated: true,
        },
      });
      renderWithTheme(<EndpointDetail row={authRow} fullSwagger={null} access="granted" />);
      expect(screen.getByText(/None mapped/)).toBeInTheDocument();
    });

    it('displays permission strings joined by AND', () => {
      const row = makeRow({
        permission: {
          httpMethod: 'GET',
          pathPattern: '/service/rest/v1/repositories',
          permissions: [
            { permission: 'nexus:repos:read', logical: 'AND' },
            { permission: 'nexus:repos:browse', logical: 'AND' },
          ],
          description: null,
          tag: null,
          authenticated: true,
        },
      });
      renderWithTheme(<EndpointDetail row={row} fullSwagger={null} access="granted" />);
      expect(screen.getByText(/nexus:repos:read AND nexus:repos:browse/)).toBeInTheDocument();
    });

    it('displays permission strings joined by OR', () => {
      const row = makeRow({
        permission: {
          httpMethod: 'GET',
          pathPattern: '/service/rest/v1/repositories',
          permissions: [
            { permission: 'nexus:a:read', logical: 'OR' },
            { permission: 'nexus:b:read', logical: 'OR' },
          ],
          description: null,
          tag: null,
          authenticated: true,
        },
      });
      renderWithTheme(<EndpointDetail row={row} fullSwagger={null} access="granted" />);
      expect(screen.getByText(/nexus:a:read OR nexus:b:read/)).toBeInTheDocument();
    });
  });

  describe('Tabs', () => {
    const row = makeRow();

    it('renders the Try It tab by default', () => {
      renderWithTheme(<EndpointDetail row={row} fullSwagger={null} access="granted" />);
      expect(screen.getByTestId('try-it-tab')).toBeInTheDocument();
    });

    it('passes accessDenied=false to TryItTab when access is granted', () => {
      renderWithTheme(<EndpointDetail row={row} fullSwagger={null} access="granted" />);
      expect(screen.getByTestId('try-it-tab')).toHaveAttribute('data-denied', 'false');
    });

    it('passes accessDenied=true to TryItTab when access is denied', () => {
      renderWithTheme(<EndpointDetail row={row} fullSwagger={null} access="denied" />);
      expect(screen.getByTestId('try-it-tab')).toHaveAttribute('data-denied', 'true');
    });

    it('switches to Who Has Access tab on click', async () => {
      renderWithTheme(<EndpointDetail row={row} fullSwagger={null} access="granted" />);
      await userEvent.click(screen.getByRole('tab', { name: /Who Has Access/ }));
      expect(screen.getByTestId('who-has-access-tab')).toBeInTheDocument();
    });

    it('switches to Grant Access tab on click', async () => {
      renderWithTheme(<EndpointDetail row={row} fullSwagger={null} access="granted" />);
      await userEvent.click(screen.getByRole('tab', { name: /Grant Access/ }));
      expect(screen.getByTestId('grant-access-tab')).toBeInTheDocument();
    });

    it('renders three tab triggers', () => {
      renderWithTheme(<EndpointDetail row={row} fullSwagger={null} access="granted" />);
      expect(screen.getByRole('tab', { name: /Try It/ })).toBeInTheDocument();
      expect(screen.getByRole('tab', { name: /Who Has Access/ })).toBeInTheDocument();
      expect(screen.getByRole('tab', { name: /Grant Access/ })).toBeInTheDocument();
    });
  });
});
