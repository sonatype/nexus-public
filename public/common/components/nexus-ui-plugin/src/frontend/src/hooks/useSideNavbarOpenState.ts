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
import { useEffect, useState } from 'react';

/**
 * Shared open/collapsed state for the left navigation sidebar.
 *
 * Returns `[isOpen, onToggleClick]`. The state is also synced to the
 * `nx-sidebar-toggle` window event so any component can open/collapse the
 * sidebar without prop drilling: dispatch `new CustomEvent('nx-sidebar-toggle')`
 * to toggle, or pass `{ detail: { open: boolean } }` to force a state.
 *
 * @param initialOpen - Initial open state (`true` = expanded, `false` = collapsed).
 */
export function useSideNavbarOpenState(initialOpen: boolean): [boolean, () => void] {
  const [isOpen, setIsOpen] = useState(initialOpen);

  const onToggleClick = () => {
    setIsOpen(!isOpen);
  };

  useEffect(() => {
    const handleGlobalToggle = (e: Event) => {
      const shouldOpen = (e as CustomEvent<{ open?: boolean }>).detail?.open;
      if (shouldOpen !== undefined) {
        setIsOpen(shouldOpen);
      } else {
        setIsOpen((prev) => !prev);
      }
    };

    window.addEventListener('nx-sidebar-toggle', handleGlobalToggle);
    return () => window.removeEventListener('nx-sidebar-toggle', handleGlobalToggle);
  }, []);

  return [isOpen, onToggleClick];
}
