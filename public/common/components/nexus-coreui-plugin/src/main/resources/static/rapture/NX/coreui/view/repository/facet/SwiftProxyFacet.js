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
 * Auto-block HTTP status codes field for Swift proxy repositories.
 *
 * Rendered as a plain text field (comma-separated status codes) so the recipe can inject it
 * directly beneath the shared auto-block checkbox rather than in its own section.
 */
Ext.define('NX.coreui.view.repository.facet.SwiftProxyFacet', {
    extend: 'Ext.form.field.Text',
    alias: 'widget.nx-coreui-repository-swift-proxy-facet',
    requires: [
        'NX.I18n'
    ],

    name: 'attributes.httpclient.autoBlockStatusCodes',
    itemId: 'autoBlockStatusCodes',
    allowBlank: true,

    /**
     * @override
     */
    initComponent: function() {
        var me = this;

        me.fieldLabel = NX.I18n.get('Repository_Facet_SwiftProxyFacet_AutoBlockStatusCodes_FieldLabel');
        me.helpText = NX.I18n.get('Repository_Facet_SwiftProxyFacet_AutoBlockStatusCodes_HelpText');
        me.emptyText = NX.I18n.get('Repository_Facet_SwiftProxyFacet_AutoBlockStatusCodes_EmptyText');

        // Reject the whole input if any comma-separated token is not a whole integer in [100, 599].
        // An empty/blank value is valid and means "use the format default".
        me.validator = function(value) {
            var tokens, i, token, code, seen;

            if (!value || value.trim() === '') {
                return true;
            }

            seen = {};
            tokens = value.split(',');
            for (i = 0; i < tokens.length; i++) {
                token = tokens[i].trim();
                if (token === '') {
                    continue;
                }
                code = parseInt(token, 10);
                if (!/^\d+$/.test(token) || code < 100 || code > 599) {
                    return NX.I18n.get('Repository_Facet_SwiftProxyFacet_AutoBlockStatusCodes_ValidationError');
                }
                if (seen[code]) {
                    return NX.I18n.get('Repository_Facet_SwiftProxyFacet_AutoBlockStatusCodes_DuplicateError');
                }
                seen[code] = true;
            }

            return true;
        };

        me.callParent();
    },

    // Convert comma-separated string to array of integers on submit
    getSubmitValue: function() {
        var value = this.getValue();
        if (!value || value.trim() === '') {
            return [];
        }
        return value.split(',').map(function(code) {
            return parseInt(code.trim(), 10);
        }).filter(function(code) {
            return !isNaN(code);
        });
    },

    // Bypass BasicForm toString coercion to preserve array type on submit
    getModelData: function() {
        var data = {};
        data[this.getName()] = this.getSubmitValue();
        return data;
    },

    // Convert array to comma-separated string on load
    setValue: function(value) {
        if (Array.isArray(value)) {
            value = value.join(', ');
        }
        return Ext.form.field.Text.prototype.setValue.call(this, value);
    }
});
