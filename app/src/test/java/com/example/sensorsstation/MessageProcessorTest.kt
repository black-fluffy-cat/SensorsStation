package com.example.sensorsstation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class MessageProcessorTest {

    private lateinit var messageProcessor: MessageProcessor
    private val firstReceivedMessage = "1/1/1/1/1#"
    private val secondReceivedMessage = "2/2/2/2/2#"
    private val partialReceivedMessage = "1/1/1/"
    private val endOfPartialReceivedMessage = "1/1#"

    @Before
    fun setUp() {
        messageProcessor = MessageProcessor()
    }

    @Test
    fun `processReceivedMessage message should be returned`() {
        val processedMsg = messageProcessor.processReceivedMessage(firstReceivedMessage)
        val firstMsgWithoutHash = firstReceivedMessage.replace("#", "")
        assertEquals(firstMsgWithoutHash, processedMsg)
    }

    @Test
    fun `processReceivedMessage message made from parts should be returned`() {
        messageProcessor.processReceivedMessage(firstReceivedMessage)
        val processedMsg = messageProcessor.processReceivedMessage(endOfPartialReceivedMessage)
        assertNotNull(processedMsg)
    }

    @Test
    fun `processReceivedMessage when processing two messages second one should be returned`() {
        val firstOneAndHalfMessage = "1/1/1/1/1#2/2/"
        val secondHalfMessage = "2/2/2#"
        messageProcessor.processReceivedMessage(firstOneAndHalfMessage)
        val processedMsg = messageProcessor.processReceivedMessage(secondHalfMessage)
        val secondMsgWithoutHash = secondReceivedMessage.replace("#", "")
        assertEquals(secondMsgWithoutHash, processedMsg)
    }

    @Test
    fun `processReceivedMessage message should be null`() {
        val processedMsg = messageProcessor.processReceivedMessage(partialReceivedMessage)
        assertNull(processedMsg)
    }

    @Test
    fun `processReceivedMessage message with hash on end should be null`() {
        val processedMsg = messageProcessor.processReceivedMessage(endOfPartialReceivedMessage)
        assertNull(processedMsg)
    }

    @Test
    fun `getUnitsFromCleanMessage from message with hash`() {
        val processedMsg = messageProcessor.processReceivedMessage(firstReceivedMessage)
        val receivedUnits = messageProcessor.getUnitsFromCleanMessage(processedMsg!!)
        assertEquals(ReceivedUnits(1, 1, 1, 1F), receivedUnits)
    }
}