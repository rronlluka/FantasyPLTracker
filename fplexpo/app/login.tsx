/**
 * Login Screen
 * Faithful RN port of LoginScreen.kt
 */
import React, { useState, useEffect, useRef } from 'react';
import {
  View, Text, TextInput, TouchableOpacity, ScrollView,
  StyleSheet, Animated, KeyboardAvoidingView, Platform,
  ActivityIndicator,
} from 'react-native';
import { useRouter } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Storage } from '@/utils/storage';
import { Api, getBackendUrl, setBackendUrl } from '@/services/api';
import { Colors, Radius, Spacing } from '@/constants/theme';

export default function LoginScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();

  const [managerIdText, setManagerIdText] = useState('');
  const [showError, setShowError] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [showHelp, setShowHelp] = useState(false);
  const [backendUrlText, setBackendUrlText] = useState(getBackendUrl());
  const [backendStatusMsg, setBackendStatusMsg] = useState<string | null>(null);
  const [isCheckingBackend, setIsCheckingBackend] = useState(false);

  // Pulse animation for glow blob
  const glowAnim = useRef(new Animated.Value(0.18)).current;
  useEffect(() => {
    Animated.loop(
      Animated.sequence([
        Animated.timing(glowAnim, { toValue: 0.32, duration: 2800, useNativeDriver: true }),
        Animated.timing(glowAnim, { toValue: 0.18, duration: 2800, useNativeDriver: true }),
      ]),
    ).start();
  }, []);

  // Auto-navigate if already logged in
  useEffect(() => {
    Storage.getManagerId().then((id) => {
      if (id) router.replace('/(tabs)/leagues');
    });
    Storage.getBackendUrl().then((url) => {
      if (url) {
        setBackendUrl(url);
        setBackendUrlText(url);
      }
    });
  }, []);

  const handleGetStarted = async () => {
    const id = parseInt(managerIdText.trim(), 10);
    if (isNaN(id) || id <= 0) {
      setShowError(true);
      setErrorMessage('Please enter a valid Manager ID');
      return;
    }
    await Storage.saveManagerId(id);
    router.replace('/(tabs)/leagues');
  };

  const handleCheckBackend = async () => {
    setIsCheckingBackend(true);
    try {
      const health = await Api.getHealth();
      setBackendStatusMsg(
        health.ok
          ? `Backend healthy · uptime ${health.uptime_seconds ?? 0}s`
          : 'Unhealthy response from backend',
      );
    } catch (e: any) {
      setBackendStatusMsg(`Health check failed: ${e.message}`);
    } finally {
      setIsCheckingBackend(false);
    }
  };

  const handleSaveBackendUrl = async () => {
    setBackendUrl(backendUrlText);
    await Storage.saveBackendUrl(backendUrlText);
    setBackendStatusMsg('Saved backend URL');
  };

  return (
    <KeyboardAvoidingView
      style={styles.root}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      {/* Background glow blob */}
      <Animated.View
        style={[styles.glowBlob, { opacity: glowAnim }]}
        pointerEvents="none"
      />

      <ScrollView
        contentContainerStyle={[styles.scroll, { paddingTop: insets.top + 16 }]}
        keyboardShouldPersistTaps="handled"
        showsVerticalScrollIndicator={false}
      >
        {/* ── HERO ──────────────────────────────────────────────── */}
        <View style={styles.heroSection}>
          {/* Live badge pill */}
          <View style={styles.badgePill}>
            <View style={styles.liveDot} />
            <Text style={styles.badgeText}>LIVE · 2024/25</Text>
          </View>

          <View style={{ height: 20 }} />

          <Text style={styles.wordmarkFPL}>FPL</Text>
          <Text style={styles.wordmarkLive}>LIVE{'\n'}TRACKER</Text>

          <View style={{ height: 16 }} />

          <Text style={styles.subtitle}>
            Real-time insights for your{'\n'}Fantasy Premier League squad.
          </Text>
        </View>

        {/* ── STAT CHIPS ────────────────────────────────────────── */}
        <View style={styles.chipRow}>
          {['Live Rank', 'Best XI', 'Transfers'].map((label) => (
            <StatChip key={label} label={label} />
          ))}
        </View>

        {/* ── SIGN-IN CARD ──────────────────────────────────────── */}
        <View style={styles.card}>
          <Text style={styles.cardTitle}>Sign in</Text>
          <Text style={styles.cardSubtitle}>
            Enter your FPL Manager ID to get started
          </Text>

          {/* Manager ID input */}
          <TextInput
            style={styles.input}
            placeholder="e.g. 685991"
            placeholderTextColor={Colors.outline}
            keyboardType="numeric"
            value={managerIdText}
            onChangeText={(t) => { setManagerIdText(t); setShowError(false); }}
            returnKeyType="go"
            onSubmitEditing={handleGetStarted}
            selectionColor={Colors.primary}
          />

          {/* How to find ID toggle */}
          <TouchableOpacity
            onPress={() => setShowHelp((v) => !v)}
            style={styles.helpToggle}
          >
            <Text style={styles.helpToggleText}>
              {showHelp ? 'Hide instructions ▲' : 'How do I find my Manager ID? ▼'}
            </Text>
          </TouchableOpacity>

          {showHelp && (
            <View style={styles.helpBox}>
              <Text style={styles.helpTitle}>Finding your Manager ID</Text>
              {[
                'Go to fantasy.premierleague.com',
                'Open the Points or Transfers page',
                'Look at the URL in your browser',
                'The entry number in the URL is your ID',
              ].map((step, i) => (
                <Text key={i} style={styles.helpStep}>{i + 1}. {step}</Text>
              ))}
              <View style={styles.urlBox}>
                <Text style={styles.urlGray}>fantasy.premierleague.com/entry/</Text>
                <Text style={styles.urlHighlight}>685991</Text>
                <Text style={styles.urlGray}>/event/12</Text>
              </View>
              <Text style={styles.helpNote}>↑ 685991 is your Manager ID</Text>
            </View>
          )}

          {showError && (
            <Text style={styles.errorText}>{errorMessage}</Text>
          )}

          <View style={{ height: 8 }} />

          {/* CTA Button */}
          <TouchableOpacity style={styles.ctaButton} onPress={handleGetStarted} activeOpacity={0.85}>
            <Text style={styles.ctaText}>GET STARTED  →</Text>
          </TouchableOpacity>

          <Text style={styles.footerNote}>No account needed · free to use</Text>

          {/* ── DEBUG BACKEND SECTION ──────────────────────────── */}
          <View style={styles.divider} />
          <Text style={styles.debugLabel}>Debug · Backend</Text>

          <TextInput
            style={[styles.input, { marginBottom: 8 }]}
            placeholder="http://127.0.0.1:3000/api/"
            placeholderTextColor={Colors.outline}
            value={backendUrlText}
            onChangeText={setBackendUrlText}
            autoCapitalize="none"
            autoCorrect={false}
            selectionColor={Colors.primary}
          />

          <View style={styles.debugRow}>
            <TouchableOpacity
              style={[styles.debugBtn, styles.debugBtnPrimary]}
              onPress={handleSaveBackendUrl}
            >
              <Text style={styles.debugBtnPrimaryText}>Save URL</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[styles.debugBtn, styles.debugBtnOutline]}
              onPress={() => {
                const def = 'http://127.0.0.1:3000/api/';
                setBackendUrlText(def);
                setBackendUrl(def);
                Storage.saveBackendUrl(def);
                setBackendStatusMsg('Reset to default URL');
              }}
            >
              <Text style={styles.debugBtnOutlineText}>Reset</Text>
            </TouchableOpacity>
          </View>

          <TouchableOpacity
            style={[styles.debugBtn, styles.debugBtnOutline, { marginTop: 8, width: '100%' }]}
            onPress={handleCheckBackend}
            disabled={isCheckingBackend}
          >
            {isCheckingBackend
              ? <ActivityIndicator color={Colors.outline} size="small" />
              : <Text style={styles.debugBtnOutlineText}>Check Backend Health</Text>}
          </TouchableOpacity>

          {backendStatusMsg && (
            <Text style={styles.backendStatus}>{backendStatusMsg}</Text>
          )}
        </View>

        <View style={{ height: 40 }} />
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

