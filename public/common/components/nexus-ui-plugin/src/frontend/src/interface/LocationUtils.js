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

export function isPageHashIncluding(paths) {
  return paths.some(path => window.location.hash.includes(path));
}

export function scrollToUsageCenter() {
  const isPreviewUI = window.location.hash.startsWith('#preview');

  // In the Preview UI the Usage Center is no longer a scrollable section on the
  // dashboard overview; it lives in the dedicated "Usage Metrics" tab. Route the
  // user straight to that tab (NEXUS-53862). The Classic UI still scrolls to the
  // in-page Usage Center element.
  if (isPreviewUI) {
    window.location.hash = '#preview/browse/welcome?tab=usage-metrics';
    return;
  }

  const targetPath = '#browse/welcome';
  const targetElementId = 'nxrm-usage-center';

  function scrollToElement() {
    const usageCenterElement = document.getElementById(targetElementId);
    if (usageCenterElement) {
      usageCenterElement.scrollIntoView({ behavior: 'smooth' });
    }
  }

  if (window.location.hash !== targetPath) {
    window.location.hash = targetPath;
    setTimeout(scrollToElement, 200);
  } else {
    scrollToElement();
  }
}