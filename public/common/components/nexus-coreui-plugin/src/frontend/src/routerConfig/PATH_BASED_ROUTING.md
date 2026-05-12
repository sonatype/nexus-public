# Path-Based Routing for Preview UI

## Overview

This document describes the migration from hash-based routing (`#preview/browse/welcome`) to path-based routing (`/preview/browse/welcome`) for the Preview UI.

## Current State

- **Default UI (ExtJS)**: Uses hash-based routing (`#browse/welcome`)
- **Preview UI**: Uses hash-based routing (`#preview/browse/welcome`)
- **Router**: `@uirouter/react` with `hashLocationPlugin`

## Target State

- **Default UI (ExtJS)**: Unchanged - continues using hash-based routing
- **Preview UI**: Path-based routing (`/preview/browse/welcome`)
- **Router**: Hybrid - `pushStateLocationPlugin` for Preview, `hashLocationPlugin` for Default

## Architecture Decision

### Why Path-Based Routing?

1. **Cleaner URLs**: `/component/npm:lodash/overview` vs `#preview/browse/search/component/npm:lodash/overview`
2. **SEO**: Search engines handle path-based URLs better (future consideration)
3. **Standards**: Modern SPAs use path-based routing
4. **Sharing**: Users can share clean URLs

### Why Hybrid Approach?

1. **Backward Compatibility**: Default UI with ExtJS must remain unchanged
2. **Incremental Migration**: Preview UI can migrate without affecting Default UI
3. **Risk Mitigation**: Issues isolated to Preview UI

## Implementation Requirements

### Frontend Changes

1. **Router Configuration** (`createRouter.js`):
   - Add `pushStateLocationPlugin` import
   - Detect route prefix to choose plugin
   - Handle transitions between hash and path routes

2. **Route Definitions** (`previewBrowseRoutes.js`, `previewAdminRoutes.js`):
   - Change URL patterns from `preview/...` to `/preview/...`
   - Add leading slash for path-based routes

3. **URL Builders** (`search.routes.ts`):
   - Update `PREVIEW_BASE_URL` from `#preview/browse/search` to `/preview/browse/search`
   - Update all build functions

4. **Backward Compatibility**:
   - Add redirects from old hash URLs to new path URLs
   - Handle direct navigation to old bookmarks

### Backend Changes (REQUIRED)

**CRITICAL**: Path-based routing requires server-side support.

1. **SPA Fallback Filter**:
   - Create new servlet filter in `nexus-rapture` module
   - Intercept requests to `/preview/*` paths
   - Forward to `/index.html` for SPA handling
   - Must NOT intercept: `/service/*`, `/static/*`, `/repository/*`

2. **Example Implementation**:
```java
@WebFilter(urlPatterns = "/preview/*")
public class PreviewUiFilter implements Filter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
        throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest) req;
        // Forward to index.html for SPA routing
        httpReq.getRequestDispatcher("/index.html").forward(req, res);
    }
}
```

3. **Location**: `public/common/components/nexus-rapture/src/main/java/org/sonatype/nexus/rapture/internal/`

## Migration Strategy

### Phase 1: Backend Filter (Prerequisite)
- Deploy SPA fallback filter
- Verify `/preview/*` paths return index.html
- No frontend changes yet

### Phase 2: Frontend Migration
- Switch Preview routes to path-based
- Add backward compatibility redirects
- Update tests

### Phase 3: Cleanup
- Remove hash enforcement code for Preview routes
- Update documentation
- Monitor for issues

## Testing Requirements

1. **Unit Tests**:
   - Route definitions resolve correctly
   - URL builders produce correct paths
   - Backward compatibility redirects work

2. **Integration Tests**:
   - Direct navigation to path URLs works
   - Browser back/forward works
   - Bookmarks redirect correctly

3. **E2E Tests**:
   - Full user flows work with new URLs
   - No broken links

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Backend filter not deployed | 404 on page refresh | Feature flag, deploy filter first |
| Broken bookmarks | User frustration | Redirect from hash to path URLs |
| ExtJS interference | Route conflicts | Keep ExtJS routes hash-based |
| SEO impact | Duplicate content | Use canonical URLs |

## Timeline

- Phase 1: Backend filter - **Requires backend team coordination**
- Phase 2: Frontend migration - 1-2 days after Phase 1
- Phase 3: Cleanup - 0.5 days

## Dependencies

- Backend team for servlet filter implementation
- QA for comprehensive testing
- DevOps for deployment coordination
