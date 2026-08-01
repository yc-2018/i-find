import { Platform } from 'react-native';
import { requireOptionalNativeModule } from 'expo-modules-core';

import {
  createUnavailableStopappStatus,
  SHIZUKU_PACKAGE_NAME,
  type StopappAvailability,
  type StopappDefrostResult,
  type StopappOpenResult,
  type StopappPermissionResult,
  type StopappStatus,
} from './StopappAssist.types';

type StopappAssistNativeModule = {
  getStatusAsync(): Promise<StopappStatus>;
  attemptDefrostAsync(targetPackageName: string): Promise<StopappDefrostResult>;
  openManagerAsync(): Promise<StopappOpenResult>;
  requestPermissionAsync(): Promise<StopappPermissionResult>;
};

const nativeModule =
  Platform.OS === 'android'
    ? requireOptionalNativeModule<StopappAssistNativeModule>('StopappAssist')
    : null;

export async function getStatusAsync(): Promise<StopappStatus> {
  if (nativeModule) {
    return nativeModule.getStatusAsync();
  }

  return createUnavailableStopappStatus(
    Platform.OS === 'android' ? 'native_module_unavailable' : 'platform_not_supported',
    Platform.OS === 'android'
      ? '\u5f53\u524d\u8fd0\u884c\u73af\u5883\u672a\u52a0\u8f7d Shizuku \u539f\u751f\u6a21\u5757\u3002'
      : '\u5f53\u524d\u5e73\u53f0\u4e0d\u652f\u6301 Shizuku \u89e3\u51bb\u3002'
  );
}

export async function attemptDefrostAsync(
  targetPackageName: string
): Promise<StopappDefrostResult> {
  if (nativeModule) {
    return nativeModule.attemptDefrostAsync(targetPackageName);
  }

  return {
    attempted: false,
    supported: false,
    success: false,
    code: Platform.OS === 'android' ? 'native_module_unavailable' : 'platform_not_supported',
    message:
      Platform.OS === 'android'
        ? '\u5f53\u524d\u8fd0\u884c\u73af\u5883\u65e0\u6cd5\u6267\u884c Shizuku \u89e3\u51bb\u3002'
        : '\u5f53\u524d\u5e73\u53f0\u4e0d\u652f\u6301 Shizuku \u89e3\u51bb\u3002',
    targetPackageName,
    attemptedCommands: [],
    successfulCommands: [],
    stdout: '',
    stderr: '',
  };
}

export async function openManagerAsync(): Promise<StopappOpenResult> {
  if (nativeModule) {
    return nativeModule.openManagerAsync();
  }

  return {
    opened: false,
    code: Platform.OS === 'android' ? 'native_module_unavailable' : 'platform_not_supported',
    message:
      Platform.OS === 'android'
        ? '\u5f53\u524d\u8fd0\u884c\u73af\u5883\u672a\u52a0\u8f7d Shizuku \u539f\u751f\u6a21\u5757\u3002'
        : '\u5f53\u524d\u5e73\u53f0\u4e0d\u652f\u6301\u6253\u5f00 Shizuku\u3002',
  };
}

export async function requestPermissionAsync(): Promise<StopappPermissionResult> {
  if (nativeModule) {
    return nativeModule.requestPermissionAsync();
  }

  return {
    granted: false,
    code: Platform.OS === 'android' ? 'native_module_unavailable' : 'platform_not_supported',
    message:
      Platform.OS === 'android'
        ? '\u5f53\u524d\u8fd0\u884c\u73af\u5883\u65e0\u6cd5\u8bf7\u6c42 Shizuku \u6388\u6743\u3002'
        : '\u5f53\u524d\u5e73\u53f0\u4e0d\u652f\u6301 Shizuku \u6388\u6743\u3002',
    shouldShowRequestPermissionRationale: false,
  };
}

export {
  SHIZUKU_PACKAGE_NAME,
  createUnavailableStopappStatus,
  type StopappAvailability,
  type StopappDefrostResult,
  type StopappOpenResult,
  type StopappPermissionResult,
  type StopappStatus,
};
