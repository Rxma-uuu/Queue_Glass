# QUEUEGLASS, a High-Frequency L3 Order Book Engine & Quantitative Research Platform

QUEUEGLASS is a high-performance, deterministic L3 matching engine and quantitative research terminal built in C++20 with a Jetpack Compose Android interface. It enables real-time level-3 order book simulation, microstructure strategy backtesting, execution policy comparison (Immediate vs. TWAP vs. Passive Limit), and AI-driven quantitative research.

---

## Key Features

- **C++20 High-Performance Engine:**
  - Real-time L3 event generator (ADD, CANCEL, EXECUTE, MODIFY).
  - Order book maintenance with price-time priority.
  - Deterministic replay, state checkpointing, and seeking.
  - Candlestick aggregation across arbitrary timeframes (`1s`, `5s`, `30s`, `2m`).
  - Rolling volatility calculation and order book depth statistics.

- **Dedicated Authentication & Data Modes:**
  - **Authenticated Account Mode:** Local encrypted account store using salted SHA-256 password hashing.
  - **Offline Demo Mode:** Instant access to native L3 market simulation without requiring account registration.
  - **Session Persistence:** Persistent login state with session restoration and logout capability.

- **Quantitative Research & Experiments:**
  - **Execution Policy Comparison:** Evaluate Immediate Market, TWAP, and Passive Limit execution policies.
  - **Microstructure Backtest:** Strategy engine evaluating order book depth imbalance and PnL.
  - **AI Research Assistant:** RAG evidence retrieval and C++ hypothesis testing pipeline.
  - **Saved Investigations:** Room database persistence for execution experiment results.

---

## Technical Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Android Jetpack Compose                  │
│   Market | Replay | Experiments | Research | Settings       │
└──────────────────────────────┬──────────────────────────────┘
                               │ JNI Bridge (libqueglass.so)
┌──────────────────────────────▼──────────────────────────────┐
│                      C++20 Core Engine                      │
│   EventGenerator │ OrderBook │ ExecutionEngine │ McpServer │
└─────────────────────────────────────────────────────────────┘
```

- **Android Client:** Kotlin, Jetpack Compose, Material 3, ViewModel, StateFlow, Room Database, DataStore/SharedPreferences.
- **Native Core:** C++20 STL, JNI Bridge, CMake, NDK.
- **Supported ABIs:** `arm64-v8a` (Physical Devices) and `x86_64` (Emulators).

---

## Setup & Build Instructions

### Prerequisites
- JDK 17 / Kotlin 2.0+

### Building the Project
1. Clone the repository:
   ```bash
   git clone https://github.com/Rxma-uuu/Queue_Glass.git
   cd Queue_Glass
   ```
2. Build the debug APK via Gradle:
   ```bash
   ./gradlew assembleDebug
   ```
3. Run native C++ test suite:
   ```bash
   cd core/tests
   mkdir build && cd build
   cmake .. && make
   ./queglass_tests
   ```

### Output APK
- **Debug APK Location:** `app/build/outputs/apk/debug/app-debug.apk`

---

## License & Security
- Plaintext passwords are **never stored**; all credentials use salted SHA-256 hashing.
- Excludes secrets, signing keys, and generated build artifacts via `.gitignore`.
