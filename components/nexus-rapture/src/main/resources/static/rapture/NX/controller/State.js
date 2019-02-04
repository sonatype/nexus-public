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
 * State controller.
 *
 * @since 3.0
 */
Ext.define('NX.controller.State', {
  extend: 'NX.app.Controller',
  requires: [
    'Ext.direct.Manager',
    'NX.Dialogs',
    'NX.Messages',
    'NX.I18n',
    'Ext.Ajax'
  ],

  models: [
    'State'
  ],
  stores: [
    'State'
  ],

  /**
   * @private
   */
  disconnectedTimes: 0,

  /**
   * Max number of times to show a warning, before disabling the UI.
   *
   * @private
   */
  maxDisconnectWarnings: 3,

  /**
   * True when state is received from server.
   *
   * @private
   */
  receiving: false,

  /**
   * @override
   */
  init: function () {
    var me = this;

    me.listen({
      controller: {
        '#State': {
          userchanged: me.onUserChanged,
          uisettingschanged: me.onUiSettingsChanged,
          licensechanged: me.onLicenseChanged,
          serveridchanged: me.reloadWhenServerIdChanged,
          clusteridchanged: me.reloadWhenServerIdChanged
        }
      },
      store: {
        '#State': {
          add: me.onEntryAdded,
          update: me.onEntryUpdated,
          remove: me.onEntryRemoved
        }
      }
    });
  },

  /**
   * Install initial state, primed from app.js
   *
   * @override
   */
  onLaunch: function () {
    var me = this;

    //<if debug>
    me.logTrace('Initial state:', NX.app.state);
    //</if>

    var uiSettings = NX.app.state['uiSettings'];

    NX.State.setBrowserSupported(
        !Ext.isIE || (Ext.isIE9p && Ext.isIE11m)
    );
    NX.State.setValue('debug', NX.app.debug);
    NX.State.setValue('receiving', false);

    // set uiSettings by the end so it does not start state pulling till all initial state hashes are known
    // this avoids unnecessary sending of state from server
    delete NX.app.state['uiSettings'];
    NX.State.setValues(NX.app.state);
    NX.State.setValues({ uiSettings: uiSettings });

    //<if debug>
    me.logInfo('State primed');
    //</if>
  },

  /**
   * @public
   * @returns {Boolean} true when status is being received from server
   */
  isReceiving: function () {
    return this.receiving;
  },

  getValue: function(key, defaultValue) {
    var model = this.getStore('State').getById(key),
        value;

    if (model) {
      value = model.get('value');
      if (Ext.isDefined(value)) {
        return value;
      }
    }
    return defaultValue;
  },

  /**
   * @public
   * @param {String} key
   * @param {Object} value
   * @param {String} [hash]
   */
  setValue: function (key, value, hash) {
    var me = this,
        store = me.getStore('State'),
        model = store.getById(key),
        hasValue = Ext.isDefined(value) && value !== null;

    if (!hasValue && model) {
      store.remove(model);
    }
    else if (hasValue && !model) {
      store.add(me.getStateModel().create({ key: key, value: value, hash: hash }));
    }
    else if (hash && !Ext.Object.equals(hash, model.get('hash'))) {
      model.set('hash', hash);

      if (key === 'user' && model.get('value').id === value.id) {
        model.set('value', value, { silent: true });
        me.fireEvent('userAuthenticated', key, value);
      }

      if (!Ext.Object.equals(value, model.get('value'))) {
        model.set('value', value);
      }
    }
    else if (!hash && hasValue) {
      model.set('hash', hash);
      model.set('value', value);
    }

    store.commitChanges();

    if (me.statusProvider) {
      if (hasValue && hash) {
        me.statusProvider.baseParams[key] = hash;
      }
      else {
        delete me.statusProvider.baseParams[key];
      }
    }

  },

  setValues: function (map) {
    var me = this,
        hash, valueToSet;

    if (map) {
      Ext.Object.each(map, function (key, value) {
        valueToSet = value;
        if (Ext.isObject(value) && Ext.isDefined(value.hash) && Ext.isDefined(value.value)) {
          hash = value.hash;
          valueToSet = value.value;
        }
        if (Ext.isDefined(valueToSet)) {
          if (!Ext.isPrimitive(valueToSet) && !Ext.isArray(valueToSet)
              && Ext.ClassManager.getByAlias('nx.state.' + key)) {
            valueToSet = Ext.ClassManager.instantiateByAlias('nx.state.' + key, valueToSet);
          }
        }
        me.setValue(key, valueToSet, hash);
      });
    }
  },

  onEntryAdded: function (store, models) {
    var me = this;
    Ext.each(models, function (model) {
      me.notifyChange(model.get('key'), model.get('value'));
    });
  },

  onEntryUpdated: function (store, model, operation, modifiedFieldNames) {
    if ((operation === Ext.data.Model.EDIT) && modifiedFieldNames.indexOf('value') > -1) {
      this.notifyChange(model.get('key'), model.get('value'), model.modified.value);
    }
  },

  onEntryRemoved: function (store, models) {
    models.forEach(function(model) {
      this.notifyChange(model.get('key'), undefined, model.get('value'));
    }, this);
  },

  notifyChange: function (key, value, oldValue) {
    var me = this;

    //<if debug>
    me.logTrace('Changed:', key, '->', (value ? value : '(deleted)'));
    //</if>

    me.fireEvent(key.toLowerCase() + 'changed', value, oldValue);
    me.fireEvent('changed', key, value, oldValue);
  },

  /**
   * Reset state pooling when uiSettings.statusInterval changes.
   *
   * @private
   */
  onUiSettingsChanged: function (uiSettings, oldUiSettings) {
    var me = this,
        newStatusInterval, oldStatusInterval;

    uiSettings = uiSettings || {};
    oldUiSettings = oldUiSettings || {};

    if (uiSettings.debugAllowed !== oldUiSettings.debugAllowed) {
      NX.State.setValue('debug', uiSettings.debugAllowed && (NX.global.location.search === '?debug'));
    }

    if (uiSettings.title !== oldUiSettings.title) {
      NX.global.document.title = NX.global.document.title.replace(oldUiSettings.title, uiSettings.title);
    }

    if (me.statusProvider) {
      oldStatusInterval = me.statusProvider.interval;
    }

    newStatusInterval = uiSettings.statusIntervalAnonymous;
    if (NX.State.getUser()) {
      newStatusInterval = uiSettings.statusIntervalAuthenticated;
    }

    if (newStatusInterval > 0) {
      if (newStatusInterval !== oldStatusInterval) {
        if (me.statusProvider) {
          me.statusProvider.disconnect();
          me.receiving = false;
        }
        me.statusProvider = Ext.direct.Manager.addProvider({
          type: 'polling',
          url: NX.direct.api.POLLING_URLS.rapture_State_get,
          interval: newStatusInterval * 1000,
          baseParams: {
          },
          listeners: {
            data: me.onServerData,
            scope: me
          }
        });

        //<if debug>
        me.logDebug('State pooling configured for', newStatusInterval, 'seconds');
        //</if>

        // fire one request for state manually to not wait for the polling interval
        me.refreshNow();
      }
    }
    else {
      if (me.statusProvider) {
        me.statusProvider.disconnect();
      }

      //<if debug>
      me.logDebug('State pooling disabled');
      //</if>
    }
  },

  /**
   * On sign-in/sign-out update status interval.
   *
   * @private
   */
  onUserChanged: function (user, oldUser) {
    var uiSettings;

    if (Ext.isDefined(user) !== Ext.isDefined(oldUser)) {
      uiSettings = NX.State.getValue('uiSettings');
      this.onUiSettingsChanged(uiSettings, uiSettings);
    }
  },

  /**
   * Called when there is new data from state callback.
   *
   * @private
   */
  onServerData: function (provider, event) {
    var me = this;
    if (event.data) {
      me.onSuccess(event);
    }
    else {
      me.onError(event);
    }
  },

  /**
   * Called when state pooling was successful.
   *
   * @private
   */
  onSuccess: function (event) {
    var me = this,
        serverId = me.getValue('serverId'),
        clusterId = me.getValue('clusterId'),
        state = event.data.data,
        clustered = Ext.isDefined(state.nodes) && state.nodes.value.enabled,
        oldClusterId = state.clusterId ? state.clusterId.value : clusterId,
        oldServerId = state.serverId ? state.serverId.value : serverId;

    me.receiving = true;

    // re-enable the UI we are now connected again
    if (me.disconnectedTimes > 0) {
      me.disconnectedTimes = 0;
      NX.Messages.add({text: NX.I18n.get('State_Reconnected_Message'), type: 'success' });
    }

    NX.State.setValue('receiving', true);

    if ( (clustered && !me.reloadWhenServerIdChanged(clusterId, oldClusterId)) ||
        (!clustered && !me.reloadWhenServerIdChanged(serverId, oldServerId)) ) {
      me.setValues(state);
    }
    // TODO: Fire global refresh event
  },

  /**
   * Called when state pooling failed.
   *
   * @private
   */
  onError: function (event) {
    var me = this;

    if (event.code === 'xhr') {
      if (event.xhr.status === 402) {
        NX.State.setValue('license', Ext.apply(Ext.clone(NX.State.getValue('license')), { installed: false }));
      }
      else {
        me.receiving = false;

        // we appear to have lost the server connection
        me.disconnectedTimes = me.disconnectedTimes + 1;

        NX.State.setValue('receiving', false);

        if (me.disconnectedTimes <= me.maxDisconnectWarnings) {
          NX.Messages.add({ text: NX.I18n.get('State_Disconnected_Message'), type: 'warning' });
        }

        // Give up after a few attempts and disable the UI
        if (me.disconnectedTimes > me.maxDisconnectWarnings) {
          NX.Messages.add({text: NX.I18n.get('State_Disconnected_Message'), type: 'danger' });

          // Stop polling
          me.statusProvider.disconnect();

          // FIXME: i18n
          // Show the UI with a modal dialog error
          NX.Dialogs.showError(
              'Server disconnected',
              'There is a problem communicating with the server',
              {
                buttonText: {
                  ok: 'Retry'
                },

                fn: function () {
                  // retry after the dialog is dismissed
                  me.statusProvider.connect();
                }
              }
          );
        }
      }
    }
    else if (event.type === 'exception') {
      NX.Messages.add({ text: event.message, type: 'danger' });
    }
  },

  /**
   * Refreshes status from server on demand.
   *
   * @public
   */
  refreshNow: function () {
    var me = this;

    // directly query for state
    Ext.Ajax.request({
      url: NX.direct.api.POLLING_URLS.rapture_State_get,
      scope: me,
      success: function(response) {
        var text = response && response.responseText;

        if (text != null) {
          me.onServerData(null, Ext.isObject(text) || Ext.isArray(text) ? text : Ext.decode(text));
        }
      }
    });

    if (me.statusProvider) {
      me.statusProvider.disconnect();
      me.statusProvider.connect();
    }
  },

  /**
   * Show messages about license.
   *
   * @private
   * @param {Object} license
   * @param {Number} license.installed
   * @param {Object} oldLicense
   * @param {Number} oldLicense.installed
   */
  onLicenseChanged: function (license, oldLicense) {
    if (license && oldLicense) {
      if (license.installed && !oldLicense.installed) {
        NX.Messages.add({ text: NX.I18n.get('State_Installed_Message'), type: 'success' });
      }
      else if (!license.installed && oldLicense.installed) {
        NX.Messages.add({ text: NX.I18n.get('State_Uninstalled_Message'), type: 'warning' });
      }
    }
  },

  reloadWhenServerIdChanged: function (serverId, oldServerId) {
    if (oldServerId && (serverId !== oldServerId) && !Ext.String.startsWith(serverId, 'ignore')) {
      // FIXME: i18n
      NX.Dialogs.showInfo(
          'Server restarted',
          'Application will be reloaded as server has been restarted',
          {
            fn: function () {
              NX.global.location.reload();
            }
          }
      );
      return true;
    }
    return false;
  }

});
