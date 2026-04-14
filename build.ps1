param(
    [string]$Version = "1.0.18"
)

$JAVAC   = 'C:\Quest\Foglight\jre\bin\javac.exe'
$JAR     = 'C:\Quest\Foglight\jre\bin\jar.exe'
$GLUEAPI = 'C:\Quest\Foglight\fglam\client\8.2.0.0-202603110530-c653f370-1029\lib\glueapi.jar'

$JACKSON_CORE  = (Get-ChildItem "$env:USERPROFILE\.m2\repository\com\fasterxml\jackson\core\jackson-core"        -Filter 'jackson-core-*.jar'    -Recurse | Where-Object { $_.Name -notlike '*sources*' } | Select-Object -First 1).FullName
$JACKSON_ANNOT = (Get-ChildItem "$env:USERPROFILE\.m2\repository\com\fasterxml\jackson\core\jackson-annotations" -Filter '*.jar'                -Recurse | Where-Object { $_.Name -notlike '*sources*' } | Select-Object -First 1).FullName
$JACKSON_BIND  = (Get-ChildItem "$env:USERPROFILE\.m2\repository\com\fasterxml\jackson\core\jackson-databind"   -Filter 'jackson-databind-*.jar' -Recurse | Where-Object { $_.Name -notlike '*sources*' } | Select-Object -First 1).FullName

$AGENT_LIB = 'C:\Quest\Foglight\fglam\agents\DatabricksAgent\1.0.6-1.0.6\lib'

$CP = "$GLUEAPI;$JACKSON_CORE;$JACKSON_ANNOT;$JACKSON_BIND"

# --- compile ---
Write-Host "Compiling Java (version $Version)..."
New-Item -ItemType Directory -Force target\classes | Out-Null
& $JAVAC -cp $CP -d target\classes src\main\java\com\quest\foglight\databricks\*.java
if ($LASTEXITCODE -ne 0) { Write-Host "COMPILE FAILED"; exit 1 }

# --- jar ---
Write-Host "Building JAR..."
& $JAR cf target\databricks-agent.jar -C target\classes .
if ($LASTEXITCODE -ne 0) { Write-Host "JAR FAILED"; exit 1 }

# --- deploy JAR to FglAM ---
Write-Host "Copying JAR to $AGENT_LIB..."
Copy-Item -Force target\databricks-agent.jar "$AGENT_LIB\databricks-agent.jar"

# --- cartridge ---
Write-Host "Building cartridge..."
python build_cartridge.py $Version
if ($LASTEXITCODE -ne 0) { Write-Host "CARTRIDGE BUILD FAILED"; exit 1 }

Write-Host ""
Write-Host "Build complete: target\DatabricksAgent-$Version.car"

# --- distribution zip ---
Write-Host "Building distribution package..."
$DIST_DIR  = "target\dist"
$AGENT_DIR = "$DIST_DIR\agent-deploy"
Remove-Item -Recurse -Force $DIST_DIR -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force "$AGENT_DIR\config" | Out-Null
New-Item -ItemType Directory -Force "$AGENT_DIR\lib"    | Out-Null

Copy-Item "src\main\resources\config\agent.manifest" "$AGENT_DIR\config\agent.manifest"
Copy-Item "INSTALL.md"                               "$DIST_DIR\INSTALL.md"
Copy-Item "target\DatabricksAgent-$Version.car"      "$DIST_DIR\DatabricksAgent-$Version.car"
Copy-Item "target\databricks-agent.jar"              "$AGENT_DIR\lib\databricks-agent.jar"
Copy-Item $JACKSON_CORE                              "$AGENT_DIR\lib\"
Copy-Item $JACKSON_ANNOT                             "$AGENT_DIR\lib\"
Copy-Item $JACKSON_BIND                              "$AGENT_DIR\lib\"

# properties template (no real token)
@"
workspaceUrl=https://<your-workspace>.azuredatabricks.net/
accessToken=<your-token-here>
collectionIntervalSeconds=60
accountId=default
accountName=Databricks
"@ | Set-Content "$AGENT_DIR\config\databricks.properties"

$ZIP = "target\DatabricksAgent-$Version-dist.zip"
Compress-Archive -Path "$DIST_DIR\*" -DestinationPath $ZIP -Force
Write-Host "Distribution zip: $ZIP"

Write-Host ""
Write-Host "Next steps:"
Write-Host "  1. Install target\DatabricksAgent-$Version.car via Foglight UI (Admin > Cartridges)"
Write-Host "  2. Restart FglAM"
