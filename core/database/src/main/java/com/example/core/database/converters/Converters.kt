package com.example.core.database.converters

import androidx.room.TypeConverter
import com.example.core.model.AspectRatio
import com.example.core.model.ClipType
import com.example.core.model.EffectType
import com.example.core.model.InterpolationType
import com.example.core.model.MediaType
import com.example.core.model.TrackType
import com.example.core.model.TransitionType

class Converters {
    @TypeConverter
    fun fromTrackType(value: TrackType): String = value.name

    @TypeConverter
    fun toTrackType(value: String): TrackType = runCatching { TrackType.valueOf(value) }.getOrDefault(TrackType.VIDEO)

    @TypeConverter
    fun fromClipType(value: ClipType): String = value.name

    @TypeConverter
    fun toClipType(value: String): ClipType = runCatching { ClipType.valueOf(value) }.getOrDefault(ClipType.VIDEO)

    @TypeConverter
    fun fromMediaType(value: MediaType): String = value.name

    @TypeConverter
    fun toMediaType(value: String): MediaType = runCatching { MediaType.valueOf(value) }.getOrDefault(MediaType.VIDEO)

    @TypeConverter
    fun fromAspectRatio(value: AspectRatio): String = value.name

    @TypeConverter
    fun toAspectRatio(value: String): AspectRatio = runCatching { AspectRatio.valueOf(value) }.getOrDefault(AspectRatio.RATIO_9_16)

    @TypeConverter
    fun fromEffectType(value: EffectType): String = value.name

    @TypeConverter
    fun toEffectType(value: String): EffectType = runCatching { EffectType.valueOf(value) }.getOrDefault(EffectType.BRIGHTNESS)

    @TypeConverter
    fun fromInterpolationType(value: InterpolationType): String = value.name

    @TypeConverter
    fun toInterpolationType(value: String): InterpolationType = runCatching { InterpolationType.valueOf(value) }.getOrDefault(InterpolationType.LINEAR)

    @TypeConverter
    fun fromTransitionType(value: TransitionType): String = value.name

    @TypeConverter
    fun toTransitionType(value: String): TransitionType = runCatching { TransitionType.valueOf(value) }.getOrDefault(TransitionType.NONE)
}
