#pragma once
#include <cstdint>
#include <vector>

namespace vidopro {

class AudioBeatDetector {
public:
    static std::vector<int64_t> detectBeats(
        const int16_t* pcmSamples,
        int totalSamples,
        int sampleRate,
        int channels,
        float sensitivity = 1.3f
    );
};

} // namespace vidopro
