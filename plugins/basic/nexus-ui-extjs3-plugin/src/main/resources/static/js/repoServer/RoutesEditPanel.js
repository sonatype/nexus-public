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
/*
 * Repository Routes Edit/Creat panel layout and controller
 */

define('repoServer/RoutesEditPanel',['Sonatype/all', 'Sonatype/strings','Nexus/ext/GridFilterBox'], function(Sonatype, Strings){
Sonatype.repoServer.RoutesEditPanel = function(config) {
  var config = config || {},
      defaultConfig = {};

  Ext.apply(this, config, defaultConfig);

  this.repoDataStore = new Ext.data.JsonStore({
        root : 'data',
        id : 'id',
        url : Sonatype.config.repos.urls.allRepositories,
        sortInfo : {
          field : 'name',
          direction : 'ASC'
        },
        fields : [{
              name : 'id'
            }, {
              name : 'name',
              sortType : Ext.data.SortTypes.asUCString
            }],
        autoLoad : true
      });

  var tfStore = new Ext.data.SimpleStore({
        fields : ['value'],
        data : [['True'], ['False']]
      });
  this.ruleTypeStore = new Ext.data.SimpleStore({
        fields : ['value'],
        data : [['Blocking'], ['Exclusive'], ['Inclusive']]
      });
  this.BLOCKING_TYPE_INDEX = 0;
  this.BLOCKING = 'Blocking';

  // A record to hold the name and id of a repository group
  this.repositoryGroupRecordConstructor = Ext.data.Record.create([{
        name : 'id'
      }, {
        name : 'name',
        sortType : Ext.data.SortTypes.asUCString
      }]);

  // Reader and datastore that queries the server for the list of repository
  // groups
  this.repositoryGroupReader = new Ext.data.JsonReader({
        root : 'data',
        id : 'id'
      }, this.repositoryGroupRecordConstructor);
  this.repositoryGroupDataStore = new Ext.data.Store({
        url : Sonatype.config.repos.urls.groups,
        reader : this.repositoryGroupReader,
        sortInfo : {
          field : 'name',
          direction : 'ASC'
        },
        autoLoad : true,
        listeners : {
          'load' : {
            fn : function() {
              var allRec = new this.repositoryGroupRecordConstructor({
                    id : '*',
                    name : 'All Repository Groups'
                  }, '*');
              this.repositoryGroupDataStore.insert(0, allRec);
            },
            scope : this
          }
        }
      });

  var ht = Sonatype.repoServer.resources.help.routes;

  this.loadDataModFuncs = {
    route : {
      ruleType : Strings.capitalize,
      repositories : this.loadRepoListHelper.createDelegate(this)
    }
  };

  this.submitDataModFuncs = {
    route : {
      ruleType : Strings.lowercase,
      repositories : this.exportRepoListHelper.createDelegate(this)
    }
  };

  this.formConfig = {};
  this.formConfig.route = {
    region : 'center',
    width : '100%',
    height : '100%',
    autoScroll : true,
    border : false,
    frame : true,
    collapsible : false,
    collapsed : false,
    labelWidth : 175,
    layoutConfig : {
      labelSeparator : ''
    },

    items : [{
          xtype : 'hidden',
          name : 'id'
        }, {
          xtype : 'textfield',
          fieldLabel : 'URL Pattern',
          itemCls : 'required-field',
          helpText : ht.pattern,
          name : 'pattern',
          width : 300,
          allowBlank : false
        }, {
          xtype : 'combo',
          fieldLabel : 'Rule Type',
          itemCls : 'required-field',
          helpText : ht.ruleType,
          name : 'ruleType',
          // hiddenName: 'connectionTimeout',
          width : 100,
          store : this.ruleTypeStore,
          displayField : 'value',
          editable : false,
          forceSelection : true,
          mode : 'local',
          triggerAction : 'all',
          emptyText : 'Select...',
          selectOnFocus : true,
          allowBlank : false,
          listeners : {
            'select' : {
              fn : function(combo, record, index) {
                this.updateTreePanel(index == this.BLOCKING_TYPE_INDEX, combo.findParentByType('form').id);
              },
              scope : this
            }
          }
        }, {
          xtype : 'combo',
          fieldLabel : 'Repository Group',
          itemCls : 'required-field',
          helpText : ht.group,
          name : 'groupId',
          width : 200,
          minWidth : 200,
          store : this.repositoryGroupDataStore,
          displayField : 'name',
          valueField : 'id',
          editable : false,
          forceSelection : true,
          mode : 'local',
          triggerAction : 'all',
          emptyText : 'Select...',
          selectOnFocus : true,
          allowBlank : false
        }, {
          xtype : 'twinpanelchooser',
          titleLeft : 'Ordered Route Repositories',
          titleRight : 'Available Repositories',
          name : 'repositories',
          valueField : 'id',
          store : this.repoDataStore,
          required : true
        }],
    buttons : [{
          text : 'Save',
          disabled : true
        }, {
          text : 'Cancel'
        }]
  };

  this.nameConcatinator = function(val, parent) {
    return Sonatype.utils.joinArrayObject(val, 'name');
  };

  this.nameFinder = function(val, parent) {
    var repos = [];
    for (var i = 0; i < val.length; i++)
    {
      repos[i] = this.repoDataStore.getById(val[i].id).data;
    }

    return Sonatype.utils.joinArrayObject(repos, 'name');
  };

  // START: Repo list ******************************************************
  this.routeRecordConstructor = Ext.data.Record.create([{
        name : 'resourceURI'
      }, {
        name : 'pattern',
        sortType : Ext.data.SortTypes.asUCString
      }, {
        name : 'ruleType'
      }, {
        name : 'groupId'
      }, {
        name : 'repositories'
      }, {
        name : 'sRepositories',
        mapping : 'repositories',
        convert : this.nameConcatinator
      }]);

  this.routesReader = new Ext.data.JsonReader({
        root : 'data',
        id : 'resourceURI'
      }, this.routeRecordConstructor);

  // @ext: must use data.Store (not JsonStore) to pass in reader instead of
  // using fields config array
  this.routesDataStore = new Ext.data.Store({
        url : Sonatype.config.repos.urls.routes,
        reader : this.routesReader,
        sortInfo : {
          field : 'pattern',
          direction : 'ASC'
        },
        autoLoad : true
      });

  this.sp = Sonatype.lib.Permissions;

  this.routesGridPanel = new Ext.grid.GridPanel({
        id : 'st-routes-grid',
        region : 'north',
        layout : 'fit',
        split : true,
        height : 200,
        minHeight : 150,
        maxHeight : 400,
        frame : false,
        autoScroll : true,
        tbar : [{
              id : 'route-refresh-btn',
              text : 'Refresh',
              iconCls : 'st-icon-refresh',
              cls : 'x-btn-text-icon',
              scope : this,
              handler : this.reloadAll
            }, {
              id : 'route-add-btn',
              text : 'Add',
              icon : Sonatype.config.resourcePath + '/static/images/icons/add.png',
              cls : 'x-btn-text-icon',
              scope : this,
              handler : this.addResourceHandler,
              disabled : !this.sp.checkPermission('nexus:routes', this.sp.CREATE)
            }, {
              id : 'route-delete-btn',
              text : 'Delete',
              icon : Sonatype.config.resourcePath + '/static/images/icons/delete.png',
              cls : 'x-btn-text-icon',
              scope : this,
              handler : this.deleteResourceHandler,
              disabled : !this.sp.checkPermission('nexus:routes', this.sp.DELETE)
            }
        ],

        // grid view options
        ds : this.routesDataStore,
        sortInfo : {
          field : 'name',
          direction : "ASC"
        },
        loadMask : true,
        deferredRender : false,
        columns : [{
              header : 'Route',
              dataIndex : 'pattern',
              width : 200
            }, {
              header : 'Rule Type',
              dataIndex : 'ruleType',
              width : 100
            }, {
              header : 'Group',
              dataIndex : 'groupId',
              width : 100
            }, {
              header : 'Repositories',
              dataIndex : 'sRepositories',
              id : 'routes-config-repos-col',
              width : 300
            }],
        autoExpandColumn : 'routes-config-repos-col',
        disableSelection : false,
        viewConfig : {
          deferEmptyText: false,
          emptyText: 'No routes defined',
          emptyTextWhileFiltering: 'No routes matched criteria: {criteria}'
        }
      });
  this.routesGridPanel.getSelectionModel().on('rowselect', this.rowSelect, this);

  this.routesGridPanel.getTopToolbar().add([
    '->',
    NX.create('Nexus.ext.GridFilterBox', {
      filteredGrid: this.routesGridPanel
    })
  ]);

  // END: Repo List ******************************************************
  // *********************************************************************

  Sonatype.repoServer.RoutesEditPanel.superclass.constructor.call(this, {
        layout : 'border',
        autoScroll : false,
        width : '100%',
        height : '100%',
        items : [this.routesGridPanel, {
              xtype : 'panel',
              id : 'route-config-forms',
              title : 'Repository Route Configuration',
              layout : 'card',
              region : 'center',
              activeItem : 0,
              deferredRender : false,
              autoScroll : false,
              frame : false,
              items : [{
                    xtype : 'panel',
                    layout : 'fit',
                    id : 'route-no-form',
                    html : '<div class="little-padding">Select a Repository Route to edit it, or click "Add" to create a new one.</div>'
                  }]
            }]
      });

  this.formCards = this.findById('route-config-forms');
};

Ext.extend(Sonatype.repoServer.RoutesEditPanel, Ext.Panel, {
      reloadAll : function() {
        this.repoDataStore.removeAll();
        this.repoDataStore.reload();
        this.routesDataStore.removeAll();
        this.routesDataStore.reload();
        this.formCards.items.each(function(item, i, len) {
              if (i > 0)
              {
                this.remove(item, true);
              }
            }, this.formCards);

        this.formCards.getLayout().setActiveItem(0);
      },

      // formInfoObj : {formPanel, isNew, [resourceURI]}
      saveHandler : function(formInfoObj) {
        var allValid = false;
        allValid = formInfoObj.formPanel.form.isValid() && formInfoObj.formPanel.find('name', 'repositories')[0].validate();

        if (allValid)
        {
          var isNew = formInfoObj.isNew;
          var uri = (isNew) ? Sonatype.config.repos.urls.routes : formInfoObj.resourceURI;
          var form = formInfoObj.formPanel.form;

          form.doAction('sonatypeSubmit', {
            method : isNew ? 'POST' : 'PUT',
            url : uri,
            waitMsg : isNew ? 'Creating repository route...' : 'Updating repository route configuration...',
            fpanel : formInfoObj.formPanel,
            dataModifiers : this.submitDataModFuncs.route,
            serviceDataObj : Sonatype.repoServer.referenceData.route,
            isNew : isNew
              // extra option to send to callback, instead of conditioning on
              // HTTP method
            });
        }
      },

      // formInfoObj : {formPanel, isNew, [resourceURI]}
      cancelHandler : function(formInfoObj) {
        var formLayout = this.formCards.getLayout();
        var gridSelectModel = this.routesGridPanel.getSelectionModel();
        var store = this.routesGridPanel.getStore();

        this.formCards.remove(formInfoObj.formPanel.id, true);
        // select previously selected form, or the default view (index == 0)
        var newIndex = this.formCards.items.length - 1;
        newIndex = (newIndex >= 0) ? newIndex : 0;
        formLayout.setActiveItem(newIndex);

        // delete row from grid if canceling a new repo form
        if (formInfoObj.isNew)
        {
          store.remove(store.getById(formInfoObj.formPanel.id));
        }

        // select the coordinating row in the grid, or none if back to default
        var i = store.indexOfId(formLayout.activeItem.id);
        if (i >= 0)
        {
          gridSelectModel.selectRow(i);
        }
        else
        {
          gridSelectModel.clearSelections();
        }
      },

      addResourceHandler : function() {
        var id = 'new_route_' + new Date().getTime();

        var config = Ext.apply({}, this.formConfig.route, {
              id : id
            });
        var formPanel = new Ext.FormPanel(config);

        formPanel.form.on('actioncomplete', this.actionCompleteHandler, this);
        formPanel.form.on('actionfailed', this.actionFailedHandler, this);
        formPanel.on('beforerender', this.beforeFormRenderHandler, this);
        formPanel.on('afterlayout', this.afterLayoutFormHandler, this, {
              single : true
            });

        var buttonInfoObj = {
          formPanel : formPanel,
          isNew : true
        };

        // save button event handler
        formPanel.buttons[0].on('click', this.saveHandler.createDelegate(this, [buttonInfoObj]));
        // cancel button event handler
        formPanel.buttons[1].on('click', this.cancelHandler.createDelegate(this, [buttonInfoObj]));

        // add place holder to grid
        var newRec = new this.routeRecordConstructor({
              pattern : 'New Route',
              ruleType : '-',
              resourceURI : 'new',
              repositories : []
            }, id); // use "new_route_" id instead of resourceURI like the
                    // reader does
        this.routesDataStore.insert(0, [newRec]);

        // add new form
        this.formCards.add(formPanel);

        // always set active and re-layout
        this.formCards.getLayout().setActiveItem(formPanel);
        formPanel.doLayout();

        this.routesGridPanel.getSelectionModel().selectRow(0);
      },

      afterLayoutFormHandler : function(formPanel, fLayout) {
        // register required field quicktip, but have to wait for elements to
        // show up in DOM
        var temp = function() {
          var els = Ext.select('.required-field .x-form-item-label, .required-field .x-panel-header-text', this.getEl());
          els.each(function(el, els, i) {
                Ext.QuickTips.register({
                      target : el,
                      cls : 'required-field',
                      title : '',
                      text : 'Required Field',
                      enabled : true
                    });
              });
        }.defer(300, formPanel);

      },

      deleteResourceHandler : function() {
        if (this.routesGridPanel.getSelectionModel().hasSelection())
        {
          var selections = this.routesGridPanel.getSelectionModel().getSelections();

          if (selections.length == 1)
          {
            var rec = this.routesGridPanel.getSelectionModel().getSelected();

            if (rec.data.resourceURI == 'new')
            {
              this.cancelHandler({
                    formPanel : Ext.getCmp(rec.id),
                    isNew : true
                  });
            }
            else
            {
              Sonatype.utils.defaultToNo();
              Sonatype.MessageBox.show({
                    animEl : this.routesGridPanel.getEl(),
                    title : 'Delete Route',
                    msg : 'Delete the ' + rec.get('pattern') + ' route?',
                    buttons : Sonatype.MessageBox.YESNO,
                    scope : this,
                    icon : Sonatype.MessageBox.QUESTION,
                    fn : function(btnName) {
                      if (btnName == 'yes' || btnName == 'ok')
                      {
                        Ext.Ajax.request({
                              callback : this.deleteCallback,
                              cbPassThru : {
                                resourceId : rec.id
                              },
                              scope : this,
                              method : 'DELETE',
                              url : rec.data.resourceURI
                            });
                      }
                    }
                  });
            }
          }
          else
          {
            Sonatype.utils.defaultToNo();
            Sonatype.MessageBox.show({
                  animEl : this.routesGridPanel.getEl(),
                  title : 'Delete Routes',
                  msg : 'Delete ' + selections.length + ' routes?',
                  buttons : Sonatype.MessageBox.YESNO,
                  scope : this,
                  icon : Sonatype.MessageBox.QUESTION,
                  fn : function(btnName) {
                    Ext.each(selections, function(rec) {
                          if (rec.data.resourceURI == 'new')
                          {
                            this.cancelHandler({
                                  formPanel : Ext.getCmp(rec.id),
                                  isNew : true
                                });
                          }
                          else
                          {
                            Ext.Ajax.request({
                                  callback : this.deleteCallback,
                                  cbPassThru : {
                                    resourceId : rec.id
                                  },
                                  scope : this,
                                  method : 'DELETE',
                                  url : rec.data.resourceURI
                                });
                          }
                        }, this);
                  }
                });
          }
        }
      },

      deleteCallback : function(options, isSuccess, response) {
        if (isSuccess)
        {
          var resourceId = options.cbPassThru.resourceId;
          var formLayout = this.formCards.getLayout();
          var gridSelectModel = this.routesGridPanel.getSelectionModel();
          var store = this.routesGridPanel.getStore();

          if (formLayout.activeItem.id == resourceId)
          {
            this.formCards.remove(resourceId, true);
            // select previously selected form, or the default view (index == 0)
            var newIndex = this.formCards.items.length - 1;
            newIndex = (newIndex >= 0) ? newIndex : 0;
            formLayout.setActiveItem(newIndex);
          }
          else
          {
            this.formCards.remove(resourceId, true);
          }

          store.remove(store.getById(resourceId));

          // select the coordinating row in the grid, or none if back to default
          var i = store.indexOfId(formLayout.activeItem.id);
          if (i >= 0)
          {
            gridSelectModel.selectRow(i);
          }
          else
          {
            gridSelectModel.clearSelections();
          }
        }
        else
        {
          Sonatype.MessageBox.alert('The server did not delete the route.');
        }
      },

      // (Ext.form.BasicForm, Ext.form.Action)
      actionCompleteHandler : function(form, action) {
        // @todo: handle server error response here!!

        if (action.type == 'sonatypeSubmit')
        {
          var isNew = action.options.isNew;

          if (isNew)
          {
            // successful create
            var respData = (Ext.decode(action.response.responseText)).data;
            // route state data doesn't have resourceURI in it like the list
            // data
            respData.resourceURI = action.getUrl() + '/' + respData.id; // add
                                                                        // this
                                                                        // to
                                                                        // match
                                                                        // the
                                                                        // list
                                                                        // data
                                                                        // field
                                                                        // to
                                                                        // create
                                                                        // the
                                                                        // record
            // @ext: this application of the convert function is needed because
            // Ext only
            // applies the convert functions in
            // Ext.data.XXXReader.readRecords(),
            // not in the constructor yielded from Ext.data.Record.create()
            respData.sRepositories = this.nameFinder(respData.repositories);

            var newRec = new this.routeRecordConstructor(respData, action.options.fpanel.id); // form
                                                                                              // and
                                                                                              // grid
                                                                                              // data
                                                                                              // id
                                                                                              // match,
                                                                                              // keep
                                                                                              // the
                                                                                              // new
                                                                                              // id

            this.routesDataStore.remove(this.routesDataStore.getById(action.options.fpanel.id)); // remove
                                                                                                  // old
                                                                                                  // one
            this.routesDataStore.addSorted(newRec);
            this.routesGridPanel.getSelectionModel().selectRecords([newRec], false);

            // set the hidden id field in the form for subsequent updates
            action.options.fpanel.find('name', 'id')[0].setValue(respData.id);
            // remove button click listeners
            action.options.fpanel.buttons[0].purgeListeners();
            action.options.fpanel.buttons[1].purgeListeners();

            var buttonInfoObj = {
              formPanel : action.options.fpanel,
              isNew : false,
              resourceURI : respData.resourceURI
            };

            // save button event handler
            action.options.fpanel.buttons[0].on('click', this.saveHandler.createDelegate(this, [buttonInfoObj]));
            // cancel button event handler
            action.options.fpanel.buttons[1].on('click', this.cancelHandler.createDelegate(this, [buttonInfoObj]));
          }
          else
          {
            var sentData = action.output.data;

            var i = this.routesDataStore.indexOfId(action.options.fpanel.id);
            var rec = this.routesDataStore.getAt(i);

            rec.beginEdit();
            rec.set('pattern', sentData.pattern);
            rec.set('ruleType', sentData.ruleType);
            rec.set('groupId', sentData.groupId);
            rec.set('repositories', sentData.repositories);
            rec.set('sRepositories', this.nameFinder(sentData.repositories));
            rec.commit();
            rec.endEdit();

            var sortState = this.routesDataStore.getSortState();
            this.routesDataStore.sort(sortState.field, sortState.direction);
          }
        }
        else if (action.type == 'sonatypeLoad')
        {
          var value = action.options.fpanel.find('name', 'ruleType')[0].getValue();
          this.updateTreePanel(value == this.BLOCKING, action.options.fpanel.id);
        }
      },

      // (Ext.form.BasicForm, Ext.form.Action)
      actionFailedHandler : function(form, action) {
        if (action.failureType == Ext.form.Action.CLIENT_INVALID)
        {
          Sonatype.MessageBox.alert('Missing or Invalid Fields', 'Please change the missing or invalid fields.').setIcon(Sonatype.MessageBox.WARNING);
        }
        // @note: server validation error are now handled just like client
        // validation errors by marking the field invalid
        // else if(action.failureType == Ext.form.Action.SERVER_INVALID){
        // Sonatype.MessageBox.alert('Invalid Fields', 'The server identified
        // invalid fields.').setIcon(Sonatype.MessageBox.ERROR);
        // }
        else if (action.failureType == Ext.form.Action.CONNECT_FAILURE)
        {
          Sonatype.utils.connectionError(action.response, 'There is an error communicating with the server.')
        }
        else if (action.failureType == Ext.form.Action.LOAD_FAILURE)
        {
          Sonatype.MessageBox.alert('Load Failure', 'The data failed to load from the server.').setIcon(Sonatype.MessageBox.ERROR);
        }

        // @todo: need global alert mechanism for fatal errors.
      },

      beforeFormRenderHandler : function(component) {
        var sp = Sonatype.lib.Permissions;
        if (sp.checkPermission('nexus:routes', sp.EDIT))
        {
          component.buttons[0].disabled = false;
        }
      },

      formDataLoader : function(formPanel, resourceURI, modFuncs) {
        formPanel.getForm().doAction('sonatypeLoad', {
              url : resourceURI,
              method : 'GET',
              fpanel : formPanel,
              dataModifiers : modFuncs,
              scope : this
            });
      },

      rowSelect : function(selectionModel, index, rec) {
        var id = rec.id; // note: rec.id is unique for new resources and equal
                          // to resourceURI for existing ones
        var formPanel = this.formCards.findById(id);

        // assumption: new route forms already exist in formCards, so they won't
        // get into this case
        if (!formPanel)
        { // create form and populate current data
          var config = Ext.apply({}, this.formConfig.route, {
                id : id
              });

          formPanel = new Ext.FormPanel(config);
          formPanel.form.on('actioncomplete', this.actionCompleteHandler, this);
          formPanel.form.on('actionfailed', this.actionFailedHandler, this);
          formPanel.on('beforerender', this.beforeFormRenderHandler, this);
          formPanel.on('afterlayout', this.afterLayoutFormHandler, this, {
                single : true
              });

          var buttonInfoObj = {
            formPanel : formPanel,
            isNew : false, // not a new route form, see assumption
            resourceURI : rec.data.resourceURI
          };

          // save button event handler
          formPanel.buttons[0].on('click', this.saveHandler.createDelegate(this, [buttonInfoObj]));
          // cancel button event handler
          formPanel.buttons[1].on('click', this.cancelHandler.createDelegate(this, [buttonInfoObj]));

          this.formDataLoader(formPanel, rec.data.resourceURI, this.loadDataModFuncs.route);
          this.formCards.add(formPanel);
        }

        // always set active and re-layout
        this.formCards.getLayout().setActiveItem(formPanel);
        formPanel.doLayout();
      },

      // requires being scoped to RoutesConfigPanel to get shared reposList data
      loadRepoListHelper : function(arr, srcObj, fpanel) {
        var tree = fpanel.find('name', 'repositories')[0];

        tree.setValue(arr);

        return arr; // return arr, even if empty to comply with sonatypeLoad
                    // data modifier requirement
      },

      exportRepoListHelper : function(val, fpanel) {
        var tree = fpanel.find('name', 'repositories')[0];

        var outputArr = [];
        var items = tree.getValue();

        for (var i = 0; i < items.length; i++)
        {
          outputArr[i] = {
            id : items[i]
          };
        }

        return outputArr;
      },

      updateTreePanel : function(blockingType, id) {
        var p = this.formCards.getLayout().activeItem.find('name', 'repositories')[0];
        p.setVisible(!blockingType);
        p.required = !blockingType;
      }
    });

});

