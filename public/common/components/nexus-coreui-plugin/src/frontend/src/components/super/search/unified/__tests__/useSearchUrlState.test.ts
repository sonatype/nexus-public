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

import { renderHook, act } from '@testing-library/react';
import { useSearchUrlState } from '../useSearchUrlState';

describe('useSearchUrlState', () => {
  // Mock pushState
  let pushStateSpy: jest.SpyInstance;

  // Helper to set up window.location with custom properties
  const setLocation = (props: Partial<Location>) => {
    const location = {
      ...window.location,
      origin: 'http://localhost',
      pathname: '/',
      search: '',
      hash: '',
      ...props,
    };
    Object.defineProperty(window, 'location', {
      value: location,
      writable: true,
      configurable: true,
    });
  };

  beforeEach(() => {
    // Reset location
    setLocation({ search: '', hash: '' });
    pushStateSpy = jest.spyOn(window.history, 'pushState').mockImplementation(() => {});
  });

  afterEach(() => {
    pushStateSpy.mockRestore();
  });

  describe('URL Parsing', () => {
    it('reads format from URL query params', () => {
      setLocation({ search: '?format=maven&q=spring' });

      const { result } = renderHook(() => useSearchUrlState());

      expect(result.current.state.format).toBe('maven');
      expect(result.current.state.query).toBe('spring');
    });

    it('defaults to "all" format when not specified', () => {
      setLocation({ search: '?q=test' });

      const { result } = renderHook(() => useSearchUrlState());

      expect(result.current.state.format).toBe('all');
      expect(result.current.state.query).toBe('test');
    });

    it('reads format from hash-based URL', () => {
      setLocation({
        search: '',
        hash: '#preview/browse/search?format=npm&q=lodash',
      });

      const { result } = renderHook(() => useSearchUrlState());

      expect(result.current.state.format).toBe('npm');
      expect(result.current.state.query).toBe('lodash');
    });

    it('parses Maven-specific filters from URL', () => {
      setLocation({
        search: '?format=maven&q=spring&maven.groupId=org.springframework&maven.artifactId=spring-core',
      });

      const { result } = renderHook(() => useSearchUrlState());

      expect(result.current.state.format).toBe('maven');
      expect(result.current.state.query).toBe('spring');
      expect(result.current.state.filters.groupId).toBe('org.springframework');
      expect(result.current.state.filters.artifactId).toBe('spring-core');
    });

    it('parses npm-specific filters from URL', () => {
      setLocation({ search: '?format=npm&q=react&group=@types' });

      const { result } = renderHook(() => useSearchUrlState());

      expect(result.current.state.format).toBe('npm');
      expect(result.current.state.query).toBe('react');
      expect(result.current.state.filters.scope).toBe('@types');
    });

    it('handles invalid format by defaulting to "all"', () => {
      setLocation({ search: '?format=invalid-format&q=test' });

      const { result } = renderHook(() => useSearchUrlState());

      expect(result.current.state.format).toBe('all');
      expect(result.current.state.query).toBe('test');
    });

    it('handles empty URL gracefully', () => {
      setLocation({ search: '', hash: '' });

      const { result } = renderHook(() => useSearchUrlState());

      expect(result.current.state.format).toBe('all');
      expect(result.current.state.query).toBe('');
      expect(result.current.state.filters).toEqual({});
    });

    it('parses Docker-specific filters from URL', () => {
      setLocation({
        search: '?format=docker&docker.imageName=nginx&docker.imageTag=latest',
      });

      const { result } = renderHook(() => useSearchUrlState());

      expect(result.current.state.format).toBe('docker');
      expect(result.current.state.filters.imageName).toBe('nginx');
      expect(result.current.state.filters.imageTag).toBe('latest');
    });
  });

  describe('State Updates', () => {
    beforeEach(() => {
      setLocation({ search: '', hash: '' });
    });

    it('setFormat updates format and clears filters', () => {
      const { result } = renderHook(() => useSearchUrlState());

      // Set some initial filters
      act(() => {
        result.current.setFilter('name', 'test');
      });

      expect(result.current.state.filters.name).toBe('test');

      // Change format - should clear filters
      act(() => {
        result.current.setFormat('docker');
      });

      expect(result.current.state.format).toBe('docker');
      expect(result.current.state.filters).toEqual({});
    });

    it('setQuery updates the query', () => {
      const { result } = renderHook(() => useSearchUrlState());

      act(() => {
        result.current.setQuery('spring-boot');
      });

      expect(result.current.state.query).toBe('spring-boot');
    });

    it('setFilter updates a single filter', () => {
      const { result } = renderHook(() => useSearchUrlState());

      act(() => {
        result.current.setFormat('maven');
      });

      act(() => {
        result.current.setFilter('groupId', 'org.apache');
      });

      expect(result.current.state.filters.groupId).toBe('org.apache');
    });

    it('setFilters updates multiple filters at once', () => {
      const { result } = renderHook(() => useSearchUrlState());

      act(() => {
        result.current.setFormat('maven');
      });

      act(() => {
        result.current.setFilters({
          groupId: 'org.springframework',
          artifactId: 'spring-core',
        });
      });

      expect(result.current.state.filters.groupId).toBe('org.springframework');
      expect(result.current.state.filters.artifactId).toBe('spring-core');
    });

    it('reset clears all state', () => {
      const { result } = renderHook(() => useSearchUrlState());

      // Set some state
      act(() => {
        result.current.setFormat('maven');
      });
      act(() => {
        result.current.setQuery('spring');
      });
      act(() => {
        result.current.setFilter('groupId', 'org.springframework');
      });

      // Reset
      act(() => {
        result.current.reset();
      });

      expect(result.current.state.format).toBe('all');
      expect(result.current.state.query).toBe('');
      expect(result.current.state.filters).toEqual({});
    });

    it('preserves query when changing format', () => {
      const { result } = renderHook(() => useSearchUrlState());

      act(() => {
        result.current.setQuery('spring');
      });

      act(() => {
        result.current.setFormat('maven');
      });

      expect(result.current.state.query).toBe('spring');
      expect(result.current.state.format).toBe('maven');
    });
  });

  describe('URL Updates', () => {
    beforeEach(() => {
      setLocation({ search: '', hash: '' });
      pushStateSpy.mockClear();
    });

    it('updates URL when format changes', () => {
      const { result } = renderHook(() => useSearchUrlState());

      act(() => {
        result.current.setFormat('docker');
      });

      expect(pushStateSpy).toHaveBeenCalled();
    });

    it('updates URL when query changes', () => {
      const { result } = renderHook(() => useSearchUrlState());

      act(() => {
        result.current.setQuery('nginx');
      });

      expect(pushStateSpy).toHaveBeenCalled();
    });

    it('updates URL when filter changes', () => {
      const { result } = renderHook(() => useSearchUrlState());

      act(() => {
        result.current.setFilter('name', 'test-package');
      });

      expect(pushStateSpy).toHaveBeenCalled();
    });
  });

  describe('getShareableUrl', () => {
    beforeEach(() => {
      setLocation({ search: '', hash: '#preview/browse/search' });
    });

    it('generates shareable URL with current state', () => {
      const { result } = renderHook(() => useSearchUrlState());

      act(() => {
        result.current.setFormat('maven');
      });
      act(() => {
        result.current.setQuery('spring');
      });

      const url = result.current.getShareableUrl();

      expect(url).toContain('format=maven');
      expect(url).toContain('q=spring');
    });

    it('omits format when it is "all"', () => {
      const { result } = renderHook(() => useSearchUrlState());

      act(() => {
        result.current.setQuery('test');
      });

      const url = result.current.getShareableUrl();

      expect(url).not.toContain('format=');
      expect(url).toContain('q=test');
    });

    it('includes filter values in shareable URL', () => {
      const { result } = renderHook(() => useSearchUrlState());

      act(() => {
        result.current.setFormat('maven');
      });
      act(() => {
        result.current.setFilter('groupId', 'org.apache');
      });

      const url = result.current.getShareableUrl();

      expect(url).toContain('maven.groupId=org.apache');
    });

    it('generates valid URL with hash routing', () => {
      const { result } = renderHook(() => useSearchUrlState());

      act(() => {
        result.current.setFormat('npm');
      });
      act(() => {
        result.current.setQuery('lodash');
      });

      const url = result.current.getShareableUrl();

      expect(url).toContain('#preview/browse/search');
      expect(url).toContain('format=npm');
      expect(url).toContain('q=lodash');
    });
  });

  describe('Browser Navigation', () => {
    it('listens for popstate events', () => {
      setLocation({ search: '?format=maven&q=initial' });

      const addEventListenerSpy = jest.spyOn(window, 'addEventListener');

      const { unmount } = renderHook(() => useSearchUrlState());

      expect(addEventListenerSpy).toHaveBeenCalledWith('popstate', expect.any(Function));

      unmount();

      addEventListenerSpy.mockRestore();
    });

    it('cleans up popstate listener on unmount', () => {
      setLocation({ search: '' });

      const removeEventListenerSpy = jest.spyOn(window, 'removeEventListener');

      const { unmount } = renderHook(() => useSearchUrlState());
      unmount();

      expect(removeEventListenerSpy).toHaveBeenCalledWith('popstate', expect.any(Function));

      removeEventListenerSpy.mockRestore();
    });
  });
});
