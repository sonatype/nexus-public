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
import type { PyPIResult } from './pypi.types';

import './PyPIResultRow.scss';

export interface PyPIResultRowProps {
  /** The PyPI result to display */
  result: PyPIResult;
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
 * Single row component for PyPI search results.
 */
export function PyPIResultRow({ result, onSelect }: PyPIResultRowProps): JSX.Element {
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
      className="pypi-result-row"
      onClick={handleClick}
      onKeyDown={handleKeyDown}
      tabIndex={0}
      role="button"
      aria-label={`View details for ${result.displayName}`}
    >
      <td className="pypi-result-row__name">
        <div className="pypi-result-row__name-primary">{result.displayName}</div>
        {result.summary && (
          <div className="pypi-result-row__summary">
            {truncate(result.summary, 60)}
          </div>
        )}
      </td>
      <td className="pypi-result-row__version">
        {result.latestVersion}
      </td>
      <td className="pypi-result-row__versions-count">
        {result.versionsCount}
      </td>
      <td className="pypi-result-row__author">
        {result.author || '-'}
      </td>
      <td className="pypi-result-row__updated">
        {formatDate(result.lastUpdated)}
      </td>
      <td className="pypi-result-row__chevron">
        <span className="pypi-result-row__chevron-icon" aria-hidden="true">
          ›
        </span>
      </td>
    </tr>
  );
}

export default PyPIResultRow;


