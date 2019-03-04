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
 * CoreUi plugin configuration.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.app.PluginConfig', {
  '@aggregate_priority': 100,

  namespaces: [
    'NX.coreui'
  ],

  requires: [
    'NX.coreui.app.PluginStrings'
  ],

  controllers: [
    {
      id: 'NX.coreui.controller.Api',
      active: function() {
        return NX.State.getValue('api') &&
            NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.Assets',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.AnonymousSettings',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.BrowseableFormats',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.Capabilities',
      active: function() {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.ComponentAssetTree',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin') &&
            NX.State.getValue('browseableformats').length > 0;
      }
    },
    'NX.coreui.controller.FeatureGroups',
    {
      id: 'NX.coreui.controller.HttpSettings',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.HealthCheckInfo',
      active: function () {
        return NX.app.Application.bundleActive('com.sonatype.nexus.plugins.nexus-clm-oss-plugin')
            || NX.app.Application.bundleActive('com.sonatype.nexus.plugins.nexus-clm-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.HealthCheckRepositoryColumn',
      active: function () {
        return NX.app.Application.bundleActive('com.sonatype.nexus.plugins.nexus-clm-oss-plugin')
            || NX.app.Application.bundleActive('com.sonatype.nexus.plugins.nexus-clm-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.HealthCheckRepositoryConfiguration',
      active: function () {
        return NX.app.Application.bundleActive('com.sonatype.nexus.plugins.nexus-clm-oss-plugin')
            || NX.app.Application.bundleActive('com.sonatype.nexus.plugins.nexus-clm-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.Clm',
      active: function () {
        return NX.app.Application.bundleActive('com.sonatype.nexus.plugins.nexus-clm-oss-plugin')
            || NX.app.Application.bundleActive('com.sonatype.nexus.plugins.nexus-clm-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.FirewallRepositoryColumn',
      active: function () {
        return NX.app.Application.bundleActive('com.sonatype.nexus.plugins.nexus-clm-oss-plugin')
            || NX.app.Application.bundleActive('com.sonatype.nexus.plugins.nexus-clm-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.LdapServers',
      active: function () {
        return NX.app.Application.bundleActive('com.sonatype.nexus.plugins.nexus-ldap-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.Log',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.Loggers',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.Metrics',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.MetricHealth',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    {
      id: 'NX.coreui.migration.Controller',
      active: function () {
        return NX.State.getValue('migration', {})['enabled'] &&
            NX.app.Application.bundleActive('com.sonatype.nexus.plugins.nexus-migration-plugin');
      }
    },
    { id: 'NX.coreui.controller.NuGetApiKey',
      active: function () {
        return NX.app.Application.bundleActive('com.sonatype.nexus.plugins.nexus-repository-nuget');
      }
    },
    {
      id: 'NX.coreui.controller.Outreach',
      active: function () {
        return NX.app.Application.bundleActive('com.sonatype.nexus.plugins.nexus-outreach-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.Bundles',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.Repositories',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.Blobstores',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    { id: 'NX.coreui.controller.Licensing',
      active: function () {
        return NX.app.Application.bundleActive('com.sonatype.nexus.plugins.nexus-licensing-plugin');
      }
    },
    { id: 'NX.coreui.controller.LicenseUsers',
      active: function () {
        return NX.app.Application.bundleActive('com.sonatype.nexus.plugins.nexus-licensing-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.Nodes',
      active: function () {
            return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.Privileges',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.RealmSettings',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.Roles',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.Search',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.SearchBower',
      active: function () {
        return NX.app.Application.bundleActive('com.sonatype.nexus.plugins.nexus-repository-bower');
      }
    },
    {
      id: 'NX.coreui.controller.SearchDocker',
      active: function () {
        return NX.app.Application.bundleActive('com.sonatype.nexus.plugins.nexus-repository-docker');
      }
    },
    {
      id: 'NX.coreui.controller.SearchGitLfs',
      active: function () {
        return NX.app.Application.bundleActive('com.sonatype.nexus.plugins.nexus-repository-gitlfs');
      }
    },
    {
      id: 'NX.coreui.controller.SearchMaven',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-repository-maven');
      }
    },
    {
      id: 'NX.coreui.controller.SearchNpm',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-repository-npm');
      }
    },
    {
      id: 'NX.coreui.controller.SearchNuget',
      active: function () {
        return NX.app.Application.bundleActive('com.sonatype.nexus.plugins.nexus-repository-nuget');
      }
    },
    {
      id: 'NX.coreui.controller.SearchPyPi',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-repository-pypi');
      }
    },
    {
      id: 'NX.coreui.controller.SearchRaw',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-repository-raw');
      }
    },
    {
      id: 'NX.coreui.controller.SearchRubygems',
      active: function () {
        return NX.app.Application.bundleActive('com.sonatype.nexus.plugins.nexus-repository-rubygems');
      }
    },
    {
      id: 'NX.coreui.controller.SearchYum',
      active: function () {
        return NX.app.Application.bundleActive('com.sonatype.nexus.plugins.nexus-repository-yum');
      }
    },
    {
      id: 'NX.coreui.controller.Selectors',
      active: function() {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.SmtpSettings',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.SslCertificates',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-ssl-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.SslTrustStore',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-ssl-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.SupportRequest',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.SupportZip',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.SysInfo',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.Tasks',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.UploadComponent',
      active: function () {
        return NX.State.getValue('upload') &&
          NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.Users',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.HealthCheckWarnings',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.CleanupPolicies',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.RoutingRules',
      active: function() {
        return NX.State.getValue('routingRules') &&
            NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    }
  ]
});
