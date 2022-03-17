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
define('Sonatype/repoServer/repoContextMenuContributions', function() {

  var REINDEX_ACTION = function ( rec, full ) {
    var indexUrl = null;
    if ( full ) {
      indexUrl = Sonatype.config.servicePath + '/data_index';
    } else {
      indexUrl = Sonatype.config.servicePath + '/data_incremental_index';
    }

    var url = indexUrl +
      rec.data.resourceURI.slice(Sonatype.config.host.length + Sonatype.config.servicePath.length);

    //make sure to provide /content path for repository root requests like ../repositories/central
    if (/.*\/repositories\/[^\/]*$/i.test(url) || /.*\/repo_groups\/[^\/]*$/i.test(url)){
      url += '/content';
    }

    Ext.Ajax.request({
      url: url,
      callback: function(options, isSuccess, response) {
        if ( !isSuccess ) {
          Sonatype.utils.connectionError( response, 'The server did not re-index the repository.' );
        }
      },
      scope: this,
      method: 'DELETE'
    });
  }

  Sonatype.Events.addListener( 'repositoryMenuInit',
    function( menu, repoRecord ) {
      if ( Sonatype.lib.Permissions.checkPermission( 'nexus:index', Sonatype.lib.Permissions.DELETE )
          && repoRecord.get( 'repoType' ) != 'virtual' ) {
        menu.add({
          text: 'Repair Index',
          handler: function( rec ) {
            REINDEX_ACTION( rec, true );
          },
          scope: this
        });
        menu.add( {
          text: 'Update Index',
          handler: function( rec ) {
            REINDEX_ACTION( rec, false );
          },
          scope: this
        });
      }
    }
  );

  Sonatype.Events.addListener( 'repositoryContentMenuInit',
    function( menu, repoRecord, contentRecord ) {
      if ( Sonatype.lib.Permissions.checkPermission( 'nexus:index', Sonatype.lib.Permissions.DELETE )
          && repoRecord.data['repoType'] != 'virtual' ) {
        menu.add( {
          text: 'Update Index',
          handler: function( rec ) {
            REINDEX_ACTION( rec, false );
          },
          scope: this
        });
      }
    }
  );

});