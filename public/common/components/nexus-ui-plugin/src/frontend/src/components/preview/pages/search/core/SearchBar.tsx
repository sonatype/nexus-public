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

import React, { useCallback } from 'react';
import { Search } from 'lucide-react';

import './SearchBar.scss';

export type SearchFormat =
  | 'all'
  | 'maven2'
  | 'npm'
  | 'nuget'
  | 'pypi'
  | 'docker'
  | 'helm'
  | 'go'
  | 'rubygems'
  | 'yum'
  | 'apt'
  | 'raw'
  | 'conan'
  | 'conda'
  | 'cargo'
  | 'cocoapods'
  | 'composer'
  | 'terraform'
  | 'swift'
  | 'gitlfs'
  | 'p2'
  | 'r'
  | 'huggingface';

export interface SearchBarProps {
  /** Current search keyword */
  keyword: string;
  /** Called when keyword changes */
  onKeywordChange: (value: string) => void;
  /** Called when search is triggered */
  onSearch: () => void;
  /** Placeholder text */
  placeholder?: string;
  /** Whether search is in progress */
  loading?: boolean;
  /** Show format dropdown */
  showFormatDropdown?: boolean;
  /** Current format filter */
  format?: SearchFormat;
  /** Called when format changes */
  onFormatChange?: (format: SearchFormat) => void;
  /** Available formats for dropdown */
  availableFormats?: SearchFormat[];
}

const FORMAT_LABELS: Record<SearchFormat, string> = {
  all: 'All Formats',
  maven2: 'Maven',
  npm: 'npm',
  nuget: 'NuGet',
  pypi: 'PyPI',
  docker: 'Docker',
  helm: 'Helm',
  go: 'Go',
  rubygems: 'RubyGems',
  yum: 'Yum',
  apt: 'Apt',
  raw: 'Raw',
  conan: 'Conan',
  conda: 'Conda',
  cargo: 'Cargo',
  cocoapods: 'CocoaPods',
  composer: 'Composer',
  terraform: 'Terraform',
  swift: 'Swift',
  gitlfs: 'Git LFS',
  p2: 'P2',
  r: 'R',
  huggingface: 'Hugging Face',
};

const DEFAULT_FORMATS: SearchFormat[] = [
  'all',
  'maven2',
  'npm',
  'nuget',
  'pypi',
  'docker',
  'helm',
  'go',
  'rubygems',
  'yum',
  'apt',
  'raw',
];

/**
 * Unified search bar component with format dropdown.
 * Format dropdown appears BEFORE the search input.
 */
export function SearchBar({
  keyword,
  onKeywordChange,
  onSearch,
  placeholder = 'Search by component name or ID',
  loading = false,
  showFormatDropdown = true,
  format = 'all',
  onFormatChange,
  availableFormats = DEFAULT_FORMATS,
}: SearchBarProps): JSX.Element {
  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent<HTMLInputElement>): void => {
      if (e.key === 'Enter' && !loading) {
        onSearch();
      }
    },
    [onSearch, loading]
  );

  const handleFormatChange = useCallback(
    (e: React.ChangeEvent<HTMLSelectElement>): void => {
      if (onFormatChange) {
        onFormatChange(e.target.value as SearchFormat);
      }
    },
    [onFormatChange]
  );

  return (
    <div className="search-bar">
      {showFormatDropdown && (
        <select
          className="search-bar__format-select"
          value={format}
          onChange={handleFormatChange}
          disabled={loading}
          aria-label="Select format"
        >
          {availableFormats.map((fmt) => (
            <option key={fmt} value={fmt}>
              {FORMAT_LABELS[fmt]}
            </option>
          ))}
        </select>
      )}

      <div className="search-bar__input-wrapper">
        <Search className="search-bar__icon" size={18} />
        <input
          type="text"
          className="search-bar__input"
          value={keyword}
          onChange={(e) => onKeywordChange(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder={placeholder}
          disabled={loading}
          aria-label="Search components"
        />
      </div>

      <button
        type="button"
        className="search-bar__button"
        onClick={onSearch}
        disabled={loading}
      >
        {loading ? 'Searching...' : 'Search'}
      </button>
    </div>
  );
}

export default SearchBar;


