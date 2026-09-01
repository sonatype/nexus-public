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
 * Repository "Settings" form for a Docker Proxy repository.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.repository.recipe.DockerProxy', {
  extend: 'NX.coreui.view.repository.RepositorySettingsForm',
  alias: 'widget.nx-coreui-repository-docker-proxy',
  requires: [
    'NX.coreui.view.repository.facet.ProxyFacet',
    'NX.coreui.view.repository.facet.StorageFacet',
    'NX.coreui.view.repository.facet.RoutingRuleFacet',
    'NX.coreui.view.repository.facet.HttpClientFacet',
    'NX.coreui.view.repository.facet.NegativeCacheFacet',
    'NX.coreui.view.repository.facet.DockerConnectorFacet',
    'NX.coreui.view.repository.facet.DockerProxyFacet',
    'NX.coreui.view.repository.facet.DockerV1Facet',
    'NX.coreui.view.repository.facet.CleanupPolicyFacet',
    'NX.coreui.view.repository.facet.FirewallFacet'
  ],

  // Matches an AWS ECR remote URL: <12-digit-accountId>.dkr.ecr.<region>.amazonaws.com
  // (including GovCloud / ISO / China partitions). Kept in sync with the backend EcrUrlParser
  // detection and the React ECR_URL_PATTERN gate.
  ecrUrlPattern: /^https?:\/\/\d{12}\.dkr\.ecr\.[a-z0-9-]+\.amazonaws\.com(\.cn)?\/?/i,

  /**
   * @override
   */
  initComponent: function() {
    var me = this;

    me.items = [
      {xtype: 'nx-coreui-repository-docker-connector-facet'},
      {xtype: 'nx-coreui-repository-docker-v1-facet'},
      {xtype: 'nx-coreui-repository-firewall-facet'},
      {xtype: 'nx-coreui-repository-proxy-facet'},
      {xtype: 'nx-coreui-repository-storage-facet'},
      {xtype: 'nx-coreui-repository-routing-rule-facet'},
      {xtype: 'nx-coreui-repository-negativecache-facet'},
      {xtype: 'nx-coreui-repository-cleanup-policy-facet'},
      {xtype: 'nx-coreui-repository-httpclient-facet'}
    ];

    me.callParent();

    me.down('#remoteUrl').setHelpText(NX.I18n.get('Repository_Facet_ProxyFacet_Docker_Remote_HelpText'));
    me.down('#proxyFieldSet').add(1, {xtype: 'nx-coreui-repository-docker-proxy-facet'});

    me.insertEcrSessionTokenField();
  },

  /**
   * Inserts the optional AWS ECR session token field into the shared HTTP authentication
   * fieldset, directly below the Password field, so all three AWS credential parts live
   * together: Access Key ID (username), Secret Access Key (password) and this session token.
   *
   * The field is only visible for AWS ECR remote URLs when the "username" auth type is
   * selected. Visibility tracks the remote URL and auth type changes.
   */
  insertEcrSessionTokenField: function() {
    var me = this,
        passwordField = me.down('#attributes_httpclient_authentication_password');

    if (!passwordField) {
      return;
    }

    var authFieldset = passwordField.up('nx-optionalfieldset[checkboxName=authEnabled]');
    if (!authFieldset) {
      return;
    }

    var passwordIdx = authFieldset.items.indexOf(passwordField);
    authFieldset.insert(passwordIdx + 1, {
      xtype: 'nx-password',
      itemId: 'ecrSessionToken',
      name: 'attributes.dockerProxy.ecrAuth.sessionToken',
      fieldLabel: NX.I18n.get('Repository_Facet_DockerProxyFacet_EcrSessionToken_FieldLabel'),
      helpText: NX.I18n.get('Repository_Facet_DockerProxyFacet_EcrSessionToken_HelpText'),
      allowBlank: true,
      hidden: true
    });

    var remoteUrlField = me.down('#remoteUrl'),
        authTypeCombo = me.down('[name=attributes.httpclient.authentication.type]');

    if (remoteUrlField) {
      remoteUrlField.on('change', me.updateEcrSessionTokenVisibility, me);
    }
    if (authTypeCombo) {
      authTypeCombo.on('change', me.updateEcrSessionTokenVisibility, me);
    }

    me.on('afterrender', me.updateEcrSessionTokenVisibility, me);
    me.on('boxready', me.updateEcrSessionTokenVisibility, me);
  },

  /**
   * Shows the ECR session token field only for ECR remote URLs with "username" auth type.
   */
  updateEcrSessionTokenVisibility: function() {
    var me = this,
        field = me.down('#ecrSessionToken');

    if (!field) {
      return;
    }

    var remoteUrlField = me.down('#remoteUrl'),
        authTypeCombo = me.down('[name=attributes.httpclient.authentication.type]'),
        remoteUrl = remoteUrlField ? remoteUrlField.getValue() : '',
        authType = authTypeCombo ? authTypeCombo.getValue() : 'username',
        isEcr = me.ecrUrlPattern.test(remoteUrl || '') && authType === 'username';

    field.setVisible(isEcr);
  },

  loadRecord: function(record) {
    var me = this,
        dockerProxyFacet = me.down('nx-coreui-repository-docker-proxy-facet'),
        urls = record.get('attributes')['dockerProxy']['foreignLayerUrlWhitelist'];

    dockerProxyFacet.resetWhitelist();
    dockerProxyFacet.formLoad = true;
    me.callParent(arguments);

    if (urls) {
      urls.forEach(function(url) {
        dockerProxyFacet.addWhitelistRow(url);
      });
    }

    dockerProxyFacet.formLoad = false;
  }
});
