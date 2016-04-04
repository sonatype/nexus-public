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
 * Configuration for Repository Groups.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.repository.facet.GroupFacet', {
  extend: 'Ext.form.FieldContainer',
  alias: 'widget.nx-coreui-repository-group-facet',
  requires: [
    'NX.I18n',
    'NX.coreui.store.RepositoryReference'
  ],

  /**
   * @cfg String
   * Set the format to narrow the format of groups available to choose from.
   */
  format: undefined,

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    me.repositoryStore = Ext.create('NX.coreui.store.RepositoryReference', {remote: true});
    me.repositoryStore.filter([
      {property: 'format', value: me.format}
    ]);
    me.repositoryStore.load(function (records, operation, success) {
      //TODO - KR hackity hack, but it appears that the store loading somehow unsets values?
      var form = me.up('form');
      if (form) {
        var record = form.getRecord();
        if (record) {
          me.repositoryStore.filter([
            {property: 'format', value: me.format},
            {
              filterFn: function (item) {
                return item.get('name') !== record.get('name');
              }
            }
          ]);
          var memberNames = record.get('attributes').group.memberNames;
          form.down('#groupMemberNames').setValue(memberNames);
          // clears isDirty state after setting the value
          form.down('#groupMemberNames').resetOriginalValue();
        }
      }
    });
    me.items = [
      {
        xtype: 'fieldset',
        cls: 'nx-form-section',
        title: NX.I18n.get('Repository_Facet_GroupFacet_Title'),

        items: {
          xtype: 'nx-itemselector',
          name: 'attributes.group.memberNames',
          itemId: 'groupMemberNames',
          fieldLabel: NX.I18n.get('Repository_Facet_GroupFacet_Members_FieldLabel'),
          helpText: NX.I18n.get('Repository_Facet_GroupFacet_Members_HelpText'),
          buttons: ['up', 'add', 'remove', 'down'],
          fromTitle: NX.I18n.get('Repository_Facet_GroupFacet_Members_FromTitle'),
          toTitle: NX.I18n.get('Repository_Facet_GroupFacet_Members_ToTitle'),
          store: me.repositoryStore,
          valueField: 'id',
          displayField: 'name',
          allowBlank: false,
          delimiter: null,
          forceSelection: true,
          queryMode: 'local',
          triggerAction: 'all',
          selectOnFocus: false,
          itemCls: 'required-field'
        }
      }
    ];

    me.callParent();
  }

});
