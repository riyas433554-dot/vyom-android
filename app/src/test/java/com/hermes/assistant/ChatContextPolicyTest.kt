package com.hermes.assistant

import org.junit.Assert.assertTrue
import org.junit.Test

class ChatContextPolicyTest {
 @Test fun contextIsBounded() { assertTrue(ChatContextPolicy.buildContext("memory", "history").length <= 2000) }
}
