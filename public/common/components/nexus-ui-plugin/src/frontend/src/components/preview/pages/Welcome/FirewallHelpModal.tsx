/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the Eclipse Foundation.
 * All other trademarks are the property of their respective owners.
 */

import React, { useEffect } from 'react';
import { Dialog, Flex, Text, Box, Button } from '@radix-ui/themes';
import { X } from 'lucide-react';

import './FirewallHelpModal.scss';

export interface FirewallHelpModalProps {
  isOpen: boolean;
  onClose: () => void;
  /** Proxy repo counts from IQ audit */
  proxyCounts: {
    total: number;
    protected: number;
    inAudit: number;
    unprotected: number;
  };
  hasMalware: boolean;
  malwareCount: number;
  hasFirewall: boolean;
}

const BODY_OPEN_CLASS = 'nxrm-firewall-help-open';

/**
 * Firewall help modal - explains ownership, configuration, and malware status.
 * 50% larger than metric help modal for more room for text.
 */
export function FirewallHelpModal({
  isOpen,
  onClose,
  proxyCounts,
  hasMalware,
  malwareCount,
  hasFirewall,
}: FirewallHelpModalProps): React.ReactElement {
  useEffect(() => {
    if (isOpen) document.body.classList.add(BODY_OPEN_CLASS);
    return () => document.body.classList.remove(BODY_OPEN_CLASS);
  }, [isOpen]);

  const { total, protected: p, inAudit, unprotected } = proxyCounts;

  return (
    <Dialog.Root open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <Dialog.Content className="nxrm-firewall-help-modal" aria-describedby={undefined}>
        <Flex justify="between" align="center" gap="3" className="nxrm-firewall-help-modal__header">
          <Dialog.Title asChild>
            <Text as="h2" size="5" weight="bold">
              Firewall Status
            </Text>
          </Dialog.Title>
          <Dialog.Close>
            <Button variant="ghost" color="gray" size="1" onClick={onClose} aria-label="Close">
              <X size={20} />
            </Button>
          </Dialog.Close>
        </Flex>

        <Box className="nxrm-firewall-help-modal__body">
          <Box className="nxrm-firewall-help-modal__section">
            <Text as="p" size="2" weight="medium" className="nxrm-firewall-help-modal__label">
              Do you own Firewall?
            </Text>
            <Text as="p" size="2" color="gray">
              {hasFirewall
                ? 'Yes. You have a Firewall license.'
                : 'No. Firewall requires a license. Contact Sonatype to enable it.'}
            </Text>
          </Box>

          <Box className="nxrm-firewall-help-modal__section">
            <Text as="p" size="2" weight="medium" className="nxrm-firewall-help-modal__label">
              Is it configured correctly?
            </Text>
            <Text as="p" size="2" color="gray">
              You have {total} proxy {total === 1 ? 'repository' : 'repositories'}.
              {total > 0 && (
                <>
                  {' '}
                  {p} {p === 1 ? 'is' : 'are'} protected (quarantine on).
                  {inAudit > 0 && (
                    <>
                      {' '}
                      {inAudit} {inAudit === 1 ? 'is' : 'are'} in audit (monitoring only; not blocking).
                    </>
                  )}
                  {unprotected > 0 && (
                    <>
                      {' '}
                      {unprotected} {unprotected === 1 ? 'is' : 'are'} unprotected (Firewall off).
                    </>
                  )}
                </>
              )}
            </Text>
          </Box>

          <Box className="nxrm-firewall-help-modal__section">
            <Text as="p" size="2" weight="medium" className="nxrm-firewall-help-modal__label">
              Do you have malicious packages?
            </Text>
            <Text as="p" size="2" color="gray">
              {hasMalware ? (
                <>Yes. {malwareCount.toLocaleString()} malicious {malwareCount === 1 ? 'package' : 'packages'} detected.</>
              ) : (
                <>No. No malicious packages detected on this instance.</>
              )}
            </Text>
          </Box>

          <Box className="nxrm-firewall-help-modal__footer">
            <Text as="p" size="1" color="gray">
              Go to Malicious Packages to manage repositories and remove malicious packages.
            </Text>
          </Box>
        </Box>
      </Dialog.Content>
    </Dialog.Root>
  );
}
