import * as FileSystem from 'expo-file-system';

const iconsDirectory = `${FileSystem.documentDirectory ?? ''}target-icons/`;

function ensureDirectoryPath() {
  if (!FileSystem.documentDirectory) {
    throw new Error('当前设备不支持本地文件目录。');
  }

  return iconsDirectory;
}

export async function copyImageToAppStorageAsync(sourceUri: string) {
  const directory = ensureDirectoryPath();
  const directoryInfo = await FileSystem.getInfoAsync(directory);

  if (!directoryInfo.exists) {
    await FileSystem.makeDirectoryAsync(directory, { intermediates: true });
  }

  const extensionMatch = sourceUri.match(/\.[a-zA-Z0-9]+(?:\?|$)/);
  const extension = extensionMatch ? extensionMatch[0].replace(/\?$/, '') : '.jpg';
  const targetUri = `${directory}${Date.now()}${extension}`;

  await FileSystem.copyAsync({ from: sourceUri, to: targetUri });

  return targetUri;
}

export async function deleteStoredIconAsync(uri: string | undefined) {
  if (!uri || !uri.startsWith('file://')) {
    return;
  }

  const info = await FileSystem.getInfoAsync(uri);
  if (info.exists) {
    await FileSystem.deleteAsync(uri, { idempotent: true });
  }
}
