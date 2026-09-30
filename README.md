# נשימת ברזל · Iron Breath

An Android app for men: breathing exercises, relaxing music, meditation and yoga. Native Java, with no external libraries.

- **Breathing** – 11 protocols (box breathing, physiological sigh, power breathing with breath-hold records, 4-7-8, coherent, 1:2, alternate nostril, kapalbhati, ujjayi, diaphragmatic, CO₂ tolerance), with an animated orb, a watch-style bezel, sound cues and vibration.
- **Sound** – a mixer of 12 soundscapes synthesized in real time (rain, sea, campfire, wind, thunder, forest night, singing bowls, drone, brown noise, and alpha/theta/delta binaural beats), with presets and a sleep timer. Keeps playing in the background.
- **Meditation** – 7 guided sessions (3-minute reset, warrior focus, anger control, confidence, stoic practice, body scan, deep sleep) with optional Hebrew voice (TTS).
- **Yoga** – 6 routines and 27 poses, with an animated figure that moves between the poses.

The images are generated procedurally (`tools/gen_images.py`).

## Build
```bash
sudo apt install android-sdk-build-tools android-sdk-platform-23 apksigner zipalign openjdk-17-jdk
./build.sh            # → build/IronBreath.apk
```
