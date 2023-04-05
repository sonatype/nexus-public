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
import {ExtJS} from '@sonatype/nexus-ui-plugin';

export const isIqServerEnabled = () => {
  return ExtJS.state().getValue('clm').enabled;
};

export const canReadHealthCheck = () => {
  return ExtJS.checkPermission('nexus:healthcheck:read');
};

export const canUpdateHealthCheck = () => {
  return ExtJS.checkPermission('nexus:healthcheck:update');
};

export const canReadFirewallStatus = () => {
  return ExtJS.checkPermission('nexus:iq-violation-summary:read');
};

export const canReadHealthCheckSummary = () => {
  return ExtJS.checkPermission('nexus:healthchecksummary:read');
};

export const canReadHealthCheckDetail = () => {
  return ExtJS.checkPermission('nexus:healthcheckdetail:read');
};
