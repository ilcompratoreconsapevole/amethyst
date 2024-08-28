/**
 * Copyright (c) 2024 Vitor Pamplona
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the
 * Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.vitorpamplona.quartz.events.nip46

import com.fasterxml.jackson.core.type.TypeReference
import com.vitorpamplona.quartz.events.ContactListEvent.ReadWrite
import com.vitorpamplona.quartz.events.Event
import java.util.UUID

class BunkerResponseGetRelays(
    id: String = UUID.randomUUID().toString(),
    val relays: Map<String, ReadWrite>,
) : BunkerResponse(id, Event.mapper.writeValueAsString(relays), null) {
    companion object {
        fun parse(
            id: String,
            result: String,
            error: String? = null,
        ) = BunkerResponseGetRelays(id, Event.mapper.readValue(result, object : TypeReference<Map<String, ReadWrite>>() {}))
    }
}
