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
import { FORMAT_CONFIG, type GenericResult } from './generic.types';

import './GenericResultRow.scss';

export interface GenericResultRowProps {
  /** The generic result to display */
  result: GenericResult;
  /** Callback when row is selected */
  onSelect: (id: string) => void;
}

/**
 * Single row component for generic search results.
 */
export function GenericResultRow({ result, onSelect }: GenericResultRowProps): JSX.Element {
  const handleClick = (): void => {
    onSelect(result.id);
  };

  const handleKeyDown = (event: React.KeyboardEvent): void => {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      onSelect(result.id);
    }
  };

  // Get format config for badge styling
  const formatConfig = FORMAT_CONFIG[result.format] || { label: result.format, color: 'gray' };

  return (
    <tr
      className="generic-result-row"
      onClick={handleClick}
      onKeyDown={handleKeyDown}
      tabIndex={0}
      role="button"
      aria-label={`View details for ${result.displayName}`}
    >
      <td className="generic-result-row__name">
        <div className="generic-result-row__name-primary">{result.displayName}</div>
        {result.group && (
          <div className="generic-result-row__group">{result.group}</div>
        )}
      </td>
      <td className="generic-result-row__format">
        <span 
          className={`generic-result-row__format-badge generic-result-row__format-badge--${formatConfig.color}`}
        >
          {formatConfig.label}
        </span>
      </td>
      <td className="generic-result-row__version">
        {result.version}
      </td>
      <td className="generic-result-row__repository">
        {result.repository}
      </td>
      <td className="generic-result-row__chevron">
        <span className="generic-result-row__chevron-icon" aria-hidden="true">
          ›
        </span>
      </td>
    </tr>
  );
}

export default GenericResultRow;


