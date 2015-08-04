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
/*global Ext, Sonatype, Nexus*/
/**
 * The server settings view.
 *
 * @event serverConfigViewPostInit
 * Global event for plugins to change the server settings view. (Uses Sonatype.Events.)
 * @param {Ext.FormPanel} The form panel used as the server settings view.
 */
Ext.define('Sonatype.repoServer.ServerEditPanel', {
  extend : 'Ext.Panel',
  requires : 'Nexus.util.Strings',

  constructor : function(cfg) {

    Ext.apply(this, cfg || {}, {
      autoScroll : true
    });

    var   self = this,
          ht = Sonatype.repoServer.resources.help.server,
          formId = Ext.id(),
          tfStore = new Ext.data.SimpleStore({
            fields : ['value'],
            data : [
              ['True'],
              ['False']
            ]
          }),
          smtpConnectionSettings = new Ext.data.SimpleStore({
            fields : ['value', 'display'],
            data : [
              ['plain', 'Use plain SMTP'],
              ['ssl', 'Use Secure SMTP (SSL)'],
              ['tls', 'Use STARTTLS negotiation (TLS)']
            ]
          });

    // Simply a record to hold details of each service type
    this.realmTypeRecordConstructor = Ext.data.Record.create([
      {
        name : 'roleHint',
        sortType : Ext.data.SortTypes.asUCString
      },
      {
        name : 'description'
      }
    ]);

    this.realmTypeReader = new Ext.data.JsonReader({
      root : 'data',
      id : 'roleHint'
    }, this.realmTypeRecordConstructor);

    this.realmTypeDataStore = new Ext.data.Store({
      url : Sonatype.config.repos.urls.realmComponents,
      reader : this.realmTypeReader,
      listeners : {
        load : {
          fn : this.loadServerConfig,
          scope : this
        }
      }
    });

    this.formPanel = new Ext.FormPanel({
      region : 'center',
      id : formId,
      trackResetOnLoad : true,
      autoScroll : true,
      border : false,
      frame : true,
      collapsible : false,
      collapsed : false,
      labelWidth : 175,
      layoutConfig : {
        labelSeparator : ''
      },
      items : [
        {
          xtype : 'fieldset',
          checkboxToggle : false,
          title : 'SMTP Settings',
          name : 'smtp-settings',
          anchor : Sonatype.view.FIELDSET_OFFSET,
          collapsible : true,
          autoHeight : true,
          layoutConfig : {
            labelSeparator : ''
          },

          items : [
            {
              xtype : 'textfield',
              fieldLabel : 'Hostname',
              helpText : ht.smtphost,
              name : 'smtpSettings.host',
              anchor : Sonatype.view.FIELD_OFFSET,
              allowBlank : false,
              itemCls : 'required-field'
            },
            {
              xtype : 'numberfield',
              fieldLabel : 'Port',
              helpText : ht.smtpport,
              name : 'smtpSettings.port',
              anchor : Sonatype.view.FIELD_OFFSET,
              allowBlank : false,
              itemCls : 'required-field'
            },
            {
              xtype : 'textfield',
              fieldLabel : 'Username',
              helpText : ht.smtpuser,
              name : 'smtpSettings.username',
              anchor : Sonatype.view.FIELD_OFFSET,
              allowBlank : true
            },
            {
              xtype : 'textfield',
              inputType : 'password',
              fieldLabel : 'Password',
              helpText : ht.smtppass,
              name : 'smtpSettings.password',
              anchor : Sonatype.view.FIELD_OFFSET,
              allowBlank : true
            },
            {
              xtype : 'combo',
              fieldLabel : 'Connection',
              helpText : ht.connection,
              width : 210,
              store : smtpConnectionSettings,
              valueField : 'value',
              displayField : 'display',
              editable : false,
              forceSelection : true,
              mode : 'local',
              triggerAction : 'all',
              emptyText : 'Select...',
              selectOnFocus : true,
              allowBlank : false,
              name : 'smtpSettings.connection',
              value : 'plain'
            },
            {
              xtype : 'textfield',
              fieldLabel : 'System Email',
              helpText : ht.smtpsysemail,
              name : 'smtpSettings.systemEmailAddress',
              anchor : Sonatype.view.FIELD_OFFSET,
              allowBlank : false,
              itemCls : 'required-field'
            }
          ],
          buttons : [
            {
              xtype : 'button',
              scope : this,
              text : 'Test SMTP settings',
              handler : this.testSmtpBtnHandler
            }
          ],
          buttonAlign : 'left'
        },
        {
          xtype : 'fieldset',
          checkboxToggle : false,
          title : 'HTTP Request Settings',
          anchor : Sonatype.view.FIELDSET_OFFSET,
          collapsible : true,
          autoHeight : true,
          layoutConfig : {
            labelSeparator : ''
          },

          items : [
            {
              xtype : 'textfield',
              fieldLabel : 'User Agent Customization',
              helpText : ht.userAgentString,
              name : 'globalConnectionSettings.userAgentString',
              anchor : Sonatype.view.FIELD_OFFSET,
              allowBlank : true
            },
            {
              xtype : 'textfield',
              fieldLabel : 'Additional URL Parameters',
              helpText : ht.queryString,
              name : 'globalConnectionSettings.queryString',
              anchor : Sonatype.view.FIELD_OFFSET,
              allowBlank : true
            },
            {
              xtype : 'numberfield',
              fieldLabel : 'Request Timeout',
              helpText : ht.connectionTimeout,
              afterText : 'seconds',
              name : 'globalConnectionSettings.connectionTimeout',
              width : 50,
              allowBlank : false,
              itemCls : 'required-field',
              allowDecimals : false,
              allowNegative : false,
              maxValue : 36000
            },
            {
              xtype : 'numberfield',
              fieldLabel : 'Request Retry Attempts',
              helpText : ht.retrievalRetryCount,
              name : 'globalConnectionSettings.retrievalRetryCount',
              width : 50,
              allowBlank : false,
              itemCls : 'required-field',
              allowDecimals : false,
              allowNegative : false,
              maxValue : 10
            }
          ]
        },
        // end http conn
        {
          xtype : 'fieldset',
          checkboxToggle : false,
          id : formId + '_' + 'securitySettings',
          title : 'Security Settings',
          anchor : Sonatype.view.FIELDSET_OFFSET,
          collapsible : true,
          autoHeight : true,
          layoutConfig : {
            labelSeparator : ''
          },
          items : [
            {
              xtype : 'twinpanelchooser',
              titleLeft : 'Selected Realms',
              titleRight : 'Available Realms',
              name : 'securityRealms',
              valueField : 'roleHint',
              displayField : 'description',
              store : this.realmTypeDataStore,
              required : true,
              halfSize : true
            },
            {
              xtype : 'fieldset',
              checkboxToggle : true,
              collapsed : true,
              id : formId + '_' + 'anonymousAccessSettings',
              name : 'anonymousAccessFields',
              title : 'Anonymous Access',
              anchor : Sonatype.view.FIELDSET_OFFSET,
              autoHeight : true,
              layoutConfig : {
                labelSeparator : ''
              },
              listeners : {
                'expand' : {
                  fn : function(panel) {
                    this.formPanel.isSecurityAnonymousAccessEnabled = true;
                    this.optionalFieldsetExpandHandler(panel);
                  },
                  scope : this
                },
                'collapse' : {
                  fn : function(panel) {
                    this.formPanel.isSecurityAnonymousAccessEnabled = false;
                    this.optionalFieldsetCollapseHandler(panel);
                  },
                  scope : this,
                  delay : 100
                }
              },

              items : [
                {
                  xtype : 'panel',
                  layout : 'fit',
                  html : '<div style="padding-bottom:10px">' + ht.anonymousAccess + '</div>'
                },
                {
                  xtype : 'textfield',
                  fieldLabel : 'Anonymous Username',
                  name : 'securityAnonymousUsername',
                  itemCls : 'required-field',
                  helpText : ht.anonUsername,
                  width : 100,
                  allowBlank : true,
                  anchor : Sonatype.view.FIELD_OFFSET
                },
                {
                  xtype : 'textfield',
                  fieldLabel : 'Anonymous Password',
                  name : 'securityAnonymousPassword',
                  itemCls : 'required-field',
                  inputType : 'password',
                  helpText : ht.anonPassword,
                  width : 100,
                  allowBlank : true,
                  minLength : 4,
                  minLengthText : "Password must be 4 characters or more",
                  maxLength : 25,
                  maxLengthText : "Password must be 25 characters or less",
                  anchor : Sonatype.view.FIELD_OFFSET
                }
              ]
            }
          ]
        },
        {
          xtype : 'fieldset',
          checkboxToggle : false,
          collapsed : false,
          collapsible : true,
          id : formId + '_' + 'globalRestApiSettings',
          name : 'globalRestApiSettings',
          title : 'Application Server Settings',
          anchor : Sonatype.view.FIELDSET_OFFSET,
          autoHeight : true,
          layoutConfig : {
            labelSeparator : ''
          },
          listeners : {
            'expand' : {
              fn : this.optionalFieldsetExpandHandler,
              scope : this
            },
            'collapse' : {
              fn : this.optionalFieldsetCollapseHandler,
              scope : this,
              delay : 100
            }
          },

          items : [
            {
              xtype : 'textfield',
              itemCls : 'required-field',
              fieldLabel : 'Base URL',
              helpText : ht.baseUrl,
              name : 'globalRestApiSettings.baseUrl',
              anchor : Sonatype.view.FIELD_OFFSET,
              allowBlank : true,
              validator : function(v) {
                if (!v.match(/^(?:http|https):\/\//i)) {
                  return 'Protocol must be http:// or https://';
                }

                var
                      forceCheckbox = this.ownerCt.find('name', 'globalRestApiSettings.forceBaseUrl')[0],
                      elp = this.getErrorCt();

                if (!this.allowBlank && forceCheckbox.checked && !Ext.isEmpty(v) && v.toLowerCase()
                      !== window.location.href.substring(0, v.length).toLowerCase()) {
                  if (!this.warningEl) {
                    if (!elp) {
                      return;
                    }
                    this.warningEl = elp.createChild({
                      cls : 'x-form-invalid-msg'
                    });
                    this.warningEl.setWidth(elp.getWidth(true));
                  }
                  this.warningEl.update('<b>WARNING:</b> ' + 'this Base URL setting does not match your actual URL!');
                  Ext.form.Field.msgFx[this.msgFx].show(this.warningEl, this);
                }
                else {
                  if (this.warningEl) {
                    Ext.form.Field.msgFx[this.msgFx].hide(this.warningEl, this);
                  }
                }
                return true;
              }
            },
            {
              xtype : 'checkbox',
              fieldLabel : 'Force Base URL',
              helpText : ht.forceBaseUrl,
              name : 'globalRestApiSettings.forceBaseUrl',
              anchor : Sonatype.view.FIELD_OFFSET,
              allowBlank : true,
              handler : function(checkbox, checked) {
                var baseUrlField = checkbox.ownerCt.find('name', 'globalRestApiSettings.baseUrl')[0];
                baseUrlField.validate();
              }
            },
            {
              xtype : 'numberfield',
              fieldLabel : 'UI Timeout',
              helpText : ht.uiTimeout,
              afterText : 'seconds',
              name : 'globalRestApiSettings.uiTimeout',
              width : 50,
              allowBlank : false,
              itemCls : 'required-field',
              allowDecimals : false,
              allowNegative : false,
              maxValue : 36000
            }
          ]
        },
        {
          xtype : 'fieldset',
          checkboxToggle : true,
          collapsed : true,
          id : formId + '_' + 'remoteProxySettings.httpProxySettings',
          name : 'remoteProxySettings.httpProxySettings',
          title : 'Default HTTP Proxy Settings (optional)',
          anchor : Sonatype.view.FIELDSET_OFFSET,
          autoHeight : true,
          layoutConfig : {
            labelSeparator : ''
          },
          listeners : {
            'expand' : {
              fn : Ext.createSequence(this.optionalFieldsetExpandHandler, function() {
                var fieldset = self.find('name', 'remoteProxySettings.httpsProxySettings')[0];
                // we need to use the DOM here because the 'checkboxToggle' is not a real Ext component
                fieldset.checkbox.dom.disabled = false;
              }),
              scope : this
            },
            'collapse' : {
              fn : Ext.createSequence(this.optionalFieldsetCollapseHandler, function() {
                var fieldset = self.find('name', 'remoteProxySettings.httpsProxySettings')[0];
                fieldset.collapse();
                // we need to use the DOM here because the 'checkboxToggle' is not a real Ext component
                fieldset.checkbox.dom.disabled = true;
              }),
              scope : this,
              delay : 100
            }
          },

          items : [
            {
              xtype : 'textfield',
              fieldLabel : 'Proxy Host',
              helpText : ht.proxyHostname,
              name : 'remoteProxySettings.httpProxySettings.proxyHostname',
              anchor : Sonatype.view.FIELD_OFFSET,
              itemCls : 'required-field',
              allowBlank : true,
              validator : function(v) {
                if (v.search(/:\//) === -1) {
                  return true;
                }

                return 'Specify hostname without the protocol, example "my.host.com"';
              }
            },
            {
              xtype : 'numberfield',
              fieldLabel : 'Proxy Port',
              helpText : ht.proxyPort,
              name : 'remoteProxySettings.httpProxySettings.proxyPort',
              width : 50,
              itemCls : 'required-field',
              allowBlank : true,
              allowDecimals : false,
              allowNegative : false,
              maxValue : 65535
            },
            {
              xtype : 'fieldset',
              checkboxToggle : true,
              collapsed : true,
              id : formId + '_' + 'remoteProxySettings.httpProxySettings.authentication',
              name : 'remoteProxySettings.httpProxySettings.authentication',
              title : 'Authentication (optional)',
              anchor : Sonatype.view.FIELDSET_OFFSET,
              autoHeight : true,
              layoutConfig : {
                labelSeparator : ''
              },
              listeners : {
                'expand' : {
                  fn : this.optionalFieldsetExpandHandler,
                  scope : this
                },
                'collapse' : {
                  fn : this.optionalFieldsetCollapseHandler,
                  scope : this,
                  delay : 100
                }
              },
              items : [
                {
                  xtype : 'textfield',
                  fieldLabel : 'Username',
                  helpText : ht.username,
                  name : 'remoteProxySettings.httpProxySettings.authentication.username',
                  width : 100,
                  allowBlank : true
                },
                {
                  xtype : 'textfield',
                  fieldLabel : 'Password',
                  helpText : ht.password,
                  inputType : 'password',
                  name : 'remoteProxySettings.httpProxySettings.authentication.password',
                  width : 100,
                  allowBlank : true
                },
                {
                  xtype : 'textfield',
                  fieldLabel : 'NT LAN Host',
                  helpText : ht.ntlmHost,
                  name : 'remoteProxySettings.httpProxySettings.authentication.ntlmHost',
                  anchor : Sonatype.view.FIELD_OFFSET,
                  allowBlank : true
                },
                {
                  xtype : 'textfield',
                  fieldLabel : 'NT LAN Manager Domain',
                  helpText : ht.ntlmDomain,
                  name : 'remoteProxySettings.httpProxySettings.authentication.ntlmDomain',
                  anchor : Sonatype.view.FIELD_OFFSET,
                  allowBlank : true
                }
              ]
            }, // end auth fieldset
            {
              xtype : 'textentrylist',
              name : 'nonProxyHosts',
              entryHelpText : ht.nonProxyHosts,
              entryLabel : 'Non Proxy Host',
              listLabel : 'Non Proxy Hosts'
            },
          ]
        },
        // end http proxy settings
        {
          xtype : 'fieldset',
          checkboxToggle : true,
          collapsed : true,
          id : formId + '_' + 'remoteProxySettings.httpsProxySettings',
          name : 'remoteProxySettings.httpsProxySettings',
          title : 'Default HTTPS Proxy Settings (optional, defaults to HTTP Proxy Settings )',
          anchor : Sonatype.view.FIELDSET_OFFSET,
          autoHeight : true,
          layoutConfig : {
            labelSeparator : ''
          },
          listeners : {
            'expand' : {
              fn : this.optionalFieldsetExpandHandler,
              scope : this
            },
            'collapse' : {
              fn : this.optionalFieldsetCollapseHandler,
              scope : this,
              delay : 100
            }
          },

          items : [
            {
              xtype : 'textfield',
              fieldLabel : 'Proxy Host',
              helpText : ht.proxyHostname,
              name : 'remoteProxySettings.httpsProxySettings.proxyHostname',
              anchor : Sonatype.view.FIELD_OFFSET,
              itemCls : 'required-field',
              allowBlank : true,
              validator : function(v) {
                if (v.search(/:\//) === -1) {
                  return true;
                }

                return 'Specify hostname without the protocol, example "my.host.com"';
              }
            },
            {
              xtype : 'numberfield',
              fieldLabel : 'Proxy Port',
              helpText : ht.proxyPort,
              name : 'remoteProxySettings.httpsProxySettings.proxyPort',
              width : 50,
              itemCls : 'required-field',
              allowBlank : true,
              allowDecimals : false,
              allowNegative : false,
              maxValue : 65535
            },
            {
              xtype : 'fieldset',
              checkboxToggle : true,
              collapsed : true,
              id : formId + '_' + 'remoteProxySettings.httpsProxySettings.authentication',
              name : 'remoteProxySettings.httpsProxySettings.authentication',
              title : 'Authentication (optional)',
              anchor : Sonatype.view.FIELDSET_OFFSET,
              autoHeight : true,
              layoutConfig : {
                labelSeparator : ''
              },
              listeners : {
                'expand' : {
                  fn : this.optionalFieldsetExpandHandler,
                  scope : this
                },
                'collapse' : {
                  fn : this.optionalFieldsetCollapseHandler,
                  scope : this,
                  delay : 100
                }
              },
              items : [
                {
                  xtype : 'textfield',
                  fieldLabel : 'Username',
                  helpText : ht.username,
                  name : 'remoteProxySettings.httpsProxySettings.authentication.username',
                  width : 100,
                  allowBlank : true
                },
                {
                  xtype : 'textfield',
                  fieldLabel : 'Password',
                  helpText : ht.password,
                  inputType : 'password',
                  name : 'remoteProxySettings.httpsProxySettings.authentication.password',
                  width : 100,
                  allowBlank : true
                },
                {
                  xtype : 'textfield',
                  fieldLabel : 'NT LAN Host',
                  helpText : ht.ntlmHost,
                  name : 'remoteProxySettings.httpsProxySettings.authentication.ntlmHost',
                  anchor : Sonatype.view.FIELD_OFFSET,
                  allowBlank : true
                },
                {
                  xtype : 'textfield',
                  fieldLabel : 'NT LAN Manager Domain',
                  helpText : ht.ntlmDomain,
                  name : 'remoteProxySettings.httpsProxySettings.authentication.ntlmDomain',
                  anchor : Sonatype.view.FIELD_OFFSET,
                  allowBlank : true
                }
              ]
            } // end auth fieldset
          ]
        },
        // end https proxy settings
        {
          xtype : 'fieldset',
          checkboxToggle : false,
          collapsed : false,
          collapsible : true,
          id : formId + '_' + 'systemNotificationSettings',
          name : 'systemNotificationSettings',
          title : 'System Notification Settings',
          anchor : Sonatype.view.FIELDSET_OFFSET,
          autoHeight : true,
          layoutConfig : {
            labelSeparator : ''
          },
          listeners : {
            'expand' : {
              fn : this.optionalFieldsetExpandHandler,
              scope : this
            },
            'collapse' : {
              fn : this.optionalFieldsetCollapseHandler,
              scope : this,
              delay : 100
            }
          },

          items : [
            {
              xtype : 'panel',
              layout : 'fit',
              html : '<div style="padding-bottom:10px">' + ht.systemNotification + '</div>'
            },
            {
              xtype : 'checkbox',
              fieldLabel : 'Enabled',
              helpText : ht.notificationsEnabled,
              name : 'systemNotificationSettings.enabled'
            },
            {
              xtype : 'textfield',
              fieldLabel : 'Email Addresses',
              helpText : ht.notificationEmailAddresses,
              name : 'systemNotificationSettings.emailAddresses',
              anchor : Sonatype.view.FIELD_OFFSET,
              allowBlank : true
            },
            {
              xtype : 'rolemanager',
              name : 'systemNotificationRoleManager',
              height : 200,
              width : 505,
              usePrivileges : false,
              doValidation : false,
              style : 'margin-top: 10px;border: 1px solid #B5B8C8;'
            }
          ]
        } // end notification settings
      ],
      buttons : [
        {
          text : 'Save',
          handler : this.saveBtnHandler,
          disabled : true,
          scope : this
        },
        {
          text : 'Cancel',
          handler : this.cancelBtnHandler,
          scope : this
        }
      ]
    });

    this.formPanel.buttons[0].scope = this.formPanel;
    this.formPanel.save = this.save;

    Sonatype.repoServer.ServerEditPanel.superclass.constructor.call(this, {
      autoScroll : false,
      layout : 'border',
      items : [this.formPanel]
    });

    this.formPanel.on('beforerender', this.beforeRenderHandler, this.formPanel);
    this.formPanel.on('afterlayout', this.afterLayoutHandler, this, {
      single : true
    });
    this.formPanel.form.on('actioncomplete', this.actionCompleteHandler, this.formPanel);
    this.formPanel.form.on('actionfailed', this.actionFailedHandler, this.formPanel);

    Sonatype.Events.fireEvent('serverConfigViewPostInit', this.formPanel);
  },

  optionalFieldsetExpandHandler : function(panel) {
    panel.items.each(function(item, i, len) {
      if (item.isXType('fieldset', true)) {
        this.optionalFieldsetExpandHandler(item);
      }
      else if (item.getEl() && item.getEl().up('div.required-field', 3)) {
        item.allowBlank = false;
      }
      else {
        item.allowBlank = true;
      }
    }, this);
  },

  optionalFieldsetCollapseHandler : function(panel) {
    panel.items.each(function(item, i, len) {
      if (item.isXType('fieldset', true)) {
        this.optionalFieldsetCollapseHandler(item);
      }
      else {
        item.allowBlank = true;
      }
    }, this);
  },

  saveBtnHandler : function() {
    var allValid = this.form.isValid() && this.find('name', 'securityRealms')[0].validate() && this.find('name',
          'systemNotificationRoleManager')[0].validate();

    if (allValid) {
      this.save();
    }
  },

  save : function() {
    var form = this.form;

    form.doAction('sonatypeSubmit', {
      method : 'PUT',
      url : Sonatype.config.repos.urls.globalSettingsState,
      waitMsg : 'Updating server configuration...',
      fpanel : this,
      dataModifiers : {
        "routing.followLinks" : Nexus.util.Strings.convert.stringContextToBool,
        "routing.groups.stopItemSearchOnFirstFoundFile" : Nexus.util.Strings.convert.stringContextToBool,
        "routing.groups.mergeMetadata" : Nexus.util.Strings.convert.stringContextToBool,
        "securityRealms" : function(val, fpanel) {
          return fpanel.find('name', 'securityRealms')[0].getValue();
        },
        "systemNotificationSettings.roles" : function(val, fpanel) {
          return fpanel.find('name', 'systemNotificationRoleManager')[0].getSelectedRoleIds();
        },
        "securityAnonymousAccessEnabled" : function(val, fpanel) {
          return fpanel.isSecurityAnonymousAccessEnabled;
        },
        "remoteProxySettings.nonProxyHosts" : function(val, fpanel) {
          return fpanel.find('name', 'nonProxyHosts')[0].getEntries();
        },
        "smtpSettings.sslEnabled" : function(val, fpanel) {
          return fpanel.find('name', 'smtpSettings.connection')[0].value === 'ssl';
        },
        "smtpSettings.tlsEnabled" : function(val, fpanel) {
          return fpanel.find('name', 'smtpSettings.connection')[0].value === 'tls';
        }
      },
      serviceDataObj : Sonatype.repoServer.referenceData.globalSettingsState,
      success : Sonatype.utils.updateGlobalTimeout
    });
  },

  cancelBtnHandler : function() {
    Sonatype.view.mainTabPanel.remove(this.id, true);
  },

  beforeRenderHandler : function() {
    var sp = Sonatype.lib.Permissions;
    if (sp.checkPermission('nexus:settings', sp.EDIT)) {
      this.buttons[0].disabled = false;
    }
  },

  afterLayoutHandler : function() {
    this.realmTypeDataStore.load();

    function registerQuickTips() {
      var els = Ext.select('.required-field .x-form-item-label', this.getEl());
      els.each(function(el, els, i) {
        Ext.QuickTips.register({
          target : el,
          cls : 'required-field',
          title : '',
          text : 'Required Field',
          enabled : true
        });
      });
    }

    // register required field quicktip, but we have to wait for elements to
    // show up in DOM
    registerQuickTips.defer(300, this.formPanel);

  },

  loadServerConfig : function() {
    // if stores aren't loaded, abort, they will load again when done
    if (!this.realmTypeDataStore.lastOptions) {
      return;
    }

    var fpanel = this.formPanel;

    this.formPanel.getForm().doAction('sonatypeLoad', {
      url : Sonatype.config.repos.urls.globalSettingsState,
      method : 'GET',
      fpanel : fpanel,
      dataModifiers : {
        "routing.followLinks" : Nexus.util.Strings.capitalize,
        "routing.groups.stopItemSearchOnFirstFoundFile" : Nexus.util.Strings.capitalize,
        "routing.groups.mergeMetadata" : Nexus.util.Strings.capitalize,
        "securityRealms" : function(arr, srcObj, fpanel) {
          fpanel.find('name', 'securityRealms')[0].setValue(arr);
          return arr; // return arr, even if empty to comply with sonatypeLoad data modifier requirement
        },
        "systemNotificationSettings.roles" : function(arr, srcObj, fpanel) {
          fpanel.find('name', 'systemNotificationRoleManager')[0].setSelectedRoleIds(arr, true);
          return arr;
        },
        "securityAnonymousAccessEnabled" : function(arr, srcObj, fpanel) {
          fpanel.isSecurityAnonymousAccessEnabled = arr;
        },
        "remoteProxySettings.nonProxyHosts" : function(arr, srcObj, fpanel) {
          fpanel.find('name', 'nonProxyHosts')[0].setEntries(arr);
        },
        "smtpSettings.sslEnabled" : function(arr, srcObj, fpanel) {
          if (arr) {
            fpanel.find('name', 'smtpSettings.connection')[0].setValue('ssl');
          }
        },
        "smtpSettings.tlsEnabled" : function(arr, srcObj, fpanel) {
          if (arr) {
            fpanel.find('name', 'smtpSettings.connection')[0].setValue('tls');
          }
        }
      }
    });
  },

  testSmtpBtnHandler : function() {
    var data, w, fpanel = this.formPanel;

    data = {
      testEmail : '',
      host : fpanel.form.findField('smtpSettings.host').getValue(),
      port : fpanel.form.findField('smtpSettings.port').getValue(),
      username : fpanel.form.findField('smtpSettings.username').getValue(),
      password : fpanel.form.findField('smtpSettings.password').getValue(),
      systemEmailAddress : fpanel.form.findField('smtpSettings.systemEmailAddress').getValue(),
      sslEnabled : fpanel.form.findField('smtpSettings.connection').getValue() === 'ssl',
      tlsEnabled : fpanel.form.findField('smtpSettings.connection').getValue() === 'tls'
    };

    w = new Ext.Window({
      title : 'Validate SMTP settings',
      closable : true,
      autoWidth : false,
      width : 350,
      autoHeight : true,
      modal : true,
      constrain : true,
      resizable : false,
      draggable : false,
      items : [
        {
          xtype : 'form',
          labelWidth : 60,
          frame : true,
          defaultType : 'textfield',
          monitorValid : true,
          items : [
            {
              xtype : 'panel',
              style : 'padding-left: 70px; padding-bottom: 10px',
              html : 'Please enter an email address which will receive the test email message.'
            },
            {
              fieldLabel : 'E-mail',
              name : 'email',
              width : 200,
              allowBlank : false
            }
          ],
          buttons : [
            {
              text : 'Validate',
              formBind : true,
              scope : this,
              handler : function() {
                var email = w.find('name', 'email')[0].getValue(),
                    mask = new Ext.LoadMask(w.el, {
                      msg : 'Validating SMTP settings...',
                      removeMask : true
                    });

                mask.show();

                this.runStmpConfigCheck(email, data, function(success) {
                  mask.hide();

                  if (success) {
                    w.close();
                  }
                });
              }
            },
            {
              text : 'Cancel',
              formBind : false,
              scope : this,
              handler : function() {
                w.close();
              }
            }
          ]
        }
      ]
    });

    w.show();
  },

  runStmpConfigCheck : function(testEmail, data, callback) {

    data.testEmail = testEmail;

    Ext.Ajax.request({
      method : 'PUT',
      url : Sonatype.config.repos.urls.smtpSettingsState,
      jsonData : {
        data : data
      },
      callback : function(options, success) {
        callback(success);
      },
      success : function() {
          Sonatype.MessageBox.show({
            title : 'SMTP configuration',
            msg : 'SMTP configuration validated successfully, check your inbox!',
            buttons : Sonatype.MessageBox.OK,
            icon : Sonatype.MessageBox.INFO
          });
      },
      failure : function(response) {
        Sonatype.utils.connectionError(response, 'Error on SMTP validation!', false, {
          hideErrorStatus : true,
          decodeErrorResponse : true
        });
      },
      scope : this
    });
  },

  // (Ext.form.BasicForm, Ext.form.Action)
  actionCompleteHandler : function(form, action) {
    if (action.type === 'sonatypeLoad') {
      // @note: this is a work around to get proper use of the isDirty()
      // function of this field
      if (action.options.fpanel.isSecurityAnonymousAccessEnabled) {
        action.options.fpanel.find('id', (action.options.fpanel.id + '_' + 'anonymousAccessSettings'))[0].expand();
      }
    }
  },

  // (Ext.form.BasicForm, Ext.form.Action)
  actionFailedHandler : function(form, action) {
    if (action.failureType === Ext.form.Action.CLIENT_INVALID) {
      Sonatype.MessageBox.alert('Missing or Invalid Fields',
            'Please change the missing or invalid fields.').setIcon(Sonatype.MessageBox.WARNING);
    }
    else if (action.failureType === Ext.form.Action.CONNECT_FAILURE) {
      Sonatype.utils.connectionError(action.response, 'There is an error communicating with the server.');
    }
    else if (action.failureType === Ext.form.Action.LOAD_FAILURE) {
      Sonatype.MessageBox.alert('Load Failure',
            'The data failed to load from the server.').setIcon(Sonatype.MessageBox.ERROR);
    }
  },

  anonymousCheckHandler : function(checkbox, checked) {
    this.ownerCt.find('name', 'securityAnonymousUsername')[0].setDisabled(!checked);
    this.ownerCt.find('name', 'securityAnonymousPassword')[0].setDisabled(!checked);
  }

});

