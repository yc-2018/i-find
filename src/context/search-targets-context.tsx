import AsyncStorage from '@react-native-async-storage/async-storage';
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type PropsWithChildren,
} from 'react';

import { builtinTargetDefaultsById, defaultSearchTargets } from '../constants/search-targets';
import type {
  SearchLaunchMode,
  SearchTarget,
  SearchTargetIconMode,
} from '../types/search-targets';
import { deleteStoredIconAsync } from '../utils/icon-storage';
import {
  createTargetId,
  normalizeEditableTarget,
  normalizeSortOrder,
  sortTargetsBySortOrder,
} from '../utils/search-targets';

const storageKey = 'ifind-search-targets';

export type EditableSearchTarget = {
  name: string;
  launchMode: SearchLaunchMode;
  schemeTemplate?: string;
  webFallbackTemplate?: string;
  androidPackageName?: string;
  iconMode: SearchTargetIconMode;
  iconValue: string;
};

type SearchTargetsContextValue = {
  isReady: boolean;
  targets: SearchTarget[];
  visibleTargets: SearchTarget[];
  deleteTarget: (id: string) => void;
  reorderTargets: (targets: SearchTarget[]) => void;
  restoreDefaults: () => void;
  saveTarget: (draft: EditableSearchTarget, existingId?: string | null) => Promise<void>;
  setTargetHidden: (id: string, hidden: boolean) => void;
};

const SearchTargetsContext = createContext<SearchTargetsContextValue | null>(null);

function cloneDefaultTargets() {
  return defaultSearchTargets.map((target) => ({ ...target }));
}

function backfillBuiltinMetadata(target: SearchTarget) {
  const builtinTarget = builtinTargetDefaultsById[target.id];

  if (!builtinTarget) {
    return target;
  }

  if (target.androidPackageName || !builtinTarget.androidPackageName) {
    return target;
  }

  return {
    ...target,
    androidPackageName: builtinTarget.androidPackageName,
  };
}

async function persistTargets(targets: SearchTarget[]) {
  await AsyncStorage.setItem(storageKey, JSON.stringify(targets));
}

async function loadTargets() {
  const storedTargets = await AsyncStorage.getItem(storageKey);

  if (!storedTargets) {
    return cloneDefaultTargets();
  }

  try {
    const parsedTargets = JSON.parse(storedTargets) as SearchTarget[];

    if (!Array.isArray(parsedTargets)) {
      return cloneDefaultTargets();
    }

    return normalizeSortOrder(
      sortTargetsBySortOrder(parsedTargets.map(backfillBuiltinMetadata))
    );
  } catch {
    return cloneDefaultTargets();
  }
}

function collectGalleryUris(targets: SearchTarget[]) {
  return targets
    .filter((target) => target.iconMode === 'gallery')
    .map((target) => target.iconValue)
    .filter(Boolean);
}

export function SearchTargetsProvider({ children }: PropsWithChildren) {
  const [targets, setTargets] = useState<SearchTarget[]>(() => cloneDefaultTargets());
  const [isReady, setIsReady] = useState(false);

  useEffect(() => {
    let active = true;

    const hydrateTargets = async () => {
      const loadedTargets = await loadTargets();

      if (!active) {
        return;
      }

      setTargets(loadedTargets);
      setIsReady(true);
    };

    void hydrateTargets();

    return () => {
      active = false;
    };
  }, []);

  const updateTargets = useCallback((nextTargets: SearchTarget[]) => {
    const normalizedTargets = normalizeSortOrder(nextTargets);
    setTargets(normalizedTargets);
    void persistTargets(normalizedTargets);
    return normalizedTargets;
  }, []);

  const saveTarget = useCallback(
    async (draft: EditableSearchTarget, existingId?: string | null) => {
      const normalizedDraft = normalizeEditableTarget(draft);

      setTargets((previousTargets) => {
        const nextTargets = [...previousTargets];

        if (existingId) {
          const currentTarget = previousTargets.find((target) => target.id === existingId);
          const targetIndex = previousTargets.findIndex((target) => target.id === existingId);

          if (!currentTarget || targetIndex < 0) {
            return previousTargets;
          }

          if (
            currentTarget.iconMode === 'gallery' &&
            currentTarget.iconValue &&
            currentTarget.iconValue !== normalizedDraft.iconValue
          ) {
            void deleteStoredIconAsync(currentTarget.iconValue);
          }

          nextTargets[targetIndex] = {
            ...currentTarget,
            ...normalizedDraft,
          };
        } else {
          nextTargets.push({
            id: createTargetId(),
            hidden: false,
            sortOrder: previousTargets.length,
            ...normalizedDraft,
          });
        }

        const normalizedTargets = normalizeSortOrder(nextTargets);
        void persistTargets(normalizedTargets);
        return normalizedTargets;
      });
    },
    []
  );

  const deleteTarget = useCallback((id: string) => {
    setTargets((previousTargets) => {
      const removedTarget = previousTargets.find((target) => target.id === id);

      if (!removedTarget) {
        return previousTargets;
      }

      if (removedTarget.iconMode === 'gallery') {
        void deleteStoredIconAsync(removedTarget.iconValue);
      }

      const nextTargets = previousTargets.filter((target) => target.id !== id);
      const normalizedTargets = normalizeSortOrder(nextTargets);
      void persistTargets(normalizedTargets);
      return normalizedTargets;
    });
  }, []);

  const setTargetHidden = useCallback((id: string, hidden: boolean) => {
    setTargets((previousTargets) => {
      const nextTargets = previousTargets.map((target) =>
        target.id === id ? { ...target, hidden } : target
      );
      const normalizedTargets = normalizeSortOrder(nextTargets);
      void persistTargets(normalizedTargets);
      return normalizedTargets;
    });
  }, []);

  const reorderTargets = useCallback(
    (nextTargets: SearchTarget[]) => {
      updateTargets(nextTargets);
    },
    [updateTargets]
  );

  const restoreDefaults = useCallback(() => {
    const galleryUris = collectGalleryUris(targets);
    galleryUris.forEach((uri) => {
      void deleteStoredIconAsync(uri);
    });

    updateTargets(cloneDefaultTargets());
  }, [targets, updateTargets]);

  const visibleTargets = useMemo(() => targets.filter((target) => !target.hidden), [targets]);

  const value = useMemo<SearchTargetsContextValue>(
    () => ({
      isReady,
      targets,
      visibleTargets,
      deleteTarget,
      reorderTargets,
      restoreDefaults,
      saveTarget,
      setTargetHidden,
    }),
    [
      isReady,
      targets,
      visibleTargets,
      deleteTarget,
      reorderTargets,
      restoreDefaults,
      saveTarget,
      setTargetHidden,
    ]
  );

  return (
    <SearchTargetsContext.Provider value={value}>{children}</SearchTargetsContext.Provider>
  );
}

export function useSearchTargets() {
  const context = useContext(SearchTargetsContext);

  if (!context) {
    throw new Error('useSearchTargets must be used within SearchTargetsProvider');
  }

  return context;
}
