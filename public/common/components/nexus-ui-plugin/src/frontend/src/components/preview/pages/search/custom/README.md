# Custom Search - Agent 2 Instructions

**Agent:** 2  
**Task:** Build Custom Search Builder UI for Preview UI  
**Priority:** 🟡 MEDIUM

---

## 📍 Location

**Worktree:**
```
/Users/mitchellsjohnson/.cursor/worktrees/nexus-internal__Workspace_/sgi
```

**Your Directory:**
```
/Users/mitchellsjohnson/.cursor/worktrees/nexus-internal__Workspace_/sgi/plugins/nexus-coreui-plugin/src/frontend/src/search/custom/
```

---

## 📋 Your Files to Create

```
custom/
├── index.ts                # Exports
├── CustomSearchPage.tsx    # Main page component
├── CustomSearchBuilder.tsx # Dynamic filter builder UI
├── CustomSearchResults.tsx # Results display
├── CustomFilterRow.tsx     # Single filter row component
├── useCustomSearch.ts      # React hook for state
├── custom.types.ts         # TypeScript types
├── custom.styles.scss      # Styles (optional)
└── __tests__/
    ├── CustomSearchPage.test.tsx
    ├── CustomSearchBuilder.test.tsx
    └── useCustomSearch.test.ts
```

---

## 🎯 Requirements

### Custom Search Purpose
Allow users to **build their own search queries** with dynamic filters. Think of it like a query builder UI.

### User Can:
1. Add multiple filter criteria
2. Choose field (format, repository, group, name, version, tag, etc.)
3. Choose operator (equals, contains, starts with, etc.)
4. Enter value
5. Add/remove filter rows dynamically
6. Execute search
7. (Future) Save custom searches

---

## 📖 Filter Types

```typescript
interface CustomFilter {
  id: string;              // Unique ID for React key
  field: FilterField;      // What field to filter
  operator: FilterOperator; // How to filter
  value: string;           // Filter value
}

type FilterField = 
  | 'format'
  | 'repository'
  | 'group'
  | 'name'
  | 'version'
  | 'tag'
  | 'keyword';

type FilterOperator =
  | 'equals'
  | 'contains'
  | 'startsWith'
  | 'endsWith';
```

---

## 🎨 UI Design

```
┌─────────────────────────────────────────────────────────────┐
│ Custom Search                                                │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│ ┌─────────┐ ┌──────────┐ ┌──────────────────┐ ┌──────────┐  │
│ │ Format ▼│ │ equals  ▼│ │ maven2           │ │ [X]      │  │
│ └─────────┘ └──────────┘ └──────────────────┘ └──────────┘  │
│                                                              │
│ ┌─────────┐ ┌──────────┐ ┌──────────────────┐ ┌──────────┐  │
│ │ Group  ▼│ │contains ▼│ │ apache           │ │ [X]      │  │
│ └─────────┘ └──────────┘ └──────────────────┘ └──────────┘  │
│                                                              │
│ [+ Add Filter]                                               │
│                                                              │
│ [Search]                                                     │
│                                                              │
├─────────────────────────────────────────────────────────────┤
│ Results                                                      │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ ...                                                      │ │
│ └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔧 Reference Implementation

Look at patterns from:

**Main page:** `/search/results/GASearchPage.tsx`  
**Hook:** `/search/results/useGASearch.ts`  
**Types:** `/search/core/search.types.ts`

---

## 🎨 UI Components

Use **Radix UI** (already installed):
```typescript
import { 
  Box, Card, Flex, Text, TextField, 
  Select, Button, IconButton, Separator 
} from '@radix-ui/themes';
import { Plus, X, Search } from 'lucide-react';
```

### Filter Row Component
```tsx
interface FilterRowProps {
  filter: CustomFilter;
  onUpdate: (id: string, updates: Partial<CustomFilter>) => void;
  onRemove: (id: string) => void;
}

function CustomFilterRow({ filter, onUpdate, onRemove }: FilterRowProps) {
  return (
    <Flex gap="2" align="center">
      <Select.Root value={filter.field} onValueChange={(v) => onUpdate(filter.id, { field: v })}>
        {/* Field options */}
      </Select.Root>
      
      <Select.Root value={filter.operator} onValueChange={(v) => onUpdate(filter.id, { operator: v })}>
        {/* Operator options */}
      </Select.Root>
      
      <TextField.Root 
        value={filter.value}
        onChange={(e) => onUpdate(filter.id, { value: e.target.value })}
      />
      
      <IconButton variant="ghost" onClick={() => onRemove(filter.id)}>
        <X size={16} />
      </IconButton>
    </Flex>
  );
}
```

---

## ⚠️ Rules

1. **ONLY create files in `/search/custom/`**
2. **DO NOT modify** other directories
3. **Import shared types** from `/search/core/`
4. **Update TASK_PLAN.md** when you start and complete each task
5. **Use TypeScript** (.tsx files)

---

## ✅ Done Criteria

- [ ] `CustomSearchPage.tsx` renders
- [ ] Users can add/remove filter rows
- [ ] Users can select field, operator, and enter value
- [ ] Search executes and shows results
- [ ] Unit tests pass
- [ ] TASK_PLAN.md updated with completion status

---

## 📝 When Complete

Update `/search/TASK_PLAN.md`:
```markdown
#### Agent 2 (Custom Search)
- [x] T-P3-2.1: Create `/search/custom/` directory structure
- [x] T-P3-2.2: Create `CustomSearchPage.tsx`
...
```

Then tell Agent 0 so they can wire the route.


