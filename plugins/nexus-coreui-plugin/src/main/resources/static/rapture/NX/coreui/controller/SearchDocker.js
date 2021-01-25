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
 * Docker repository search contribution.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.SearchDocker', {
  extend: 'NX.app.Controller',
  requires: [
    'NX.I18n'
  ],

  /**
   * @override
   */
  init: function () {
    var me = this,
        search = me.getController('NX.coreui.controller.Search');

    search.registerCriteria([
      {
        id: 'attributes.docker.imageName',
        group: NX.I18n.get('SearchDocker_Group'),
        config: {
          format: 'docker',
          fieldLabel: NX.I18n.get('SearchDocker_Image_Name_FieldLabel'),
          width: 300
        }
      },
      {
        id: 'attributes.docker.imageTag',
        group: NX.I18n.get('SearchDocker_Group'),
        config: {
          format: 'docker',
          fieldLabel: NX.I18n.get('SearchDocker_Image_Tag_FieldLabel')
        }
      },
      {
        id: 'attributes.docker.layerAncestry',
        group: NX.I18n.get('SearchDocker_Group'),
        config: {
          format: 'docker',
          fieldLabel: NX.I18n.get('SearchDocker_LayerId_FieldLabel'),
          width: 500
        }
      },
      {
        id: 'assets.attributes.docker.content_digest',
        group: NX.I18n.get('SearchDocker_Group'),
        config: {
          format: 'docker',
          fieldLabel: NX.I18n.get('SearchDocker_ContentDigest_FieldLabel'),
          width: 500
        }
      }
    ], me);

    search.registerFilter({
      id: 'docker',
      name: 'Docker',
      text: NX.I18n.get('SearchDocker_Text'),
      description: NX.I18n.get('SearchDocker_Description'),
      readOnly: true,
      criterias: [
        {id: 'format', value: 'docker', hidden: true},
        {id: 'attributes.docker.imageName'},
        {id: 'attributes.docker.imageTag'},
        {id: 'attributes.docker.layerAncestry'},
        {id: 'assets.attributes.docker.content_digest'}
      ]
    }, me);
  }

});
