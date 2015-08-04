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
define('Sonatype/repoServer/LdapConfigPanel', function() {

  Sonatype.repoServer.LdapConfigPanel = function( config ) {
    var config = config || {};
    var defaultConfig = {
      title: 'LDAP Configuration'
    };
    Ext.apply(this, config, defaultConfig);

    this.servicePath = {
      connectionInfo: Sonatype.config.servicePath + '/ldap/conn_info',
      userAndGroupConfig: Sonatype.config.servicePath + '/ldap/user_group_conf',
      testConnectionInfo: Sonatype.config.servicePath + '/ldap/test_auth',
      testUserAndGroupConfig: Sonatype.config.servicePath + '/ldap/test_user_conf'
    };
    this.referenceData = {
      connectionInfo: {
        searchBase: '',
        authScheme: '',
        protocol: '',
        host: '',
        port: '',
        realm: '',
        systemUsername: '',
        systemPassword: ''
      },
      userAndGroupConfig: {
        emailAddressAttribute: '',
        ldapGroupsAsRoles: false,
        groupBaseDn: '',
        groupIdAttribute: '',
        groupMemberAttribute: '',
        groupMemberFormat: '',
        groupObjectClass: '',
        userPasswordAttribute: '',
        userIdAttribute: '',
        userObjectClass: '',
        userBaseDn: '',
        userRealNameAttribute: '',
        userSubtree: false,
        groupSubtree: false,
        userMemberOfAttribute: '',
        ldapFilter: ''
      }
    };

    var protocolStore = new Ext.data.SimpleStore( { fields:['value'], data:[['ldap'],['ldaps']] });

    var authenticationMethodStore = new Ext.data.SimpleStore( { fields:['value','display'],
      data:[
        ['simple', 'Simple Authentication'],
        ['none', 'Anonymous Authentication'],
        ['DIGEST-MD5', 'DIGEST-MD5'],
        ['CRAM-MD5', 'CRAM-MD5']
      ]
    });

    var groupTypeStore = new Ext.data.SimpleStore( { fields:['value','display'],
      data:[
        ['dynamic', 'Dynamic Groups'],
        ['static', 'Static Groups']
      ]
    });

    this.formPanel = new Ext.FormPanel({
      region: 'center',
      trackResetOnLoad: true,
      autoScroll: true,
      border: false,
      frame: true,
      collapsible: false,
      collapsed: false,
      labelWidth: 175,
      layoutConfig: {
        labelSeparator: ''
      },

      items: [
        {
          xtype: 'panel',
          buttonAlign: 'left',
          items: [
            {
              xtype: 'fieldset',
              checkboxToggle: false,
              title: 'Connection',
              anchor: Sonatype.view.FIELDSET_OFFSET_WITH_SCROLL,
              collapsible: true,
              autoHeight: true,
              layoutConfig: {
                labelSeparator: ''
              },

              items: [
                {
                  xtype: 'combo',
                  fieldLabel: 'Protocol',
                  itemCls: 'required-field',
                  helpText: 'Use plain text (ldap://) or secure (ldaps://) connection.',
                  name: 'protocol',
                  width: 200,
                  store: protocolStore,
                  valueField: 'value',
                  displayField: 'value',
                  editable: false,
                  forceSelection: true,
                  mode: 'local',
                  triggerAction: 'all',
                  emptyText: 'Select...',
                  selectOnFocus: true,
                  allowBlank: false,
                  listeners: {
                    select: {
                      fn: function( combo, record, index ) {
                        var newValue = combo.getValue();
                        var port = this.formPanel.find( 'name', 'port' )[0];
                        var portValue = port.getRawValue();
                        if ( newValue == 'ldap' && ( portValue == 636 || portValue == 0 ) ) {
                          port.setValue( 389 );
                        }
                        else if ( newValue == 'ldaps' && ( portValue == 389 || portValue == 0 ) ) {
                          port.setValue( 636 );
                        }
                      },
                      scope: this
                    }
                  }
                },
                {
                  xtype: 'textfield',
                  fieldLabel: 'Hostname',
                  helpText: 'The host name of the LDAP server.',
                  name: 'host',
                  anchor: Sonatype.view.FIELD_OFFSET_WITH_SCROLL,
                  allowBlank: false,
                  itemCls: 'required-field'
                },
                {
                  xtype: 'numberfield',
                  fieldLabel: 'Port',
                  helpText: 'The port the LDAP server is listening on (ldap - 389, ldaps - 636).',
                  name: 'port',
                  anchor: Sonatype.view.FIELD_OFFSET_WITH_SCROLL,
                  allowBlank: false,
                  itemCls: 'required-field'
                },
                {
                  xtype: 'textfield',
                  fieldLabel: 'Search Base',
                  helpText: 'LDAP location to be added to the connection URL, e.g. "dc=sonatype,dc=com".',
                  name: 'searchBase',
                  anchor: Sonatype.view.FIELD_OFFSET_WITH_SCROLL,
                  itemCls: 'required-field',
                  allowBlank: false
                }
              ]
            },
            {
              xtype: 'fieldset',
              checkboxToggle: false,
              title: 'Authentication',
              anchor: Sonatype.view.FIELDSET_OFFSET_WITH_SCROLL,
              collapsible: true,
              autoHeight: true,
              layoutConfig: {
                labelSeparator: ''
              },

              items: [
                {
                  xtype: 'combo',
                  fieldLabel: 'Authentication Method',
                  itemCls: 'required-field',
                  helpText: 'Authentication method.',
                  name: 'authScheme',
                  width: 200,
                  store: authenticationMethodStore,
                  valueField: 'value',
                  displayField: 'display',
                  editable: false,
                  forceSelection: true,
                  mode: 'local',
                  triggerAction: 'all',
                  emptyText: 'Select...',
                  selectOnFocus: true,
                  allowBlank: false,
                  listeners: {
                    select: {
                      fn: this.authSchemeSelectHandler,
                      scope: this
                    }
                  }
                },
                {
                  xtype: 'textfield',
                  fieldLabel: 'SASL Realm',
                  helpText: 'The SASL realm to bind to, e.g. "mydomain.com".',
                  name: 'realm',
                  anchor: Sonatype.view.FIELD_OFFSET_WITH_SCROLL,
                  allowBlank: true
                },
                {
                  xtype: 'textfield',
                  fieldLabel: 'Username',
                  helpText: 'The username or DN to bind with. If simple authentication is used, this has to be a fully qualified user name.',
                  name: 'systemUsername',
                  anchor: Sonatype.view.FIELD_OFFSET_WITH_SCROLL,
                  allowBlank: true
                },
                {
                  xtype: 'textfield',
                  fieldLabel: 'Password',
                  inputType: 'password',
                  helpText: 'The password to bind with.',
                  name: 'systemPassword',
                  anchor: Sonatype.view.FIELD_OFFSET_WITH_SCROLL,
                  allowBlank: true
                }
              ]
            }
          ],
          buttons: [
            {
              text: 'Check Authentication',
              minWidth: 120,
              handler: this.testConnectionButtonHandler,
              scope: this
            }
          ]
        },
        {
          xtype: 'panel',
          buttonAlign: 'left',
          items: [
            {
              xtype: 'fieldset',
              checkboxToggle: false,
              title: 'User Element Mapping',
              anchor: Sonatype.view.FIELDSET_OFFSET_WITH_SCROLL,
              collapsible: true,
              autoHeight: true,
              layoutConfig: {
                labelSeparator: ''
              },

              items: [
                {
                  xtype: 'textfield',
                  fieldLabel: 'Base DN',
                  helpText: 'Base location in the LDAP that the users are found, relative to the search base ("ou=people").',
                  name: 'userBaseDn',
                  anchor: Sonatype.view.FIELD_OFFSET_WITH_SCROLL,
                  allowBlank: true
                },
                {
                  xtype: 'checkbox',
                  fieldLabel: 'User Subtree',
                  helpText: 'Check this box if users are located in structures below the ' +
                    'user Base DN. If all users are located in ou=people,dc=example,dc=com, ' +
                    'this value should be false. If users are located in organizations such ' +
                    'as ou=development,ou=people,dc=example,dc=com, this value should be true.',
                  name: 'userSubtree',
                  itemCls: 'required-field',
                  allowBlank: false
                },
                {
                  xtype: 'textfield',
                  fieldLabel: 'Object Class',
                  helpText: 'LDAP class for user objects ("inetOrgPerson").',
                  name: 'userObjectClass',
                  anchor: Sonatype.view.FIELD_OFFSET_WITH_SCROLL,
                  itemCls: 'required-field',
                  allowBlank: false
                },
                {
                  xtype: 'textfield',
                  fieldLabel: 'User Filter',
                  helpText: 'LDAP search filter to additionally limit user search (for example "attribute=foo" or "(|(mail=*@domain.com)(uid=dom*))".',
                  name: 'ldapFilter',
                  anchor: Sonatype.view.FIELD_OFFSET_WITH_SCROLL,
                  allowBlank: true
                },
                {
                  xtype: 'textfield',
                  fieldLabel: 'User ID Attribute',
                  helpText: 'LDAP attribute containing user id ("userIdAttribute").',
                  name: 'userIdAttribute',
                  anchor: Sonatype.view.FIELD_OFFSET_WITH_SCROLL,
                  itemCls: 'required-field',
                  allowBlank: false
                },
                {
                  xtype: 'textfield',
                  fieldLabel: 'Real Name Attribute',
                  helpText: 'LDAP attribute containng the real name of the user ("cn").',
                  name: 'userRealNameAttribute',
                  anchor: Sonatype.view.FIELD_OFFSET_WITH_SCROLL,
                  itemCls: 'required-field',
                  allowBlank: false
                },
                {
                  xtype: 'textfield',
                  fieldLabel: 'E-Mail Attribute',
                  helpText: 'LDAP attribute containing e-mail address ("emailAddressAttribute").',
                  name: 'emailAddressAttribute',
                  anchor: Sonatype.view.FIELD_OFFSET_WITH_SCROLL,
                  itemCls: 'required-field',
                  allowBlank: false
                },
                {
                  xtype: 'textfield',
                  fieldLabel: 'Password Attribute',
                  helpText: 'LDAP attribute containing the password ("userPassword").  If this field is blank the user will be authenticated against a bind with the LDAP server.',
                  name: 'userPasswordAttribute',
                  anchor: Sonatype.view.FIELD_OFFSET_WITH_SCROLL,
                  allowBlank: true
                }
              ]
            },
            {
              xtype: 'fieldset',
              checkboxToggle: true,
              collapsed: false,
              title: 'Group Element Mapping',
              anchor: Sonatype.view.FIELDSET_OFFSET_WITH_SCROLL,
              autoHeight: true,
              layoutConfig: {
                labelSeparator: ''
              },
              listeners: {
                'expand' : {
                  fn: this.optionalFieldsetExpandHandler,
                  scope: this
                },
                'collapse' : {
                  fn: this.optionalFieldsetCollapseHandler,
                  scope: this,
                  delay: 100
                }
              },

              items: [
                {
                  xtype: 'combo',
                  itemCls: 'required-field',
                  fieldLabel: 'Group Type',
                  helpText: 'The type of group to use, static or dynamic.',
                  name: 'groupType',
                  width: 200,
                  store: groupTypeStore,
                  valueField: 'value',
                  displayField: 'display',
                  editable: false,
                  forceSelection: true,
                  mode: 'local',
                  triggerAction: 'all',
                  emptyText: 'Select...',
                  selectOnFocus: true,
                  allowBlank: false,
                  listeners: {
                    select: {
                      fn: function( combo, record, index ) {
                        var newValue = combo.getValue();

                        if ( newValue == 'static' ){
                          this.hideComponent( this.formPanel.find( 'name', 'userMemberOfAttribute' )[0] );
                          this.showComponent( this.formPanel.find( 'name', 'groupBaseDn' )[0] );
                          this.showComponent( this.formPanel.find( 'name', 'groupSubtree' )[0] );
                          this.showComponent( this.formPanel.find( 'name', 'groupObjectClass' )[0] );
                          this.showComponent( this.formPanel.find( 'name', 'groupIdAttribute' )[0] );
                          this.showComponent( this.formPanel.find( 'name', 'groupMemberAttribute' )[0] );
                          this.showComponent( this.formPanel.find( 'name', 'groupMemberFormat' )[0] );

                          this.formPanel.doLayout();
                        }
                        else {
                          this.showComponent( this.formPanel.find( 'name', 'userMemberOfAttribute' )[0] );
                          this.hideComponent( this.formPanel.find( 'name', 'groupBaseDn' )[0] );
                          this.hideComponent( this.formPanel.find( 'name', 'groupSubtree' )[0] );
                          this.hideComponent( this.formPanel.find( 'name', 'groupObjectClass' )[0] );
                          this.hideComponent( this.formPanel.find( 'name', 'groupIdAttribute' )[0] );
                          this.hideComponent( this.formPanel.find( 'name', 'groupMemberAttribute' )[0] );
                          this.hideComponent( this.formPanel.find( 'name', 'groupMemberFormat' )[0] );

                          this.formPanel.doLayout();
                        }
                      },
                      scope: this
                    }
                  }
                },
                {
                  xtype: 'textfield',
                  fieldLabel: 'Base DN',
                  helpText: 'Base location in the LDAP that the groups are found, relative to the search base ("ou=Group").',
                  name: 'groupBaseDn',
                  anchor: Sonatype.view.FIELD_OFFSET_WITH_SCROLL,
                  allowBlank: true,
                  hideParent: true
                },
                {
                  xtype: 'checkbox',
                  fieldLabel: 'Group Subtree',
                  helpText: 'Check this box if groups are located in structures below the ' +
                    'group Base DN. If all groups are located in ou=group,dc=example,dc=com, ' +
                    'this value should be false. If groups are located in organizations such ' +
                    'as ou=devgroups,ou=groups,dc=example,dc=com, this value should be true.',
                  name: 'groupSubtree',
                  itemCls: 'required-field',
                  allowBlank: true,
                  hideParent: true
                },
                {
                  xtype: 'textfield',
                  fieldLabel: 'Object Class',
                  helpText: 'LDAP class for group objects ("posixGroup").',
                  name: 'groupObjectClass',
                  anchor: Sonatype.view.FIELD_OFFSET_WITH_SCROLL,
                  itemCls: 'required-field',
                  allowBlank: true,
                  hideParent: true
                },
                {
                  xtype: 'textfield',
                  fieldLabel: 'Group ID Attribute',
                  helpText: 'LDAP attribute containing group id ("cn").',
                  name: 'groupIdAttribute',
                  anchor: Sonatype.view.FIELD_OFFSET_WITH_SCROLL,
                  itemCls: 'required-field',
                  allowBlank: true,
                  hideParent: true
                },
                {
                  xtype: 'textfield',
                  fieldLabel: 'Group Member Attribute',
                  helpText: 'LDAP attribute containing the usernames for the group.',
                  name: 'groupMemberAttribute',
                  anchor: Sonatype.view.FIELD_OFFSET_WITH_SCROLL,
                  itemCls: 'required-field',
                  allowBlank: true,
                  hideParent: true
                },
                {
                  xtype: 'textfield',
                  fieldLabel: 'Group Member Format',
                  helpText: 'The format of User ID stored in the Group Member Attribute. A token "${dn}" can be used to lookup the FQDN of the user or use something like "uid=${username},ou=people,o=sonatype" where "${username}" is replaced with the Username value.',
                  name: 'groupMemberFormat',
                  anchor: Sonatype.view.FIELD_OFFSET_WITH_SCROLL,
                  itemCls: 'required-field',
                  allowBlank: true,
                  hideParent: true
                },
                {
                  xtype: 'textfield',
                  fieldLabel: 'Member of Attribute',
                  helpText: 'Groups are generally one of two types in LDAP systems -' +
                    ' static or dynamic. A static group maintains its own' +
                    ' membership list. A dynamic group records its membership on a' +
                    ' user entry. If dynamic groups this should be set to the' +
                    ' attribute used to store the attribute that holds groups DN in the user object.',
                  name: 'userMemberOfAttribute',
                  anchor: Sonatype.view.FIELD_OFFSET_WITH_SCROLL,
                  itemCls: 'required-field',
                  allowBlank: true,
                  hideParent: true
                },
                {
                  xtype: 'checkbox',
                  hidden: true,
                  hideLabel: true,
                  height: 0,
                  name: 'ldapGroupsAsRoles',
                  checked: false
                }
              ]
            }
          ],
          buttons: [
            {
              text: 'Check User Mapping',
              minWidth: 120,
              handler: this.testUserAndGroupConfigButtonHandler,
              scope: this
            }
          ]
        }
      ],
      buttons: [
        {
          text: 'Save',
          handler: this.saveButtonHandler,
          disabled: true,
          scope: this
        },
        {
          text: 'Cancel',
          handler: this.cancelButtonHandler,
          scope: this
        }
      ]
    });


    //A record to hold the name and id of a repository
    this.userRecordConstructor = Ext.data.Record.create( [
      { name: 'userId', sortType:Ext.data.SortTypes.asUCString },
      { name: 'name' },
      { name: 'email' },
      { name: 'roles' },
      { name: 'displayRoles', mapping:'roles', convert: this.joinRoles.createDelegate( this ) }
    ] );

    //Reader and datastore that queries the server for the list of currently defined users
    this.usersReader = new Ext.data.JsonReader( { root: 'data', id: 'userId' }, this.userRecordConstructor );
    this.usersDataStore = new Ext.data.Store({
  //    url: Sonatype.config.repos.urls.users,
      reader: this.usersReader,
      sortInfo: {field: 'userId', direction: 'ASC'},
      autoLoad: false
    });

    this.usersGridPanel = new Ext.grid.GridPanel({
      title: 'User Mapping Test Results',
      region: 'south',
      layout: 'fit',
      collapsible: true,
      collapsed: true,
      split: true,
      height: 200,
      minHeight: 150,
      maxHeight: 400,
      frame: false,
      autoScroll: true,

      //grid view options
      ds: this.usersDataStore,
      sortInfo: { field: 'userId', direction: 'asc' },
      loadMask: true,
      deferredRender: false,
      columns: [
        { header: 'User ID', dataIndex: 'userId', width:100, id: 'user-config-userid-col' },
        { header: 'Name', dataIndex: 'name', width:175, id: 'user-config-name-col' },
        { header: 'Email', dataIndex: 'email', width:175, id: 'user-config-email-col' },
        { header: 'Roles', dataIndex: 'displayRoles', width:175, id: 'user-config-roles-col' }
      ],
      autoExpandColumn: 'user-config-roles-col',
      disableSelection: true,
      viewConfig: {
        emptyText: 'No user records found.'
      }
  //    listeners: {
  //      expand: {
  //        fn: function( panel ) {
  //          this.doLayout();
  //        },
  //        scope: this
  //      }
  //    }
    });

    Sonatype.repoServer.LdapConfigPanel.superclass.constructor.call(this, {
      autoScroll: false,
      layout: 'border',
      items: [
        this.formPanel,
        this.usersGridPanel
      ]
    });

    this.formPanel.on( 'beforerender', this.beforeRenderHandler, this.formPanel );
    this.formPanel.on( 'afterlayout', this.afterLayoutHandler, this, { single: true } );
    this.formPanel.form.on( 'actioncomplete', this.actionCompleteHandler, this );
    this.formPanel.form.on( 'actionfailed', this.actionFailedHandler, this.formPanel );
  };

  Ext.extend(Sonatype.repoServer.LdapConfigPanel, Ext.Panel, {
    authSchemeSelectHandler : function( combo, record, index ){
      if ( combo.getValue() == 'simple' ){
        this.find('name', 'realm' )[0].disable();
        this.find('name', 'systemUsername' )[0].enable();
        this.find('name', 'systemPassword' )[0].enable();
      }
      else if ( combo.getValue() == 'none' ){
        this.find('name', 'realm' )[0].disable();
        this.find('name', 'systemUsername' )[0].disable();
        this.find('name', 'systemUsername' )[0].setValue( '' );
        this.find('name', 'systemPassword' )[0].disable();
        this.find('name', 'systemPassword' )[0].setValue( '' );
      }
      else if ( combo.getValue() == 'DIGEST-MD5' ){
        this.find('name', 'realm' )[0].enable();
        this.find('name', 'systemUsername' )[0].enable();
        this.find('name', 'systemPassword' )[0].enable();
      }
      else if ( combo.getValue() == 'CRAM-MD5' ){
        this.find('name', 'realm' )[0].enable();
        this.find('name', 'systemUsername' )[0].enable();
        this.find('name', 'systemPassword' )[0].enable();
      }
    },

    beforeRenderHandler : function(){
      var sp = Sonatype.lib.Permissions;
      if(sp.checkPermission('nexus:settings', sp.EDIT)){
        this.buttons[0].disabled = false;
      }
    },

    //(Ext.form.BasicForm, Ext.form.Action)
    actionCompleteHandler : function( form, action ) {
      if ( action.type == 'sonatypeSubmit' ) {
        if ( action.options.url == this.servicePath.testUserAndGroupConfig ) {
          var title = 'User Mapping Test Results';
          if ( action.output.data.ldapGroupsAsRoles == true && action.result.data != null ) {
            var r = action.result.data;
            var n = 0;
            for ( var i = 0; i < r.length; i++ ) {
              n += r[i].roles.length;
            }
            if ( n == 0 ) {
              title += ' <span class="x-toolbar-warning"><b>WARNING:</b> the test returned no roles, group mapping may not be valid.</span>';
            }
          }
          this.usersGridPanel.setTitle( title );
          this.usersDataStore.loadData( action.result.data ? action.result : { data: [] } );
          this.usersGridPanel.expand();
        }
        else if ( action.options.url == this.servicePath.testConnectionInfo ) {
          Sonatype.MessageBox.alert( 'Authentication Test',
            'LDAP connection and authentication test completed successfully.' )
            .setIcon( Sonatype.MessageBox.INFO );
        }
      }
      else if ( action.type == 'sonatypeLoad' &&
        action.options.url == this.servicePath.userAndGroupConfig ) {

        var userComponent = this.find( 'name', 'userMemberOfAttribute' )[0];

        if ( !Ext.isEmpty( userComponent.getValue() ) ) {
          this.find( 'name', 'groupType' )[0].setValue( 'dynamic' );

          this.showComponent( userComponent );
          this.hideComponent( this.find( 'name', 'groupBaseDn' )[0] );
          this.hideComponent( this.find( 'name', 'groupSubtree' )[0] );
          this.hideComponent( this.find( 'name', 'groupObjectClass' )[0] );
          this.hideComponent( this.find( 'name', 'groupIdAttribute' )[0] );
          this.hideComponent( this.find( 'name', 'groupMemberAttribute' )[0] );
          this.hideComponent( this.find( 'name', 'groupMemberFormat' )[0] );
        }
        else {
          this.find( 'name', 'groupType' )[0].setValue( 'static' );

          this.hideComponent( userComponent );
          this.showComponent( this.find( 'name', 'groupBaseDn' )[0] );
          this.showComponent( this.find( 'name', 'groupSubtree' )[0] );
          this.showComponent( this.find( 'name', 'groupObjectClass' )[0] );
          this.showComponent( this.find( 'name', 'groupIdAttribute' )[0] );
          this.showComponent( this.find( 'name', 'groupMemberAttribute' )[0] );
          this.showComponent( this.find( 'name', 'groupMemberFormat' )[0] );
        }

        var ldapGroupsAsRoles = this.find( 'name', 'ldapGroupsAsRoles' )[0];
        if ( ldapGroupsAsRoles.getValue() != true ) {
          ldapGroupsAsRoles.ownerCt.collapse();
        }
      }
    },

    hideComponent : function( component ) {
      component.disable();
      component.hide();
    },

    showComponent : function( component ) {
      component.enable();
      component.show();
    },

    //(Ext.form.BasicForm, Ext.form.Action)
    actionFailedHandler : function( form, action ) {
      if ( action.failureType == null ) {
        Sonatype.utils.connectionError( action.response, null, null, action.options );
      }
      else if(action.failureType == Ext.form.Action.CLIENT_INVALID){
        Sonatype.MessageBox.alert('Missing or Invalid Fields', 'Please change the missing or invalid fields.').setIcon(Sonatype.MessageBox.WARNING);
      }
      //@note: server validation error are now handled just like client validation errors by marking the field invalid
  //  else if(action.failureType == Ext.form.Action.SERVER_INVALID){
  //    Sonatype.MessageBox.alert('Invalid Fields', 'The server identified invalid fields.').setIcon(Sonatype.MessageBox.ERROR);
  //  }
      else if(action.failureType == Ext.form.Action.CONNECT_FAILURE){
        Sonatype.utils.connectionError( action.response, 'There is an error communicating with the server.' )
      }
      else if(action.failureType == Ext.form.Action.LOAD_FAILURE){
        Sonatype.MessageBox.alert('Load Failure', 'The data failed to load from the server.').setIcon(Sonatype.MessageBox.ERROR);
      }


      //@todo: need global alert mechanism for fatal errors.
    },

    testConnectionButtonHandler : function() {
      var form = this.formPanel.form;

      if ( ! form.isValid() ) return;

      form.doAction( 'sonatypeSubmit', {
        method: 'PUT',
        url: this.servicePath.testConnectionInfo,
        waitMsg: 'Testing LDAP connection and authentication...',
        fpanel: this.formPanel,
        serviceDataObj: this.referenceData.connectionInfo,
        hideErrorStatus: true,
        autoValidation: false
      } );
    },

    testUserAndGroupConfigButtonHandler : function() {
      var form = this.formPanel.form;

      if ( ! form.isValid() ) return;

      form.doAction( 'sonatypeSubmit', {
        method: 'PUT',
        url: this.servicePath.testUserAndGroupConfig,
        waitMsg: 'Testing user and role mapping...',
        fpanel: this.formPanel,
        params: {
          userLimitCount: 20,
          _dc: new Date().getTime()
        },
        serviceDataObj: Ext.apply( {}, this.referenceData.connectionInfo, this.referenceData.userAndGroupConfig ),
        autoValidation: false
      } );
    },

    saveButtonHandler : function() {
      var form = this.formPanel.form;

      if ( ! form.isValid() ) return;

      if ( this.formPanel.find( 'name', 'groupType' )[0].value == 'static' ) {
          this.formPanel.find( 'name', 'userMemberOfAttribute' )[0].setValue('');
      }

      form.doAction('sonatypeSubmit', {
        method: 'PUT',
        url: this.servicePath.connectionInfo,
        waitMsg: 'Updating LDAP configuration...',
        fpanel: this.formPanel,
        serviceDataObj: this.referenceData.connectionInfo
      });

      form.doAction('sonatypeSubmit', {
        method: 'PUT',
        url: this.servicePath.userAndGroupConfig,
        waitMsg: 'Updating user and group mapping...',
        fpanel: this.formPanel,
        serviceDataObj: this.referenceData.userAndGroupConfig
      });
    },

    cancelButtonHandler : function() {
      Sonatype.view.mainTabPanel.remove( this.id, true );
    },

    afterLayoutHandler : function(){

      // invoke form data load
      this.formPanel.getForm().doAction( 'sonatypeLoad', {
        url: this.servicePath.connectionInfo,
        method: 'GET',
        fpanel: this.formPanel
      } );
      this.formPanel.getForm().doAction( 'sonatypeLoad', {
        url: this.servicePath.userAndGroupConfig,
        method: 'GET',
        fpanel: this.formPanel
      } );

      // register required field quicktip, but have to wait for elements to show up in DOM
      var temp = function(){
        var els = Ext.select('.required-field .x-form-item-label', this.formPanel.getEl());
        els.each(function(el, els, i){
          Ext.QuickTips.register({
            target: el,
            cls: 'required-field',
            title: '',
            text: 'Required Field',
            enabled: true
          });
        });
      }.defer(300, this);
    },

    optionalFieldsetExpandHandler : function(panel){
      this.find( 'name', 'ldapGroupsAsRoles' )[0].setValue( true );
      panel.items.each(function(item, i, len){
        if (item.isXType('fieldset', true)){
          this.optionalFieldsetExpandHandler(item);
        }
        else if (item.getEl() != null && item.getEl().up('div.required-field', 3)){
          item.allowBlank = false;
        }
        else {
          item.allowBlank = true;
        }
      }, this); // "this" is RepoEditPanel
    },

    optionalFieldsetCollapseHandler : function(panel){
      this.find( 'name', 'ldapGroupsAsRoles' )[0].setValue( false );
      panel.items.each(function(item, i, len){
        if (item.isXType('fieldset', true)){
          this.optionalFieldsetCollapseHandler(item);
        }
        else {
          item.allowBlank = true;
        }
      }, this); // "this" is RepoEditPanel
    },

    joinRoles: function( value, parent ) {
      var s = "";
      for ( var i = 0; i < value.length; i++ ) {
        if ( i > 0 ) {
          s += ', ';
        }
        s += value[i];
      }
      return s;
    }
  });

  Sonatype.Events.addListener( 'nexusNavigationInit', function( nexusPanel ) {
    nexusPanel.add( {
      enabled: Sonatype.lib.Permissions.checkPermission( 'nexus:ldapconninfo', Sonatype.lib.Permissions.READ ),
      sectionId: 'st-nexus-security',
      title: 'LDAP Configuration',
      tabId: 'ldap-configuration',
      tabCode: Sonatype.repoServer.LdapConfigPanel
    } );
  } );

});
