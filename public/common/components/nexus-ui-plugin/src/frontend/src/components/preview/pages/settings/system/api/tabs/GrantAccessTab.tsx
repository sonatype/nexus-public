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
import { Box, Text } from '@radix-ui/themes';

import { GrantWizard } from '../grant/GrantWizard';
import { canGrantAccess } from '../utils/endpointPermissions';
import type { MergedApiEndpoint } from '../utils/mergeSwaggerPermissions';

export interface GrantAccessTabProps {
  row: MergedApiEndpoint;
  /** When false, skip loading grant wizard data */
  active: boolean;
}

export function GrantAccessTab({ row, active }: GrantAccessTabProps) {
  if (!canGrantAccess()) {
    return (
      <Box data-testid="api-grant-access-tab">
        <Text size="2" color="gray">
          You need additional permissions to grant API access (roles read/update/create and users update). Contact an
          administrator.
        </Text>
      </Box>
    );
  }

  return (
    <Box data-testid="api-grant-access-tab">
      <GrantWizard key={`${row.httpMethod}-${row.fullPath}`} row={row} active={active} />
    </Box>
  );
}
