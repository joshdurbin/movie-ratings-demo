package io.durbs.movieratings.codec.mongo

import com.mongodb.DBCollection
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.durbs.movieratings.model.User
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
class UserCodec implements CollectibleCodec<User> {

  public static final String USERNAME_PROPERTY = 'username'
  public static final String NAME_PROPERTY = 'name'
  public static final String PASSWORD_PROPERTY = 'password'
  public static final String EMAIL_ADDRESS_PROPERTY = 'emailAddress'

  public static final String COLLECTION_NAME = 'user'

  static final Codec<Document> documentCodec = new DocumentCodec()

  @Override
  User decode(final BsonReader reader, final DecoderContext decoderContext) {

    final Document document = documentCodec.decode(reader, decoderContext)

    new User(
      id: document.getObjectId(DBCollection.ID_FIELD_NAME),
      username: document.getString(USERNAME_PROPERTY),
      name: document.getString(NAME_PROPERTY),
      password: document.getString(PASSWORD_PROPERTY),
      emailAddress: document.getString(EMAIL_ADDRESS_PROPERTY))
  }

  @Override
  void encode(final BsonWriter writer, final User user, final EncoderContext encoderContext) {

    final Document document = new Document()

    if (user.id) {
      document.put(DBCollection.ID_FIELD_NAME, user.id)
    }

    if (user.username) {
      document.put(USERNAME_PROPERTY, user.username)
    }

    if (user.name) {
      document.put(NAME_PROPERTY, user.name)
    }

    if (user.password) {
      document.put(PASSWORD_PROPERTY, user.password)
    }

    if (user.emailAddress) {
      document.put(EMAIL_ADDRESS_PROPERTY, user.emailAddress)
    }

    documentCodec.encode(writer, document, encoderContext)
  }

  @Override
  Class<User> getEncoderClass() {
    User
  }

  @Override
  User generateIdIfAbsentFromDocument(final User user) {

    if (!documentHasId(user)) {
      user.setId(new ObjectId())
    }

    user
  }

  @Override
  boolean documentHasId(final User user) {
    user.id
  }

  @Override
  BsonValue getDocumentId(final User user) {

    if (!documentHasId(user)) {
      throw new IllegalStateException("The user does not contain an ${DBCollection.ID_FIELD_NAME}")
    }

    new BsonString(user.id as String)
  }
}
