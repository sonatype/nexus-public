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

import React, {useState, useEffect, useRef, useCallback} from 'react';
import {useCurrentStateAndParams, useRouter} from '@uirouter/react';
import {TextField} from '@radix-ui/themes';
import {Search as SearchIcon} from 'lucide-react';
import {FormatBadge} from '@sonatype/nexus-ui-plugin';
import {ExtJS, useIsVisible, handleExtJsUnsavedChanges} from '@sonatype/nexus-ui-plugin';
import {ROUTE_NAMES} from '../../routerConfig/routeNames/routeNames';
import Axios from 'axios';

import './SearchRadix.scss';

const DEBOUNCE_MS = 200;
const MIN_QUERY_LENGTH = 2;
const MAX_SUGGESTIONS = 8;

/**
 * Fetch search suggestions from the lightweight suggest API.
 */
async function fetchSuggestions(query) {
  if (!query || query.trim().length < MIN_QUERY_LENGTH) {
    return [];
  }
  try {
    const response = await Axios.get('/service/rest/v1/search/suggest', {
      params: { q: query.trim(), limit: MAX_SUGGESTIONS }
    });
    return response.data || [];
  } catch (error) {
    console.error('[SearchRadix] Failed to fetch suggestions:', error);
    return [];
  }
}

/**
 * Get display name for a suggestion.
 */
function getDisplayName(suggestion) {
  if (suggestion.group && suggestion.group !== suggestion.name) {
    return `${suggestion.group}:${suggestion.name}`;
  }
  return suggestion.name;
}

/**
 * Global search bar with autocomplete in the header.
 * 
 * Features:
 * - Debounced autocomplete suggestions
 * - Keyboard navigation (up/down arrows, Enter, Escape)
 * - Click to select suggestion
 * - Format badges on suggestions
 * 
 * Routing behavior:
 * - Preview UI: Navigate to #preview/browse/search?q=<query>
 * - Default UI: Navigate to #browse/search/generic=keyword=<query>
 */
