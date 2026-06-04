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

import React, { useState, useRef, useEffect, useCallback } from 'react';
import { Box, Card, Flex, Spinner, Text, TextField } from '@radix-ui/themes';
import { Search, X } from 'lucide-react';
import { searchInRepository, type SearchResultItem as SearchResult } from './browse.api';
import { SearchResultItem } from './SearchResultItem';

import './InRepositorySearch.scss';

const DEBOUNCE_MS = 300;
const MIN_QUERY_LENGTH = 2;
const MAX_RESULTS = 20;

/**
 * Build a browse-tree-compatible path from a search result.
 *
 * The search API returns generic group/name/version fields whose mapping to
 * actual browse tree node IDs varies by format.  For example:
 *   - Maven:  group="org.springframework" → browse path org/springframework/name/version
 *   - npm:    group="types" (scope without @) → browse node is "@types"
 *   - NuGet:  group is typically null, browse is name/version
 *   - PyPI:   group is typically null, browse is name/version
 *   - Docker: browse tree is under v2/name/tags/version
 */
function buildBrowsePath(result: SearchResult): string {
  if (result.path) return result.path;

  const format = result.format?.toLowerCase();

  switch (format) {
    case 'npm': {
      // npm scoped: group="types" → tree node "@types", child "jquery"
      // npm unscoped: group=null, name="boostxyz"
      const scope = result.group ? `@${result.group}` : '';
      return [scope, result.name, result.version].filter(Boolean).join('/');
    }

    case 'maven2': {
      const group = result.group ? result.group.replace(/\./g, '/') : '';
      return [group, result.name, result.version].filter(Boolean).join('/');
    }

    case 'docker': {
      // Docker browse tree: v2/{imageName}/tags/{tag}
      return ['v2', result.name, 'tags', result.version].filter(Boolean).join('/');
    }

    default: {
      // NuGet, PyPI, raw, and others: name/version (group usually absent)
      const group = result.group || '';
      return [group, result.name, result.version].filter(Boolean).join('/');
    }
  }
}

export interface InRepositorySearchProps {
  repositoryName: string;
  onSelectResult: (path: string, result?: SearchResult) => void;
}

/**
 * In-repository search component with type-ahead and dropdown results.
 * Searches within a single repository using the /service/rest/v1/search API.
 */
