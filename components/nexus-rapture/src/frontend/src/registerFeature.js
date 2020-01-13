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

/**
 * @param feature - {
 *   mode: 'browse' || 'admin',
 *   path: '/somepath',
 *   text: 'menu label',
 *   description: 'description used for the header when visiting the feature',
 *   view: <reactViewReference>,
 *   iconCls: 'x-fa fa-icon-type',
 *   visibility: {
 *     bundle: 'an optional bundle expected to be available for the feature to be visible',
 *     featureFlags: [{ // optional
 *       key: 'featureFlagName',
 *       defaultValue: true // the value the feature flag is set to by default (optional)
 *     }],
 *     permissions: ['optional array of permission strings', 'nexus:settings:read']
 *   }
 * }
 */
export default function registerFeature(feature) {
  console.log(`Register feature`, feature);
  const reactViewController = Ext.getApplication().getController('NX.coreui.controller.react.ReactViewController');
  Ext.getApplication().getFeaturesController().registerFeature({
    mode: feature.mode,
    path: feature.path,
    text: feature.text,
    description: feature.description,
    view: {
      xtype: 'nx-coreui-react-main-container',
      itemId: 'react-view',
      reactView: feature.view
    },
    iconCls: feature.iconCls,
    visible: function () {
      var isActive = true;

      if (!feature.visibility) {
        console.warn('feature is active due to no visibility configuration defined', feature);
        return isActive;
      }

      if (feature.visibility.bundle) {
        isActive = isActive && NX.app.Application.bundleActive(feature.visibility.bundle);
      }

      if (isActive && feature.visibility.featureFlags) {
        isActive = Ext.Array.every(feature.visibility.featureFlags, function(featureFlag) {
          return NX.State.getValue(featureFlag.key, featureFlag.defaultValue);
        });
      }

      if (isActive && feature.visibility.permissions) {
        isActive = feature.visibility.permissions.every((permission) => NX.Permissions.check(permission));
      }

      return isActive;
    }
  }, reactViewController);
}
