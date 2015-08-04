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
define('Sonatype/repoServer/PluginConsolePanel', function() {

  Sonatype.repoServer.PluginConsolePanel = function( config ) {
    var config = config || {};
    var defaultConfig = {
      title: 'Plugin Console'
    };
    Ext.apply( this, config, defaultConfig );

    Sonatype.repoServer.PluginConsolePanel.superclass.constructor.call( this, {
      url: Sonatype.config.servicePath + '/plugin_console/plugin_infos',
      dataAntoLoad: true,
      tabbedChildren: true,
      dataSortInfo: { field: 'name', direction: 'asc' },
      tbar: [],
      columns: [
      { name: 'name',
        header: 'Name',
        width: 300
      },
      { name: 'version',
        header: 'Version',
        width: 150
      },
      { name: 'description',
        header: 'Description',
        width: 300
      },
      { name: 'status',
        id: 'status',
        header: 'Status',
        width: 100,
        renderer: function( value ){
          if ( Ext.isEmpty(value) ){
            return value;
          }
          return value.charAt(0).toUpperCase() + value.slice(1).toLowerCase();
        }
      },
      { name: 'scmVersion' },
      { name: 'scmTimestamp' },
      { name: 'failureReason' },
      { name: 'site'},
      { name: 'documentation'}
      ],
      autoExpandColumn: 'status',
      rowClickEvent: 'pluginInfoInit'
    });
  };

  Ext.extend( Sonatype.repoServer.PluginConsolePanel, Sonatype.panels.GridViewer, {
    emptyText: 'No plugins available',
    emptyTextWhileFiltering: 'No plugins matched criteria: {criteria}'
  } );

  Sonatype.Events.addListener( 'nexusNavigationInit', function( nexusPanel ) {
    var sp = Sonatype.lib.Permissions;
    if ( sp.checkPermission( 'nexus:pluginconsoleplugininfos', sp.READ) ){
      nexusPanel.add( {
        enabled: true,
        sectionId: 'st-nexus-config',
        title: 'Plugin Console',
        tabId: 'plugin_console',
        tabTitle: 'Plugin Console',
        tabCode: Sonatype.repoServer.PluginConsolePanel
      } );
    }
  } );
});