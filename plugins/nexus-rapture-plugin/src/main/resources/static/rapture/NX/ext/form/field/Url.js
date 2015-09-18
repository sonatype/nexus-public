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

/**
 * An URL **{@link Ext.form.field.Text}**.
 *
 * @since 3.0
 */
Ext.define('NX.ext.form.field.Url', {
  extend: 'Ext.form.field.Text',
  alias: 'widget.nx-url',
  requires: [ 'NX.util.Validator' ],

  validator: function (value) {
    var valid = NX.util.Validator.isURL(value, {
      protocols: ['http', 'https'],
      require_protocol: true,
      allow_underscores: true
    });
    if (valid) {
      return true;
    }
    return 'This field should be a URL in the format "http:/' + '/www.example.com"';
  },

  useTrustStore: function (field) {
    if (Ext.String.startsWith(field.getValue(), 'https://')) {
      return {
        name: 'useTrustStoreFor' + Ext.String.capitalize(field.getName()),
        url: field
      };
    }
    return undefined;
  }

});
