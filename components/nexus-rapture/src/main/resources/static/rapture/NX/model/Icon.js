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
 * Icon model.
 *
 * @since 3.0
 */
Ext.define('NX.model.Icon', {
  extend: 'Ext.data.Model',

  idProperty: 'cls',
  fields: [
    { name: 'cls', type: 'string' },
    { name: 'name', type: 'string' },
    { name: 'file', type: 'string' },
    { name: 'variant', type: 'string' },
    { name: 'height', type: 'int' },
    { name: 'width', type: 'int' },
    { name: 'url', type: 'string' },
    { name: 'preload', type: 'boolean' }
  ]
});
