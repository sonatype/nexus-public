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
 * Migration controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.migration.Controller', {
  extend: 'NX.wizard.Controller',
  requires: [
    'NX.I18n',
    'NX.Permissions',
    'NX.coreui.migration.PlanStepDetailWindow',
    'NX.coreui.migration.RepositoryStore',
    'NX.coreui.migration.PreviewStore',
    'NX.coreui.migration.ProgressStore',
    'NX.coreui.migration.Panel',
    'NX.coreui.migration.OverviewStep',
    'NX.coreui.migration.ContentStep',
    'NX.coreui.migration.AgentStep',
    'NX.coreui.migration.RepositoryDefaultsStep',
    'NX.coreui.migration.RepositoriesStep',
    'NX.coreui.migration.PreviewStep',
    'NX.coreui.migration.PhasePrepareStep',
    'NX.coreui.migration.PhaseSyncStep',
    'NX.coreui.migration.PhaseFinishStep'
  ],

  stores: [
    'Blobstore',
    'NX.coreui.migration.RepositoryStore',
    'NX.coreui.migration.PreviewStore',
    'NX.coreui.migration.ProgressStore'
  ],

  /**
   * @override
   */
  init: function () {
    var me = this;

    me.callParent();

    me.getApplication().getIconController().addIcons({
      'migration-customize': {
        file: 'wrench_orange.png',
        variants: ['x16', 'x32']
      },
      'migration-step-pending': {
        file: 'lorry.png',
        variants: ['x16', 'x32']
      },
      'migration-step-running': {
        file: 'resultset_next.png',
        variants: ['x16', 'x32']
      },
      'migration-step-done': {
        file: 'tick.png',
        variants: ['x16', 'x32']
      },
      'migration-step-error': {
        file: 'exclamation.png',
        variants: ['x16', 'x32']
      }
    });

    me.getApplication().getFeaturesController().registerFeature({
      mode: 'admin',
      path: '/System/Upgrade',
      text: NX.I18n.render(me, 'Feature_Text'),
      description: NX.I18n.render(me, 'Feature_Description'),
      view: {xtype: 'nx-coreui-migration-panel'},
      iconConfig: {
        file: 'server_go.png',
        variants: ['x16', 'x32']
      },
      visible: function () {
        return NX.Permissions.check('nexus:migration:read');
      }
    }, me);

    me.registerSteps([
      'NX.coreui.migration.OverviewStep',
      'NX.coreui.migration.AgentStep',
      'NX.coreui.migration.ContentStep',
      'NX.coreui.migration.RepositoryDefaultsStep',
      'NX.coreui.migration.RepositoriesStep',
      'NX.coreui.migration.PreviewStep',
      'NX.coreui.migration.PhasePrepareStep',
      'NX.coreui.migration.PhaseSyncStep',
      'NX.coreui.migration.PhaseFinishStep'
    ]);

    me.listen({
      component: {
        'nx-coreui-migration-panel': {
          added: me.load,
          beforerender: me.activate,
          removed: me.reset
        }
      }
    });
  },

  // TODO: Could potentially use StateContributor to inform initial migration state?

  /**
   * @private
   */
  activate: function () {
    var me = this;

    me.mask(NX.I18n.render(me, 'Activate_Mask'));

    // fetch migration status and adapt UI to state, or force cancel
    NX.direct.migration_Assistant.status(function (response, event) {
      me.unmask();

      if (event.status && response.success) {
        var state = response.data.state;
        //<if debug>
        me.logDebug('Upgrade state:', state);
        //</if>

        switch (state) {
          case 'INITIAL':
            break;

          case 'CONNECTED':
          case 'CONFIGURED':
              me.incompleteCancel();
            break;

          case 'PHASE_PREPARE':
            me.moveToStepNamed('NX.coreui.migration.PhasePrepareStep');
            break;

          case 'PHASE_SYNC':
            //at this point we don't have a list of repositories, but we need to check the status anyway
            me.getContext().add('checkSyncStatus', true);
            me.moveToStepNamed('NX.coreui.migration.PhaseSyncStep');
            break;

          case 'PHASE_FINISH':
            me.moveToStepNamed('NX.coreui.migration.PhaseFinishStep');
            break;

          default:
            me.logError('Unknown state:', state);
            break;
        }
      }
    });
  },

  /**
   * @private
   */
  incompleteCancel: function() {
    var me = this;

    NX.Dialogs.showInfo(
        NX.I18n.render(me, 'IncompleteCancel_Title'),
        NX.I18n.render(me, 'IncompleteCancel_Text'),
        {
          fn: function () {
            me.mask(NX.I18n.render(me, 'IncompleteCancel_Mask'));
            NX.direct.migration_Assistant.cancel(function (response, event) {
              if (event.status && response.success) {
                me.reset();
              }
              me.unmask();
            });
          }
        }
    );
  },

  /**
   * Configure migration plan.
   */
  configure: function () {
    var me = this,
        ctx = me.getContext(),
        config;

    me.mask(NX.I18n.render(me, 'Configure_Mask'));

    // aggregate migration plan configuration from context
    config = {
      options: ctx.get('content-options'),
      repositories: ctx.get('selected-repositories')
    };

    // configure plan
    NX.direct.migration_Assistant.configure(config, function (response, event) {
      if (event.status && response.success) {
        ctx.add('plan-preview', response.data);

        me.unmask();
        me.moveNext();

        NX.Messages.success(NX.I18n.render(me, 'Configure_Message'));
      }
      else {
        // ensure we unmask
        me.unmask();
      }
    });
  },

  /**
   * Display plan-step detail window.
   *
   * @param id {String} plan-step id
   */
  displayPlanStepDetail: function (id) {
    var me = this;

    me.mask(NX.I18n.render(me, 'PlanStepDetail_Mask'));

    NX.direct.migration_Progress.detail(id, function (response, event) {
      if (event.status && response.success) {
        Ext.create('NX.coreui.migration.PlanStepDetailWindow', {
          detail: response.data
        });
      }

      me.unmask();
    });
  },

  /**
   * Customized cancel behavior to prompt user.
   *
   * @override
   */
  cancel: function () {
    var me = this;

    NX.Dialogs.askConfirmation(
        NX.I18n.render(me, 'Cancel_Confirm_Title'),
        NX.I18n.render(me, 'Cancel_Confirm_Text'),
        function () {
          me.mask(NX.I18n.render(me, 'Cancel_Mask'));

          NX.direct.migration_Assistant.cancel(function (response, event) {
            me.unmask();

            if (event.status && response.success) {
              // invoke super, callParent() unavailable in callback
              me.self.superclass.cancel.call(me);

              NX.Messages.success(NX.I18n.render(me, 'Cancel_Message'));
            }
          });
        }
    );
  }
});
