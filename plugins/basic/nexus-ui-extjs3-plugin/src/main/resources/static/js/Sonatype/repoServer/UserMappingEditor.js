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
/*global Ext, Sonatype, NX*/
NX.define('Sonatype.repoServer.UserMappingEditor', {
  extend : 'Sonatype.ext.FormPanel',
  requireSuper : false,

  requires : ['Sonatype.repoServer.UserEditPanel'],

  constructor : function(cfg) {
    var config = cfg || {};
    var defaultConfig = {
      uri : Sonatype.config.repos.urls.plexusUser,
      dataModifiers : {
        load : {
          roles : function(arr, srcObj, fpanel) {
            var roleManager = fpanel.find('name', 'roleManager')[0];
            if (!roleManager) {
              // guard against rec.commit() without fpanel set - no need to translate then
              return arr;
            }
            if (this.lastLoadedId) {
              roleManager.setUserId(this.lastLoadedId);
            }
            else if (fpanel.payload && fpanel.payload.get) {
              roleManager.setUserId(fpanel.payload.get('userId'));
            }
            roleManager.setSelectedRoleIds(arr, true);
            return arr;
          }
        },
        submit : {
          roles : function(value, fpanel) {
            return fpanel.find('name', 'roleManager')[0].getSelectedRoleIds();
          }
        }
      },
      referenceData : {
        userId : '',
        source : '',
        roles : []
      }
    };
    Ext.apply(this, config, defaultConfig);

    var ht = Sonatype.repoServer.resources.help.users;

    this.COMBO_WIDTH = 300;

    var useridField;
    if (this.payload.id === 'new_mapping') {
      useridField = {
        xtype : 'trigger',
        triggerClass : 'x-form-search-trigger',
        fieldLabel : 'Enter a User ID',
        itemCls : 'required-field',
        labelStyle : 'margin-left: 15px; width: 185px;',
        name : 'userId',
        allowBlank : false,
        width : this.COMBO_WIDTH,
        listeners : {
          specialkey : {
            fn : function(f, e) {
              if (e.getKey() === e.ENTER) {
                this.loadUserId.createDelegate(this);
              }
            }
          },
          change : {
            fn : function(control, newValue, oldValue) {
              if (newValue != this.lastLoadedId) {
                this.loadUserId();
              }
            },
            scope : this
          }
        },
        onTriggerClick : this.loadUserId.createDelegate(this)
      };
    } else {
      useridField = {
        xtype : 'textfield',
        fieldLabel : 'User ID',
        itemCls : 'required-field',
        labelStyle : 'margin-left: 15px; width: 185px;',
        name : 'userId',
        disabled : true,
        allowBlank : false,
        width : this.COMBO_WIDTH,
        userFound : true
      };
    }

    Sonatype.repoServer.UserMappingEditor.superclass.constructor.call(this, {
      items : [
        {
          xtype : 'panel',
          layout : 'form',
          width : 600,
          items : [useridField, {
            xtype : 'textfield',
            fieldLabel : 'Realm',
            itemCls : 'required-field',
            labelStyle : 'margin-left: 15px; width: 185px;',
            name : 'source',
            disabled : true,
            allowBlank : false,
            width : this.COMBO_WIDTH
          }, {
            xtype : 'textfield',
            fieldLabel : 'First Name',
            itemCls : 'required-field',
            labelStyle : 'margin-left: 15px; width: 185px;',
            name : 'firstName',
            disabled : true,
            allowBlank : false,
            width : this.COMBO_WIDTH
          }, {
            xtype : 'textfield',
            fieldLabel : 'Last Name',
            itemCls : 'required-field',
            labelStyle : 'margin-left: 15px; width: 185px;',
            name : 'lastName',
            disabled : true,
            allowBlank : false,
            width : this.COMBO_WIDTH
          }, {
            xtype : 'textfield',
            fieldLabel : 'Email',
            itemCls : 'required-field',
            labelStyle : 'margin-left: 15px; width: 185px;',
            name : 'email',
            disabled : true,
            allowBlank : false,
            width : this.COMBO_WIDTH
          }, {
            id : "usermapping-rolemanager",
            xtype : 'rolemanager',
            name : 'roleManager',
            height : 200,
            width : 505,
            usePrivileges : false
          }]
        }
      ],
      listeners : {
        load : this.loadHandler,
        submit : this.submitHandler,
        scope : this
      }
    });
  },
  saveHandler : function(button, event) {
    if (this.isValid()) {
      var method = 'PUT';
      var roleManager = this.find('name', 'roleManager')[0];
      var roles = roleManager.getSelectedRoleIds();
      if (roles.length === 0) {
        if (roleManager.noRolesOnStart) {
          // if there weren't any nexus roles on load, and we're not saving
          // any - do nothing
          return;
        }
        else {
          method = 'DELETE';
        }
      }

      var url = Sonatype.config.repos.urls.userToRoles + '/' + this.form.findField('source').getValue() + '/'
            + this.form.findField('userId').getValue();

      this.form.doAction('sonatypeSubmit', {
        method : method,
        url : url,
        waitMsg : 'Updating records...',
        fpanel : this,
        dataModifiers : this.dataModifiers.submit,
        serviceDataObj : this.referenceData,
        validationModifiers : this.validationModifiers,
        isNew : this.isNew
        // extra option to send to callback, instead of conditioning on
        // method
      });
    }
  },

  // update roles if the user record with the same id is displayed in the grid
  // (auto-update doesn't work since the mapping resource does not return anything)
  submitHandler : function(form, action, receivedData) {
    var s = '', roles = [], store, rec, resourceURI,
          roleManager = this.find('name', 'roleManager')[0];

    if (this.payload.id === 'new_mapping' && this.payload.hostPanel) {
      store = this.payload.hostPanel.dataStore;
    }
    else if (this.payload.store) {
      store = this.payload.store;
    }

    if (store) {
      rec = store.getById(action.output.data.userId);
      if (!rec && this.payload.hostPanel && this.loadedUserData) {
        resourceURI = Sonatype.config.host + Sonatype.config.repos.urls.plexusUser + '/' + this.loadedUserData.userId;
        rec = new store.reader.recordType({
          //                  name : this.loadedUserData.name,
          email : this.loadedUserData.email,
          source : this.loadedUserData.source,
          userId : this.loadedUserData.userId,
          resourceURI : resourceURI,
          roles : roles,
          displayRoles : s
        }, resourceURI);
        rec.autoCreateNewRecord = true;
        store.addSorted(rec);
      }

      if (rec) {
        rec.beginEdit();
        rec.set('displayRoles', '<i>Updating...</i>');
        rec.commit();
        rec.endEdit();
        Ext.Ajax.request({
          url : Sonatype.config.host + Sonatype.config.repos.urls.plexusUser + '/' + this.loadedUserData.userId,
          success : function(response, options) {
            var i, s = '', receivedData = Ext.decode(response.responseText).data;

            for (i = 0; i < receivedData.roles.length; i += 1) {
              if (s) {
                s += ', ';
              }
              s += receivedData.roles[i].name;
            }

            rec.beginEdit();
            rec.set('roles', receivedData.roles);
            rec.set('displayRoles', s);
            rec.commit();
            rec.endEdit();
          },
          failure : function(response, options) {
            rec.beginEdit();
            rec.set('displayRoles', 'Update failed');
            rec.commit();
            rec.endEdit();
          }
        });
      }
    }
  },

  isValid : function() {
    return this.form.findField('userId').userFound && this.form.findField('source').getValue() && this.find('name',
          'roleManager')[0].validate();
  },

  loadHandler : function(form, action, receivedData) {
    this.loadedUserData = receivedData;
  },

  loadUserId : function() {
    var testField = this.form.findField('userId');
    testField.clearInvalid();
    testField.userFound = true;
    this.lastLoadedId = testField.getValue();
    var roleManager = this.find('name', 'roleManager')[0];
    roleManager.setUserId(testField.getValue());

    this.form.doAction('sonatypeLoad', {
      url : this.uri + '/' + testField.getValue(),
      method : 'GET',
      fpanel : this,
      testField : testField,
      suppressStatus : 404,
      dataModifiers : this.dataModifiers.load,
      scope : this
    });
  },

  actionFailedHandler : function(form, action) {
    if (action.response.status === 404) {
      if (action.options.testField) {
        action.options.testField.markInvalid('User record not found.');
        action.options.testField.userFound = false;
      }
      else {
        var s;
        if (this.payload && this.payload.data) {
          s = this.payload.data.source;
        }
        Sonatype.MessageBox.show({
          title : 'Error',
          msg : 'Unable to retrieve user details.' + '<br/><br/>' + 'Please make sure the ' + (s || 'selected')
                + ' realm is enabled<br/>on the server administration panel.',
          buttons : Sonatype.MessageBox.OK,
          icon : Sonatype.MessageBox.ERROR,
          animEl : 'mb3'
        });
      }
    }
    else {
      return Sonatype.repoServer.UserMappingEditor.superclass.actionFailedHandler.call(this, form, action);
    }
  },
  validationModifiers : {
    'roles' : function(error, panel) {
      Ext.getCmp('usermapping-rolemanager').markInvalid(error.msg);
    }
  }

}, function() {
  Sonatype.Events.addListener('userViewInit', function(cardPanel, rec) {
    var config = {
      payload : rec,
      tabTitle : 'Config'
    };

    if (rec.data.source !== 'default') {
      cardPanel.add(new Sonatype.repoServer.UserMappingEditor(config));
    }
  });
});
