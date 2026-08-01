import { useEffect, useMemo, useRef, useState } from 'react';
import {
  Alert,
  FlatList,
  InteractionManager,
  Keyboard,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { SafeAreaView, useSafeAreaInsets } from 'react-native-safe-area-context';

import { SearchCard } from '../src/components/SearchCard';
import { colors } from '../src/constants/colors';
import { useSearchTargets } from '../src/context/search-targets-context';
import { useStopappSettings } from '../src/context/stopapp-context';
import { notify } from '../src/utils/notify';
import {
  openExternalUrl,
  openSearchTarget,
  sortTargetsBySortOrder,
} from '../src/utils/search-targets';

const COPY = {
  enterKeywordFirst: '\u8bf7\u5148\u8f93\u5165\u641c\u7d22\u5185\u5bb9',
  tapCardHint: '\u8bf7\u76f4\u63a5\u70b9\u51fb\u5e73\u53f0\u5361\u7247\u5f00\u59cb\u641c\u7d22',
  searchPlaceholder: '\u968f\u60f3\u968f\u641c...',
  emptyReadyTitle: '\u8fd8\u6ca1\u6709\u53ef\u89c1\u7684\u641c\u7d22\u9879',
  emptyReadyBody:
    '\u53bb\u914d\u7f6e\u9875\u65b0\u589e\u6216\u53d6\u6d88\u9690\u85cf\u540e\uff0c\u8fd9\u91cc\u5c31\u4f1a\u663e\u793a\u3002',
  loadingTitle: '\u6b63\u5728\u8bfb\u53d6\u4f60\u7684\u641c\u7d22\u9879...',
  openSettings: '\u6253\u5f00\u914d\u7f6e\u9875',
  fallbackTitle: '\u76ee\u6807 App \u53ef\u80fd\u6ca1\u6253\u5f00',
  fallbackBodyPrefix: '\u6ca1\u6709\u6210\u529f\u62c9\u8d77\u201c',
  fallbackBodySuffix:
    '\u201d\uff0c\u8fd9\u4e2a App \u53ef\u80fd\u4ecd\u7136\u5904\u4e8e\u51bb\u7ed3\u72b6\u6001\u3002\u4f60\u53ef\u4ee5\u5148\u6253\u5f00 Shizuku \u786e\u8ba4\u6388\u6743\u548c\u670d\u52a1\u72b6\u6001\uff0c\u6216\u8005\u76f4\u63a5\u6539\u7528\u7f51\u9875\u641c\u7d22\u3002',
  openShizuku: '\u6253\u5f00 Shizuku',
  continueWeb: '\u7f51\u9875\u641c\u7d22',
  cancel: '\u53d6\u6d88',
  shizukuOpenFailed: '\u6ca1\u6709\u6210\u529f\u6253\u5f00 Shizuku',
  webFallbackOpenFailed: '\u7f51\u9875\u641c\u7d22\u6ca1\u6709\u6210\u529f\u6253\u5f00',
};

export default function HomeScreen() {
  const insets = useSafeAreaInsets();
  const router = useRouter();
  const { isReady, visibleTargets } = useSearchTargets();
  const { attemptDefrost, autoDefrostEnabled, openManager, status: shizukuStatus } =
    useStopappSettings();
  const [keyword, setKeyword] = useState('');
  const [submittingId, setSubmittingId] = useState<string | null>(null);
  const inputRef = useRef<TextInput>(null);

  const cards = useMemo(() => sortTargetsBySortOrder(visibleTargets), [visibleTargets]);

  useEffect(() => {
    let focusTimer: ReturnType<typeof setTimeout> | null = null;

    const focusTask = InteractionManager.runAfterInteractions(() => {
      focusTimer = setTimeout(() => {
        inputRef.current?.focus();
      }, 120);
    });

    return () => {
      focusTask.cancel();
      if (focusTimer) {
        clearTimeout(focusTimer);
      }
    };
  }, []);

  const handleOpenShizuku = async () => {
    const result = await openManager();
    if (!result.opened) {
      notify(COPY.shizukuOpenFailed);
    }
  };

  const handleOpenWebFallback = async (webFallbackUrl: string) => {
    const opened = await openExternalUrl(webFallbackUrl);

    if (!opened) {
      notify(COPY.webFallbackOpenFailed);
    }
  };

  const handleSearch = async (targetId: string) => {
    const target = cards.find((item) => item.id === targetId);
    const trimmedKeyword = keyword.trim();

    if (!target) {
      return;
    }

    if (!trimmedKeyword) {
      notify(COPY.enterKeywordFirst);
      return;
    }

    Keyboard.dismiss();
    setSubmittingId(targetId);

    try {
      if (
        autoDefrostEnabled &&
        target.launchMode === 'schemeFirst' &&
        target.androidPackageName
      ) {
        await attemptDefrost(target.androidPackageName);
      }

      const result = await openSearchTarget(target, trimmedKeyword);

      if (result.opened) {
        return;
      }

      if (result.webFallbackUrl) {
        const shouldOfferStopapp =
          Boolean(target.androidPackageName) &&
          (shizukuStatus.launchable || shizukuStatus.serviceRunning);

        if (shouldOfferStopapp) {
          Alert.alert(
            COPY.fallbackTitle,
            `${COPY.fallbackBodyPrefix}${target.name}${COPY.fallbackBodySuffix}`,
            [
              {
                text: COPY.openShizuku,
                onPress: () => {
                  void handleOpenShizuku();
                },
              },
              {
                text: COPY.continueWeb,
                onPress: () => {
                  void handleOpenWebFallback(result.webFallbackUrl!);
                },
              },
              {
                text: COPY.cancel,
                style: 'cancel',
              },
            ]
          );
          return;
        }

        await handleOpenWebFallback(result.webFallbackUrl);
        return;
      }

      notify(result.message);
    } finally {
      setSubmittingId(null);
    }
  };

  return (
    <SafeAreaView edges={['top', 'bottom']} style={styles.safeArea}>
      <View style={styles.container}>
        <View style={styles.searchShell}>
          <MaterialCommunityIcons color={colors.accent} name="magnify" size={22} />
          <TextInput
            autoCapitalize="none"
            autoCorrect={false}
            autoFocus
            blurOnSubmit={false}
            onChangeText={setKeyword}
            onSubmitEditing={() => notify(COPY.tapCardHint)}
            placeholder={COPY.searchPlaceholder}
            placeholderTextColor={colors.muted}
            ref={inputRef}
            returnKeyType="search"
            selectionColor={colors.accent}
            style={styles.searchInput}
            value={keyword}
          />
        </View>

        <FlatList
          columnWrapperStyle={styles.row}
          contentContainerStyle={styles.listContent}
          data={cards}
          keyExtractor={(item) => item.id}
          keyboardShouldPersistTaps="handled"
          ListEmptyComponent={
            isReady ? (
              <View style={styles.emptyState}>
                <Text style={styles.emptyTitle}>{COPY.emptyReadyTitle}</Text>
                <Text style={styles.emptyBody}>{COPY.emptyReadyBody}</Text>
              </View>
            ) : (
              <View style={styles.emptyState}>
                <Text style={styles.emptyTitle}>{COPY.loadingTitle}</Text>
              </View>
            )
          }
          numColumns={4}
          renderItem={({ item }) => (
            <SearchCard
              disabled={submittingId === item.id}
              onPress={() => void handleSearch(item.id)}
              target={item}
            />
          )}
          showsVerticalScrollIndicator={false}
        />

        <TouchableOpacity
          accessibilityLabel={COPY.openSettings}
          activeOpacity={0.9}
          onPress={() => router.push('/settings')}
          style={[styles.gearButton, { bottom: insets.bottom + 18 }]}
        >
          <MaterialCommunityIcons color={colors.surface} name="cog-outline" size={24} />
        </TouchableOpacity>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: colors.canvas,
  },
  container: {
    flex: 1,
    paddingHorizontal: 16,
    paddingTop: 10,
  },
  searchShell: {
    marginTop: 4,
    flexDirection: 'row',
    alignItems: 'center',
    borderRadius: 999,
    backgroundColor: colors.surface,
    paddingHorizontal: 18,
    paddingVertical: 6,
    shadowColor: colors.shadow,
    shadowOpacity: 0.08,
    shadowRadius: 14,
    shadowOffset: { width: 0, height: 8 },
    elevation: 3,
  },
  searchInput: {
    flex: 1,
    color: colors.text,
    fontSize: 16,
    paddingVertical: 14,
    paddingLeft: 10,
  },
  listContent: {
    paddingTop: 18,
    paddingBottom: 120,
  },
  row: {
    justifyContent: 'space-between',
    marginBottom: 14,
  },
  emptyState: {
    marginTop: 56,
    alignItems: 'center',
    paddingHorizontal: 24,
  },
  emptyTitle: {
    color: colors.text,
    fontSize: 18,
    fontWeight: '700',
    textAlign: 'center',
  },
  emptyBody: {
    marginTop: 6,
    color: colors.subtleText,
    fontSize: 14,
    lineHeight: 21,
    textAlign: 'center',
  },
  gearButton: {
    position: 'absolute',
    right: 18,
    height: 56,
    width: 56,
    borderRadius: 28,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.accent,
    shadowColor: colors.shadow,
    shadowOpacity: 0.18,
    shadowRadius: 20,
    shadowOffset: { width: 0, height: 12 },
    elevation: 8,
  },
});
