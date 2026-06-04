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
/**
 * Validates IPv4, IPv6, and CIDR notation for IP addresses.
 *
 * Supported formats:
 * - IPv4: 192.168.1.1
 * - IPv4 CIDR: 10.0.0.0/24
 * - IPv6 Full: 2001:0db8:0000:0000:0000:0000:0000:0001
 * - IPv6 Compressed: 2001:db8::1
 * - IPv6 Localhost: ::1
 * - IPv6 CIDR: 2001:db8::/32
 * - IPv4-mapped IPv6: ::ffff:192.168.1.1
 *
 * @param {string} ip - IP address to validate
 * @returns {boolean} true if valid IPv4 or IPv6 address/CIDR
 */
export const isValidIP = (ip) => {
  if (!ip || typeof ip !== 'string') {
    return false;
  }

  const trimmedIp = ip.trim();

  // IPv4 address pattern - rejects leading zeros (e.g., 192.168.001.001)
  // Each octet must be: 0, or 1-9 followed by optional digits (no leading zeros)
  const ipv4Pattern = /^((0|[1-9]\d{0,2})\.){3}(0|[1-9]\d{0,2})$/;

  // IPv4 CIDR pattern - rejects leading zeros in IP octets
  const ipv4CidrPattern = /^((0|[1-9]\d{0,2})\.){3}(0|[1-9]\d{0,2})\/\d{1,2}$/;

  // IPv6 patterns (comprehensive regex covering all valid formats)
  const ipv6Pattern = /^(([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|::)$/;

  // IPv6 CIDR pattern
  const ipv6CidrPattern = /^([0-9a-fA-F:]+)\/(\d{1,3})$/;

  // IPv4-mapped IPv6 pattern (::ffff:192.168.1.1) - rejects leading zeros in IPv4 part
  const ipv4MappedIpv6Pattern = /^::ffff:((0|[1-9]\d{0,2})\.){3}(0|[1-9]\d{0,2})$/i;

  // Check IPv4 address
  if (ipv4Pattern.test(trimmedIp)) {
    const parts = trimmedIp.split('.');
    return parts.every(part => {
      const num = parseInt(part, 10);
      return num >= 0 && num <= 255;
    });
  }

  // Check IPv4 CIDR notation
  if (ipv4CidrPattern.test(trimmedIp)) {
    const [ipPart, cidr] = trimmedIp.split('/');
    const cidrNum = parseInt(cidr, 10);

    // Recursively validate the IP part
    if (!isValidIP(ipPart)) {
      return false;
    }

    // CIDR must be 0-32 for IPv4
    return cidrNum >= 0 && cidrNum <= 32;
  }

  // Check IPv6 address (handles full, compressed, localhost, link-local)
  if (ipv6Pattern.test(trimmedIp)) {
    // Additional validation: ensure at most one :: compression
    const doubleColonCount = (trimmedIp.match(/::/g) || []).length;
    if (doubleColonCount > 1) {
      return false;
    }

    // Additional validation: no more than 8 groups
    const groups = trimmedIp.split(':').filter(g => g !== '');
    if (groups.length > 8) {
      return false;
    }

    return true;
  }

  // Check IPv6 CIDR notation
  if (ipv6CidrPattern.test(trimmedIp)) {
    const [ipPart, cidr] = trimmedIp.split('/');
    const cidrNum = parseInt(cidr, 10);

    // Recursively validate the IP part
    if (!isValidIP(ipPart)) {
      return false;
    }

    // CIDR must be 0-128 for IPv6
    return cidrNum >= 0 && cidrNum <= 128;
  }

  // Check IPv4-mapped IPv6 address
  if (ipv4MappedIpv6Pattern.test(trimmedIp)) {
    // Extract the IPv4 part and validate it
    const ipv4Part = trimmedIp.substring(7); // Remove "::ffff:"
    return isValidIP(ipv4Part);
  }

  return false;
};

/**
 * Normalizes an IPv6 address to its compressed canonical form.
 * This matches the normalization done by Google Guava's InetAddresses.toAddrString().
 *
 * Rules:
 * 1. Remove leading zeros from each group (0db8 -> db8)
 * 2. Replace the longest sequence of consecutive all-zero groups with ::
 * 3. If multiple sequences have the same length, replace the first one
 * 4. Lowercase all hex digits
 *
 * @param {string} ipv6 - IPv6 address to normalize
 * @returns {string} Normalized IPv6 address
 */
export const normalizeIPv6 = (ipv6) => {
  if (!ipv6 || typeof ipv6 !== 'string') {
    return ipv6;
  }

  const trimmed = ipv6.trim().toLowerCase();

  // Handle IPv4-mapped IPv6
  if (trimmed.startsWith('::ffff:')) {
    return trimmed;
  }

  // Handle already compressed forms
  if (trimmed === '::' || trimmed === '::1') {
    return trimmed;
  }

  // Expand :: to full form first for easier processing
  let expanded = trimmed;
  if (expanded.includes('::')) {
    const parts = expanded.split('::');
    const left = parts[0] ? parts[0].split(':') : [];
    const right = parts[1] ? parts[1].split(':') : [];
    const missing = 8 - left.length - right.length;
    const middle = Array(missing).fill('0');
    expanded = [...left, ...middle, ...right].join(':');
  }

  // Split into groups and remove leading zeros
  const groups = expanded.split(':').map(group => {
    // Remove leading zeros, but keep at least one digit
    return group.replace(/^0+/, '') || '0';
  });

  // Ensure we have exactly 8 groups
  if (groups.length !== 8) {
    return ipv6; // Return original if something went wrong
  }

  // Find the longest sequence of consecutive zeros
  let longestStart = -1;
  let longestLen = 0;
  let currentStart = -1;
  let currentLen = 0;

  for (let i = 0; i < groups.length; i++) {
    if (groups[i] === '0') {
      if (currentStart === -1) {
        currentStart = i;
        currentLen = 1;
      } else {
        currentLen++;
      }
    } else {
      if (currentLen > longestLen) {
        longestStart = currentStart;
        longestLen = currentLen;
      }
      currentStart = -1;
      currentLen = 0;
    }
  }
  // Check at end of loop
  if (currentLen > longestLen) {
    longestStart = currentStart;
    longestLen = currentLen;
  }

  // Compress the longest zero sequence (only if length > 1)
  if (longestLen > 1) {
    const left = groups.slice(0, longestStart);
    const right = groups.slice(longestStart + longestLen);

    if (left.length === 0 && right.length === 0) {
      return '::';
    } else if (left.length === 0) {
      return '::' + right.join(':');
    } else if (right.length === 0) {
      return left.join(':') + '::';
    } else {
      return left.join(':') + '::' + right.join(':');
    }
  }

  return groups.join(':');
};

/**
 * Validates and normalizes an IP address or CIDR notation.
 * Returns an object with validation result and normalized value.
 *
 * @param {string} ip - IP address or CIDR to validate
 * @returns {{isValid: boolean, normalized: string|null, error: string|null}}
 */
export const validateAndNormalize = (ip) => {
  if (!isValidIP(ip)) {
    return { isValid: false, normalized: null, error: 'Invalid IP address format' };
  }

  const trimmed = ip.trim();

  // Check if it's CIDR notation
  if (trimmed.includes('/')) {
    const [ipPart, cidr] = trimmed.split('/');

    // Check if it's IPv6 CIDR
    if (ipPart.includes(':')) {
      const normalizedIp = normalizeIPv6(ipPart);
      return { isValid: true, normalized: `${normalizedIp}/${cidr}`, error: null };
    }

    // IPv4 CIDR - no normalization needed
    return { isValid: true, normalized: trimmed, error: null };
  }

  // Check if it's IPv6
  if (trimmed.includes(':')) {
    return { isValid: true, normalized: normalizeIPv6(trimmed), error: null };
  }

  // IPv4 - no normalization needed
  return { isValid: true, normalized: trimmed, error: null };
};
