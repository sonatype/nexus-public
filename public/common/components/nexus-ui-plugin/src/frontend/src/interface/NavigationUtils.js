/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

import ExtJS from "./ExtJS";

/**
 * This is used to check the visibility of route defined with UIRouter. It expects a visibilityRequirements block
 *
 * visibilityRequirements may have any of the following:
 *
 *   bundleActive: string
 *   licenseValid: { key: string, defaultValue: boolean } []
 *   statesEnabled: { key: string, defaultValue: boolean } []
 *   permissions : string []
 *   requiresPermission: string
 *   requiresAnyPermission: string []
 *   permissionPrefix: string
 *   permissionPrefixes: string []
 *   editions: string []
 *   requiresUser: boolean
 *
 * @param visibilityRequirements
 * @returns {boolean}
 */
export function isVisible(visibilityRequirements) {
  if (!visibilityRequirements) {
    return true;
  }

  const {
    bundle,
    licenseValid,
    statesEnabled,
    permissions,
    requiresPermission,
    requiresAnyPermission,
    permissionPrefix,
    permissionPrefixes,
    capability,
    editions,
    requiresUser,
    browseableFormat,
    notClustered,
    anonymousAccessOrHasUser
  } = visibilityRequirements;

  // check that all our expected global dependencies are in place
  // If ExtJS isn't ready yet, check for session cookie to determine visibility
  const depsValid = hasValidDependencies();
  if (!depsValid) {
    // If no visibility requirements exist, allow the route (for login, etc.)
    const hasAnyRequirements = bundle || licenseValid || statesEnabled || permissions ||
                                requiresPermission || requiresAnyPermission || permissionPrefix ||
                                permissionPrefixes || capability || editions || requiresUser ||
                                browseableFormat || notClustered || anonymousAccessOrHasUser;
    if (!hasAnyRequirements) {
      return true;
    }
    // CRITICAL FIX for debug mode refresh: If user has session cookie, assume they're authorized
    // until ExtJS loads and can properly check permissions. This prevents logout on refresh.
    if (document.cookie.includes('NXSESSIONID')) {
      return true;
    }
    return false;
  }

  const Application = NX?.app?.Application;
  const Security = NX?.Security;

  // hide this route on HA
  if (notClustered && isClustered()) {
    return false;
  }

  if (bundle && !Application.bundleActive(bundle)) {
    // check that the bundles required by the route are enabled
    return false;
  }

  // check that all licenses required by this are present
  if (licenseValid && !areAllRequiredLicensesPresent(licenseValid)) {
    return false;
  }

  // check that all required statesEnabled for this route are present
  if (statesEnabled && !areAllRequiredStatesEnabled(statesEnabled)) {
    return false;
  }

  // check that all required permissions are present
  if (permissions && !areAllRequiredPermissionsPresent(permissions)) {
    return false
  }

  // check a single required permission (convenience shorthand for a single permission string)
  if (requiresPermission && !NX.Permissions.check(requiresPermission)) {
    return false;
  }

  // check that at least one of the listed permissions is present
  if (requiresAnyPermission && !hasAnyOfTheRequiredPermissions(requiresAnyPermission)) {
    return false;
  }

  if (permissionPrefix && !hasAnyPermissionWithPrefix(permissionPrefix)) {
    return false;
  }

  // check that user has ANY permission matching at least one of the prefixes
  if (permissionPrefixes && !hasAnyPermissionWithAnyPrefix(permissionPrefixes)) {
    return false;
  }

  // check that edition requirements are met, i.e. must be PRO or COMMUNITY
  if (editions && !meetsEditionRequirement(editions)) {
    return false;
  }

  // check if the required capability is enabled and active
  if (capability && !isTheRequiredCapabilityPresentAndActive(capability)) {
    return false;
  }

  if (requiresUser && !Security.hasUser() && !hasAnonymousPermissions()) {
    return false
  }

  if (browseableFormat && !isFormatBrowseable(browseableFormat)) {
    return false;
  }

  if (anonymousAccessOrHasUser &&
      !(!!NX.State.getValue('anonymousUsername') || Security.hasUser())) {
    return false;
  }

  return true;
}

function isFormatBrowseable(browseableFormat) {
  return Ext.getApplication().getController('NX.coreui.controller.BrowseableFormats').getFormats().includes(browseableFormat);
}

function areAllRequiredStatesEnabled(statesEnabled) {
  return statesEnabled.every(state => {
    const stateValue = NX.State.getValue(state.key, state.defaultValue);

    if (typeof stateValue === "boolean") {
      return stateValue;
    }
    else if (Array.isArray(stateValue)) {
      return stateValue.length > 0;
    }
    else {
      return stateValue?.enabled ?? false;
    }
  });
}

