package com.example

import org.junit.Assert.*
import org.junit.Test
import java.security.MessageDigest

class ExampleUnitTest {

    @Test
    fun testPersonalizationVariables() {
        val raw = "Hi {name}, meeting scheduled for {first_name}."
        val name = "Sarah Jenkins"
        val firstName = name.split(" ").first()
        val result = raw.replace("{name}", name).replace("{first_name}", firstName)

        assertEquals("Hi Sarah Jenkins, meeting scheduled for Sarah.", result)
    }

    @Test
    fun testSpamRiskScoring() {
        val cleanMsg = "Hi John, confirming our roadmap review tomorrow afternoon."
        val spamMsg = "URGENT FREE WINNER CLICK HERE NOW TO CLAIM YOUR PRIZE 100% GUARANTEED!!!"

        assertTrue(spamMsg.count { it.isUpperCase() } > cleanMsg.count { it.isUpperCase() })
        assertTrue(spamMsg.contains("FREE"))
        assertTrue(spamMsg.contains("WINNER"))
    }

    @Test
    fun testPinHashingDeterminism() {
        fun hashPin(pin: String): String {
            val salt = "WA_PRO_SECURE_SALT_2026"
            val md = MessageDigest.getInstance("SHA-256")
            val bytes = md.digest((salt + pin).toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }

        val hash1 = hashPin("1234")
        val hash2 = hashPin("1234")
        val hashWrong = hashPin("5678")

        assertEquals(hash1, hash2)
        assertNotEquals(hash1, hashWrong)
    }

    @Test
    fun testDeletedMessageDetectionString() {
        fun isDeletionNotice(text: String): Boolean {
            val lower = text.lowercase().trim()
            return lower.contains("this message was deleted") ||
                    lower.contains("message was deleted") ||
                    lower.contains("message deleted") ||
                    lower.contains("you deleted this message") ||
                    lower.contains("sender revoked") ||
                    lower.contains("revoked a message") ||
                    lower.contains("revoked this message") ||
                    lower.contains("este mensaje fue eliminado") ||
                    lower.contains("ce message a été supprimé") ||
                    lower.contains("diese nachricht wurde gelöscht") ||
                    lower.contains("esta mensagem foi apagada") ||
                    lower.contains("questo messaggio è stato eliminato") ||
                    lower.contains("यह संदेश हटा दिया गया") ||
                    lower == "deleted" ||
                    lower == "revoked"
        }

        assertTrue(isDeletionNotice("This message was deleted"))
        assertTrue(isDeletionNotice("this message was deleted."))
        assertTrue(isDeletionNotice("Message was deleted by sender"))
        assertTrue(isDeletionNotice("The sender revoked a message"))
        assertTrue(isDeletionNotice("Este mensaje fue eliminado"))
        assertTrue(isDeletionNotice("Ce message a été supprimé"))
        assertTrue(isDeletionNotice("Diese Nachricht wurde gelöscht"))
        assertFalse(isDeletionNotice("Hey, see you tomorrow at 10 AM!"))
    }

    @Test
    fun testSenderNormalization() {
        fun normalizeSender(sender: String): String {
            return sender
                .replace(Regex("\\s*\\(\\d+\\s+(?:new\\s+)?messages?\\)", RegexOption.IGNORE_CASE), "")
                .replace(Regex("^WhatsApp:\\s*", RegexOption.IGNORE_CASE), "")
                .trim()
        }

        assertEquals("Sarah Jenkins", normalizeSender("Sarah Jenkins (2 messages)"))
        assertEquals("David Chen", normalizeSender("David Chen (5 new messages)"))
        assertEquals("Project Group", normalizeSender("Project Group (1 message)"))
        assertEquals("Elena Rostova", normalizeSender("Elena Rostova"))
    }
}
