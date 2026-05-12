/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are
 * trademarks of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark
 * of the Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

/**
 * Repository Creation wizard – Step 5: Enable RHC (Repository Health Check).
 * Proxy repos only. Same pattern as Firewall: Enable or None (skip).
 */
import React, { useState } from 'react';
import { Box, Flex, Text, Card, Button } from '@radix-ui/themes';
import { ShieldCheck } from 'lucide-react';
import { useRepositoriesApi } from './useRepositoriesApi';

import './RepositoryRHCStep.scss';

export interface RepositoryRHCStepProps {
  /** Required for immediate mode; omit for deferred mode */
  repositoryName?: string;
  /** In immediate mode: called after API succeeds. In deferred: not used (Create Repository triggers form submit). */
  onComplete?: () => void;
  /** Deferred mode: store choice, no API calls. Applied when Create Repository is clicked. */
  mode?: 'immediate' | 'deferred';
  /** Deferred mode: current choice */
  value?: 'enable' | 'none';
  /** Deferred mode: when user selects Enable or None */
  onChoice?: (choice: 'enable' | 'none') => void;
}

export function RepositoryRHCStep({
  repositoryName = '',
  onComplete,
  mode = 'immediate',
  value = 'none',
  onChoice,
}: RepositoryRHCStepProps): JSX.Element {
  const { enableHealthCheck } = useRepositoriesApi();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const isDeferred = mode === 'deferred';

  const handleEnable = async () => {
    if (isDeferred) {
      onChoice?.('enable');
      return;
    }
    setLoading(true);
    setError(null);
    try {
      await enableHealthCheck(repositoryName);
      onComplete?.();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to enable Health Check');
    } finally {
      setLoading(false);
    }
  };

  const handleNone = () => {
    if (isDeferred) {
      onChoice?.('none');
    } else {
      onComplete?.();
    }
  };

  return (
    <Box p="4" className="repository-rhc-step">
      <Card size="2">
        <Flex direction="column" gap="4">
          <Flex align="center" gap="3">
            <ShieldCheck size={28} color="var(--blue-9)" aria-hidden />
            <Box>
              <Text size="4" weight="bold" as="div" mb="1">
                Enable Repository Health Check
              </Text>
              <Text size="2" color="gray" as="div">
                Repository Health Check analyzes components for security vulnerabilities and license issues. You can
                enable it now or configure it later from the repository profile.
              </Text>
            </Box>
          </Flex>

          {error && (
            <Text size="2" color="red">
              {error}
            </Text>
          )}

          {isDeferred && (
            <Text size="2" color="gray" mb="1" as="div">
              {value === 'enable' ? 'RHC will be enabled' : 'Current choice: None (skip for now)'}
            </Text>
          )}

          <Flex gap="3" wrap="wrap">
            <Button
              type="button"
              variant={isDeferred && value === 'none' ? 'solid' : 'soft'}
              color="gray"
              size="3"
              onClick={handleNone}
              disabled={!isDeferred && loading}
              aria-pressed={isDeferred && value === 'none'}
              className={isDeferred && value === 'none' ? 'repository-rhc-step__btn--selected' : ''}
            >
              None
            </Button>
            <Button
              type="button"
              variant={isDeferred && value === 'enable' ? 'solid' : 'soft'}
              color="blue"
              size="3"
              onClick={handleEnable}
              disabled={!isDeferred && loading}
              aria-pressed={isDeferred && value === 'enable'}
              className={isDeferred && value === 'enable' ? 'repository-rhc-step__btn--selected' : ''}
            >
              Enable Health Check
            </Button>
          </Flex>
        </Flex>
      </Card>
    </Box>
  );
}
