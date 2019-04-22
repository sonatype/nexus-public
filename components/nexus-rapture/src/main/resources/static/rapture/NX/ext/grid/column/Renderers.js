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
/*global Ext*/

/**
 * Renderer helpers.
 *
 * @since 3.0
 */
Ext.define('NX.ext.grid.column.Renderers', {
  requires: [
    'NX.I18n'
  ],
  singleton: true,

  /**
   * Renderer which will use no-data glyph if given value is undefined or null.
   */
  optionalData: function(value) {
    return value ?
        Ext.htmlEncode(value) :
        '<span class="x-fa fa-ban" style="opacity: 0.33;" aria-label="' + Ext.util.Format.htmlEncode(NX.I18n.get('Column_No_Data')) + '"/>';
  },

  /**
   * Renderer which will display either 'Allowed' or 'Blocked' with a corresponding glyph depending on the value
   * and will dim the text if the rule is not defined in the record.
   */
  allowedBlocked: function(value, column, record) {
    var maybeDim = (record.get('rule') ? '' : ' opacity: 0.33;') + '"/> ';
    return value ?
        '<span class="x-fa fa-check-circle" style="color: #1C8145;' + maybeDim + Ext.util.Format.htmlEncode(NX.I18n.get('RoutingRules_GlobalRoutingPreview_Grid_Allowed')) :
        '<span class="x-fa fa-ban" style="color: #C70000;' + maybeDim + Ext.util.Format.htmlEncode(NX.I18n.get('RoutingRules_GlobalRoutingPreview_Grid_Blocked'));
  },

  /**
   * Renderer which will display either the rule defined or a dimmed 'None' with a no-data glyph.
   */
  optionalRule: function(value) {
    return value ?
        Ext.htmlEncode(value) :
        '<span class="x-fa fa-ban" style="opacity: 0.33;" /> ' + Ext.util.Format.htmlEncode(NX.I18n.get('RoutingRules_GlobalRoutingPreview_Grid_None'));
  }
});
