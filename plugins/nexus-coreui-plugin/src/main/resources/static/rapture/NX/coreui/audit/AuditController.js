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
 * Audit controller.
 *
 * @since 3.1
 */
Ext.define('NX.coreui.audit.AuditController', {
  extend: 'NX.app.Controller',
  requires: [
    'NX.Conditions',
    'NX.State',
    'NX.Messages',
    'NX.Dialogs',
    'NX.I18n',
    'NX.coreui.audit.AuditList'
  ],

  stores: [
    'NX.coreui.audit.AuditStore'
  ],

  refs: [
    {
      ref: 'list',
      selector: 'nx-coreui-audit-list'
    },
    {
      ref: 'content',
      selector: 'nx-feature-content'
    }
  ],

  /**
   * @override
   */
  init: function () {
    var me = this;

    me.getApplication().getIconController().addIcons({
      'audit-default': {
        file: 'report.png',
        variants: ['x16', 'x32']
      },
      'audit-blobstore': {
        file: 'drive_network.png',
        variants: ['x16', 'x32']
      },
      'audit-capability': {
        file: 'brick.png',
        variants: ['x16', 'x32']
      },
      'audit-httpclient': {
        file: 'lorry.png',
        variants: ['x16', 'x32']
      },
      'audit-logging': {
        file: 'book.png',
        variants: ['x16', 'x32']
      },
      'audit-repository': {
        file: 'database.png',
        variants: ['x16', 'x32']
      },
      'audit-email': {
        file: 'email.png',
        variants: ['x16', 'x32']
      },
      'audit-tasks': {
        file: 'time.png',
        variants: ['x16', 'x32']
      },
      'audit-license': {
        file: 'license_key.png',
        variants: ['x16', 'x32']
      },
      'audit-security.anonymous': {
        file: 'user_silhouette.png',
        variants: ['x16', 'x32']
      },
      'audit-security.crowd': {
        file: 'crowd.png',
        variants: ['x16']
      },
      'audit-security.privilege': {
        file: 'medal_gold_red.png',
        variants: ['x16', 'x32']
      },
      'audit-security.realm': {
        file: 'shield.png',
        variants: ['x16', 'x32']
      },
      'audit-security.role': {
        file: 'user_policeman.png',
        variants: ['x16', 'x32']
      },
      'audit-security.ldap': {
        file: 'book_addresses.png',
        variants: ['x16', 'x32']
      },
      'audit-security.sslcertificate': {
        file: 'ssl_certificates.png',
        variants: ['x16', 'x32']
      },
      'audit-security.user': {
        file: 'user.png',
        variants: ['x16', 'x32']
      },
      'audit-security.user-role-mapping': {
        file: 'user.png',
        variants: ['x16', 'x32']
      }
    });

    me.getApplication().getFeaturesController().registerFeature({
      mode: 'admin',
      path: '/System/Audit',
      text: NX.I18n.render(this, 'Text'),
      description: NX.I18n.render(this, 'Description'),
      view: { xtype: 'nx-coreui-audit-list' },
      iconConfig: {
        file: 'report_stack.png',
        variants: ['x16', 'x32']
      },
      visible: function () {
        var state = NX.State.getValue('audit');
        return NX.Permissions.check('nexus:audit:read') && state && state.enabled;
      }
    }, me);

    me.listen({
      controller: {
        '#Refresh': {
          refresh: me.load
        }
      },
      component: {
        'nx-coreui-audit-list': {
          afterrender: me.load
        },
        'nx-coreui-audit-list button[action=clear]': {
          click: me.clear,
          afterrender: me.bindClearButton
        }
      }
    });
  },

  /**
   * @private
   */
  load: function () {
    var list = this.getList();

    if (list) {
      list.getStore().load();
    }
  },

  /**
   * @private
   */
  clear: function () {
    var me = this;

    NX.Dialogs.askConfirmation(NX.I18n.render(me, 'Clear_Title'), NX.I18n.render(me, 'Clear_Body'), function () {
      me.getContent().getEl().mask(NX.I18n.render(me, 'Clear_Mask'));
      NX.direct.audit_Audit.clear(function (response) {
        me.getContent().getEl().unmask();
        me.load();
        if (Ext.isObject(response) && response.success) {
          NX.Messages.add({ text: NX.I18n.render(me,'Clear_Success'), type: 'success' });
        }
      });
    });
  },

  /**
   * @private
   */
  bindClearButton: function (button) {
    button.mon(
        NX.Conditions.isPermitted('nexus:audit:delete'),
        {
          satisfied: button.enable,
          unsatisfied: button.disable,
          scope: button
        }
    );
  }
});
