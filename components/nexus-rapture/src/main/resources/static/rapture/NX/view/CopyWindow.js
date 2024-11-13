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
 * About window.
 *
 * @since 3.0
 */
Ext.define('NX.view.CopyWindow', {
  extend: 'NX.view.ModalDialog',
  alias: 'widget.nx-copywindow',
  requires: [
    'NX.I18n',
    'NX.Icons'
  ],

  layout: {
    type: 'vbox',
    align: 'stretch'
  },

  ui: 'nx-inset',

  /**
   * @property
   * The text to be selected for copying
   */
  copyText: '',

  /**
   * @property
   * The repository format to be used for help text
   */
  repoFormat: '',

  /**
   * @property
   * The message to use when prompting the user to copy/paste
   */
  defaultMessage: 'Copy to clipboard: #{key}, Enter',

  /**
   * @override
   */
  initComponent: function () {
    var me = this,
        message = this.format(this.defaultMessage);

    me.width = NX.view.ModalDialog.MEDIUM_MODAL;

    me.title = message;
    me.items = {
      xtype: 'form',
      defaults: {
        anchor: '100%'
      },
      items: [
        {
          xtype: 'component',
          html: '<p>' + me.getHelpText(me.repoFormat) + '<p>'
        },
        {
          xtype: 'textfield',
          name: 'url',
          value: me.copyText,
          selectOnFocus: true
        }
      ],
      buttonAlign: 'left',
      buttons: [
        {
          text: NX.I18n.get('Button_Close'),
          action: 'close',
          bindToEnter: true,
          handler: function () {
            me.close();
          }
        }
      ]
    };
    me.defaultFocus = 'textfield';

    me.callParent();
  },

  /**
   * @private
   * @param Substitute the keyboard shortcut for copy, given the current platform
   * @returns {string}
   */
  format: function (message) {
    var copyKey = (/mac os x/i.test(navigator.userAgent) ? '⌘' : 'Ctrl') + '+C';
    return message.replace(/#{\s*key\s*}/g, copyKey);
  },

  getHelpText: function (repoFormat) {
    var repoFormatLabels = {
      apt: 'Apt',
      cocoapods: 'CocoaPods',
      conan: 'Conan',
      conda: 'Conda',
      docker: 'Docker',
      gitlfs: 'Git LFS',
      go: 'Go',
      helm: 'Helm',
      maven2: 'Maven',
      npm: 'npm',
      nuget: 'NuGet',
      p2: 'p2',
      pypi: 'PyPI',
      r: 'R',
      raw: 'Raw',
      rubygems: 'RubyGems',
      yum: 'Yum'
    };

    return NX.I18n.format(
      'Repository_Copy_URL', 
      repoFormat,
      repoFormatLabels[repoFormat]
    );
  }
});
