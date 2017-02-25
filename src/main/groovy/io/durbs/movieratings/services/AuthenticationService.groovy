package io.durbs.movieratings.services

import com.google.inject.Inject
import com.google.inject.Singleton
import com.lambdaworks.redis.SetArgs
import com.lambdaworks.redis.api.rx.RedisReactiveCommands
import com.mongodb.DBCollection
import com.mongodb.client.model.Updates
import com.mongodb.rx.client.MongoDatabase
import com.mongodb.rx.client.Success
import de.qaware.heimdall.PasswordFactory
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.durbs.movieratings.Constants
import io.durbs.movieratings.MovieRatingsConfig
import io.durbs.movieratings.codec.mongo.UserCodec
import io.durbs.movieratings.model.User
import io.jsonwebtoken.Header
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.bson.types.ObjectId
import ratpack.exec.Blocking
import rx.Observable
import rx.functions.Func1

import java.time.LocalDateTime
import java.time.ZoneOffset

import static com.mongodb.client.model.Filters.eq

@Singleton
@CompileStatic
@Slf4j
class AuthenticationService {

  @Inject
  RedisReactiveCommands<String, User> userRedisCommands

  @Inject
  MovieRatingsConfig movieRatingsConfig

  @Inject
  MongoDatabase mongoDatabase

  private Func1 USER_TO_JWT = { User user ->

    Jwts
      .builder()
      .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
      .setIssuedAt(new Date())
      .setExpiration(Date.from(LocalDateTime.now().plusHours(movieRatingsConfig.jwtTokenTTLInHours).toInstant(ZoneOffset.UTC)))
      .claim(Constants.JWT_USER_OBJECT_ID_CLAIM, user.id.toString())
      .signWith(SignatureAlgorithm.HS512, movieRatingsConfig.jwtSigningKey)
      .compact()
  } as Func1

  static String getUserCacheKey(String userId) {

    "${Constants.REDIS_USER_KEY_PREFIX}-${userId}"
  }

  Observable<String> createAccount(User userToInsert) {

    userToInsert.setPassword(PasswordFactory.create().hash(userToInsert.password))

    Observable<User> userInsertionObservable = mongoDatabase.getCollection(UserCodec.COLLECTION_NAME, User)
      .insertOne(userToInsert)
      .flatMap({ Success success ->

      mongoDatabase.getCollection(UserCodec.COLLECTION_NAME, User)
        .find(eq(UserCodec.USERNAME_PROPERTY, userToInsert.username))
        .toObservable()
    } as Func1)

    mongoDatabase.getCollection(UserCodec.COLLECTION_NAME, User)
      .find(eq(UserCodec.USERNAME_PROPERTY, userToInsert))
      .toObservable()
      .switchIfEmpty(userInsertionObservable)
      .map(USER_TO_JWT)
      .bindExec()
  }

  Observable<String> authenticate(String username, String password) {

    mongoDatabase.getCollection(UserCodec.COLLECTION_NAME, User)
      .find(eq(UserCodec.USERNAME_PROPERTY, username))
      .toObservable()
      .filter { User user ->

      PasswordFactory.create().verify(password, user.password)
    }.doOnNext { User user ->

      if (PasswordFactory.create().needsRehash(user.password)) {

        log.info("Rehashing password for username '${username}'")

        mongoDatabase.getCollection(UserCodec.COLLECTION_NAME, User)
          .updateOne(eq(UserCodec.USERNAME_PROPERTY, username), Updates.set(UserCodec.PASSWORD_PROPERTY, PasswordFactory.create().hash(password)))
          .subscribe()
      }

    }.map(USER_TO_JWT)
      .bindExec()
  }

  Observable<User> validateToken(String token) {

    Blocking.get {

      Jwts
        .parser()
        .setSigningKey(movieRatingsConfig.jwtSigningKey)
        .parseClaimsJws(token)
        .body.get(Constants.JWT_USER_OBJECT_ID_CLAIM)
    }
    .observe()
      .flatMap({ String id ->

      userRedisCommands.get(getUserCacheKey(id))
        .switchIfEmpty(
        mongoDatabase.getCollection(UserCodec.COLLECTION_NAME, User)
          .find(eq(DBCollection.ID_FIELD_NAME, new ObjectId(id)))
          .toObservable()
          .doOnNext { User user ->

          userRedisCommands.set(getUserCacheKey(id), user, SetArgs.Builder.ex(movieRatingsConfig.userCacheTTLInSeconds)).subscribe()
        }
      )
    } as Func1)
      .bindExec()
  }
}
