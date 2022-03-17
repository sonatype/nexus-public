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
define('Sonatype/repoServer/LdapUserEditor', function() {

  Sonatype.repoServer.LdapUserEditor = function( config ) {
    var config = config || {};
    var defaultConfig = {
      uri: Sonatype.config.servicePath + '/ldap/users',
      dataModifiers: {
        load: {
          roles: function( arr, srcObj, fpanel ) {
            var arr2 = [];
            var ldapRoles = 0;
            for ( var i = 0; i < arr.length; i++ ) {
              var a = arr[i];
              var readOnly = false;
              if ( a.source == 'LDAP' ) {
                readOnly = true;
                ldapRoles++;
              }
              arr2.push( {
                id: a.roleId,
                name: a.name,
                readOnly: readOnly
              } );
            }
            var roleBox = fpanel.find( 'name', 'roles' )[0];
            roleBox.setValue( arr2 );
            roleBox.nexusRolesEmptyOnLoad = ( arr.length == ldapRoles );

            return arr;
          }
        },
        submit: {
          roles: function( value, fpanel ) {
            return fpanel.find( 'name', 'roles' )[0].getValue();
          }
        }
      },
      referenceData: {
        userId: '',
        roles: []
      }
    };
    Ext.apply( this, config, defaultConfig );

    if ( ! Sonatype.lib.Permissions.checkPermission( 'nexus:ldapuserrolemap',
        Sonatype.lib.Permissions.EDIT ) ) {
      this.readOnly = true;
    }


    //A record to hold the name and id of a role
    this.roleRecordConstructor = Ext.data.Record.create( [
      { name: 'id' },
      { name: 'name', sortType: Ext.data.SortTypes.asUCString }
    ] );
    this.roleReader = new Ext.data.JsonReader(
      { root: 'data', id: 'id' }, this.roleRecordConstructor );
    this.roleDataStore = new Ext.data.Store( {
      url: Sonatype.config.repos.urls.roles,
      reader: this.roleReader,
      sortInfo: { field: 'name', direction: 'ASC' },
      autoLoad: true
    } );

    var ht = Sonatype.repoServer.resources.help.users;

    this.COMBO_WIDTH = 300;

    Sonatype.repoServer.DefaultUserEditor.superclass.constructor.call( this, {
      items: [
        {
          xtype: 'textfield',
          fieldLabel: 'User ID',
          itemCls: 'required-field',
          labelStyle: 'margin-left: 15px; width: 185px;',
          helpText: ht.userId,
          name: 'userId',
          disabled: true,
          allowBlank: false,
          width: this.COMBO_WIDTH
        },
        {
          xtype: 'textfield',
          fieldLabel: 'Name',
          itemCls: 'required-field',
          labelStyle: 'margin-left: 15px; width: 185px;',
          helpText: ht.name,
          name: 'name',
          disabled: true,
          allowBlank: false,
          width: this.COMBO_WIDTH
        },
        {
          xtype: 'textfield',
          fieldLabel: 'Email',
          itemCls: 'required-field',
          labelStyle: 'margin-left: 15px; width: 185px;',
          helpText: ht.email,
          name: 'email',
          disabled: true,
          allowBlank: false,
          width: this.COMBO_WIDTH
        },
        {
          xtype: 'twinpanelchooser',
          titleLeft: 'Selected Roles',
          titleRight: 'Available Roles',
          name: 'roles',
          valueField: 'id',
          store: this.roleDataStore,
          required: true,
          nodeIcon: Sonatype.config.extPath + '/resources/images/default/tree/folder.gif'
        }
      ]
    } );
  };

  Ext.extend( Sonatype.repoServer.LdapUserEditor, Sonatype.ext.FormPanel, {
    saveHandler : function( button, event ){
      if ( this.isValid() ) {
        var method = 'PUT';
        var roleBox = this.find( 'name', 'roles' )[0];
        var roles = roleBox.getValue();
        if ( roles.length == 0 ) {
          if ( roleBox.nexusRolesEmptyOnLoad ) {
            // if there weren't any nexus roles on load, and we're not saving any - do nothing
            return;
          }
          else {
            method = 'DELETE';
            roleBox.nexusRolesEmptyOnLoad = true;
          }
        }
        else {
          roleBox.nexusRolesEmptyOnLoad = false;
        }

        this.form.doAction( 'sonatypeSubmit', {
          method: method,
          url: this.uri + '/' + this.payload.data.userId,
          waitMsg: 'Updating records...',
          fpanel: this,
          dataModifiers: this.dataModifiers.submit,
          serviceDataObj: this.referenceData,
          isNew: this.isNew //extra option to send to callback, instead of conditioning on method
        } );
      }
    },

    isValid: function() {
      return this.form.isValid() && this.find( 'name', 'roles' )[0].validate();
    }
  } );

  Sonatype.Events.addListener( 'userListInit', function( userContainer ) {
    var url = Sonatype.config.servicePath + '/plexus_users/LDAP';
    if ( Sonatype.lib.Permissions.checkPermission( 'nexus:ldapuserrolemap', Sonatype.lib.Permissions.READ ) ) {
      Ext.Ajax.request( {
        url: url,
        suppressStatus: 503, // the server will return an error if LDAP is not configured
        success: function( response, options ) {
          var resp = Ext.decode( response.responseText );
          if ( resp.data ) {
            var data = resp.data;
            for ( var i = 0; i < data.length; i++ ) {
              data[i].resourceURI = Sonatype.config.servicePath + '/plexus_user/' + data[i].userId;
              if ( data[i].roles ) {
                for ( var j = 0; j < data[i].roles.length; j++ ) {
                  data[i].roles[j] = data[i].roles[j].roleId;
                }
              }
            }
            userContainer.addRecords( data, 'LDAP', Sonatype.repoServer.LdapUserEditor );
          }
        },
        scope: userContainer
      } );
    }
  } );
});
