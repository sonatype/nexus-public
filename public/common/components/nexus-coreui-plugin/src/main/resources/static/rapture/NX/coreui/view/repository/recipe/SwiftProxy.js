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
/**
 * Repository "Settings" form for a Swift Proxy repository
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.repository.recipe.SwiftProxy', {
    extend: 'NX.coreui.view.repository.RepositorySettingsForm',
    alias: 'widget.nx-coreui-repository-swift-proxy',
    requires: [
        'NX.coreui.view.repository.facet.ProxyFacet',
        'NX.coreui.view.repository.facet.StorageFacet',
        'NX.coreui.view.repository.facet.RoutingRuleFacet',
        'NX.coreui.view.repository.facet.BearerHttpClientFacet',
        'NX.coreui.view.repository.facet.NegativeCacheFacet',
        'NX.coreui.view.repository.facet.CleanupPolicyFacet',
        'NX.coreui.view.repository.facet.SwiftProxyFacet',
        'NX.coreui.view.repository.facet.FirewallFacet'
    ],

    /**
     * @override
     */
    initComponent: function() {
        var me = this;
        var swiftFirewallEnabled = NX.State.getValue('swiftFirewallEnabled');

        me.items = [];

        if (swiftFirewallEnabled) {
            me.items.push({xtype: 'nx-coreui-repository-firewall-facet'});
        }

        me.items = me.items.concat([
            {xtype: 'nx-coreui-repository-proxy-facet'},
            {xtype: 'nx-coreui-repository-storage-facet'},
            {xtype: 'nx-coreui-repository-routing-rule-facet'},
            {xtype: 'nx-coreui-repository-negativecache-facet'},
            {xtype: 'nx-coreui-repository-cleanup-policy-facet'},
            {xtype: 'nx-coreui-repository-httpclient-facet-with-bearer-token'}
        ]);

        me.callParent();
        var remoteUrlField = me.down('#remoteUrl');
        remoteUrlField.setHelpText(NX.I18n.get('Repository_Facet_SwiftFacet_Remote_HelpText'));

        // Swift proxies may only proxy the GitHub root URL. The Swift format's fetch
        // path is hardcoded to github.com, so any other remote yields a repository that cannot serve
        // packages. Exact string comparison against the two accepted values -- NOT a prefix match,
        // which would admit 'https://github.com.evil.example/', and not URL parsing.
        //
        // Case-sensitive by design: Swift's provider lookup matches the host case-sensitively, and a
        // mixed-case remote makes '.swift'-suffixed packages (URITemplate.swift) fail intermittently.
        // The shared URL validation applied to this field is case-insensitive, so this rule is added
        // on top of it rather than replacing it.
        //
        // Ext.form.field.Text defaults allowOnlyWhitespace to true, which would let a
        // whitespace-only value satisfy allowBlank:false. Turning it off makes the field's own
        // "required" message cover blank AND whitespace-only input, matching the React UI's
        // not-blank rule so the two surfaces surface the same message for the same input.
        remoteUrlField.allowOnlyWhitespace = false;

        remoteUrlField.validator = function(value) {
            // Defer to the field's allowBlank/allowOnlyWhitespace handling so the "required"
            // message is unchanged for blank input.
            if (!value || !value.trim()) {
                return true;
            }
            if (value === 'https://github.com/' || value === 'https://github.com') {
                return true;
            }
            return NX.I18n.get('Repository_Facet_SwiftFacet_Remote_ValidationError');
        };

        // Place the Swift-only auto-block HTTP status codes field directly below the
        // auto-block checkbox in the shared proxy fieldset, rather than in its own section.
        var proxyFieldSet = me.down('#proxyFieldSet');
        var autoBlockField = me.down('checkbox[name=attributes.httpclient.autoBlock]');
        if (proxyFieldSet && autoBlockField) {
            proxyFieldSet.insert(proxyFieldSet.items.indexOf(autoBlockField) + 1,
                {xtype: 'nx-coreui-repository-swift-proxy-facet'});
        }
    }
});
