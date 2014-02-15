@Grab('de.m3y.gmqs:g-mqs:1.0-SNAPSHOT')

import de.m3y.gmqs.Mqs

new Mqs().hostname('localhost').port(1414).channel('TEST_CHANNEL').withQueueManager('test_qm') {
    String correlationId = 'some.correlation.id'
    withQueue('myQueue', Mqs.QueueOptions.SEND) {
        println 'Sending ...'
        sendToQueue('My message', correlationId)
    }
    withQueue('myQueue', Mqs.QueueOptions.RECEIVE) {
        timeout(2000)
        println 'Receiving ...'
        String response = receiveMessageByCorrelationId(correlationId)
        println('Received: ' + response)
    }
}
