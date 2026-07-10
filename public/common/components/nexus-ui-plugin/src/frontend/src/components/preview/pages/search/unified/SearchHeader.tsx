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

import React, { useState, useCallback, useMemo } from 'react';
import { Flex, Box, TextField, Select } from '@radix-ui/themes';
import { Search } from 'lucide-react';

import { SearchFormat, SearchHeaderProps } from './unified.types';
import { getFormatOptions, getPlaceholderForFormat } from './searchFilters';
import './SearchHeader.scss';

/**
 * SearchHeader - Format dropdown and search input bar
 *
 * Layout: [Format Dropdown] [Search Input with icon]
 *
 * Features:
 * - Format dropdown on LEFT of search input
 * - Dynamic placeholder text based on selected format
 * - Live filtering: `onSearch` is invoked on every keystroke with the current
 *   input value. Consumers should treat it as a controlled filter callback,
 *   not a submit action.
 *
 * Controlled-input contract:
 * - `query` is read on mount and on subsequent external changes (e.g. clearing
 *   the filter from outside) to seed/reset the input. It is NOT meant to echo
 *   the live value back from the parent on every keystroke.
 * - Consumers should keep their own filter state and pass `setFilterState` (or
 *   equivalent) directly as `onSearch`. Do not derive `query` from state that
 *   `onSearch` itself updates on every keystroke; that pattern works today
 *   because React bails on equal state, but it muddies the contract and risks
 *   render loops if a future consumer transforms the value before passing it
 *   back through `query`.
 */
export default function SearchHeader({
  format,
  onFormatChange,
  query,
  onSearch,
  placeholder: customPlaceholder,
}: SearchHeaderProps) {
  // Local state for the search input
  const [inputValue, setInputValue] = useState(query);

  // Get format options from centralized config
  const formatOptions = useMemo(() => getFormatOptions(), []);

  // Get placeholder text (use custom if provided, otherwise derive from format)
  const placeholderText = customPlaceholder || getPlaceholderForFormat(format);

  // Handle format change
  const handleFormatChange = useCallback((value: string) => {
    onFormatChange(value as SearchFormat);
  }, [onFormatChange]);

  // Live filter: update local state and notify the parent on every keystroke.
  const handleInputChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const next = e.target.value;
    setInputValue(next);
    onSearch(next);
  }, [onSearch]);

  // Sync inputValue with query prop when it changes externally
  React.useEffect(() => {
    setInputValue(query);
  }, [query]);

  return (
    <div className="search-header">
      <Flex align="center" gap="2" className="search-header__container">
        {/* Format Dropdown */}
        <Box className="search-header__format-dropdown">
          <Select.Root value={format} onValueChange={handleFormatChange}>
            <Select.Trigger
              className="search-header__format-trigger"
              placeholder="Select format"
            />
            <Select.Content className="search-header__format-content">
              {formatOptions.map((option) => (
                <Select.Item
                  key={option.id}
                  value={option.id}
                  className="search-header__format-item"
                >
                  {option.label}
                </Select.Item>
              ))}
            </Select.Content>
          </Select.Root>
        </Box>

        {/* Search Input */}
        <Box className="search-header__search-input" flexGrow="1">
          <TextField.Root
            value={inputValue}
            onChange={handleInputChange}
            placeholder={placeholderText}
            className="search-header__input"
            size="3"
          >
            <TextField.Slot side="left">
              <Search size={18} className="search-header__search-icon" />
            </TextField.Slot>
          </TextField.Root>
        </Box>
      </Flex>
    </div>
  );
}

// Re-export from searchFilters for convenience
export { getFormatOptions, getPlaceholderForFormat } from './searchFilters';

