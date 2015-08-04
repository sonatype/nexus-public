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
define('Nexus/form/action',['extjs', 'nexus', 'Nexus/messagebox', 'Sonatype/utils'], function(Ext, Nexus, messagebox, Sonatype) {

Ext.namespace('Nexus.form');

/**
 * @class Ext.form.Action.sonatypeSubmit
 * @extends Ext.form.Action A custom sonatype form serializer that submits JSON
 *          text to the Sonatype service and processes the returned response.
 *          ToDo: Define the error response format and variations. How to expose
 *          this to callbacks and event handlers? Other data may be placed into
 *          the response for processing the the Ext.form.BasicForm's callback or
 *          event handler methods. The object decoded from this JSON is
 *          available in the result property. Note: no form.errorReader
 *          accepted. JSON error format (when defined) is the only format
 *          accepted. Additional option params: serviceDataObj: reference object
 *          that matches the service's data object for this action fpanel:
 *          FormPanel dataModifiers: optional functions to modify or collect
 *          data values Additional values: after submit the output object is
 *          available on action.output. This is the native object that was
 *          serialized and sent to the server.
 */
Ext.form.Action.sonatypeSubmit = function(form, options) {
  if (options.autoValidation === undefined)
  {
    options.autoValidation = true;
  }
  Ext.form.Action.sonatypeSubmit.superclass.constructor.call(this, form, options);
};

  function setUnpacked(obj, flatName, value) {
    var ctx = obj, id = flatName;

    Ext.each( flatName.split('.'), function(part) {
      ctx = obj;
      id = part;
      obj = obj[part];
    });

    ctx[id] = value;
  }


Ext.extend(Ext.form.Action.sonatypeSubmit, Ext.form.Action, {
  /**
   * @cfg {boolean} clientValidation Determines whether a Form's fields are
   *      validated in a final call to
   *      {@link Ext.form.BasicForm#isValid isValid} prior to submission. Pass
   *      <tt>false</tt> in the Form's submit options to prevent this. If not
   *      defined, pre-submission field validation is performed.
   */
  type : 'sonatypeSubmit',

  // private
  run : function() {
    var
          sJsonOutput,
          o = this.options,
          method = this.getMethod(),
          isPost = method === 'POST';

    if (o.clientValidation === false || this.form.isValid())
    {
      sJsonOutput = this.serializeForm(this.options.fpanel, this.form);

      Ext.Ajax.request(Ext.apply(this.createCallback(o), {
        jsonData : sJsonOutput,
        url : this.getUrl(!isPost),
        method : method,
        params : isPost ? this.getParams() : null,
        isUpload : this.form.fileUpload
      }));
    }
    else if (o.clientValidation !== false)
    { // client validation failed
      this.failureType = Ext.form.Action.CLIENT_INVALID;
      this.form.afterAction(this, false);
    }
  },

  // override connection failure because server validation errors come back with
  // 400 code
  failure : function(response) {
    this.response = response;
    if (response.status === 400 && this.options.autoValidation)
    { // validation error
      this.success(response);
      return;
    }

    this.failureType = Ext.form.Action.CONNECT_FAILURE;
    this.form.afterAction(this, false);
  },

  // private
  success : function(response) {
    var
          i, errorObj,
          result = this.processResponse(response),
          remainingErrors = [];

    // if a 204 response, we arent looking at errors, it should go through ok
    if (result === true || result.data || response.status === 200 || response.status === 204 ||
      // 1223 is the IE way to deal with 204 http://goo.gl/xzJt1
          response.status === 1223)
    {
      this.form.afterAction(this, true);
      return;
    }

    var contentType = response.getResponseHeader("Content-Type");

    // response will be a json serialized org.sonatype.sisu.siesta.common.error.ErrorXO
    if( contentType === "application/vnd.siesta-error-v1+json") {
      result = { errors: [{ id:'*', msg : result.message }]};
    }

    // response will be a json serialized array of org.sonatype.sisu.siesta.common.validation.ValidationErrorXO
    // convert error message field (ExtJs expects a field named "msg")
    if( contentType === "application/vnd.siesta-validation-errors-v1+json") {
        result = { errors : result };
        for (i = 0; i < result.errors.length; i=i+1) {
          result.errors[i].msg = result.errors[i].message;
        }
    }

    if (result.errors !== null && result.errors !== undefined)
    {
      if (this.options.validationModifiers)
      {
        for (i = 0; i < result.errors.length; i=i+1)
        {
          if (this.options.validationModifiers[result.errors[i].id])
          {
            if (typeof(this.options.validationModifiers[result.errors[i].id]) === 'function')
            {
              (this.options.validationModifiers[result.errors[i].id])(result.errors[i], this.options.fpanel);
            }
            else
            {
              errorObj = result.errors[i];
              errorObj.id = this.options.validationModifiers[result.errors[i].id];
              remainingErrors[remainingErrors.length] = errorObj;
            }
          }
          else
          {
            remainingErrors[remainingErrors.length] = result.errors[i];
          }
        }

        result.errors = remainingErrors;
      }

      if (result.errors.length === 1 && result.errors[0].id === '*')
      {
        messagebox.show({
          title : 'Configuration Error',
          msg : result.errors[0].msg,
          buttons : messagebox.OK,
          icon : messagebox.ERROR
        });
        return;
      }

      this.form.markInvalid(result.errors);
      this.failureType = Ext.form.Action.SERVER_INVALID;
    }
    this.form.afterAction(this, false);

    // if we came in here on 400 error, and we properly marked field as error,
    // hide dialog
    if (result.errors)
    {
      messagebox.hide();
    }
  },

  // private
  handleResponse : function(response) {
    try
    {
      return Ext.decode(response.responseText); // throws SyntaxError
    }
    catch (e)
    {
      return false;
    }
  },

  // private
  serializeForm : function(fpanel, form) {
    // note: srcObj (form.sonatypeLoadedData) is not modified only walked
    var
          output = Sonatype.utils.cloneObj(this.options.serviceDataObj),
          resultOutput = this.serializeFormHelper(fpanel, output, this.options.serviceDataObj, '');

    if(this.options.noEnvelope){
      this.output = resultOutput || output;
    } else {
      this.output = {
        "data" : resultOutput || output
      };
    }

    Nexus.Log.debug(this.options.method + ' ' + this.options.url + ' ', this.output);

    return Ext.encode(this.output);
  },

  // serializeHelper(object fpanel, object accObj, object srcObj, string
  // sPrepend, [string sVal])
  // Leave off sVal arg to call on root data obj
  // Walks the data object sent from the server originally, and plucks field
  // values from form
  // applying modifier functions if specified. Handles collapsed fleldsets by
  // returning null
  // as object value to server.
  // Invariant: srcObj is not modified! If this changes, call with a cloned copy
  serializeFormHelper : function(fpanel, accObj, srcObj, sPrepend, sVal) {
    var i, value, nextPrepend, fieldValue, fieldSet, flatName, field, fieldId, obj, context;
    if (sVal)
    { // non-root case
      nextPrepend = sPrepend + sVal + '.';
      value = srcObj[sVal]; // @todo: "value" name here is whack because, it's
      // not the field value
    }
    else
    { // root case
      nextPrepend = sPrepend;
      value = srcObj;
    }

    obj = accObj;
    context = accObj;

    // this function goes through flatName and 'unpacks' the objects in the path
    // and sets the value
    // was written as "eval('accObj.' + flatName + '=' + fieldValue)" before
    if (this.options.dataModifiers && this.options.dataModifiers.rootData)
    {
      return (this.options.dataModifiers.rootData)(fieldValue, fpanel);
    }
    else if (Ext.type(value) === 'object')
    {
      if (sVal)
      { // only write object serialization for non-root objects
        fieldSet = Ext.value(
              Ext.getCmp(fpanel.id + '_' + sPrepend + sVal),
              fpanel.find('name', 'fieldset_' + sPrepend + sVal)[0]
        );

        if (fieldSet && fieldSet.collapsed)
        {
          setUnpacked(accObj, sPrepend + sVal, null);
          return; // skip recursive calls for children form items
        }
      }

      for (i in value)
      {
        if ( value.hasOwnProperty(i)) {
          this.serializeFormHelper(fpanel, accObj, value, nextPrepend, i);
        }
      }
    }
    else
    { // only non-root case should ever get in here
      flatName = sPrepend + sVal;
      field = fpanel.form.findField(flatName);

      if (field && !Ext.isEmpty(field.getValue(), false))
      { // getValue normalizes undefined to '', but it's still false
        fieldValue = field.getValue();
      }

      // data mod function gets the field value if the field exists. o/w null is
      // passed
      fieldValue = (this.options.dataModifiers && this.options.dataModifiers[flatName]) ?
            (this.options.dataModifiers[flatName])(fieldValue, fpanel) :
            fieldValue;

      setUnpacked(accObj, flatName, fieldValue);
    }
  }

  // @note: this was going to be used for processing hierarchical error
  // messaging
  // // walks the error response and flattens each field into a flat object by
  // fieldname
  // flattenErrors : function(accObj, srcObj, sPrepend, sVal){
  // var value, nextPrepend;
  // if (sVal){ //non-root case
  // nextPrepend = sPrepend + sVal + '.';
  // value = srcObj[sVal];
  // }
  // else { //root case
  // nextPrepend = sPrepend;
  // value = srcObj;
  // }
  //
  // if (Ext.type(value) === 'object'){
  // for (var i in value){
  // this.flattenErrors(fpanel, accObj, value, nextPrepend, i);
  // }
  // }
  // else { //only non-root case should ever get in here
  // var flatName = sPrepend + sVal;
  // if(!Ext.isEmpty(value, true)){
  // accObj[flatName] = value;
  // }
  // }
  // }
});

/**
 * @class Ext.form.Action.sonatypeLoad
 * @extends Ext.form.Action A class which handles loading of data from Sonatype
 *          service into the Fields of an Ext.form.BasicForm. Expected repsonse
 *          format { data: { clientName: "Fred. Olsen Lines", portOfLoading:
 *          "FXT", portOfDischarge: "OSL" } } Other data may be placed into the
 *          response for processing the Ext.form.BasicForm Form's callback or
 *          event handler methods. The object decoded from this JSON is
 *          available in the result property. Needed Improvements: create a
 *          standard way to for callbacks to assess server respone's success.
 *          The regular loader gave access to this in the data.success field
 *          that it required in its data format. We could access
 *          this.response.HTTPcode (?). Should we push that down in an acessible
 *          way, so every callback doesn't need to understand our service
 *          response codes. Notes No form.reader may be used here. JSON data
 *          format is assumed this loader Additional options params: fpanel the
 *          FromPanel containing this form dataModifiers (optional)
 */
Ext.form.Action.sonatypeLoad = function(form, options) {
  Ext.form.Action.sonatypeLoad.superclass.constructor.call(this, form, options);
};

Ext.extend(Ext.form.Action.sonatypeLoad, Ext.form.Action, {
  // private
  type : 'sonatypeLoad',

  // private
  run : function() {
    Ext.Ajax.request(Ext.apply(this.createCallback(this.options), {
      method : this.getMethod(),
      url : this.getUrl(false),
      params : this.getParams(),
      suppressStatus : this.options.suppressStatus
    }));
  },

  // private
  // note: service response object "data" value expected here in result.data
  success : function(response) {
    var flatData, result = this.processResponse(response);

    data = result.data;
    if(this.options.noEnvelope){
      data = result;
    }
    if (result === true || !data)
    {
      this.failureType = Ext.form.Action.LOAD_FAILURE;
      this.form.afterAction(this, false);
      return;
    }
    this.form.clearInvalid();
    flatData = this.translateDataToFieldValues(this.options.fpanel, data);
    this.form.setValues(flatData);
    this.form.afterAction(this, true);
  },

  // private
  // called from in Ext.form.Action.processResponse
  handleResponse : function(response) {
    return Ext.decode(response.responseText);
  },

  // private
  // takes result.data and returns flattened data to pass to
  // this.form.setValues()
  translateDataToFieldValues : function(fpanel, data) {
    var flat = {};
    this.translateHelper(fpanel, flat, data, '');
    return flat;
  },

  // translateHelper(object accObj, object srcObj, string sPrepend, [string
  // sVal])
  // Leave off sVal arg to call on root data obj
  translateHelper : function(fpanel, accObj, srcObj, sPrepend, sVal) {
    var value, nextPrepend, hasNonEmptyChildren = false, i, thisChildNotEmpty, fieldSet, flatName;
    if (sVal)
    { // non-root case
      nextPrepend = sPrepend + sVal + '.';
      value = srcObj[sVal];
    }
    else
    { // root case
      nextPrepend = sPrepend;
      value = srcObj;
    }

    if (this.options.dataModifiers && this.options.dataModifiers.rootData)
    {
      accObj = this.options.dataModifiers.rootData(value, srcObj, fpanel);
    }
    else if (Ext.type(value) === 'object')
    {
      for (i in value)
      {
        if ( value.hasOwnProperty(i)) {
          thisChildNotEmpty = this.translateHelper(fpanel, accObj, value, nextPrepend, i);
          hasNonEmptyChildren = hasNonEmptyChildren || thisChildNotEmpty;
        }
      }

      if (sVal) { // only write object serialization for non-root objects
        if (hasNonEmptyChildren)
        {
          fieldSet = Ext.getCmp(fpanel.id + '_' + sPrepend + sVal);
          if (!fieldSet)
          {
            fieldSet = fpanel.find('name', 'fieldset_' + sPrepend + sVal)[0];
          }
          if (fieldSet)
          {
	      if ( fieldSet.rendered ) {
		  fieldSet.expand();
	      } else {
		  fieldSet.collapsed = false;
	      }
          }
        }
        accObj['.' + sPrepend + sVal] = hasNonEmptyChildren;
      }
      return hasNonEmptyChildren;
    }
    else
    { // only non-root case should ever get in here
      flatName = sPrepend + sVal;
      // note: all vaues passed to modifier funcs, even if the value is
      // undefined, null, or empty!
      // Modifier funcs should ALWAYS return a value, even if it's
      // unmodified.
      value = (this.options.dataModifiers && this.options.dataModifiers[flatName]) ? this.options.dataModifiers[flatName](value, srcObj, fpanel) : value;
      if (Ext.isEmpty(value, true))
      {
        return false;
      }

      // flatName is correct here, because we're selecting field ids with it
      accObj[flatName] = value;
      return true;
    }
  }
});

Ext.form.Action.ACTION_TYPES.sonatypeLoad = Ext.form.Action.sonatypeLoad;
Ext.form.Action.ACTION_TYPES.sonatypeSubmit = Ext.form.Action.sonatypeSubmit;

});
