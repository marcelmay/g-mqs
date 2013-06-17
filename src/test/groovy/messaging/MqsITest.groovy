package messaging

import com.ibm.mq.MQQueue
import com.ibm.mq.MQQueueManager

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
      withQueue(config.queueSend, Mqs.QueueOptions.SEND) {
        sendToQueue(msg, correlationId)
      }
      withQueue(config.queueSend, Mqs.QueueOptions.RECEIVE) {
        String received = receiveMessageByCorrelationId(correlationId)
        assert received == msg
      }
    }
  }
}
