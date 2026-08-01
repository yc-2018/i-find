export const SHIZUKU_PACKAGE_NAME = 'moe.shizuku.privileged.api';
export const STOPAPP_PACKAGE_NAME = SHIZUKU_PACKAGE_NAME;

export type StopappAvailability =
  | 'not_installed'
  | 'prerequisites_missing'
  | 'ready'
  | 'unavailable';

export type DefrostBackendMode = 'adb' | 'root' | 'unknown' | 'unavailable';

export type StopappStatus = {
  packageName: string;
  availability: StopappAvailability;
  nativeModuleAvailable: boolean;
  installed: boolean;
  launchable: boolean;
  versionName: string | null;
  versionCode: number | null;
  publicDefrostSupported: boolean;
  permissionGranted: boolean;
  serviceRunning: boolean;
  backendMode: DefrostBackendMode;
  reason: string;
  message: string;
  requiredPrerequisites: string[];
  exportedActivityCount: number;
  exportedServiceCount: number;
  exportedReceiverCount: number;
  exportedProviderCount: number;
};

export type StopappDefrostResult = {
  attempted: boolean;
  supported: boolean;
  success: boolean;
  code: string;
  message: string;
  targetPackageName: string;
  attemptedCommands: string[];
  successfulCommands: string[];
  stdout: string;
  stderr: string;
};

export type StopappOpenResult = {
  opened: boolean;
  code: string;
  message: string;
};

export type StopappPermissionResult = {
  granted: boolean;
  code: string;
  message: string;
  shouldShowRequestPermissionRationale: boolean;
};

export function createUnavailableStopappStatus(
  reason: string,
  message: string
): StopappStatus {
  return {
    packageName: SHIZUKU_PACKAGE_NAME,
    availability: 'unavailable',
    nativeModuleAvailable: false,
    installed: false,
    launchable: false,
    versionName: null,
    versionCode: null,
    publicDefrostSupported: false,
    permissionGranted: false,
    serviceRunning: false,
    backendMode: 'unavailable',
    reason,
    message,
    requiredPrerequisites: [],
    exportedActivityCount: 0,
    exportedServiceCount: 0,
    exportedReceiverCount: 0,
    exportedProviderCount: 0,
  };
}
