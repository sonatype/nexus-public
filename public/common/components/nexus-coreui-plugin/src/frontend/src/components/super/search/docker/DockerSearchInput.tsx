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

import React, { useState, useRef, useEffect } from 'react';
import type { DockerSuggestion } from './docker.types';
import { mockSuggestApi } from './mockData';

import './DockerSearchInput.scss';

export interface DockerSearchInputProps {
  /** Current search value */
  value: string;
  /** Callback when value changes */
  onChange: (value: string) => void;
  /** Callback when search is submitted */
  onSearch: (value: string) => void;
  /** Callback when a suggestion is selected */
  onSuggestionSelect?: (id: string) => void;
  /** Placeholder text */
  placeholder?: string;
  /** Whether input is disabled */
  disabled?: boolean;
}

/**
 * Search input with typeahead suggestions.
 */
export function DockerSearchInput({
  value,
  onChange,
  onSearch,
  onSuggestionSelect,
  placeholder = 'Search Docker images...',
  disabled = false,
}: DockerSearchInputProps): JSX.Element {
  const [suggestions, setSuggestions] = useState<DockerSuggestion[]>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [loading, setLoading] = useState(false);
  const [selectedIndex, setSelectedIndex] = useState(-1);
  const inputRef = useRef<HTMLInputElement>(null);
  const suggestionsRef = useRef<HTMLUListElement>(null);
  const debounceRef = useRef<NodeJS.Timeout>();

  // Fetch suggestions when value changes
  useEffect(() => {
    if (debounceRef.current) {
      clearTimeout(debounceRef.current);
    }

    if (value.length < 2) {
      setSuggestions([]);
      setShowSuggestions(false);
      return;
    }

    debounceRef.current = setTimeout(async () => {
      setLoading(true);
      try {
        // TODO: Replace with real API call
        const response = await mockSuggestApi({ query: value });
        setSuggestions(response.suggestions);
        setShowSuggestions(response.suggestions.length > 0);
        setSelectedIndex(-1);
      } catch {
        setSuggestions([]);
      } finally {
        setLoading(false);
      }
    }, 200);

    return () => {
      if (debounceRef.current) {
        clearTimeout(debounceRef.current);
      }
    };
  }, [value]);

  const handleInputChange = (event: React.ChangeEvent<HTMLInputElement>): void => {
    onChange(event.target.value);
  };

  const handleKeyDown = (event: React.KeyboardEvent): void => {
    if (!showSuggestions || suggestions.length === 0) {
      if (event.key === 'Enter') {
        onSearch(value);
      }
      return;
    }

    switch (event.key) {
      case 'ArrowDown':
        event.preventDefault();
        setSelectedIndex((prev) => Math.min(prev + 1, suggestions.length - 1));
        break;
      case 'ArrowUp':
        event.preventDefault();
        setSelectedIndex((prev) => Math.max(prev - 1, -1));
        break;
      case 'Enter':
        event.preventDefault();
        if (selectedIndex >= 0) {
          const selected = suggestions[selectedIndex];
          if (onSuggestionSelect) {
            onSuggestionSelect(selected.id);
          } else {
            onChange(selected.displayText);
            onSearch(selected.displayText);
          }
        } else {
          onSearch(value);
        }
        setShowSuggestions(false);
        break;
      case 'Escape':
        setShowSuggestions(false);
        setSelectedIndex(-1);
        break;
    }
  };

  const handleSuggestionClick = (suggestion: DockerSuggestion): void => {
    if (onSuggestionSelect) {
      onSuggestionSelect(suggestion.id);
    } else {
      onChange(suggestion.displayText);
      onSearch(suggestion.displayText);
    }
    setShowSuggestions(false);
  };

  const handleBlur = (): void => {
    // Delay hiding to allow click on suggestion
    setTimeout(() => {
      setShowSuggestions(false);
    }, 200);
  };

  const handleFocus = (): void => {
    if (suggestions.length > 0) {
      setShowSuggestions(true);
    }
  };

  return (
    <div className="docker-search-input">
      <div className="docker-search-input__wrapper">
        <span className="docker-search-input__icon" aria-hidden="true">
          🐳
        </span>
        <input
          ref={inputRef}
          type="text"
          className="docker-search-input__field"
          value={value}
          onChange={handleInputChange}
          onKeyDown={handleKeyDown}
          onBlur={handleBlur}
          onFocus={handleFocus}
          placeholder={placeholder}
          disabled={disabled}
          aria-label="Search Docker images"
          aria-autocomplete="list"
          aria-expanded={showSuggestions}
          aria-controls="docker-search-suggestions"
          role="combobox"
        />
        {loading && (
          <span className="docker-search-input__loading" aria-hidden="true">
            ...
          </span>
        )}
      </div>

      {showSuggestions && suggestions.length > 0 && (
        <ul
          ref={suggestionsRef}
          id="docker-search-suggestions"
          className="docker-search-input__suggestions"
          role="listbox"
        >
          {suggestions.map((suggestion, index) => (
            <li
              key={suggestion.id}
              className={`docker-search-input__suggestion${
                index === selectedIndex ? ' docker-search-input__suggestion--selected' : ''
              }`}
              onClick={() => handleSuggestionClick(suggestion)}
              role="option"
              aria-selected={index === selectedIndex}
            >
              {suggestion.displayText}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

export default DockerSearchInput;


