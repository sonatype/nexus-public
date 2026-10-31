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
import { Box, Text, Card, Grid, Link } from '@radix-ui/themes';
import { ExternalLink } from 'lucide-react';
import { scrollToUsageCenter } from '../../../../../../interface/LocationUtils';

import './LicensedUsage.scss';

interface LicensedUsageProps {
  maxRepoRequests: string;
  maxRepoComponents: string;
}

/**
 * LicensedUsage - Display usage limits and links
 */
export function LicensedUsage({ maxRepoRequests, maxRepoComponents }: LicensedUsageProps) {
  return (
    <Card className="licensed-usage">
      <Text as="h3" size="4" weight="medium" className="licensed-usage__title">
        Licensed Usage
      </Text>

      <Grid columns="2" gap="4" className="licensed-usage__grid">
        {/* Left Column */}
        <Box className="licensed-usage__column">
          <Box className="licensed-usage__field">
            <Text as="label" size="2" weight="medium" className="licensed-usage__label">
              Requests Per Month
            </Text>
            <Text size="2" className="licensed-usage__value">
              {maxRepoRequests}
            </Text>
          </Box>

          <Box className="licensed-usage__field">
            <Text as="label" size="2" weight="medium" className="licensed-usage__label">
              Total Components
            </Text>
            <Text size="2" className="licensed-usage__value">
              {maxRepoComponents}
            </Text>
          </Box>
        </Box>

        {/* Right Column */}
        <Box className="licensed-usage__column">
          <Box className="licensed-usage__description">
            <Text size="2" className="licensed-usage__description-text">
              Your license is based on the total components stored and monthly requests.
              Track your current consumption on the{' '}
              <Link
                href="#"
                onClick={(e) => {
                  e.preventDefault();
                  scrollToUsageCenter();
                }}
                className="licensed-usage__link"
              >
                Usage Center
              </Link>
              .
            </Text>
            <Text size="2" className="licensed-usage__description-text">
              <Link
                href="http://links.sonatype.com/products/nexus/pro/store?utm_medium=product&utm_source=nexus_repository&utm_campaign=repo_pricing_expansion"
                target="_blank"
                rel="noopener noreferrer"
                className="licensed-usage__link"
              >
                Contact us
                <ExternalLink size={12} style={{ marginLeft: '4px', display: 'inline-block' }} />
              </Link>
              {' '}for additional capacity.
            </Text>
          </Box>
        </Box>
      </Grid>
    </Card>
  );
}

export default LicensedUsage;


