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
 * Maven2 Component Details Provider.
 *
 * @since 3.next
 */
Ext.define('NX.coreui.view.component.Maven2ComponentDetailsProvider', {
  alias: 'nx-coreui-maven2-component-details-provider',
  singleton: true,
  requires: [
    'NX.I18n'
  ],

  getDeleteButtonText: function(componentModel) {
    return this.isSnapshot(componentModel) ?
        NX.I18n.get('ComponentDetails_Delete_Button_Snapshot') :
        NX.I18n.get('ComponentDetails_Delete_Button');
  },

  isSnapshot: function(componentModel) {
    var id;

    if (!componentModel) {
      return false;
    }

    id = componentModel.get('id');
    return (componentModel.get('format') === 'maven2') &&
        id.indexOf('-SNAPSHOT') === (id.length - '-SNAPSHOT'.length);
  }
});
