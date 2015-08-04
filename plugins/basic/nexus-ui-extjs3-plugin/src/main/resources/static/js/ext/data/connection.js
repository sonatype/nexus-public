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
/*global require*/
define('ext/data/connection',['extjs'], function(Ext) {
Ext.override(Ext.data.Connection, {
  extendFormUploadURL : function(url) {
    // hack for IE: if a non success response is received, we can't
    // access the response data, because an error page is loaded from local disc
    // for most response codes, making the containing iframe protected because it has
    // a different domain (CORS)
    if (Ext.isIE) {
      if (url.indexOf('?') >= 0) {
        url += '&forceSuccess=true';
      }
      else {
        url += '?forceSuccess=true';
      }
    }
    return url;
  },
  doFormUploadOrig : Ext.data.Connection.prototype.doFormUpload,
  doFormUpload : function(o, ps, url) {
    this.doFormUploadOrig.call(this, o, ps, this.extendFormUploadURL(url));
  }
});
});