export default function SearchRadix({ isPreviewUI = false }) {
  const [searchValue, setSearchValue] = useState('');
  const [suggestions, setSuggestions] = useState([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [selectedIndex, setSelectedIndex] = useState(-1);
  const [isLoading, setIsLoading] = useState(false);
  
  const inputRef = useRef(null);
  const suggestionsRef = useRef(null);
  const debounceRef = useRef(null);
  
  const router = useRouter();
  const {state} = useCurrentStateAndParams();

  // Check visibility for Default UI search route
  const searchRoute = router.stateRegistry.get(ROUTE_NAMES.BROWSE.SEARCH.GENERIC);
  const defaultUIVisible = useIsVisible(searchRoute?.data?.visibilityRequirements);

  // Fetch suggestions with debounce
  // NOTE: All hooks must be called before any conditional returns!
  useEffect(() => {
    if (debounceRef.current) {
      clearTimeout(debounceRef.current);
    }

    if (searchValue.trim().length < MIN_QUERY_LENGTH) {
      setSuggestions([]);
      setShowSuggestions(false);
      return;
    }

    // Autocomplete suggestions are only available in Preview UI
    // Default UI uses the traditional search page (no autocomplete)
    if (!isPreviewUI) {
      return;
    }

    setIsLoading(true);
    debounceRef.current = setTimeout(async () => {
      const results = await fetchSuggestions(searchValue);
      setSuggestions(results);
      const shouldShow = results.length > 0;
      setShowSuggestions(shouldShow);
      setSelectedIndex(-1);
      setIsLoading(false);
    }, DEBOUNCE_MS);

    return () => {
      if (debounceRef.current) {
        clearTimeout(debounceRef.current);
      }
    };
  }, [searchValue]);

  // Close suggestions when clicking outside
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (
        suggestionsRef.current && 
        !suggestionsRef.current.contains(event.target) &&
        inputRef.current &&
        !inputRef.current.contains(event.target)
      ) {
        setShowSuggestions(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Navigate to search page
  const navigateToSearch = useCallback((query) => {
    setShowSuggestions(false);
    
    if (isPreviewUI) {
      if (query) {
        router.stateService.go('preview.browse.search.unified', { q: query });
      } else {
        router.stateService.go('preview.browse.search.unified');
      }
    } else {
      const menuCtrl =
        window.Ext && Ext.getApplication && Ext.getApplication().getController
          ? Ext.getApplication().getController('Menu')
          : null;

      handleExtJsUnsavedChanges(menuCtrl, async () => {
        if (state.name !== ROUTE_NAMES.BROWSE.SEARCH.GENERIC) {
          await router.stateService.go(ROUTE_NAMES.BROWSE.SEARCH.GENERIC, {
            keyword: query ? `=keyword=${encodeURIComponent(query)}` : null
          });
        } else if (query) {
          ExtJS.search(query);
        }
      });
    }
  }, [isPreviewUI, state.name, router]);

  // Handle suggestion selection - navigate to the component detail page
  const handleSelectSuggestion = useCallback((suggestion) => {
    setShowSuggestions(false);
    setSearchValue('');

    if (isPreviewUI) {
      // Build gaId (format:group:name) for the component detail route
      const parts = [suggestion.format];
      if (suggestion.group) parts.push(suggestion.group);
      parts.push(suggestion.name);
      const gaId = parts.join(':');

      const params = {
        gaId,
        keyword: suggestion.name,
      };
      if (suggestion.version) {
        params.version = suggestion.version;
      }

      // Defer navigation to next tick - ensures it runs after any focus/blur/portal
      // handlers that could cancel navigation when triggered by mousedown/click
      queueMicrotask(() => {
        router.stateService.go('preview.browse.search.component', params);
      });
    } else {
      const displayName = getDisplayName(suggestion);
      const menuCtrl =
        window.Ext && Ext.getApplication && Ext.getApplication().getController
          ? Ext.getApplication().getController('Menu')
          : null;

      handleExtJsUnsavedChanges(menuCtrl, async () => {
        if (state.name !== ROUTE_NAMES.BROWSE.SEARCH.GENERIC) {
          await router.stateService.go(ROUTE_NAMES.BROWSE.SEARCH.GENERIC, {
            keyword: `=keyword=${encodeURIComponent(displayName)}`
          });
        } else {
          ExtJS.search(displayName);
        }
      });
    }
  }, [isPreviewUI, state.name, router]);

  // Both UIs should behave identically - use the same visibility check (permission-based)
  // In dev, anonymous has nexus:search:read so search shows
  // In prod, anonymous typically doesn't have this permission so search is hidden
  // NOTE: This check must come AFTER all hooks to avoid React hook violations
  if (!defaultUIVisible) {
    return null;
  }

  // Handle keyboard navigation
  const handleKeyDown = (e) => {
    if (!showSuggestions || suggestions.length === 0) {
      if (e.key === 'Enter') {
        e.preventDefault();
        navigateToSearch(searchValue.trim());
      }
      return;
    }

    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault();
        setSelectedIndex(prev => 
          prev < suggestions.length - 1 ? prev + 1 : prev
        );
        break;
      case 'ArrowUp':
        e.preventDefault();
        setSelectedIndex(prev => prev > 0 ? prev - 1 : -1);
        break;
      case 'Enter':
        e.preventDefault();
        if (selectedIndex >= 0 && selectedIndex < suggestions.length) {
          handleSelectSuggestion(suggestions[selectedIndex]);
        } else {
          navigateToSearch(searchValue.trim());
        }
        break;
      case 'Escape':
        setShowSuggestions(false);
        setSelectedIndex(-1);
        break;
    }
  };

  // Handle input focus
  const handleFocus = () => {
    if (suggestions.length > 0 && searchValue.trim().length >= MIN_QUERY_LENGTH) {
      setShowSuggestions(true);
    }
  };

  return (
    <div className="search-radix-container" ref={inputRef}>
      <TextField.Root
        placeholder="Search components or CVEs..."
        value={searchValue}
        onChange={(e) => setSearchValue(e.target.value)}
        onKeyDown={handleKeyDown}
        onFocus={handleFocus}
        size="2"
        className="search-radix-input"
        data-analytics-id="nxrm-global-header-search-input"
      >
        <TextField.Slot>
          <SearchIcon size={16} />
        </TextField.Slot>
      </TextField.Root>

      {/* Autocomplete dropdown */}
      {showSuggestions && suggestions.length > 0 && (
        <div className="search-radix-suggestions" ref={suggestionsRef}>
          {suggestions.map((suggestion, index) => {
            const displayName = getDisplayName(suggestion);
            const isSelected = index === selectedIndex;
            
            return (
              <div
                key={`${suggestion.repository}-${suggestion.group}-${suggestion.name}-${suggestion.version}`}
                className={`search-radix-suggestion ${isSelected ? 'search-radix-suggestion--selected' : ''}`}
                onMouseDown={(e) => {
                  // Use onMouseDown to trigger selection immediately before blur
                  e.preventDefault();
                  e.stopPropagation();
                  handleSelectSuggestion(suggestion);
                }}
                onMouseEnter={() => setSelectedIndex(index)}
                role="option"
                aria-selected={isSelected}
                data-testid={`search-suggestion-${index}`}
              >
                <div className="search-radix-suggestion__content">
                  <span className="search-radix-suggestion__name">{displayName}</span>
                  <span className="search-radix-suggestion__version">{suggestion.version}</span>
                </div>
                <FormatBadge format={suggestion.format} size={14} className="search-radix-suggestion__format" />
              </div>
            );
          })}
          
          {/* Footer hint */}
          <div className="search-radix-suggestions__footer">
            Press Enter to search all results
          </div>
        </div>
      )}
    </div>
  );
}
