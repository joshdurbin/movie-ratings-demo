package io.durbs.movieratings

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.pool.KryoFactory
import com.esotericsoftware.kryo.pool.KryoPool
import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import com.lambdaworks.redis.RedisClient
import com.lambdaworks.redis.api.rx.RedisReactiveCommands
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
import io.durbs.movieratings.codec.mongo.UserCodec as MongoUserCodec
import io.durbs.movieratings.codec.redis.UserCodec as RedisUserCodec
import io.durbs.movieratings.config.MongoConfig
import io.durbs.movieratings.config.RedisConfig
import io.durbs.movieratings.config.SecurityConfig
import io.durbs.movieratings.handler.auth.JWTTokenHandler
import io.durbs.movieratings.handler.auth.RegistrationHandler
import io.durbs.movieratings.handler.auth.LoginHandler
import io.durbs.movieratings.model.User
import io.durbs.movieratings.services.AuthenticationService
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.configuration.CodecRegistry
import ratpack.config.ConfigData

@CompileStatic
@Slf4j
class MovieRatingsModule extends AbstractModule {

  @Override
  protected void configure() {

    bind(RedisUserCodec)
    bind(JWTTokenHandler)
    bind(RegistrationHandler)
    bind(LoginHandler)
    bind(AuthenticationService)

  }

  @Provides
  @Singleton
  KryoPool provideKryoPool() {

    new KryoPool.Builder(new KryoFactory() {

      @Override
      Kryo create() {

        new Kryo()
      }
    }).softReferences().build()
  }

  @Provides
  @Singleton
  MongoConfig provideMongoConfig(final ConfigData configData) {

    configData.get(MongoConfig.CONFIG_ROOT, MongoConfig)
  }

  @Provides
  @Singleton
  RedisConfig provideRedisConfig(final ConfigData configData) {

    configData.get(RedisConfig.CONFIG_ROOT, RedisConfig)
  }

  @Provides
  @Singleton
  SecurityConfig provideSecurityConfig(final ConfigData configData) {

    configData.get(SecurityConfig.CONFIG_ROOT, SecurityConfig)
  }

  @Provides
  @Singleton
  RedisReactiveCommands<String, User> productRedisCommands(
    final RedisConfig redisConfig,
    final RedisUserCodec userCodec) {

    final RedisClient redisClient = RedisClient.create(redisConfig.uri)
    redisClient.connect(userCodec).reactive()
  }

  @Provides
  @Singleton
  MongoDatabase provideMongoDatabase(final MongoConfig mongoConfig) {

    final ConnectionString connectionString = new ConnectionString(mongoConfig.uri)

    final CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
      MongoClient.getDefaultCodecRegistry(),
      CodecRegistries.fromCodecs(new MongoUserCodec()))

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
