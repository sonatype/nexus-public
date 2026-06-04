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

import React, { useState, ReactNode } from 'react';
import { ChevronDown, ChevronRight } from 'lucide-react';

export interface FilterSectionProps {
  /** Section title */
  title: string;
  /** Whether the section is initially expanded */
  defaultExpanded?: boolean;
  /** Number badge to show (e.g., active filter count) */
  badge?: number;
  /** Child filter components */
  children: ReactNode;
}

/**
 * Collapsible filter section wrapper.
 * Used in the search sidebar to group filters.
 */
export function FilterSection({
  title,
  defaultExpanded = true,
  badge,
  children,
}: FilterSectionProps): JSX.Element {
  const [expanded, setExpanded] = useState(defaultExpanded);

  return (
    <div className="filter-section">
      <button
        type="button"
        className="filter-section__header"
        onClick={() => setExpanded(!expanded)}
        aria-expanded={expanded}
      >
        <span className="filter-section__icon">
          {expanded ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
        </span>
        <span className="filter-section__title">{title}</span>
        {badge !== undefined && badge > 0 && (
          <span className="filter-section__badge">{badge}</span>
        )}
      </button>

      {expanded && (
        <div className="filter-section__content">
          {children}
        </div>
      )}
    </div>
  );
}

export default FilterSection;


