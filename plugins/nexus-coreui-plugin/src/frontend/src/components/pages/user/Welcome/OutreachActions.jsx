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
import React from 'react';
import {
  faDatabase,
  faExternalLinkAlt,
  faMedkit,
  faSearch,
} from '@fortawesome/free-solid-svg-icons';
import {ExtJS, Permissions} from '@sonatype/nexus-ui-plugin';

import QuickAction from './QuickAction';
import UIStrings from '../../../../constants/UIStrings';

import './OutreachActions.scss';

const {WELCOME: {ACTIONS: {
  SYSTEM_HEALTH,
  CLEANUP_POLICIES,
  BROWSE,
  SEARCH,
  RELEASE_NOTES,
  DOCUMENTATION,
  COMMUNITY,
}}} = UIStrings;

const EXTERNAL_LINKS = {
  RELEASE_NOTES: 'https://links.sonatype.com/products/nxrm3/release-notes',
  REPO_MANAGER: 'https://links.sonatype.com/products/nxrm3',
  COMMUNITY: 'https://community.sonatype.com',
};

const ACTIONS = [
  {
    title: SYSTEM_HEALTH.title,
    subTitle: SYSTEM_HEALTH.subTitle,
    icon: faMedkit,
    isVisible: () => ExtJS.checkPermission(Permissions.METRICS.READ),
    action: () => window.location.hash = '#admin/support/status',
  },
  {
    title: CLEANUP_POLICIES.title,
    subTitle: CLEANUP_POLICIES.subTitle,
    icon: faSearch,
    isVisible: () => ExtJS.checkPermission(Permissions.ADMIN),
    action: () => window.location.hash = '#admin/repository/cleanuppolicies',
  },
  {
    title: BROWSE.title,
    subTitle: BROWSE.subTitle,
    icon: faDatabase,
    isVisible: () => ExtJS.state().getValue('browseableformats').length > 0,
    action: () => window.location.hash = '#browse/browse',
  },
  {
    title: SEARCH.title,
    subTitle: SEARCH.subTitle,
    icon: faSearch,
    isVisible: () => ExtJS.checkPermission(Permissions.SEARCH.READ),
    action: () => window.location.hash = '#browse/search',
  },
  {
    title: RELEASE_NOTES.title,
    subTitle: RELEASE_NOTES.subTitle,
    icon: faExternalLinkAlt,
    isVisible: () => true,
    action: () => window.open(EXTERNAL_LINKS.RELEASE_NOTES, '_blank'),
  },
  {
    title: DOCUMENTATION.title,
    subTitle: DOCUMENTATION.subTitle,
    icon: faExternalLinkAlt,
    isVisible: () => true,
    action: () => window.open(EXTERNAL_LINKS.REPO_MANAGER, '_blank'),
  },
  {
    title: COMMUNITY.title,
    subTitle: COMMUNITY.subTitle,
    icon: faExternalLinkAlt,
    isVisible: () => true,
    action: () => window.open(EXTERNAL_LINKS.COMMUNITY, '_blank'),
  },
];

const getActiveActions = () => {
  const maxSize = 3;
  const activeActions = ACTIONS.filter(action => action.isVisible());
  return activeActions.slice(0, maxSize);
}

export default function OutreachActions() {
  const activeActions = getActiveActions();

  return (
      <div className="nxrm-outreach-actions">
        {activeActions.map(({title, subTitle, icon, action}) => (
            <QuickAction
                key={title}
                title={title}
                subTitle={subTitle}
                icon={icon}
                action={action}
                className="nxrm-outreach-action"
            />
        ))}
      </div>
  );
};
