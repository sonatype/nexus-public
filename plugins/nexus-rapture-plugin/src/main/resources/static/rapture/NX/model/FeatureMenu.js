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
/*global Ext*/

/**
 * Feature menu tree model.
 *
 * @since 3.0
 */
Ext.define('NX.model.FeatureMenu', {
  extend: 'Ext.data.TreeModel',

  // FIXME: Set ID for module... unsure what this should be in a tree though

  fields: [
    { name: 'path' },
    { name: 'mode' },
    { name: 'text' },
    { name: 'weight', defaultValue: 100 },
    { name: 'group', defaultValue: false },
    { name: 'view' },
    { name: 'bookmark' },
    { name: 'iconName' }
  ]
});
