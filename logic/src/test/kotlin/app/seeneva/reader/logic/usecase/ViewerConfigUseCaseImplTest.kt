/*
 * This file is part of Seeneva Android Reader
 * Copyright (C) 2021 Sergei Solodovnikov
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package app.seeneva.reader.logic.usecase

import app.seeneva.reader.logic.ComicsSettings
import app.seeneva.reader.logic.entity.configuration.ViewerConfig
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.unmockkAll
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ViewerConfigUseCaseImplTest {
    @MockK
    lateinit var settings: ComicsSettings

    private lateinit var useCase: ViewerConfigUseCase

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)

        useCase = ViewerConfigUseCaseImpl(settings)
    }

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `default config is valid`() {
        useCase.validate(ViewerConfig())
    }

    @Test
    fun `default assisted bubble zoom scale is smaller than legacy hardcoded value`() {
        // Legacy hardcoded scale which was used before this setting was introduced (viewer_balloon_scale_xy = 0.5)
        val legacyScale = 0.5f

        assert(ViewerConfig.DEFAULT_ASSISTED_BUBBLE_ZOOM_SCALE < legacyScale)
    }

    @Test
    fun `config with too small bubble zoom scale is invalid`() {
        assertFailsWith<IllegalArgumentException> {
            useCase.validate(
                ViewerConfig(
                    assistedBubbleZoomScale = ViewerConfig.ASSISTED_BUBBLE_ZOOM_SCALE_MIN - 0.01f
                )
            )
        }
    }

    @Test
    fun `config with too big bubble zoom scale is invalid`() {
        assertFailsWith<IllegalArgumentException> {
            useCase.validate(
                ViewerConfig(
                    assistedBubbleZoomScale = ViewerConfig.ASSISTED_BUBBLE_ZOOM_SCALE_MAX + 0.01f
                )
            )
        }
    }

    @Test
    fun `config with bubble zoom scale in range is valid`() {
        useCase.validate(
            ViewerConfig(
                assistedBubbleZoomScale = (ViewerConfig.ASSISTED_BUBBLE_ZOOM_SCALE_MIN + ViewerConfig.ASSISTED_BUBBLE_ZOOM_SCALE_MAX) / 2
            )
        )
    }

    @Test
    fun `old serialized config without new field deserializes with default bubble zoom scale`() {
        // Simulates an old persisted JSON blob which doesn't have the "assisted_bubble_zoom_scale" field
        val oldJson = """{"keep_screen_on":true,"brightness":-1.0,"tts":true}"""

        val config = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            .decodeFromString(ViewerConfig.serializer(), oldJson)

        assert(config.assistedBubbleZoomScale == ViewerConfig.DEFAULT_ASSISTED_BUBBLE_ZOOM_SCALE)
    }
}
