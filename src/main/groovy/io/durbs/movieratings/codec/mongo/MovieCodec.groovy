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

  public static final String NAME_PROPERTY = 'name'
  public static final String IMDB_ID_PROPERTY = 'imdbId'
  public static final String DESCRIPTION_PROPERTY = 'description'
  public static final String POSTER_IMAGE_URI_PROPERTY = 'posterImageURI'
  public static final String YEAR_RELEASED_PROPERTY = 'yearReleased'
  public static final String GENRE_PROPERTY = 'genre'
  public static final String ACTORS_PROPERTY = 'actors'
  public static final String DIRECTOR_PROPERTY = 'director'

  public static final String COLLECTION_NAME = 'movie'

  static final Codec<Document> documentCodec = new DocumentCodec()

  @Override
  Movie decode(final BsonReader reader, final DecoderContext decoderContext) {

    final Document document = documentCodec.decode(reader, decoderContext)

    new Movie(
      id: document.getObjectId(DBCollection.ID_FIELD_NAME),
      name: document.getString(NAME_PROPERTY),
      imdbId: document.getString(IMDB_ID_PROPERTY),
      description: document.getString(DESCRIPTION_PROPERTY),
      posterImageURI: document.getString(POSTER_IMAGE_URI_PROPERTY),
      yearReleased: document.getInteger(YEAR_RELEASED_PROPERTY),
      genre: document.get(GENRE_PROPERTY, List),
      actors: document.get(ACTORS_PROPERTY, List),
      director: document.getString(DIRECTOR_PROPERTY))
  }

  @Override
  void encode(final BsonWriter writer, final Movie movie, final EncoderContext encoderContext) {

    final Document document = new Document()

    document.put(DBCollection.ID_FIELD_NAME, movie.id)
    document.put(NAME_PROPERTY, movie.name)
    document.put(IMDB_ID_PROPERTY, movie.imdbId)
    document.put(DESCRIPTION_PROPERTY, movie.description)
    document.put(POSTER_IMAGE_URI_PROPERTY, movie.posterImageURI)
    document.put(YEAR_RELEASED_PROPERTY, movie.yearReleased)
    document.put(GENRE_PROPERTY, movie.genre)
    document.put(ACTORS_PROPERTY, movie.actors)
    document.put(DIRECTOR_PROPERTY, movie.director)

    documentCodec.encode(writer, document, encoderContext)
  }

  @Override
  Class<Movie> getEncoderClass() {
    Movie
  }

  @Override
  Movie generateIdIfAbsentFromDocument(final Movie movie) {

    if (!documentHasId(movie)) {
      movie.setId(new ObjectId())
    }

    movie
  }

  @Override
  boolean documentHasId(final Movie movie) {
    movie.id
  }

  @Override
  BsonValue getDocumentId(final Movie movie) {

    if (!documentHasId(movie)) {
      throw new IllegalStateException("The movie does not contain an ${DBCollection.ID_FIELD_NAME}")
    }

    new BsonString(movie.id as String)
  }
}
