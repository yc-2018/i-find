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
import { AppState, type AppStateStatus } from 'react-native';

import {
  attemptDefrostAsync,
  createUnavailableStopappStatus,
  getStatusAsync,
  openManagerAsync,
  requestPermissionAsync,
  type StopappDefrostResult,
  type StopappOpenResult,
  type StopappPermissionResult,
  type StopappStatus,
} from '../../modules/stopapp-assist/src';

const storageKey = 'ifind-stopapp-settings';

type StopappPreferences = {
  autoDefrostEnabled: boolean;
};

type StopappContextValue = {
  isReady: boolean;
  autoDefrostEnabled: boolean;
  canAutoDefrost: boolean;
  status: StopappStatus;
  attemptDefrost: (targetPackageName: string) => Promise<StopappDefrostResult>;
  openManager: () => Promise<StopappOpenResult>;
  requestPermission: () => Promise<StopappPermissionResult>;
  refreshStatus: () => Promise<void>;
  setAutoDefrostEnabled: (enabled: boolean) => Promise<void>;
};

const defaultPreferences: StopappPreferences = {
  autoDefrostEnabled: false,
};

const initialStatus = createUnavailableStopappStatus(
  'status_pending',
  '\u6b63\u5728\u8bfb\u53d6\u5c0f\u9ed1\u5c4b\u72b6\u6001\u3002'
);

const StopappContext = createContext<StopappContextValue | null>(null);

async function persistPreferences(preferences: StopappPreferences) {
  await AsyncStorage.setItem(storageKey, JSON.stringify(preferences));
}

async function loadPreferences() {
  const storedValue = await AsyncStorage.getItem(storageKey);

  if (!storedValue) {
    return defaultPreferences;
  }

  try {
    const parsedValue = JSON.parse(storedValue) as Partial<StopappPreferences>;
    return {
      autoDefrostEnabled: Boolean(parsedValue.autoDefrostEnabled),
    };
  } catch {
    return defaultPreferences;
  }
}

async function readStatusSafely() {
  try {
    return await getStatusAsync();
  } catch (error) {
    return createUnavailableStopappStatus(
      'native_status_error',
      error instanceof Error ? error.message : 'Unknown native status error.'
    );
  }
}

export function StopappProvider({ children }: PropsWithChildren) {
  const [isReady, setIsReady] = useState(false);
  const [autoDefrostEnabled, setAutoDefrostEnabledState] = useState(false);
  const [status, setStatus] = useState<StopappStatus>(initialStatus);

  const canAutoDefrost = status.availability === 'ready' && status.publicDefrostSupported;

  const refreshStatus = useCallback(async () => {
    const nextStatus = await readStatusSafely();
    setStatus(nextStatus);

    setAutoDefrostEnabledState((currentEnabled) => {
      const nextEnabled =
        nextStatus.availability === 'ready' && nextStatus.publicDefrostSupported
          ? currentEnabled
          : false;

      if (nextEnabled !== currentEnabled) {
        void persistPreferences({ autoDefrostEnabled: nextEnabled });
      }

      return nextEnabled;
    });
  }, []);

  useEffect(() => {
    let active = true;

    const hydrate = async () => {
      const [preferences, nextStatus] = await Promise.all([
        loadPreferences(),
        readStatusSafely(),
      ]);

      if (!active) {
        return;
      }

      const nextAutoDefrostEnabled =
        nextStatus.availability === 'ready' && nextStatus.publicDefrostSupported
          ? preferences.autoDefrostEnabled
          : false;

      setStatus(nextStatus);
      setAutoDefrostEnabledState(nextAutoDefrostEnabled);
      setIsReady(true);

      if (nextAutoDefrostEnabled !== preferences.autoDefrostEnabled) {
        void persistPreferences({ autoDefrostEnabled: nextAutoDefrostEnabled });
      }
    };

    void hydrate();

    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    const subscription = AppState.addEventListener('change', (nextState: AppStateStatus) => {
      if (nextState === 'active') {
        void refreshStatus();
      }
    });

    return () => {
      subscription.remove();
    };
  }, [refreshStatus]);

  const setAutoDefrostEnabled = useCallback(
    async (enabled: boolean) => {
      const nextEnabled = enabled && canAutoDefrost;
      setAutoDefrostEnabledState(nextEnabled);
      await persistPreferences({ autoDefrostEnabled: nextEnabled });
    },
    [canAutoDefrost]
  );

  const openManager = useCallback(async () => {
    const result = await openManagerAsync();
    if (result.opened) {
      void refreshStatus();
    }
    return result;
  }, [refreshStatus]);

  const attemptDefrost = useCallback(async (targetPackageName: string) => {
    return attemptDefrostAsync(targetPackageName);
  }, []);

  const requestPermission = useCallback(async () => {
    const result = await requestPermissionAsync();
    void refreshStatus();
    return result;
  }, [refreshStatus]);

  const value = useMemo<StopappContextValue>(
    () => ({
      isReady,
      autoDefrostEnabled,
      canAutoDefrost,
      status,
      attemptDefrost,
      openManager,
      requestPermission,
      refreshStatus,
      setAutoDefrostEnabled,
    }),
    [
      isReady,
      autoDefrostEnabled,
      canAutoDefrost,
      status,
      attemptDefrost,
      openManager,
      requestPermission,
      refreshStatus,
      setAutoDefrostEnabled,
    ]
  );

  return <StopappContext.Provider value={value}>{children}</StopappContext.Provider>;
}

export function useStopappSettings() {
  const context = useContext(StopappContext);

  if (!context) {
    throw new Error('useStopappSettings must be used within StopappProvider');
  }

  return context;
}
