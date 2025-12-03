Packaging verification — latest run (2025-12-02)

Summary

- Installer MSI produced: `target/jpackage-msi/GraphDigitizer-1.1.msi` — present
- App-image folder: `target/jpackage/GraphDigitizer` — not present in this run (the `jpackage` step removed the folder before MSI creation)
- jlink runtime image used for packaging: `target/jlink-image` — present
- Key diagnostic logs captured during the process:
  - `target/jlink-driver/app-run3.log` (detailed JavaFX startup logs)
  - `target/jlink-native-debug.log` (native debug flags output)
  - `target/jlink-driver/run.log` (launcher driver run log)

Where artifacts live

- jlink runtime image: `target/jlink-image`
- jpackage app-image (if preserved): `target/jpackage/GraphDigitizer`
- installer MSI: `target/jpackage-msi/GraphDigitizer-1.1.msi`
- staged jpackage input (app + libs): `target/jpackage-input/` (contains `graph-digitizer.jar` and `lib/*.jar`)

Next steps you can request

- "Re-run packaging and keep app-image" — re-run `mvn -DskipTests package -Pnative` but skip the cleanup step so the `app-image` folder remains under `target/jpackage/GraphDigitizer` (I can do this and then list its contents).
- "Build and sign MSI" — run the packaging and then apply codesigning (requires access to a signing certificate).

Commands used to reproduce the run

1) Build + prepare jpackage input and run jpackage profile:

```powershell
mvn -DskipTests package -Pnative
```

2) Create the jlink runtime image (example used in this run):

```powershell
& 'C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot\bin\jlink.exe' \
  --strip-debug --no-man-pages --no-header-files \
  --add-modules java.desktop,java.management,java.logging,java.xml,javafx.base,javafx.graphics,javafx.controls,javafx.fxml,javafx.swing \
  --module-path 'C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot\jmods;C:\javafx-jmods-21.0.9' \
  --output target/jlink-image
```

3) Run the packaged app against the created runtime for verification (expanded classpath):

```powershell
$cp = (Get-ChildItem .\target\jpackage-input\lib -Filter *.jar | ForEach-Object { $_.FullName }) -join ';'
& .\target\jlink-image\bin\java \
  "-Djava.library.path=$PWD\target\jlink-image\bin;$PWD\target\jlink-image\bin\javafx" \
  "-Dprism.verbose=true" "-Djavafx.verbose=true" \
  -cp "$PWD\target\jlink-driver\bin;$PWD\target\jpackage-input\graph-digitizer.jar;$cp" \
  com.digitizer.ui.GraphDigitizerApp 2>&1 | Tee-Object .\target\jlink-driver\app-run3.log
```

Notes

- The `pom.xml` in the `native` profile was updated to copy runtime-scoped dependencies into `target/jpackage-input/lib` and to pass `--runtime-image ${project.build.directory}/jlink-image` to `jpackage`.
- If you'd like, I can re-run packaging to preserve the `app-image` folder and then enumerate its contents and sizes.
