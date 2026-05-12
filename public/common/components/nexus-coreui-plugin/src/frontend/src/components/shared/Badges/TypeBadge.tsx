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
import { Badge, Flex } from '@radix-ui/themes';
import { Cloud, Database, FolderSync, LucideIcon } from 'lucide-react';
import { TYPE_LABELS } from '../../super/settings/repository/repositories/types';

import './Badges.scss';

export interface TypeBadgeProps {
  /** Repository type (hosted, proxy, group) */
  type: 'hosted' | 'proxy' | 'group';
  /** Custom class name */
  className?: string;
  /** Whether to show the icon */
  showIcon?: boolean;
}

const TYPE_CONFIG: Record<string, { color: any; icon: LucideIcon }> = {
  proxy: { color: 'blue', icon: Cloud },
  hosted: { color: 'green', icon: Database },
  group: { color: 'purple', icon: FolderSync },
};

/**
 * TypeBadge - A standardized semantic badge for repository types.
 * Blue = Proxy, Green = Hosted, Purple = Group.
 */
export function TypeBadge({
  type,
  className = '',
  showIcon = true,
}: TypeBadgeProps): JSX.Element {
  const config = TYPE_CONFIG[type] || { color: 'gray', icon: Database };
  const Icon = config.icon;
  const label = TYPE_LABELS[type] || type;

  return (
    <Badge
      color={config.color}
      variant="soft"
      className={`type-badge type-badge--${type} ${className}`}
    >
      <Flex align="center" gap="1">
        {showIcon && <Icon size={12} />}
        {label}
      </Flex>
    </Badge>
  );
}

export default TypeBadge;
