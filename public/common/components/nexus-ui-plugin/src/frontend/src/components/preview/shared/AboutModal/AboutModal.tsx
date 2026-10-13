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

import React, { useEffect } from 'react';
import { Box, Button, Dialog, Flex, Heading, Text } from '@radix-ui/themes';
import { Loader2 } from 'lucide-react';
import { useAboutModal } from './useAboutModal';

import './AboutModal.scss';

export const ABOUT_MODAL_STRINGS = {
  TITLE: 'About Sonatype Nexus Repository',
  LEGAL_TITLE: 'Legal Terms and Copyright',
  CLOSE: 'Close',
  RETRY: 'Retry',
  LOADING: 'Loading legal information...',
  ERROR: 'Unable to load legal information.',
  VERSION: 'Version',
  EDITION: 'Edition',
  BUILD_REVISION: 'Build Revision',
  BUILD_TIMESTAMP: 'Build Timestamp',
};

export interface AboutModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /* Cloud tenants do not surface Version/Build details; only the legal panel. */
  isCloud: boolean;
  version: string;
  edition: string;
  buildRevision: string;
  buildTimestamp: string;
}

interface InfoRowProps {
  label: string;
  value: string;
}

function InfoRow({ label, value }: InfoRowProps) {
  return (
    <Flex justify="between" align="center" gap="4" className="about-modal__info-row">
      <Text size="2" color="gray">
        {label}
      </Text>
      <Text size="2" weight="medium">
        {value}
      </Text>
    </Flex>
  );
}

/*
 * Native Preview UI About modal. Matches Preview UI standards in dark and light
 * mode, replacing the classic ExtJS About window for the Preview surface only.
 * The legal panel injects the server-rendered /COPYRIGHT.html body verbatim so
 * the edition-branched legal text and links stay byte-for-byte identical to the
 * classic UI.
 */
export function AboutModal({
  open,
  onOpenChange,
  isCloud,
  version,
  edition,
  buildRevision,
  buildTimestamp,
}: AboutModalProps) {
  const { status, legalHtml, fetchLegal } = useAboutModal();

  useEffect(() => {
    if (open && status === 'idle') {
      fetchLegal();
    }
  }, [open, status, fetchLegal]);

  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Content maxWidth="560px" data-testid="about-modal" aria-describedby={undefined}>
        <Dialog.Title>{ABOUT_MODAL_STRINGS.TITLE}</Dialog.Title>

        <Flex direction="column" gap="4" mt="2">
          {!isCloud && (
            <Box className="about-modal__info" data-testid="about-modal-info">
              <InfoRow label={ABOUT_MODAL_STRINGS.VERSION} value={version} />
              <InfoRow label={ABOUT_MODAL_STRINGS.EDITION} value={edition} />
              <InfoRow label={ABOUT_MODAL_STRINGS.BUILD_REVISION} value={buildRevision} />
              <InfoRow label={ABOUT_MODAL_STRINGS.BUILD_TIMESTAMP} value={buildTimestamp} />
            </Box>
          )}

          <Box>
            <Heading size="3" mb="2">
              {ABOUT_MODAL_STRINGS.LEGAL_TITLE}
            </Heading>

            {status === 'loading' && (
              <Flex align="center" gap="2" data-testid="about-modal-loading">
                <Loader2 size={16} style={{ animation: 'spin 1s linear infinite' }} />
                <Text size="2" color="gray">
                  {ABOUT_MODAL_STRINGS.LOADING}
                </Text>
              </Flex>
            )}

            {status === 'error' && (
              <Flex direction="column" align="start" gap="2" data-testid="about-modal-error">
                <Text size="2" color="red">
                  {ABOUT_MODAL_STRINGS.ERROR}
                </Text>
                <Button variant="soft" color="gray" size="1" onClick={fetchLegal}>
                  {ABOUT_MODAL_STRINGS.RETRY}
                </Button>
              </Flex>
            )}

            {status === 'loaded' && (
              <Box
                className="about-modal__legal"
                data-testid="about-modal-legal"
                dangerouslySetInnerHTML={{ __html: legalHtml }}
              />
            )}
          </Box>

          <Flex justify="end" mt="2">
            <Dialog.Close>
              <Button variant="solid" color="blue" size="2" data-testid="about-modal-close">
                {ABOUT_MODAL_STRINGS.CLOSE}
              </Button>
            </Dialog.Close>
          </Flex>
        </Flex>
      </Dialog.Content>
    </Dialog.Root>
  );
}

export default AboutModal;
