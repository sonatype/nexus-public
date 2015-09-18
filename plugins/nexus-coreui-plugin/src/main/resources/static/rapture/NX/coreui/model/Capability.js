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
/*global Ext, NX*/

/**
 * Capability model.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.model.Capability', {
  extend: 'Ext.data.Model',
  fields: [
    {name: 'id', type: 'string', sortType: 'asUCText'},
    {name: 'typeId', type: 'string', sortType: 'asUCText'},
    {name: 'enabled', type: 'boolean'},
    {name: 'notes', type: 'string', sortType: 'asUCText'},
    {name: 'properties', type: 'auto' /*object*/},

    {name: 'active', type: 'boolean'},
    {name: 'error', type: 'boolean'},
    {name: 'description', type: 'string', sortType: 'asUCText'},
    {name: 'state', type: 'string', sortType: 'asUCText'},
    {name: 'stateDescription', type: 'string', sortType: 'asUCText'},
    {name: 'status', type: 'string', sortType: 'asUCText'},
    {name: 'typeName', type: 'string', sortType: 'asUCText'},
    {name: 'tags', type: 'auto' /*object*/}
  ]
});
