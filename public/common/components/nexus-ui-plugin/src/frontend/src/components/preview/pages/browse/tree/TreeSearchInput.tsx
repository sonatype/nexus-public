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

import React, { useCallback, useRef, useEffect } from 'react';
import { Box, Flex, IconButton, Text, TextField } from '@radix-ui/themes';
import { Search, X } from 'lucide-react';

import type { SearchValidation } from './useTreeSearch';

/**
 * UI Strings for the tree search input.
 */
const STRINGS = {
  placeholder: 'Filter tree...',
  clearLabel: 'Clear filter',
  matchesLabel: (count: number) => `${count} match${count === 1 ? '' : 'es'}`,
  noMatches: 'No matches',
};

/**
 * Props for TreeSearchInput component.
 */
export interface TreeSearchInputProps {
  /** Current search term */
  value: string;
  /** Callback when search term changes */
  onChange: (value: string) => void;
  /** Callback to clear the search */
  onClear: () => void;
  /** Validation state */
  validation: SearchValidation;
  /** Number of matching nodes */
  matchCount: number;
  /** Whether search is active */
  isSearchActive: boolean;
  /** Whether the input is disabled */
  disabled?: boolean;
  /** Auto-focus the input on mount */
  autoFocus?: boolean;
}

/**
 * Search input component for filtering the browse tree.
 *
 * Features:
 * - Search icon prefix
 * - Clear button when text is entered
 * - Match count display
 * - Validation error display
 * - Keyboard shortcut support (Escape to clear)
 */
export function TreeSearchInput({
  value,
  onChange,
  onClear,
  validation,
  matchCount,
  isSearchActive,
  disabled = false,
  autoFocus = false,
}: TreeSearchInputProps): JSX.Element {
  const inputRef = useRef<HTMLInputElement>(null);

  // Auto-focus on mount if requested
  useEffect(() => {
    if (autoFocus && inputRef.current) {
      inputRef.current.focus();
    }
  }, [autoFocus]);

  /**
   * Handle input change.
   */
  const handleChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      onChange(e.target.value);
    },
    [onChange]
  );

  /**
   * Handle keyboard events.
   */
  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent<HTMLInputElement>) => {
      if (e.key === 'Escape' && value) {
        e.preventDefault();
        onClear();
      }
    },
    [value, onClear]
  );

  /**
   * Handle clear button click.
   */
  const handleClear = useCallback(() => {
    onClear();
    inputRef.current?.focus();
  }, [onClear]);

  const showMatchCount = isSearchActive && validation.isValid;
  const hasError = !validation.isValid;

  return (
    <Box className="tree-search-input" mb="2" data-testid="tree-search-input">
      <TextField.Root
        ref={inputRef}
        placeholder={STRINGS.placeholder}
        value={value}
        onChange={handleChange}
        onKeyDown={handleKeyDown}
        disabled={disabled}
        size="2"
        color={hasError ? 'red' : undefined}
        data-testid="tree-search-field"
        aria-label="Filter repository tree"
        aria-invalid={hasError}
        aria-describedby={hasError ? 'tree-search-error' : undefined}
      >
        <TextField.Slot>
          <Search size={14} aria-hidden />
        </TextField.Slot>
        {value && (
          <TextField.Slot side="right">
            <IconButton
              variant="ghost"
              color="gray"
              size="1"
              onClick={handleClear}
              aria-label={STRINGS.clearLabel}
              data-testid="tree-search-clear"
            >
              <X size={14} />
            </IconButton>
          </TextField.Slot>
        )}
      </TextField.Root>

      {/* Match count or error message */}
      <Flex justify="between" align="center" mt="1" minHeight="20px">
        {hasError && (
          <Text
            id="tree-search-error"
            size="1"
            color="red"
            data-testid="tree-search-error"
          >
            {validation.error}
          </Text>
        )}
        {showMatchCount && (
          <Text
            size="1"
            color={matchCount === 0 ? 'orange' : 'gray'}
            data-testid="tree-search-match-count"
          >
            {matchCount === 0 ? STRINGS.noMatches : STRINGS.matchesLabel(matchCount)}
          </Text>
        )}
      </Flex>
    </Box>
  );
}

export default TreeSearchInput;
