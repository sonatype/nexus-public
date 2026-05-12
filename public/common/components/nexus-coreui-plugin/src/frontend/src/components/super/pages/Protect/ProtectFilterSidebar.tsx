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

import React, { useMemo } from 'react';
import { FilterSidebar, type FilterSection } from '../../../shared/FilterSidebar';
import type { ProtectFilterCounts } from './useProtectData';

export interface ProtectFilterState {
  format: string[];
  protection: string[];
  healthCheck: string[];
  cleanup: string[];
}

export interface ProtectFilterSidebarProps {
  counts: ProtectFilterCounts;
  value: ProtectFilterState;
  onChange: (next: ProtectFilterState) => void;
  disabled?: boolean;
  hasFirewall?: boolean;
}

const PROTECTION_LABELS: Record<string, string> = {
  quarantine: 'Quarantine',
  audit: 'Audit',
  none: 'None',
  unsupported: 'Not supported',
};

export default function ProtectFilterSidebar({
  counts,
  value,
  onChange,
  disabled,
  hasFirewall = false,
}: ProtectFilterSidebarProps) {
  const sections: FilterSection[] = useMemo(() => {
    const formatOptions = [...counts.formats.entries()]
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([fmt, c]) => ({ value: fmt, label: fmt, count: c }));

    const hcOptions = [
      { value: 'enabled', label: 'Enabled', count: counts.healthCheck.enabled },
      { value: 'disabled', label: 'Disabled', count: counts.healthCheck.disabled },
      { value: 'unsupported', label: 'Not supported (format)', count: counts.healthCheck.unsupported },
    ].filter((o) => o.count > 0);

    const result: FilterSection[] = [
      {
        id: 'format',
        label: 'Format',
        type: 'checkbox',
        options: formatOptions,
        value: value.format,
        defaultExpanded: true,
      },
      {
        id: 'healthCheck',
        label: 'Health Check',
        type: 'checkbox',
        options: hcOptions,
        value: value.healthCheck,
        defaultExpanded: false,
      },
    ];

    if (hasFirewall) {
      const protectionOptions = [...counts.protection.entries()]
        .filter(([k]) => k !== 'unsupported' || (counts.protection.get('unsupported') ?? 0) > 0)
        .map(([k, c]) => ({
          value: k,
          label: PROTECTION_LABELS[k] ?? k,
          count: c,
        }));

      const cleanupOptions = [
        { value: 'delete', label: 'Delete', count: counts.cleanup.delete },
        { value: 'audit', label: 'Audit', count: counts.cleanup.audit },
        { value: 'off', label: 'Off', count: counts.cleanup.off },
      ].filter((o) => o.count > 0);

      result.splice(1, 0, {
        id: 'protection',
        label: 'Protection',
        type: 'checkbox',
        options: protectionOptions,
        value: value.protection,
        defaultExpanded: true,
      });

      result.push({
        id: 'cleanup',
        label: 'Auto Remediation',
        type: 'checkbox',
        options: cleanupOptions,
        value: value.cleanup,
        defaultExpanded: false,
      });
    }

    return result;
  }, [counts, value.cleanup, value.format, value.healthCheck, value.protection, hasFirewall]);

  const onFilterChange = (sectionId: string, v: string | string[]) => {
    const arr = Array.isArray(v) ? v : [v];
    onChange({
      ...value,
      [sectionId]: arr,
    } as ProtectFilterState);
  };

  const onClear = () =>
    onChange({
      format: [],
      protection: [],
      healthCheck: [],
      cleanup: [],
    });

  return (
    <FilterSidebar
      sections={sections}
      onFilterChange={onFilterChange}
      onClear={onClear}
      disabled={disabled}
      title="Filters"
      className="nxrm-protect-hub__sidebar"
    />
  );
}
