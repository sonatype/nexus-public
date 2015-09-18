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
 * Feature model.
 *
 * @since 3.0
 */
Ext.define('NX.model.Feature', {
  extend: 'Ext.data.Model',

  idProperty: 'path',
  fields: [
    { name: 'path' },
    { name: 'text' },
    { name: 'mode', defaultValue: 'admin' },
    { name: 'weight', defaultValue: 100 },
    { name: 'group', defaultValue: false },
    { name: 'view', defaultValue: undefined },
    { name: 'helpKeyword', defaultValue: undefined },
    { name: 'visible', defaultValue: true },
    { name: 'expanded', defaultValue: true },
    { name: 'bookmark', defaultValue: undefined },
    { name: 'iconName', defaultValue: undefined },
    { name: 'description', defaultValue: undefined },
    { name: 'authenticationRequired', defaultValue: true }
  ]
});
