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

  emptyText: NX.I18n.get('Search_TextSearchCriteria_Filter_EmptyText'),

  padding: '0 5 0 0',
  width: 100,
  labelAlign: 'top',
  labelSeparator: '',

  filter: function() {
    var me = this;
    if (me.value) {
      return { property: me.criteriaId, value: me.value };
    }
    return undefined;
  },

  initComponent: function() {
    var me = this;

    if (me.removable) {
      me.trigger2Cls = 'nx-form-fa-minus-circle-trigger';
    }

    me.callParent(arguments);

    me.addEvents(
        /**
         * @event criteriaremoved
         * Fires when search criteria is removed.
         */
        'criteriaremoved'
    );
  },

  /**
   * @private
   * Fire event about user removing the search criteria.
   */
  onTrigger2Click: function() {
    var me = this;

    me.fireEvent('criteriaremoved', me);
  }

});
