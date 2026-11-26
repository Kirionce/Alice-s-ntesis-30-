#!/usr/bin/env bash
set -euo pipefail

ANDROID_SDK_DIR="/opt/android-sdk"
GRADLE_VERSION="9.3.1"
GRADLE_DIR="/opt/gradle/gradle-${GRADLE_VERSION}"
CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
GRADLE_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"

echo "==> Instalando utilidades base..."
if ! command -v unzip >/dev/null 2>&1 || ! command -v wget >/dev/null 2>&1; then
  sudo apt-get update
  sudo apt-get install -y unzip wget
fi

if ! command -v java >/dev/null 2>&1; then
  sudo apt-get update
  sudo apt-get install -y openjdk-17-jdk
fi

echo "==> Configurando Android SDK en ${ANDROID_SDK_DIR}..."
sudo mkdir -p "${ANDROID_SDK_DIR}/cmdline-tools"
sudo chown -R "$(id -u):$(id -g)" "${ANDROID_SDK_DIR}"

if [ ! -x "${ANDROID_SDK_DIR}/cmdline-tools/latest/bin/sdkmanager" ]; then
  wget -q "${CMDLINE_TOOLS_URL}" -O /tmp/cmdline-tools.zip
  rm -rf "${ANDROID_SDK_DIR}/cmdline-tools/latest" /tmp/cmdline-tools-extract
  mkdir -p /tmp/cmdline-tools-extract
  unzip -q /tmp/cmdline-tools.zip -d /tmp/cmdline-tools-extract
  mv /tmp/cmdline-tools-extract/cmdline-tools "${ANDROID_SDK_DIR}/cmdline-tools/latest"
  rm -rf /tmp/cmdline-tools.zip /tmp/cmdline-tools-extract
fi

export ANDROID_HOME="${ANDROID_SDK_DIR}"
export ANDROID_SDK_ROOT="${ANDROID_SDK_DIR}"
export PATH="${ANDROID_SDK_DIR}/cmdline-tools/latest/bin:${ANDROID_SDK_DIR}/platform-tools:${PATH}"

echo "==> Aceptando licencias e instalando Android SDK 36..."
set +o pipefail
yes | sdkmanager --licenses >/dev/null 2>&1 || true
set -o pipefail
sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.0.0"

echo "==> Instalando Gradle ${GRADLE_VERSION}..."
if [ ! -x "${GRADLE_DIR}/bin/gradle" ]; then
  sudo mkdir -p /opt/gradle
  wget -q "${GRADLE_URL}" -O /tmp/gradle.zip
  sudo unzip -q -o /tmp/gradle.zip -d /opt/gradle
  rm -f /tmp/gradle.zip
fi
sudo ln -sf "${GRADLE_DIR}/bin/gradle" /usr/local/bin/gradle

echo "==> Exportando variables de entorno..."
PROFILE_FILE="/etc/profile.d/android-sdk.sh"
sudo tee "${PROFILE_FILE}" >/dev/null <<EOF
export ANDROID_HOME="${ANDROID_SDK_DIR}"
export ANDROID_SDK_ROOT="${ANDROID_SDK_DIR}"
export PATH="${ANDROID_SDK_DIR}/cmdline-tools/latest/bin:${ANDROID_SDK_DIR}/platform-tools:${GRADLE_DIR}/bin:\$PATH"
EOF

if ! grep -q "ANDROID_SDK_ROOT=" "${HOME}/.bashrc" 2>/dev/null; then
  cat >> "${HOME}/.bashrc" <<EOF

# Android SDK & Gradle (Alice & Yui)
export ANDROID_HOME="${ANDROID_SDK_DIR}"
export ANDROID_SDK_ROOT="${ANDROID_SDK_DIR}"
export PATH="${ANDROID_SDK_DIR}/cmdline-tools/latest/bin:${ANDROID_SDK_DIR}/platform-tools:${GRADLE_DIR}/bin:\$PATH"
EOF
fi

if [ -f "debug.keystore.base64" ] && [ ! -f "debug.keystore" ]; then
  echo "==> Restaurando debug.keystore desde debug.keystore.base64..."
  base64 -d debug.keystore.base64 > debug.keystore
fi

echo "==> Entorno listo. Ejecuta: gradle :app:assembleDebug"
