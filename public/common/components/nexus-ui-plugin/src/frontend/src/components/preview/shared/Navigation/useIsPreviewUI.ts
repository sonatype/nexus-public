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

import { useState, useEffect } from 'react';

/**
 * Hook to detect if we're in Preview UI mode.
 * Checks if the URL hash starts with '#preview'.
 *
 * @returns {boolean} True if the current route is in Preview UI mode
 */
export function useIsPreviewUI(): boolean {
  const [isPreview, setIsPreview] = useState(() => window.location.hash.startsWith('#preview'));

  useEffect(() => {
    function checkPreview() {
      setIsPreview(window.location.hash.startsWith('#preview'));
    }

    window.addEventListener('hashchange', checkPreview);
    return () => window.removeEventListener('hashchange', checkPreview);
  }, []);

  return isPreview;
}
