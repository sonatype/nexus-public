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
/*global NX,Nexus,Ext,Sonatype*/
/**
 * Example Plexusplugin plugin UI. Only to demonstrate how Plexus plugins work
 * and can contribute to UI.
 *
 * @since 2.7
 */
NX.define('Nexus.plexusplugin.PView', {
  extend : 'Ext.Panel',
  /*
   * config object: { feedUrl ; required title }
   */
  constructor : function(cfg) {
    var self = this;

    Ext.apply(self, cfg || {}, {
      feedUrl : '',
      title : 'PlexusPlugin Plugin'
    });
  }
}, function() {
  Sonatype.config.repos.urls.feeds = Sonatype.config.servicePath + '/feeds';
  Sonatype.Events.addListener('nexusNavigationInit', function(panel) {
    var sp = Sonatype.lib.Permissions;
    panel.add({
      enabled : sp.checkPermission('nexus:status', sp.READ),
      sectionId : 'st-nexus-views',
      title : 'PlexusPlugin Output',
      tabId : 'plxplg-view',
      tabCode : Nexus.plexusplugin.PView
    });
  });
});

