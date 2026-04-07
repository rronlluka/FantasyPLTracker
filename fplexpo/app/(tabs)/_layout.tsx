/**
 * Bottom Tab Navigator
 * Matches the StitchBottomNav in MainAppScreen.kt
 * Tabs: Leagues | My Team | Stats | Matches
 */
import React from 'react';
import { Tabs, useRouter } from 'expo-router';
import {
  View, Text, TouchableOpacity, StyleSheet,
  Platform,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Colors, Radius } from '@/constants/theme';

// Simple SVG-free icons using unicode / emoji characters
function TabIcon({ name, color, focused }: { name: string; color: string; focused: boolean }) {
  const icons: Record<string, string> = {
    leagues: '🏆',
    'my-team': '👤',
    stats:   '📊',
    matches: '⚽',
  };
  return (
    <Text style={{ fontSize: focused ? 22 : 20, opacity: focused ? 1 : 0.6 }}>
      {icons[name] ?? '●'}
    </Text>
  );
}

export default function TabLayout() {
  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarStyle: styles.tabBar,
        tabBarActiveTintColor: Colors.onPrimary,
        tabBarInactiveTintColor: Colors.outline,
        tabBarLabelStyle: styles.tabLabel,
        tabBarItemStyle: styles.tabItem,
        tabBarBackground: () => <View style={styles.tabBarBg} />,
      }}
    >
      <Tabs.Screen
        name="leagues"
        options={{
          title: 'LEAGUES',
          tabBarIcon: ({ focused }) => (
            <TabIcon name="leagues" color={focused ? Colors.onPrimary : Colors.outline} focused={focused} />
          ),
          tabBarActiveTintColor: Colors.onPrimary,
          tabBarActiveBackgroundColor: 'transparent',
        }}
      />
      <Tabs.Screen
        name="my-team"
        options={{
          title: 'MY TEAM',
          tabBarIcon: ({ focused }) => (
            <TabIcon name="my-team" color={focused ? Colors.onPrimary : Colors.outline} focused={focused} />
          ),
        }}
      />
      <Tabs.Screen
        name="stats"
        options={{
          title: 'STATS',
          tabBarIcon: ({ focused }) => (
            <TabIcon name="stats" color={focused ? Colors.onPrimary : Colors.outline} focused={focused} />
          ),
        }}
      />
      <Tabs.Screen
        name="matches"
        options={{
          title: 'MATCHES',
          tabBarIcon: ({ focused }) => (
            <TabIcon name="matches" color={focused ? Colors.onPrimary : Colors.outline} focused={focused} />
          ),
        }}
      />
      {/* Hide legacy placeholder files from the tab bar */}
      <Tabs.Screen name="index"   options={{ href: null }} />
      <Tabs.Screen name="explore" options={{ href: null }} />
    </Tabs>
  );
}

const styles = StyleSheet.create({
  tabBar: {
    backgroundColor: Colors.background,
    borderTopWidth: 0,
    elevation: 0,
    height: Platform.OS === 'ios' ? 85 : 65,
    paddingBottom: Platform.OS === 'ios' ? 24 : 8,
    paddingTop: 8,
  },
  tabBarBg: {
    flex: 1,
    backgroundColor: Colors.background,
    borderTopWidth: 1,
    borderTopColor: Colors.outlineVariant + '4D',
  },
  tabLabel: {
    fontSize: 9,
    fontWeight: '700',
    letterSpacing: 1,
    marginTop: 2,
  },
  tabItem: {
    borderRadius: Radius.md,
    marginHorizontal: 4,
    marginVertical: 4,
  },
});
