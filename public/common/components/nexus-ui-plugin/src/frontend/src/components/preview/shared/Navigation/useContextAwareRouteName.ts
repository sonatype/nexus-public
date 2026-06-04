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
import { useContext } from 'react';
import { PreviewUIContext } from './PreviewUIContext';

/**
 * Hook to get context-aware route name.
 * Prefixes with 'preview.' when in Preview UI mode.
 *
 * @param name - Base route name (e.g., 'browse.welcome')
 * @returns Context-aware route name (e.g., 'preview.browse.welcome' in Preview UI)
 */
export function useContextAwareRouteName(name: string | undefined): string | undefined {
  const isPreviewUI = useContext(PreviewUIContext);

  if (!name) return name;

  // If already has preview prefix, return as is
  if (name.startsWith('preview.')) {
    return isPreviewUI ? name : name.replace('preview.', '');
  }

  // Add preview prefix if in preview mode
  return isPreviewUI ? `preview.${name}` : name;
}
