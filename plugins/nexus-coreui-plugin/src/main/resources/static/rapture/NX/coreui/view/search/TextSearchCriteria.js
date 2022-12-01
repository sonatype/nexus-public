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
/*global Ext, NX*/

/**
 * Generic text search criteria.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.search.TextSearchCriteria', {
  extend: 'NX.ext.SearchBox',
  alias: 'widget.nx-coreui-searchcriteria-text',
  requires: [
    'NX.I18n'
  ],
  mixins: {
    searchCriteria: 'NX.coreui.view.search.SearchCriteria'
  },

  /**
   * @cfg [removable=false] If search criteria should be removable.
   */
  removable: false,

  // Width must be defined on the class, otherwise overrides won’t be applied correctly
  width: 100,

  triggers: {
    remove: {
      cls:  'nx-form-fa-minus-circle-trigger',
      handler: function() {
        var me = this;
        // Fire event about user removing the search criteria.
        me.fireEvent('criteriaremoved', me);
      }
    }
  },

  filter: function() {
    var me = this;
    if (me.value) {
      return { property: me.criteriaId, value: me.value };
    }
    return undefined;
  },

  /**
   * @override
   */
  initComponent: function() {
    var me = this;

    me.emptyText = NX.I18n.get('Search_TextSearchCriteria_Filter_EmptyText');
    me.padding = '0 5 0 0';
    me.labelAlign = 'top';
    me.labelSeparator = '';

    me.triggers.remove.setHidden(!me.removable);

    me.callParent();
  },

  onValueChange: function(trigger, value) {
    this.setValue(value);
    this.resetOriginalValue();
  }

});
