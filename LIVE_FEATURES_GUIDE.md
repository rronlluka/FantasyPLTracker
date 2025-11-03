# Live Match Features Guide

## Overview
The FPL Tracker app now includes comprehensive live match tracking features that differentiate between players who are currently playing, have finished their matches, or haven't played yet.

## Visual Indicators

### 1. Live Match Status

#### LIVE Players
Players currently in active matches display:
- **Pulsing "LIVE" badge** in red above the card
- **Animated red border** around the player card (pulses between 30% and 100% opacity)
- Updates every 1 second for smooth animation
- Takes priority over Captain/Vice-Captain badges (C/V badges only show if not live)

#### Finished Matches
Players whose matches have concluded:
- Standard card appearance
- Complete statistics displayed
- No special animations

#### Upcoming Matches
Players yet to play:
- Standard card appearance
- Opponent information displayed
- No live indicators

### 2. Player Statistics

Each player card now displays:

#### Points Display
- Total points with captain multiplier applied
- Color-coded background:
  - **Green** (bright): 10+ points
  - **Green** (light): 6-9 points
  - **Red**: 0-2 points
  - **Grey**: 3-5 points

#### Goals and Assists
- **⚽ Goals**: Soccer ball emoji with count (e.g., "⚽2")
- **🅰️ Assists**: A emoji with count (e.g., "🅰️1")
- Only displayed if the player has goals or assists
- Shown in a compact row below points

#### Opponent Information
- **Home matches**: "vs [TEAM]" (e.g., "vs ARS")
- **Away matches**: "@ [TEAM]" (e.g., "@ CHE")
- Displays team short name (3 letters)
- Shown at the bottom of each card in grey text

## Technical Implementation

### Data Flow

```
1. Load Fixtures for Gameweek
   ↓
2. Match Players to Their Fixtures
   ↓
3. Determine Match Status (live/finished/upcoming)
   ↓
4. Load Live Stats (goals, assists, points)
   ↓
5. Render with Appropriate Visual Indicators
```

### API Data Sources

#### Fixtures Endpoint
- `GET /api/fixtures/?event={event_id}`
- Provides:
  - Match status (started, finished)
  - Home and away teams
  - Current minutes played
  - Scores

#### Live Gameweek Endpoint
- `GET /api/event/{event_id}/live/`
- Provides:
  - Real-time player stats
  - Goals scored
  - Assists made
  - Total points
  - Minutes played

#### Bootstrap Data
- `GET /api/bootstrap-static/`
- Provides:
  - Team information
  - Team short names
  - Team colors

### State Management

The `ManagerFormationViewModel` manages:
- `isLive`: Boolean flag for live match status
- `hasPlayed`: Boolean flag for completed matches
- `fixture`: Player's fixture data
- `opponentTeam`: Opponent team information
- `liveStats`: Real-time player statistics

## UI Components

### FootballPitch Component

#### PlayerCardOnPitch
Enhanced player card with:
- Size: 68dp wide × 92dp tall
- Conditional rendering based on match status
- Animation support for live matches
- Dynamic content based on stats

Key sections:
1. **Top Badge** (22dp height)
   - Captain/Vice-Captain badge OR
   - Live indicator badge
2. **Jersey** (30dp × 30dp)
   - Team color background
   - Team abbreviation
3. **Player Name** (8.5sp)
   - Truncated if too long
4. **Stats Section**
   - Points display
   - Goals/Assists row
   - Opponent team

### BenchPlayerCard Component

Similar to pitch cards but:
- Size: 75dp wide × 100dp tall
- No live border animation
- Position label above (GKP, 1. DEF, etc.)
- Same statistics display

## Animation Details

### Pulsing Effect

```kotlin
infiniteTransition.animateFloat(
    initialValue = 0.3f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
        animation = tween(1000, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
    )
)
```

- Duration: 1 second per cycle
- Easing: FastOutSlowInEasing for smooth transitions
- Alpha range: 30% to 100%
- Applied to:
  - LIVE badge background
  - Card border color

## User Experience

### Benefits

1. **At-a-Glance Status**
   - Instantly see which players are currently playing
   - No need to check external sources

