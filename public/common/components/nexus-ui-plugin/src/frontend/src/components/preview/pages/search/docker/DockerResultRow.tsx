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
import type { DockerResult } from './docker.types';

import './DockerResultRow.scss';

export interface DockerResultRowProps {
  /** The Docker result to display */
  result: DockerResult;
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
 * Single row component for Docker search results.
 * Displays: displayName, latestTag, tagsCount, size, repository
 */
export function DockerResultRow({ result, onSelect }: DockerResultRowProps): JSX.Element {
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
      className="docker-result-row"
      onClick={handleClick}
      onKeyDown={handleKeyDown}
      tabIndex={0}
      role="button"
      aria-label={`View details for ${result.displayName}`}
    >
      <td className="docker-result-row__name">
        <div className="docker-result-row__name-primary">{result.displayName}</div>
        <div className="docker-result-row__name-secondary">{result.imageName}</div>
      </td>
      <td className="docker-result-row__tag">
        <code className="docker-result-row__tag-code">{result.latestTag}</code>
      </td>
      <td className="docker-result-row__tags-count">
        {result.tagsCount}
      </td>
      <td className="docker-result-row__size">
        {result.size ?? '-'}
      </td>
      <td className="docker-result-row__repository">
        {result.repository ?? '-'}
      </td>
      <td className="docker-result-row__updated">
        {formatDate(result.lastUpdated)}
      </td>
      <td className="docker-result-row__chevron">
        <span className="docker-result-row__chevron-icon" aria-hidden="true">
          ›
        </span>
      </td>
    </tr>
  );
}

export default DockerResultRow;


