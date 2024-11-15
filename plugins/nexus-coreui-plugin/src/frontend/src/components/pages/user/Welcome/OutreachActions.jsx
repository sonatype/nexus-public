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
import {useMachine} from '@xstate/react';
import {
  NxModal,
  NxH2,
  NxFooter, NxButton, NxButtonBar,
} from '@sonatype/react-shared-components';
import {
  faDatabase,
  faExternalLinkAlt,
  faMedkit,
  faSearch,
  faLink,
} from '@fortawesome/free-solid-svg-icons';
import {ExtJS, Permissions} from '@sonatype/nexus-ui-plugin';

import QuickAction from './QuickAction';
import OutreachActionsMachine from './OutreachActionsMachine';
import UIStrings from '../../../../constants/UIStrings';

import './OutreachActions.scss';

const {WELCOME: {
  ACTIONS: {
    SYSTEM_HEALTH,
    CLEANUP_POLICIES,
    BROWSE,
    SEARCH,
    RELEASE_NOTES,
    DOCUMENTATION,
    COMMUNITY,
    CONNECT,
  },
  CONNECT_MODAL,
}} = UIStrings;

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
    isVisible: () => ExtJS.checkPermission(Permissions.METRICS.READ) && ExtJS.state().getUser(),
    event: {type: 'REDIRECT', url: '#admin/support/status'},
  },
  {
    title: CLEANUP_POLICIES.title,
    subTitle: CLEANUP_POLICIES.subTitle,
    icon: faSearch,
    isVisible: () => ExtJS.checkPermission(Permissions.ADMIN) && ExtJS.state().getUser(),
    event: {type: 'REDIRECT', url: '#admin/repository/cleanuppolicies'},
  },
  {
    title: BROWSE.title,
    subTitle: BROWSE.subTitle,
    icon: faDatabase,
    isVisible: () => ExtJS.state().getValue('browseableformats').length > 0,
    event: {type: 'REDIRECT', url: '#browse/browse'},
  },
  {
    title: SEARCH.title,
    subTitle: SEARCH.subTitle,
    icon: faSearch,
    isVisible: () => ExtJS.checkPermission(Permissions.SEARCH.READ),
    event: {type: 'REDIRECT', url: '#browse/search'},
  },
  {
    title: CONNECT.title,
    subTitle: CONNECT.subTitle,
    icon: faLink,
    isVisible: (context) => {
      return ExtJS.state().getValue('browseableformats').length > 0 && context.showConnectAction;
    },
    event: {type: 'OPEN_CONNECT_MODAL'},
  },
  {
    title: RELEASE_NOTES.title,
    subTitle: RELEASE_NOTES.subTitle,
    icon: faExternalLinkAlt,
    isVisible: () => true,
    event: {type: 'OPEN', url: EXTERNAL_LINKS.RELEASE_NOTES},
  },
  {
    title: DOCUMENTATION.title,
    subTitle: DOCUMENTATION.subTitle,
    icon: faExternalLinkAlt,
    isVisible: () => true,
    event: {type: 'OPEN', url: EXTERNAL_LINKS.REPO_MANAGER},
  },
  {
    title: COMMUNITY.title,
    subTitle: COMMUNITY.subTitle,
    icon: faExternalLinkAlt,
    isVisible: () => true,
    event: {type: 'OPEN', url: EXTERNAL_LINKS.COMMUNITY},
  },
];

const getActiveActions = (context) => {
  const maxSize = 3;
  const activeActions = ACTIONS.filter(action => action.isVisible(context));
  return activeActions.slice(0, maxSize);
}

export default function OutreachActions() {
  const [state, send] = useMachine(OutreachActionsMachine, {devTools: true});
  const showConnectModal = state.matches('showingConnectModal');
  const activeActions = getActiveActions(state.context);
  const closeConnectModal = () => send({type: 'CLOSE_MODAL'});

  return (
      <>
        <div className="nxrm-outreach-actions">
          {activeActions.map(({title, subTitle, icon, event}) => (
              <QuickAction
                  key={title}
                  title={title}
                  subTitle={subTitle}
                  icon={icon}
                  action={() => send(event)}
                  className="nxrm-outreach-action"
              />
          ))}
        </div>
        {showConnectModal &&
            <NxModal
                onCancel={closeConnectModal}
                aria-labelledby="modal-header-text"
                className="nxrm-connect-modal"
            >
              <NxModal.Header>
                <NxH2 id="modal-header-text">{CONNECT_MODAL.TITLE}</NxH2>
              </NxModal.Header>
              <NxModal.Content>
                <dl className="nxrm-connect-modal__steps">
                  <dt>{CONNECT_MODAL.FIRST_STEP_TEXT}</dt>
                  <dd>
                    <img
                        className="nxrm-connect-modal__repositories-table-img"
                        alt="Repositories Table"
                        src="./static/rapture/resources/images/welcome/repositories_table.png"
                    />
                  </dd>
                  <dt>{CONNECT_MODAL.SECOND_STEP_TEXT}</dt>
                  <dd>
                    <img
                        alt="Copy repository URL"
                        src="./static/rapture/resources/images/welcome/copy_repository_url.png"
                    />
                  </dd>
                </dl>
              </NxModal.Content>
              <NxFooter>
                <NxButtonBar>
                  <NxButton
                      type="button"
                      variant="primary"
                      onClick={closeConnectModal}
                  >
                    {UIStrings.CLOSE}
                  </NxButton>
                </NxButtonBar>
              </NxFooter>
            </NxModal>
        }
      </>
  );
};
