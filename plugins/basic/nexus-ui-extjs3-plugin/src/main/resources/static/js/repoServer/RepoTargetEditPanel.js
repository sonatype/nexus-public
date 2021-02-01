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
 * Target Edit/Create panel layout and controller
 */
define('repoServer/RepoTargetEditPanel',['Sonatype/all','Nexus/ext/GridFilterBox'], function(){

Sonatype.repoServer.RepoTargetEditPanel = function(config) {
  var config = config || {},
      defaultConfig = {};

  Ext.apply(this, config, defaultConfig);

  var ht = Sonatype.repoServer.resources.help.repoTargets;

  this.actions = {
    refresh : new Ext.Action({
          text : 'Refresh',
          iconCls : 'st-icon-refresh',
          scope : this,
          handler : this.reloadAll
        }),
    deleteAction : new Ext.Action({
          text : 'Delete',
          scope : this,
          handler : this.deleteHandler
        })
  };

  // Methods that will take the incoming json data and map over to the ui
  // controls
  this.loadDataModFunc = {
    "patterns" : this.loadPatternsTreeHelper.createDelegate(this)
  };

  // Methods that will take the data from the ui controls and map over to json
  this.submitDataModFunc = {
    "patterns" : this.exportPatternsTreeHelper.createDelegate(this)
  };

  // A record to hold the name and id of a repository
  this.repoTargetRecordConstructor = Ext.data.Record.create([{
        name : 'resourceURI'
      }, {
        name : 'id'
      }, {
        name : 'contentClass'
      }, {
        name : 'name',
        sortType : Ext.data.SortTypes.asUCString
      }, {
        name : 'patterns'
      }]);

  // A record to hold the contentClasses
  this.contentClassRecordConstructor = Ext.data.Record.create([{
        name : 'contentClass'
      }, {
        name : 'name',
        sortType : Ext.data.SortTypes.asUCString
      }]);

  // Reader and datastore that queries the server for the list of repo targets
  this.repoTargetsReader = new Ext.data.JsonReader({
        root : 'data',
        id : 'resourceURI'
      }, this.repoTargetRecordConstructor);
  this.repoTargetsDataStore = new Ext.data.Store({
        url : Sonatype.config.repos.urls.repoTargets,
        reader : this.repoTargetsReader,
        sortInfo : {
          field : 'name',
          direction : 'ASC'
        },
        autoLoad : true
      });

  // Reader and datastore that queries the server for the list of content
  // classes
  this.contentClassesReader = new Ext.data.JsonReader({
        root : 'data',
        id : 'contentClass'
      }, this.contentClassRecordConstructor);
  this.contentClassesDataStore = new Ext.data.Store({
        url : Sonatype.config.repos.urls.repoContentClasses,
        reader : this.contentClassesReader,
        sortInfo : {
          field : 'name',
          direction : 'ASC'
        },
        autoLoad : false
      });

  this.COMBO_WIDTH = 300;

  // Build the form
  this.formConfig = {
    region : 'center',
    width : '100%',
    height : '100%',
    autoScroll : true,
    border : false,
    frame : true,
    collapsible : false,
    collapsed : false,
    labelWidth : 150,
    layoutConfig : {
      labelSeparator : ''
    },

    items : [{
          xtype : 'textfield',
          fieldLabel : 'Name',
          itemCls : 'required-field',
          helpText : ht.name,
          name : 'name',
          allowBlank : false,
          width : this.COMBO_WIDTH,
          validator : this.checkForDuplicates.createDelegate(this)
        }, {
          xtype : 'combo',
          fieldLabel : 'Repository Type',
          itemCls : 'required-field',
          helpText : ht.contentClass,
          name : 'contentClass',
          width : this.COMBO_WIDTH,
          store : this.contentClassesDataStore,
          displayField : 'name',
          valueField : 'contentClass',
          editable : false,
          forceSelection : true,
          mode : 'local',
          triggerAction : 'all',
          emptyText : 'Select...',
          selectOnFocus : true,
          allowBlank : false,
          disabled : true,
          listeners : {
            'select' : {
              fn : function(combo, record, index) {},
              scope : this
            }
          }
        }, {
          xtype : 'panel',
          style : 'padding-top: 20px',
          layout : 'column',
          items : [{
                xtype : 'panel',
                layout : 'form',
                width : 475,
                items : [{
                      xtype : 'textfield',
                      fieldLabel : 'Pattern Expression',
                      helpText : ht.pattern,
                      name : 'pattern',
                      width : 300
                    }]
              }, {
                xtype : 'panel',
                width : 120,
                items : [{
                      xtype : 'button',
                      text : 'Add',
                      style : 'padding-left: 7px',
                      minWidth : 100,
                      id : 'button-add',
                      handler : this.addNewPattern,
                      scope : this
                    }]
              }]
        }, {
          xtype : 'panel',
          layout : 'column',
          autoHeight : true,
          style : 'padding-left: 155px',
          items : [{
                xtype : 'treepanel',
                id : 'repoTargets-pattern-list', // note: unique ID is assinged
                                                  // before instantiation
                name : 'repoTargets-pattern-list',
                title : 'Patterns',
                cls : 'required-field',
                border : true, // note: this seem to have no effect w/in form
                                // panel
                bodyBorder : true, // note: this seem to have no effect w/in
                                    // form panel
                // note: this style matches the expected behavior
                bodyStyle : 'background-color:#FFFFFF; border: 1px solid #B5B8C8',
                style : 'padding: 0 20px 0 0',
                width : 320,
                height : 150,
                animate : true,
                lines : false,
                autoScroll : true,
                containerScroll : true,
                // @note: root node must be instantiated uniquely for each
                // instance of treepanel
                // @ext: can TreeNode be registerd as a component with an xtype
                // so this new root node
                // may be instantiated uniquely for each form panel that uses
                // this config?
                rootVisible : false,
                enableDD : false
              }, {
                xtype : 'panel',
                width : 120,
                items : [{
                      xtype : 'button',
                      text : 'Remove',
                      style : 'padding-left: 6px',
                      minWidth : 100,
                      id : 'button-remove',
                      handler : this.removePattern,
                      scope : this
                    }, {
                      xtype : 'button',
                      text : 'Remove All',
                      style : 'padding-left: 6px; margin-top: 5px',
                      minWidth : 100,
                      id : 'button-remove-all',
                      handler : this.removeAllPatterns,
                      scope : this
                    }]
              }]
        }, {
          xtype : 'hidden',
          name : 'id'
        }],
    buttons : [{
          text : 'Save',
          disabled : true
        }, {
          text : 'Cancel'
        }]
  };

  this.sp = Sonatype.lib.Permissions;

  this.repoTargetsGridPanel = new Ext.grid.GridPanel({
        id : 'st-repoTargets-grid',
        region : 'north',
        layout : 'fit',
        split : true,
        height : 200,
        minHeight : 150,
        maxHeight : 400,
        frame : false,
        autoScroll : true,
        tbar : [{
              id : 'repoTarget-refresh-btn',
              text : 'Refresh',
              iconCls : 'st-icon-refresh',
              cls : 'x-btn-text-icon',
              scope : this,
              handler : this.reloadAll
            }, {
              id : 'repoTarget-add-btn',
              text : 'Add',
              icon : Sonatype.config.resourcePath + '/static/images/icons/add.png',
              cls : 'x-btn-text-icon',
              scope : this,
              handler : this.addResourceHandler,
              disabled : !this.sp.checkPermission('nexus:targets', this.sp.CREATE)
            }, {
              id : 'repoTarget-delete-btn',
              text : 'Delete',
              icon : Sonatype.config.resourcePath + '/static/images/icons/delete.png',
              cls : 'x-btn-text-icon',
              scope : this,
              handler : this.deleteHandler,
              disabled : !this.sp.checkPermission('nexus:targets', this.sp.DELETE)
            }
        ],

        // grid view options
        ds : this.repoTargetsDataStore,
        sortInfo : {
          field : 'name',
          direction : "ASC"
        },
        loadMask : true,
        deferredRender : false,
        columns : [{
              header : 'Name',
              dataIndex : 'name',
              width : 200
            }, {
              header : 'Repository Type',
              dataIndex : 'contentClass',
              width : 200
            }, {
              header : 'Patterns',
              dataIndex : 'patterns',
              width : 200,
              id : 'patterns-expandable-col'
            }],
        autoExpandColumn : 'patterns-expandable-col',
        disableSelection : false,
        viewConfig : {
          deferEmptyText: false,
          emptyText: 'No repository targets defined',
          emptyTextWhileFiltering: 'No repository targets matched criteria: {criteria}'
        }
      });
  this.repoTargetsGridPanel.getSelectionModel().on('rowselect', this.rowSelect, this);
  this.repoTargetsGridPanel.on('rowcontextmenu', this.contextClick, this);

  this.repoTargetsGridPanel.getTopToolbar().add([
      '->',
      NX.create('Nexus.ext.GridFilterBox', {
        filteredGrid: this.repoTargetsGridPanel
      })
  ]);

  Sonatype.repoServer.RepoTargetEditPanel.superclass.constructor.call(this, {
        layout : 'border',
        autoScroll : false,
        width : '100%',
        height : '100%',
        items : [this.repoTargetsGridPanel, {
              xtype : 'panel',
              id : 'repoTarget-config-forms',
              title : 'Repository Target Configuration',
              layout : 'card',
              region : 'center',
              activeItem : 0,
              deferredRender : false,
              autoScroll : false,
              frame : false,
              items : [{
                    xtype : 'panel',
                    layout : 'fit',
                    html : '<div class="little-padding">Select a target to edit it, or click "Add" to create a new one.</div>'
                  }]
            }]
      });

  this.formCards = this.findById('repoTarget-config-forms');
};

Ext.extend(Sonatype.repoServer.RepoTargetEditPanel, Ext.Panel, {
      // Dump the currently stored data and requery for everything
      reloadAll : function() {
        this.repoTargetsDataStore.reload();
        this.formCards.items.each(function(item, i, len) {
              if (i > 0)
              {
                this.remove(item, true);
              }
            }, this.formCards);

        this.formCards.getLayout().setActiveItem(0);
      },

      saveHandler : function(formInfoObj) {
        // validate the form to make sure all the error messages are present
        var formIsValid = formInfoObj.formPanel.form.isValid();

        var fpanel = formInfoObj.formPanel;
        var treePanel = fpanel.findById(fpanel.id + '_repoTargets-pattern-list');
        var patternField = fpanel.find('name', 'pattern')[0];
        if (!treePanel.root.hasChildNodes())
        {
          patternField.markInvalid('The target should have at least one pattern.');
          return;
        }

        if (formIsValid)
        {
          var isNew = formInfoObj.isNew;
          var createUri = Sonatype.config.repos.urls.repoTargets;
          var updateUri = (formInfoObj.resourceURI) ? formInfoObj.resourceURI : '';
          var form = formInfoObj.formPanel.form;

          form.doAction('sonatypeSubmit', {
            method : (isNew) ? 'POST' : 'PUT',
            url : isNew ? createUri : updateUri,
            waitMsg : isNew ? 'Creating Target...' : 'Updating Target...',
            fpanel : formInfoObj.formPanel,
            dataModifiers : this.submitDataModFunc,
            serviceDataObj : Sonatype.repoServer.referenceData.repoTargets,
            isNew : isNew
              // extra option to send to callback, instead of conditioning on
              // method
            });
        }
      },

      cancelHandler : function(formInfoObj) {
        var formLayout = this.formCards.getLayout();
        var gridSelectModel = this.repoTargetsGridPanel.getSelectionModel();
        var store = this.repoTargetsGridPanel.getStore();

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
        var id = 'new_target_' + new Date().getTime();

        var config = Ext.apply({}, this.formConfig, {
              id : id
            });

        config = this.initializeTreeRoots(id, config);

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

        // add new form
        this.formCards.add(formPanel);

        // add place holder to grid
        var newRec = new this.repoTargetRecordConstructor({
              name : 'New Target',
              resourceURI : 'new'
            }, id); // use "new_user_" id instead of resourceURI like the reader
                    // does
        this.repoTargetsDataStore.insert(0, [newRec]);
        this.repoTargetsGridPanel.getSelectionModel().selectRow(0);

        // always set active and re-layout
        formPanel.doLayout();
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

      deleteHandler : function() {
        if (this.ctxRecord || this.repoTargetsGridPanel.getSelectionModel().hasSelection())
        {
          var rec = this.ctxRecord ? this.ctxRecord : this.repoTargetsGridPanel.getSelectionModel().getSelected();

          if (rec.data.resourceURI == 'new')
          {
            this.cancelHandler({
                  formPanel : Ext.getCmp(rec.id),
                  isNew : true
                });
          }
          else
          {
            // @note: this handler selects the "No" button as the default
            // @todo: could extend Sonatype.MessageBox to take the button to
            // select as a param
            Sonatype.MessageBox.getDialog().on('show', function() {
                  this.focusEl = this.buttons[2]; // ack! we're offset dependent
                                                  // here
                  this.focus();
                }, Sonatype.MessageBox.getDialog(), {
                  single : true
                });

            Sonatype.MessageBox.show({
                  animEl : this.repoTargetsGridPanel.getEl(),
                  title : 'Delete Target?',
                  msg : 'Delete the ' + rec.get('name') + ' target?',
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
      },

      deleteCallback : function(options, isSuccess, response) {
        if (isSuccess)
        {
          var resourceId = options.cbPassThru.resourceId;
          var formLayout = this.formCards.getLayout();
          var gridSelectModel = this.repoTargetsGridPanel.getSelectionModel();
          var store = this.repoTargetsGridPanel.getStore();

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
          Sonatype.MessageBox.alert('The server did not delete the target.');
        }
      },

      // (Ext.form.BasicForm, Ext.form.Action)
      actionCompleteHandler : function(form, action) {
        // @todo: handle server error response here!!

        if (action.type == 'sonatypeSubmit')
        {
          var isNew = action.options.isNew;
          var receivedData = action.handleResponse(action.response).data;
          if (isNew)
          {

            var dataObj = {
              id : receivedData.id,
              name : receivedData.name,
              resourceURI : action.getUrl() + '/' + receivedData.id,
              contentClass : receivedData.contentClass,
              patterns : receivedData.patterns
            };

            var newRec = new this.repoTargetRecordConstructor(dataObj, action.options.fpanel.id);

            this.repoTargetsDataStore.remove(this.repoTargetsDataStore.getById(action.options.fpanel.id)); // remove
                                                                                                            // old
                                                                                                            // one
            this.repoTargetsDataStore.addSorted(newRec);
            this.repoTargetsGridPanel.getSelectionModel().selectRecords([newRec], false);

            // set the hidden id field in the form for subsequent updates
            action.options.fpanel.find('name', 'id')[0].setValue(receivedData.id);

            // remove button click listeners
            action.options.fpanel.buttons[0].purgeListeners();
            action.options.fpanel.buttons[1].purgeListeners();

            var buttonInfoObj = {
              formPanel : action.options.fpanel,
              isNew : false,
              resourceURI : dataObj.resourceURI
            };

            // save button event handler
            action.options.fpanel.buttons[0].on('click', this.saveHandler.createDelegate(this, [buttonInfoObj]));

            // cancel button event handler
            action.options.fpanel.buttons[1].on('click', this.cancelHandler.createDelegate(this, [buttonInfoObj]));
          }
          else
          {
            var sentData = action.output.data;

            var i = this.repoTargetsDataStore.indexOfId(action.options.fpanel.id);
            var rec = this.repoTargetsDataStore.getAt(i);

            this.updateRepoTargetRecord(rec, sentData);

            var sortState = this.repoTargetsDataStore.getSortState();
            this.repoTargetsDataStore.sort(sortState.field, sortState.direction);
          }
        }
      },

      updateRepoTargetRecord : function(rec, receivedData) {
        rec.beginEdit();
        rec.set('name', receivedData.name);
        rec.set('id', receivedData.id);
        rec.set('contentClass', receivedData.contentClass);
        rec.set('patterns', receivedData.patterns);
        rec.commit();
        rec.endEdit();
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
        // TODO
        if (sp.checkPermission('nexus:targets', sp.EDIT))
        {
          component.buttons[0].disabled = false;
        }
        if (sp.checkPermission('nexus:componentscontentclasses', sp.READ))
        {
          this.contentClassesDataStore.load();
          var contentClassCombo = component.items.get(1);
          contentClassCombo.disabled = false;
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
          var config = Ext.apply({}, this.formConfig, {
                id : id
              });

          config = this.initializeTreeRoots(id, config);

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

          formPanel.buttons[0].on('click', this.saveHandler.createDelegate(this, [buttonInfoObj]));
          formPanel.buttons[1].on('click', this.cancelHandler.createDelegate(this, [buttonInfoObj]));

          this.formDataLoader(formPanel, rec.data.resourceURI, this.loadDataModFunc);

          this.formCards.add(formPanel);
          this.formCards.getLayout().setActiveItem(formPanel);
          formPanel.doLayout();
        }
        else
        {
          // always set active
          this.formCards.getLayout().setActiveItem(formPanel);
        }
      },

      contextClick : function(grid, index, e) {
        this.contextHide();

        if (e.target.nodeName == 'A')
          return; // no menu on links

        this.ctxRow = this.repoTargetsGridPanel.view.getRow(index);
        this.ctxRecord = this.repoTargetsGridPanel.store.getAt(index);
        Ext.fly(this.ctxRow).addClass('x-node-ctx');

        // @todo: would be faster to pre-render the six variations of the menu
        // for whole instance
        var menu = new Ext.menu.Menu({
              items : [this.actions.refresh]
            });

        if (this.sp.checkPermission('nexus:targets', this.sp.DELETE))
        {
          menu.add(this.actions.deleteAction);
        }

        // TODO: Add additional menu items

        menu.on('hide', this.contextHide, this);
        e.stopEvent();
        menu.showAt(e.getXY());
      },

      contextHide : function() {
        if (this.ctxRow)
        {
          Ext.fly(this.ctxRow).removeClass('x-node-ctx');
          this.ctxRow = null;
          this.ctxRecord = null;
        }
      },

      initializeTreeRoots : function(id, config) {
        // @note: there has to be a better way to do this. Depending on offsets
        // is very error prone
        var newConfig = config;

        newConfig.items[3].items[0].id = id + '_repoTargets-pattern-list';
        newConfig.items[3].items[0].root = new Ext.tree.TreeNode({
              text : 'root'
            });

        return newConfig;
      },

      loadPatternsTreeHelper : function(arr, srcObj, fpanel) {
        var treePanel = fpanel.findById(fpanel.id + '_repoTargets-pattern-list');

        var pattern;

        for (var i = 0; i < arr.length; i++)
        {
          pattern = arr[i];
          this.addPatternNode(treePanel, pattern);
        }

        return arr; // return arr, even if empty to comply with sonatypeLoad
                    // data modifier requirement
      },

      exportPatternsTreeHelper : function(val, fpanel) {
        var treePanel = fpanel.findById(fpanel.id + '_repoTargets-pattern-list');

        var outputArr = [];
        var nodes = treePanel.root.childNodes;

        for (var i = 0; i < nodes.length; i++)
        {
          outputArr[i] = nodes[i].attributes.payload;
        }

        return outputArr;
      },

      addPatternNode : function(treePanel, pattern) {
        var id = Ext.id();

        treePanel.root.appendChild(new Ext.tree.TreeNode({
                  id : id,
                  text : pattern,
                  payload : pattern,
                  allowChildren : false,
                  draggable : false,
                  leaf : true,
                  nodeType : 'pattern',
                  icon : Sonatype.config.extPath + '/resources/images/default/tree/leaf.gif'
                }));
      },

      addNewPattern : function() {
        var fpanel = this.formCards.getLayout().activeItem;
        var treePanel = fpanel.findById(fpanel.id + '_repoTargets-pattern-list');
        var patternField = fpanel.find('name', 'pattern')[0];
        var pattern = patternField.getRawValue();

        if (pattern)
        {
          var nodes = treePanel.root.childNodes;
          for (var i = 0; i < nodes.length; i++)
          {
            if (pattern == nodes[i].attributes.payload)
            {
              patternField.markInvalid('This pattern already exists');
              return;
            }
          }

          this.addPatternNode(treePanel, pattern);
          patternField.setRawValue('');
        }
      },

      removePattern : function() {
        var fpanel = this.formCards.getLayout().activeItem;
        var treePanel = fpanel.findById(fpanel.id + '_repoTargets-pattern-list');

        var selectedNode = treePanel.getSelectionModel().getSelectedNode();
        if (selectedNode)
        {
          treePanel.root.removeChild(selectedNode);
        }
      },

      removeAllPatterns : function() {
        var fpanel = this.formCards.getLayout().activeItem;
        var treePanel = fpanel.findById(fpanel.id + '_repoTargets-pattern-list');
        var treeRoot = treePanel.root;

        while (treeRoot.lastChild)
        {
          treeRoot.removeChild(treeRoot.lastChild);
        }
      },

      checkForDuplicates : function(value) {
        var selectedRec = this.repoTargetsGridPanel.getSelectionModel().getSelected();
        var result = this.repoTargetsDataStore.findBy(function(rec, id) {
              return selectedRec != rec && rec.get('name') == value;
            });
        if (result >= 0)
        {
          return 'A target with this name already exists.';
        }
        return true;
      }

    });

});

