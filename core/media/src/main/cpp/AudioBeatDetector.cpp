#include "AudioBeatDetector.h"
#include <cmath>
#include <algorithm>
#include <numeric>

namespace vidopro {

std::vector<int64_t> AudioBeatDetector::detectBeats(
    const int16_t* pcmSamples,
    int totalSamples,
    int sampleRate,
    int channels,
    float sensitivity
) {
    std::vector<int64_t> beatTimestampsMs;
    if (!pcmSamples || totalSamples <= 0 || sampleRate <= 0 || channels <= 0) {
        return beatTimestampsMs;
    }

    const int windowSize = 1024;
    const int hopSize = 512;
    int numFrames = (totalSamples / channels - windowSize) / hopSize;
    if (numFrames <= 0) return beatTimestampsMs;

    std::vector<float> energies(numFrames, 0.0f);

    // Compute energy for each window
    for (int f = 0; f < numFrames; ++f) {
        int startSample = f * hopSize * channels;
        double sumSq = 0.0;

        for (int i = 0; i < windowSize; ++i) {
            int idx = startSample + i * channels;
            if (idx >= totalSamples) break;
            int16_t s = pcmSamples[idx];
            sumSq += static_cast<double>(s) * s;
        }

        energies[f] = static_cast<float>(std::sqrt(sumSq / windowSize));
    }

    // Dynamic threshold over local window (~40 frames approx 0.5 - 1.0 sec)
    const int historySize = 30;
    const int minBeatDistanceFrames = std::max(1, static_cast<int>((0.20 * sampleRate) / hopSize)); // Min 200ms between beats
    int lastBeatFrame = -minBeatDistanceFrames;

    for (int f = historySize; f < numFrames - historySize; ++f) {
        float localSum = 0.0f;
        for (int j = f - historySize; j <= f + historySize; ++j) {
            localSum += energies[j];
        }
        float localAvg = localSum / (2 * historySize + 1);

        // Check if current frame is a local peak and exceeds sensitivity threshold
        float threshold = localAvg * sensitivity;
        if (energies[f] > threshold &&
            energies[f] > energies[f - 1] &&
            energies[f] >= energies[f + 1] &&
            (f - lastBeatFrame) >= minBeatDistanceFrames) {

            int64_t timestampMs = static_cast<int64_t>((static_cast<double>(f * hopSize) / sampleRate) * 1000.0);
            beatTimestampsMs.push_back(timestampMs);
            lastBeatFrame = f;
        }
    }

    return beatTimestampsMs;
}

} // namespace vidopro
