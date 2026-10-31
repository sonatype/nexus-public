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

import React, { createContext, useContext, useRef, useState, useEffect } from 'react';
import { Tooltip as RadixTooltip } from '@radix-ui/themes';
import { Provider as RadixTooltipProvider } from '@radix-ui/react-tooltip';

import './TooltipContainerContext.scss';

const TooltipContainerContext = createContext(null);

/**
 * Provider that renders a portal container div inside the Radix Theme subtree.
 * Tooltips rendered to this container inherit theme variables (--gray-12, --gray-1)
 * instead of losing them when portaled to body.
 */
export function TooltipContainerProvider({ children }) {
  const containerRef = useRef(null);
  const [container, setContainer] = useState(null);

  useEffect(() => {
    setContainer(containerRef.current);
  }, []);

  return (
    <RadixTooltipProvider delayDuration={200}>
      <TooltipContainerContext.Provider value={container}>
        <div
          ref={containerRef}
          className="nxrm-tooltip-container"
          aria-hidden
        />
        {children}
      </TooltipContainerContext.Provider>
    </RadixTooltipProvider>
  );
}

/**
 * Hook to get the portal container. Use for DropdownMenu.Content, Select.Content, etc.
 * so they render above the top nav (same z-index layer as tooltips).
 */
export function usePortalContainer() {
  return useContext(TooltipContainerContext);
}

/**
 * Tooltip that renders inside the Theme subtree so it inherits Radix theme variables.
 * Falls back to body when used outside TooltipContainerProvider.
 */
export function Tooltip(props) {
  const container = useContext(TooltipContainerContext);
  return <RadixTooltip container={container} {...props} />;
}
