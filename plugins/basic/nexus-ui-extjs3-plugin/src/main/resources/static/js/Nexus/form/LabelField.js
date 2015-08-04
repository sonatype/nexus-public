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
define('Nexus/form/LabelField',['extjs', 'nexus'], function(Ext, Nexus) {
Ext.namespace('Nexus.form');

/**
 * A field using a label to display the value.
 */
Nexus.form.LabelField = Ext.extend( Ext.form.Field, {
  onRender : function(ct, position){
    this.defaultAutoCreate = {
      tag : 'label',
      // a label inside a form element inherits 'clear: left' -> broken layout
      style : 'clear: none;',
      html : this.value
    };
    Nexus.form.LabelField.superclass.onRender.call(this, ct, position);
  },

  // overriding value getter and setter to make this behave more like a field
  // (a label's el.dom has no 'value' attribute which is what the superclass tries
  // to manipulate)
  getValue : function() {
    return this.getRawValue();
  },
  setValue : function(value) {
    this.setRawValue(value);
  },
  getRawValue : function() {
    return this.value;
  },
  setRawValue : function(value) {
    this.value = value;
    if (this.rendered) {
      this.el.dom.innerHTML = this.value;
    }
  }
});

Ext.reg('labelfield', Nexus.form.LabelField);
});

