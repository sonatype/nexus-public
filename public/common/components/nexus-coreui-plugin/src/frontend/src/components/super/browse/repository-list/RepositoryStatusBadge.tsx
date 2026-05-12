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
import { Flex, Text } from '@radix-ui/themes';
import { Circle } from 'lucide-react';
import type { RepositoryStatusBadgeProps } from './repository-list.types';

import './RepositoryStatusBadge.scss';

/**
 * RepositoryStatusBadge displays the online/offline status of a repository.
 *
 * Features:
 * - Green indicator for online repositories
 * - Red indicator for offline repositories
 * - Optional description and reason text
 */
export function RepositoryStatusBadge({ status }: RepositoryStatusBadgeProps): JSX.Element {
  const isOnline = status?.online ?? false;
  const statusText = isOnline ? 'Online' : 'Offline';
  const statusClass = isOnline ? 'nxrm-status-badge--online' : 'nxrm-status-badge--offline';

  return (
    <Flex direction="column" gap="1" className="nxrm-status-badge">
      <Flex align="center" gap="2">
        <Circle
          size={8}
          className={`nxrm-status-badge__indicator ${statusClass}`}
          aria-hidden="true"
        />
        <Text size="2" weight="medium">
          {statusText}
        </Text>
        {status?.description && (
          <Text size="2" color="gray">
            - {status.description}
          </Text>
        )}
      </Flex>
      {status?.reason && (
        <Text size="1" color="gray" className="nxrm-status-badge__reason">
          {status.reason}
        </Text>
      )}
    </Flex>
  );
}

export default RepositoryStatusBadge;

