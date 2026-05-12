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
import React, { forwardRef, useImperativeHandle } from 'react';
import {useMachine} from '@xstate/react';
import { Dialog, Button, Box, Flex, Heading, Card, Text } from '@radix-ui/themes';
import {
  Activity,
  ExternalLink,
  Database,
  Search,
  Link as LinkIcon,
  Sparkles,
  ChevronRight
} from 'lucide-react';
import {ExtJS, Permissions} from '@sonatype/nexus-ui-plugin';

import OutreachActionsMachine from '../../../pages/user/Welcome/OutreachActionsMachine';
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

const ICON_MAP = {
  [SYSTEM_HEALTH.title]: Activity,
  [CLEANUP_POLICIES.title]: Sparkles,
  [BROWSE.title]: Database,
  [SEARCH.title]: Search,
  [CONNECT.title]: LinkIcon,
  [RELEASE_NOTES.title]: ExternalLink,
  [DOCUMENTATION.title]: ExternalLink,
  [COMMUNITY.title]: ExternalLink,
};

const ACTIONS = [
  {
    title: SYSTEM_HEALTH.title,
    subTitle: SYSTEM_HEALTH.subTitle,
    icon: Activity,
    isVisible: () => ExtJS.checkPermission(Permissions.METRICS.READ) && ExtJS.state().getUser(),
    event: {type: 'REDIRECT', url: '#preview/admin/support/metrichealth'},
  },
  {
    title: CLEANUP_POLICIES.title,
    subTitle: CLEANUP_POLICIES.subTitle,
    icon: Sparkles,
    isVisible: () => ExtJS.checkPermission(Permissions.ADMIN) && ExtJS.state().getUser(),
    event: {type: 'REDIRECT', url: '#preview/admin/repository/cleanuppolicies'},
  },
  {
    title: BROWSE.title,
    subTitle: BROWSE.subTitle,
    icon: Database,
    isVisible: () => ExtJS.state().getValue('browseableformats').length > 0,
    event: {type: 'REDIRECT', url: '#preview/browse'},
  },
  {
    title: SEARCH.title,
    subTitle: SEARCH.subTitle,
    icon: Search,
    isVisible: () => ExtJS.checkPermission(Permissions.SEARCH.READ) && ExtJS.state().getUser(),
    event: {type: 'REDIRECT', url: '#preview/browse/search'},
  },
  {
    title: CONNECT.title,
    subTitle: CONNECT.subTitle,
    icon: LinkIcon,
    isVisible: (context) => {
      return ExtJS.state().getValue('browseableformats').length > 0 && context.showConnectAction;
    },
    event: {type: 'OPEN_CONNECT_MODAL'},
  },
  {
    title: RELEASE_NOTES.title,
    subTitle: RELEASE_NOTES.subTitle,
    icon: ExternalLink,
    isVisible: () => true,
    event: {type: 'OPEN', url: EXTERNAL_LINKS.RELEASE_NOTES},
  },
  {
    title: DOCUMENTATION.title,
    subTitle: DOCUMENTATION.subTitle,
    icon: ExternalLink,
    isVisible: () => true,
    event: {type: 'OPEN', url: EXTERNAL_LINKS.REPO_MANAGER},
  },
  {
    title: COMMUNITY.title,
    subTitle: COMMUNITY.subTitle,
    icon: ExternalLink,
    isVisible: () => true,
    event: {type: 'OPEN', url: EXTERNAL_LINKS.COMMUNITY},
  },
];

/** Titles to exclude from dashboard per DASHBOARD-TABBED-REDESIGN (drop System Health, Cleanup Policies) */
export const DASHBOARD_EXCLUDED_TITLES = [SYSTEM_HEALTH.title, CLEANUP_POLICIES.title];

const getActiveActions = (context, excludedTitles = []) => {
  const maxSize = 3;
  const filtered = ACTIONS.filter(
    (action) => !excludedTitles.includes(action.title) && action.isVisible(context)
  );
  return filtered.slice(0, maxSize);
};

function QuickActionCard({title, subTitle, icon: Icon, action}) {
  const handleEnter = (event) => {
    if (event.key === 'Enter') {
      action();
    }
  };

  return (
    <Card
      className="nxrm-quick-action-card"
      size="1"
      tabIndex="0"
      role="button"
      onClick={action}
      onKeyDown={handleEnter}
    >
      <Box p="3" className="nxrm-quick-action-body">
        <div className="nxrm-quick-action-logo">
          <div className="nxrm-quick-action-icon-container">
            <Icon size={24} />
          </div>
        </div>
        <div className="nxrm-quick-action-name">
          <Heading as="h3" size="3" className="nxrm-quick-action-title">{title}</Heading>
          <Text size="1" color="gray" as="p" className="nxrm-quick-action-subtitle">{subTitle}</Text>
        </div>
        <div className="nxrm-quick-action-chevron">
          <ChevronRight size={20} />
        </div>
      </Box>
    </Card>
  );
}

function OutreachActionsComponent({ excludedTitles = [], showCards = true }, ref) {
  const [state, send] = useMachine(OutreachActionsMachine, {devTools: true});
  const showConnectModal = state.matches('showingConnectModal');
  const activeActions = getActiveActions(state.context, excludedTitles);
  const closeConnectModal = () => send({type: 'CLOSE_MODAL'});

  useImperativeHandle(ref, () => ({
    openConnectModal: () => send({type: 'OPEN_CONNECT_MODAL'}),
  }), [send]);

  return (
    <>
      {showCards && (
        <div className="nxrm-outreach-actions-grid">
          {activeActions.map(({title, subTitle, icon, event}) => (
            <QuickActionCard
              key={title}
              title={title}
              subTitle={subTitle}
              icon={icon}
              action={() => send(event)}
            />
          ))}
        </div>
      )}

      <Dialog.Root open={showConnectModal} onOpenChange={(open) => !open && closeConnectModal()}>
        <Dialog.Content maxWidth="600px">
          <Dialog.Title>{CONNECT_MODAL.TITLE}</Dialog.Title>
          
          <Flex direction="column" gap="4" mt="4">
            <Box>
              <Heading as="h3" size="4" mb="2">{CONNECT_MODAL.FIRST_STEP_TEXT}</Heading>
              <Box
                style={{
                  border: '1px solid var(--gray-6)',
                  borderRadius: 'var(--radius-3)',
                  overflow: 'hidden',
                }}
              >
                <img
                  alt="Repositories Table"
                  src="./static/rapture/resources/images/welcome/repositories_table.png"
                  style={{width: '100%', display: 'block'}}
                />
              </Box>
            </Box>
            
            <Box>
              <Heading as="h3" size="4" mb="2">{CONNECT_MODAL.SECOND_STEP_TEXT}</Heading>
              <Box
                style={{
                  border: '1px solid var(--gray-6)',
                  borderRadius: 'var(--radius-3)',
                  overflow: 'hidden',
                }}
              >
                <img
                  alt="Copy repository URL"
                  src="./static/rapture/resources/images/welcome/copy_repository_url.png"
                  style={{width: '100%', display: 'block'}}
                />
              </Box>
            </Box>
          </Flex>

          <Flex gap="3" mt="5" justify="end">
            <Dialog.Close>
              <Button variant="solid">Close</Button>
            </Dialog.Close>
          </Flex>
        </Dialog.Content>
      </Dialog.Root>
    </>
  );
}

const OutreachActions = forwardRef(OutreachActionsComponent);
export default OutreachActions;

