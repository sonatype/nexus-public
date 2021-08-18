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
 * Configuration for Nuget Repository Groups.
 *
 * @since 3.24
 */
Ext.define('NX.coreui.view.repository.facet.NugetGroupFacet', {
  extend: 'Ext.form.FieldContainer',
  alias: 'widget.nx-coreui-repository-nuget-group-facet',
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
  initComponent: function() {
    var me = this;

    var form = me.up('form');
    me.repositoryStore = Ext.create('NX.coreui.store.RepositoryReference', {remote: true, sorters: undefined});
    me.repositoryStore.filter([
      {property: 'format', value: me.format}
    ]);
    me.repositoryStore.load(function(records, operation, success) {
      if (form) {
        var record = form.getRecord();
        if (record) {
          me.repositoryStore.filter([
            {property: 'format', value: me.format},
            {
              filterFn: function(item) {
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

    var nugetRepositories = [];
    Ext.Ajax.request({
      url: NX.util.Url.relativePath + '/service/rest/beta/repositories',
      method: 'GET',
      headers: {'Content-Type': 'application/json'},
      success: function(response) {
        var repositories = JSON.parse(response.responseText);
        Ext.each(repositories, function(repository) {
          if (repository.format === me.format) {
            nugetRepositories = nugetRepositories.concat(
                getNugetVersionToRepositories(repository.name, repositories, []));
          }
        });
      }
    });

    function getNugetVersionToRepositories(repositoryName, repositories, groupParents) {
      var repository = repositories
          .filter(function(repository) {
            return repository.name === repositoryName;
          })[0];

      var result = [];
      if (repository.proxy) {
        groupParents.push(repository.name);
        result.push({
          repositories: groupParents,
          nugetVersion: repository.nugetProxy.nugetVersion
        });
      }
      else if (repository.group) {
        groupParents.push(repository.name);
        Ext.each(repository.group.memberNames, function(repositoryGroupMember) {
          result = result.concat(getNugetVersionToRepositories(repositoryGroupMember, repositories, groupParents));
        });
      }

      return result;
    }

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
          itemCls: 'required-field',
          listeners: {
            change: function(field, newValues, oldValues, eOpts) {
              var nugetGroupValidationField = form.down("#nugetGroupValidationLabel");

              var isSaveButtonDisabled = false;
              var isFirstRepositoryV3;
              var firstRepositoryName;
              Ext.each(newValues, function (repositoryName) {
                var isV3Repository = isNugetV3Version(repositoryName);
                if (isV3Repository === undefined) {
                  return true;
                }
                if (isFirstRepositoryV3 === undefined) {
                  firstRepositoryName = repositoryName;
                  isFirstRepositoryV3 = isV3Repository;
                }
                else {
                  var isNextSameAsFirst = !Boolean(isFirstRepositoryV3 ^ isV3Repository);
                  if (!isNextSameAsFirst) {
                    isSaveButtonDisabled = true;
                    nugetGroupValidationField.setValue(
                        NX.I18n.format('Repository_Facet_NugetGroupFacet_NugetGroupValidationLabel', repositoryName,
                            getNugetVersionString(isV3Repository),
                            firstRepositoryName,
                            getNugetVersionString(isFirstRepositoryV3)));
                    return false;
                  }
                }
              });

              checkSaveButtonState(isSaveButtonDisabled, nugetGroupValidationField);
              if (isSaveButtonDisabled) {
                field.resetOriginalValue();
              }
            }
          }
        }
      },
      {
        xtype: 'displayfield',
        itemId: 'nugetGroupValidationLabel'
      }
    ];

    function checkSaveButtonState (isSaveButtonDisabled, nugetGroupValidationField) {
      var saveButton = Ext.ComponentQuery.query('button[action=save]')[0];
      var addButton = Ext.ComponentQuery.query('button[action=add]')[1];
      if (isSaveButtonDisabled) {
        nugetGroupValidationField.setVisible(true);
        if (saveButton) {
          saveButton.setDisabled(true);
        }
        if (addButton) {
          addButton.setDisabled(true);
        }
      }
      else {
        nugetGroupValidationField.setVisible(false);
        if (saveButton) {
          saveButton.setDisabled(false);
        }
        if (addButton) {
          addButton.setDisabled(false);
        }
      }
    }

    function getNugetVersionString(isV3Version) {
      return isV3Version ? 'v3' : 'v2';
    }

    // return undefined if {repositoryName} belongs to hosted
    function isNugetV3Version(repositoryName) {
      return nugetRepositories
          .filter(function(repository) {
            return repository.repositories.includes(repositoryName);
          })
          .map(function(repository) {
            return repository.nugetVersion === 'V3';
          })[0];
    }

    me.callParent();
  }
});
