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
/*global define*/
define('Nexus/configuration/Ajax', ['extjs'], function(Ext) {
  Ext.Ajax.defaultHeaders = {
    'accept' : 'application/json,application/vnd.siesta-error-v1+json,application/vnd.siesta-validation-errors-v1+json',

    // HACK: Setting request header to allow analytics to tell if the request came from the UI or not
    // HACK: This has some issues, will only catch ajax requests, etc... but may be fine for now
    'X-Nexus-UI': 'true'
  };

  Ext.Ajax.on('beforerequest', function (connection, options) {
    if (options.isUpload && options.url && XMLHttpRequest.tokenName) {
      options.url = options.url
          + (options.url.indexOf('?') > 0 ? '&' : '?') + XMLHttpRequest.tokenName + '=' + XMLHttpRequest.tokenValue();
    }
  });

  Ext.Ajax.on('requestexception', function(connection, response) {
    if ( response && Ext.isFunction(response.getResponseHeader) ) { // timeouts/socket closed response does not have this method(?)
      if (XMLHttpRequest.tokenName) {
        var tokenValue = response.getResponseHeader(XMLHttpRequest.tokenName);
        if (tokenValue) {
          XMLHttpRequest.tokenValue = function () {
            return tokenValue;
          };
        }
      }
      var contentType = response.getResponseHeader('Content-Type');
      if ( contentType === 'application/vnd.siesta-error-v1+json') {
        response.siestaError = Ext.decode(response.responseText);
      } else if ( contentType === 'application/vnd.siesta-validation-errors-v1+json') {
        response.siestaValidationError = Ext.decode(response.responseText);
      }
    }
  });

  Ext.Ajax.on('requestcomplete', function (connection, response) {
    if (response && Ext.isFunction(response.getResponseHeader)) {
      if (XMLHttpRequest.tokenName) {
        var tokenValue = response.getResponseHeader(XMLHttpRequest.tokenName);
        if (tokenValue) {
          XMLHttpRequest.tokenValue = function () {
            return tokenValue;
          };
        }
      }
    }
  });

  // Set default HTTP headers
  Ext.lib.Ajax.defaultPostHeader = 'application/json; charset=utf-8';
});