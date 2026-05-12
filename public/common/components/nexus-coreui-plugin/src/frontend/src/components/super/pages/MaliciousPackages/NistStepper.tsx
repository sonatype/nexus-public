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
import { Flex, Box, Text } from '@radix-ui/themes';
import { Check, Circle, ChevronRight } from 'lucide-react';

import type { NistPhase } from './types';

const PHASES: NistPhase[] = ['ALERT', 'TRIAGE', 'CONTAINMENT', 'ERADICATION', 'RECOVERY', 'POST_INCIDENT'];

const PHASE_LABELS: Record<NistPhase, string> = {
  ALERT: 'Alert',
  TRIAGE: 'Triage',
  CONTAINMENT: 'Containment',
  ERADICATION: 'Eradication',
  RECOVERY: 'Recovery',
  POST_INCIDENT: 'Post Incident',
};

interface NistStepperProps {
  currentPhase: NistPhase | null;
}

export function NistStepper({ currentPhase }: NistStepperProps): React.ReactElement | null {
  if (currentPhase === null) return null;

  const currentIndex = PHASES.indexOf(currentPhase);

  return (
    <Flex role="navigation" aria-label="Incident response phases" align="center" gap="1" wrap="wrap">
      {PHASES.map((phase, index) => {
        const isCompleted = index < currentIndex;
        const isCurrent = index === currentIndex;

        return (
          <React.Fragment key={phase}>
            {index > 0 && (
              <ChevronRight size={16} color="var(--gray-8)" aria-hidden="true" />
            )}
            <Flex align="center" gap="1">
              {isCompleted ? (
                <Check size={16} color="var(--green-9)" aria-hidden="true" />
              ) : (
                <Circle
                  size={16}
                  color={isCurrent ? 'var(--blue-9)' : 'var(--gray-6)'}
                  fill={isCurrent ? 'var(--blue-9)' : 'none'}
                  aria-hidden="true"
                />
              )}
              <Text
                size="2"
                weight={isCurrent ? 'bold' : 'regular'}
                style={{ color: isCompleted ? 'var(--green-9)' : isCurrent ? 'var(--blue-9)' : 'var(--gray-8)' }}
              >
                {PHASE_LABELS[phase]}
              </Text>
            </Flex>
          </React.Fragment>
        );
      })}
    </Flex>
  );
}
