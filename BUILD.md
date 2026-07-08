# Building and installing the RSSFeed Scanner plugin

This plugin has no external build system beyond a JDK — it is compiled directly
against a BiglyBT installation. The repository is already laid out as the final
in-jar structure (`org/kmallan/azureus/rssfeed/*.java` for code and
`org/kmallan/resource/...` for language files and icons), so building is just
"compile the `org/` tree, then zip it into a jar".

> Note: the legacy `build.xml` expects a different `src/` + `include/` layout that
> does not match this repository, so it is not used by the steps below.

## Prerequisites

1. **A JDK** to compile with (any recent JDK works; JDK 21+ recommended).
2. **A BiglyBT installation** — needed both for the compile classpath and to run
   and test the plugin. The install directory contains the jars you compile
   against:
   - `BiglyBT.jar` (BiglyBT core + SWT UI classes + bundled Apache commons-lang)
   - `swt.jar` (SWT)
3. The bundled **`json-io_*.jar`** in this repository (a runtime dependency of the
   plugin, also needed on the compile classpath).

### Match the bytecode target to BiglyBT's runtime

BiglyBT ships its own JRE (in the `jre/` subfolder of the install). The built
classes must not use a **newer** bytecode version than that JRE, or BiglyBT will
fail to load the plugin with `UnsupportedClassVersionError`. Check the JRE version
with:

```
"<BiglyBT install>/jre/bin/java" -version
```

Different BiglyBT installs bundle different JREs (seen in the wild: Java 8 on older
installs, Java 21 on recent ones). To load everywhere, the build **targets Java 8
by default** (`--release 8`) — the plugin uses no language/API features newer than
8, and a Java 8 jar also runs fine on newer JREs (11/17/21). You can override with
`-Release <N>` if desired. (Very new JDKs warn that `--release 8` is obsolete but
still support it; raise the target if a future JDK removes it.)

## Option A — Windows (PowerShell helper script)

`build-plugin.ps1` (in the repo root) automates everything:

```powershell
# Build only -> dist\rssfeed_<version>.jar
.\build-plugin.ps1 -BiglyBTDir "C:\Program Files\BiglyBT"

# Build and install into your per-user plugins folder, then restart BiglyBT
.\build-plugin.ps1 -BiglyBTDir "C:\Program Files\BiglyBT" -Install
```

Useful parameters:

- `-JavaHome "C:\Program Files\Java\jdk-26.0.1"` — pick a specific JDK if it is not
  on `PATH` / `JAVA_HOME`.
- `-Release 8` — bytecode target (defaults to 8; see note above).
- `-PluginsDir <path>` — override the install target (defaults to
  `%APPDATA%\BiglyBT\plugins`).

The script discovers all jars under the BiglyBT install, adds the repo's
`json-io` jar, compiles `org/**/*.java`, bundles the resources/icons, and writes
`dist\<plugin.id>_<plugin.version>.jar`. With `-Install` it also copies the jar,
`plugin.properties`, and `json-io` into `<PluginsDir>\rssfeed\`.

## Option B — Any platform (manual commands)

From the repository root (adjust paths / separators for your OS — use `:` instead
of `;` on Linux/macOS):

```sh
# 1. Compile
javac --release 8 -encoding UTF-8 \
  -cp "/path/to/BiglyBT/BiglyBT.jar:/path/to/BiglyBT/swt.jar:json-io_2.5.2.1.jar" \
  -d build $(find org -name '*.java')

# 2. Copy the resources into the compiled tree
cp -r org/kmallan/resource build/org/kmallan/
cp plugin.properties build/

# 3. Package the jar
jar cf dist/rssfeed_1.8.7.jar -C build .
```

## Installing / testing the built plugin

Pick one:

- **Copy into the plugins folder** (what `-Install` does): create a folder named
  `rssfeed` under your BiglyBT user-configuration directory's `plugins/` subfolder
  and put `rssfeed_<version>.jar`, `plugin.properties`, and `json-io_*.jar` in it.
  The user-config directory is:
  - Windows: `%APPDATA%\BiglyBT`
  - macOS: `~/Library/Application Support/BiglyBT`
  - Linux: your BiglyBT config directory (see **Tools ▸ Options ▸ Files** in BiglyBT
    for the exact path)
- **Or use the BiglyBT UI:** *Tools ▸ Plugins ▸ Installation Wizard ▸ By File*, and
  point it at the built jar.

Then **restart BiglyBT**. Open the plugin from the **View** menu (it is not shown
automatically). If a `rssfeed.options` file already exists in the plugin folder,
leave it in place — it holds your feeds, filters, and history.

> When reinstalling, make sure only one `rssfeed_*.jar` is present in the plugin
> folder (delete older-version jars first) to avoid duplicate-class issues.
