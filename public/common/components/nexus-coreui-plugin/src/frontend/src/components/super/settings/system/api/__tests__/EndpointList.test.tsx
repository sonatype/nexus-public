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

import { EndpointList, endpointRowId } from '../EndpointList';
import type { MergedApiEndpoint } from '../utils/mergeSwaggerPermissions';
import type { EndpointAccessDot } from '../utils/endpointAccess';

function endpoint(overrides: Partial<MergedApiEndpoint> = {}): MergedApiEndpoint {
  return {
    httpMethod: 'GET',
    swaggerPathKey: '/v1/status',
    fullPath: '/service/rest/v1/status',
    tag: 'Status',
    permission: null,
    ...overrides,
  };
}

function renderWithTheme(ui: React.ReactElement) {
  return render(<Theme>{ui}</Theme>);
}

const ENDPOINTS: MergedApiEndpoint[] = [
  endpoint({ httpMethod: 'GET', fullPath: '/service/rest/v1/status', tag: 'Status', summary: 'Health check' }),
  endpoint({ httpMethod: 'POST', fullPath: '/service/rest/v1/repositories', tag: 'Repositories' }),
  endpoint({ httpMethod: 'DELETE', fullPath: '/service/rest/v1/repositories/{name}', tag: 'Repositories' }),
  endpoint({ httpMethod: 'GET', fullPath: '/service/rest/v1/search', tag: 'Search' }),
];

function buildAccessMap(endpoints: MergedApiEndpoint[], dot: EndpointAccessDot): Record<string, EndpointAccessDot> {
  const map: Record<string, EndpointAccessDot> = {};
  for (const e of endpoints) {
    map[endpointRowId(e)] = dot;
  }
  return map;
}

describe('EndpointList', () => {
  const defaultProps = {
    endpoints: ENDPOINTS,
    accessById: buildAccessMap(ENDPOINTS, 'granted'),
    selectedId: null,
    onSelect: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('endpointRowId', () => {
    it('produces method|path composite key', () => {
      expect(endpointRowId(ENDPOINTS[0])).toBe('GET|/service/rest/v1/status');
    });
  });

  describe('Rendering', () => {
    it('renders the search input', () => {
      renderWithTheme(<EndpointList {...defaultProps} />);
      expect(screen.getByRole('textbox', { name: 'Search endpoints' })).toBeInTheDocument();
    });

    it('renders all endpoint rows in a listbox', () => {
      renderWithTheme(<EndpointList {...defaultProps} />);
      expect(screen.getByRole('listbox', { name: 'API endpoints' })).toBeInTheDocument();
      expect(screen.getAllByRole('option')).toHaveLength(ENDPOINTS.length);
    });

    it('displays HTTP method badges and paths for each row', () => {
      renderWithTheme(<EndpointList {...defaultProps} />);
      expect(screen.getAllByText('GET')).toHaveLength(2);
      expect(screen.getByText('POST')).toBeInTheDocument();
      expect(screen.getByText('DELETE')).toBeInTheDocument();
      expect(screen.getByText('/service/rest/v1/status')).toBeInTheDocument();
    });

    it('groups endpoints by tag', () => {
      renderWithTheme(<EndpointList {...defaultProps} />);
      expect(screen.getByText('Status')).toBeInTheDocument();
      expect(screen.getByText('Repositories')).toBeInTheDocument();
      expect(screen.getByText('Search')).toBeInTheDocument();
    });
  });

  describe('Loading State', () => {
    it('renders skeleton placeholders when loading', () => {
      renderWithTheme(<EndpointList {...defaultProps} loading={true} />);
      expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
      const busy = document.querySelector('[aria-busy="true"]');
      expect(busy).toBeInTheDocument();
    });
  });

  describe('Empty State', () => {
    it('shows empty message when no endpoints match', () => {
      renderWithTheme(<EndpointList {...defaultProps} endpoints={[]} />);
      expect(screen.getByText(/No endpoints match/)).toBeInTheDocument();
    });
  });

  describe('Selection', () => {
    it('calls onSelect when an endpoint row is clicked', async () => {
      const onSelect = jest.fn();
      renderWithTheme(<EndpointList {...defaultProps} onSelect={onSelect} />);
      const options = screen.getAllByRole('option');
      await userEvent.click(options[0]);
      expect(onSelect).toHaveBeenCalledTimes(1);
      const calledWith = onSelect.mock.calls[0][0] as MergedApiEndpoint;
      expect(calledWith.tag).toBe('Repositories');
    });

    it('marks the selected row with aria-selected', () => {
      const selectedId = endpointRowId(ENDPOINTS[1]);
      renderWithTheme(<EndpointList {...defaultProps} selectedId={selectedId} />);
      const options = screen.getAllByRole('option');
      const selectedOption = options.find((o) => o.getAttribute('aria-selected') === 'true');
      expect(selectedOption).toBeDefined();
      expect(selectedOption).toHaveTextContent('/service/rest/v1/repositories');
    });
  });

  describe('Search Filtering', () => {
    it('filters endpoints by path text', async () => {
      renderWithTheme(<EndpointList {...defaultProps} />);
      const searchInput = screen.getByRole('textbox', { name: 'Search endpoints' });
      await userEvent.type(searchInput, 'status');
      expect(screen.getAllByRole('option')).toHaveLength(1);
      expect(screen.getByText('/service/rest/v1/status')).toBeInTheDocument();
    });

    it('filters by summary text', async () => {
      renderWithTheme(<EndpointList {...defaultProps} />);
      const searchInput = screen.getByRole('textbox', { name: 'Search endpoints' });
      await userEvent.type(searchInput, 'Health check');
      expect(screen.getAllByRole('option')).toHaveLength(1);
    });

    it('shows empty message when search has no matches', async () => {
      renderWithTheme(<EndpointList {...defaultProps} />);
      const searchInput = screen.getByRole('textbox', { name: 'Search endpoints' });
      await userEvent.type(searchInput, 'nonexistent-path');
      expect(screen.getByText(/No endpoints match/)).toBeInTheDocument();
    });
  });

  describe('Only-Denied Checkbox', () => {
    it('renders session label by default', () => {
      renderWithTheme(<EndpointList {...defaultProps} />);
      expect(screen.getByText("Only endpoints I can't access")).toBeInTheDocument();
    });

    it('renders roleLens label when accessDotPalette is roleLens', () => {
      renderWithTheme(<EndpointList {...defaultProps} accessDotPalette="roleLens" />);
      expect(screen.getByText('Only endpoints this role cannot access')).toBeInTheDocument();
    });

    it('filters to only denied endpoints when checked', async () => {
      const access = buildAccessMap(ENDPOINTS, 'granted');
      access[endpointRowId(ENDPOINTS[1])] = 'denied';
      renderWithTheme(<EndpointList {...defaultProps} accessById={access} />);

      const checkbox = screen.getByRole('checkbox');
      await userEvent.click(checkbox);
      expect(screen.getAllByRole('option')).toHaveLength(1);
      expect(screen.getByText('/service/rest/v1/repositories')).toBeInTheDocument();
    });
  });
});
