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
/*global Ext, Sonatype, NX, Nexus*/
NX.define('Sonatype.repoServer.DefaultUserEditor', {
  extend : 'Sonatype.ext.FormPanel',
  requireSuper : false,

  requires : ['Sonatype.repoServer.UserEditPanel'],

  requirejs : ['Sonatype/all'],

  constructor : function(cfg) {
    var config = cfg || {};
    var defaultConfig = {
      uri : Sonatype.config.repos.urls.users,
      labelWidth : 100,
      dataModifiers : {
        load : {
          roles : function(arr, srcObj, fpanel) {
            fpanel.find('name', 'roleManager')[0].setSelectedRoleIds(arr, true);
            return arr;
          }
        },
        submit : {
          roles : function(value, fpanel) {
            return fpanel.find('name', 'roleManager')[0].getSelectedRoleIds();
          },
          email : function(value, fpanel) {
            return Ext.util.Format.trim(value);
          }
        }
      }
    };
    Ext.apply(this, config, defaultConfig);

    // List of user statuses
    this.statusStore = new Ext.data.SimpleStore({
      fields : ['value', 'display'],
      data : [
        ['active', 'Active'],
        ['disabled', 'Disabled']
      ]
    });

    var ht = Sonatype.repoServer.resources.help.users;

    this.COMBO_WIDTH = 300;

    this.checkPayload();

    var items = [
      {
        xtype : 'textfield',
        fieldLabel : 'User ID',
        itemCls : 'required-field',
        labelStyle : 'margin-left: 15px; width: 185px;',
        helpText : ht.userId,
        name : 'userId',
        disabled : !this.isNew,
        allowBlank : false,
        width : this.COMBO_WIDTH,
        validator : Nexus.util.Strings.validateId
      },
      {
        xtype : 'textfield',
        fieldLabel : 'First Name',
        labelStyle : 'margin-left: 15px; width: 185px;',
        helpText : ht.firstName,
        name : 'firstName',
        allowBlank : false,
        itemCls : 'required-field',
        htmlDecode : true,
        width : this.COMBO_WIDTH,
        validator : function(v) {
          if (v && v.length !== 0 && v.match(Sonatype.repoServer.UserEditPanel.WHITE_SPACE_REGEX)) {
            return true;
          }

          return 'First Name cannot start with whitespace.';
        }
      },
      {
        xtype : 'textfield',
        fieldLabel : 'Last Name',
        labelStyle : 'margin-left: 15px; width: 185px;',
        helpText : ht.lastName,
        name : 'lastName',
        allowBlank : false,
        itemCls : 'required-field',
        htmlDecode : true,
        width : this.COMBO_WIDTH,
        validator : function(v) {
          if (v && v.length !== 0 && v.match(Sonatype.repoServer.UserEditPanel.WHITE_SPACE_REGEX)) {
            return true;
          }
          return 'Last Name cannot start with whitespace.';
        }
      },
      {
        xtype : 'textfield',
        fieldLabel : 'Email',
        itemCls : 'required-field',
        labelStyle : 'margin-left: 15px; width: 185px;',
        helpText : ht.email,
        name : 'email',
        allowBlank : false,
        width : this.COMBO_WIDTH
      },
      {
        xtype : 'combo',
        fieldLabel : 'Status',
        labelStyle : 'margin-left: 15px; width: 185px;',
        itemCls : 'required-field',
        helpText : ht.status,
        name : 'status',
        store : this.statusStore,
        displayField : 'display',
        valueField : 'value',
        editable : false,
        forceSelection : true,
        mode : 'local',
        triggerAction : 'all',
        emptyText : 'Select...',
        selectOnFocus : true,
        allowBlank : false,
        width : this.COMBO_WIDTH
      }
    ];

    if (this.isNew) {
      items.push({
        xtype : 'textfield',
        fieldLabel : 'New Password (optional)',
        inputType : 'password',
        labelStyle : 'margin-left: 15px; width: 185px;',
        helpText : ht.password,
        name : 'password',
        allowBlank : true,
        width : this.COMBO_WIDTH
      });
      items.push({
        xtype : 'textfield',
        fieldLabel : 'Confirm Password',
        inputType : 'password',
        labelStyle : 'margin-left: 15px; width: 185px;',
        helpText : ht.reenterPassword,
        name : 'confirmPassword',
        allowBlank : true,
        width : this.COMBO_WIDTH,
        validator : function(s) {
          var firstField = this.ownerCt.find('name', 'password')[0];
          if (firstField && firstField.getRawValue() !== s) {
            return "Passwords don't match";
          }
          return true;
        }
      });
    }

    items.push({
      xtype : 'rolemanager',
      name : 'roleManager',
      height : 200,
      width : 490,
      usePrivileges : false,
      style : 'margin-left: 15px;margin-top: 10px;border: 1px solid #B5B8C8;'
    });

    Sonatype.repoServer.DefaultUserEditor.superclass.constructor.call(this, {
      items : items,
      listeners : {
        submit : {
          fn : this.submitHandler,
          scope : this
        }
      }
    });
  },

  // FIXME parts of this look like c/p from UserMappingEditor
  combineRoles : function(val) {
    var s = '', i;
    if (val) {
      for (i = 0; i < val.length; i++) {
        if (s) {
          s += ', ';
        }
        s += this.find('name', 'roleManager')[0].getRoleNameFromId(val[i]);
      }
    }

    return s;
  },

  isValid : function() {
    return this.form.isValid() && this.find('name', 'roleManager')[0].validate();
  },

  saveHandler : function(button, event) {
    var password = this.form.getValues().password;
    this.referenceData = (this.isNew && password) ? Sonatype.repoServer.referenceData.userNew
          : Sonatype.repoServer.referenceData.users;

    return Sonatype.repoServer.DefaultUserEditor.superclass.saveHandler.call(this, button, event);
  },

  submitHandler : function(form, action, receivedData) {
    if (this.isNew) {
      receivedData.source = 'default';
      receivedData.displayRoles = this.combineRoles(receivedData.roles);
      return;
    }

    var rec = this.payload;
    rec.beginEdit();
    rec.set('firstName', receivedData.firstName);
    rec.set('lastName', receivedData.lastName);
    rec.set('email', receivedData.email);
    rec.set('status', receivedData.status);
    rec.set('displayRoles', this.combineRoles(receivedData.roles));
    rec.commit();
    rec.endEdit();
  },
  validationModifiers : {
    'roles' : function(error, panel) {
      panel.find('name', 'roleManager')[0].markInvalid(error.msg);
    }
  }
}, function() {

  Sonatype.Events.addListener('userViewInit', function(cardPanel, rec) {
    var config = {
      payload : rec,
      tabTitle : 'Config'
    };

    if (rec.data.source === 'default') {
      cardPanel.add(new Sonatype.repoServer.DefaultUserEditor(config));
    }
  });
});
