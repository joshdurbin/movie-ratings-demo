package io.durbs.movieratings

import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import com.mongodb.ConnectionString
import com.mongodb.MongoClient
import com.mongodb.async.client.MongoClientSettings
import com.mongodb.connection.ClusterSettings
import com.mongodb.connection.ConnectionPoolSettings
import com.mongodb.connection.ServerSettings
import com.mongodb.connection.SocketSettings
import com.mongodb.connection.SslSettings
import com.mongodb.connection.netty.NettyStreamFactoryFactory
import com.mongodb.rx.client.MongoClients
import com.mongodb.rx.client.MongoDatabase
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.durbs.movieratings.codec.UserCodec
import io.durbs.movieratings.config.MongoConfig
import io.durbs.movieratings.config.SecurityConfig
import io.durbs.movieratings.handler.CreateAccountHandler
import io.durbs.movieratings.handler.LoginHandler
import io.durbs.movieratings.services.AuthenticationService
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.configuration.CodecRegistry
import ratpack.config.ConfigData

@CompileStatic
@Slf4j
class MovieRatingsModule extends AbstractModule {

  @Override
  protected void configure() {

    bind(CreateAccountHandler)
    bind(LoginHandler)
    bind(AuthenticationService)

  }

  @Provides
  @Singleton
  MongoConfig provideMongoConfig(final ConfigData configData) {

    configData.get(MongoConfig.CONFIG_ROOT, MongoConfig)
  }

  @Provides
  @Singleton
  SecurityConfig provideSecurityConfig(final ConfigData configData) {

    configData.get(SecurityConfig.CONFIG_ROOT, SecurityConfig)
  }

  @Provides
  @Singleton
  MongoDatabase provideMongoDatabase(final MongoConfig mongoConfig) {

    final ConnectionString connectionString = new ConnectionString(mongoConfig.uri)

    final CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
      MongoClient.getDefaultCodecRegistry(),
      CodecRegistries.fromCodecs(new UserCodec()))

    MongoClientSettings.Builder mongoClientSettingsBuidler = MongoClientSettings.builder()
      .codecRegistry(codecRegistry)
      .clusterSettings(ClusterSettings.builder().applyConnectionString(connectionString).build())
      .connectionPoolSettings(ConnectionPoolSettings.builder().applyConnectionString(connectionString).build())
      .serverSettings(ServerSettings.builder().build()).credentialList(connectionString.getCredentialList())
      .sslSettings(SslSettings.builder().applyConnectionString(connectionString).build())
      .socketSettings(SocketSettings.builder().applyConnectionString(connectionString).build())

    if (SslSettings.builder().applyConnectionString(connectionString).build().enabled) {
      mongoClientSettingsBuidler.streamFactoryFactory(new NettyStreamFactoryFactory())
    }

    MongoClients.create(mongoClientSettingsBuidler.build()).getDatabase(mongoConfig.db)
  }
}
