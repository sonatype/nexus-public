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
import AnonymousSettings from './components/pages/admin/AnonymousSettings/AnonymousSettings';
import UIStrings from './constants/UIStrings';

window.plugins.push({
  id: 'nexus-coreui-plugin',

  features: [
    {
      mode: 'admin',
      path: '/Security/Anonymous',
      text: UIStrings.ANONYMOUS_SETTINGS.MENU.text,
      description: UIStrings.ANONYMOUS_SETTINGS.MENU.description,
      view: AnonymousSettings,
      iconCls: 'x-fa fa-id-card',
      visibility: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        featureFlags: [
          {
            key: 'reactFrontend',
            defaultValue: true
          }
        ],
        permissions: ['nexus:settings:read']
      }
    }
  ]
});
