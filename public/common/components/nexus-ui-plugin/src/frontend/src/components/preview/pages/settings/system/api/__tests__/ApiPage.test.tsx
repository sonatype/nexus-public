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
import { render, screen, waitFor, act } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';

import { ApiPage } from '../ApiPage';

const mockGetValue = jest.fn((_key: string, defaultValue: boolean) => defaultValue);

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtJS: {
    urlOf: jest.fn((path: string) => `http://localhost:8081${path}`),
    checkPermission: jest.fn(() => true),
    state: () => ({
      getValue: (...args: [string, boolean]) => mockGetValue(...args),
    }),
  },
}));

jest.mock('../hooks/useEndpointPermissions', () => ({
  useEndpointPermissions: () => ({
    data: { endpoints: [] },
    loading: false,
    error: null,
  }),
}));

jest.mock('../hooks/useApiModuleDeepLink', () => ({
  useApiModuleDeepLink: () => ({
    viewAsUserId: null,
    roleLensId: null,
    permissionFilter: null,
    endpointParam: null,
    warnings: [],
  }),
}));

jest.mock('../hooks/useViewAsUserAccess', () => ({
  useViewAsUserAccess: () => ({
    accessById: null,
    loading: false,
    error: null,
  }),
}));

jest.mock('../../../security/privileges/usePrivilegesApi', () => ({
  usePrivilegesApi: () => ({
    fetchPrivileges: jest.fn().mockResolvedValue({ data: [], total: 0 }),
  }),
}));

jest.mock('../../../security/roles/useRolesApi', () => ({
  useRolesApi: () => ({
    fetchRoles: jest.fn().mockResolvedValue([]),
    findRole: jest.fn().mockResolvedValue(null),
  }),
}));

jest.mock('../ApiLayout', () => ({
  ApiLayout: ({ leftPanel, rightPanel }: any) => (
    <div data-testid="api-layout">
      <div data-testid="api-layout-left">{leftPanel}</div>
      <div data-testid="api-layout-right">{rightPanel}</div>
    </div>
  ),
}));

jest.mock('../EndpointList', () => ({
  EndpointList: () => <div data-testid="endpoint-list">EndpointList</div>,
  endpointRowId: (row: any) => `${row.method}-${row.path}`,
}));

jest.mock('../EndpointDetail', () => ({
  EndpointDetail: () => <div data-testid="endpoint-detail">EndpointDetail</div>,
}));

function renderWithTheme(component: React.ReactElement) {
  return render(<Theme>{component}</Theme>);
}

const mockFetch = jest.fn();

beforeAll(() => {
  global.fetch = mockFetch;
});

afterAll(() => {
  // @ts-ignore
  delete global.fetch;
});

describe('ApiPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockGetValue.mockImplementation((_key: string, defaultValue: boolean) => defaultValue);
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ openapi: '3.0.0', paths: {} }),
    });
  });

  describe('Component Rendering', () => {
    it('should render the page with correct structure', async () => {
      await act(async () => {
        renderWithTheme(<ApiPage />);
      });

      expect(screen.getByTestId('api-page')).toBeInTheDocument();
      expect(screen.getByRole('heading', { name: 'API' })).toBeInTheDocument();
      expect(screen.getByText('Documentation, permissions, and access tools')).toBeInTheDocument();
    });

    it('should apply custom className when provided', async () => {
      await act(async () => {
        renderWithTheme(<ApiPage className="custom-class" />);
      });

      const page = screen.getByTestId('api-page');
      expect(page).toHaveClass('api-page');
      expect(page).toHaveClass('custom-class');
    });

    it('should render help section', async () => {
      await act(async () => {
        renderWithTheme(<ApiPage />);
      });

      expect(screen.getByText('About API Documentation')).toBeInTheDocument();
      expect(screen.getByRole('link', { name: /documentation/i })).toHaveAttribute(
        'href',
        'https://help.sonatype.com/en/rest-and-integration-api.html'
      );
    });

    it('should render the endpoint list and detail panels', async () => {
      await act(async () => {
        renderWithTheme(<ApiPage />);
      });

      expect(screen.getByTestId('api-layout')).toBeInTheDocument();
      expect(screen.getByTestId('endpoint-list')).toBeInTheDocument();
      expect(screen.getByTestId('endpoint-detail')).toBeInTheDocument();
    });
  });

  describe('Loading States', () => {
    it('should have data-loading attribute', async () => {
      await act(async () => {
        renderWithTheme(<ApiPage />);
      });

      expect(screen.getByTestId('api-page')).toHaveAttribute('data-loading');
    });

    it('should set data-loading to false when loaded', async () => {
      await act(async () => {
        renderWithTheme(<ApiPage />);
      });

      expect(screen.getByTestId('api-page')).toHaveAttribute('data-loading', 'false');
    });
  });

  describe('Error Handling', () => {
    it('should show swagger error when fetch fails', async () => {
      mockFetch.mockRejectedValueOnce(new Error('network'));

      await act(async () => {
        renderWithTheme(<ApiPage />);
      });

      await waitFor(() => {
        expect(screen.getByText(/API documentation failed to load/)).toBeInTheDocument();
      });
    });
  });

  describe('Accessibility', () => {
    it('should have descriptive page heading', async () => {
      await act(async () => {
        renderWithTheme(<ApiPage />);
      });

      expect(screen.getByRole('heading', { level: 1, name: 'API' })).toBeInTheDocument();
    });

    it('should have accessible external link', async () => {
      await act(async () => {
        renderWithTheme(<ApiPage />);
      });

      const externalLink = screen.getByRole('link', { name: /documentation/i });
      expect(externalLink).toHaveAttribute('target', '_blank');
      expect(externalLink).toHaveAttribute('rel', 'noopener noreferrer');
    });
  });

  describe('Data Attributes', () => {
    it('should have correct test IDs', async () => {
      await act(async () => {
        renderWithTheme(<ApiPage />);
      });

      expect(screen.getByTestId('api-page')).toBeInTheDocument();
      expect(screen.getByTestId('api-layout')).toBeInTheDocument();
    });
  });

  describe('Breadcrumb Navigation', () => {
    it('renders breadcrumbs for API page', async () => {
      await act(async () => {
        renderWithTheme(<ApiPage />);
      });

      // Breadcrumbs: Settings (clickable) > API (current page)
      expect(screen.getByRole('button', { name: 'Settings' })).toBeInTheDocument();
      expect(screen.getByRole('heading', { name: 'API' })).toBeInTheDocument();
    });

    it('clicking Settings breadcrumb navigates to settings page', async () => {
      await act(async () => {
        renderWithTheme(<ApiPage />);
      });

      screen.getByRole('button', { name: 'Settings' }).click();
      expect(window.location.hash).toBe('#preview/admin/settings');
    });
  });
});
