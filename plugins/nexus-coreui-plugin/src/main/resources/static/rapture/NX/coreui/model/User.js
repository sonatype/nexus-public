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
 * User model.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.model.User', {
  extend: 'Ext.data.Model',
  idProperty: 'userId',
  fields: [
    {name: 'userId', type: 'string', sortType: 'asUCText'},
    {name: 'version', type: 'string', sortType: 'asUCText'},
    {name: 'realm', type: 'string', sortType: 'asUCText'},
    {name: 'firstName', type: 'string', sortType: 'asUCText'},
    {name: 'lastName', type: 'string', sortType: 'asUCText'},
    {name: 'email', type: 'string', sortType: 'asUCText'},
    {name: 'status', type: 'string', sortType: 'asUCText'},
    {name: 'roles', type: 'auto' /*array*/},
    {name: 'external', type: 'boolean'},
    {name: 'externalRoles', type: 'auto' /*array*/}
  ]
});
