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
define('ext/form/basicform',['extjs'], function(Ext) {
Ext.override(Ext.form.BasicForm, {
  clearInvalid : function() {
    // same as before, but ignore items without clearInvalid (== non-form-items)
    this.items.each(function(f) {
      if (f.clearInvalid) {
        f.clearInvalid();
      }
    });
  },
  /**
   * Override findField to look for enabled field and return that, otherwise
   * return first found
   */
  findField : function(id) {
    var
          findMatchingField,
          field = this.items.get(id),
          fallbackField = null;

    if (!Ext.isObject(field)) {
      //searches for the field corresponding to the given id. Used recursively for composite fields
      findMatchingField = function(f) {
        if (f.isFormField) {
          if (f.dataIndex === id || f.id === id || f.getName() === id) {
            fallbackField = f;
            if (!f.disabled) {
              field = f;
              return false;
            }
          } else if (f.isComposite) {
            return f.items.each(findMatchingField);
          } else if (f instanceof Ext.form.CheckboxGroup && f.rendered) {
            return f.eachItem(findMatchingField);
          }
        }
      };

      this.items.each(findMatchingField);
    }
    return field || fallbackField;
  }
});
});
