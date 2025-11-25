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

## CI recommendations


- Prefer building installers on native runners for each OS/arch when possible (Windows runners for MSI, macOS runners for DMG). This avoids cross-host toolchain issues.

- If native runners are not available, create runtime images in containers that match the target architecture and then run `jpackage` on a host that supports producing the installer type (e.g., create an `arm64` runtime on Linux and then run `jpackage` for MSI on a Windows runner using that runtime image).

## Troubleshooting


- `jpackage` may produce symlinks inside app-image outputs. The `scripts/` helpers attempt to copy files while dereferencing symlinks (use `rsync -aL` or `cp -rL`).

- If an MSI fails to install on a target arch, verify the runtime image architecture matches the target and that the app's native libraries (JavaFX) were built for that arch.

## References


- `jlink` docs: [Oracle jlink docs](https://docs.oracle.com/javase/21/docs/specs/man/jlink.html)

- `jpackage` docs: [Oracle jpackage docs](https://docs.oracle.com/javase/21/docs/specs/man/jpackage.html)
