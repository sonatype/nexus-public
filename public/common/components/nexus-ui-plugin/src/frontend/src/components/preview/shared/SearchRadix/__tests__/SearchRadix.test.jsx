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
import {act, render, screen, fireEvent, waitFor} from '@testing-library/react';
import Axios from 'axios';
import SearchRadix from '../SearchRadix';
import {useCurrentStateAndParams, useRouter} from '@uirouter/react';
import useIsVisible from '../../../../../router/useIsVisible';
import {useIsCloud} from '../../hooks';

jest.mock('axios');

// Source: '../hooks' (1 up from SearchRadix/). From __tests__/ needs 2 ups.
// Spread the real barrel: the component also pulls `useDebouncedWithDiscard` and
// `debouncedValueOf` from here, and a bare object would stub those out too.
jest.mock('../../hooks', () => ({
  ...jest.requireActual('../../hooks'),
  useIsCloud: jest.fn(),
}));

jest.mock('@uirouter/react', () => ({
  useCurrentStateAndParams: jest.fn(),
  useRouter: jest.fn(),
}));

// Source: '../../../../router/useIsVisible' (4 up from SearchRadix/).
// From __tests__/ needs 5 ups.
jest.mock('../../../../../router/useIsVisible', () => jest.fn());

jest.mock('../../../../../router/extJsUnsavedChanges', () => ({
  handleExtJsUnsavedChanges: jest.fn((_ctrl, cb) => cb()),
}));

jest.mock('../../../../../interface/ExtJS', () => ({
  default: {search: jest.fn()},
}));

// Source: '../Badges' (1 up from SearchRadix/).
// From __tests__/ needs 2 ups.
jest.mock('../../Badges', () => ({
  FormatBadge: ({format}) => <span data-testid="format-badge">{format}</span>,
}));

jest.mock('@radix-ui/themes', () => ({
  TextField: {
    Root: ({children, placeholder, value, onChange, onKeyDown, onFocus, ...rest}) => (
      <div>
        <input placeholder={placeholder} value={value} onChange={onChange} onKeyDown={onKeyDown} onFocus={onFocus} {...rest} />
        {children}
      </div>
    ),
    Slot: ({children}) => <span>{children}</span>,
  },
}));

const ROUTES = {
  searchGeneric: 'browse.search.generic',
  previewSearchUnified: 'preview.browse.search.unified',
  previewSearchComponent: 'preview.browse.search.component',
};

/**
 * The debounce window this component is expected to honour. Asserted as a value rather than
 * imported so the tests pin the intended behaviour, not whatever the module happens to declare.
 */
const DEBOUNCE_MS = 200;

const mockGo = jest.fn();

function makeRouter() {
  return {
    stateService: {go: mockGo},
    stateRegistry: {
      get: jest.fn().mockReturnValue({data: {visibilityRequirements: 'nexus:search:read'}}),
    },
  };
}

function setupMocks({isVisible = true, stateName = 'some.state', isCloud = false} = {}) {
  useCurrentStateAndParams.mockReturnValue({state: {name: stateName}});
  useRouter.mockReturnValue(makeRouter());
  useIsVisible.mockReturnValue(isVisible);
  useIsCloud.mockReturnValue(isCloud);
}

