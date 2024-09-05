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
 * Content (features area) controller.
 *
 * @since 3.0
 */
Ext.define('NX.controller.Content', {
  extend: 'NX.app.Controller',
  requires: [
    'NX.Icons',
    'NX.State'
  ],

  views: [
    'feature.Content'
  ],

  refs: [
    {
      ref: 'featureContent',
      selector: 'nx-feature-content'
    }
  ],

  /**
   * @override
   */
  init: function () {
    var me = this;

    me.listen({
      controller: {
        '#Menu': {
          featureselected: me.onFeatureSelected
        }
      },
      component: {
        'nx-feature-content': {
          resize: function (obj) {
            var drilldown;
            if (obj) {
              drilldown = obj.down('nx-drilldown');
              if (drilldown) {
                drilldown.fireEvent('syncsize');
              }
            }
          }
        }
      }
    });
  },

  /**
   * Update content to selected feature view.
   *
   * @private
   * @param {NX.model.Feature} feature selected feature
   */
  onFeatureSelected: function (feature) {
    var me = this,
        content = me.getFeatureContent(),
        view = feature.get('view'),
        text = feature.get('text'),
        iconName = feature.get('iconName'),
        iconCls = feature.get('iconCls'),
        description = feature.get('description'),
        cmp;

    // create new view and replace any current view
    if (Ext.isString(view)) {
      cmp = me.getView(view).create({});
    }
    else {
      cmp = Ext.widget(view);
    }
    me.mon(cmp, 'destroy', function () {
      //<if debug>
      me.logTrace('Destroyed:', cmp.self.getName());
      //</if>
    });

    // remove the current contents
    content.removeAll();

    // update title and icon
    content.setTitle(text);
    if (iconCls) {
      content.setIconCls(iconCls + " nx-icon");
    } else {
      content.setIconCls(NX.Icons.cls(iconName, 'x32'));
    }

    // Reset unsaved changes flag
    content.resetUnsavedChangesFlag();

    // set browser title
    NX.global.document.title = text + ' - ' + NX.State.getValue('uiSettings').title;

    // update description
    if (description === undefined) {
      description = '';
    }
    content.setDescription(description);

    // Update the breadcrumb
    content.showRoot();

    content.maybeShowMaliciousRiskOnDisk();

    // install new feature view
    content.add(cmp);

    // fire activate event to view component
    cmp.fireEvent('activate', cmp);

    //<if debug>
    me.logInfo('Content changed to:', text, 'class:', cmp.self.getName());
    //</if>
  }

});
