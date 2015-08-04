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

define('ext/form/displayfield',['extjs'], function(Ext) {
Ext.form.TimestampDisplayField = Ext.extend(Ext.form.DisplayField, {
  setValue : function(v) {
    if (typeof v !== 'number') {
      this.setRawValue(v);
      return this;
    }

    // java give the timestamp in miliseconds, extjs consumes it in seconds
    var toSecs = Math.round(v / 1000);
    v = new Date.parseDate(toSecs, 'U').toString();
    this.setRawValue(v);
    return this;
  }
});

Ext.reg('timestampDisplayField', Ext.form.TimestampDisplayField);

Ext.form.ByteDisplayField = Ext.extend(Ext.form.DisplayField, {
  setValue : function(v) {
    if (typeof v !== 'number') {
      this.setRawValue(v);
      return this;
    }

    if (v < 1024)
    {
      v = v + ' Bytes';
    }
    else if (v < 1048576)
    {
      v = (v / 1024).toFixed(2) + ' KB';
    }
    else if (v < 1073741824)
    {
      v = (v / 1048576).toFixed(2) + ' MB';
    }
    else
    {
      v = (v / 1073741824).toFixed(2) + ' GB';
    }
    this.setRawValue(v);
    return this;
  }
});

Ext.reg('byteDisplayField', Ext.form.ByteDisplayField);

});
