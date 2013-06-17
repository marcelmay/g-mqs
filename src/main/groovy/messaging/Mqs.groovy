package messaging

import com.ibm.mq.*
import com.ibm.mq.constants.MQConstants

class Mqs {

  class UnknownMqsException extends Exception {
    UnknownMqsException(final MQException ex) {
      super(ex)
    }
  }

  class NoMoreMessagesException extends Exception {
    NoMoreMessagesException(final MQException ex) {
      super(ex)
    }

    static def boolean matches(MQException ex) {
      ex.getCompCode() == 2 && ex.getReason() == 2033
    }
  }

  enum QueueOptions {
    SEND(MQConstants.MQOO_OUTPUT | MQConstants.MQOO_FAIL_IF_QUIESCING),
    RECEIVE(MQConstants.MQOO_INPUT_AS_Q_DEF | MQConstants.MQOO_OUTPUT)
    int value

    QueueOptions(int pValue) {
      value = pValue
    }
  }

  String hostname
  Integer port
  String channel
  MQQueueManager queueManager
  MQQueue queue

  def hostname(String pHostname) { hostname = pHostname; this }

  def port(Integer pPort) { port = pPort; this }

  def channel(String pChannel) { channel = pChannel; this }

  /**
   * Configures using a config object.
   *
   * Expects properties hostname, port and channel .
   * @param config the config object.
   * @return self
   */
  def configuredBy(ConfigObject config) {
    hostname(config.hostname)
    port(config.port instanceof String ? Integer.valueOf(config.port) : config.port) // Support String and Integer
    channel(config.channel)
    this
  }

  def Mqs withQueueManager(String queueManagerName, Closure closure) {
    prepareQueueManager(queueManagerName)
    try {
      closure.delegate = this
      closure.call()
    } finally {
      closeQueueManager()
    }
    this
  }

  def Mqs closeQueueManager() {
    if (queueManager != null) {
      queueManager.disconnect()
      queueManager = null
    }
    this
  }

  def Mqs prepareQueueManager(String queueManagerName) {
    MQEnvironment.@hostname = hostname
    MQEnvironment.@port = port
    MQEnvironment.@channel = channel
    queueManager = new MQQueueManager(queueManagerName)
    this
  }

  def Mqs withQueue(String queueName, QueueOptions queueOptions, Closure closure) {
    prepareQueue(queueName, queueOptions)
    try {
      closure.delegate = this
      closure.call(queue)
    } finally {
      closeQueue()
    }
    this
  }

  def Mqs prepareQueue(String queueName, QueueOptions queueOptions) {
    queue = createQueue(queueName, queueOptions)
    this
  }

  def MQQueue createQueue(String queueName, QueueOptions queueOptions) {
    queueManager.accessQueue(queueName, queueOptions.value)
  }

  def Mqs closeQueue() {
    if (queue != null) {
      queue.close()
      queue = null
    }
    this
  }

  def receiveMessageByCorrelationId(String corellationId) {
    MQGetMessageOptions options = new MQGetMessageOptions()
    options.options = MQConstants.MQGMO_WAIT
    options.matchOptions = MQConstants.MQMO_MATCH_CORREL_ID
    options.waitInterval = 1000

    MQMessage message = new MQMessage()
    message.correlationId = corellationId

    try {
      queue.get(message, options)
    } catch (MQException ex) {
      mapException(ex)
    }

    getContent(message)
  }

  def foreachMessageReceived(Closure closure) {
    MQGetMessageOptions options = new MQGetMessageOptions()
    options.options = MQConstants.MQGMO_WAIT
    options.waitInterval = 1000
    int count = 0

    try {
      while (true) {
        MQMessage message = new MQMessage();
        queue.get(message, options);
        String content = getContent(message)
        closure.delegate = this
        closure.call(content)
        count++
      }
    } catch (MQException ex) {
      if(!NoMoreMessagesException.matches(ex)) {
        throw new UnknownMqsException(ex)
      }
    }
    return count
  }

  def sendToQueue(String text) {
    sendToQueue(queue, text)
  }

  def static sendToQueue(MQQueue sentQueue, String text) {
    sendToQueue(sentQueue, text, null)
  }

  def sendToQueue(String text, String correlationId) {
    sendToQueue(queue, text, correlationId)
  }

  def static sendToQueue(MQQueue sentQeue, String text, String correlationId) {
    def message = new MQMessage()
    message.setVersion(MQConstants.MQMD_VERSION_2)
    message.format = MQConstants.MQFMT_STRING
    message.characterSet = 1208 // UTF-8
    if (correlationId) {
      message.correlationId = correlationId
    }
    message.writeString(text)
    sentQeue.put(message, new MQPutMessageOptions())
  }

  def sleepMs(ms) {
    if (ms > 0L) {
      // Throttle throughput
      sleep((ms) as long) { ex ->
        println('Got interrupted!')
        ex.printStackTrace()
      }
    }
  }

  def static String getContent(MQMessage msg) {
    int strLen = msg.getMessageLength()
    byte[] strData = new byte[strLen]
    msg.readFully(strData)
    return new String(strData)
  }

  def mapException(MQException ex) {
    if (NoMoreMessagesException.matches(ex)) {
      throw new NoMoreMessagesException(ex)
    } else throw new UnknownMqsException(ex)
  }
}

