/**
 * Stats Tab (StatsScreen.kt port)
 * Coming soon placeholder screen.
 */
import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { Colors } from '@/constants/theme';

export default function StatsScreen() {
  return (
    <View style={styles.root}>
      <Text style={styles.emoji}>📊</Text>
      <Text style={styles.title}>STATS</Text>
      <Text style={styles.subtitle}>Coming soon</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: Colors.background,
    justifyContent: 'center',
    alignItems: 'center',
    gap: 12,
  },
  emoji: {
    fontSize: 48,
  },
  title: {
    fontSize: 12, fontWeight: '800',
    color: Colors.primary,
    letterSpacing: 4,
  },
  subtitle: {
    fontSize: 13,
    color: Colors.outline,
  },
});
