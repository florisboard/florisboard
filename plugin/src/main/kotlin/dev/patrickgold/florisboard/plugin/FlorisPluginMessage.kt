/*
 * Copyright (C) 2023 Patrick Goldinger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.patrickgold.florisboard.plugin

import android.os.Message

/**
 * Class which wraps a [Message] object and also helps correctly encoding and decoding the message's what and data
 * fields.
 *
 * Layout of the binary what flags:
 * | Byte 3 | Byte 2 | Byte 1 | Byte 0 |
 * |--------|--------|--------|--------|
 * |0       |        |        |    1111| Source of the message, may either be [SOURCE_SERVICE] or [SOURCE_CONSUMER].
 * |0       |        |        |1111    | Type of the message, may either be [TYPE_REQUEST] or [TYPE_RESPONSE].
 * |0       |        |11111111|        | Message action, may be one of the ACTION_* constants.
 * |01111111|11111111|        |        | Reserved for future use.
 */
@JvmInline
value class FlorisPluginMessage(val msg: Message) {
    companion object {
        const val SOURCE_SERVICE = 1
        const val SOURCE_CONSUMER = 2

        const val TYPE_REQUEST = 1
        const val TYPE_RESPONSE = 2

        const val ACTION_PRELOAD = 1
        const val ACTION_SPELL = 2
        const val ACTION_SUGGEST = 3

        private const val M_SOURCE = 0x0000000F
        private val O_SOURCE = M_SOURCE.countTrailingZeroBits()
        private const val M_TYPE = 0x000000F0
        private val O_TYPE = M_TYPE.countTrailingZeroBits()
        private const val M_ACTION = 0x0000FF00
        private val O_ACTION = M_ACTION.countTrailingZeroBits()

        private const val MESSAGE_DATA = "org.florisboard.plugin.MESSAGE_DATA"

        private fun encodeWhatField(source: Int, type: Int, action: Int): Int {
            return ((source shl O_SOURCE) and M_SOURCE) or
                ((type shl O_TYPE) and M_TYPE) or
                ((action shl O_ACTION) and M_ACTION)
        }

        private fun decodeWhatField(what: Int): Triple<Int, Int, Int> {
            return Triple(
                ((what and M_SOURCE) shr O_SOURCE),
                ((what and M_TYPE) shr O_TYPE),
                ((what and M_ACTION) shr O_ACTION),
            )
        }

        fun new(what: Int, id: Int, data: String?): FlorisPluginMessage {
            val msg = Message.obtain(null, what, id, 0)
            if (data != null) {
                msg.data.putString(MESSAGE_DATA, data)
            }
            return FlorisPluginMessage(msg)
        }

        fun requestToService(action: Int, id: Int = 0, data: String? = null): FlorisPluginMessage {
            val what = encodeWhatField(SOURCE_CONSUMER, TYPE_REQUEST, action)
            return new(what, id, data)
        }

        fun replyToConsumer(action: Int, id: Int = 0, data: String? = null): FlorisPluginMessage {
            val what = encodeWhatField(SOURCE_SERVICE, TYPE_RESPONSE, action)
            return new(what, id, data)
        }

        fun requestToConsumer(action: Int, id: Int = 0, data: String? = null): FlorisPluginMessage {
            val what = encodeWhatField(SOURCE_SERVICE, TYPE_REQUEST, action)
            return new(what, id, data)
        }

        fun replyToService(action: Int, id: Int = 0, data: String? = null): FlorisPluginMessage {
            val what = encodeWhatField(SOURCE_CONSUMER, TYPE_RESPONSE, action)
            return new(what, id, data)
        }
    }

    fun metadata(): Triple<Int, Int, Int> {
        return decodeWhatField(msg.what)
    }

    fun id(): Int {
        return msg.arg1
    }

    fun data(): String? {
        return msg.peekData()?.getString(MESSAGE_DATA)
    }

    override fun toString(): String {
        return msg.toString()
    }
}
