package io.durbs.movieratings.codec.mongo

import com.mongodb.DBCollection
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.durbs.movieratings.model.Movie
import org.bson.BsonReader
import org.bson.BsonString
import org.bson.BsonValue
import org.bson.BsonWriter
import org.bson.Document
import org.bson.codecs.Codec
import org.bson.codecs.CollectibleCodec
import org.bson.codecs.DecoderContext
import org.bson.codecs.DocumentCodec
import org.bson.codecs.EncoderContext
import org.bson.types.ObjectId

@CompileStatic
@Slf4j
class MovieCodec implements CollectibleCodec<Movie> {

  public static String NAME_PROPERTY = 'name'
  public static String IMDB_ID_PROPERTY = 'imdbId'
  public static String DESCRIPTION_PROPERTY = 'description'
  public static String POSTER_IMAGE_URI_PROPERTY = 'posterImageURI'
  public static String YEAR_RELEASED_PROPERTY = 'yearReleased'
  public static String GENRE_PROPERTY = 'genre'
  public static String ACTORS_PROPERTY = 'actors'
  public static String DIRECTORS_PROPERTY = 'directors'
  public static String WRITERS_PROPERTY = 'writers'
  public static String LANGUAGES_PROPERTY = 'languages'
  public static String MPAA_RATING_PROPERTY = 'mpaaRating'
  public static String AWARDS_PROPERTY = 'awards'

  public static String COLLECTION_NAME = 'movie'

  static Codec<Document> documentCodec = new DocumentCodec()

  @Override
  Movie decode(BsonReader reader, DecoderContext decoderContext) {

    Document document = documentCodec.decode(reader, decoderContext)

    new Movie(
      id: document.getObjectId(DBCollection.ID_FIELD_NAME),
      name: document.getString(NAME_PROPERTY),
      imdbId: document.getString(IMDB_ID_PROPERTY),
      description: document.getString(DESCRIPTION_PROPERTY),
      posterImageURI: document.getString(POSTER_IMAGE_URI_PROPERTY),
      yearReleased: document.getInteger(YEAR_RELEASED_PROPERTY),
      genre: document.get(GENRE_PROPERTY, List),
      actors: document.get(ACTORS_PROPERTY, List),
      directors: document.get(DIRECTORS_PROPERTY, List),
      writers: document.get(WRITERS_PROPERTY, List),
      languages: document.get(LANGUAGES_PROPERTY, List),
      mpaaRating: document.getString(MPAA_RATING_PROPERTY),
      awards: document.getString(AWARDS_PROPERTY),)
  }

  @Override
  void encode(BsonWriter writer, Movie movie, EncoderContext encoderContext) {

    Document document = new Document()

    document.put(DBCollection.ID_FIELD_NAME, movie.id)

    if (movie.name) {
      document.put(NAME_PROPERTY, movie.name)
    }

    if (movie.imdbId) {
      document.put(IMDB_ID_PROPERTY, movie.imdbId)
    }

    if (movie.description) {
      document.put(DESCRIPTION_PROPERTY, movie.description)
    }

    if (movie.posterImageURI) {
      document.put(POSTER_IMAGE_URI_PROPERTY, movie.posterImageURI)
    }

    if (movie.yearReleased) {
      document.put(YEAR_RELEASED_PROPERTY, movie.yearReleased)
    }

    if (movie.genre) {
      document.put(GENRE_PROPERTY, movie.genre)
    }

    if (movie.actors) {
      document.put(ACTORS_PROPERTY, movie.actors)
    }

    if (movie.directors) {
      document.put(DIRECTORS_PROPERTY, movie.directors)
    }

    if (movie.writers) {
      document.put(WRITERS_PROPERTY, movie.writers)
    }

    if (movie.languages) {
      document.put(LANGUAGES_PROPERTY, movie.languages)
    }

    if (movie.mpaaRating) {
      document.put(MPAA_RATING_PROPERTY, movie.mpaaRating)
    }

    if (movie.awards) {
      document.put(AWARDS_PROPERTY, movie.awards)
    }

    documentCodec.encode(writer, document, encoderContext)
  }

  @Override
  Class<Movie> getEncoderClass() {
    Movie
  }

  @Override
  Movie generateIdIfAbsentFromDocument(Movie movie) {

    if (!documentHasId(movie)) {
      movie.setId(new ObjectId())
    }

    movie
  }

  @Override
  boolean documentHasId(Movie movie) {
    movie.id
  }

  @Override
  BsonValue getDocumentId(Movie movie) {

    if (!documentHasId(movie)) {
      throw new IllegalStateException("The movie does not contain an ${DBCollection.ID_FIELD_NAME}")
    }

    new BsonString(movie.id as String)
  }
}
