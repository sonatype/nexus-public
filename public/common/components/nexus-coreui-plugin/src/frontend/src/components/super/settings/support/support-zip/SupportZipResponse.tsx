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
import { Box, Flex, Text, Heading } from '@radix-ui/themes';
import { CheckCircle2, Download, AlertTriangle } from 'lucide-react';
import { ExtJS } from '@sonatype/nexus-ui-plugin';

import { SettingsButton, SettingsAlert } from '../../../shared/form';
import { SupportZipResponse as SupportZipResponseType } from './types';
import { useSupportZipApi } from './useSupportZipApi';

import './SupportZipResponse.scss';

interface SupportZipResponseProps {
  response: SupportZipResponseType;
  nodeId?: string;
}

/**
 * SupportZipResponse - Displays the result of creating a support ZIP
 */
export function SupportZipResponse({ response, nodeId }: SupportZipResponseProps) {
  const { getDownloadUrl } = useSupportZipApi();

  const handleDownload = () => {
    const url = ExtJS.urlOf(getDownloadUrl(response.name));
    ExtJS.downloadUrl(url);
  };

  return (
    <Box className="support-zip-response" data-testid="support-zip-response">
      <Flex align="center" gap="3" mb="4">
        <CheckCircle2 size={24} className="support-zip-response__icon" />
        <Heading as="h3" size="4" weight="medium">
          Support ZIP Created
        </Heading>
      </Flex>

      {response.truncated && (
        <Box mb="4">
          <SettingsAlert type="warning" data-testid="support-zip-truncated-warning">
            <Flex align="center" gap="2">
              <AlertTriangle size={16} />
              <Text>The support ZIP was truncated due to size limits.</Text>
            </Flex>
          </SettingsAlert>
        </Box>
      )}

      <Box className="support-zip-response__details">
        <Text size="2" color="gray" mb="2">
          Support ZIP has been created. You can reference this file on the filesystem or download it
          from your browser.
        </Text>

        <Box className="support-zip-response__info" mt="4">
          {nodeId && (
            <Flex className="support-zip-response__row">
              <Text size="2" weight="medium" className="support-zip-response__label">
                Node:
              </Text>
              <Text size="2" className="support-zip-response__value" data-testid="support-zip-response-node">
                {nodeId}
              </Text>
            </Flex>
          )}
          <Flex className="support-zip-response__row">
            <Text size="2" weight="medium" className="support-zip-response__label">
              Name:
            </Text>
            <Text size="2" className="support-zip-response__value" data-testid="support-zip-response-name">
              {response.name}
            </Text>
          </Flex>
          <Flex className="support-zip-response__row">
            <Text size="2" weight="medium" className="support-zip-response__label">
              Size:
            </Text>
            <Text size="2" className="support-zip-response__value" data-testid="support-zip-response-size">
              {response.size}
            </Text>
          </Flex>
          <Flex className="support-zip-response__row">
            <Text size="2" weight="medium" className="support-zip-response__label">
              Path:
            </Text>
            <Text size="2" className="support-zip-response__value support-zip-response__value--path" data-testid="support-zip-response-path">
              {response.file}
            </Text>
          </Flex>
        </Box>

        <Box mt="4">
          <SettingsButton
            variant="primary"
            onClick={handleDownload}
            icon={Download}
            data-testid="support-zip-download-button"
          >
            Download
          </SettingsButton>
        </Box>
      </Box>
    </Box>
  );
}

export default SupportZipResponse;


