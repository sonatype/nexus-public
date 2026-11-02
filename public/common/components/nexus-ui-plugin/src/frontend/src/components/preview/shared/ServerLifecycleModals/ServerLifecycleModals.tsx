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
import { ServerRestartModal, useServerRestart } from '../ServerRestart';
import {
  ServerDisconnectedModal,
  useServerDisconnected,
} from '../ServerDisconnected';

/**
 * ServerLifecycleModals - coordinates the two Preview UI server-lifecycle modals so
 * they never stack. A crash-then-redeploy can fire a disconnect and a restart in the
 * same window. A restart is terminal (a reload is imminent and mandatory), so it wins:
 * the disconnect modal is hidden while the restart modal is showing.
 *
 * Hiding the disconnect modal through the controlled `open` prop does not trigger
 * Radix's onOpenChange (that fires only on user-driven close), so no spurious DISMISS
 * is sent and the disconnect poll is left untouched for the imminent reload.
 */
export function ServerLifecycleModals(): JSX.Element {
  const { isRestarted, isReloading, confirmReload } = useServerRestart();
  const { isDisconnected, isRetrying, requestRetry, dismiss } =
    useServerDisconnected();

  return (
    <>
      <ServerRestartModal
        isOpen={isRestarted}
        isReloading={isReloading}
        onReload={confirmReload}
      />
      <ServerDisconnectedModal
        isOpen={isDisconnected && !isRestarted}
        isRetrying={isRetrying}
        onRetry={requestRetry}
        onDismiss={dismiss}
      />
    </>
  );
}

export default ServerLifecycleModals;
