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
define('Sonatype/repoServer/NexusUpgradeChecker', function() {

  Sonatype.Events.addListener( 'nexusStatus', function() {
    if ( !this.checkedNewVersion
      && Sonatype.lib.Permissions.checkPermission('nexus:status', Sonatype.lib.Permissions.READ)
      && !Ext.isEmpty( Sonatype.utils.editionShort )
      && !Ext.isEmpty( Sonatype.utils.version)){
      Ext.Ajax.request( {
        scope: this,
        method: 'GET',
        suppressStatus: [404,401,-1],
        url: Sonatype.config.servicePath + '/lvo/nexus-' +
          Sonatype.utils.editionShort.substr( 0, 3 ).toLowerCase() + '/' + Sonatype.utils.version,
        success: function( response, options ) {
          this.checkedNewVersion = true;
          var r = Ext.decode( response.responseText );

            // we get 404 if there is no new version, so we always have a valid response here
          Sonatype.utils.postWelcomePageAlert(
            '<span style="color:#000">' +
            '<b>UPGRADE AVAILABLE:</b> ' +
            'Nexus ' + Sonatype.utils.edition + ' ' + r.response.version + ' is now available. ' +
            '<a href="' + r.response.url + '" target="_blank">Download now!</a>' +
            '</span>'
          );
        },
        failure: function() {
          this.checkedNewVersion = true;
        }
      });
    }
  } );

  Ext.override( Sonatype.repoServer.ServerEditPanel, {
    beforeRenderHandler : Sonatype.repoServer.ServerEditPanel.prototype.beforeRenderHandler.createInterceptor( function(){
      var sp = Sonatype.lib.Permissions;
      if(sp.checkPermission('nexus:settings', sp.READ)
          && sp.checkPermission('nexus:settings', sp.EDIT)){
        this.add({
            xtype: 'fieldset',
            checkboxToggle:false,
            collapsible: true,
            id: this.id + '_' + 'lvoConfigurationSettings',
            title: 'New Version Availability',
            anchor: Sonatype.view.FIELDSET_OFFSET,
            autoHeight:true,
            layoutConfig: {
              labelSeparator: ''
            },
            items: [
              {
                xtype: 'checkbox',
                fieldLabel: 'Enable',
                helpText: 'Nexus will check for new releases, and will notify you in the Welcome tab when available.',
                name: 'enableVersionUpdates',
                anchor: Sonatype.view.FIELD_OFFSET,
                allowBlank: true
              }
            ]
        });
        Ext.Ajax.request({
          callback: function(options, isSuccess, response){
            if(!isSuccess){
              Sonatype.utils.connectionError( response, 'Unable to load Version Notification Configuration.', false, options.options );
            }
            else{
              var result = Ext.util.JSON.decode( response.responseText );

              this.find('name', 'enableVersionUpdates')[0].setValue( result.data.enabled );
            }
          },
          scope: this,
          method: 'GET',
          url: Sonatype.config.servicePath + '/lvo_config'
        });
      }
    }),
    actionCompleteHandler : Sonatype.repoServer.ServerEditPanel.prototype.actionCompleteHandler.createInterceptor( function(form, action){
      var sp = Sonatype.lib.Permissions;
      if ( action.type == 'sonatypeSubmit'){
        if(sp.checkPermission('nexus:settings', sp.EDIT)){
            Ext.Ajax.request({
              callback: function(options, isSuccess, response){
                if(!isSuccess){
                  Sonatype.utils.connectionError( response, 'Unable to store Notification Configuration.', false, options.options );
                }
              },
              scope: this,
              method: 'PUT',
              url: Sonatype.config.servicePath + '/lvo_config',
              jsonData: '{"data":{"enabled":' + this.find('name', 'enableVersionUpdates')[0].getValue() + '}}'
            });
          }
        }
      })
  });
});