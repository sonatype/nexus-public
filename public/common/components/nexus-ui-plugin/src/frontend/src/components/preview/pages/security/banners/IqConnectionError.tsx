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

import React, { useEffect, useRef, useState } from 'react';
import { Button, Callout, Flex } from '@radix-ui/themes';
import { TriangleAlert } from 'lucide-react';
import { ENDPOINTS, restClient } from '../../../../../interface/api';
import { ExtJS } from '../../../../../interface/ExtJS';
import { useHasUser } from '../../../shared/hooks/useHasUser';
import type { FirewallSecurityPagesState } from '../../../shared/useFirewallSecurityPagesTier';

type NxWindow = { NX?: { State?: { refreshNow?: () => void } } };

/**
 * Shown when IQ Server is configured but unreachable. Retry asks the server to re-probe IQ, then re-reads the
 * UI state so the tier recomputes. Tabs are not rendered.
 */
export function IqConnectionError(): JSX.Element {
  const [pending, setPending] = useState(false);
  const [failed, setFailed] = useState(false);
  const isMounted = useRef(true);
  const hasUser = useHasUser();
  const canRetry = ExtJS.usePermission(
    () => ExtJS.checkPermission('nexus:settings:read') || ExtJS.checkPermission('nexus:iq-violation-summary:read'),
    [hasUser]
  );

  useEffect(() => {
    isMounted.current = true;
    return () => {
      isMounted.current = false;
    };
  }, []);

  const retry = async () => {
    setPending(true);
    setFailed(false);
    try {
      const response = await restClient.post<FirewallSecurityPagesState>(ENDPOINTS.FIREWALL_SECURITY_IQ_STATUS_REFRESH);
      if (isMounted.current && response?.iqReachable !== true) {
        setFailed(true);
      }
      (window as unknown as NxWindow).NX?.State?.refreshNow?.();
    } catch {
      if (isMounted.current) {
        setFailed(true);
      }
    } finally {
      if (isMounted.current) {
        setPending(false);
      }
    }
  };

  // TODO(Hermes): final copy
  return (
    <Callout.Root role="alert" color="red" mb="4" data-testid="iq-connection-error">
      <Callout.Icon>
        <TriangleAlert size={16} />
      </Callout.Icon>
      <Callout.Text>
        <Flex asChild align="center" justify="between" gap="4" wrap="wrap">
          <span>
            <span>
              Nexus Repository cannot reach IQ Server. Check the IQ Server connection, then retry.
              {failed && ' Nexus Repository still cannot reach IQ Server. Try again later.'}
            </span>
            {canRetry && (
              <Button size="2" variant="outline" onClick={retry} disabled={pending} data-analytics-id="nxrm-security-iq-retry-btn">
                {pending ? 'Retrying...' : 'Retry'}
              </Button>
            )}
          </span>
        </Flex>
      </Callout.Text>
    </Callout.Root>
  );
}

export default IqConnectionError;
