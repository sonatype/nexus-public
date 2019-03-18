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
 * Routing Rules controller.
 *
 * @since 3.next
 */
Ext.define('NX.coreui.controller.RoutingRules', {
  extend: 'NX.controller.Drilldown',
  requires: [
    'NX.Conditions',
    'NX.Messages',
    'NX.Permissions',
    'NX.I18n',
    'NX.State'
  ],
  masters: [
    'nx-coreui-routing-rules-list'
  ],
  models: [
    'RoutingRule'
  ],
  stores: [
    'RoutingRule'
  ],
  views: [
    'routing.RoutingRulesAdd',
    'routing.RoutingRulesEdit',
    'routing.RoutingRulesFeature',
    'routing.RoutingRulesList',
    'routing.RoutingRulesSettingsForm'
  ],
  refs: [
    {ref: 'feature', selector: 'nx-coreui-routing-rules-feature'},
    {ref: 'list', selector: 'nx-coreui-routing-rules-list'},
    {ref: 'routingRulesAdd', selector: 'nx-coreui-routing-rules-feature nx-coreui-routing-rules-add'},
    {ref: 'routingRulesEdit', selector: 'nx-coreui-routing-rules-feature nx-coreui-routing-rules-edit'}
  ],
  icons: {
    'routing-rules-default': {
      file: 'router.png',
      variants: ['x16', 'x32']
    }
  },

  permission: 'nexus:repository-admin:*:*',

  /**
   * @override
   */
  init: function() {
    this.features = {
      mode: 'admin',
      path: '/Repository/RoutingRules',
      text: NX.I18n.get('RoutingRules_Text'),
      description: NX.I18n.get('RoutingRules_Description'),
      view: {xtype: 'nx-coreui-routing-rules-feature'},
      iconConfig: {
        file: 'router.png',
        variants: ['x16', 'x32']
      },
      visible: function() {
        return NX.Permissions.check('nexus:repository-admin:*:*:read') && NX.State.getValue('routingRules');
      },
      weight: 500
    };

    this.callParent();

    this.listen({
      controller: {
        '#Refresh': {
          refresh: this.loadStores
        }
      },
      store: {
        '#RoutingRule': {
          load: this.reselect
        }
      },
      component: {
        'nx-coreui-routing-rules-list': {
          beforerender: this.loadStores
        },
        'nx-coreui-routing-rules-list button[action=new]': {
          click: this.showAddWindow
        },
        'nx-coreui-routing-rules-add nx-coreui-routing-rules-settings-form button[action=create]': {
          click: this.createRoutingRule
        },
        'nx-coreui-routing-rules-edit nx-coreui-routing-rules-settings-form button[action=save]': {
          afterrender: this.bindSaveButton,
          click: this.updateRoutingRule
        },
        'nx-coreui-routing-rules-add nx-coreui-routing-rules-single-preview button[action=test]': {
          click: this.testRoutingRule.bind(this, this.getRoutingRulesAdd)
        },
        'nx-coreui-routing-rules-edit nx-coreui-routing-rules-single-preview button[action=test]': {
          click: this.testRoutingRule.bind(this, this.getRoutingRulesEdit)
        },
        'nx-coreui-routing-rules-add nx-coreui-routing-rules-settings-form textfield[name^=matchers]': {
          change: this.onMatchersChange.bind(this, this.getRoutingRulesAdd)
        },
        'nx-coreui-routing-rules-add nx-coreui-routing-rules-settings-form panel[cls=nx-repeated-row]': {
          removed: this.onMatchersChange.bind(this, this.getRoutingRulesAdd)
        },
        'nx-coreui-routing-rules-edit nx-coreui-routing-rules-settings-form textfield[name^=matchers]': {
          change: this.onMatchersChange.bind(this, this.getRoutingRulesEdit)
        },
        'nx-coreui-routing-rules-edit nx-coreui-routing-rules-settings-form panel[cls=nx-repeated-row]': {
          removed: this.onMatchersChange.bind(this, this.getRoutingRulesEdit)
        }
      }
    });

    this.apiUrl = NX.util.Url.baseUrl + '/service/rest/internal/ui/routing-rules';
  },

  /**
   * @override
   * @protected
   * Enable 'New' button when user has necessary permission and routingRules feature flag is enabled
   */
  bindNewButton: function(button) {
    this.bindButton(button, ':add');
  },

  /**
   * @override
   * @protected
   * Enable 'Delete' button when user has necessary permission and routingRules feature flag is enabled
   */
  bindDeleteButton: function(button) {
    this.bindButton(button, ':delete');
  },

  /**
   * Enable 'Save' button when user has necessary permission and routingRules feature flag is enabled
   */
  bindSaveButton: function(button) {
    this.bindButton(button, ':edit');
  },

  bindButton: function(button, action) {
    button.mon(
        NX.Conditions.and(
            NX.Conditions.isPermitted(this.permission + action),
            NX.Conditions.watchState('routingRules')
        ),
        {
          satisfied: function () {
            button.enable();
          },
          unsatisfied: function () {
            button.disable();
          }
        }
    );
  },

  /**
   * @override
   */
  getDescription: function(model) {
    return model.get('name');
  },

  /**
   * @override
   */
  onSelection: function(list, model) {
    if (model) {
      this.getRoutingRulesEdit().loadRecord(model);
    }
  },

  /**
   * @private
   */
  showAddWindow: function() {
    // Show the first panel in the create wizard, and set the breadcrumb
    this.setItemName(1, NX.I18n.get('RoutingRules_Create_Title'));
    this.loadCreateWizard(1, Ext.create('widget.nx-coreui-routing-rules-add'));
  },

  createRoutingRule: function(button) {
    var form = button.up('form').getForm();

    if (form.isValid()) {
      Ext.Ajax.request({
        url: this.apiUrl,
        method: 'POST',
        headers: {
          'Content-Type' : 'application/json'
        },
        jsonData: this.getEncodedFormValues(form),
        success: this.makeSubmitSuccessHandler(form.getValues().name, 'RoutingRule_Create_Message'),
        failure: this.onSubmitFailure.bind(this, form)
      });
    }
  },

  updateRoutingRule: function(button) {
    var form = button.up('form').getForm(),
        routingRuleModel = this.getSelectedModel();

    if (form.isValid()) {
      Ext.Ajax.request({
        url: this.getEncodedModelUrl(routingRuleModel),
        method: 'PUT',
        headers: {
          'Content-Type' : 'application/json'
        },
        jsonData: this.getEncodedFormValues(form),
        success: this.makeSubmitSuccessHandler(form.getValues().name, 'RoutingRule_Update_Message'),
        failure: this.onSubmitFailure.bind(this, form)
      });
    }
  },

  getEncodedFormValues: function(form) {
    var formValues = {matchers: []};

    form.getFields().items.forEach(function(field) {
      var name = field.getName(),
          value = field.getValue();
      if (0 === name.indexOf('matchers')) {
        formValues.matchers.push(value);
      }
      else {
        formValues[name] = value;
      }
    });

    return Ext.encode(formValues);
  },

  getEncodedModelUrl: function(model) {
    return this.apiUrl + "/" + window.encodeURIComponent(model.get('name'));
  },

  makeSubmitSuccessHandler: function(routingRuleName, successMessageKey) {
    return function() {
      this.getRoutingRuleStore().load();
      this.loadView(0);
      NX.Messages.add({
        text: NX.I18n.format(successMessageKey, routingRuleName),
        type: 'success'
      });
    }.bind(this);
  },

  onSubmitFailure: function(form, response, opts) {
    var status = response.status;

    if (400 === status) {
      this.handleValidationError(form, response.responseText);
    }
    else {
      NX.Messages.error('An unknown error occurred: ' + status + ' - ' + response.statusText);
      console.error('Submit failure!!!', response, opts);
    }
  },

  handleValidationError: function(form, responseText) {
    var validationErrors = JSON.parse(responseText),
        markInvalidErrors = validationErrors.map(function(error) {
          return {
            field: error.id,
            message: error.message
          };
        }, this);

    form.markInvalid(markInvalidErrors);
  },

  /**
   * @private
   */
  deleteModel: function(model) {
    var description = this.getDescription(model);

    Ext.Ajax.request({
      url: this.getEncodedModelUrl(model),
      method: 'DELETE',
      success: function () {
        this.getRoutingRuleStore().remove(model);
        this.loadView(0);
        NX.Messages.add({
          text: NX.I18n.format('RoutingRule_Delete_Message', description),
          type: 'success'
        });
      }.bind(this),
      failure: this.onSubmitFailure.bind(this, this.getRoutingRulesEdit().down('form'))
    });
  },

  testRoutingRule: function(viewComponentGetter) {
    var viewComponent = viewComponentGetter.apply(this),
        settingsFormComponent = viewComponent.down('nx-coreui-routing-rules-settings-form'),
        settingsForm = settingsFormComponent.getForm(),
        singlePreview = viewComponent.down('nx-coreui-routing-rules-single-preview'),
        testForm = singlePreview.getForm(),
        settingValues = Ext.decode(this.getEncodedFormValues(settingsForm));

    Ext.Ajax.request({
      url: this.apiUrl + '/test',
      method: 'POST',
      headers: {
        'Content-Type' : 'application/json'
      },
      jsonData: Ext.encode({
        mode: settingValues.mode,
        matchers: settingValues.matchers,
        path: '/' + testForm.getValues().path
      }),
      success: function(response) {
        singlePreview.setTestResult(JSON.parse(response.responseText));
      }.bind(this),
      failure: this.onSubmitFailure.bind(this, settingsForm)
    });
  },

  onMatchersChange: function(viewComponentGetter) {
    var viewComponent = viewComponentGetter.apply(this),
        singlePreview = viewComponent.down('nx-coreui-routing-rules-single-preview');
    singlePreview.hideTestResult();
  }
});
