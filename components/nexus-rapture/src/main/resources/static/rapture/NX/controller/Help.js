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
 * Help controller.
 *
 * @since 3.0
 */
Ext.define('NX.controller.Help', {
  extend: 'NX.app.Controller',
  requires: [
    'NX.Icons',
    'NX.I18n',
    'NX.Windows'
  ],

  views: [
    'header.Help',
    'AboutWindow'
  ],

  statics: {
    /**
     * The base-url for help links.
     *
     * @private
     * @property {String}
     * @readonly
     */
    baseUrl: 'https://links.sonatype.com/products/nexus',

    getDocsUrl: function() {
      return NX.controller.Help.baseUrl + '/docs/' + NX.State.getVersionMajorMinor();
    }
  },

  /**
   * @override
   */
  init: function () {
    var me = this;

    me.getApplication().getIconController().addIcons({
      'help-support': {
        file: 'support.png',
        variants: ['x16', 'x32']
      },
      'help-issues': {
        file: 'bug.png',
        variants: ['x16', 'x32']
      },
      'help-manual': {
        file: 'book_picture.png',
        variants: ['x16', 'x32']
      },
      'help-community': {
        file: 'users_4.png',
        variants: ['x16', 'x32']
      },
      'help-kb': {
        file: 'brain_trainer.png',
        variants: ['x16', 'x32']
      }
    });

    me.listen({
      component: {
        'nx-header-help menuitem[action=about]': {
          click: me.onAbout
        },
        'nx-header-help menuitem[action=docs]': {
          click: me.onDocs
        },
        'nx-header-help menuitem[action=support]': {
          click: me.onSupport
        },
        'nx-header-help menuitem[action=issues]': {
          click: me.onIssues
        },
        'nx-header-help menuitem[action=community]': {
          click: me.onCommunity
        },
        'nx-header-help menuitem[action=kb]': {
          click: me.onKnowledgeBase
        }
      }
    });
  },

  /**
   * @private
   * @param {String} section
   */
  openUrl: function(section) {
    NX.Windows.open(NX.controller.Help.baseUrl + '/' + section);
  },

  /**
   * @private
   */
  onAbout: function() {
    Ext.widget('nx-aboutwindow');
  },

  /**
   * @private
   */
  onDocs: function() {
    NX.Windows.open(NX.controller.Help.getDocsUrl());
  },

  /**
   * @private
   */
  onSupport: function() {
    this.openUrl('support');
  },

  /**
   * @private
   */
  onIssues: function() {
    this.openUrl('issues');
  },

  /**
   * @private
   */
  onCommunity: function() {
    this.openUrl('community');
  },

  /**
   * @private
   */
  onKnowledgeBase: function() {
    this.openUrl('kb');
  }
});
