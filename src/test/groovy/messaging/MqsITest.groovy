package messaging

import com.ibm.mq.MQQueue
import com.ibm.mq.MQQueueManager

/**
 * Note: Requires a test MQS instance!
 */
class MqsITest extends GroovyTestCase {
  ConfigObject config
  Mqs mqs

  @Override
  protected void setUp() {
    super.setUp()

    config = new ConfigObject()
    config.hostname = "vb"
    config.channel = "MM.CHANNEL"
    config.port = 1414
    config.queueManager = "qm_test2"
    config.queueSend = 'MM.SEND'
    config.queueReceive = 'MM.RECEIVE'

    mqs = new Mqs().configuredBy(config)
  }

  void testDestroyed() {
    MQQueue queueCreated
    MQQueueManager queueManagerCreated
    mqs.withQueueManager(config.queueManager) {
      assertNotNull(queueManager)
      assert queueManager.isConnected()
      queueManagerCreated = queueManager
      withQueue(config.queueSend, Mqs.QueueOptions.SEND) {
        // whatever
        assertNotNull(queue)
        assert queue.isOpen()
        queueCreated = queue
      }
    }
    assertNull(mqs.queue)
    assert !queueCreated.isOpen()
    assertNull(mqs.queueManager)
    assert !queueManagerCreated.isConnected()
  }

  void testSendAndReceive() {
    String correlationId = 'foo.bar'
    String msg = getClass().getResource('Sample.xml').getText('UTF-8')
    mqs.withQueueManager(config.queueManager) {
      // Make sure every queue is empty
      withQueue(config.queueSend, Mqs.QueueOptions.RECEIVE) {
        purgeQueue()
      }
      withQueue(config.queueReceive, Mqs.QueueOptions.RECEIVE) {
        purgeQueue()
      }

      // Send with correlation id
      withQueue(config.queueSend, Mqs.QueueOptions.SEND) {
        sendToQueue(msg, correlationId)
      }
      // Expect match with correlation id
      withQueue(config.queueSend, Mqs.QueueOptions.RECEIVE) {
        String received = receiveMessageByCorrelationId(correlationId)
        assert received == msg
      }
      // Read with no messages in queue
      withQueue(config.queueSend, Mqs.QueueOptions.RECEIVE) {
        try {
          receiveMessageByCorrelationId(correlationId)
          fail('Excpected exception')
        } catch (Mqs.NoMoreMessagesException ex) {
          // ok
        }
      }
      // Send
      withQueue(config.queueSend, Mqs.QueueOptions.SEND) {
        sendToQueue(msg + '_1')
        sendToQueue(msg + '_2')
      }
      // Expect two read
      withQueue(config.queueSend, Mqs.QueueOptions.RECEIVE) {
        assert foreachMessageReceived { content ->
          assert content == msg + '_1' || content == msg + '_2'
        } == 2
      }

      // Receive without timeout
      withQueue(config.queueSend, Mqs.QueueOptions.SEND) {
        sendToQueue(msg + '_3')
      }
      withQueue(config.queueSend, Mqs.QueueOptions.RECEIVE) {
        String received = receiveMessage()
        assert received == msg+'_3'
      }

      // Receive with timeout
      withQueue(config.queueSend, Mqs.QueueOptions.SEND) {
        sendToQueue(msg + '_4')
      }
      withQueue(config.queueSend, Mqs.QueueOptions.RECEIVE) {
        timeout(2000)
        String received = receiveMessage()
        assert received == msg+'_4'
      }
    }
  }

  void testPurge() {
    String msg = getClass().getResource('Sample.xml').getText('UTF-8')
    mqs.withQueueManager(config.queueManager) {
      // Send
      withQueue(config.queueSend, Mqs.QueueOptions.SEND) {
        sendToQueue(msg + '_1')
        sendToQueue(msg + '_2')
      }
      // Expect two read
      withQueue(config.queueSend, Mqs.QueueOptions.RECEIVE) {
        purgeQueue()
        assert foreachMessageReceived() == 0
      }
    }
  }
}
