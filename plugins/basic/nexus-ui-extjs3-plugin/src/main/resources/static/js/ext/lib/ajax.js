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
define('ext/lib/ajax',['extjs'], function(Ext){
Ext.lib.Ajax.setHeader = function(o) {
  var combinedHeaders, prop;

  // Sonatype: Safari and IE don't always overwrite headers correctly, so need
  // to merge default and provide headers before writing
  if (this.hasDefaultHeaders && this.hasHeaders)
  {
    combinedHeaders = Ext.applyIf(this.headers, this.defaultHeaders);

    for (prop in combinedHeaders)
    {
      if (combinedHeaders.hasOwnProperty(prop))
      {
        o.conn.setRequestHeader(prop, combinedHeaders[prop]);
      }
    }

    this.headers = {};
    this.hasHeaders = false;
  }
  else if (this.hasDefaultHeaders)
  {
    for (prop in this.defaultHeaders)
    {
      if (this.defaultHeaders.hasOwnProperty(prop))
      {
        o.conn.setRequestHeader(prop, this.defaultHeaders[prop]);
      }
    }
  }
  else if (this.hasHeaders)
  {
    for (prop in this.headers)
    {
      if (this.headers.hasOwnProperty(prop))
      {
        o.conn.setRequestHeader(prop, this.headers[prop]);
      }
    }
    this.headers = {};
    this.hasHeaders = false;
  }
};

Ext.lib.Ajax.handleTransactionResponse = function(o, callback, isAbort) {
  if (!callback)
  {
    this.releaseObject(o);
    return;
  }

  var httpStatus, responseObject;

  try
  {
    if (o.conn.status !== undefined && o.conn.status !== 0)
    {
      httpStatus = o.conn.status;
    }
    else
    {
      httpStatus = 13030;
    }
  }
  catch (e)
  {

    httpStatus = 13030;
  }

  if ((httpStatus >= 200 && httpStatus < 300) || httpStatus === 1223)
  {
    responseObject = this.createResponseObject(o, callback.argument);
    if (callback.success)
    {
      if (!callback.scope)
      {
        callback.success(responseObject);
      }
      else
      {

        callback.success.apply(callback.scope, [responseObject]);
      }
    }
  } else {
    switch (httpStatus) {
      case 12002 :
      case 12029 :
      case 12030 :
      case 12031 :
      case 12152 :
      case 13030 :
        responseObject = this.createExceptionObject(o.tId, callback.argument, (isAbort || false));
        if (callback.failure)
        {
          if (!callback.scope)
          {
            callback.failure(responseObject);
          }
          else
          {
            callback.failure.apply(callback.scope, [responseObject]);
          }
        }
        break;
      default :
        responseObject = this.createResponseObject(o, callback.argument);
        if (callback.failure)
        {
          if (!callback.scope)
          {
            callback.failure(responseObject);
          }
          else
          {
            callback.failure.apply(callback.scope, [responseObject]);
          }
        }
    }
  }

  this.releaseObject(o);
  responseObject = null;
};
});
