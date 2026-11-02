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
  endpoint({
    httpMethod: 'GET',
    fullPath: '/service/rest/v1/status',
    tag: 'Status',
    summary: 'Health check',
    operationId: 'getSystemStatus',
  }),
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

  // NEXUS-54907: the Method dropdown is gone; the search box matches methods instead.
  describe('Method dropdown removal', () => {
    it('shouldNotRenderAMethodFilterDropdown', () => {
      renderWithTheme(<EndpointList {...defaultProps} />);
      expect(screen.queryByText('Method')).not.toBeInTheDocument();
      expect(screen.getAllByRole('combobox')).toHaveLength(1);
    });

    it('shouldStillRenderTheTagFilterDropdown', () => {
      renderWithTheme(<EndpointList {...defaultProps} />);
      expect(screen.getByText('Tag')).toBeInTheDocument();
      expect(screen.getByRole('combobox')).toBeInTheDocument();
    });

    it('shouldAdvertiseMethodSearchInThePlaceholder', () => {
      renderWithTheme(<EndpointList {...defaultProps} />);
      expect(screen.getByRole('textbox', { name: 'Search endpoints' })).toHaveAttribute(
        'placeholder',
        'Search by method, path or summary'
      );
    });
  });

  describe('Method search', () => {
    it('shouldListOnlyEndpointsWithTheTypedMethod', async () => {
      renderWithTheme(<EndpointList {...defaultProps} />);
      await userEvent.type(screen.getByRole('textbox', { name: 'Search endpoints' }), 'DELETE');
      expect(screen.getAllByRole('option')).toHaveLength(1);
      expect(screen.getByText('/service/rest/v1/repositories/{name}')).toBeInTheDocument();
    });

    it('shouldMatchMethodCaseInsensitively', async () => {
      renderWithTheme(<EndpointList {...defaultProps} />);
      await userEvent.type(screen.getByRole('textbox', { name: 'Search endpoints' }), 'get');
      expect(screen.getAllByRole('option')).toHaveLength(2);
    });

    it('shouldMatchMethodAndPartialPathTypedTogether', async () => {
      renderWithTheme(<EndpointList {...defaultProps} />);
      await userEvent.type(screen.getByRole('textbox', { name: 'Search endpoints' }), 'GET /v1/status');
      expect(screen.getAllByRole('option')).toHaveLength(1);
      expect(screen.getByText('/service/rest/v1/status')).toBeInTheDocument();
    });

    it('shouldRequireEveryTokenToMatch', async () => {
      renderWithTheme(<EndpointList {...defaultProps} />);
      await userEvent.type(screen.getByRole('textbox', { name: 'Search endpoints' }), 'POST /v1/status');
      expect(screen.getByText(/No endpoints match/)).toBeInTheDocument();
    });

    it('shouldIgnoreExtraWhitespaceBetweenTokens', async () => {
      renderWithTheme(<EndpointList {...defaultProps} />);
      await userEvent.type(screen.getByRole('textbox', { name: 'Search endpoints' }), '  GET   status  ');
      expect(screen.getAllByRole('option')).toHaveLength(1);
      expect(screen.getByText('/service/rest/v1/status')).toBeInTheDocument();
    });

    it('shouldMatchByOperationId', async () => {
      renderWithTheme(<EndpointList {...defaultProps} />);
      await userEvent.type(screen.getByRole('textbox', { name: 'Search endpoints' }), 'getSystemStatus');
      expect(screen.getAllByRole('option')).toHaveLength(1);
      expect(screen.getByText('/service/rest/v1/status')).toBeInTheDocument();
    });
  });

  // NEXUS-54907: a token that names an HTTP method is matched exactly against the
  // method field, so it cannot match path, summary or operationId text.
  describe('Method search exactness', () => {
    const COLLIDING: MergedApiEndpoint[] = [
      endpoint({
        httpMethod: 'PUT',
        fullPath: '/service/rest/v1/repositories/maven',
        tag: 'Repositories',
        summary: 'Update a repository',
        operationId: 'updateRepository',
      }),
      endpoint({
        httpMethod: 'GET',
        fullPath: '/service/rest/v1/tasks',
        tag: 'Tasks',
        summary: 'List task output logs',
        operationId: 'getTaskOutput',
      }),
      endpoint({
        httpMethod: 'POST',
        fullPath: '/service/rest/v1/headers',
        tag: 'Headers',
        summary: 'Create a response header',
        operationId: 'createHeader',
      }),
    ];

    const collidingProps = {
      ...defaultProps,
      endpoints: COLLIDING,
      accessById: buildAccessMap(COLLIDING, 'granted' as EndpointAccessDot),
    };

    it('shouldNotMatchPutAgainstOutputInASummary', async () => {
      renderWithTheme(<EndpointList {...collidingProps} />);
      await userEvent.type(screen.getByRole('textbox', { name: 'Search endpoints' }), 'PUT');
      expect(screen.getAllByRole('option')).toHaveLength(1);
      expect(screen.getByText('/service/rest/v1/repositories/maven')).toBeInTheDocument();
    });

    it('shouldNotMatchHeadAgainstHeaderInASummary', async () => {
      renderWithTheme(<EndpointList {...collidingProps} />);
      await userEvent.type(screen.getByRole('textbox', { name: 'Search endpoints' }), 'HEAD');
      expect(screen.getByText(/No endpoints match/)).toBeInTheDocument();
    });

    it('shouldNotMatchGetAgainstOperationIdPrefix', async () => {
      renderWithTheme(<EndpointList {...collidingProps} />);
      await userEvent.type(screen.getByRole('textbox', { name: 'Search endpoints' }), 'GET');
      expect(screen.getAllByRole('option')).toHaveLength(1);
      expect(screen.getByText('/service/rest/v1/tasks')).toBeInTheDocument();
    });

    it('shouldStillMatchNonMethodTokensAsSubstrings', async () => {
      renderWithTheme(<EndpointList {...collidingProps} />);
      await userEvent.type(screen.getByRole('textbox', { name: 'Search endpoints' }), 'output');
      expect(screen.getAllByRole('option')).toHaveLength(1);
      expect(screen.getByText('/service/rest/v1/tasks')).toBeInTheDocument();
    });

    it('shouldStillMatchAPartiallyTypedMethodAsASubstring', async () => {
      renderWithTheme(<EndpointList {...collidingProps} />);
      await userEvent.type(screen.getByRole('textbox', { name: 'Search endpoints' }), 'header');
      expect(screen.getAllByRole('option')).toHaveLength(1);
      expect(screen.getByText('/service/rest/v1/headers')).toBeInTheDocument();
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

  // Design restyle: method colors, access icons, result count, headings, path wrap
  describe('Method badge colors', () => {
    function badgeFor(method: string): HTMLElement {
      const el = screen.getByText(method).closest('.api-endpoint-list__method');
      if (!el) {
        throw new Error(`no method badge found for ${method}`);
      }
      return el as HTMLElement;
    }

    it('shouldColorEachMethodBadgeByItsOwnColor', () => {
      const endpoints: MergedApiEndpoint[] = [
        endpoint({ httpMethod: 'GET', fullPath: '/g', tag: 'T' }),
        endpoint({ httpMethod: 'POST', fullPath: '/p', tag: 'T' }),
        endpoint({ httpMethod: 'PUT', fullPath: '/u', tag: 'T' }),
        endpoint({ httpMethod: 'DELETE', fullPath: '/d', tag: 'T' }),
      ];
      renderWithTheme(
        <EndpointList {...defaultProps} endpoints={endpoints} accessById={buildAccessMap(endpoints, 'granted')} />
      );
      expect(badgeFor('GET')).toHaveAttribute('data-accent-color', 'blue');
      expect(badgeFor('POST')).toHaveAttribute('data-accent-color', 'green');
      expect(badgeFor('PUT')).toHaveAttribute('data-accent-color', 'amber');
      expect(badgeFor('DELETE')).toHaveAttribute('data-accent-color', 'red');
    });

    it('shouldColorPatchBadgeOrange', () => {
      const endpoints: MergedApiEndpoint[] = [endpoint({ httpMethod: 'PATCH', fullPath: '/pa', tag: 'T' })];
      renderWithTheme(
        <EndpointList {...defaultProps} endpoints={endpoints} accessById={buildAccessMap(endpoints, 'granted')} />
      );
      expect(badgeFor('PATCH')).toHaveAttribute('data-accent-color', 'orange');
    });

    it('shouldFallBackToGrayForUnmappedMethod', () => {
      const endpoints: MergedApiEndpoint[] = [endpoint({ httpMethod: 'HEAD', fullPath: '/h', tag: 'T' })];
      renderWithTheme(
        <EndpointList {...defaultProps} endpoints={endpoints} accessById={buildAccessMap(endpoints, 'granted')} />
      );
      expect(badgeFor('HEAD')).toHaveAttribute('data-accent-color', 'gray');
    });
  });

  describe('Access icons', () => {
    function renderSingle(state: EndpointAccessDot, palette: 'session' | 'roleLens' = 'session') {
      const only: MergedApiEndpoint[] = [endpoint({ httpMethod: 'GET', fullPath: '/only', tag: 'T' })];
      const access: Record<string, EndpointAccessDot> = { [endpointRowId(only[0])]: state };
      renderWithTheme(
        <EndpointList {...defaultProps} endpoints={only} accessById={access} accessDotPalette={palette} />
      );
    }

    // Assert the stable in-house contracts (aria-label + stroke color), not lucide CSS classes.
    function iconStroke(indicator: HTMLElement): string | null {
      return indicator.querySelector('svg')?.getAttribute('stroke') ?? null;
    }

    it('shouldLabelGrantedAccessWithGreenIcon', () => {
      renderSingle('granted');
      const indicator = screen.getByRole('img', { name: 'Full access' });
      expect(iconStroke(indicator)).toBe('var(--green-9)');
    });

    it('shouldLabelDeniedAccessWithRedIcon', () => {
      renderSingle('denied');
      const indicator = screen.getByRole('img', { name: 'No access' });
      expect(iconStroke(indicator)).toBe('var(--red-9)');
    });

    it('shouldLabelPartialAccessWithAmberIcon', () => {
      renderSingle('partial');
      const indicator = screen.getByRole('img', { name: 'Partial access' });
      expect(iconStroke(indicator)).toBe('var(--amber-9)');
    });

    it('shouldLabelUnknownAccessWithGrayIcon', () => {
      renderSingle('unknown');
      const indicator = screen.getByRole('img', { name: 'Access unknown' });
      expect(iconStroke(indicator)).toBe('var(--gray-8)');
    });

    it('shouldUseBluePaletteForGrantedInRoleLens', () => {
      renderSingle('granted', 'roleLens');
      const indicator = screen.getByRole('img', { name: 'Full access' });
      expect(iconStroke(indicator)).toBe('var(--blue-9)');
    });
  });

  describe('Result count', () => {
    it('shouldShowTotalWhenNoFilterApplied', () => {
      renderWithTheme(<EndpointList {...defaultProps} />);
      expect(screen.getByText('Showing 4 of 4 endpoints')).toBeInTheDocument();
    });

    it('shouldUpdateCountAsSearchNarrowsResults', async () => {
      renderWithTheme(<EndpointList {...defaultProps} />);
      await userEvent.type(screen.getByRole('textbox', { name: 'Search endpoints' }), 'status');
      expect(screen.getByText('Showing 1 of 4 endpoints')).toBeInTheDocument();
    });
  });

  describe('Tag group headings', () => {
    it('shouldRenderGroupHeadingWithUppercaseClass', () => {
      renderWithTheme(<EndpointList {...defaultProps} />);
      const heading = screen.getByText('Status');
      expect(heading).toHaveClass('api-endpoint-list__group-title');
    });
  });

  describe('Path wrapping', () => {
    it('shouldRenderFullPathWithoutTruncation', () => {
      const longPath = '/service/rest/v1/repositories/maven/{repositoryName}/{group}/{artifact}/maven-metadata.xml.sha512';
      const endpoints: MergedApiEndpoint[] = [endpoint({ httpMethod: 'GET', fullPath: longPath, tag: 'Maven' })];
      renderWithTheme(
        <EndpointList {...defaultProps} endpoints={endpoints} accessById={buildAccessMap(endpoints, 'granted')} />
      );
      const path = screen.getByText(longPath);
      expect(path).toHaveClass('api-endpoint-list__path');
      expect(path).toHaveTextContent(longPath);
    });
  });
});
