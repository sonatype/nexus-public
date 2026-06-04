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
import { Dialog, Flex, Text, Box, Button } from '@radix-ui/themes';
import { X } from 'lucide-react';
import { getMetricMethodology, type MetricType } from './metricMethodology';

import './MetricHelpModal.scss';

export interface MetricHelpModalProps {
  metricType: MetricType | null;
  isOpen: boolean;
  onClose: () => void;
  /** Egress card shows TBD for brand-new instances */
  isEgressTbd?: boolean;
}

const BODY_OPEN_CLASS = 'nxrm-metric-help-open';

/**
 * Centered half-screen modal explaining how a dashboard metric is calculated.
 * Designed for DevOps users who need clarity without reading docs.
 * Modal content and overlay are opaque (not transparent).
 */
export function MetricHelpModal({
  metricType,
  isOpen,
  onClose,
  isEgressTbd = false,
}: MetricHelpModalProps): React.ReactElement {
  const content = metricType ? getMetricMethodology(metricType, isEgressTbd) : null;

  useEffect(() => {
    if (isOpen) {
      document.body.classList.add(BODY_OPEN_CLASS);
    }
    return () => document.body.classList.remove(BODY_OPEN_CLASS);
  }, [isOpen]);

  return (
    <Dialog.Root open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <Dialog.Content className="nxrm-metric-help-modal" aria-describedby={undefined}>
        <Flex justify="between" align="center" gap="3" className="nxrm-metric-help-modal__header">
          <Dialog.Title asChild>
            <Text as="h2" size="5" weight="bold">
              How is {content?.title ?? 'this metric'} calculated?
            </Text>
          </Dialog.Title>
          <Dialog.Close>
            <Button variant="ghost" color="gray" size="1" onClick={onClose} aria-label="Close">
              <X size={20} />
            </Button>
          </Dialog.Close>
        </Flex>

        {content && (
          <Box className="nxrm-metric-help-modal__body">
            <Box className="nxrm-metric-help-modal__section">
              <Text as="p" size="2" weight="medium" className="nxrm-metric-help-modal__label">
                The chart
              </Text>
              <Text as="p" size="2" color="gray">
                {content.chartSection}
              </Text>
            </Box>
            <Box className="nxrm-metric-help-modal__section">
              <Text as="p" size="2" weight="medium" className="nxrm-metric-help-modal__label">
                When it&apos;s calculated
              </Text>
              <Text as="p" size="2" color="gray">
                {content.whenCalculated}
              </Text>
            </Box>
            <Box className="nxrm-metric-help-modal__section">
              <Text as="p" size="2" weight="medium" className="nxrm-metric-help-modal__label">
                The header number
              </Text>
              <Text as="p" size="2" color="gray">
                {content.headerSection}
              </Text>
            </Box>
            <Box className="nxrm-metric-help-modal__footer">
              <Text as="p" size="1" color="gray">
                See Settings → System → Tasks → &quot;Metric aggregation&quot; for when metrics last ran.
              </Text>
            </Box>
          </Box>
        )}
      </Dialog.Content>
    </Dialog.Root>
  );
}
