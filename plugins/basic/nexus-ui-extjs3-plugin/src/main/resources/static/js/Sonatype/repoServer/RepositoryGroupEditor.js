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
/*global NX, Sonatype, Nexus, Ext*/
/*
 * Repository Groups Edit/Create panel layout and controller
 */
NX.define('Sonatype.repoServer.RepositoryGroupEditor', {
        extend : 'Nexus.ext.FormPanel',

        requirejs : ['Sonatype/all'],

        constructor : function(config) {
          var
                ht = Sonatype.repoServer.resources.help.groups,
                defaultConfig = {
                  dataModifiers : {
                    load : {
                      repositories : this.loadRepositories.createDelegate(this),
                      exposed : Nexus.util.Strings.capitalize
                    },
                    submit : {
                      repositories : this.saveRepositories.createDelegate(this),
                      exposed : Nexus.util.Strings.convert.stringContextToBool
                    }
                  },
                  validationModifiers : {
                    repositories : this.repositoriesValidationError.createDelegate(this)
                  },
                  referenceData : Sonatype.repoServer.referenceData.group,
                  uri : Sonatype.config.repos.urls.groups
                };
          Ext.apply(this, config || {}, defaultConfig);

          this.tfStore = new Ext.data.SimpleStore({
            fields : ['value'],
            data : [
              ['True'],
              ['False']
            ]
          });

          this.providerStore = new Ext.data.JsonStore({
            root : 'data',
            id : 'provider',
            fields : [
              {
                name : 'description',
                sortType : Ext.data.SortTypes.asUCString
              },
              {
                name : 'format'
              },
              {
                name : 'provider'
              }
            ],
            sortInfo : {
              field : 'description',
              direction : 'asc'
            },
            url : Sonatype.config.repos.urls.repoTypes + '?repoType=group'
          });

          this.contentClassStore = new Ext.data.JsonStore({
            root : 'data',
            id : 'contentClass',
            fields : [
              { name : 'contentClass' },
              { name : 'name' },
              { name : 'groupable' },
              { name : 'compatibleTypes' }
            ],
            url : Sonatype.config.repos.urls.repoContentClasses
          });

          this.repoStore = new Ext.data.JsonStore({
            root : 'data',
            id : 'id',
            fields : [
              {
                name : 'id'
              },
              {
                name : 'resourceURI'
              },
              {
                name : 'format'
              },
              {
                name : 'name',
                sortType : Ext.data.SortTypes.asUCString
              }
            ],
            sortInfo : {
              field : 'name',
              direction : 'asc'
            },
            url : Sonatype.config.repos.urls.allRepositories
          });

          this.checkPayload();

          Sonatype.repoServer.RepositoryGroupEditor.superclass.constructor.call(this, {
            dataStores : [this.providerStore, this.repoStore, this.contentClassStore],
            items : [
              {
                xtype : 'textfield',
                fieldLabel : 'Group ID',
                itemCls : 'required-field',
                helpText : ht.id,
                name : 'id',
                width : 200,
                allowBlank : false,
                disabled : !this.isNew,
                validator : Nexus.util.Strings.validateId
              },
              {
                xtype : 'textfield',
                fieldLabel : 'Group Name',
                itemCls : 'required-field',
                helpText : ht.name,
                name : 'name',
                width : 200,
                allowBlank : false
              },
              {
                xtype : 'combo',
                fieldLabel : 'Provider',
                itemCls : 'required-field',
                helpText : ht.provider,
                name : 'provider',
                width : this.COMBO_WIDTH,
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
                  select : {
                    fn : this.providerSelectHandler,
                    scope : this
                  }
                }
              },
              {
                xtype : 'textfield',
                fieldLabel : 'Format',
                itemCls : 'required-field',
                helpText : ht.format,
                name : 'format',
                width : 100,
                disabled : true,
                allowBlank : false
              },
              {
                xtype : 'combo',
                fieldLabel : 'Publish URL',
                itemCls : 'required-field',
                helpText : ht.exposed,
                name : 'exposed',
                width : 75,
                store : this.tfStore,
                displayField : 'value',
                editable : false,
                forceSelection : true,
                mode : 'local',
                triggerAction : 'all',
                emptyText : 'Select...',
                selectOnFocus : true,
                allowBlank : false,
                value : 'True'
              },
              {
                xtype : 'twinpanelchooser',
                titleLeft : 'Ordered Group Repositories',
                titleRight : 'Available Repositories',
                name : 'repositories',
                valueField : 'id',
                store : this.repoStore,
                validateLeftItems : true,
                validateLeftItemsText : 'Invalid Repository Found'
              }
            ],
            listeners : {
              submit : this.submitHandler,
              scope : this
            }
          });
        },

        isValid : function() {
          if (!this.form.isValid()) {
            return false;
          }
          var repoBox = this.find('name', 'repositories')[0];
          if (!repoBox.validate()) {
            return false;
          }
          return true;
        },

        loadRepositories : function(arr, srcObject, fpanel) {
          var i, repoBox = fpanel.find('name', 'repositories')[0];
          this.repoStore.filterBy(function(rec, id) {
            var compatibleClasses, contentClass = this.contentClassStore.getById(rec.data.format);

            // if we have the content class
            // and the content class is compatible with the repo type
            if (contentClass && rec.data.id !== srcObject.id) {
              compatibleClasses = contentClass.get('compatibleTypes');

              for (i = 0; i < compatibleClasses.length; i += 1) {
                if (compatibleClasses[i] === srcObject.format) {
                  return true;
                }
              }
            }
            return false;
          }, this);

          repoBox.setValue(arr);
        },

        providerSelectHandler : function(combo, rec, index) {
          combo.ownerCt.find('name', 'format')[0].setValue(rec.data.format);
          this.loadRepositories([], rec.data, this);
        },

        saveRepositories : function(value, fpanel) {
          var
                i, response = [],
                repoBox = fpanel.find('name', 'repositories')[0],
                repoIds = repoBox.getValue();

          for (i = 0; i < repoIds.length; i += 1) {
            response.push({
              id : repoIds[i]
            });
          }

          return response;
        },

        submitHandler : function(form, action, receivedData) {
          if (this.isNew) {
            if (!receivedData.resourceURI) {
              receivedData.displayStatus = Sonatype.utils.joinArrayObject(action.output.data.repositories, 'name');
              receivedData.resourceURI =
                    Sonatype.config.host + Sonatype.config.repos.urls.groups + '/' + receivedData.id;
            }
            receivedData.userManaged = true;
            return;
          }

          var rec = this.payload;
          rec.beginEdit();
          rec.set('name', action.output.data.name);
          rec.set('format', action.output.data.format);
          rec.set('exposed', action.output.data.exposed);
          rec.set('displayStatus', Sonatype.utils.joinArrayObject(action.output.data.repositories, 'name'));
          rec.commit();
          rec.endEdit();
        },

        // @override
        addSorted : function(store, rec) {
          var i, insertIndex, tempRec;
          for (i = 0; i < store.getCount(); i += 1) {
            tempRec = store.getAt(i);
            if (tempRec.get('repoType') !== 'group') {
              insertIndex = i;
              break;
            }
            if (tempRec.get('name').toLowerCase() > rec.get('name').toLowerCase()) {
              insertIndex = i;
              break;
            }
          }
          store.insert(insertIndex, [rec]);
        },
        repositoriesValidationError : function(error, fpanel) {
          var repos = fpanel.find('name', 'repositories')[0];
          repos.markTreeInvalid(null, error.msg);
        }
      },
      function() {

        Sonatype.Events.addListener('repositoryViewInit', function(cardPanel, rec) {
          var sp = Sonatype.lib.Permissions, editor;

          if (rec.data.repoType === 'group' && rec.data.userManaged && sp.checkPermission('nexus:repogroups', sp.READ)
                && (sp.checkPermission('nexus:repogroups', sp.CREATE) || sp.checkPermission('nexus:repogroups',
                sp.EDIT))) {

            editor = new Sonatype.repoServer.RepositoryGroupEditor({
              tabTitle : 'Configuration',
              name : 'configuration',
              payload : rec
            });

            cardPanel.add(editor);

            if (rec.data.resourceURI) {
              // I have to say this is a poor solution, get the resource once, if
              // ok, show the tab and get again.
              // But I cannot think out a better, simpler solution.
              Ext.Ajax.request({
                url : Sonatype.config.repos.urls.groups + '/' + rec.get('id'),
                method : 'GET',
                scope : this,
                callback : function(options, isSuccess, response) {
                  if (!isSuccess) {
                    cardPanel.remove(editor);
                  }
                }
              });
            }
          }
        });

        Sonatype.Events.addListener('repositoryAddMenuInit', function(menu) {
          var sp = Sonatype.lib.Permissions;

          if (sp.checkPermission('nexus:repogroups', sp.CREATE)) {
            menu.add('-');
            menu.add({
              text : 'Repository Group',
              autoCreateNewRecord : true,
              handler : function(container, rec, item, e) {
                rec.beginEdit();
                rec.set('repoType', 'group');
                rec.set('exposed', true);
                rec.set('userManaged', true);
                rec.commit();
                rec.endEdit();
              },
              scope : this
            });
          }
        });

      });