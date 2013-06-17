package messaging

class MqsTest extends GroovyTestCase {
  ConfigObject config

  @Override
  protected void setUp() {
    super.setUp()

    config = new ConfigObject()
    config.hostname = 'localhost'
    config.port = 1414
    config.channel = 'sample-channel'
  }


  void testConfigureWithConfigObject() {
    def mqs = new Mqs().configuredBy(config)
    assert mqs.hostname == config.hostname
    assert mqs.port == config.port
    assert mqs.channel == config.channel
  }

  void testConfigureDirect() {
    def mqs = new Mqs().hostname(config.hostname).port(config.port).channel(config.channel)
    assert mqs.hostname == config.hostname
    assert mqs.port == config.port
    assert mqs.channel == config.channel
  }
}
