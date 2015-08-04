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
 * Service Schedule Edit/Create panel layout and controller
 */

define('repoServer/SchedulesEditPanel',['Sonatype/all', 'Sonatype/strings','Nexus/ext/GridFilterBox'], function(Sonatype, Strings){
Sonatype.repoServer.SchedulesEditPanel = function(config) {
  var config = config || {},
      defaultConfig = {};

  Ext.apply(this, config, defaultConfig);

  var ht = Sonatype.repoServer.resources.help.schedules;

  // List of schedule types
  // None removed for the time being
  this.scheduleTypeStore = new Ext.data.SimpleStore({
        fields : ['value'],
        data : [['Manual'], ['Once'], ['Hourly'], ['Daily'], ['Weekly'], ['Monthly'], ['Advanced']]
      });
  // List of weekdays
  this.weekdaysStore = new Ext.data.SimpleStore({
        fields : ['name', 'id'],
        data : [['Sunday', 'sunday'], ['Monday', 'monday'], ['Tuesday', 'tuesday'], ['Wednesday', 'wednesday'], ['Thursday', 'thursday'], ['Friday', 'friday'], ['Saturday', 'saturday']]
      });

  this.customTypes = {};

  this.stopButton = new Ext.Button({
    id : 'schedule-stop-btn',
    text : 'Cancel',
    icon : Sonatype.config.resourcePath + '/static/images/icons/time_delete.png',
    cls : 'x-btn-text-icon',
    scope : this,
    handler : this.stopHandler,
    disabled : true
      });

  this.runButton = new Ext.Button({
        id : 'schedule-run-btn',
        text : 'Run',
        icon : Sonatype.config.resourcePath + '/static/images/icons/time_go.png',
        cls : 'x-btn-text-icon',
        scope : this,
        handler : this.runHandler,
        disabled : true
      });

  this.deleteButton = new Ext.Button({
        id : 'schedule-delete-btn',
        text : 'Delete',
        icon : Sonatype.config.resourcePath + '/static/images/icons/delete.png',
        cls : 'x-btn-text-icon',
        scope : this,
        handler : this.deleteHandler,
        disabled : true
      });

  this.disableEditingHeader = new Ext.Panel({
        id : 'disablingMsg',
        name : 'disablingMsg',
        layout : 'table',
        hidden : true,
        style : 'font-size: 18px; padding: 5px 0px 5px 15px',
        items : [{
              html : '<b>This is scheduled to be run.  It can\'t be edited or deleted.</b><br><hr/>'
            }]
      });

  // Methods that will take the incoming json data and map over to the ui
  // controls
  this.loadDataModFuncs = {
    internal : {
      schedule : Strings.capitalize,
      properties : this.importServicePropertiesHelper.createDelegate(this)
    },
    manual : {
      schedule : Strings.capitalize,
      properties : this.importServicePropertiesHelper.createDelegate(this)
    },
    once : {
      schedule : Strings.capitalize,
      properties : this.importServicePropertiesHelper.createDelegate(this),
      startDate : this.importStartDateHelper.createDelegate(this),
      startTime : this.importStartTimeHelper.createDelegate(this)
    },
    hourly : {
      schedule : Strings.capitalize,
      properties : this.importServicePropertiesHelper.createDelegate(this),
      startDate : this.importStartDateHelper.createDelegate(this),
      startTime : this.importStartTimeHelper.createDelegate(this)
    },
    daily : {
      schedule : Strings.capitalize,
      properties : this.importServicePropertiesHelper.createDelegate(this),
      startDate : this.importStartDateHelper.createDelegate(this),
      recurringTime : this.importRecurringTimeHelper.createDelegate(this)
    },
    weekly : {
      schedule : Strings.capitalize,
      properties : this.importServicePropertiesHelper.createDelegate(this),
      startDate : this.importStartDateHelper.createDelegate(this),
      recurringTime : this.importRecurringTimeHelper.createDelegate(this),
      recurringDay : this.importRecurringDayHelper.createDelegate(this)
    },
    monthly : {
      schedule : Strings.capitalize,
      properties : this.importServicePropertiesHelper.createDelegate(this),
      startDate : this.importStartDateHelper.createDelegate(this),
      recurringTime : this.importRecurringTimeHelper.createDelegate(this),
      recurringDay : this.importMonthlyRecurringDayHelper.createDelegate(this)
    },
    advanced : {
      schedule : Strings.capitalize,
      properties : this.importServicePropertiesHelper.createDelegate(this)
    }
  };

  // Methods that will take the data from the ui controls and map over to json
  this.submitDataModFuncs = {
    manual : {
      schedule : Strings.lowercase,
      properties : this.exportServicePropertiesHelper.createDelegate(this)
    },
    once : {
      schedule : Strings.lowercase,
      properties : this.exportServicePropertiesHelper.createDelegate(this),
      startDate : this.exportStartDateHelper.createDelegate(this),
      startTime : this.exportStartTimeHelper.createDelegate(this)
    },
    hourly : {
      schedule : Strings.lowercase,
      properties : this.exportServicePropertiesHelper.createDelegate(this),
      startDate : this.exportStartDateHelper.createDelegate(this),
      startTime : this.exportStartTimeHelper.createDelegate(this)
    },
    daily : {
      schedule : Strings.lowercase,
      properties : this.exportServicePropertiesHelper.createDelegate(this),
      startDate : this.exportStartDateHelper.createDelegate(this),
      recurringTime : this.exportRecurringTimeHelper.createDelegate(this)
    },
    weekly : {
      schedule : Strings.lowercase,
      properties : this.exportServicePropertiesHelper.createDelegate(this),
      startDate : this.exportStartDateHelper.createDelegate(this),
      recurringTime : this.exportRecurringTimeHelper.createDelegate(this),
      recurringDay : this.exportRecurringDayHelper.createDelegate(this)
    },
    monthly : {
      schedule : Strings.lowercase,
      properties : this.exportServicePropertiesHelper.createDelegate(this),
      startDate : this.exportStartDateHelper.createDelegate(this),
      recurringTime : this.exportRecurringTimeHelper.createDelegate(this),
      recurringDay : this.exportMonthlyRecurringDayHelper.createDelegate(this)
    },
    advanced : {
      schedule : Strings.lowercase,
      properties : this.exportServicePropertiesHelper.createDelegate(this)
    }
  };

  // A record to hold the name and id of a repository
  this.repositoryRecordConstructor = Ext.data.Record.create([{
        name : 'id'
      }, {
        name : 'name',
        sortType : Ext.data.SortTypes.asUCString
      }]);

  // A record to hold the name and id of a repository group
  this.repositoryGroupRecordConstructor = Ext.data.Record.create([{
        name : 'id'
      }, {
        name : 'name',
        sortType : Ext.data.SortTypes.asUCString
      }]);

  this.repositoryOrGroupRecordConstructor = Ext.data.Record.create([{
        name : 'id'
      }, {
        name : 'name',
        sortType : Ext.data.SortTypes.asUCString
      }]);

  // Simply a record to hold details of each service type
  this.serviceTypeRecordConstructor = Ext.data.Record.create([{
        name : 'id',
        sortType : Ext.data.SortTypes.asUCString
      }, {
        name : 'name'
      }, {
        name : 'formFields'
      }]);

  // A record that holds the data for each service in the system
  this.scheduleRecordConstructor = Ext.data.Record.create([{
        name : 'resourceURI'
      }, {
        name : 'enabled'
      }, {
        name : 'id'
      }, {
        name : 'name',
        sortType : Ext.data.SortTypes.asUCString
      }, {
        name : 'typeName'
      }, {
        name : 'typeId'
      }, {
        name : 'readableStatus'
      }, {
        name : 'alertEmail'
      }, {
        name : 'schedule'
      }, {
        name : 'nextRunTimeInMillis',
        convert : function(v) {
          return v?new Date(v):'n/a';
        }
      }, {
        name : 'lastRunTimeInMillis',
        convert : function(v) {
          return v?new Date(v):'n/a';
        }
      }, {
        name : 'lastRunResult'
      }]);

  // Datastore that will hold both repos and repogroups
  this.repoOrGroupDataStore = new Ext.data.SimpleStore({
        fields : ['id', 'name'],
        id : 'id'
      });

  // Reader and datastore that queries the server for the list of repositories
  this.repositoryReader = new Ext.data.JsonReader({
        root : 'data',
        id : 'id'
      }, this.repositoryRecordConstructor);
  this.repositoryDataStore = new Ext.data.Store({
        url : Sonatype.config.repos.urls.repositories,
        reader : this.repositoryReader,
        sortInfo : {
          field : 'name',
          direction : 'ASC'
        },
        autoLoad : true,
        listeners : {
          'load' : {
            fn : function() {
              this.repositoryDataStore.each(function(item, i, len) {
                    var newRec = new this.repositoryOrGroupRecordConstructor({
                          id : item.data.id,
                          name : item.data.name + ' (Repo)'
                        }, item.id);
                    this.repoOrGroupDataStore.add([newRec]);
                  }, this);
              var allRec = new this.repositoryRecordConstructor({
                    id : 'all_repo',
                    name : 'All Repositories'
                  }, 'all_repo');
              this.repoOrGroupDataStore.insert(0, allRec);
            },
            scope : this
          }
        }
      });
  this.repositoryTargetDataStore = new Ext.data.JsonStore({
    url : Sonatype.config.repos.urls.repoTargets,
    root : 'data',
    id : 'id',
    fields : ['id', 'name', 'resourceURI', 'contentClass'],
    autoLoad: true
  });

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
              this.repositoryGroupDataStore.each(function(item, i, len) {
                    var newRec = new this.repositoryOrGroupRecordConstructor({
                          id : item.data.id,
                          name : item.data.name + ' (Group)'
                        }, item.id);
                    this.repoOrGroupDataStore.add([newRec]);
                  }, this);
            },
            scope : this
          }
        }
      });

  // Reader and datastore that queries the server for the list of service types
  this.serviceTypeReader = new Ext.data.JsonReader({
        root : 'data',
        id : 'id'
      }, this.serviceTypeRecordConstructor);
  this.serviceTypeDataStore = new Ext.data.Store({
        url : Sonatype.config.repos.urls.scheduleTypes,
        reader : this.serviceTypeReader,
        sortInfo : {
          field : 'name',
          direction : 'ASC'
        },
        autoLoad : true
      });

  // Reader and datastore that queries the server for the list of currently
  // defined services
  this.schedulesReader = new Ext.data.JsonReader({
        root : 'data',
        id : 'resourceURI'
      }, this.scheduleRecordConstructor);
  this.schedulesDataStore = new Ext.data.Store({
        url : Sonatype.config.repos.urls.schedules,
        reader : this.schedulesReader,
        sortInfo : {
          field : 'name',
          direction : 'ASC'
        },
        autoLoad : true
      });

  this.COMBO_WIDTH = 300;
  
  this.timeZone = new Date().toTimeString();
  this.timeZone = this.timeZone.substring(this.timeZone.indexOf(" "));

  // Build the form
  this.formConfig = {};
  this.formConfig.schedule = {
    region : 'center',
    width : '100%',
    height : '100%',
    autoScroll : true,
    border : false,
    frame : true,
    collapsible : false,
    collapsed : false,
    labelWidth : 200,
    layoutConfig : {
      labelSeparator : ''
    },

    items : [/* this.disableEditingHeader, */{
          xtype : 'hidden',
          name : 'id'
        }, {
          xtype : 'checkbox',
          fieldLabel : 'Enabled',
          labelStyle : 'margin-left: 15px; width: 185px;',
          helpText : ht.enabled,
          name : 'enabled',
          allowBlank : false,
          checked : true
        }, {
          xtype : 'textfield',
          fieldLabel : 'Name',
          labelStyle : 'margin-left: 15px; width: 185px;',
          itemCls : 'required-field',
          helpText : ht.name,
          name : 'name',
          width : this.COMBO_WIDTH,
          allowBlank : false
        }, {
          xtype : 'combo',
          fieldLabel : 'Task Type',
          labelStyle : 'margin-left: 15px; width: 185px;',
          itemCls : 'required-field',
          helpText : ht.serviceType,
          name : 'typeId',
          store : this.serviceTypeDataStore,
          displayField : 'name',
          valueField : 'id',
          editable : false,
          forceSelection : true,
          mode : 'local',
          triggerAction : 'all',
          emptyText : 'Select...',
          selectOnFocus : true,
          allowBlank : false,
          width : this.COMBO_WIDTH
        }, {
          xtype : 'panel',
          id : 'service-type-config-card-panel',
          header : false,
          layout : 'card',
          region : 'center',
          activeItem : 0,
          bodyStyle : 'padding:15px',
          deferredRender : false,
          autoScroll : false,
          frame : false,
          items : []
        }, {
          xtype : 'textfield',
          fieldLabel : 'Alert Email',
          labelStyle : 'margin-left: 15px; width: 185px;',
          helpText : ht.alertEmail,
          name : 'alertEmail',
          width : this.COMBO_WIDTH,
          allowBlank : true
        }, {
          xtype : 'combo',
          fieldLabel : 'Recurrence',
          labelStyle : 'margin-left: 15px; width: 185px;',
          itemCls : 'required-field',
          helpText : ht.serviceSchedule,
          name : 'schedule',
          store : this.scheduleTypeStore,
          displayField : 'value',
          editable : false,
          forceSelection : true,
          mode : 'local',
          triggerAction : 'all',
          emptyText : 'Select...',
          selectOnFocus : true,
          allowBlank : false,
          width : this.COMBO_WIDTH
        }, {
          xtype : 'panel',
          id : 'schedule-config-card-panel',
          header : false,
          layout : 'card',
          region : 'center',
          activeItem : 0,
          bodyStyle : 'padding:15px',
          deferredRender : false,
          autoScroll : false,
          frame : false,
          items : [{
                xtype : 'fieldset',
                checkboxToggle : false,
                title : 'Schedule Settings',
                anchor : Sonatype.view.FIELDSET_OFFSET,
                collapsible : false,
                autoHeight : true,
                labelWidth : 175,
                layoutConfig : {
                  labelSeparator : ''
                },
                items : [{
                      xtype : 'label',
                      text : 'Without recurrence, this service can only be run manually.'
                    }]
              }, {
                xtype : 'fieldset',
                checkboxToggle : false,
                title : 'Schedule Settings',
                anchor : Sonatype.view.FIELDSET_OFFSET,
                collapsible : false,
                autoHeight : true,
                labelWidth : 175,
                layoutConfig : {
                  labelSeparator : ''
                },
                items : [{
                      xtype : 'datefield',
                      fieldLabel : 'Start Date',
                      itemCls : 'required-field',
                      helpText : ht.startDate,
                      name : 'startDate',
                      disabled : true,
                      allowBlank : false,
                      value : new Date()
                    }, {
                      xtype : 'timefield',
                      fieldLabel : 'Start Time',
                      itemCls : 'required-field',
                      afterText : this.timeZone,
                      helpText : ht.startTime,
                      name : 'startTime',
                      width : 75,
                      disabled : true,
                      allowBlank : false,
                      format : 'H:i'
                    }]
              }, {
                xtype : 'fieldset',
                checkboxToggle : false,
                title : 'Schedule Settings',
                anchor : Sonatype.view.FIELDSET_OFFSET,
                collapsible : false,
                autoHeight : true,
                labelWidth : 175,
                layoutConfig : {
                  labelSeparator : ''
                },
                items : [{
                      xtype : 'datefield',
                      fieldLabel : 'Start Date',
                      itemCls : 'required-field',
                      helpText : ht.startDate,
                      name : 'startDate',
                      disabled : true,
                      allowBlank : false,
                      value : new Date()
                    }, {
                      xtype : 'timefield',
                      fieldLabel : 'Start Time',
                      itemCls : 'required-field',
                      afterText : this.timeZone,
                      helpText : ht.startTime,
                      name : 'startTime',
                      width : 75,
                      disabled : true,
                      allowBlank : false,
                      format : 'H:i'
                    }]
              }, {
                xtype : 'fieldset',
                checkboxToggle : false,
                title : 'Schedule Settings',
                anchor : Sonatype.view.FIELDSET_OFFSET,
                collapsible : false,
                autoHeight : true,
                labelWidth : 175,
                layoutConfig : {
                  labelSeparator : ''
                },
                items : [{
                      xtype : 'datefield',
                      fieldLabel : 'Start Date',
                      itemCls : 'required-field',
                      helpText : ht.startDate,
                      name : 'startDate',
                      disabled : true,
                      allowBlank : false,
                      value : new Date()
                    }, {
                      xtype : 'timefield',
                      fieldLabel : 'Recurring Time',
                      itemCls : 'required-field',
                      afterText : this.timeZone,
                      helpText : ht.recurringTime,
                      name : 'recurringTime',
                      width : 75,
                      disabled : true,
                      allowBlank : false,
                      format : 'H:i'
                    }]
              }, {
                xtype : 'fieldset',
                checkboxToggle : false,
                title : 'Schedule Settings',
                anchor : Sonatype.view.FIELDSET_OFFSET,
                collapsible : false,
                autoHeight : true,
                labelWidth : 175,
                layoutConfig : {
                  labelSeparator : ''
                },
                items : [{
                      xtype : 'datefield',
                      fieldLabel : 'Start Date',
                      itemCls : 'required-field',
                      helpText : ht.startDate,
                      name : 'startDate',
                      disabled : true,
                      allowBlank : false,
                      value : new Date()
                    }, {
                      xtype : 'timefield',
                      fieldLabel : 'Recurring Time',
                      itemCls : 'required-field',
                      afterText : this.timeZone,
                      helpText : ht.recurringTime,
                      name : 'recurringTime',
                      width : 75,
                      disabled : true,
                      allowBlank : false,
                      format : 'H:i'
                    }, {
                      xtype : 'twinpanelchooser',
                      titleLeft : 'Selected Days',
                      titleRight : 'Available Days',
                      name : 'weekdays',
                      displayField : 'name',
                      valueField : 'id',
                      store : this.weekdaysStore,
                      required : true,
                      halfSize : true
                    }]
              }, {
                xtype : 'fieldset',
                checkboxToggle : false,
                title : 'Schedule Settings',
                anchor : Sonatype.view.FIELDSET_OFFSET,
                collapsible : false,
                autoHeight : true,
                labelWidth : 175,
                layoutConfig : {
                  labelSeparator : ''
                },
                items : [{
                      xtype : 'datefield',
                      fieldLabel : 'Start Date',
                      itemCls : 'required-field',
                      helpText : ht.startDate,
                      name : 'startDate',
                      disabled : true,
                      allowBlank : false,
                      value : new Date()
                    }, {
                      xtype : 'timefield',
                      fieldLabel : 'Recurring Time',
                      itemCls : 'required-field',
                      afterText : this.timeZone,
                      helpText : ht.recurringTime,
                      name : 'recurringTime',
                      width : 75,
                      disabled : true,
                      allowBlank : false,
                      format : 'H:i'
                    }, {
                      xtype : 'panel',
                      layout : 'column',
                      items : [{
                            width : 180,
                            xtype : 'label',
                            text : 'Days'
                          }, {
                            xtype : 'panel',
                            layout : 'column',
                            items : [{
                                  xtype : 'panel',
                                  width : 50,
                                  items : [{
                                        xtype : 'checkbox',
                                        boxLabel : '1',
                                        name : 'day1'
                                      }, {
                                        xtype : 'checkbox',
                                        boxLabel : '8',
                                        name : 'day8'
                                      }, {
                                        xtype : 'checkbox',
                                        boxLabel : '15',
                                        name : 'day15'
                                      }, {
                                        xtype : 'checkbox',
                                        boxLabel : '22',
                                        name : 'day22'
                                      }, {
                                        xtype : 'checkbox',
                                        boxLabel : '29',
                                        name : 'day29'
                                      }]
                                }, {
                                  xtype : 'panel',
                                  width : 50,
                                  items : [{
                                        xtype : 'checkbox',
                                        boxLabel : '2',
                                        name : 'day2'
                                      }, {
                                        xtype : 'checkbox',
                                        boxLabel : '9',
                                        name : 'day9'
                                      }, {
                                        xtype : 'checkbox',
                                        boxLabel : '16',
                                        name : 'day16'
                                      }, {
                                        xtype : 'checkbox',
                                        boxLabel : '23',
                                        name : 'day23'
                                      }, {
                                        xtype : 'checkbox',
                                        boxLabel : '30',
                                        name : 'day30'
                                      }]
                                }, {
                                  xtype : 'panel',
                                  width : 50,
                                  items : [{
                                        xtype : 'checkbox',
                                        boxLabel : '3',
                                        name : 'day3'
                                      }, {
                                        xtype : 'checkbox',
                                        boxLabel : '10',
                                        name : 'day10'
                                      }, {
                                        xtype : 'checkbox',
                                        boxLabel : '17',
                                        name : 'day17'
                                      }, {
                                        xtype : 'checkbox',
                                        boxLabel : '24',
                                        name : 'day24'
                                      }, {
                                        xtype : 'checkbox',
                                        boxLabel : '31',
                                        name : 'day31'
                                      }]
                                }, {
                                  xtype : 'panel',
                                  width : 50,
                                  items : [{
                                        xtype : 'checkbox',
                                        boxLabel : '4',
                                        name : 'day4'
                                      }, {
                                        xtype : 'checkbox',
                                        boxLabel : '11',
                                        name : 'day11'
                                      }, {
                                        xtype : 'checkbox',
                                        boxLabel : '18',
                                        name : 'day18'
                                      }, {
                                        xtype : 'checkbox',
                                        boxLabel : '25',
                                        name : 'day25'
                                      }, {
                                        xtype : 'checkbox',
                                        boxLabel : 'Last',
                                        name : 'dayLast'
                                      }]
                                }, {
                                  xtype : 'panel',
                                  width : 50,
                                  items : [{
                                        xtype : 'checkbox',
                                        boxLabel : '5',
                                        name : 'day5'
                                      }, {
                                        xtype : 'checkbox',
                                        boxLabel : '12',
                                        name : 'day12'
                                      }, {
                                        xtype : 'checkbox',
                                        boxLabel : '19',
                                        name : 'day19'
                                      }, {
                                        xtype : 'checkbox',
                                        boxLabel : '26',
                                        name : 'day26'
                                      }]
                                }, {
                                  xtype : 'panel',
                                  width : 50,
                                  items : [{
                                        xtype : 'checkbox',
                                        boxLabel : '6',
                                        name : 'day6'
                                      }, {
                                        xtype : 'checkbox',
                                        boxLabel : '13',
                                        name : 'day13'
                                      }, {
                                        xtype : 'checkbox',
                                        boxLabel : '20',
                                        name : 'day20'
                                      }, {
                                        xtype : 'checkbox',
                                        boxLabel : '27',
                                        name : 'day27'
                                      }]
                                }, {
                                  xtype : 'panel',
                                  width : 50,
                                  items : [{
                                        xtype : 'checkbox',
                                        boxLabel : '7',
                                        name : 'day7'
                                      }, {
                                        xtype : 'checkbox',
                                        boxLabel : '14',
                                        name : 'day14'
                                      }, {
                                        xtype : 'checkbox',
                                        boxLabel : '21',
                                        name : 'day21'
                                      }, {
                                        xtype : 'checkbox',
                                        boxLabel : '28',
                                        name : 'day28'
                                      }]
                                }]
                          }]
                    }]
              }, {
                xtype : 'fieldset',
                checkboxToggle : false,
                title : 'Schedule Settings',
                anchor : Sonatype.view.FIELDSET_OFFSET,
                collapsible : false,
                autoHeight : true,
                labelWidth : 175,
                layoutConfig : {
                  labelSeparator : ''
                },
                items : [{
                      xtype : 'textfield',
                      fieldLabel : 'CRON expression',
                      itemCls : 'required-field',
                      name : 'cronCommand',
                      helpText : ht.cronCommand,
                      disabled : true,
                      allowBlank : false,
                      width : this.COMBO_WIDTH
                    }, {
                      xtype : 'panel',
                      layout : 'fit',
                      html : Sonatype.repoServer.resources.help.cronBriefHelp.text
                    }, {
                      xtype : 'button',
                      text : 'Show More Help >>',
                      hideLabel : true,
                      listeners : {
                        click : {
                          fn : function(target, event) {
                            var helpIndex = target.ownerCt.items.indexOf(target);
                            var helpPanel = target.ownerCt.items.itemAt(helpIndex + 1);
                            if (helpPanel.collapsed)
                            {
                              helpPanel.expand(false);
                              target.setText("Hide Extra Help <<");
                            }
                            else
                            {
                              helpPanel.collapse(false);
                              target.setText("Show More Help >>");
                            }
                          },
                          scope : this
                        }
                      }
                    }, {
                      xtype : 'panel',
                      collapsible : true,
                      collapsed : true,
                      layout : 'fit',
                      html : Sonatype.repoServer.resources.help.cronBigHelp.text
                    }]
              }]
        }],
    buttons : [{
          text : 'Save',
          disabled : true
        }, {
          text : 'Cancel'
        }]
  };

  this.sp = Sonatype.lib.Permissions;

  this.schedulesGridPanel = new Ext.grid.GridPanel({
        id : 'st-schedules-grid',
        region : 'north',
        layout : 'fit',
        split : true,
        height : 200,
        minHeight : 150,
        maxHeight : 400,
        frame : false,
        autoScroll : true,
        tbar : [{
              id : 'schedule-refresh-btn',
              text : 'Refresh',
              iconCls : 'st-icon-refresh',
              cls : 'x-btn-text-icon',
              scope : this,
              handler : this.reloadAll
            }, {
              id : 'schedule-add-btn',
              text : 'Add',
              icon : Sonatype.config.resourcePath + '/static/images/icons/add.png',
              cls : 'x-btn-text-icon',
              scope : this,
              handler : this.addResourceHandler,
              disabled : !this.sp.checkPermission('nexus:tasks', this.sp.CREATE)
            },
            this.runButton,
            this.stopButton,
            this.deleteButton
        ],

        // grid view options
        ds : this.schedulesDataStore,
        sortInfo : {
          field : 'name',
          direction : "ASC"
        },
        loadMask : true,
        deferredRender : false,
        columns : [{
              header : 'Enabled',
              dataIndex : 'enabled',
              width : 50,
              id : 'schedule-config-enabled-col'
            }, {
              header : 'Name',
              dataIndex : 'name',
              width : 175,
              id : 'schedule-config-name-col'
            }, {
              header : 'Type',
              dataIndex : 'typeName',
              width : 175,
              id : 'schedule-config-service-type-col'
            }, {
              header : 'Status',
              dataIndex : 'readableStatus',
              width : 100,
              id : 'schedule-config-service-status-col'
            }, {
              header : 'Schedule',
              dataIndex : 'schedule',
              width : 100,
              id : 'schedule-config-service-schedule-col'
            }, {
              header : 'Next Run',
              dataIndex : 'nextRunTimeInMillis',
              width : 250,
              id : 'schedule-config-service-next-run-col'
            }, {
              header : 'Last Run',
              dataIndex : 'lastRunTimeInMillis',
              width : 250,
              id : 'schedule-config-service-last-run-col'
            }, {
              header : 'Last Result',
              dataIndex : 'lastRunResult',
              width : 175,
              id : 'schedule-config-service-last-result-col'
            }],
        autoExpandColumn : 'schedule-config-service-last-result-col',
        disableSelection : false,
        viewConfig : {
          deferEmptyText: false,
          emptyText: 'No scheduled tasks defined',
          emptyTextWhileFiltering: 'No scheduled tasks matched criteria: {criteria}'
        }
      });
  this.schedulesGridPanel.getSelectionModel().on('rowselect', this.rowSelect, this);

  this.schedulesGridPanel.getTopToolbar().add([
    '->',
    NX.create('Nexus.ext.GridFilterBox', {
      filteredGrid: this.schedulesGridPanel
    })
  ]);

  Sonatype.repoServer.SchedulesEditPanel.superclass.constructor.call(this, {
        layout : 'border',
        autoScroll : false,
        width : '100%',
        height : '100%',
        items : [this.schedulesGridPanel, {
              xtype : 'panel',
              id : 'schedule-config-forms',
              title : 'Scheduled Task Configuration',
              layout : 'card',
              region : 'center',
              activeItem : 0,
              deferredRender : false,
              autoScroll : false,
              frame : false,
              items : [{
                    xtype : 'panel',
                    layout : 'fit',
                    html : '<div class="little-padding">Select a scheduled task to edit it, or click "Add" to create a new one.</div>'
                  }]
            }]
      });

  this.formCards = this.findById('schedule-config-forms');

  this.on('render', this.initializeCustomTypes, this);
};

