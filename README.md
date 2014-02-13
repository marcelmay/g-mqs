A WebSphere MQS Groovy wrapper
------------------------------

g-mqs provides a [Groovy](http://groovy.codehaus.org) DSL wrapper around
[WebSphere MQS](http://www.ibm.com/software/products/en/wmq) client API.

Groovy example
--------------
'''
new Mqs().hostname('localhost').port(1414).channel('TEST_CHANNEL').withQueueManager('test_qm') {
  String correlationId = 'some.correlation.id'
  withQueue(config.queueSend, Mqs.QueueOptions.SEND) {
    sendToQueue('My message', correlationId)
  }
  withQueue(config.queueSend, Mqs.QueueOptions.RECEIVE) {
    String response = receiveMessageByCorrelationId(correlationId)
    println('Received: '+ response)
  }
}
'''

Building from source
--------------------

* Gradle

  The project requires Gradle for building from source. If you do not have Gradle installed yet, have a look at
  the [Gradle homepage](http://gradle.org).

* Dependencies : Maven REPO vs. local

  The required WebSphere MQS JARs are commercially licensed by IBM and are therefore not available by public Maven repos.

  **Tip:** There's an evaluation version [available](http://www.ibm.com/software/products/en/wmq)!

  You'll have to spawn these to your (local) Maven repo using the Gradle helper:

  1. Go to the helper directory:  
    cd mqs-repo-spawning

  2. Copy the following list of MQS JARs into this directory:  
    com.ibm.mq.commonservices.jar
    com.ibm.mq.connector.jar
    com.ibm.mq.headers.jar
    com.ibm.mq.jar
    com.ibm.mq.jmqi.jar
    com.ibm.mq.jmqi.local.jar
    com.ibm.mq.jmqi.remote.jar
    com.ibm.mq.jmqi.system.jar
    com.ibm.mq.jms.admin.jar
    com.ibm.mq.pcf.jar
    com.ibm.mqetclient.jar
    com.ibm.mqjms.jar
    com.ibm.msg.client.commonservices.j2se.jar
    com.ibm.msg.client.commonservices.jar
    com.ibm.msg.client.jms.internal.jar
    com.ibm.msg.client.jms.jar
    com.ibm.msg.client.matchspace.jar
    com.ibm.msg.client.provider.jar
    com.ibm.msg.client.ref.jar
    com.ibm.msg.client.wmq.common.jar
    com.ibm.msg.client.wmq.factories.jar
    com.ibm.msg.client.wmq.jar
    com.ibm.msg.client.wmq.v6.jar
    dhbcore.jar

  3. Update the build.gradle (probably the MQS version, and maybe the local or remote repo)

  4. Run Gradle  
     By default this will spawn the JARs to your local repo.

    gradle uploadArchives

* Compiling and creating JARs

  To compile and create the JARs, run

    gradle clean install

