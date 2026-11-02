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
import { Badge } from '@radix-ui/themes';
import {
  EDITION_ENTERPRISE,
  EDITION_PRO,
  type EditionId,
  getEditionDisplayName,
} from '../../../../interface/editionUtils';

export type EditionPillTier = 'pro' | 'enterprise';

const PILL_BY_TIER: Record<EditionPillTier, { editionId: EditionId; color: 'blue' | 'purple' }> = {
  pro: { editionId: EDITION_PRO, color: 'blue' },
  enterprise: { editionId: EDITION_ENTERPRISE, color: 'purple' },
};

export interface EditionPillProps {
  tier: EditionPillTier;
}

/** Names the edition a feature requires, e.g. next to a control the current license doesn't cover. */
export function EditionPill({ tier }: EditionPillProps): React.ReactElement {
  const { editionId, color } = PILL_BY_TIER[tier];
  return (
    <Badge variant="soft" color={color} size="1" data-testid={`edition-pill-${tier}`}>
      {getEditionDisplayName(editionId)}
    </Badge>
  );
}

export default EditionPill;
