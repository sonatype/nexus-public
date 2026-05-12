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
import PropTypes from 'prop-types';
import { Box, Flex, Heading, Text, Separator } from '@radix-ui/themes';
import { ChevronDown } from 'lucide-react';

import './SettingsFormSection.scss';

/**
 * SettingsFormSection - Groups related form fields with a title
 * 
 * @example
 * <SettingsFormSection 
 *   title="S3 Settings" 
 *   description="Configure S3 bucket settings"
 *   icon={<Cloud size={20} />}
 * >
 *   <SettingsTextInput name="bucket" ... />
 *   <SettingsTextInput name="region" ... />
 * </SettingsFormSection>
 */
export function SettingsFormSection({
  title,
  description,
  children,
  icon,
  collapsible = false,
  defaultCollapsed = false,
  className = '',
}) {
  const [collapsed, setCollapsed] = React.useState(defaultCollapsed);

  const handleToggle = () => {
    if (collapsible) {
      setCollapsed(!collapsed);
    }
  };

  return (
    <Box className={`settings-section ${className}`.trim()}>
      {title && (
        collapsible ? (
          <button
            type="button"
            className={`settings-section__header settings-section__header--collapsible`}
            onClick={handleToggle}
            aria-expanded={!collapsed}
          >
            <Flex align="center" gap="2">
              {icon && (
                <Box className="settings-section__icon">
                  {icon}
                </Box>
              )}
              <Box>
                <Heading as="h2" size="3" weight="medium" className="settings-section__title">
                  {title}
                  {collapsible && (
                    <ChevronDown
                      size={14}
                      data-testid="section-chevron"
                      className={`settings-section__chevron ${collapsed ? 'settings-section__chevron--collapsed' : ''}`}
                    />
                  )}
                </Heading>
                {description && (
                  <Text as="p" size="2" className="settings-section__description">
                    {description}
                  </Text>
                )}
              </Box>
            </Flex>
          </button>
        ) : (
          <Box
            className="settings-section__header"
            onClick={handleToggle}
          >
            <Flex align="center" gap="2">
              {icon && (
                <Box className="settings-section__icon">
                  {icon}
                </Box>
              )}
              <Box>
                <Heading as="h2" size="3" weight="medium" className="settings-section__title">
                  {title}
                </Heading>
                {description && (
                  <Text as="p" size="2" className="settings-section__description">
                    {description}
                  </Text>
                )}
              </Box>
            </Flex>
          </Box>
        )
      )}
      {(!collapsible || !collapsed) && (
        <Box className="settings-section__content">
          {children}
        </Box>
      )}
      <Separator size="4" className="settings-section__separator" />
    </Box>
  );
}

SettingsFormSection.propTypes = {
  /** Section title */
  title: PropTypes.string,
  /** Optional description text */
  description: PropTypes.string,
  /** Form fields */
  children: PropTypes.node.isRequired,
  /** Optional icon element to display before the title */
  icon: PropTypes.node,
  /** Whether section can be collapsed */
  collapsible: PropTypes.bool,
  /** Initial collapsed state */
  defaultCollapsed: PropTypes.bool,
  /** Additional CSS class */
  className: PropTypes.string,
};

export default SettingsFormSection;