export function InRepositorySearch({
  repositoryName,
  onSelectResult,
}: InRepositorySearchProps): JSX.Element {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<SearchResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [isOpen, setIsOpen] = useState(false);
  const [selectedIndex, setSelectedIndex] = useState(-1);
  const [error, setError] = useState<string | null>(null);

  const inputRef = useRef<HTMLInputElement>(null);
  const resultsRef = useRef<HTMLDivElement>(null);
  const debounceRef = useRef<NodeJS.Timeout>();

  // Cleanup debounce on unmount
  useEffect(() => {
    return () => {
      if (debounceRef.current) {
        clearTimeout(debounceRef.current);
      }
    };
  }, []);

  // Handle search with debounce
  useEffect(() => {
    if (debounceRef.current) {
      clearTimeout(debounceRef.current);
    }

    if (query.length < MIN_QUERY_LENGTH) {
      setResults([]);
      setIsOpen(false);
      setError(null);
      return;
    }

    debounceRef.current = setTimeout(async () => {
      setLoading(true);
      setError(null);
      try {
        const searchResults = await searchInRepository(repositoryName, query, MAX_RESULTS);
        setResults(searchResults);
        setIsOpen(true);
        setSelectedIndex(-1);
      } catch (err) {
        setError('Search failed. Please try again.');
        setResults([]);
        setIsOpen(true);
      } finally {
        setLoading(false);
      }
    }, DEBOUNCE_MS);
  }, [query, repositoryName]);

  const handleInputChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    setQuery(e.target.value);
  }, []);

  const handleClear = useCallback(() => {
    setQuery('');
    setResults([]);
    setIsOpen(false);
    setSelectedIndex(-1);
    setError(null);
    inputRef.current?.focus();
  }, []);

  const handleSelectResult = useCallback(
    (result: SearchResult) => {
      // Build path from result - use path if available, otherwise construct from group/name/version
      // For Maven: convert group dots to slashes (org.springframework -> org/springframework)
      const group = result.group ? result.group.replace(/\./g, '/') : '';
      const path = result.path || [group, result.name, result.version].filter(Boolean).join('/');
      onSelectResult(path, result);
      setIsOpen(false);
      setQuery('');
      setResults([]);
    },
    [onSelectResult]
  );

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      if (!isOpen || results.length === 0) {
        return;
      }

      switch (e.key) {
        case 'ArrowDown':
          e.preventDefault();
          setSelectedIndex((prev) => Math.min(prev + 1, results.length - 1));
          break;
        case 'ArrowUp':
          e.preventDefault();
          setSelectedIndex((prev) => Math.max(prev - 1, -1));
          break;
        case 'Enter':
          e.preventDefault();
          if (selectedIndex >= 0 && selectedIndex < results.length) {
            handleSelectResult(results[selectedIndex]);
          }
          break;
        case 'Escape':
          e.preventDefault();
          setIsOpen(false);
          setSelectedIndex(-1);
          break;
      }
    },
    [isOpen, results, selectedIndex, handleSelectResult]
  );

  const handleBlur = useCallback(() => {
    // Delay closing to allow click on result
    setTimeout(() => {
      setIsOpen(false);
      setSelectedIndex(-1);
    }, 200);
  }, []);

  const handleFocus = useCallback(() => {
    if (results.length > 0 && query.length >= MIN_QUERY_LENGTH) {
      setIsOpen(true);
    }
  }, [results.length, query.length]);

  return (
    <Box className="in-repo-search" position="relative">
      <TextField.Root
        ref={inputRef}
        placeholder={`Search in ${repositoryName}...`}
        value={query}
        onChange={handleInputChange}
        onKeyDown={handleKeyDown}
        onBlur={handleBlur}
        onFocus={handleFocus}
        size="2"
        aria-label={`Search in ${repositoryName}`}
        aria-autocomplete="list"
        aria-expanded={isOpen}
        aria-controls="in-repo-search-results"
        role="combobox"
      >
        <TextField.Slot>
          <Search size={16} aria-hidden />
        </TextField.Slot>
        <TextField.Slot side="right">
          {loading && <Spinner size="1" />}
          {!loading && query && (
            <Box
              as="button"
              onClick={handleClear}
              style={{
                background: 'none',
                border: 'none',
                cursor: 'pointer',
                padding: '2px',
                display: 'flex',
                alignItems: 'center',
              }}
              aria-label="Clear search"
            >
              <X size={14} />
            </Box>
          )}
        </TextField.Slot>
      </TextField.Root>

      {isOpen && results.length > 0 && (
        <Card
          ref={resultsRef}
          id="in-repo-search-results"
          className="in-repo-search__dropdown"
          role="listbox"
          aria-label="Search results"
        >
          {results.map((result, index) => (
            <SearchResultItem
              key={result.id}
              result={result}
              onClick={() => handleSelectResult(result)}
              onMouseDown={() => handleSelectResult(result)}
              isSelected={index === selectedIndex}
            />
          ))}
        </Card>
      )}

      {isOpen && results.length === 0 && query.length >= MIN_QUERY_LENGTH && !loading && (
        <Card className="in-repo-search__dropdown in-repo-search__dropdown--empty">
          <Box p="3">
            <Text size="2" color="gray">
              No results found for &quot;{query}&quot;
            </Text>
          </Box>
        </Card>
      )}

      {error && (
        <Card className="in-repo-search__dropdown in-repo-search__dropdown--error">
          <Box p="3">
            <Text size="2" color="red">
              {error}
            </Text>
          </Box>
        </Card>
      )}
    </Box>
  );
}

export default InRepositorySearch;
