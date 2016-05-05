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
  public static final String DESCRIPTION_PROPERTY = 'description'
  public static final String IMAGE_URI_PROPERTY = 'imageURI'

  public static final String COLLETION_NAME = 'movie'

  static final Codec<Document> documentCodec = new DocumentCodec()

  @Override
  Movie decode(final BsonReader reader, final DecoderContext decoderContext) {

    final Document document = documentCodec.decode(reader, decoderContext)

    new Movie(
      id: document.getObjectId(DBCollection.ID_FIELD_NAME),
      name: document.getString(NAME_PROPERTY),
      description: document.getString(DESCRIPTION_PROPERTY),
      imageURI: document.getString(IMAGE_URI_PROPERTY))
  }

  @Override
  void encode(final BsonWriter writer, final Movie movie, final EncoderContext encoderContext) {

    final Document document = new Document()

    if (movie.id) {
      document.put(DBCollection.ID_FIELD_NAME, movie.id)
    }

    if (movie.name) {
      document.put(NAME_PROPERTY, movie.name)
    }

    if (movie.description) {
      document.put(DESCRIPTION_PROPERTY, movie.description)
    }

    if (movie.imageURI) {
      document.put(IMAGE_URI_PROPERTY, movie.imageURI)
    }

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
