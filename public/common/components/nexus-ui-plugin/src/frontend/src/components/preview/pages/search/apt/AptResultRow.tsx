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
import type { AptResult } from './apt.types';

import './AptResultRow.scss';

export interface AptResultRowProps {
  /** The Apt result to display */
  result: AptResult;
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
 * Formats installed size for display.
 */
function _formatSize(bytes: number | undefined): string {
  if (!bytes) return '-';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

/**
 * Single row component for Apt search results.
 */
export function AptResultRow({ result, onSelect }: AptResultRowProps): JSX.Element {
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
      className="apt-result-row"
      onClick={handleClick}
      onKeyDown={handleKeyDown}
      tabIndex={0}
      role="button"
      aria-label={`View details for ${result.displayName}`}
    >
      <td className="apt-result-row__name">
        <div className="apt-result-row__name-primary">{result.displayName}</div>
        {result.description && (
          <div className="apt-result-row__description">
            {truncate(result.description, 60)}
          </div>
        )}
      </td>
      <td className="apt-result-row__version">
        {result.latestVersion}
      </td>
      <td className="apt-result-row__architecture">
        <span className="apt-result-row__arch-badge">
          {result.architecture}
        </span>
      </td>
      <td className="apt-result-row__distribution">
        {result.distribution || '-'}
      </td>
      <td className="apt-result-row__section">
        {result.section || '-'}
      </td>
      <td className="apt-result-row__updated">
        {formatDate(result.lastUpdated)}
      </td>
      <td className="apt-result-row__chevron">
        <span className="apt-result-row__chevron-icon" aria-hidden="true">
          ›
        </span>
      </td>
    </tr>
  );
}

export default AptResultRow;


