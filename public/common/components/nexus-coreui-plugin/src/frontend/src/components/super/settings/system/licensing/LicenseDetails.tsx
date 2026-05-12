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
import { Box, Flex, Text, Card, Grid } from '@radix-ui/themes';
import { LicenseData, formatDate, parseLicenseTypes } from './types';

import './LicenseDetails.scss';

interface LicenseDetailsProps {
  license: LicenseData;
}

/**
 * LicenseDetails - Display license information in read-only format
 */
export function LicenseDetails({ license }: LicenseDetailsProps) {
  const licenseTypes = parseLicenseTypes(license.licenseType);
  const effectiveDate = formatDate(license.effectiveDate);
  const expirationDate = formatDate(license.expirationDate);

  return (
    <Card className="license-details">
      <Text as="h3" size="4" weight="medium" className="license-details__title">
        Licensing
      </Text>

      <Grid columns="2" gap="4" className="license-details__grid">
        {/* Left Column */}
        <Box className="license-details__column">
          <Box className="license-details__field">
            <Text as="label" size="2" weight="medium" className="license-details__label">
              Company
            </Text>
            <Text size="2" className="license-details__value">
              {license.contactCompany || '—'}
            </Text>
          </Box>

          <Box className="license-details__field">
            <Text as="label" size="2" weight="medium" className="license-details__label">
              Name
            </Text>
            <Text size="2" className="license-details__value">
              {license.contactName || '—'}
            </Text>
          </Box>

          <Box className="license-details__field">
            <Text as="label" size="2" weight="medium" className="license-details__label">
              Email
            </Text>
            <Text size="2" className="license-details__value">
              {license.contactEmail || '—'}
            </Text>
          </Box>
        </Box>

        {/* Right Column */}
        <Box className="license-details__column">
          <Box className="license-details__field">
            <Text as="label" size="2" weight="medium" className="license-details__label">
              Effective Date
            </Text>
            <Text size="2" className="license-details__value">
              {effectiveDate}
            </Text>
          </Box>

          <Box className="license-details__field">
            <Text as="label" size="2" weight="medium" className="license-details__label">
              Expiration Date
            </Text>
            <Text size="2" className="license-details__value">
              {expirationDate}
            </Text>
          </Box>

          <Box className="license-details__field">
            <Text as="label" size="2" weight="medium" className="license-details__label">
              License Type(s)
            </Text>
            {licenseTypes.length > 0 ? (
              <Flex direction="column" gap="1">
                {licenseTypes.map((type, index) => (
                  <Text key={index} size="2" className="license-details__value">
                    {type}
                  </Text>
                ))}
              </Flex>
            ) : (
              <Text size="2" className="license-details__value">
                —
              </Text>
            )}
          </Box>

          {license.licensedUsers !== undefined && license.licensedUsers > 0 && (
            <Box className="license-details__field">
              <Text as="label" size="2" weight="medium" className="license-details__label">
                Number of Licensed Users
              </Text>
              <Text size="2" className="license-details__value">
                {license.licensedUsers}
              </Text>
            </Box>
          )}

          <Box className="license-details__field">
            <Text as="label" size="2" weight="medium" className="license-details__label">
              Fingerprint
            </Text>
            <Text size="2" className="license-details__value license-details__fingerprint">
              {license.fingerprint || '—'}
            </Text>
          </Box>
        </Box>
      </Grid>
    </Card>
  );
}

export default LicenseDetails;


