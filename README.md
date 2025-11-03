# Fantasy Premier League Tracker

A comprehensive Android app for tracking Fantasy Premier League (FPL) manager stats, league standings, and team formations.

## Features

### 1. Enhanced Initial Screen 🎨
- **Beautiful football-themed UI** with orange gradient background
- **Dual input options**:
  - Enter Team ID with "LET'S GO" button
  - OR enter League ID with "SEARCH" button
- **Professional card-based design**
- **Favorites system** - Auto-navigates to favorite league on launch
- FPL-branded logo and modern typography

### 2. Manager Stats Screen
- View manager's overall stats (points, rank, gameweek performance)
- Display all leagues the manager has joined in a dropdown
- Click any league to view its standings
- View recent gameweek history
- Button to view current team formation

### 3. Enhanced League Standings Screen 📊
- **Professional table layout** with dark theme (#1E1E1E)
- **Comprehensive statistics**:
  - Rank with change indicators (🟢 up, 🔴 down, ⚪ same)
  - Team name and manager name (dual-line display)
  - **In Play**: Number of players currently playing (green highlight)
  - **To Start**: Number of players yet to start (orange highlight)
  - **GW Net**: Gameweek points
  - **Total**: Overall season points
- **User's team highlighted** in blue
- **Favorites system**: Star icon to save/load favorite league
- **Quick league switcher**: Change league icon for instant switching
- **Clickable rows** to view team formations
- Modern FPL purple header (#37003C)

### 4. Manager Formation Screen
- **Beautiful football pitch visualization** - Players displayed on an authentic green pitch with markings
- **Live match indicator** - Players currently playing show a pulsing "LIVE" badge and red border
- Formation layout showing players in their actual positions (GK, DEF, MID, FWD)
- Player cards with team colors/jerseys
- Display live points for each player with multiplier for captain
- **Goals ⚽ and Assists 🅰️** displayed on player cards with count
- **Opponent team** shown below each player (vs for home, @ for away)
- Captain (C) and Vice-Captain (V) badges above player cards
- Color-coded points indicators (green for high scores, red for low)
- Match status differentiation:
  - **LIVE**: Red pulsing badge and border for players currently playing
  - **Finished**: Standard display for completed matches
  - **Not Started**: Standard display for upcoming matches
- Substitutes section with same detailed information
- Gameweek summary header with key stats (GW Points, Total Points, Rank)
- Additional info showing transfers, costs, and active chips

## API Endpoints Used

The app uses the following Fantasy Premier League API endpoints:

1. **bootstrap-static/** - Get all players, teams, and gameweeks
2. **entry/{manager_id}/** - Get manager data
3. **entry/{manager_id}/history/** - Get manager's historical data
4. **entry/{manager_id}/event/{event_id}/picks/** - Get manager's team for a gameweek
5. **event/{event_id}/live/** - Get live gameweek data
6. **leagues-classic/{league_id}/standings/** - Get league standings

## Project Structure

```
com.fpl.tracker/
├── data/
│   ├── api/
│   │   ├── FPLApiService.kt          # Retrofit API interface
│   │   └── RetrofitInstance.kt       # Retrofit singleton
│   ├── models/
│   │   ├── BootstrapData.kt          # Bootstrap data models
│   │   ├── ManagerData.kt            # Manager data models
│   │   ├── ManagerHistory.kt         # Manager history models
│   │   ├── ManagerPicks.kt           # Manager picks models
│   │   ├── LeagueStandings.kt        # League standings models
│   │   ├── LiveGameweek.kt           # Live gameweek models
│   │   └── Fixtures.kt               # Fixtures models
│   ├── repository/
│   │   └── FPLRepository.kt          # Repository layer
│   └── preferences/
│       └── PreferencesManager.kt     # SharedPreferences manager
├── viewmodel/
│   ├── ManagerStatsViewModel.kt      # Manager stats ViewModel
│   ├── LeagueStandingsViewModel.kt   # League standings ViewModel
│   └── ManagerFormationViewModel.kt  # Manager formation ViewModel
├── ui/
│   └── screens/
│       ├── InitialScreen.kt          # Initial/Login screen
│       ├── ManagerStatsScreen.kt     # Manager stats screen
│       ├── LeagueStandingsScreen.kt  # League standings screen
│       └── ManagerFormationScreen.kt # Manager formation screen
├── navigation/
│   └── NavGraph.kt                   # Navigation graph
└── MainActivity.kt                   # Main activity

```

## Technologies Used

- **Kotlin** - Programming language
- **Jetpack Compose** - Modern UI toolkit
- **Retrofit** - HTTP client for API calls
- **Gson** - JSON parsing
- **Navigation Compose** - Navigation between screens
- **ViewModel & StateFlow** - State management
- **Coroutines** - Asynchronous programming
- **Material 3** - Material Design components

## How to Use

### Finding Your Manager ID
1. Go to https://fantasy.premierleague.com/
2. Log in to your account
3. Click on "Points" or "My Team"
4. Look at the URL: `https://fantasy.premierleague.com/entry/{YOUR_ID}/event/{GAMEWEEK}`
5. The number after `/entry/` is your Manager ID

### Finding League ID
1. Go to https://fantasy.premierleague.com/
2. Navigate to "Leagues & Cups"
3. Click on the league you want to track
4. Look at the URL: `https://fantasy.premierleague.com/leagues/{LEAGUE_ID}/standings/c`
5. The number after `/leagues/` is your League ID

### Using the App
1. **First Launch**: Enter your Manager ID and optionally League ID
2. **Subsequent Launches**: App will auto-navigate to your saved manager stats
3. **View Leagues**: Click on any league in the dropdown on Manager Stats screen
4. **View Team**: Click "View Current Team" to see formation and player points
5. **View Other Managers**: Click on any manager in league standings to see their team

## Testing in Postman

You can test the API endpoints before running the app:

### Example Requests

1. **Get Bootstrap Data**
   ```
   GET https://fantasy.premierleague.com/api/bootstrap-static/
   ```

2. **Get Manager Data** (replace {manager_id})
   ```
   GET https://fantasy.premierleague.com/api/entry/123456/
   ```

3. **Get League Standings** (replace {league_id})
   ```
   GET https://fantasy.premierleague.com/api/leagues-classic/123456/standings/
   ```

4. **Get Manager Picks** (replace {manager_id} and {event_id})
   ```
   GET https://fantasy.premierleague.com/api/entry/123456/event/1/picks/
   ```

## Building and Running

1. Open the project in Android Studio
2. Sync Gradle files
3. Run the app on an emulator or physical device (API 28+)
4. Enter your Manager ID and League ID on the first screen

## Notes

- The API endpoints are community-documented and not officially published
- Some endpoints may have rate limits
- Live data updates may have a delay
- Internet connection is required for all features
- Minimum SDK: API 28 (Android 9.0)
- Target SDK: API 36

## Visual Features

### Football Pitch View
The app includes a custom-drawn football pitch with:
- Authentic pitch markings (center circle, penalty areas, goal areas)
- Green pitch background matching real football aesthetics
- Player cards positioned according to their formation
- Team-specific jersey colors for easy identification
- Captain/Vice-Captain badges
- **Live match indicators** with pulsing animations
- **Real-time stats**: Goals, assists, and opponent information
- Live point updates with color coding

### Live Match Features
- **Visual Differentiation**: Players currently playing have:
  - Pulsing red "LIVE" badge
  - Animated red border around card
  - Real-time point updates
- **Match Statistics**: 
  - Goals scored (⚽) with count
  - Assists made (🅰️) with count
  - Opponent team displayed (vs/@ notation)
- **Status Tracking**:
  - Live matches: Animated indicators
  - Finished matches: Full stats displayed
  - Upcoming matches: Opponent info shown

### Design Highlights
- Material 3 Design System
- FPL brand colors (purple header: #37003C)
- Responsive layouts for different screen sizes
- Smooth animations and transitions
- Professional card-based UI

## Future Enhancements

- Add player comparison features
- Show fixture difficulty ratings
- Add transfer history tracking
- Implement live score updates with notifications
- Add charts and graphs for performance trends
- Support for head-to-head leagues
- Player search functionality
- Drag-and-drop to reorder formation
- Player images/photos in cards
- Historical formation view for past gameweeks

## License

This is an educational project for learning purposes.

## Disclaimer

This app is not affiliated with or endorsed by the Premier League or Fantasy Premier League. All data is publicly available through the Fantasy Premier League API.

