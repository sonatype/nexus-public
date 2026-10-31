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
import * as Dialog from '@radix-ui/react-dialog';
import { Flex, Heading, Box, Link } from '@radix-ui/themes';
import { X, Download } from 'lucide-react';

import { SettingsButton } from '../../../../shared/form';

import './LicenseAgreementModal.scss';

interface LicenseAgreementModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  licenseUrl: string;
  onAccept: () => void;
  onDecline: () => void;
}

/**
 * LicenseAgreementModal - Modal dialog for license agreement acceptance
 */
export function LicenseAgreementModal({
  open,
  onOpenChange,
  licenseUrl,
  onAccept,
  onDecline,
}: LicenseAgreementModalProps) {
  const handleAccept = () => {
    onAccept();
    onOpenChange(false);
  };

  const handleDecline = () => {
    onDecline();
    onOpenChange(false);
  };

  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="license-agreement-modal__overlay" />
        <Dialog.Content className="license-agreement-modal__content">
          <Flex direction="column" height="100%">
            {/* Header */}
            <Flex align="center" justify="between" className="license-agreement-modal__header">
              <Heading as="h2" size="4" weight="medium">
                Nexus Repository Manager License Agreement
              </Heading>
              <Dialog.Close asChild>
                <SettingsButton variant="ghost" size="1" className="license-agreement-modal__close" icon={X} />
              </Dialog.Close>
            </Flex>

            {/* Content */}
            <Box className="license-agreement-modal__body">
              {/* licenseUrl is always a local relative path (e.g. /PRO-LICENSE.html) from ExtJS.proLicenseUrl() */}
              <iframe
                className="license-agreement-modal__iframe"
                title="Nexus Repository Manager License Agreement"
                src={licenseUrl}
              />
            </Box>

            {/* Footer */}
            <Flex align="center" justify="between" className="license-agreement-modal__footer">
              <Link
                href={licenseUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="license-agreement-modal__download-link"
              >
                <Download size={16} />
                Download a copy of the agreement
              </Link>
              <Flex gap="2">
                <SettingsButton variant="secondary" onClick={handleDecline}>
                  I Decline
                </SettingsButton>
                <SettingsButton variant="primary" onClick={handleAccept}>
                  I Accept
                </SettingsButton>
              </Flex>
            </Flex>
          </Flex>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}

export default LicenseAgreementModal;

