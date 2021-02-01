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
/*global define*/
define('Sonatype/init',['extjs', 'sonatype', 'Nexus/util/observable', 'sonatype'], function(Ext, Sonatype, Nexus) {
  Ext.apply(Sonatype, {
          init : function() {
            Ext.QuickTips.init();
            Ext.apply(Ext.QuickTips.getQuickTip(), {
              showDelay : 250,
              hideDelay : 300,
              dismissDelay : 0
              // don't automatically hide quicktip
            });

            Ext.History.init();

            Ext.get('header').hide();
            Ext.get('welcome-tab').hide();

            window.Sonatype.state.CookieProvider = new window.Sonatype.lib.CookieProvider({
              expires : new Date(new Date().getTime() + (1000 * 60 * 60 * 24 * 365))
              // expires in 1 year
            });

            Sonatype.resources.help = {};

            // Default anonymous user permissions; 3-bit permissions: delete | edit | read
            Sonatype.user.anon = {
              username : '',
              isLoggedIn : false,
              repoServer : {}
            };

            Sonatype.user.curr = {
              username : '',
              isLoggedIn : false,
              repoServer : {}
            };
          }
        }
  );

  Sonatype.Events = new Nexus.util.Observable();

  // FIXME circular dependency sonatype -> sonatype/view -> sonatype/headlinks, but headlinks needs 'Sonatype'
  Sonatype.headLinks = Ext.emptyFn;

  Ext.apply(Sonatype.headLinks.prototype, {
    /**
     * Update the head links based on the current status of Nexus
     */
    updateLinks : function() {
      var
            right = Ext.get('head-link-r'),
            loggedIn = Sonatype.user.curr.isLoggedIn;

      if (loggedIn)
      {
        this.updateRightWhenLoggedIn(right);
      }
      else
      {
        this.updateRightWhenLoggedOut(right);
      }
    },

    updateRightWhenLoggedIn : function(linkEl) {
      linkEl.update(Sonatype.user.curr.username);
      linkEl.addClass('head-link-logged-in');
      linkEl.un('click', Sonatype.repoServer.RepoServer.loginHandler, Sonatype.repoServer.RepoServer);
      linkEl.on('click', Sonatype.repoServer.RepoServer.showProfileMenu);
    },
    updateRightWhenLoggedOut : function(linkEl) {
      linkEl.un('click', Sonatype.repoServer.RepoServer.showProfileMenu);
      linkEl.update('Log In');

      this.setClickLink(linkEl);
      linkEl.removeClass('head-link-logged-in');
    },
    setClickLink : function(el) {
      el.removeAllListeners();
      el.on('click', Sonatype.repoServer.RepoServer.loginHandler, Sonatype.repoServer.RepoServer);
    }
  });

  return Sonatype;
});
