# Agent 3: Docker Search Implementation

## Your Assignment
Build the Docker search UI for Preview UI (`#preview/browse/search/docker`).

## Worktree Path
```
/Users/mitchellsjohnson/.cursor/worktrees/nexus-internal__Workspace_/sgi
```

## Your Directory
```
/Users/mitchellsjohnson/.cursor/worktrees/nexus-internal__Workspace_/sgi/plugins/nexus-coreui-plugin/src/frontend/src/search/docker/
```

## Reference Implementation
Copy the pattern from Maven search:
```
/Users/mitchellsjohnson/.cursor/worktrees/nexus-internal__Workspace_/sgi/plugins/nexus-coreui-plugin/src/frontend/src/search/results/
```

## Files to Create

| File | Purpose |
|------|---------|
| `index.ts` | Public exports |
| `DockerSearchPage.tsx` | Main search page (copy GASearchPage pattern) |
| `DockerSearchFilters.tsx` | Docker-specific filters |
| `DockerSearchResults.tsx` | Results table |
| `DockerDetailPage.tsx` | Image detail view |
| `useDockerSearch.ts` | Search state hook |
| `docker.types.ts` | Docker-specific types |

## Docker-Specific Filters

```typescript
interface DockerSearchFilters {
  imageName?: string;   // Image name (e.g., nginx, ubuntu)
  tag?: string;         // Image tag (e.g., latest, 1.0.0, alpine)
  digest?: string;      // SHA256 digest
}
```

## Docker Result Fields

```typescript
interface DockerResult {
  id: string;           // Unique ID
  imageName: string;    // Full image name (registry/name)
  displayName: string;  // Short display name
  latestTag: string;    // Most recent tag
  tagsCount: number;    // Number of tags
  size?: string;        // Image size (human readable)
  lastUpdated: string;
}
```

## Route to Register

After completing, tell Agent 0 to add this route to `previewBrowseRoutes.js`:
```javascript
{
  name: 'preview.browse.search.docker',
  url: '/docker/:keyword',
  component: DockerSearchPage,
  params: {keyword: {value: null, raw: true, dynamic: true}},
  data: {title: 'Docker Search'},
}
```

## Acceptance Criteria

- [ ] Search by imageName, tag, digest
- [ ] Results show: displayName, latestTag, tagsCount, size
- [ ] Click row navigates to detail page
- [ ] URL reflects search state (bookmarkable)
- [ ] Use mock data initially (Agent 0 will connect API later)

## When Done

1. Update TASK_PLAN.md with your completion status
2. Notify Agent 0 that Docker search is ready for route registration