2. **Detailed Stats**
   - Goals and assists displayed prominently
   - Easy to track player performance

3. **Opponent Context**
   - Know who each player is facing
   - Home/away distinction clear

4. **Visual Hierarchy**
   - Live players draw attention
   - Captain information preserved when not live
   - Color-coded performance indicators

### Usage Scenarios

#### During Live Gameweek
1. Open team formation
2. See pulsing LIVE indicators for active matches
3. Monitor real-time points updates
4. Check goals/assists as they happen

#### After Matches
1. View complete statistics
2. See all goals and assists
3. Compare performance across players
4. No distracting animations

#### Before Gameweek
1. Preview opponent matchups
2. Plan captain choices
3. Assess fixture difficulty

## Customization Options

### Changing Live Indicator Color

Edit `PlayerCardOnPitch` in `FootballPitch.kt`:
```kotlin
color = Color(0xFFFF0000) // Red - change to your preference
```

### Adjusting Animation Speed

Modify the `tween` duration:
```kotlin
animation = tween(1000) // milliseconds
```

### Modifying Badge Style

Change the LIVE badge appearance:
```kotlin
Box(
    modifier = Modifier
        .background(
            color = Color(0xFFFF0000), // Background color
            shape = RoundedCornerShape(4.dp) // Border radius
        )
        .padding(horizontal = 6.dp, vertical = 2.dp) // Internal padding
)
```

## Testing

### Test Scenarios

1. **Live Match**
   - Find a gameweek with ongoing matches
   - Verify LIVE badge appears
   - Check animation is smooth
   - Confirm border pulses

2. **Finished Match**
   - View a completed gameweek
   - Verify no LIVE indicators
   - Check stats are displayed correctly

3. **Mixed Status**
   - View gameweek with some finished, some live matches
   - Confirm correct differentiation

4. **Captain Live**
   - Check captain badge shows even when not live
   - Verify LIVE takes precedence when playing

### Debug Information

The ViewModel provides:
```kotlin
playerDetail.isLive // true if currently playing
playerDetail.hasPlayed // true if match finished
playerDetail.fixture // fixture details
```

## Performance Considerations

1. **Animations**
   - Use `rememberInfiniteTransition` for efficiency
   - Single animation instance per card
   - Low CPU overhead

2. **Data Updates**
   - Fixture data loaded once per screen load
   - Live stats can be refreshed as needed
   - Cached in ViewModel state

3. **Rendering**
   - Conditional rendering reduces unnecessary draws
   - Only live players have animation overhead

## Troubleshooting

### Live indicator not showing
- Check fixture data is loading correctly
- Verify `started` and `finished` flags in fixture
- Ensure current time is during match

### Stats not updating
- Verify live gameweek endpoint is responding
- Check player element ID matches
- Confirm live stats are in response

### Animation not smooth
- Check device performance
- Reduce animation duration if needed
- Verify no other heavy operations running

### Opponent team not showing
- Verify fixture matching logic
- Check team ID mapping
- Ensure bootstrap data loaded

## Future Enhancements

Potential improvements:
1. **Live score updates** - Show actual match score
2. **Minute indicators** - Display minutes played
3. **Red/Yellow cards** - Show bookings with icons
4. **Bonus point indicators** - Live bonus point tracking
5. **Auto-refresh** - Periodic data updates during live gameweeks
6. **Push notifications** - Alert on player goals/assists
7. **Video highlights** - Link to goal videos
8. **Live chat/commentary** - Match updates and insights

## API Considerations

### Rate Limiting
- Be mindful of API request frequency
- Cache data when possible
- Don't refresh more than once per minute during live games

### Data Accuracy
- Live data may have delays (30-60 seconds typical)
- Stats are provisional until match ends
- Bonus points calculated after matches finish

### Error Handling
- Gracefully handle missing fixture data
- Default to standard display if live stats unavailable
- Show cached data if API is down

## Conclusion

The live match features provide a comprehensive, visually appealing way to track your FPL team in real-time. The combination of animations, detailed statistics, and clear opponent information creates an engaging user experience that rivals the official app.

