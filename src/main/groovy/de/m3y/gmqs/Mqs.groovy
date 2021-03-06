package de.m3y.gmqs

import com.ibm.mq.*
import com.ibm.mq.constants.MQConstants

class Mqs {

  abstract class MqsException extends Exception {
    MqsException(final Throwable cause) {
      super(cause)
    }
  }

  class UnknownMqsException extends MqsException {
    UnknownMqsException(final MQException ex) {
      super(ex)
    }
  }

  class NoMoreMessagesException extends MqsException {
    NoMoreMessagesException(final MQException ex) {
      super(ex)
    }

    static boolean matches(MQException ex) {
      ex.compCode == 2 && ex.reason == 2033
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

  class MqGetMessageOptionsBuilder {
    MQGetMessageOptions options = new MQGetMessageOptions()

    MqGetMessageOptionsBuilder waitInterval(int ms) {
      if (timeout > 0) {
        options.options += MQConstants.MQGMO_WAIT
        options.waitInterval = ms
      }
      this
    }

    MqGetMessageOptionsBuilder noWait() {
      options.options += MQConstants.MQGMO_NO_WAIT
      this
    }

    MqGetMessageOptionsBuilder matchCorrelationId() {
      options.matchOptions = MQConstants.MQMO_MATCH_CORREL_ID
      this
    }

    MQGetMessageOptions create() {
      options
    }

    MqGetMessageOptionsBuilder failIfQuiescing() {
      options.options += MQConstants.MQGMO_FAIL_IF_QUIESCING
      this
    }
  }

  String hostname
  Integer port
  String channel
  int timeout
  MQQueueManager queueManager
  MQQueue queue

  Mqs hostname(String pHostname) { hostname = pHostname; this }

  Mqs port(Integer pPort) { port = pPort; this }

  Mqs channel(String pChannel) { channel = pChannel; this }

  Mqs timeout(int ms) { timeout = ms; this }

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

  Mqs withQueueManager(String queueManagerName, Closure closure) {
    prepareQueueManager(queueManagerName)
    try {
      closure.delegate = this
      closure.call()
    } finally {
      closeQueueManager()
    }
    this
  }

  Mqs closeQueueManager() {
    if (queueManager != null) {
      queueManager.disconnect()
      queueManager = null
    }
    this
  }

  Mqs prepareQueueManager(String queueManagerName) {
    MQEnvironment.@hostname = hostname
    MQEnvironment.@port = port
    MQEnvironment.@channel = channel
    queueManager = new MQQueueManager(queueManagerName)
    this
  }

  Mqs withQueue(String queueName, QueueOptions queueOptions, Closure closure) {
    prepareQueue(queueName, queueOptions)
    try {
      closure.delegate = this
      closure.call(queue)
    } finally {
      closeQueue()
    }
    this
  }

  Mqs prepareQueue(String queueName, QueueOptions queueOptions) {
    queue = createQueue(queueName, queueOptions)
    this
  }

  MQQueue createQueue(String queueName, QueueOptions queueOptions) {
    queueManager.accessQueue(queueName, queueOptions.value)
  }

  Mqs closeQueue() {
    if (queue != null) {
      queue.close()
      queue = null
    }
    this
  }

  String receiveMessageByCorrelationId(String correlationId) {
    queueGetContent(new MqGetMessageOptionsBuilder().waitInterval(timeout).matchCorrelationId().create()) {
      MQMessage message ->
        message.correlationId = correlationId.bytes
    }
  }


  String receiveMessage() {
    queueGetContent(new MqGetMessageOptionsBuilder().waitInterval(timeout).create())
  }

  int foreachMessageReceived(Closure closure) {
    int count = 0
    queueGet(new MqGetMessageOptionsBuilder().waitInterval(timeout).create(), true) { message ->
      String content = getContent(message)
      closure.delegate = this
      closure.call(content)
      count++
    }
    count
  }

  def purgeQueue() {
    queueGet(new MqGetMessageOptionsBuilder().noWait().failIfQuiescing().create(), true) {
      // Nothing
    }
  }

  private queueGet(MQGetMessageOptions options, boolean loop = false, Closure closure) {
    try {
      while (loop) {
        MQMessage message = new MQMessage()
        queue.get(message, options)
        closure.delegate = this
        closure.call(message)
      }
    } catch (MQException ex) {
      if (!NoMoreMessagesException.matches(ex)) {
        throw new UnknownMqsException(ex)
      }
    }
  }

  private String queueGetContent(MQGetMessageOptions options) {
    queueGetContent(options) { }
  }

  private String queueGetContent(MQGetMessageOptions options, Closure closure) {
    try {
      MQMessage message = new MQMessage()
      closure.delegate = this
      closure.call(message)
      queue.get(message, options)
      getContent(message)
    } catch (MQException ex) {
      mapException(ex)
    }
  }

  def sendToQueue(String text) {
    sendToQueue(queue, text)
  }

  static sendToQueue(MQQueue sentQueue, String text) {
    sendToQueue(sentQueue, text, null)
  }

  def sendToQueue(String text, String correlationId) {
    sendToQueue(queue, text, correlationId)
  }

  static sendToQueue(MQQueue sentQeue, String text, String correlationId) {
    def message = new MQMessage()
    message.setVersion(MQConstants.MQMD_VERSION_2)
    message.format = MQConstants.MQFMT_STRING
    message.characterSet = 1208 // UTF-8
    if (correlationId) {
      message.correlationId = correlationId.bytes
    }
    message.writeString(text)
    sentQeue.put(message, new MQPutMessageOptions())
  }

  static String getContent(MQMessage msg) {
    int strLen = msg.messageLength
    byte[] strData = new byte[strLen]
    msg.readFully(strData)
    new String(strData)
  }

  def mapException(MQException ex) throws MqsException {
    if (NoMoreMessagesException.matches(ex)) {
      throw new NoMoreMessagesException(ex)
    }
    throw new UnknownMqsException(ex)
  }
}

