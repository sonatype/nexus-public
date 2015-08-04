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
/*global NX, Ext, Nexus*/

/**
 * Container for icons used by capabilities.
 *
 * @since capabilities 2.2.2
 */
NX.define('Nexus.capabilities.Icons', {
  extend: 'Nexus.util.IconContainer',
  singleton: true,

  /**
   * @constructor
   */
  constructor: function () {
    var self = this;

    // helper to build an icon config with variants, where variants live in directories, foo.png x16 -> x16/foo.png
    function iconConfig(fileName, variants) {
      var config = {};
      if (variants === undefined) {
        variants = ['x32', 'x16'];
      }
      Ext.each(variants, function (variant) {
        config[variant] = variant + '/' + fileName;
      });
      return config;
    }

    self.constructor.superclass.constructor.call(self, {
      stylePrefix: 'nx-capabilities-icon-',

      icons: {
        capability:           iconConfig('brick.png'),
        capability_add:       iconConfig('brick_add.png'),
        capability_delete:    iconConfig('brick_delete.png'),
        capability_new:       iconConfig('brick_edit.png'),
        capability_active:    iconConfig('brick_valid.png'),
        capability_passive:   iconConfig('brick_error.png'),
        capability_disabled:  iconConfig('brick_grey.png'),
        capability_error:     iconConfig('brick_error.png'),
        cross:                'cross.png',
        cross_grey:           'cross_grey.png',
        filter:               'filter.png',
        filter_grey:          'filter_grey.png',
        warning:              'error.png',
        magnifier:            'magnifier.png',
        magnifier_grey:       'magnifier_grey.png',
        refresh:              'arrow_refresh.png',
        selectionEmpty:       '@warning',
        enable:               '@capability_active',
        disable:              '@capability_disabled'
      }
    });
  },

  /**
   * Return the icon for the given capability.
   * @param capability
   * @return {*} Icon; never null/undefined.
   */
  iconFor: function (capability) {
    var self = this,
        typeName = capability.typeName,
        enabled = capability.enabled,
        active = capability.active,
        error = capability.error,
        iconName;

    if (!typeName) {
      iconName = 'capability_new';
    }
    else if (enabled && error) {
      iconName = 'capability_error';
    }
    else if (enabled && active) {
      iconName = 'capability_active';
    }
    else if (enabled && !active) {
      iconName = 'capability_passive';
    }
    else {
      iconName = 'capability_disabled';
    }
    return self.get(iconName);
  }

});