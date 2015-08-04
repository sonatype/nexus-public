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
define('ext/form/TextField',['extjs', 'Nexus/util'], function(Ext, Nexus) {
Ext.override(Ext.form.TextField, {
  /**
   * @cfg {Boolean} htmlDecode
   * <tt>true</tt> to decode html entities in the value given to
   * Ext.form.TextField.setValue and Ext.form.TextField.setRawValue
   * before setting the actual value.
   * <p/>
   * This is needed for displaying the 'literal' value in the text field when it was received by the server,
   * for example in the repository name. The REST layer will encode to html entities, which will be correct
   * for html rendering, but text fields without this configuration will display '&quot;test&quot;' instead
   * of the originally sent '"test"'.
   */
  htmlDecode : false,

  /**
   * @cfg {Boolean} htmlConvert
   * <tt>true</tt> to decode html entities in the value given to
   * Ext.form.TextField.set(Raw)Value
   * before setting the actual value, and encode html entities again
   * in the call to Ext.form.TextField.get(Raw)Value.
   * <p/>
   * This is needed for displaying the 'literal' value in the text field when it was received by the server
   * (see htmlDecode configuration doc), and display to the user correctly before round-tripping to the server again
   * (e.g. in a grid field).
   * <p/>
   * when this config is set, the value has to be html-decoded again before sending it to the server, because the REST layer
   * will encode the string again.
   * <p/>
   * Default value is false.
   */
  htmlConvert : false,

  setRawValueOrig : Ext.form.TextField.prototype.setRawValue,
  setValueOrig : Ext.form.TextField.prototype.setValue,
  getRawValueOrig : Ext.form.TextField.prototype.getRawValue,
  getValueOrig : Ext.form.TextField.prototype.getValue,

  setRawValue : function(value) {
    if ( this.htmlDecode || this.htmlConvert )
    {
      value = Nexus.util.Format.htmlDecode(value);
    }
    this.setRawValueOrig(value);
  },
  setValue : function(value) {
    if ( this.htmlDecode || this.htmlConvert )
    {
      value = Nexus.util.Format.htmlDecode(value);
    }
    this.setValueOrig(value);
  },
  getRawValue : function() {
    var value = this.getRawValueOrig();
    if ( this.htmlConvert )
    {
      value = Ext.util.Format.htmlEncode(value);
    }
    return value;
  },
  getValue : function() {
    var value = this.getValueOrig();
    if ( this.htmlConvert )
    {
      value = Ext.util.Format.htmlEncode(value);
    }
    return value;
  }
});
});

