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
import { Button, Callout, Flex } from '@radix-ui/themes';
import { Info } from 'lucide-react';
import { useSref } from '@uirouter/react';
import { ExtJS } from '../../../../../interface/ExtJS';
import { useHasUser } from '../../../shared/hooks/useHasUser';

/**
 * Shown on Pro+/Enterprise when IQ Server is not configured. The Configure IQ Server action is offered only to
 * users who can open the IQ settings (nexus:settings:read).
 */
export function NoIqBanner(): JSX.Element {
  const hasUser = useHasUser();
  const canConfigure = ExtJS.usePermission(() => ExtJS.checkPermission('nexus:settings:read'), [hasUser]);
  const { href, onClick } = useSref('preview.admin.iq');

  // TODO(Hermes): final copy
  return (
    <Callout.Root role="status" color="blue" mb="4" data-testid="no-iq-banner">
      <Callout.Icon>
        <Info size={16} />
      </Callout.Icon>
      <Callout.Text>
        <Flex asChild align="center" justify="between" gap="4" wrap="wrap">
          <span>
            <span>
              Connect Sonatype IQ Server to see live Firewall security data. The pages below show example data.
            </span>
            {canConfigure && (
              <Button asChild size="2" variant="outline">
                <a href={href} onClick={onClick} data-analytics-id="nxrm-security-configure-iq-btn">
                  Configure IQ Server
                </a>
              </Button>
            )}
          </span>
        </Flex>
      </Callout.Text>
    </Callout.Root>
  );
}

export default NoIqBanner;
