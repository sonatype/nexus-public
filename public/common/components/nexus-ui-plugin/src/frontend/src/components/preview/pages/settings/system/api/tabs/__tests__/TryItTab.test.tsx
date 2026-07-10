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

import { TryItTab } from '../TryItTab';
import type { MergedApiEndpoint } from '../../utils/mergeSwaggerPermissions';

const mockSwaggerUI = jest.fn(() => <div data-testid="swagger-ui" />);
jest.mock('swagger-ui-react', () => ({
  __esModule: true,
  default: (props: Record<string, unknown>) => {
    mockSwaggerUI(props);
    return <div data-testid="swagger-ui" />;
  },
}));

jest.mock('swagger-ui-react/swagger-ui.css', () => ({}));

function renderWithTheme(ui: React.ReactElement) {
  return render(<Theme>{ui}</Theme>);
}

function baseRow(overrides: Partial<MergedApiEndpoint> = {}): MergedApiEndpoint {
  return {
    httpMethod: 'GET',
    swaggerPathKey: '/v1/status',
    fullPath: '/service/rest/v1/status',
    tag: 'Status',
    summary: 'Health check',
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

const FULL_SWAGGER: Record<string, unknown> = {
  swagger: '2.0',
  basePath: '/service/rest',
  paths: {
    '/v1/status': {
      get: {
        operationId: 'getStatus',
        summary: 'Health check',
        tags: ['Status'],
        responses: { '200': { description: 'OK' } },
      },
    },
  },
};

describe('TryItTab', () => {
  beforeEach(() => {
    mockSwaggerUI.mockClear();
  });

  it('renders fallback message when fullSwagger is null', () => {
    renderWithTheme(<TryItTab fullSwagger={null} row={baseRow()} accessDenied={false} />);
    expect(screen.getByText(/API documentation is not available/)).toBeInTheDocument();
    expect(screen.queryByTestId('swagger-ui')).not.toBeInTheDocument();
  });

  it('renders SwaggerUI when fullSwagger is provided', () => {
    renderWithTheme(<TryItTab fullSwagger={FULL_SWAGGER} row={baseRow()} accessDenied={false} />);
    expect(screen.getByTestId('swagger-ui')).toBeInTheDocument();
  });

  it('passes docExpansion="full" to SwaggerUI', () => {
    renderWithTheme(<TryItTab fullSwagger={FULL_SWAGGER} row={baseRow()} accessDenied={false} />);
    expect(mockSwaggerUI).toHaveBeenCalledWith(
      expect.objectContaining({ docExpansion: 'full' })
    );
  });

  it('passes defaultModelsExpandDepth={-1} to SwaggerUI', () => {
    renderWithTheme(<TryItTab fullSwagger={FULL_SWAGGER} row={baseRow()} accessDenied={false} />);
    expect(mockSwaggerUI).toHaveBeenCalledWith(
      expect.objectContaining({ defaultModelsExpandDepth: -1 })
    );
  });

  it('passes a sliced spec containing only the selected endpoint', () => {
    renderWithTheme(<TryItTab fullSwagger={FULL_SWAGGER} row={baseRow()} accessDenied={false} />);
    expect(mockSwaggerUI).toHaveBeenCalledTimes(1);
    const passedSpec = mockSwaggerUI.mock.calls[0][0].spec as Record<string, unknown>;
    const paths = passedSpec.paths as Record<string, unknown>;
    expect(Object.keys(paths)).toEqual(['/v1/status']);
  });

  it('shows access denied banner when accessDenied is true', () => {
    renderWithTheme(<TryItTab fullSwagger={FULL_SWAGGER} row={baseRow()} accessDenied={true} />);
    expect(screen.getByText(/You may not have permission/)).toBeInTheDocument();
  });

  it('does not show access denied banner when accessDenied is false', () => {
    renderWithTheme(<TryItTab fullSwagger={FULL_SWAGGER} row={baseRow()} accessDenied={false} />);
    expect(screen.queryByText(/You may not have permission/)).not.toBeInTheDocument();
  });
});
