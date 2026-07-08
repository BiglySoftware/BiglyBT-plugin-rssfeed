# Build (and optionally install) the RSSFeed Scanner plugin.
#
# The repo is already laid out as the final in-jar structure
# (org/kmallan/azureus/rssfeed/*.java + org/kmallan/resource/...), so we compile
# in place and jar it up -- no need for the src/ layout that build.xml expects.
#
# Requires: a JDK on PATH or JAVA_HOME set, and a BiglyBT install to compile against.
#
# Examples:
#   .\build-plugin.ps1 -BiglyBTDir "C:\Program Files\BiglyBT"
#   .\build-plugin.ps1 -BiglyBTDir "C:\Program Files\BiglyBT" -Install
#
param(
  # BiglyBT install directory (searched recursively for its jars, incl. SWT).
  [string]$BiglyBTDir,
  # Extra jars to add to the compile classpath (optional).
  [string[]]$Jars = @(),
  # If set, copies the built jar + plugin.properties into the plugin folder.
  [switch]$Install,
  # Target BiglyBT plugins dir for -Install (per-user default).
  [string]$PluginsDir = "$env:APPDATA\BiglyBT\plugins",
  # Optional explicit JDK home.
  [string]$JavaHome,
  # Target bytecode release. Java 8 maximizes compatibility (some BiglyBT
  # installs still run on a Java 8 runtime) and the resulting jar also loads on
  # newer JREs (11/17/21). The plugin uses no language/API features newer than 8.
  # Raise this if a future JDK drops Java 8 target support.
  [string]$Release = "8"
)

$ErrorActionPreference = "Stop"
$repo = $PSScriptRoot

function Resolve-Tool([string]$name) {
  if ($JavaHome) {
    $p = Join-Path $JavaHome "bin\$name.exe"
    if (Test-Path $p) { return $p }
  }
  if ($env:JAVA_HOME) {
    $p = Join-Path $env:JAVA_HOME "bin\$name.exe"
    if (Test-Path $p) { return $p }
  }
  $c = Get-Command $name -ErrorAction SilentlyContinue
  if ($c) { return $c.Source }
  throw "Could not find '$name'. Install a JDK and set JAVA_HOME or pass -JavaHome."
}

$javac = Resolve-Tool "javac"
$jar   = Resolve-Tool "jar"
Write-Host "javac: $javac"
Write-Host "jar:   $jar"

# --- read plugin version --------------------------------------------------
$props = Get-Content (Join-Path $repo "plugin.properties")
$version = ($props | Where-Object { $_ -match "^plugin\.version=" }) -replace "^plugin\.version=", ""
$pluginId = (($props | Where-Object { $_ -match "^plugin\.id=" }) -replace "^plugin\.id=", "").Trim()
if (-not $version) { throw "Could not read plugin.version from plugin.properties" }
$version = $version.Trim()
Write-Host "Building $pluginId version $version"

# --- assemble classpath ----------------------------------------------------
$cpJars = New-Object System.Collections.Generic.List[string]

if ($BiglyBTDir) {
  if (-not (Test-Path $BiglyBTDir)) { throw "BiglyBTDir not found: $BiglyBTDir" }
  Get-ChildItem -Path $BiglyBTDir -Recurse -Filter *.jar -ErrorAction SilentlyContinue |
    ForEach-Object { $cpJars.Add($_.FullName) }
}
# bundled json-io in the repo
Get-ChildItem -Path $repo -Filter "json-io*.jar" -ErrorAction SilentlyContinue |
  ForEach-Object { $cpJars.Add($_.FullName) }
foreach ($j in $Jars) { $cpJars.Add($j) }

if ($cpJars.Count -eq 0) {
  throw "No classpath jars found. Pass -BiglyBTDir <BiglyBT install> (needs the app jar + SWT jar)."
}
$cp = [string]::Join(";", $cpJars)
Write-Host "Classpath has $($cpJars.Count) jar(s)."

# --- clean build/dist ------------------------------------------------------
$build = Join-Path $repo "build"
$dist  = Join-Path $repo "dist"
if (Test-Path $build) { Remove-Item $build -Recurse -Force }
New-Item -ItemType Directory -Path $build | Out-Null
if (-not (Test-Path $dist)) { New-Item -ItemType Directory -Path $dist | Out-Null }

# --- compile ---------------------------------------------------------------
$srcFiles = Get-ChildItem -Path (Join-Path $repo "org") -Recurse -Filter *.java | ForEach-Object { $_.FullName }
Write-Host "Compiling $($srcFiles.Count) source files..."
$argfile = Join-Path $build "sources.txt"
$srcFiles | Set-Content -Path $argfile -Encoding UTF8
& $javac --release $Release -encoding UTF-8 -cp $cp -d $build ("@" + $argfile)
if ($LASTEXITCODE -ne 0) { throw "javac failed (exit $LASTEXITCODE)" }
Remove-Item $argfile

# --- copy resources into the jar tree (org/kmallan/resource/...) -----------
$resSrc = Join-Path $repo "org\kmallan\resource"
$resDst = Join-Path $build "org\kmallan\resource"
New-Item -ItemType Directory -Path $resDst -Force | Out-Null
Get-ChildItem -Path $resSrc -Recurse -Include *.properties, *.stf, *.gif | ForEach-Object {
  $rel = $_.FullName.Substring($resSrc.Length).TrimStart('\')
  $target = Join-Path $resDst $rel
  New-Item -ItemType Directory -Path (Split-Path $target) -Force | Out-Null
  Copy-Item $_.FullName $target -Force
}

# plugin.properties also inside the jar root (harmless, and convenient)
Copy-Item (Join-Path $repo "plugin.properties") (Join-Path $build "plugin.properties") -Force

# --- package ---------------------------------------------------------------
$jarName = "${pluginId}_${version}.jar"
$jarPath = Join-Path $dist $jarName
if (Test-Path $jarPath) { Remove-Item $jarPath -Force }
Write-Host "Packaging $jarName ..."
& $jar cf $jarPath -C $build .
if ($LASTEXITCODE -ne 0) { throw "jar failed (exit $LASTEXITCODE)" }
Write-Host "Built: $jarPath"

# --- optional install ------------------------------------------------------
if ($Install) {
  $target = Join-Path $PluginsDir $pluginId
  New-Item -ItemType Directory -Path $target -Force | Out-Null
  Copy-Item $jarPath (Join-Path $target $jarName) -Force
  Copy-Item (Join-Path $repo "plugin.properties") (Join-Path $target "plugin.properties") -Force
  # runtime dep: ship json-io alongside if BiglyBT doesn't already provide it
  Get-ChildItem -Path $repo -Filter "json-io*.jar" | ForEach-Object {
    Copy-Item $_.FullName (Join-Path $target $_.Name) -Force
  }
  Write-Host "Installed to: $target"
  Write-Host "Restart BiglyBT to load the plugin."
}
