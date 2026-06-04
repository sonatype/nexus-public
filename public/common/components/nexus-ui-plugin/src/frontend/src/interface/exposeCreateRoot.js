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
import React from 'react';
import { createRoot } from 'react-dom/client';

/**
 * Exposes React and createRoot on window so that ExtJS views can mount React
 * components. NX/view/SecondaryContainer.js relies on both:
 *   - window.react  for react.createElement()
 *   - window.createRoot  for mounting React trees into ExtJS DOM nodes
 *
 * Must be called from each implementation plugin's entry point (nexus-coreui-plugin,
 * nexus-cloudui-plugin). Placing it here rather than in nexus-rapture avoids
 * bundling react and react-dom twice — the plugin already needs them for its
 * own rendering.
 */
export default function exposeCreateRoot() {
  window.react = React;
  window.createRoot = createRoot;
}
