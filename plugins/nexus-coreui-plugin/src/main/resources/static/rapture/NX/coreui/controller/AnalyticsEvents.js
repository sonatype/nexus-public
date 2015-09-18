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
 * Analytics Events controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.AnalyticsEvents', {
  extend: 'NX.app.Controller',
  requires: [
    'NX.util.Url',
    'NX.util.DownloadHelper',
    'NX.Conditions',
    'NX.State',
    'NX.Messages',
    'NX.Dialogs',
    'NX.I18n'
  ],

  stores: [
    'AnalyticsEvent'
  ],
  views: [
    'analytics.AnalyticsEventList',
    'analytics.EventsZipCreated'
  ],
  refs: [
    {
      ref: 'list',
      selector: 'nx-coreui-analytics-event-list'
    },
    {
      ref: 'content',
      selector: 'nx-feature-content'
    },
    {
      ref: 'main',
      selector: 'nx-main'
    }
  ],

  /**
   * @override
   */
  init: function () {
    var me = this;

    me.getApplication().getIconController().addIcons({
      'analyticsevent-default': {
        file: 'transmit.png',
        variants: ['x16', 'x32']
      },
      'analyticsevent-rest': {
        file: 'transmit.png',
        variants: ['x16', 'x32']
      },
      'analyticsevent-ui': {
        file: 'transmit_blue.png',
        variants: ['x16', 'x32']
      },
      'analyticsevent-zip': {
        file: 'file_extension_zip.png',
        variants: ['x16', 'x32']
      }
    });

    me.getApplication().getFeaturesController().registerFeature({
      mode: 'admin',
      path: '/Support/Analytics/Events',
      text: NX.I18n.get('AnalyticsEvents_Text'),
      description: NX.I18n.get('AnalyticsEvents_Description'),
      view: { xtype: 'nx-coreui-analytics-event-list' },
      iconConfig: {
        file: 'transmit.png',
        variants: ['x16', 'x32']
      },
      visible: function () {
        var analyticsState = NX.State.getValue('analytics');
        return NX.Permissions.check('nexus:analytics:read') && analyticsState && analyticsState.enabled;
      }
    }, me);

    me.listen({
      controller: {
        '#Refresh': {
          refresh: me.load
        }
      },
      component: {
        'nx-coreui-analytics-event-list': {
          afterrender: me.load
        },
        'nx-coreui-analytics-event-list button[action=clear]': {
          click: me.clear,
          afterrender: me.bindClearButton
        },
        'nx-coreui-analytics-event-list button[action=export]': {
          click: me.exportEvents
        },
        'nx-coreui-analytics-event-list button[action=submit]': {
          click: me.submit,
          afterrender: me.bindSubmitButton
        },
        'nx-coreui-analytics-eventszipcreated button[action=download]': {
          click: me.download
        }
      }
    });
  },

  /**
   * @private
   * Load analytics event store.
   */
  load: function () {
    var list = this.getList();

    if (list) {
      list.getStore().load();
    }
  },

  /**
   * @private
   * Clear all events.
   */
  clear: function () {
    var me = this;

    NX.Dialogs.askConfirmation(NX.I18n.get('AnalyticsEvents_Clear_Title'), NX.I18n.get('AnalyticsEvents_Clear_Body'), function () {
      me.getContent().getEl().mask(NX.I18n.get('AnalyticsEvents_Clear_Mask'));
      NX.direct.analytics_Events.clear(function (response) {
        me.getContent().getEl().unmask();
        me.load();
        if (Ext.isObject(response) && response.success) {
          NX.Messages.add({ text: NX.I18n.get('AnalyticsEvents_Clear_Success'), type: 'success' });
        }
      });
    });
  },

  /**
   * @private
   * Export and download events.
   */
  exportEvents: function () {
    var me = this;

    NX.Dialogs.askConfirmation(NX.I18n.get('AnalyticsEvents_Export_Title'), NX.I18n.get('AnalyticsEvents_Export_Body'), function () {
          me.getMain().getEl().mask(NX.I18n.get('AnalyticsEvents_Export_Mask'));
          NX.direct.analytics_Events.exportAll(function (response) {
            me.getMain().getEl().unmask();
            if (Ext.isObject(response) && response.success) {
              Ext.widget('nx-coreui-analytics-eventszipcreated').setValues(response.data);
            }
          });
        });
  },

  /**
   * @private
   * Download events file.
   */
  download: function (button) {
    var win = button.up('window'),
        fileName = win.down('form').getValues().name;

    NX.Security.doWithAuthenticationToken(NX.I18n.get('AnalyticsEvents_Authenticate_Title'),
        {
          success: function (authToken) {
            NX.util.DownloadHelper.downloadUrl(NX.util.Url.urlOf(
                'service/siesta/wonderland/download/' + fileName + '?t=' + NX.util.Base64.encode(authToken)
            ));
            win.close();
          }
        }
    );
  },

  /**
   * @private
   * Submit events to Sonatype.
   */
  submit: function () {
    NX.Dialogs.askConfirmation(NX.I18n.get('AnalyticsEvents_Submit_Title'), NX.I18n.get('AnalyticsEvents_Submit_Body'), function () {
      NX.Security.doWithAuthenticationToken(NX.I18n.get('AnalyticsEvents_Submit_HelpText'),
          {
            success: function (authToken) {
              NX.direct.analytics_Events.submit(authToken, function (response) {
                if (Ext.isObject(response) && response.success) {
                  NX.Messages.add({ text: NX.I18n.get('AnalyticsEvents_Submit_Success'), type: 'success' });
                }
              });
            }
          }
      );
    });
  },

  /**
   * @private
   * Enable 'Clear' when user has 'delete' permission.
   */
  bindClearButton: function (button) {
    button.mon(
        NX.Conditions.isPermitted('nexus:analytics:delete'),
        {
          satisfied: button.enable,
          unsatisfied: button.disable,
          scope: button
        }
    );
  },

  /**
   * @private
   * Enable 'Submit' when user has 'create' permission.
   */
  bindSubmitButton: function (button) {
    button.mon(
        NX.Conditions.isPermitted('nexus:analytics:create'),
        {
          satisfied: button.enable,
          unsatisfied: button.disable,
          scope: button
        }
    );
  }

});
