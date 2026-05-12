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
import {render, screen, fireEvent, waitFor} from '@testing-library/react';
import '@testing-library/jest-dom';
import Axios from 'axios';

import SearchRadix from './SearchRadix';

const mockGo = jest.fn().mockResolvedValue(undefined);
const mockSearch = jest.fn();

jest.mock('@uirouter/react', () => ({
  useRouter: () => ({
    stateService: {go: mockGo},
    stateRegistry: {
      get: () => ({data: {visibilityRequirements: []}}),
    },
  }),
  useCurrentStateAndParams: () => ({
    state: {name: 'browse.welcome'},
    params: {},
  }),
}));

let mockIsVisible = true;

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtJS: {
    search: (...args) => mockSearch(...args),
    state: () => ({getValue: () => false}),
    showSuccessMessage: jest.fn(),
  },
  useIsVisible: () => mockIsVisible,
  handleExtJsUnsavedChanges: (_ctrl, fn) => fn(),
}));

jest.mock('axios', () => ({
  get: jest.fn().mockResolvedValue({data: []}),
}));

jest.mock('../../routerConfig/routeNames/routeNames', () => ({
  ROUTE_NAMES: {
    BROWSE: {
      SEARCH: {
        ROOT: 'browse.search',
        GENERIC: 'browse.search.generic',
      },
    },
  },
}));

describe('SearchRadix', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockIsVisible = true;
  });

  describe('rendering', () => {
    it('renders the search input', () => {
      render(<SearchRadix />);
      expect(screen.getByPlaceholderText('Search components or CVEs...')).toBeInTheDocument();
    });

    it('renders nothing when visibility check fails', () => {
      mockIsVisible = false;
      const {container} = render(<SearchRadix />);
      expect(container.firstChild).toBeNull();
    });
  });

  describe('Default UI navigation (bug rhv)', () => {
    it('navigates via router on Enter with query (not hardcoded hash)', () => {
      render(<SearchRadix isPreviewUI={false} />);

      const input = screen.getByPlaceholderText('Search components or CVEs...');
      fireEvent.change(input, {target: {value: 'logback'}});
      fireEvent.keyDown(input, {key: 'Enter'});

      expect(mockGo).toHaveBeenCalledWith(
        'browse.search.generic',
        {keyword: '=keyword=logback'}
      );
    });

    it('navigates via router on Enter with empty query (keyword null)', () => {
      render(<SearchRadix isPreviewUI={false} />);

      const input = screen.getByPlaceholderText('Search components or CVEs...');
      fireEvent.keyDown(input, {key: 'Enter'});

      expect(mockGo).toHaveBeenCalledWith(
        'browse.search.generic',
        {keyword: null}
      );
    });

    it('does not set window.location.hash directly', () => {
      const originalHash = window.location.hash;
      render(<SearchRadix isPreviewUI={false} />);

      const input = screen.getByPlaceholderText('Search components or CVEs...');
      fireEvent.keyDown(input, {key: 'Enter'});

      expect(window.location.hash).toBe(originalHash);
    });
  });

  describe('Preview UI navigation', () => {
    it('navigates to unified search on Enter with query', () => {
      render(<SearchRadix isPreviewUI={true} />);

      const input = screen.getByPlaceholderText('Search components or CVEs...');
      fireEvent.change(input, {target: {value: 'react'}});
      fireEvent.keyDown(input, {key: 'Enter'});

      expect(mockGo).toHaveBeenCalledWith(
        'preview.browse.search.unified',
        {q: 'react'}
      );
    });

    it('navigates to unified search on Enter without query', () => {
      render(<SearchRadix isPreviewUI={true} />);

      const input = screen.getByPlaceholderText('Search components or CVEs...');
      fireEvent.keyDown(input, {key: 'Enter'});

      expect(mockGo).toHaveBeenCalledWith('preview.browse.search.unified');
    });
  });

  describe('input behavior', () => {
    it('updates input value on typing', () => {
      render(<SearchRadix />);

      const input = screen.getByPlaceholderText('Search components or CVEs...');
      fireEvent.change(input, {target: {value: 'test-input'}});

      expect(input).toHaveValue('test-input');
    });
  });

  describe('autocomplete behavior', () => {
    it('navigates to component detail when a suggestion is clicked in Preview UI', async () => {
      const mockSuggestions = [
        { name: 'test-comp', format: 'maven2', group: 'org.test', version: '1.0', repository: 'maven-releases' }
      ];
      Axios.get.mockResolvedValue({ data: mockSuggestions });

      render(<SearchRadix isPreviewUI={true} />);

      const input = screen.getByPlaceholderText('Search components or CVEs...');
      fireEvent.change(input, { target: { value: 'test' } });

      // Wait for suggestions to appear
      await waitFor(() => {
        expect(screen.getByTestId('search-suggestion-0')).toBeInTheDocument();
      });

      // Simulate mousedown (triggers selection - navigation is deferred via queueMicrotask)
      fireEvent.mouseDown(screen.getByTestId('search-suggestion-0'));

      await waitFor(() => {
        expect(mockGo).toHaveBeenCalledWith('preview.browse.search.component', {
          gaId: 'maven2:org.test:test-comp',
          keyword: 'test-comp',
          version: '1.0'
        });
      });
    });

    it('navigates to component detail when Enter is pressed on a selected suggestion', async () => {
      const mockSuggestions = [
        { name: 'test-comp', format: 'maven2', group: 'org.test', version: '1.0', repository: 'maven-releases' }
      ];
      Axios.get.mockResolvedValue({ data: mockSuggestions });

      render(<SearchRadix isPreviewUI={true} />);

      const input = screen.getByPlaceholderText('Search components or CVEs...');
      fireEvent.change(input, { target: { value: 'test' } });

      await waitFor(() => {
        expect(screen.getByTestId('search-suggestion-0')).toBeInTheDocument();
      });

      // Press ArrowDown to select the first suggestion
      fireEvent.keyDown(input, { key: 'ArrowDown' });
      
      // Press Enter to navigate (navigation is deferred via queueMicrotask)
      fireEvent.keyDown(input, { key: 'Enter' });

      await waitFor(() => {
        expect(mockGo).toHaveBeenCalledWith('preview.browse.search.component', {
          gaId: 'maven2:org.test:test-comp',
          keyword: 'test-comp',
          version: '1.0'
        });
      });
    });
  });
});
