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
/*global define*/

define('Nexus/ext/FormPanel',['extjs', 'sonatype', 'nexus', 'Nexus/messagebox'], function(Ext, Sonatype, Nexus, mbox) {

Ext.namespace('Nexus.ext');

/*
 * Generic object editor (intended to be subclassed). When used with
 * Sonatype.panels.GridViewer, instanced of this editor panel will never be
 * reused. When the form is submitted, all child panels related to this grid
 * record are re-created, so the editor does not have to worry about altering
 * its state after submit (as opposed to having to disable certain fields after
 * the new record is saved, which we had to do previously). Config options:
 * cancelButton: if set to "true", the form will display a "Cancel" button, so
 * the invoker can subscribe to a "cancel" event and do the necessary cleanup
 * (e.g. close the panel). By default, the form will display a "Reset" button
 * instead, which reloads the form when clicked. Note a special dataModifier of
 * 'rootData' will work with entire contents of json response rather than on a
 * field by field basis. Using this modifier will then render other modifiers
 * useless dataModifiers: { // data modifiers on form submit/load load: { attr1:
 * func1, attr2: func2 }, save: { ... } } dataStores: an array of data stores
 * this editor depends on. It will make sure all stores are loaded before the
 * form load request is sent. The stores should be configured with auto load
 * off. listeners: { // custom events offered by the editor panel cancel: { fn:
 * function( panel ) { // do cleanup, remove the form from the container, //
 * delete the temporary grid record, etc. }, scope: this }, load: { fn:
 * function( form, action, receivedData ) { // do extra work for data load if
 * needed }, scope: this }, submit: { fn: function( form, action, receivedData ) { //
 * update the grid record and do other stuff if needed var rec = this.payload;
 * rec.beginEdit(); rec.set( 'attr1', receivedData.attr1 ); rec.set( 'attr2',
 * receivedData.attr2 ); rec.commit(); rec.endEdit(); }, scope: this } }
 * payload: the grid record being edited referenceData: a reference data object
 * that's used as a template on form submit uri: base URL for JSON requests. It
 * will be used for POST requests when creating a new object, or for PUT (with
 * payload.id appended) if the record does not a resourceURI attribute
 */
Nexus.ext.FormPanel = function(cfg) {
  var
        config = cfg || {},
        defaultConfig = {
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
          }
        };

  Ext.apply(this, config, defaultConfig);

  this.checkPayload();
  if (this.isNew && (this.cancelButton === null || this.cancelButton === undefined))
  {
    this.cancelButton = true;
  }

  Nexus.ext.FormPanel.superclass.constructor.call(this, {
    buttons : (config.readOnly || this.readOnly) ? [] : [{
      text : 'Save',
      handler : this.saveHandler,
      scope : this
    }, {
      handler : this.cancelButton ? this.cancelHandler : this.resetHandler,
      scope : this,
      text : this.cancelButton ? 'Cancel' : 'Reset'
    }]
  });

  this.on('afterlayout', this.initData, this, {
    single : true
  });
  this.on('afterlayout', this.registerRequiredQuicktips, this, {
    single : true
  });
  this.form.on('actioncomplete', this.actionCompleteHandler, this);
  this.form.on('actionfailed', this.actionFailedHandler, this);

  this.addEvents({
    cancel : true,
    load : true,
    submit : true
  });
};

Ext.extend(Nexus.ext.FormPanel, Ext.FormPanel, {
  convertDataValue : function(value, store, idProperty, nameProperty) {
    if (value)
    {
      var rec = store.getAt(store.find(idProperty, value));
      if (rec)
      {
        return rec.data[nameProperty];
      }
    }
    return '';
  },
  checkPayload : function() {
    this.isNew = false;
    if (this.payload)
    {
      if (this.payload.id && this.payload.id.substring(0, 4) === 'new_')
      {
        this.isNew = true;
      }
    }
  },

  checkStores : function() {
    var i, store;
    if (this.dataStores)
    {
      for (i = 0; i < this.dataStores.length; i=i+1)
      {
        store = this.dataStores[i];
        if (!store.lastOptions)
        {
          return false;
        }
      }
    }
    return true;
  },

  dataStoreLoadHandler : function(store, records, options) {
    if (this.checkStores())
    {
      this.loadData();
    }
  },

  registerRequiredQuicktips : function(formPanel, fLayout) {
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

  cancelHandler : function(button, event) {
    this.fireEvent('cancel', this);
  },

  resetHandler : function(button, event) {
    this.loadData();
  },

  initData : function() {
    var i, store;

    if (this.dataStores)
    {
      for (i = 0; i < this.dataStores.length; i=i+1)
      {
        store = this.dataStores[i];
        store.on('load', this.dataStoreLoadHandler, this);
        if (store.autoLoad !== true)
        {
          store.load();
        }
      }
    }
    else
    {
      this.loadData();
    }
  },

  loadData : function() {
    if (this.isNew)
    {
      this.form.reset();
    }
    else
    {
      this.form.doAction('sonatypeLoad', {
        url : this.getActionURL(),
        method : 'GET',
        fpanel : this,
        dataModifiers : this.dataModifiers ? this.dataModifiers.load : {},
        scope : this
      });
    }
  },

  isValid : function() {
    return this.form.isValid();
  },

  saveHandler : function(button, event) {
    if (this.isValid())
    {
      this.form.doAction('sonatypeSubmit', {
        method : this.getSaveMethod(),
        url : this.getActionURL(),
        waitMsg : this.isNew ? 'Creating a new record...' : 'Updating records...',
        fpanel : this,
        validationModifiers : this.validationModifiers,
        dataModifiers : this.dataModifiers ? this.dataModifiers.submit : {},
        serviceDataObj : this.referenceData,
        isNew : this.isNew
        // extra option to send to callback, instead of conditioning on
        // method
      });
    }
  },

  actionFailedHandler : function(form, action) {
    if (action.failureType === Ext.form.Action.CLIENT_INVALID)
    {
      mbox.alert('Missing or Invalid Fields', 'Please change the missing or invalid fields.').setIcon(mbox.WARNING);
    }
    else if (action.failureType === Ext.form.Action.CONNECT_FAILURE || action.response)
    {
      Sonatype.utils.connectionError(action.response, 'There is an error communicating with the server.');
    }
    else if (action.failureType === Ext.form.Action.LOAD_FAILURE)
    {
      mbox.alert('Load Failure', 'The data failed to load from the server.').setIcon(Nexus.MessageBox.ERROR);
    }
  },

  // (Ext.form.BasicForm, Ext.form.Action)
  actionCompleteHandler : function(form, action) {
    var receivedData = action.handleResponse(action.response).data, i, r, rec, store;
    if (!receivedData)
    {
      receivedData = {};
    }

    if (action.type === 'sonatypeSubmit')
    {
      this.fireEvent('submit', form, action, receivedData);

      if (this.isNew && this.payload.autoCreateNewRecord)
      {
        store = this.payload.store;
        store.remove(this.payload);

        if (Ext.isArray(receivedData))
        {
          for (i = 0; i < receivedData.length; i=i+1)
          {
            r = receivedData[i];
            rec = new store.reader.recordType(r, r.resourceURI);
            this.addSorted(store, rec);
          }
        }
        else
        {
          rec = new store.reader.recordType(receivedData, receivedData.resourceURI);
          rec.autoCreateNewRecord = true;
          this.addSorted(store, rec);
        }
      }
      this.isNew = false;
      this.payload.autoCreateNewRecord = false;
    }
    else if (action.type === 'sonatypeLoad')
    {
      this.fireEvent('load', form, action, receivedData);
    }
  },

  addSorted : function(store, rec) {
    store.addSorted(rec);
  },

  getActionURL : function() {
    var uri;
    if (this.isNew) {
      uri = this.uri;
    } else if (this.payload.data && this.payload.data.resourceURI) {
      // if resourceURI is supplied, return it
      uri = this.payload.data.resourceURI;
    } else {
      // otherwise construct a uri
      uri = this.uri + '/' + this.payload.id;
    }

    return uri;
  },

  getSaveMethod : function() {
    return this.isNew ? 'POST' : 'PUT';
  },

  optionalFieldsetExpandHandler : function(panel) {
    panel.items.each(function(item, i, len) {
      if (item.rendered && item.getEl().up('div.required-field', 3))
      {
        item.allowBlank = false;
      }
      else if (item.isXType('fieldset', true))
      {
        this.optionalFieldsetExpandHandler(item);
      }
    }, this);
  },

  optionalFieldsetCollapseHandler : function(panel) {
    panel.items.each(function(item, i, len) {
      if (item.rendered && item.getEl().up('div.required-field', 3))
      {
        item.allowBlank = true;
      }
      else if (item.isXType('fieldset', true))
      {
        this.optionalFieldsetCollapseHandler(item);
      }
    }, this);
  }
});

Sonatype.ext.FormPanel = Nexus.ext.FormPanel;

return Nexus.ext.FormPanel;

});

