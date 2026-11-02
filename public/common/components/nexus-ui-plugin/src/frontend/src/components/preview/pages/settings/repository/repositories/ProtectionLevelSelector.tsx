/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are
 * trademarks of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a
 * trademark of the Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

/**
 * Reusable protection level selector: None | Audit | Quarantine | (PCCS).
 *
 * Rendered as a dropdown (Radix Select) rather than a button group (FIRE-958) so the control
 * reads as a form field rather than an action — selecting an item only reports the choice to
 * the caller via {@link ProtectionLevelSelectorProps.onChange}; it does not persist anything
 * itself. Committing/discarding the choice is the caller's responsibility.
 *
 * The PCCS option is only rendered when the caller passes `pccsSupported`, which mirrors the
 * legacy ExtJS {@code FirewallFacet.js} / React-in-classic {@code FirewallConfiguration.jsx}
 * behaviour where PCCS is gated by the format-capabilities API (currently npm/pypi only).
 */
import React from 'react';
import { Select } from '@radix-ui/themes';

export type ProtectionLevel = 'none' | 'audit' | 'quarantine' | 'pccs';

const LABELS: Record<ProtectionLevel, string> = {
  none: 'None – do not use Firewall',
  audit: 'Audit Only - allow components that violate policy',
  quarantine: 'Quarantine - block components that violate policy',
  pccs: 'PCCS - Quarantine, plus metadata filtering to help clients select a policy compliance version',
};

export interface ProtectionLevelSelectorProps {
  value: ProtectionLevel;
  onChange: (level: ProtectionLevel) => void;
  disabled?: boolean;
  size?: '1' | '2' | '3';
  /**
   * When true, includes a fourth `PCCS` option. Caller is responsible for resolving whether
   * the repository's format supports PCCS — typically via {@code fetchPccsSupportedFormats()}.
   * Defaults to false, preserving the original 3-level behaviour.
   */
  pccsSupported?: boolean;
  /**
   * ID of the caller's visible label element. Select.Trigger renders as role="combobox", which
   * ARIA does not let take its accessible name from its content, so without this a screen reader
   * only announces the selected value and never what the control is (WCAG 4.1.2). Mirrors the
   * pattern in settings/system/iq-server/SettingsTab.tsx.
   */
  'aria-labelledby'?: string;
}

export function ProtectionLevelSelector({
  value,
  onChange,
  disabled = false,
  size = '2',
  pccsSupported = false,
  'aria-labelledby': ariaLabelledBy,
}: ProtectionLevelSelectorProps): JSX.Element {
  const levels: ProtectionLevel[] = pccsSupported
    ? ['none', 'audit', 'quarantine', 'pccs']
    : ['none', 'audit', 'quarantine'];

  return (
    <Select.Root
      value={value}
      onValueChange={(level) => onChange(level as ProtectionLevel)}
      disabled={disabled}
      size={size}
    >
      <Select.Trigger
        data-testid="protection-level-select"
        aria-labelledby={ariaLabelledBy}
        style={{ width: '100%' }}
      />
      <Select.Content position="popper" side="bottom" align="start" sideOffset={4}>
        {levels.map((level) => (
          <Select.Item key={level} value={level} data-testid={`protection-level-option-${level}`}>
            {LABELS[level]}
          </Select.Item>
        ))}
      </Select.Content>
    </Select.Root>
  );
}
