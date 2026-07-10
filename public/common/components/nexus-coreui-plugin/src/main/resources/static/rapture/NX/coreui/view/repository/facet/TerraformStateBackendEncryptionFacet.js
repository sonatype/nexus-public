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
 * Configuration specific to Terraform State Backend encryption.
 *
 * Encryption is MANDATORY for all Terraform State Backend repositories.
 * State files contain sensitive infrastructure data and must be encrypted at rest.
 */
Ext.define('NX.coreui.view.repository.facet.TerraformStateBackendEncryptionFacet', {
    extend: 'Ext.form.FieldContainer',
    alias: 'widget.nx-coreui-repository-terraformstatebackend-encryption-facet',
    requires: [
        'NX.I18n'
    ],

    /**
     * @override
     */
    initComponent: function() {
        var me = this;

        me.items = [
            {
                xtype: 'fieldset',
                cls: 'nx-form-section',
                title: NX.I18n.get('Repository_Facet_TerraformStateBackendEncryptionFacet_Title'),
                items: [
                    {
                        xtype: 'displayfield',
                        value: NX.I18n.get('Repository_Facet_TerraformStateBackendEncryptionFacet_Required_Info'),
                        cls: 'nx-info-alert',
                        margin: '0 0 15 0'
                    },
                    {
                        xtype: 'hidden',
                        name: 'attributes.terraformStateBackend.encryption.enabled',
                        value: 'true'
                    },
                    {
                        xtype: 'nx-password',
                        name: 'attributes.terraformStateBackend.encryption.encryptionKey',
                        fieldLabel: NX.I18n.get('Repository_Facet_TerraformStateBackendEncryptionFacet_EncryptionKey_FieldLabel'),
                        helpText: NX.I18n.get('Repository_Facet_TerraformStateBackendEncryptionFacet_EncryptionKey_HelpText'),
                        allowBlank: false
                    },
                    {
                        xtype: 'nx-password',
                        name: 'attributes.terraformStateBackend.encryption.confirmEncryptionKey',
                        fieldLabel: NX.I18n.get('Repository_Facet_TerraformStateBackendEncryptionFacet_ConfirmEncryptionKey_FieldLabel'),
                        helpText: NX.I18n.get('Repository_Facet_TerraformStateBackendEncryptionFacet_ConfirmEncryptionKey_HelpText'),
                        allowBlank: false,
                        validator: function(value) {
                            var form = this.up('form');
                            var encryptionKeyField = form.down('[name=attributes.terraformStateBackend.encryption.encryptionKey]');
                            if (encryptionKeyField && value !== encryptionKeyField.getValue()) {
                                return NX.I18n.get('Repository_Facet_TerraformStateBackendEncryptionFacet_KeyMismatch_HelpText');
                            }
                            return true;
                        }
                    },
                    {
                        xtype: 'numberfield',
                        name: 'attributes.terraformStateBackend.lockTimeoutMinutes',
                        fieldLabel: NX.I18n.get('Repository_Facet_TerraformStateBackendEncryptionFacet_LockTimeout_FieldLabel'),
                        helpText: NX.I18n.get('Repository_Facet_TerraformStateBackendEncryptionFacet_LockTimeout_HelpText'),
                        value: 30,
                        minValue: 1,
                        maxValue: 1440,
                        allowDecimals: false
                    },
                    {
                        xtype: 'numberfield',
                        name: 'attributes.terraformStateBackend.maxStateSizeMB',
                        fieldLabel: NX.I18n.get('Repository_Facet_TerraformStateBackendEncryptionFacet_MaxStateSize_FieldLabel'),
                        helpText: NX.I18n.get('Repository_Facet_TerraformStateBackendEncryptionFacet_MaxStateSize_HelpText'),
                        value: 256,
                        minValue: 1,
                        maxValue: 512,
                        allowDecimals: false
                    }
                ]
            }
        ];

        me.callParent();
    }
});
