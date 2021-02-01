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
/*global Ext,Sonatype*/
(function () {

  var hostedHandler = Sonatype.repoServer.HostedRepositoryEditor.prototype.afterProviderSelectHandler;

  Ext.override( Sonatype.repoServer.HostedRepositoryEditor, {
    afterProviderSelectHandler:function ( combo, rec, index ) {
      hostedHandler.apply( this, arguments );

      if ( rec.data.provider === 'site' ) {
        this.find( 'name', 'writePolicy' )[0].setValue( 'ALLOW_WRITE' );
      } else {
        this.find( 'name', 'writePolicy' )[0].setValue( 'ALLOW_WRITE_ONCE' );
      }
    }
  } );

  Ext.override( Sonatype.repoServer.HostedRepositorySummaryPanel, {
    populateFields:function ( arr, srcObj, fpanel ) {
      Sonatype.repoServer.HostedRepositorySummaryPanel.superclass.populateFields.call( this, arr, srcObj, fpanel );

      if ( this.payload.data.format === 'site' ) {
        this.populateSiteDistributionManagementField(
          this.payload.data.id, this.payload.data.contentResourceURI
        );
      } else {
        this.populateDistributionManagementField(
          this.payload.data.id, this.payload.data.repoPolicy, this.payload.data.contentResourceURI, this.payload.data.format
        );
      }
    },
    populateSiteDistributionManagementField:function ( id, uri ) {
      var distMgmtString = '<distributionManagement>\n  <site>\n    <id>${repositoryId}</id>\n    <url>${repositoryUrl}</url>\n  </site>\n</distributionManagement>';

      distMgmtString = distMgmtString.replaceAll( '${repositoryId}', id );
      distMgmtString = distMgmtString.replaceAll( '${repositoryUrl}', uri );

      this.find( 'name', 'distMgmtField' )[0].setRawValue( distMgmtString );
    }
  } );
}());
