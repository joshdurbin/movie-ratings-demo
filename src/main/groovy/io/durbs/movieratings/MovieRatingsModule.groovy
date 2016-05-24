package io.durbs.movieratings

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.pool.KryoFactory
import com.esotericsoftware.kryo.pool.KryoPool
import com.fasterxml.jackson.annotation.JsonInclude
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
import io.durbs.movieratings.codec.KryoRedisCodec
import io.durbs.movieratings.codec.ObjectIDSerializer
import io.durbs.movieratings.codec.mongo.MovieCodec
import io.durbs.movieratings.codec.mongo.RatingCodec
import io.durbs.movieratings.codec.mongo.UserCodec
import io.durbs.movieratings.handling.chainaction.MovieRestEndpoint
import io.durbs.movieratings.handling.handler.ErrorHandler
import io.durbs.movieratings.handling.handler.JWTTokenHandler
import io.durbs.movieratings.handling.handler.LoginHandler
import io.durbs.movieratings.handling.handler.MovieIDExtractionAndVerificationHandler
import io.durbs.movieratings.handling.handler.RegistrationHandler
import io.durbs.movieratings.model.ComputedUserRating
import io.durbs.movieratings.model.ExternalRating
import io.durbs.movieratings.model.User
import io.durbs.movieratings.services.AuthenticationService
import io.durbs.movieratings.services.MovieService
import io.durbs.movieratings.services.OMDBService
import io.durbs.movieratings.services.RatingService
import org.bson.Document
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.configuration.CodecRegistry
import org.bson.types.ObjectId
import ratpack.config.ConfigData
import ratpack.error.ServerErrorHandler
import ratpack.service.Service
import ratpack.service.StartEvent
import ratpack.service.StopEvent
import rx.Observable
import rx.functions.Func3

import javax.validation.Validation
import javax.validation.Validator

@CompileStatic
@Slf4j
class MovieRatingsModule extends AbstractModule {

  @Override
  protected void configure() {

    // handlers
    bind(JWTTokenHandler)
    bind(LoginHandler)
    bind(MovieIDExtractionAndVerificationHandler)
    bind(RegistrationHandler)
    bind(ServerErrorHandler).to(ErrorHandler)

    // chain
    bind(MovieRestEndpoint)

    // services
    bind(AuthenticationService)
    bind(MovieService)
    bind(OMDBService)
    bind(RatingService)
  }

  @Provides
  @Singleton
  Validator provideValidator() {
    Validation.buildDefaultValidatorFactory().validator
  }

  @Provides
  @Singleton
  ObjectMapper provideCustomObjectMapper() {

    new ObjectMapper()
      .registerModule(new SimpleModule().addSerializer(ObjectId, new ObjectIDSerializer()))
      .setSerializationInclusion(JsonInclude.Include.NON_NULL)
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
  MovieRatingsConfig provideAPIConfig(final ConfigData configData) {

    configData.get(MovieRatingsConfig)
  }

  @Provides
  @Singleton
  RedisReactiveCommands<String, User> userRedisCommands(
    final MovieRatingsConfig config,
    final KryoPool kryoPool) {

    final RedisClient redisClient = RedisClient.create(config.redisURI)
    redisClient.connect(new KryoRedisCodec<User>(User, kryoPool)).reactive()
  }

  @Provides
  @Singleton
  RedisReactiveCommands<String, ComputedUserRating> computedRatingCommands(
    final MovieRatingsConfig config,
    final KryoPool kryoPool) {

    final RedisClient redisClient = RedisClient.create(config.redisURI)
    redisClient.connect(new KryoRedisCodec<ComputedUserRating>(ComputedUserRating, kryoPool)).reactive()
  }

  @Provides
  @Singleton
  RedisReactiveCommands<String, ExternalRating> externalRatingCommands(
    final MovieRatingsConfig config,
    final KryoPool kryoPool) {

    final RedisClient redisClient = RedisClient.create(config.redisURI)
    redisClient.connect(new KryoRedisCodec<ExternalRating>(ExternalRating, kryoPool)).reactive()
  }

  @Provides
  @Singleton
  MongoDatabase provideMongoDatabase(final MovieRatingsConfig config) {

    final ConnectionString connectionString = new ConnectionString(config.mongoURI)

    final CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
      MongoClient.getDefaultCodecRegistry(),
      CodecRegistries.fromCodecs(new MovieCodec(), new RatingCodec(), new UserCodec()))

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

    MongoClients.create(mongoClientSettingsBuidler.build()).getDatabase(config.mongoDb)
  }

  @Provides
  @Singleton
  public Service establishMongoIndexes(final MongoDatabase mongoDatabase,
                                       final RedisReactiveCommands<String, ComputedUserRating> computedRatingCommands,
                                       final RedisReactiveCommands<String, ExternalRating> externalRatingsCommands,
                                       final RedisReactiveCommands<String, User> userRatingsCommand) {

    new Service() {

      @Override
      void onStart(StartEvent event) throws Exception {

        final Observable<String> userIndexes = mongoDatabase.getCollection(UserCodec.COLLECTION_NAME)
          .createIndexes([new IndexModel(new Document(UserCodec.USERNAME_PROPERTY, 1), new IndexOptions(unique: true))])

        final Observable<String> movieIndexes = mongoDatabase.getCollection(MovieCodec.COLLECTION_NAME)
          .createIndexes([
          new IndexModel(new Document('$**', 'text'), new IndexOptions(weights: new Document().append(MovieCodec.NAME_PROPERTY, 1))),
          new IndexModel(new Document(MovieCodec.IMDB_ID_PROPERTY, 1), new IndexOptions(unique: true))])

        final Observable<String> ratingIndexes = mongoDatabase.getCollection(RatingCodec.COLLECTION_NAME)
          .createIndexes([new IndexModel(new Document(RatingCodec.MOVIE_ID_PROPERTY, 1).append(RatingCodec.USER_ID_PROPERTY, 1), new IndexOptions(unique: true))])

        Observable.zip(userIndexes, movieIndexes, ratingIndexes, { String userCollectionIndexNames,
                                                                   String movieCollectionIndexNames,
                                                                   String ratingCollectionIndexNames ->

        } as Func3).bindExec().subscribe()
      }

      @Override
      void onStop(StopEvent stopEvent) throws Exception {

        computedRatingCommands.close()
        externalRatingsCommands.close()
        userRatingsCommand.close()
      }
    }
  }
}
