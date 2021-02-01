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
/*global Ext, Sonatype, Nexus*/
define('Nexus/profile/Summary',['extjs', 'Nexus/profile/UserProfile', 'Nexus/ext/FormPanel', 'Nexus/config'], function(Ext, profile){

Ext.namespace('Nexus.profile');
Nexus.profile.Summary = function(config) {
  var
        cfg = config || {},
        defaultConfig,
        items,
        // external => user is not from XML realm but LDAP, Crowd, ...
        isExternalUser = Sonatype.user.curr.loggedInUserSource !== 'default',
        ht = Sonatype.repoServer.resources.help.users,
        WHITE_SPACE_PREFIX = /^\s/;

  if ( !cfg.username ){
    throw 'No username in config';
  }

  this.FIELD_WIDTH = 250;

  items = [
    {
      // NEXUS-5294 Disable Save for external users
      xtype : 'panel',
      cls : 'x-form-invalid-msg',
      border : false,
      html : 'Externally configured users are read-only (' + Sonatype.user.curr.loggedInUserSource + ')',
      hidden : !isExternalUser,
      width : this.FIELD_WIDTH * 2
    },
    {
      xtype : 'textfield',
      fieldLabel : 'User ID',
      itemCls : 'required-field',
      labelStyle : 'margin-left: 15px; width: 185px;',
      helpText : ht.userId,
      name : 'userId',
      disabled : true,
      allowBlank : false,
      width : this.FIELD_WIDTH
    },
    {
      xtype : 'textfield',
      fieldLabel : 'First Name',
      labelStyle : 'margin-left: 15px; width: 185px;',
      helpText : ht.firstName,
      name : 'firstName',
      allowBlank : false,
      itemCls : 'required-field',
      width : this.FIELD_WIDTH,
      disabled : isExternalUser,
      validator : function(v) {
        if (v && v.length !== 0 && (!v.match(WHITE_SPACE_PREFIX))) {
          return true;
        }
        else {
          return 'First Name cannot start with whitespace.';
        }
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
      width : this.FIELD_WIDTH,
      disabled : isExternalUser,
      validator : function(v) {
        if (v && v.length !== 0 && (!v.match(WHITE_SPACE_PREFIX))) {
          return true;
        }
        else {
          return 'Last Name cannot start with whitespace.';
        }
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
      disabled : isExternalUser,
      width : this.FIELD_WIDTH
    },
    {
      xtype : 'labelfield',
      labelStyle : 'margin-left: 15px; width: 185px;',
      fieldLabel : 'Password',
      value : 'Change Password',
      width : this.FIELD_WIDTH,
      style : 'text-decoration: underline; color: blue; cursor: pointer;',
      hidden : isExternalUser,
      hideLabel : isExternalUser,
      listeners : {
        'render' : function(component) {
          component.getEl().on('click', function() {
            Sonatype.utils.changePassword();
          });
        }
      }
    }
  ];

  defaultConfig = {
    minWidth : 650,
    labelWidth : 50,
    items : items
  };

  Ext.apply(this, cfg, defaultConfig);

  // URI template, payload.id will be appended
  this.uri = Sonatype.config.servicePath + '/user_account';

  // this is a template for the data to be sent from the form fields
  this.referenceData = {
    userId : '',
    firstName : '',
    lastName : '',
    email : ''
  };

  // defining payload.id is a must, see Sonatype.ext.FormPanel#getActionUrl
  // in short, `payload.id` will be used to extend `this.uri` if `payload.data.resourceUri` is not set.
  // otherwise, `payload.data.resourceUri` is used as is.
  this.payload = {
    id : this.username
  };

  // NEXUS-5294 Disable Save for external users
  this.isValid = function() {
    return !isExternalUser && Nexus.profile.Summary.superclass.isValid.apply(this, arguments);
  };

  // mandatory call
  this.checkPayload();

  Nexus.profile.Summary.superclass.constructor.call(this, cfg);
};

Ext.extend(Nexus.profile.Summary, Sonatype.ext.FormPanel, {
  isValid : function() {
    return this.form.isValid();
  }
});

/* don't add this view to the admin tabs in security/users, but it would work like this:
Sonatype.Events.addListener('userAdminViewInit', function(views) {
  views.push({
    name : 'Summary', // title of the tab

    // class definition of the panel, constructor will be called with {username:$selectedUsername}
    item : Nexus.profile.Summary
  });
});
*/

profile.register('Summary', Nexus.profile.Summary, ['user']);

  /* FIXME remove this when UserProfile is stable
  var testPanel = function(){
    var config = {
      html : "This is a panel"
    };
    testPanel.superclass.constructor.call(this, config);
  };

  Nexus.profile.show = true;

  Ext.extend(testPanel, Ext.Panel, {
    refreshContent : function() {
      alert('refreshing');
    },
    shouldShow : function() {
      return Nexus.profile.show;
    }
  })

  Nexus.profile.register('test', testPanel);
   */

});
