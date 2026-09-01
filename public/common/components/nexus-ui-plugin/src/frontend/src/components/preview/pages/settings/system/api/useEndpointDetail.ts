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

import { useCallback, useEffect, useMemo } from 'react';
import { useMachine } from '@xstate/react';

import { ExtJS } from '../../../../../../interface/ExtJS';

import { createEndpointDetailMachine, type EndpointDetailTab } from './endpointDetailMachine';
import { canGrantAccess, canReadSecurityDirectory } from './utils/endpointPermissions';

export interface UseEndpointDetailReturn {
  tab: EndpointDetailTab;
  hasSecurityDirectoryRead: boolean;
  hasGrantAccess: boolean;
  selectTab: (tab: string) => void;
}

const KNOWN_TABS: readonly EndpointDetailTab[] = ['try', 'who', 'grant'];

function isKnownTab(value: string): value is EndpointDetailTab {
  return (KNOWN_TABS as readonly string[]).includes(value);
}

/**
 * ViewModel for {@link ../EndpointDetail}. Wraps {@link createEndpointDetailMachine} and
 * feeds it permission changes as they resolve so the machine owns the current-tab
 * selection and any auto-reset when the active tab loses visibility.
 */
export function useEndpointDetail(): UseEndpointDetailReturn {
  const hasUser = ExtJS.useUser() ?? false;
  const hasSecurityDirectoryRead = ExtJS.usePermission(() => canReadSecurityDirectory(), [hasUser]);
  const hasGrantAccess = ExtJS.usePermission(() => canGrantAccess(), [hasUser]);

  const machine = useMemo(
    () =>
      createEndpointDetailMachine({
        hasSecurityDirectoryRead,
        hasGrantAccess,
      }),
    // eslint-disable-next-line react-hooks/exhaustive-deps -- machine is created once; permission changes flow in via PERMISSIONS_UPDATED events below
    []
  );

  const [state, send] = useMachine(machine);

  useEffect(() => {
    send({ type: 'PERMISSIONS_UPDATED', hasSecurityDirectoryRead, hasGrantAccess });
  }, [hasSecurityDirectoryRead, hasGrantAccess, send]);

  const tab: EndpointDetailTab = state.matches('who') ? 'who' : state.matches('grant') ? 'grant' : 'try';

  const selectTab = useCallback(
    (nextTab: string) => {
      if (isKnownTab(nextTab)) {
        send({ type: 'SELECT_TAB', tab: nextTab });
      }
    },
    [send]
  );

  return {
    tab,
    hasSecurityDirectoryRead,
    hasGrantAccess,
    selectTab,
  };
}
