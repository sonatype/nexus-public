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
 * LDAP Schema Template model.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.model.LdapSchemaTemplate', {
  extend: 'Ext.data.Model',
  fields: [
    {name: 'name', type: 'string', sortType: 'asUCText'},

    {name: 'userBaseDn', type: 'string'},
    {name: 'userSubtree', type: 'boolean'},
    {name: 'userObjectClass', type: 'string'},
    {name: 'userLdapFilter', type: 'string'},
    {name: 'userIdAttribute', type: 'string'},
    {name: 'userRealNameAttribute', type: 'string'},
    {name: 'userEmailAddressAttribute', type: 'string'},
    {name: 'userPasswordAttribute', type: 'string'},

    {name: 'ldapGroupsAsRoles', type: 'boolean'},
    {name: 'userMemberOfAttribute', type: 'string'},
    {name: 'groupType', type: 'string'},
    {name: 'groupBaseDn', type: 'string'},
    {name: 'groupSubtree', type: 'boolean'},
    {name: 'groupIdAttribute', type: 'string'},
    {name: 'groupMemberAttribute', type: 'string'},
    {name: 'groupMemberFormat', type: 'string'},
    {name: 'groupObjectClass', type: 'string'}
  ]
});
