# Generic Search - Agent 1 Instructions

**Agent:** 1  
**Task:** Build Generic Search UI for Preview UI  
**Priority:** 🔴 HIGH

---

## 📍 Location

**Worktree:**
```
/Users/mitchellsjohnson/.cursor/worktrees/nexus-internal__Workspace_/sgi
```

**Your Directory:**
```
/Users/mitchellsjohnson/.cursor/worktrees/nexus-internal__Workspace_/sgi/plugins/nexus-coreui-plugin/src/frontend/src/search/generic/
```

---

## 📋 Your Files to Create

```
generic/
├── index.ts              # Exports
├── GenericSearchPage.tsx # Main page component
├── GenericSearchFilters.tsx
├── GenericSearchResults.tsx
├── GenericDetailPage.tsx # Detail view for any format
├── useGenericSearch.ts   # React hook for state
├── generic.types.ts      # TypeScript types
├── generic.styles.scss   # Styles (optional)
└── __tests__/
    ├── GenericSearchPage.test.tsx
    └── useGenericSearch.test.ts
```

---

## 🎯 Requirements

### Generic Search Purpose
This is the **catch-all search** that works for ANY component format. Users can search across all repositories without specifying a format.

### Search Filters
```typescript
interface GenericSearchFilters {
  q?: string;           // Free text query
  repository?: string;  // Repository name
  format?: string;      // Optional format filter
  group?: string;       // Group/namespace
  name?: string;        // Component name
  version?: string;     // Version
}
```

### Results Display
- Show format badge/icon for each result (npm, maven, docker, etc.)
- Show repository name
- Show namespace/group if available
- Show name and version
- Link to appropriate detail page based on format

---

## 📖 Reference Implementation

Copy patterns from Maven search:

**Main page:** `/search/results/GASearchPage.tsx`  
**Hook:** `/search/results/useGASearch.ts`  
**Types:** `/search/core/search.types.ts`

---

## 🔧 API

Use mock data initially. The real API is:
```
GET /service/rest/v1/search?q={query}&repository={repo}&format={format}
```

### Mock Data Pattern
```typescript
const mockResults = [
  {
    id: 'maven:org.apache.commons:commons-lang3',
    format: 'maven2',
    repository: 'maven-central',
    group: 'org.apache.commons',
    name: 'commons-lang3',
    version: '3.12.0',
  },
  {
    id: 'npm:react',
    format: 'npm',
    repository: 'npm-proxy-v1',
    group: null,
    name: 'react',
    version: '19.1.0',
  },
  {
    id: 'docker:library/nginx',
    format: 'docker',
    repository: 'docker-proxy-v1',
    group: 'library',
    name: 'nginx',
    version: 'latest',
  },
];
```

---

## 🎨 UI Components

Use **Radix UI** (already installed):
```typescript
import { Box, Card, Flex, Text, TextField, Select, Button, Badge } from '@radix-ui/themes';
```

### Format Badges
```tsx
function FormatBadge({ format }: { format: string }) {
  const colors: Record<string, string> = {
    maven2: 'orange',
    npm: 'red',
    nuget: 'blue',
    docker: 'cyan',
    pypi: 'yellow',
  };
  return <Badge color={colors[format] || 'gray'}>{format}</Badge>;
}
```

---

## ⚠️ Rules

1. **ONLY create files in `/search/generic/`**
2. **DO NOT modify** other directories
3. **Import shared types** from `/search/core/`
4. **Update TASK_PLAN.md** when you start and complete each task
5. **Use TypeScript** (.tsx files)

---

## ✅ Done Criteria

- [ ] `GenericSearchPage.tsx` renders with search input and filters
- [ ] Results display with format badges
- [ ] Clicking a result navigates to appropriate detail page
- [ ] Unit tests pass
- [ ] TASK_PLAN.md updated with completion status

---

## 📝 When Complete

Update `/search/TASK_PLAN.md`:
```markdown
#### Agent 1 (Generic Search)
- [x] T-P3-1.1: Create `/search/generic/` directory structure
- [x] T-P3-1.2: Create `GenericSearchPage.tsx`
...
```

Then tell Agent 0 so they can wire the route.


