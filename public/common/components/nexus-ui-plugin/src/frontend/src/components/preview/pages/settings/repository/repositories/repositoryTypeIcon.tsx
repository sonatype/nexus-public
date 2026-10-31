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
import { Database, Cloud, FolderTree, LucideIcon } from 'lucide-react';

export type RepositoryTypeKind = 'hosted' | 'proxy' | 'group';

export interface RepositoryTypeConfig {
  icon: LucideIcon;
  /** CSS custom-property color string for direct icon `color` prop usage, e.g. 'var(--green-9)' */
  iconColor: string;
  /** Radix Themes named color token, for components like <Badge color="..."> */
  radixColor: 'green' | 'blue' | 'purple';
}

/**
 * Single source of truth for repository-type iconography and color, shared across
 * the Usage tab, Repository Summary, and the Type badge. hosted=green/Database,
 * proxy=blue/Cloud, group=purple/FolderTree.
 */
export const REPOSITORY_TYPE_CONFIG: Record<RepositoryTypeKind, RepositoryTypeConfig> = {
  hosted: { icon: Database, iconColor: 'var(--green-9)', radixColor: 'green' },
  proxy: { icon: Cloud, iconColor: 'var(--blue-9)', radixColor: 'blue' },
  group: { icon: FolderTree, iconColor: 'var(--purple-9)', radixColor: 'purple' },
};

export interface RepositoryTypeIconProps {
  type: RepositoryTypeKind;
  size?: number;
  className?: string;
}

/** Renders the canonical icon for a repository type, colored per REPOSITORY_TYPE_CONFIG. */
export function RepositoryTypeIcon({ type, size = 16, className }: RepositoryTypeIconProps): JSX.Element {
  const config = REPOSITORY_TYPE_CONFIG[type];
  const Icon = config.icon;
  return <Icon size={size} color={config.iconColor} className={className} />;
}
