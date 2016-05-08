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
 * Audit model.
 *
 * @since 3.1
 */
Ext.define('NX.coreui.audit.AuditModel', {
  extend: 'Ext.data.Model',
  requires: [
    'Ext.data.SequentialIdGenerator'
  ],
  idgen: 'sequential',
  fields: [
    {name: 'domain', type: 'string', sortType: 'asUCText'},
    {name: 'type', type: 'string', sortType: 'asUCText'},
    {name: 'context', type: 'string', sortType: 'asUCText'},
    {name: 'timestamp', type: 'date'},
    {name: 'nodeId', type: 'string', sortType: 'asUCText'},
    {name: 'initiator', type: 'string', sortType: 'asUCText'},
    {name: 'attributes', type: 'auto' /*object*/}
  ]
});
