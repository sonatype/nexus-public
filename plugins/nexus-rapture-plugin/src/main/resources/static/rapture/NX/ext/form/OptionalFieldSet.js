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
 * A **{@link Ext.form.FieldSet}** that enable/disable contained items on expand/collapse.
 *
 * @since 3.0
 */
Ext.define('NX.ext.form.OptionalFieldSet', {
  extend: 'Ext.form.FieldSet',
  alias: 'widget.nx-optionalfieldset',
  cls: 'nx-optionalfieldset',

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    me.on('add', me.setupMonitorOnChange, me);

    me.callParent(arguments);

    // When state changes, repeat the evaluation
    me.on('collapse', me.enableContainedItems, me);
    me.on('expand', me.enableContainedItems, me);
    me.on('afterrender', me.enableContainedItems, me);
  },

  /**
   * @private
   */
  enableContainedItems: function (container, enable) {
    var me = this;

    if (!Ext.isDefined(enable)) {
      enable = !container.collapsed;
    }

    if (container.items) {
      container.items.each(function (item) {
        if (enable) {
          if (!item.disabledOnCollapse && !item.isXType('container')) {
            item.enable();
          }
          delete item.disabledOnCollapse;
          if (item.isXType('nx-optionalfieldset')) {
            if (item.collapsedOnCollapse === false) {
              item.expand();
            }
            delete item.collapsedOnCollapse;
          }
        }
        else {
          if (!Ext.isDefined(item.disabledOnCollapse)) {
            item.disabledOnCollapse = item.isDisabled();
          }
          if (!item.isXType('container')) {
            item.disable();
          }
          if (item.isXType('nx-optionalfieldset')) {
            if (!Ext.isDefined(item.collapsedOnCollapse)) {
              item.collapsedOnCollapse = item.collapsed;
            }
            item.collapse();
          }
        }
        if (!item.isXType('nx-optionalfieldset')) {
          me.enableContainedItems(item, enable);
        }
        if (Ext.isFunction(item.validate)) {
          item.validate();
        }
      });
    }
  },

  /**
   * @private
   * Watch for change events for contained components in order to automatically expand the toggle/checkbox.
   */
  setupMonitorOnChange: function(container, component) {
    var me = this;

    if (me === container) {
      me.mon(component, 'change', function(field, value) {
        if (value && me.collapsed) {
          me.expand();
        }
      });
    }
  }

});
