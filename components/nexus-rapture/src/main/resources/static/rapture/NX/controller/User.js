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
 * User controller.
 *
 * @since 3.0
 */
Ext.define('NX.controller.User', {
  extend: 'NX.app.Controller',
  requires: [
    'NX.util.Base64',
    'NX.Messages',
    'NX.State',
    'NX.I18n',
    'NX.view.header.Mode',
    'NX.util.Window',
    'Ext.Deferred',
    'Ext.Array'
  ],

  views: [
    'header.SignIn',
    'header.SignOut',
    'Authenticate',
    'SignIn',
    'ExpireSession'
  ],

  refs: [
    {
      ref: 'signInButton',
      selector: 'nx-header-signin'
    },
    {
      ref: 'signOutButton',
      selector: 'nx-header-signout'
    },
    {
      ref: 'userMode',
      selector: 'nx-header-mode[name=user]'
    },
    {
      ref: 'signIn',
      selector: 'nx-signin'
    },
    {
      ref: 'authenticate',
      selector: 'nx-authenticate'
    }
  ],

  /**
   * @override
   */
  init: function () {
    var me = this;

    me.getApplication().getIconController().addIcons({
      'authenticate': {
        file: 'lock.png',
        variants: ['x16', 'x32']
      }
    });

    me.listen({
      controller: {
        '#State': {
          userchanged: me.onUserChanged
        }
      },
      component: {
        'nx-header-panel': {
          afterrender: me.manageButtons
        },
        'nx-header-signin': {
          click: me.askToAuthenticate
        },
        'nx-expire-session button[action=signin]': {
          click: me.askToAuthenticate
        },
        'nx-header-signout': {
          click: me.onClickSignOut
        },
        'nx-signin button[action=signin]': {
          click: me.signIn
        },
        'nx-authenticate button[action=authenticate]': {
          click: me.doAuthenticateAction
        }
      }
    });
  },

  /**
   * @private
   */
  onUserChanged: function (user, oldUser) {
    var me = this;

    if (user && !oldUser) {
      NX.Messages.add({text: NX.I18n.format('User_SignedIn_Message', user.id), type: 'default'});
      me.fireEvent('signin', user);
    }
    else if (!user && oldUser) {
      NX.Messages.add({text: NX.I18n.get('User_SignedOut_Message'), type: 'default'});
      me.fireEvent('signout');
    }

    if (!user) {
      NX.util.Window.closeWindows();
    }

    me.manageButtons();
  },

  /**
   * Returns true if there is an authenticated user.
   *
   * @public
   * @return {boolean}
   */
  hasUser: function () {
    return Ext.isDefined(NX.State.getUser());
  },

  /**
   * Requests authentication of the user with the {@code authRequest} event. If no listeners are registered or none
   * perform authentication {@code showSignInWindow} will be called.
   *
   * The {@code authRequest} event is sent with an array argument; listeners are expected to push a
   * {@code Ext.Deferred} that is resolved when the listener has completed its attempt at authentication; the deferred
   * objects are expected to resolve with the authenticated user object if they handled authentication, or null
   * otherwise. If any handlers fails via {@code Ext.Deferred.reject} the default path of showing the sign-in window
   * will occur.
   *
   * If no user is found after all listeners deferred operations are complete the sign in window will be shown.
   *
   * @param message
   * @param options
   */
  askToAuthenticate: function (message, options) {
    var me = this,
        authedUser = null,
        currentUser = NX.State.getUser(),
        handlers = [];

    me.fireEvent('authRequest', handlers);

    Ext.Deferred.all(handlers).then(function(authedUsers) {
      if (Ext.isArray(authedUsers)) {
        // take the first authenticated user
        authedUser = Ext.Array.findBy(authedUsers, function(item) {
          return Ext.isObject(item);
        });
      }
    }, function() {
      // one of the handlers finished via reject; assume new sign-in
      currentUser = null;
      authedUser = null;
    }).always(function() {
      if (authedUser) {
        NX.State.setUser(authedUser);
      }
      else if (currentUser) {
        me.showAuthenticateWindow(message, Ext.apply(options || {}, {authenticateAction: me.authenticate}),
            currentUser);
      }
      else {
        me.showSignInWindow(options);
      }
    });
  },

  /**
   * Begins the process of retrieving an authentication token that will be used for a subsequent action. The auth token
   * is requested thru the {@code authTokenRequest} event; if no listeners provide a token the user will be prompted
   * for their credentials in order to obtain one.
   *
   * The {@code authTokenRequest} event is sent with an array argument and string argument:
   *    - array: listeners are expected to push a {@code Ext.Deferred} that is resolved with a token or null once
   *        completed. If a listener fails via {@code Ext.Deferred.reject} this will be treated as cancelling the
   *        action.
   *    - string: the message that would be shown in the authentication window
   *
   * @public
   * @param {String} [message] Message to be shown in authentication window
   * @param {Object} [options] TODO
   */
  doWithAuthenticationToken: function (message, options) {
    var me = this,
        token = null,
        handlers = [];

    me.fireEvent('authTokenRequest', handlers, message);

    Ext.Deferred.all(handlers).then(function(tokens) {
      if (Ext.isArray(tokens)) {
        // take the first defined token
        token = Ext.Array.findBy(tokens, function(item) {
          return !!item;
        });
      }
    }, function() {
      token = 'cancel';
    }).always(function() {
      if (token !== 'cancel') {
        if (!token) {
          me.showAuthenticateWindow(message,
              Ext.apply(options || {}, {authenticateAction: me.retrieveAuthenticationToken})
          );
        }
        else {
          if (Ext.isFunction(options.success)) {
            options.success.call(options.scope, token, options);
          }
        }
      }
    });
  },

  /**
   * Shows sign-in window.
   *
   * @private
   * @param {Object} [options] TODO
   */
  showSignInWindow: function (options) {
    var me = this;

    if (!me.getSignIn()) {
      me.getSignInView().create({options: options});
    }
  },

  /**
   * Shows authenticate window.
   *
   * @private
   * @param {String} [message] Message to be shown in authentication window
   * @param {Object} [options] TODO
   * @param {Object} [user] Optional user object that represents the current user
   */
  showAuthenticateWindow: function (message, options, user) {
    var me = this,
        username = user ? user.id : (NX.State.getUser().id || null),
        win;

    if (!me.getAuthenticate()) {
      win = me.getAuthenticateView().create({message: message, options: options});
      if (username) {
        win.down('form').getForm().setValues({username: username});
        win.down('#password').focus();
      }
    }
  },

  /**
   * @private
   */
  signIn: function (button) {
    var me = this,
        win = button.up('window'),
        form = button.up('form'),
        values = form.getValues(),
        b64username = NX.util.Base64.encode(values.username),
        b64password = NX.util.Base64.encode(values.password);

    win.getEl().mask(NX.I18n.get('User_SignIn_Mask'));

    //<if debug>
    me.logDebug('Sign-in user: "', values.username, '" ...');
    //</if>

    me.doSignIn(b64username, b64password, values, button);
  },

  /**
   * @private
   */
  doAuthenticateAction: function (button) {
    var win = button.up('window');

    // invoke optional authenticateAction callback registered on window
    if (win.options && Ext.isFunction(win.options.authenticateAction)) {
      win.options.authenticateAction.call(this, button);
    }
  },

  // TODO: anything that may change the authentication/session should probably not be
  // TODO: done via extjs as it can batch, and the batch operation could impact the
  // TODO: sanity of the requests if authentication changes mid execution of batch operations

  doSignIn: function(b64username, b64password, values, button) {
    var me = this,
        win = button.up('window');

    Ext.Ajax.request({
      url: NX.util.Url.urlOf('service/rapture/session'),
      method: 'POST',
      params: {
        username: b64username,
        password: b64password
      },
      scope: me,
      suppressStatus: true,
      success: function () {
        win.getEl().unmask();
        NX.State.setUser({id: values.username});
        win.close();

        // invoke optional success callback registered on window
        if (win.options && Ext.isFunction(win.options.success)) {
          win.options.success.call(win.options.scope, win.options);
        }
      },
      failure: function (response) {
        var message = NX.I18n.get('User_Credentials_Message');
        if (response.status === 0) {
          message = NX.I18n.get('User_ConnectFailure_Message');
        }
        win.getEl().unmask();
        NX.Messages.add({
          text: message,
          type: 'warning'
        });
      }
    });
  },

  /**
   * @private
   */
  authenticate: function (button) {
    var me = this,
        win = button.up('window'),
        form = button.up('form'),
        user = NX.State.getUser(),
        values = Ext.applyIf(form.getValues(), {username: user ? user.id : undefined}),
        b64username = NX.util.Base64.encode(values.username),
        b64password = NX.util.Base64.encode(values.password);

    win.getEl().mask(NX.I18n.get('User_Controller_Authenticate_Mask'));

    //<if debug>
    this.logDebug('Authenticating user "', values.username, '" ...');
    //</if>

    me.doSignIn(b64username, b64password, values, button);
  },

  /**
   * @private
   */
  retrieveAuthenticationToken: function (button) {
    var win = button.up('window'),
        form = button.up('form'),
        user = NX.State.getUser(),
        values = Ext.applyIf(form.getValues(), {username: user ? user.id : undefined}),
        b64username = NX.util.Base64.encode(values.username),
        b64password = NX.util.Base64.encode(values.password);

    win.getEl().mask(NX.I18n.get('User_Retrieving_Mask'));

    //<if debug>
    this.logDebug('Retrieving authentication token...');
    //</if>

    NX.direct.rapture_Security.authenticationToken(b64username, b64password, function (response) {
      win.getEl().unmask();
      if (Ext.isObject(response) && response.success) {
        win.close();

        // invoke optional success callback registered on window
        if (win.options && Ext.isFunction(win.options.success)) {
          win.options.success.call(win.options.scope, response.data, win.options);
        }
      }
    });
  },

  /**
   * @private
   */
  onClickSignOut: function () {
    var me = this;

    if (me.fireEvent('beforesignout')) {
      me.signOut();
    }
  },

  /**
   * @public
   */
  signOut: function () {
    var me = this;

    me.logDebug('Sign-out');

    // TODO: Mask?

    Ext.Ajax.request({
      url: NX.util.Url.urlOf('service/rapture/session'),
      method: 'DELETE',
      scope: me,
      suppressStatus: true,
      success: function () {
        NX.State.setUser(undefined);
      }
    });
  },

  manageButtons: function () {
    var me = this,
        user = NX.State.getUser(),
        signInButton = me.getSignInButton(),
        signOutButton = me.getSignOutButton(),
        userMode = me.getUserMode();

    if (signInButton) {
      if (user) {
        signInButton.hide();
        userMode.show();
        userMode.getViewModel().set('text', user.id);
        userMode.getViewModel().set('tooltip', NX.I18n.format('User_Tooltip', user.id));
        signOutButton.show();
      }
      else {
        signInButton.show();
        userMode.hide();
        signOutButton.hide();
      }
    }
  }

});
