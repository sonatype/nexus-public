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
import { ExtJS, useIsVisible } from '@sonatype/nexus-ui-plugin';
import { faCircle, faBell } from "@fortawesome/free-solid-svg-icons";
import { NxButton, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { useRouter } from '@uirouter/react';

export default function SystemStatus() {
  const supportStatusStateIdentifier = 'admin.support.status';
  const healthChecksFailed = ExtJS.useState(() => ExtJS.state().getValue('health_checks_failed', false));
  const router = useRouter();

  const visibilityRequirements =
      router.stateRegistry.get(supportStatusStateIdentifier)
          ?.data
          ?.visibilityRequirements;

  const isVisibleValue = useIsVisible(visibilityRequirements);

  // Cloud / preview may not register admin.support.status; avoid console noise and render nothing.
  if (!visibilityRequirements || !isVisibleValue) {
    return null;
  }

  return (
      <NxButton
          onClick={onClick}
          title="System Status"
          aria-label="System Status"
          variant="icon-only"
          data-analytics-id="nxrm-global-header-system-status"
      >
        {healthChecksFailed
            ? (<span
                role="alert" aria-label="system status -- unhealthy" className="fa-layers fa-fw">
                <NxFontAwesomeIcon icon={faBell}/>
                <NxFontAwesomeIcon
                    className="nxrm-health-check-alert-circle"
                    icon={faCircle}
                    transform="shrink-8 up-5 right-5"
                />
              </span>)
            : <span role="status" aria-label="system status -- healthy">
                <NxFontAwesomeIcon icon={faBell}/>
                </span>
        }
      </NxButton>
  )

  function onClick() {
    router.stateService.go(supportStatusStateIdentifier);
  }
}
