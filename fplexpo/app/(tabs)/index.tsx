/**
 * Unused legacy placeholder — redirects to leagues tab.
 * This file is kept so the router doesn't show an error.
 */
import { Redirect } from 'expo-router';

export default function LegacyIndex() {
  return <Redirect href="/(tabs)/leagues" />;
}
