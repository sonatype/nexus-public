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
 * Notification window.
 *
 * @since 3.0
 */
Ext.define('NX.view.message.Notification', {
  extend: 'Ext.ux.window.Notification',
  alias: 'widget.nx-message-notification',

  minWidth: 200,
  maxWidth: 400,
  autoShow: true,

  manager: 'default',

  // top-right, but do not obscure the header toolbar
  position: 'tr',
  paddingX: 10,
  paddingY: 55,

  stickWhileHover: true,
  slideInDuration: 800,
  slideBackDuration: 1500,
  autoCloseDelay: 4000,
  slideInAnimation: 'elasticIn',
  slideBackAnimation: 'elasticIn'
});
