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
import type { RawResult } from './raw.types';
import './RawResultRow.scss';

export interface RawResultRowProps {
  result: RawResult;
  onClick?: (result: RawResult) => void;
}

function formatFileSize(bytes?: number): string {
  if (!bytes) return '-';
  const units = ['B', 'KB', 'MB', 'GB'];
  let size = bytes;
  let unitIndex = 0;
  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024;
    unitIndex++;
  }
  return `${size.toFixed(1)} ${units[unitIndex]}`;
}

export function RawResultRow({ result, onClick }: RawResultRowProps): React.ReactElement {
  const handleClick = () => onClick?.(result);
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      onClick?.(result);
    }
  };

  return (
    <div
      className="raw-result-row"
      onClick={handleClick}
      onKeyDown={handleKeyDown}
      role="button"
      tabIndex={0}
      aria-label={`View details for ${result.name}`}
    >
      <div className="raw-result-row__icon">📄</div>
      <div className="raw-result-row__content">
        <div className="raw-result-row__name">{result.name}</div>
        <div className="raw-result-row__path">{result.path}</div>
      </div>
      <div className="raw-result-row__meta">
        <span className="raw-result-row__repository">{result.repository}</span>
        <span className="raw-result-row__size">{formatFileSize(result.size)}</span>
        {result.contentType && (
          <span className="raw-result-row__type">{result.contentType}</span>
        )}
      </div>
    </div>
  );
}

export default RawResultRow;