function areAllRequiredPermissionsPresent(permissions) {
  return permissions.every((permission) => {
    const hasPermission = NX.Permissions.check(permission);
    return hasPermission;
  });
}

/**
 * Check if user has ANY of the listed permissions.
 * Returns true if at least one permission is present.
 *
 * @param {string[]} permissions - Array of permissions to check
 * @returns {boolean} True if user has any of the permissions
 */
function hasAnyOfTheRequiredPermissions(permissions) {
  if (!NX.Permissions) {
    return false;
  }

  for (const permission of permissions) {
    if (NX.Permissions.check(permission)) {
      return true;
    }
  }

  return false;
}

/**
 * Check if user has ANY permission starting with the given prefix.
 * This replicates the behavior of NX.Permissions.checkExistsWithPrefix from the older version.
 *
 * @param {string} prefix - The permission prefix to check
 * @returns {boolean} True if user has any permission starting with the prefix
 */
function hasAnyPermissionWithPrefix(prefix) {
  if (!NX.Permissions || !NX.Permissions.permissions) {
    return false;
  }

  const permissions = NX.Permissions.permissions;

  // Check if any permission starts with the prefix and is permitted
  for (const permission in permissions) {
    if (permission.startsWith(prefix) && permissions[permission] === true) {
      return true;
    }
  }

  return false;
}

/**
 * Check if user has ANY permission starting with ANY of the given prefixes.
 * This is useful for routes that should be visible with multiple types of permissions,
 * such as Browse which should be visible for both repository-view and repository-content-selector permissions.
 *
 * @param {string[]} prefixes - Array of permission prefixes to check
 * @returns {boolean} True if user has any permission starting with any of the prefixes
 */
function hasAnyPermissionWithAnyPrefix(prefixes) {
  if (!prefixes || prefixes.length === 0) {
    return false;
  }

  if (!NX.Permissions || !NX.Permissions.permissions) {
    return false;
  }

  // Check if any permission matches any of the prefixes
  for (const prefix of prefixes) {
    if (hasAnyPermissionWithPrefix(prefix)) {
      return true;
    }
  }

  return false;
}

function areAllRequiredLicensesPresent(licenseValid) {
  return licenseValid.every(
      licenseValid => {
        const value = NX.State.getValue(licenseValid.key, licenseValid.defaultValue)
        if (typeof value !== 'object') {
          return false;
        }

        return value.licenseValid;
      });
}

function isTheRequiredCapabilityPresentAndActive(capability) {
  const activeTypes = NX.State.getValue('capabilityActiveTypes') || [];
  const createdTypes = NX.State.getValue('capabilityCreatedTypes') || [];

  return activeTypes.includes(capability) && createdTypes.includes(capability);
}

function meetsEditionRequirement(editions) {
  return editions.some((edition) => {
    return NX.State.getEdition() === edition;
  });
}

function isClustered() {
  return !!NX.State.getValue('nexus.datastore.clustered.enabled');
}

function hasValidDependencies() {
  const Application = NX.app?.Application;
  const State = NX.State;
  const Permissions = NX.Permissions;
  const Security = NX.Security;

  // NX.getApplication is attached by ExtJS only after Ext.app.Application
  // is instantiated (initNamespace). Without it, NX.State.getValue() will
  // throw "NX.getApplication is not a function" because internally it calls
  // NX.getApplication().getStateController(). Guard here so React routes that
  // run before ExtJS finishes booting fall through to the cookie/no-deps path.
  const appReady =
      typeof window.NX?.getApplication === 'function' &&
      !!window.NX.getApplication();

  return !!(Application && State && Permissions && Security && appReady);
}

// A `requiresUser` route should also be visible to an anonymous subject that has
// been granted permissions (e.g. anonymous assigned nx-admin — NEXUS-47114).
// Per-route permission checks above already filter routes the anonymous user
// cannot access, so this only re-opens routes the anonymous user genuinely has
// rights to.
//
// Permissions live at NX.Permissions.permissions (a flat map of id → permitted),
// populated by NX.controller.Permissions from rapture_Security.getPermissions.
// They are NOT contributed to NX.State — SecurityComponent.getState() only
// contributes `user` and `anonymousUsername`.
function hasAnonymousPermissions() {
  const Security = NX?.Security;
  if (!Security || Security.hasUser()) {
    return false;
  }
  const perms = NX?.Permissions?.permissions;
  return !!perms && Object.keys(perms).length > 0;
}

export function useIsVisible(visibilityRequirements) {
  return ExtJS.useVisiblityWithChanges(() => isVisible(visibilityRequirements));
}

export function isExtjsCapabilitiesEnabled() {
  return ExtJS.state().getValue('nexus.extjs.capabilities.enabled');
}

export function isReactCapabilitiesEnabled() {
  return ExtJS.state().getValue('nexus.react.capabilities.enabled');
}
