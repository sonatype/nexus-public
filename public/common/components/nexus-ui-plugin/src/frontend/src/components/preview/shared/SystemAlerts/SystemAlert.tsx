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

import React, { useState } from 'react';
import { Box, Flex, Text, Button, IconButton } from '@radix-ui/themes';
import { Info, AlertTriangle, AlertCircle, CheckCircle, X } from 'lucide-react';

import './SystemAlert.scss';

/** Alert severity tiers. Each maps to a Radix color scale via the .scss. */
export type SystemAlertTier = 'info' | 'warning' | 'error' | 'success';

const TIER_ICONS = {
  info: Info,
  warning: AlertTriangle,
  error: AlertCircle,
  success: CheckCircle,
} as const;

/**
 * Optional call-to-action button (e.g. "View Details").
 *
 * Provide `onClick` OR `href`. When both are set, `onClick` takes precedence
 * and `href` is ignored. Prefer `onClick` with UI-Router (stateService.go) for
 * in-app navigation; `href` is a fallback that sets `window.location.hash`.
 */
export interface SystemAlertAction {
  /** Button label. */
  label: string;
  /** Click handler. Takes precedence over `href` when both are provided. */
  onClick?: () => void;
  /** Hash navigation target (without leading '#'). Used only when `onClick` is not set. */
  href?: string;
  /** Analytics id passed through to the button. */
  analyticsId?: string;
}

export interface SystemAlertProps {
  /** Bold first-line title. Optional — omit for single-line, message-only banners. */
  title?: string;
  /** Second-line descriptive message. */
  message: React.ReactNode;
  /** Severity tier; drives color + icon. Defaults to 'info'. */
  tier?: SystemAlertTier;
  /**
   * Optional primary CTA (e.g. "View Details"). Configurable per consumer.
   * Omit to render no action button.
   */
  action?: SystemAlertAction;
  /**
   * When true, renders a dismiss (X) button and the banner can be closed.
   * Defaults to false (non-dismissable) — matches system-state banners that
   * must persist while the underlying condition is active.
   */
  dismissable?: boolean;
  /** Called after the user dismisses the banner (only when dismissable). */
  onDismiss?: () => void;
  /** Additional CSS class. */
  className?: string;
}

/**
 * SystemAlert - Application-scoped banner for the Preview UI.
 *
 * Full-width, top-of-app notice rendered above the global header. Follows the
 * Sonatype design-system "System Alerts" pattern: leading tier icon, bold title
 * line, descriptive message line, and an optional right-aligned CTA. Colors are
 * design-token driven so the banner adapts to light and dark themes.
 *
 * Both the CTA action and dismissability are configurable by the consumer.
 */
export function SystemAlert({
  title,
  message,
  tier = 'info',
  action,
  dismissable = false,
  onDismiss,
  className = '',
}: SystemAlertProps): React.ReactElement | null {
  const [dismissed, setDismissed] = useState(false);

  if (dismissed) {
    return null;
  }

  const Icon = TIER_ICONS[tier] || Info;

  const handleDismiss = () => {
    setDismissed(true);
    onDismiss?.();
  };

  const handleAction = () => {
    // `action` is always set here — the CTA button that calls this handler
    // only renders when `action` is truthy. Guard kept for type narrowing.
    if (!action) return;
    if (action.onClick) {
      action.onClick();
    } else if (action.href) {
      window.location.hash = action.href;
    }
  };

  return (
    <Box
      className={`nxrm-system-alert nxrm-system-alert--${tier} ${className}`.trim()}
      role={tier === 'error' ? 'alert' : 'status'}
      aria-live={tier === 'error' ? 'assertive' : 'polite'}
      data-testid="nxrm-system-alert"
    >
      <Flex align="center" justify="between" gap="3" className="nxrm-system-alert__inner">
        <Flex align="center" gap="3" className="nxrm-system-alert__lead">
          <Icon size={20} className="nxrm-system-alert__icon" aria-hidden />
          <Box className="nxrm-system-alert__text">
            {title && (
              <Text size="2" weight="bold" className="nxrm-system-alert__title">
                {title}
              </Text>
            )}
            <Text size="2" className="nxrm-system-alert__message">
              {message}
            </Text>
          </Box>
        </Flex>

        <Flex align="center" gap="2" className="nxrm-system-alert__actions">
          {action && (
            <Button
              variant="solid"
              size="2"
              onClick={handleAction}
              data-analytics-id={action.analyticsId}
              className="nxrm-system-alert__cta"
            >
              {action.label}
            </Button>
          )}
          {dismissable && (
            <IconButton
              variant="ghost"
              color="gray"
              size="1"
              onClick={handleDismiss}
              aria-label="Dismiss alert"
              className="nxrm-system-alert__dismiss"
            >
              <X size={16} />
            </IconButton>
          )}
        </Flex>
      </Flex>
    </Box>
  );
}

export default SystemAlert;
