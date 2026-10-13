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
import { Box, IconButton, Text, TextField } from '@radix-ui/themes';
import { X } from 'lucide-react';

export interface TextFilterProps {
  /** Filter ID */
  id: string;
  /** Display label */
  label: string;
  /** Current value */
  value: string;
  /** Callback when value changes */
  onChange: (id: string, value: string) => void;
  /** Callback to trigger search (called after debounce or immediately on Enter) */
  onSearch?: () => void;
  /** Placeholder text */
  placeholder?: string;
  /** Whether the filter is disabled */
  disabled?: boolean;
  /** Debounce delay in milliseconds for onChange-triggered searches (default: 300) */
  debounceMs?: number;
  /** Called when the input gains focus. Receives the filter id and its input element. */
  onInputFocus?: (id: string, el: HTMLInputElement) => void;
  /** Called when the input loses focus. Receives the filter id. */
  onInputBlur?: (id: string) => void;
}

/**
 * Text input filter component for the search sidebar.
 *
 * The input itself is uncontrolled by the debounce: every keystroke is reported through `onChange`
 * immediately, and only `onSearch` is debounced by the configurable `debounceMs` prop (default
 * 300ms — matches the search sidebar's other text filters). Enter searches at once and cancels
 * the pending debounce; Escape clears the filter.
 */
export function TextFilter({
  id,
  label,
  value,
  onChange,
  onSearch,
  placeholder,
  disabled = false,
  debounceMs = 300,
  onInputFocus,
  onInputBlur,
}: TextFilterProps): JSX.Element {
  const inputId = `filter-${id}`;
  // Track debounce timer
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Clean up debounce timer on unmount
  useEffect(() => {
    return () => {
      if (debounceRef.current) {
        clearTimeout(debounceRef.current);
      }
    };
  }, []);

  const handleChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const newValue = e.target.value;
      
      // Update UI state immediately
      onChange(id, newValue);
      
      // Cancel any pending debounced search
      if (debounceRef.current) {
        clearTimeout(debounceRef.current);
      }
      
      // Schedule debounced search
      if (onSearch) {
        debounceRef.current = setTimeout(() => {
          onSearch();
        }, debounceMs);
      }
    },
    [id, onChange, onSearch, debounceMs]
  );

  const handleClear = useCallback(() => {
    // Cancel any pending debounced search
    if (debounceRef.current) {
      clearTimeout(debounceRef.current);
    }
    
    onChange(id, '');
    
    // Trigger immediate search on clear
    if (onSearch) {
      // Use setTimeout(0) to ensure state update is applied first
      setTimeout(onSearch, 0);
    }
  }, [id, onChange, onSearch]);

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent<HTMLInputElement>) => {
      if (e.key === 'Escape' && value) {
        e.preventDefault();
        handleClear();
      } else if (e.key === 'Enter') {
        e.preventDefault();
        // Cancel pending debounce and execute immediately
        if (debounceRef.current) {
          clearTimeout(debounceRef.current);
        }
        onSearch?.();
      }
    },
    [value, handleClear, onSearch]
  );

  return (
    <Box>
      <Text as="label" size="1" weight="medium" htmlFor={inputId}>
        {label}
      </Text>
      <TextField.Root
        id={inputId}
        size="2"
        mt="1"
        spellCheck={false}
        placeholder={placeholder}
        value={value}
        onChange={handleChange}
        onKeyDown={handleKeyDown}
        onFocus={onInputFocus ? (e) => onInputFocus(id, e.currentTarget) : undefined}
        onBlur={onInputBlur ? () => onInputBlur(id) : undefined}
        disabled={disabled}
      >
        {value && !disabled && (
          <TextField.Slot side="right">
            <IconButton
              type="button"
              variant="ghost"
              color="gray"
              size="1"
              onClick={handleClear}
              aria-label={`Clear ${label} filter`}
            >
              <X size={14} />
            </IconButton>
          </TextField.Slot>
        )}
      </TextField.Root>
    </Box>
  );
}

export default TextFilter;


