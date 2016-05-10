package io.durbs.movieratings

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.pool.KryoFactory
import com.esotericsoftware.kryo.pool.KryoPool
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import com.lambdaworks.redis.RedisClient
import com.lambdaworks.redis.api.rx.RedisReactiveCommands
import com.mongodb.ConnectionString
import com.mongodb.MongoClient
import com.mongodb.async.client.MongoClientSettings
import com.mongodb.client.model.IndexModel
import com.mongodb.client.model.IndexOptions
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
import io.durbs.movieratings.codec.ObjectIDSerializer
import io.durbs.movieratings.codec.mongo.MovieCodec
import io.durbs.movieratings.codec.mongo.RatingCodec
import io.durbs.movieratings.codec.mongo.UserCodec
import io.durbs.movieratings.codec.mongo.UserCodec as MongoUserCodec
import io.durbs.movieratings.codec.redis.RatedMovieCodec
import io.durbs.movieratings.codec.redis.UserCodec as RedisUserCodec
import io.durbs.movieratings.config.APIConfig
import io.durbs.movieratings.config.MongoConfig
import io.durbs.movieratings.config.RedisConfig
import io.durbs.movieratings.config.SecurityConfig
import io.durbs.movieratings.handling.MovieRestEndpoint
import io.durbs.movieratings.handling.auth.JWTTokenHandler
import io.durbs.movieratings.handling.auth.RegistrationHandler
import io.durbs.movieratings.handling.auth.LoginHandler
import io.durbs.movieratings.model.persistent.RatedMovie
import io.durbs.movieratings.model.persistent.User
import io.durbs.movieratings.services.AuthenticationService
import io.durbs.movieratings.services.MovieService
import org.bson.Document
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.configuration.CodecRegistry
import org.bson.types.ObjectId
import ratpack.config.ConfigData
import ratpack.service.Service
import ratpack.service.StartEvent
import ratpack.service.StopEvent
import rx.Observable
import rx.functions.Func3

@CompileStatic
@Slf4j
class MovieRatingsModule extends AbstractModule {

  @Override
  protected void configure() {

    // redis codecs
    bind(RatedMovieCodec)
    bind(RedisUserCodec)

    // handlers
    bind(JWTTokenHandler)
    bind(LoginHandler)
    bind(RegistrationHandler)

    // chain
    bind(MovieRestEndpoint)

    // services
    bind(AuthenticationService)
    bind(MovieService)

  }

  @Provides
  @Singleton
  ObjectMapper provideCustomObjectMapper() {

    new ObjectMapper().registerModule(new SimpleModule().addSerializer(ObjectId, new ObjectIDSerializer()))
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
  APIConfig provideAPIConfig(final ConfigData configData) {

    configData.get(APIConfig.CONFIG_ROOT, APIConfig)
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
  RedisReactiveCommands<String, User> userRedisCommands(
    final RedisConfig redisConfig,
    final RedisUserCodec userCodec) {

    final RedisClient redisClient = RedisClient.create(redisConfig.uri)
    redisClient.connect(userCodec).reactive()
  }

  @Provides
  @Singleton
  RedisReactiveCommands<String, RatedMovie> ratedMovieRedisCommands(
    final RedisConfig redisConfig,
    final RatedMovieCodec ratedMovieCodec) {

    final RedisClient redisClient = RedisClient.create(redisConfig.uri)
    redisClient.connect(ratedMovieCodec).reactive()
  }

  @Provides
  @Singleton
  MongoDatabase provideMongoDatabase(final MongoConfig mongoConfig) {

    final ConnectionString connectionString = new ConnectionString(mongoConfig.uri)

    final CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
      MongoClient.getDefaultCodecRegistry(),
      CodecRegistries.fromCodecs(new MovieCodec(), new RatingCodec(), new MongoUserCodec()))

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

  @Provides
  @Singleton
  public Service setup(final MongoDatabase mongoDatabase,
                       final RedisReactiveCommands<String, User> userRedisCommands,
                       final RedisReactiveCommands<String, RatedMovie> ratedMovieRedisCommands) {

    new Service() {

      @Override
      void onStart(StartEvent event) throws Exception {

        final Observable<String> userIndexes = mongoDatabase.getCollection(UserCodec.COLLETION_NAME)
          .createIndexes([new IndexModel(new Document(UserCodec.USERNAME_PROPERTY, 1))])

        final Observable<String> movieIndexes = mongoDatabase.getCollection(MovieCodec.COLLETION_NAME)
          .createIndexes([new IndexModel(new Document('$**', 'text'), new IndexOptions(weights: new Document().append(MovieCodec.NAME_PROPERTY, 1)))])

        final Observable<String> ratingIndexes = mongoDatabase.getCollection(RatingCodec.COLLETION_NAME)
          .createIndexes([new IndexModel(new Document(RatingCodec.MOVIE_ID_PROPERTY, 1).append(RatingCodec.USER_ID_PROPERTY, 1), new IndexOptions(unique: true))])

        Observable.zip(userIndexes, movieIndexes, ratingIndexes, { String userCollectionIndexNames,
          String movieCollectionIndexNames,
          String ratingCollectionIndexNames ->

        } as Func3).bindExec().subscribe()
      }

      @Override
      void onStop(StopEvent stopEvent) throws Exception {

        userRedisCommands.close()
        ratedMovieRedisCommands.close()
      }
    }
  }
}
