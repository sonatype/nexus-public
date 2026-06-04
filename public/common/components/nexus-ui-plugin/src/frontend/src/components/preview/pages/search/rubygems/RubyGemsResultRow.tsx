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
import type { RubyGemsResult } from './rubygems.types';

import './RubyGemsResultRow.scss';

export interface RubyGemsResultRowProps {
  /** The RubyGems result to display */
  result: RubyGemsResult;
  /** Callback when row is selected */
  onSelect: (id: string) => void;
}

/**
 * Formats a date string for display.
 */
function formatDate(isoDate: string): string {
  const date = new Date(isoDate);
  return date.toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
}

/**
 * Truncates text to a maximum length.
 */
function truncate(text: string | undefined, maxLength: number): string {
  if (!text) return '-';
  if (text.length <= maxLength) return text;
  return `${text.slice(0, maxLength)}...`;
}

/**
 * Single row component for RubyGems search results.
 */
export function RubyGemsResultRow({ result, onSelect }: RubyGemsResultRowProps): JSX.Element {
  const handleClick = (): void => {
    onSelect(result.id);
  };

  const handleKeyDown = (event: React.KeyboardEvent): void => {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      onSelect(result.id);
    }
  };

  return (
    <tr
      className="rubygems-result-row"
      onClick={handleClick}
      onKeyDown={handleKeyDown}
      tabIndex={0}
      role="button"
      aria-label={`View details for ${result.displayName}`}
    >
      <td className="rubygems-result-row__name">
        <div className="rubygems-result-row__name-primary">{result.displayName}</div>
        {result.summary && (
          <div className="rubygems-result-row__summary">
            {truncate(result.summary, 60)}
          </div>
        )}
      </td>
      <td className="rubygems-result-row__version">
        {result.latestVersion}
      </td>
      <td className="rubygems-result-row__versions-count">
        {result.versionsCount}
      </td>
      <td className="rubygems-result-row__platform">
        <span className={`rubygems-result-row__platform-badge rubygems-result-row__platform-badge--${result.platform}`}>
          {result.platform}
        </span>
      </td>
      <td className="rubygems-result-row__authors">
        {truncate(result.authors, 25)}
      </td>
      <td className="rubygems-result-row__updated">
        {formatDate(result.lastUpdated)}
      </td>
      <td className="rubygems-result-row__chevron">
        <span className="rubygems-result-row__chevron-icon" aria-hidden="true">
          ›
        </span>
      </td>
    </tr>
  );
}

export default RubyGemsResultRow;


