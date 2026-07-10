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
 * The PCCS button is only rendered when the caller passes `pccsSupported`, which mirrors the
 * legacy ExtJS {@code FirewallFacet.js} / React-in-classic {@code FirewallConfiguration.jsx}
 * behaviour where PCCS is gated by the format-capabilities API (currently npm/pypi only).
 */
import React from 'react';
import { Flex, Button } from '@radix-ui/themes';

import './ProtectionLevelSelector.scss';

export type ProtectionLevel = 'none' | 'audit' | 'quarantine' | 'pccs';

const LABELS: Record<ProtectionLevel, string> = {
  none: 'None',
  audit: 'Audit',
  quarantine: 'Quarantine',
  pccs: 'PCCS',
};

const COLORS: Record<ProtectionLevel, 'gray' | 'amber' | 'green' | 'orange'> = {
  none: 'gray',
  audit: 'amber',
  quarantine: 'green',
  pccs: 'orange',
};

export interface ProtectionLevelSelectorProps {
  value: ProtectionLevel;
  onChange: (level: ProtectionLevel) => void;
  disabled?: boolean;
  size?: '1' | '2' | '3';
  /**
   * When true, includes a fourth `PCCS` button. Caller is responsible for resolving whether
   * the repository's format supports PCCS — typically via {@code fetchPccsSupportedFormats()}.
   * Defaults to false, preserving the original 3-level behaviour.
   */
  pccsSupported?: boolean;
}

export function ProtectionLevelSelector({
  value,
  onChange,
  disabled = false,
  size = '2',
  pccsSupported = false,
}: ProtectionLevelSelectorProps): JSX.Element {
  const levels: ProtectionLevel[] = pccsSupported
    ? ['none', 'audit', 'quarantine', 'pccs']
    : ['none', 'audit', 'quarantine'];

  return (
    <Flex gap="2" align="center" wrap="wrap" className="protection-level-selector">
      {levels.map((level) => (
        <Button
          key={level}
          type="button"
          size={size}
          variant={value === level ? 'solid' : 'soft'}
          color={COLORS[level]}
          onClick={() => onChange(level)}
          disabled={disabled}
          className={`protection-level-selector__btn ${
            value === level ? 'protection-level-selector__btn--selected' : ''
          }`}
          aria-pressed={value === level}
        >
          {LABELS[level]}
        </Button>
      ))}
    </Flex>
  );
}
