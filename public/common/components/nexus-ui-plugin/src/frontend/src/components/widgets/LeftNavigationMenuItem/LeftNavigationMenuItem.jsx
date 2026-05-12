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
import { faLink } from '@fortawesome/free-solid-svg-icons';
import { NxGlobalSidebar2NavigationLink } from '@sonatype/react-shared-components';
import PropTypes from 'prop-types';
import { useNavigationLinkState } from '../../../router/useNavigationLinkState';
import classnames from 'classnames';

/**
 * Left navigation menu item
 *
 * @param name router state name
 * @param text the text to be shown for the link
 * @param icon the icon to be shown for the link
 * @param selectedState optional, router state to use for isActive logic. This needs to point to a parent state
 * when the Menu Item should be active for multiple child states.
 * @param params optional, route parameters - needed in order for left nav link to work after refreshing. This is
 * only needed for List/Details pages that are not using ui-router to handle details parameters. This will not be needed
 * once all details pages are using ui-router.
 */
export function LeftNavigationMenuItem({ name, text, icon, selectedState, params, ...props }) {
  const { isVisible, href, isSelected } = useNavigationLinkState(name, selectedState, params);

  if (!isVisible) {
    return null;
  }

  if (!href) {
    console.warn(`LeftNavigationMenuItem -> failed to resolve link information from route name "${name}"`);
  }

  // Phase 4.0: Context-aware navigation - preserve preview/default mode
  const currentPath = typeof window !== 'undefined' ? window.location.hash : '';
  const isPreviewUI = currentPath.startsWith('#preview'); // No slash after #
  
  let contextAwareHref = href;
  if (isPreviewUI && href && !href.includes('preview')) {
    // Extract path after # (e.g., "#browse/upload" -> "browse/upload")
    let path = href;
    if (href.includes('#')) {
      path = href.substring(href.indexOf('#') + 1);
    }
    // Remove leading / if present
    path = path.replace(/^\//, '');
    // Add preview/ prefix (creates "#preview/browse/upload")
    contextAwareHref = '#preview/' + path;
    console.log('[LeftNav] Preview UI - stay in preview:', href, '->', contextAwareHref);
  }

  const classNames = classnames('nxrm-left-nav__link', {
    'nxrm-left-nav__link--no-icon': !icon
  });
  return (
    <NxGlobalSidebar2NavigationLink
      className={classNames}
      text={text}
      isSelected={isSelected}
      href={contextAwareHref}
      icon={icon || faLink}
      {...props}
    />
  );
}

LeftNavigationMenuItem.propTypes = {
  name: PropTypes.string.isRequired,
  text: PropTypes.string.isRequired,
  icon: PropTypes.object,
  selectedState: PropTypes.string,
  params: PropTypes.object
};
