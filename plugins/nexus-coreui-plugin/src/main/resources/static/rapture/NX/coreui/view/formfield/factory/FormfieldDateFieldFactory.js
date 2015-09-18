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
/*global Ext, NX*/

/*global NX*/

/**
 * 'datefield' factory.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.formfield.factory.FormfieldDateFieldFactory', {
  singleton: true,
  alias: [
    'nx.formfield.factory.datefield',
    'nx.formfield.factory.date'
  ],

  /**
   * Creates a datefield.
   * @param formField form field to create datefield for
   * @returns {*} created datefield (never null)
   */
  create: function (formField) {
    var item = {
      xtype: 'datefield',
      htmlDecode: true,
      fieldLabel: formField.label,
      itemCls: formField.required ? 'required-field' : '',
      helpText: formField.helpText,
      allowBlank: !formField.required,
      regex: formField.regexValidation ? new RegExp(formField.regexValidation) : null,
      value: new Date()
    };
    if (formField.initialValue) {
      item.value = new Date(Number(formField.initialValue));
    }
    return item;
  }

});