Ext.extend(Sonatype.repoServer.SchedulesEditPanel, Ext.Panel, {
      initializeCustomTypes : function() {
        Sonatype.Events.fireEvent('initializeCustomTypes', this.customTypes);
      },

      // Dump the currently stored data and requery for everything
      reloadAll : function() {
        var gridSelectModel = this.schedulesGridPanel.getSelectionModel();
        gridSelectModel.clearSelections();
        this.formCards.getLayout().setActiveItem(0);

        this.runButton.disable();
        this.stopButton.disable();
        this.deleteButton.disable();

        this.schedulesDataStore.removeAll();
        this.schedulesDataStore.reload();
        this.repoOrGroupDataStore.removeAll();
        this.repositoryDataStore.reload();
        this.repositoryTargetDataStore.reload();
        this.repositoryGroupDataStore.reload();
        this.serviceTypeDataStore.reload();
        this.formCards.items.each(function(item, i, len) {
              if (i > 0)
              {
                this.remove(item, true);
              }
            }, this.formCards);

        // //Enable add button on refresh
        // this.schedulesGridPanel.getTopToolbar().items.get('schedule-add-btn').enable();
      },

      markTreeInvalid : function(tree) {
        var elp = tree.getEl();

        if (!tree.errorEl)
        {
          tree.errorEl = elp.createChild({
                cls : 'x-form-invalid-msg'
              });
          tree.errorEl.setWidth(elp.getWidth(true)); // note removed -20 like
          // on form fields
        }
        tree.invalid = true;
        tree.errorEl.update(tree.invalidText);
        elp.child('.x-panel-body').setStyle({
              'background-color' : '#fee',
              border : '1px solid #dd7870'
            });
        Ext.form.Field.msgFx['normal'].show(tree.errorEl, tree);
      },

      saveHandler : function(formInfoObj) {
        var allValid = false;
        allValid = formInfoObj.formPanel.form.isValid();

        // form validation for weekdays tree, only when it's shown
        var scheduleWeekConfig = Ext.getCmp(formInfoObj.formPanel.id + '_schedule-config-card-panel').items.itemAt(4);

        if (scheduleWeekConfig.isVisible())
        {
          allValid = (allValid && formInfoObj.formPanel.find('name', 'weekdays')[0].validate());
        }

        if (allValid)
        {
          var isNew = formInfoObj.isNew;
          var serviceSchedule = formInfoObj.formPanel.find('name', 'schedule')[0].getValue().toLowerCase();
          var createUri = Sonatype.config.repos.urls.schedules;
          var updateUri = (formInfoObj.resourceURI) ? formInfoObj.resourceURI : '';
          var form = formInfoObj.formPanel.form;

          form.doAction('sonatypeSubmit', {
            method : (isNew) ? 'POST' : 'PUT',
            url : isNew ? createUri : updateUri,
            waitMsg : isNew ? 'Creating scheduled task...' : 'Updating scheduled task configuration...',
            fpanel : formInfoObj.formPanel,
            dataModifiers : this.submitDataModFuncs[serviceSchedule],
            serviceDataObj : Sonatype.repoServer.referenceData.schedule[serviceSchedule],
            isNew : isNew,
            scope : this,
            success : function() {
              if (this.sp.checkPermission('nexus:tasksrun', this.sp.READ))
              {
                this.runButton.enable();
              }
              else
              {
                this.runButton.disable();
              }
              this.stopButton.disable();
            }
              // extra option to send to callback, instead of conditioning on
              // method
            });
        }
      },

      cancelHandler : function(formInfoObj) {
        var formLayout = this.formCards.getLayout();
        var gridSelectModel = this.schedulesGridPanel.getSelectionModel();
        var store = this.schedulesGridPanel.getStore();

        this.formCards.remove(formInfoObj.formPanel.id, true);

        if (this.formCards.items.length > 1)
        {
          formLayout.setActiveItem(this.formCards.items.length - 1);
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
          formLayout.setActiveItem(0);
          gridSelectModel.clearSelections();
        }

        // delete row from grid if cancelling a new repo form
        if (formInfoObj.isNew)
        {
          store.remove(store.getById(formInfoObj.formPanel.id));
        }
      },

      addResourceHandler : function() {
        // first disable the add button, at least until
        // save/cancel/delete/refresh
        // this.schedulesGridPanel.getTopToolbar().items.get('schedule-add-btn').disable();
        var id = 'new_schedule_' + new Date().getTime();

        var config = Ext.apply({}, this.formConfig.schedule, {
              id : id
            });
        config = this.configUniqueIdHelper(id, config);
        Ext.apply(config.items[4].items, FormFieldGenerator(id, 'Task Settings', 'serviceProperties_', this.serviceTypeDataStore, this.repositoryDataStore, this.repositoryGroupDataStore, this.repoOrGroupDataStore, this.customTypes, this.COMBO_WIDTH, this.repositoryTargetDataStore));
        var formPanel = new Ext.FormPanel(config);

        formPanel.form.on('actioncomplete', this.actionCompleteHandler, this);
        formPanel.form.on('actionfailed', this.actionFailedHandler, this);
        formPanel.on('afterlayout', this.afterLayoutFormHandler, this, {
              single : true
            });

        var serviceTypeField = formPanel.find('name', 'typeId')[0];
        serviceTypeField.on('select', this.serviceTypeSelectHandler, formPanel);

        var serviceScheduleField = formPanel.find('name', 'schedule')[0];
        serviceScheduleField.on('select', this.serviceScheduleSelectHandler, formPanel);

        var buttonInfoObj = {
          formPanel : formPanel,
          isNew : true
        };

        // save button event handler
        formPanel.buttons[0].on('click', this.saveHandler.createDelegate(this, [buttonInfoObj]));
        // cancel button event handler
        formPanel.buttons[1].on('click', this.cancelHandler.createDelegate(this, [buttonInfoObj]));

        var sp = Sonatype.lib.Permissions;
        if (sp.checkPermission('nexus:tasks', sp.EDIT))
        {
          formPanel.buttons[0].disabled = false;
        }

        this.importRecurringDayHelper([], {}, formPanel);

        // add new form
        this.formCards.add(formPanel);

        // add place holder to grid
        var newRec = new this.scheduleRecordConstructor({
              name : 'New Scheduled Task',
              resourceURI : 'new'
            }, id); // use "new_schedule_" id instead of resourceURI like the
        // reader does
        this.schedulesDataStore.insert(0, [newRec]);
        this.schedulesGridPanel.getSelectionModel().selectRow(0);
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
        if (this.ctxRecord || this.schedulesGridPanel.getSelectionModel().hasSelection())
        {
          var rec = this.ctxRecord ? this.ctxRecord : this.schedulesGridPanel.getSelectionModel().getSelected();

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
                  animEl : this.schedulesGridPanel.getEl(),
                  title : 'Delete Scheduled Task?',
                  msg : 'Delete the ' + rec.get('name') + ' scheduled task?',
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
          this.reloadAll();
        }
        else
        {
          Sonatype.utils.connectionError(response, 'The server did not delete the task.', null, null, true);
        }
      },

      stopHandler : function() {
        if (this.ctxRecord || this.schedulesGridPanel.getSelectionModel().hasSelection())
        {
          var rec = this.ctxRecord ? this.ctxRecord : this.schedulesGridPanel.getSelectionModel().getSelected();

          Sonatype.MessageBox.show({
                animEl : this.schedulesGridPanel.getEl(),
                title : 'Cancel Scheduled Task?',
                msg : 'Cancel the ' + rec.get('name') + ' scheduled task?',
                buttons : Sonatype.MessageBox.YESNO,
                scope : this,
                icon : Sonatype.MessageBox.QUESTION,
                fn : function(btnName) {
                  if (btnName == 'yes' || btnName == 'ok')
                  {
                    Ext.Ajax.request({
                          callback : this.stopCallback,
                          cbPassThru : {
                            resourceId : rec.id
                          },
                          scope : this,
                          method : 'DELETE',
                          url : Sonatype.config.repos.urls.scheduleRun + '/' + rec.data.id + '?cancelOnly=true'
                        });
                  }
                }
              });
        }
      },

      stopCallback : function(options, isSuccess, response) {
        if (isSuccess)
        {
          this.reloadAll();
        }
        else
        {
          Sonatype.utils.connectionError(response, 'The server did not delete the task.', null, null, true);
        }
      },

      runHandler : function() {
        if (this.ctxRecord && this.ctxRecord.data.resourceURI != 'new')
        {
          var rec = this.ctxRecord;
          Sonatype.MessageBox.getDialog().on('show', function() {
                this.focusEl = this.buttons[2]; // ack! we're offset dependent
                // here
                this.focus();
              }, Sonatype.MessageBox.getDialog(), {
                single : true
              });

          Sonatype.MessageBox.show({
                animEl : this.schedulesGridPanel.getEl(),
                title : 'Run Scheduled Task?',
                msg : 'Run the ' + rec.get('name') + ' scheduled task?',
                buttons : Sonatype.MessageBox.YESNO,
                scope : this,
                icon : Sonatype.MessageBox.QUESTION,
                fn : function(btnName) {
                  if (btnName == 'yes' || btnName == 'ok')
                  {
                    this.alreadyDeferred = false;
                    Ext.Ajax.request({
                          callback : this.runCallback,
                          cbPassThru : {
                            resourceId : rec.id
                          },
                          scope : this,
                          method : 'GET',
                          url : Sonatype.config.repos.urls.scheduleRun + '/' + rec.data.id
                        });
                  }
                }
              });
        }
      },

      runCallback : function(options, isSuccess, response) {
        if (!isSuccess)
        {
          Sonatype.utils.connectionError(response, 'The server did not run the scheduled task.');
        }
        else if (!this.alreadyDeferred)
        {
          var receivedData = Ext.decode(response.responseText).data;
          var i = this.schedulesDataStore.indexOfId(options.cbPassThru.resourceId);
          var rec = this.schedulesDataStore.getAt(i);

          this.updateServiceRecord(rec, receivedData);

          this.schedulesDataStore.commitChanges();

          var sortState = this.schedulesDataStore.getSortState();
          this.schedulesDataStore.sort(sortState.field, sortState.direction);
          this.alreadyDeferred = true;
          this.runCallback.defer(500, this, [options, isSuccess, response]);
        }
        else
        {
          this.reloadAll();
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
            // successful create
            var sentData = action.output.data;

            var dataObj = {
              name : receivedData.resource.name,
              enabled : receivedData.resource.enabled,
              id : receivedData.resource.id,
              resourceURI : receivedData.resourceURI,
              typeName : this.serviceTypeDataStore.getAt(this.serviceTypeDataStore.findBy(function(record, id) {
                    if (record.data.id == receivedData.resource.typeId)
                    {
                      return true;
                    }
                    return false;
                  })).data.name,
              typeId : receivedData.resource.typeId,
              readableStatus : receivedData.readableStatus,
              alertEmail : receivedData.resource.alertEmail,
              schedule : receivedData.resource.schedule,
              nextRunTimeInMillis : receivedData.nextRunTimeInMillis?new Date(receivedData.nextRunTimeInMillis):'n/a',
              lastRunTimeInMillis : receivedData.lastRunTimeInMillis?new Date(receivedData.lastRunTimeInMillis):'n/a',
              lastRunResult : receivedData.lastRunResult
            };

            var newRec = new this.scheduleRecordConstructor(dataObj, action.options.fpanel.id);

            this.schedulesDataStore.remove(this.schedulesDataStore.getById(action.options.fpanel.id)); // remove
            // old
            // one
            this.schedulesDataStore.addSorted(newRec);
            this.schedulesGridPanel.getSelectionModel().selectRecords([newRec], false);

            // set the hidden id field in the form for subsequent updates
            action.options.fpanel.find('name', 'id')[0].setValue(receivedData.resourceURI);
            // remove button click listeners
            action.options.fpanel.buttons[0].purgeListeners();
            action.options.fpanel.buttons[1].purgeListeners();

            var buttonInfoObj = {
              formPanel : action.options.fpanel,
              isNew : false,
              resourceURI : dataObj.resourceURI
            };

            if (dataObj.schedule == 'once')
            {
              action.options.fpanel.buttons[0].disable();
            }
            else
            {
              // save button event handler
              action.options.fpanel.buttons[0].on('click', this.saveHandler.createDelegate(this, [buttonInfoObj]));
            }

            // cancel button event handler
            action.options.fpanel.buttons[1].on('click', this.cancelHandler.createDelegate(this, [buttonInfoObj]));

            // disable the service type, only avaiable on add
            action.options.fpanel.find('name', 'typeId')[0].disable();

            // //Enable add button on save complete
            // this.schedulesGridPanel.getTopToolbar().items.get('schedule-add-btn').enable();
          }
          else
          {
            var sentData = action.output.data;

            var i = this.schedulesDataStore.indexOfId(action.options.fpanel.id);
            var rec = this.schedulesDataStore.getAt(i);

            this.updateServiceRecord(rec, receivedData);

            var sortState = this.schedulesDataStore.getSortState();
            this.schedulesDataStore.sort(sortState.field, sortState.direction);
          }
          
        // NEXUS-4365: after a save action, panel state is broken: fields are enabled but empty, combo box values are missing.
        // -> rebuild panel after save action
        var formLayout = this.formCards.getLayout();
        var fp = action.options.fpanel;
        this.formCards.remove(fp.id, true);

        // switch to empty
        formLayout.setActiveItem(0);
        
        var store = this.schedulesGridPanel.getStore();
        this.schedulesGridPanel.getSelectionModel().selectRow(store.indexOfId(fp.id));
        // END NEXUS-4365
    }
    else if ( action.type == 'sonatypeLoad' )
    {
        // NEXUS-4363 disable the south panel after service-type-config-card values are loaded
        var formPanel = action.options.fpanel;
        var readableStatus = formPanel.readableStatus;
        if (!readableStatus || readableStatus == 'Waiting' || readableStatus == '')
        {
        //this.disableEditingHeader.setVisible(false);
        // only layout if change is needed
        if ( formPanel.disabled )
        {
            formPanel.enable();
            formPanel.doLayout();
        }
        }
        else if (!formPanel.disabled)
        {
        //this.disableEditingHeader.setVisible(true);
        formPanel.disable();
        formPanel.doLayout();
        }
        }
      },

      updateServiceRecord : function(rec, receivedData) {
        rec.beginEdit();
        rec.set('name', receivedData.resource.name);
        rec.set('enabled', receivedData.resource.enabled);
        rec.set('typeId', receivedData.resource.typeId);
        rec.set('typeName', this.serviceTypeDataStore.getAt(this.serviceTypeDataStore.findBy(function(record, id) {
                  if (record.data.id == receivedData.resource.typeId)
                  {
                    return true;
                  }
                  return false;
                })).data.name);
        rec.set('alertEmail', receivedData.resource.alertEmail);
        rec.set('schedule', receivedData.resource.schedule);
        rec.set('readableStatus', receivedData.readableStatus);
        rec.set('nextRunTimeInMillis', receivedData.nextRunTimeInMillis?new Date(receivedData.nextRunTimeInMillis):'n/a');
        rec.set('lastRunTimeInMillis', receivedData.lastRunTimeInMillis?new Date(receivedData.lastRunTimeInMillis):'n/a');
        rec.set('lastRunResult', receivedData.lastRunResult);
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
          if (action.response.responseText.indexOf("There is no task with ID=") > -1 )
          {
            Sonatype.MessageBox.alert('Selected task was removed', 'The selected task was removed by another process and cannot be displayed.');
            this.reloadAll();
          }
          else
          {
            Sonatype.utils.connectionError(action.response, 'There is an error communicating with the server.');
          }
        }
        else if (action.failureType == Ext.form.Action.LOAD_FAILURE)
        {
          Sonatype.MessageBox.alert('Load Failure', 'The data failed to load from the server.').setIcon(Sonatype.MessageBox.ERROR);
        }

        // @todo: need global alert mechanism for fatal errors.
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
        this.ctxRow = this.schedulesGridPanel.view.getRow(index);
        this.ctxRecord = this.schedulesGridPanel.store.getAt(index);

        this.runButton.disable();
        this.stopButton.disable();
        if (this.sp.checkPermission('nexus:tasks', this.sp.DELETE))
        {
            this.deleteButton.enable();
        }
        else
        {
            this.deleteButton.disable();
        }

        var readableStatus = rec.data.readableStatus;

        if (rec.data.name.substring(0, 4) == 'New ')
        {
          this.runButton.disable();
          this.stopButton.disable();
        }
        else if (readableStatus == 'Cancelling')
        {
          this.stopButton.disable();
          this.runButton.disable();
          this.deleteButton.disable();
        }
        else if (readableStatus == 'Waiting')
        {
          if (this.sp.checkPermission('nexus:tasksrun', this.sp.READ))
          {
            this.runButton.enable();
          }
          else
          {
            this.runButton.disable();
          }
          this.stopButton.disable();
        }
        else
        {
          if (this.sp.checkPermission('nexus:tasksrun', this.sp.READ))
          {
            this.stopButton.enable();
          }
          else
          {
            this.stopButton.disable();
          }
          this.runButton.disable();
        }

        var id = rec.id; // note: rec.id is unique for new resources and equal
        // to resourceURI for existing ones
        var formPanel = this.formCards.findById(id);
        var schedulePanel = null;

        // assumption: new route forms already exist in formCards, so they won't
        // get into this case
        if (!formPanel)
        { // create form and populate current data
          var config = Ext.apply({}, this.formConfig.schedule, {
                id : id
              });
          config = this.configUniqueIdHelper(id, config);
          Ext.apply(config.items[4].items, FormFieldGenerator(id, 'Task Settings', 'serviceProperties_', this.serviceTypeDataStore, this.repositoryDataStore, this.repositoryGroupDataStore, this.repoOrGroupDataStore, this.customTypes, this.COMBO_WIDTH, this.repositoryTargetDataStore));
          formPanel = new Ext.FormPanel(config);

          formPanel.form.on('actioncomplete', this.actionCompleteHandler, this);
          formPanel.form.on('actionfailed', this.actionFailedHandler, this);
          formPanel.on('afterlayout', this.afterLayoutFormHandler, this, {
                single : true
              });

          // On load need to make sure and set the proper schedule type card as
          // active
          schedulePanel = formPanel.findById(formPanel.id + '_schedule-config-card-panel');
          if (rec.data.schedule == 'once')
          {
            schedulePanel.activeItem = 1;
          }
          else if (rec.data.schedule == 'hourly')
          {
            schedulePanel.activeItem = 2;
          }
          else if (rec.data.schedule == 'daily')
          {
            schedulePanel.activeItem = 3;
          }
          else if (rec.data.schedule == 'weekly')
          {
            schedulePanel.activeItem = 4;
          }
          else if (rec.data.schedule == 'monthly')
          {
            schedulePanel.activeItem = 5;
          }
          else if (rec.data.schedule == 'advanced')
          {
            schedulePanel.activeItem = 6;
          }
          // Then enable each field in the active card (after this point, select
          // handler takes care of all
          // enable/disable transitions
          if (schedulePanel.items.items[schedulePanel.activeItem].items)
          {
            schedulePanel.items.items[schedulePanel.activeItem].items.each(function(item) {
                  item.disabled = false;
                });
          }

          // Need to do the same w/ service type and make sure the correct card
          // is active. Dynamic cards, so little more generic
          var serviceTypePanel = formPanel.findById(formPanel.id + '_service-type-config-card-panel');
          serviceTypePanel.hide();
          serviceTypePanel.items.each(function(item, i, len) {
                if (item.id === id + '_' + rec.data.typeId)
                {
                  serviceTypePanel.activeItem = i;
                  if (item.items && item.items.length !== 0)
                  {
                    serviceTypePanel.show();
                    item.items.each(function(item) {
                          item.disabled = false;
                        });
                  }
                }
              });

          formPanel.find('name', 'typeId')[0].disable();
          formPanel.find('name', 'schedule')[0].on('select', this.serviceScheduleSelectHandler, formPanel);

          var buttonInfoObj = {
            formPanel : formPanel,
            isNew : false, // not a new route form, see assumption
            resourceURI : rec.data.resourceURI
          };

          formPanel.buttons[0].on('click', this.saveHandler.createDelegate(this, [buttonInfoObj]));
          formPanel.buttons[1].on('click', this.cancelHandler.createDelegate(this, [buttonInfoObj]));

          var sp = Sonatype.lib.Permissions;
          if (sp.checkPermission('nexus:tasks', sp.EDIT) && rec.data.schedule != 'internal')
          {
            formPanel.buttons[0].disabled = false;
          }

          this.importRecurringDayHelper([], {}, formPanel);

          this.formDataLoader(formPanel, rec.data.resourceURI, this.loadDataModFuncs[rec.data.schedule]);

          this.formCards.add(formPanel);
        }
        // save readable status
        formPanel.readableStatus = readableStatus;

        // always set active
        this.formCards.getLayout().setActiveItem(formPanel);
        formPanel.doLayout();
      },

      serviceTypeSelectHandler : function(combo, record, index) {
        var serviceTypePanel = this.findById(this.id + '_service-type-config-card-panel');
        // First disable all the items currently on screen, so they wont be
        // validated/submitted etc
        serviceTypePanel.getLayout().activeItem.items.each(function(item) {
              item.disable();
            });
        // Then find the proper card to activate (based upon id of the
        // serviceType)
        // Then enable the fields in that card
        var formId = this.id;
        serviceTypePanel.items.each(function(item, i, len) {
              if (item.id === formId + '_' + record.data.id)
              {
                serviceTypePanel.getLayout().setActiveItem(item);
                if (item.items.length === 0) {
                  serviceTypePanel.hide();
                } else  {
                  serviceTypePanel.show();
                  item.items.each(function(item) {
                    item.enable();
                  });
                }
              }
            }, serviceTypePanel);

        serviceTypePanel.doLayout();
      },

      serviceScheduleSelectHandler : function(combo, record, index) {
        var schedulePanel = this.findById(this.id + '_schedule-config-card-panel');
        // First disable all the items currently on screen, so they wont be
        // validated/submitted etc
        schedulePanel.getLayout().activeItem.items.each(function(item) {
              item.disable();
            });
        // Then find the proper card to activate (based upon the selected
        // schedule type)
        if (record.data.value == 'Once')
        {
          schedulePanel.getLayout().setActiveItem(schedulePanel.items.itemAt(1));
        }
        else if (record.data.value == 'Hourly')
        {
          schedulePanel.getLayout().setActiveItem(schedulePanel.items.itemAt(2));
        }
        else if (record.data.value == 'Daily')
        {
          schedulePanel.getLayout().setActiveItem(schedulePanel.items.itemAt(3));
        }
        else if (record.data.value == 'Weekly')
        {
          schedulePanel.getLayout().setActiveItem(schedulePanel.items.itemAt(4));
        }
        else if (record.data.value == 'Monthly')
        {
          schedulePanel.getLayout().setActiveItem(schedulePanel.items.itemAt(5));
        }
        else if (record.data.value == 'Advanced')
        {
          schedulePanel.getLayout().setActiveItem(schedulePanel.items.itemAt(6));
        }
        else
        {
          schedulePanel.getLayout().setActiveItem(schedulePanel.items.itemAt(0));
        }
        // Then enable the fields in that card
        schedulePanel.getLayout().activeItem.items.each(function(item) {
              item.enable();
              if (item.xtype == 'datefield')
              {
                item.setValue(item.value);
              }
              else if (item.xtype == 'timefield')
              {
                if (item.value == null)
                {
                  item.setValue(new Date().format('H:i'));
                }
                else
                {
                  item.setValue(item.value);
                }
              }
            });
        schedulePanel.doLayout();
      },

      // creates a unique config object with specific IDs on the two grid item
      configUniqueIdHelper : function(id, config) {
        // @note: there has to be a better way to do this. Depending on offsets
        // is very error prone
        var newConfig = config;

        this.assignItemIds(id, newConfig.items);

        return newConfig;
      },

      assignItemIds : function(id, items) {
        for (var i = 0; i < items.length; i++)
        {
          var item = items[i];
          if (item.id)
          {
            if (!item.originalId)
            {
              item.originalId = item.id;
            }
            item.id = id + '_' + item.originalId;
          }
          if (item.items)
          {
            this.assignItemIds(id, item.items);
          }
        }
      },

      exportStartDateHelper : function(val, fpanel) {
        // as there is no Date transfer in json, i pass the long representation
        // of the date.
        var selectedStartDate = "";

        var startDateFields = fpanel.find('name', 'startDate');
        // Need to find the startDate that is currently enabled, as multiple
        // cards will have a startDate field
        for (var i = 0; i < startDateFields.length; i++)
        {
          if (!startDateFields[i].disabled)
          {
            selectedStartDate = startDateFields[i];
            break;
          }
        }

        return '' + selectedStartDate.getValue().getTime();
      },
      exportStartTimeHelper : function(val, fpanel) {
        var selectedStartTime = "";

        var startTimeFields = fpanel.find('name', 'startTime');
        // Need to find the startTime that is currently enabled, as multiple
        // cards will have a startDate field
        for (var i = 0; i < startTimeFields.length; i++)
        {
          if (!startTimeFields[i].disabled)
          {
            selectedStartTime = startTimeFields[i];
            break;
          }
        }

        // rest api is using 24 hour clock
        var hours = selectedStartTime.getValue().substring(0, selectedStartTime.getValue().indexOf(':'));
        var minutes = selectedStartTime.getValue().substring(selectedStartTime.getValue().indexOf(':') + 1, selectedStartTime.getValue().indexOf(':') + 3);

        return hours + ':' + minutes;
      },
      exportRecurringTimeHelper : function(val, fpanel) {
        var selectedRecurringTime = "";

        var recurringTimeFields = fpanel.find('name', 'recurringTime');
        // Need to find the recurringTime that is currently enabled, as multiple
        // cards will have a startDate field
        for (var i = 0; i < recurringTimeFields.length; i++)
        {
          if (!recurringTimeFields[i].disabled)
          {
            selectedRecurringTime = recurringTimeFields[i];
            break;
          }
        }

        // rest api is using 24 hour clock
        var hours = parseInt(selectedRecurringTime.getValue().substring(0, selectedRecurringTime.getValue().indexOf(':')), 10);
        var minutes = selectedRecurringTime.getValue().substring(selectedRecurringTime.getValue().indexOf(':') + 1, selectedRecurringTime.getValue().indexOf(':') + 3);

        return hours + ':' + minutes;
      },
      exportRecurringDayHelper : function(val, fpanel) {
        var weekdaysBox = fpanel.find('name', 'weekdays')[0];
        var weekdaysIds = weekdaysBox.getValue();

        var response = [];
        for (var i = 0; i < weekdaysIds.length; i++)
        {
          response[i] = Strings.lowercase(weekdaysIds[i]);
        }

        return response;
      },
      exportMonthlyRecurringDayHelper : function(val, fpanel) {
        var outputArr = [];
        var j = 0;
        // Another pretty simple conversion, just find all selected check boxes
        // and send along the day number
        for (var i = 1; i <= 31; i++)
        {
          if (fpanel.find('name', 'day' + i)[0].getValue())
          {
            outputArr[j++] = '' + i;
          }
        }
        // and last if necessary
        if (fpanel.find('name', 'dayLast')[0].getValue())
        {
          outputArr[j] = 'last';
        }
        return outputArr;
      },
      exportServicePropertiesHelper : function(val, fpanel) {
        return FormFieldExporter(fpanel, '_service-type-config-card-panel', 'serviceProperties_', this.customTypes);
      },
      importStartDateHelper : function(val, srcObj, fpanel) {
        var selectedStartDate = "";

        var startDateFields = fpanel.find('name', 'startDate');
        // Find the correct startDate field, as their will be multiples, all but
        // 1 disabled
        for (var i = 0; i < startDateFields.length; i++)
        {
          if (!startDateFields[i].disabled)
          {
            selectedStartDate = startDateFields[i];
            break;
          }
        }

        // translate the long representation back into date
        var importedDate = new Date(Number(val));
        selectedStartDate.setValue(importedDate);
        return importedDate;
      },
      importStartTimeHelper : function(val, srcObj, fpanel) {
        var selectedStartTime = "";

        var startTimeFields = fpanel.find('name', 'startTime');
        // Find the correct startTime field, as their will be multiples, all but
        // 1 disabled
        for (var i = 0; i < startTimeFields.length; i++)
        {
          if (!startTimeFields[i].disabled)
          {
            selectedStartTime = startTimeFields[i];
            break;
          }
        }

        var startDate = new Date(Number(srcObj.startDate));
        var hours = startDate.getHours();
        var minutes = startDate.getMinutes();

        var importedTime = (hours<10?'0':'') + hours + ':' + (minutes<10?'0':'') + minutes;
        selectedStartTime.setValue(importedTime);
        return importedTime;
      },
      importRecurringTimeHelper : function(val, srcObj, fpanel) {
        var selectedRecurringTime = "";

        var recurringTimeFields = fpanel.find('name', 'recurringTime');
        // Find the correct recurringTime field, as their will be multiples, all
        // but 1 disabled
        for (var i = 0; i < recurringTimeFields.length; i++)
        {
          if (!recurringTimeFields[i].disabled)
          {
            selectedRecurringTime = recurringTimeFields[i];
            break;
          }
        }

        var startDate = new Date(Number(srcObj.startDate));
        var hours = startDate.getHours();
        var minutes = startDate.getMinutes();

        var importedTime = (hours<10?'0':'') + hours + ':' + (minutes<10?'0':'') + minutes;
        selectedRecurringTime.setValue(importedTime);
        return importedTime;
      },
      importRecurringDayHelper : function(arr, srcObj, fpanel) {
        var weekdayBox = fpanel.find('name', 'weekdays')[0];
        weekdayBox.setValue(arr);

        return arr; // return arr, even if empty to comply with sonatypeLoad
        // data modifier requirement
      },
      importMonthlyRecurringDayHelper : function(arr, srcObj, fpanel) {
        // simply look at each item, and select the proper checkbox
        for (var i = 0; i < arr.length; i++)
        {
          var checkbox = fpanel.find('name', 'day' + arr[i])[0];
          if (checkbox == null)
          {
            checkbox = fpanel.find('name', 'dayLast')[0];
          }
          checkbox.setValue(true);
        }

        return arr;
      },
      importServicePropertiesHelper : function(val, srcObj, fpanel) {
        FormFieldImporter(srcObj, fpanel, 'serviceProperties_', this.customTypes);
        return val;
      }
    });

});

