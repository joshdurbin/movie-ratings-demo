package io.durbs.movieratings.services

import com.google.inject.Inject
import com.google.inject.Singleton
import com.lambdaworks.redis.api.rx.RedisReactiveCommands
import com.mongodb.DBCollection
import com.mongodb.client.model.Updates
import com.mongodb.rx.client.MongoDatabase
import com.mongodb.rx.client.Success
import de.qaware.heimdall.PasswordFactory
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.durbs.movieratings.codec.mongo.UserCodec
import io.durbs.movieratings.config.SecurityConfig
import io.durbs.movieratings.model.User
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

  static final String USER_HASH_KEY = 'user'

  @Inject
  RedisReactiveCommands<String, User> userRedisCommands

  @Inject
  SecurityConfig securityConfig

  @Inject
  MongoDatabase mongoDatabase

  /**
   *
   * @param username
   * @param password
   * @param emailAddress
   * @return JWT token or error
   */
  Observable<String> createAccount(final String username, final String password, final String emailAddress) {

    final Observable<User> userInsertionAndQueryObservable = mongoDatabase.getCollection(UserCodec.COLLETION_NAME, User)
      .insertOne(new User(
      username: username,
      password: PasswordFactory.create().hash(password),
      emailAddress: emailAddress))
      .flatMap({ final Success success ->

      mongoDatabase.getCollection(UserCodec.COLLETION_NAME, User)
        .find(eq(UserCodec.USERNAME_PROPERTY, username))
        .toObservable()
      } as Func1)

    mongoDatabase.getCollection(UserCodec.COLLETION_NAME, User)
      .find(eq(UserCodec.USERNAME_PROPERTY, username))
      .toObservable()
      .switchIfEmpty(userInsertionAndQueryObservable)
      .map({ final User retrievedUser ->

        Jwts
          .builder()
          .setId(retrievedUser.id.toString())
          .setIssuedAt(new Date())
          .signWith(SignatureAlgorithm.HS512, securityConfig.jwtSigningKey)
          .setExpiration(Date.from(LocalDateTime.now().plusHours(securityConfig.jwtTokenTTLInHours).toInstant(ZoneOffset.UTC)))
          .compact()
      } as Func1)
      .bindExec()
  }

  /**
   *
   * @param username
   * @param password
   * @return JWT token or error
   */
  Observable<String> authenticate(final String username, final String password) {

    mongoDatabase.getCollection(UserCodec.COLLETION_NAME, User)
      .find(eq(UserCodec.USERNAME_PROPERTY, username))
      .toObservable()
      .filter { final User user ->

        PasswordFactory.create().verify(password, user.password)
      }.doOnNext { final User user ->

        if (PasswordFactory.create().needsRehash(user.password)) {

          log.info("Rehashing password for username '${username}'")

          mongoDatabase.getCollection(UserCodec.COLLETION_NAME, User)
            .updateOne(eq(UserCodec.USERNAME_PROPERTY, username), Updates.set(UserCodec.PASSWORD_PROPERTY, PasswordFactory.create().hash(password)))
            .subscribe()
        }

      }.map { final User user ->

        Jwts
          .builder()
          .setId(user.id.toString())
          .setIssuedAt(new Date())
          .signWith(SignatureAlgorithm.HS512, securityConfig.jwtSigningKey)
          .setExpiration(Date.from(LocalDateTime.now().plusHours(securityConfig.jwtTokenTTLInHours).toInstant(ZoneOffset.UTC)))
          .compact()
      }
      .bindExec()
  }

  Observable<User> validateToken(final String token) {

    Blocking.get {

      Jwts
        .parser()
        .setSigningKey(securityConfig.jwtSigningKey)
        .parseClaimsJws(token)
        .body.get('jti')
    }.observe().flatMap( { final String id ->

      userRedisCommands.hget(USER_HASH_KEY, id)
        .switchIfEmpty(
          mongoDatabase.getCollection(UserCodec.COLLETION_NAME, User)
            .find(eq(DBCollection.ID_FIELD_NAME, new ObjectId(id)))
            .toObservable()
            .doOnNext { final User user ->

               userRedisCommands.hset(USER_HASH_KEY, user.id.toString(), user).subscribe()
          }
      )
    } as Func1)
    .bindExec()
  }
}
