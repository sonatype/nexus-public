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
export const ROWS = [
  {
    alertEmail: null,
    cronExpression: null,
    enabled: true,
    id: '637447d0-dd81-46b0-bdfe-693b4e7b8295',
    lastRun: '2023-02-02T11:49:28.404+01:00',
    lastRunResult: 'Ok [0s]',
    name: 'a-test',
    nextRun: null,
    notificationCondition: 'FAILURE',
    recurringDays: null,
    runnable: true,
    schedule: 'manual',
    startDate: null,
    status: 'WAITING',
    statusDescription: 'Waiting',
    stoppable: false,
    timeZoneOffset: null,
    typeId: 'tags.cleanup',
    typeName: 'Admin - Cleanup tags',
    properties: {},
  },
  {
    alertEmail: null,
    cronExpression: '0 0 1 * * ?',
    enabled: true,
    id: 'fa891e8a-b921-4e43-b6b9-99849380eed2',
    lastRun: null,
    lastRunResult: null,
    name: 'Cleanup service',
    nextRun: '2023-02-03T01:00:00.000+01:00',
    notificationCondition: 'FAILURE',
    recurringDays: null,
    runnable: true,
    schedule: 'advanced',
    startDate: '2023-02-02T11:19:02.308+01:00',
    status: 'WAITING',
    statusDescription: 'Waiting',
    stoppable: false,
    timeZoneOffset: null,
    typeId: 'repository.cleanup',
    typeName: 'Admin - Cleanup repositories using their associated policies',
    properties: {},
  },
  {
    alertEmail: null,
    cronExpression: null,
    enabled: true,
    id: '9a5351e8-03bf-4862-873e-ba64b84e9816',
    lastRun: null,
    lastRunResult: null,
    name: 'replication replication replication replication replication replication replication replication',
    nextRun: null,
    notificationCondition: 'FAILURE',
    recurringDays: null,
    runnable: true,
    schedule: 'manual',
    startDate: null,
    status: 'WAITING',
    statusDescription: 'Waiting',
    stoppable: false,
    timeZoneOffset: null,
    typeId: 'replication.blobattributesbackfill',
    typeName: 'Replication - Backfill blob store attributes with component metadata',
    properties: {},
  },
  {
    alertEmail: null,
    cronExpression: null,
    enabled: true,
    id: '0316d67d-f251-4cf9-b5f9-42d50953dc05',
    lastRun: null,
    lastRunResult: null,
    name: 'Test Create Cocoapods Proxy - Store remote Url in Attributes Task',
    nextRun: '2023-02-02T20:34:00.000+01:00',
    notificationCondition: 'FAILURE',
    recurringDays: null,
    runnable: true,
    schedule: 'once',
    startDate: '2023-02-02T20:34:00.000+01:00',
    status: 'WAITING',
    statusDescription: 'Waiting',
    stoppable: false,
    timeZoneOffset: null,
    typeId: 'repository.cocoapods.store-remote-url-in-attributes',
    typeName: 'Cocoapods Proxy - Store remote Url in Attributes',
    properties: {},
  },
];
