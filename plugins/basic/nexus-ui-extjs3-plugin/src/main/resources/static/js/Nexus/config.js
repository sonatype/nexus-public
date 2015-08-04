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
define('Nexus/config',['extjs', 'Nexus/messagebox', 'Sonatype/init', 'Nexus/configuration/Ajax'], function(Ext, mbox, Sonatype) {

  // ********* Set ExtJS options
  // *************************************************


  // set Sonatype defaults for Ext widgets
  Ext.form.Field.prototype.msgTarget = 'under';

  mbox.minWidth = 200;

  Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

  Sonatype.config = (function() {
    var
          host = window.location.protocol + '//' + window.location.host,
          path = window.location.pathname,
          contextPath = path.substr(0, path.lastIndexOf('/')),
          servicePathSnippet = '/service/local',
          servicePath = contextPath + servicePathSnippet,
          browsePathSnippet = '/content',
          contentPath = contextPath + browsePathSnippet,
          repoBrowsePathSnippet = browsePathSnippet + '/repositories',
          groupBrowsePathSnippet = browsePathSnippet + '/groups',
          repoServicePathSnippet = servicePathSnippet + '/repositories',
          groupServicePathSnippet = servicePathSnippet + '/repo_groups';

    return {
      isDebug : window.location.search === '?debug',
      host : host,
      contextPath : contextPath,
      servicePath : servicePath,
      // @deprecated use contextPath
      resourcePath : contextPath,
      extPath : contextPath + '/static/ext-3.4.1.1',
      contentPath : contentPath,
      cssPath : '/styles',
      jsPath : '/js',
      browsePathSnippet : browsePathSnippet,

      installedServers : {
        repoServer : true
      },

      repos : {
        snippets : {
          repoBrowsePathSnippet : repoBrowsePathSnippet,
          groupBrowsePathSnippet : groupBrowsePathSnippet,
          repoServicePathSnippet : repoServicePathSnippet,
          groupServicePathSnippet : groupServicePathSnippet
        },
        urls : {
          login : servicePath + '/authentication/login',
          logout : servicePath + '/authentication/logout',
          globalSettings : servicePath + '/global_settings',
          globalSettingsState : servicePath + '/global_settings/current',
          restApiSettings: servicePath + '/rest_api_settings',
          repositories : servicePath + '/repositories',
          allRepositories : servicePath + '/all_repositories',
          repositoryStatuses : servicePath + '/repository_statuses',
          repoTemplates : servicePath + '/templates/repositories',
          repoTemplate : {
            virtual : servicePath + '/templates/repositories/default_virtual_m2_m1',
            hosted : servicePath + '/templates/repositories/default_hosted_release', // default
            hosted_release : servicePath + '/templates/repositories/default_hosted_release',
            hosted_snapshot : servicePath + '/templates/repositories/default_hosted_snapshot',
            proxy : servicePath + '/templates/repositories/default_proxy_release', // default
            proxy_release : servicePath + '/templates/repositories/default_proxy_release',
            proxy_snapshot : servicePath + '/templates/repositories/default_proxy_snapshot'
          },

          metadata : servicePath + '/metadata',
          cache : servicePath + '/data_cache',
          groups : servicePath + '/repo_groups',
          routes : servicePath + '/repo_routes',
          configs : servicePath + '/configs',
          configCurrent : servicePath + '/configs/current',
          status : servicePath + '/status?perms=1',
          schedules : servicePath + '/schedules',
          scheduleRun : servicePath + '/schedule_run',
          scheduleTypes : servicePath + '/schedule_types',
          upload : servicePath + '/artifact/maven/content',
          redirect : servicePath + '/artifact/maven/redirect',
          trash : servicePath + '/wastebasket',
          plexusUsersAllConfigured : servicePath + '/plexus_users/allConfigured',
          plexusUsersDefault : servicePath + '/plexus_users/default',
          plexusUsers : servicePath + '/plexus_users',
          userLocators : servicePath + '/components/userLocators',
          searchUsers : servicePath + '/user_search',
          plexusUser : servicePath + '/plexus_user',
          userToRoles : servicePath + '/user_to_roles',
          users : servicePath + '/users',
          usersReset : servicePath + '/users_reset',
          usersForgotId : servicePath + '/users_forgotid',
          usersForgotPassword : servicePath + '/users_forgotpw',
          usersChangePassword : servicePath + '/users_changepw',
          usersSetPassword : servicePath + '/users_setpw',
          roles : servicePath + '/roles',
          plexusRoles : servicePath + '/plexus_roles',
          plexusRolesAll : servicePath + '/plexus_roles/all',
          externalRolesAll : servicePath + '/external_role_map/all',
          privileges : servicePath + '/privileges',
          repoTargets : servicePath + '/repo_targets',
          repoContentClasses : servicePath + '/components/repo_content_classes',
          realmComponents : servicePath + '/components/realm_types',
          repoTypes : servicePath + '/components/repo_types',
          repoMirrors : servicePath + '/repository_mirrors',
          repoPredefinedMirrors : servicePath + '/repository_predefined_mirrors',
          privilegeTypes : servicePath + '/privilege_types',
          smtpSettingsState : servicePath + '/check_smtp_settings'
        }
      },

      content : {
        groups : contentPath + '/groups',
        repositories : contentPath + '/repositories'
      }
    };
  }());

  return Sonatype.config;
});