describe('SearchRadix', () => {
  beforeEach(() => {
    jest.useFakeTimers();
    setupMocks();
    mockGo.mockClear();
    Axios.get.mockResolvedValue({data: []});
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  describe('visibility guard', () => {
    it('renders nothing when the route is not visible', () => {
      setupMocks({isVisible: false});
      const {container} = render(<SearchRadix isPreviewUI routes={ROUTES} />);
      expect(container.firstChild).toBeNull();
    });

    it('renders the search input when the route is visible', () => {
      render(<SearchRadix isPreviewUI routes={ROUTES} />);
      expect(screen.getByPlaceholderText(/search/i)).toBeInTheDocument();
    });

    it('shouldRenderPlaceholderWithoutMentioningCves', () => {
      render(<SearchRadix isPreviewUI routes={ROUTES} />);
      expect(
        screen.getByPlaceholderText('Search by name, group, or version')
      ).toBeInTheDocument();
    });
  });

  describe('input behaviour', () => {
    it('updates the search value on input change', () => {
      render(<SearchRadix isPreviewUI routes={ROUTES} />);
      const input = screen.getByPlaceholderText(/search/i);
      fireEvent.change(input, {target: {value: 'react'}});
      expect(input.value).toBe('react');
    });

    it('does not fetch suggestions for queries shorter than 2 chars', () => {
      render(<SearchRadix isPreviewUI routes={ROUTES} />);
      fireEvent.change(screen.getByPlaceholderText(/search/i), {target: {value: 'r'}});
      act(() => {
        jest.runAllTimers();
      });
      expect(Axios.get).not.toHaveBeenCalled();
    });

    it('fetches suggestions after debounce for queries >= 2 chars in preview UI', async () => {
      Axios.get.mockResolvedValueOnce({
        data: [{name: 'react', group: 'facebook', format: 'npm', version: '18.0.0', repository: 'npm-proxy'}],
      });
      render(<SearchRadix isPreviewUI routes={ROUTES} />);
      fireEvent.change(screen.getByPlaceholderText(/search/i), {target: {value: 're'}});
      act(() => {
        jest.runAllTimers();
      });
      await waitFor(() => expect(Axios.get).toHaveBeenCalled());
    });

    it('does not fetch in default (non-preview) UI mode', () => {
      render(<SearchRadix isPreviewUI={false} routes={ROUTES} />);
      fireEvent.change(screen.getByPlaceholderText(/search/i), {target: {value: 'react'}});
      act(() => {
        jest.runAllTimers();
      });
      expect(Axios.get).not.toHaveBeenCalled();
    });
  });

  describe('Default UI navigation (bug rhv)', () => {
    it('navigates via router on Enter with query (not hardcoded hash)', () => {
      render(<SearchRadix isPreviewUI={false} routes={ROUTES} />);

      const input = screen.getByPlaceholderText(/search/i);
      fireEvent.change(input, {target: {value: 'logback'}});
      fireEvent.keyDown(input, {key: 'Enter'});

      expect(mockGo).toHaveBeenCalledWith(
        ROUTES.searchGeneric,
        {keyword: '=keyword=logback'}
      );
    });

    it('navigates via router on Enter with empty query (keyword null)', () => {
      render(<SearchRadix isPreviewUI={false} routes={ROUTES} />);

      const input = screen.getByPlaceholderText(/search/i);
      fireEvent.keyDown(input, {key: 'Enter'});

      expect(mockGo).toHaveBeenCalledWith(
        ROUTES.searchGeneric,
        {keyword: null}
      );
    });

    it('does not set window.location.hash directly', () => {
      const originalHash = window.location.hash;
      render(<SearchRadix isPreviewUI={false} routes={ROUTES} />);

      const input = screen.getByPlaceholderText(/search/i);
      fireEvent.keyDown(input, {key: 'Enter'});

      expect(window.location.hash).toBe(originalHash);
    });
  });

  describe('data-analytics-id (per-distribution)', () => {
    it('uses the self-hosted analytics id when not on cloud', () => {
      setupMocks({isCloud: false});
      render(<SearchRadix isPreviewUI routes={ROUTES} />);
      expect(screen.getByPlaceholderText(/search/i)).toHaveAttribute(
        'data-analytics-id',
        'nxrm-header-search'
      );
    });

    it('uses the cloud analytics id when on cloud', () => {
      setupMocks({isCloud: true});
      render(<SearchRadix isPreviewUI routes={ROUTES} />);
      expect(screen.getByPlaceholderText(/search/i)).toHaveAttribute(
        'data-analytics-id',
        'nxrm-global-header-search-input'
      );
    });
  });

  describe('Enter key navigation (preview UI)', () => {
    it('navigates to previewSearchUnified with a query on Enter', () => {
      render(<SearchRadix isPreviewUI routes={ROUTES} />);
      const input = screen.getByPlaceholderText(/search/i);
      fireEvent.change(input, {target: {value: 'my-lib'}});
      fireEvent.keyDown(input, {key: 'Enter'});
      expect(mockGo).toHaveBeenCalledWith(
        ROUTES.previewSearchUnified,
        {q: 'my-lib'},
        {inherit: false, reload: ROUTES.previewSearchUnified},
      );
    });

    it('navigates to previewSearchUnified without params on Enter with empty query', () => {
      render(<SearchRadix isPreviewUI routes={ROUTES} />);
      fireEvent.keyDown(screen.getByPlaceholderText(/search/i), {key: 'Enter'});
      expect(mockGo).toHaveBeenCalledWith(
        ROUTES.previewSearchUnified,
        {},
        {inherit: false, reload: ROUTES.previewSearchUnified},
      );
    });

    it('navigates without inheriting stale router params (AT-017)', () => {
      render(<SearchRadix isPreviewUI routes={ROUTES} />);
      const input = screen.getByPlaceholderText(/search/i);
      fireEvent.change(input, {target: {value: 'shared02'}});
      fireEvent.keyDown(input, {key: 'Enter'});
      expect(mockGo).toHaveBeenCalledWith(
        ROUTES.previewSearchUnified,
        {q: 'shared02'},
        {inherit: false, reload: ROUTES.previewSearchUnified},
      );
    });

    it('resets params on an empty search (AT-017)', () => {
      render(<SearchRadix isPreviewUI routes={ROUTES} />);
      fireEvent.keyDown(screen.getByPlaceholderText(/search/i), {key: 'Enter'});
      expect(mockGo).toHaveBeenCalledWith(
        ROUTES.previewSearchUnified,
        {},
        {inherit: false, reload: ROUTES.previewSearchUnified},
      );
    });

    it('forces the transition when the term matches the router cache (AT-017)', () => {
      // inherit: false alone is not enough. The search page writes its URL with
      // raw pushState, so the router keeps a stale `q`; re-submitting that same
      // term produces a same-state/same-params transition, which UI-Router
      // rejects as 'SameAsCurrent' and the page never re-syncs. The reload
      // option names the state rather than passing `true`, which would resolve
      // to the registry root and remount the whole preview shell.
      render(<SearchRadix isPreviewUI routes={ROUTES} />);
      const input = screen.getByPlaceholderText(/search/i);
      fireEvent.change(input, {target: {value: 'spring'}});
      fireEvent.keyDown(input, {key: 'Enter'});

      const [, , options] = mockGo.mock.calls.at(-1);
      expect(options.reload).toBe(ROUTES.previewSearchUnified);
      expect(options.reload).not.toBe(true);
    });
  });

  describe('suggestions dropdown', () => {
    const suggestions = [
      {name: 'react', group: 'facebook', format: 'npm', version: '18.0.0', repository: 'npm-proxy'},
      {name: 'react-dom', group: 'react', format: 'npm', version: '18.0.0', repository: 'npm-proxy'},
    ];

    async function openSuggestions() {
      Axios.get.mockResolvedValueOnce({data: suggestions});
      render(<SearchRadix isPreviewUI routes={ROUTES} />);
      const input = screen.getByPlaceholderText(/search/i);
      fireEvent.change(input, {target: {value: 'react'}});
      act(() => {
        jest.runAllTimers();
      });
      await waitFor(() => screen.getByText(/press enter to search/i));
      return input;
    }

    it('shows the "Press Enter" footer text', async () => {
      await openSuggestions();
      expect(screen.getByText(/press enter to search/i)).toBeInTheDocument();
    });

    it('renders a format badge for each suggestion', async () => {
      await openSuggestions();
      expect(screen.getAllByTestId('format-badge').length).toBeGreaterThan(0);
    });

    it('highlights a suggestion on mouse enter', async () => {
      await openSuggestions();
      const items = screen.getAllByRole('option');
      fireEvent.mouseEnter(items[0]);
      expect(items[0]).toHaveClass('search-radix-suggestion--selected');
    });

    it('navigates to component detail on suggestion mousedown', async () => {
      await openSuggestions();
      fireEvent.mouseDown(screen.getAllByRole('option')[0]);
      await waitFor(() =>
        expect(mockGo).toHaveBeenCalledWith(
          ROUTES.previewSearchComponent,
          expect.objectContaining({gaId: expect.any(String)})
        )
      );
    });

    it('closes suggestions when clicking outside', async () => {
      await openSuggestions();
      fireEvent.mouseDown(document.body);
      expect(screen.queryByRole('option')).toBeNull();
    });

    it('shows the version in each suggestion', async () => {
      await openSuggestions();
      expect(screen.getAllByText('18.0.0').length).toBeGreaterThan(0);
    });

    it('collapses immediately when the query is deleted below the minimum length', async () => {
      const input = await openSuggestions();

      fireEvent.change(input, {target: {value: 'r'}});

      // No timer advance: shortening the query must close the dropdown at once rather than
      // leaving stale suggestions visible for another debounce interval.
      expect(screen.queryByRole('option')).toBeNull();
    });
  });

  describe('keyboard navigation in suggestions', () => {
    const suggestions = [
      {name: 'alpha', group: 'g', format: 'npm', version: '1.0.0', repository: 'r'},
      {name: 'beta', group: 'g', format: 'npm', version: '2.0.0', repository: 'r'},
    ];

    async function openSuggestions() {
      Axios.get.mockResolvedValueOnce({data: suggestions});
      render(<SearchRadix isPreviewUI routes={ROUTES} />);
      const input = screen.getByPlaceholderText(/search/i);
      fireEvent.change(input, {target: {value: 'al'}});
      act(() => {
        jest.runAllTimers();
      });
      await waitFor(() => screen.getAllByRole('option'));
      return input;
    }

    it('highlights the first suggestion on ArrowDown', async () => {
      const input = await openSuggestions();
      fireEvent.keyDown(input, {key: 'ArrowDown'});
      expect(screen.getAllByRole('option')[0]).toHaveClass('search-radix-suggestion--selected');
    });

    it('clears selection on ArrowUp from index 0', async () => {
      const input = await openSuggestions();
      fireEvent.keyDown(input, {key: 'ArrowUp'});
      screen.getAllByRole('option').forEach(item =>
        expect(item).not.toHaveClass('search-radix-suggestion--selected')
      );
    });

    it('closes suggestions on Escape', async () => {
      const input = await openSuggestions();
      fireEvent.keyDown(input, {key: 'Escape'});
      expect(screen.queryByRole('option')).toBeNull();
    });

    it('navigates to component detail when Enter is pressed on a highlighted suggestion', async () => {
      const input = await openSuggestions();
      fireEvent.keyDown(input, {key: 'ArrowDown'});
      fireEvent.keyDown(input, {key: 'Enter'});
      await waitFor(() =>
        expect(mockGo).toHaveBeenCalledWith(
          ROUTES.previewSearchComponent,
          expect.objectContaining({gaId: expect.any(String)})
        )
      );
    });
  });

  describe('re-entering a query whose suggestions were discarded', () => {
    const suggestions = [
      {name: 'react', group: 'facebook', format: 'npm', version: '18.0.0', repository: 'npm-proxy'},
      {name: 'react-dom', group: 'react', format: 'npm', version: '18.0.0', repository: 'npm-proxy'},
    ];

    async function openSuggestions() {
      render(<SearchRadix isPreviewUI routes={ROUTES} />);
      const input = screen.getByPlaceholderText(/search/i);
      fireEvent.change(input, {target: {value: 'react'}});
      await act(async () => {
        jest.advanceTimersByTime(DEBOUNCE_MS);
      });
      await waitFor(() => expect(screen.getAllByRole('option')).toHaveLength(2));
      expect(Axios.get).toHaveBeenCalledTimes(1);
      return input;
    }

    beforeEach(() => {
      Axios.get.mockResolvedValue({data: suggestions});
    });

    it('fetches again when the same query is re-entered after dropping below the minimum length', async () => {
      const input = await openSuggestions();

      // Below the minimum: the list collapses at once, and nothing is requested for the short query.
      fireEvent.change(input, {target: {value: 'r'}});
      expect(screen.queryByRole('option')).toBeNull();
      await act(async () => {
        jest.advanceTimersByTime(DEBOUNCE_MS / 2);
      });
      expect(Axios.get).toHaveBeenCalledTimes(1);

      // Back to the query that was already searched, inside the same debounce window. The debounced
      // value settles on what it last published, so this is the case a plain value debounce swallows.
      fireEvent.change(input, {target: {value: 'react'}});
      await act(async () => {
        jest.advanceTimersByTime(DEBOUNCE_MS);
      });

      expect(Axios.get).toHaveBeenCalledTimes(2);
      expect(Axios.get).toHaveBeenLastCalledWith(
        '/service/rest/v1/search/suggest',
        expect.objectContaining({params: expect.objectContaining({q: 'react'})}),
      );
      await waitFor(() => expect(screen.getAllByRole('option')).toHaveLength(2));
    });

    it('fetches again when the same query is retyped right after picking a suggestion', async () => {
      const input = await openSuggestions();

      // Picking a suggestion empties the field, which discards the suggestions through the same
      // below-minimum path.
      fireEvent.mouseDown(screen.getAllByRole('option')[0]);
      expect(input.value).toBe('');
      expect(screen.queryByRole('option')).toBeNull();
      await act(async () => {
        jest.advanceTimersByTime(DEBOUNCE_MS / 2);
      });
      expect(Axios.get).toHaveBeenCalledTimes(1);

      fireEvent.change(input, {target: {value: 'react'}});
      await act(async () => {
        jest.advanceTimersByTime(DEBOUNCE_MS);
      });

      expect(Axios.get).toHaveBeenCalledTimes(2);
      await waitFor(() => expect(screen.getAllByRole('option')).toHaveLength(2));
    });
  });

  describe('suggestion responses that land after the query was discarded', () => {
    const suggestions = [
      {name: 'react', group: 'facebook', format: 'npm', version: '18.0.0', repository: 'npm-proxy'},
      {name: 'react-dom', group: 'react', format: 'npm', version: '18.0.0', repository: 'npm-proxy'},
    ];

    function deferred() {
      let resolve;
      const promise = new Promise((res) => {
        resolve = res;
      });
      return {promise, resolve};
    }

    it('ignores a response that lands after the field was cleared', async () => {
      const request = deferred();
      Axios.get.mockReturnValue(request.promise);

      render(<SearchRadix isPreviewUI routes={ROUTES} />);
      const input = screen.getByPlaceholderText(/search/i);
      fireEvent.change(input, {target: {value: 'react'}});
      await act(async () => {
        jest.advanceTimersByTime(DEBOUNCE_MS);
      });
      expect(Axios.get).toHaveBeenCalledTimes(1);

      // Clear the field while the suggest request is still running.
      fireEvent.change(input, {target: {value: ''}});
      await act(async () => {});
      expect(screen.queryByRole('option')).toBeNull();

      // The abandoned request answers late; it must not reopen the dropdown over an empty input.
      await act(async () => {
        request.resolve({data: suggestions});
      });

      expect(screen.queryByRole('option')).toBeNull();
    });

    it('ignores a response that lands after a suggestion was picked', async () => {
      const pending = deferred();
      Axios.get.mockResolvedValueOnce({data: suggestions}).mockReturnValue(pending.promise);

      render(<SearchRadix isPreviewUI routes={ROUTES} />);
      const input = screen.getByPlaceholderText(/search/i);
      fireEvent.change(input, {target: {value: 'react'}});
      await act(async () => {
        jest.advanceTimersByTime(DEBOUNCE_MS);
      });
      await waitFor(() => expect(screen.getAllByRole('option')).toHaveLength(2));

      // Extend the query so a second request is in flight while the list is still open.
      fireEvent.change(input, {target: {value: 'react-dom'}});
      await act(async () => {
        jest.advanceTimersByTime(DEBOUNCE_MS);
      });
      expect(Axios.get).toHaveBeenCalledTimes(2);

      // Picking a suggestion empties the field and discards the list.
      fireEvent.mouseDown(screen.getAllByRole('option')[0]);
      expect(input.value).toBe('');
      expect(screen.queryByRole('option')).toBeNull();

      await act(async () => {
        pending.resolve({data: suggestions});
      });

      expect(screen.queryByRole('option')).toBeNull();
    });
  });

  describe('focus behaviour', () => {
    it('re-shows suggestions on focus when suggestions already exist', async () => {
      const suggestions = [{name: 'lib', group: 'g', format: 'npm', version: '1.0', repository: 'r'}];
      Axios.get.mockResolvedValueOnce({data: suggestions});
      render(<SearchRadix isPreviewUI routes={ROUTES} />);
      const input = screen.getByPlaceholderText(/search/i);
      fireEvent.change(input, {target: {value: 'li'}});
      act(() => {
        jest.runAllTimers();
      });
      await waitFor(() => screen.getAllByRole('option'));
      fireEvent.mouseDown(document.body);
      fireEvent.focus(input);
      expect(screen.getAllByRole('option').length).toBeGreaterThan(0);
    });
  });

  describe('suggestion navigation includes version', () => {
    it('passes the version in navigation params when the suggestion has one', async () => {
      const suggestions = [{name: 'pkg', group: 'g', format: 'npm', version: '3.0.0', repository: 'r'}];
      Axios.get.mockResolvedValueOnce({data: suggestions});
      render(<SearchRadix isPreviewUI routes={ROUTES} />);
      const input = screen.getByPlaceholderText(/search/i);
      fireEvent.change(input, {target: {value: 'pk'}});
      act(() => {
        jest.runAllTimers();
      });
      await waitFor(() => screen.getAllByRole('option'));
      fireEvent.mouseDown(screen.getAllByRole('option')[0]);
      await waitFor(() =>
        expect(mockGo).toHaveBeenCalledWith(
          ROUTES.previewSearchComponent,
          expect.objectContaining({version: '3.0.0'})
        )
      );
    });
  });
});
