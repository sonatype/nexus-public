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
import {IconButton, Box} from '@radix-ui/themes';
import {Bell, AlertCircle} from 'lucide-react';
import {ExtJS, useIsVisible} from '@sonatype/nexus-ui-plugin';
import {useRouter} from '@uirouter/react';

export default function SystemStatusRadix() {
  const supportStatusStateIdentifier = 'admin.support.status';
  const healthChecksFailed = ExtJS.useState(() => ExtJS.state().getValue('health_checks_failed', false));
  const router = useRouter();

  const visibilityRequirements =
      router.stateRegistry.get(supportStatusStateIdentifier)
          ?.data
          ?.visibilityRequirements;

  const isVisibleValue = useIsVisible(visibilityRequirements);

  // Cloud / preview may not register admin.support.status; avoid console noise.
  if (!visibilityRequirements || !isVisibleValue) {
    return null;
  }

  function onClick() {
    router.stateService.go(supportStatusStateIdentifier);
  }

  return (
    <Box style={{position: 'relative'}}>
      <IconButton
        variant="ghost"
        onClick={onClick}
        title="System Status"
        aria-label={healthChecksFailed ? "System status -- unhealthy" : "System Status"}
      >
        <Bell size={18} />
      </IconButton>
      {healthChecksFailed && (
        <Box
          style={{
            position: 'absolute',
            top: '4px',
            right: '4px',
            pointerEvents: 'none'
          }}
        >
          <AlertCircle size={12} color="var(--red-9)" />
        </Box>
      )}
    </Box>
  );
}





