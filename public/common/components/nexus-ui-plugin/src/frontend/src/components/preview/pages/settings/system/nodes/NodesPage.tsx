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
import { Box, Text } from '@radix-ui/themes';
import { Server, ExternalLink } from 'lucide-react';

import { ExtJS } from '../../../../../../interface/ExtJS';
import { PageHeader } from '../../../../shared';
import { SettingsFormSection, SettingsAlert } from '../../../../shared/form';
import { NodesList } from './NodesList';
import { NodesPageProps } from './types';

import './NodesPage.scss';

const navigateTo = (path: string) => {
  window.location.hash = path;
};

const HA_DOCUMENTATION_URL = 'https://links.sonatype.com/products/nxrm/high-availability';

/**
 * NodesPage - Main Nodes management page for Preview UI
 *
 * Displays cluster nodes in the system.
 */
export function NodesPage({ className }: NodesPageProps) {
  const isClustered = ExtJS.useState(() => ExtJS.state().getValue('nexus.datastore.clustered.enabled'));

  return (
    <Box className={`nodes-page ${className || ''}`.trim()} data-analytics-id="nxrm-nodes-nav-detail">
      <PageHeader
        icon={Server}
        title="Nodes"
        description="View cluster nodes in this Nexus Repository instance"
        breadcrumbs={[
          { label: 'Settings', onClick: () => navigateTo('#preview/admin/settings') },
          { label: 'Nodes' }
        ]}
      />

      {/* Content */}
      <Box className="nodes-page__content">
        {/* Single-node informational banner */}
        {!isClustered && (
          <Box className="nodes-page__banner">
            <SettingsAlert type="info" data-testid="single-node-banner">
              This instance is running in single-node mode. High Availability cluster nodes are not expected. See the{' '}
              <a
                href={HA_DOCUMENTATION_URL}
                target="_blank"
                rel="noopener noreferrer"
              >
                high availability documentation
                <ExternalLink size={12} aria-hidden="true" />
              </a>
              {' '}for more information.
            </SettingsAlert>
          </Box>
        )}

        {/* Nodes List Section */}
        <SettingsFormSection title="Nodes">
          <Text size="2" className="nodes-page__section-description">
            Nodes participating in this Nexus Repository instance.
          </Text>
          <Box className="nodes-page__nodes-list">
            <NodesList />
          </Box>
        </SettingsFormSection>
      </Box>
    </Box>
  );
}

export default NodesPage;
