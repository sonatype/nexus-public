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

import React, {JSX} from 'react';
import {
  FORMAT_SVGS,
  FORMAT_IMAGES,
  FORMAT_ICONS,
  TYPE_ICONS,
  DEFAULT_FORMAT_ICON,
} from './formatIcons';

import './FormatIcon.scss';

/**
 * Scaling factor for Simple Icons brand SVGs.
 *
 * Simple Icons includes built-in padding in their viewBox (see https://github.com/simple-icons/simple-icons/blob/develop/CONTRIBUTING.md#adherence-to-the-specification).
 * This padding ensures icons from different brands align visually but means the actual
 * glyph is smaller than the requested size. The 0.75 factor compensates for this padding
 * to achieve visual parity with lucide-react icons which have no such padding.
 *
 * IMPORTANT: The `size` prop represents the tile/container size, not the final glyph size.
 * For brand SVGs, the rendered glyph will be 75% of the tile size. For lucide fallbacks,
 * the glyph fills the tile (100%).
 *
 * If Simple Icons ever ships a tighter viewBox, re-tune this constant.
 */
const SIMPLE_ICONS_VIEWBOX_PADDING_FACTOR = 0.75;

export interface FormatIconProps {
  format: string;
  type?: 'hosted' | 'proxy' | 'group';
  size?: number;
  className?: string;
  useBrandLogo?: boolean;
}

/**
 * FormatIcon - Renders a brand logo or fallback icon for a repo format.
 *
 * Brand logos are bundled as SVG components from @icons-pack/react-simple-icons,
 * ensuring they load without internet access. Each brand SVG renders in its
 * default brand color (via the `color="default"` prop), matching the visual
 * appearance previously served by cdn.simpleicons.org. When useBrandLogo is
 * false, lucide-react fallback icons are displayed instead.
 *
 * Note: The `size` prop specifies the tile/container dimensions. For brand SVGs, the
 * actual icon glyph renders at 75% of this size to compensate for Simple Icons viewBox
 * padding (see SIMPLE_ICONS_VIEWBOX_PADDING_FACTOR). This ensures visual consistency
 * with lucide-react fallback icons which have no such padding.
 */
export function FormatIcon({
  format,
  type,
  size = 32,
  className = '',
  useBrandLogo = true,
}: FormatIconProps): JSX.Element {
  const SvgIcon = useBrandLogo ? FORMAT_SVGS[format] : null;
  const imageUrl = useBrandLogo ? FORMAT_IMAGES[format] : null;
  const FallbackIcon = FORMAT_ICONS[format] || DEFAULT_FORMAT_ICON;
  const TypeIcon = type ? TYPE_ICONS[type] : null;

  return (
    <div
      className={`format-icon-tile ${className}`}
      style={{
        width: size,
        height: size,
        display: 'inline-flex',
        position: 'relative',
        alignItems: 'center',
        justifyContent: 'center',
      }}
    >
      {SvgIcon ? (
        <SvgIcon
          size={size * SIMPLE_ICONS_VIEWBOX_PADDING_FACTOR}
          // "default" is a sentinel value in @icons-pack/react-simple-icons that triggers
          // brand-color rendering. Do not remove — IconComponent accepts any string so this
          // compiles cleanly, but replacing it silently breaks brand colors.
          color="default"
          className="format-icon-tile__logo"
          title=""
          aria-hidden="true"
        />
      ) : imageUrl ? (
        <img
          src={imageUrl}
          alt=""
          className="format-icon-tile__logo"
          style={{
            maxWidth: size * SIMPLE_ICONS_VIEWBOX_PADDING_FACTOR,
            maxHeight: size * SIMPLE_ICONS_VIEWBOX_PADDING_FACTOR,
            width: 'auto',
            height: 'auto',
            objectFit: 'contain'
          }}
          aria-hidden="true"
        />
      ) : (
        <FallbackIcon
          size={size}
          className="format-icon-tile__fallback"
          aria-hidden="true"
        />
      )}

      {type && TypeIcon && (
        <div
          className={`format-icon-tile__type-badge format-icon-tile__type-badge--${type}`}
          style={{
            position: 'absolute',
            bottom: '-4px',
            right: '-4px',
          }}
        >
          <TypeIcon size={12} aria-hidden />
        </div>
      )}
    </div>
  );
}

export default FormatIcon;
