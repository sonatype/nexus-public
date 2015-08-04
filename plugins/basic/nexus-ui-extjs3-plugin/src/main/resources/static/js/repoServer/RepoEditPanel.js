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
/*
 * Repository Edit/Create panel layout and controller
 */

define('repoServer/RepoEditPanel',['Sonatype/all', 'Sonatype/strings'], function(Sonatype, Strings){
var REPO_REMOTE_STORAGE_REGEXP = /^(?:http|https|ftp):\/\//i;

Sonatype.repoServer.AbstractRepositoryEditor = function(config) {
  var config = config || {};
  var defaultConfig = {
    uri : Sonatype.config.repos.urls.repositories,
    resetButton : true,
    defaultTimeoutValues : {}
  };
  Ext.apply(this, config, defaultConfig);

  Sonatype.repoServer.AbstractRepositoryEditor.superclass.constructor.call(this, {
        listeners : {
          submit : {
            fn : this.submitHandler,
            scope : this
          },
          load : {
              fn : this.loadHandler,
              scope : this
          }
        }
      });

  this.on('show', this.showHandler, this);
};

Ext.extend(Sonatype.repoServer.AbstractRepositoryEditor, Sonatype.ext.FormPanel, {
      loadData : function() {
        if (this.isNew)
        {
          var templateModifiers = Ext.apply({}, {
                id : Strings.returnEmptyStr,
                name : Strings.returnEmptyStr
              }, this.dataModifiers.load);

          this.form.on('actioncomplete', function(form, action) {
                form.clearInvalid();
              }, {
                single : true
              });
          this.form.doAction('sonatypeLoad', {
                url : Sonatype.config.repos.urls.repoTemplate[this.payload.data.repoType],
                method : 'GET',
                fpanel : this,
                dataModifiers : templateModifiers,
                scope : this,
                success : this.templateLoadSuccess.createDelegate(this)
              });
        }
        else
        {
          Sonatype.repoServer.AbstractRepositoryEditor.superclass.loadData.call(this);
        }
      },

      /* For plugin contributions:

      contributions added here are valid for hosted and proxy repository editors.

      When a form is build or it's values reset (e.g. load/select provider),
      contributions are selected according to their conditions:
      the condition objects's properties will be checked for equality with
      properties from the data set.
        e.g. var condition = { provider: 'maven2' }
      If all keys match,  the 'enter' method will be called with the form data and the form itself.

      The 'leave' method is called only when the provider selection changed (for new repositories).
      The data given is already changed and does not resemble the data used to select contributions.
      This method will only be called if the contribution's 'enter' method was called before.

       */
      contributions : {
        add : function(condition, enter, leave) {
          this.list.push(
            {
              condition: condition,
              enter: enter,
              leave: leave
            }
          )
        },
        list : new Array(),
        lastPlugins : new Array()
      },

      contribute : function( data, action)
      {
          if ( action === 'leave' && this.contributions.lastPlugins.length != 0 )
          {
              for ( i = 0; i < this.contributions.lastPlugins.length; i++ )
              {
                  contrib = this.contributions.lastPlugins[i];
                  contrib.leave( data, this.form );
              }
              this.contributions.lastPlugins = new Array();
          } else {
              plugins:
              for ( i = 0; i < this.contributions.list.length; i++ )
              {
                  contrib = this.contributions.list[i];
                  for ( key in contrib.condition )
                  {
                      if ( contrib.condition.hasOwnProperty( key ) && data[key] !== contrib.condition[key] )
                      {
                          continue plugins;
                      }
                  }
                  this.contributions.lastPlugins.push(contrib);
                  contrib[action]( data, this.form );
              }
          }
      },

      providerSelectHandler : function(combo, rec, index) {
          this.form.findField( 'format' ).setValue( rec.data.format );
          this.form.findField( 'providerRole' ).setValue( rec.data.providerRole );
          this.afterProviderSelectHandler( combo, rec, index );

          // let plugins change UI
          this.contribute( rec.data, 'enter' )
      },

      templateLoadSuccess : function(form, action) {},


      repoPolicySelectHandler : function(combo, rec, index) {
        var repoPolicy = rec.data.value.toLowerCase();
        var fields = ['notFoundCacheTTL', 'artifactMaxAge', 'metadataMaxAge', 'itemMaxAge'];

        if (this.lastPolicy != repoPolicy)
        {
          if (this.setBackExpValsFunc)
          {
            this.setBackExpValsFunc();
          }
          else
          {
            var oldValues = {};
            for (var i = fields.length - 1; i >= 0; i--)
            {
              var formField = this.form.findField(fields[i]);
              if (formField)
              {
                oldValues[fields[i]] = formField.getValue();
              }
            }

            this.setBackExpValsFunc = function() {
              this.form.setValues(oldValues);
              this.setBackExpValsFunc = null;
            }.createDelegate(this);

            var repoType = this.form.findField('repoType').getValue();

            if (this.defaultTimeoutValues[repoPolicy])
            {
              this.form.setValues(this.defaultTimeoutValues[repoPolicy]);
            }
            else
            {
              Ext.Ajax.request({
                    scope : this,
                    url : Sonatype.config.repos.urls.repoTemplate[repoType + '_' + repoPolicy],
                    callback : function(options, success, response) {
                      if (success)
                      {
                        var templateData = Ext.decode(response.responseText);
                        this.defaultTimeoutValues[repoPolicy] = {};
                        for (var i = fields.length - 1; i >= 0; i--)
                        {
                          var formField = this.form.findField(fields[i]);
                          if (formField)
                          {
                            this.defaultTimeoutValues[repoPolicy][fields[i]] = templateData.data[fields[i]];
                          }
                        }

                        this.form.setValues(this.defaultTimeoutValues[repoPolicy]);
                      }
                    }
                  });
            }

          }

          // filter the Deploy Policy combo
          this.updateWritePolicy();

          this.lastPolicy = repoPolicy;
        }
      },

      repoPolicySubmitDataModifier : function() {

      },
      updateWritePolicy : function() {

      },

      submitHandler : function(form, action, receivedData) {
        if (this.isNew)
        {
          if (!receivedData.resourceURI)
          {
            receivedData.resourceURI = Sonatype.config.host + Sonatype.config.repos.urls.repositories + '/' + receivedData.id;
            if (receivedData.exposed == null)
            {
              receivedData.exposed = true;
            }
            if (receivedData.userManaged == null)
            {
              receivedData.userManaged = true;
            }
          }

          var repoPanel = Ext.getCmp('view-repositories');
          repoPanel.statusStart();

          // convert case
          receivedData.repoPolicy = Strings.upperFirstCharLowerRest(receivedData.repoPolicy);

          return;
        }

        var rec = this.payload;
        rec.beginEdit();
        rec.set('name', receivedData.name);
        rec.set('repoType', receivedData.repoType);
        rec.set('format', receivedData.format);
        rec.set('repoPolicy', Strings.upperFirstCharLowerRest(receivedData.repoPolicy));

        // give subclasses the opportunity to update data as well (ATM only for proxy repo URLs)
        if (this.updateRecord) {
          this.updateRecord(rec, receivedData);
        }

        rec.commit();
        rec.endEdit();

      },

      // @override
      addSorted : function(store, rec) {
        // make sure listing group first, then the repositories sorted
        var insertIndex = store.getCount();
        for (var i = 0; i < store.getCount(); i++)
        {
          var tempRec = store.getAt(i);
          if (tempRec.get('repoType') == 'group')
          {
            continue;
          }
          if (tempRec.get('name').toLowerCase() > rec.get('name').toLowerCase())
          {
            insertIndex = i;
            break;
          }
        }
        // hack the policy
        if (rec.get('repoPolicy') && rec.get('repoPolicy') == 'MIXED')
        {
          rec.beginEdit();
          rec.set('repoPolicy', '');
          rec.commit();
          rec.endEdit();
        }

        store.insert(insertIndex, [rec]);
      },
      isMavenFormat : function(format) {
        return format.indexOf('maven') == 0;
      },
      updateRepoPolicyField : function(form, format, repoPolicy) {
        var repoPolicyField = form.findField('repoPolicy');
        if (this.isMavenFormat(format))
        {
          repoPolicyField.enable();
          repoPolicyField.setValue(Strings.upperFirstCharLowerRest(repoPolicy || 'RELEASE'));
        } else {
          repoPolicyField.setValue('Mixed');
          repoPolicyField.disable();
        }
      },
      updateIndexableCombo : function(form, format) {
        var indexableCombo = form.findField('indexable');
        if (this.isMavenFormat(format))
        {
          indexableCombo.enable();
        }
        else
        {
          indexableCombo.setValue('False');
          indexableCombo.disable();
        }
      },
      updateDownloadRemoteIndexCombo : function(form, format) {
        var downloadRemoteIndexCombo = form.findField('downloadRemoteIndexes');

        if (downloadRemoteIndexCombo)
        {
          if (this.isMavenFormat(format))
          {
            downloadRemoteIndexCombo.enable();
          }
          else
          {
            downloadRemoteIndexCombo.setValue('False');
            downloadRemoteIndexCombo.disable();
          }
        }
      },
      updateFields : function(form, data) {
        this.updateIndexableCombo(form, data.format);
        this.updateRepoPolicyField(form, data.format, data.repoPolicy)
        this.updateDownloadRemoteIndexCombo(form, data.format);
        this.contribute(data, 'enter')
      },
      afterProviderSelectHandler : function(combo, rec, index) {
        this.updateFields(this.form, rec.data);
      },
      showHandler : function() {
        this.updateWritePolicy();
      },
      loadHandler : function(form, action, receivedData) {
        this.updateFields(form, receivedData)
      }
    });

Sonatype.repoServer.HostedRepositoryEditor = function(config) {
  var config = config || {};
  var defaultConfig = {
    dataModifiers : {
      load : {
        repoPolicy : Strings.upperFirstCharLowerRest,
        browseable : Strings.capitalize,
        indexable : Strings.capitalize,
        exposed : Strings.capitalize
      },
      submit : {
        repoPolicy : Strings.uppercase,
        browseable : Strings.convert.stringContextToBool,
        indexable : Strings.convert.stringContextToBool,
        exposed : Strings.convert.stringContextToBool,
        downloadRemoteIndexes : function() {
          return false;
        },
        autoBlockActive : function() {
          return false;
        },
        checksumPolicy : function() {
          return 'IGNORE';
        }
      }
    },
    referenceData : Sonatype.repoServer.referenceData.repositoryState.hosted
  };
  Ext.apply(this, config, defaultConfig);

  var ht = Sonatype.repoServer.resources.help.repos;

  this.tfStore = new Ext.data.SimpleStore({
        fields : ['value'],
        data : [['True'], ['False']]
      });

  this.policyStore = new Ext.data.SimpleStore({
        fields : ['value'],
        data : [['Release'], ['Snapshot']]
      });

  this.writePolicyStore = new Ext.data.SimpleStore({
        fields : ['value', 'display'],
        data : [['ALLOW_WRITE', 'Allow Redeploy'], ['ALLOW_WRITE_ONCE', 'Disable Redeploy'], ['READ_ONLY', 'Read Only']]
      });

  this.providerStore = new Ext.data.JsonStore({
        root : 'data',
        id : 'provider',
        fields : [{
              name : 'description',
              sortType : Ext.data.SortTypes.asUCString
            }, {
              name : 'format'
            }, {
              name : 'providerRole'
            }, {
              name : 'provider'
            }],
        sortInfo : {
          field : 'description',
          direction : 'asc'
        },
        url : Sonatype.config.repos.urls.repoTypes + '?repoType=hosted',
        autoLoad : true
      });

  this.checkPayload();

  Sonatype.repoServer.HostedRepositoryEditor.superclass.constructor.call(this, {
        dataStores : [this.providerStore],
        items : [{
              xtype : 'textfield',
              fieldLabel : 'Repository ID',
              itemCls : 'required-field',
              helpText : ht.id,
              name : 'id',
              width : 200,
              allowBlank : false,
              disabled : !this.isNew,
              validator : Strings.validateId
            }, {
              xtype : 'textfield',
              fieldLabel : 'Repository Name',
              itemCls : 'required-field',
              helpText : ht.name,
              name : 'name',
              width : 200,
              htmlDecode : true,
              allowBlank : false
            }, {
              xtype : 'textfield',
              fieldLabel : 'Repository Type',
              itemCls : 'required-field',
              helpText : ht.repoType,
              name : 'repoType',
              width : 100,
              disabled : true,
              allowBlank : false
            }, {
              xtype : 'combo',
              fieldLabel : 'Provider',
              itemCls : 'required-field',
              helpText : ht.provider,
              name : 'provider',
              width : 150,
              store : this.providerStore,
              displayField : 'description',
              valueField : 'provider',
              editable : false,
              forceSelection : true,
              mode : 'local',
              triggerAction : 'all',
              emptyText : 'Select...',
              selectOnFocus : true,
              allowBlank : false,
              disabled : !this.isNew,
              listeners : {
                select : this.providerSelectHandler,
                beforeselect : function(combo, rec, index) { this.contribute(rec.data, 'leave'); return true },
                scope : this
              }
            }, {
              xtype : 'hidden',
              name : 'providerRole'
            }, {
              xtype : 'textfield',
              fieldLabel : 'Format',
              itemCls : 'required-field',
              helpText : ht.format,
              name : 'format',
              width : 100,
              disabled : true,
              allowBlank : false
            }, {
              xtype : 'combo',
              fieldLabel : 'Repository Policy',
              itemCls : 'required-field',
              helpText : ht.repoPolicy,
              name : 'repoPolicy',
              width : 80,
              store : this.policyStore,
              displayField : 'value',
              editable : false,
              forceSelection : true,
              mode : 'local',
              triggerAction : 'all',
              emptyText : 'Select...',
              selectOnFocus : true,
              allowBlank : false,
              listeners : {
                select : this.repoPolicySelectHandler,
                scope : this
              }
            }, {
              xtype : 'textfield',
              fieldLabel : 'Default Local Storage Location',
              helpText : ht.defaultLocalStorageUrl,
              name : 'defaultLocalStorageUrl',
              anchor : Sonatype.view.FIELD_OFFSET_WITH_SCROLL,
              allowBlank : true,
              disabled : true
            }, {
              xtype : 'textfield',
              fieldLabel : 'Override Local Storage Location',
              helpText : ht.overrideLocalStorageUrl,
              name : 'overrideLocalStorageUrl',
              anchor : Sonatype.view.FIELD_OFFSET_WITH_SCROLL,
              allowBlank : true
            }, {
              xtype : 'fieldset',
              checkboxToggle : false,
              title : 'Access Settings',
              anchor : Sonatype.view.FIELDSET_OFFSET,
              collapsible : true,
              autoHeight : true,
              layoutConfig : {
                labelSeparator : ''
              },
              defaults : {
                xtype : 'combo',
                fieldLabel : 'default',
                itemCls : 'required-field',
                name : 'default',
                width : 75,
                store : this.tfStore,
                displayField : 'value',
                editable : false,
                forceSelection : true,
                mode : 'local',
                triggerAction : 'all',
                emptyText : 'Select...',
                selectOnFocus : true,
                allowBlank : false
              },
              items : [{
                    fieldLabel : 'Deployment Policy',
                    helpText : ht.writePolicy,
                    name : 'writePolicy',
                    store : this.writePolicyStore,
                    displayField : 'display',
                    valueField : 'value',
                    width : 120,
                    listWidth : 120,
                    lastQuery : ''
                  }, {
                    fieldLabel : 'Allow File Browsing',
                    helpText : ht.browseable,
                    name : 'browseable'
                  }, {
                    fieldLabel : 'Include in Search',
                    helpText : ht.indexable,
                    name : 'indexable'
                  }, {
                    fieldLabel : 'Publish URL',
                    helpText : ht.exposed,
                    name : 'exposed'
                  }]
            }, {
              xtype : 'fieldset',
              checkboxToggle : false,
              title : 'Expiration Settings',
              anchor : Sonatype.view.FIELDSET_OFFSET,
              collapsible : true,
              autoHeight : true,
              layoutConfig : {
                labelSeparator : ''
              },
              defaults : {
                xtype : 'numberfield',
                fieldLabel : 'default',
                itemCls : 'required-field',
                afterText : 'minutes',
                name : 'default',
                width : 50,
                allowBlank : false,
                allowDecimals : false,
                allowNegative : true,
                minValue : -1,
                maxValue : 511000
              },
              items : [{
                    fieldLabel : 'Not Found Cache TTL',
                    helpText : ht.notFoundCacheTTL,
                    name : 'notFoundCacheTTL'
                  }]
            }]
      });

};

Ext.extend(Sonatype.repoServer.HostedRepositoryEditor, Sonatype.repoServer.AbstractRepositoryEditor, {
      updateWritePolicy : function() {

        var repoPolicyField = this.find('name', 'repoPolicy')[0];
        var repoPolicy = repoPolicyField.getValue();

        var writePolicyField = this.find('name', 'writePolicy')[0];

        // filter out the redeploy option for SNAPSHOT repos
        if (("" + repoPolicy).toLowerCase() == 'snapshot')
        {
          // first change the value if it is ALLOW_WRITE_ONCE
          if (writePolicyField.getValue() != 'READ_ONLY')
          {
            writePolicyField.setValue('ALLOW_WRITE');
          }

          this.writePolicyStore.filterBy(function(rec, id) {
                return rec.data.value != 'ALLOW_WRITE_ONCE';
              });
        }
        else
        {
          writePolicyField.store.clearFilter();

          if (writePolicyField.getValue() == null || writePolicyField.getValue() == '')
          {
            writePolicyField.setValue('ALLOW_WRITE_ONCE');
          }
        }
      }
    });

Sonatype.repoServer.ProxyRepositoryEditor = function(config) {
  var config = config || {};
  var defaultConfig = {
    dataModifiers : {
      load : {
        repoPolicy : Strings.upperFirstCharLowerRest,
        browseable : Strings.capitalize,
        indexable : Strings.capitalize,
        exposed : Strings.capitalize,
        downloadRemoteIndexes : Strings.capitalize,
        autoBlockActive : Strings.capitalize,
        fileTypeValidation : Strings.capitalize,
        checksumPolicy : Strings.upperFirstCharLowerRest
      },
      submit : {
        repoPolicy : Strings.uppercase,
        browseable : Strings.convert.stringContextToBool,
        indexable : Strings.convert.stringContextToBool,
        exposed : Strings.convert.stringContextToBool,
        downloadRemoteIndexes : Strings.convert.stringContextToBool,
        autoBlockActive : Strings.convert.stringContextToBool,
        fileTypeValidation : Strings.convert.stringContextToBool,
        checksumPolicy : Strings.uppercase
      }
    },
    validationModifiers : {
      remoteStorageUrl : 'remoteStorage.remoteStorageUrl'
    },
    referenceData : Sonatype.repoServer.referenceData.repositoryState.proxy
  };
  Ext.apply(this, config, defaultConfig);

  var ht = Sonatype.repoServer.resources.help.repos;

  this.tfStore = new Ext.data.SimpleStore({
        fields : ['value'],
        data : [['True'], ['False']]
      });

  this.policyStore = new Ext.data.SimpleStore({
        fields : ['value'],
        data : [['Release'], ['Snapshot']]
      });

  this.checksumPolicyStore = new Ext.data.SimpleStore({
        fields : ['label', 'value'],
        data : [['Ignore', 'IGNORE'], ['Warn', 'WARN'], ['StrictIfExists', 'STRICT_IF_EXISTS'], ['Strict', 'STRICT']]
      });

  this.providerStore = new Ext.data.JsonStore({
        root : 'data',
        id : 'provider',
        fields : [{
              name : 'description',
              sortType : Ext.data.SortTypes.asUCString
            }, {
              name : 'format'
            }, {
              name : 'providerRole'
            }, {
              name : 'provider'
            }],
        sortInfo : {
          field : 'description',
          direction : 'asc'
        },
        url : Sonatype.config.repos.urls.repoTypes + '?repoType=proxy',
        autoLoad : true
      });

  this.checkPayload();

  Sonatype.repoServer.ProxyRepositoryEditor.superclass.constructor.call(this, {
        dataStores : [this.providerStore],
        items : [{
              xtype : 'textfield',
              fieldLabel : 'Repository ID',
              itemCls : 'required-field',
              helpText : ht.id,
              name : 'id',
              width : 200,
              allowBlank : false,
              disabled : !this.isNew,
              validator : Strings.validateId
            }, {
              xtype : 'textfield',
              fieldLabel : 'Repository Name',
              itemCls : 'required-field',
              helpText : ht.name,
              name : 'name',
              width : 200,
              allowBlank : false
            }, {
              xtype : 'textfield',
              fieldLabel : 'Repository Type',
              itemCls : 'required-field',
              helpText : ht.repoType,
              name : 'repoType',
              width : 100,
              disabled : true,
              allowBlank : false
            }, {
              xtype : 'combo',
              fieldLabel : 'Provider',
              itemCls : 'required-field',
              helpText : ht.provider,
              name : 'provider',
              width : 150,
              store : this.providerStore,
              displayField : 'description',
              valueField : 'provider',
              editable : false,
              forceSelection : true,
              mode : 'local',
              triggerAction : 'all',
              emptyText : 'Select...',
              selectOnFocus : true,
              allowBlank : false,
              disabled : !this.isNew,
              listeners : {
                select : this.providerSelectHandler,
                beforeselect : function(combo, rec, index) { this.contribute(rec.data, 'leave'); return true },
                scope : this
              }
            }, {
              xtype : 'hidden',
              name : 'providerRole'
            }, {
              xtype : 'textfield',
              fieldLabel : 'Format',
              itemCls : 'required-field',
              helpText : ht.format,
              name : 'format',
              width : 100,
              disabled : true,
              allowBlank : false
            }, {
              xtype : 'combo',
              fieldLabel : 'Repository Policy',
              itemCls : 'required-field',
              helpText : ht.repoPolicy,
              name : 'repoPolicy',
              width : 80,
              store : this.policyStore,
              displayField : 'value',
              editable : false,
              forceSelection : true,
              mode : 'local',
              triggerAction : 'all',
              emptyText : 'Select...',
              selectOnFocus : true,
              allowBlank : false,
              listeners : {
                select : this.repoPolicySelectHandler,
                scope : this
              }
            }, {
              xtype : 'textfield',
              fieldLabel : 'Default Local Storage Location',
              helpText : ht.defaultLocalStorageUrl,
              name : 'defaultLocalStorageUrl',
              anchor : Sonatype.view.FIELD_OFFSET_WITH_SCROLL,
              allowBlank : true,
              disabled : true
            }, {
              xtype : 'textfield',
              fieldLabel : 'Override Local Storage Location',
              helpText : ht.overrideLocalStorageUrl,
              name : 'overrideLocalStorageUrl',
              anchor : Sonatype.view.FIELD_OFFSET_WITH_SCROLL,
              allowBlank : true
            }, {
              xtype : 'fieldset',
              checkboxToggle : false,
              title : 'Remote Repository Access',
              anchor : Sonatype.view.FIELDSET_OFFSET_WITH_SCROLL,
              collapsible : true,
              autoHeight : true,
              layoutConfig : {
                labelSeparator : ''
              },
              items : [{
                    xtype : 'textfield',
                    fieldLabel : 'Remote Storage Location',
                    itemCls : 'required-field',
                    helpText : ht.remoteStorageUrl,
                    emptyText : 'http://some-remote-repository/repo-root',
                    name : 'remoteStorage.remoteStorageUrl',
                    anchor : Sonatype.view.FIELD_OFFSET,
                    allowBlank : false,
                    validator : function(v) {
                      if (v.match(REPO_REMOTE_STORAGE_REGEXP))
                      {
                        return true;
                      }
                      else
                      {
                        return 'Protocol must be http://, https:// or ftp://';
                      }
                    }
                  }, {
                    xtype : 'combo',
                    fieldLabel : 'Download Remote Indexes',
                    helpText : ht.downloadRemoteIndexes,
                    name : 'downloadRemoteIndexes',
                    itemCls : 'required-field',
                    width : 75,
                    store : this.tfStore,
                    displayField : 'value',
                    editable : false,
                    forceSelection : true,
                    mode : 'local',
                    triggerAction : 'all',
                    emptyText : 'Select...',
                    selectOnFocus : true,
                    allowBlank : false
                  }, {}, {
                    xtype : 'combo',
                    fieldLabel : 'Auto Blocking Enabled',
                    helpText : ht.autoBlockActive,
                    name : 'autoBlockActive',
                    itemCls : 'required-field',
                    width : 75,
                    store : this.tfStore,
                    displayField : 'value',
                    editable : false,
                    forceSelection : true,
                    mode : 'local',
                    triggerAction : 'all',
                    emptyText : 'Select...',
                    selectOnFocus : true,
                    allowBlank : false
                  }, {
                    xtype : 'combo',
                    fieldLabel : 'File Content Validation',
                    helpText : ht.fileTypeValidation,
                    name : 'fileTypeValidation',
                    itemCls : 'required-field',
                    width : 75,
                    store : this.tfStore,
                    displayField : 'value',
                    editable : false,
                    forceSelection : true,
                    mode : 'local',
                    triggerAction : 'all',
                    emptyText : 'Select...',
                    selectOnFocus : true,
                    allowBlank : false
                  }, {
                    xtype : 'combo',
                    fieldLabel : 'Checksum Policy',
                    itemCls : 'required-field',
                    helpText : ht.checksumPolicy,
                    name : 'checksumPolicy',
                    width : 95,
                    store : this.checksumPolicyStore,
                    displayField : 'label',
                    valueField : 'value',
                    editable : false,
                    forceSelection : true,
                    mode : 'local',
                    triggerAction : 'all',
                    emptyText : 'Select...',
                    selectOnFocus : true,
                    allowBlank : false
                  }, {
                    xtype : 'fieldset',
                    checkboxToggle : true,
                    title : 'Authentication (optional)',
                    name : 'fieldset_remoteStorage.authentication',
                    collapsed : true,
                    autoHeight : true,
                    layoutConfig : {
                      labelSeparator : ''
                    },
                    items : [{
                          xtype : 'textfield',
                          fieldLabel : 'Username',
                          helpText : ht.remoteUsername,
                          name : 'remoteStorage.authentication.username',
                          width : 100,
                          allowBlank : true
                        }, {
                          xtype : 'textfield',
                          fieldLabel : 'Password',
                          helpText : ht.remotePassword,
                          inputType : 'password',
                          name : 'remoteStorage.authentication.password',
                          width : 100,
                          allowBlank : true
                        }, {
                          xtype : 'textfield',
                          fieldLabel : 'NT LAN Host',
                          helpText : ht.remoteNtlmHost,
                          name : 'remoteStorage.authentication.ntlmHost',
                          anchor : Sonatype.view.FIELD_OFFSET,
                          allowBlank : true
                        }, {
                          xtype : 'textfield',
                          fieldLabel : 'NT LAN Manager Domain',
                          helpText : ht.remoteNtlmDomain,
                          name : 'remoteStorage.authentication.ntlmDomain',
                          anchor : Sonatype.view.FIELD_OFFSET,
                          allowBlank : true
                        }]
                  }]
            }, // end remote storage)
            {
              xtype : 'fieldset',
              checkboxToggle : false,
              title : 'Access Settings',
              anchor : Sonatype.view.FIELDSET_OFFSET_WITH_SCROLL,
              collapsible : true,
              autoHeight : true,
              layoutConfig : {
                labelSeparator : ''
              },
              defaults : {
                xtype : 'combo',
                fieldLabel : 'default',
                itemCls : 'required-field',
                name : 'default',
                width : 75,
                store : this.tfStore,
                displayField : 'value',
                editable : false,
                forceSelection : true,
                mode : 'local',
                triggerAction : 'all',
                emptyText : 'Select...',
                selectOnFocus : true,
                allowBlank : false
              },

              items : [{
                    fieldLabel : 'Allow File Browsing',
                    helpText : ht.browseable,
                    name : 'browseable'
                  }, {
                    fieldLabel : 'Include in Search',
                    helpText : ht.indexable,
                    name : 'indexable'
                  }, {
                    fieldLabel : 'Publish URL',
                    helpText : ht.exposed,
                    name : 'exposed'
                  }]
            }, {
              xtype : 'fieldset',
              checkboxToggle : false,
              title : 'Expiration Settings',
              anchor : Sonatype.view.FIELDSET_OFFSET_WITH_SCROLL,
              collapsible : true,
              autoHeight : true,
              layoutConfig : {
                labelSeparator : ''
              },
              defaults : {
                xtype : 'numberfield',
                fieldLabel : 'default',
                itemCls : 'required-field',
                afterText : 'minutes',
                name : 'default',
                width : 50,
                allowBlank : false,
                allowDecimals : false,
                allowNegative : true,
                minValue : -1,
                maxValue : 511000
              },

              items : [{
                    fieldLabel : 'Not Found Cache TTL',
                    helpText : ht.notFoundCacheTTL,
                    name : 'notFoundCacheTTL'
                  }, {
                    fieldLabel : 'Artifact Max Age',
                    helpText : ht.artifactMaxAge,
                    name : 'artifactMaxAge'
                  }, {
                    fieldLabel : 'Metadata Max Age',
                    helpText : ht.metadataMaxAge,
                    name : 'metadataMaxAge'
                  }, {
                    fieldLabel : 'Item Max Age',
                    helpText : ht.itemMaxAge,
                    name : 'itemMaxAge'
                  }]
            },

            {
              xtype : 'fieldset',
              checkboxToggle : true,
              title : 'HTTP Request Settings (optional)',
              anchor : Sonatype.view.FIELDSET_OFFSET_WITH_SCROLL,
              name : 'fieldset_remoteStorage.connectionSettings',
              collapsed : true,
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
              items : [{
                    xtype : 'textfield',
                    fieldLabel : 'User Agent',
                    helpText : ht.userAgentString,
                    name : 'remoteStorage.connectionSettings.userAgentString',
                    anchor : Sonatype.view.FIELD_OFFSET,
                    allowBlank : true
                  }, {
                    xtype : 'textfield',
                    fieldLabel : 'Additional URL Parameters',
                    helpText : ht.queryString,
                    name : 'remoteStorage.connectionSettings.queryString',
                    anchor : Sonatype.view.FIELD_OFFSET,
                    allowBlank : true
                  }, {
                    xtype : 'numberfield',
                    fieldLabel : 'Request Timeout',
                    itemCls : 'required-field',
                    helpText : ht.connectionTimeout,
                    afterText : 'seconds',
                    name : 'remoteStorage.connectionSettings.connectionTimeout',
                    width : 75,
                    allowBlank : true,
                    allowDecimals : false,
                    allowNegative : false,
                    maxValue : 36000
                  }, {
                    xtype : 'numberfield',
                    fieldLabel : 'Request Retry Attempts',
                    itemCls : 'required-field',
                    helpText : ht.retrievalRetryCount,
                    name : 'remoteStorage.connectionSettings.retrievalRetryCount',
                    width : 50,
                    allowBlank : true,
                    allowDecimals : false,
                    allowNegative : false,
                    maxValue : 10
                  }]
            } // end http conn
        ]
      });
};

Ext.extend(Sonatype.repoServer.ProxyRepositoryEditor, Sonatype.repoServer.AbstractRepositoryEditor, {
      loadHandler : function(form, action, receivedData) {
        Sonatype.repoServer.AbstractRepositoryEditor.prototype.loadHandler.apply(this, arguments);

        var repoType = receivedData.repoType;

        if (repoType == 'proxy' && receivedData.remoteStorage && !receivedData.remoteStorage.remoteStorageUrl.match(REPO_REMOTE_STORAGE_REGEXP))
        {
          var rsUrl = this.form.findField('remoteStorage.remoteStorageUrl');
          rsUrl.disable();
          rsUrl.clearInvalid();

          // Disable the editor - this is a temporary measure,
          // until we find a better solution for procurement repos
          this.buttons[0].disable();
        }
      },

      updateRecord : function(rec, receivedData) {
        rec.set('remoteUri', receivedData.remoteStorage.remoteStorageUrl);
      }

    });

Sonatype.repoServer.VirtualRepositoryEditor = function(config) {
  var config = config || {};
  var defaultConfig = {
    dataModifiers : {
      load : {
        syncAtStartup : Strings.capitalize
      },
      submit : {
        syncAtStartup : Strings.convert.stringContextToBool,
        exposed : function() {
          return true;
        }
      }
    },
    referenceData : Sonatype.repoServer.referenceData.repositoryState.virtual
  };
  Ext.apply(this, config, defaultConfig);

  var ht = Sonatype.repoServer.resources.help.repos;

  this.COMBO_WIDTH = 300;

  this.tfStore = new Ext.data.SimpleStore({
        fields : ['value'],
        data : [['True'], ['False']]
      });

  this.repoStore = new Ext.data.JsonStore({
        root : 'data',
        id : 'id',
        fields : [{
              name : 'id'
            }, {
              name : 'name',
              sortType : Ext.data.SortTypes.asUCString
            }, {
              name : 'format'
            }, {
              name : 'repoType'
            }],
        sortInfo : {
          field : 'name',
          direction : 'asc'
        },
        url : Sonatype.config.repos.urls.repositories,
        autoLoad : true
      });

  this.providerStore = new Ext.data.JsonStore({
        root : 'data',
        id : 'provider',
        fields : [{
              name : 'description',
              sortType : Ext.data.SortTypes.asUCString
            }, {
              name : 'format'
            }, {
              name : 'providerRole'
            }, {
              name : 'provider'
            }],
        sortInfo : {
          field : 'description',
          direction : 'asc'
        },
        url : Sonatype.config.repos.urls.repoTypes + '?repoType=shadow',
        autoLoad : true
      });

  this.checkPayload();

  Sonatype.repoServer.VirtualRepositoryEditor.superclass.constructor.call(this, {
        dataStores : [this.providerStore, this.repoStore],
        items : [{
              xtype : 'textfield',
              fieldLabel : 'Repository ID',
              itemCls : 'required-field',
              helpText : ht.id,
              name : 'id',
              width : 200,
              allowBlank : false,
              disabled : !this.isNew,
              validator : Strings.validateId
            }, {
              xtype : 'textfield',
              fieldLabel : 'Repository Name',
              itemCls : 'required-field',
              helpText : ht.name,
              name : 'name',
              width : 200,
              allowBlank : false
            }, {
              xtype : 'textfield',
              fieldLabel : 'Repository Type',
              itemCls : 'required-field',
              helpText : ht.repoType,
              name : 'repoType',
              width : 100,
              disabled : true,
              allowBlank : false
            }, {
              xtype : 'combo',
              fieldLabel : 'Provider',
              itemCls : 'required-field',
              helpText : ht.provider,
              name : 'provider',
              width : 150,
              store : this.providerStore,
              displayField : 'description',
              valueField : 'provider',
              editable : false,
              forceSelection : true,
              mode : 'local',
              triggerAction : 'all',
              emptyText : 'Select...',
              selectOnFocus : true,
              allowBlank : false,
              disabled : !this.isNew,
              listeners : {
                select : this.providerSelectHandler,
                beforeselect : function(combo, rec, index) { this.contribute(rec.data, 'leave'); return true },
                scope : this
              }
            }, {
              xtype : 'hidden',
              name : 'providerRole'
            }, {
              xtype : 'textfield',
              fieldLabel : 'Format',
              itemCls : 'required-field',
              helpText : ht.format,
              name : 'format',
              width : 100,
              disabled : true,
              allowBlank : false
            }, {
              xtype : 'combo',
              fieldLabel : 'Source Nexus Repository ID',
              itemCls : 'required-field',
              helpText : ht.shadowOf,
              name : 'shadowOf',
              width : 200,
              midWidth : 200,
              store : this.repoStore,
              displayField : 'name',
              valueField : 'id',
              editable : false,
              forceSelection : true,
              mode : 'local',
              triggerAction : 'all',
              emptyText : 'Select...',
              selectOnFocus : true,
              allowBlank : false,
              // this config can solve the problem of 'filter is not applied the
              // first time'
              lastQuery : ''
            }, {
              xtype : 'combo',
              fieldLabel : 'Synchronize on Startup',
              itemCls : 'required-field',
              helpText : ht.syncAtStartup,
              name : 'syncAtStartup',
              width : 75,
              store : this.tfStore,
              displayField : 'value',
              editable : false,
              forceSelection : true,
              mode : 'local',
              triggerAction : 'all',
              emptyText : 'Select...',
              selectOnFocus : true,
              allowBlank : false
            }]
      });
};

Ext.extend(Sonatype.repoServer.VirtualRepositoryEditor, Sonatype.repoServer.AbstractRepositoryEditor, {

      templateLoadSuccess : function(form, action) {
        var rec = {
          data : {
            provider : this.find('name', 'provider')[0].getValue()
          }
        };

        this.afterProviderSelectHandler(null, rec, null);
      },
      afterProviderSelectHandler : function(combo, rec, index) {
        var provider = rec.data.provider;
        var sourceRepoCombo = this.form.findField('shadowOf');
        sourceRepoCombo.clearValue();
        sourceRepoCombo.clearInvalid();
        //sourceRepoCombo.focus();
        if (provider == 'm1-m2-shadow')
        {
          sourceRepoCombo.store.filterBy(function fn(rec, id) {
                if (rec.data.repoType != 'virtual' && rec.data.format == 'maven1')
                {
                  return true;
                }
                return false;
              });
        }
        else if (provider == 'm2-m1-shadow')
        {
          sourceRepoCombo.store.filterBy(function fn(rec, id) {
                if (rec.data.repoType != 'virtual' && rec.data.format == 'maven2')
                {
                  return true;
                }
                return false;
              });
        }
      },
      loadHandler : function(form, action, receivedData) {}
    });

Sonatype.Events.addListener('repositoryViewInit', function(cardPanel, rec) {
  var sp = Sonatype.lib.Permissions;

  var repoEditors = {
    hosted : Sonatype.repoServer.HostedRepositoryEditor,
    proxy : Sonatype.repoServer.ProxyRepositoryEditor,
    virtual : Sonatype.repoServer.VirtualRepositoryEditor
  };

  var editor = repoEditors[rec.data.repoType];

  if (editor && rec.data.userManaged && sp.checkPermission('nexus:repositories', sp.READ) && (sp.checkPermission('nexus:repositories', sp.CREATE) || sp.checkPermission('nexus:repositories', sp.DELETE) || sp.checkPermission('nexus:repositories', sp.EDIT)))
  {
    cardPanel.add(new editor({
          tabTitle : 'Configuration',
          name : 'configuration',
          payload : rec
        }));
  }
});

Sonatype.Events.addListener('repositoryAddMenuInit', function(menu) {
      var sp = Sonatype.lib.Permissions;

      if (sp.checkPermission('nexus:repositories', sp.CREATE))
      {
        var createRepoFunc = function(container, rec, item, e) {
          rec.beginEdit();
          rec.set('repoType', item.value);
          rec.set('exposed', true);
          rec.set('userManaged', true);
          rec.commit();
          rec.endEdit();
        };

        menu.add(['-', {
              text : 'Hosted Repository',
              value : 'hosted',
              autoCreateNewRecord : true,
              handler : createRepoFunc
            }, {
              text : 'Proxy Repository',
              value : 'proxy',
              autoCreateNewRecord : true,
              handler : createRepoFunc
            }, {
              text : 'Virtual Repository',
              value : 'virtual',
              autoCreateNewRecord : true,
              handler : createRepoFunc
            }]);
      }
    });

});

