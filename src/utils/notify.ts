import { Alert, Platform, ToastAndroid } from 'react-native';

export function notify(message: string, title = 'I find') {
  if (Platform.OS === 'android') {
    ToastAndroid.show(message, ToastAndroid.SHORT);
    return;
  }

  Alert.alert(title, message);
}