function StatChip({ label }: { label: string }) {
  return (
    <View style={statChipStyles.chip}>
      <View style={statChipStyles.dot} />
      <Text style={statChipStyles.label}>{label}</Text>
    </View>
  );
}

const statChipStyles = StyleSheet.create({
  chip: {
    flex: 1,
    backgroundColor: Colors.surface,
    borderRadius: Radius.md,
    paddingVertical: 14,
    paddingHorizontal: 10,
    alignItems: 'center',
    gap: 4,
  },
  dot: {
    width: 8, height: 8,
    borderRadius: 4,
    backgroundColor: Colors.primary,
  },
  label: {
    fontSize: 11, fontWeight: '600',
    color: Colors.onSurfaceVariant,
    textAlign: 'center',
  },
});

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  glowBlob: {
    position: 'absolute',
    width: 320, height: 320,
    borderRadius: 160,
    backgroundColor: Colors.primary,
    top: 40, left: -60,
  },
  scroll: {
    paddingHorizontal: 24,
  },
  heroSection: {
    paddingTop: 36,
    paddingBottom: 8,
  },
  badgePill: {
    flexDirection: 'row',
    alignItems: 'center',
    alignSelf: 'flex-start',
    backgroundColor: Colors.primaryContainer + '99',
    borderRadius: Radius.full,
    paddingHorizontal: 12,
    paddingVertical: 5,
    gap: 6,
  },
  liveDot: {
    width: 7, height: 7,
    borderRadius: 3.5,
    backgroundColor: Colors.primary,
  },
  badgeText: {
    fontSize: 11, fontWeight: '700',
    color: Colors.primary,
    letterSpacing: 1,
  },
  wordmarkFPL: {
    fontSize: 52, fontWeight: '900',
    color: '#FFFFFF',
    lineHeight: 52, letterSpacing: -1,
  },
  wordmarkLive: {
    fontSize: 52, fontWeight: '900',
    color: Colors.primary,
    lineHeight: 52, letterSpacing: -1,
  },
  subtitle: {
    fontSize: 16, color: 'rgba(255,255,255,0.55)',
    lineHeight: 24,
  },
  chipRow: {
    flexDirection: 'row',
    gap: 10,
    marginVertical: 20,
  },
  card: {
    backgroundColor: Colors.surface,
    borderRadius: 20,
    padding: 24,
  },
  cardTitle: {
    fontSize: 22, fontWeight: '700',
    color: '#FFFFFF',
  },
  cardSubtitle: {
    fontSize: 13,
    color: 'rgba(255,255,255,0.45)',
    marginTop: 4, marginBottom: 20,
  },
  input: {
    borderWidth: 1.5,
    borderColor: Colors.outlineVariant,
    borderRadius: Radius.md,
    padding: 14,
    color: '#FFFFFF',
    fontSize: 15,
    backgroundColor: Colors.surfaceContainer,
    marginBottom: 4,
  },
  helpToggle: {
    paddingVertical: 8,
  },
  helpToggleText: {
    fontSize: 12, fontWeight: '600',
    color: Colors.primary,
  },
  helpBox: {
    backgroundColor: Colors.surfaceHigh,
    borderRadius: Radius.md,
    padding: 14,
    marginBottom: 12,
  },
  helpTitle: {
    fontSize: 13, fontWeight: '700',
    color: Colors.primary,
    marginBottom: 8,
  },
  helpStep: {
    fontSize: 12,
    color: 'rgba(255,255,255,0.75)',
    marginBottom: 3,
  },
  urlBox: {
    backgroundColor: 'rgba(19,19,19,0.7)',
    borderRadius: Radius.sm,
    padding: 10,
    marginTop: 8,
  },
  urlGray: {
    fontSize: 11,
    color: 'rgba(255,255,255,0.5)',
    fontFamily: Platform.OS === 'ios' ? 'Courier' : 'monospace',
  },
  urlHighlight: {
    fontSize: 15, fontWeight: '700',
    color: Colors.secondary,
    fontFamily: Platform.OS === 'ios' ? 'Courier' : 'monospace',
  },
  helpNote: {
    fontSize: 11,
    color: 'rgba(255,255,255,0.6)',
    fontStyle: 'italic',
    marginTop: 6,
  },
  errorText: {
    color: '#FF6B6B',
    fontSize: 12,
    marginBottom: 6,
  },
  ctaButton: {
    backgroundColor: Colors.primary,
    borderRadius: Radius.md,
    height: 52,
    justifyContent: 'center',
    alignItems: 'center',
  },
  ctaText: {
    fontSize: 15, fontWeight: '900',
    color: Colors.onPrimary,
    letterSpacing: 1,
  },
  footerNote: {
    fontSize: 11,
    color: 'rgba(255,255,255,0.3)',
    textAlign: 'center',
    marginTop: 10,
  },
  divider: {
    height: 1,
    backgroundColor: 'rgba(255,255,255,0.08)',
    marginVertical: 16,
  },
  debugLabel: {
    fontSize: 12, fontWeight: '700',
    color: Colors.primary + 'B3',
    letterSpacing: 0.5,
    marginBottom: 8,
  },
  debugRow: {
    flexDirection: 'row',
    gap: 8,
  },
  debugBtn: {
    flex: 1,
    borderRadius: Radius.sm,
    padding: 10,
    alignItems: 'center',
    justifyContent: 'center',
  },
  debugBtnPrimary: {
    backgroundColor: Colors.primaryContainer,
  },
  debugBtnPrimaryText: {
    fontSize: 12,
    color: Colors.primary,
    fontWeight: '600',
  },
  debugBtnOutline: {
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.15)',
  },
  debugBtnOutlineText: {
    fontSize: 12,
    color: 'rgba(255,255,255,0.6)',
  },
  backendStatus: {
    fontSize: 11,
    color: Colors.primary,
    marginTop: 6,
  },
});
