package io.durbs.movieratings.services

import com.google.common.base.Splitter
import com.google.inject.Inject
import com.google.inject.Singleton
import com.netflix.hystrix.HystrixCommandGroupKey
import com.netflix.hystrix.HystrixCommandKey
import com.netflix.hystrix.HystrixObservableCommand
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.durbs.movieratings.Constants
import io.durbs.movieratings.model.ExternalRating
import io.durbs.movieratings.model.Movie
import ratpack.http.client.HttpClient
import ratpack.http.client.ReceivedResponse
import rx.Observable

import java.text.NumberFormat

import static ratpack.rx.RxRatpack.observe

@Singleton
@CompileStatic
@Slf4j
class OMDBService {

  static final ExternalRating NO_EXTERNAL_RATING = new ExternalRating(imdbRating: 0.0,
    totalImdbRatings: 0,
    rottenTomatoRating: 0.0,
    totalRottenTomatoRatings: 0,
    rottenTomatoUserRating: 0.0,
    totalRottenTomatoUserRatings: 0,
    metascore: 0)

  static final String OMDB_HYSTRIX_COMMAND_GROUND_KEY = 'omdb-service'

  @Inject
  HttpClient httpClient

  static final Splitter COMMA_SPLITTER = Splitter.on(Constants.OMDB_DEFAULT_DELIMITER).trimResults().omitEmptyStrings()

  Observable<Movie> getOMDBMovie(final String imdbId) {

    new HystrixObservableCommand<Movie>(
      HystrixObservableCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(OMDB_HYSTRIX_COMMAND_GROUND_KEY))
        .andCommandKey(HystrixCommandKey.Factory.asKey('getOMDBMovie'))) {

      @Override
      protected Observable<Movie> construct() {

        final String endpoint = "http://www.omdbapi.com/?i=${imdbId}&plot=full"
        final URI endpointURI = endpoint.toURI()

        log.trace("Opening a GET request to ${endpoint}...")

        observe(httpClient.get(endpointURI)).map { final ReceivedResponse receivedResponse ->

          final JsonSlurper slurper = new JsonSlurper()

          final Object parsedObject = slurper.parse(receivedResponse.body.inputStream)
          final Map<String, String> parsedObjectMap = parsedObject as Map

          new Movie(name: parsedObjectMap.get('Title'),
            imdbId: parsedObjectMap.get('imdbID'),
            description: parsedObjectMap.get('Plot'),
            posterImageURI: parsedObjectMap.get('Poster'),
            yearReleased: parsedObjectMap.get('Year') as Integer,
            genre: COMMA_SPLITTER.splitToList(parsedObjectMap.get('Genre')),
            actors: COMMA_SPLITTER.splitToList(parsedObjectMap.get('Actors')),
            directors: COMMA_SPLITTER.splitToList(parsedObjectMap.get('Director')),
            writers: COMMA_SPLITTER.splitToList(parsedObjectMap.get('Writer')),
            languages: COMMA_SPLITTER.splitToList(parsedObjectMap.get('Language')),
            mpaaRating: parsedObjectMap.get('Rated'),
            awards: parsedObjectMap.get('Awards'))
        }.defaultIfEmpty(null)
      }

      @Override
      protected Observable<Movie> resumeWithFallback() {
        Observable.empty()
      }
    }.toObservable()
    .bindExec()
  }

  Observable<ExternalRating> getOMDBExternalRatingForMovie(final String imdbId) {

    new HystrixObservableCommand<ExternalRating>(
      HystrixObservableCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(OMDB_HYSTRIX_COMMAND_GROUND_KEY))
        .andCommandKey(HystrixCommandKey.Factory.asKey('getOMDBExternalRatingForMovie'))) {

      @Override
      protected Observable<ExternalRating> construct() {

        final String endpoint = "http://www.omdbapi.com/?i=${imdbId}&tomatoes=true"
        final URI endpointURI = endpoint.toURI()

        log.trace("Opening a GET request to ${endpoint}...")

        observe(httpClient.get(endpointURI)).map { final ReceivedResponse receivedResponse ->

          final JsonSlurper slurper = new JsonSlurper()

          final Object parsedObject = slurper.parse(receivedResponse.body.inputStream)
          final Map<String, String> parsedObjectMap = parsedObject as Map

          final Number totalImdbRatings = NumberFormat.getInstance(Locale.US).parse(parsedObjectMap.get('imdbVotes') as String)

          new ExternalRating(imdbRating: parsedObjectMap.get('imdbRating') as Double,
            totalImdbRatings: totalImdbRatings.intValue(),
            rottenTomatoRating: parsedObjectMap.get('tomatoRating') as Double,
            totalRottenTomatoRatings: parsedObjectMap.get('tomatoReviews') as Integer,
            rottenTomatoUserRating: parsedObjectMap.get('tomatoUserRating') as Double,
            totalRottenTomatoUserRatings: parsedObjectMap.get('tomatoUserReviews') as Integer,
            metascore: parsedObjectMap.get('Metascore') as Integer)
        }.defaultIfEmpty(NO_EXTERNAL_RATING)
      }

      @Override
      protected Observable<ExternalRating> resumeWithFallback() {
        Observable.just(NO_EXTERNAL_RATING)
      }
    }.toObservable()
    .bindExec()
  }
}
