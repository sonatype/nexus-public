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
import {useMachine} from "@xstate/react";
import MaliciousRiskOnDiskMachine from "./MaliciousRiskOnDiskMachine";
import {NxErrorAlert, NxLoadWrapper} from "@sonatype/react-shared-components";
import {ExtJS} from '@sonatype/nexus-ui-plugin';

function MaliciousRiskOnDiskContent() {
  const LOAD_ERROR = 'Error loading malicious risk on disk data ';

  const [state, send] = useMachine(MaliciousRiskOnDiskMachine, {devtools: true});
  const isLoading = state.matches('loading');
  const loadError = state.matches('loadError') ? LOAD_ERROR : null;
  const totalCount = state.context.maliciousRiskOnDisk.totalCount;

  function retry() {
    send('RETRY');
  }

  return (
      <NxLoadWrapper isLoading={isLoading} error={loadError} retryHandler={retry}>
        <NxErrorAlert>
          <div>
            Total count: {totalCount}
          </div>
        </NxErrorAlert>
      </NxLoadWrapper>
  );
}

export default function MaliciousRiskOnDisk() {
  const isRiskOnDiskEnabled = ExtJS.state().getValue('nexus.malicious.risk.on.disk.enabled');
  const user = ExtJS.useUser();
  const userIsLogged = user ?? false;
  const showMaliciousRiskOnDisk = userIsLogged && isRiskOnDiskEnabled;

  if (!showMaliciousRiskOnDisk) {
    return null;
  }

  return <MaliciousRiskOnDiskContent/>;
}
