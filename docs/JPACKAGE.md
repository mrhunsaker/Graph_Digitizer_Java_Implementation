# jlink / jpackage Guide

This document explains how to produce runtime images and native installers for multiple architectures, including examples for producing an `arm64` runtime image on an `x86_64` host.

## Why runtime images

jpackage bundles a runtime image into the generated installer when you pass `--runtime-image`. To create architecture-specific installers (for example Windows `x64` and `arm64` MSIs) you must supply runtime images that match the target CPU architecture.

You can obtain runtime images by:


- Downloading prebuilt JDK/JRE distributions for the target arch (Temurin, Liberica, Azul, etc.) and using their `jmods` or full directory as the `--runtime-image` source.

- Creating a minimal runtime with `jlink` from a matching JDK (recommended for reproducible packaging).

- Using an ARM64 container (Docker + QEMU) on an x86_64 host to run `jlink` and produce an `arm64` image.

## Example: `jlink` (native)

Run this on a machine with an ARM64 JDK or inside an ARM64 container:

```bash
jlink \
  --module-path $JAVA_HOME/jmods:target/lib \
  --add-modules java.base,java.desktop,java.logging,javafx.controls,javafx.graphics \
  --output /work/runtime-arm64 \
  --strip-debug --no-man-pages --no-header-files

```

Adjust the `--add-modules` list to match the modules your app actually needs. The `target/lib` path should include any modularized dependencies you want to link.

## Example: produce an `arm64` runtime on x86_64 using Docker

If you don't have native ARM64 hardware, you can run an ARM64 JDK inside Docker (requires QEMU support, which many Docker installations provide by default when you use `--platform linux/arm64`).

```bash
docker run --rm --platform linux/arm64 \
  -v "$(pwd)":/work -w /work \
  eclipse-temurin:17-jdk \
  bash -c "jlink --module-path /opt/java/openjdk/jmods --add-modules java.base,java.desktop,javafx.controls,javafx.graphics --output /work/runtime-arm64 --strip-debug --no-man-pages --no-header-files"

```

After this command `/work/runtime-arm64` (on your host) will contain a runtime image built for `arm64`.

## Using runtime images with `jpackage`

Example for Windows MSI (run on Windows host):

```powershell
& "$Env:JAVA_HOME\bin\jpackage.exe" --type msi --input target/ --main-jar graph-digitizer.jar --name GraphDigitizer --runtime-image C:\path\to\runtime-x64 --dest target/generated_builds

```

For ARM64 MSI you would pass `--runtime-image C:\path\to\runtime-arm64` (and run jpackage on a Windows host or produce a Windows-compatible runtime image accordingly).

### Recommended JVM & JavaFX builds

- Use a HotSpot JDK (we recommend Eclipse Temurin) for `jlink` and `jpackage` — HotSpot builds are the most commonly tested and are known to work well with JavaFX. Some OpenJ9 builds have shown native compatibility issues with JavaFX rendering or native libraries.
- Download JavaFX SDK/jmods that exactly match the `javafx.version` in `pom.xml` (for example `21.0.2`). Use the platform-specific `jmods` or platform jars for the target OS/arch.
- When creating runtime images, use the JDK that matches the target architecture (x64 vs arm64) and include `java.desktop` and `java.management` modules if your app uses AWT/Swing interop or management APIs.

## CI recommendations


- Prefer building installers on native runners for each OS/arch when possible (Windows runners for MSI, macOS runners for DMG). This avoids cross-host toolchain issues.

- If native runners are not available, create runtime images in containers that match the target architecture and then run `jpackage` on a host that supports producing the installer type (e.g., create an `arm64` runtime on Linux and then run `jpackage` for MSI on a Windows runner using that runtime image).

## Troubleshooting


- `jpackage` may produce symlinks inside app-image outputs. The `scripts/` helpers attempt to copy files while dereferencing symlinks (use `rsync -aL` or `cp -rL`).

- If an MSI fails to install on a target arch, verify the runtime image architecture matches the target and that the app's native libraries (JavaFX) were built for that arch.

- **AWT / Accessibility startup error**: On some Windows systems the bundled launcher may attempt to load the Java Accessibility Access Bridge (`com.sun.java.accessibility.AccessBridge`) which is not always present in minimal runtime images. When the AWT Toolkit attempts to load configured assistive technologies and the AccessBridge class is missing, startup can fail with an `AWTError: Assistive Technology not found: com.sun.java.accessibility.AccessBridge`.

  - Quick workaround (recommended for most builds): instruct the JVM to skip assistive-technology lookups by adding the JVM option:

    ```text
    -Djavax.accessibility.assistive_technologies=
    ```

    When using `jpackage` this should be included as a `--java-options` argument so generated launchers include the property. Example (snippet from this project's `pom.xml`):

    ```text
    --java-options -Djavax.accessibility.assistive_technologies=
    ```

  - Alternative: if you require full Access Bridge support, bundle the AccessBridge classes and native bits in the runtime image and enable them explicitly. This increases runtime size and complexity and is only needed for systems that rely on the Access Bridge.

  - Note: This project's `README.md` and `pom.xml` are updated to include this `--java-options` by default for the Windows `jpackage` steps.

## References


- `jlink` docs: [Oracle jlink docs](https://docs.oracle.com/javase/21/docs/specs/man/jlink.html)

- `jpackage` docs: [Oracle jpackage docs](https://docs.oracle.com/javase/21/docs/specs/man/jpackage.html)
