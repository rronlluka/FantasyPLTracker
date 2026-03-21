import os
import re

login_file = "app/src/main/java/com/fpl/tracker/ui/screens/LoginScreen.kt"
with open(login_file, "r") as f:
    content = f.read()
content = re.sub(r'import com\.fpl\.tracker\.ui\.theme\.DeepSpace\n', '', content)
content = re.sub(r'import com\.fpl\.tracker\.ui\.theme\.NightSky\n', '', content)
content = content.replace('NightSky', 'MaterialTheme.colorScheme.surface')
content = content.replace('DeepSpace', 'MaterialTheme.colorScheme.background')
with open(login_file, "w") as f:
    f.write(content)

main_file = "app/src/main/java/com/fpl/tracker/ui/screens/MainAppScreen.kt"
with open(main_file, "r") as f:
    content = f.read()
content = re.sub(r'import com\.fpl\.tracker\.ui\.theme\.Starfield\n', '', content)
content = content.replace('Starfield', 'MaterialTheme.colorScheme.surfaceContainer')
with open(main_file, "w") as f:
    f.write(content)

matches_file = "app/src/main/java/com/fpl/tracker/ui/screens/MatchesScreen.kt"
with open(matches_file, "r") as f:
    content = f.read()
content = re.sub(r'private val FplPurple = MaterialTheme\.colorScheme\.surface\n', '', content)
content = content.replace('FplPurple', 'MaterialTheme.colorScheme.surface')
with open(matches_file, "w") as f:
    f.write(content)

print("done")
