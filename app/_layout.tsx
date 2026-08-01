import 'react-native-gesture-handler';

import { Stack } from 'expo-router';
import { StatusBar } from 'react-native';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { colors } from '../src/constants/colors';
import { SearchTargetsProvider } from '../src/context/search-targets-context';
import { StopappProvider } from '../src/context/stopapp-context';

export default function RootLayout() {
  return (
    <GestureHandlerRootView style={{ flex: 1, backgroundColor: colors.canvas }}>
      <SafeAreaProvider>
        <StopappProvider>
          <SearchTargetsProvider>
            <StatusBar barStyle="dark-content" backgroundColor={colors.canvas} />
            <Stack
              screenOptions={{
                headerShown: false,
                animation: 'slide_from_right',
                contentStyle: { backgroundColor: colors.canvas },
              }}
            />
          </SearchTargetsProvider>
        </StopappProvider>
      </SafeAreaProvider>
    </GestureHandlerRootView>
  );
}
