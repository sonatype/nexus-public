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
import {inspect} from '@xstate/inspect';

export default function configurePlugins() {
  // Declare an initial (empty) array for plugin configurations
  window.plugins = [];
  window.ReactComponents = {};
  window.BlobStoreTypes = {};

  // A function for the ExtJS codebase to call to register React plugins
  window.onStart = function() {
    window.plugins.forEach((plugin) => {
      if (plugin.features) {
        plugin.features.forEach(registerFeature);
      }
    })
  };

  if (window.location.search.startsWith('?debug')) {
    console.log('initialize xstate inspector');
    inspect({
      iframe: false
    });
  }
}

/**
 * @param feature - {
 *   mode: 'browse' || 'admin',
 *   path: '/somepath',
 *   text: 'menu label',
 *   textComplement: 'text complement for menu label'
 *   description: 'description used for the header when visiting the feature',
 *   view: <reactViewReference>,
 *   iconCls: 'x-fa fa-icon-type',
 *   visibility: {
 *     bundle: 'an optional bundle expected to be available for the feature to be visible',
 *     featureFlags: [{ // optional
 *       key: 'featureFlagName',
 *       defaultValue: true // the value the feature flag is set to by default (optional)
 *     }],
 *     licenseValid: [{ // optional
 *       key: 'stateWithLicenseFlagName',
 *       defaultValue: false // the value the license validity is set to by default (optional)
 *     }],
 *     statesEnabled: [{ // optional
 *       key: 'stateWithEnabledFlagName',
 *       defaultValue: false // the value the state enablement is set to by default (optional)
 *     }],
 *     permissions: ['optional array of permission strings', 'nexus:settings:read']
 *   }
 * }
 */
function registerFeature(feature) {
  console.log(`Register feature`, feature);
  const reactViewController = Ext.getApplication().getController('NX.coreui.controller.react.ReactViewController');
  Ext.getApplication().getFeaturesController().registerFeature({
    mode: feature.mode,
    path: feature.path,
    text: feature.text,
    textComplement: feature.textComplement,
    description: feature.description,
    weight: feature.weight,
    view: {
      xtype: 'nx-coreui-react-main-container',
      itemId: 'react-view',
      reactView: feature.view
    },
    iconCls: feature.iconCls,
    visible: function () {
      var isVisible = true;
      const visibility = feature.visibility;

      if (!visibility) {
        console.warn('feature is active due to no visibility configuration defined', feature);
        return isVisible;
      }

      if (visibility.bundle) {
        isVisible = NX.app.Application.bundleActive(visibility.bundle)
        console.debug("bundleActive="+isVisible, visibility.bundle);
      }

      if (isVisible && visibility.licenseValid) {
        isVisible = visibility.licenseValid.every(licenseValid => NX.State.getValue(licenseValid.key, licenseValid.defaultValue)['licenseValid']);
        console.debug("licenseValid="+isVisible, visibility.licenseValid);
      }

      if (isVisible && visibility.featureFlags) {
        isVisible = visibility.featureFlags.every(featureFlag => NX.State.getValue(featureFlag.key, featureFlag.defaultValue));
        console.debug("featureFlagsActive="+isVisible, visibility.featureFlags);
      }

      if (isVisible && visibility.statesEnabled) {
        isVisible = visibility.statesEnabled.every(state => {
          const stateValue = NX.State.getValue(state.key, state.defaultValue);
          if (typeof stateValue === "boolean") {
            return stateValue;
          }
          else if (Array.isArray(stateValue)) {
            return stateValue.length > 0;
          }
          else {
            return stateValue.enabled;
          }
        });
        console.debug("statesEnabled="+isVisible, visibility.statesEnabled);
      }

      if (isVisible && visibility.permissions) {
        isVisible = visibility.permissions.every((permission) => NX.Permissions.check(permission));
        console.debug("permissionCheck="+isVisible, visibility.permissions);
      }

      if (isVisible && visibility.editions) {
        isVisible = visibility.editions.every((edition) =>  NX.State.getEdition() === edition);
        console.debug("editionCheck="+isVisible, visibility.editions);
      }

      if (isVisible && visibility.requiresUser) {
        isVisible = NX.Security.hasUser();
      }

      return isVisible;
    }
  }, reactViewController);
}
