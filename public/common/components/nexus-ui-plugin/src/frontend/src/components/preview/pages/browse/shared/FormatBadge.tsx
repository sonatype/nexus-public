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
import { Badge } from '@radix-ui/themes';
import type { FormatBadgeProps } from '../browse.types';

/**
 * Color map for repository formats.
 */
type BadgeColor = 'blue' | 'green' | 'orange' | 'purple' | 'red' | 'gray' | 'cyan' | 'pink';

const formatColorMap: Record<string, BadgeColor> = {
  // Package managers
  maven2: 'orange',
  npm: 'red',
  nuget: 'purple',
  pypi: 'blue',
  rubygems: 'red',
  go: 'cyan',
  cargo: 'orange',
  composer: 'purple',
  cocoapods: 'orange',
  conan: 'blue',
  conda: 'green',
  r: 'blue',

  // Container/Cloud
  docker: 'blue',
  helm: 'blue',

  // OS packages
  apt: 'green',
  yum: 'green',
  raw: 'gray',

  // Other
  gitlfs: 'orange',
  p2: 'purple',
  terraform: 'purple',
  terraformbackend: 'purple',
  swift: 'orange',
  huggingface: 'pink',
};

/**
 * Display name overrides (e.g., "maven2" -> "maven").
 */
const formatDisplayMap: Record<string, string> = {
  maven2: 'maven',
  gitlfs: 'git-lfs',
  huggingface: 'hugging-face',
};

/**
 * Renders a colored badge for a repository format.
 *
 * @example
 * <FormatBadge format="maven2" />
 * <FormatBadge format="npm" />
 */
export function FormatBadge({ format }: FormatBadgeProps): JSX.Element {
  const lowerFormat = format.toLowerCase();
  const color = formatColorMap[lowerFormat] || 'gray';
  const displayName = formatDisplayMap[lowerFormat] || lowerFormat;

  return (
    <Badge color={color} variant="soft" size="1">
      {displayName}
    </Badge>
  );
}

export default FormatBadge;
