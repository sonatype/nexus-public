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

import { useCallback, useMemo } from 'react';
import { useMachine } from '@xstate/react';
import { ExtJS } from '../../../../../../interface/ExtJS';
import { createLicensingMachine } from './licensingMachine';
import { LicenseData } from './types';

export interface UseLicensingReturn {
  license: LicenseData;
  loading: boolean;
  error: string | null;
  activeTab: string;
  setActiveTab: (tab: string) => void;
  handleLicenseInstalled: (license: LicenseData) => void;
  dismissError: () => void;
  canViewHistoricalUsage: boolean;
}

/**
 * Integration hook for the Licensing page. Wraps licensingMachine and exposes a
 * simple UI-facing contract. The historical-usage permission check lives here.
 */
export function useLicensing(): UseLicensingReturn {
  const machine = useMemo(() => createLicensingMachine(), []);
  const [state, send] = useMachine(machine);

  const canViewHistoricalUsage = useMemo(
    () => ExtJS.checkPermission('nexus:metrics:read'),
    [],
  );

  const setActiveTab = useCallback((tab: string) => send({ type: 'SET_TAB', tab }), [send]);
  const handleLicenseInstalled = useCallback(
    (license: LicenseData) => send({ type: 'LICENSE_INSTALLED', license }),
    [send],
  );
  const dismissError = useCallback(() => send({ type: 'DISMISS_ERROR' }), [send]);

  return {
    license: state.context.license,
    loading: state.matches('loading'),
    error: state.context.loadError,
    activeTab: state.context.activeTab,
    setActiveTab,
    handleLicenseInstalled,
    dismissError,
    canViewHistoricalUsage,
  };
}
