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
 * Reusable protection level selector: None | Audit | Quarantine.
 * Same 3-button treatment as Malware Defense with outline on selected.
 */
import React from 'react';
import { Flex, Button } from '@radix-ui/themes';

import './ProtectionLevelSelector.scss';

export type ProtectionLevel = 'none' | 'audit' | 'quarantine';

const LABELS: Record<ProtectionLevel, string> = {
  none: 'None',
  audit: 'Audit',
  quarantine: 'Quarantine',
};

export interface ProtectionLevelSelectorProps {
  value: ProtectionLevel;
  onChange: (level: ProtectionLevel) => void;
  disabled?: boolean;
  size?: '1' | '2' | '3';
}

export function ProtectionLevelSelector({
  value,
  onChange,
  disabled = false,
  size = '2',
}: ProtectionLevelSelectorProps): JSX.Element {
  return (
    <Flex gap="2" align="center" className="protection-level-selector">
      {(['none', 'audit', 'quarantine'] as const).map((level) => (
        <Button
          key={level}
          type="button"
          size={size}
          variant={value === level ? 'solid' : 'soft'}
          color={level === 'none' ? 'gray' : level === 'audit' ? 'amber' : 'green'}
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
