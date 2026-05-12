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
import { Box, Flex, Text, Card, Button } from '@radix-ui/themes';
import { Construction, ArrowLeft, Sparkles, FlaskConical } from 'lucide-react';

import { canPreviewWip, getWipPreviewUrl } from '../../../config/previewFeatureFlags';
import './ComingSoonPage.scss';

/**
 * Coming Soon / Not Implemented Placeholder Page
 * Used for Preview UI features that are still being developed
 * 
 * SECURITY:
 * - Production: Shows Coming Soon only, no way to access WIP
 * - Development: Shows Coming Soon with "View WIP" link for testing
 */
export default function ComingSoonPage({ 
  featureName = 'This Feature',
  featureKey = null,
  description = 'We\'re working hard to bring this feature to the new Nexus One UI.',
  showBackButton = true,
  defaultUrl = '#browse/welcome',
}) {
  const handleGoBack = () => {
    window.history.back();
  };

  const handleGoToDefault = () => {
    window.location.hash = defaultUrl.replace('#', '');
  };

  // Check if WIP preview is available (dev mode only)
  const showWipLink = featureKey && canPreviewWip(featureKey);
  const wipUrl = showWipLink ? getWipPreviewUrl(featureKey) : null;

  const handleViewWip = () => {
    if (wipUrl) {
      window.location.href = wipUrl;
    }
  };

  return (
    <div className="coming-soon-page">
      <Card className="coming-soon-page__card">
        <Flex direction="column" align="center" gap="6" p="8">
          {/* Icon */}
          <div className="coming-soon-page__icon">
            <Construction size={64} strokeWidth={1.5} />
            <Sparkles size={24} className="coming-soon-page__sparkle" />
          </div>

          {/* Title */}
          <Text size="8" weight="bold" className="coming-soon-page__title">
            Coming Soon
          </Text>

          {/* Feature Name */}
          <Text size="5" color="gray" className="coming-soon-page__feature">
            {featureName}
          </Text>

          {/* Description */}
          <Text size="3" color="gray" align="center" className="coming-soon-page__description">
            {description}
          </Text>

          {/* Migration Notice */}
          <Box className="coming-soon-page__notice">
            <Text size="2" color="gray">
              This page is part of the Nexus One UI rollout. The feature will be available soon 
              with a fresh new look using Radix UI components.
            </Text>
          </Box>

          {/* Actions */}
          <Flex gap="3" mt="4">
            {showBackButton && (
              <Button 
                variant="soft" 
                size="3"
                onClick={handleGoBack}
                className="coming-soon-page__button"
              >
                <ArrowLeft size={18} />
                Go Back
              </Button>
            )}
            <Button 
              variant="solid" 
              size="3"
              onClick={handleGoToDefault}
              className="coming-soon-page__button coming-soon-page__button--primary"
            >
              Go to Dashboard
            </Button>
          </Flex>

          {/* DEV ONLY: View WIP Version */}
          {showWipLink && (
            <Box className="coming-soon-page__dev-section">
              <Text size="1" color="orange" weight="medium" mb="2">
                🔧 Development Mode
              </Text>
              <Button 
                variant="outline" 
                size="2"
                onClick={handleViewWip}
                className="coming-soon-page__button coming-soon-page__button--wip"
              >
                <FlaskConical size={16} />
                View WIP Version
              </Button>
              <Text size="1" color="gray" mt="2">
                This link is only visible in development.
              </Text>
            </Box>
          )}
        </Flex>
      </Card>
    </div>
  );
}


