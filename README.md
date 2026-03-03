# Aura AI - Local AI Assistant for Android

[![Android CI](https://github.com/yourusername/AuraAI/actions/workflows/build.yml/badge.svg)](https://github.com/yourusername/AuraAI/actions/workflows/build.yml)

Aura AI is a powerful offline AI assistant that runs completely on your device using Qwen 3-0.6B ONNX model. It can handle conversations and control your phone for various tasks.

## ✨ Features

- 🤖 **Local AI Processing**: Runs Qwen 3-0.6B model locally using ONNX Runtime
- 💬 **Smart Conversations**: Context-aware chat with history
- 📱 **Device Control**: Open apps, search web, scroll, and more
- 🔒 **Privacy First**: All processing happens on-device, no internet required
- 💾 **Persistent Storage**: Chat history saved locally using Room database
- 🎨 **Material Design**: Modern UI with Jetpack Compose
- 🌙 **Dark/Light Theme**: Automatic theme support
- 📋 **Conversation History**: Browse and manage past conversations

## 🎯 Available Commands

- "Open [app name]" - Launch any installed app (e.g., "open Chrome")
- "Search for [query]" - Perform web search (e.g., "search for cute cats")
- "Scroll up/down" - Scroll the current screen
- "Go back/home" - Navigation commands
- Plus regular conversation!

## 📥 Installation

1. Download the latest APK from [Releases](https://github.com/yourusername/AuraAI/releases)
2. Install the app on your Android device (requires Android 8.0+)
3. Grant storage permissions when prompted

## ⚙️ Model Setup

1. Launch the app
2. Note the model directory path shown on screen
3. Place your model files in the specified directory:
   - Model: `qwen-3-0.6b.onnx`
   - Tokenizer: `tokenizer.json`
4. Click "Scan for Model" to verify
5. Start chatting!

## 🏗️ Build from Source

```bash
# Clone the repository
git clone https://github.com/yourusername/AuraAI.git
cd AuraAI

# Make gradlew executable
chmod +x gradlew

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug
