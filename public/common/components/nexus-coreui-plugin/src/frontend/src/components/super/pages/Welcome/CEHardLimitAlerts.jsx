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
import {Callout, Flex, Text, Link} from '@radix-ui/themes';
import {AlertTriangle} from 'lucide-react';
import {ExtJS} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';

const {WELCOME: {CE_HARD_LIMIT_ALERT}} = UIStrings;

export default function CEHardLimitAlerts() {
  const isCommunityEdition = ExtJS.state().getValue('status')?.edition === 'COMMUNITY';
  const hardLimitReached = ExtJS.state().getValue('hardLimitReached');
  
  if (!isCommunityEdition || !hardLimitReached) {
    return null;
  }

  return (
    <Callout.Root color="red" size="1">
      <Flex gap="2" align="center">
        <AlertTriangle size={16} style={{flexShrink: 0}} />
        <Text weight="medium" size="2">{CE_HARD_LIMIT_ALERT.TITLE}</Text>
        <Text size="2">
          {CE_HARD_LIMIT_ALERT.MESSAGE}{' '}
          <Link href={CE_HARD_LIMIT_ALERT.LINK_URL} target="_blank" size="2">
            {CE_HARD_LIMIT_ALERT.LINK_TEXT}
          </Link>
        </Text>
      </Flex>
    </Callout.Root>
  );
}
