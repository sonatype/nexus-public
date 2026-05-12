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

import { NxDivider, NxGlobalSidebar2, NxGlobalSidebar2NavigationLink } from '@sonatype/react-shared-components';
import { faArrowLeft, faArrowRight, faExternalLinkAlt } from '@fortawesome/free-solid-svg-icons';
import UIStrings from '../../constants/UIStrings';
import { ROUTE_NAMES } from '../../routerConfig/routeNames/routeNames';
import { useDefaultAdminRouteName } from './useDefaultAdminRouteName';
import { useClmDashboardVisibility } from './useClmDashboardVisibility';
import {
  LeftNavigationMenuItem,
  LeftNavigationMenuCollapsibleItem,
  LeftNavigationMenuCollapsibleChildItem
} from '@sonatype/nexus-ui-plugin';
import './LeftNavigationMenu.scss';
import useSideNavbarCollapsedState from '../../hooks/useSideNavbarCollapsedState';

export default function LeftNavigationMenu() {
  const { BROWSE, ADMIN } = ROUTE_NAMES;
  const [isOpen, onToggleClick] = useSideNavbarCollapsedState(true);

  const adminInitialRouteName = useDefaultAdminRouteName();
  const isSettingsVisible = !!adminInitialRouteName;
  const clmState = useClmDashboardVisibility();

  return (
    <NxGlobalSidebar2
      className="nxrm-left-nav"
      isOpen={isOpen}
      onToggleClick={onToggleClick}
      toggleOpenIcon={faArrowLeft}
      toggleCloseIcon={faArrowRight}
      logoAltText="Sonatype Nexus Repository"
      logoLink="#"
    >
      <LeftNavigationMenuItem
        name={BROWSE.WELCOME.ROOT}
        text={UIStrings.WELCOME.MENU.text}
        icon={UIStrings.WELCOME.MENU.icon}
        data-analytics-id="nxrm-global-navbar-welcome"
      />

      <LeftNavigationMenuCollapsibleItem
        name={BROWSE.SEARCH.GENERIC}
        text={UIStrings.SEARCH.KEYWORD.MENU.text}
        icon={UIStrings.SEARCH.KEYWORD.MENU.icon}
        params={{ keyword: null }}
        selectedState={BROWSE.SEARCH.ROOT}
        data-analytics-id="nxrm-global-navbar-search"
      >
        <LeftNavigationMenuCollapsibleChildItem
          name={BROWSE.SEARCH.CUSTOM}
          text={UIStrings.SEARCH.CUSTOM.MENU.text}
          icon={UIStrings.SEARCH.CUSTOM.MENU.icon}
          data-analytics-id="nxrm-global-navbar-search-custom"
          params={{ keyword: null }}
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={BROWSE.SEARCH.APT}
          text={UIStrings.SEARCH.APT.MENU.text}
          icon={UIStrings.SEARCH.APT.MENU.icon}
          data-analytics-id="nxrm-global-navbar-search-apt"
          params={{ keyword: null }}
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={BROWSE.SEARCH.CARGO}
          text={UIStrings.SEARCH.CARGO.MENU.text}
          icon={UIStrings.SEARCH.CARGO.MENU.icon}
          data-analytics-id="nxrm-global-navbar-search-cargo"
          params={{ keyword: null }}
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={BROWSE.SEARCH.COCOAPODS}
          text={UIStrings.SEARCH.COCOAPODS.MENU.text}
          icon={UIStrings.SEARCH.COCOAPODS.MENU.icon}
          data-analytics-id="nxrm-global-navbar-search-cocoapods"
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={BROWSE.SEARCH.COMPOSER}
          text={UIStrings.SEARCH.COMPOSER.MENU.text}
          icon={UIStrings.SEARCH.COMPOSER.MENU.icon}
          data-analytics-id="nxrm-global-navbar-search-composer"
          params={{ keyword: null }}
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={BROWSE.SEARCH.CONAN}
          text={UIStrings.SEARCH.CONAN.MENU.text}
          icon={UIStrings.SEARCH.CONAN.MENU.icon}
          data-analytics-id="nxrm-global-navbar-search-conan"
          params={{ keyword: null }}
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={BROWSE.SEARCH.CONDA}
          text={UIStrings.SEARCH.CONDA.MENU.text}
          icon={UIStrings.SEARCH.CONDA.MENU.icon}
          data-analytics-id="nxrm-global-navbar-search-conda"
          params={{ keyword: null }}
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={BROWSE.SEARCH.DOCKER}
          text={UIStrings.SEARCH.DOCKER.MENU.text}
          icon={UIStrings.SEARCH.DOCKER.MENU.icon}
          data-analytics-id="nxrm-global-navbar-search-docker"
          params={{ keyword: null }}
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={BROWSE.SEARCH.GITLFS}
          text={UIStrings.SEARCH.GITLFS.MENU.text}
          icon={UIStrings.SEARCH.GITLFS.MENU.icon}
          data-analytics-id="nxrm-global-navbar-search-gitlfs"
          params={{ keyword: null }}
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={BROWSE.SEARCH.GOLANG}
          text={UIStrings.SEARCH.GOLANG.MENU.text}
          icon={UIStrings.SEARCH.GOLANG.MENU.icon}
          data-analytics-id="nxrm-global-navbar-search-golang"
          params={{ keyword: null }}
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={BROWSE.SEARCH.HELM}
          text={UIStrings.SEARCH.HELM.MENU.text}
          icon={UIStrings.SEARCH.HELM.MENU.icon}
          data-analytics-id="nxrm-global-navbar-search-helm"
          params={{ keyword: null }}
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={BROWSE.SEARCH.HUGGING_FACE}
          text={UIStrings.SEARCH.HUGGING_FACE.MENU.text}
          icon={UIStrings.SEARCH.HUGGING_FACE.MENU.icon}
          data-analytics-id="nxrm-global-navbar-search-hugging_face"
          params={{ keyword: null }}
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={BROWSE.SEARCH.MAVEN}
          text={UIStrings.SEARCH.MAVEN.MENU.text}
          icon={UIStrings.SEARCH.MAVEN.MENU.icon}
          data-analytics-id="nxrm-global-navbar-search-maven"
          params={{ keyword: null }}
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={BROWSE.SEARCH.NPM}
          text={UIStrings.SEARCH.NPM.MENU.text}
          icon={UIStrings.SEARCH.NPM.MENU.icon}
          data-analytics-id="nxrm-global-navbar-search-npm"
          params={{ keyword: null }}
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={BROWSE.SEARCH.NUGET}
          text={UIStrings.SEARCH.NUGET.MENU.text}
          icon={UIStrings.SEARCH.NUGET.MENU.icon}
          data-analytics-id="nxrm-global-navbar-search-nuget"
          params={{ keyword: null }}
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={BROWSE.SEARCH.P2}
          text={UIStrings.SEARCH.P2.MENU.text}
          icon={UIStrings.SEARCH.P2.MENU.icon}
          data-analytics-id="nxrm-global-navbar-search-p2"
          params={{ keyword: null }}
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={BROWSE.SEARCH.PYPI}
          text={UIStrings.SEARCH.PYPI.MENU.text}
          icon={UIStrings.SEARCH.PYPI.MENU.icon}
          data-analytics-id="nxrm-global-navbar-search-pypi"
          params={{ keyword: null }}
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={BROWSE.SEARCH.PUB}
          text={UIStrings.SEARCH.PUB.MENU.text}
          icon={UIStrings.SEARCH.PUB.MENU.icon}
          data-analytics-id="nxrm-global-navbar-search-pub"
          params={{ keyword: null }}
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={BROWSE.SEARCH.R}
          text={UIStrings.SEARCH.R.MENU.text}
          icon={UIStrings.SEARCH.R.MENU.icon}
          data-analytics-id="nxrm-global-navbar-search-r"
          params={{ keyword: null }}
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={BROWSE.SEARCH.RAW}
          text={UIStrings.SEARCH.RAW.MENU.text}
          icon={UIStrings.SEARCH.RAW.MENU.icon}
          data-analytics-id="nxrm-global-navbar-search-raw"
          params={{ keyword: null }}
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={BROWSE.SEARCH.RUBYGEMS}
          text={UIStrings.SEARCH.RUBYGEMS.MENU.text}
          icon={UIStrings.SEARCH.RUBYGEMS.MENU.icon}
          data-analytics-id="nxrm-global-navbar-search-rubygems"
          params={{ keyword: null }}
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={BROWSE.SEARCH.TERRAFORM}
          text={UIStrings.SEARCH.TERRAFORM.MENU.text}
          icon={UIStrings.SEARCH.TERRAFORM.MENU.icon}
          data-analytics-id="nxrm-global-navbar-search-terraform"
          params={{ keyword: null }}
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={BROWSE.SEARCH.YUM}
          text={UIStrings.SEARCH.YUM.MENU.text}
          icon={UIStrings.SEARCH.YUM.MENU.icon}
          data-analytics-id="nxrm-global-navbar-search-yum"
          params={{ keyword: null }}
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={BROWSE.SEARCH.SWIFT}
          text={UIStrings.SEARCH.SWIFT.MENU.text}
          icon={UIStrings.SEARCH.SWIFT.MENU.icon}
          data-analytics-id="nxrm-global-navbar-search-swift"
          params={{ keyword: null }}
        />
      </LeftNavigationMenuCollapsibleItem>

      <LeftNavigationMenuItem
        name={BROWSE.BROWSE.ROOT}
        text={UIStrings.BROWSE.MENU.text}
        icon={UIStrings.BROWSE.MENU.icon}
        params={{ repo: null }}
        data-analytics-id="nxrm-global-navbar-browse"
      />
      <LeftNavigationMenuItem
        selectedState={BROWSE.UPLOAD.ROOT}
        name={BROWSE.UPLOAD.LIST}
        text={UIStrings.UPLOAD.MENU.text}
        icon={UIStrings.UPLOAD.MENU.icon}
        params={{ itemId: null }}
        data-analytics-id="nxrm-global-navbar-upload"
      />
      <LeftNavigationMenuItem
        name={BROWSE.TAGS.ROOT}
        text={UIStrings.TAGS.MENU.text}
        icon={UIStrings.TAGS.MENU.icon}
        params={{ itemId: null }}
        data-analytics-id="nxrm-global-navbar-tags"
      />

      <LeftNavigationMenuItem
        name={BROWSE.MALWARERISK.ROOT}
        text={UIStrings.MALICIOUS_RISK.MENU.text}
        icon={UIStrings.MALICIOUS_RISK.MENU.icon}
        data-analytics-id="nxrm-global-navbar-malwarerisk"
      />

      {isSettingsVisible && (
        <>
          <NxDivider />
          <LeftNavigationMenuItem
            name={adminInitialRouteName}
            selectedState={ADMIN.DIRECTORY}
            text={UIStrings.ADMIN_DIRECTORY.MENU.text}
            icon={UIStrings.ADMIN_DIRECTORY.MENU.icon}
            data-analytics-id="nxrm-global-navbar-admin"
          />
        </>
      )}

      {clmState?.showDashboard ? (
        <NxGlobalSidebar2NavigationLink
          className="nxrm-left-nav__link"
          text={UIStrings.IQ_SERVER.OPEN_DASHBOARD_LINK}
          href={clmState.url}
          icon={faExternalLinkAlt}
          referrerPolicy="no-referrer"
          target="_blank"
          data-analytics-id="nxrm-global-navbar-iq-server"
        />
      ) : null}
    </NxGlobalSidebar2>
  );
}
