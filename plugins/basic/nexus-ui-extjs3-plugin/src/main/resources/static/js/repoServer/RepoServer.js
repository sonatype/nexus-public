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
/*global define*/
define('repoServer/RepoServer',['extjs', 'sonatype', 'Sonatype/lib', 'Nexus/config', 'Sonatype/utils', 'Sonatype/view', 'Nexus/navigation'], function(Ext, Sonatype){
  // Repository main Controller(conglomerate) Singleton
  Sonatype.repoServer.RepoServer = (function() {
    var cfg = Sonatype.config.repos,
        sp = Sonatype.lib.Permissions,
          lastFormHeight = -1;

    // ************************************
    return {
      pomDepTmpl : new Ext.XTemplate('<dependency><groupId>{groupId}</groupId><artifactId>{artifactId}</artifactId><version>{version}</version></dependency>'),

      loginFormConfig : {
        labelAlign : 'right',
        labelWidth : 60,
        frame : true,

        defaultType : 'textfield',
        monitorValid : true,
        listeners : {
          clientvalidation : function(form, valid) {
            // IE does not resize the window when invalid messages are shown
            var window, formHeight = form.getHeight();

            if ( lastFormHeight !== formHeight ) {
              window = form.findParentByType('window');
              if ( window !== undefined ) {
                lastFormHeight = formHeight;
                window.setHeight(formHeight + form.getToolbarHeight());
              }
            }
          },
          scope : this
        },

        items : [{
              id : 'usernamefield',
              fieldLabel : 'Username',
              name : 'username',
              tabIndex : 1,
              width : 200,
              allowBlank : false
            }, {
              id : 'passwordfield',
              fieldLabel : 'Password',
              name : 'password',
              tabIndex : 2,
              inputType : 'password',
              width : 200,
              allowBlank : false
            }]
        // buttons added later to provide scope to handler

      },

      statusComplete : function(statusResponse) {
        this.resetMainTabPanel();

        this.createSubComponents(); // update left panel
      },

      // Each Sonatype server will need one of these
      initServerTab : function() {

        Sonatype.Events.addListener('nexusNavigationInit', this.addNexusNavigationItems, this);

        Sonatype.Events.addListener('nexusStatus', this.nexusStatusEvent, this);

        // Left Panel
        this.nexusPanel = new Sonatype.navigation.NavigationPanel({
              id : 'st-nexus-tab',
              title : 'Nexus Repository Manager'
            });

        this.createSubComponents();

        Sonatype.view.serverTabPanel.add(this.nexusPanel);
        Sonatype.view.serverTabPanel.hideTabStripItem('st-nexus-tab');

        this.loginFormConfig.buttons = [{
              id : 'loginbutton',
              text : 'Log In',
              tabIndex : 3,
              formBind : true,
              scope : this,
              handler : function() {
                var usernameField = this.loginForm.find('name', 'username')[0],
                    passwordField = this.loginForm.find('name', 'password')[0];

                if (usernameField.isValid() && passwordField.isValid())
                {
                  Sonatype.utils.doLogin(this.loginWindow, usernameField.getValue(), passwordField.getValue());
                }
              }
            }];

        this.loginFormConfig.keys = {
          key : Ext.EventObject.ENTER,
          fn : this.loginFormConfig.buttons[0].handler,
          scope : this
        };

        this.loginForm = new Ext.form.FormPanel(this.loginFormConfig);
        this.loginWindow = new Ext.Window({
              id : 'login-window',
              title : 'Nexus Repository Manager Log In',
              animateTarget : Ext.get('head-link-r'),
              closable : true,
              closeAction : 'hide',
              onEsc : function() {
                if ( this.closable ) {
                  Ext.Window.prototype.onEsc.apply(this, arguments);
                }
              },
              autoWidth : false,
              width : 300,
              modal : true,
              constrain : true,
              resizable : false,
              draggable : false,
              items : [this.loginForm]
            });

        this.loginWindow.on('show', function() {
              var
                    panel, field,
                    // NEXUS-5108 hide close button when anon access is disabled
                    anonDisabled = Sonatype.utils.anonDisabled || false; // if anonDisabled is undefined -> boolean

              this.loginWindow.closable = !anonDisabled;
              this.loginWindow.tools.close.setVisible(!anonDisabled);

              field = this.loginForm.find('name', 'username')[0];
              if (field.getRawValue())
              {
                field = this.loginForm.find('name', 'password')[0];
              }
              field.focus(true, 100);
            }, this);

        this.loginWindow.on('close', function() {
              this.loginForm.getForm().reset();
            }, this);

        this.loginWindow.on('hide', function() {
              this.loginForm.getForm().reset();
              Sonatype.view.afterLoginToken = null;
            }, this);
      },

      // Add/Replace Nexus left hand components
      createSubComponents : function() {
        var wasRendered = this.nexusPanel.rendered;

        if (wasRendered)
        {
          this.nexusPanel.getEl().mask('Updating...', 'loading-indicator');
          this.nexusPanel.items.each(function(item, i, len) {
                this.remove(item, true);
              }, this.nexusPanel);
        }

        Sonatype.Events.fireEvent('nexusNavigationInit', this.nexusPanel);

        // fire second event so plugins can contribute navigation items, and set the order
        Sonatype.Events.fireEvent('nexusNavigationPostInit', this.nexusPanel);

        if (wasRendered)
        {
          this.nexusPanel.doLayout();
          this.nexusPanel.getEl().unmask();
          // this.nexusPanel.enable();
        }

        Sonatype.Events.fireEvent('nexusNavigationInitComplete', this.nexusPanel);

      },

      nexusStatusEvent : function() {

        // check the user status, if it is not set, then reset the panels
        if (!Sonatype.user.curr.repoServer)
        {
          this.resetMainTabPanel();
        }
      },

      addNexusNavigationItems : function(nexusPanel) {

        // Views Group **************************************************
        nexusPanel.add({
              title : 'Views/Repositories',
              id : 'st-nexus-views',
              items : [{
                    enabled : sp.checkPermission('nexus:repostatus', sp.READ),
                    title : 'Repositories',
                    tabId : 'view-repositories',
                    tabCode : Sonatype.repoServer.RepositoryPanel,
                    tabTitle : 'Repositories'
                  }, {
                    enabled : sp.checkPermission('nexus:targets', sp.READ) && (sp.checkPermission('nexus:targets', sp.CREATE) || sp.checkPermission('nexus:targets', sp.DELETE) || sp.checkPermission('nexus:targets', sp.EDIT)),
                    title : 'Repository Targets',
                    tabId : 'targets-config',
                    tabCode : Sonatype.repoServer.RepoTargetEditPanel
                  }, {
                    enabled : sp.checkPermission('nexus:routes', sp.READ) && (sp.checkPermission('nexus:routes', sp.CREATE) || sp.checkPermission('nexus:routes', sp.DELETE) || sp.checkPermission('nexus:routes', sp.EDIT)),
                    title : 'Routing',
                    tabId : 'routes-config',
                    tabCode : Sonatype.repoServer.RoutesEditPanel
                  }]
            });

        // Config Group **************************************************
        nexusPanel.add({
              title : 'Enterprise',
              id : 'st-nexus-enterprise'
            });

        // Security Group **************************************************
        nexusPanel.add({
              title : 'Security',
              id : 'st-nexus-security',
              collapsed : true,
              items : [
                  {
                    enabled : sp.checkPermission('security:users', sp.READ) && (sp.checkPermission('security:users', sp.CREATE) || sp.checkPermission('security:users', sp.DELETE) || sp.checkPermission('security:users', sp.EDIT)),
                    title : 'Users',
                    tabId : 'security-users',
                    tabCode : Sonatype.repoServer.UserEditPanel
                  }, {
                    enabled : sp.checkPermission('security:roles', sp.READ) && (sp.checkPermission('security:roles', sp.CREATE) || sp.checkPermission('security:roles', sp.DELETE) || sp.checkPermission('security:roles', sp.EDIT)),
                    title : 'Roles',
                    tabId : 'security-roles',
                    tabCode : Sonatype.repoServer.RoleEditPanel
                  }, {
                    enabled : sp.checkPermission('security:privileges', sp.READ) && (sp.checkPermission('security:privileges', sp.CREATE) || sp.checkPermission('security:privileges', sp.DELETE) || sp.checkPermission('security:privileges', sp.EDIT)),
                    title : 'Privileges',
                    tabId : 'security-privileges',
                    tabCode : Sonatype.repoServer.PrivilegeEditPanel
                  }]
            });

        // Config Group **************************************************
        nexusPanel.add({
              title : 'Administration',
              id : 'st-nexus-config',
              collapsed : true,
              items : [{
                    enabled : sp.checkPermission('nexus:settings', sp.READ) && (sp.checkPermission('nexus:settings', sp.CREATE) || sp.checkPermission('nexus:settings', sp.DELETE) || sp.checkPermission('nexus:settings', sp.EDIT)),
                    title : 'Server',
                    tabId : 'nexus-config',
                    tabCode : Sonatype.repoServer.ServerEditPanel,
                    tabTitle : 'nexus'
                  }, {
                    enabled : sp.checkPermission('nexus:tasks', sp.READ),
                    title : 'Scheduled Tasks',
                    tabId : 'schedules-config',
                    tabCode : Sonatype.repoServer.SchedulesEditPanel
                  }]
            });

        nexusPanel.add({
              title : 'Help',
              id : 'st-nexus-docs',
              collapsible : true,
              collapsed : true,
              items : [{
                    title : 'Welcome',
                    tabId : 'welcome',
                    tabCode : Sonatype.view.welcomePanel
                  },
                  {
                    title : 'About',
                    tabId : 'AboutNexus',
                    tabCode : Sonatype.repoServer.HelpAboutPanel
                  }, {
                    title : 'Browse Issue Tracker',
                    href : 'http://links.sonatype.com/products/nexus/oss/issues'
                  }, {
                    title : 'Documentation',
                    tabId : 'Documentation',
                    tabCode : Sonatype.repoServer.Documentation
                  }
              ]
            });
      },

      logout : function() {
        Sonatype.utils.refreshTask.stop();

        // do logout
        Ext.Ajax.request({
          scope : this,
          method : 'GET',
          url : Sonatype.config.repos.urls.logout,
          callback : function(options, success, response) {
            Sonatype.view.justLoggedOut = true;
            Sonatype.utils.loadNexusStatus();
            window.location.hash = 'welcome';
          }
        });
      },

      loginHandler : function() {
        var cp, username;

        if (Sonatype.user.curr.isLoggedIn)
        {
          this.logout();
        }
        else
        {
          this.loginForm.getForm().clearInvalid();
          cp = Sonatype.state.CookieProvider;
          username = cp.get('username', null);
          if (username)
          {
            this.loginForm.find('name', 'username')[0].setValue(username);
          }
          this.loginWindow.show();
        }
      },
      profileMenu : new Ext.menu.Menu({
        cls : 'user-profile-menu',
        shadow: false,
        items : [
          {
            text : 'Profile',
            handler : function() {
              Sonatype.view.mainTabPanel.addOrShowTab('profile', Nexus.profile.UserProfile);
            },
            listeners : {
              render : function(cmp) {
                cmp.setVisible(Sonatype.utils.editionShort === "OSS" || ( Sonatype.utils.licenseInstalled && !Sonatype.utils.licenseExpired));
              }
            }
          },
          {
            text : 'Logout',
            handler : function() {
              Sonatype.repoServer.RepoServer.loginHandler();
            }
          }
        ],
        listeners : {
          show : function() {
            Ext.get('head-link-r').addClass('profile-menu-visible')
          },
          hide : function() {
            Ext.get('head-link-r').removeClass('profile-menu-visible')
          }
        }
      }),

      /**
       * Shows the profile menu at the position of the scope of this function.
       */
      showProfileMenu : function() {
        Sonatype.repoServer.RepoServer.profileMenu.show(this);
      },

      resetMainTabPanel : function() {
        Sonatype.view.mainTabPanel.items.each(function(item, i, len) {
              this.remove(item, true);
            }, Sonatype.view.mainTabPanel);
        Sonatype.view.mainTabPanel.activeTab = null;
        Sonatype.view.supportedNexusTabs = {};

        var welcomePanelConfig = {
          layout : 'auto',
          width : 670,
          closable : false,
          items : []
        };
        var welcomeTabConfig = {
          title : 'Welcome',
          id : 'welcome',
          closable : false,
          items : [{
                layout : 'column',
                border : false,
                defaults : {
                  border : false,
                  style : 'padding-top: 30px;'
                },
                items : [{
                      columnWidth : .5,
                      html : '&nbsp;'
                    }, welcomePanelConfig, {
                      columnWidth : .5,
                      html : '&nbsp;'
                    }]
              }],
          listeners : {
            render : {
              fn : function() {
                Sonatype.Events.fireEvent('welcomeTabRender');
              },
              single : true,
              delay : 300
            }
          }
        };

        var welcomeMsg = '<p style="text-align:center;"><a href="http://nexus.sonatype.org" target="new">' + '<img src="images/nexus650x55.png" border="0" alt="Welcome to the Nexus Repository Manager"></a>' + '</p>';

        var statusEnabled = sp.checkPermission('nexus:status', sp.READ);
        if (Sonatype.utils.anonDisabled && (!Sonatype.user.curr.isLoggedIn)) {
          // show login window if anonymous access is disabled (unauthorized for status resource)
          Sonatype.repoServer.RepoServer.loginHandler();
        } else if (!statusEnabled) {
          // other error, could not retrieve status. Backend down.
          welcomeMsg += '</br><p style="color:red">Warning: Could not connect to Nexus.</p>';
        }

        welcomePanelConfig.items.push({
              border : false,
              html : '<div class="little-padding">' + welcomeMsg + '</div>'
            });

        var itemCount = welcomePanelConfig.items.length;

        Sonatype.Events.fireEvent('welcomePanelInit', this, welcomePanelConfig);
        Sonatype.Events.fireEvent('welcomeTabInit', this, welcomeTabConfig);

        // If nothing was added, then add the default blurb, if perm'd of course
        if (welcomePanelConfig.items.length <= itemCount && sp.checkPermission('nexus:repostatus', sp.READ))
        {
          welcomePanelConfig.items.push({
                layout : 'form',
                border : false,
                frame : false,
                labelWidth : 10,
                items : [{
                      border : false,
                      html : '<div class="little-padding">' + '<br/><p>You may browse the repositories using the options on the left.</p>' + '</div>'
                    }]
              });
        }

        Sonatype.view.welcomePanel = function(config) {
          var cfg = config || {},
              defaultConfig = {};

          Ext.apply(this, cfg, defaultConfig);

          Sonatype.view.welcomePanel.superclass.constructor.call(this, welcomeTabConfig );
        };

        Ext.extend(Sonatype.view.welcomePanel, Ext.Panel);

        Sonatype.view.welcomeTab = new Sonatype.view.welcomePanel(welcomeTabConfig);
        Sonatype.view.mainTabPanel.add(Sonatype.view.welcomeTab);

        // set closable to false again, mainTabPanel.defaults contains 'closable : true'
        Sonatype.view.welcomeTab.closable = false;

        // HACK: close is not a tool button, so we need to hide it manually
        Ext.get(Sonatype.view.welcomeTab.tabEl).down('a.x-tab-strip-close').toggleClass('x-hidden');

        Sonatype.view.mainTabPanel.setActiveTab(Sonatype.view.welcomeTab);
      },

    };
  })();

});
