package io.durbs.movieratings.codec.mongo

import com.mongodb.DBCollection
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.durbs.movieratings.model.Rating
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
class RatingCodec implements CollectibleCodec<Rating> {

  public static final String USER_ID_PROPERTY = 'userId'
  public static final String MOVIE_ID_PROPERTY = 'movieId'
  public static final String RATING_PROPERTY = 'rating'
  public static final String COMMENT_PROPERTY = 'comment'

  public static final String COLLETION_NAME = 'rating'

  static final Codec<Document> documentCodec = new DocumentCodec()

  @Override
  Rating decode(final BsonReader reader, final DecoderContext decoderContext) {

    final Document document = documentCodec.decode(reader, decoderContext)

    new Rating(
      id: document.getObjectId(DBCollection.ID_FIELD_NAME),
      userId: document.getObjectId(USER_ID_PROPERTY),
      movieId: document.getObjectId(MOVIE_ID_PROPERTY),
      rating: document.getInteger(RATING_PROPERTY),
      comment: document.getString(COMMENT_PROPERTY))
  }

  @Override
  void encode(final BsonWriter writer, final Rating rating, final EncoderContext encoderContext) {

    final Document document = new Document()

    if (rating.id) {
      document.put(DBCollection.ID_FIELD_NAME, rating.id)
    }

    if (rating.userId) {
      document.put(USER_ID_PROPERTY, rating.userId)
    }

    if (rating.movieId) {
      document.put(MOVIE_ID_PROPERTY, rating.movieId)
    }

    if (rating.rating) {
      document.put(RATING_PROPERTY, rating.rating)
    }

    if (rating.comment) {
      document.put(COMMENT_PROPERTY, rating.comment)
    }

    documentCodec.encode(writer, document, encoderContext)
  }

  @Override
  Class<Rating> getEncoderClass() {
    Rating
  }

  @Override
  Rating generateIdIfAbsentFromDocument(final Rating rating) {

    if (!documentHasId(rating)) {
      rating.setId(new ObjectId())
    }

    rating
  }

  @Override
  boolean documentHasId(final Rating rating) {
    rating.id
  }

  @Override
  BsonValue getDocumentId(final Rating rating) {

    if (!documentHasId(rating)) {
      throw new IllegalStateException("The rating does not contain an ${DBCollection.ID_FIELD_NAME}")
    }

    new BsonString(rating.id as String)
  }
}
