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
import { Callout } from '@radix-ui/themes';
import { TriangleAlert } from 'lucide-react';

export interface UpgradeIqNoticeProps {
  /** Minimum supported IQ Server version, from the Firewall security pages state. */
  minIqVersion?: string | null;
}

/**
 * Shown when the connected IQ Server is older than the minimum supported version. Tabs are not rendered.
 */
export function UpgradeIqNotice({ minIqVersion }: UpgradeIqNoticeProps): JSX.Element {
  // TODO(Hermes): final copy
  const target = minIqVersion ? `${minIqVersion} or later` : 'a newer version';
  return (
    <Callout.Root role="status" color="amber" mb="4" data-testid="upgrade-iq-notice">
      <Callout.Icon>
        <TriangleAlert size={16} />
      </Callout.Icon>
      <Callout.Text>Upgrade IQ Server to {target} to use the Firewall security pages.</Callout.Text>
    </Callout.Root>
  );
}

export default UpgradeIqNotice;
