/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

import {useEffect, useState} from "react";

function renderExtComponent(extContainerRef, extView, extParams, currentTitle, currentIcon) {
  const page = document.getElementsByClassName("nxrm-ext-js-wrapper")[0];
  // Use viewport height minus wrapper's top offset for the initial height.
  // page.offsetHeight can reflect a stale/overflowed value if the wrapper has already grown;
  // viewport-relative calculation gives the true available space.
  const top = page ? page.getBoundingClientRect().top : 0;
  const initialHeight = page ? Math.max(0, window.innerHeight - top) : 0;
  const initialWidth = page ? page.offsetWidth : 0;

  return Ext.create('NX.view.feature.Content', {
    itemId: 'feature-content',
    renderTo: extContainerRef.current,
    height: initialHeight,
    width: initialWidth,
    autoScroll: true,
    cls: 'nx-feature-content',
    currentTitle,
    currentIcon,
    layout: 'fit',
    items: [Ext.create(extView, extParams)]
  });
}

/**
 * Render an ExtJs container to the DOM
 * @param extContainerRef ObjectRef
 * @param extView string
 * @param extParams object
 * @param currentTitle string | undefined
 * @param currentIcon string | undefined
 * @returns {unknown}
 */
export function useExtComponent(extContainerRef, extView, extParams, currentTitle, currentIcon) {
  const [extComponent, setExtComponent] = useState(null);

  // Render and track the Ext JS component
  useEffect(() => {
    if (!extComponent) {
      setExtComponent(renderExtComponent(
          extContainerRef,
          extView,
          extParams,
          currentTitle,
          currentIcon
      ));
    }

    return () => {
      if (extComponent) {
        extComponent.destroy();
      }
    };
  }, [extComponent, extContainerRef]);

  return extComponent;
}
